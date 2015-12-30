package com.uracer.racer.service;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Khoi Le
 */
@Component
public class BaseService {

    public static volatile String TEMP_DIR = "temp";
    private static final String PATH_IMAGE = TEMP_DIR + "/data/image.png";
    private static final String PATH_QR_IMAGE = TEMP_DIR + "/qr.png";
    public static final String PATH_HTML = TEMP_DIR + "/print.html";
    @Autowired
    private Environment env;
    @Autowired
    private FTPClient ftpClient;

    public void renderWebPage(Image image, Image qr, String text, String id) throws IOException, ZipException {

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();
        URL url = getClass().getClassLoader().getResource("template.vm");
        Template template = velocityEngine.getTemplate(url.getPath());
        template.initDocument();

        buildZipFile(getStreamFromImage(image), text, id);
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(PATH_HTML)));
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("id", id);
        valueMap.put("text", text);
        valueMap.put("image", saveImagetoLocal(image, PATH_IMAGE));
        valueMap.put("qr", saveImagetoLocal(qr, PATH_QR_IMAGE));
        VelocityContext velocityContext = new VelocityContext(valueMap);
        template.merge(velocityContext, writer);
        writer.flush();
        writer.close();
    }

    private String saveImagetoLocal(Image image, String path) throws IOException {

        InputStream is = getStreamFromImage(image);
        FileUtils.writeByteArrayToFile(new File(path), IOUtils.toByteArray(is));
        return path;
    }

    public InputStream buildZipFile(InputStream data, String text, String id) throws IOException, ZipException {

        TEMP_DIR = System.getenv("TEMP_DIR") != null ? System.getenv("TEMP_DIR") : TEMP_DIR;
        FileUtils.writeByteArrayToFile(new File(PATH_IMAGE), IOUtils.toByteArray(data));
        FileUtils.writeStringToFile(new File(TEMP_DIR + "/data/text.txt"), text);
        String zippedFilePath = TEMP_DIR + "/" + id + ".zip";
        ZipFile zipFile = new ZipFile(zippedFilePath);
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        zipParameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        zipFile.addFolder(TEMP_DIR + "/data", zipParameters);
        return zipFile.getInputStream(zipFile.getFileHeader(zippedFilePath));
    }

    public InputStream getStreamFromImage(Image image) throws IOException {

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };
        ImageIO.write(bufferedImage, "png", output);
        return new ByteArrayInputStream(output.toByteArray(), 0, output.size());
    }

    public InputStream getStreamFromImage(BufferedImage image) throws IOException {

        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };
        ImageIO.write(image, "png", output);
        return new ByteArrayInputStream(output.toByteArray(), 0, output.size());
    }

    public String readQRCodeFromCamera() {
        Webcam webcam = Webcam.getDefault(); // non-default (e.g. USB) webcam can be used too
        webcam.open();

        Result result = null;
        BufferedImage image = null;

        while (webcam.isOpen()) {
            if ((image = webcam.getImage()) == null) {
                continue;
            }
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                result = new MultiFormatReader().decode(bitmap);
                if (result != null) {
                    return result.getText();
                }
            } catch (NotFoundException e) {
                // it means there is no QR code in image
                e.printStackTrace();
            }
        }
        return "";
    }

    public synchronized OutputStream downloadFile(String fileName) throws Exception {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        executeFTPTask(targets -> ftpClient.retrieveFile(fileName, outputStream));
        return outputStream;
    }

    public synchronized void uploadFile(InputStream fileData, String fileName) throws Exception {

        executeFTPTask(targets -> {
            if (!ArrayUtils.isEmpty(targets)) {
                InputStream is = (InputStream) targets[0];
                ftpClient.storeFile(fileName, is);
            } else {
                ftpClient.storeFile(fileName, fileData);
            }
        });
    }

    protected void executeFTPTask(TaskExecutor taskExecutor) throws Exception {

        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
        ftpClient.connect(env.getProperty("ftp.url"));
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())
                && ftpClient.login(env.getProperty("ftp.username"), env.getProperty("ftp.password"))) {
            taskExecutor.execute();
        }

    }
}
