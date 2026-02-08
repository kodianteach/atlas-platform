package co.com.atlas.notification;

import co.com.atlas.model.notification.gateways.NotificationGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementación no-op del gateway de notificaciones.
 * Se usa cuando las notificaciones están deshabilitadas.
 */
@Component
@ConditionalOnProperty(name = "notification.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoOpNotificationAdapter implements NotificationGateway {

    @Override
    public Mono<Void> sendEmail(String to, String subject, String htmlContent) {
        log.debug("NoOp: sendEmail to={}, subject={}", to, subject);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendAdminPreRegistrationEmail(
            String to,
            String adminName,
            String temporaryPassword,
            String activationUrl,
            String expiresAt,
            int expirationHours) {
        log.info("NoOp: Would send pre-registration email to {} with activation URL: {}", to, activationUrl);
        log.info("NoOp: Temporary password: {}, Expires: {} ({} hours)", temporaryPassword, expiresAt, expirationHours);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendOrganizationInvitationEmail(
            String to,
            String organizationName,
            String invitationUrl,
            String invitedByName,
            String expiresAt) {
        log.debug("NoOp: sendOrganizationInvitationEmail to={}, org={}", to, organizationName);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendActivationConfirmationEmail(String to, String userName) {
        log.debug("NoOp: sendActivationConfirmationEmail to={}", to);
        return Mono.empty();
    }
}
