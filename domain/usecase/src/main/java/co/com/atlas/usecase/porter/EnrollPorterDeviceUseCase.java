package co.com.atlas.usecase.porter;

import co.com.atlas.model.auth.AuthToken;
import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyGeneratorGateway;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.permission.Permission;
import co.com.atlas.model.permission.gateways.PermissionRepository;
import co.com.atlas.model.porter.DeviceInfo;
import co.com.atlas.model.porter.EnrollmentResult;
import co.com.atlas.model.porter.PorterEnrollmentAuditAction;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.role.Role;
import co.com.atlas.model.role.gateways.RoleRepository;
import co.com.atlas.model.userorganization.UserOrganization;
import co.com.atlas.model.userorganization.gateways.UserOrganizationRepository;
import co.com.atlas.model.userrolemulti.gateways.UserRoleMultiRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.List;

import java.time.Instant;

/**
 * Caso de uso para enrolar un dispositivo de portería.
 *
 * Flujo:
 * 1. Validar token (hash SHA-256, estado PENDING, no expirado)
 * 2. Consumir token (PENDING → CONSUMED)
 * 3. Obtener o generar claves Ed25519 para la organización (lazy)
 * 4. Activar usuario portero (PRE_REGISTERED → ACTIVE)
 * 5. Registrar auditoría (device_info, IP, user-agent)
 * 6. Retornar EnrollmentResult con verificationKeyJwk + keyId
 */
@RequiredArgsConstructor
public class EnrollPorterDeviceUseCase {

    private static final int DEFAULT_MAX_CLOCK_SKEW_MINUTES = 5;
    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final PorterEnrollmentTokenRepository tokenRepository;
    private final PorterEnrollmentAuditRepository auditRepository;
    private final AuthUserRepository authUserRepository;
    private final OrganizationRepository organizationRepository;
    private final CryptoKeyRepository cryptoKeyRepository;
    private final CryptoKeyGeneratorGateway cryptoKeyGeneratorGateway;
    private final JwtTokenGateway jwtTokenGateway;
    private final UserRoleMultiRepository userRoleMultiRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    /**
     * Comando de enrolamiento con datos del dispositivo.
     */
    public record EnrollCommand(
            String token,
            DeviceInfo deviceInfo,
            String clientIp,
            String clientUserAgent
    ) {}

