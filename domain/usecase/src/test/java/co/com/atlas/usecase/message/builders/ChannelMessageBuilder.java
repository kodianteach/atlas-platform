package co.com.atlas.usecase.message.builders;

import co.com.atlas.model.message.ChannelMessage;
import co.com.atlas.model.message.MessageStatus;

import java.time.Instant;

/**
 * Test data builder para ChannelMessage.
 * Patrón fluent: ChannelMessageBuilder.aChannelMessage().withId(1L).build()
 */
public class ChannelMessageBuilder {

    private Long id = 1L;
    private Long organizationId = 1L;
    private Long senderId = 10L;
    private String senderName = "Carlos Rodríguez";
    private String senderRole = "ADMIN_ATLAS";
    private String content = "Mensaje de prueba";
    private MessageStatus status = MessageStatus.SENT;
    private Boolean isEdited = false;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = null;
    private Instant deletedAt = null;

    public static ChannelMessageBuilder aChannelMessage() {
        return new ChannelMessageBuilder();
    }

    public ChannelMessageBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ChannelMessageBuilder withOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public ChannelMessageBuilder withSenderId(Long senderId) {
        this.senderId = senderId;
        return this;
    }

    public ChannelMessageBuilder withSenderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public ChannelMessageBuilder withSenderRole(String senderRole) {
        this.senderRole = senderRole;
        return this;
    }

    public ChannelMessageBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public ChannelMessageBuilder withStatus(MessageStatus status) {
        this.status = status;
        return this;
    }

    public ChannelMessageBuilder withIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
        return this;
    }

    public ChannelMessageBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ChannelMessageBuilder withUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ChannelMessageBuilder withDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
        return this;
    }

    public ChannelMessage build() {
        return ChannelMessage.builder()
                .id(id)
                .organizationId(organizationId)
                .senderId(senderId)
                .senderName(senderName)
                .senderRole(senderRole)
                .content(content)
                .status(status)
                .isEdited(isEdited)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
