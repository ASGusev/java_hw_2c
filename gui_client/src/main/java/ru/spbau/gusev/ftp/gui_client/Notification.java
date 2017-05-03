package ru.spbau.gusev.ftp.gui_client;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import javax.annotation.Nonnull;

/**
 * A wrapper on Dialog class for showing simple notifications.
 */
public class Notification {
    private final Dialog errorDialog;
    private final static String BUTTON_TEXT = "Ok";

    /**
     * Creates a notification with supplied title and message.
     * @param title the for the dialog.
     * @param message the message for the dialog.
     */
    Notification(@Nonnull String title, @Nonnull String message) {
        errorDialog = new Dialog<String>();
        errorDialog.setTitle(title);
        errorDialog.setContentText(message);
        errorDialog.getDialogPane().getButtonTypes().add(
                new ButtonType(BUTTON_TEXT));
    }

    /**
     * Shows the dialog.
     */
    void show() {
        errorDialog.show();
    }
}
