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
     * Copies a file to the directory.
     * @param dirPath the path to a working dir containing file.
     * @param filePath the resting path from the working dir to the file to copy.
     * @throws VCS.NoSuchFileException in case the provided path does not lead to a
     * valid file.
     */
    void addFile(@Nonnull Path dirPath, @Nonnull Path filePath) throws VCS.NoSuchFileException {
        try {
            if (!Files.isRegularFile(dirPath.resolve(filePath))) {
                throw new VCS.NoSuchFileException();
            }

            Files.createDirectories(dir.resolve(filePath).getParent());
            Files.copy(dirPath.resolve(filePath), dir.resolve(filePath),
                    StandardCopyOption.REPLACE_EXISTING);

            hashes.put(filePath,
                    new HashedFile(Paths.get(dir.resolve(filePath).toString()), dir));
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Copies the provided directory content into the current folder.
     * @param src the directory to be copied.
     */
    protected void cloneDirectory(@Nonnull HashedDirectory src) {
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
        if (hashesPath == null) {
            throw new IllegalStateException();
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
     * Recalculates hashes for all files in the directory.
     */
    protected void updateHashes() {
        hashes.clear();
        try {
            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        hashes.put(path, new HashedFile(dir, path));
                    });
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Copies a file represented by a HashedFile object into the folder.
     * @param file the file to copy.
     */
    protected void copyFile(@Nonnull HashedFile file) {
        try {
            Path newFilePath = dir.resolve(file.getPath());

            Files.createDirectories(newFilePath.getParent());
            Files.copy(file.getFullPath(), newFilePath,
                    StandardCopyOption.REPLACE_EXISTING);

            hashes.put(file.getPath(), new HashedFile(file.getPath(), dir, file.getHash()));
        } catch (IOException e) {
            throw new VCS.FileSystemError();
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
            throw new VCS.FileSystemError();
        }
    }
}
