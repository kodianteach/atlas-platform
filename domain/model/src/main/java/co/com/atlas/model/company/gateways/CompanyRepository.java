package co.com.atlas.model.company.gateways;

import co.com.atlas.model.company.Company;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Company.
 */
public interface CompanyRepository {
    
    /**
     * Busca una empresa por ID.
     */
    Mono<Company> findById(Long id);
    
    /**
     * Busca una empresa por slug.
     */
    Mono<Company> findBySlug(String slug);
    
    /**
     * Lista todas las empresas activas.
     */
    Flux<Company> findAllActive();
    
    /**
     * Guarda o actualiza una empresa.
     */
    Mono<Company> save(Company company);
    
    /**
     * Soft delete de una empresa.
     */
    Mono<Void> delete(Long id);
    
    /**
     * Verifica si existe una empresa con el slug especificado.
     */
    Mono<Boolean> existsBySlug(String slug);
}
