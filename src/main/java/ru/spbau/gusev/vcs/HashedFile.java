package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
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
    private final Path dir;

    /**
     * Creates an object from file path an already calculated hash.
     * @param path path to file.
     * @param dir path to the hashed directory which the file belongs to.
     * @param hash hash of file.
     */
    HashedFile(@Nonnull Path path, @Nonnull Path dir, @Nonnull String hash) {
        this.hash = hash;
        this.path = path;
        this.dir = dir;
    }

    /**
     * Creates an object calculating hash.
     * @param path path to the file.
     * @param dir path to the hashed directory which the file belongs to.
     */
    HashedFile(@Nonnull Path path, @Nonnull Path dir) {
        this.path = path;
        this.dir = dir;
        hash = calcFileHash(path.toString());
    }

    /**
     * Calculates SHA-1 hash of the file.
     * @param filePath path to the file to calculate hash.
     * @return SHA-1 hash of the provided file.
     */
    @Nonnull
    protected static String calcFileHash(@Nonnull String filePath) {
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
    @Nonnull
    public String getHash() {
        return hash;
    }

    /**
     * Gets the path to the file.
     * @return the path to the file.
     */
    @Nonnull
    public Path getPath() {
        return path;
    }

    /**
     * Returns the directory to which the file belongs.
     * @return dir
     */
    @Nonnull
    public Path getDir() {
        return dir;
    }

    /**
     * Compares this object with another HashedFile. Two HashedFiles are considered equal
     * if their hashes coincide.
     * @param obj another object to compare this one with.
     * @return true if the given object is an equal HashedFile, false if it is not equal
     * or is not a HashedFile object.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof HashedFile &&
                hash.equals(((HashedFile) obj).hash);
    }

    /**
     * Gets the complete path to the file. Complete path consists of the directory path
     * and the file path.
     * @return the path to the file.
     */
    @Nonnull
    protected Path getFullPath() {
        return dir.resolve(path);
    }
}
