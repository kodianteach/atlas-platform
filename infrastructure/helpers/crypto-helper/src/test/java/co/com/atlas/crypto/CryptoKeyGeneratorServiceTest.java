package co.com.atlas.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para CryptoKeyGeneratorService.
 */
class CryptoKeyGeneratorServiceTest {

    private CryptoKeyGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new CryptoKeyGeneratorService("TestMasterKey256bit!ForUnitTests");
    }

    @Test
    @DisplayName("Should generate valid Ed25519 key pair")
    void shouldGenerateKeyPair() {
        KeyPair keyPair = service.generateKeyPair();

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isNotNull();
        assertThat(keyPair.getPrivate()).isNotNull();
        assertThat(keyPair.getPublic().getAlgorithm()).isIn("Ed25519", "EdDSA");
        assertThat(keyPair.getPrivate().getAlgorithm()).isIn("Ed25519", "EdDSA");
    }

    @Test
    @DisplayName("Should export public key as valid JWK format")
    void shouldExportPublicKeyAsJwk() {
        KeyPair keyPair = service.generateKeyPair();

        String jwk = service.exportPublicKeyAsJwk(keyPair.getPublic());

        assertThat(jwk).isNotBlank();
        assertThat(jwk).contains("\"kty\":\"OKP\"");
        assertThat(jwk).contains("\"crv\":\"Ed25519\"");
        assertThat(jwk).contains("\"x\":");
    }

    @Test
    @DisplayName("Should encrypt and decrypt private key round-trip")
    void shouldEncryptAndDecryptRoundTrip() {
        KeyPair keyPair = service.generateKeyPair();
        PrivateKey originalKey = keyPair.getPrivate();

        String encrypted = service.encryptPrivateKey(originalKey);
        assertThat(encrypted).isNotBlank();

        PrivateKey decrypted = service.decryptPrivateKey(encrypted);
        assertThat(decrypted).isNotNull();
        assertThat(decrypted.getEncoded()).isEqualTo(originalKey.getEncoded());
    }

    @Test
    @DisplayName("Decrypted key should produce valid signatures verifiable with original public key")
    void decryptedKeyShouldProduceValidSignatures() throws Exception {
        KeyPair keyPair = service.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();

        String encrypted = service.encryptPrivateKey(keyPair.getPrivate());
        PrivateKey decryptedKey = service.decryptPrivateKey(encrypted);

        // Sign data with decrypted key
        byte[] data = "Test data for signature verification".getBytes();
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(decryptedKey);
        signer.update(data);
        byte[] signature = signer.sign();

        // Verify with original public key
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(publicKey);
        verifier.update(data);
        assertThat(verifier.verify(signature)).isTrue();
    }

    @Test
    @DisplayName("Different key pairs should produce different JWKs")
    void differentKeyPairsShouldProduceDifferentJwks() {
        KeyPair keyPair1 = service.generateKeyPair();
        KeyPair keyPair2 = service.generateKeyPair();

        String jwk1 = service.exportPublicKeyAsJwk(keyPair1.getPublic());
        String jwk2 = service.exportPublicKeyAsJwk(keyPair2.getPublic());

        assertThat(jwk1).isNotEqualTo(jwk2);
    }

    @Test
    @DisplayName("Encrypted output should differ even for same key (random IV)")
    void encryptedOutputShouldDifferDueToRandomIv() {
        KeyPair keyPair = service.generateKeyPair();

        String encrypted1 = service.encryptPrivateKey(keyPair.getPrivate());
        String encrypted2 = service.encryptPrivateKey(keyPair.getPrivate());

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("Should fail to decrypt with wrong master key")
    void shouldFailWithWrongMasterKey() {
        KeyPair keyPair = service.generateKeyPair();
        String encrypted = service.encryptPrivateKey(keyPair.getPrivate());

        CryptoKeyGeneratorService wrongKeyService =
                new CryptoKeyGeneratorService("WrongMasterKeyThatIsDifferent!!");

        assertThatThrownBy(() -> wrongKeyService.decryptPrivateKey(encrypted))
                .isInstanceOf(CryptoKeyGeneratorService.CryptoOperationException.class);
    }
}
