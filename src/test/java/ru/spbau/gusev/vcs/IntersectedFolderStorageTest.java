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

public class IntersectedFolderStorageTest {
    private static final Path TEST_FOLDER = Paths.get("test_folder");
    private static final Path TEST_LIST = Paths.get("test_list");
    private static final Path CUR_DIR = Paths.get(".");
    private IntersectedFolderStorage storage;

    @Before
    public void prepare() throws IOException {
        Files.createDirectory(TEST_FOLDER);
        Files.createFile(TEST_LIST);
        storage = new IntersectedFolderStorage(TEST_FOLDER, TEST_LIST);
    }

    @After
    public void finish() throws IOException {
        Files.delete(TEST_LIST);
        HashedDirectory.deleteDir(TEST_FOLDER);
        storage = null;
    }

    @Test
    public void addTest() throws IOException {
        final Path filePath1 = Paths.get("file1");
        final Path filePath2 = Paths.get("file2");
        final Path filePath3 = Paths.get("file3");
        final String fileContent1 = "content1";
        final String fileContent2 = "content2";

        try {
            Files.write(filePath1, fileContent1.getBytes());
            Files.write(filePath2, fileContent2.getBytes());
            Files.write(filePath3, fileContent2.getBytes());

            // Adding files
            HashedFile hashedFile = new HashedFile(filePath1, CUR_DIR);
            TrackedFile sharedHashedFile = storage.add(hashedFile);
            storage.add(new HashedFile(filePath2, CUR_DIR));
            storage.add(new HashedFile(filePath3, CUR_DIR));
            storage.writeCounters();

            // Checking that both files sre in the folder.
            String hash1 = hashedFile.getHash();
            String hash2 = HashedFile.calcFileHash(filePath2.toString());
            Assert.assertTrue(Files.exists(TEST_FOLDER.resolve(hash1)));
            Assert.assertEquals(Collections.singletonList(fileContent1),
                    Files.readAllLines(TEST_FOLDER.resolve(hash1)));
            Assert.assertTrue(Files.exists(TEST_FOLDER.resolve(hash2)));
            Assert.assertEquals(Collections.singletonList(fileContent2),
                    Files.readAllLines(TEST_FOLDER.resolve(hash2)));

            // Checking that both files are in the list.
            Assert.assertEquals(2, Files.readAllLines(TEST_LIST).size());
            Assert.assertTrue(Files.readAllLines(TEST_LIST).contains(hash1 + " 1"));
            Assert.assertTrue(Files.readAllLines(TEST_LIST).contains(hash2 + " 2"));

            //Checking that the returned SharedHashedFile is correct.
            Assert.assertEquals(hash1, sharedHashedFile.getHash());
            Assert.assertEquals(filePath1, sharedHashedFile.getName());
        } finally {
            Files.delete(filePath1);
            Files.delete(filePath2);
            Files.delete(filePath3);
        }
    }

    @Test
    public void getTest() throws IOException, VCS.NoSuchFileException {
        final Path filePath = Paths.get("file");
        final String fileContent = "content";

        try {
            Files.write(filePath, fileContent.getBytes());
            HashedFile hashedFile = new HashedFile(filePath, CUR_DIR);
            storage.add(hashedFile);

            String hash = hashedFile.getHash();
            TrackedFile sharedHashedFile = storage.getFile(hash, filePath);

            Assert.assertEquals(hash, sharedHashedFile.getHash());
            Assert.assertEquals(filePath, sharedHashedFile.getName());
        } finally {
            Files.delete(filePath);
        }
    }

    @Test
    public void deleteTest() throws IOException, VCS.NoSuchFileException {
        final Path filePath1 = Paths.get("file1");
        final Path filePath2 = Paths.get("file2");
        final Path filePath3 = Paths.get("file3");
        final String fileContent1 = "content1";
        final String fileContent2 = "content2";

        try {
            Files.write(filePath1, fileContent1.getBytes());
            Files.write(filePath2, fileContent2.getBytes());
            Files.write(filePath3, fileContent2.getBytes());

            storage.add(new HashedFile(filePath1, CUR_DIR));
            storage.add(new HashedFile(filePath2, CUR_DIR));
            storage.add(new HashedFile(filePath3, CUR_DIR));

            String hash1 = HashedFile.calcFileHash(filePath1.toString());
            String hash2 = HashedFile.calcFileHash(filePath2.toString());

            storage.delete(hash1);
            storage.delete(hash2);
            storage.writeCounters();

            Assert.assertTrue(Files.notExists(TEST_FOLDER.resolve(hash1)));
            Assert.assertTrue(Files.exists(TEST_FOLDER.resolve(hash2)));

            Assert.assertEquals(Collections.singletonList(hash2 + " 1"),
                    Files.readAllLines(TEST_LIST));
        } finally {
            Files.delete(filePath1);
            Files.delete(filePath2);
            Files.delete(filePath3);
        }
    }
}
