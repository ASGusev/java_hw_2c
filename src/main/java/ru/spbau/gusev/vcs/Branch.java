package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
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
    private static final String REPO_DIR = ".vcs";
    private static final String BRANCHES_DIR = "branches";

    private final String name;
    private final Path commitsListPath;
    private final Repository repository;

    private Branch(@Nonnull String name, @Nonnull Repository repository)
            throws VCS.NoSuchBranchException {
        this.name = name;
        this.repository = repository;
        commitsListPath = Paths.get(REPO_DIR, BRANCHES_DIR, name);
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
    @Nonnull
    protected static Branch create(@Nonnull String name, Integer parentCommit,
                                   @Nonnull Repository repository)
            throws VCS.BranchAlreadyExistsException, VCS.BadRepoException {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty branch name.");
        }

        Path descPath = Paths.get(REPO_DIR, BRANCHES_DIR, name);
        if (Files.exists(descPath)) {
            throw new VCS.BranchAlreadyExistsException();
        }
        if (!Files.isDirectory(descPath.getParent())) {
            throw new VCS.BadRepoException("Branches folder not found.");
        }
        try {
            Files.write(descPath, (parentCommit.toString()
                    + '\n').getBytes());
        } catch (IOException e) {
            throw new VCS.FileSystemError("Branch description creation error.");
        }
        try {
            return new Branch(name, repository);
        } catch (VCS.NoSuchBranchException e) {
            throw new VCS.BadRepoException("Branch creation failed.");
        }
    }

    /**
     * Reads an existing branch from the repo.
     * @param name the name of the branch to getExisting
     * @return an object representing the requested branch.
     * @throws VCS.NoSuchBranchException if the requested branch does not exist.
     */
    @Nonnull
    protected static Branch getByName(@Nonnull String name,
                                      @Nonnull Repository repository)
            throws VCS.NoSuchBranchException {
        return new Branch(name, repository);
    }

    /**
     * Gets the name of the branch.
     * @return the name of the branch.
     */
    @Nonnull
    protected String getName() {
        return name;
    }

    /**
     * Adds a commit to the end of the branch.
     * @param newCommit a commit to add.
     */
    protected void addCommit(@Nonnull Commit newCommit) {
        try {
            Files.write(commitsListPath, (newCommit.getNumber().toString() + '\n')
                            .getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new VCS.FileSystemError("Error writing commit number to branch's " +
                    "list.");
        }
    }

    /**
     * Gets the number of hte last commit in the branch.
     * @return the number of hte last commit in the branch.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    @Nonnull
    protected Integer getHeadNumber() throws VCS.BadRepoException {
        String headNumber = "-1";
        try (Scanner scanner = new Scanner(commitsListPath)) {
            while (scanner.hasNext()) {
                headNumber = scanner.next();
            }
        } catch (IOException e) {
            throw new VCS.FileSystemError("Error reading branch's commits list.");
        }
        try {
            return (Integer.valueOf(headNumber));
        } catch (NumberFormatException e) {
            throw new VCS.BadRepoException("Incorrect branch description format.");
        }
    }

    /**
     * Gets the branch's head commit.
     * @return a Commit object representing the branch's head commit.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    @Nonnull
    protected Commit getHead() throws VCS.BadRepoException {
        try {
            return Commit.read(getHeadNumber(), repository);
        } catch (VCS.NoSuchCommitException e) {
            throw new VCS.BadRepoException("Branch's head commit not found.");
        }
    }

    /**
     * Makes a log of commits in the branch.
     * @return a list with descriptions of all commits of the branch.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    @Nonnull
    protected List<VCS.CommitDescription> getLog() throws VCS.BadRepoException {
        try {
            return Files.lines(commitsListPath).skip(1).map(number -> {
                try {
                    Commit commit = Commit.read(Integer.valueOf(number),
                            repository);
                    return new VCS.CommitDescription(commit);
                } catch (VCS.NoSuchCommitException | VCS.BadRepoException e) {
                    throw new Error();
                }
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new VCS.FileSystemError("Error reading branch's commit list.");
        } catch (Error e) {
            throw new VCS.BadRepoException("Error reading commit data.");
        }
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
        try {
            Files.lines(commitsListPath).forEach(commit -> {
                try {
                    Commit.read(Integer.valueOf(commit), repository).delete();
                } catch (VCS.NoSuchCommitException | VCS.BadRepoException e) {
                    throw new Error();
                }
            });
            Files.delete(commitsListPath);
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        } catch (Error e) {
            throw new VCS.BadRepoException("Error deleting commit.");
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
