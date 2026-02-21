package co.com.atlas.api.invitation;

import co.com.atlas.api.common.SecurityContextHelper;
import co.com.atlas.api.common.SecurityContextHelper.AuthContext;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.invitation.InvitationFilters;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.usecase.invitation.InvitationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for invitation history queries with filters.
 * Data scoping: ADMIN_ATLAS sees organization-level, OWNER/RESIDENT sees unit-level.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationHistoryHandler {

    private final InvitationUseCase invitationUseCase;

    /**
     * Retrieves invitation history with optional filters.
     * Supports query params: type, status, unitId, search, dateFrom, dateTo.
     * Scopes data based on role from TenantContext.
     */
    public Mono<ServerResponse> getInvitationHistory(ServerRequest request) {
        Long unitId = extractOptionalHeaderLong(request, "X-Unit-Id");
        InvitationFilters filters = buildFilters(request);

        return SecurityContextHelper.extractAuthContext()
                .flatMap(ctx -> {
                    Long organizationId = ctx.organizationId();
                    boolean isAdmin = ctx.hasRole("ADMIN_ATLAS");

                    log.info("Getting invitation history for org={}, isAdmin={}", organizationId, isAdmin);

                    return invitationUseCase.getInvitationHistory(organizationId, unitId, filters, isAdmin)
                            .map(this::toResponseMap)
                            .collectList()
                            .flatMap(invitations -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(ApiResponse.success(invitations, "Historial de invitaciones obtenido")));
                });
    }

    private InvitationFilters buildFilters(ServerRequest request) {
        InvitationFilters.InvitationFiltersBuilder builder = InvitationFilters.builder();

        request.queryParam("type").ifPresent(t -> {
            try {
                builder.type(InvitationType.valueOf(t.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid type values
            }
        });

        request.queryParam("status").ifPresent(s -> {
            try {
                builder.status(InvitationStatus.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid status values
            }
        });

        request.queryParam("unitId").ifPresent(u -> {
            try {
                builder.unitId(Long.parseLong(u));
            } catch (NumberFormatException ignored) {
                // Ignore invalid unit ID
            }
        });

        request.queryParam("search").ifPresent(builder::search);

        request.queryParam("dateFrom").ifPresent(d -> {
            try {
                builder.dateFrom(Instant.parse(d));
            } catch (Exception ignored) {
                // Ignore invalid date format
            }
        });

        request.queryParam("dateTo").ifPresent(d -> {
            try {
                builder.dateTo(Instant.parse(d));
            } catch (Exception ignored) {
                // Ignore invalid date format
            }
        });

        return builder.build();
    }

    private Map<String, Object> toResponseMap(co.com.atlas.model.invitation.Invitation invitation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", invitation.getId());
        map.put("organizationId", invitation.getOrganizationId());
        if (invitation.getUnitId() != null) map.put("unitId", invitation.getUnitId());
        map.put("email", invitation.getEmail() != null ? invitation.getEmail() : "");
        map.put("type", invitation.getType() != null ? invitation.getType().name() : "");
        map.put("status", invitation.getStatus() != null ? invitation.getStatus().name() : "");
        map.put("invitedBy", invitation.getInvitedBy());
        if (invitation.getExpiresAt() != null) map.put("expiresAt", invitation.getExpiresAt().toString());
        if (invitation.getAcceptedAt() != null) map.put("acceptedAt", invitation.getAcceptedAt().toString());
        if (invitation.getCreatedAt() != null) map.put("createdAt", invitation.getCreatedAt().toString());
        return map;
    }

    private Long extractOptionalHeaderLong(ServerRequest request, String headerName) {
        String value = request.headers().firstHeader(headerName);
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
