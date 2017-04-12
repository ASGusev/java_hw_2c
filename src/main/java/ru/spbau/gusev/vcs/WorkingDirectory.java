package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class WorkingDirectory {
    private final Path workingDir;
    private final List<Path> ignoredPaths;
    private final static String IGNORE_LIST_FILENAME = ".ignore";

    WorkingDirectory(@Nonnull Path dir) {
        workingDir = dir;

        // Files with paths beginning with the paths listed in the .ignore file are
        // not tracked.
        ignoredPaths = new ArrayList<>();
        ignoredPaths.add(workingDir.resolve(Repository.REPO_DIR_NAME));
        Path ignoredListPath = workingDir.resolve(IGNORE_LIST_FILENAME);
        ignoredPaths.add(ignoredListPath);
        if (Files.exists(ignoredListPath)) {
            try {
                for (String line: Files.readAllLines(ignoredListPath)) {
                    try {
                        ignoredPaths.add(workingDir.resolve(line));
                    } catch (InvalidPathException e) {}
                }
            } catch (IOException e) {
                throw new VCS.FileSystemError();
            }
        }
    }

    /**
     * Copies the given TrackedFile to the working directory.
     * @param file the file to copy.
     */
    protected void add(@Nonnull TrackedFile file) {
        try {
            Files.createDirectories(workingDir.resolve(file.getName()).
                    toAbsolutePath().getParent());
            Files.copy(file.getLocation(), workingDir.resolve(file.getName()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Creates a HashedFile representation of a file in the working directory.
     * @param fileName the name of the file.
     * @return a HashedFile object pointing to the given file.
     * @throws VCS.NoSuchFileException if no file with the given name exists.
     */
    @Nonnull
    protected HashedFile getHashedFile(@Nonnull String fileName)
            throws VCS.NoSuchFileException {
        Path filePath = Paths.get(fileName);
        if (Files.notExists(workingDir.resolve(filePath))) {
            throw new VCS.NoSuchFileException();
        }

        return new HashedFile(filePath, workingDir);
    }

    /**
     * Deletes the file by the given path from the working directory.
     * @param path the path to the file to delete.
     * @return true if a file with given path was deleted, false if it didn't exist.
     */
    protected boolean delete(@Nonnull Path path) {
        path = workingDir.resolve(path);
        if (!Files.exists(path)) {
            return false;
        }

        try {
            if (Files.isRegularFile(path)) {
                Files.delete(path);
            } else {
                HashedDirectory.deleteDir(path);
            }
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        return true;
    }

    /**
     * Deletes files that satisfy the given predicate.
     * @param condition the predicate that returns true if a file should be deleted.
     */
    protected void deleteIf(@Nonnull Predicate<Path> condition) {
        try {
            Files.walk(workingDir)
                    .filter(Files::isRegularFile)
                    .filter(this::isNotIgnored)
                    .filter(path -> condition.test(workingDir.relativize(path)))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new VCS.FileSystemError();
                        }
                    });
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Creates a Stream of files in the directory.
     * @return a Stream of files in the directory.
     */
    @Nonnull
    protected Stream<HashedFile> getFiles() {
        try {
            return Files.walk(workingDir)
                    .filter(Files::isRegularFile)
                    .filter(this::isNotIgnored)
                    .map(path -> new HashedFile(workingDir.relativize(path), workingDir));
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    private boolean isNotIgnored(@Nonnull Path path) {
        for (Path ignored: ignoredPaths) {
            if (path.startsWith(ignored)) {
                return false;
            }
        }
        return true;
    }
}
