package co.com.atlas.api.visit;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.visit.dto.VisitApprovalDto;
import co.com.atlas.api.visit.dto.VisitRequestDto;
import co.com.atlas.api.visit.dto.VisitRequestResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.visit.RecurrenceType;
import co.com.atlas.model.visit.VisitRequest;
import co.com.atlas.usecase.visit.VisitRequestUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para operaciones de VisitRequest.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VisitHandler {

    private final VisitRequestUseCase visitRequestUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(VisitRequestDto.class)
                .flatMap(req -> {
                    VisitRequest visitRequest = VisitRequest.builder()
                            .organizationId(req.getOrganizationId())
                            .unitId(req.getUnitId())
                            .visitorName(req.getVisitorName())
                            .visitorDocument(req.getVisitorDocument())
                            .visitorPhone(req.getVisitorPhone())
                            .visitorEmail(req.getVisitorEmail())
                            .reason(req.getReason())
                            .validFrom(req.getValidFrom())
                            .validUntil(req.getValidUntil())
                            .recurrenceType(req.getRecurrenceType() != null ? RecurrenceType.valueOf(req.getRecurrenceType()) : RecurrenceType.ONCE)
                            .maxEntries(req.getMaxEntries())
                            .build();
                    Long requestedBy = extractUserIdFromRequest(request);
                    return visitRequestUseCase.create(visitRequest, requestedBy);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return visitRequestUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getByOrganization(ServerRequest request) {
        Long organizationId = Long.parseLong(request.pathVariable("organizationId"));
        return visitRequestUseCase.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(visits -> {
                    ApiResponse<Object> response = ApiResponse.success(visits, "Visitas obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> getByUnit(ServerRequest request) {
        Long unitId = Long.parseLong(request.pathVariable("unitId"));
        return visitRequestUseCase.findByUnitId(unitId)
                .map(this::toResponse)
                .collectList()
                .flatMap(visits -> {
                    ApiResponse<Object> response = ApiResponse.success(visits, "Visitas obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> getPendingByOrganization(ServerRequest request) {
        Long organizationId = Long.parseLong(request.pathVariable("organizationId"));
        return visitRequestUseCase.findPendingByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(visits -> {
                    ApiResponse<Object> response = ApiResponse.success(visits, "Visitas pendientes obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> approve(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        Long approvedBy = extractUserIdFromRequest(request);
        return visitRequestUseCase.approve(id, approvedBy)
                .flatMap(visitRequest -> {
                    VisitRequestResponse data = toResponse(visitRequest);
                    ApiResponse<VisitRequestResponse> response = ApiResponse.success(data, "Visita aprobada");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                })
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> reject(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        Long rejectedBy = extractUserIdFromRequest(request);
        return request.bodyToMono(VisitApprovalDto.class)
                .flatMap(dto -> visitRequestUseCase.reject(id, rejectedBy, dto.getReason()))
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> cancel(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        Long userId = extractUserIdFromRequest(request);
        return visitRequestUseCase.cancel(id, userId)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Visita cancelada");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    private Long extractUserIdFromRequest(ServerRequest request) {
        return request.headers().firstHeader("X-User-Id") != null 
                ? Long.parseLong(request.headers().firstHeader("X-User-Id")) 
                : 1L;
    }

    private Mono<ServerResponse> buildSuccessResponse(VisitRequest visitRequest) {
        VisitRequestResponse data = toResponse(visitRequest);
        ApiResponse<VisitRequestResponse> response = ApiResponse.success(data, "Operación exitosa");
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

    private VisitRequestResponse toResponse(VisitRequest visit) {
        return VisitRequestResponse.builder()
                .id(visit.getId())
                .organizationId(visit.getOrganizationId())
                .unitId(visit.getUnitId())
                .requestedBy(visit.getRequestedBy())
                .visitorName(visit.getVisitorName())
                .visitorDocument(visit.getVisitorDocument())
                .visitorPhone(visit.getVisitorPhone())
                .visitorEmail(visit.getVisitorEmail())
                .reason(visit.getReason())
                .validFrom(visit.getValidFrom())
                .validUntil(visit.getValidUntil())
                .recurrenceType(visit.getRecurrenceType() != null ? visit.getRecurrenceType().name() : null)
                .maxEntries(visit.getMaxEntries())
                .status(visit.getStatus() != null ? visit.getStatus().name() : null)
                .createdAt(visit.getCreatedAt())
                .build();
    }
}
