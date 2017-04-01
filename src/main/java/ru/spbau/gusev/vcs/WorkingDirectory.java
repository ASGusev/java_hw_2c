package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WorkingDirectory {
    private final static Path WORKING_DIR = Paths.get(".");
    private final static Map<Path, HashedFile> hashedFiles = new HashMap<>();
    private final static List<Path> ignoredPaths;
    private final static String IGNORE_LIST_FILENAME = ".ignore";
    private final static Path IGNORE_LIST_PATH = Paths.get(IGNORE_LIST_FILENAME);

    static {
        ignoredPaths = new ArrayList<>();
        ignoredPaths.add(Paths.get(Repository.REPO_DIR_NAME));
        if (Files.exists(IGNORE_LIST_PATH)) {
            try {
                for (String line: Files.readAllLines(IGNORE_LIST_PATH)) {
                    try {
                        ignoredPaths.add(Paths.get(line));
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
     * @throws VCS.NoSuchFileException if a file with the given path does not exist.
     */
    protected static void deleteFile(@Nonnull Path filePath) throws VCS.NoSuchFileException {
        if (!Files.exists(filePath)) {
            throw new VCS.NoSuchFileException();
        }

        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Checks if a file with given path exists in the working directory.
     * @param filePath the path to file to check.
     * @return true if such file exists, false if it doesn't or is not a regular file.
     */
    protected static boolean contains(@Nonnull Path filePath) {
        return Files.isRegularFile(filePath);
    }
}
