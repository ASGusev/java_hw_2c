import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;


public class GameWindow {
    private Game game;
    private GridPane gameField;
    private Stage window;
    private GameButton[] buttons = new GameButton[Game.FIELD_SIDE * Game.FIELD_SIDE];

    GameWindow (Game game, Stage window) {
        this.game = game;
        gameField = new GridPane();

        for (int i = 0; i < Game.FIELD_SIDE; i++) {
            for (int j = 0; j < Game.FIELD_SIDE; j++) {
                GameButton gameButton = new GameButton(i, j);
                buttons[i * Game.FIELD_SIDE + j] = gameButton;
                gameField.add(gameButton.getButton(), i + 1, j + 1);
            }
        }
        game.setOnUpdate(() -> {
            Platform.runLater(this::updateField);
        });


        this.window = window;
        window.setScene(new Scene(gameField, 300 , 300));
    }

    public void show() {
        window.show();
    }

    private class GameButton {
        private final int x;
        private final int y;
        private final Button button;
        private Character character = null;

        public GameButton(int x, int y) {
            this.x = x;
            this.y = y;
            button = new Button(" ");
            button.setOnAction(event -> {
                try {
                    if (character == null) {
                        game.step(x, y);
                        updateField();
                    }
                } catch (Game.BadStepException e) {

                }
            });
        }

        public Button getButton() {
            return button;
        }

        public void setChar(Character c) {
            character = c;
            button.setText(c.toString());
        }
    }

    private void updateField() {
        Character[] field = game.getGameField();
        for (int i = 0; i < Game.FIELD_SIDE; i++) {
            for (int j = 0; j < Game.FIELD_SIDE; j++) {
                int pos = i * Game.FIELD_SIDE + j;
                if (field[pos] != Game.NONE) {
                    buttons[pos].setChar(field[pos]);
                }
            }
        }
    }
}
