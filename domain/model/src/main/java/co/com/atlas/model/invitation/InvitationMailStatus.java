package co.com.atlas.model.invitation;

/**
 * Estados del envío de correo de invitación.
 */
public enum InvitationMailStatus {
    /**
     * Invitación pendiente de envío.
     */
    PENDING,
    
    /**
     * Correo enviado exitosamente.
     */
    SENT,
    
    /**
     * Error al enviar el correo.
     */
    FAILED,
    
    /**
     * Correo rebotado (bounce).
     */
    BOUNCED;
    
    /**
     * Verifica si el estado indica que el correo fue enviado.
     */
    public boolean wasSent() {
        return this == SENT;
    }
    
    /**
     * Verifica si el estado indica error.
     */
    public boolean isError() {
        return this == FAILED || this == BOUNCED;
    }
    
    /**
     * Verifica si se puede reintentar el envío.
     */
    public boolean canRetry() {
        return this == PENDING || this == FAILED;
    }
}
