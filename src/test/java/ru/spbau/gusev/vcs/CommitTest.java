package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

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

        try (RepoDir repo = new RepoDir()) {
            Path filePath = Paths.get(RepoDir.ROOT, RepoDir.STAGE,
                    TEST_FILE_NAME);
            Files.write(filePath, TEST_CONTENT.getBytes());
            String fileHash = HashedFile.calcFileHash(filePath.toString());
            Files.write(Paths.get(RepoDir.ROOT, RepoDir.STAGE_LIST),
                    (TEST_FILE_NAME + " " + fileHash).getBytes());

            //Mocking all other classes objects to be used in the test
            Branch branch = Mockito.mock(Branch.class);
            Mockito.when(branch.getName()).thenReturn(RepoDir.MASTER);

            StagingZone stagingZone = Mockito.mock(StagingZone.class);
            Mockito.when(stagingZone.getFiles()).thenReturn(Stream.of(
                    new HashedFile(Paths.get(TEST_FILE_NAME),
                            Paths.get(RepoDir.ROOT, RepoDir.STAGE))));

            IntersectedFolderStorage storage =
                    Mockito.mock(IntersectedFolderStorage.class);
            Mockito.when(storage.add(Mockito.any())).thenReturn(new TrackedFile() {
                private Path location = Paths.get(RepoDir.ROOT,
                        RepoDir.COMMITS_FILES, fileHash);

                {
                    Files.write(location, TEST_CONTENT.getBytes());
                }

                @Nonnull
                @Override
                public String getHash() {
                    return fileHash;
                }

                @Nonnull
                @Override
                public Path getName() {
                    return Paths.get(TEST_FILE_NAME);
                }

                @Nonnull
                @Override
                public Path getLocation() {
                    return location;
                }
            });

            Repository repository = Mockito.mock(Repository.class);
            Mockito.when(repository.getCurBranch()).thenReturn(branch);
            Mockito.when(repository.getCommitsNumber()).thenReturn(1);
            Mockito.when(repository.getStagingZone()).thenReturn(stagingZone);
            Mockito.when(repository.getCommitStorage()).thenReturn(storage);
            Mockito.when(repository.getUserName()).thenReturn(RepoDir.USERNAME);

            Commit commit = Commit.create(MESSAGE, repository);
            Path commitDir = Paths.get(RepoDir.ROOT, RepoDir.COMMITS,
                    commit.getNumber().toString());

            Scanner metadataScanner = new Scanner(
                    commitDir.resolve("metadata"));
            metadataScanner.next();
            Assert.assertEquals(RepoDir.MASTER,
                    metadataScanner.next());
            Assert.assertEquals(RepoDir.USERNAME, metadataScanner.next());
            Assert.assertEquals(0, metadataScanner.nextInt());
            metadataScanner.nextLine();
            Assert.assertEquals(MESSAGE, metadataScanner.nextLine());

            Assert.assertEquals(EXPECTED_CONTENT,
                    Files.readAllLines(Paths.get(RepoDir.ROOT,
                            RepoDir.COMMITS_FILES, fileHash)));
        }
    }
}
