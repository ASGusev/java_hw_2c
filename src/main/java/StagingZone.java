import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A class representing the Staging zone. The function of the staging zone is
 * determining which files should be included in the next commit.
 */
public abstract class StagingZone {
    protected static final String STAGE_DIR = "stage";
    protected static final String STAGE_LIST = "stage_list";
    private static final Path STAGE_PATH =
            Paths.get(Repository.REPO_DIR_NAME, STAGE_DIR);
    private static final Path LIST_PATH =
            Paths.get(Repository.REPO_DIR_NAME, STAGE_LIST);
    private static final HashedDirectory STAGE_HASH_DIR =
            new HashedDirectory(STAGE_PATH, LIST_PATH);

    /**
     * Adds a file to the staging directory, including it into the next commit.
     * @param filePath the path to the file to add.
     * @throws VCS.NoSuchFileException if the supplied path does not lead to a
     * correct file.
     */
    protected static void addFile(Path filePath) throws VCS.NoSuchFileException {
        STAGE_HASH_DIR.addFile(Paths.get("."), filePath);
        STAGE_HASH_DIR.flushHashes();
    }

    /**
     * Gets the stage directory.
     * @return a HashedDirectory object representing the directory with staged files.
     */
    protected static HashedDirectory getDir() {
        return STAGE_HASH_DIR;
    }

    /**
     * Removes all staged files from the staging zone.
     */
    protected static void wipe() {
        STAGE_HASH_DIR.clear();
        STAGE_HASH_DIR.flushHashes();
    }

    /**
     * Copies all files from the given directory to the staging zone.
     * @param dir the directory which contains files to be staged.
     */
    protected static void cloneDir(HashedDirectory dir) {
        wipe();
        STAGE_HASH_DIR.cloneDirectory(dir);
        STAGE_HASH_DIR.flushHashes();
    }
}
