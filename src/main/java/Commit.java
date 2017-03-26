import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

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

    protected Integer getFather() {
        return father;
    }

    private final Integer number;
    private final long creationTime;
    private final String message;
    private final Branch branch;
    private final String author;
    private final Integer father;

    protected Commit(String message) throws VCS.BadRepoException,
            VCS.BadPositionException {
        if (Files.notExists(Paths.get(VCS.REPO_DIR_NAME, VCS.COMMITS_DIR_NAME))) {
            throw new VCS.BadRepoException();
        }

        number = Repository.getCommitsNumber() + 1;
        Path rootDir = Paths.get(VCS.REPO_DIR_NAME, VCS.COMMITS_DIR_NAME,
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

            branch.addCommit(number);
            Repository.setCurrentCommit(this);
            Repository.updateCommitCounter(number);
        } catch (IOException | VCS.FileSystemError e) {
            e.printStackTrace();
            try {
                HashedDirectory.deleteDir(rootDir);
            } catch (IOException e1) {}
            throw new VCS.FileSystemError();
        }
    }

    protected Commit(Integer number) throws VCS.NoSuchCommitException,
            VCS.BadRepoException {
        this.number = number;
        Path rootDir = Paths.get(Repository.REPO_DIR_NAME, Repository.COMMITS_DIR_NAME,
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
        }
    }
}
