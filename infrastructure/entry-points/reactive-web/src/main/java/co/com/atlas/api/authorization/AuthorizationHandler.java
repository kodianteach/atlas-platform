package co.com.atlas.api.authorization;

import co.com.atlas.api.authorization.dto.AuthorizationRequestDto;
import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.ServiceType;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.FileStorageGateway;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.tenant.TenantContext;
import co.com.atlas.usecase.authorization.CreateAuthorizationUseCase;
import co.com.atlas.usecase.authorization.GetAuthorizationByIdUseCase;
import co.com.atlas.usecase.authorization.GetAuthorizationsUseCase;
import co.com.atlas.usecase.authorization.RevokeAuthorizationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Handler para endpoints autenticados de autorización de visitantes.
 * <p>
 * Maneja operaciones CRUD sobre autorizaciones de ingreso con QR firmado.
 * Los endpoints requieren autenticación JWT y el TenantContext debe estar inicializado.
 * </p>
 *
 * @author Atlas Platform Team
 * @since HU #6
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationHandler {

    private final CreateAuthorizationUseCase createAuthorizationUseCase;
    private final GetAuthorizationsUseCase getAuthorizationsUseCase;
    private final GetAuthorizationByIdUseCase getAuthorizationByIdUseCase;
    private final RevokeAuthorizationUseCase revokeAuthorizationUseCase;
    private final FileStorageGateway fileStorageGateway;
    private final AccessEventRepository accessEventRepository;

    /**
     * Crea una nueva autorización de visitante con documento de identidad.
     * Acepta multipart/form-data con campos JSON + archivo PDF.
     *
     * @param request ServerRequest con multipart: "data" (JSON) + "document" (PDF file)
     * @return ServerResponse con la autorización creada
     */
    public Mono<ServerResponse> create(ServerRequest request) {
        Long userId = TenantContext.getUserIdOrThrow();
        Long organizationId = TenantContext.getOrganizationIdOrThrow();

        return request.multipartData()
                .flatMap(multipartData -> {
                    // Extraer el JSON de los datos del formulario
                    Part dataPart = multipartData.getFirst("data");
                    Part documentPart = multipartData.getFirst("document");

                    if (dataPart == null) {
                        return Mono.error(new BusinessException(
                                "El campo 'data' es obligatorio", "MISSING_DATA_FIELD"));
                    }

                    // Leer el JSON del campo "data"
                    Mono<AuthorizationRequestDto> dtoMono;
                    if (dataPart instanceof FormFieldPart formField) {
                        dtoMono = parseFormFieldToDto(formField.value());
                    } else {
                        dtoMono = DataBufferUtils.join(dataPart.content())
                                .map(dataBuffer -> {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    DataBufferUtils.release(dataBuffer);
                                    return new String(bytes);
                                })
                                .flatMap(this::parseFormFieldToDto);
                    }

                    // Leer el PDF del campo "document" (opcional)
                    Mono<byte[]> pdfMono;
                    if (documentPart instanceof FilePart filePart) {
                        pdfMono = DataBufferUtils.join(filePart.content())
                                .map(dataBuffer -> {
                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(bytes);
                                    DataBufferUtils.release(dataBuffer);
                                    return bytes;
                                });
                    } else {
                        pdfMono = Mono.just(new byte[0]);
                    }

                    return Mono.zip(dtoMono, pdfMono);
                })
                .flatMap(tuple -> {
                    AuthorizationRequestDto dto = tuple.getT1();
                    byte[] pdfContent = tuple.getT2();

                    VisitorAuthorization authorization = toDomain(dto, organizationId);
                    return createAuthorizationUseCase.execute(authorization, pdfContent, userId);
                })
                .flatMap(created -> buildSuccessResponse(created, "Autorización creada exitosamente"))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, request));
    }

    /**
     * Lista las autorizaciones según el rol del usuario.
     * ADMIN_ATLAS: todas las de la organización.
     * OWNER: las de sus unidades.
     * TENANT/FAMILY: solo las propias.
     *
     * @param request ServerRequest
     * @return ServerResponse con listado de autorizaciones
     */
    public Mono<ServerResponse> getAll(ServerRequest request) {
        Long userId = TenantContext.getUserIdOrThrow();
        Long organizationId = TenantContext.getOrganizationIdOrThrow();
        List<String> roles = TenantContext.getRoles();

        return getAuthorizationsUseCase.execute(userId, organizationId, roles)
                .collectList()
                .flatMap(authorizations ->
                        buildSuccessResponse(authorizations, "Autorizaciones consultadas exitosamente"))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, request));
    }

    /**
     * Consulta una autorización por ID con validación de acceso.
     *
     * @param request ServerRequest con path variable "id"
     * @return ServerResponse con la autorización encontrada
     */
    public Mono<ServerResponse> getById(ServerRequest request) {
        Long authorizationId = Long.parseLong(request.pathVariable("id"));
        Long userId = TenantContext.getUserIdOrThrow();
        Long organizationId = TenantContext.getOrganizationIdOrThrow();
        List<String> roles = TenantContext.getRoles();

        return getAuthorizationByIdUseCase.execute(authorizationId, userId, organizationId, roles)
                .flatMap(authorization ->
                        buildSuccessResponse(authorization, "Autorización encontrada"))
                .onErrorResume(NotFoundException.class, e -> buildNotFoundResponse(e, request))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, request));
    }

    /**
     * Revoca una autorización activa. Solo el creador o ADMIN_ATLAS pueden revocar.
     *
     * @param request ServerRequest con path variable "id"
     * @return ServerResponse con la autorización revocada
     */
    public Mono<ServerResponse> revoke(ServerRequest request) {
        Long authorizationId = Long.parseLong(request.pathVariable("id"));
        Long userId = TenantContext.getUserIdOrThrow();
        List<String> roles = TenantContext.getRoles();

        return revokeAuthorizationUseCase.execute(authorizationId, userId, roles)
                .flatMap(revoked ->
                        buildSuccessResponse(revoked, "Autorización revocada exitosamente"))
                .onErrorResume(NotFoundException.class, e -> buildNotFoundResponse(e, request))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, request));
    }

    /**
     * Descarga el documento de identidad adjunto a una autorización.
     *
     * @param request ServerRequest con path variable "id"
     * @return ServerResponse con el PDF como application/pdf
     */
    public Mono<ServerResponse> downloadDocument(ServerRequest request) {
        Long authorizationId = Long.parseLong(request.pathVariable("id"));
        Long userId = TenantContext.getUserIdOrThrow();
        Long organizationId = TenantContext.getOrganizationIdOrThrow();
        List<String> roles = TenantContext.getRoles();

        return getAuthorizationByIdUseCase.execute(authorizationId, userId, organizationId, roles)
                .flatMap(authorization -> {
                    String documentKey = authorization.getIdentityDocumentKey();
                    if (documentKey == null || documentKey.isEmpty()) {
                        return Mono.error(new NotFoundException("No hay documento adjunto a esta autorización"));
                    }
                    return fileStorageGateway.retrieve(documentKey);
                })
                .flatMap(pdfBytes ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header("Content-Disposition", "inline; filename=\"documento-identidad.pdf\"")
                                .bodyValue(pdfBytes))
                .onErrorResume(IOException.class, e -> {
                    log.warn("Documento no encontrado en storage: {}", e.getMessage());
                    return buildNotFoundResponse(
                            new NotFoundException("El documento de identidad no está disponible. Puede haber sido eliminado del almacenamiento."),
                            request);
                })
                .onErrorResume(NotFoundException.class, e -> buildNotFoundResponse(e, request))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, request));
    }

    /**
     * Lista los eventos de acceso (validaciones) para una autorización.
     *
     * @param request ServerRequest con path variable "id"
     * @return ServerResponse con listado de AccessEvent
     */
    public Mono<ServerResponse> getAccessEvents(ServerRequest request) {
        Long authorizationId = Long.parseLong(request.pathVariable("id"));
        TenantContext.getUserIdOrThrow();
        TenantContext.getOrganizationIdOrThrow();

        return accessEventRepository.findByAuthorizationId(authorizationId)
                .collectList()
                .flatMap(events ->
                        buildSuccessResponse(events, "Eventos de acceso consultados exitosamente"))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, request));
    }

    // ─── Private Helpers ────────────────────────────────────────────────────

    private Mono<AuthorizationRequestDto> parseFormFieldToDto(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            AuthorizationRequestDto dto = mapper.readValue(json, AuthorizationRequestDto.class);
            return Mono.just(dto);
        } catch (Exception e) {
            return Mono.error(new BusinessException(
                    "Error al parsear los datos de la autorización: " + e.getMessage(),
                    "INVALID_JSON_DATA"));
        }
    }

    private VisitorAuthorization toDomain(AuthorizationRequestDto dto, Long organizationId) {
        return VisitorAuthorization.builder()
                .organizationId(organizationId)
                .unitId(dto.getUnitId())
                .personName(dto.getPersonName())
                .personDocument(dto.getPersonDocument())
                .serviceType(ServiceType.valueOf(dto.getServiceType()))
                .validFrom(Instant.parse(dto.getValidFrom()))
                .validTo(Instant.parse(dto.getValidTo()))
                .vehiclePlate(dto.getVehiclePlate())
                .vehicleType(dto.getVehicleType())
                .vehicleColor(dto.getVehicleColor())
                .status(AuthorizationStatus.ACTIVE)
                .build();
    }

    private <T> Mono<ServerResponse> buildSuccessResponse(T data, String message) {
        ApiResponse<T> response = ApiResponse.success(data, message);
        return ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .bodyValue(response);
    }

    private Mono<ServerResponse> buildErrorResponse(BusinessException e, ServerRequest request) {
        log.warn("Error de negocio en authorization handler: {} - {}", e.getErrorCode(), e.getMessage());
        ApiResponse<Void> response = ApiResponse.error(e.getHttpStatus(), e.getMessage());
        return ServerResponse.status(e.getHttpStatus())
                .contentType(APPLICATION_JSON)
                .bodyValue(response);
    }

    private Mono<ServerResponse> buildNotFoundResponse(NotFoundException e, ServerRequest request) {
        log.warn("Recurso no encontrado: {}", e.getMessage());
        ApiResponse<Void> response = ApiResponse.error(404, e.getMessage());
        return ServerResponse.status(404)
                .contentType(APPLICATION_JSON)
                .bodyValue(response);
    }
}
