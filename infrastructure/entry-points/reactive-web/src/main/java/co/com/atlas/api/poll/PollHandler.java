package co.com.atlas.api.poll;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.poll.dto.PollOptionResponse;
import co.com.atlas.api.poll.dto.PollRequest;
import co.com.atlas.api.poll.dto.PollResponse;
import co.com.atlas.api.poll.dto.PollResultsResponse;
import co.com.atlas.api.poll.dto.VoteRequest;
import co.com.atlas.model.common.PostPollFilter;
import co.com.atlas.model.poll.Poll;
import co.com.atlas.model.poll.PollOption;
import co.com.atlas.model.poll.gateways.PollVoteRepository;
import co.com.atlas.usecase.poll.PollUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PollHandler {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN_ATLAS", "ADMIN");

    private final PollUseCase pollUseCase;
    private final PollVoteRepository pollVoteRepository;

    public Mono<ServerResponse> create(ServerRequest request) {
        Long authorId = Long.valueOf(request.headers().firstHeader("X-User-Id"));
        
        return request.bodyToMono(PollRequest.class)
                .flatMap(req -> {
                    Poll poll = Poll.builder()
                            .organizationId(req.getOrganizationId())
                            .authorId(authorId)
                            .title(req.getTitle())
                            .description(req.getDescription())
                            .allowMultiple(req.getAllowMultiple())
                            .isAnonymous(req.getIsAnonymous())
                            .build();
                    return pollUseCase.create(poll, req.getOptions());
                })
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return pollUseCase.findById(id)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> findByOrganizationId(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return pollUseCase.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(polls -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(polls, "Encuestas obtenidas exitosamente")));
    }

    public Mono<ServerResponse> findActive(ServerRequest request) {
        Long organizationId = Long.valueOf(request.pathVariable("organizationId"));
        return pollUseCase.findActiveByOrganizationId(organizationId)
                .map(this::toResponse)
                .collectList()
                .flatMap(polls -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(polls, "Encuestas activas obtenidas")));
    }

    public Mono<ServerResponse> activate(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return pollUseCase.activate(id, organizationId)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> close(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return pollUseCase.close(id, organizationId)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> vote(ServerRequest request) {
        Long pollId = Long.valueOf(request.pathVariable("id"));
        Long userId = Long.valueOf(request.headers().firstHeader("X-User-Id"));
        
        return request.bodyToMono(VoteRequest.class)
                .flatMap(req -> pollUseCase.vote(pollId, req.getOptionId(), userId))
                .flatMap(vote -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(vote, "Voto registrado exitosamente")));
    }

    /**
     * Obtiene resultados de encuesta con visibilidad condicional de votantes por rol.
     * ADMIN_ATLAS y ADMIN ven quién votó; otros roles solo ven agregados.
     */
    public Mono<ServerResponse> getResults(ServerRequest request) {
        Long pollId = Long.valueOf(request.pathVariable("id"));
        String userRole = request.headers().firstHeader("X-User-Role");
        boolean isAdmin = userRole != null && ADMIN_ROLES.contains(userRole);
        
        return pollUseCase.getResults(pollId)
                .flatMap(poll -> {
                    if (isAdmin) {
                        return pollVoteRepository.findByPollId(pollId)
                                .map(vote -> PollResultsResponse.VoterInfo.builder()
                                        .userId(vote.getUserId())
                                        .optionId(vote.getOptionId())
                                        .votedAt(vote.getCreatedAt())
                                        .build())
                                .collectList()
                                .flatMap(voters -> {
                                    PollResultsResponse resultsResponse = toResultsResponse(poll, voters);
                                    return ServerResponse.ok()
                                            .bodyValue(ApiResponse.success(resultsResponse, "Resultados obtenidos"));
                                });
                    }
                    PollResultsResponse resultsResponse = toResultsResponse(poll, null);
                    return ServerResponse.ok()
                            .bodyValue(ApiResponse.success(resultsResponse, "Resultados obtenidos"));
                });
    }

    private Mono<ServerResponse> buildSuccessResponse(Poll poll) {
        return ServerResponse.ok()
                .bodyValue(ApiResponse.success(toResponse(poll), "Operación exitosa"));
    }

    private PollResponse toResponse(Poll poll) {
        Long totalVotes = poll.getOptions() != null 
                ? poll.getOptions().stream()
                        .mapToLong(opt -> opt.getVoteCount() != null ? opt.getVoteCount() : 0L)
                        .sum()
                : 0L;

        List<PollOptionResponse> optionResponses = poll.getOptions() != null 
                ? poll.getOptions().stream()
                        .map(opt -> toOptionResponse(opt, totalVotes))
                        .collect(Collectors.toList())
                : null;

        return PollResponse.builder()
                .id(poll.getId())
                .organizationId(poll.getOrganizationId())
                .authorId(poll.getAuthorId())
                .title(poll.getTitle())
                .description(poll.getDescription())
                .allowMultiple(poll.getAllowMultiple())
                .isAnonymous(poll.getIsAnonymous())
                .status(poll.getStatus() != null ? poll.getStatus().name() : null)
                .startsAt(poll.getStartsAt())
                .endsAt(poll.getEndsAt())
                .createdAt(poll.getCreatedAt())
                .options(optionResponses)
                .totalVotes(totalVotes)
                .build();
    }

    private PollOptionResponse toOptionResponse(PollOption option, Long totalVotes) {
        Long voteCount = option.getVoteCount() != null ? option.getVoteCount() : 0L;
        Double percentage = totalVotes > 0 ? (voteCount * 100.0) / totalVotes : 0.0;
        
        return PollOptionResponse.builder()
                .id(option.getId())
                .optionText(option.getOptionText())
                .sortOrder(option.getSortOrder())
                .voteCount(voteCount)
                .percentage(percentage)
                .build();
    }

    /**
     * Mapea Poll a PollResultsResponse con lista de votantes condicional.
     */
    private PollResultsResponse toResultsResponse(Poll poll, List<PollResultsResponse.VoterInfo> voters) {
        Long totalVotes = poll.getOptions() != null
                ? poll.getOptions().stream()
                        .mapToLong(opt -> opt.getVoteCount() != null ? opt.getVoteCount() : 0L)
                        .sum()
                : 0L;

        List<PollOptionResponse> optionResponses = poll.getOptions() != null
                ? poll.getOptions().stream()
                        .map(opt -> toOptionResponse(opt, totalVotes))
                        .toList()
                : null;

        return PollResultsResponse.builder()
                .id(poll.getId())
                .organizationId(poll.getOrganizationId())
                .authorId(poll.getAuthorId())
                .title(poll.getTitle())
                .description(poll.getDescription())
                .allowMultiple(poll.getAllowMultiple())
                .isAnonymous(poll.getIsAnonymous())
                .status(poll.getStatus() != null ? poll.getStatus().name() : null)
                .startsAt(poll.getStartsAt())
                .endsAt(poll.getEndsAt())
                .createdAt(poll.getCreatedAt())
                .options(optionResponses)
                .totalVotes(totalVotes)
                .voters(voters)
                .build();
    }

    /**
     * Búsqueda paginada de encuestas con filtros dinámicos para panel admin.
     */
    public Mono<ServerResponse> searchPolls(ServerRequest request) {
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        PostPollFilter filter = PostPollFilter.builder()
                .status(request.queryParam("status").orElse(null))
                .dateFrom(request.queryParam("dateFrom").map(Instant::parse).orElse(null))
                .dateTo(request.queryParam("dateTo").map(Instant::parse).orElse(null))
                .search(request.queryParam("search").orElse(null))
                .page(request.queryParam("page").map(Integer::parseInt).orElse(0))
                .size(request.queryParam("size").map(Integer::parseInt).orElse(10))
                .build();

        return pollUseCase.findByFilters(organizationId, filter)
                .flatMap(pageResponse -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(pageResponse, "Encuestas obtenidas exitosamente")));
    }

    /**
     * Obtiene estadísticas de encuestas por estado para la organización.
     */
    public Mono<ServerResponse> getPollStats(ServerRequest request) {
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        return pollUseCase.countByStatus(organizationId)
                .flatMap(stats -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(stats, "Estadísticas obtenidas exitosamente")));
    }
}
