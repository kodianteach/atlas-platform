package co.com.atlas.usecase.preregistration;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.preregistration.AdminActivationToken;
import co.com.atlas.model.preregistration.ActivationTokenStatus;
import co.com.atlas.model.preregistration.PreRegistrationAuditAction;
import co.com.atlas.model.preregistration.PreRegistrationAuditLog;
import co.com.atlas.model.preregistration.gateways.AdminActivationTokenRepository;
import co.com.atlas.model.preregistration.gateways.PreRegistrationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Caso de uso para reenvío de email de pre-registro.
 * Permite reenviar el email de activación a un usuario ya pre-registrado.
 */
@RequiredArgsConstructor

public class ResendPreRegistrationUseCase {
    
    private final AuthUserRepository authUserRepository;
    private final AdminActivationTokenRepository tokenRepository;
    private final PreRegistrationAuditRepository auditRepository;
    private final NotificationGateway notificationGateway;
    
    private static final int DEFAULT_EXPIRATION_HOURS = 24;
    private static final int TEMP_PASSWORD_LENGTH = 12;
    
    /**
     * Comando para reenvío de pre-registro.
     */
    public record ResendCommand(
            String email,
            String baseActivationUrl,
            Integer expirationHours,
            Long operatorId,
            String operatorIp,
            String operatorUserAgent
    ) {}
    
    /**
     * Resultado del reenvío.
     */
    public record ResendResult(
            Long userId,
            Long tokenId,
            String email,
            String names,
            Instant expiresAt,
            String message
    ) {}
    
    /**
     * Ejecuta el reenvío de email de pre-registro.
     */
    public Mono<ResendResult> execute(ResendCommand command) {

        return authUserRepository.findByEmail(command.email())
                .switchIfEmpty(Mono.error(new BusinessException(
                        "Usuario no encontrado con email: " + command.email(), 
                        "USER_NOT_FOUND")))
                .flatMap(user -> {
                    if (user.getStatus() != UserStatus.PRE_REGISTERED) {
                        return Mono.error(new BusinessException(
                                "El usuario no está en estado pre-registrado. Estado actual: " + user.getStatus(),
                                "INVALID_USER_STATUS"));
                    }
                    return invalidateExistingTokens(user.getId())
                            .then(createNewTokenAndNotify(user, command));
                });
    }
    
    private Mono<Void> invalidateExistingTokens(Long userId) {
        return tokenRepository.findByUserId(userId)
                .filter(token -> token.getStatus() == ActivationTokenStatus.PENDING)
                .flatMap(token -> {
                    AdminActivationToken expired = token.toBuilder()
                            .status(ActivationTokenStatus.EXPIRED)
                            .updatedAt(Instant.now())
                            .build();
                    return tokenRepository.save(expired);
                })
                .then();
    }
    
    private Mono<ResendResult> createNewTokenAndNotify(AuthUser user, ResendCommand command) {
        // Generar nuevo token y contraseña temporal
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        String tempPassword = generateSecurePassword();
        
        int expHours = command.expirationHours() != null ? command.expirationHours() : DEFAULT_EXPIRATION_HOURS;
        Instant expiresAt = Instant.now().plus(expHours, ChronoUnit.HOURS);
        
        AdminActivationToken token = AdminActivationToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .initialPasswordHash(tempPassword)
                .expiresAt(expiresAt)
                .status(ActivationTokenStatus.PENDING)
                .createdBy(command.operatorId())
                .ipAddress(command.operatorIp())
                .userAgent(command.operatorUserAgent())
                .metadata("{\"action\": \"resend\", \"timestamp\": \"" + Instant.now().toString() + "\"}")
                .build();
        
        String activationUrl = buildActivationUrl(command.baseActivationUrl(), rawToken);
        String expiresAtFormatted = formatDateTime(expiresAt);
        
        return tokenRepository.save(token)
                .flatMap(savedToken -> 
                    // Actualizar contraseña del usuario
                    authUserRepository.save(user.toBuilder()
                            .passwordHash(tempPassword)
                            .build())
                    .thenReturn(savedToken)
                )
                .flatMap(savedToken -> 
                    auditRepository.save(PreRegistrationAuditLog.builder()
                            .tokenId(savedToken.getId())
                            .action(PreRegistrationAuditAction.CREATED)
                            .performedBy(command.operatorId())
                            .ipAddress(command.operatorIp())
                            .userAgent(command.operatorUserAgent())
                            .details("{\"action\": \"token_resent\", \"timestamp\": \"" + Instant.now().toString() + "\"}")
                            .build())
                    .thenReturn(savedToken)
                )
                .flatMap(savedToken -> {

                    
                    return notificationGateway.sendAdminPreRegistrationEmail(
                            user.getEmail(),
                            user.getNames(),
                            tempPassword,
                            activationUrl,
                            expiresAtFormatted,
                            expHours
                    )

                    .then(auditRepository.save(PreRegistrationAuditLog.builder()
                            .tokenId(savedToken.getId())
                            .action(PreRegistrationAuditAction.EMAIL_SENT)
                            .performedBy(command.operatorId())
                            .ipAddress(command.operatorIp())
                            .build()))
                    .thenReturn(new ResendResult(
                            user.getId(),
                            savedToken.getId(),
                            user.getEmail(),
                            user.getNames(),
                            expiresAt,
                            "Email de pre-registro reenviado exitosamente"
                    ));
                });
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateSecurePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    private String buildActivationUrl(String baseUrl, String token) {
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return url + "activate?token=" + token;
    }
    
    private String formatDateTime(Instant instant) {
        return DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.of("America/Bogota"))
                .format(instant);
    }
}
