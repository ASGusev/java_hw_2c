import com.sun.org.apache.regexp.internal.RE;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Stream;

public class VCS {
    private static final String REPO_DIR_NAME = ".vcs";
    private static final String BRANCHES_DIR_NAME = "branches";
    private static final String COMMITS_DIR_NAME = "commits";
    private static final String USERNAME_FILE = "user";
    private static final String COMMITS_COUNTER_FILENAME = "commit";
    private static final String POSITION_FILENAME = "position";
    private static final String STAGE_LIST = "stage_list";
    private static final String STAGE_DIR = "stage";
    private static final String COMMIT_CONTENT_DIR = "content";
    private static final String COMMIT_METADATA_FILE = "metadata";
    private static final String COMMIT_FILES_LIST = "files_list";

    public static void createRepo(String author) throws RepoExistsException, IOException {
        if (Files.exists(Paths.get(REPO_DIR_NAME), LinkOption.NOFOLLOW_LINKS)) {
            throw new RepoExistsException();
        }

        Files.createDirectory(Paths.get(REPO_DIR_NAME));

        //Initialising master branch
        Files.createDirectory(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME));
        FileWriter branchWriter = new FileWriter(Paths.get(REPO_DIR_NAME,
                BRANCHES_DIR_NAME, "master").toString());
        branchWriter.write("0\n");
        branchWriter.close();

