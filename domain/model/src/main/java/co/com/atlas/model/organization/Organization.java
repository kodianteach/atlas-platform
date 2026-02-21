package co.com.atlas.model.organization;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Organization (Ciudadela o Conjunto Residencial).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Organization {
    private Long id;
    private Long companyId;
    private String code;
    private String name;
    private String slug;
    private OrganizationType type;
    private Boolean usesZones;
    /**
     * Tipos de unidades permitidas en esta organización.
     * Valores posibles: "HOUSE", "APARTMENT", "HOUSE,APARTMENT"
     */
    private String allowedUnitTypes;
    private String description;
    private String settings;
    private String status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    /**
     * Verifica si esta organización permite un tipo de unidad específico.
     * @param unitType El tipo de unidad a verificar (HOUSE o APARTMENT)
     * @return true si el tipo está permitido, false en caso contrario
     */
    public boolean allowsUnitType(String unitType) {
        if (allowedUnitTypes == null || allowedUnitTypes.isBlank()) {
            return true;
        }
        return allowedUnitTypes.toUpperCase().contains(unitType.toUpperCase());
    }
}
