package co.com.atlas.model.porter;

/**
 * Value object con información del dispositivo de portería.
 * Se registra en el backend durante el enrolamiento.
 */
public record DeviceInfo(
        String platform,
        String model,
        String appVersion,
        String userAgent
) {}
