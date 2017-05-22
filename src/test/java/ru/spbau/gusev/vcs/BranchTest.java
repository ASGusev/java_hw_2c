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

        try (TestingRepo repo = new TestingRepo()) {
            Branch created = Branch.create(BRANCH_NAME);
            Path branchDescPath = Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, BRANCH_NAME);
            Assert.assertTrue("Branch creation failure",
                    Files.exists(branchDescPath));

            Files.write(Paths.get(TestingRepo.ROOT, TestingRepo.POSITION),
                    (TestingRepo.MASTER + "\n0").getBytes());
            created.delete();
            Assert.assertTrue("Branch deletion failure",
                    Files.notExists(branchDescPath));
        }
    }

    @Test
    public void commitAdditionTest() throws VCS.RepoAlreadyExistsException,
            VCS.BadPositionException, VCS.BadRepoException, IOException,
            VCS.NoSuchBranchException, VCS.NoSuchCommitException {
        try (TestingRepo repo = new TestingRepo()) {
            Branch masterBranch = Branch.getByName(Repository.DEFAULT_BRANCH);
            Assert.assertEquals(Repository.DEFAULT_BRANCH, masterBranch.getName());
            repo.commit(1, TestingRepo.MASTER, new ArrayList<>(),
                    0, "msg");
            Commit commit = new Commit(1);
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

        try (TestingRepo repo = new TestingRepo()) {
            long time = System.currentTimeMillis();
            repo.commit(1, TestingRepo.MASTER, new ArrayList<>(), time,
                    COMMIT_MESSAGE);
            Branch branch = Branch.getByName(TestingRepo.MASTER);
            List<VCS.CommitDescription> log = branch.getLog();
            VCS.CommitDescription commitDescription = log.get(0);

            Assert.assertEquals(1, commitDescription.getNumber());
            Assert.assertEquals(TestingRepo.USERNAME, commitDescription.getAuthor());
            Assert.assertEquals(TestingRepo.MASTER, commitDescription.getBranch());
            Assert.assertEquals(COMMIT_MESSAGE, commitDescription.getMessage());
            Assert.assertEquals(time, commitDescription.getTime().getTimeInMillis());
        }
    }
}
