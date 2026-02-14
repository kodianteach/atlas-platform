package co.com.atlas.api.unit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request para procesar carga masiva de unidades validada.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request para procesar carga masiva de unidades")
public class BulkUploadProcessRequest {

    @Schema(description = "Lista de filas a procesar (previamente validadas)", required = true)
    private List<BulkUploadValidationRequest.BulkRowDto> rows;
    
    @Schema(description = "Tipo de unidad a crear", example = "APARTMENT", allowableValues = {"APARTMENT", "HOUSE"})
    private String unitType;
}
