import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RepositoryTest {
    @Test
    public void createTest() throws IOException, VCS.RepoAlreadyExistsException {
        final String USERNAME = "user";
        try {
            Repository.create(USERNAME);

            Path repoPath = Paths.get(Repository.REPO_DIR_NAME);
            Assert.assertTrue(Files.isDirectory(repoPath));
            Assert.assertTrue(Files.isDirectory(repoPath.resolve(
                    Repository.BRANCHES_DIR_NAME)));
            Assert.assertTrue(Files.isDirectory(repoPath.resolve(
                    Repository.COMMITS_DIR_NAME)));

            Assert.assertEquals(Files.readAllLines(repoPath.resolve(Repository.USERNAME_FILE)),
                    Collections.singletonList(USERNAME));

            Assert.assertEquals(
                    Files.readAllLines(repoPath.resolve(Repository.COMMITS_COUNTER_FILENAME)),
                    Collections.singletonList("1"));

            Assert.assertEquals(Arrays.asList("master", "0"),
                    Files.readAllLines(repoPath.resolve(Repository.POSITION_FILENAME)));
        } finally {
            HashedDirectory.deleteDir(Paths.get(Repository.REPO_DIR_NAME));
        }
    }

    @Test
    public void setUserTest() throws
            IOException, VCS.RepoAlreadyExistsException, VCS.BadRepoException {
        try {
            final String USERNAME_1 = "user1";
            final String USERNAME_2 = "user2";
            Repository.create(USERNAME_1);
            Repository.setUserName(USERNAME_2);
            Assert.assertEquals(Files.readAllLines(
                    Paths.get(Repository.REPO_DIR_NAME, Repository.USERNAME_FILE)),
                    Collections.singletonList(USERNAME_2));
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
        }
    }

    @Test
    public void checkoutTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.NoSuchFileException, VCS.BadPositionException, VCS.BadRepoException,
            VCS.NoSuchCommitException {
        final String FILE_1 = "file1";
        final String FILE_2 = "file2";
        Path path1 = Paths.get(FILE_1);
        Path path2 = Paths.get(FILE_2);

        try {
            Repository.create("usr");
            Files.write(path1, "1.1".getBytes());
            StagingZone.addFile(path1);
            Commit commit1 = new Commit(FILE_1);

            Files.write(path1, "1.2".getBytes());
            Files.write(path2, "2.1".getBytes());
            StagingZone.addFile(path1);
            StagingZone.addFile(path2);
            Commit commit2 = new Commit(FILE_2);

            Repository.checkoutCommit(commit1.getNumber());
            Assert.assertTrue(Files.notExists(path2));
            List<String> file1Content = Files.readAllLines(path1);
            Assert.assertEquals(Collections.singletonList("1.1"), file1Content);
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            if (Files.exists(path1)) {
                Files.delete(path1);
            }
            if (Files.exists(path2)) {
                Files.delete(path2);
            }
        }
    }
}
