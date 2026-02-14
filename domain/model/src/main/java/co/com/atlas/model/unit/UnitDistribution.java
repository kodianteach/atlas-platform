package co.com.atlas.model.unit;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Value object para distribución de unidades por rango.
 * Permite crear múltiples unidades con un código prefijo y rango numérico.
 * Ejemplo: code="B", min=1, max=3 genera B-1, B-2, B-3
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class UnitDistribution {
    
    /**
     * ID de la organización donde se crearán las unidades.
     * Requerido.
     */
    private Long organizationId;
    
    /**
     * ID de la zona (opcional, depende de si la organización usa zonas).
     */
    private Long zoneId;
    
    /**
     * ID de la torre (opcional, solo para CIUDADELA).
     */
    private Long towerId;
    
    /**
     * Número mínimo del rango de unidades a crear.
     * Requerido. Debe ser >= 1.
     */
    private Integer min;
    
    /**
     * Número máximo del rango de unidades a crear.
     * Requerido. Debe ser >= min.
     */
    private Integer max;
    
    /**
     * Código prefijo para las unidades.
     * Ejemplo: "B" generará B-1, B-2, etc.
     * Requerido.
     */
    private String code;
    
    /**
     * Tipo de unidad: APARTMENT o HOUSE.
     * Requerido.
     */
    private UnitType type;
    
    /**
     * Si está habilitada la gestión de vehículos para estas unidades.
     * Opcional. Default: false.
     */
    private Boolean vehiclesEnabled;
    
    /**
     * Límite de vehículos por unidad.
     * Solo aplica si vehiclesEnabled = true.
     * Debe ser > 0 si vehiclesEnabled = true.
     */
    private Integer vehicleLimit;
    
    /**
     * Piso de las unidades (solo para APARTMENT).
     * Opcional.
     */
    private Integer floor;
    
    /**
     * Información del propietario a asignar a cada unidad.
     * Opcional. Si se proporciona, se crea/asocia el usuario.
     */
    private OwnerInfo owner;
    
    /**
     * Si se debe enviar la invitación inmediatamente al crear.
     * Solo aplica si se proporciona owner.
     */
    private Boolean sendInvitationImmediately;
    
    /**
     * Genera el código de una unidad específica en el rango.
     * @param number número de la unidad en el rango
     * @return código completo (ej: "B-1")
     */
    public String generateUnitCode(int number) {
        return code + "-" + number;
    }
    
    /**
     * Calcula la cantidad de unidades a crear.
     * @return cantidad de unidades
     */
    public int getUnitCount() {
        if (min == null || max == null) {
            return 0;
        }
        return max - min + 1;
    }
    
    /**
     * Verifica si el rango es válido.
     * @return true si min <= max y ambos son >= 1
     */
    public boolean isValidRange() {
        return min != null && max != null && min >= 1 && max >= min;
    }
    
    /**
     * Verifica si la configuración de vehículos es consistente.
     * @return true si la configuración es válida
     */
    public boolean isValidVehicleConfig() {
        if (Boolean.TRUE.equals(vehiclesEnabled)) {
            return vehicleLimit != null && vehicleLimit > 0;
        } else {
            return vehicleLimit == null || vehicleLimit == 0;
        }
    }
}
