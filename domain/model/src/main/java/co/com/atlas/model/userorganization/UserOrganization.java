package co.com.atlas.model.userorganization;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para UserOrganization (Membresía multi-tenant).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserOrganization {
    private Long id;
    private Long userId;
    private Long organizationId;
    private String status;
    private Instant joinedAt;
    private Instant leftAt;
    private Instant createdAt;
    private Instant updatedAt;
}
