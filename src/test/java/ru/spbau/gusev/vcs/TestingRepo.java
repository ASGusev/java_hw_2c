package ru.spbau.gusev.vcs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestingRepo implements AutoCloseable {
    protected static final String ROOT = ".vcs";
    protected static final String BRANCHES = "branches";
    protected static final String MASTER = "master";
    protected static final String COMMITS = "commits";
    protected static final String METADATA = "metadata";
    protected static final String FILES_LIST = "files_list";
    protected static final String COMMITS_FILES = "commits_files";
    protected static final String COMMIT = "commit";
    protected static final String COMMITS_FILES_LIST = "commits_files_list";
    protected static final String POSITION = "position";
    protected static final String STAGE_LIST = "stage_list";
    protected static final String USER = "user";
    protected static final String USERNAME = "usr";
    protected static final String STAGE = "stage";

    public TestingRepo() throws IOException {
        Path repoRoot = Paths.get(ROOT);

        Files.createDirectory(repoRoot);
        Files.createDirectory(repoRoot.resolve(BRANCHES));
        Files.write(repoRoot.resolve(BRANCHES).resolve(MASTER), "0".getBytes());
        Files.createDirectory(repoRoot.resolve(COMMITS));
        Files.createDirectory(repoRoot.resolve(COMMITS).resolve("0"));
        Files.createFile(repoRoot.resolve(COMMITS).resolve("0")
                .resolve(FILES_LIST));
        Files.write(repoRoot.resolve(COMMITS).resolve("0").resolve(METADATA),
                ("0\n"+ MASTER + "\n" + USERNAME + "\n-1\nInitial commit.").
                        getBytes());
        Files.createDirectory(repoRoot.resolve(COMMITS_FILES));
        Files.createDirectory(repoRoot.resolve(STAGE));

        Files.write(repoRoot.resolve(COMMIT), "1".getBytes());
        Files.createFile(repoRoot.resolve(COMMITS_FILES_LIST));
        Files.write(repoRoot.resolve(POSITION), (MASTER + "\n0").getBytes());
        Files.createFile(repoRoot.resolve(STAGE_LIST));
        Files.write(repoRoot.resolve(USER), USERNAME.getBytes());
    }


    @Override
    public void close() throws IOException {
        HashedDirectory.deleteDir(ROOT);
    }
}
