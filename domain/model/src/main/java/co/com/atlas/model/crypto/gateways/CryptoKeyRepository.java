package co.com.atlas.model.crypto.gateways;

import co.com.atlas.model.crypto.OrganizationCryptoKey;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de claves criptográficas de organización.
 */
public interface CryptoKeyRepository {

    /**
     * Guarda o actualiza una clave criptográfica.
     */
    Mono<OrganizationCryptoKey> save(OrganizationCryptoKey cryptoKey);

    /**
     * Busca la clave activa de una organización.
     */
    Mono<OrganizationCryptoKey> findActiveByOrganizationId(Long organizationId);

    /**
     * Busca una clave por su key identifier (kid).
     */
    Mono<OrganizationCryptoKey> findByKeyId(String keyId);
}
