package ru.spbau.gusev.vcs;

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
        final Path stageDir = Paths.get("stage_dir");
        final Path stageList = Paths.get("stage_list");
        Path filePath = Paths.get(FILE_NAME);

        try {
            Files.write(filePath, FILE_CONTENT.getBytes());
            Files.createDirectory(stageDir);
            Files.createFile(stageList);
            String expectedHash = FILE_NAME + " " + HashedFile.calcFileHash(FILE_NAME);

            StagingZone stagingZone = new StagingZone(stageDir, stageList);
            stagingZone.addFile(new HashedFile(filePath, Paths.get(".")));
            Assert.assertTrue(Files.exists(stageDir.resolve(FILE_NAME)));
            List<String> stageHashes = Files.readAllLines(stageList);
            Assert.assertEquals(Collections.singletonList(expectedHash), stageHashes);
        } finally {
            Files.delete(stageList);
            HashedDirectory.deleteDir(stageDir);
            Files.delete(filePath);
        }
    }

    @Test
    public void wipeTest() throws IOException, VCS.NoSuchFileException {
        final String FILE_NAME = "foo";
        final String FILE_CONTENT = "bar";
        final Path stageDir = Paths.get("stage_dir");
        final Path stageList = Paths.get("stage_list");
        Path filePath = stageDir.resolve(FILE_NAME);

        try {
            Files.createDirectory(stageDir);
            Files.write(filePath, FILE_CONTENT.getBytes());

            String hash = FILE_NAME + " " + HashedFile.calcFileHash(filePath.toString());
            Files.write(stageList, hash.getBytes());

            StagingZone stagingZone = new StagingZone(stageDir, stageList);
            stagingZone.wipe();

            Assert.assertEquals(Collections.emptyList(), Files.readAllLines(stageList));
            Assert.assertEquals(0, Files.list(stageDir).count());
        } finally {
            Files.delete(stageList);
            HashedDirectory.deleteDir(stageDir);
        }
    }

    @Test
    public void removeTest() throws IOException, VCS.NoSuchFileException {
        final String FILE_NAME = "foo";
        final String FILE_CONTENT = "bar";
        final Path stageDir = Paths.get("stage_dir");
        final Path stageList = Paths.get("stage_list");
        Path filePath = stageDir.resolve(FILE_NAME);

        try {
            Files.createDirectory(stageDir);
            Files.write(filePath, FILE_CONTENT.getBytes());

            String hash = FILE_NAME + " " + HashedFile.calcFileHash(filePath.toString());
            Files.write(stageList, hash.getBytes());

            StagingZone stagingZone = new StagingZone(stageDir, stageList);
            stagingZone.removeFile(Paths.get(FILE_NAME));

            Assert.assertEquals(Collections.emptyList(), Files.readAllLines(stageList));
            Assert.assertEquals(0, Files.list(stageDir).count());
        } finally {
            Files.delete(stageList);
            HashedDirectory.deleteDir(stageDir);
        }
    }
@Test
    public void containsTest() throws IOException, VCS.NoSuchFileException {
        final String FILE_NAME = "foo";
        final String FILE_CONTENT = "bar";
        final Path stageDir = Paths.get("stage_dir");
        final Path stageList = Paths.get("stage_list");
        Path filePath = stageDir.resolve(FILE_NAME);

        try {
            Files.createDirectory(stageDir);
            Files.write(filePath, FILE_CONTENT.getBytes());

            String hash = FILE_NAME + " " + HashedFile.calcFileHash(filePath.toString());
            Files.write(stageList, hash.getBytes());

            StagingZone stagingZone = new StagingZone(stageDir, stageList);
            Assert.assertTrue(stagingZone.contains(Paths.get(FILE_NAME)));
        } finally {
            Files.delete(stageList);
            HashedDirectory.deleteDir(stageDir);
        }
    }

    @Test
    public void getHashedFileTest() throws IOException, VCS.NoSuchFileException {
        final String FILE_NAME = "foo";
        final String FILE_CONTENT = "bar";
        final Path stageDir = Paths.get("stage_dir");
        final Path stageList = Paths.get("stage_list");
        Path stageFilePath = stageDir.resolve(FILE_NAME);
        Path filePath = Paths.get(FILE_NAME);

        try {
            Files.createDirectory(stageDir);
            Files.write(stageFilePath, FILE_CONTENT.getBytes());

            String hash = FILE_NAME + " " + HashedFile.calcFileHash(stageFilePath.toString());
            Files.write(stageList, hash.getBytes());

            StagingZone stagingZone = new StagingZone(stageDir, stageList);
            HashedFile hashedFile = stagingZone.getHashedFile(filePath);
            Assert.assertEquals(filePath, hashedFile.getPath());
            Assert.assertEquals(stageFilePath, hashedFile.getFullPath());
            Assert.assertEquals(stageDir, hashedFile.getDir());
            Assert.assertEquals(HashedFile.calcFileHash(stageFilePath.toString()),
                    hashedFile.getHash());
        } finally {
            Files.delete(stageList);
            HashedDirectory.deleteDir(stageDir);
        }
    }
}
