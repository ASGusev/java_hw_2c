import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BranchTest {
    @Test
    public void creationDeletionTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.BranchAlreadyExistsException, VCS.BadRepoException,
            VCS.NoSuchBranchException, VCS.BadPositionException {
        final String BRANCH_NAME = "br";
        VCS.createRepo("usr");
        try {
            Branch.create(BRANCH_NAME);
            Path branchDescPath = Paths.get(Repository.REPO_DIR_NAME,
                    Repository.BRANCHES_DIR_NAME, BRANCH_NAME);
            Assert.assertTrue("Branch creation failure",
                    Files.exists(branchDescPath));
            Branch created = Branch.getByName(BRANCH_NAME);

            created.delete();
            Assert.assertTrue("Branch deletion failure",
                    Files.notExists(branchDescPath));
        } finally {
            HashedDirectory.deleteDir(Paths.get(Repository.REPO_DIR_NAME));
        }
    }
}
