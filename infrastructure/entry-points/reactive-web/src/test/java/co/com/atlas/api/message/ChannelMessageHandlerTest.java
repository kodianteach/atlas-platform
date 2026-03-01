package co.com.atlas.api.message;

import co.com.atlas.api.message.dto.EditMessageDto;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.message.ChannelMessage;
import co.com.atlas.model.message.MessageStatus;
import co.com.atlas.usecase.message.ChannelMessageUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelMessageHandlerTest {

    @Mock
    private ChannelMessageUseCase channelMessageUseCase;

    private ChannelMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChannelMessageHandler(channelMessageUseCase);
    }

    @Test
    void getHistory_shouldReturn200WithMessages() {
        ChannelMessage msg = ChannelMessage.builder()
                .id(1L).organizationId(1L).senderId(10L)
                .senderName("Admin").senderRole("ADMIN_ATLAS")
                .content("Hola").status(MessageStatus.SENT)
                .isEdited(false).createdAt(Instant.now())
                .build();

        when(channelMessageUseCase.getHistory(eq(1L), any(Instant.class)))
                .thenReturn(Flux.just(msg));

        MockServerRequest request = MockServerRequest.builder()
                .header("X-Organization-Id", "1")
                .build();

        StepVerifier.create(handler.getHistory(request))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(200))
                .verifyComplete();
    }

    @Test
    void editMessage_shouldReturn200OnSuccess() {
        ChannelMessage updated = ChannelMessage.builder()
                .id(1L).organizationId(1L).senderId(10L)
                .senderName("Admin").senderRole("ADMIN_ATLAS")
                .content("Editado").status(MessageStatus.SENT)
                .isEdited(true).createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(channelMessageUseCase.editMessage(eq(1L), eq("Editado"), eq(10L)))
                .thenReturn(Mono.just(updated));

        EditMessageDto dto = EditMessageDto.builder().content("Editado").build();
        MockServerRequest request = MockServerRequest.builder()
                .header("X-User-Id", "10")
                .pathVariable("id", "1")
                .body(Mono.just(dto));

        StepVerifier.create(handler.editMessage(request))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(200))
                .verifyComplete();
    }

    @Test
    void editMessage_notOwner_shouldReturn403() {
        when(channelMessageUseCase.editMessage(eq(1L), anyString(), eq(20L)))
                .thenReturn(Mono.error(new BusinessException(
                        "Solo puedes editar tus propios mensajes", "FORBIDDEN", 403)));

        EditMessageDto dto = EditMessageDto.builder().content("Editado").build();
        MockServerRequest request = MockServerRequest.builder()
                .header("X-User-Id", "20")
                .pathVariable("id", "1")
                .body(Mono.just(dto));

        StepVerifier.create(handler.editMessage(request))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(403))
                .verifyComplete();
    }

    @Test
    void deleteMessage_shouldReturn200OnSuccess() {
        when(channelMessageUseCase.deleteMessage(eq(1L), eq(10L)))
                .thenReturn(Mono.empty());

        MockServerRequest request = MockServerRequest.builder()
                .header("X-User-Id", "10")
                .pathVariable("id", "1")
                .build();

        StepVerifier.create(handler.deleteMessage(request))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(200))
                .verifyComplete();
    }

    @Test
    void deleteMessage_notFound_shouldReturn404() {
        when(channelMessageUseCase.deleteMessage(eq(999L), eq(10L)))
                .thenReturn(Mono.error(new NotFoundException("Mensaje", 999L)));

        MockServerRequest request = MockServerRequest.builder()
                .header("X-User-Id", "10")
                .pathVariable("id", "999")
                .build();

        StepVerifier.create(handler.deleteMessage(request))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(404))
                .verifyComplete();
    }
}
