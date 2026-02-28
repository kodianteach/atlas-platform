package co.com.atlas.usecase.poll.builders;

import co.com.atlas.model.poll.Poll;
import co.com.atlas.model.poll.PollOption;
import co.com.atlas.model.poll.PollStatus;

import java.time.Instant;
import java.util.List;

/**
 * Builder de test para Poll.
 */
public class PollBuilder {

    private Long id = 1L;
    private Long organizationId = 100L;
    private Long authorId = 10L;
    private String title = "Encuesta de prueba";
    private String description = "Descripción de encuesta de prueba";
    private Boolean allowMultiple = false;
    private Boolean isAnonymous = false;
    private PollStatus status = PollStatus.DRAFT;
    private Instant startsAt = null;
    private Instant endsAt = null;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = null;
    private Instant deletedAt = null;
    private List<PollOption> options = null;

    public static PollBuilder aPoll() {
        return new PollBuilder();
    }

    public PollBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public PollBuilder withOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public PollBuilder withAuthorId(Long authorId) {
        this.authorId = authorId;
        return this;
    }

    public PollBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public PollBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public PollBuilder withAllowMultiple(Boolean allowMultiple) {
        this.allowMultiple = allowMultiple;
        return this;
    }

    public PollBuilder withIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
        return this;
    }

    public PollBuilder withStatus(PollStatus status) {
        this.status = status;
        return this;
    }

    public PollBuilder withOptions(List<PollOption> options) {
        this.options = options;
        return this;
    }

    public PollBuilder withStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
        return this;
    }

    public PollBuilder withEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
        return this;
    }

    public Poll build() {
        return Poll.builder()
                .id(id)
                .organizationId(organizationId)
                .authorId(authorId)
                .title(title)
                .description(description)
                .allowMultiple(allowMultiple)
                .isAnonymous(isAnonymous)
                .status(status)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .options(options)
                .build();
    }
}
