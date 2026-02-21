package co.com.atlas.model.authorization.gateways;

import reactor.core.publisher.Mono;

/**
 * Gateway para generación de imágenes QR.
 */
public interface QrImageGeneratorGateway {

    /**
     * Genera una imagen QR desde un contenido de texto.
     *
     * @param content Contenido a codificar en el QR
     * @param width   Ancho de la imagen en píxeles
     * @param height  Alto de la imagen en píxeles
     * @return Mono con los bytes de la imagen PNG
     */
    Mono<byte[]> generateQrImage(String content, int width, int height);
}
