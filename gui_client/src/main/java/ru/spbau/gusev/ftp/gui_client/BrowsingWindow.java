package ru.spbau.gusev.ftp.gui_client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import ru.spbau.gusev.ftp.client.FTPClient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Stack;

/**
 * A window with an interface for browsing a server.
 */
public class BrowsingWindow {
    private final static int BROWSING_WINDOW_HEIGHT = 640;
    private final static int BROWSING_WINDOW_WIDTH = 360;
    private final static String BROWSING_WINDOW_TITLE = "GUI Client - browse %s:%d";
    private final static String PARENT_FOLDER = "..";
    private static final String ROOT_PATH = "";
    private static final String GET_ERROR_TITLE = "Error";
    private static final String GET_ERROR_MESSAGE = "Impossible to download file.";
    private static final String LIST_ERROR_TITLE = "Error";
    private static final String LIST_ERROR_MESSAGE =
            "Impossible to load folder content.";
    private static final String DOWNLOAD_FINISH_TITLE = "Download finished.";
    private static final String DOWNLOAD_FINISH_MESSAGE =
            "File %s successfully downloaded.";

    private final Stage window;
    private Scene browsingScene;
    private FTPClient browsingClient;
    private Stack<String> positions;
    private ObservableList<String> dirEntriesObservable;
    private List<FTPClient.DirEntry> dirEntries;
    private SelectionModel listSelectionModel;
    private Text positionText;

    /**
     * Creates an instance with a given FTPClient object.
     * @param client the client to use.
     */
    public BrowsingWindow(@Nonnull FTPClient client) {
        browsingClient = client;
        window = new Stage();
        window.setTitle(String.format(BROWSING_WINDOW_TITLE, client.getAddress(),
                client.getPort()));
        makeBrowsingScene();
        window.setScene(browsingScene);
    }

    /**
     * Shows the window to the user.
     */
    public void show() {
        window.show();
    }

    private void makeBrowsingScene() {
        GridPane browsingGrid = new GridPane();

        positionText = new Text(ROOT_PATH);
        positionText.setFont(Font.font(14));
        positions = new Stack<>();
        positions.push(ROOT_PATH);
        browsingGrid.add(positionText, 0, 0);

        dirEntriesObservable = FXCollections.observableArrayList();
        ListView<String> entriesList = new ListView<>();
        entriesList.setItems(dirEntriesObservable);
        listSelectionModel = entriesList.getSelectionModel();
        entriesList.setOnMouseClicked(clickHandler);
        browsingGrid.add(entriesList, 0, 1);

        browsingGrid.setGridLinesVisible(true);
        ColumnConstraints gridColumnConstraints = new ColumnConstraints();
        gridColumnConstraints.setHgrow(Priority.ALWAYS);
        browsingGrid.getColumnConstraints().add(gridColumnConstraints);
        RowConstraints gridRowConstraints = new RowConstraints();
        gridRowConstraints.setVgrow(Priority.ALWAYS);
        browsingGrid.getRowConstraints().add(new RowConstraints());
        browsingGrid.getRowConstraints().add(gridRowConstraints);

        browsingScene = new Scene(browsingGrid, BROWSING_WINDOW_WIDTH,
                BROWSING_WINDOW_HEIGHT);
        loadFolder();
    }

    private void loadFolder() {
        positionText.setText(positions.peek());
        try {
            dirEntries = browsingClient.executeList(positions.peek());
            if (positions.size() != 1) {
                dirEntries.add(0, new FTPClient.DirEntry(PARENT_FOLDER, true));
            }
            dirEntriesObservable.clear();
            dirEntries.forEach(entry ->  {
                String entryName = entry.getPath();
                Path entryPath = Paths.get(entryName);
                if (entryPath.getNameCount() > 1) {
                    entryName = entryPath.getName(entryPath.getNameCount() - 1)
                            .toString();
                }
                if (entry.isDir() && !entryName.equals(PARENT_FOLDER)) {
                    entryName += '/';
                }
                dirEntriesObservable.add(entryName);
            });
        } catch (IOException e) {
            new Notification(LIST_ERROR_TITLE, LIST_ERROR_MESSAGE).show();
        }
    }

    private EventHandler<MouseEvent> clickHandler = event -> {
        int selectedIndex = listSelectionModel.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= dirEntries.size()) {
            return;
        }
        FTPClient.DirEntry entry = dirEntries.get(selectedIndex);
        if (entry.isDir()) {
            if (entry.getPath().equals(PARENT_FOLDER)) {
                positions.pop();
            } else {
                positions.push(entry.getPath());
            }
            loadFolder();
        } else {
            try {
                FTPClient downloadingClient = new FTPClient();
                if (browsingClient.getAddress() != null) {
                    downloadingClient.connect(browsingClient.getAddress(),
                            browsingClient.getPort());
                } else {
                    new Notification(GET_ERROR_TITLE, GET_ERROR_MESSAGE).show();
                }

                new Thread(() -> {
                    try {
                        downloadingClient.executeGet(entry.getPath());
                        Platform.runLater(() ->
                                new Notification(DOWNLOAD_FINISH_TITLE,
                                        String.format(DOWNLOAD_FINISH_MESSAGE,
                                                entry.getPath())).show());
                    } catch (IOException e) {
                        Platform.runLater(() ->
                                new Notification(GET_ERROR_TITLE,
                                        GET_ERROR_MESSAGE + e.getMessage())
                                        .show());
                    }
                }).start();
            } catch (IOException e) {
                new Notification(GET_ERROR_TITLE, GET_ERROR_MESSAGE).show();
            }
        }
    };
}
