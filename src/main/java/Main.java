import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main extends Application {
    private boolean[] creating = {false};
    int port = 4000;

    @Override
    public void start(Stage primaryStage) throws Exception {
        GridPane pane = new GridPane();

        Button createButton = new Button("Create game");
        createButton.setOnAction(event -> {
            try {
                if (!creating[0]) {
                    creating[0] = true;
                    new Thread(new GameCreator(primaryStage)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        pane.add(createButton, 1, 1);

        TextField ipField = new TextField();
        Button connectButton = new Button("Connect");
        connectButton.setOnAction(event -> {
            String ip = ipField.getText();
            try {
                Socket socket = new Socket(ip, port);
                Game game = new Game(socket, 'O');
                new GameWindow(game, primaryStage).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        pane.add(ipField, 1, 2);
        pane.add(connectButton, 1, 3);

        Scene scene = new Scene(pane, 300, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private class GameCreator implements Runnable {
        private ServerSocket socket;
        private final Stage stage;

        private GameCreator(Stage stage) throws IOException {
            socket = new ServerSocket(port);
            this.stage = stage;
        }

        @Override
        public void run() {
            try {
                final Socket opponentSocket = socket.accept();
                Platform.runLater(() -> {
                    Game game = null;
                    try {
                        game = new Game(opponentSocket, 'X');
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    GameWindow gameWindow = new GameWindow(game, stage);
                    gameWindow.show();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
