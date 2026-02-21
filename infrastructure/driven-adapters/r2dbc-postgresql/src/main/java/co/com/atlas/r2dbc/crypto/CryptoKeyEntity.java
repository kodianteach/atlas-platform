package co.com.atlas.r2dbc.crypto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para organization_crypto_keys.
 * Almacena pares de claves EdDSA (Ed25519) por organización.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("organization_crypto_keys")
public class CryptoKeyEntity {

    @Id
    private Integer id;

    @Column("organization_id")
    private Integer organizationId;

    private String algorithm;

    @Column("key_id")
    private String keyId;

    @Column("public_key_jwk")
    private String publicKeyJwk;

    @Column("private_key_encrypted")
    private String privateKeyEncrypted;

    @Column("is_active")
    private Boolean isActive;

    @Column("created_at")
    private Instant createdAt;

    @Column("rotated_at")
    private Instant rotatedAt;
}
