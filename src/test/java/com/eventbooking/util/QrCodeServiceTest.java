package com.eventbooking.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrCodeServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"c0530c44-3f13-46aa-a4c7-49fcc51ba2bc", "EVT-Événement-2026 / 001"})
    void generatedPngDecodesToTheExactTicketCode(String code) throws Exception {
        var output = new ByteArrayOutputStream();
        QrCodeService.writePng(code, output);
        var image = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
        int width = image.getWidth();
        int height = image.getHeight();
        var pixels = image.getRGB(0, 0, width, height, null, 0, width);
        var bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));

        assertEquals(code, new QRCodeReader().decode(bitmap).getText());
    }
}
