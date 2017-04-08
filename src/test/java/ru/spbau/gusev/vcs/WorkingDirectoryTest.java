package ru.spbau.gusev.vcs;


import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

public class WorkingDirectoryTest {
    Path testDir = Paths.get("test_dir");
    Path curDir = Paths.get(".");

    @Test
    public void addTest() throws IOException {
        Path filePath = Paths.get("foo");
        String fileContent = "bar";

        try {
            Files.createDirectory(testDir);
            WorkingDirectory workingDirectory = new WorkingDirectory(testDir);
            Files.write(filePath, fileContent.getBytes());
            workingDirectory.add(new HashedFile(filePath, curDir));

            Assert.assertTrue(Files.isRegularFile(testDir.resolve(filePath)));
            Assert.assertEquals(Collections.singletonList(fileContent),
                    Files.readAllLines(testDir.resolve(filePath)));
        } finally {
            HashedDirectory.deleteDir(testDir);
            Files.delete(filePath);
        }
    }

    @Test
    public void deleteTest() throws IOException {
        Path filePath = Paths.get("foo");
        String fileContent = "bar";

        try {
            Files.createDirectory(testDir);
            WorkingDirectory workingDirectory = new WorkingDirectory(testDir);
            Files.write(testDir.resolve(filePath), fileContent.getBytes());
            workingDirectory.deleteFile(filePath);

            Assert.assertTrue(Files.isRegularFile(testDir.resolve(filePath)));
            Assert.assertEquals(Collections.singletonList(fileContent),
                    Files.readAllLines(testDir.resolve(filePath)));
        } finally {
            HashedDirectory.deleteDir(testDir);
        }
    }

    @Test
    public void deleteIfTest() throws IOException {
        Path filePath1 = Paths.get("foo");
        Path filePath2 = Paths.get("bar");

        try {
            Files.createDirectory(testDir);
            WorkingDirectory workingDirectory = new WorkingDirectory(testDir);
            Files.createFile(testDir.resolve(filePath1));
            Files.createFile(testDir.resolve(filePath2));
            workingDirectory.deleteIf(filePath1::equals);

            Assert.assertTrue(Files.notExists(testDir.resolve(filePath1)));
            Assert.assertTrue(Files.exists(testDir.resolve(filePath2)));
        } finally {
            HashedDirectory.deleteDir(testDir);
        }
    }

    @Test
    public void getHashedFileTest() throws IOException, VCS.NoSuchFileException {
        Path filePath = Paths.get("foo");
        String fileContent = "bar";

        try {
            Files.createDirectory(testDir);
            WorkingDirectory workingDirectory = new WorkingDirectory(testDir);
            Files.write(testDir.resolve(filePath), fileContent.getBytes());

            HashedFile hashedFile = workingDirectory.getHashedFile(filePath.toString());
            Assert.assertEquals(testDir, hashedFile.getDir());
            Assert.assertEquals(filePath, hashedFile.getPath());
            Assert.assertEquals(HashedFile.calcFileHash
                    (testDir.resolve(filePath).toString()), hashedFile.getHash());
        } finally {
            HashedDirectory.deleteDir(testDir);
        }
    }

    @Test
    public void getFilesTest() throws IOException {
        Path filePath1 = Paths.get("foo");
        Path filePath2 = Paths.get("bar");

        try {
            Files.createDirectory(testDir);
            Files.createFile(testDir.resolve(filePath1));
            Files.createFile(testDir.resolve(filePath2));
            Files.write(testDir.resolve(".ignore"), filePath2.toString().getBytes());
            WorkingDirectory workingDirectory = new WorkingDirectory(testDir);

            Stream<HashedFile> filesInDir = workingDirectory.getFiles();
            Assert.assertTrue(filesInDir.map(HashedFile::getPath)
                    .allMatch(filePath1::equals));
        } finally {
            HashedDirectory.deleteDir(testDir);
        }
    }
}
