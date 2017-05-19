import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

/**
 * A class containing methods for calculating the MD5 hash of a file or a folder.
 */
public abstract class MD5Calculator {
    private static final int BUFFER_SIZE = 1 << 12;

    /**
     * Calculates the MD5 hash of a file or a folder in one thread.
     * @param path the path to the file pr folder.
     * @return the MD5 hash in byte[] form.
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the MessageDogest cannot find the MD5 algorithm.
     */
    public static byte[] md5WithSingleThread(String path) throws IOException,
            NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("MD5");
        Path filePath = Paths.get(path);

        if (Files.isDirectory(filePath)) {
            try {
                Files.walk(filePath)
                        .forEach(curPath -> {
                            try {
                                if (Files.isRegularFile(curPath)) {
                                    digestFile(digest, curPath);
                                } else {
                                    int lastNameIndex = curPath.getNameCount() - 1;
                                    digest.update(curPath.getName(lastNameIndex).
                                            toString().getBytes());
                                }
                            } catch (IOException e) {
                                Error e1 = new Error();
                                e1.addSuppressed(e);
                                throw e1;
                            }
                        });
            } catch (Error e) {
                throw (IOException) e.getSuppressed()[0];
            }
        } else {
            digestFile(digest, filePath);
        }

        return digest.digest();
    }

    /**
     * Calculates the MD5 hash of given file or folder using a ForkJoinPool.
     * @param path the path to the file or folder.
     * @return a byte array with the MD5 hash of the file/folder.
     * @throws NoSuchAlgorithmException if the MessageDigest cannot find MD5 algorithm.
     */
    public static byte[] md5ForkJoin(String path) throws IOException,
            NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        Path filePath = Paths.get(path);
        try {
            ForkJoinPool pool = new ForkJoinPool();
            HashingTask task = new HashingTask(filePath, messageDigest);
            pool.submit(task);
            task.join();
        } catch (Error e) {
            throw (IOException)e.getSuppressed()[0];
        }

        return messageDigest.digest();
    }

    private static void digestFile(MessageDigest digest, Path file) throws IOException {
        FileInputStream fin = new FileInputStream(file.toString());
        boolean read = false;
        byte[] buffer = new byte[BUFFER_SIZE];
        while (!read) {
            int justRead = fin.read(buffer);
            if (justRead < BUFFER_SIZE) {
                read = true;
            }
            digest.update(buffer, 0, justRead);
        }
    }

    private static class HashingTask extends RecursiveAction {
        private final Path path;
        private final MessageDigest messageDigest;

        HashingTask(Path path, MessageDigest messageDigest) {
            this.path = path;
            this.messageDigest = messageDigest;
        }

        @Override
        protected void compute() {
            try {
                if (Files.isDirectory(path)) {
                    int lastNameIndex = path.getNameCount() - 1;
                    messageDigest.update(path.getName(lastNameIndex).toString().getBytes());

                    List<HashingTask> newTasks = Files.list(path).map(path1 ->
                            new HashingTask(path1, messageDigest)).collect(Collectors.toList());
                    invokeAll(newTasks);
                } else {
                    digestFile(messageDigest, path);
                }
            } catch (IOException e) {
                Error e1 = new Error();
                e1.addSuppressed(e);
                throw e1;
            }
        }
    }
}
