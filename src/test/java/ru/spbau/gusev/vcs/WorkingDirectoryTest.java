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
    private static final Path TEST_DIR = Paths.get("test_dir");
    private static final Path CUR_DIR = Paths.get(".");

    @Test
    public void addTest() throws IOException {
        Path filePath = Paths.get("foo");
        String fileContent = "bar";

        try {
            Files.createDirectory(TEST_DIR);
            WorkingDirectory workingDirectory = new WorkingDirectory(TEST_DIR);
            Files.write(filePath, fileContent.getBytes());
            workingDirectory.add(new HashedFile(filePath, CUR_DIR));

            Assert.assertTrue(Files.isRegularFile(TEST_DIR.resolve(filePath)));
            Assert.assertEquals(Collections.singletonList(fileContent),
                    Files.readAllLines(TEST_DIR.resolve(filePath)));
        } finally {
            HashedDirectory.deleteDir(TEST_DIR);
            Files.delete(filePath);
        }
    }

    @Test
    public void deleteTest() throws IOException {
        Path filePath = Paths.get("foo");
        String fileContent = "bar";

        try {
            Files.createDirectory(TEST_DIR);
            WorkingDirectory workingDirectory = new WorkingDirectory(TEST_DIR);
            Files.write(TEST_DIR.resolve(filePath), fileContent.getBytes());
            workingDirectory.delete(filePath);

            Assert.assertTrue(Files.notExists(TEST_DIR.resolve(filePath)));
        } finally {
            HashedDirectory.deleteDir(TEST_DIR);
        }
    }

    @Test
    public void deleteIfTest() throws IOException {
        Path filePath1 = Paths.get("foo");
        Path filePath2 = Paths.get("bar");

        try {
            Files.createDirectory(TEST_DIR);
            WorkingDirectory workingDirectory = new WorkingDirectory(TEST_DIR);
            Files.createFile(TEST_DIR.resolve(filePath1));
            Files.createFile(TEST_DIR.resolve(filePath2));
            workingDirectory.deleteIf(filePath1::equals);

            Assert.assertTrue(Files.notExists(TEST_DIR.resolve(filePath1)));
            Assert.assertTrue(Files.exists(TEST_DIR.resolve(filePath2)));
        } finally {
            HashedDirectory.deleteDir(TEST_DIR);
        }
    }

    @Test
    public void getHashedFileTest() throws IOException, VCS.NoSuchFileException {
        Path filePath = Paths.get("foo");
        String fileContent = "bar";

        try {
            Files.createDirectory(TEST_DIR);
            WorkingDirectory workingDirectory = new WorkingDirectory(TEST_DIR);
            Files.write(TEST_DIR.resolve(filePath), fileContent.getBytes());

            HashedFile hashedFile = workingDirectory.getHashedFile(filePath.toString());
            Assert.assertEquals(TEST_DIR, hashedFile.getDir());
            Assert.assertEquals(filePath, hashedFile.getName());
            Assert.assertEquals(HashedFile.calcFileHash
                    (TEST_DIR.resolve(filePath).toString()), hashedFile.getHash());
        } finally {
            HashedDirectory.deleteDir(TEST_DIR);
        }
    }

    @Test
    public void getFilesTest() throws IOException {
        Path filePath1 = Paths.get("foo");
        Path filePath2 = Paths.get("bar");

        try {
            Files.createDirectory(TEST_DIR);
            Files.createFile(TEST_DIR.resolve(filePath1));
            Files.createFile(TEST_DIR.resolve(filePath2));
            Files.write(TEST_DIR.resolve(".ignore"), filePath2.toString().getBytes());
            WorkingDirectory workingDirectory = new WorkingDirectory(TEST_DIR);

            Stream<HashedFile> filesInDir = workingDirectory.getFiles();
            Assert.assertTrue(filesInDir.map(HashedFile::getName)
                    .allMatch(filePath1::equals));
        } finally {
            HashedDirectory.deleteDir(TEST_DIR);
        }
    }
}
