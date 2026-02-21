package co.com.atlas.r2dbc.crypto;

import co.com.atlas.model.crypto.OrganizationCryptoKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CryptoKeyRepositoryAdapter.
 * Verifies correct mapping between domain model and R2DBC entity,
 * including INT↔Long conversion for organization_id.
 */
@ExtendWith(MockitoExtension.class)
class CryptoKeyRepositoryAdapterTest {

    @Mock
    private CryptoKeyReactiveRepository reactiveRepository;

    @InjectMocks
    private CryptoKeyRepositoryAdapter adapter;

    private OrganizationCryptoKey sampleDomainKey;
    private CryptoKeyEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleDomainKey = OrganizationCryptoKey.builder()
                .organizationId(42L)
                .algorithm("Ed25519")
                .keyId("key-abc-123")
                .publicKeyJwk("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"testPublicKey\"}")
                .privateKeyEncrypted("encrypted-private-key-data")
                .isActive(true)
                .build();

        sampleEntity = CryptoKeyEntity.builder()
                .id(1)
                .organizationId(42)
                .algorithm("Ed25519")
                .keyId("key-abc-123")
                .publicKeyJwk("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"testPublicKey\"}")
                .privateKeyEncrypted("encrypted-private-key-data")
                .isActive(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save domain key and return with generated ID")
    void shouldSaveAndReturnWithId() {
        when(reactiveRepository.save(any(CryptoKeyEntity.class)))
                .thenReturn(Mono.just(sampleEntity));

        StepVerifier.create(adapter.save(sampleDomainKey))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isEqualTo(1L);
                    assertThat(saved.getOrganizationId()).isEqualTo(42L);
                    assertThat(saved.getAlgorithm()).isEqualTo("Ed25519");
                    assertThat(saved.getKeyId()).isEqualTo("key-abc-123");
                    assertThat(saved.getPublicKeyJwk()).contains("testPublicKey");
                    assertThat(saved.getPrivateKeyEncrypted()).isEqualTo("encrypted-private-key-data");
                    assertThat(saved.getIsActive()).isTrue();
                    assertThat(saved.getCreatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(reactiveRepository).save(argThat(entity -> {
            assertThat(entity.getOrganizationId()).isEqualTo(42); // INT not Long
            assertThat(entity.getCreatedAt()).isNotNull(); // auto-set if null
            return true;
        }));
    }

    @Test
    @DisplayName("Should find active key by organization ID with INT conversion")
    void shouldFindActiveByOrganizationId() {
        when(reactiveRepository.findByOrganizationIdAndIsActive(eq(42), eq(true)))
                .thenReturn(Mono.just(sampleEntity));

        StepVerifier.create(adapter.findActiveByOrganizationId(42L))
                .assertNext(key -> {
                    assertThat(key.getOrganizationId()).isEqualTo(42L);
                    assertThat(key.getIsActive()).isTrue();
                    assertThat(key.getKeyId()).isEqualTo("key-abc-123");
                })
                .verifyComplete();

        // Verify Long→int conversion happened
        verify(reactiveRepository).findByOrganizationIdAndIsActive(42, true);
    }

    @Test
    @DisplayName("Should return empty when no active key for organization")
    void shouldReturnEmptyWhenNoActiveKey() {
        when(reactiveRepository.findByOrganizationIdAndIsActive(eq(99), eq(true)))
                .thenReturn(Mono.empty());

        StepVerifier.create(adapter.findActiveByOrganizationId(99L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find key by key ID")
    void shouldFindByKeyId() {
        when(reactiveRepository.findByKeyId(eq("key-abc-123")))
                .thenReturn(Mono.just(sampleEntity));

        StepVerifier.create(adapter.findByKeyId("key-abc-123"))
                .assertNext(key -> {
                    assertThat(key.getKeyId()).isEqualTo("key-abc-123");
                    assertThat(key.getPublicKeyJwk()).contains("Ed25519");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when key ID not found")
    void shouldReturnEmptyWhenKeyIdNotFound() {
        when(reactiveRepository.findByKeyId(eq("nonexistent")))
                .thenReturn(Mono.empty());

        StepVerifier.create(adapter.findByKeyId("nonexistent"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should set createdAt on save if null")
    void shouldSetCreatedAtOnSaveIfNull() {
        OrganizationCryptoKey keyWithoutCreatedAt = OrganizationCryptoKey.builder()
                .organizationId(1L)
                .algorithm("Ed25519")
                .keyId("new-key")
                .publicKeyJwk("{}")
                .privateKeyEncrypted("enc")
                .isActive(true)
                .build();

        when(reactiveRepository.save(any(CryptoKeyEntity.class)))
                .thenAnswer(invocation -> {
                    CryptoKeyEntity entity = invocation.getArgument(0);
                    entity.setId(10);
                    return Mono.just(entity);
                });

        StepVerifier.create(adapter.save(keyWithoutCreatedAt))
                .assertNext(saved -> {
                    assertThat(saved.getCreatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(reactiveRepository).save(argThat(entity -> entity.getCreatedAt() != null));
    }

    @Test
    @DisplayName("Should handle null ID in domain to entity conversion")
    void shouldHandleNullIdInConversion() {
        OrganizationCryptoKey keyWithNullId = OrganizationCryptoKey.builder()
                .organizationId(5L)
                .algorithm("Ed25519")
                .keyId("new-key")
                .publicKeyJwk("{}")
                .privateKeyEncrypted("enc")
                .isActive(true)
                .build();

        when(reactiveRepository.save(any(CryptoKeyEntity.class)))
                .thenReturn(Mono.just(sampleEntity));

        StepVerifier.create(adapter.save(keyWithNullId))
                .assertNext(saved -> assertThat(saved.getId()).isNotNull())
                .verifyComplete();

        verify(reactiveRepository).save(argThat(entity -> entity.getId() == null));
    }
}
