import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class representing a file with calculated hash.
 */
public class HashedFile {
    private final String hash;
    private final Path path;

    /**
     * Creates an object from file path an already calculated hash.
     * @param path path to file.
     * @param hash hash of file.
     */
    HashedFile(Path path, String hash) {
        this.hash = hash;
        this.path = path;
    }

    /**
     * Creates an object calculating hash.
     * @param path path to the file.
     */
    HashedFile(Path path) {
        this.path = path;
        hash = calcFileHash(path.toString());
    }

    /**
     * Calculates SHA-1 hash of the file.
     * @param filePath path to the file to calculate hash.
     * @return SHA-1 hash of the provided file.
     */
    protected static String calcFileHash(String filePath) {
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

    /**
     * Gets hash of the file.
     * @return the hash of the file.
     */
    public String getHash() {
        return hash;
    }

    /**
     * Gets the path to the file.
     * @return the path to the file.
     */
    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HashedFile &&
                hash.equals(((HashedFile) obj).hash);
    }
}
