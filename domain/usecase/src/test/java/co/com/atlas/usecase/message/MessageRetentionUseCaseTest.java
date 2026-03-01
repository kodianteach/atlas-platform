package co.com.atlas.usecase.message;

import co.com.atlas.model.message.gateways.ChannelMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRetentionUseCaseTest {

    @Mock
    private ChannelMessageRepository channelMessageRepository;

    private MessageRetentionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MessageRetentionUseCase(channelMessageRepository);
    }

    @Test
    void cleanOldMessages_shouldDeleteMessagesOlderThan30Days() {
        when(channelMessageRepository.deleteOlderThan(any(Instant.class)))
                .thenReturn(Mono.just(15L));

        StepVerifier.create(useCase.cleanOldMessages())
                .assertNext(count -> assertThat(count).isEqualTo(15L))
                .verifyComplete();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(channelMessageRepository).deleteOlderThan(cutoffCaptor.capture());

        Instant expectedCutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        assertThat(cutoffCaptor.getValue())
                .isBetween(expectedCutoff.minus(1, ChronoUnit.SECONDS),
                        expectedCutoff.plus(1, ChronoUnit.SECONDS));
    }
}
