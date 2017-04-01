package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class representing the Staging zone. The function of the staging zone is
 * determining which files should be included in the next commit.
 */
public abstract class StagingZone {
    protected static final String STAGE_DIR = "stage";
    protected static final String STAGE_LIST = "stage_list";
    private static final Path STAGE_PATH =
            Paths.get(Repository.REPO_DIR_NAME, STAGE_DIR);
    private static final Path LIST_PATH =
            Paths.get(Repository.REPO_DIR_NAME, STAGE_LIST);
    private static final HashedDirectory STAGE_HASH_DIR =
            new HashedDirectory(STAGE_PATH, LIST_PATH);

    /**
     * Adds a file to the staging directory, including it into the next commit.
     * @param filePath the path to the file to add.
     * @throws VCS.NoSuchFileException if the supplied path does not lead to a
     * correct file.
     */
    protected static void addFile(@Nonnull Path filePath) throws VCS.NoSuchFileException {
        addFile(WorkingDirectory.getHashedFileByName(filePath.toString()));
    }

    /**
     * Adds a file to the staging directory, including it into the next commit.
     * @param file the file to add represented by a HashedFile object.
     */
    protected static void addFile(@Nonnull HashedFile file) {
        STAGE_HASH_DIR.copyFile(file);
        STAGE_HASH_DIR.flushHashes();
    }

    /**
     * Removes all staged files from the staging zone.
     */
    protected static void wipe() {
        STAGE_HASH_DIR.clear();
        STAGE_HASH_DIR.flushHashes();
    }

    /**
     * Copies all files from the given directory to the staging zone.
     * @param dir the directory which contains files to be staged.
     */
    protected static void cloneDir(@Nonnull HashedDirectory dir) {
        wipe();
        STAGE_HASH_DIR.cloneDirectory(dir);
        STAGE_HASH_DIR.flushHashes();
    }

    /**
     * Creates a stream containing all files from staging directory as HashedFile objects.
     * @return a stream with all staged files.
     */
    protected static Stream<HashedFile> getFiles() {
        return STAGE_HASH_DIR.getFiles();
    }

    /**
     * Removes the file pointed by given path from staging zone.
     * @param file the path pointing to the staged file to delete.
     * @return true if a file with the given path was removed from the staging zone,
     * false if it had not been staged.
     */
    protected static boolean removeFile(@Nonnull Path file) throws VCS.BadRepoException {
        if (!Files.exists(STAGE_PATH)) {
            throw new VCS.BadRepoException();
        }

        try {
            STAGE_HASH_DIR.deleteFile(file);
            STAGE_HASH_DIR.flushHashes();
        } catch (VCS.NoSuchFileException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a file with given path is in the staging zone.
     * @param filePath the path to check.
     * @return true if a file with given path exists in the staging zone, false otherwise.
     */
    protected static boolean contains(@Nonnull Path filePath) {
        return STAGE_HASH_DIR.contains(filePath);
    }

    /**
     * Lists all staged files that are not present in the last commit in their staged
     * condition.
     * @return a list containing names of all staged files.
     * @throws VCS.BadRepoException if the repository data folder is corrupt.
     */
    @Nonnull
    protected static List<String> getStagedFiles() throws VCS.BadRepoException {
        Commit headCommit = Repository.getCurrentCommit();
        return STAGE_HASH_DIR.getFiles()
                .filter(file -> !file.equals(headCommit.getHashedFile(file.getPath())))
                .map(HashedFile::getPath)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    /**
     * Gets a HashedFile representation of a file from the staging zone by its path.
     * @param filePath the path to the file.
     * @return a HashedFile representation of the file or null if the file doesn't exist.
     */
    @Nullable
    protected static HashedFile getHashedFile(@Nullable Path filePath) {
        return STAGE_HASH_DIR.getHashedFile(filePath);
    }
}
