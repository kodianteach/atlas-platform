package co.com.atlas.api.unit;

import co.com.atlas.api.common.dto.ErrorResponse;
import co.com.atlas.api.unit.dto.BulkUploadProcessResponse;
import co.com.atlas.api.unit.dto.BulkUploadValidationResponse;
import co.com.atlas.api.unit.dto.UnitDistributionRequest;
import co.com.atlas.api.unit.dto.UnitDistributionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Router para distribución y carga masiva de unidades.
 */
@Configuration
@Tag(name = "Unit Distribution", description = "Distribución y carga masiva de unidades")
public class UnitDistributionRouterRest {

    @Bean("unitDistributionRoutes")
    @RouterOperations({
            @RouterOperation(
                    path = "/api/units/distribute",
                    method = RequestMethod.POST,
                    beanClass = UnitDistributionHandler.class,
                    beanMethod = "distribute",
                    operation = @Operation(
                            operationId = "distributeUnits",
                            summary = "Distribuir unidades por rango",
                            description = "Crea múltiples unidades especificando un rango numérico. Opcionalmente asigna propietario e invitación.",
                            tags = {"Unit Distribution"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = UnitDistributionRequest.class))
                            ),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Distribución completada",
                                            content = @Content(schema = @Schema(implementation = UnitDistributionResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/bulk-upload/validate",
                    method = RequestMethod.POST,
                    beanClass = UnitDistributionHandler.class,
                    beanMethod = "validateBulkUpload",
                    operation = @Operation(
                            operationId = "validateBulkUpload",
                            summary = "Validar archivo de carga masiva",
                            description = "Valida un archivo Excel o CSV con unidades y propietarios. Retorna errores y vista previa.",
                            tags = {"Unit Distribution"},
                            parameters = {
                                    @Parameter(name = "organizationId", description = "ID de la organización", required = true)
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Validación completada",
                                            content = @Content(schema = @Schema(implementation = BulkUploadValidationResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error de validación",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/units/bulk-upload/process",
                    method = RequestMethod.POST,
                    beanClass = UnitDistributionHandler.class,
                    beanMethod = "processBulkUpload",
                    operation = @Operation(
                            operationId = "processBulkUpload",
                            summary = "Procesar carga masiva",
                            description = "Procesa un archivo Excel o CSV validado, creando unidades e invitaciones.",
                            tags = {"Unit Distribution"},
                            parameters = {
                                    @Parameter(name = "organizationId", description = "ID de la organización", required = true),
                                    @Parameter(name = "createdById", description = "ID del usuario que crea", required = true),
                                    @Parameter(name = "sendInvitations", description = "Enviar invitaciones inmediatamente", required = false)
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Procesamiento completado",
                                            content = @Content(schema = @Schema(implementation = BulkUploadProcessResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Error durante procesamiento",
                                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> unitDistributionRoutes(UnitDistributionHandler handler) {
        return route(POST("/api/units/distribute").and(accept(MediaType.APPLICATION_JSON)), handler::distribute)
                .andRoute(POST("/api/units/bulk-upload/validate").and(accept(MediaType.APPLICATION_JSON)), 
                        handler::validateBulkUpload)
                .andRoute(POST("/api/units/bulk-upload/process").and(accept(MediaType.APPLICATION_JSON)), 
                        handler::processBulkUpload);
    }
}
