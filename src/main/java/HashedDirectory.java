import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class HashedDirectory {
    private final Path dir;
    private final Path hashesPath;
    private Map<Path, HashedFile> hashes;

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

    protected static void wipeDir(File dir) throws IOException {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                wipeDir(file);
            }
            file.delete();
        }
    }

    protected static void deleteDir(Path dir) throws IOException {
        wipeDir(dir.toFile());
        Files.delete(dir);
    }

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

    protected void cloneDirectory(HashedDirectory src) {
        try {
            hashes.putAll(src.hashes);
            Files.walk(src.dir).forEach(srcPath -> {
                try {
                    Path targetPath = dir.resolve(src.dir.relativize(srcPath));
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new VCS.FileSystemError();
                }
            });
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    protected void flushHashes() {
        try {
            BufferedWriter hashWriter = new BufferedWriter(
                    new FileWriter(hashesPath.toString()));
            hashes.forEach((path, s) -> {
                try {
                    hashWriter.write(path.toString() + ' ' + s + '\n');
                } catch (IOException e) {
                    throw new VCS.FileSystemError();
                }
            });
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    public Map<Path, HashedFile> getFileDescriptions() {
        return hashes;
    }
}
