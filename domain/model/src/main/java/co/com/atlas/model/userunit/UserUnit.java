package co.com.atlas.model.userunit;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Modelo de dominio para UserUnit (Vinculación usuario-unidad).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserUnit {
    private Long id;
    private Long userId;
    private Long unitId;
    private Long roleId;
    private OwnershipType ownershipType;
    private Boolean isPrimary;
    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private Boolean isActive;
    private String status;
    private Long invitedBy;
    private Instant joinedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
