package co.com.atlas.api.invitation;

import co.com.atlas.api.common.SecurityContextHelper;
import co.com.atlas.api.common.SecurityContextHelper.AuthContext;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.invitation.dto.CreateResidentInvitationRequest;
import co.com.atlas.api.invitation.dto.ResidentRegistrationRequest;
import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.usecase.invitation.InvitationUseCase;
import co.com.atlas.usecase.invitation.ResidentRegistrationData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Handler for resident invitation operations.
 * Handles creation of resident invitations (by OWNER) and
 * resident self-registration (external, no auth required).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResidentInvitationHandler {

    private final InvitationUseCase invitationUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a resident invitation. Called by authenticated OWNER.
     */
    public Mono<ServerResponse> createResidentInvitation(ServerRequest request) {
        Long unitId = extractOptionalHeader(request, "X-Unit-Id");

        return SecurityContextHelper.extractAuthContext()
                .flatMap(ctx -> {
                    Long organizationId = ctx.organizationId();
                    Long userId = ctx.userId();

                    log.info("Creating resident invitation for org={}, unit={} by user={}", organizationId, unitId, userId);

                    return request.bodyToMono(CreateResidentInvitationRequest.class)
                .defaultIfEmpty(new CreateResidentInvitationRequest())
                .flatMap(req -> {
                    String permissionsJson = null;
                    if (req.getPermissions() != null && !req.getPermissions().isEmpty()) {
                        try {
                            permissionsJson = objectMapper.writeValueAsString(req.getPermissions());
                        } catch (JsonProcessingException e) {
                            return Mono.error(new BusinessException("Invalid permissions format", "INVALID_PERMISSIONS"));
                        }
                    }
                    return invitationUseCase.createResidentInvitation(organizationId, unitId, userId, permissionsJson);
                })
                .flatMap(invitation -> {
                    String invitationUrl = invitation.getMetadata() != null
                            ? extractUrlFromMetadata(invitation.getMetadata())
                            : "";
                    Map<String, Object> responseData = Map.of(
                            "invitationId", invitation.getId(),
                            "invitationUrl", invitationUrl,
                            "expiresAt", invitation.getExpiresAt().toString(),
                            "token", invitation.getInvitationToken()
                    );
                    return ServerResponse.status(HttpStatus.CREATED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.success(responseData, "Invitación para residente generada exitosamente"));
                })
                .onErrorResume(NotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(404, e.getMessage())))
                .onErrorResume(e -> {
                    log.error("Error creating resident invitation", e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(500, "Error interno del servidor"));
                });
                }); // end extractAuthContext flatMap
    }

    /**
     * Registers a resident via invitation token. External endpoint (no auth).
     */
    public Mono<ServerResponse> acceptResidentRegistration(ServerRequest request) {
        return request.bodyToMono(ResidentRegistrationRequest.class)
                .flatMap(req -> {
                    ResidentRegistrationData data = ResidentRegistrationData.builder()
                            .names(req.getNames())
                            .phone(req.getPhone())
                            .documentType(parseDocumentType(req.getDocumentType()))
                            .documentNumber(req.getDocumentNumber())
                            .password(req.getPassword())
                            .confirmPassword(req.getConfirmPassword())
                            .build();

                    return invitationUseCase.acceptResidentRegister(req.getToken(), data);
                })
                .flatMap(invitation -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.success(Map.of(
                                "message", "Registro de residente completado exitosamente",
                                "organizationId", invitation.getOrganizationId(),
                                "unitId", invitation.getUnitId() != null ? invitation.getUnitId() : ""
                        ), "Registro exitoso")))
                .onErrorResume(BusinessException.class, e -> {
                    HttpStatus status = switch (e.getErrorCode()) {
                        case "INVITATION_EXPIRED" -> HttpStatus.GONE;
                        case "INVITATION_ACCEPTED" -> HttpStatus.CONFLICT;
                        case "WEAK_PASSWORD", "PASSWORDS_MISMATCH", "INVALID_TOKEN_TYPE" -> HttpStatus.BAD_REQUEST;
                        default -> HttpStatus.BAD_REQUEST;
                    };
                    return ServerResponse.status(status)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(status.value(), e.getMessage(), e.getErrorCode(), null));
                })
                .onErrorResume(NotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(404, e.getMessage())))
                .onErrorResume(e -> {
                    log.error("Error during resident registration", e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(500, "Error interno del servidor"));
                });
    }

    private Long extractOptionalHeader(ServerRequest request, String headerName) {
        String value = request.headers().firstHeader(headerName);
        if (value == null) {
            return null;
        }
        return Long.parseLong(value);
    }

    private String extractUrlFromMetadata(String metadata) {
        try {
            return objectMapper.readTree(metadata).path("invitationUrl").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private DocumentType parseDocumentType(String type) {
        try {
            return type != null ? DocumentType.valueOf(type) : DocumentType.CC;
        } catch (IllegalArgumentException e) {
            return DocumentType.CC;
        }
    }
}
