package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An class containing VCS public API methods.
 */
public class VCS {
    /**
     * Initialises a repository in the current directory. A folder with all the
     * necessary information is created.
     * @param author the first username for the created repo
     * @throws RepoAlreadyExistsException if a repo is already initialised in current folder
     */
    public static void createRepo(@Nonnull String author) throws RepoAlreadyExistsException {
        Repository.create(author);
    }

    /**
     * Sets the current username in the repository in the given value
     * @param name new username
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     */
    public static void setUserName(@Nonnull String name) throws BadRepoException {
        Repository.getExisting().setUserName(name);
    }

    /**
     * Creates a new commit with a supplied message including all the changes
     * that have been added to the stage.
     * @param message the message that will be added to commit
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws BadPositionException if the current commit is not the head of the
     * current branch.
     */
    public static void commit(@Nonnull String message) throws BadRepoException,
            BadPositionException, NothingToCommitException {
        Repository repository = Repository.getExisting();
        if (getStaged().isEmpty()) {
            throw new NothingToCommitException();
        }
        repository.setCurrentCommit(Commit.create(message, repository));
    }

    /**
     * Adds a file to the stage zone. Files form the stage zone are included in the
     * next commit.
     * @param path the path to the file to be added.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchFileException if the given path does not lead to a file.
     */
    public static void addFile(@Nonnull String path) throws BadRepoException, NoSuchFileException {
        Repository repository = Repository.getExisting();
        HashedFile file = repository.getWorkingDirectory().getHashedFile(path);
        repository.getStagingZone().add(file);
    }

    /**
     * Creates a new branch with the given name.
     * @param branchName the name for the new branch.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws BranchAlreadyExistsException if a branch with this name already exists.
     */
    public static void createBranch(@Nonnull String branchName)
            throws BadRepoException, BranchAlreadyExistsException {
        Repository repository = Repository.getExisting();
        repository.setCurrentBranch(Branch.create(branchName,
                repository.getCurrentCommitNumber(), repository));
    }

    /**
     * Deletes the branch with the given name.
     * @param branchName - the name of the branch to be deleted.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    public static void deleteBranch(@Nonnull String branchName) throws BadRepoException,
            NoSuchBranchException, BadPositionException {
        Repository repository = Repository.getExisting();
        Branch branchToDelete = Branch.getByName(branchName, repository);
        if (!branchToDelete.getName().equals(Repository.DEFAULT_BRANCH) &&
                branchToDelete.equals(repository.getCurBranch())) {
            branchToDelete.delete();
        }
    }

    /**
     * Prepares a list with information about all the commits in a specified branch.
     * @return a list of CommitDescription objects representing commits of the specified branch.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    @Nonnull
    public static List<CommitDescription> getLog() throws BadRepoException,
            NoSuchBranchException {
        Branch curBranch = Repository.getExisting().getCurBranch();
        return curBranch.getLog();
    }

    /**
     * Returns the directory to the condition in which it was at the moment of the
     * specified commit.
     * @param commitID the number of the required commit.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchCommitException if a commit with the given number does
     * not exist.
     */
    public static void checkoutCommit(int commitID) throws BadRepoException,
            NoSuchCommitException {
        Repository repository = Repository.getExisting();
        repository.checkoutCommit(commitID);
    }

    /**
     * Returns the working directory to the condition in which in was at the moment
     * of the last commit of a branch.
     * @param branchName the name of the required branch
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with given name does not exist.
     */
    public static void checkoutBranch(@Nonnull String branchName) throws BadRepoException,
            NoSuchBranchException {
        Repository repository = Repository.getExisting();
        Branch newBranch = Branch.getByName(branchName, repository);
        try {
            repository.checkoutCommit(newBranch.getHeadNumber());
            repository.setCurrentBranch(newBranch);
        } catch (NoSuchCommitException e) {
            throw new BadRepoException("Branch's head commit not found.");
        }
    }

    /**
     * Merges the specified branch into current. The changes if the merged branch
     * have higher priority than in the current.
     * @param branchName the name of the branch to merge
     * @throws BadPositionException if the current position is not a branch head.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    public static void merge(@Nonnull String branchName) throws BadPositionException,
            BadRepoException, NoSuchBranchException {
        Repository repo = Repository.getExisting();
        Merger.merge(repo, Branch.getByName(branchName, repo));
    }

    /**
     * Removes file with given name from the working directory and staging zone.
     * @param filename name of the file to remove.
     * @throws NoSuchFileException if a file with the given name does not exist
     * @throws BadRepoException if the repository folder is corrupt.
     */
    public static void remove(@Nonnull String filename) throws NoSuchFileException,
            BadRepoException {
        Path filePath = Paths.get(filename);
        Repository repository = Repository.getExisting();

        if (!repository.getStagingZone().removeFile(filePath)) {
            throw new NoSuchFileException();
        }
        repository.getWorkingDirectory().delete(filePath);
    }

    /**
     * Returns the file with given name to its stage in the current commit.
     * @param filename the name of file to reset.
     */
    public static void reset(@Nonnull String filename) throws BadRepoException,
            NoSuchFileException {
        Repository repository = Repository.getExisting();
        Path filePath = Paths.get(filename);
        repository.getCurrentCommit().resetFile(filePath,
                repository.getWorkingDirectory(), repository.getStagingZone());
    }

    /**
     * Removes all the files that have not been added to the repository from
     * the working directory.
     */
    public static void clean() throws BadRepoException {
        Repository repository = Repository.getExisting();
        final StagingZone stagingZone = repository.getStagingZone();
        repository.getWorkingDirectory().deleteIf(file -> !stagingZone.contains(file));
    }

