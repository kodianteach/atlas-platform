package co.com.atlas.r2dbc.systemconfiguration;

import co.com.atlas.model.config.gateways.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Implementación del gateway SystemConfigurationRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class SystemConfigurationRepositoryAdapter implements SystemConfigurationRepository {

    private final SystemConfigurationReactiveRepository repository;

    @Override
    public Mono<String> getString(String key) {
        return repository.findByConfigKey(key)
                .map(SystemConfigurationEntity::getConfigValue);
    }

    @Override
    public Mono<Integer> getInteger(String key, Integer defaultValue) {
        return getString(key)
                .map(Integer::parseInt)
                .onErrorReturn(defaultValue)
                .defaultIfEmpty(defaultValue);
    }

    @Override
    public Mono<Boolean> getBoolean(String key, Boolean defaultValue) {
        return getString(key)
                .map(value -> Boolean.parseBoolean(value) || "1".equals(value) || "true".equalsIgnoreCase(value))
                .onErrorReturn(defaultValue)
                .defaultIfEmpty(defaultValue);
    }
}
