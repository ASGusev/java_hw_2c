import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class VCSTest {
    private static final String REPO_DIR_NAME = ".vcs";
    private static final String BRANCHES_DIR_NAME = "branches";
    private static final String COMMITS_DIR_NAME = "commits";
    private static final String USERNAME_FILE = "user";
    private static final String COMMITS_COUNTER_FILENAME = "commit";
    private static final String POSITION_FILENAME = "position";
    private static final String STAGE_DIR = "stage";
    private static final String COMMIT_CONTENT_DIR = "content";
    private static final String COMMIT_METADATA_FILE = "metadata";
    private static final String COMMIT_FILES_LIST = "files_list";

    @Test
    public void createRepoTest() throws IOException, VCS.RepoAlreadyExistsException {
        final String USERNAME = "user";
        try {
            VCS.createRepo(USERNAME);

            Assert.assertTrue(Files.isDirectory(Paths.get(REPO_DIR_NAME)));
            Assert.assertTrue(Files.isDirectory(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME)));
            Assert.assertTrue(Files.isDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME)));

            Assert.assertEquals(Files.readAllLines(Paths.get(REPO_DIR_NAME, USERNAME_FILE)),
                    Collections.singletonList(USERNAME));

            Assert.assertEquals(
                    Files.readAllLines(Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME)),
                    Collections.singletonList("0"));

            Assert.assertEquals(Arrays.asList("master", "0"),
                    Files.readAllLines(Paths.get(REPO_DIR_NAME, POSITION_FILENAME)));
        } finally {
            deleteDir(REPO_DIR_NAME);
        }
    }

    @Test
    public void setUserTest() throws
            IOException, VCS.RepoAlreadyExistsException, VCS.BadRepoException {
        try {
            final String USERNAME_1 = "user1";
            final String USERNAME_2 = "user2";
            VCS.createRepo(USERNAME_1);
            VCS.setUser(USERNAME_2);
            Assert.assertEquals(Files.readAllLines(Paths.get(REPO_DIR_NAME, USERNAME_FILE)),
                    Collections.singletonList(USERNAME_2));
        } finally {
            deleteDir(REPO_DIR_NAME);
        }
    }

    @Test
    public void addTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.BadRepoException, VCS.NoSuchFileException {
        final String TEST_CONTENT = "foo";
        final List<String> EXPECTED_CONTENT = Collections.singletonList(TEST_CONTENT);
        final String TEST_FILENAME = "foo";
        VCS.createRepo("user");
        Path testFilePath = Paths.get(TEST_FILENAME);
        try {
            Files.write(testFilePath, TEST_CONTENT.getBytes());
            VCS.addFile(TEST_FILENAME);
            Path stagedFilePath = Paths.get(REPO_DIR_NAME, STAGE_DIR, TEST_FILENAME);
            List<String> addedContend = Files.readAllLines(stagedFilePath);
            Assert.assertEquals(EXPECTED_CONTENT, addedContend);
        } finally {
            Files.delete(testFilePath);
            deleteDir(REPO_DIR_NAME);
        }
    }

    @Test
    public void commitTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException, VCS.NoSuchFileException, VCS.BadPositionException {
        final String USERNAME = "user";
        final String TEST_CONTENT = "foo";
        final String TEST_FILE_NAME = "foo";
        final String MESSAGE = "bar";
        final List<String> EXPECTED_CONTENT = Collections.singletonList(TEST_CONTENT);
        VCS.createRepo(USERNAME);

        try {
            Files.write(Paths.get(TEST_FILE_NAME), TEST_CONTENT.getBytes());
            VCS.addFile(TEST_FILE_NAME);
            VCS.commit(MESSAGE);
            List<String> committedContent = Files.readAllLines(Paths.get(REPO_DIR_NAME,
                    COMMITS_DIR_NAME, "1", COMMIT_CONTENT_DIR, "foo"));
            Assert.assertEquals(EXPECTED_CONTENT, committedContent);

            Path commitMetadata = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME, "1"
                    , COMMIT_METADATA_FILE);
            Scanner metadataScanner = new Scanner(commitMetadata);
            metadataScanner.next();
            Assert.assertEquals("master", metadataScanner.next());
            Assert.assertEquals("user", metadataScanner.next());
            Assert.assertEquals(0, metadataScanner.nextInt());
            metadataScanner.nextLine();
            Assert.assertEquals(MESSAGE, metadataScanner.nextLine());
        } finally {
            Files.delete(Paths.get(TEST_FILE_NAME));
            deleteDir(REPO_DIR_NAME);
        }
    }

    @Test
    public void branchTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.BranchAlreadyExistsException, VCS.BadRepoException,
            VCS.NoSuchBranchException {
        final String BRANCH_NAME = "br";
        VCS.createRepo("usr");
        try {
            VCS.createBranch(BRANCH_NAME);
            Path branchDescPath = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME,
                    BRANCH_NAME);
            Assert.assertTrue("Branch creation failure",
                    Files.exists(branchDescPath));

            VCS.deleteBranch(BRANCH_NAME);
            Assert.assertTrue("Branch deletion failure",
                    Files.notExists(branchDescPath));
        } finally {
            deleteDir(REPO_DIR_NAME);
        }
    }

    private static void deleteDir(String dir) throws IOException {
        File dirFile = new File(dir);
        wipeDir(dirFile);
        dirFile.delete();
    }

    private static void wipeDir(File dir) throws IOException {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                wipeDir(file);
            }
            file.delete();
        }
    }
}
