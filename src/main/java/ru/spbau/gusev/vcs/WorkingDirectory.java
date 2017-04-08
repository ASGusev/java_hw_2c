package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class WorkingDirectory {
    private final static Path WORKING_DIR = Paths.get(".");
    private final static Map<Path, HashedFile> hashedFiles = new HashMap<>();
    private final static List<Path> ignoredPaths;
    private final static String IGNORE_LIST_FILENAME = ".ignore";
    private final static Path IGNORE_LIST_PATH = Paths.get(IGNORE_LIST_FILENAME);

    static {
        //Files with paths beginning with the paths listed in the .ignore file are not tracked.
        ignoredPaths = new ArrayList<>();
        ignoredPaths.add(Paths.get(".", Repository.REPO_DIR_NAME));
        ignoredPaths.add(Paths.get(".", IGNORE_LIST_FILENAME));
        if (Files.exists(IGNORE_LIST_PATH)) {
            try {
                for (String line: Files.readAllLines(IGNORE_LIST_PATH)) {
                    try {
                        ignoredPaths.add(Paths.get(".", line));
                    } catch (InvalidPathException e) {}
                }
            } catch (IOException e) {
                throw new VCS.FileSystemError();
            }
        }
    }

    /**
     * Copies the given HashedFile to the working directory.
     * @param file the file to copy.
     */
    protected static void addFile(@Nonnull HashedFile file) {
        try {
            Files.createDirectories(WORKING_DIR.resolve(file.getPath()).
                    toAbsolutePath().getParent());
            Files.copy(file.getFullPath(), file.getPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            hashedFiles.put(file.getPath(), new HashedFile(file.getPath(), WORKING_DIR,
                    file.getHash()));
        } catch (IOException e) {
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
    protected static HashedFile getHashedFileByName(@Nonnull String fileName)
            throws VCS.NoSuchFileException {
        Path filePath = Paths.get(fileName);
        if (Files.notExists(filePath)) {
            throw new VCS.NoSuchFileException();
        }

        return new HashedFile(filePath, WORKING_DIR);
    }

    /**
     * Deletes the file by the given path from the working directory.
     * @param filePath the path to the file to delete.
     * @return true if a file with given path was deleted, false if it didn't exist.
     */
    protected static boolean deleteFile(@Nonnull Path filePath) {
        if (!Files.exists(filePath)) {
            return false;
        }

        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        return true;
    }

    /**
     * Checks if a file with given path exists in the working directory.
     * @param filePath the path to file to check.
     * @return true if such file exists, false if it doesn't or is not a regular file.
     */
    protected static boolean contains(@Nonnull Path filePath) {
        return Files.isRegularFile(filePath);
    }

    /**
     * Deletes files that satisfy the given predicate.
     * @param condition the predicate that returns true if a file should be deleted.
     */
    protected static void removeIf(@Nonnull Predicate<Path> condition) {
        try {
            Files.walk(WORKING_DIR)
                    .filter(Files::isRegularFile)
                    .filter(WorkingDirectory::isNotIgnored)
                    .filter(condition)
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
     * Lists all files in the working directory that satisfy the given predicate.
     * @param condition the predicate that return true if the given file should be
     *                  listed.
     * @return a List containing all the files on which the given predicate returns
     * true.
     */
    @Nonnull
    protected static List<String> getSatisfying(Predicate<Path> condition) {
        try {
            return Files.walk(WORKING_DIR)
                    .filter(Files::isRegularFile)
                    .filter(WorkingDirectory::isNotIgnored)
                    .filter(condition)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Creates a Stream of files in the directory.
     * @return a Stream of files in the directory.
     */
    @Nonnull
    protected static Stream<HashedFile> getFiles() {
        try {
            return Files.walk(WORKING_DIR)
                    .filter(Files::isRegularFile)
                    .filter(WorkingDirectory::isNotIgnored)
                    .map(path -> new HashedFile(path, WORKING_DIR));
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    private static boolean isNotIgnored(Path path) {
        for (Path ignored: ignoredPaths) {
            if (path.startsWith(ignored)) {
                return false;
            }
        }
        return true;
    }
}
