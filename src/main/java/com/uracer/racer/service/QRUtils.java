package com.uracer.racer.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.CharSet;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author khoi
 *         Simple QR code util compatibility with JavaFX
 */
public class QRUtils {

    public static String readQRCode(Image image, String charset, Map hintMap)
            throws Exception {
        if (image == null) {
            return null;
        }
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(SwingFXUtils.fromFXImage(image, null))));
        Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap, hintMap);
        return Charset.forName(charset).encode(qrCodeResult.getText()).toString();
    }

    public static Image createQRCode(String text, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix byteMatrix = null;
        try {
            byteMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        int matrixWidth = byteMatrix.getWidth();

        BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, matrixWidth, matrixWidth);
        // Paint and save the image using the ByteMatrix
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < matrixWidth; i++) {
            for (int j = 0; j < matrixWidth; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        return SwingFXUtils.toFXImage(image, null);
    }
}
