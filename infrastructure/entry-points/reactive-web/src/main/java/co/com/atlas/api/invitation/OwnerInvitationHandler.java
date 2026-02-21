package co.com.atlas.api.invitation;

import co.com.atlas.api.common.SecurityContextHelper;
import co.com.atlas.api.common.SecurityContextHelper.AuthContext;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.invitation.dto.OwnerRegistrationRequest;
import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
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

import java.util.Map;

/**
 * Handler for owner invitation operations.
 * Handles creation of owner invitations (by ADMIN_ATLAS) and
 * owner self-registration (external, no auth required).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerInvitationHandler {

    private final InvitationUseCase invitationUseCase;

    /**
     * Creates an owner invitation. Called by authenticated ADMIN_ATLAS.
     * Extracts organizationId and userId from ReactiveSecurityContextHolder.
     */
    public Mono<ServerResponse> createOwnerInvitation(ServerRequest request) {
        return SecurityContextHelper.extractAuthContext()
                .flatMap(ctx -> {
                    Long organizationId = ctx.organizationId();
                    Long userId = ctx.userId();

                    if (organizationId == null || userId == null) {
                        return ServerResponse.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(400, "No se pudo determinar la organización o el usuario del contexto de autenticación"));
                    }

                    log.info("Creating owner invitation for org={} by user={}", organizationId, userId);

                    return invitationUseCase.createOwnerInvitation(organizationId, userId)
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
                            .bodyValue(ApiResponse.success(responseData, "Invitación para propietario generada exitosamente"));
                })
                .onErrorResume(NotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(404, e.getMessage())))
                .onErrorResume(e -> {
                    log.error("Error creating owner invitation", e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(500, "Error interno del servidor"));
                });
                }); // end extractAuthContext flatMap
    }

    /**
     * Validates an invitation token. External endpoint (no auth required).
     * Returns token status and type information.
     */
    public Mono<ServerResponse> validateToken(ServerRequest request) {
        String token = request.queryParam("token").orElse("");
        if (token.isBlank()) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(400, "Token es requerido"));
        }

        return invitationUseCase.findByToken(token)
                .flatMap(invitation -> {
                    boolean isExpired = invitation.getExpiresAt().isBefore(java.time.Instant.now());
                    boolean isConsumed = invitation.getStatus() == InvitationStatus.ACCEPTED;

                    Map<String, Object> data = Map.of(
                            "valid", !isExpired && !isConsumed && invitation.getStatus() == InvitationStatus.PENDING,
                            "status", isConsumed ? "CONSUMED" : isExpired ? "EXPIRED" : invitation.getStatus().name(),
                            "type", invitation.getType().name(),
                            "organizationId", invitation.getOrganizationId(),
                            "unitId", invitation.getUnitId() != null ? invitation.getUnitId() : "",
                            "unitCode", invitation.getUnitId() != null ? invitation.getUnitId().toString() : ""
                    );

                    HttpStatus status = isExpired ? HttpStatus.GONE
                            : isConsumed ? HttpStatus.CONFLICT
                            : HttpStatus.OK;

                    return ServerResponse.status(status)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.success(data, "Token validado"));
                })
                .onErrorResume(NotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(404, "El enlace de invitación es inválido")));
    }

    /**
     * Registers an owner via invitation token. External endpoint (no auth).
     */
    public Mono<ServerResponse> acceptOwnerRegistration(ServerRequest request) {
        return request.bodyToMono(OwnerRegistrationRequest.class)
                .flatMap(req -> {
                    // Validate passwords match
                    if (!req.getPassword().equals(req.getConfirmPassword())) {
                        return Mono.error(new BusinessException(
                                "Las contraseñas no coinciden", "PASSWORDS_MISMATCH"));
                    }

                    DocumentType docType = parseDocumentType(req.getDocumentType());
                    OwnerRegistrationData data = OwnerRegistrationData.builder()
                            .names(req.getNames())
                            .phone(req.getPhone())
                            .documentType(docType)
                            .documentNumber(req.getDocumentNumber())
                            .email(req.getEmail())
                            .password(req.getPassword())
                            .build();

                    return invitationUseCase.acceptOwnerSelfRegister(req.getToken(), data, req.getUnitId());
                })
                .flatMap(invitation -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.success(Map.of(
                                "message", "Registro completado exitosamente",
                                "organizationId", invitation.getOrganizationId(),
                                "unitId", invitation.getUnitId() != null ? invitation.getUnitId() : ""
                        ), "Registro exitoso")))
                .onErrorResume(BusinessException.class, e -> {
                    HttpStatus status = switch (e.getErrorCode()) {
                        case "INVITATION_EXPIRED" -> HttpStatus.GONE;
                        case "INVITATION_ACCEPTED" -> HttpStatus.CONFLICT;
                        case "WEAK_PASSWORD", "PASSWORDS_MISMATCH", "INVALID_TOKEN_TYPE", "INVALID_UNIT" ->
                                HttpStatus.BAD_REQUEST;
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
                    log.error("Error during owner registration", e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.error(500, "Error interno del servidor"));
                });
    }

    private DocumentType parseDocumentType(String type) {
        try {
            return type != null ? DocumentType.valueOf(type) : DocumentType.CC;
        } catch (IllegalArgumentException e) {
            return DocumentType.CC;
        }
    }

    private String extractUrlFromMetadata(String metadata) {
        // Simple extraction from JSON metadata
        int start = metadata.indexOf("\"invitationUrl\": \"");
        if (start == -1) return "";
        start += "\"invitationUrl\": \"".length();
        int end = metadata.indexOf("\"", start);
        return end > start ? metadata.substring(start, end) : "";
    }
}
