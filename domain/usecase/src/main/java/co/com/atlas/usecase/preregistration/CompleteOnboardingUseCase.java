package co.com.atlas.usecase.preregistration;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.company.Company;
import co.com.atlas.model.company.gateways.CompanyRepository;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.OrganizationType;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.userorganization.UserOrganization;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userrolemulti.UserRoleMulti;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Caso de uso para completar el onboarding de un administrador.
 * 
 * Este caso de uso permite a usuarios con estado ACTIVATED crear
 * su compañía y organización inicial.
 */
@RequiredArgsConstructor
public class CompleteOnboardingUseCase {
    
    private final AuthUserRepository authUserRepository;
    private final CompanyRepository companyRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final RoleRepository roleRepository;
    private final UserRoleMultiRepository userRoleMultiRepository;
    
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final String ADMIN_ATLAS_ROLE_CODE = "ADMIN_ATLAS";
    
    /**
     * Comando para completar onboarding.
     */
    public record OnboardingCommand(
            Long userId,
            String companyName,
            String companyTaxId,
            String companyIndustry,
            String companyAddress,
            String companyCountry,
            String companyCity,
            String organizationName,
            String organizationCode,
            String organizationType,
            Boolean usesZones,
            String organizationDescription
    ) {}
    
    /**
     * Resultado del onboarding.
     */
    public record OnboardingResult(
            Long companyId,
            String companySlug,
            Long organizationId,
            String organizationCode,
            String message
    ) {}
    
    /**
     * Ejecuta el onboarding completo.
     * 
     * @param command Datos del onboarding
     * @return Resultado con IDs de company y organization creados
     */
    public Mono<OnboardingResult> execute(OnboardingCommand command) {
        return validateUserStatus(command.userId())
                .then(Mono.defer(() -> validateCommand(command)))
                .then(Mono.defer(() -> createCompanyAndOrganization(command)));
    }
    
