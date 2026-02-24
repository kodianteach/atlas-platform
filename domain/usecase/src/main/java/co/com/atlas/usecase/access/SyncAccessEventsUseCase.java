package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

/**
 * Caso de uso para sincronización de eventos de acceso offline.
 * Recibe un lote de eventos generados offline y los persiste en batch.
 */
@RequiredArgsConstructor
public class SyncAccessEventsUseCase {

    private final AccessEventRepository accessEventRepository;

    /**
     * Sincroniza un lote de eventos offline al backend.
     *
     * @param events Lista de eventos de acceso generados offline
     * @return Eventos persistidos con IDs asignados
     */
    public Flux<AccessEvent> execute(List<AccessEvent> events) {
        if (events == null || events.isEmpty()) {
            return Flux.empty();
        }

        Instant now = Instant.now();
        List<AccessEvent> enrichedEvents = events.stream()
                .map(event -> event.toBuilder()
                        .syncedAt(now)
                        .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : now)
                        .build())
                .toList();

        return accessEventRepository.saveBatch(enrichedEvents);
    }
}
