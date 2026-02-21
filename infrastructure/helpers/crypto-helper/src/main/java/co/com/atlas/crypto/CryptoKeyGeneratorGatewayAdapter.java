package co.com.atlas.crypto;

import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyGeneratorGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.KeyPair;
import java.time.Instant;
import java.util.UUID;

/**
 * Implementación del gateway de generación de claves criptográficas.
 * Delega al CryptoKeyGeneratorService para operaciones Ed25519 + AES-256/GCM.
 * Las operaciones criptográficas se ejecutan en boundedElastic para no bloquear el event loop.
 */
@Component
@RequiredArgsConstructor
public class CryptoKeyGeneratorGatewayAdapter implements CryptoKeyGeneratorGateway {

    private final CryptoKeyGeneratorService cryptoKeyGeneratorService;

    @Override
    public Mono<OrganizationCryptoKey> generateForOrganization(Long organizationId) {
        return Mono.fromCallable(() -> {
            KeyPair keyPair = cryptoKeyGeneratorService.generateKeyPair();
            String publicKeyJwk = cryptoKeyGeneratorService.exportPublicKeyAsJwk(keyPair.getPublic());
            String privateKeyEncrypted = cryptoKeyGeneratorService.encryptPrivateKey(keyPair.getPrivate());
            String keyId = UUID.randomUUID().toString();

            return OrganizationCryptoKey.builder()
                    .organizationId(organizationId)
                    .algorithm("Ed25519")
                    .keyId(keyId)
                    .publicKeyJwk(publicKeyJwk)
                    .privateKeyEncrypted(privateKeyEncrypted)
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
