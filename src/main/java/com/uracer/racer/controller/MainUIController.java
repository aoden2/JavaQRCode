package com.uracer.racer.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.print.attribute.standard.Media;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

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

                        BufferedImage img = ImageIO.read(currentFile);
                        imgPhoto.setImage(SwingFXUtils.toFXImage(img, new WritableImage(200, 200)));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
