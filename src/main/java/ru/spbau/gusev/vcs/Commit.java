package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class representing a commit in the repository.
 */
public class Commit {
    private static final String COMMIT_METADATA_FILE = "metadata";
    private static final String COMMIT_FILES_LIST = "files_list";

    private final Integer number;
    private final long creationTime;
    private final String message;
    private final Branch branch;
    private final String author;
    private final Integer father;
    private final Path rootDir;
    private final IntersectedFolder contentFolder;

    /**
     * Creates a new commit in the repository with given message in the current
     * branch and sets it as the global head.
     * @param message the commit message.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     * @throws VCS.BadPositionException if current commit is not the head of its branch.
     */
    protected Commit(@Nonnull String message) throws VCS.BadRepoException,
            VCS.BadPositionException {
        if (Files.notExists(Paths.get(Repository.REPO_DIR_NAME,
                Repository.COMMITS_DIR_NAME))) {
            throw new VCS.BadRepoException("Commits directory not found.");
        }
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Empty commit message.");
        }

        number = Repository.getCommitsNumber();
        rootDir = Paths.get(Repository.REPO_DIR_NAME, Repository.COMMITS_DIR_NAME,
                number.toString());
        creationTime = System.currentTimeMillis();
        this.message = message;
        branch = Repository.getCurBranch();
        author = Repository.getUserName();
        father = Repository.getCurrentCommitNumber();

        if (!branch.getHeadNumber().equals(father)) {
            throw new VCS.BadPositionException("Attempt to create a commit not in " +
                    "the head of a branch.");
        }

