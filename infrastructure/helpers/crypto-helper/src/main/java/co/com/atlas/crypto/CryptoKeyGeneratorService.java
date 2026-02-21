package co.com.atlas.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Servicio de generación y manejo de claves criptográficas EdDSA (Ed25519).
 * Genera pares de claves, exporta clave pública como JWK,
 * y cifra/descifra la clave privada con AES-256/GCM usando master key.
 */
@Component
public class CryptoKeyGeneratorService {

    private static final String ALGORITHM = "Ed25519";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final byte[] masterKeyBytes;
    private final ObjectMapper objectMapper;

    public CryptoKeyGeneratorService(
            @Value("${atlas.crypto.master-key}") String masterKey) {
        this.masterKeyBytes = deriveMasterKey(masterKey);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Genera un par de claves Ed25519.
     */
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new CryptoOperationException("Error generating Ed25519 key pair", e);
        }
    }

    /**
     * Exporta la clave pública como JWK (JSON Web Key).
     * Formato: { "kty": "OKP", "crv": "Ed25519", "x": "<base64url-public-key>" }
     */
    public String exportPublicKeyAsJwk(PublicKey publicKey) {
        try {
            // Ed25519 public key raw bytes: encoded format is X.509 SubjectPublicKeyInfo
            // The last 32 bytes are the raw key
            byte[] encoded = publicKey.getEncoded();
            byte[] rawPublicKey = Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);

            String x = Base64.getUrlEncoder().withoutPadding().encodeToString(rawPublicKey);

            ObjectNode jwk = objectMapper.createObjectNode();
            jwk.put("kty", "OKP");
            jwk.put("crv", "Ed25519");
            jwk.put("x", x);

            return objectMapper.writeValueAsString(jwk);
        } catch (Exception e) {
            throw new CryptoOperationException("Error exporting public key as JWK", e);
        }
    }

    /**
     * Cifra la clave privada con AES-256/GCM usando la master key.
     * Formato de salida: Base64(IV[12] + ciphertext + authTag)
     */
    public String encryptPrivateKey(PrivateKey privateKey) {
        try {
            byte[] encoded = privateKey.getEncoded();

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(encoded);

            // Concatenate IV + ciphertext (includes auth tag)
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new CryptoOperationException("Error encrypting private key", e);
        }
    }

    /**
     * Descifra la clave privada cifrada con AES-256/GCM.
     */
    public PrivateKey decryptPrivateKey(String encrypted) {
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] privateKeyBytes = cipher.doFinal(ciphertext);

            // Reconstruct Ed25519 private key from PKCS#8 encoding
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            java.security.spec.PKCS8EncodedKeySpec pkcs8Spec =
                    new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
            return keyFactory.generatePrivate(pkcs8Spec);
        } catch (Exception e) {
            throw new CryptoOperationException("Error decrypting private key", e);
        }
    }

    /**
     * Derives a 256-bit key from the master key string.
     * Uses SHA-256 hash to ensure consistent 32-byte key regardless of input length.
     */
    private byte[] deriveMasterKey(String masterKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(masterKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new CryptoOperationException("Error deriving master key", e);
        }
    }

    /**
     * Runtime exception for crypto operation failures.
     */
    public static class CryptoOperationException extends RuntimeException {
        public CryptoOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
