package co.com.atlas.model.crypto.gateways;

import co.com.atlas.model.crypto.OrganizationCryptoKey;
import reactor.core.publisher.Mono;

/**
 * Gateway para generación y gestión de claves criptográficas.
 * La implementación concreta genera pares Ed25519, exporta JWK y cifra con AES-256/GCM.
 */
public interface CryptoKeyGeneratorGateway {

    /**
     * Genera un par de claves Ed25519, cifra la privada y devuelve
     * un OrganizationCryptoKey listo para persistir.
     *
     * @param organizationId ID de la organización
     * @return OrganizationCryptoKey con publicKeyJwk, privateKeyEncrypted y keyId
     */
    Mono<OrganizationCryptoKey> generateForOrganization(Long organizationId);
}
