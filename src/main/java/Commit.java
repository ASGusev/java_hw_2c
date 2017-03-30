import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A class representing a commit in the repository.
 */
public class Commit {
    protected static final String COMMIT_CONTENT_DIR = "content";
    protected static final String COMMIT_METADATA_FILE = "metadata";
    protected static final String COMMIT_FILES_LIST = "files_list";

    protected Integer getNumber() {
        return number;
    }

    protected long getCreationTime() {
        return creationTime;
    }

    protected String getMessage() {
        return message;
    }

    protected Branch getBranch() {
        return branch;
    }

    protected String getAuthor() {
        return author;
    }

    protected Commit getFather() throws VCS.BadRepoException {
        try {
            return new Commit(father);
        } catch (VCS.NoSuchCommitException e) {
            throw new VCS.BadRepoException();
        }
    }

    private final Integer number;
    private final long creationTime;
    private final String message;
    private final Branch branch;
    private final String author;
    private final Integer father;
    private final Path rootDir;

    /**
     * Creates a new commit in the repository with given message in the current
     * branch and sets it as the global head.
     * @param message the commit message.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     * @throws VCS.BadPositionException if current commit is not the head of its branch.
     */
    protected Commit(String message) throws VCS.BadRepoException,
            VCS.BadPositionException {
        if (Files.notExists(Paths.get(Repository.REPO_DIR_NAME,
                Repository.COMMITS_DIR_NAME))) {
            throw new VCS.BadRepoException();
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
            throw new VCS.BadPositionException();
        }

        try {
            Files.createDirectory(rootDir);
            Files.createDirectory(rootDir.resolve(COMMIT_CONTENT_DIR));

            BufferedWriter metadataWriter = new BufferedWriter(new FileWriter(
                    rootDir.resolve(COMMIT_METADATA_FILE).toString()));
            metadataWriter.write(String.valueOf(creationTime) + '\n');
            metadataWriter.write(branch.getName() + '\n');
            metadataWriter.write(author + '\n');
            metadataWriter.write(father.toString() + '\n');
            metadataWriter.write(message);
            metadataWriter.close();

            HashedDirectory contentDir = new HashedDirectory(
                    rootDir.resolve(COMMIT_CONTENT_DIR), rootDir.resolve(COMMIT_FILES_LIST));
            contentDir.cloneDirectory(StagingZone.getDir());
            contentDir.flushHashes();

            branch.addCommit(this);
            Repository.setCurrentCommit(this);
            Repository.updateCommitCounter(number + 1);
        } catch (IOException | VCS.FileSystemError e) {
            e.printStackTrace();
            try {
                HashedDirectory.deleteDir(rootDir);
            } catch (IOException e1) {}
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Reads an already existing commit from the repository.
     * @param number the number of commit to be read.
     * @throws VCS.NoSuchCommitException if a commit with the given number does not
     * exist.
     * @throws VCS.BadRepoException if the repository data folder is corrupt.
     */
    protected Commit(Integer number) throws VCS.NoSuchCommitException,
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
            while (metadataScanner.hasNext()) {
                messageBuilder.append(metadataScanner.nextLine());
                messageBuilder.append('\n');
            }
            message = messageBuilder.toString();
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        } catch (VCS.NoSuchBranchException e) {
            throw new VCS.BadRepoException();
        }
    }

    /**
     * Removes all the commit's files from the working directory.
     */
    protected void clear() {
        Path contentDir = rootDir.resolve(COMMIT_CONTENT_DIR);
        try {
            Files.walk(contentDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(contentDir.relativize(path));
                        } catch (IOException e) {
                            throw new VCS.FileSystemError();
                        }
                    });
            Files.walk(contentDir)
                    .forEach(path -> {
                        try {
                            if (Files.isDirectory(path) && !path.equals(contentDir)) {
                                HashedDirectory.deleteDir(contentDir.relativize(path));
                            }
                        } catch (IOException e) {
                            throw new VCS.FileSystemError();
                        }
                    });
        } catch (IOException e) {
            throw new VCS.FileSystemError();
        }
    }

    /**
     * Copies all the files from the commit to the working directory and sets
     * this commit as global head.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
    protected void checkout() throws VCS.BadRepoException {
        Path contentDir = rootDir.resolve(COMMIT_CONTENT_DIR);
        HashedDirectory contentDirectory = new HashedDirectory(contentDir,
                rootDir.resolve(COMMIT_FILES_LIST));

        HashedDirectory workingDirectory = Repository.getWorkingDirectory();
        workingDirectory.cloneDirectory(contentDirectory);

        HashedDirectory stageDir = StagingZone.getDir();
        stageDir.cloneDirectory(contentDirectory);
        stageDir.flushHashes();

        Repository.setCurrentCommit(this);
    }

    /**
     * Restores the history between the initial commit and the current one.
     * @return a list containing all current commit's predessors sorted by
     * creation time.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     */
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

    @Override
    public boolean equals(Object o) {
        return o instanceof Commit && ((Commit)o).number.equals(this.number);
    }

    /**
     * Gets a mapping from file path in commit to its hash and path from the
     * working directory.
     * @return a Map from file path to its description.
     */
    public Map<Path, HashedFile> getFileDescriptions() {
        return new HashedDirectory(rootDir.resolve(COMMIT_CONTENT_DIR),
                rootDir.resolve(COMMIT_FILES_LIST)).getFileDescriptions();
    }
}
