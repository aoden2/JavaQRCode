package com.uracer.racer.controller;

import com.uracer.racer.service.BaseService;
import com.uracer.racer.service.QRUtils;
import com.uracer.racer.service.Subscriber;
import javafx.application.HostServices;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

@Data
@Slf4j
public class MainUIController implements javafx.fxml.Initializable, Subscriber {

    protected static volatile File currentFile;
    protected HostServices hostServices;
    @FXML
    protected Button btnOpen;
    @FXML
    protected Button btnSave;
    @FXML
    protected Button btnPrint;
    @FXML
    protected TextField txtId;
    @FXML
    protected TextField txtText;
    @FXML
    protected ImageView imgPhoto;
    @FXML
    protected ImageView imgQR;
    @FXML
    protected ImageView imgWebcam;
    @FXML
    protected Label lbStatus;

    @Autowired
    private Environment env;
    @Autowired
    private FTPClient ftpClient;
    @Autowired
    private BaseService baseService;
    private String currentId;

    @Override
    public void doWhenReceivedUpdates(Object... messages) {
        try {
            this.currentId = (String) messages[0];
            this.imgQR.setImage((Image) messages[1]);
            String zipPath = BaseService.TEMP_DIR + "/" + this.currentId + ".zip";
            OutputStream os = new DataOutputStream(new FileOutputStream(zipPath, false));
            baseService.downloadFile(currentId + ".zip", os);
            os.close();
            ZipFile zipFile = new ZipFile(zipPath);
            zipFile.extractAll(BaseService.TEMP_DIR);

            this.imgPhoto.setImage(SwingFXUtils.toFXImage(ImageIO.read(new File(BaseService.PATH_IMAGE)), null));

            txtId.setText(currentId);
            txtText.setText(FileUtils.readFileToString(new File(BaseService.TXT_PATH)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        File temp = new File(BaseService.TEMP_DIR);
        if (!temp.exists() || temp.isFile()) {
            temp.mkdir();
        }
        new Thread(() -> {

            try {
                BaseService.readQRCodeFromCamera(imgWebcam, this);
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        btnPrint.setOnAction(event -> {

            try {
                lbStatus.setText("Processing...");
                Image image = QRUtils.createQRCode(txtId.getText(),
                        Integer.parseInt(env.getProperty("width")),
                        Integer.parseInt(env.getProperty("height")));
                baseService.saveImageToLocal(image, BaseService.PATH_QR_IMAGE);
                imgQR.setImage(image);
                URL url = new File(BaseService.PATH_HTML).toURI().toURL();
                baseService.renderWebPage(imgPhoto.getImage(), imgQR.getImage(), txtText.getText(), txtId.getText());
                hostServices.showDocument(url.toString());
                lbStatus.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        btnSave.setOnAction(event -> {

            InputStream zipFile = null;
            try {
                lbStatus.setText("Saving...");
                Image image = QRUtils.createQRCode(txtId.getText(),
                        Integer.parseInt(env.getProperty("width")),
                        Integer.parseInt(env.getProperty("height")));
                Image uploadedImage = imgPhoto.getImage();
                baseService.saveImageToLocal(image, BaseService.PATH_QR_IMAGE);
                zipFile = baseService.buildZipFile(
                        baseService.getStreamFromImage(SwingFXUtils.fromFXImage(uploadedImage, null)),
                        txtText.getText(),
                        txtId.getText()
                );
                baseService.uploadFile(
                        zipFile,
                        txtId.getText() + ".zip"
                );
                imgQR.setImage(image);
                lbStatus.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                        FileUtils.cleanDirectory(new File(BaseService.TEMP_DIR));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnOpen.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose you photo");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png")
                    , new FileChooser.ExtensionFilter("BMP", "*.bmp")
                    , new FileChooser.ExtensionFilter("JPEG files (*.jpg)", "*.jpg"));
            Button btn = (Button) event.getSource();
            currentFile = fileChooser.showOpenDialog(btn.getScene().getWindow());

            if (currentFile != null) {

                try {
                    if (Files.probeContentType(currentFile.toPath()).equals("image/jpeg")
                            || Files.probeContentType(currentFile.toPath()).equals("image/png")
                            || Files.probeContentType(currentFile.toPath()).equals("image/bmp")) {

                        showImage(currentFile, imgPhoto);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showImage(File currentFile, ImageView imgPhoto) throws IOException {
        BufferedImage img = ImageIO.read(currentFile);
        imgPhoto.setImage(SwingFXUtils.toFXImage(img, new WritableImage(200, 200)));
    }
}
