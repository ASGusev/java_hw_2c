import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            showHelp();
        } else {
            switch (args[0]) {
                case "init": {
                    if (args.length == 1) {
                        System.out.println("Username required to initialise repo.");
                    } else {
                        try {
                            VCS.createRepo(args[1]);
                        } catch (VCS.AlreadyExistsException e) {
                            System.out.println("Repo already exists.");
                        } catch (IOException e) {
                            System.out.println("File system error.");
                        }
                    }
                    break;
                }
                case "add": {
                    if (args.length == 1) {
                        System.out.println("Specify files to add.");
                    } else {
                        for (int i = 1; i < args.length; i++) {
                            try {
                                VCS.addFile(args[i]);
                            } catch (VCS.BadRepoException e) {
                                System.out.println("Incorrect repo.");
                            } catch (VCS.NonExistentFileException e) {
                                System.out.println("File " + args[i] + " " +
                                        "cannot be found.");
                            }
                        }
                    }
                    break;
                }
                case "commit": {
                    if (args.length == 1) {
                        System.out.println("Commit message required.");
                    } else {
                        try {
                            VCS.commit(args[1]);
                        } catch (IOException e) {
                            System.out.println("Filesystem error");
                            e.printStackTrace();
                        } catch (VCS.BadRepoException e) {
                            System.out.println("Incorrect repo");
                        }
                    }
                    break;
                }
                case "branch": {
                    if (args.length == 1) {
                        System.out.println("Branches:");
                        //TODO: list branches
                    } else if (args.length == 3) {
                        switch (args[1]) {
                            case "create": {
                                try {
                                    VCS.Branch.createBranch(args[2]);
                                } catch (VCS.BadRepoException e) {
                                    System.out.println("Incorrect repo");
                                } catch (VCS.AlreadyExistsException e) {
                                    System.out.println("A branch with this name " +
                                            "already exists.");
                                } catch (VCS.FileSystemError e) {
                                    System.out.println("Filesystem error");
                                }
                                break;
                            }
                            case "delete": {
                                if (args[2].equals("master")) {
                                    System.out.println("Branch master cannot be deleted.");
                                } else {
                                    try {
                                        VCS.Branch.deleteBranch(args[2]);
                                    } catch (VCS.FileSystemError e) {
                                        System.out.println("Filesystem error");
                                    } catch (VCS.BadRepoException e) {
                                        System.out.println("Incorrect repo");
                                    } catch (VCS.NonExistentBranchException e) {
                                        System.out.println("A branch called " +
                                                args[2] + " does not exist.");
                                    }
                                }
                                break;
                            }
                            default: {
                                showHelp();
                            }
                        }
                    } else {
                        showHelp();
                    }
                    break;
                }
                case "log": {
                    try {
                        List<VCS.Commit> commits = VCS.Branch.getLog(VCS.getCurBranch());
                        System.out.printf("%4s%12s %12s%8s %s\n", "ID", "Author",
                                "Date", "Time", "Message");
                        commits.forEach(commit ->
                            System.out.printf("%4d%12s  %3$tF %3$tT %4$s\n",
                                    commit.getNumber(), commit.getAuthor(),
                                    commit.getTime(), commit.getMessage())
                        );
                    } catch (VCS.FileSystemError e) {
                        System.out.println("Filesystem error");
                    } catch (VCS.BadRepoException | VCS.NonExistentBranchException e) {
                        System.out.println("Incorrect repo");
                    }
                    break;
                }
                case "checkout": {
                    if (args.length == 3) {
                        switch (args[1]) {
                            case "branch": {
                                try {
                                    VCS.checkoutBranch(args[2]);
                                } catch (VCS.BadRepoException e) {
                                    System.out.println("Incorrect repo");
                                } catch (VCS.NonExistentBranchException e) {
                                    System.out.println("A branch called " +
                                            args[2] + " does not exist.");
                                }
                                break;
                            }
                            case "commit": {
                                try {
                                    VCS.checkoutCommit(Integer.valueOf(args[2]));
                                } catch (VCS.BadRepoException e) {
                                    System.out.println("Incorrect repo");
                                } catch (VCS.NoSuchCommitException e) {
                                    System.out.println("A commit with number " +
                                            args[2] + " does not exist.");
                                }
                                break;
                            }
                            default: {
                                showHelp();
                            }
                        }
                    } else {
                        showHelp();
                    }
                    break;
                }

                default: {
                    showHelp();
                }
            }
        }
    }

    private static void showHelp() {
        //TODO: help
        System.out.println("help)");
    }
}
