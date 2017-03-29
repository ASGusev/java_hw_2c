import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class StagingZone {
    private static final String STAGE_LIST = "stage_list";
    private static final Path STAGE_PATH =
            Paths.get(Repository.REPO_DIR_NAME, Repository.STAGE_DIR);
    private static final Path LIST_PATH =
            Paths.get(Repository.REPO_DIR_NAME, STAGE_LIST);
    private static final HashedDirectory STAGE_DIR =
            new HashedDirectory(STAGE_PATH, LIST_PATH);

    protected static void addFile(Path filePath) throws VCS.NoSuchFileException {
        STAGE_DIR.addFile(Paths.get("."), filePath);
        STAGE_DIR.flushHashes();
    }

    protected static HashedDirectory getDir() {
        return STAGE_DIR;
    }
}
