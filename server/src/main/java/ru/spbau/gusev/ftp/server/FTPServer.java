package ru.spbau.gusev.ftp.server;

import javax.annotation.Nonnull;
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

/**
 * A simple file server.
 */
public class FTPServer {
    private AsynchronousServerSocketChannel serverSocketChannel;
    private final int port;
    private final Set<ClientConnection> clients;

    /**
     * Creates a server for the specified port.
     * @param port the port for the server.
     */
    FTPServer(int port) {
        this.port = port;
        clients = new HashSet<>();
    }

    /**
     * Enables the server to accept requests.
     * @throws IOException if an error staring up occurs.
     */
    public void start() throws IOException {
        serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.accept(null, connectionHandler);
    }

    /**
     * Stops accepting requests.
     * @throws IOException if an error during close operation occurs.
     */
    public void stop() throws IOException {
        serverSocketChannel.close();
        clients.forEach(ClientConnection::close);
        clients.clear();
    }

    /**
     * A CompletionHandler for accepting new connections.
     */
    private CompletionHandler<AsynchronousSocketChannel, Object> connectionHandler =
            new CompletionHandler<AsynchronousSocketChannel, Object>() {

                @Override
                public void completed(AsynchronousSocketChannel result, Object attachment) {
                    ClientConnection client = new ClientConnection(result);
                    client.startListening();
                    clients.add(client);
                    serverSocketChannel.accept(null, this);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    serverSocketChannel.accept(null, this);
                }
            };

    /**
     * A class containing all the logic of request processing.
     */
    private class ClientConnection implements CompletionHandler<Integer, Object> {
        private final static int FILE_BUFFER_SIZE = 1 << 12;
        private final static int REQUEST_TYPE_GET = 1;
        private final static int REQUEST_TYPE_LIST = 2;

        private final AsynchronousSocketChannel socketChannel;
        private final ByteBuffer requestBuffer;

        private ClientConnection(@Nonnull AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
            requestBuffer = ByteBuffer.allocate(Integer.BYTES);
        }

        private void startListening() {
            socketChannel.read(requestBuffer, null, this);
        }

        @Override
        public void completed(Integer result, Object attachment) {
            if (result == -1) {
                try{
                    socketChannel.close();
                } catch (IOException e) {}
                clients.remove(this);
                return;
            }

            fillBufferFromSocket(requestBuffer);
            int requestType = requestBuffer.getInt();

            ByteBuffer lenBuffer = ByteBuffer.allocate(Integer.BYTES);
            fillBufferFromSocket(lenBuffer);
            int pathLen = lenBuffer.getInt();

            char[] pathSymbols = new char[pathLen];
            ByteBuffer pathBuffer = ByteBuffer.allocate(pathLen * Character.BYTES);
            fillBufferFromSocket(pathBuffer);
            for (int i = 0; i < pathLen; i++) {
                pathSymbols[i] = pathBuffer.getChar();
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
            clients.remove(this);
        }

        private void close() {
            try {
                socketChannel.close();
            } catch (IOException e) {}
        }

        private void processRequestGet(@Nonnull Path path) {
            ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
            if (Files.isRegularFile(path)) {
                long size;
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
                            writeBufferToSocket(fileBuffer);
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

        private void processRequestList(@Nonnull Path path) {
            List<DirEntry> entries = Collections.emptyList();
            if (Files.isDirectory(path)) {
                try {
                    entries = Files.list(path)
                            .sorted()
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
            System.out.println(entries.toString());
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
            writeBufferToSocket(responseBuffer);
        }

        private void fillBufferFromSocket(@Nonnull ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                Future reading = socketChannel.read(buffer);
                try {
                    reading.get();
                } catch (InterruptedException | ExecutionException e) {
                    buffer.clear();
                    startListening();
                    return;
                }
            }
            buffer.flip();
        }

        private void writeBufferToSocket(@Nonnull ByteBuffer buffer) {
            while (buffer.remaining() != 0) {
                Future writing = socketChannel.write(buffer);
                while (!writing.isDone()) {
                    try {
                        writing.get();
                    } catch (InterruptedException | ExecutionException e) {
                        close();
                        return;
                    }
                }
            }
        }

        private class DirEntry {
            private final String path;
            private final boolean isDir;

            private DirEntry(@Nonnull Path path) {
                this.path = path.toString();
                isDir = Files.isDirectory(path);
            }

            public @Nonnull String getPath() {
                return path;
            }

            public @Nonnull boolean isDir() {
                return isDir;
            }
        }
    }
}
