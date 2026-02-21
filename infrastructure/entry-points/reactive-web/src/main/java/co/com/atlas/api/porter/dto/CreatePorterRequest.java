package co.com.atlas.api.porter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request para crear un portero.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para crear un portero")
public class CreatePorterRequest {

    @Schema(description = "Nombre descriptivo del portero", example = "Carlos Portería Norte", requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;

    @Schema(description = "Tipo de portero: PORTERO_GENERAL o PORTERO_DELIVERY", example = "PORTERO_GENERAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String porterType;
}
