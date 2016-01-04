package com.uracer.racer;

import com.uracer.racer.config.SpringBeansConfig;
import com.uracer.racer.controller.MainUIController;
import com.uracer.racer.service.BaseService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;

public class Application extends javafx.application.Application {

    private ApplicationContext context;
    @Autowired
    private BaseService baseService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        context = new AnnotationConfigApplicationContext(SpringBeansConfig.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setOnCloseRequest(event -> {
            try {
                FileUtils.cleanDirectory(new File(BaseService.TEMP_DIR));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.exit(0);
            }
        });
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/UracerUI.fxml"));
        Parent parent = fxmlLoader.load();
        MainUIController controller = fxmlLoader.getController();
        controller.setHostServices(getHostServices());
        context.getAutowireCapableBeanFactory().autowireBean(controller);
        Scene scene = new Scene(parent);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    }
