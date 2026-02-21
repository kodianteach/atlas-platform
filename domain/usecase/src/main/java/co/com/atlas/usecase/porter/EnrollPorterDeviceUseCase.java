package co.com.atlas.usecase.porter;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyGeneratorGateway;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.porter.DeviceInfo;
import co.com.atlas.model.porter.EnrollmentResult;
import co.com.atlas.model.porter.PorterEnrollmentAuditAction;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

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

    private final PorterEnrollmentTokenRepository tokenRepository;
    private final PorterEnrollmentAuditRepository auditRepository;
    private final AuthUserRepository authUserRepository;
    private final OrganizationRepository organizationRepository;
    private final CryptoKeyRepository cryptoKeyRepository;
    private final CryptoKeyGeneratorGateway cryptoKeyGeneratorGateway;

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
        // Paso 1: Obtener o generar claves para la organización
        Mono<OrganizationCryptoKey> cryptoKeyMono = getOrCreateCryptoKey(token.getOrganizationId());

        // Paso 2: Activar usuario portero
        Mono<AuthUser> activatedUserMono = activatePorterUser(token.getUserId());

        // Paso 3: Obtener nombre de organización
        Mono<Organization> orgMono = organizationRepository.findById(token.getOrganizationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Organización no encontrada")));

        return Mono.zip(cryptoKeyMono, activatedUserMono, orgMono)
                .flatMap(tuple -> {
                    OrganizationCryptoKey cryptoKey = tuple.getT1();
                    AuthUser user = tuple.getT2();
                    Organization org = tuple.getT3();

                    // Paso 4: Registrar auditoría
                    return saveAuditLog(token, command)
                            .thenReturn(new EnrollmentResult(
                                    user.getId(),
                                    user.getNames(),
                                    org.getName(),
                                    cryptoKey.getPublicKeyJwk(),
                                    cryptoKey.getKeyId(),
                                    DEFAULT_MAX_CLOCK_SKEW_MINUTES
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
     */
    private Mono<AuthUser> activatePorterUser(Long userId) {
        return authUserRepository.findById(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Usuario portero no encontrado")))
                .flatMap(user -> {
                    if (user.getStatus() == UserStatus.ACTIVE) {
                        return Mono.just(user); // ya activado (idempotente)
                    }
                    AuthUser activated = user.toBuilder()
                            .status(UserStatus.ACTIVE)
                            .active(true)
                            .build();
                    return authUserRepository.save(activated);
                });
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
}
