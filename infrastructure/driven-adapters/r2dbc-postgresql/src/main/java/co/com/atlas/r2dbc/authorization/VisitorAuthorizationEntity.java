package co.com.atlas.r2dbc.authorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entidad de base de datos para visitor_authorizations.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("visitor_authorizations")
public class VisitorAuthorizationEntity {

    @Id
    private Long id;

    @Column("organization_id")
    private Long organizationId;

    @Column("unit_id")
    private Long unitId;

    @Column("created_by_user_id")
    private Long createdByUserId;

    @Column("person_name")
    private String personName;

    @Column("person_document")
    private String personDocument;

    @Column("service_type")
    private String serviceType;

    @Column("valid_from")
    private Instant validFrom;

    @Column("valid_to")
    private Instant validTo;

    @Column("vehicle_plate")
    private String vehiclePlate;

    @Column("vehicle_type")
    private String vehicleType;

    @Column("vehicle_color")
    private String vehicleColor;

    @Column("identity_document_key")
    private String identityDocumentKey;

    @Column("signed_qr")
    private String signedQr;

    private String status;

    @Column("revoked_at")
    private Instant revokedAt;

    @Column("revoked_by")
    private Long revokedBy;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
