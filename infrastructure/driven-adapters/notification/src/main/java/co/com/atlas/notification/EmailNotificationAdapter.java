package co.com.atlas.notification;

import co.com.atlas.model.notification.gateways.NotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Adapter de notificaciones por email.
 * Implementa el gateway de notificaciones usando Spring Mail.
 * Solo se activa cuando notification.enabled=true
 */
@Component
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements NotificationGateway {

    private final JavaMailSender mailSender;

    @Value("${notification.from-email:noreply@atlasplatform.com}")
    private String fromEmail;

    @Value("${notification.from-name:Atlas Platform}")
    private String fromName;

    @Value("${notification.enabled:true}")
    private boolean notificationEnabled;

    @Override
    public Mono<Void> sendEmail(String to, String subject, String htmlContent) {
        if (!notificationEnabled) {
            log.info("Notifications disabled. Skipping email to: {}", to);
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromEmail, fromName);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                log.info("Email sent successfully to: {}", to);
                return null;
            } catch (MessagingException e) {
                log.error("Failed to send email to: {}", to, e);
                throw new RuntimeException("Failed to send email", e);
            } catch (Exception e) {
                log.error("Unexpected error sending email to: {}", to, e);
                throw new RuntimeException("Unexpected error sending email", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    @Override
    public Mono<Void> sendAdminPreRegistrationEmail(String to, String adminName, 
            String temporaryPassword, String activationUrl, String expiresAt, int expirationHours) {
        String subject = "🔐 Atlas Platform - Activación de cuenta de administrador";
        String htmlContent = buildPreRegistrationEmail(adminName, to, temporaryPassword, 
                activationUrl, expiresAt, expirationHours);
        return sendEmail(to, subject, htmlContent);
    }

    @Override
    public Mono<Void> sendOrganizationInvitationEmail(String to, String organizationName, 
            String invitationUrl, String invitedByName, String expiresAt) {
        log.info("EmailNotificationAdapter: Sending organization invitation email to={}, org={}, url={}", to, organizationName, invitationUrl);
        String subject = String.format("Invitación a %s", organizationName);
        String htmlContent = buildOrganizationInvitationEmail(organizationName, 
                invitationUrl, invitedByName, expiresAt);
        return sendEmail(to, subject, htmlContent);
    }

    @Override
    public Mono<Void> sendActivationConfirmationEmail(String to, String userName) {
        String subject = "✅ Atlas Platform - Cuenta activada exitosamente";
        String htmlContent = buildActivationConfirmationEmail(userName);
        return sendEmail(to, subject, htmlContent);
    }

    private String buildPreRegistrationEmail(String adminName, String email, 
            String temporaryPassword, String activationUrl, String expiresAt, int expirationHours) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #1e3a5f 0%%, #2d5a87 100%%); color: white; padding: 30px 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px 25px; background-color: white; }
                    .credentials-box { background: linear-gradient(135deg, #f8f9fa 0%%, #e9ecef 100%%); padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #1e3a5f; }
                    .credentials-box h3 { margin-top: 0; color: #1e3a5f; }
                    .credential { margin: 10px 0; font-family: 'Courier New', monospace; font-size: 14px; }
                    .credential strong { color: #495057; }
                    .credential code { background-color: #fff; padding: 5px 10px; border-radius: 4px; border: 1px solid #dee2e6; display: inline-block; margin-top: 5px; }
                    .btn-container { text-align: center; margin: 30px 0; }
                    .btn-activate { display: inline-block; background: linear-gradient(135deg, #1e3a5f 0%%, #2d5a87 100%%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; }
                    .warning { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 8px; padding: 15px; margin: 20px 0; }
                    .warning-icon { font-size: 20px; }
                    .link-fallback { background-color: #f8f9fa; padding: 15px; margin: 20px 0; border-radius: 8px; word-break: break-all; font-size: 12px; border: 1px solid #dee2e6; }
                    .footer { text-align: center; padding: 20px; color: #6c757d; font-size: 12px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; }
                    .steps { margin: 20px 0; }
                    .step { display: flex; margin: 15px 0; }
                    .step-number { background-color: #1e3a5f; color: white; width: 30px; height: 30px; border-radius: 50%%; display: flex; align-items: center; justify-content: center; font-weight: bold; margin-right: 15px; flex-shrink: 0; }
                    .step-content { flex: 1; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🏢 Atlas Platform</h1>
                        <p style="margin: 5px 0 0 0; opacity: 0.9;">Administración de Organizaciones Residenciales</p>
                    </div>
                    <div class="content">
                        <h2>¡Bienvenido, %s!</h2>
                        <p>Has sido pre-registrado como <strong>Administrador</strong> en Atlas Platform. 
                        A continuación encontrarás tus credenciales temporales para activar tu cuenta.</p>
                        
                        <div class="credentials-box">
                            <h3>📧 Credenciales de acceso</h3>
                            <div class="credential">
                                <strong>Email:</strong><br>
                                <code>%s</code>
                            </div>
                            <div class="credential">
                                <strong>Contraseña temporal:</strong><br>
                                <code>%s</code>
                            </div>
                        </div>
                        
                        <div class="steps">
                            <h3>Pasos para activar tu cuenta:</h3>
                            <div class="step">
                                <span class="step-number">1</span>
                                <div class="step-content">Haz clic en el botón "Activar mi cuenta"</div>
                            </div>
                            <div class="step">
                                <span class="step-number">2</span>
                                <div class="step-content">Ingresa tu email y contraseña temporal</div>
                            </div>
                            <div class="step">
                                <span class="step-number">3</span>
                                <div class="step-content">Establece tu nueva contraseña</div>
                            </div>
                            <div class="step">
                                <span class="step-number">4</span>
                                <div class="step-content">Crea tu compañía y organización</div>
                            </div>
                        </div>
                        
                        <div class="btn-container">
                            <a href="%s" class="btn-activate">Activar mi cuenta</a>
                        </div>
                        
                        <div class="warning">
                            <span class="warning-icon">⚠️</span> <strong>Importante:</strong><br>
                            Este enlace expirará el <strong>%s</strong> (%d horas desde su creación).<br>
                            Por seguridad, cambia tu contraseña temporal inmediatamente después de activar tu cuenta.
                        </div>
                        
                        <p>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                        <div class="link-fallback">
                            %s
                        </div>
                    </div>
                    <div class="footer">
                        <p>Este es un correo automático, por favor no responder.</p>
                        <p>© 2026 Atlas Platform. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                adminName,
                email,
                temporaryPassword,
                activationUrl,
                expiresAt,
                expirationHours,
                activationUrl
            );
    }

    private String buildOrganizationInvitationEmail(String organizationName, 
            String invitationUrl, String invitedByName, String expiresAt) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #1e3a5f 0%%, #2d5a87 100%%); color: white; padding: 30px 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 30px 20px; background-color: #f9f9f9; }
                    .org-info { background-color: white; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #1e3a5f; }
                    .btn-container { text-align: center; margin: 30px 0; }
                    .btn-accept { display: inline-block; background: linear-gradient(135deg, #1e3a5f 0%%, #2d5a87 100%%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 8px; font-weight: bold; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 ¡Has sido invitado!</h1>
                    </div>
                    <div class="content">
                        <h2>Hola,</h2>
                        <p><strong>%s</strong> te ha invitado a unirte a:</p>
                        
                        <div class="org-info">
                            <p><strong>🏢 Organización:</strong> %s</p>
                            <p><strong>📅 Válido hasta:</strong> %s</p>
                        </div>
                        
                        <div class="btn-container">
                            <a href="%s" class="btn-accept">Aceptar Invitación</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2026 Atlas Platform. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                invitedByName != null ? invitedByName : "Un administrador",
                organizationName,
                expiresAt != null ? expiresAt : "7 días",
                invitationUrl
            );
    }

    private String buildActivationConfirmationEmail(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #28a745 0%%, #20c997 100%%); color: white; padding: 30px 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { padding: 30px 20px; background-color: #f9f9f9; }
                    .success-icon { font-size: 60px; text-align: center; display: block; }
                    .next-steps { background-color: white; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #28a745; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✅ ¡Cuenta activada!</h1>
                    </div>
                    <div class="content">
                        <span class="success-icon">🎉</span>
                        <h2 style="text-align: center;">¡Felicidades, %s!</h2>
                        <p style="text-align: center;">Tu cuenta de administrador ha sido activada exitosamente.</p>
                        
                        <div class="next-steps">
                            <h3>Próximos pasos:</h3>
                            <ul>
                                <li>Crea tu compañía (holding)</li>
                                <li>Configura tu primera organización (ciudadela, conjunto o condominio)</li>
                                <li>Invita a residentes y personal de seguridad</li>
                            </ul>
                        </div>
                        
                        <p style="text-align: center;">Ya puedes iniciar sesión y comenzar a configurar tu organización.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 Atlas Platform. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }
    
    @Override
    public Mono<Void> sendOwnerInvitationEmail(String to, String token, String activationUrl, 
            co.com.atlas.model.invitation.Invitation invitation) {
        String subject = "🏠 Atlas Platform - Invitación como propietario";
        String htmlContent = buildOwnerInvitationEmail(to, token, activationUrl, invitation);
        return sendEmail(to, subject, htmlContent);
    }
    
    private String buildOwnerInvitationEmail(String email, String token, String activationUrl,
            co.com.atlas.model.invitation.Invitation invitation) {
        String fullActivationUrl = activationUrl + "?token=" + token;
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #2E7D32, #43A047); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px; color: #333; }
                    .btn { display: inline-block; background: #2E7D32; color: white !important; padding: 15px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; font-weight: bold; }
                    .btn:hover { background: #1B5E20; }
                    .info-box { background: #E8F5E9; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #43A047; }
                    .footer { background: #f8f8f8; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🏠 Bienvenido a tu nuevo hogar</h1>
                    </div>
                    <div class="content">
                        <h2>¡Has sido invitado como propietario!</h2>
                        <p>Te damos la bienvenida a la plataforma de gestión de tu residencia.</p>
                        
                        <div class="info-box">
                            <p><strong>Email:</strong> %s</p>
                        </div>
                        
                        <p>Para activar tu cuenta y comenzar a usar la plataforma:</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="btn">Activar mi cuenta</a>
                        </div>
                        
                        <p style="font-size: 12px; color: #666;">
                            Si no puedes hacer clic en el botón, copia y pega este enlace en tu navegador:<br>
                            <span style="word-break: break-all;">%s</span>
                        </p>
                        
                        <div class="info-box">
                            <h3>¿Qué podrás hacer?</h3>
                            <ul>
                                <li>Gestionar tu unidad residencial</li>
                                <li>Registrar vehículos</li>
                                <li>Autorizar visitas</li>
                                <li>Recibir notificaciones de la administración</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p>Este correo fue enviado automáticamente. Por favor no responder.</p>
                        <p>© 2026 Atlas Platform. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(email, fullActivationUrl, fullActivationUrl);
    }
}