        //Preparing initial commit
        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME));
        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                "0"));
        BufferedWriter metadataWriter = new BufferedWriter(new FileWriter(
                Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                        "0", COMMIT_METADATA_FILE).toString()));
        metadataWriter.write(LocalDateTime.now().
                format(DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.SHORT, FormatStyle.MEDIUM)));
        metadataWriter.write(author);
        metadataWriter.write("Initial commit");
        metadataWriter.close();
        Files.createFile(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME, "0",
                COMMIT_FILES_LIST));
        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME, "0",
                COMMIT_CONTENT_DIR));

        //Setting username
        FileWriter authorWriter = new FileWriter(REPO_DIR_NAME +
                '/' + USERNAME_FILE);
        authorWriter.write(author);
        authorWriter.close();

        //Setting up commit counter
        FileWriter commitsWriter = new FileWriter(REPO_DIR_NAME +
                '/' + COMMITS_COUNTER_FILENAME);
        commitsWriter.write("0");
        commitsWriter.close();

        //Setting up position tracking
        FileWriter positionWriter = new FileWriter(REPO_DIR_NAME + "/" +
                POSITION_FILENAME);
        positionWriter.write("master\n0");
        positionWriter.close();

        //Creating stage directory
        Files.createDirectory(Paths.get(REPO_DIR_NAME, STAGE_DIR));
    }

    public static void setUser(String name) throws BadRepoException, IOException {
        if (!Files.exists(Paths.get(REPO_DIR_NAME, USERNAME_FILE))) {
            throw new BadRepoException();
        }
        FileWriter writer = new FileWriter(REPO_DIR_NAME + "/" + USERNAME_FILE);
        writer.write(name);
        writer.close();
    }

    public static void commit(String message) throws IOException, BadRepoException {
        if (Files.notExists(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME))) {
            throw new BadRepoException();
        }

        Integer number = getCommitsNumber() + 1;
        List<String> prevPos = Files.readAllLines(Paths.get(REPO_DIR_NAME,
                POSITION_FILENAME));
        Integer pos = Integer.valueOf(prevPos.get(1));
        String branch = prevPos.get(0);

        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                number.toString()));

        BufferedWriter metadataWriter = new BufferedWriter(new FileWriter(
                Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                        number.toString(), COMMIT_METADATA_FILE).toString()));
        metadataWriter.write(LocalDateTime.now().
                format(DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.SHORT, FormatStyle.MEDIUM)) + '\n');
        metadataWriter.write(branch + '\n');
        metadataWriter.write(getUserName() + '\n');
        metadataWriter.write(message);
        metadataWriter.close();

        BufferedWriter filesListWriter = new BufferedWriter(new FileWriter(
                Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME, number.toString(),
                        COMMIT_FILES_LIST).toString()
        ));

        Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                number.toString(), COMMIT_CONTENT_DIR));

        Set<String> staged = new HashSet<>();
        Path contentDirPath = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                number.toString(), COMMIT_CONTENT_DIR);
        Path stageDirPath = Paths.get(REPO_DIR_NAME, STAGE_DIR);
        Files.walk(Paths.get(REPO_DIR_NAME, STAGE_DIR)).forEach(path -> {
            Path relativePath = stageDirPath.relativize(path);
            staged.add(relativePath.toString());
            try {
                if (!path.equals(stageDirPath)) {
                    filesListWriter.write(relativePath.toString() + ' ' +
                            getFileHash(path.toString()) + '\n');
                    Files.move(path, contentDirPath.resolve(relativePath));
                }
            } catch (IOException e) {
                throw new FileSystemError();
            }
        });

        Map <String, String> oldFiles = new HashMap<>();
        Files.lines(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME, pos.toString(),
                COMMIT_FILES_LIST)).forEach(line -> {
                    int sep = line.indexOf(' ');
                    String filename = line.substring(0, sep);
                    oldFiles.put(filename, line);
        });

        Path parentCommitContent = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                pos.toString(), COMMIT_CONTENT_DIR);
        Files.walk(parentCommitContent).forEach(path -> {
            Path relativePath = parentCommitContent.relativize(path);
            try {
                if (!path.equals(parentCommitContent) &&
                        !staged.contains(relativePath.toString())) {
                    filesListWriter.write(oldFiles.get(relativePath.toString()) + '\n');
                    Files.copy(path, contentDirPath.resolve(relativePath));
                }
            } catch (IOException e) {
                throw new FileSystemError();
            }
        });

        filesListWriter.close();

        Files.write(Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME),
                String.valueOf(number).getBytes());
        Files.write(Paths.get(REPO_DIR_NAME, POSITION_FILENAME),
                (branch + '\n' + number).getBytes());
        Files.write(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME, branch),
                (number.toString() + '\n').getBytes(), StandardOpenOption.APPEND);
        wipeDir(stageDirPath);
    }

    public static void addFile(String path) throws BadRepoException, UnrecognisedFileException {
        if (Files.exists(Paths.get(path))) {
            Path stageDirPath = Paths.get(REPO_DIR_NAME, STAGE_DIR);
            if (Files.notExists(stageDirPath)) {
                throw new BadRepoException();
            }
            try {
                Files.copy(Paths.get(path), stageDirPath.resolve(path),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FileSystemError();
            }
        } else {
            throw new UnrecognisedFileException();
        }
    }

    private static void deleteDir(Path dir) throws IOException {
        wipeDir(dir);
        Files.delete(dir);
    }

    private static void wipeDir(Path dir) throws IOException {
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
    }

    private static int getCommitsNumber() throws BadRepoException, IOException {
        List<String> lines = Files.readAllLines(
                Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME));
        if (lines.size() != 1) {
            throw new BadRepoException();
        }
        int commitsNumber;
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

    private static String getFileHash(String filePath) {
        DigestInputStream stream;
        byte[] hash = null;
        try (FileInputStream fin = new FileInputStream(filePath)) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            stream = new DigestInputStream(fin, messageDigest);
            while (stream.read() != -1) {}
            hash = messageDigest.digest();
        } catch (IOException e) {
            throw new FileSystemError();
        } catch (NoSuchAlgorithmException e) {}
        return new BigInteger(1, hash).toString();
    }

    public static class RepoExistsException extends Exception {};

    public static class BadRepoException extends Exception{};

    public static class FileSystemError extends Error{};

    public static class UnrecognisedFileException extends Exception{};
}
