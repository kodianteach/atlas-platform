package co.com.atlas.api.invitation;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.invitation.dto.AcceptInvitationRequest;
import co.com.atlas.api.invitation.dto.InvitationRequest;
import co.com.atlas.api.invitation.dto.InvitationResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.usecase.invitation.InvitationUseCase;
import co.com.atlas.usecase.invitation.OwnerRegistrationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Handler para operaciones de Invitation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationHandler {

    private final InvitationUseCase invitationUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(InvitationRequest.class)
                .flatMap(req -> {
                    Invitation invitation = Invitation.builder()
                            .organizationId(req.getOrganizationId())
                            .unitId(req.getUnitId())
                            .email(req.getEmail())
                            .type(parseInvitationType(req.getType()))
                            .roleId(req.getRoleId())
                            .build();
                    Long invitedBy = extractUserIdFromRequest(request);
                    return invitationUseCase.create(invitation, invitedBy);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return invitationUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getByOrganization(ServerRequest request) {
        Long organizationId = Long.parseLong(request.pathVariable("organizationId"));
        return invitationUseCase.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(invitations -> {
                    ApiResponse<Object> response = ApiResponse.success(invitations, "Invitaciones obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> getByUnit(ServerRequest request) {
        Long unitId = Long.parseLong(request.pathVariable("unitId"));
        Long organizationId = extractOrganizationIdFromRequest(request);
        return invitationUseCase.findByOrganizationId(organizationId)
                .filter(inv -> unitId.equals(inv.getUnitId()))
                .map(this::toResponse)
                .collectList()
                .flatMap(invitations -> {
                    ApiResponse<Object> response = ApiResponse.success(invitations, "Invitaciones obtenidas");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> accept(ServerRequest request) {
        return request.bodyToMono(AcceptInvitationRequest.class)
                .flatMap(req -> {
                    // Validaciones básicas
                    if (req.getToken() == null || req.getToken().isBlank()) {
                        return Mono.error(new BusinessException("Token requerido"));
                    }
                    if (req.getNames() == null || req.getNames().isBlank()) {
                        return Mono.error(new BusinessException("Nombres requeridos"));
                    }
                    if (req.getDocumentType() == null || req.getDocumentType().isBlank()) {
                        return Mono.error(new BusinessException("Tipo de documento requerido"));
                    }
                    if (req.getDocumentNumber() == null || req.getDocumentNumber().isBlank()) {
                        return Mono.error(new BusinessException("Número de documento requerido"));
                    }
                    if (req.getPassword() == null || req.getPassword().isBlank()) {
                        return Mono.error(new BusinessException("Contraseña requerida"));
                    }
                    if (!req.getPassword().equals(req.getConfirmPassword())) {
                        return Mono.error(new BusinessException("Las contraseñas no coinciden"));
                    }
                    
                    DocumentType docType = parseDocumentType(req.getDocumentType());
                    
                    OwnerRegistrationData ownerData = OwnerRegistrationData.builder()
                            .names(req.getNames())
                            .phone(req.getPhone())
                            .documentType(docType)
                            .documentNumber(req.getDocumentNumber())
                            .password(req.getPassword())
                            .build();
                    
                    return invitationUseCase.acceptWithOwnerData(req.getToken(), ownerData);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }
    
    private DocumentType parseDocumentType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Mono<ServerResponse> cancel(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return invitationUseCase.cancel(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Invitación cancelada");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> resend(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return invitationUseCase.resend(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    private Long extractUserIdFromRequest(ServerRequest request) {
        // En producción, esto vendría del JWT token
        return request.headers().firstHeader("X-User-Id") != null 
                ? Long.parseLong(request.headers().firstHeader("X-User-Id")) 
                : 1L;
    }

    private Mono<ServerResponse> buildSuccessResponse(Invitation invitation) {
        InvitationResponse data = toResponse(invitation);
        ApiResponse<InvitationResponse> response = ApiResponse.success(data, "Operación exitosa");
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

    private Long extractOrganizationIdFromRequest(ServerRequest request) {
        return request.headers().firstHeader("X-Organization-Id") != null 
                ? Long.parseLong(request.headers().firstHeader("X-Organization-Id")) 
                : null;
    }

    /**
     * Parsea el tipo de invitación con validación y mensaje amigable.
     */
    private InvitationType parseInvitationType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return InvitationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            String validValues = Arrays.stream(InvitationType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(
                    "Tipo de invitación inválido: '" + type + "'. Valores válidos: " + validValues,
                    "INVALID_INVITATION_TYPE",
                    400
            );
        }
    }

    private InvitationResponse toResponse(Invitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .organizationId(invitation.getOrganizationId())
                .unitId(invitation.getUnitId())
                .email(invitation.getEmail())
                .invitationToken(invitation.getInvitationToken())
                .type(invitation.getType() != null ? invitation.getType().name() : null)
                .roleId(invitation.getRoleId())
                .status(invitation.getStatus() != null ? invitation.getStatus().name() : null)
                .invitedBy(invitation.getInvitedBy())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
