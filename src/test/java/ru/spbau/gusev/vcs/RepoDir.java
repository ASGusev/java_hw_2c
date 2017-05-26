package ru.spbau.gusev.vcs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepoDir implements AutoCloseable {
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

    public RepoDir() throws IOException {
        Path repoRoot = Paths.get(ROOT);

        Files.createDirectory(repoRoot);
        Files.createDirectory(repoRoot.resolve(BRANCHES));
        Files.createDirectory(repoRoot.resolve(COMMITS));
        Files.createDirectory(repoRoot.resolve(COMMITS_FILES));
        Files.createDirectory(repoRoot.resolve(STAGE));
        Files.createFile(repoRoot.resolve(BRANCHES).resolve(MASTER));

        Files.write(repoRoot.resolve(COMMIT), "1".getBytes());
        Files.createFile(repoRoot.resolve(COMMITS_FILES_LIST));
        Files.write(repoRoot.resolve(POSITION), (MASTER + "\n0").getBytes());
        Files.createFile(repoRoot.resolve(STAGE_LIST));
        Files.write(repoRoot.resolve(USER), USERNAME.getBytes());

        commit(0, MASTER, new ArrayList<>(), 0,
                "Initial commit.", -1);
    }


    @Override
    public void close() throws IOException {
        HashedDirectory.deleteDir(ROOT);
    }

    public void commit(Integer number, String branch, List<String> files,
                       long time, String message, Integer parent)
            throws IOException {
        Path commitRoot = Paths.get(ROOT, COMMITS, number.toString());
        Files.createDirectory(commitRoot);

        Files.write(commitRoot.resolve(FILES_LIST),
                String.join("\n", files).getBytes());

        try (BufferedWriter metadataWriter =
                Files.newBufferedWriter(commitRoot.resolve(METADATA))) {
            metadataWriter.write(String.valueOf(time) + "\n");
            metadataWriter.write(branch + "\n");
            metadataWriter.write(USERNAME + "\n");
            metadataWriter.write(parent.toString() + "\n");
            metadataWriter.write(message);
        }

        Files.write(Paths.get(ROOT, BRANCHES, branch),
                (number.toString() + "\n").getBytes(), StandardOpenOption.APPEND);
    }
}
