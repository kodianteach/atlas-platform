package co.com.atlas.api.message;

import co.com.atlas.api.message.dto.EditMessageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@Tag(name = "Messages", description = "Canal de mensajería privada entre admins y porteros")
public class ChannelMessageRouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/messages",
                    method = RequestMethod.GET,
                    beanClass = ChannelMessageHandler.class,
                    beanMethod = "getHistory",
                    operation = @Operation(
                            operationId = "getMessageHistory",
                            summary = "Obtener historial de mensajes",
                            description = "Obtiene el historial de mensajes de la organización (últimos 30 días). Requiere header X-Organization-Id.",
                            tags = {"Messages"},
                            parameters = {
                                    @Parameter(name = "X-Organization-Id", in = ParameterIn.HEADER, required = true, description = "ID de la organización")
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/messages/{id}",
                    method = RequestMethod.PUT,
                    beanClass = ChannelMessageHandler.class,
                    beanMethod = "editMessage",
                    operation = @Operation(
                            operationId = "editMessage",
                            summary = "Editar mensaje",
                            description = "Edita un mensaje propio. Requiere headers X-User-Id y body con nuevo contenido.",
                            tags = {"Messages"},
                            parameters = {
                                    @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID del mensaje"),
                                    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true, description = "ID del usuario")
                            },
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = EditMessageDto.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Mensaje editado"),
                                    @ApiResponse(responseCode = "403", description = "No autorizado para editar"),
                                    @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/messages/{id}",
                    method = RequestMethod.DELETE,
                    beanClass = ChannelMessageHandler.class,
                    beanMethod = "deleteMessage",
                    operation = @Operation(
                            operationId = "deleteMessage",
                            summary = "Eliminar mensaje",
                            description = "Elimina un mensaje propio (soft-delete). Requiere header X-User-Id.",
                            tags = {"Messages"},
                            parameters = {
                                    @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID del mensaje"),
                                    @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, required = true, description = "ID del usuario")
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Mensaje eliminado"),
                                    @ApiResponse(responseCode = "403", description = "No autorizado para eliminar"),
                                    @ApiResponse(responseCode = "404", description = "Mensaje no encontrado")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> messageRoutes(ChannelMessageHandler handler) {
        return route(GET("/api/messages"), handler::getHistory)
                .andRoute(PUT("/api/messages/{id}"), handler::editMessage)
                .andRoute(DELETE("/api/messages/{id}"), handler::deleteMessage);
    }
}
