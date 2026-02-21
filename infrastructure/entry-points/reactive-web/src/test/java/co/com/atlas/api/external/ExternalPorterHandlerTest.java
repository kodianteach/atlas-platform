package co.com.atlas.api.external;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.porter.EnrollmentResult;
import co.com.atlas.usecase.porter.EnrollPorterDeviceUseCase;
import co.com.atlas.usecase.porter.ValidateEnrollmentTokenUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExternalPorterHandler.
 * Tests validate-token and enroll endpoints with mocked use cases.
 */
@ExtendWith(MockitoExtension.class)
class ExternalPorterHandlerTest {

    @Mock
    private ValidateEnrollmentTokenUseCase validateEnrollmentTokenUseCase;

    @Mock
    private EnrollPorterDeviceUseCase enrollPorterDeviceUseCase;

    @InjectMocks
    private ExternalPorterHandler handler;

    @Nested
    @DisplayName("GET /api/external/porter/validate-token")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return 200 with valid token data")
        void shouldReturnOkWithValidToken() {
            var result = new ValidateEnrollmentTokenUseCase.TokenValidationResult(
                    1L, "Carlos Portería", "Conjunto Residencial Atlas",
                    Instant.now().plusSeconds(86400), true
            );

            when(validateEnrollmentTokenUseCase.execute(anyString()))
                    .thenReturn(Mono.just(result));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/validate-token?token=abc123"))
                    .queryParam("token", "abc123")
                    .build();

            StepVerifier.create(handler.validateToken(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    })
                    .verifyComplete();

            verify(validateEnrollmentTokenUseCase).execute("abc123");
        }

        @Test
        @DisplayName("Should return 400 when token is missing")
        void shouldReturnBadRequestWhenTokenMissing() {
            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/validate-token"))
                    .build();

            StepVerifier.create(handler.validateToken(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    })
                    .verifyComplete();

            verifyNoInteractions(validateEnrollmentTokenUseCase);
        }

        @Test
        @DisplayName("Should return 400 when token is blank")
        void shouldReturnBadRequestWhenTokenBlank() {
            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/validate-token?token="))
                    .queryParam("token", "  ")
                    .build();

            StepVerifier.create(handler.validateToken(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    })
                    .verifyComplete();

            verifyNoInteractions(validateEnrollmentTokenUseCase);
        }

        @Test
        @DisplayName("Should handle BusinessException from use case")
        void shouldHandleBusinessException() {
            when(validateEnrollmentTokenUseCase.execute(anyString()))
                    .thenReturn(Mono.error(new BusinessException("Token expirado", "TOKEN_EXPIRED", 410)));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/validate-token?token=expired-token"))
                    .queryParam("token", "expired-token")
                    .build();

            StepVerifier.create(handler.validateToken(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.GONE);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle unexpected errors gracefully")
        void shouldHandleUnexpectedError() {
            when(validateEnrollmentTokenUseCase.execute(anyString()))
                    .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/validate-token?token=test"))
                    .queryParam("token", "test")
                    .build();

            StepVerifier.create(handler.validateToken(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("POST /api/external/porter/enroll")
    class EnrollTests {

        @Test
        @DisplayName("Should return 200 with enrollment result on success")
        void shouldReturnOkOnSuccessfulEnroll() {
            var enrollResult = new EnrollmentResult(
                    1L, "Carlos Portería", "Conjunto Atlas",
                    "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"test\"}",
                    "key-123", 5
            );

            when(enrollPorterDeviceUseCase.execute(any(EnrollPorterDeviceUseCase.EnrollCommand.class)))
                    .thenReturn(Mono.just(enrollResult));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/enroll"))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("X-Forwarded-For", "192.168.1.100")
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                    .body(Mono.just(createEnrollRequestBody()));

            StepVerifier.create(handler.enroll(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    })
                    .verifyComplete();

            verify(enrollPorterDeviceUseCase).execute(any());
        }

        @Test
        @DisplayName("Should extract client IP from X-Forwarded-For header")
        void shouldExtractClientIpFromXForwardedFor() {
            var enrollResult = new EnrollmentResult(
                    1L, "Carlos", "Atlas", "{}", "key-1", 5
            );

            when(enrollPorterDeviceUseCase.execute(any(EnrollPorterDeviceUseCase.EnrollCommand.class)))
                    .thenReturn(Mono.just(enrollResult));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/enroll"))
                    .header("X-Forwarded-For", "10.0.0.5")
                    .header("User-Agent", "TestAgent")
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                    .body(Mono.just(createEnrollRequestBody()));

            StepVerifier.create(handler.enroll(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
                    })
                    .verifyComplete();

            verify(enrollPorterDeviceUseCase).execute(argThat(cmd ->
                    "10.0.0.5".equals(cmd.clientIp())
            ));
        }

        @Test
        @DisplayName("Should handle BusinessException during enrollment")
        void shouldHandleBusinessExceptionDuringEnroll() {
            when(enrollPorterDeviceUseCase.execute(any(EnrollPorterDeviceUseCase.EnrollCommand.class)))
                    .thenReturn(Mono.error(new BusinessException("Token ya consumido", "TOKEN_CONSUMED", 410)));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/enroll"))
                    .header("User-Agent", "TestAgent")
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                    .body(Mono.just(createEnrollRequestBody()));

            StepVerifier.create(handler.enroll(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.GONE);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle unexpected errors during enrollment")
        void shouldHandleUnexpectedErrorDuringEnroll() {
            when(enrollPorterDeviceUseCase.execute(any(EnrollPorterDeviceUseCase.EnrollCommand.class)))
                    .thenReturn(Mono.error(new RuntimeException("Crypto key generation failed")));

            MockServerRequest request = MockServerRequest.builder()
                    .uri(URI.create("/api/external/porter/enroll"))
                    .header("User-Agent", "TestAgent")
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                    .body(Mono.just(createEnrollRequestBody()));

            StepVerifier.create(handler.enroll(request))
                    .assertNext(response -> {
                        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    })
                    .verifyComplete();
        }

        private co.com.atlas.api.external.dto.EnrollDeviceRequest createEnrollRequestBody() {
            var req = new co.com.atlas.api.external.dto.EnrollDeviceRequest();
            req.setToken("test-token-123");
            req.setPlatform("Android");
            req.setModel("Samsung Galaxy S24");
            req.setAppVersion("1.0.0");
            return req;
        }
    }
}
