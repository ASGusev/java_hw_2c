package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class VCSTest {
    @Test
    public void getCreatedTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException {
        Path filePath = Paths.get("file");
        try {
            Repository.create("usr");
            Files.createFile(filePath);
            Assert.assertEquals(Collections.singletonList(filePath.toString()),
                    VCS.getCreated());
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            Files.deleteIfExists(filePath);
        }
    }

    @Test
    public void getStagedTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException, VCS.NoSuchFileException {
        Path filePath = Paths.get("file");
        try {
            Repository.create("usr");
            Files.createFile(filePath);
            VCS.addFile(filePath.toString());
            Assert.assertEquals(Collections.singletonList(filePath.toString()),
                    VCS.getStaged());
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            Files.deleteIfExists(filePath);
        }
    }

    @Test
    public void getChangedTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException, VCS.NoSuchFileException {
        Path filePath = Paths.get("file");
        try {
            Repository.create("usr");
            Files.createFile(filePath);
            VCS.addFile(filePath.toString());
            Files.write(filePath, "V1".getBytes());
            Assert.assertEquals(Collections.singletonList(filePath.toString()),
                    VCS.getChanged());
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            Files.deleteIfExists(filePath);
        }
    }

    @Test
    public void getRemovedTest() throws IOException, VCS.RepoAlreadyExistsException,
            VCS.BadRepoException, VCS.NoSuchFileException, VCS.NothingToCommitException, VCS.BadPositionException {
        Path filePath = Paths.get("file");
        try {
            Repository.create("usr");
            Files.createFile(filePath);
            VCS.addFile(filePath.toString());
            VCS.commit("ff");
            VCS.remove(filePath.toString());

            Assert.assertEquals(Collections.singletonList(filePath.toString()),
                    VCS.getRemoved());
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            Files.deleteIfExists(filePath);
        }
    }
}
