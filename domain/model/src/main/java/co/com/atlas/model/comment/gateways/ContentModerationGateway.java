package co.com.atlas.model.comment.gateways;

import reactor.core.publisher.Mono;

/**
 * Gateway para moderación de contenido.
 * Abstrae la lógica de filtrado de contenido inapropiado.
 */
public interface ContentModerationGateway {

    /**
     * Verifica si el contenido es apropiado según las reglas de moderación.
     *
     * @param content texto a evaluar
     * @return Mono<Boolean> true si el contenido es apropiado, false si contiene groserías o contenido inapropiado
     */
    Mono<Boolean> isContentAppropriate(String content);
}
