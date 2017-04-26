package ru.spbau.gusev.ftp.server;

import org.junit.Assert;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FTPServerTest {
    private final String LOOP = "127.0.0.1";
    private Path REQUEST_PATH = Paths.get("req_path");
    private int PORT = 3000;

    @Test
    public void getTest() throws IOException {
        String fileContent = "content";
        byte[] fileContentBytes = fileContent.getBytes();
        Files.write(REQUEST_PATH, fileContentBytes);

        FTPServer server = new FTPServer(PORT);
        server.start();

        try (Socket clientSocket = new Socket(LOOP, PORT)) {
            DataOutputStream socketOutputStream =
                    new DataOutputStream(clientSocket.getOutputStream());
            socketOutputStream.writeInt(1);
            writeStringToStream(socketOutputStream, REQUEST_PATH.toString());
            socketOutputStream.flush();

            DataInputStream socketInputStream =
                    new DataInputStream(clientSocket.getInputStream());
            long responseLength = socketInputStream.readLong();
            Assert.assertEquals(fileContentBytes.length, responseLength);
            for (int i = 0; i < responseLength; i++) {
                byte responseByte = socketInputStream.readByte();
                Assert.assertEquals(fileContentBytes[i], responseByte);
            }

            socketInputStream.close();
            socketOutputStream.close();
        } finally {
            Files.deleteIfExists(REQUEST_PATH);
            server.stop();
        }
    }

    @Test
    public void listTest() throws IOException {
        String fileName = "file";
        String dirName = "dir";
        Path dirPath = REQUEST_PATH.resolve(dirName);
        Path filePath = REQUEST_PATH.resolve(fileName);

        FTPServer server = new FTPServer(PORT);
        server.start();

        try (Socket clientSocket = new Socket(LOOP, PORT)) {
            Files.createDirectory(REQUEST_PATH);
            Files.write(filePath, "cont".getBytes());
            Files.createDirectory(dirPath);

            DataOutputStream socketOutputStream =
                    new DataOutputStream(clientSocket.getOutputStream());
            socketOutputStream.writeInt(2);
            writeStringToStream(socketOutputStream, REQUEST_PATH.toString());
            socketOutputStream.flush();

            DataInputStream socketInputStream =
                    new DataInputStream(clientSocket.getInputStream());
            Assert.assertEquals(2, socketInputStream.readInt());
            Assert.assertEquals(dirPath.toString(),
                    readStringFromStream(socketInputStream));
            Assert.assertEquals(1, socketInputStream.readByte());
            Assert.assertEquals(filePath.toString(),
                    readStringFromStream(socketInputStream));
            Assert.assertEquals(0, socketInputStream.readByte());

            socketInputStream.close();
            socketOutputStream.close();
        } finally {
            server.stop();
            Files.deleteIfExists(filePath);
            Files.deleteIfExists(dirPath);
            Files.deleteIfExists(REQUEST_PATH);
        }
    }

    private void writeStringToStream(DataOutputStream stream, String str)
            throws IOException {
        stream.writeInt(str.length());
        for (char c: str.toCharArray()) {
            stream.writeChar(c);
        }
    }

    private String readStringFromStream(DataInputStream stream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int len = stream.readInt();
        for (int i = 0; i < len; i++) {
            stringBuilder.append(stream.readChar());
        }
        return stringBuilder.toString();
    }
}