        try {
            Files.createDirectory(rootDir);

            BufferedWriter metadataWriter = new BufferedWriter(new FileWriter(
                    rootDir.resolve(COMMIT_METADATA_FILE).toString()));
            metadataWriter.write(String.valueOf(creationTime) + '\n');
            metadataWriter.write(branch.getName() + '\n');
            metadataWriter.write(author + '\n');
            metadataWriter.write(father.toString() + '\n');
            metadataWriter.write(message);
            metadataWriter.close();

            contentFolder = new IntersectedFolder(Repository.getCommitStorage(),
                    rootDir.resolve(COMMIT_FILES_LIST));
            Repository.getStagingZone().getFiles().forEach(contentFolder::add);
            contentFolder.writeList();

            branch.addCommit(this);
            Repository.setCurrentCommit(this);
            Repository.updateCommitCounter(number + 1);
        } catch (IOException | VCS.FileSystemError e) {
            try {
                HashedDirectory.deleteDir(rootDir);
            } catch (IOException e1) {}
            throw new VCS.FileSystemError("Error writing new commit data.");
        }
    }

    /**
     * Reads an already existing commit from the repository.
     * @param number the number of commit to be read.
     * @throws VCS.NoSuchCommitException if a commit with the given number does not
     * exist.
     * @throws VCS.BadRepoException if the repository data folder is corrupt.
     */
    protected Commit(@Nonnull Integer number) throws VCS.NoSuchCommitException,
            VCS.BadRepoException {
        this.number = number;
        rootDir = Paths.get(Repository.REPO_DIR_NAME, Repository.COMMITS_DIR_NAME,
                number.toString());

        if (Files.notExists(rootDir)) {
            throw new VCS.NoSuchCommitException();
        }

        try (Scanner metadataScanner =
                     new Scanner(rootDir.resolve(COMMIT_METADATA_FILE))) {
            creationTime = metadataScanner.nextLong();
            branch = Branch.getByName(metadataScanner.next());
            author = metadataScanner.next();
            father = Integer.valueOf(metadataScanner.next());

            StringBuilder messageBuilder = new StringBuilder();
            metadataScanner.nextLine();
            messageBuilder.append(metadataScanner.nextLine());
            while (metadataScanner.hasNext()) {
                messageBuilder.append('\n');
                messageBuilder.append(metadataScanner.nextLine());
            }
            message = messageBuilder.toString();

            contentFolder = new IntersectedFolder(Repository.getCommitStorage(),
                    rootDir.resolve(COMMIT_FILES_LIST));
        } catch (IOException e) {
            throw new VCS.FileSystemError("Error reading commit data.");
        } catch (VCS.NoSuchBranchException e) {
            throw new VCS.BadRepoException("Requested commit not found.");
        }
    }

    /**
     * Gets the commit number.
     * @return the commit number.
     */
    @Nonnull
    protected Integer getNumber() {
        return number;
    }

    /**
     * Gets the commit creation time. Time is measured in millisecond from the beginning
     * of the UNIX epoch.
     * @return the commit creation time.
     */
    @Nonnull
    protected long getCreationTime() {
        return creationTime;
    }

    /**
     * Gets the commit message.
     * @return the commit message.
     */
    @Nonnull
    protected String getMessage() {
        return message;
    }

    /**
     * Gets the branch that this commit belongs to.
     * @return the branch that this commit belongs to.
     */
    @Nonnull
    protected Branch getBranch() {
        return branch;
    }

    /**
     * Gets the commit author's username.
     * @return the commit author's username.
     */
    @Nonnull
    protected String getAuthor() {
        return author;
    }

    /**
     * Gets the parental commit, the commit which was the global head before this
     * commit creation.
     * @return the parental commit.
     */
    @Nonnull
    protected Commit getFather() throws VCS.BadRepoException {
        try {
            return new Commit(father);
        } catch (VCS.NoSuchCommitException e) {
            throw new VCS.BadRepoException("Error reading commit's father.");
        }
    }

    /**
     * Removes all the commit's files from the given working directory.
     * @param directory the directory from which the commit files should be removed.
     */
    protected void removeFrom(@Nonnull WorkingDirectory directory) {
        contentFolder.getFiles()
                .forEach(file -> directory.delete(file.getName()));
    }

    /**
     * Copies all the files from the commit to the given working directory and sets
     * this commit as global head.
     * @param directory the directory which the commit files should be copied to.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected void checkout(@Nonnull WorkingDirectory directory) throws
            VCS.BadRepoException {
        contentFolder.getFiles().forEach(directory::add);

        final StagingZone stagingZone = Repository.getStagingZone();
        contentFolder.getFiles().forEach(stagingZone::add);

        Repository.setCurrentCommit(this);
    }

    /**
     * Restores the history between the initial commit and the current one.
     * @return a list containing all current commit's predessors sorted by
     * creation time.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    @Nonnull
    protected List<Commit> getPedigree() throws VCS.BadRepoException {
        ArrayList<Commit> pedigree = new ArrayList<>();
        pedigree.add(this);
        Commit pos = this;
        while (!pos.getNumber().equals(0)) {
            pos = pos.getFather();
            pedigree.add(pos);
        }
        Collections.reverse(pedigree);
        return pedigree;
    }

    /**
     * Compares two Commit objects. Two commit objects are considered equal if their
     * numbers coincide.
     * @param o the object to compare with.
     * @return true if the given object is an equal Commit object, false if it is not
     * equal or is not a Commit object.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Commit && ((Commit)o).number.equals(this.number);
    }

    /**
     * Gets a stream with all the files in the commit.
     * @return a Stream with TrackedFile objects for all files in the repository.
     */
    @Nonnull
    public Stream<TrackedFile> getFiles() {
        return contentFolder.getFiles();
    }

    /**
     * Restores file with given path in the working directory to the its condition at
     * the moment of commit creation.
     * @param filePath the path to the file to restore.
     * @param directory the directory where the file should be reset.
     */
    protected void resetFile(@Nonnull Path filePath,
                             @Nonnull WorkingDirectory directory,
                             @Nonnull StagingZone stagingZone) {
        if (contentFolder.contains(filePath)) {
            TrackedFile file = contentFolder.getFile(filePath);
            stagingZone.add(file);
            directory.add(file);
        } else {
            stagingZone.removeFile(filePath);
            directory.delete(filePath);
        }
    }

    /**
     * Gets a HashedFile representation of a file from this commit by its path.
     * @param filePath the path to the file.
     * @return a HashedFile representation of the file or null if the file doesn't exist.
     */
    @Nullable
    protected TrackedFile getFile(@Nonnull Path filePath) {
        if (!contentFolder.contains(filePath)) {
            return null;
        }

        return contentFolder.getFile(filePath);
    }

    /**
     * Lists all the files that have been removed from the repository since creation of
     * this commit.
     * @return a List with all removed files.
     */
    @Nonnull
    protected List<String> getRemovedFiles() throws VCS.BadRepoException {
        final StagingZone stagingZone = Repository.getStagingZone();
        return contentFolder.getFiles()
                .filter(file -> !stagingZone.contains(file.getName()))
                .map(TrackedFile::getName)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    /**
     * Deletes the commit from the repository.
     */
    protected void delete() {
        contentFolder.getFiles()
                .forEach(file -> {
                    try {
                        contentFolder.delete(file.getName());
                    } catch (VCS.NoSuchFileException e) {
                        throw new VCS.FileSystemError("A file from commit cannot " +
                                "be found.");
                    }
                });
        try {
            HashedDirectory.deleteDir(rootDir);
        } catch (IOException e) {
            throw new VCS.FileSystemError("Error deleting commit's folder.");
        }
    }
}
