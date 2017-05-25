import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.function.Consumer;

public class Game implements Runnable {
    public static int FIELD_SIDE = 3;
    public static Character NONE = 'N';

    private final Socket socket;
    private boolean ourTurn;
    private Character ourLetter;
    private Character[] gameField = new Character[FIELD_SIDE * FIELD_SIDE];
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Runnable onUpdate;
    private Consumer<Character> onFinish;

    Game(Socket socket, Character letter) throws IOException {
        this.socket = socket;
        ourLetter = letter;
        ourTurn = letter.equals('X');
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());

        Arrays.fill(gameField, 'N');

        new Thread(this).start();
    }

    public void step(int x, int y) throws BadStepException {
        int pos = x * FIELD_SIDE + y;
        if (!ourTurn || gameField[pos] != NONE) {
            throw new BadStepException();
        }

        gameField[pos] = ourLetter;
        ourTurn = false;
        sendField();
    }

    private void sendField() {
        try {
            for (Character c : gameField) {
                outputStream.writeChar(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            try {
                gameField[0] = inputStream.readChar();
                for (int i = 1; i < FIELD_SIDE * FIELD_SIDE; i++) {
                    gameField[i] = inputStream.readChar();
                }
                running = false;
                for (int i = 0; i < FIELD_SIDE * FIELD_SIDE; i++) {
                    if (gameField[i].equals(NONE)) {
                        running = true;
                    }
                }
                if (running) {
                    onUpdate.run();
                }

                ourTurn = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    public Character[] getGameField() {
        return gameField;
    }

    public class BadStepException extends Exception {}
}
