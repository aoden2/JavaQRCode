package com.uracer.racer.controller;

import com.uracer.racer.service.BaseService;
import com.uracer.racer.service.QRUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

@Data
@Slf4j
public class MainUIController implements javafx.fxml.Initializable {

    protected static volatile File currentFile;
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
    protected ApplicationContext context;
    @Autowired
    private Environment env;
    @Autowired
    private FTPClient ftpClient;
    @Autowired
    private BaseService baseService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        btnPrint.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

            }
        });

        btnSave.setOnAction(event -> {

            try {
                Image image = QRUtils.createQRCode(txtText.getText(),
                        Integer.parseInt(env.getProperty("width")),
                        Integer.parseInt(env.getProperty("height")));
                baseService.uploadFile(
                        baseService.buildZipFile(
                                baseService.getStreamFromImage(SwingFXUtils.fromFXImage(image, null)),
                                txtText.getText(),
                                txtId.getText()
                        ),
                        txtId.getText()
                );
                imgQR.setImage(image);
            } catch (Exception e) {
                e.printStackTrace();
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
