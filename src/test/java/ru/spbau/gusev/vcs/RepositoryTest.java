package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        try (RepoDir repo = new RepoDir()) {
            final String CUSTOM_USERNAME = "user2";
            Repository.getExisting().setUserName(CUSTOM_USERNAME);
            Assert.assertEquals(Files.readAllLines(
                    Paths.get(Repository.REPO_DIR_NAME, Repository.USERNAME_FILE)),
                    Collections.singletonList(CUSTOM_USERNAME));
        }
    }

    @Test
    public void checkoutTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.NoSuchFileException, VCS.BadPositionException, VCS.BadRepoException,
            VCS.NoSuchCommitException, NoSuchAlgorithmException {
        final String FILE_1 = "file1";
        final String FILE_2 = "file2";
        final String CONTENT_1_1 = "1.1";
        final String CONTENT_1_2 = "1.2";
        final String CONTENT_2_1 = "2.1";
        final Path path1 = Paths.get(FILE_1);
        final Path path2 = Paths.get(FILE_2);
        final Path storagePath = Paths.get(RepoDir.ROOT,
                RepoDir.COMMITS_FILES);

        try (RepoDir repo = new RepoDir()) {
            Repository repository = Repository.getExisting();
            MessageDigest digest = MessageDigest.getInstance("MD5");
            String hash_1_1 = new BigInteger(
                    digest.digest(CONTENT_1_1.getBytes())).toString();
            String hash_1_2 = new BigInteger(
                    digest.digest(CONTENT_1_2.getBytes())).toString();
            String hash_2_1 = new BigInteger(
                    digest.digest(CONTENT_2_1.getBytes())).toString();

            Files.write(storagePath.resolve(hash_1_1), CONTENT_1_1.getBytes());
            Files.write(storagePath.resolve(hash_1_2), CONTENT_1_2.getBytes());
            Files.write(storagePath.resolve(hash_2_1), CONTENT_2_1.getBytes());

            List<String> files1 = Collections.singletonList(FILE_1 + " " + hash_1_1);
            List<String> files2 = Arrays.asList(FILE_1 + " " + hash_1_2,
                    FILE_2 + " " + hash_2_1);

            repo.commit(1, RepoDir.MASTER, files1, 0, "msg", 0);
            repo.commit(2, RepoDir.MASTER, files2, 0, "msg", 2);

            repository.checkoutCommit(1);
            Assert.assertTrue(Files.notExists(path2));
            List<String> file1Content = Files.readAllLines(path1);
            Assert.assertEquals(Collections.singletonList("1.1"), file1Content);
        } finally {
            Files.deleteIfExists(path1);
            Files.deleteIfExists(path2);
        }
    }
}
