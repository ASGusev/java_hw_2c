package ru.spbau.test2;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    private final static int FIELD_SIZE = 4;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        new GameWindow(primaryStage, new Game(FIELD_SIZE)).show();
    }
}
