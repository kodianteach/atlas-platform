package co.com.atlas.model.porter;

/**
 * Estados posibles de un token de enrolamiento de portero.
 */
public enum PorterEnrollmentTokenStatus {
    /** Token pendiente de ser usado. */
    PENDING,
    /** Token consumido exitosamente. */
    CONSUMED,
    /** Token expirado por tiempo. */
    EXPIRED,
    /** Token revocado manualmente (por regeneración). */
    REVOKED
}
