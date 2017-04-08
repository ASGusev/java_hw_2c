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
import java.util.stream.Stream;

public class HashedDirectoryTest {
    private static final Path TEST_DIR = Paths.get("test_dir");
    private static final Path TEST_LIST = Paths.get("test_list");
    private static final Path CUR_DIR = Paths.get(".");
    private HashedDirectory hashedDirectory;

    @Before
    public void prepare() throws IOException {
        Files.createDirectory(TEST_DIR);
        Files.createFile(TEST_LIST);
        hashedDirectory = new HashedDirectory(TEST_DIR, TEST_LIST);
    }

    @After
    public void finish() throws IOException {
        if (Files.isDirectory(TEST_DIR)) {
            HashedDirectory.deleteDir(TEST_DIR);
        }
        if (Files.isRegularFile(TEST_LIST)) {
            Files.delete(TEST_LIST);
        }
        hashedDirectory = null;
    }

    @Test
    public void addTest() throws IOException {
        final Path filePath = Paths.get("foo");
        final String fileContent = "bar";

        try {
            Files.write(filePath, fileContent.getBytes());
            HashedFile hashedFile = new HashedFile(filePath, CUR_DIR);
            hashedDirectory.add(hashedFile);
            hashedDirectory.flushHashes();

            String expectedHash = filePath.toString() + ' ' +
                    HashedFile.calcFileHash(filePath.toString());

            Assert.assertTrue(Files.exists(TEST_DIR.resolve(filePath)));
            Assert.assertEquals(Collections.singletonList(fileContent),
                    Files.readAllLines(TEST_DIR.resolve(filePath)));
            Assert.assertEquals(Collections.singletonList(expectedHash),
                    Files.readAllLines(TEST_LIST));
        } finally {
            Files.delete(filePath);
        }
    }

    @Test
    public void clearTest() throws IOException, VCS.NoSuchFileException {
        final Path filePath = Paths.get("foo");
        final String fileContent = "bar";

        try {
            Files.write(filePath, fileContent.getBytes());
            HashedFile hashedFile = new HashedFile(filePath, CUR_DIR);
            hashedDirectory.add(hashedFile);
            hashedDirectory.clear();
            hashedDirectory.flushHashes();

            Assert.assertTrue(Files.notExists(TEST_DIR.resolve(filePath)));
            Assert.assertEquals(Collections.emptyList(),
                    Files.readAllLines(TEST_LIST));
        } finally {
            Files.delete(filePath);
        }
    }

    @Test
    public void deleteTest() throws IOException, VCS.NoSuchFileException {
        final Path filePath = Paths.get("foo");
        final String fileContent = "bar";

        try {
            Files.write(filePath, fileContent.getBytes());
            HashedFile hashedFile = new HashedFile(filePath, CUR_DIR);
            hashedDirectory.add(hashedFile);
            hashedDirectory.deleteFile(filePath);
            hashedDirectory.flushHashes();

            Assert.assertTrue(Files.notExists(TEST_DIR.resolve(filePath)));
            Assert.assertEquals(Collections.emptyList(),
                    Files.readAllLines(TEST_LIST));
        } finally {
            Files.delete(filePath);
        }
    }

    @Test
    public void containsTest() throws IOException {
        final Path filePath = Paths.get("foo");
        final String fileContent = "bar";

        try {
            Files.write(filePath, fileContent.getBytes());
            HashedFile hashedFile = new HashedFile(filePath, CUR_DIR);
            hashedDirectory.add(hashedFile);

            Assert.assertTrue(hashedDirectory.contains(filePath));
        } finally {
            Files.delete(filePath);
        }
    }

    @Test
    public void getFilesTest() throws IOException {
        final Path filePath = Paths.get("file");

        try {
            Files.createFile(filePath);

            hashedDirectory.add(new HashedFile(filePath, CUR_DIR));
            Stream<Path> files = hashedDirectory.getFiles()
                    .map(HashedFile::getPath);

            Assert.assertTrue(files.allMatch(filePath::equals));
            Assert.assertEquals(1, hashedDirectory.getFiles().count());
        } finally {
            Files.delete(filePath);
        }
    }
}
