import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            showHelp();
        } else {
            switch (args[0]) {
                case "init": {
                    if (args.length == 1) {
                        showHelp();
                    } else {
                        try {
                            VCS.createRepo(args[1]);
                        } catch (VCS.RepoAlreadyExistsException e) {
                            System.out.println("Repo already exists.");
                        } catch (VCS.FileSystemError e) {
                            System.out.println("File system error.");
                        }
                    }
                    break;
                }
                case "add": {
                    if (args.length == 1) {
                        showHelp();
                    } else {
                        for (int i = 1; i < args.length; i++) {
                            try {
                                VCS.addFile(args[i]);
                            } catch (VCS.BadRepoException e) {
                                System.out.println("Incorrect repo.");
                            } catch (VCS.NoSuchFileException e) {
                                System.out.println("File " + args[i] + " " +
                                        "cannot be found.");
                            }
                        }
                    }
                    break;
                }
                case "commit": {
                    if (args.length == 1) {
                        System.out.println("CommitDescription message required.");
                    } else {
                        try {
                            VCS.commit(args[1]);
                        } catch (VCS.BadRepoException e) {
                            System.out.println("Incorrect repo");
                        } catch (VCS.BadPositionException e) {
                            System.out.println("You must be in the head of a " +
                                    "branch to commit.");
                        }
                    }
                    break;
                }
                case "branch": {
                    if (args.length == 3) {
                        switch (args[1]) {
                            case "create": {
                                try {
                                    VCS.createBranch(args[2]);
                                } catch (VCS.BadRepoException e) {
                                    System.out.println("Incorrect repo");
                                } catch (VCS.BranchAlreadyExistsException e) {
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
                                        VCS.deleteBranch(args[2]);
                                    } catch (VCS.FileSystemError e) {
                                        System.out.println("Filesystem error");
                                    } catch (VCS.BadRepoException e) {
                                        System.out.println("Incorrect repo");
                                    } catch (VCS.NoSuchBranchException e) {
                                        System.out.println("A branch called " +
                                                args[2] + " does not exist.");
                                    } catch (VCS.BadPositionException e) {
                                        System.out.println("Current branch cannot be" +
                                                " deleted.");
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
                        List<VCS.CommitDescription> commits = VCS.getLog();
                        System.out.printf("%4s%12s %12s%8s %s\n", "ID", "Author",
                                "Date", "Time", "Message");
                        commits.forEach(commit ->
                            System.out.printf("%4d%12s  %3$tF %3$tT %4$s\n",
                                    commit.getNumber(), commit.getAuthor(),
                                    commit.getTime(), commit.getMessage())
                        );
                    } catch (VCS.FileSystemError e) {
                        System.out.println("Filesystem error");
                    } catch (VCS.BadRepoException | VCS.NoSuchBranchException e) {
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
                                } catch (VCS.NoSuchBranchException e) {
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

                case "merge": {
                    if (args.length < 2) {
                        System.out.println("Specify a branch too merge.");
                    } else {
                        try {
                            VCS.merge(args[1]);
                        } catch (VCS.NoSuchBranchException e) {
                            System.out.println("Branch " + args[1] + " does not exist.");
                        } catch (VCS.BadRepoException e) {
                            System.out.println("Incorrect repo.");
                        } catch (VCS.BadPositionException e) {
                            System.out.println("You must be in the head of a branch " +
                                    "to merge another one into it.");
                        }
                    }
                    break;
                }
                case "login": {
                    if (args.length < 2) {
                        showHelp();
                    } else {
                        try {
                            VCS.setUserName(args[1]);
                        } catch (VCS.BadRepoException e) {
                            System.out.println("Incorrect repo.");
                        }
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
        System.out.println("Commands include:\n" +
        "init <username> - initialises repo with the given username\n" +
        "add <file> - adds the file to the next commit\n" +
        "commit <message> - commits changes\n" +
        "branch create <name> - creates a branch with provided name\n" +
        "branch delete <name> - deletes specified branch\n" +
        "log - shows commit history in current branch\n" +
        "checkout commit <number> - returns the repo to the state of the " +
                "specified commit\n" +
        "checkout branch <name> - checks out the head of the given branch\n" +
        "merge <branch name> - merges the given branch into current\n" +
        "login <new username> - changes current username to the given one");
    }
}
