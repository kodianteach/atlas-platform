package co.com.atlas.model.unit.validation;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.unit.UnitDistribution;
import co.com.atlas.model.unit.UnitType;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de distribución de unidades.
 * Aplica reglas de negocio para creación masiva de unidades.
 * 
 * Usa System.Logger en lugar de @Slf4j (regla de dominio).
 */
public final class UnitDistributionValidator {
    
    private static final System.Logger LOGGER = System.getLogger(UnitDistributionValidator.class.getName());
    
    private UnitDistributionValidator() {
        // Utility class
    }
    
    /**
     * Valida que organizationId sea obligatorio y válido.
     * 
     * @param distribution distribución a validar
     * @throws BusinessException si organizationId es inválido
     */
    public static void validateOrganizationId(UnitDistribution distribution) {
        if (distribution.getOrganizationId() == null || distribution.getOrganizationId() <= 0) {
            throw new BusinessException(
                "El ID de la organización es obligatorio y debe ser mayor a 0",
                "ORGANIZATION_ID_REQUIRED"
            );
        }
    }
    
    /**
     * Valida que el rango min-max sea correcto.
     * 
     * @param distribution distribución a validar
     * @throws BusinessException si el rango es inválido
     */
    public static void validateRange(UnitDistribution distribution) {
        if (distribution.getMin() == null) {
            throw new BusinessException("El valor mínimo (min) es obligatorio", "MIN_REQUIRED");
        }
        
        if (distribution.getMax() == null) {
            throw new BusinessException("El valor máximo (max) es obligatorio", "MAX_REQUIRED");
        }
        
        if (distribution.getMin() < 1) {
            throw new BusinessException(
                "El valor mínimo debe ser mayor o igual a 1",
                "MIN_INVALID"
            );
        }
        
        if (distribution.getMax() < distribution.getMin()) {
            throw new BusinessException(
                "El valor máximo (" + distribution.getMax() + 
                ") debe ser mayor o igual al mínimo (" + distribution.getMin() + ")",
                "RANGE_INVALID"
            );
        }
        
        int count = distribution.getUnitCount();
        LOGGER.log(System.Logger.Level.DEBUG, 
            "Rango válido: min={0}, max={1}, unidades a crear={2}", 
            distribution.getMin(), distribution.getMax(), count);
    }
    
    /**
     * Valida que el código prefijo sea válido.
     * 
     * @param distribution distribución a validar
     * @throws BusinessException si el código es inválido
     */
    public static void validateCode(UnitDistribution distribution) {
        if (distribution.getCode() == null || distribution.getCode().isBlank()) {
            throw new BusinessException(
                "El código de prefijo es obligatorio",
                "CODE_REQUIRED"
            );
        }
        
        String code = distribution.getCode().trim();
        if (code.length() > 10) {
            throw new BusinessException(
                "El código de prefijo no puede exceder 10 caracteres",
                "CODE_TOO_LONG"
            );
        }
        
        // Solo alfanumérico y guiones
        if (!code.matches("^[A-Za-z0-9\\-]+$")) {
            throw new BusinessException(
                "El código de prefijo solo puede contener letras, números y guiones",
                "CODE_INVALID_FORMAT"
            );
        }
    }
    
    /**
     * Valida que el tipo de unidad sea válido.
     * 
     * @param distribution distribución a validar
     * @throws BusinessException si el tipo es inválido
     */
    public static void validateUnitType(UnitDistribution distribution) {
        if (distribution.getType() == null) {
            throw new BusinessException(
                "El tipo de unidad es obligatorio. Valores permitidos: APARTMENT, HOUSE",
                "TYPE_REQUIRED"
            );
        }
        
        // UnitType ya es un enum, pero validamos explícitamente
        if (distribution.getType() != UnitType.APARTMENT && distribution.getType() != UnitType.HOUSE) {
            throw new BusinessException(
                "Tipo de unidad no válido. Valores permitidos: APARTMENT, HOUSE",
                "TYPE_INVALID"
            );
        }
    }
    
