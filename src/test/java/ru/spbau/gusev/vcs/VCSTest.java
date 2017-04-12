package ru.spbau.gusev.vcs;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class VCSTest {
    private final Path ignorePath = Paths.get(".ignore");
    private final String ignoredFiles = ".vcs\n" +
            ".git\n" +
            "src\n" +
            "build\n" +
            "gradle\n" +
            ".gradle\n" +
            ".idea\n" +
            ".travis.yml\n" +
            ".gitignore\n" +
            "build.gradle\n" +
            "settings.gradle\n" +
            "gradlew\n" +
            "gradlew.bat\n" +
            "readme.md\n";

    @Before
    public void makeIgnore() throws IOException {
        Files.write(ignorePath, ignoredFiles.getBytes());
    }

    @After
    public void delIgnore() throws IOException {
        Files.deleteIfExists(ignorePath);
    }

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

    @Test
    public void cleanTest() throws VCS.RepoAlreadyExistsException, IOException,
            VCS.BadRepoException {
        Path filePath = Paths.get("file");

        try {
            Repository.create("usr");
            Files.createFile(filePath);
            VCS.clean();
            Assert.assertTrue(Files.notExists(filePath));
        } finally {
            HashedDirectory.deleteDir(Repository.REPO_DIR_NAME);
            Files.deleteIfExists(filePath);
        }
    }
}
