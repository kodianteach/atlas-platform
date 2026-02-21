package co.com.atlas.model.unit;

/**
 * Value object que representa una unidad rechazada durante la distribución masiva.
 * Contiene el código de la unidad y el motivo del rechazo.
 *
 * @param code   Código de la unidad rechazada (ej: "A-1")
 * @param reason Motivo del rechazo (ej: "Ya existe")
 */
public record RejectedUnit(String code, String reason) {

    /**
     * Razón estándar para unidades duplicadas.
     */
    public static final String REASON_DUPLICATE = "Ya existe";

    /**
     * Crea una instancia de RejectedUnit por duplicado.
     *
     * @param code Código de la unidad duplicada
     * @return nueva instancia con razón de duplicado
     */
    public static RejectedUnit duplicate(String code) {
        return new RejectedUnit(code, REASON_DUPLICATE);
    }
}
