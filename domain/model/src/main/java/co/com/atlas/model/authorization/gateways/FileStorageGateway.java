package co.com.atlas.model.authorization.gateways;

import reactor.core.publisher.Mono;

/**
 * Gateway de almacenamiento de archivos.
 * Abstracción para almacenar documentos asociados a autorizaciones.
 * Diseñado para swap a S3 sin cambios en dominio.
 */
public interface FileStorageGateway {

    /**
     * Almacena un archivo.
     *
     * @param key         Clave única del archivo (ruta relativa)
     * @param content     Contenido del archivo en bytes
     * @param contentType Tipo MIME del archivo
     * @return Mono con la clave del archivo almacenado
     */
    Mono<String> store(String key, byte[] content, String contentType);

    /**
     * Recupera un archivo por su clave.
     *
     * @param key Clave del archivo
     * @return Mono con el contenido del archivo en bytes
     */
    Mono<byte[]> retrieve(String key);

    /**
     * Elimina un archivo por su clave.
     *
     * @param key Clave del archivo
     * @return Mono vacío al completar
     */
    Mono<Void> delete(String key);
}
