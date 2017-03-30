import java.nio.file.*;
import java.util.*;

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
    public static void createRepo(String author) throws RepoAlreadyExistsException {
        Repository.create(author);
    }

    /**
     * Sets the current username in the repository in the given value
     * @param name new username
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     */
    public static void setUserName(String name) throws BadRepoException {
        Repository.setUserName(name);
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
    public static void commit(String message) throws BadRepoException,
            BadPositionException {
        Repository.setCurrentCommit(new Commit(message));
    }

    /**
     * Adds a file to the stage zone. Files form the stage zone are included in the
     * next commit.
     * @param path the path to the file to be added.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchFileException if the given path does not lead to a file.
     */
    public static void addFile(String path) throws BadRepoException, NoSuchFileException {
        StagingZone.addFile(Paths.get(path));
    }

    /**
     * Creates a new branch with the given name.
     * @param branchName the name for the new branch.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws BranchAlreadyExistsException if a branch with this name already exists.
     */
    public static void createBranch(String branchName) throws BadRepoException,
            BranchAlreadyExistsException {
        Repository.setCurrentBranch(Branch.create(branchName));
    }

    /**
     * Deletes the branch with the given name.
     * @param branchName - the name of the branch to be deleted.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    public static void deleteBranch(String branchName) throws BadRepoException,
            NoSuchBranchException, BadPositionException {
        Branch.getByName(branchName).delete();
    }

    /**
     * Prepares a list with information about all the commits in a specified branch.
     * @return a list of CommitDescription objects representing commits of the specified branch.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    public static List<CommitDescription> getLog() throws BadRepoException,
            NoSuchBranchException {
        Branch curBranch = Repository.getCurBranch();
        return curBranch.getLog();
    }

    /**
     * Gets the number of the currently selected commit.
     * @return the number of the current commit
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     */
    public static int getCurCommit() throws BadRepoException {
        return Repository.getCurrentCommitNumber();
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
        Repository.checkoutCommit(commitID);
    }

    /**
     * Returns the working directory to the condition in which in was at the moment
     * of the last commit of a branch.
     * @param branchName the name of the required branch
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with given name does not exist.
     */
    public static void checkoutBranch(String branchName) throws BadRepoException,
            NoSuchBranchException {
        Branch newBranch = Branch.getByName(branchName);
        try {
            Repository.checkoutCommit(newBranch.getHeadNumber());
        } catch (NoSuchCommitException e) {
            throw new BadRepoException();
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
    public static void merge(String branchName) throws BadPositionException,
            BadRepoException, NoSuchBranchException {
        Merger.merge(Branch.getByName(branchName));
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

        CommitDescription(Commit commit) {
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
        public String getBranch() {
            return branch;
        }

        /**
         * Gets author of the commit.
         * @return the author of this commit.
         */
        public String getAuthor() {
            return author;
        }

        /**
         * Gets the message of the commit.
         * @return the commit message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Gets time of the commit creation.
         * @return a Calendar object representing the commit creation time.
         */
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
    public static class BadRepoException extends Exception{}

    /**
     * An error thrown if the filesystem throws an IOException.
     */
    public static class FileSystemError extends Error{}

    /**
     * An exception thrown if the specified file does not exist.
     */
    public static class NoSuchFileException extends Exception{}

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
    public static class BadPositionException extends Exception{}
}
