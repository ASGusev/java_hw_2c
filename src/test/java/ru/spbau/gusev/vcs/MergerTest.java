package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

public class MergerTest {
    @Test
    public void simpleTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException, VCS.BadPositionException, VCS.NoSuchFileException,
            VCS.NoSuchCommitException, VCS.BranchAlreadyExistsException {
        final Path FILE_TO_KEEP = Paths.get("file_keep");
        final Path FILE_TO_UPDATE = Paths.get("file_upd");
        final Path FILE_TO_CREATE = Paths.get("file_create");
        final String KEPT_CONTENT = "keep";
        final String CREATED_CONTENT = "created";
        final String V1 = "v1";
        final String V2 = "v2";

        try {
            Repository.create("usr");
            StagingZone stagingZone = Repository.getStagingZone();

            Files.write(FILE_TO_KEEP, KEPT_CONTENT.getBytes());
            Files.write(FILE_TO_UPDATE, V1.getBytes());
            stagingZone.add(new HashedFile(FILE_TO_KEEP, Paths.get(".")));
            stagingZone.add(new HashedFile(FILE_TO_UPDATE, Paths.get(".")));
            Commit masterCommit1 = new Commit("m1");

            Repository.checkoutCommit(masterCommit1.getNumber());
            Branch work = Branch.create("work");

            Files.write(FILE_TO_CREATE, CREATED_CONTENT.getBytes());
            Files.write(FILE_TO_UPDATE, V2.getBytes());
            stagingZone.add(new HashedFile(FILE_TO_UPDATE, Paths.get(".")));
            stagingZone.add(new HashedFile(FILE_TO_CREATE, Paths.get(".")));
            Commit workCommit = new Commit("w1");

            Repository.checkoutCommit(masterCommit1.getNumber());
            Merger.merge(work);

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
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            if (Files.exists(FILE_TO_KEEP)) {
                Files.delete(FILE_TO_KEEP);
            }
            if (Files.exists(FILE_TO_UPDATE)) {
                Files.delete(FILE_TO_UPDATE);
            }
            if (Files.exists(FILE_TO_CREATE)) {
                Files.delete(FILE_TO_CREATE);
            }
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
            Commit masterCommit1 = new Commit("v4");

            Files.write(FILE_UPDATED_IN_MASTER, "v5".getBytes());
            Files.write(FILE_UPDATED_IN_BOTH, "v5".getBytes());
            Files.write(FILE_CREATED_IN_MASTER, "v5".getBytes());
            Files.write(FILE_CREATED_IN_BOTH, "v5".getBytes());
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_MASTER, curDir));
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_BOTH, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_MASTER, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_BOTH, curDir));
            Commit masterCommit2 = new Commit("v5");

            Repository.checkoutCommit(masterCommit1.getNumber());
            Branch workBranch = Branch.create("work");
            Files.write(FILE_UPDATED_IN_WORK, "v6".getBytes());
            Files.write(FILE_UPDATED_IN_BOTH, "v6".getBytes());
            Files.write(FILE_CREATED_IN_WORK, "v6".getBytes());
            Files.write(FILE_CREATED_IN_BOTH, "v6".getBytes());
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_WORK, curDir));
            stagingZone.add(new HashedFile(FILE_UPDATED_IN_BOTH, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_WORK, curDir));
            stagingZone.add(new HashedFile(FILE_CREATED_IN_BOTH, curDir));
            Commit workCommit = new Commit("v6");

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
            if (Files.exists(FILE_CREATED_IN_BOTH)) {
                Files.delete(FILE_CREATED_IN_BOTH);
            }
            if (Files.exists(FILE_CREATED_IN_MASTER)) {
                Files.delete(FILE_CREATED_IN_MASTER);
            }
            if (Files.exists(FILE_CREATED_IN_WORK)) {
                Files.delete(FILE_CREATED_IN_WORK);
            }
            if (Files.exists(FILE_UPDATED_IN_BOTH)) {
                Files.delete(FILE_UPDATED_IN_BOTH);
            }
            if (Files.exists(FILE_UPDATED_IN_MASTER)) {
                Files.delete(FILE_UPDATED_IN_MASTER);
            }
            if (Files.exists(FILE_UPDATED_IN_WORK)) {
                Files.delete(FILE_UPDATED_IN_WORK);
            }
        }
    }
}
