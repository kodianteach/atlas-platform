package co.com.atlas.r2dbc.message;

import co.com.atlas.model.message.ChannelMessage;
import co.com.atlas.model.message.MessageReadStatus;
import co.com.atlas.model.message.MessageStatus;
import co.com.atlas.model.message.gateways.ChannelMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class ChannelMessageRepositoryAdapter implements ChannelMessageRepository {

    private final ChannelMessageReactiveRepository messageRepository;
    private final MessageReadStatusReactiveRepository readStatusRepository;

    @Override
    public Mono<ChannelMessage> save(ChannelMessage message) {
        ChannelMessageEntity entity = toEntity(message);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return messageRepository.save(entity).map(this::toDomain);
    }

    @Override
    public Mono<ChannelMessage> findById(Long id) {
        return messageRepository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Flux<ChannelMessage> findByOrganizationId(Long organizationId, Instant since) {
        return messageRepository.findByOrganizationIdAndCreatedAtAfter(organizationId, since)
                .map(this::toDomain);
    }

    @Override
    public Mono<ChannelMessage> update(ChannelMessage message) {
        ChannelMessageEntity entity = toEntity(message);
        entity.setUpdatedAt(Instant.now());
        return messageRepository.save(entity).map(this::toDomain);
    }

    @Override
    public Mono<Void> softDelete(Long id, Instant deletedAt) {
        return messageRepository.findById(id)
                .flatMap(entity -> {
                    entity.setDeletedAt(deletedAt);
                    entity.setStatus(MessageStatus.DELETED.name());
                    return messageRepository.save(entity);
                })
                .then();
    }

    @Override
    public Mono<Long> deleteOlderThan(Instant cutoff) {
        return messageRepository.deleteByCreatedAtBefore(cutoff);
    }

    @Override
    public Mono<MessageReadStatus> saveReadStatus(MessageReadStatus readStatus) {
        MessageReadStatusEntity entity = toReadStatusEntity(readStatus);
        if (entity.getReadAt() == null) {
            entity.setReadAt(Instant.now());
        }
        return readStatusRepository.save(entity).map(this::toReadStatusDomain);
    }

    @Override
    public Flux<MessageReadStatus> findReadStatusByMessageId(Long messageId) {
        return readStatusRepository.findByMessageId(messageId)
                .map(this::toReadStatusDomain);
    }

    // ---- Mapping methods ----

    private ChannelMessage toDomain(ChannelMessageEntity entity) {
        return ChannelMessage.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .senderId(entity.getSenderId())
                .senderName(entity.getSenderName())
                .senderRole(entity.getSenderRole())
                .content(entity.getContent())
                .status(entity.getStatus() != null ? MessageStatus.valueOf(entity.getStatus()) : null)
                .isEdited(entity.getIsEdited())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private ChannelMessageEntity toEntity(ChannelMessage domain) {
        return ChannelMessageEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .senderId(domain.getSenderId())
                .senderName(domain.getSenderName())
                .senderRole(domain.getSenderRole())
                .content(domain.getContent())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .isEdited(domain.getIsEdited())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    private MessageReadStatus toReadStatusDomain(MessageReadStatusEntity entity) {
        return MessageReadStatus.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .userId(entity.getUserId())
                .readAt(entity.getReadAt())
                .build();
    }

    private MessageReadStatusEntity toReadStatusEntity(MessageReadStatus domain) {
        return MessageReadStatusEntity.builder()
                .id(domain.getId())
                .messageId(domain.getMessageId())
                .userId(domain.getUserId())
                .readAt(domain.getReadAt())
                .build();
    }
}
