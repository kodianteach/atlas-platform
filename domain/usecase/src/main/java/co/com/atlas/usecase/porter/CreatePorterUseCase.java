package co.com.atlas.usecase.porter;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterEnrollmentAuditAction;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.PorterType;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.userrolemulti.UserRoleMulti;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Caso de uso para creación de porteros.
 * Un portero es un usuario (tabla users) con rol PORTERO_GENERAL o PORTERO_DELIVERY.
 * No se crea registro en tabla separada; se reutiliza users + user_roles_multi.
 */
@RequiredArgsConstructor
public class CreatePorterUseCase {

    private final PorterEnrollmentTokenRepository tokenRepository;
    private final PorterEnrollmentAuditRepository auditRepository;
    private final AuthUserRepository authUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleMultiRepository userRoleMultiRepository;
    private final OrganizationRepository organizationRepository;

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final String INTERNAL_EMAIL_DOMAIN = "atlas.internal";
    private static final String ENROLLMENT_BASE_URL = "/porter-enroll";

    /**
     * Comando para creación de portero.
     */
    public record CreatePorterCommand(
            String displayName,
            PorterType porterType
    ) {}

    /**
     * Resultado de la creación de portero.
     */
    public record CreatePorterResult(
            Porter porter,
            String enrollmentUrl
    ) {}

    /**
     * Ejecuta la creación de un portero.
     *
     * @param command       Datos del portero a crear
     * @param organizationId ID de la organización
     * @param adminUserId    ID del admin que crea el portero
     */
    public Mono<CreatePorterResult> execute(CreatePorterCommand command, Long organizationId, Long adminUserId) {
        return validateCommand(command)
                .then(Mono.defer(() -> organizationRepository.findById(organizationId)
                            .switchIfEmpty(Mono.error(new BusinessException(
                                    "Organización no encontrada", "ORGANIZATION_NOT_FOUND")))
                            .flatMap(org -> createPorterUser(org, command)
                                    .flatMap(user -> assignPorterRole(user, command.porterType(), organizationId)
                                            .then(generateEnrollmentToken(user, org, adminUserId, command)
                                                    .flatMap(tokenAndUrl -> registerAudit(tokenAndUrl.token, adminUserId)
                                                            .thenReturn(new CreatePorterResult(
                                                                    toPorterProjection(user, command.porterType(), organizationId),
                                                                    tokenAndUrl.url))
                                                    )
                                            )
                                    )
                            )));
    }

    private Mono<Void> validateCommand(CreatePorterCommand command) {
        if (command.displayName() == null || command.displayName().isBlank()) {
            return Mono.error(new BusinessException(
                    "El nombre descriptivo es requerido", "INVALID_DISPLAY_NAME"));
        }
        if (command.porterType() == null) {
            return Mono.error(new BusinessException(
                    "El tipo de portero es requerido", "INVALID_PORTER_TYPE"));
        }
        return Mono.empty();
    }

    private Mono<AuthUser> createPorterUser(Organization org, CreatePorterCommand command) {
        String orgSlug = org.getSlug() != null ? org.getSlug() : String.valueOf(org.getId());
        String syntheticEmail = "porter-" + UUID.randomUUID() + "@" + orgSlug + "." + INTERNAL_EMAIL_DOMAIN;

        AuthUser newUser = AuthUser.builder()
                .names(command.displayName())
                .email(syntheticEmail)
                .passwordHash(null)
                .active(false)
                .status(UserStatus.PRE_REGISTERED)
                .build();

        return authUserRepository.save(newUser);
    }

    private Mono<Void> assignPorterRole(AuthUser user, PorterType porterType, Long organizationId) {
        String roleCode = porterType.name();
        return roleRepository.findByCode(roleCode)
                .switchIfEmpty(Mono.error(new BusinessException(
                        "Rol " + roleCode + " no encontrado en el sistema", "ROLE_NOT_FOUND")))
                .flatMap(role -> {
                    UserRoleMulti userRole = UserRoleMulti.builder()
                            .userId(user.getId())
                            .organizationId(organizationId)
                            .roleId(role.getId())
                            .isPrimary(true)
                            .build();
                    return userRoleMultiRepository.save(userRole);
                })
                .then();
    }

    private record TokenAndUrl(PorterEnrollmentToken token, String url) {}

    private Mono<TokenAndUrl> generateEnrollmentToken(AuthUser user, Organization org,
                                                       Long adminUserId, CreatePorterCommand command) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS);

        String metadata = String.format("{\"displayName\":\"%s\",\"porterType\":\"%s\"}",
                command.displayName(), command.porterType().name());

        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .userId(user.getId())
                .organizationId(org.getId())
                .tokenHash(tokenHash)
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(expiresAt)
                .createdBy(adminUserId)
                .metadata(metadata)
                .build();

        return tokenRepository.save(token)
                .map(savedToken -> {
                    String enrollmentUrl = ENROLLMENT_BASE_URL + "?token=" + rawToken;
                    return new TokenAndUrl(savedToken, enrollmentUrl);
                });
    }

    private Mono<Void> registerAudit(PorterEnrollmentToken token, Long adminUserId) {
        PorterEnrollmentAuditLog createdLog = PorterEnrollmentAuditLog.builder()
                .tokenId(token.getId())
                .action(PorterEnrollmentAuditAction.CREATED)
                .performedBy(adminUserId)
                .build();

        PorterEnrollmentAuditLog urlLog = PorterEnrollmentAuditLog.builder()
                .tokenId(token.getId())
                .action(PorterEnrollmentAuditAction.URL_GENERATED)
                .performedBy(adminUserId)
                .build();

        return auditRepository.save(createdLog)
                .then(auditRepository.save(urlLog))
                .then();
    }

    private Porter toPorterProjection(AuthUser user, PorterType porterType, Long organizationId) {
        return Porter.builder()
                .id(user.getId())
                .names(user.getNames())
                .email(user.getEmail())
                .porterType(porterType)
                .status(user.getStatus() != null ? user.getStatus().name() : "PRE_REGISTERED")
                .organizationId(organizationId)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
