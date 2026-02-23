package co.com.atlas.r2dbc.qr;

import co.com.atlas.model.authorization.gateways.QrImageGeneratorGateway;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Implementación de generación de imagen QR usando ZXing.
 * Genera imágenes PNG con nivel de corrección de error M.
 */
@Component
@Slf4j
public class ZxingQrImageGeneratorAdapter implements QrImageGeneratorGateway {

    @Override
    public Mono<byte[]> generateQrImage(String content, int width, int height) {
        return Mono.fromCallable(() -> {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            log.debug("QR generado: {}x{} px, {} bytes", width, height, outputStream.size());
            return outputStream.toByteArray();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
