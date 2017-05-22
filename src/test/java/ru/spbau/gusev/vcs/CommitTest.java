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
        final String TEST_CONTENT = "foo";
        final String TEST_FILE_NAME = "foo";
        final String MESSAGE = "bar";
        final List<String> EXPECTED_CONTENT =
                Collections.singletonList(TEST_CONTENT);

        try (TestingRepo repo = new TestingRepo()) {
            Path filePath = Paths.get(TestingRepo.ROOT, TestingRepo.STAGE,
                    TEST_FILE_NAME);
            Files.write(filePath, TEST_CONTENT.getBytes());
            String fileHash = HashedFile.calcFileHash(filePath.toString());
            Files.write(Paths.get(TestingRepo.ROOT, TestingRepo.STAGE_LIST),
                    (TEST_FILE_NAME + " " + fileHash).getBytes());

            Commit commit = new Commit(MESSAGE);
            Path commitDir = Paths.get(TestingRepo.ROOT, TestingRepo.COMMITS,
                    commit.getNumber().toString());

            Scanner metadataScanner = new Scanner(
                    commitDir.resolve("metadata"));
            metadataScanner.next();
            Assert.assertEquals(TestingRepo.MASTER,
                    metadataScanner.next());
            Assert.assertEquals(TestingRepo.USERNAME, metadataScanner.next());
            Assert.assertEquals(0, metadataScanner.nextInt());
            metadataScanner.nextLine();
            Assert.assertEquals(MESSAGE, metadataScanner.nextLine());

            Assert.assertEquals(EXPECTED_CONTENT,
                    Files.readAllLines(Paths.get(TestingRepo.ROOT,
                            TestingRepo.COMMITS_FILES, fileHash)));
        }
    }
}
