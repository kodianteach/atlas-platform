package co.com.atlas.usecase.company;

import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.company.Company;
import co.com.atlas.model.company.gateways.CompanyRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Caso de uso para gestión de compañías.
 */
@RequiredArgsConstructor
public class CompanyUseCase {
    
    private final CompanyRepository companyRepository;
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    
    /**
     * Crea una nueva compañía.
     */
    public Mono<Company> create(Company company) {
        String slug = generateSlug(company.getName());
        return companyRepository.existsBySlug(slug)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new DuplicateException("Company", "slug", slug));
                    }
                    Company newCompany = company.toBuilder()
                            .slug(slug)
                            .status("ACTIVE")
                            .isActive(true)
                            .build();
                    return companyRepository.save(newCompany);
                });
    }
    
    /**
     * Obtiene una compañía por ID.
     */
    public Mono<Company> findById(Long id) {
        return companyRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Company", id)));
    }
    
    /**
     * Obtiene una compañía por slug.
     */
    public Mono<Company> findBySlug(String slug) {
        return companyRepository.findBySlug(slug)
                .switchIfEmpty(Mono.error(new NotFoundException("Company con slug " + slug)));
    }
    
    /**
     * Lista todas las compañías activas.
     */
    public Flux<Company> findAllActive() {
        return companyRepository.findAllActive();
    }
    
    /**
     * Actualiza una compañía.
     */
    public Mono<Company> update(Long id, Company company) {
        return findById(id)
                .flatMap(existing -> {
                    Company updated = existing.toBuilder()
                            .name(company.getName() != null ? company.getName() : existing.getName())
                            .taxId(company.getTaxId() != null ? company.getTaxId() : existing.getTaxId())
                            .industry(company.getIndustry() != null ? company.getIndustry() : existing.getIndustry())
                            .website(company.getWebsite() != null ? company.getWebsite() : existing.getWebsite())
                            .address(company.getAddress() != null ? company.getAddress() : existing.getAddress())
                            .country(company.getCountry() != null ? company.getCountry() : existing.getCountry())
                            .city(company.getCity() != null ? company.getCity() : existing.getCity())
                            .build();
                    return companyRepository.save(updated);
                });
    }
    
    /**
     * Elimina una compañía (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(existing -> companyRepository.delete(id));
    }
    
    private String generateSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
