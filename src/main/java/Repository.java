import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;

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
        return Branch.getByName(curBranchName);
    }

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

    protected static Commit getCurrentCommit() throws VCS.BadRepoException {
        try {
            return new Commit(getCurrentCommitNumber());
        } catch (VCS.NoSuchCommitException e){
            throw new VCS.BadRepoException();
        }
    }

    protected static void setCurrentBranch(Branch branch) {
        Path posPath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        try {
            List<String> pos = Files.readAllLines(posPath);
            Files.write(posPath, (branch.getName() + '\n' + pos.get(1)).getBytes());
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

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

    protected static HashedDirectory getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = new HashedDirectory(Paths.get("."),
                    Paths.get(REPO_DIR_NAME, WORKING_DIR_HASHES));
        }
        return workingDirectory;
    }

    protected static void checkoutCommit(Integer commitID) throws
            VCS.BadRepoException, VCS.NoSuchCommitException {
        Commit curCommit = Repository.getCurrentCommit();
        curCommit.clear();
        Commit newCommit = new Commit(commitID);
        newCommit.checkout();
    }
}
