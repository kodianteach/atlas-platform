package co.com.atlas.usecase.preregistration;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.DocumentType;
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
    private final RoleRepository roleRepository;
    private final UserRoleMultiRepository userRoleMultiRepository;
    
    // Código del rol de administrador
    private static final String ADMIN_ATLAS_ROLE_CODE = "ADMIN_ATLAS";
    
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
            String documentType,
            String documentNumber,
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
            String documentType,
            String documentNumberMasked,
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
                .flatMap(user -> assignAdminRole(user)
                        .then(createActivationTokenAndNotify(user, command)));
    }
    
    /**
     * Asigna el rol ADMIN_ATLAS al usuario sin organización (organization_id = null).
     * El organization_id se asignará cuando complete el onboarding.
     */
    private Mono<Void> assignAdminRole(AuthUser user) {
        return roleRepository.findByCode(ADMIN_ATLAS_ROLE_CODE)
                .switchIfEmpty(Mono.error(new BusinessException(
                        "Rol ADMIN_ATLAS no encontrado en el sistema", "ROLE_NOT_FOUND")))
                .flatMap(role -> {
                    UserRoleMulti userRole = UserRoleMulti.builder()
                            .userId(user.getId())
                            .organizationId(null) // Sin organización hasta completar onboarding
                            .roleId(role.getId())
                            .isPrimary(true)
                            .build();
                    return userRoleMultiRepository.save(userRole);
                })
                .then();
    }
    
    private Mono<Void> validateCommand(PreRegisterCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            return Mono.error(new BusinessException("El email es requerido", "INVALID_EMAIL"));
        }
        if (command.names() == null || command.names().isBlank()) {
            return Mono.error(new BusinessException("El nombre es requerido", "INVALID_NAME"));
        }
        if (command.documentType() == null || command.documentType().isBlank()) {
            return Mono.error(new BusinessException("El tipo de documento es requerido", "INVALID_DOCUMENT_TYPE"));
        }
        if (command.documentNumber() == null || command.documentNumber().isBlank()) {
            return Mono.error(new BusinessException("El número de documento es requerido", "INVALID_DOCUMENT_NUMBER"));
        }
        // Validar que el tipo de documento sea válido
        try {
            DocumentType.valueOf(command.documentType());
        } catch (IllegalArgumentException e) {
            return Mono.error(new BusinessException(
                    "Tipo de documento inválido. Valores permitidos: CC, CE, NIT, PA, TI, PEP", 
                    "INVALID_DOCUMENT_TYPE"));
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
        
        DocumentType docType = DocumentType.valueOf(command.documentType());
        
        AuthUser newUser = AuthUser.builder()
                .names(command.names())
                .email(command.email())
                .documentType(docType)
                .documentNumber(command.documentNumber())
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
                .metadata(toJsonMetadata(command.metadata()))
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
                        user.getDocumentType() != null ? user.getDocumentType().name() : null,
                        maskDocumentNumber(user.getDocumentNumber()),
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
    
    /**
     * Enmascara un número de documento mostrando solo los últimos 4 caracteres.
     * Ejemplo: "1234567890" -> "****7890"
     */
    private String maskDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.length() <= 4) {
            return "****";
        }
        int visibleChars = 4;
        int maskedLength = documentNumber.length() - visibleChars;
        return "*".repeat(maskedLength) + documentNumber.substring(maskedLength);
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
    
    /**
     * Convierte el metadata a JSON válido.
     * Si ya es JSON válido, lo retorna tal como está.
     * Si es texto plano, lo envuelve en un objeto JSON.
     */
    private String toJsonMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return "{}";
        }
        String trimmed = metadata.trim();
        // Si ya parece ser JSON válido (objeto o array), retornarlo
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || 
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        // Envolver texto plano en JSON válido
        // Escapar caracteres especiales para JSON
        String escaped = trimmed
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"message\": \"" + escaped + "\"}";
    }
}
