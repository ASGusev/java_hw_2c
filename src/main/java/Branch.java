import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Branch {
    private final String name;
    private final Path commitsListPath;

    private Branch(String name) {
        this.name = name;
        commitsListPath = Paths.get(Repository.REPO_DIR_NAME,
                Repository.BRANCHES_DIR_NAME, name);
    }

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
        return new Branch(name);
    }

    protected static Branch getByName(String name) {
        return new Branch(name);
    }

    protected String getName() {
        return name;
    }

    protected void addCommit(Integer commitNumber) {
        try {
            Files.write(commitsListPath, (commitNumber.toString() + '\n').getBytes(),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

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

    protected Commit getHead() throws VCS.BadRepoException {
        try {
            return new Commit(getHeadNumber());
        } catch (VCS.NoSuchCommitException e) {
            throw new VCS.BadRepoException();
        }
    }

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
                    throw new VCS.FileSystemError();
                }
            });
        } catch (IOException e){
            throw new VCS.FileSystemError();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Branch && ((Branch)o).name.equals(this.name);
    }
}
