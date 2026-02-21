package co.com.atlas.r2dbc.crypto;

import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway CryptoKeyRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class CryptoKeyRepositoryAdapter implements CryptoKeyRepository {

    private final CryptoKeyReactiveRepository repository;

    @Override
    public Mono<OrganizationCryptoKey> save(OrganizationCryptoKey cryptoKey) {
        CryptoKeyEntity entity = toEntity(cryptoKey);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<OrganizationCryptoKey> findActiveByOrganizationId(Long organizationId) {
        return repository.findByOrganizationIdAndIsActive(organizationId.intValue(), true)
                .map(this::toDomain);
    }

    @Override
    public Mono<OrganizationCryptoKey> findByKeyId(String keyId) {
        return repository.findByKeyId(keyId)
                .map(this::toDomain);
    }

    private OrganizationCryptoKey toDomain(CryptoKeyEntity entity) {
        return OrganizationCryptoKey.builder()
                .id(entity.getId() != null ? entity.getId().longValue() : null)
                .organizationId(entity.getOrganizationId() != null ? entity.getOrganizationId().longValue() : null)
                .algorithm(entity.getAlgorithm())
                .keyId(entity.getKeyId())
                .publicKeyJwk(entity.getPublicKeyJwk())
                .privateKeyEncrypted(entity.getPrivateKeyEncrypted())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .rotatedAt(entity.getRotatedAt())
                .build();
    }

    private CryptoKeyEntity toEntity(OrganizationCryptoKey key) {
        return CryptoKeyEntity.builder()
                .id(key.getId() != null ? key.getId().intValue() : null)
                .organizationId(key.getOrganizationId() != null ? key.getOrganizationId().intValue() : null)
                .algorithm(key.getAlgorithm())
                .keyId(key.getKeyId())
                .publicKeyJwk(key.getPublicKeyJwk())
                .privateKeyEncrypted(key.getPrivateKeyEncrypted())
                .isActive(key.getIsActive())
                .createdAt(key.getCreatedAt())
                .rotatedAt(key.getRotatedAt())
                .build();
    }
}
