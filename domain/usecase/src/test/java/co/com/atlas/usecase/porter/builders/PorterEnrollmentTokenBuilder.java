package co.com.atlas.usecase.porter.builders;

import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Builder de test para PorterEnrollmentToken.
 */
public class PorterEnrollmentTokenBuilder {

    private Long id = 1L;
    private Long userId = 10L;
    private Long organizationId = 100L;
    private String tokenHash = "dGVzdC10b2tlbi1oYXNo";
    private PorterEnrollmentTokenStatus status = PorterEnrollmentTokenStatus.PENDING;
    private Instant createdAt = Instant.now();
    private Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
    private Instant consumedAt = null;
    private Long createdBy = 5L;

    public static PorterEnrollmentTokenBuilder aToken() {
        return new PorterEnrollmentTokenBuilder();
    }

    public PorterEnrollmentTokenBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public PorterEnrollmentTokenBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public PorterEnrollmentTokenBuilder withOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public PorterEnrollmentTokenBuilder withStatus(PorterEnrollmentTokenStatus status) {
        this.status = status;
        return this;
    }

    public PorterEnrollmentTokenBuilder withExpired() {
        this.expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
        return this;
    }

    public PorterEnrollmentTokenBuilder withCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public PorterEnrollmentToken build() {
        return PorterEnrollmentToken.builder()
                .id(id)
                .userId(userId)
                .organizationId(organizationId)
                .tokenHash(tokenHash)
                .status(status)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .consumedAt(consumedAt)
                .createdBy(createdBy)
                .build();
    }
}
