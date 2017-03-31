import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A class maintaining a folder where hashes are calculated for all files.
 */
public class HashedDirectory {
    private final Path dir;
    private final Path hashesPath;
    private Map<Path, HashedFile> hashes;

    /**
     * Creates a HashedDirectory object with supplied directory and supplied file
     * with hashes.
     * @param dir the path to the directory.
     * @param hashesPath the path to the file with hashes.
     */
    protected HashedDirectory(Path dir, Path hashesPath) {
        this.dir = dir;
        this.hashesPath = hashesPath;
        hashes = new LinkedHashMap<>();
        try {
            if (Files.exists(hashesPath)) {
                for (String line : Files.readAllLines(hashesPath)) {
                    String[] parts = line.split(" ");
                    hashes.put(Paths.get(parts[0]),
                            new HashedFile(Paths.get(parts[0]), parts[1]));
                }
            } else {
                Files.createFile(hashesPath);
            }
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Removes all the files from the given directory.
     * @throws IOException if a deletion error occurs.
     */
    protected static void wipeDir(File dir) throws IOException {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                wipeDir(file);
            }
            file.delete();
        }
    }

    /**
     * Recursively deletes the given folder.
     * @param dir the directory to delete.
     * @throws IOException if a deletion error occurs.
     */
    protected static void deleteDir(Path dir) throws IOException {
        wipeDir(dir.toFile());
        Files.delete(dir);
    }

    /**
     * Recursively deletes the given folder.
     * @param dir the path to the directory to delete.
     * @throws IOException if a deletion error occurs.
     */
    protected static void deleteDir(String dir) throws IOException {
        deleteDir(Paths.get(dir));
    }

    /**
     * Copies a file to the directory.
     * @param dirPath the path to a working dir containing file.
     * @param filePath the resting path from the working dir to the file to copy.
     * @throws VCS.NoSuchFileException in case the provided path does not lead to a
     * valid file.
     */
    void addFile(Path dirPath, Path filePath) throws VCS.NoSuchFileException {
        try {
            if (!Files.isRegularFile(filePath)) {
                throw new VCS.NoSuchFileException();
            }

            Files.createDirectories(dir.resolve(filePath).getParent());
            Files.copy(dirPath.resolve(filePath), dir.resolve(filePath),
                    StandardCopyOption.REPLACE_EXISTING);

            hashes.put(filePath,
                    new HashedFile(Paths.get(dir.resolve(filePath).toString())));
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Copies the provided directory content into the current folder.
     * @param src the directory to be copied.
     */
    protected void cloneDirectory(HashedDirectory src) {
        try {
            hashes.putAll(src.hashes);
            Files.walk(src.dir).filter(Files::isRegularFile).forEach(srcPath -> {
                try {
                    Path targetPath = dir.resolve(src.dir.relativize(srcPath));
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new VCS.FileSystemError();
                }
            });
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Writes the file hashes to the hashes file.
     */
    protected void flushHashes() {
        try {
            BufferedWriter hashWriter = new BufferedWriter(
                    new FileWriter(hashesPath.toString()));
            hashes.forEach((path, s) -> {
                try {
                    hashWriter.write(path.toString() + ' ' + s.getHash() + '\n');
                } catch (IOException e) {
                    throw new VCS.FileSystemError();
                }
            });
            hashWriter.close();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Gets a map with file descriptions.
     * @return a Map from file path in the directory to its hash and absolute path.
     */
    protected Map<Path, HashedFile> getFileDescriptions() {
        return hashes;
    }

    protected void clear() {
        try {
            wipeDir(dir.toFile());
            hashes.clear();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }
}
