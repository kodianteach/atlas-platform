package co.com.atlas.api.me;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.unit.gateways.UnitRepository;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import co.com.atlas.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para endpoints del usuario autenticado (/api/me).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MeHandler {

    private final OrganizationRepository organizationRepository;
    private final UserUnitRepository userUnitRepository;
    private final UnitRepository unitRepository;

    /**
     * GET /api/me/residence
     * Retorna la organización y unidad principal del usuario autenticado.
     */
    public Mono<ServerResponse> getMyResidence(ServerRequest request) {
        Long userId = TenantContext.getUserIdOrThrow();
        Long organizationId = TenantContext.getOrganizationIdOrThrow();

        Mono<String> orgNameMono = organizationRepository.findById(organizationId)
                .map(org -> org.getName())
                .defaultIfEmpty("Organización");

        Mono<MyResidenceResponse> responseMono = userUnitRepository.findPrimaryByUserId(userId)
                .flatMap(userUnit -> unitRepository.findById(userUnit.getUnitId())
                        .map(unit -> MyResidenceResponse.builder()
                                .unitCode(unit.getCode())
                                .ownershipType(userUnit.getOwnershipType() != null
                                        ? userUnit.getOwnershipType().name() : "RESIDENT")
                                .build())
                )
                .switchIfEmpty(Mono.defer(() ->
                        // If no primary, try first active unit
                        userUnitRepository.findActiveByUserId(userId)
                                .next()
                                .flatMap(userUnit -> unitRepository.findById(userUnit.getUnitId())
                                        .map(unit -> MyResidenceResponse.builder()
                                                .unitCode(unit.getCode())
                                                .ownershipType(userUnit.getOwnershipType() != null
                                                        ? userUnit.getOwnershipType().name() : "RESIDENT")
                                                .build())
                                )
                                .switchIfEmpty(Mono.just(MyResidenceResponse.builder()
                                        .unitCode(null)
                                        .ownershipType("RESIDENT")
                                        .build()))
                ));

        return Mono.zip(orgNameMono, responseMono)
                .map(tuple -> {
                    MyResidenceResponse res = tuple.getT2();
                    res.setOrganizationName(tuple.getT1());
                    res.setRoleName(resolveRoleName(res.getOwnershipType()));
                    return res;
                })
                .flatMap(res -> {
                    ApiResponse<MyResidenceResponse> apiResponse = ApiResponse.success(res, "Residencia del usuario");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                })
                .onErrorResume(e -> {
                    log.error("Error obteniendo residencia del usuario: {}", e.getMessage());
                    ApiResponse<Object> errorResponse = ApiResponse.error("No se pudo obtener la información de residencia");
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(errorResponse);
                });
    }

    private String resolveRoleName(String ownershipType) {
        if (ownershipType == null) return "Residente";
        return switch (ownershipType) {
            case "OWNER" -> "Propietario";
            case "TENANT" -> "Arrendatario";
            case "FAMILY" -> "Familiar";
            case "GUEST" -> "Invitado";
            default -> "Residente";
        };
    }
}
