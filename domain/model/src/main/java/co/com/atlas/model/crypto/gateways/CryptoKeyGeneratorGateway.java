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

    /**
     * Firma un payload con la clave privada cifrada de la organización.
     * Descifra la clave privada con AES-256/GCM y firma con Ed25519.
     *
     * @param payload Datos a firmar (Base64URL)
     * @param encryptedPrivateKey Clave privada cifrada con AES-256/GCM
     * @return Firma en Base64URL
     */
    Mono<String> signPayload(String payload, String encryptedPrivateKey);
}
