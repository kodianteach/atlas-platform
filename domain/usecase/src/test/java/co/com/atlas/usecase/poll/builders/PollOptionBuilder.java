package co.com.atlas.usecase.poll.builders;

import co.com.atlas.model.poll.PollOption;

import java.time.Instant;

/**
 * Builder de test para PollOption.
 */
public class PollOptionBuilder {

    private Long id = 1L;
    private Long pollId = 1L;
    private String optionText = "Opción de prueba";
    private Integer sortOrder = 0;
    private Instant createdAt = Instant.now();
    private Long voteCount = 0L;

    public static PollOptionBuilder aPollOption() {
        return new PollOptionBuilder();
    }

    public PollOptionBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public PollOptionBuilder withPollId(Long pollId) {
        this.pollId = pollId;
        return this;
    }

    public PollOptionBuilder withOptionText(String optionText) {
        this.optionText = optionText;
        return this;
    }

    public PollOptionBuilder withSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public PollOptionBuilder withVoteCount(Long voteCount) {
        this.voteCount = voteCount;
        return this;
    }

    public PollOption build() {
        return PollOption.builder()
                .id(id)
                .pollId(pollId)
                .optionText(optionText)
                .sortOrder(sortOrder)
                .createdAt(createdAt)
                .voteCount(voteCount)
                .build();
    }
}
