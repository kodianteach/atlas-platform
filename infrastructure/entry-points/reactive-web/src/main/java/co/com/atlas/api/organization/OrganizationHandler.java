package co.com.atlas.api.organization;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.organization.dto.OrganizationRequest;
import co.com.atlas.api.organization.dto.OrganizationResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.OrganizationType;
import co.com.atlas.usecase.organization.OrganizationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para operaciones de Organization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationHandler {

    private final OrganizationUseCase organizationUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(OrganizationRequest.class)
                .flatMap(req -> {
                    Organization org = Organization.builder()
                            .companyId(req.getCompanyId())
                            .code(req.getCode())
                            .name(req.getName())
                            .type(req.getType() != null ? OrganizationType.valueOf(req.getType()) : null)
                            .usesZones(req.getUsesZones())
                            .description(req.getDescription())
                            .settings(req.getSettings())
                            .build();
                    return organizationUseCase.create(org);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return organizationUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getByCompany(ServerRequest request) {
        Long companyId = Long.parseLong(request.pathVariable("companyId"));
        return organizationUseCase.findByCompanyId(companyId)
                .map(this::toResponse)
                .collectList()
                .flatMap(orgs -> {
                    ApiResponse<Object> response = ApiResponse.success(orgs, "Organizaciones obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> getAll(ServerRequest request) {
        return organizationUseCase.findAllActive()
                .map(this::toResponse)
                .collectList()
                .flatMap(orgs -> {
                    ApiResponse<Object> response = ApiResponse.success(orgs, "Organizaciones obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return request.bodyToMono(OrganizationRequest.class)
                .flatMap(req -> {
                    Organization org = Organization.builder()
                            .id(id)
                            .companyId(req.getCompanyId())
                            .code(req.getCode())
                            .name(req.getName())
                            .type(req.getType() != null ? OrganizationType.valueOf(req.getType()) : null)
                            .usesZones(req.getUsesZones())
                            .description(req.getDescription())
                            .settings(req.getSettings())
                            .build();
                    return organizationUseCase.update(id, org);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return organizationUseCase.delete(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Organización eliminada");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    private Mono<ServerResponse> buildSuccessResponse(Organization org) {
        OrganizationResponse data = toResponse(org);
        ApiResponse<OrganizationResponse> response = ApiResponse.success(data, "Operación exitosa");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

    private Mono<ServerResponse> buildErrorResponse(Exception e, HttpStatus status, String path) {
        ApiResponse<Void> response = ApiResponse.error(status.value(), e.getMessage(), path, null);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .companyId(org.getCompanyId())
                .code(org.getCode())
                .name(org.getName())
                .slug(org.getSlug())
                .type(org.getType() != null ? org.getType().name() : null)
                .usesZones(org.getUsesZones())
                .description(org.getDescription())
                .settings(org.getSettings())
                .status(org.getStatus())
                .isActive(org.getIsActive())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
