package co.com.atlas.r2dbc.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para Vehicles.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("vehicles")
public class VehicleEntity {

    @Id
    private Long id;

    @Column("unit_id")
    private Long unitId;

    @Column("organization_id")
    private Long organizationId;

    private String plate;

    @Column("vehicle_type")
    private String vehicleType;

    private String brand;

    private String model;

    private String color;

    @Column("owner_name")
    private String ownerName;

    @Column("is_active")
    private Boolean isActive;

    @Column("registered_by")
    private Long registeredBy;

    private String notes;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("deleted_at")
    private Instant deletedAt;
}
