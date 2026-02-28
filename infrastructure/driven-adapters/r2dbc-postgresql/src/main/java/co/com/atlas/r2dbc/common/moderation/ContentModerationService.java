package co.com.atlas.r2dbc.common.moderation;

import co.com.atlas.model.comment.gateways.ContentModerationGateway;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Implementación de moderación de contenido basada en lista de palabras prohibidas.
 * Detecta groserías y contenido inapropiado mediante coincidencia normalizada.
 * Extensible a futuro con ML o APIs externas.
 */
@Component
public class ContentModerationService implements ContentModerationGateway {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    public Mono<Boolean> isContentAppropriate(String content) {
        if (content == null || content.isBlank()) {
            return Mono.just(true);
        }

        String normalized = normalizeText(content);

        boolean hasProfanity = ProfanityWordList.FORBIDDEN_WORDS.stream()
                .map(this::normalizeText)
                .anyMatch(normalized::contains);

        return Mono.just(!hasProfanity);
    }

    /**
     * Normaliza texto: minúsculas, sin acentos, sin caracteres especiales repetidos.
     */
    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return normalized;
    }
}
