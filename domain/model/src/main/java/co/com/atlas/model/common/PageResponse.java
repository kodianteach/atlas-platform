package co.com.atlas.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta paginada genérica.
 * Encapsula una página de resultados con metadatos de paginación.
 *
 * @param <T> Tipo de elementos en la página
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    /** Contenido de la página actual. */
    private List<T> content;

    /** Número de página actual (0-based). */
    private int page;

    /** Tamaño de página solicitado. */
    private int size;

    /** Total de elementos en todas las páginas. */
    private long totalElements;

    /** Total de páginas disponibles. */
    private int totalPages;

    /**
     * Crea una PageResponse a partir de los datos y metadatos de paginación.
     *
     * @param content       Lista de elementos de la página
     * @param page          Número de página (0-based)
     * @param size          Tamaño de página
     * @param totalElements Total de elementos
     * @param <T>           Tipo de elemento
     * @return PageResponse construido
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
