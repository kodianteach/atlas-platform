package co.com.atlas.model.notification.gateways;

import reactor.core.publisher.Mono;

/**
 * Gateway para envío de notificaciones.
 * Abstrae el mecanismo de envío de notificaciones.
 */
public interface NotificationGateway {
    
    /**
     * Envía una notificación por email genérica.
     * 
     * @param to dirección de correo destino
     * @param subject asunto del correo
     * @param body cuerpo del mensaje (puede ser HTML)
     * @return Mono vacío cuando se completa
     */
    Mono<Void> sendEmail(String to, String subject, String body);
    
    /**
     * Envía email de pre-registro de administrador con credenciales temporales.
     * 
     * @param to dirección de correo del administrador pre-registrado
     * @param adminName nombre del administrador
     * @param temporaryPassword contraseña temporal (en claro, solo para el email)
     * @param activationUrl URL completa de activación con token
     * @param expiresAt fecha de expiración formateada
     * @param expirationHours horas hasta expiración
     * @return Mono vacío cuando se completa
     */
    Mono<Void> sendAdminPreRegistrationEmail(
            String to,
            String adminName,
            String temporaryPassword,
            String activationUrl,
            String expiresAt,
            int expirationHours
    );
    
    /**
     * Envía invitación a organización por email.
     * 
     * @param to dirección de correo destino
     * @param organizationName nombre de la organización
     * @param invitationUrl URL de la invitación
     * @param invitedByName nombre de quien envía la invitación
     * @param expiresAt fecha de expiración de la invitación
     * @return Mono vacío cuando se completa
     */
    Mono<Void> sendOrganizationInvitationEmail(
            String to,
            String organizationName,
            String invitationUrl,
            String invitedByName,
            String expiresAt
    );
    
    /**
     * Envía confirmación de activación de cuenta.
     * 
     * @param to dirección de correo destino
     * @param userName nombre del usuario
     * @return Mono vacío cuando se completa
     */
    Mono<Void> sendActivationConfirmationEmail(String to, String userName);
}
