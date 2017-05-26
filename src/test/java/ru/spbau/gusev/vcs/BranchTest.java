package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

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

        try (RepoDir repo = new RepoDir()) {
            Branch created = Branch.create(BRANCH_NAME, 0,
                    Repository.getExisting());
            Path branchDescPath = Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, BRANCH_NAME);
            Assert.assertTrue("Branch creation failure",
                    Files.exists(branchDescPath));

            Files.write(Paths.get(RepoDir.ROOT, RepoDir.POSITION),
                    (RepoDir.MASTER + "\n0").getBytes());
            created.delete();
            Assert.assertTrue("Branch deletion failure",
                    Files.notExists(branchDescPath));
        }
    }

    @Test
    public void commitAdditionTest() throws VCS.RepoAlreadyExistsException,
            VCS.BadPositionException, VCS.BadRepoException, IOException,
            VCS.NoSuchBranchException, VCS.NoSuchCommitException {
        try (RepoDir repo = new RepoDir()) {
            Repository repository = Mockito.mock(Repository.class);

            Branch masterBranch = Branch.getByName(Repository.DEFAULT_BRANCH,
                    repository);
            Assert.assertEquals(Repository.DEFAULT_BRANCH, masterBranch.getName());
            repo.commit(1, RepoDir.MASTER, new ArrayList<>(),
                    0, "msg", 0);
            Commit commit = Commit.read(1, repository);
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

        try (RepoDir repo = new RepoDir()) {
            long time = System.currentTimeMillis();
            repo.commit(1, RepoDir.MASTER, new ArrayList<>(), time,
                    COMMIT_MESSAGE, 0);
            Branch branch = Branch.getByName(RepoDir.MASTER,
                    Repository.getExisting());
            List<VCS.CommitDescription> log = branch.getLog();
            VCS.CommitDescription commitDescription = log.get(0);

            Assert.assertEquals(1, commitDescription.getNumber());
            Assert.assertEquals(RepoDir.USERNAME, commitDescription.getAuthor());
            Assert.assertEquals(RepoDir.MASTER, commitDescription.getBranch());
            Assert.assertEquals(COMMIT_MESSAGE, commitDescription.getMessage());
            Assert.assertEquals(time, commitDescription.getTime().getTimeInMillis());
        }
    }
}
