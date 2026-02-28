package co.com.atlas.r2dbc.common.moderation;

import java.util.Set;

/**
 * Lista configurable de palabras prohibidas para moderación de contenido.
 * Se puede extender a futuro con carga desde BD o archivo externo.
 */
public final class ProfanityWordList {

    private ProfanityWordList() {
        // Utility class
    }

    /**
     * Conjunto de palabras prohibidas en español.
     * Incluye groserías, insultos y contenido inapropiado.
     */
    public static final Set<String> FORBIDDEN_WORDS = Set.of(
            "idiota", "estúpido", "estupido", "imbécil", "imbecil",
            "mierda", "puta", "puto", "pendejo", "pendeja",
            "marica", "maricón", "maricon", "hijueputa", "malparido",
            "malparida", "gonorrea", "carepicha", "carechimba",
            "hp", "hdp", "hpta", "ctm", "ptm",
            "basura", "cerdo", "cerda", "asqueroso", "asquerosa",
            "maldito", "maldita", "desgraciado", "desgraciada",
            "tarado", "tarada", "retrasado", "retrasada",
            "inútil", "inutil", "bruto", "bruta"
    );
}
