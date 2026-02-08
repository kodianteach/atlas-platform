package co.com.atlas.usecase.poll;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.poll.Poll;
import co.com.atlas.model.poll.PollOption;
import co.com.atlas.model.poll.PollStatus;
import co.com.atlas.model.poll.PollVote;
import co.com.atlas.model.poll.gateways.PollOptionRepository;
import co.com.atlas.model.poll.gateways.PollRepository;
import co.com.atlas.model.poll.gateways.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Caso de uso para gestión de encuestas.
 */
@RequiredArgsConstructor
public class PollUseCase {
    
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    
    /**
     * Crea una nueva encuesta con opciones.
     */
    public Mono<Poll> create(Poll poll, List<String> optionTexts) {
        if (optionTexts == null || optionTexts.size() < 2) {
            return Mono.error(new BusinessException("INVALID_OPTIONS", "Una encuesta debe tener al menos 2 opciones"));
        }
        
        Poll newPoll = poll.toBuilder()
                .status(PollStatus.DRAFT)
                .createdAt(Instant.now())
                .build();
        
        return pollRepository.save(newPoll)
                .flatMap(savedPoll -> {
                    Flux<PollOption> optionsFlux = Flux.fromIterable(optionTexts)
                            .index()
                            .map(tuple -> PollOption.builder()
                                    .pollId(savedPoll.getId())
                                    .optionText(tuple.getT2())
                                    .sortOrder(tuple.getT1().intValue())
                                    .createdAt(Instant.now())
                                    .build());
                    
                    return pollOptionRepository.saveAll(optionsFlux)
                            .collectList()
                            .map(options -> savedPoll.toBuilder().options(options).build());
                });
    }
    
    /**
     * Obtiene una encuesta por ID con opciones y conteo de votos.
     */
    public Mono<Poll> findById(Long id) {
        return pollRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Poll", id)))
                .flatMap(poll -> pollOptionRepository.findByPollId(id)
                        .flatMap(option -> pollVoteRepository.countByOptionId(option.getId())
                                .map(count -> option.toBuilder().voteCount(count).build()))
                        .collectList()
                        .map(options -> poll.toBuilder().options(options).build()));
    }
    
    /**
     * Lista encuestas de una organización.
     */
    public Flux<Poll> findByOrganizationId(Long organizationId) {
        return pollRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Lista encuestas activas de una organización.
     */
    public Flux<Poll> findActiveByOrganizationId(Long organizationId) {
        return pollRepository.findActiveByOrganizationId(organizationId);
    }
    
    /**
     * Activa una encuesta (cambia estado a ACTIVE).
     */
    public Mono<Poll> activate(Long id) {
        return findById(id)
                .flatMap(poll -> {
                    if (poll.getStatus() != PollStatus.DRAFT) {
                        return Mono.error(new BusinessException("INVALID_STATE", "Solo se pueden activar encuestas en borrador"));
                    }
                    Poll updatedPoll = poll.toBuilder()
                            .status(PollStatus.ACTIVE)
                            .startsAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return pollRepository.save(updatedPoll);
                });
    }
    
    /**
     * Cierra una encuesta.
     */
    public Mono<Poll> close(Long id) {
        return findById(id)
                .flatMap(poll -> {
                    if (poll.getStatus() != PollStatus.ACTIVE) {
                        return Mono.error(new BusinessException("INVALID_STATE", "Solo se pueden cerrar encuestas activas"));
                    }
                    Poll updatedPoll = poll.toBuilder()
                            .status(PollStatus.CLOSED)
                            .endsAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return pollRepository.save(updatedPoll);
                });
    }
    
    /**
     * Emite un voto en una encuesta.
     */
    public Mono<PollVote> vote(Long pollId, Long optionId, Long userId) {
        return findById(pollId)
                .flatMap(poll -> {
                    if (poll.getStatus() != PollStatus.ACTIVE) {
                        return Mono.error(new BusinessException("POLL_NOT_ACTIVE", "La encuesta no está activa"));
                    }
                    
                    // Verificar que la opción pertenece a la encuesta
                    boolean validOption = poll.getOptions().stream()
                            .anyMatch(opt -> opt.getId().equals(optionId));
                    if (!validOption) {
                        return Mono.error(new BusinessException("INVALID_OPTION", "La opción no pertenece a esta encuesta"));
                    }
                    
                    // Verificar si ya votó (solo si no es anónimo y no permite múltiple)
                    if (!Boolean.TRUE.equals(poll.getAllowMultiple()) && !Boolean.TRUE.equals(poll.getIsAnonymous())) {
                        return pollVoteRepository.existsByPollIdAndUserId(pollId, userId)
                                .flatMap(exists -> {
                                    if (Boolean.TRUE.equals(exists)) {
                                        return Mono.error(new BusinessException("ALREADY_VOTED", "Ya has votado en esta encuesta"));
                                    }
                                    return saveVote(pollId, optionId, userId, poll.getIsAnonymous());
                                });
                    }
                    
                    return saveVote(pollId, optionId, userId, poll.getIsAnonymous());
                });
    }
    
    private Mono<PollVote> saveVote(Long pollId, Long optionId, Long userId, Boolean isAnonymous) {
        PollVote vote = PollVote.builder()
                .pollId(pollId)
                .optionId(optionId)
                .userId(Boolean.TRUE.equals(isAnonymous) ? null : userId)
                .createdAt(Instant.now())
                .build();
        return pollVoteRepository.save(vote);
    }
    
    /**
     * Obtiene resultados de una encuesta.
     */
    public Mono<Poll> getResults(Long pollId) {
        return findById(pollId);
    }
}
