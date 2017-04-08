package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

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
    protected static final String DEFAULT_BRANCH = "master";
    protected static final String STAGE_DIR = "stage";
    protected static final String STAGE_LIST = "stage_list";

    private static StagingZone stage;
    private static WorkingDirectory workingDirectory;

    /**
     * Initialises a repository in the current directory. A folder with all the
     * necessary information is created.
     * @param author the first username for the created repository.
     * @throws VCS.RepoAlreadyExistsException if a repository is already initialised
     * in the current folder.
     * @throws IllegalArgumentException if the author parameter is an empty string.
     */
    protected static void create(@Nonnull String author) throws VCS.RepoAlreadyExistsException {
        if (Files.exists(Paths.get(Repository.REPO_DIR_NAME), LinkOption.NOFOLLOW_LINKS)) {
            throw new VCS.RepoAlreadyExistsException();
        }
        if (author.isEmpty()) {
            throw new IllegalArgumentException();
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
            Files.createFile(Paths.get(REPO_DIR_NAME, STAGE_LIST));
            getStagingZone().wipe();

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
    @Nonnull
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
    @Nonnull
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
    protected static void setUserName(@Nonnull String name) throws VCS.BadRepoException {
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
    @Nonnull
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
    @Nonnull
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
    protected static void setCurrentBranch(@Nonnull Branch branch) {
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
    protected static void setCurrentCommit(@Nonnull Commit commit) throws VCS.BadRepoException {
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
    protected static void updateCommitCounter(@Nonnull Integer val) throws VCS.BadRepoException {
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
     * Checks out a supplied commit. The working directory is returned to the
     * condition of that commit.
     * @param commitID the number of the commit too checkout.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     * @throws VCS.NoSuchCommitException if the requested commit does not exist.
     */
    protected static void checkoutCommit(@Nonnull Integer commitID) throws
            VCS.BadRepoException, VCS.NoSuchCommitException {
        Commit curCommit = Repository.getCurrentCommit();
        curCommit.removeFrom(getWorkingDirectory());
        Commit newCommit = new Commit(commitID);
        newCommit.checkout(workingDirectory);
    }

    /**
     * Gets the staging zone of the repository.
     * @return a StagingZone object for this repository.
     */
    @Nonnull
    protected static StagingZone getStagingZone() throws VCS.BadRepoException {
         if (stage == null) {
             try {
                 stage = new StagingZone(Paths.get(REPO_DIR_NAME, STAGE_DIR),
                         Paths.get(REPO_DIR_NAME, STAGE_LIST));
             } catch (VCS.NoSuchFileException e) {
                 throw new VCS.BadRepoException();
             }
         }
         return stage;
    }

    /**
     * Gets the WorkingDirectory representation of this repository's working directory.
     * @return a WorkingDirectory object referring to the repository's working
     * directory.
     */
    @Nonnull
    protected static WorkingDirectory getWorkingDirectory() {
        if (workingDirectory == null) {
            workingDirectory = new WorkingDirectory(Paths.get("."));
        }
        return workingDirectory;
    }

    /**
     * Lists all the branches in the repository.
     * @return a list containing all the branches in the repository.
     * @throws VCS.BadRepoException if the repository is corrupt.
     */
    @Nonnull
    protected static List<Branch> getBranches() throws VCS.BadRepoException {
        Path branchesDir = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME);
        if (!Files.isDirectory(branchesDir)) {
            throw new VCS.BadRepoException();
        }

        try {
            return Files.list(branchesDir)
                    .map(line -> {
                        try {
                            String branchName = branchesDir.relativize(line).toString();
                            return Branch.getByName(branchName);
                        } catch (VCS.NoSuchBranchException e) {
                            throw new Error();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        } catch (Error e) {
            throw new VCS.BadRepoException();
        }
    }
}
