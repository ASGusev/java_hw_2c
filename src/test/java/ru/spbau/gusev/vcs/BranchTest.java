package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BranchTest {
    @Test
    public void creationDeletionTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.BranchAlreadyExistsException, VCS.BadRepoException,
            VCS.NoSuchBranchException, VCS.BadPositionException {
        final String BRANCH_NAME = "br";

        try (RepoMock repo = new RepoMock()) {
            Branch created = Branch.create(BRANCH_NAME, 0);
            Path branchDescPath = Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, BRANCH_NAME);
            Assert.assertTrue("Branch creation failure",
                    Files.exists(branchDescPath));

            Files.write(Paths.get(RepoMock.ROOT, RepoMock.POSITION),
                    (RepoMock.MASTER + "\n0").getBytes());
            created.delete();
            Assert.assertTrue("Branch deletion failure",
                    Files.notExists(branchDescPath));
        }
    }

    @Test
    public void commitAdditionTest() throws VCS.RepoAlreadyExistsException,
            VCS.BadPositionException, VCS.BadRepoException, IOException,
            VCS.NoSuchBranchException, VCS.NoSuchCommitException {
        try (RepoMock repo = new RepoMock()) {
            Branch masterBranch = Branch.getByName(Repository.DEFAULT_BRANCH);
            Assert.assertEquals(Repository.DEFAULT_BRANCH, masterBranch.getName());
            repo.commit(1, RepoMock.MASTER, new ArrayList<>(),
                    0, "msg", 0);
            Commit commit = Commit.read(1);
            masterBranch.addCommit(commit);
            List<String> masterCommits = Files.readAllLines(Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, masterBranch.getName()));
            Assert.assertEquals(String.valueOf(1),
                    masterCommits.get(masterCommits.size() - 1));

            Assert.assertEquals(commit, masterBranch.getHead());
        }
    }

    @Test
    public void logTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadPositionException, VCS.BadRepoException, VCS.NoSuchBranchException {
        final String COMMIT_MESSAGE = "msg";

        try (RepoMock repo = new RepoMock()) {
            long time = System.currentTimeMillis();
            repo.commit(1, RepoMock.MASTER, new ArrayList<>(), time,
                    COMMIT_MESSAGE, 0);
            Branch branch = Branch.getByName(RepoMock.MASTER);
            List<VCS.CommitDescription> log = branch.getLog();
            VCS.CommitDescription commitDescription = log.get(0);

            Assert.assertEquals(1, commitDescription.getNumber());
            Assert.assertEquals(RepoMock.USERNAME, commitDescription.getAuthor());
            Assert.assertEquals(RepoMock.MASTER, commitDescription.getBranch());
            Assert.assertEquals(COMMIT_MESSAGE, commitDescription.getMessage());
            Assert.assertEquals(time, commitDescription.getTime().getTimeInMillis());
        }
    }
}
