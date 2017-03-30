import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;

/**
 * A class with methods for operating with the repository.
 */
public abstract class Repository {
    protected static final String REPO_DIR_NAME = ".vcs";
    protected static final String BRANCHES_DIR_NAME = "branches";
    protected static final String COMMITS_DIR_NAME = "commits";
    protected static final String USERNAME_FILE = "user";
    protected static final String COMMITS_COUNTER_FILENAME = "commit";
    protected static final String POSITION_FILENAME = "position";
    protected static final String STAGE_DIR = "stage";
    protected static final String DEFAULT_BRANCH = "master";
    protected static final String WORKING_DIR_HASHES = "hashes";
    private static HashedDirectory workingDirectory;

    /**
     * Initialises a repository in the current directory. A folder with all the
     * necessary information is created.
     * @param author the first username for the created repo
     * @throws VCS.RepoAlreadyExistsException if a repo is already initialised in current folder
     */
    protected static void create(String author) throws VCS.RepoAlreadyExistsException {
        if (Files.exists(Paths.get(Repository.REPO_DIR_NAME), LinkOption.NOFOLLOW_LINKS)) {
            throw new VCS.RepoAlreadyExistsException();
        }
        try {
            Files.createDirectory(Paths.get(Repository.REPO_DIR_NAME));

            //Setting username
            Files.write(Paths.get(REPO_DIR_NAME, USERNAME_FILE), author.getBytes());

            //Setting up commit counter
            Files.write(Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME),
                    "0".getBytes());

            //Creating stage directory
            Files.createDirectory(Paths.get(REPO_DIR_NAME, STAGE_DIR));

            //Setting up position tracking
            Files.write(Paths.get(REPO_DIR_NAME, POSITION_FILENAME),
                    (DEFAULT_BRANCH + "\n-1").getBytes());

            //Initialising master branch
            Files.createDirectory(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME));
            Files.createFile(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME, DEFAULT_BRANCH));

            //Preparing initial commit
            Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME));
            try {
                new Commit("Initial commit.");
            } catch (VCS.BadPositionException e) {
                throw new VCS.BadRepoException();
            }
        } catch (IOException | VCS.BadRepoException e) {
            try {
                HashedDirectory.deleteDir(Paths.get(REPO_DIR_NAME));
            } catch (IOException e1) {}
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Get the number of commits in the repository.
     * @return the number of commits in the repository.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static int getCommitsNumber() throws VCS.BadRepoException {
        List<String> lines;
        int commitsNumber;
        try {
            lines = Files.readAllLines(Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME));
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        if (lines.size() != 1) {
            throw new VCS.BadRepoException();
        }
        try {
            commitsNumber = Integer.valueOf(lines.get(0));
        } catch (NumberFormatException e) {
            throw new VCS.BadRepoException();
        }
        return commitsNumber;
    }

    /**
     * Gets current branch.
     * @return a Branch object representing the current branch.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static Branch getCurBranch() throws VCS.BadRepoException {
        String curBranchName;
        Path posFilePath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        if (Files.notExists(posFilePath)) {
            throw new VCS.BadRepoException();
        }

        try (Scanner scanner = new Scanner(posFilePath)) {
            curBranchName = scanner.next();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        try {
            return Branch.getByName(curBranchName);
        } catch (VCS.NoSuchBranchException e) {
            throw new VCS.BadRepoException();
        }
    }

    /**
     * Gets the name of the current user.
     * @return username.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static String getUserName() throws VCS.BadRepoException {
        String username;
        try {
            if (Files.notExists(Paths.get(REPO_DIR_NAME, USERNAME_FILE))) {
                throw new VCS.BadRepoException();
            }
            BufferedReader reader = new BufferedReader(
                    new FileReader(Paths.get(REPO_DIR_NAME, USERNAME_FILE).toString()));
            username = reader.readLine();
            reader.close();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        return username;
    }

    /**
     * Sets updates current user's name.
     * @param name new user name.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static void setUserName(String name) throws VCS.BadRepoException {
        if (!Files.exists(Paths.get(REPO_DIR_NAME, USERNAME_FILE))) {
            throw new VCS.BadRepoException();
        }
        try (FileWriter writer = new FileWriter(
                Paths.get(REPO_DIR_NAME, USERNAME_FILE).toString())) {
            writer.write(name);
        } catch (IOException e){
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Gets the number of the current head.
     * @return the number of the currently heading commit.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static Integer getCurrentCommitNumber() throws VCS.BadRepoException {
        int curCommit;
        Path posFilePath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        if (Files.notExists(posFilePath)) {
            throw new VCS.BadRepoException();
        }

        try (Scanner scanner = new Scanner(posFilePath)) {
            scanner.next();
            curCommit = scanner.nextInt();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        return curCommit;
    }

    /**
     * Gets current commit.
     * @return a Commit object representing the currently heading commit.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static Commit getCurrentCommit() throws VCS.BadRepoException {
        try {
            return new Commit(getCurrentCommitNumber());
        } catch (VCS.NoSuchCommitException e){
            throw new VCS.BadRepoException();
        }
    }

    /**
     * Updates the current branch.
     * @param branch new current branch.
     */
    protected static void setCurrentBranch(Branch branch) {
        Path posPath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        try {
            List<String> pos = Files.readAllLines(posPath);
            Files.write(posPath, (branch.getName() + '\n' + pos.get(1)).getBytes());
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Updates current commit.
     * @param commit the new current commit.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static void setCurrentCommit(Commit commit) throws VCS.BadRepoException {
        Path posPath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        if (Files.notExists(posPath)) {
            throw new VCS.BadRepoException();
        }
        try {
            Files.write(posPath, (commit.getBranch().getName() + '\n' +
                    commit.getNumber().toString()).getBytes());
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Sets the commit counter into given value.
     * @param val new commit counter value.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static void updateCommitCounter(Integer val) throws VCS.BadRepoException {
        Path counterPath = Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME);
        if (Files.notExists(counterPath)) {
            throw new VCS.BadRepoException();
        }
        try {
            Files.write(counterPath, val.toString().getBytes());
        } catch (IOException e){
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Gets the working directory as a HashedDirectory object.
     * @return a HashedDirectory object representing the working directory.
     */
    protected static HashedDirectory getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = new HashedDirectory(Paths.get("."),
                    Paths.get(REPO_DIR_NAME, WORKING_DIR_HASHES));
        }
        return workingDirectory;
    }

    /**
     * Checks out a supplied commit. The working directory is returned to the
     * condition of that commit.
     * @param commitID the number of the commit too checkout.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     * @throws VCS.NoSuchCommitException if the requested commit does not exist.
     */
    protected static void checkoutCommit(Integer commitID) throws
            VCS.BadRepoException, VCS.NoSuchCommitException {
        Commit curCommit = Repository.getCurrentCommit();
        curCommit.clear();
        Commit newCommit = new Commit(commitID);
        newCommit.checkout();
    }
}
