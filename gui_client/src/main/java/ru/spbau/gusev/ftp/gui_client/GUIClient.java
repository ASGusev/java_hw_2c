package ru.spbau.gusev.ftp.gui_client;

import javafx.application.Application;
import javafx.stage.Stage;

public class GUIClient extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        ConnectingWindow window = new ConnectingWindow(primaryStage);
        window.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
