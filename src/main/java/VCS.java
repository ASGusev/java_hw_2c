import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private static final String STAGE_DIR = "stage";
    private static final String COMMIT_CONTENT_DIR = "content";
    private static final String COMMIT_METADATA_FILE = "metadata";
    private static final String COMMIT_FILES_LIST = "files_list";
    private static final String DEFAULT_BRANCH = "master";

    /**
     * Initialises a repository in the current directory. A folder with all the
     * necessary information is created.
     * @param author the first username for the created repo
     * @throws RepoAlreadyExistsException if a repo is already initialised in current folder
     */
    public static void createRepo(String author) throws RepoAlreadyExistsException {
        if (Files.exists(Paths.get(REPO_DIR_NAME), LinkOption.NOFOLLOW_LINKS)) {
            throw new RepoAlreadyExistsException();
        }
        try {
            Files.createDirectory(Paths.get(REPO_DIR_NAME));

            //Initialising master branch
            Files.createDirectory(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME));
            FileWriter branchWriter = new FileWriter(Paths.get(REPO_DIR_NAME,
                    BRANCHES_DIR_NAME, DEFAULT_BRANCH).toString());
            branchWriter.write("0\n");
            branchWriter.close();

            //Preparing initial commit
            Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME));
            Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                    "0"));
            BufferedWriter metadataWriter = new BufferedWriter(new FileWriter(
                    Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                            "0", COMMIT_METADATA_FILE).toString()));
            metadataWriter.write(String.valueOf(System.currentTimeMillis()) + '\n');
            metadataWriter.write(author + '\n');
            metadataWriter.write("0\n");
            metadataWriter.write("Initial commit\n");
            metadataWriter.close();
            Files.createFile(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME, "0",
                    COMMIT_FILES_LIST));
            Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME, "0",
                    COMMIT_CONTENT_DIR));

            //Setting username
            FileWriter authorWriter = new FileWriter(
                    Paths.get(REPO_DIR_NAME, USERNAME_FILE).toString());
            authorWriter.write(author);
            authorWriter.close();

            //Setting up commit counter
            FileWriter commitsWriter = new FileWriter(
                    Paths.get(REPO_DIR_NAME, COMMITS_COUNTER_FILENAME).toString());
            commitsWriter.write("0");
            commitsWriter.close();

            //Setting up position tracking
            FileWriter positionWriter = new FileWriter(
                    Paths.get(REPO_DIR_NAME, POSITION_FILENAME).toString());
            positionWriter.write(DEFAULT_BRANCH + "\n0");
            positionWriter.close();

            //Creating stage directory
            Files.createDirectory(Paths.get(REPO_DIR_NAME, STAGE_DIR));
        } catch (IOException e) {
            throw new FileSystemError();
        }
    }

    /**
     * Sets the current username in the repository in the given value
     * @param name new username
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     */
    public static void setUser(String name) throws BadRepoException {
        if (!Files.exists(Paths.get(REPO_DIR_NAME, USERNAME_FILE))) {
            throw new BadRepoException();
        }
        try (FileWriter writer = new FileWriter(
                Paths.get(REPO_DIR_NAME, USERNAME_FILE).toString())) {
            writer.write(name);
        } catch (IOException e){
            throw new FileSystemError();
        }
    }

    /**
     * Creates a new commit with a supplied message including all the changes
     * that have been added to the stage.
     * @param message the message that will be added to commit
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws BadPositionException if the current commit is not the head of the
     * current branch.
     */
    public static void commit(String message) throws BadRepoException,
            BadPositionException {
        if (Files.notExists(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME))) {
            throw new BadRepoException();
        }

        try {
            Integer number = getCommitsNumber() + 1;
            Integer pos = getCurCommit();
            String branch = getCurBranch();
            Integer branchHead;
            try {
                branchHead = getBranchHead(branch);
            } catch (NoSuchBranchException e) {
                throw new BadRepoException();
            }
            if (!branchHead.equals(pos)) {
                throw new BadPositionException();
            }

            Files.createDirectory(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                    number.toString()));

            BufferedWriter metadataWriter = new BufferedWriter(new FileWriter(
                    Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                            number.toString(), COMMIT_METADATA_FILE).toString()));
            metadataWriter.write(String.valueOf(System.currentTimeMillis()) + '\n');
            metadataWriter.write(branch + '\n');
            metadataWriter.write(getUserName() + '\n');
            metadataWriter.write(branchHead.toString() + '\n');
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

            Map<String, String> oldFiles = new HashMap<>();
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
        } catch (IOException e) {
            throw new FileSystemError();
        }
    }

    /**
     * Adds a file to the stage zone. Files form the stage zone are included in the
     * next commit.
     * @param path the path to the file to be added.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchFileException if the given path does not lead to a file.
     */
    public static void addFile(String path) throws BadRepoException, NoSuchFileException {
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
            throw new NoSuchFileException();
        }
    }

    /**
     * Creates a new branch with the given name.
     * @param branchName the name for the new branch.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws BranchAlreadyExistsException if a branch with this name already exists.
     */
    public static void createBranch(String branchName) throws BadRepoException,
            BranchAlreadyExistsException {
        Path posFile = Paths.get(REPO_DIR_NAME, POSITION_FILENAME);
        if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME)) ||
                Files.notExists(posFile)) {
            throw new BadRepoException();
        }
        Path branchDescription = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME, branchName);
        if (Files.exists(branchDescription)) {
            throw new BranchAlreadyExistsException();
        }
        try {
            Files.createFile(branchDescription);
            List<String> pos = Files.readAllLines(posFile);
            Files.write(posFile, (branchName + '\n' + pos.get(1)).getBytes());
        } catch (IOException e) {
            throw new FileSystemError();
        }
    }

    /**
     * Deletes the branch with the given name.
     * @param branchName - the name of the branch to be deleted.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    public static void deleteBranch(String branchName) throws BadRepoException,
            NoSuchBranchException {
        if (branchName.equals(DEFAULT_BRANCH)) {
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
            throw new NoSuchBranchException();
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
                        BRANCHES_DIR_NAME, DEFAULT_BRANCH));
                while (branchScanner.hasNext()) {
                    newPos = branchScanner.nextLine();
                }

                FileWriter posWriter = new FileWriter(posFile.toString());
                posWriter.write(DEFAULT_BRANCH + '\n');
                posWriter.write(newPos);
                posWriter.close();
            }

            Files.delete(branchDescription);
        } catch (IOException e) {
            throw new FileSystemError();
        }
    }

    /**
     * Prepares a list with information about all the commits in a specified branch.
     * @param branchName the name of a branch to be listed.
     * @return a list of Commit objects representing commits of the specified branch.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    public static List<Commit> getLog(String branchName) throws BadRepoException,
            NoSuchBranchException {
        if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME))) {
            throw new BadRepoException();
        }
        Path branchDescriptionPath = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME,
                branchName);
        if (Files.notExists(branchDescriptionPath)) {
            throw new NoSuchBranchException();
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
                    commitScanner.next();
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

    /**
     * Gets the name of the currently selected branch.
     * @return the name of the current branch.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     */
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

    /**
     * Gets the number of the currently selected commit.
     * @return the number of the current commit
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     */
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

    /**
     * A class representing a commit object.
     */
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

        /**
         * Get the commit number.
         * @return commit number.
         */
        public int getNumber() {
            return number;
        }

        /**
         * Get the commit's branch
         * @return the branch that the commit belongs to.
         */
        public String getBranch() {
            return branch;
        }

        /**
         * Gets author of the commit.
         * @return the author of this commit.
         */
        public String getAuthor() {
            return author;
        }

        /**
         * Gets the message of the commit.
         * @return the commit message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Gets time of the commit creation.
         * @return a Calendar object representing the commit creation time.
         */
        public Calendar getTime() {
            return time;
        }
    }

    /**
     * Returns the directory to the condition in which it was at the moment of the
     * specified commit.
     * @param commitID the number of the required commit.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchCommitException if a commit with the given number does
     * not exist.
     */
    public static void checkoutCommit(int commitID) throws BadRepoException,
            NoSuchCommitException {
        int curCommit = getCurCommit();
        Path curCommitList = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                String.valueOf(curCommit), COMMIT_FILES_LIST);
        clearCommitted(curCommitList);

        Path newContent = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                String.valueOf(commitID), COMMIT_CONTENT_DIR);
        try {
            Files.walk(newContent).forEach(path -> {
                if (!path.equals(newContent)) {
                    Path relativePath = newContent.relativize(path);
                    try {
                        Files.copy(path, relativePath);
                    } catch (IOException e) {
                        throw new FileSystemError();
                    }
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
            e.printStackTrace();
            throw new FileSystemError();
        }
    }

    /**
     * Removes all the files included in a commit from the working directory.
     * @param commitList the number of the commit to be cleared.
     */
    private static void clearCommitted(Path commitList) {
        try {
            Files.readAllLines(commitList).forEach(line -> {
                Path path = Paths.get(line.substring(0, line.indexOf(' ')));
                try {
                    if (Files.exists(path)) {
                        if (Files.isDirectory(path)) {
                            deleteDir(path);
                        } else {
                            Files.delete(path);
                        }
                    }
                } catch (IOException e) {
                    throw new FileSystemError();
                }
            });
        } catch (IOException e) {
            throw new FileSystemError();
        }
    }

    /**
     * Returns the working directory to the condition in which in was at the moment
     * of the last commit of a branch.
     * @param branchName the name of the required branch
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with given name does not exist.
     */
    public static void checkoutBranch(String branchName) throws BadRepoException,
            NoSuchBranchException {
        int branchHead = getBranchHead(branchName);
        try {
            checkoutCommit(branchHead);
        } catch (NoSuchCommitException e) {
            throw new BadRepoException();
        }
    }

    /**
     * Merges the specified branch into current. The changes if the merged branch
     * have higher priority than in the current.
     * @param branchName the name of the branch to merge
     * @throws BadPositionException if the current position is not a branch head.
     * @throws BadRepoException if the repository information folder is
     * not in a correct condition.
     * @throws NoSuchBranchException if a branch with the given name does not exist.
     */
    public static void merge(String branchName) throws BadPositionException,
            BadRepoException, NoSuchBranchException {
        Integer curCommit = getCurCommit();
        Integer mergedCommit = getBranchHead(branchName);
        if (curCommit != getBranchHead(getCurBranch())) {
            throw new BadPositionException();
        }
        if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME, branchName))) {
            throw new NoSuchBranchException();
        }

        List<Integer> curCommitOrigin = getCommitOrigin(curCommit);
        List<Integer> mergedCommitOrigin = getCommitOrigin(mergedCommit);

        int i = 0;
        while (i < curCommitOrigin.size() && i < mergedCommitOrigin.size() &&
                curCommitOrigin.get(i).equals(mergedCommitOrigin.get(i))) {
            i++;
        }
        Integer commonPredecessor = curCommitOrigin.get(i - 1);

        Map<String, FileDescription> sourceFiles = getCommitFilesInfo(commonPredecessor);
        Map<String, FileDescription> mergedFiles = getCommitFilesInfo(mergedCommit);
        Map<String, FileDescription> curFiles = getCommitFilesInfo(curCommit);
        Map<String, FileDescription> resFiles = new HashMap<>();

        resFiles.putAll(curFiles);
        resFiles.putAll(mergedFiles);

        sourceFiles.forEach((name, desc) -> {
            if (!curFiles.containsKey(name) || !mergedFiles.containsKey(name)) {
                resFiles.remove(name);
            }
        });

        clearCommitted(Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                curCommit.toString(), COMMIT_FILES_LIST));
        resFiles.forEach((name, desc) -> {
            Path newPath = Paths.get(REPO_DIR_NAME, name);
            try {
                Files.copy(desc.getPath(), newPath);
                addFile(name);
            } catch (BadRepoException | NoSuchFileException | IOException e) {
                throw new FileSystemError();
            }
        });
        commit("Branch " + branchName + " merged");
    }

    private static class FileDescription {
        private String hash;
        private Path path;

        FileDescription(String hash, Path path) {
            this.hash = hash;
            this.path = path;
        }

        public String getHash() {
            return hash;
        }

        public Path getPath() {
            return path;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FileDescription &&
                    hash.equals(((FileDescription)obj).hash);
        }
    }

    /**
     * Gets a pedigree of the specified commit from the repository initialisation.
     */
    private static List<Integer> getCommitOrigin(Integer commitID) throws
            BadRepoException {
        ArrayList<Integer> origin = new ArrayList<>();
        Integer pos = commitID;
        while (!pos.equals(0)) {
            origin.add(pos);
            pos = getCommitParent(pos);
        }
        origin.add(0);
        Collections.reverse(origin);
        return origin;
    }

    /**
     * Gets the position of the head before the specified commit.
     */
    private static Integer getCommitParent(Integer commitID) throws
            BadRepoException {
        Path commitDescription = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                commitID.toString(), COMMIT_METADATA_FILE);
        if (Files.notExists(commitDescription)) {
            throw new BadRepoException();
        }

        Integer parent;
        try (Scanner descriptionScanner = new Scanner(commitDescription)) {
            descriptionScanner.next();
            descriptionScanner.next();
            parent = descriptionScanner.nextInt();
        } catch (IOException e) {
            throw new FileSystemError();
        }
        return parent;
    }

    /**
     * Gets a map from file name to its properties.
     */
    private static Map<String, FileDescription> getCommitFilesInfo(Integer commitID)
            throws BadRepoException {
        Path filesListPath = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                commitID.toString(), COMMIT_FILES_LIST);
        Path contentDir = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                commitID.toString(), COMMIT_CONTENT_DIR);

        if (Files.notExists(filesListPath) || Files.notExists(contentDir)) {
            throw new BadRepoException();
        }
        Map<String, FileDescription> files = new HashMap<>();
        try {
            Files.lines(filesListPath).forEach(line -> {
                String[] info = line.split(" ");
                files.put(info[0], new FileDescription(info[1],
                        contentDir.resolve(info[0])));
            });
        } catch (IOException e) {
            throw new FileSystemError();
        }
        return files;
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

    public static String getUserName() throws IOException {
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
            NoSuchBranchException {
        Path branchPath = Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME, branchName);
        if (Files.notExists(Paths.get(REPO_DIR_NAME, BRANCHES_DIR_NAME))) {
            throw new BadRepoException();
        }
        if (Files.notExists(branchPath)) {
            throw new NoSuchBranchException();
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
        Path metadataPath = Paths.get(REPO_DIR_NAME, COMMITS_DIR_NAME,
                String.valueOf(commit), COMMIT_METADATA_FILE);
        Scanner scanner = new Scanner(metadataPath);
        scanner.next();
        branch = scanner.next();
        scanner.close();
        return branch;
    }

    /**
     * An exception thrown in case of an attempt to create a repository where
     * it already exists.
     */
    public static class RepoAlreadyExistsException extends Exception{}

    /**
     * An exception thrown if a branch required to create already exists.
     */
    public static class BranchAlreadyExistsException extends Exception{}

    /**
     * An exception thrown if the repository service folder is damaged.
     */
    public static class BadRepoException extends Exception{}

    /**
     * An error thrown if the filesystem throws an IOException.
     */
    public static class FileSystemError extends Error{}

    /**
     * An exception thrown if the specified file does not exist.
     */
    public static class NoSuchFileException extends Exception{}

    /**
     * An exception thrown if the specified branch does not exist.
     */
    public static class NoSuchBranchException extends Exception{}

    /**
     * An exception thrown if the specified commit does not exist.
     */
    public static class NoSuchCommitException extends Exception{}

    /**
     * An exception thrown if an operation is impossible because of the head
     * position.
     */
    public static class BadPositionException extends Exception{}
}
