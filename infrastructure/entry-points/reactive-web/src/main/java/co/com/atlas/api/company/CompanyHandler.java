package co.com.atlas.api.company;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.company.dto.CompanyRequest;
import co.com.atlas.api.company.dto.CompanyResponse;
import co.com.atlas.model.company.Company;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.usecase.company.CompanyUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler para operaciones de Company.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyHandler {

    private final CompanyUseCase companyUseCase;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CompanyRequest.class)
                .flatMap(req -> {
                    Company company = Company.builder()
                            .name(req.getName())
                            .taxId(req.getTaxId())
                            .industry(req.getIndustry())
                            .website(req.getWebsite())
                            .address(req.getAddress())
                            .country(req.getCountry())
                            .city(req.getCity())
                            .build();
                    return companyUseCase.create(company);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return companyUseCase.findById(id)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getBySlug(ServerRequest request) {
        String slug = request.pathVariable("slug");
        return companyUseCase.findBySlug(slug)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    public Mono<ServerResponse> getAll(ServerRequest request) {
        return companyUseCase.findAllActive()
                .map(this::toResponse)
                .collectList()
                .flatMap(companies -> {
                    ApiResponse<Object> response = ApiResponse.success(companies, "Compañías obtenidas exitosamente");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                });
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return request.bodyToMono(CompanyRequest.class)
                .flatMap(req -> {
                    Company company = Company.builder()
                            .id(id)
                            .name(req.getName())
                            .taxId(req.getTaxId())
                            .industry(req.getIndustry())
                            .website(req.getWebsite())
                            .address(req.getAddress())
                            .country(req.getCountry())
                            .city(req.getCity())
                            .build();
                    return companyUseCase.update(id, company);
                })
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()))
                .onErrorResume(BusinessException.class, e -> buildErrorResponse(e, HttpStatus.BAD_REQUEST, request.path()));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return companyUseCase.delete(id)
                .then(Mono.defer(() -> {
                    ApiResponse<Void> response = ApiResponse.success(null, "Compañía eliminada exitosamente");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(response);
                }))
                .onErrorResume(NotFoundException.class, e -> buildErrorResponse(e, HttpStatus.NOT_FOUND, request.path()));
    }

    private Mono<ServerResponse> buildSuccessResponse(Company company) {
        CompanyResponse data = toResponse(company);
        ApiResponse<CompanyResponse> response = ApiResponse.success(data, "Operación exitosa");
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

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .taxId(company.getTaxId())
                .industry(company.getIndustry())
                .website(company.getWebsite())
                .address(company.getAddress())
                .country(company.getCountry())
                .city(company.getCity())
                .status(company.getStatus())
                .isActive(company.getIsActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
