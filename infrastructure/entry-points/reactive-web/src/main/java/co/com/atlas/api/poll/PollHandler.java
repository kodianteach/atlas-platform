package co.com.atlas.api.poll;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.poll.dto.PollOptionResponse;
import co.com.atlas.api.poll.dto.PollRequest;
import co.com.atlas.api.poll.dto.PollResponse;
import co.com.atlas.api.poll.dto.VoteRequest;
import co.com.atlas.model.poll.Poll;
import co.com.atlas.model.poll.PollOption;
import co.com.atlas.usecase.poll.PollUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PollHandler {

    private final PollUseCase pollUseCase;

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
        return pollUseCase.activate(id)
                .flatMap(this::buildSuccessResponse);
    }

    public Mono<ServerResponse> close(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return pollUseCase.close(id)
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

    public Mono<ServerResponse> getResults(ServerRequest request) {
        Long pollId = Long.valueOf(request.pathVariable("id"));
        return pollUseCase.getResults(pollId)
                .flatMap(this::buildSuccessResponse);
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
}
