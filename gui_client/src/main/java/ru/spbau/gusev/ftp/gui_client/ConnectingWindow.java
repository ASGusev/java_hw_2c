package ru.spbau.gusev.ftp.gui_client;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import ru.spbau.gusev.ftp.client.FTPClient;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A window that allows user to connect ot a server.
 */
public class ConnectingWindow {
    private final static String CONNECT_TITLE = "GUI client - connect";
    private final static int CONNECTION_WINDOW_HEIGHT = 240;
    private final static int CONNECTION_WINDOW_WIDTH = 320;
    private static final String ERROR_MESSAGE = "Connection error.";
    private static final String RETURN_BUTTON_TEXT = "Back";
    private Stage window;
    private final TextField serverField = new TextField();
    private final TextField portField = new TextField();
    private Scene connectScene;
    private Scene errorScene;

    /**
     * Creates an instance in the given stage.
     * @param window the window to use.
     */
    public ConnectingWindow(@Nonnull Stage window) {
        this.window = window;
        makeConnectScene();
        makeErrorScene();
        window.setTitle(CONNECT_TITLE);
        window.setScene(connectScene);
    }

    /**
     * Shows the window on the screen.
     */
    public void show() {
        window.show();
    }

    private void makeConnectScene() {
        int SERVER_PROMPT_ROW_INDEX = 0;
        int SERVER_FIELD_ROW_INDEX = SERVER_PROMPT_ROW_INDEX + 1;
        int PORT_PROMPT_ROW_INDEX = SERVER_FIELD_ROW_INDEX + 1;
        int PORT_FIELD_ROW_INDEX = PORT_PROMPT_ROW_INDEX + 1;
        int CONNECT_BUTTON_ROW_INDEX = PORT_FIELD_ROW_INDEX + 1;

        GridPane connectionGrid = new GridPane();
        connectionGrid.setAlignment(Pos.CENTER);
        connectionGrid.setVgap(10);

        Text serverPrompt = new Text("Server address:");
        Text portPrompt = new Text("Port:");
        Button connectionButton = new Button("Connect");
        connectionButton.setOnAction(onConnectClick);

        connectionGrid.add(serverPrompt, 0, SERVER_PROMPT_ROW_INDEX);
        connectionGrid.add(serverField, 0, SERVER_FIELD_ROW_INDEX, 2, 1);
        connectionGrid.add(portPrompt, 0, PORT_PROMPT_ROW_INDEX);
        connectionGrid.add(portField, 0, PORT_FIELD_ROW_INDEX, 2, 1);
        connectionGrid.add(connectionButton, 1, CONNECT_BUTTON_ROW_INDEX);

        connectScene = new Scene(connectionGrid, CONNECTION_WINDOW_WIDTH,
                CONNECTION_WINDOW_HEIGHT);
    }

    private void makeErrorScene() {
        GridPane errorGrid = new GridPane();
        errorGrid.setAlignment(Pos.CENTER);
        errorGrid.setHgap(10);

        Text messageTest = new Text(ERROR_MESSAGE);
        messageTest.setFont(Font.font(16));
        errorGrid.add(messageTest, 0, 0);

        Button backButton = new Button(RETURN_BUTTON_TEXT);
        backButton.setOnAction(event1 -> window.setScene(connectScene));
        errorGrid.add(backButton, 0, 1);

        errorScene = new Scene(errorGrid, CONNECTION_WINDOW_WIDTH,
                CONNECTION_WINDOW_HEIGHT);
    }

    private final EventHandler<ActionEvent> onConnectClick = event -> {
        try {
            FTPClient client = new FTPClient();
            String address = serverField.getText();
            int port = Integer.valueOf(portField.getText());
            client.connect(address, port);
            BrowsingWindow browsingWindow = new BrowsingWindow(client);
            browsingWindow.show();
            window.hide();
        } catch (IOException | NumberFormatException e) {
            window.setScene(errorScene);
        }
    };
}
