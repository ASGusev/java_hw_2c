import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;

public class HashedDirectory {
    private final Path dir;
    private final Path hashesPath;

    HashedDirectory(Path dir, Path hashesPath) {
        this.dir = dir;
        this.hashesPath = hashesPath;
    }

    protected static String getFileHash(String filePath) {
        DigestInputStream stream;
        byte[] hash = null;
        try (FileInputStream fin = new FileInputStream(filePath)) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            stream = new DigestInputStream(fin, messageDigest);
            while (stream.read() != -1) {}
            hash = messageDigest.digest();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        } catch (NoSuchAlgorithmException e) {}
        return new BigInteger(1, hash).toString();
    }

    protected static void wipeDir(Path dir) throws IOException {
        Stream<Path> content = Files.list(dir);
        content.forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    deleteDir(path);
                } else {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new VCS.FileSystemError();
            }
        });
        content.close();
    }

    protected static void deleteDir(Path dir) throws IOException {
        wipeDir(dir);
        Files.delete(dir);
    }

    void addFile(Path filePath) throws VCS.NoSuchFileException {
        try {
            if (!Files.isRegularFile(filePath)) {
                throw new VCS.NoSuchFileException();
            }

            Files.createDirectories(dir.resolve(filePath).getParent());
            Files.copy(filePath, dir.resolve(filePath), StandardCopyOption.REPLACE_EXISTING);

            List<String> hashes = Files.readAllLines(hashesPath);
            hashes.removeIf(s -> s.startsWith(filePath.toString()));
            BufferedWriter listWriter = new BufferedWriter(
                    new FileWriter(hashesPath.toString()));
            for (String hash : hashes) {
                listWriter.write(hash + '\n');
            }
            listWriter.write(filePath.toString() + ' ' +
                    HashedDirectory.getFileHash(filePath.toString()) + '\n');
            listWriter.close();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    protected void cloneDirectory(HashedDirectory src) {
        try {
            Files.copy(src.hashesPath, hashesPath, StandardCopyOption.REPLACE_EXISTING);
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
}
