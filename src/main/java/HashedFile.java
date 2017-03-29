import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class HashedFile {
    private final String hash;
    private final Path path;

    HashedFile(Path path, String hash) {
        this.hash = hash;
        this.path = path;
    }

    HashedFile(Path path) {
        this.path = path;
        hash = calcFileHash(path.toString());
    }

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

    public String getHash() {
        return hash;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HashedFile &&
                hash.equals(((HashedFile) obj).hash);
    }


}
