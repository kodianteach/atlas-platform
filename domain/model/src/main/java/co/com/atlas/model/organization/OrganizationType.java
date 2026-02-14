package co.com.atlas.model.organization;

/**
 * Tipos de organización soportados por el sistema.
 */
public enum OrganizationType {
    /**
     * Ciudadela: Tiene zonas → torres → apartamentos/casas.
     * Puede configurarse para permitir APARTMENT, HOUSE o ambos.
     */
    CIUDADELA,
    
    /**
     * Conjunto: Tiene zonas (opcional) → apartamentos/casas.
     * Puede configurarse para permitir APARTMENT, HOUSE o ambos.
     */
    CONJUNTO,
    
    /**
     * Condominio: Tiene zonas → casas únicamente.
     * Solo permite HOUSE como tipo de unidad.
     */
    CONDOMINIO
}
