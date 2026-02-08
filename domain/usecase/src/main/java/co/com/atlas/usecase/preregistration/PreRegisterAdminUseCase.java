package co.com.atlas.usecase.preregistration;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.preregistration.AdminActivationToken;
import co.com.atlas.model.preregistration.ActivationTokenStatus;
import co.com.atlas.model.preregistration.PreRegistrationAuditAction;
import co.com.atlas.model.preregistration.PreRegistrationAuditLog;
import co.com.atlas.model.preregistration.gateways.AdminActivationTokenRepository;
import co.com.atlas.model.preregistration.gateways.PreRegistrationAuditRepository;
import lombok.RequiredArgsConstructor;
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
 * Caso de uso para pre-registro de administradores.
 * 
 * Este caso de uso permite a operadores de la plataforma crear usuarios
 * pre-registrados que recibirán credenciales temporales por email.
 */
@RequiredArgsConstructor
public class PreRegisterAdminUseCase {
    
    private final AuthUserRepository authUserRepository;
    private final AdminActivationTokenRepository tokenRepository;
    private final PreRegistrationAuditRepository auditRepository;
    private final NotificationGateway notificationGateway;
    
    // Configuración de expiración (puede ser inyectada desde configuración)
    private static final int DEFAULT_EXPIRATION_HOURS = 24;
    private static final int MIN_EXPIRATION_HOURS = 1;  // 1 hora mínimo
    private static final int MAX_EXPIRATION_HOURS = 168; // 7 días máximo
    
    // Longitud de la contraseña temporal
    private static final int TEMP_PASSWORD_LENGTH = 12;
    
    /**
     * Comando para pre-registro de administrador.
     */
    public record PreRegisterCommand(
            String email,
            String names,
            String phone,
            Integer expirationHours,
            String baseActivationUrl,
            Long operatorId,
            String operatorIp,
            String operatorUserAgent,
            String metadata
    ) {}
    
    /**
     * Resultado del pre-registro.
     */
    public record PreRegisterResult(
            Long userId,
            Long tokenId,
            String email,
            String names,
            Instant expiresAt,
            String message
    ) {}
    
    /**
     * Ejecuta el pre-registro de un administrador.
     * 
     * @param command Datos del pre-registro
     * @return Resultado del pre-registro (sin exponer contraseña)
     */
    public Mono<PreRegisterResult> execute(PreRegisterCommand command) {
        return validateCommand(command)
                .then(checkEmailUnique(command.email()))
                .then(createPreRegisteredUser(command))
                .flatMap(user -> createActivationTokenAndNotify(user, command));
    }
    
    private Mono<Void> validateCommand(PreRegisterCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            return Mono.error(new BusinessException("El email es requerido", "INVALID_EMAIL"));
        }
        if (command.names() == null || command.names().isBlank()) {
            return Mono.error(new BusinessException("El nombre es requerido", "INVALID_NAME"));
        }
        if (command.baseActivationUrl() == null || command.baseActivationUrl().isBlank()) {
            return Mono.error(new BusinessException("La URL de activación es requerida", "INVALID_URL"));
        }
        
        int expHours = command.expirationHours() != null ? command.expirationHours() : DEFAULT_EXPIRATION_HOURS;
        if (expHours < MIN_EXPIRATION_HOURS || expHours > MAX_EXPIRATION_HOURS) {
            return Mono.error(new BusinessException(
                    String.format("Las horas de expiración deben estar entre %d y %d", 
                            MIN_EXPIRATION_HOURS, MAX_EXPIRATION_HOURS),
                    "INVALID_EXPIRATION"));
        }
        
        return Mono.empty();
    }
    
    private Mono<Void> checkEmailUnique(String email) {
        return authUserRepository.findByEmail(email)
                .flatMap(existing -> {
                    // Si el usuario existe pero está en PRE_REGISTERED, permitir re-enviar
                    if (existing.getStatus() == UserStatus.PRE_REGISTERED) {
                        return Mono.error(new DuplicateException(
                                "Usuario ya pre-registrado. Use el endpoint de reenvío.", 
                                "email", email));
                    }
                    return Mono.error(new DuplicateException("Usuario", "email", email));
                })
                .then();
    }
    
    private Mono<AuthUser> createPreRegisteredUser(PreRegisterCommand command) {
        // Generar contraseña temporal (se enviará por email, se guarda el hash)
        String tempPassword = generateSecurePassword();
        
        AuthUser newUser = AuthUser.builder()
                .names(command.names())
                .email(command.email())
                .passwordHash(tempPassword) // El repository debe hacer el hash
                .phone(command.phone())
                .active(false) // No activo hasta completar activación
                .status(UserStatus.PRE_REGISTERED)
                .build();
        
        return authUserRepository.save(newUser);
    }
    
    private Mono<PreRegisterResult> createActivationTokenAndNotify(AuthUser user, PreRegisterCommand command) {
        // Generar token de activación
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        
        int expHours = command.expirationHours() != null ? command.expirationHours() : DEFAULT_EXPIRATION_HOURS;
        Instant expiresAt = Instant.now().plus(expHours, ChronoUnit.HOURS);
        
        // Generar contraseña temporal para el email
        String tempPassword = generateSecurePassword();
        
        AdminActivationToken token = AdminActivationToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .initialPasswordHash(tempPassword) // El adapter debe hacer el hash
                .expiresAt(expiresAt)
                .status(ActivationTokenStatus.PENDING)
                .createdBy(command.operatorId())
                .ipAddress(command.operatorIp())
                .userAgent(command.operatorUserAgent())
                .metadata(command.metadata())
                .build();
        
        String activationUrl = buildActivationUrl(command.baseActivationUrl(), rawToken);
        String expiresAtFormatted = formatDateTime(expiresAt);
        
        return tokenRepository.save(token)
                .flatMap(savedToken -> 
                    // Actualizar usuario con passwordHash real
                    authUserRepository.save(user.toBuilder()
                            .passwordHash(tempPassword) // El adapter hará el hash
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
                            .build())
                    .thenReturn(savedToken)
                )
                .flatMap(savedToken ->
                    notificationGateway.sendAdminPreRegistrationEmail(
                            user.getEmail(),
                            user.getNames(),
                            tempPassword, // Contraseña en claro solo para el email
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
                    .thenReturn(savedToken)
                )
                .map(savedToken -> new PreRegisterResult(
                        user.getId(),
                        savedToken.getId(),
                        user.getEmail(),
                        user.getNames(),
                        expiresAt,
                        "Pre-registro creado exitosamente. Email enviado."
                ));
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
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
    
    private String buildActivationUrl(String baseUrl, String token) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "token=" + token;
    }
    
    private String formatDateTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("America/Bogota"));
        return formatter.format(instant);
    }
}
