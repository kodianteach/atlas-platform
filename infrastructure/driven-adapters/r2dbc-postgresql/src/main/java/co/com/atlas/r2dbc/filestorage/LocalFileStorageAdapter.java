package co.com.atlas.r2dbc.filestorage;

import co.com.atlas.model.authorization.gateways.FileStorageGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Implementación local del almacenamiento de archivos.
 * Almacena archivos en el sistema de archivos local.
 * Diseñado para swap a S3 sin cambios en dominio/use-cases.
 */
@Component
@Slf4j
public class LocalFileStorageAdapter implements FileStorageGateway {

    private final String basePath;

    public LocalFileStorageAdapter(
            @Value("${atlas.storage.base-path:/tmp/atlas/uploads}") String basePath) {
        this.basePath = basePath;
    }

    @Override
    public Mono<String> store(String key, byte[] content, String contentType) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(basePath, key);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Archivo almacenado: {}", key);
            return key;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<byte[]> retrieve(String key) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(basePath, key);
            if (!Files.exists(filePath)) {
                throw new IOException("Archivo no encontrado: " + key);
            }
            return Files.readAllBytes(filePath);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(String key) {
        return Mono.fromRunnable(() -> {
            try {
                Path filePath = Paths.get(basePath, key);
                Files.deleteIfExists(filePath);
                log.info("Archivo eliminado: {}", key);
            } catch (IOException e) {
                log.warn("Error eliminando archivo {}: {}", key, e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
