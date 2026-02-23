package co.com.atlas.crypto;

import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyGeneratorGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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

    @Override
    public Mono<String> signPayload(String payload, String encryptedPrivateKey) {
        return Mono.fromCallable(() -> {
            PrivateKey privateKey = cryptoKeyGeneratorService.decryptPrivateKey(encryptedPrivateKey);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(privateKey);
            sig.update(payloadBytes);
            byte[] signature = sig.sign();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
