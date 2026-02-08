package co.com.atlas.model.organization.gateways;

import co.com.atlas.model.organization.Organization;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Organization.
 */
public interface OrganizationRepository {
    
    /**
     * Busca una organización por ID.
     */
    Mono<Organization> findById(Long id);
    
    /**
     * Busca una organización por código.
     */
    Mono<Organization> findByCode(String code);
    
    /**
     * Busca una organización por slug.
     */
    Mono<Organization> findBySlug(String slug);
    
    /**
     * Lista todas las organizaciones de una empresa.
     */
    Flux<Organization> findByCompanyId(Long companyId);
    
    /**
     * Lista todas las organizaciones activas.
     */
    Flux<Organization> findAllActive();
    
    /**
     * Lista las organizaciones a las que pertenece un usuario.
     */
    Flux<Organization> findByUserId(Long userId);
    
    /**
     * Guarda o actualiza una organización.
     */
    Mono<Organization> save(Organization organization);
    
    /**
     * Soft delete de una organización.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si existe una organización con el código especificado.
     */
    Mono<Boolean> existsByCode(String code);
}
