package co.com.atlas.usecase.organization;

import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.company.gateways.CompanyRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Caso de uso para gestión de organizaciones.
 */
@RequiredArgsConstructor
public class OrganizationUseCase {
    
    private final OrganizationRepository organizationRepository;
    private final CompanyRepository companyRepository;
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    
    /**
     * Crea una nueva organización.
     */
    public Mono<Organization> create(Organization organization) {
        return companyRepository.findById(organization.getCompanyId())
                .switchIfEmpty(Mono.error(new NotFoundException("Company", organization.getCompanyId())))
                .flatMap(company -> organizationRepository.existsByCode(organization.getCode())
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                return Mono.error(new DuplicateException("Organization", "code", organization.getCode()));
                            }
                            String slug = generateSlug(organization.getName());
                            Organization newOrg = organization.toBuilder()
                                    .slug(slug)
                                    .status("ACTIVE")
                                    .isActive(true)
                                    .build();
                            return organizationRepository.save(newOrg);
                        }));
    }
    
    /**
     * Obtiene una organización por ID.
     */
    public Mono<Organization> findById(Long id) {
        return organizationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Organization", id)));
    }
    
    /**
     * Obtiene una organización por código.
     */
    public Mono<Organization> findByCode(String code) {
        return organizationRepository.findByCode(code)
                .switchIfEmpty(Mono.error(new NotFoundException("Organization con code " + code)));
    }
    
    /**
     * Lista las organizaciones de una compañía.
     */
    public Flux<Organization> findByCompanyId(Long companyId) {
        return organizationRepository.findByCompanyId(companyId);
    }
    
    /**
     * Lista las organizaciones de un usuario.
     */
    public Flux<Organization> findByUserId(Long userId) {
        return organizationRepository.findByUserId(userId);
    }
    
    /**
     * Lista todas las organizaciones activas.
     */
    public Flux<Organization> findAllActive() {
        return organizationRepository.findAllActive();
    }
    
    /**
     * Actualiza una organización.
     */
    public Mono<Organization> update(Long id, Organization organization) {
        return findById(id)
                .flatMap(existing -> {
                    Organization updated = existing.toBuilder()
                            .name(organization.getName() != null ? organization.getName() : existing.getName())
                            .description(organization.getDescription() != null ? organization.getDescription() : existing.getDescription())
                            .settings(organization.getSettings() != null ? organization.getSettings() : existing.getSettings())
                            .usesZones(organization.getUsesZones() != null ? organization.getUsesZones() : existing.getUsesZones())
                            .build();
                    return organizationRepository.save(updated);
                });
    }
    
    /**
     * Elimina una organización (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(existing -> organizationRepository.delete(id));
    }
    
    private String generateSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
