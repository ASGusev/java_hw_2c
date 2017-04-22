package ru.spbau.gusev.ftp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FTPServer {
    private AsynchronousServerSocketChannel serverSocketChannel;
    private final int port;
    private final Set<ClientConnection> clientConnections;

    FTPServer(int port) {
        this.port = port;
        clientConnections = new HashSet<>();
    }

    public void start() throws IOException {
        serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.accept(null, connectionHandler);
    }

    public void stop() throws IOException {
        serverSocketChannel.close();
        clientConnections.forEach(ClientConnection::close);
        clientConnections.clear();
    }

    private CompletionHandler<AsynchronousSocketChannel, Object> connectionHandler =
            new CompletionHandler<AsynchronousSocketChannel, Object>() {

                @Override
                public void completed(AsynchronousSocketChannel result, Object attachment) {
                    ClientConnection client = new ClientConnection(result);
                    client.startListening();
                    clientConnections.add(client);
                    serverSocketChannel.accept(null, this);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    serverSocketChannel.accept(null, this);
                }
            };

    private class ClientConnection implements CompletionHandler<Integer, Object> {
        private final static int MAX_PATH_LENGTH = 2048;
        private final static int FILE_BUFFER_SIZE = 1 << 12;
        private final static int REQUEST_TYPE_GET = 1;
        private final static int REQUEST_TYPE_LIST = 2;

        private final AsynchronousSocketChannel socketChannel;
        private final ByteBuffer requestBuffer;

        ClientConnection(AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
            requestBuffer = ByteBuffer.allocate(Integer.BYTES +
                    MAX_PATH_LENGTH * Character.BYTES);
        }

        private void startListening() {
            socketChannel.read(requestBuffer, null, this);
        }

        @Override
        public void completed(Integer result, Object attachment) {
            requestBuffer.flip();
            int requestType = requestBuffer.getInt();
            int pathLen = requestBuffer.getInt();
            char[] pathSymbols = new char[pathLen];
            for (int i = 0; i < pathLen; i++) {
                pathSymbols[i] = requestBuffer.getChar();
            }
            Path path = Paths.get(String.valueOf(pathSymbols));

            switch (requestType) {
                case REQUEST_TYPE_GET: {
                    processRequestGet(path);
                    break;
                }
                case REQUEST_TYPE_LIST: {
                    processRequestList(path);
                    break;
                }
            }
            requestBuffer.clear();
            startListening();
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            clientConnections.remove(this);
        }

        private void close() {
            try {
                socketChannel.close();
            } catch (IOException e) {}
        }

        private void processRequestGet(Path path) {
            ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
            if (Files.isRegularFile(path)) {
                long size = 0;
                try {
                    size = Files.size(path);
                } catch (IOException e) {
                    size = 0;
                }
                sizeBuffer.putLong(size);
                sizeBuffer.flip();
                socketChannel.write(sizeBuffer);

                if (size != 0) {
                    ByteBuffer fileBuffer = ByteBuffer.allocate(FILE_BUFFER_SIZE);
                    try (FileChannel fileChannel = FileChannel.open(path)) {
                        while (fileChannel.position() != fileChannel.size()) {
                            fileChannel.read(fileBuffer);
                            fileBuffer.flip();
                            while (fileBuffer.remaining() != 0) {
                                Future writing = socketChannel.write(fileBuffer);
                                while (!writing.isDone()) {
                                    try {
                                        writing.get();
                                    } catch (InterruptedException | ExecutionException e) {}
                                }
                            }
                            fileBuffer.clear();
                        }
                    } catch (IOException e) {
                        close();
                    }
                }
            } else {
                sizeBuffer.putLong(0);
                sizeBuffer.flip();
                socketChannel.write(sizeBuffer);
            }
        }

        private void processRequestList(Path path) {
            List<DirEntry> entries = Collections.emptyList();
            if (Files.isDirectory(path)) {
                try {
                    entries = Files.list(path)
                            .map(DirEntry::new)
                            .collect(Collectors.toList());
                } catch (IOException e) {}
            }

            int listSize = entries.size();
            int responseSize = Integer.BYTES;
            for (DirEntry entry: entries) {
                responseSize += Integer.BYTES +
                        entry.getPath().length() * Character.BYTES + Byte.BYTES;
            }
            ByteBuffer responseBuffer = ByteBuffer.allocate(responseSize);
            responseBuffer.putInt(listSize);
            for (DirEntry entry: entries) {
                String entryPath = entry.getPath();
                responseBuffer.putInt(entryPath.length());
                for (char c: entryPath.toCharArray()) {
                    responseBuffer.putChar(c);
                }
                if (entry.isDir()) {
                    responseBuffer.put((byte)1);
                } else {
                    responseBuffer.put((byte) 0);
                }
            }
            responseBuffer.flip();
            while (responseBuffer.remaining() != 0) {
                Future writing = socketChannel.write(responseBuffer);
                while (!writing.isDone()) {
                    try {
                        writing.get();
                    } catch (InterruptedException | ExecutionException e) {}
                }
            }
        }

        private class DirEntry {
            private final String path;
            private final boolean isDir;

            private DirEntry(Path path) {
                this.path = path.toString();
                isDir = Files.isDirectory(path);
            }

            public String getPath() {
                return path;
            }

            public boolean isDir() {
                return isDir;
            }
        }
    }
}
