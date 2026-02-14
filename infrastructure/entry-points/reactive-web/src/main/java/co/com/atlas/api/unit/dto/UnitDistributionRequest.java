package co.com.atlas.api.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para distribución manual de unidades (creación por rango).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para distribución de unidades por rango")
public class UnitDistributionRequest {
    
    @Schema(description = "ID de la organización donde se crearán las unidades", example = "1", required = true)
    private Long organizationId;
    
    @Schema(description = "Número inicial del rango de unidades", example = "101", required = true)
    private Integer rangeStart;
    
    @Schema(description = "Número final del rango de unidades", example = "110", required = true)
    private Integer rangeEnd;
    
    @Schema(description = "Prefijo para el código de unidad", example = "APTO-")
    private String codePrefix;
    
    @Schema(description = "Tipo de unidad", example = "APARTMENT", allowableValues = {"APARTMENT", "HOUSE", "LOCAL", "OFFICE", "PARKING", "WAREHOUSE", "OTHER"})
    private String unitType;
    
    @Schema(description = "Habilitar vehículos para estas unidades", example = "true")
    private Boolean vehiclesEnabled;
    
    @Schema(description = "Límite máximo de vehículos por unidad", example = "2")
    private Integer vehicleLimit;
    
    //@Schema(description = "Información del propietario (opcional)")
    //private OwnerInfoRequest owner;
    
    //@Schema(description = "Enviar correo de invitación inmediatamente", example = "true")
    //private Boolean sendInvitationImmediately;
    
    @Schema(description = "ID de la torre (opcional)", example = "1")
    private Long towerId;
    
    @Schema(description = "ID de la zona (opcional)", example = "1")
    private Long zoneId;
    
    @Schema(description = "Piso de las unidades", example = "1")
    private Integer floor;
}
