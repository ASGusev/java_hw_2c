package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A class representing the Staging zone. The function of the staging zone is
 * determining which files should be included in the next commit.
 */
public class StagingZone {
    private final HashedDirectory stageHashDir;

    /**
     * Creates a StagingZone Object with given stage path and files list path.
     * @param stagePath the path to the staging directory.
     * @param listPath the path to the list of staged files.
     */
    protected StagingZone(Path stagePath, Path listPath) throws
            VCS.NoSuchFileException {
        if (!Files.isDirectory(stagePath)) {
            throw new VCS.NoSuchFileException("Stage directory not found.");
        }
        if (!Files.isRegularFile(listPath)) {
            throw new VCS.NoSuchFileException("Stage list not found.");
        }
        stageHashDir = new HashedDirectory(stagePath, listPath);
    }

    /**
     * Adds a file to the staging directory, including it into the next commit.
     * @param file the file to add represented by a HashedFile object.
     */
    protected void add(@Nonnull TrackedFile file) {
        stageHashDir.add(file);
        stageHashDir.writeHashes();
    }

    /**
     * Removes all staged files from the staging zone.
     */
    protected void wipe() {
        stageHashDir.clear();
        stageHashDir.writeHashes();
    }

    /**
     * Creates a stream containing all files from staging directory as HashedFile objects.
     * @return a stream with all staged files.
     */
    protected Stream<HashedFile> getFiles() {
        return stageHashDir.getFiles();
    }

    /**
     * Removes the file pointed by given path from staging zone.
     * @param file the path pointing to the staged file to delete.
     * @return true if a file with the given path was removed from the staging zone,
     * false if it had not been staged.
     */
    protected boolean removeFile(@Nonnull Path file) {
        try {
            stageHashDir.deleteFile(file);
            stageHashDir.writeHashes();
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
    protected boolean contains(@Nonnull Path filePath) {
        return stageHashDir.contains(filePath);
    }

    /**
     * Gets a HashedFile representation of a file from the staging zone by its path.
     * @param filePath the path to the file.
     * @return a HashedFile representation of the file or null if the file doesn't exist.
     */
    @Nullable
    protected HashedFile getHashedFile(@Nullable Path filePath) {
        return stageHashDir.getHashedFile(filePath);
    }
}