    private Mono<AuthUser> validateUserStatus(Long userId) {
        return authUserRepository.findById(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Usuario", userId)))
                .flatMap(user -> {
                    if (user.getStatus() == null) {
                        return Mono.error(new BusinessException(
                                "El usuario no tiene un estado válido", "INVALID_USER_STATUS"));
                    }
                    
                    // Solo usuarios ACTIVATED pueden completar onboarding
                    if (user.getStatus() != UserStatus.ACTIVATED) {
                        String errorMsg = switch (user.getStatus()) {
                            case PRE_REGISTERED -> "Debe activar su cuenta antes de crear una organización";
                            case PENDING_ACTIVATION -> "Debe completar el proceso de activación";
                            case ACTIVE -> "Este usuario ya completó el onboarding";
                            case SUSPENDED -> "Esta cuenta está suspendida";
                            case ACTIVATED -> "Estado inesperado"; // No debería llegar aquí
                        };
                        return Mono.error(new BusinessException(errorMsg, "INVALID_STATUS_FOR_ONBOARDING"));
                    }
                    
                    return Mono.just(user);
                });
    }
    
    private Mono<Void> validateCommand(OnboardingCommand command) {
        if (command.companyName() == null || command.companyName().isBlank()) {
            return Mono.error(new BusinessException(
                    "El nombre de la compañía es requerido", "INVALID_COMPANY_NAME"));
        }
        if (command.organizationName() == null || command.organizationName().isBlank()) {
            return Mono.error(new BusinessException(
                    "El nombre de la organización es requerido", "INVALID_ORGANIZATION_NAME"));
        }
        if (command.organizationType() == null || command.organizationType().isBlank()) {
            return Mono.error(new BusinessException(
                    "El tipo de organización es requerido (CIUDADELA, CONJUNTO o CONDOMINIO)", 
                    "INVALID_ORGANIZATION_TYPE"));
        }
        
        String orgType = command.organizationType().toUpperCase();
        if (!orgType.equals("CIUDADELA") && !orgType.equals("CONJUNTO") && !orgType.equals("CONDOMINIO")) {
            return Mono.error(new BusinessException(
                    "El tipo de organización debe ser CIUDADELA, CONJUNTO o CONDOMINIO", 
                    "INVALID_ORGANIZATION_TYPE"));
        }
        
        return Mono.empty();
    }
    
    private Mono<OnboardingResult> createCompanyAndOrganization(OnboardingCommand command) {
        String companySlug = generateSlug(command.companyName());
        
        // Verificar que el slug de company no exista
        return companyRepository.existsBySlug(companySlug)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new BusinessException(
                                "Ya existe una compañía con nombre similar", "DUPLICATE_COMPANY"));
                    }
                    return createCompany(command, companySlug);
                })
                .flatMap(company -> createOrganization(command, company)
                        .flatMap(org -> createUserOrganizationLink(command.userId(), org)
                                .then(updateUserToActive(command.userId()))
                                .thenReturn(new OnboardingResult(
                                        company.getId(),
                                        company.getSlug(),
                                        org.getId(),
                                        org.getCode(),
                                        "Onboarding completado exitosamente"
                                ))
                        )
                );
    }
    
    private Mono<Company> createCompany(OnboardingCommand command, String slug) {
        Company company = Company.builder()
                .name(command.companyName())
                .slug(slug)
                .taxId(command.companyTaxId())
                .industry(command.companyIndustry())
                .address(command.companyAddress())
                .country(command.companyCountry())
                .city(command.companyCity())
                .status("ACTIVE")
                .isActive(true)
                .build();
        
        return companyRepository.save(company);
    }
    
    private Mono<Organization> createOrganization(OnboardingCommand command, Company company) {
        String orgCode = command.organizationCode() != null && !command.organizationCode().isBlank()
                ? command.organizationCode()
                : generateOrgCode(command.organizationName());
        
        String orgSlug = generateSlug(command.organizationName());
        OrganizationType orgType = OrganizationType.valueOf(command.organizationType().toUpperCase());
        
        // Determinar allowedUnitTypes según tipo de organización
        String allowedUnitTypes = switch (orgType) {
            case CIUDADELA -> "HOUSE,APARTMENT"; // Ciudadela permite ambos tipos
            case CONJUNTO -> "HOUSE,APARTMENT";  // Conjunto permite ambos tipos
            case CONDOMINIO -> "HOUSE";           // Condominio solo permite casas
        };
        
        Organization org = Organization.builder()
                .companyId(company.getId())
                .code(orgCode)
                .name(command.organizationName())
                .slug(orgSlug)
                .type(orgType)
                .usesZones(command.usesZones() != null ? command.usesZones() : true)
                .allowedUnitTypes(allowedUnitTypes)
                .description(command.organizationDescription())
                .status("ACTIVE")
                .isActive(true)
                .build();
        
        return organizationRepository.save(org);
    }
    
    private Mono<Void> createUserOrganizationLink(Long userId, Organization org) {
        // Crear el vínculo usuario-organización
        UserOrganization userOrg = UserOrganization.builder()
                .userId(userId)
                .organizationId(org.getId())
                .status("ACTIVE")
                .build();
        
        return userOrganizationRepository.save(userOrg)
                .then(assignAdminAtlasRole(userId, org.getId()));
    }
    
    /**
     * Asigna el rol ADMIN_ATLAS al usuario en la organización creada.
     * Si el usuario ya tiene el rol con organization_id = NULL (asignado durante pre-registro),
     * actualiza ese registro con el nuevo organization_id.
     * Si no existe, crea uno nuevo.
     */
    private Mono<Void> assignAdminAtlasRole(Long userId, Long organizationId) {
        return roleRepository.findByCode(ADMIN_ATLAS_ROLE_CODE)
                .switchIfEmpty(Mono.error(new BusinessException(
                        "Rol ADMIN_ATLAS no encontrado en el sistema", "ROLE_NOT_FOUND")))
                .flatMap(role -> 
                    // Buscar roles existentes sin organización (asignados en pre-registro)
                    userRoleMultiRepository.findByUserIdAndOrganizationIdIsNull(userId)
                            .filter(urm -> urm.getRoleId().equals(role.getId()))
                            .next()
                            .flatMap(existingRole -> {
                                // Actualizar el registro existente con el nuevo organization_id
                                UserRoleMulti updated = existingRole.toBuilder()
                                        .organizationId(organizationId)
                                        .build();
                                return userRoleMultiRepository.save(updated);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // No existe, crear nuevo
                                UserRoleMulti userRole = UserRoleMulti.builder()
                                        .userId(userId)
                                        .organizationId(organizationId)
                                        .roleId(role.getId())
                                        .isPrimary(true)
                                        .build();
                                return userRoleMultiRepository.save(userRole);
                            }))
                )
                .then();
    }
    
    private Mono<Void> updateUserToActive(Long userId) {
        return authUserRepository.findById(userId)
                .flatMap(user -> {
                    AuthUser updated = user.toBuilder()
                            .status(UserStatus.ACTIVE)
                            .build();
                    return authUserRepository.save(updated);
                })
                .then();
    }
    
    private String generateSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
    
    private String generateOrgCode(String name) {
        if (name == null) return "ORG";
        String code = name.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(name.length(), 10));
        return code.isEmpty() ? "ORG" : code;
    }
}
