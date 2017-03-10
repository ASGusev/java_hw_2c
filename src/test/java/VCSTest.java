import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class VCSTest {
    private static final String REPO_DIR_NAME = ".vcs";
    private static final String BRANCHES_DIR_NAME = "branches";
    private static final String COMMITS_DIR_NAME = "commits";
    private static final String USERNAME_FILE = "user";
    private static final String COMMITS_COUNTER_FILENAME = "commit";
    private static final String POSITION_FILENAME = "position";

    @Test
    public void createRepoTest() throws IOException, VCS.RepoExistsException {
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

            Assert.assertEquals(
                    Files.readAllLines(Paths.get(REPO_DIR_NAME, POSITION_FILENAME)),
                    Arrays.asList("", "0"));
        } finally {
            VCS.removeRepo();
        }
    }

    @Test
    public void setUserTest() throws
            IOException, VCS.RepoExistsException, VCS.BadRepoException {
        try {
            final String USERNAME_1 = "user1";
            final String USERNAME_2 = "user2";
            VCS.createRepo(USERNAME_1);
            VCS.setUser(USERNAME_2);
            Assert.assertEquals(Files.readAllLines(Paths.get(REPO_DIR_NAME, USERNAME_FILE)),
                    Collections.singletonList(USERNAME_2));
        } finally {
            VCS.removeRepo();
        }
    }
    /*
    @Test
    public void commitTest() throws IOException, VCS.RepoExistsException,
            VCS.BadRepoException {
    }
    */
}
