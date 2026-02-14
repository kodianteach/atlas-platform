package co.com.atlas.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para validación de placa (API de guarda).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlateValidationResponse {
    private boolean allowed;
    private String plate;
    private String unitCode;
    private String vehicleType;
    private String ownerName;
    private String message;
}
