import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

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

            Assert.assertEquals(Arrays.asList("master", "1"),
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
}
