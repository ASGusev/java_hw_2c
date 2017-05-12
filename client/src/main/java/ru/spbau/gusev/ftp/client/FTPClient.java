package ru.spbau.gusev.ftp.client;

import ru.spbau.gusev.ftp.protocol.Protocol;
import ru.spbau.gusev.ftp.utils.Utils;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A very small file sharing client.
 */
public class FTPClient {
    private SocketChannel socketChannel;
    private final static int MAX_PATH_LEN = 1 << 11;
    private final static int MAX_REQUEST_SIZE = Integer.BYTES +
            MAX_PATH_LEN * Character.BYTES;
    private final static int FILE_BUFFER_SIZE = 1 << 12;
    private String address;
    private int port;

    /**
     * Gets the address of the connected server.
     * @return the address used to connect.
     */
    @Nullable
    public String getAddress() {
        return address;
    }

    /**
     * Gets the port of the connected server.
     * @return the port used to connect.
     */
    public int getPort() {
        return port;
    }

    /**
     * Connects to a file server.
     * @param address the address of the server.
     * @param port the port of the server.
     * @throws IOException if connection is impossible.
     */
    public void connect(@Nonnull String address, int port) throws IOException {
        this.address = address;
        this.port = port;
        socketChannel = SocketChannel.open(new InetSocketAddress(address, port));
    }

    /**
     * Cuts a connection with a server.
     * @throws IOException if disconnection is impossible.
     */
    public void disconnect() throws IOException {
        socketChannel.close();
        socketChannel = null;
    }

    /**
     * Executes a get request - downloads a file with the given path.
     * @param path the path to the file to download.
     * @throws IOException if a downloading error happens.
     */
    public void executeGet(@Nonnull String path) throws IOException {
        ByteBuffer requestMessageBuffer = ByteBuffer.allocate(MAX_REQUEST_SIZE);
        requestMessageBuffer.putInt(Protocol.REQUEST_TYPE_GET);
        Utils.writeStringToBuffer(path, requestMessageBuffer);
        requestMessageBuffer.flip();
        socketChannel.write(requestMessageBuffer);

        ByteBuffer sizeBuffer = ByteBuffer.allocate(Long.BYTES);
        while (sizeBuffer.hasRemaining()) {
            socketChannel.read(sizeBuffer);
        }
        sizeBuffer.flip();
        long fileSize = sizeBuffer.getLong();

        if (fileSize == 0) {
            throw new NoSuchFileException(path);
        }

        long written = 0;
        ByteBuffer fileBuffer = ByteBuffer.allocate(FILE_BUFFER_SIZE);
        Path targetPath = Paths.get(path);
        if (targetPath.getNameCount() > 1) {
            targetPath = targetPath.getParent().relativize(targetPath);
        }
        FileChannel fileChannel = FileChannel.open(targetPath,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        while (written != fileSize) {
            socketChannel.read(fileBuffer);
            fileBuffer.flip();
            while (fileBuffer.remaining() != 0) {
                written += fileChannel.write(fileBuffer);
            }
            fileBuffer.clear();
        }
        fileChannel.close();
    }

    /**
     * Executes a list request, listing the given folder.
     * @param path the path to the folder to list
     * @return a List with DirEntry objects for all the directory entries.
     * @throws IOException if an error happens.
     */
    @Nonnull
    public List<DirEntry> executeList(@Nonnull String path) throws IOException {
        ByteBuffer requestMessageBuffer = ByteBuffer.allocate(MAX_REQUEST_SIZE);
        requestMessageBuffer.putInt(Protocol.REQUEST_TYPE_LIST);
        Utils.writeStringToBuffer(path, requestMessageBuffer);
        requestMessageBuffer.flip();
        socketChannel.write(requestMessageBuffer);

        ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        socketChannel.read(sizeBuffer);
        sizeBuffer.flip();
        int itemsNumber = sizeBuffer.getInt();

        if (itemsNumber == 0) {
            throw new NoSuchFileException(path);
        }

        List <DirEntry> content = new ArrayList<>();
        ChannelStringReader pathReader = new ChannelStringReader(socketChannel);
        ByteBuffer flagBuffer = ByteBuffer.allocate(Byte.BYTES);
        for (int i = 0; i < itemsNumber; i++) {
            String entryPath = pathReader.read();
            socketChannel.read(flagBuffer);
            flagBuffer.flip();
            boolean isDir = flagBuffer.get() == 1;
            flagBuffer.clear();
            content.add(new DirEntry(entryPath, isDir));
        }
        return content;
    }

    /**
     * A class representing a folder entry on the server.
     */
    public static class DirEntry {
        private final String path;
        private final boolean dir;

        public DirEntry(@Nonnull String path, boolean dir) {
            this.path = path;
            this.dir = dir;
        }

        @Nonnull
        public String getPath() {
            return path;
        }

        public boolean isDir() {
            return dir;
        }
    }

    private static class ChannelStringReader {
        private final ByteChannel channel;
        private final ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);

        private ChannelStringReader(ByteChannel channel) {
            this.channel = channel;
        }

        private String read() throws IOException {
            while (sizeBuffer.position() != sizeBuffer.capacity()) {
                channel.read(sizeBuffer);
            }
            sizeBuffer.flip();
            int stringSize = sizeBuffer.getInt();
            sizeBuffer.clear();

            ByteBuffer contentBuffer = ByteBuffer.allocate(Character.BYTES * stringSize);
            while (contentBuffer.position() != contentBuffer.capacity()) {
                channel.read(contentBuffer);
            }
            contentBuffer.flip();

            char[] symbols = new char[stringSize];
            for (int i = 0; i < stringSize; i++) {
                symbols[i] = contentBuffer.getChar();
            }
            return String.valueOf(symbols);
        }
    }
}
