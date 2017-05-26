package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A folder that may intersect with other folders. Files present in several folders
 * linking to one storage are only stored once.
 */
public class IntersectedFolder {
    private final IntersectedFolderStorage storage;
    private final Path listPath;
    private final Map<Path, TrackedFile> files = new HashMap<>();

    /**
     * Creates a IntersectedFolder with the given storage and path to files list.
     * @param storage the storage where files should be kept.
     * @param listPath the path to the file with a list of the folder content.
     */
    IntersectedFolder(@Nonnull IntersectedFolderStorage storage,
                      @Nonnull Path listPath) {
        this.storage = storage;
        this.listPath = listPath;
        if (Files.isRegularFile(listPath)) {
            try {
                for (String line: Files.readAllLines(listPath)) {
                    String[] parts = line.split(" ");
                    if (parts.length != 2) {
                        throw new IllegalStateException("Incorrect intersected " +
                                "folder files list format");
                    }
                    Path name = Paths.get(parts[0]);
                    String hash = parts[1];
                    files.put(name, storage.getFile(hash, name));
                }
            } catch (IOException e) {
                throw new VCS.FileSystemError(e.getMessage());
            } catch (VCS.NoSuchFileException e) {
                throw new IllegalStateException("Intersected folder file not found");
            }
        }
    }

    /**
     * Adds a file to the directory.
     * @param file the file to add.
     */
    protected void add(@Nonnull TrackedFile file) {
        files.put(file.getName(), storage.add(file));
    }

    /**
     * Picks a file from the folder.
     * @param name the path to the file in the working directory.
     * @return a TrackedFile Object representing the required file.
     */
    @Nullable
    protected TrackedFile getFile(@Nonnull Path name) {
        return files.get(name);
    }

    /**
     * Deletes a file from the folder.
     * @param name the path to the file in the  working directory.
     * @throws VCS.NoSuchFileException if a file with the given name does not exist in
     * the folder.
     */
    protected void delete(@Nonnull Path name) throws VCS.NoSuchFileException {
        storage.delete(files.get(name).getHash());
        files.remove(name);
    }

    /**
     * Writes the list of files to the disk.
     */
    protected void writeList() {
        try {
            if (Files.exists(listPath)) {
                Files.delete(listPath);
            }
            BufferedWriter listWriter = Files.newBufferedWriter(listPath, StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);
            files.forEach((path, sharedHashedFile) -> {
                try {
                    listWriter.write(path + " " + sharedHashedFile.getHash() + "\n");
                } catch (IOException e) {
                    throw new VCS.FileSystemError(e.getMessage());
                }
            });
            listWriter.close();
            storage.writeCounters();
        } catch (IOException e) {
            throw new VCS.FileSystemError(e.getMessage());
        }
    }

    /**
     * Creates a stream containing all files in the folder.
     * @return a Stream object containing TrackedFile representations of all files in
     * the folder.
     */
    @Nonnull
    protected Stream<TrackedFile> getFiles() {
        return files.values().stream();
    }

    /**
     * Checks if a file with given name exists in the folder.
     * @param fileName the name of the file to check.
     * @return true if a file with the given name exists in the folder, false if it
     * doesn't.
     */
    protected boolean contains(@Nonnull Path fileName) {
        return files.containsKey(fileName);
    }
}
