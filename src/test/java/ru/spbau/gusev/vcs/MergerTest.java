package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MergerTest {
    @Test
    public void simpleTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException, VCS.BadPositionException, VCS.NoSuchFileException,
            VCS.NoSuchCommitException, VCS.BranchAlreadyExistsException,
            NoSuchAlgorithmException, VCS.NoSuchBranchException {
        final Path FILE_TO_KEEP = Paths.get("file_keep");
        final Path FILE_TO_UPDATE = Paths.get("file_upd");
        final Path FILE_TO_CREATE = Paths.get("file_create");
        final String KEPT_CONTENT = "keep";
        final String CREATED_CONTENT = "created";
        final String V1 = "v1";
        final String V2 = "v2";

        try (RepoMock repo = new RepoMock()) {
            Path storage = Paths.get(RepoMock.ROOT, RepoMock.COMMITS_FILES);

            MessageDigest digest = MessageDigest.getInstance("MD5");
            String hashKeep = new BigInteger(digest.digest(
                    KEPT_CONTENT.getBytes())).toString();
            String hashV1 = new BigInteger(digest.digest(V1.getBytes()))
                    .toString();
            String hashV2 = new BigInteger(digest.digest(V2.getBytes()))
                    .toString();
            String hashCreate = new BigInteger(digest.digest
                    (CREATED_CONTENT.getBytes())).toString();

            Files.write(storage.resolve(hashCreate), CREATED_CONTENT.getBytes());
            Files.write(storage.resolve(hashKeep), KEPT_CONTENT.getBytes());
            Files.write(storage.resolve(hashV1), V1.getBytes());
            Files.write(storage.resolve(hashV2), V2.getBytes());

            Files.write(Paths.get(RepoMock.ROOT, RepoMock.COMMITS_FILES_LIST),
                    (hashCreate + " 1\n" + hashKeep + " 1\n" + hashV1 + " 1\n" +
                    hashV2 + " 1\n").getBytes());

            List<String> files1 = Arrays.asList(
                    FILE_TO_KEEP.toString() + " " + hashKeep,
                    FILE_TO_UPDATE.toString() + " " + hashV1);

            repo.commit(1, RepoMock.MASTER, files1, 0, "m1", 0);

            Files.write(Paths.get(RepoMock.ROOT, RepoMock.BRANCHES,
                    "work"), "1\n".getBytes());

            List<String> files2 = Arrays.asList(
                    FILE_TO_CREATE.toString() + " " + hashCreate,
                    FILE_TO_UPDATE.toString() + " " + hashV2);
            repo.commit(2, "work", files2, 0, "m2", 0);

            Files.write(Paths.get(RepoMock.ROOT, RepoMock.POSITION),
                    "master\n1".getBytes());
            Files.write(Paths.get(RepoMock.ROOT, RepoMock.COMMIT), "3".getBytes());
            Merger.merge(Branch.getByName("work"));

            Assert.assertEquals(Collections.singletonList(KEPT_CONTENT),
                    Files.readAllLines(FILE_TO_KEEP));
            Assert.assertTrue(Files.exists(FILE_TO_CREATE));
            Assert.assertEquals(Collections.singletonList(V2),
                    Files.readAllLines(FILE_TO_UPDATE));

            Files.delete(Paths.get(Repository.REPO_DIR_NAME,
                    "commits_files_list"));
            Files.createFile(Paths.get(Repository.REPO_DIR_NAME,
                    "commits_files_list"));
        } finally {
            Files.deleteIfExists(FILE_TO_KEEP);
            Files.deleteIfExists(FILE_TO_UPDATE);
            Files.deleteIfExists(FILE_TO_CREATE);
        }
    }

    @Test
    public void conflictsTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException, VCS.BadPositionException, VCS.NoSuchFileException,
            VCS.NoSuchCommitException, VCS.BranchAlreadyExistsException  {
        final Path FILE_UPDATED_IN_MASTER = Paths.get("file_upd_master");
        final Path FILE_UPDATED_IN_WORK = Paths.get("file_upd_work");
        final Path FILE_UPDATED_IN_BOTH = Paths.get("file_upd_both");
        final Path FILE_CREATED_IN_MASTER = Paths.get("file_cr_master");
        final Path FILE_CREATED_IN_WORK = Paths.get("file_cr_work");
        final Path FILE_CREATED_IN_BOTH = Paths.get("file_cr_both");

        List<String> V2 = Collections.singletonList("v5");
        List<String> V3 = Collections.singletonList("v6");

        try {
            Repository.create("usr");
            StagingZone stagingZone = Repository.getStagingZone();
            Path curDir = Paths.get(".");

            Files.write(FILE_UPDATED_IN_MASTER, "v4".getBytes());
            Files.write(FILE_UPDATED_IN_WORK, "v4".getBytes());
            Files.write(FILE_UPDATED_IN_BOTH, "v4".getBytes());
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_MASTER, curDir));
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_WORK, curDir));
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_BOTH, curDir));
            Commit masterCommit1 = Commit.create("v4");

            Files.write(FILE_UPDATED_IN_MASTER, "v5".getBytes());
            Files.write(FILE_UPDATED_IN_BOTH, "v5".getBytes());
            Files.write(FILE_CREATED_IN_MASTER, "v5".getBytes());
            Files.write(FILE_CREATED_IN_BOTH, "v5".getBytes());
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_MASTER, curDir));
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_BOTH, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_MASTER, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_BOTH, curDir));
            Commit masterCommit2 = Commit.create("v5");

            Repository.checkoutCommit(masterCommit1.getNumber());
            Branch workBranch = Branch.create("work",
                    masterCommit1.getNumber());
            Repository.setCurrentBranch(workBranch);
            Files.write(FILE_UPDATED_IN_WORK, "v6".getBytes());
            Files.write(FILE_UPDATED_IN_BOTH, "v6".getBytes());
            Files.write(FILE_CREATED_IN_WORK, "v6".getBytes());
            Files.write(FILE_CREATED_IN_BOTH, "v6".getBytes());
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_WORK, curDir));
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_BOTH, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_WORK, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_BOTH, curDir));
            Commit workCommit = Commit.create("v6");

            Repository.checkoutCommit(masterCommit2.getNumber());
            Merger.merge(workBranch);

            Assert.assertEquals(V2, Files.readAllLines(FILE_CREATED_IN_MASTER));
            Assert.assertEquals(V2, Files.readAllLines(FILE_UPDATED_IN_MASTER));
            Assert.assertEquals(V3, Files.readAllLines(FILE_UPDATED_IN_WORK));
            Assert.assertEquals(V3, Files.readAllLines(FILE_CREATED_IN_WORK));
            Assert.assertEquals(V3, Files.readAllLines(FILE_UPDATED_IN_BOTH));
            Assert.assertEquals(V3, Files.readAllLines(FILE_CREATED_IN_BOTH));
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            Files.deleteIfExists(FILE_CREATED_IN_BOTH);
            Files.deleteIfExists(FILE_CREATED_IN_MASTER);
            Files.deleteIfExists(FILE_CREATED_IN_WORK);
            Files.deleteIfExists(FILE_UPDATED_IN_BOTH);
            Files.deleteIfExists(FILE_UPDATED_IN_MASTER);
            Files.deleteIfExists(FILE_UPDATED_IN_WORK);
        }
    }
}
