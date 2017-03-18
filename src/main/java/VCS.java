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
import java.util.stream.Collectors;
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

    public static void createRepo(String author) throws AlreadyExistsException, IOException {
        if (Files.exists(Paths.get(REPO_DIR_NAME), LinkOption.NOFOLLOW_LINKS)) {
            throw new AlreadyExistsException();
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
        //metadataWriter.write(LocalDateTime.now().
        //        format(DateTimeFormatter.ofLocalizedDateTime(
        //                FormatStyle.SHORT, FormatStyle.MEDIUM)));
        metadataWriter.write(String.valueOf(System.currentTimeMillis()));
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
        //metadataWriter.write(LocalDateTime.now().
        //        format(DateTimeFormatter.ofLocalizedDateTime(
        //                FormatStyle.SHORT, FormatStyle.MEDIUM)) + '\n');
        metadataWriter.write(String.valueOf(System.currentTimeMillis()) + '\n');
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

    public static void addFile(String path) throws BadRepoException, NonExistentFileException {
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
            throw new NonExistentFileException();
        }
    }

    public static class Branch {
        public static void createBranch(String branchName) throws BadRepoException,
                AlreadyExistsException {
            Path posFile = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
            if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME)) ||
                    Files.notExists(posFile)) {
                throw new BadRepoException();
            }
            Path branchDescription = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME, branchName);
            if (Files.exists(branchDescription)) {
                throw new AlreadyExistsException();
            }
            try {
                Files.createFile(branchDescription);
                List<String> pos = Files.readAllLines(posFile);
                Files.write(posFile, (branchName + '\n' + pos.get(1)).getBytes());
            } catch (IOException e) {
                throw new FileSystemError();
            }
        }

        public static void deleteBranch(String branchName) throws BadRepoException,
                NonExistentBranchException {
            if (branchName.equals("master")) {
                throw new IllegalArgumentException();
            }
            Path branchDescription = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME,
                    branchName);
            Path posFile = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
            if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME)) ||
                    Files.notExists(posFile)) {
                throw new BadRepoException();
            }
            if (Files.notExists(branchDescription)) {
                throw new NonExistentBranchException();
            }

            try {
                Path commitsDir = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME);
                Files.lines(branchDescription).forEach(commit -> {
                    try {
                        deleteDir(commitsDir.resolve(commit));
                    } catch (IOException e) {
                        throw new FileSystemError();
                    }
                });
                Scanner posScanner = new Scanner(posFile);
                String curBranch = posScanner.next();
                posScanner.close();
                if (curBranch.equals(branchName)) {
                    String newPos = "0";
                    Scanner branchScanner = new Scanner(Paths.get(REPO_DIR_NAME,
                            BRANCHES_DIR_NAME, "master"));
                    while (branchScanner.hasNext()) {
                        newPos = branchScanner.nextLine();
                    }

                    FileWriter posWriter = new FileWriter(posFile.toString());
                    posWriter.write("master" + '\n');
                    posWriter.write(newPos);
                    posWriter.close();
                }

                Files.delete(branchDescription);
            } catch (IOException e) {
                throw new FileSystemError();
            }
        }

        public static List<Commit> getLog(String branchName) throws BadRepoException,
                NonExistentBranchException {
            if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME))) {
                throw new BadRepoException();
            }
            Path branchDescriptionPath = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME,
                    branchName);
            if (Files.notExists(branchDescriptionPath)) {
                throw new NonExistentBranchException();
            }

            Path commitsDir = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME);
            List<Commit> commitList;
            try {
                commitList = Files.lines(branchDescriptionPath).map(number -> {
                    Path commitDescription = commitsDir.resolve(number).
                            resolve(COMMIT_METADATA_FILE);
                    Commit commit;
                    try (Scanner commitScanner = new Scanner(commitDescription)) {
                        long time = commitScanner.nextLong();
                        String branch = commitScanner.next();
                        String author = commitScanner.next();
                        StringBuilder messageBuilder = new StringBuilder();
                        while (commitScanner.hasNext()) {
                            messageBuilder.append(commitScanner.nextLine());
                        }
                        String message = messageBuilder.toString();
                        commit = new Commit(Integer.valueOf(number), branch, author,
                                message, time);
                    } catch (IOException e) {
                        throw new FileSystemError();
                    }
                    return commit;
                }).collect(Collectors.toList());
            } catch (IOException e) {
                throw new FileSystemError();
            }
            return commitList;
        }
    }

    public static String getCurBranch() throws BadRepoException {
        String curBranch;
        Path posFilePath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        if (Files.notExists(posFilePath)) {
            throw new BadRepoException();
        }

        try (Scanner scanner = new Scanner(posFilePath)) {
            curBranch = scanner.next();
        } catch (IOException e) {
            throw new FileSystemError();
        }
        return curBranch;
    }

    public static int getCurCommit() throws BadRepoException {
        int curCommit;
        Path posFilePath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        if (Files.notExists(posFilePath)) {
            throw new BadRepoException();
        }

        try (Scanner scanner = new Scanner(posFilePath)) {
            scanner.next();
            curCommit = scanner.nextInt();
        } catch (IOException e) {
            throw new FileSystemError();
        }
        return curCommit;
    }

    public static class Commit {
        private int number;
        private String branch;
        private String author;
        private String message;
        private Calendar time;

        Commit(int number, String branch, String author, String message, long moment) {
            this.number = number;
            this.branch = branch;
            this.author = author;
            this.message = message;
            Calendar.Builder builder = new Calendar.Builder();
            builder.setInstant(moment);
            time = builder.build();
        }

        public int getNumber() {
            return number;
        }

        public String getBranch() {
            return branch;
        }

        public String getAuthor() {
            return author;
        }

        public String getMessage() {
            return message;
        }

        public Calendar getTime() {
            return time;
        }
    }

    public static void checkoutCommit(int commitID) throws BadRepoException,
            NoSuchCommitException {
        int curCommit = getCurCommit();
        Path curCommitList = Paths.get(REPO_DIR_NAME, COMMIT_METADATA_FILE,
                String.valueOf(curCommit), COMMIT_FILES_LIST);
        try {
            Files.readAllLines(curCommitList).forEach(line -> {
                Path path = Paths.get(line.substring(0, line.indexOf(' ')));
                try {
                    if (Files.isDirectory(path)) {
                        deleteDir(path);
                    } else {
                        Files.delete(path);
                    }
                } catch (IOException e) {
                    throw new FileSystemError();
                }
            });
        } catch (IOException e) {
            throw new FileSystemError();
        }

        Path newContent = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                String.valueOf(commitID), COMMIT_CONTENT_DIR);
        try {
            Files.walk(newContent).forEach(path -> {
                Path relativePath = newContent.relativize(path);
                try {
                    Files.copy(path, relativePath);
                } catch (IOException e) {
                    throw new FileSystemError();
                }
            });
        } catch (IOException e) {
            throw new FileSystemError();
        }

        Path posFilePath = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        try {
            String branch = getCommitBranch(commitID);
            Files.write(posFilePath, (branch + '\n' + String.valueOf(commitID))
                    .getBytes());
        } catch (IOException e) {
            throw new FileSystemError();
        }
    }

    public static void checkoutBranch(String branchName) throws BadRepoException,
            NonExistentBranchException {
        int branchHead = getBranchHead(branchName);
        try {
            checkoutCommit(branchHead);
        } catch (NoSuchCommitException e) {
            throw new BadRepoException();
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

    private static int getBranchHead(String branchName) throws BadRepoException,
            NonExistentBranchException {
        Path branchPath = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME, branchName);
        if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME))) {
            throw new BadRepoException();
        }
        if (Files.notExists(branchPath)) {
            throw new NonExistentBranchException();
        }

        String number = null;
        try (Scanner scanner = new Scanner(branchPath)) {
            while (scanner.hasNext()) {
                number = scanner.next();
            }
        } catch (IOException e) {
            throw new FileSystemError();
        }
        return Integer.valueOf(number);
    }

    private static String getCommitBranch(int commit) throws IOException {
        String branch;
        Path metadataPath = Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME,
                String.valueOf(commit), COMMIT_CONTENT_DIR);
        Scanner scanner = new Scanner(metadataPath);
        scanner.next();
        branch = scanner.next();
        scanner.close();
        return branch;
    }

    public static class AlreadyExistsException extends Exception {}

    public static class BadRepoException extends Exception{}

    public static class FileSystemError extends Error{}

    public static class NonExistentFileException extends Exception{}

    public static class NonExistentBranchException extends Exception{}

    public static class NoSuchCommitException extends Exception{}
}
