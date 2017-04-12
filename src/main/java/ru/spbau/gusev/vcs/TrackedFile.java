package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * An interface representing a copy of a file in the repository.
 */
public interface TrackedFile {
    /**
     * Gets the hash of the file.
     * @return the hash of the file.
     */
    @Nonnull
    String getHash();

    /**
     * Gets the path to the file in the working directory as at the moment of the file
     * addition to the repository.
     * @return the path to the file from the working directory.
     */
    @Nonnull
    Path getName();

    /**
     * Gets a path to the current real location of the file.
     * @return a path leading to a copy of the file on the disk.
     */
    @Nonnull
    Path getLocation();
}