    /**
     * Lists all staged files that are not present in the last commit in their staged
     * condition.
     * @return a list containing names of all staged files.
     * @throws VCS.BadRepoException if the repository data folder is corrupt.
     */
    @Nonnull
    public static List<String> getStaged() throws BadRepoException {
        Repository repository = Repository.getExisting();
        Commit currentCommit = repository.getCurrentCommit();
        return repository.getStagingZone().getFiles()
                .filter(file -> {
                    TrackedFile fileInCommit = currentCommit.getFile(file.getName());
                    return  (fileInCommit == null || !file.equals(fileInCommit));
                })
                .map(HashedFile::toString)
                .collect(Collectors.toList());
    }

    /**
     * Lists all the files that have been added to the repository but are in a different
     * condition at the moment of method call.
     * @return a list containing names of all changed files.
     */
    @Nonnull
    public static List<String> getChanged() throws BadRepoException {
        Repository repository = Repository.getExisting();
        final StagingZone stagingZone = repository.getStagingZone();
        return repository.getWorkingDirectory().getFiles()
                .filter(file -> {
                    HashedFile stagedFile = stagingZone.getHashedFile(file.getName());
                    return stagedFile != null && !file.equals(stagedFile);
                })
                .map(HashedFile::toString)
                .collect(Collectors.toList());
    }

    /**
     * Lists all the files in the working directory that have not been added to the
     * repository.
     * @return a List containing names of all files that are not in the repository.
     */
    @Nonnull
    public static List<String> getCreated() throws BadRepoException {
        Repository repository = Repository.getExisting();
        final StagingZone stagingZone = repository.getStagingZone();
        return repository.getWorkingDirectory().getFiles()
                .filter(file -> !stagingZone.contains(file.getName()))
                .map(HashedFile::toString)
                .collect(Collectors.toList());
    }

    /**
     * Lists files that have been removed since the creation of the current commit.
     * @return a list with all removed files.
     */
    @Nonnull
    public static List<String> getRemoved() throws BadRepoException {
        Repository repository = Repository.getExisting();
        return repository.getCurrentCommit().getRemovedFiles(
                repository.getStagingZone());
    }

    /**
     * Gets name of current branch.
     * @return the name of the current branch.
     * @throws BadRepoException if the repository folder is corrupt.
     */
    @Nonnull
    public static String getCurBranch() throws BadRepoException {
        return Repository.getExisting().getCurBranch().getName();
    }

    /**
     * Gets a list of all existing branch names.
     * @return a list containing names of all branches.
     */
    @Nonnull
    public static List<String> getBranchNames() {
        return Repository.getExisting().getBranchNames();
    }

    /**
     * A class representing a commit object for interface.
     */
    public static class CommitDescription {
        private int number;
        private String branch;
        private String author;
        private String message;
        private Calendar time;

        CommitDescription(@Nonnull Commit commit) {
            this.number = commit.getNumber();
            this.branch = commit.getBranch().getName();
            this.author = commit.getAuthor();
            this.message = commit.getMessage();
            Calendar.Builder builder = new Calendar.Builder();
            builder.setInstant(commit.getCreationTime());
            time = builder.build();
        }

        /**
         * Get the commit number.
         * @return commit number.
         */
        public int getNumber() {
            return number;
        }

        /**
         * Get the commit's branch
         * @return the branch that the commit belongs to.
         */
        @Nonnull
        public String getBranch() {
            return branch;
        }

        /**
         * Gets author of the commit.
         * @return the author of this commit.
         */
        @Nonnull
        public String getAuthor() {
            return author;
        }

        /**
         * Gets the message of the commit.
         * @return the commit message
         */
        @Nonnull
        public String getMessage() {
            return message;
        }

        /**
         * Gets time of the commit creation.
         * @return a Calendar object representing the commit creation time.
         */
        @Nonnull
        public Calendar getTime() {
            return time;
        }
    }

    /**
     * An exception thrown in case of an attempt to create a repository where
     * it already exists.
     */
    public static class RepoAlreadyExistsException extends Exception{}

    /**
     * An exception thrown if a branch required to create already exists.
     */
    public static class BranchAlreadyExistsException extends Exception{}

    /**
     * An exception thrown if the repository service folder is damaged.
     */
    public static class BadRepoException extends Exception {
        BadRepoException() {}

        BadRepoException(String message) {
            super(message);
        }
    }

    /**
     * An error thrown if the filesystem throws an IOException.
     */
    public static class FileSystemError extends Error {
        public FileSystemError() {
        }

        public FileSystemError(String message) {
            super(message);
        }

        public FileSystemError(Throwable cause) {
            super(cause);
        }
    }

    /**
     * An exception thrown if the specified file does not exist.
     */
    public static class NoSuchFileException extends Exception {
        public NoSuchFileException() {
        }

        public NoSuchFileException(String message) {
            super(message);
        }
    }

    /**
     * An exception thrown if the specified branch does not exist.
     */
    public static class NoSuchBranchException extends Exception{}

    /**
     * An exception thrown if the specified commit does not exist.
     */
    public static class NoSuchCommitException extends Exception{}

    /**
     * An exception thrown if an operation is impossible because of the head
     * position.
     */
    public static class BadPositionException extends Exception {
        public BadPositionException() {
        }

        public BadPositionException(String message) {
            super(message);
        }
    }

    /**
     * An exception thrown in case of an attempt to make a commit with no changes since
     * the previous one.
     */
    public static class NothingToCommitException extends Exception{};
}
