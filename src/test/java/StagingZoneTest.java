import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class StagingZoneTest {
    @Test
    public void addFileTest() throws IOException, VCS.NoSuchFileException,
            VCS.RepoAlreadyExistsException {
        final String FILE_NAME = "foo";
        final String FILE_CONTENT = "bar";
        Path filePath = Paths.get(FILE_NAME);

        try {
            Repository.create("usr");
            Files.write(filePath, FILE_CONTENT.getBytes());
            String expectedHash = FILE_NAME + " " + HashedFile.calcFileHash(FILE_NAME);

            StagingZone.addFile(filePath);
            Assert.assertTrue(Files.exists(Paths.get(Repository.REPO_DIR_NAME,
                    StagingZone.STAGE_DIR, FILE_NAME)));
            List<String> stageHashes = Files.readAllLines(
                    Paths.get(Repository.REPO_DIR_NAME, StagingZone.STAGE_LIST));
            Assert.assertEquals(Collections.singletonList(expectedHash), stageHashes);
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            Files.delete(filePath);
        }
    }
}
