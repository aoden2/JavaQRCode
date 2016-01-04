package com.uracer.racer.service;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Khoi Le
 */
@Component
public class BaseService {

    public static volatile String TEMP_DIR = System.getenv("TEMP_DIR") != null ? System.getenv("TEMP_DIR") : "temp";
    public static final String TXT_PATH = TEMP_DIR + "/data/text.txt";
    public static final String PATH_IMAGE = TEMP_DIR + "/data/image.png";
    public static final String PATH_QR_IMAGE = TEMP_DIR + "/qr.png";
    public static final String PATH_HTML = TEMP_DIR + "/print.html";
    private static Webcam webcam = Webcam.getDefault();
    @Autowired
    private Environment env;
    @Autowired
    private FTPClient ftpClient;
    ;

    public static void readQRCodeFromCamera(ImageView imageView, Subscriber subscriber) {

        if (webcam == null) {
            webcam = Webcam.getDefault();
        }
        if (webcam != null && !webcam.isOpen()) {

            webcam.open();
        }
        Result result = null;
        BufferedImage image = null;

        while (webcam.isOpen()) {

            if ((image = webcam.getImage()) == null) {
                continue;
            }
            if (imageView != null) {

                imageView.setImage(SwingFXUtils.toFXImage(image, null));
            }
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                result = new MultiFormatReader().decode(bitmap);
                if (result != null) {
                    Image qr = QRUtils.createQRCode(result.getText(), 200, 200);
                    subscriber.doWhenReceivedUpdates(new Object[]{result.getText(), qr});
                }
            } catch (Exception e) {
                // it means there is no QR code in image
//                e.printStackTrace();
            }
        }
    }

    public void renderWebPage(Image image, Image qr, String text, String id) throws IOException, ZipException, URISyntaxException {

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();

        Template template = velocityEngine.getTemplate("template.vm");
        template.initDocument();

        InputStream is = buildZipFile(getStreamFromImage(image), text, id);
        is.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(PATH_HTML)));
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("id", id);
        valueMap.put("text", text);
        valueMap.put("image", saveImageToLocal(image, "data/image.png"));
        valueMap.put("qr", saveImageToLocal(qr, "qr.png"));
        VelocityContext velocityContext = new VelocityContext(valueMap);
        template.merge(velocityContext, writer);
        writer.flush();
        writer.close();
    }

    public String saveImageToLocal(Image image, String path) throws IOException {

        InputStream is = getStreamFromImage(image);
        FileUtils.writeByteArrayToFile(new File(path), IOUtils.toByteArray(is));
        return path;
    }

    public InputStream buildZipFile(InputStream data, String text, String id) throws IOException, ZipException {

        File file = new File(TEMP_DIR);
        if (!file.exists() || file.isFile()) {
            file.mkdir();
        }
        FileUtils.writeByteArrayToFile(new File(PATH_IMAGE), IOUtils.toByteArray(data));
        FileUtils.writeStringToFile(new File(TXT_PATH), text);
        String zippedFilePath = TEMP_DIR + "/" + id + ".zip";
        ZipFile zipFile = new ZipFile(zippedFilePath);
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        zipParameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        zipFile.addFolder(TEMP_DIR + "/data", zipParameters);
        return new FileInputStream(zipFile.getFile());
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

    public synchronized Boolean downloadFile(String fileName, OutputStream os) throws Exception {
        return (Boolean) executeFTPTask(targets -> ftpClient.retrieveFile(fileName, os));
    }

    public synchronized void uploadFile(InputStream fileData, String fileName) throws Exception {

        executeFTPTask(targets -> {
            if (!ArrayUtils.isEmpty(targets)) {
                InputStream is = (InputStream) targets[0];
                ftpClient.storeFile(fileName, is);
            } else {
                ftpClient.storeFile(fileName, fileData);
            }
            return null;
        });
    }

    protected Object executeFTPTask(TaskExecutor taskExecutor) throws Exception {

        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
        ftpClient.connect(env.getProperty("ftp.url"));
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())
                && ftpClient.login(env.getProperty("ftp.username"), env.getProperty("ftp.password"))) {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return taskExecutor.execute();
        }
        return null;
    }
}
