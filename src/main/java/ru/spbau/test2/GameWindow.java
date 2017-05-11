package ru.spbau.test2;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.*;

public class GameWindow {
    private final Stage window;
    private final Game game;
    private final boolean[] locked = new boolean[1];
    private final Timer finishTimer = new Timer();
    private final static long UPDATE_DELAY = 500;
    private final List<List<FieldCell>> field = new ArrayList<>();

    public GameWindow(Stage window, Game game) {
        this.window = window;
        this.game = game;

        GridPane fieldGrid = new GridPane();
        for (int i = 0; i < game.getN(); i++) {
            field.add(new ArrayList<>());
            for (int j = 0; j < game.getN(); j++) {
                FieldCell cell = new FieldCell(game.getCell(i, j));
                cell.setOnAction(onButtonClick);
                field.get(i).add(cell);
                fieldGrid.add(cell, i, j);
            }
        }

        Scene fieldScene = new Scene(fieldGrid);
        window.setScene(fieldScene);
    }

    public void show() {
        window.show();
    }

    private EventHandler<ActionEvent> onButtonClick = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            synchronized (locked) {
                if (locked[0]) {
                    return;
                }
            }
            FieldCell touchedCell = (FieldCell) event.getSource();
            touchedCell.showText();
            synchronized (game) {
                if (!game.hasOpened()) {
                    game.open(touchedCell.getCell());
                } else {
                    Game.Cell prevGCell = game.getOpened();
                    game.close();
                    FieldCell prevCell = field.get(prevGCell.getX()).get(prevGCell.getY());
                    synchronized (locked) {
                        locked[0] = true;
                    }
                    if (prevCell.getContent().equals(touchedCell.getContent())) {
                        finishTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> {
                                    prevCell.setDisable(true);
                                    touchedCell.setDisable(true);
                                    locked[0] = false;
                                });
                            }
                        }, UPDATE_DELAY);
                    } else {
                        finishTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> {
                                    prevCell.hideText();
                                    touchedCell.hideText();
                                    locked[0] = false;
                                });
                            }
                        }, UPDATE_DELAY);
                    }
                }
            }
        }
    };

    private class FieldCell extends Button {
        private final String DEFAULT_TEXT = " ";
        private Game.Cell cell;

        private FieldCell(Game.Cell cell) {
            super();
            setText(DEFAULT_TEXT);
            this.cell = cell;
        }

        private String getContent() {
            return String.valueOf(cell.getValue());
        }

        private void showText() {
            setText(getContent());
        }

        private void hideText() {
            setText(DEFAULT_TEXT);
        }

        private Game.Cell getCell() {
            return cell;
        }
    }
}
