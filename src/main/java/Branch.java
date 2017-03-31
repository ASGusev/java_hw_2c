import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * A class representing a branch in the repository.
 */
public class Branch {
    private final String name;
    private final Path commitsListPath;

    private Branch(String name) throws VCS.NoSuchBranchException {
        this.name = name;
        commitsListPath = Paths.get(Repository.REPO_DIR_NAME,
                Repository.BRANCHES_DIR_NAME, name);
        if (Files.notExists(commitsListPath)) {
            throw new VCS.NoSuchBranchException();
        }
    }

    /**
     * Creates a new branch in the repository.
     * @param name the name for the new branch.
     * @return an object representing the new branch.
     * @throws VCS.BranchAlreadyExistsException if a branch with the given
     * name already exists in the repository.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected static Branch create(String name) throws VCS.BranchAlreadyExistsException,
            VCS.BadRepoException {
        Path descPath = Paths.get(Repository.REPO_DIR_NAME,
                Repository.BRANCHES_DIR_NAME, name);
        if (Files.exists(descPath)) {
            throw new VCS.BranchAlreadyExistsException();
        }
        try {
            Files.write(descPath, (Repository.getCurrentCommitNumber().toString() + '\n').
                    getBytes());
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        try {
            Branch newBranch = new Branch(name);
            Repository.setCurrentBranch(newBranch);
            return newBranch;
        } catch (VCS.NoSuchBranchException e) {
            throw new VCS.BadRepoException();
        }
    }

    /**
     * Reads an existing branch from the repo.
     * @param name the name of the branch to get
     * @return an object representing the requested branch.
     * @throws VCS.NoSuchBranchException if the requested branch does not exist.
     */
    protected static Branch getByName(String name) throws VCS.NoSuchBranchException {
        return new Branch(name);
    }

    protected String getName() {
        return name;
    }

    /**
     * Adds a commit to the end of the branch.
     * @param newCommit a commit to add.
     */
    protected void addCommit(Commit newCommit) {
        try {
            Files.write(commitsListPath, (newCommit.getNumber().toString() + '\n')
                            .getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Gets the number of hte last commit in the branch.
     * @return the number of hte last commit in the branch.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected Integer getHeadNumber() throws VCS.BadRepoException {
        String headNumber = "-1";
        try (Scanner scanner = new Scanner(commitsListPath)) {
            while (scanner.hasNext()) {
                headNumber = scanner.next();
            }
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
        try {
            return (Integer.valueOf(headNumber));
        } catch (NumberFormatException e) {
            throw new VCS.BadRepoException();
        }
    }

    /**
     * Gets the branch's head commit.
     * @return a Commit object representing the branch's head commit.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected Commit getHead() throws VCS.BadRepoException {
        try {
            return new Commit(getHeadNumber());
        } catch (VCS.NoSuchCommitException e) {
            throw new VCS.BadRepoException();
        }
    }

    /**
     * Makes a log of commits in the branch.
     * @return a list with descriptions of all commits of the branch.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected List<VCS.CommitDescription> getLog() throws VCS.BadRepoException {
        List<VCS.CommitDescription> commitList;
        try {
            commitList = Files.lines(commitsListPath).map(number -> {
                try {
                    Commit commit = new Commit(Integer.valueOf(number));
                    return new VCS.CommitDescription(commit);
                } catch (VCS.NoSuchCommitException | VCS.BadRepoException e) {
                    throw new Error();
                }
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        } catch (Error e) {
            throw new VCS.BadRepoException();
        }
        return commitList;
    }

    /**
     * Deletes the branch from the repository. Deleting the master branch is
     * impossible.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     * @throws VCS.BadPositionException in case of an attempt to remove the current
     * branch.
     * @throws IllegalArgumentException in case of attempt to delete the master branch.
     */
    void delete() throws VCS.BadRepoException, VCS.BadPositionException {
        if (name.equals(Repository.DEFAULT_BRANCH)) {
            throw new UnsupportedOperationException();
        }
        if (this.equals(Repository.getCurBranch())) {
            throw new VCS.BadPositionException();
        }

        Path commitsDir = Paths.get(Repository.REPO_DIR_NAME, Repository.COMMITS_DIR_NAME);
        try {
            Files.lines(commitsListPath).forEach(commit -> {
                try {
                    HashedDirectory.deleteDir(commitsDir.resolve(commit));
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new VCS.FileSystemError();
                }
            });
            Path record = Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, name);
            Files.delete(record);
        } catch (IOException e){
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Compares this object with another Branch object. Two Branch objects are considered
     * equal if their names coincide.
     * @param o object to compare with.
     * @return true if the given object is an equal Branch object, false if is is not equal
     * or is not a Branch object.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Branch && ((Branch)o).name.equals(this.name);
    }
}
