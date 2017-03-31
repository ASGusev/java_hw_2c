import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BranchTest {
    @Test
    public void creationDeletionTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.BranchAlreadyExistsException, VCS.BadRepoException,
            VCS.NoSuchBranchException, VCS.BadPositionException {
        final String BRANCH_NAME = "br";
        VCS.createRepo("usr");
        try {
            Branch created = Branch.create(BRANCH_NAME);
            Path branchDescPath = Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, BRANCH_NAME);
            Assert.assertTrue("Branch creation failure",
                    Files.exists(branchDescPath));

            Repository.setCurrentBranch(Branch.getByName(Repository.DEFAULT_BRANCH));
            created.delete();
            Assert.assertTrue("Branch deletion failure",
                    Files.notExists(branchDescPath));
        } finally {
            HashedDirectory.deleteDir(Paths.get(Repository.REPO_DIR_NAME));
        }
    }

    @Test
    public void commitAdditionTest() throws VCS.RepoAlreadyExistsException,
            VCS.BadPositionException, VCS.BadRepoException, IOException,
            VCS.NoSuchBranchException {
        try {
            Repository.create("usr");
            Branch masterBranch = Branch.getByName(Repository.DEFAULT_BRANCH);
            Assert.assertEquals(Repository.DEFAULT_BRANCH, masterBranch.getName());
            Commit commit = new Commit("test");
            masterBranch.addCommit(commit);
            List<String> masterCommits = Files.readAllLines(Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, masterBranch.getName()));
            Assert.assertEquals(commit.getNumber().toString(),
                    masterCommits.get(masterCommits.size() - 1));

            Assert.assertEquals(commit, masterBranch.getHead());
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
        }
    }
}
