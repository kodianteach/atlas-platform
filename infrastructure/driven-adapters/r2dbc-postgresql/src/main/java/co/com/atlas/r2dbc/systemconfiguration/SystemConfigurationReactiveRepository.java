package co.com.atlas.r2dbc.systemconfiguration;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo para SystemConfigurationEntity.
 */
public interface SystemConfigurationReactiveRepository extends ReactiveCrudRepository<SystemConfigurationEntity, Long> {
    
    Mono<SystemConfigurationEntity> findByConfigKey(String configKey);
}
