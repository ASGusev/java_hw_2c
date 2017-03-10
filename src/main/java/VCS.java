import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class VCS {
    private static final String REPO_DIR_NAME = ".vcs";
    private static final String BRANCHES_DIR_NAME = "branches";
    private static final String COMMITS_DIR_NAME = "commits";
    private static final String USERNAME_FILE = "user";
    private static final String COMMITS_COUNTER_FILENAME = "commit";
    private static final String POSITION_FILENAME = "position";
    private static final String COMMIT_CONTENT_DIR = "content";
    private static final String COMMIT_METADATA_FILE = "metadata";

    public static void createRepo(String author) throws RepoExistsException, IOException {
        if (Files.exists(Paths.get(REPO_DIR_NAME), LinkOption.NOFOLLOW_LINKS)) {
            throw new RepoExistsException();
        }

        Files.createDirectory(Paths.get(REPO_DIR_NAME));

        Files.createDirectory(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME));
        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME));

        //Files.write(Paths.get(REPO_DIR_NAME, USERNAME_FILE), author);
        FileWriter authorWriter = new FileWriter(REPO_DIR_NAME +
                '/' + USERNAME_FILE);
        authorWriter.write(author);
        authorWriter.close();

        //Files.createFile(Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME));
        FileWriter commitsWriter = new FileWriter(REPO_DIR_NAME +
                '/' + COMMITS_COUNTER_FILENAME);
        commitsWriter.write("0");
        commitsWriter.close();

        //Files.createFile(Paths.get(REPO_DIR_NAME, POSITION_FILENAME));
        FileWriter positionWriter = new FileWriter(REPO_DIR_NAME + "/" +
                POSITION_FILENAME);
        positionWriter.write("\n");
        positionWriter.write("0");
        positionWriter.close();
    }

    public static void setUser(String name) throws BadRepoException, IOException {
        if (!Files.exists(Paths.get(REPO_DIR_NAME, USERNAME_FILE))) {
            throw new BadRepoException();
        }
        FileWriter writer = new FileWriter(REPO_DIR_NAME + "/" + USERNAME_FILE);
        writer.write(name);
        writer.close();
    }

    public static void removeRepo() throws IOException {
        deleteDir(REPO_DIR_NAME);
    }

    public static void commit(String message) throws IOException, BadRepoException {
        if (Files.notExists(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME))) {
            throw new BadRepoException();
        }

        Integer number = getCommitsNumber();

        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                number.toString()));

        BufferedWriter metadataWriter = new BufferedWriter(new FileWriter(
                Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                        number.toString(), COMMIT_METADATA_FILE).toString()));
        metadataWriter.write(LocalDateTime.now().
                format(DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.SHORT, FormatStyle.MEDIUM)));
        metadataWriter.write(getUserName());
        metadataWriter.write(message);
        metadataWriter.close();

        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                number.toString(), COMMIT_CONTENT_DIR));
    }

    private static void deleteDir(String dirName) throws IOException {
        deleteDir(Paths.get(dirName));
    }

    private static void deleteDir(Path dir) throws IOException {
        Stream<Path> content = Files.list(dir);
        content.forEach(path -> {
            try {
                if (Files.isDirectory(path)) {
                    deleteDir(path);
                } else {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new VCS.FileSystemError();
            }
        });
        content.close();
        Files.delete(dir);
    }

    private static int getCommitsNumber() throws BadRepoException, IOException {
        List<String> lines = Files.readAllLines(
                Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME));
        if (lines.size() != 1) {
            throw new BadRepoException();
        }
        int commitsNumber = -1;
        try {
            commitsNumber = Integer.valueOf(lines.get(0));
        } catch (NumberFormatException e) {
            throw new BadRepoException();
        }
        return commitsNumber;
    }

    private static String getUserName() throws IOException {
        BufferedReader reader = new BufferedReader(
                new FileReader(Paths.get(REPO_DIR_NAME, USERNAME_FILE).toString()));
        String username = reader.readLine();
        reader.close();
        return username;
    }

    public static class RepoExistsException extends Exception {};

    public static class BadRepoException extends Exception{};

    public static class FileSystemError extends Error{};
}
