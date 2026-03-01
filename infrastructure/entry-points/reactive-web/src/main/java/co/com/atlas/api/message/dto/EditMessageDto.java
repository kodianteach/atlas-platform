package co.com.atlas.api.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para edición de un mensaje existente en el canal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para editar el contenido de un mensaje propio")
public class EditMessageDto {

    @Schema(description = "Nuevo contenido del mensaje", example = "Mensaje corregido")
    private String content;
}
