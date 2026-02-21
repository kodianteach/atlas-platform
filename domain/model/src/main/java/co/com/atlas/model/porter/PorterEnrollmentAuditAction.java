package co.com.atlas.model.porter;

/**
 * Acciones de auditoría para el enrolamiento de porteros.
 * Coincide con el ENUM de la tabla porter_enrollment_audit_log.
 */
public enum PorterEnrollmentAuditAction {
    CREATED,
    URL_GENERATED,
    URL_REGENERATED,
    CONSUMED,
    EXPIRED,
    REVOKED
}
