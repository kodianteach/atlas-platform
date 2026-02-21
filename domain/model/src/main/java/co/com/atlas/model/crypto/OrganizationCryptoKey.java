package co.com.atlas.model.crypto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para par de claves criptográficas por organización.
 * Almacena clave pública (JWK) y clave privada cifrada (AES-256/GCM).
 * La clave pública se entrega al dispositivo de portería para verificación offline.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrganizationCryptoKey {
    private Long id;
    private Long organizationId;
    private String algorithm;
    private String keyId;
    private String publicKeyJwk;
    private String privateKeyEncrypted;
    private Boolean isActive;
    private Instant createdAt;
    private Instant rotatedAt;

    /**
     * Verifica si la clave está activa para uso.
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
}
