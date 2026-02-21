package co.com.atlas.usecase.porter.builders;

import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterType;

import java.time.Instant;

/**
 * Builder de test para Porter (proyección desde users + roles).
 */
public class PorterBuilder {

    private Long id = 10L;
    private String names = "Portería Principal";
    private String email = "porter-xxx@mi-conjunto.atlas.internal";
    private PorterType porterType = PorterType.PORTERO_GENERAL;
    private String status = "PRE_REGISTERED";
    private Long organizationId = 100L;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = null;

    public static PorterBuilder aPorter() {
        return new PorterBuilder();
    }

    public PorterBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public PorterBuilder withNames(String names) {
        this.names = names;
        return this;
    }

    public PorterBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public PorterBuilder withOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public PorterBuilder withPorterType(PorterType porterType) {
        this.porterType = porterType;
        return this;
    }

    public PorterBuilder withStatus(String status) {
        this.status = status;
        return this;
    }

    public Porter build() {
        return Porter.builder()
                .id(id)
                .names(names)
                .email(email)
                .porterType(porterType)
                .status(status)
                .organizationId(organizationId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
