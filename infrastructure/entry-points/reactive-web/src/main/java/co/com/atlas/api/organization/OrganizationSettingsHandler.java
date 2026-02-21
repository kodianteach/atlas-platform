package co.com.atlas.api.organization;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.organization.dto.OrganizationSettingsDto;
import co.com.atlas.api.organization.dto.OrganizationSettingsResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.OrganizationConfiguration;
import co.com.atlas.tenant.TenantContext;
import co.com.atlas.usecase.organization.OrganizationSettingsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para configuración de organización.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationSettingsHandler {

    private final OrganizationSettingsUseCase organizationSettingsUseCase;

    /**
     * Obtiene la configuración de la organización del usuario autenticado.
     * 
     * GET /api/organization/settings
     */
    public Mono<ServerResponse> getSettings(ServerRequest request) {
        Long organizationId = TenantContext.getOrganizationIdOrThrow();

        return organizationSettingsUseCase.getSettings(organizationId)
            .flatMap(settings -> {
                OrganizationSettingsResponse response = OrganizationSettingsResponse.builder()
                    .maxUnitsPerDistribution(settings.getMaxUnitsPerDistributionOrDefault())
                    .enableOwnerPermissionManagement(
                        settings.getEnableOwnerPermissionManagement() != null
                            ? settings.getEnableOwnerPermissionManagement() : false)
                    .message("Configuración obtenida exitosamente")
                    .build();

                ApiResponse<OrganizationSettingsResponse> apiResponse =
                    ApiResponse.success(response, response.getMessage());

                return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(apiResponse);
            })
            .onErrorResume(NotFoundException.class, e ->
                buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND))
            .onErrorResume(BusinessException.class, e ->
                buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
    }

    /**
     * Actualiza la configuración de la organización del usuario autenticado.
     * 
     * PUT /api/organization/settings
     */
    public Mono<ServerResponse> updateSettings(ServerRequest request) {
        Long organizationId = TenantContext.getOrganizationIdOrThrow();

        return request.bodyToMono(OrganizationSettingsDto.class)
            .flatMap(req -> {
                if (req.getMaxUnitsPerDistribution() == null || req.getMaxUnitsPerDistribution() < 1) {
                    return Mono.error(new BusinessException(
                        "maxUnitsPerDistribution debe ser mayor a 0",
                        "INVALID_MAX_UNITS"
                    ));
                }

                OrganizationConfiguration configuration = OrganizationConfiguration.builder()
                    .maxUnitsPerDistribution(req.getMaxUnitsPerDistribution())
                    .enableOwnerPermissionManagement(
                        req.getEnableOwnerPermissionManagement() != null
                            ? req.getEnableOwnerPermissionManagement() : false)
                    .build();

                return organizationSettingsUseCase.updateSettings(organizationId, configuration);
            })
            .flatMap(updatedSettings -> {
                OrganizationSettingsResponse response = OrganizationSettingsResponse.builder()
                    .maxUnitsPerDistribution(updatedSettings.getMaxUnitsPerDistributionOrDefault())
                    .enableOwnerPermissionManagement(
                        updatedSettings.getEnableOwnerPermissionManagement() != null
                            ? updatedSettings.getEnableOwnerPermissionManagement() : false)
                    .message("Configuración actualizada exitosamente")
                    .build();

                ApiResponse<OrganizationSettingsResponse> apiResponse =
                    ApiResponse.success(response, response.getMessage());

                return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(apiResponse);
            })
            .onErrorResume(NotFoundException.class, e ->
                buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND))
            .onErrorResume(BusinessException.class, e ->
                buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
    }

    private Mono<ServerResponse> buildErrorResponse(String message, HttpStatus status) {
        ApiResponse<Object> errorResponse = ApiResponse.error(message);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }
}
