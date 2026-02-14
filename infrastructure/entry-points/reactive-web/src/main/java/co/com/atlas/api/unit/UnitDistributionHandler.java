package co.com.atlas.api.unit;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.unit.dto.BulkUploadProcessRequest;
import co.com.atlas.api.unit.dto.BulkUploadProcessResponse;
import co.com.atlas.api.unit.dto.BulkUploadValidationRequest;
import co.com.atlas.api.unit.dto.BulkUploadValidationResponse;
import co.com.atlas.api.unit.dto.UnitDistributionRequest;
import co.com.atlas.api.unit.dto.UnitDistributionResponse;
import co.com.atlas.model.auth.DocumentType;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.unit.BulkUnitRow;
import co.com.atlas.model.unit.OwnerInfo;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.UnitDistribution;
import co.com.atlas.model.unit.UnitType;
import co.com.atlas.usecase.unit.UnitBulkUploadUseCase;
import co.com.atlas.usecase.unit.UnitDistributionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler para distribución y carga masiva de unidades.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnitDistributionHandler {

    private final UnitDistributionUseCase unitDistributionUseCase;
    private final UnitBulkUploadUseCase unitBulkUploadUseCase;

    /**
     * Distribuye unidades por rango (creación manual).
     * 
     * POST /api/units/distribute
     */
    public Mono<ServerResponse> distribute(ServerRequest request) {
        return extractCreatedBy()
            .flatMap(createdBy -> request.bodyToMono(UnitDistributionRequest.class)
                .flatMap(req -> {
                    // Validaciones básicas
                    if (req.getOrganizationId() == null) {
                        return Mono.error(new BusinessException("organizationId es requerido"));
                    }
                    if (req.getRangeStart() == null || req.getRangeEnd() == null) {
                        return Mono.error(new BusinessException("rangeStart y rangeEnd son requeridos"));
                    }
                    
                    UnitType unitType = parseUnitType(req.getUnitType());
                    
                    UnitDistribution distribution = UnitDistribution.builder()
                            .organizationId(req.getOrganizationId())
                            .min(req.getRangeStart())
                            .max(req.getRangeEnd())
                            .code(req.getCodePrefix())
                            .type(unitType)
                            .vehiclesEnabled(req.getVehiclesEnabled())
                            .vehicleLimit(req.getVehicleLimit())
                            .owner(null)
                            .sendInvitationImmediately(false)
                            .towerId(req.getTowerId())
                            .zoneId(req.getZoneId())
                            .floor(req.getFloor())
                            .build();
                    
                    return unitDistributionUseCase.createByDistribution(distribution, createdBy)
                            .collectList();
                })
                .flatMap(units -> {
                    List<Long> unitIds = units.stream()
                            .map(Unit::getId)
                            .collect(Collectors.toList());
                    List<String> unitCodes = units.stream()
                            .map(Unit::getCode)
                            .collect(Collectors.toList());
                    
                    UnitDistributionResponse response = UnitDistributionResponse.builder()
                            .unitsCreated(units.size())
                            .unitIds(unitIds)
                            .unitCodes(unitCodes)
                            .invitationsSent(0) // Por calcular si hay owner
                            .message("Se crearon " + units.size() + " unidades exitosamente")
                            .errors(new HashMap<>())
                            .build();
                    
                    ApiResponse<UnitDistributionResponse> apiResponse = 
                            ApiResponse.success(response, "Distribución completada");
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                })
                .onErrorResume(BusinessException.class, e -> 
                        buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST)));
    }

    /**
     * Valida datos de carga masiva (JSON con lista de filas).
     * 
     * POST /api/units/bulk-upload/validate
     */
    public Mono<ServerResponse> validateBulkUpload(ServerRequest request) {
        Long organizationId = Long.parseLong(request.queryParam("organizationId")
                .orElseThrow(() -> new BusinessException("organizationId es requerido")));
        
        return request.bodyToMono(BulkUploadValidationRequest.class)
                .flatMap(req -> {
                    if (req.getRows() == null || req.getRows().isEmpty()) {
                        return Mono.error(new BusinessException("rows es requerido y no puede estar vacío"));
                    }
                    
                    // Convertir DTOs a modelos de dominio
                    List<BulkUnitRow> rows = req.getRows().stream()
                            .map(this::mapRowToModel)
                            .collect(Collectors.toList());
                    
                    return unitBulkUploadUseCase.validate(rows, organizationId);
                })
                .flatMap(result -> {
                    List<BulkUploadValidationResponse.RowError> errors = new ArrayList<>();
                    
                    // Mapear errores de las filas inválidas
                    if (result.getErrorRows() != null) {
                        for (BulkUnitRow row : result.getErrorRows()) {
                            if (row.getErrors() != null) {
                                for (String errorMsg : row.getErrors()) {
                                    errors.add(BulkUploadValidationResponse.RowError.builder()
                                            .rowNumber(row.getRowNumber())
                                            .field("general")
                                            .error(errorMsg)
                                            .value(null)
                                            .build());
                                }
                            }
                        }
                    }
                    
                    // Mapear preview de filas válidas
                    List<BulkUploadValidationResponse.RowPreview> preview = new ArrayList<>();
                    if (result.getValidRows() != null) {
                        preview = result.getValidRows().stream()
                                .limit(10)
                                .map(row -> BulkUploadValidationResponse.RowPreview.builder()
                                        .rowNumber(row.getRowNumber())
                                        .unitCode(row.getGeneratedCode())
                                        .unitType(null)
                                        .ownerEmail(row.getOwnerEmail())
                                        .ownerDocumentType(row.getDocumentType() != null 
                                                ? row.getDocumentType().name() : null)
                                        .ownerDocumentNumber(row.getDocumentNumber())
                                        .vehiclesEnabled(row.getVehicleLimit() != null 
                                                && row.getVehicleLimit() > 0)
                                        .vehicleLimit(row.getVehicleLimit())
                                        .build())
                                .collect(Collectors.toList());
                    }
                    
                    int validCount = result.getValidRows() != null ? result.getValidRows().size() : 0;
                    int errorCount = result.getErrorRows() != null ? result.getErrorRows().size() : 0;
                    boolean hasCriticalErrors = result.getHasCriticalErrors() != null 
                            && result.getHasCriticalErrors();
                    
                    BulkUploadValidationResponse response = BulkUploadValidationResponse.builder()
                            .validRows(validCount)
                            .errorRows(errorCount)
                            .totalRows(result.getTotalRows())
                            .hasCriticalErrors(hasCriticalErrors)
                            .errors(errors)
                            .preview(preview)
                            .message(hasCriticalErrors 
                                    ? "El archivo contiene errores que deben corregirse" 
                                    : "Datos validados correctamente")
                            .build();
                    
                    ApiResponse<BulkUploadValidationResponse> apiResponse = 
                            ApiResponse.success(response, response.getMessage());
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                })
                .onErrorResume(BusinessException.class, e -> 
                        buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
    }

    /**
     * Procesa carga masiva validada.
     * 
     * POST /api/units/bulk-upload/process
     */
    public Mono<ServerResponse> processBulkUpload(ServerRequest request) {
        Long organizationId = Long.parseLong(request.queryParam("organizationId")
                .orElseThrow(() -> new BusinessException("organizationId es requerido")));
        Long createdById = Long.parseLong(request.queryParam("createdById")
                .orElseThrow(() -> new BusinessException("createdById es requerido")));
        boolean sendInvitations = Boolean.parseBoolean(
                request.queryParam("sendInvitations").orElse("true"));
        
        return request.bodyToMono(BulkUploadProcessRequest.class)
                .flatMap(req -> {
                    if (req.getRows() == null || req.getRows().isEmpty()) {
                        return Mono.error(new BusinessException("rows es requerido y no puede estar vacío"));
                    }
                    
                    UnitType unitType = parseUnitType(req.getUnitType());
                    
                    // Convertir DTOs a modelos de dominio
                    List<BulkUnitRow> rows = req.getRows().stream()
                            .map(this::mapRowToModel)
                            .collect(Collectors.toList());
                    
                    return unitBulkUploadUseCase.processBulk(
                            rows, organizationId, unitType, sendInvitations, createdById)
                            .collectList();
                })
                .flatMap(units -> {
                    List<Long> unitIds = units.stream()
                            .map(Unit::getId)
                            .collect(Collectors.toList());
                    
                    BulkUploadProcessResponse response = BulkUploadProcessResponse.builder()
                            .unitsCreated(units.size())
                            .ownersInvited(0) // TODO: calcular invitaciones enviadas
                            .errorsCount(0)
                            .unitIds(unitIds)
                            .errors(new HashMap<>())
                            .message("Se crearon " + units.size() + " unidades exitosamente")
                            .success(true)
                            .build();
                    
                    ApiResponse<BulkUploadProcessResponse> apiResponse = 
                            ApiResponse.success(response, response.getMessage());
                    
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                })
                .onErrorResume(BusinessException.class, e -> 
                        buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
    }

    /**
     * Convierte DTO de fila a modelo de dominio.
     */
    private BulkUnitRow mapRowToModel(BulkUploadValidationRequest.BulkRowDto dto) {
        DocumentType docType = parseDocumentType(dto.getDocumentType());
        String generatedCode = (dto.getPrefix() != null ? dto.getPrefix() : "") + 
                               (dto.getUnitNumber() != null ? dto.getUnitNumber() : "");
        
        return BulkUnitRow.builder()
                .rowNumber(dto.getRowNumber())
                .unitNumber(dto.getUnitNumber())
                .prefix(dto.getPrefix())
                .ownerEmail(dto.getOwnerEmail())
                .documentNumber(dto.getDocumentNumber())
                .documentTypeCode(dto.getDocumentType())
                .documentType(docType)
                .vehicleLimit(dto.getVehicleLimit())
                .generatedCode(generatedCode)
                .valid(true)
                .errors(new ArrayList<>())
                .build();
    }

    /**
     * Extrae el userId del contexto de seguridad de Spring.
     */
    private Mono<Long> extractCreatedBy() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(authentication -> {
                    String userId = (String) authentication.getPrincipal();
                    return Long.parseLong(userId);
                })
                .defaultIfEmpty(1L); // Fallback para desarrollo
    }

    private DocumentType parseDocumentType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Tipo de documento no reconocido: {}", type);
            return null;
        }
    }

    private UnitType parseUnitType(String type) {
        if (type == null || type.isBlank()) {
            return UnitType.APARTMENT;
        }
        try {
            return UnitType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Tipo de unidad no reconocido: {}", type);
            return UnitType.APARTMENT;
        }
    }

    private Mono<ServerResponse> buildErrorResponse(String message, HttpStatus status) {
        ApiResponse<Object> errorResponse = ApiResponse.error(message);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }
}
