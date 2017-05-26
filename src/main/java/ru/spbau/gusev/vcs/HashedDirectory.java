package ru.spbau.gusev.vcs;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

/**
 * A class maintaining a folder where hashes are calculated for all files.
 */
public class HashedDirectory {
    private final Path dir;
    private final Path hashesPath;
    private final Map<Path, HashedFile> hashes;

    /**
     * Gets the path to the directory.
     * @return the directory path.
     */
    public Path getDir() {
        return dir;
    }

    /**
     * Gets the path to the files list.
     * @return the path to the list of files.
     */
    public Path getHashesPath() {
        return hashesPath;
    }

    /**
     * Creates a HashedDirectory object with supplied directory and supplied file
     * with hashes.
     * @param dir the path to the directory.
     * @param hashesPath the path to the file with hashes.
     */
    protected HashedDirectory(@Nonnull Path dir, @Nullable Path hashesPath) {
        this.dir = dir;
        this.hashesPath = hashesPath;
        hashes = new LinkedHashMap<>();
        try {
            if (hashesPath != null) {
                if (Files.exists(hashesPath)) {
                    for (String line : Files.readAllLines(hashesPath)) {
                        String[] parts = line.split(" ");
                        hashes.put(Paths.get(parts[0]),
                                new HashedFile(Paths.get(parts[0]), dir, parts[1]));
                    }
                } else {
                    Files.createFile(hashesPath);
                }
            }
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Removes all the files from the given directory.
     * @throws IOException if a deletion error occurs.
     */
    protected static void wipeDir(@Nonnull File dir) throws IOException {
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
    protected static void deleteDir(@Nonnull Path dir) throws IOException {
        wipeDir(dir.toFile());
        Files.delete(dir);
    }

    /**
     * Recursively deletes the given folder.
     * @param dir the path to the directory to delete.
     * @throws IOException if a deletion error occurs.
     */
    protected static void deleteDir(@Nonnull String dir) throws IOException {
        deleteDir(Paths.get(dir));
    }

    /**
     * Writes the file hashes to the hashes file.
     */
    protected void writeHashes() {
        if (hashesPath == null) {
            throw new IllegalStateException("Attempt to write hashes in a " +
                    "HashedDirectory without hashes list path specified.");
        }
        try {
            if (Files.exists(hashesPath)) {
                Files.delete(hashesPath);
            }
            BufferedWriter hashWriter = Files.newBufferedWriter(hashesPath,
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            hashes.forEach((path, s) -> {
                try {
                    hashWriter.write(path.toString() + ' ' + s.getHash() + '\n');
                } catch (IOException e) {
                    throw new VCS.FileSystemError("Error writing hashes to " +
                            path.toString());
                }
            });
            hashWriter.close();
        } catch (IOException e) {
            throw new VCS.FileSystemError(e.getMessage());
        }
    }

    /**
     * Gets a map with file descriptions.
     * @return a Map from file path in the directory to its hash and absolute path.
     */
    @Nonnull
    protected Map<Path, HashedFile> getFileDescriptions() {
        return hashes;
    }

    /**
     * Removes all files from the directory.
     */
    protected void clear() {
        try {
            wipeDir(dir.toFile());
            hashes.clear();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Copies a file represented by a HashedFile object into the folder.
     * @param file the file to copy.
     */
    protected void add(@Nonnull TrackedFile file) {
        try {
            Path newFilePath = dir.resolve(file.getName());

            Files.createDirectories(newFilePath.getParent());
            Files.copy(file.getLocation(), newFilePath,
                    StandardCopyOption.REPLACE_EXISTING);

            hashes.put(file.getName(), new HashedFile(file.getName(), dir, file.getHash()));
        } catch (IOException e) {
            throw new VCS.FileSystemError(e.getMessage());
        }
    }

    /**
     * Makes a stream containing all files from the directory in HashedFile form.
     * @return a stream with all files.
     */
    @Nonnull
    protected Stream<HashedFile> getFiles() {
        return hashes.values().stream();
    }

    /**
     * Deletes the file by the given path from the directory.
     * @param path the path pointing to the file that should be deleted.
     * @throws VCS.NoSuchFileException if the given path does not point to a file in
     * the directory.
     */
    protected void deleteFile(@Nonnull Path path) throws VCS.NoSuchFileException {
        if (Files.notExists(dir.resolve(path))) {
            throw new VCS.NoSuchFileException();
        }

        hashes.remove(path);
        try {
            Files.delete(dir.resolve(path));
        } catch (IOException e) {
            throw new VCS.FileSystemError(e.getMessage());
        }
    }

    /**
     * Checks if a file with the given path exists in the directory.
     * @param filePath the path to the file to check.
     * @return true if such file exists, false if it doesn't.
     */
    protected boolean contains(@Nonnull Path filePath) {
        return Files.isRegularFile(dir.resolve(filePath));
    }

    /**
     * Gets a HashedFile representation of a file from the directory by its path.
     * @param filePath the path to the file in directory.
     * @return a HashedFile representation of the file or null if the file doesn't exist.
     */
    @Nullable
    protected HashedFile getHashedFile(@Nonnull Path filePath) {
        return hashes.get(filePath);
    }
}
