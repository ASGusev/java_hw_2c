package ru.spbau.gusev.ftp.client;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class FTPClientTest {
    private final String LOOP = "127.0.0.1";
    private final int PORT = 3000;
    private final String REQUEST_PATH = "req_path";

    @Test
    public void testGet() throws IOException, InterruptedException {
        String fileContentString = "content";

        byte[] fileContentBytes = fileContentString.getBytes();
        ByteBuffer serverResponse = ByteBuffer.allocate(Long.BYTES
                + fileContentBytes.length);
        serverResponse.putLong((long) fileContentBytes.length);
        serverResponse.put(fileContentBytes);
        serverResponse.flip();

        MockedServer serverMock = new MockedServer(serverResponse.array(), PORT);
        new Thread(serverMock).start();
        Thread.sleep(100);

        FTPClient client = new FTPClient();
        client.connect(LOOP, PORT);
        client.executeGet(REQUEST_PATH);
        client.disconnect();

        Assert.assertEquals(1, serverMock.getRequestCode());
        Assert.assertEquals(REQUEST_PATH, serverMock.getRequestPath());
        Path filePath = Paths.get(REQUEST_PATH);
        Assert.assertEquals(Collections.singletonList(fileContentString),
                Files.readAllLines(filePath));
        Files.deleteIfExists(filePath);
    }

    @Test
    public void testList() throws IOException, InterruptedException {
        String fileName = "file";
        String dirName = "directory";

        int responseLength = Integer.BYTES;
        responseLength += 2 * (Integer.BYTES + 1);
        responseLength += (fileName.length() + dirName.length()) * Character.BYTES;
        ByteBuffer serverResponse = ByteBuffer.allocate(responseLength);
        serverResponse.putInt(2);
        FTPClient.writeStringToBuffer(fileName, serverResponse);
        serverResponse.put((byte) 0);
        FTPClient.writeStringToBuffer(dirName, serverResponse);
        serverResponse.put((byte) 1);
        serverResponse.flip();

        MockedServer mockedServer = new MockedServer(serverResponse.array(), PORT);
        new Thread(mockedServer).start();
        Thread.sleep(100);

        FTPClient client = new FTPClient();
        client.connect(LOOP, PORT);
        List<FTPClient.DirEntry> entries = client.executeList(REQUEST_PATH);
        client.disconnect();

        Assert.assertEquals(2, mockedServer.getRequestCode());
        Assert.assertEquals(REQUEST_PATH, mockedServer.getRequestPath());
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals(fileName, entries.get(0).getPath());
        Assert.assertFalse(entries.get(0).isDir());
        Assert.assertEquals(dirName, entries.get(1).getPath());
        Assert.assertTrue(entries.get(1).isDir());
    }

    private class MockedServer implements Runnable {
        private final byte[] response;
        private final int port;
        private int requestCode;
        private String requestPath;

        private int getRequestCode() {
            return requestCode;
        }

        private String getRequestPath() {
            return requestPath;
        }

        private MockedServer(byte[] response, int port) {
            this.response = response;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                Socket socket = serverSocket.accept();
                DataInputStream dataIn = new DataInputStream(socket.getInputStream());

                requestCode = dataIn.readInt();
                StringBuilder pathBuilder = new StringBuilder();
                int pathLength = dataIn.readInt();
                for (int i = 0; i < pathLength; i++) {
                    pathBuilder.append(dataIn.readChar());
                }
                requestPath = pathBuilder.toString();
                OutputStream out = socket.getOutputStream();
                out.write(response);
                out.flush();
                out.close();

                dataIn.close();
                socket.close();
                serverSocket.close();
            } catch (IOException e) {
                Assert.fail();
            }
        }
    }
}