    /**
     * Ejecuta el enrolamiento del dispositivo de portería.
     *
     * @param command Datos del enrolamiento
     * @return Resultado con clave pública JWK para verificación offline
     */
    public Mono<EnrollmentResult> execute(EnrollCommand command) {
        if (command.token() == null || command.token().isBlank()) {
            return Mono.error(new BusinessException(
                    "Token de enrolamiento requerido", "TOKEN_REQUIRED"));
        }

        String tokenHash = ValidateEnrollmentTokenUseCase.hashToken(command.token());

        return tokenRepository.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new NotFoundException("Token inválido o no encontrado")))
                .flatMap(this::validateAndConsumeToken)
                .flatMap(token -> processEnrollment(token, command));
    }

    private Mono<PorterEnrollmentToken> validateAndConsumeToken(PorterEnrollmentToken token) {
        if (token.getStatus() == PorterEnrollmentTokenStatus.CONSUMED) {
            return Mono.error(new BusinessException(
                    "Este token ya fue utilizado", "TOKEN_ALREADY_USED"));
        }
        if (token.getStatus() == PorterEnrollmentTokenStatus.REVOKED) {
            return Mono.error(new BusinessException(
                    "Este token fue revocado", "TOKEN_REVOKED"));
        }
        if (token.getStatus() == PorterEnrollmentTokenStatus.EXPIRED || token.isExpired()) {
            return Mono.error(new BusinessException(
                    "Este token ha expirado", "TOKEN_EXPIRED"));
        }
        if (token.getStatus() != PorterEnrollmentTokenStatus.PENDING) {
            return Mono.error(new BusinessException(
                    "Token en estado inválido", "TOKEN_INVALID_STATE"));
        }

        // Consumir token
        PorterEnrollmentToken consumed = token.toBuilder()
                .status(PorterEnrollmentTokenStatus.CONSUMED)
                .consumedAt(Instant.now())
                .build();

        return tokenRepository.save(consumed);
    }

    private Mono<EnrollmentResult> processEnrollment(PorterEnrollmentToken token,
                                                      EnrollCommand command) {
        // Paso 0: Generar contraseña segura para futuros login
        String rawPassword = generatePassword();

        // Paso 1: Obtener o generar claves para la organización
        Mono<OrganizationCryptoKey> cryptoKeyMono = getOrCreateCryptoKey(token.getOrganizationId());

        // Paso 2: Activar usuario portero y asignar contraseña
        Mono<AuthUser> activatedUserMono = activatePorterUser(token.getUserId(), rawPassword);

        // Paso 3: Obtener nombre de organización
        Mono<Organization> orgMono = organizationRepository.findById(token.getOrganizationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Organización no encontrada")));

        return Mono.zip(cryptoKeyMono, activatedUserMono, orgMono)
                .flatMap(tuple -> {
                    OrganizationCryptoKey cryptoKey = tuple.getT1();
                    AuthUser user = tuple.getT2();
                    Organization org = tuple.getT3();

                    // Paso 4: Crear membresía user_organizations + set lastOrganizationId
                    // Paso 5: Registrar auditoría
                    // Paso 6: Generar JWT para sesión automática
                    return createUserOrganizationMembership(user.getId(), token.getOrganizationId())
                            .then(authUserRepository.updateLastOrganization(user.getId(), token.getOrganizationId()))
                            .then(saveAuditLog(token, command))
                            .then(generatePorterJwt(user, token.getOrganizationId()))
                            .map(authToken -> new EnrollmentResult(
                                    user.getId(),
                                    user.getNames(),
                                    org.getName(),
                                    cryptoKey.getPublicKeyJwk(),
                                    cryptoKey.getKeyId(),
                                    DEFAULT_MAX_CLOCK_SKEW_MINUTES,
                                    authToken.getAccessToken(),
                                    authToken.getRefreshToken(),
                                    authToken.getDefaultRoute(),
                                    user.getUsername(),
                                    rawPassword
                            ));
                });
    }

    /**
     * Obtiene la clave activa de la organización o genera una nueva (lazy).
     */
    private Mono<OrganizationCryptoKey> getOrCreateCryptoKey(Long organizationId) {
        return cryptoKeyRepository.findActiveByOrganizationId(organizationId)
                .switchIfEmpty(Mono.defer(() ->
                        cryptoKeyGeneratorGateway.generateForOrganization(organizationId)
                                .flatMap(cryptoKeyRepository::save)
                ));
    }

    /**
     * Activa el usuario portero: PRE_REGISTERED → ACTIVE, active = true.
     * Asigna la contraseña en texto plano — el adapter se encarga de hashearla.
     */
    private Mono<AuthUser> activatePorterUser(Long userId, String rawPassword) {
        return authUserRepository.findById(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Usuario portero no encontrado")))
                .flatMap(user -> {
                    if (user.getStatus() == UserStatus.ACTIVE && user.getPasswordHash() != null) {
                        return Mono.just(user); // ya activado (idempotente)
                    }
                    AuthUser activated = user.toBuilder()
                            .status(UserStatus.ACTIVE)
                            .active(true)
                            .passwordHash(rawPassword)
                            .build();
                    return authUserRepository.save(activated);
                });
    }

    /**
     * Crea la membresía del portero en user_organizations si no existe.
     */
    private Mono<Void> createUserOrganizationMembership(Long userId, Long organizationId) {
        return userOrganizationRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .switchIfEmpty(Mono.defer(() -> {
                    UserOrganization membership = UserOrganization.builder()
                            .userId(userId)
                            .organizationId(organizationId)
                            .status("ACTIVE")
                            .joinedAt(Instant.now())
                            .build();
                    return userOrganizationRepository.save(membership);
                }))
                .then();
    }

    /**
     * Genera JWT para el portero recién enrolado, cargando roles y permisos.
     */
    private Mono<AuthToken> generatePorterJwt(AuthUser user, Long organizationId) {
        return userRoleMultiRepository.findByUserIdAndOrganizationId(user.getId(), organizationId)
                .flatMap(urm -> roleRepository.findById(urm.getRoleId()))
                .collectList()
                .flatMap(roles -> loadPermissionsForRoles(roles)
                        .map(permissions -> user.toBuilder()
                                .organizationId(organizationId)
                                .roles(roles)
                                .permissions(permissions)
                                .enabledModules(List.of("ATLAS_CORE"))
                                .build()))
                .flatMap(jwtTokenGateway::generateTokenPair);
    }

    private Mono<List<Permission>> loadPermissionsForRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Mono.just(List.of());
        }
        return Flux.fromIterable(roles)
                .flatMap(role -> permissionRepository.findByRoleId(role.getId()))
                .distinct(Permission::getCode)
                .collectList();
    }

    private Mono<PorterEnrollmentAuditLog> saveAuditLog(PorterEnrollmentToken token,
                                                         EnrollCommand command) {
        String details = formatDeviceDetails(command.deviceInfo());

        PorterEnrollmentAuditLog auditLog = PorterEnrollmentAuditLog.builder()
                .tokenId(token.getId())
                .action(PorterEnrollmentAuditAction.CONSUMED)
                .performedBy(token.getUserId())
                .ipAddress(command.clientIp())
                .userAgent(command.clientUserAgent())
                .details(details)
                .createdAt(Instant.now())
                .build();

        return auditRepository.save(auditLog);
    }

    private String formatDeviceDetails(DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return "{}";
        }
        return String.format(
                "{\"platform\":\"%s\",\"model\":\"%s\",\"appVersion\":\"%s\"}",
                nullSafe(deviceInfo.platform()),
                nullSafe(deviceInfo.model()),
                nullSafe(deviceInfo.appVersion())
        );
    }

    private String nullSafe(String value) {
        return value != null ? value.replace("\"", "\\\"") : "";
    }

    /**
     * Genera una contraseña alfanumérica segura (sin caracteres ambiguos: 0/O, 1/l/I).
     */
    private String generatePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
