package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class CommitTest {
    @Test
    public void creationTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.NoSuchFileException, VCS.NoSuchBranchException, VCS.BadRepoException,
            VCS.BadPositionException {
        final String USERNAME = "user";
        final String TEST_CONTENT = "foo";
        final String TEST_FILE_NAME = "foo";
        final String MESSAGE = "bar";
        final List<String> EXPECTED_CONTENT = Collections.singletonList(TEST_CONTENT);

        try {
            VCS.createRepo(USERNAME);
            Files.write(Paths.get(TEST_FILE_NAME), TEST_CONTENT.getBytes());
            StagingZone.addFile(WorkingDirectory.getHashedFileByName(TEST_FILE_NAME));
            Commit commit = new Commit(MESSAGE);
            Path commitDir = Paths.get(Repository.REPO_DIR_NAME, Repository.COMMITS_DIR_NAME,
                    commit.getNumber().toString());
            List<String> committedContent = Files.readAllLines(
                    commitDir.resolve(Commit.COMMIT_CONTENT_DIR).resolve(TEST_FILE_NAME));
            Assert.assertEquals(EXPECTED_CONTENT, committedContent);

            Scanner metadataScanner = new Scanner(
                    commitDir.resolve(Commit.COMMIT_METADATA_FILE));
            metadataScanner.next();
            Assert.assertEquals(Repository.getCurBranch().getName(),
                    metadataScanner.next());
            Assert.assertEquals(USERNAME, metadataScanner.next());
            Assert.assertEquals(0, metadataScanner.nextInt());
            metadataScanner.nextLine();
            Assert.assertEquals(MESSAGE, metadataScanner.nextLine());
        } finally {
            Files.delete(Paths.get(TEST_FILE_NAME));
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
        }
    }
}