    /**
     * Valida la configuración de vehículos.
     * 
     * Reglas:
     * - Si vehiclesEnabled = false → vehicleLimit debe ser null o 0
     * - Si vehiclesEnabled = true → vehicleLimit > 0
     * - No se permiten valores negativos
     * 
     * @param distribution distribución a validar
     * @throws BusinessException si la configuración es inconsistente
     */
    public static void validateVehicleConfiguration(UnitDistribution distribution) {
        Boolean vehiclesEnabled = distribution.getVehiclesEnabled();
        Integer vehicleLimit = distribution.getVehicleLimit();
        
        // Validar que no haya valores negativos
        if (vehicleLimit != null && vehicleLimit < 0) {
            throw new BusinessException(
                "El límite de vehículos no puede ser negativo",
                "VEHICLE_LIMIT_NEGATIVE"
            );
        }
        
        // Si vehículos habilitados, el límite debe ser mayor a 0
        if (Boolean.TRUE.equals(vehiclesEnabled)) {
            if (vehicleLimit == null || vehicleLimit <= 0) {
                throw new BusinessException(
                    "Si vehiclesEnabled es true, vehicleLimit debe ser mayor a 0",
                    "VEHICLE_LIMIT_REQUIRED_WHEN_ENABLED"
                );
            }
            LOGGER.log(System.Logger.Level.DEBUG, 
                "Configuración de vehículos: habilitado=true, límite={0}", vehicleLimit);
        }
        
        // Si vehículos deshabilitados, el límite debe ser null o 0
        if (Boolean.FALSE.equals(vehiclesEnabled) || vehiclesEnabled == null) {
            if (vehicleLimit != null && vehicleLimit > 0) {
                throw new BusinessException(
                    "Si vehiclesEnabled es false, vehicleLimit debe ser null o 0",
                    "VEHICLE_LIMIT_NOT_ALLOWED_WHEN_DISABLED"
                );
            }
        }
    }
    
    /**
     * Valida la información del propietario si está presente.
     * 
     * @param distribution distribución a validar
     * @throws BusinessException si la información del propietario es incompleta
     */
    public static void validateOwnerInfo(UnitDistribution distribution) {
        if (distribution.getOwner() == null) {
            return; // Owner es opcional
        }
        
        var owner = distribution.getOwner();
        List<String> errors = new ArrayList<>();
        
        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            errors.add("email del propietario");
        } else if (!isValidEmail(owner.getEmail())) {
            throw new BusinessException(
                "El email del propietario no tiene un formato válido",
                "OWNER_EMAIL_INVALID"
            );
        }
        
        if (owner.getDocumentType() == null) {
            errors.add("tipo de documento del propietario");
        }
        
        if (owner.getDocumentNumber() == null || owner.getDocumentNumber().isBlank()) {
            errors.add("número de documento del propietario");
        }
        
        if (!errors.isEmpty()) {
            throw new BusinessException(
                "Información incompleta del propietario. Campos requeridos: " + String.join(", ", errors),
                "OWNER_INFO_INCOMPLETE"
            );
        }
    }
    
    /**
     * Valida la consistencia de sendInvitationImmediately con owner.
     * 
     * @param distribution distribución a validar
     */
    public static void validateInvitationConfiguration(UnitDistribution distribution) {
        if (Boolean.TRUE.equals(distribution.getSendInvitationImmediately()) 
                && distribution.getOwner() == null) {
            LOGGER.log(System.Logger.Level.WARNING, 
                "sendInvitationImmediately=true pero no hay owner definido");
            throw new BusinessException(
                "No se puede enviar invitación sin información del propietario",
                "INVITATION_REQUIRES_OWNER"
            );
        }
    }
    
    /**
     * Ejecuta validación completa de la distribución.
     * 
     * @param distribution distribución a validar
     * @throws BusinessException si hay errores de validación
     */
    public static void validateComplete(UnitDistribution distribution) {
        if (distribution == null) {
            throw new BusinessException("La distribución no puede ser nula", "DISTRIBUTION_NULL");
        }
        
        LOGGER.log(System.Logger.Level.INFO, "Iniciando validación de distribución de unidades");
        
        validateOrganizationId(distribution);
        validateRange(distribution);
        validateCode(distribution);
        validateUnitType(distribution);
        validateVehicleConfiguration(distribution);
        validateOwnerInfo(distribution);
        validateInvitationConfiguration(distribution);
        
        LOGGER.log(System.Logger.Level.INFO, 
            "Validación exitosa: código={0}, rango={1}-{2}, unidades={3}", 
            distribution.getCode(), 
            distribution.getMin(), 
            distribution.getMax(),
            distribution.getUnitCount());
    }
    
    /**
     * Validación básica de formato de email.
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        // Regex simplificado para email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
