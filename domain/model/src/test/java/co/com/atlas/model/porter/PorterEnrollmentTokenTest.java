package co.com.atlas.model.porter;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class PorterEnrollmentTokenTest {

    @Test
    void isExpired_shouldReturnTrue_whenExpiresAtIsInThePast() {
        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .id(1L)
                .userId(10L)
                .organizationId(100L)
                .tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertTrue(token.isExpired());
    }

    @Test
    void isExpired_shouldReturnFalse_whenExpiresAtIsInTheFuture() {
        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .id(1L)
                .userId(10L)
                .organizationId(100L)
                .tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        assertFalse(token.isExpired());
    }

    @Test
    void isExpired_shouldReturnFalse_whenExpiresAtIsNull() {
        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .id(1L)
                .userId(10L)
                .organizationId(100L)
                .tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(null)
                .build();

        assertFalse(token.isExpired());
    }

    @Test
    void isValid_shouldReturnTrue_whenStatusIsPendingAndNotExpired() {
        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .id(1L)
                .userId(10L)
                .organizationId(100L)
                .tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        assertTrue(token.isValid());
    }

    @Test
    void isValid_shouldReturnFalse_whenStatusIsConsumed() {
        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .id(1L)
                .userId(10L)
                .organizationId(100L)
                .tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.CONSUMED)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        assertFalse(token.isValid());
    }

    @Test
    void isValid_shouldReturnFalse_whenStatusIsRevoked() {
        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .id(1L)
                .userId(10L)
                .organizationId(100L)
                .tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.REVOKED)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        assertFalse(token.isValid());
    }

    @Test
    void isValid_shouldReturnFalse_whenTokenIsExpired() {
        PorterEnrollmentToken token = PorterEnrollmentToken.builder()
                .id(1L)
                .userId(10L)
                .organizationId(100L)
                .tokenHash("hash")
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertFalse(token.isValid());
    }
}
