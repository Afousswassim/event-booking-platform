package com.eventbooking.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class QrCodeService {
    private QrCodeService() {}

    public static void writePng(String code, OutputStream output) throws IOException {
        try {
            var matrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 256, 256,
                    Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name()));
            var image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xff000000 : 0xffffffff);
                }
            }
            if (!ImageIO.write(image, "png", output)) throw new IOException("PNG encoder unavailable");
        } catch (WriterException failure) {
            throw new IOException("Could not encode the ticket QR", failure);
        }
    }
}
