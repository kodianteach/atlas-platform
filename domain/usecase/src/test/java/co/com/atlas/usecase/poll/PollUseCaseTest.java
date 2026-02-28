package co.com.atlas.usecase.poll;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.poll.Poll;
import co.com.atlas.model.poll.PollOption;
import co.com.atlas.model.poll.PollStatus;
import co.com.atlas.model.poll.PollVote;
import co.com.atlas.model.poll.gateways.PollOptionRepository;
import co.com.atlas.model.poll.gateways.PollRepository;
import co.com.atlas.model.poll.gateways.PollVoteRepository;
import co.com.atlas.usecase.poll.builders.PollBuilder;
import co.com.atlas.usecase.poll.builders.PollOptionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollUseCaseTest {

    @Mock private PollRepository pollRepository;
    @Mock private PollOptionRepository pollOptionRepository;
    @Mock private PollVoteRepository pollVoteRepository;

    private PollUseCase pollUseCase;

    private static final Long POLL_ID = 1L;
    private static final Long ORG_ID = 100L;
    private static final Long USER_ID = 10L;
    private static final Long OPTION_ID_1 = 1L;
    private static final Long OPTION_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        pollUseCase = new PollUseCase(pollRepository, pollOptionRepository, pollVoteRepository);
    }

    @Test
    void shouldCreatePollWithOptionsSuccessfully() {
        // Arrange
        Poll savedPoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withOrganizationId(ORG_ID)
                .withStatus(PollStatus.DRAFT)
                .build();

        PollOption option1 = PollOptionBuilder.aPollOption().withId(1L).withOptionText("Sí").build();
        PollOption option2 = PollOptionBuilder.aPollOption().withId(2L).withOptionText("No").build();

        when(pollRepository.save(any(Poll.class))).thenReturn(Mono.just(savedPoll));
        when(pollOptionRepository.saveAll(any(Flux.class))).thenReturn(Flux.just(option1, option2));

        Poll input = PollBuilder.aPoll().withId(null).build();

        // Act & Assert
        StepVerifier.create(pollUseCase.create(input, List.of("Sí", "No")))
                .assertNext(result -> {
                    assertThat(result.getStatus()).isEqualTo(PollStatus.DRAFT);
                    assertThat(result.getOptions()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectPollWithLessThanTwoOptions() {
        Poll input = PollBuilder.aPoll().build();

        StepVerifier.create(pollUseCase.create(input, List.of("Solo una opción")))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("al menos 2 opciones"))
                .verify();
    }

    @Test
    void shouldActivateDraftPoll() {
        // Arrange
        Poll draftPoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.DRAFT)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build(),
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_2).withPollId(POLL_ID).build()
                ))
                .build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(draftPoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(draftPoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(0L));
        when(pollRepository.save(any(Poll.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Act & Assert
        StepVerifier.create(pollUseCase.activate(POLL_ID))
                .assertNext(result -> {
                    assertThat(result.getStatus()).isEqualTo(PollStatus.ACTIVE);
                    assertThat(result.getStartsAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectActivationOfNonDraftPoll() {
        Poll activePoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.ACTIVE)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build()
                ))
                .build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(activePoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(activePoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(0L));

        StepVerifier.create(pollUseCase.activate(POLL_ID))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("borrador"))
                .verify();
    }

    @Test
    void shouldCloseActivePoll() {
        Poll activePoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.ACTIVE)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build()
                ))
                .build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(activePoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(activePoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(5L));
        when(pollRepository.save(any(Poll.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(pollUseCase.close(POLL_ID))
                .assertNext(result -> {
                    assertThat(result.getStatus()).isEqualTo(PollStatus.CLOSED);
                    assertThat(result.getEndsAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldVoteSuccessfully() {
        Poll activePoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.ACTIVE)
                .withAllowMultiple(false)
                .withIsAnonymous(false)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build(),
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_2).withPollId(POLL_ID).build()
                ))
                .build();

        PollVote savedVote = PollVote.builder()
                .id(1L).pollId(POLL_ID).optionId(OPTION_ID_1).userId(USER_ID).build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(activePoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(activePoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(0L));
        when(pollVoteRepository.existsByPollIdAndUserId(POLL_ID, USER_ID)).thenReturn(Mono.just(false));
        when(pollVoteRepository.save(any(PollVote.class))).thenReturn(Mono.just(savedVote));

        StepVerifier.create(pollUseCase.vote(POLL_ID, OPTION_ID_1, USER_ID))
                .assertNext(vote -> {
                    assertThat(vote.getUserId()).isEqualTo(USER_ID);
                    assertThat(vote.getOptionId()).isEqualTo(OPTION_ID_1);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateVoteEvenWhenAnonymous() {
        // AC: 4, 5 — Duplicate vote must be rejected even with isAnonymous=true
        Poll anonymousPoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.ACTIVE)
                .withAllowMultiple(false)
                .withIsAnonymous(true)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build()
                ))
                .build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(anonymousPoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(anonymousPoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(3L));
        when(pollVoteRepository.existsByPollIdAndUserId(POLL_ID, USER_ID)).thenReturn(Mono.just(true));

        StepVerifier.create(pollUseCase.vote(POLL_ID, OPTION_ID_1, USER_ID))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("Ya has votado"))
                .verify();
    }

    @Test
    void shouldAlwaysSaveUserIdInVote() {
        // AC: 4 — userId must always be saved for auditing, even when isAnonymous=true
        Poll anonymousPoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.ACTIVE)
                .withAllowMultiple(false)
                .withIsAnonymous(true)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build()
                ))
                .build();

        PollVote savedVote = PollVote.builder()
                .id(1L).pollId(POLL_ID).optionId(OPTION_ID_1).userId(USER_ID).build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(anonymousPoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(anonymousPoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(0L));
        when(pollVoteRepository.existsByPollIdAndUserId(POLL_ID, USER_ID)).thenReturn(Mono.just(false));
        when(pollVoteRepository.save(any(PollVote.class))).thenReturn(Mono.just(savedVote));

        StepVerifier.create(pollUseCase.vote(POLL_ID, OPTION_ID_1, USER_ID))
                .assertNext(vote -> {
                    assertThat(vote.getUserId()).isEqualTo(USER_ID);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectVoteOnClosedPoll() {
        Poll closedPoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.CLOSED)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build()
                ))
                .build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(closedPoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(closedPoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(10L));

        StepVerifier.create(pollUseCase.vote(POLL_ID, OPTION_ID_1, USER_ID))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("no está activa"))
                .verify();
    }

    @Test
    void shouldRejectVoteOnInvalidOption() {
        Poll activePoll = PollBuilder.aPoll()
                .withId(POLL_ID)
                .withStatus(PollStatus.ACTIVE)
                .withOptions(List.of(
                        PollOptionBuilder.aPollOption().withId(OPTION_ID_1).withPollId(POLL_ID).build()
                ))
                .build();

        when(pollRepository.findById(POLL_ID)).thenReturn(Mono.just(activePoll));
        when(pollOptionRepository.findByPollId(POLL_ID)).thenReturn(Flux.fromIterable(activePoll.getOptions()));
        when(pollVoteRepository.countByOptionId(anyLong())).thenReturn(Mono.just(0L));

        Long invalidOptionId = 999L;
        StepVerifier.create(pollUseCase.vote(POLL_ID, invalidOptionId, USER_ID))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("no pertenece"))
                .verify();
    }
}
