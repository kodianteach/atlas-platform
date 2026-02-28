package co.com.atlas.usecase.poll.builders;

import co.com.atlas.model.poll.PollVote;

import java.time.Instant;

/**
 * Builder de test para PollVote.
 */
public class PollVoteBuilder {

    private Long id = 1L;
    private Long pollId = 1L;
    private Long optionId = 1L;
    private Long userId = 10L;
    private Instant createdAt = Instant.now();

    public static PollVoteBuilder aPollVote() {
        return new PollVoteBuilder();
    }

    public PollVoteBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public PollVoteBuilder withPollId(Long pollId) {
        this.pollId = pollId;
        return this;
    }

    public PollVoteBuilder withOptionId(Long optionId) {
        this.optionId = optionId;
        return this;
    }

    public PollVoteBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public PollVote build() {
        return PollVote.builder()
                .id(id)
                .pollId(pollId)
                .optionId(optionId)
                .userId(userId)
                .createdAt(createdAt)
                .build();
    }
}
