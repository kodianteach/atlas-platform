package co.com.atlas.api.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para envío de mensajes al canal de mensajería.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para envío de un mensaje de texto al canal")
public class ChannelMessageDto {

    @Schema(description = "Contenido del mensaje de texto", example = "Buenos días, ¿cómo va la portería?")
    private String content;
}
