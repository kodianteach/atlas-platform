package co.com.atlas.r2dbc.crypto;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para organization_crypto_keys.
 */
public interface CryptoKeyReactiveRepository extends ReactiveCrudRepository<CryptoKeyEntity, Integer> {

    Mono<CryptoKeyEntity> findByOrganizationIdAndIsActive(Integer organizationId, Boolean isActive);

    Mono<CryptoKeyEntity> findByKeyId(String keyId);
}
