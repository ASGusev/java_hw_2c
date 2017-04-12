package ru.spbau.gusev.vcs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class IntersectedFolderTest {
    private IntersectedFolderStorage mockedStorage;
    private final Path folderList = Paths.get("folder_list");
    private final Path file = Paths.get("file");
    private final String fileContent = "ddd";
    private final Path curDir = Paths.get(".");
    /*
    @Before
    public void makeMock() throws VCS.NoSuchFileException {
        mockedStorage = Mockito.mock(IntersectedFolderStorage.class);
        Mockito.when(mockedStorage.add(Mockito.any())).thenAnswer(inv ->
                inv.getArgument(0));
        Mockito.when(mockedStorage.getFile(Mockito.anyString(), Mockito.any()))
                .thenAnswer(inv -> {
                    String hash = inv.getArgument(0);
                    TrackedFile ret = Mockito.mock(TrackedFile.class);
                    Mockito.when(ret.getHash()).thenReturn(hash);
                    return ret;
                });
    }

    @Test
    public void addTest() throws IOException {
        try {
            Files.write(file, fileContent.getBytes());

            HashedFile hashedFile = new HashedFile(file, curDir);

            IntersectedFolder folder = new IntersectedFolder(mockedStorage, folderList);

            folder.add(hashedFile);
            folder.writeList();

            List<String> expectedList = Collections.singletonList(file + " " +
                    hashedFile.getHash());

            Assert.assertEquals(expectedList, Files.readAllLines(folderList));
        } finally {
            if (Files.exists(folderList)) {
                Files.delete(folderList);
            }
            if (Files.exists(file)) {
                Files.delete(file);
            }
        }
    }

    @Test
    public void getFileTest() throws IOException {
        try {
            Files.write(file, fileContent.getBytes());
            String hash = HashedFile.calcFileHash(file.toString());

            Files.write(folderList, (file.toString() + " " + hash + "\n").getBytes());

            IntersectedFolder folder = new IntersectedFolder(mockedStorage, folderList);

            Assert.assertEquals(hash, folder.getFile(file).getHash());
        } finally {
            if (Files.exists(file)) {
                Files.delete(file);
            }
            if (Files.exists(folderList)) {
                Files.delete(folderList);
            }
        }
    }

    @Test
    public void deleteTest() throws VCS.NoSuchFileException, IOException {
        try {
            Files.write(file, fileContent.getBytes());

            Files.write(folderList, (file.toString() + " " +
                    HashedFile.calcFileHash(file.toString()) + "\n").getBytes());

            IntersectedFolder folder = new IntersectedFolder(mockedStorage, folderList);
            folder.delete(file);
            folder.writeList();

            Assert.assertTrue(Files.readAllLines(folderList).isEmpty());
            Mockito.verify(mockedStorage).delete(Mockito.anyString());
        } finally {
            if (Files.exists(file)) {
                Files.delete(file);
            }
            if (Files.exists(folderList)) {
                Files.delete(folderList);
            }
        }
    }
    */
}
