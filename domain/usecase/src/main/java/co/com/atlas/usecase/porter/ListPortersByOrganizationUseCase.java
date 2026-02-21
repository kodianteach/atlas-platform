package co.com.atlas.usecase.porter;

import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.gateways.PorterRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Caso de uso para listar porteros de una organización.
 */
@RequiredArgsConstructor
public class ListPortersByOrganizationUseCase {

    private final PorterRepository porterRepository;

    /**
     * Lista todos los porteros de la organización indicada.
     *
     * @param organizationId ID de la organización
     * @return Flux con los porteros de la organización
     */
    public Flux<Porter> execute(Long organizationId) {
        return porterRepository.findByOrganizationId(organizationId);
    }
}
