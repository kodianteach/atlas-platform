package co.com.atlas.usecase.message;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.message.ChannelMessage;
import co.com.atlas.model.message.MessageReadStatus;
import co.com.atlas.model.message.MessageStatus;
import co.com.atlas.model.message.gateways.ChannelMessageRepository;
import co.com.atlas.usecase.message.builders.ChannelMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelMessageUseCaseTest {

    @Mock
    private ChannelMessageRepository channelMessageRepository;

    private ChannelMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChannelMessageUseCase(channelMessageRepository);
    }

    @Test
    void sendMessage_shouldSaveAndReturnMessage() {
        ChannelMessage message = ChannelMessageBuilder.aChannelMessage()
                .withId(null)
                .withContent("Hola equipo")
                .build();

        ChannelMessage savedMessage = ChannelMessageBuilder.aChannelMessage()
                .withId(1L)
                .withContent("Hola equipo")
                .withStatus(MessageStatus.SENT)
                .build();

        when(channelMessageRepository.save(any(ChannelMessage.class)))
                .thenReturn(Mono.just(savedMessage));

        StepVerifier.create(useCase.sendMessage(message))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getContent()).isEqualTo("Hola equipo");
                    assertThat(result.getStatus()).isEqualTo(MessageStatus.SENT);
                })
                .verifyComplete();
    }

    @Test
    void sendMessage_emptyContent_shouldReturnError() {
        ChannelMessage message = ChannelMessageBuilder.aChannelMessage()
                .withContent("")
                .build();

        StepVerifier.create(useCase.sendMessage(message))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void sendMessage_nullContent_shouldReturnError() {
        ChannelMessage message = ChannelMessageBuilder.aChannelMessage()
                .withContent(null)
                .build();

        StepVerifier.create(useCase.sendMessage(message))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void editMessage_shouldUpdateContentSuccessfully() {
        Long messageId = 1L;
        Long userId = 10L;

        ChannelMessage existing = ChannelMessageBuilder.aChannelMessage()
                .withId(messageId)
                .withSenderId(userId)
                .withContent("Contenido original")
                .build();

        ChannelMessage updated = ChannelMessageBuilder.aChannelMessage()
                .withId(messageId)
                .withSenderId(userId)
                .withContent("Contenido editado")
                .withIsEdited(true)
                .build();

        when(channelMessageRepository.findById(messageId))
                .thenReturn(Mono.just(existing));
        when(channelMessageRepository.update(any(ChannelMessage.class)))
                .thenReturn(Mono.just(updated));

        StepVerifier.create(useCase.editMessage(messageId, "Contenido editado", userId))
                .assertNext(result -> {
                    assertThat(result.getContent()).isEqualTo("Contenido editado");
                    assertThat(result.getIsEdited()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void editMessage_notOwner_shouldReturnError() {
        Long messageId = 1L;
        Long ownerId = 10L;
        Long anotherUserId = 20L;

        ChannelMessage existing = ChannelMessageBuilder.aChannelMessage()
                .withId(messageId)
                .withSenderId(ownerId)
                .build();

        when(channelMessageRepository.findById(messageId))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.editMessage(messageId, "Nuevo contenido", anotherUserId))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void editMessage_deletedMessage_shouldReturnError() {
        Long messageId = 1L;
        Long userId = 10L;

        ChannelMessage existing = ChannelMessageBuilder.aChannelMessage()
                .withId(messageId)
                .withSenderId(userId)
                .withDeletedAt(Instant.now())
                .build();

        when(channelMessageRepository.findById(messageId))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.editMessage(messageId, "Nuevo contenido", userId))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void editMessage_notFound_shouldReturnError() {
        when(channelMessageRepository.findById(999L))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.editMessage(999L, "Contenido", 10L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void deleteMessage_shouldSoftDeleteSuccessfully() {
        Long messageId = 1L;
        Long userId = 10L;

        ChannelMessage existing = ChannelMessageBuilder.aChannelMessage()
                .withId(messageId)
                .withSenderId(userId)
                .build();

        when(channelMessageRepository.findById(messageId))
                .thenReturn(Mono.just(existing));
        when(channelMessageRepository.softDelete(eq(messageId), any(Instant.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.deleteMessage(messageId, userId))
                .verifyComplete();
    }

    @Test
    void deleteMessage_notOwner_shouldReturnError() {
        Long messageId = 1L;
        Long ownerId = 10L;
        Long anotherUserId = 20L;

        ChannelMessage existing = ChannelMessageBuilder.aChannelMessage()
                .withId(messageId)
                .withSenderId(ownerId)
                .build();

        when(channelMessageRepository.findById(messageId))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.deleteMessage(messageId, anotherUserId))
                .expectError(BusinessException.class)
                .verify();
    }

    @Test
    void markAsRead_shouldSaveReadStatus() {
        Long messageId = 1L;
        Long userId = 20L;

        MessageReadStatus readStatus = MessageReadStatus.builder()
                .id(1L)
                .messageId(messageId)
                .userId(userId)
                .readAt(Instant.now())
                .build();

        when(channelMessageRepository.saveReadStatus(any(MessageReadStatus.class)))
                .thenReturn(Mono.just(readStatus));

        StepVerifier.create(useCase.markAsRead(messageId, userId))
                .assertNext(result -> {
                    assertThat(result.getMessageId()).isEqualTo(messageId);
                    assertThat(result.getUserId()).isEqualTo(userId);
                })
                .verifyComplete();
    }

    @Test
    void getHistory_shouldReturnMessages() {
        Long organizationId = 1L;
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);

        ChannelMessage msg1 = ChannelMessageBuilder.aChannelMessage().withId(1L).withContent("Msg 1").build();
        ChannelMessage msg2 = ChannelMessageBuilder.aChannelMessage().withId(2L).withContent("Msg 2").build();

        when(channelMessageRepository.findByOrganizationId(eq(organizationId), any(Instant.class)))
                .thenReturn(Flux.just(msg1, msg2));

        StepVerifier.create(useCase.getHistory(organizationId, since))
                .assertNext(result -> assertThat(result.getContent()).isEqualTo("Msg 1"))
                .assertNext(result -> assertThat(result.getContent()).isEqualTo("Msg 2"))
                .verifyComplete();
    }

    @Test
    void getReadStatus_shouldReturnReadStatuses() {
        Long messageId = 1L;

        MessageReadStatus rs1 = MessageReadStatus.builder()
                .id(1L).messageId(messageId).userId(10L).readAt(Instant.now()).build();
        MessageReadStatus rs2 = MessageReadStatus.builder()
                .id(2L).messageId(messageId).userId(20L).readAt(Instant.now()).build();

        when(channelMessageRepository.findReadStatusByMessageId(messageId))
                .thenReturn(Flux.just(rs1, rs2));

        StepVerifier.create(useCase.getReadStatus(messageId))
                .expectNextCount(2)
                .verifyComplete();
    }
}
