package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncAccessEventsUseCaseTest {

    @Mock private AccessEventRepository accessEventRepository;

    private SyncAccessEventsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SyncAccessEventsUseCase(accessEventRepository);
    }

    @Test
    void shouldReturnEmptyForNullList() {
        StepVerifier.create(useCase.execute(null))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForEmptyList() {
        StepVerifier.create(useCase.execute(Collections.emptyList()))
                .verifyComplete();
    }

    @Test
    void shouldSyncBatchOfEvents() {
        List<AccessEvent> events = List.of(
                AccessEvent.builder().organizationId(1L).porterUserId(10L)
                        .scanResult(ScanResult.VALID).scannedAt(Instant.now()).build(),
                AccessEvent.builder().organizationId(1L).porterUserId(10L)
                        .scanResult(ScanResult.INVALID).scannedAt(Instant.now()).build()
        );

        when(accessEventRepository.saveBatch(anyList()))
                .thenAnswer(inv -> {
                    List<AccessEvent> saved = inv.getArgument(0);
                    return Flux.fromIterable(saved).map(e -> e.toBuilder().id((long) (saved.indexOf(e) + 1)).build());
                });

        StepVerifier.create(useCase.execute(events))
                .assertNext(event -> {
                    assertThat(event.getId()).isNotNull();
                    assertThat(event.getSyncedAt()).isNotNull();
                })
                .assertNext(event -> assertThat(event.getSyncedAt()).isNotNull())
                .verifyComplete();
    }
}
