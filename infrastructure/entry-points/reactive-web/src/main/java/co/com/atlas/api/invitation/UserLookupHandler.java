package co.com.atlas.api.invitation;

import co.com.atlas.api.common.SecurityContextHelper;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.usecase.invitation.UserLookupUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler for user lookup operations (autocompletion in registration forms)
 * and unit search (autocomplete for unit selection).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserLookupHandler {

    private final UserLookupUseCase userLookupUseCase;

    /**
     * Looks up a user by document type and number.
     * Returns partial user data for form autocompletion.
     * Query params: documentType, documentNumber
     */
    public Mono<ServerResponse> lookupByDocument(ServerRequest request) {
        String documentType = request.queryParam("documentType").orElse("");
        String documentNumber = request.queryParam("documentNumber").orElse("");

        if (documentType.isBlank() || documentNumber.isBlank()) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(400, "documentType y documentNumber son requeridos"));
        }

        return userLookupUseCase.lookupByDocument(documentType, documentNumber)
                .map(this::toUserLookupResponse)
                .flatMap(data -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.success(data, "Usuario encontrado")))
                .switchIfEmpty(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.success(Map.of("found", false), "Usuario no encontrado")));
    }

    /**
     * Looks up a user by email.
     * Returns partial user data for form autocompletion.
     * Query param: email
     */
    public Mono<ServerResponse> lookupByEmail(ServerRequest request) {
        String email = request.queryParam("email").orElse("");

        if (email.isBlank()) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiResponse.error(400, "email es requerido"));
        }

        return userLookupUseCase.lookupByEmail(email)
                .map(this::toUserLookupResponse)
                .flatMap(data -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.success(data, "Usuario encontrado")))
                .switchIfEmpty(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.success(Map.of("found", false), "Usuario no encontrado")));
    }

    /**
     * Searches units by code prefix for autocomplete.
     * Query params: query
     * Organization is extracted from JWT security context.
     */
    public Mono<ServerResponse> searchUnits(ServerRequest request) {
        return SecurityContextHelper.extractAuthContext()
                .flatMap(ctx -> {
                    Long organizationId = ctx.organizationId();
                    if (organizationId == null) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.error(400, "No se pudo determinar la organización del contexto de autenticación"));
                    }

                    String query = request.queryParam("query").orElse("");
                    if (query.isBlank()) {
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ApiResponse.success(java.util.List.of(), "Sin resultados"));
                    }

                    return userLookupUseCase.searchUnits(organizationId, query)
                            .map(this::toUnitResponse)
                            .collectList()
                            .flatMap(units -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(ApiResponse.success(units, "Unidades encontradas")));
                })
                .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.error(401, "No autenticado")));
    }

    private Map<String, Object> toUserLookupResponse(AuthUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("found", true);
        data.put("names", user.getNames() != null ? user.getNames() : "");
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("phone", user.getPhone() != null ? user.getPhone() : "");
        if (user.getDocumentType() != null) {
            data.put("documentType", user.getDocumentType().name());
        }
        data.put("documentNumber", user.getDocumentNumber() != null ? user.getDocumentNumber() : "");
        return data;
    }

    private Map<String, Object> toUnitResponse(Unit unit) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", unit.getId());
        data.put("code", unit.getCode() != null ? unit.getCode() : "");
        data.put("type", unit.getType() != null ? unit.getType() : "");
        return data;
    }
}
