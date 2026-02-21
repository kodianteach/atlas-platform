package co.com.atlas.api.porter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Response con datos del portero.
 * Un portero es un usuario con rol PORTERO_GENERAL o PORTERO_DELIVERY.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos del portero")
public class PorterResponse {

    @Schema(description = "ID del portero (= users.id)", example = "42")
    private Long id;

    @Schema(description = "ID de la organización", example = "100")
    private Long organizationId;

    @Schema(description = "Nombre del portero", example = "Carlos Portería Norte")
    private String names;

    @Schema(description = "Email del portero", example = "porter-xxx@org.atlas.internal")
    private String email;

    @Schema(description = "Tipo de portero", example = "PORTERO_GENERAL")
    private String porterType;

    @Schema(description = "Estado del usuario", example = "PRE_REGISTERED")
    private String status;

    @Schema(description = "Fecha de creación", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "URL de enrolamiento (solo disponible al crear o regenerar)", example = "/porter-enroll?token=abc123")
    private String enrollmentUrl;
}
