package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * A storage for files from IntersectedFolder-s. Only a single copy of a file is
 * stored regardless how many folders contain it. A file is deleted when no folders
 * link to it anymore.
 */
public class IntersectedFolderStorage {
    private final Path folder;
    private final Path counterList;
    private final Map<String,Integer> counters = new HashMap<>();

    /**
     * Creates a storage object.
     * @param folder the folder where files should be kept.
     * @param counterList the path to a file with a list of the storage content.
     */
    IntersectedFolderStorage(@Nonnull Path folder, @Nonnull Path counterList) {
        this.folder = folder;
        this.counterList = counterList;
        if (Files.isRegularFile(counterList)) {
            try {
                for (String line: Files.readAllLines(counterList)) {
                    String[] parts = line.split(" ");
                    if (parts.length != 2) {
                        throw new IllegalStateException("Incorrect " +
                                "IntersectedFolderStorage file list format.");
                    }
                    String hash = parts[0];
                    Integer counter = Integer.valueOf(parts[1]);
                    counters.put(hash, counter);
                }
            } catch (IOException e) {
                throw new VCS.FileSystemError(e.getMessage());
            }
        }
    }

    /**
     * Adds a file to the storage. The file is not copied if is already present in
     * the storage.
     * @param file the file to be added.
     * @return a TrackedFile representation of the file in the storage.
     */
    @Nonnull
    protected TrackedFile add(@Nonnull TrackedFile file) {
        Integer refsCounter = counters.getOrDefault(file.getHash(), 0);
        if (refsCounter.equals(0)) {
            Path newPath = folder.resolve(file.getHash());
            try {
                Files.copy(file.getLocation(), newPath);
            } catch (IOException e) {
                throw new VCS.FileSystemError(e.getMessage());
            }
        }
        counters.put(file.getHash(), ++refsCounter);
        return new SharedHashedFile(file.getHash(), file.getName());
    }

    /**
     * Gets a file from the storage.
     * @param hash the hash of the file.
     * @param name the path to th file in the working directory.
     * @return a TrackedFile representation of the required file.
     * @throws VCS.NoSuchFileException if a file with the given hash does not exist
     * in the storage.
     */
    @Nonnull
    protected TrackedFile getFile(@Nonnull String hash, @Nonnull Path name)
            throws VCS.NoSuchFileException {
        if (!Files.notExists(folder.resolve(name))) {
            throw new VCS.NoSuchFileException();
        }
        return new SharedHashedFile(hash, name);
    }

    /**
     * Writes file link counters to the disk.
     */
    protected void writeCounters() {
        try {
            if (Files.exists(counterList)) {
                Files.delete(counterList);
            }
            BufferedWriter countersWriter = Files.newBufferedWriter(counterList,
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            counters.forEach((hash, counter) -> {
                try {
                    countersWriter.write(hash + ' ' + counter.toString() + '\n');
                } catch (IOException e){
                    throw new VCS.FileSystemError(e.getMessage());
                }
            });
            countersWriter.close();
        } catch (IOException e) {
            throw new VCS.FileSystemError(e.getMessage());
        }
    }

    /**
     * Deletes a link to the file with the given hash. If no links to this file rest,
     * it is removed from the disk.
     * @param hash the hash of the file to remove.
     * @throws VCS.NoSuchFileException if a file with the given hash does not exist
     * in the repository.
     */
    protected void delete(@Nonnull String hash) throws VCS.NoSuchFileException {
        Integer counter = counters.get(hash);
        if (counter == null) {
            throw new VCS.NoSuchFileException();
        }

        counter--;
        if (counter.equals(0)) {
            counters.remove(hash);

            Path filePath = folder.resolve(hash);
            if (!Files.isRegularFile(filePath)) {
                throw new VCS.NoSuchFileException();
            }
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                throw new VCS.FileSystemError(e.getMessage());
            }
        } else {
            counters.put(hash, counter);
        }
    }

    private class SharedHashedFile implements TrackedFile {
        private final String hash;
        private final Path name;

        public SharedHashedFile(@Nonnull String hash, @Nonnull Path name) {
            this.hash = hash;
            this.name = name;
        }

        @Nonnull
        public String getHash() {
            return hash;
        }

        @Nonnull
        public Path getName() {
            return name;
        }

        @Nonnull
        public Path getLocation() {
            return folder.resolve(hash);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TrackedFile && hash.equals(((TrackedFile) o).getHash());
        }
    }
}
