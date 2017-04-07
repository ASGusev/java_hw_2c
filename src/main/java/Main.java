import java.util.List;
import ru.spbau.gusev.vcs.*;

import javax.annotation.Nonnull;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }
        try {
            switch (args[0]) {
                case "init": {
                    init(args);
                    break;
                }
                case "add": {
                    add(args);
                    break;
                }
                case "commit": {
                    commit(args);
                    break;
                }
                case "branch": {
                    tryProcessBranch(args);
                    break;
                }
                case "log": {
                    showLog();
                    break;
                }
                case "checkout": {
                    checkout(args);
                    break;
                }
                case "merge": {
                    merge(args);
                    break;
                }
                case "login": {
                    login(args);
                    break;
                }
                case "rm": {
                    rm(args);
                    break;
                }
                case "reset": {
                    reset(args);
                    break;
                }
                case "clean": {
                    VCS.clean();
                    break;
                }
                case "status": {
                    status();
                    break;
                }

                default: {
                    showHelp();
                }
            }
        } catch (VCS.FileSystemError e) {
            System.out.println("File system error.");
        } catch (Throwable t) {
            System.out.println("Unknown error.");
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
        "login <new username> - changes current username to the given one\n" +
        "rm <filename> - removes the file with given name from the working directory " +
                "and stage\n" +
        "reset <filename> - restores the file with given name to the state captured " +
                "in the current head commit\n" +
        "clean - removes from the working directory all the files that have not been " +
                "added to the repository\n" +
        "status - to show information about new, changed, deleted and staged files");
    }

    private static void init(@Nonnull String[] args) {
        if (args.length == 1) {
            showHelp();
        } else {
            try {
                VCS.createRepo(args[1]);
            } catch (VCS.RepoAlreadyExistsException e) {
                System.out.println("Repo already exists.");
            }
        }
    }

    private static void add(@Nonnull String[] args) {
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
    }

    private static void commit(@Nonnull String[] args) {
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
    }

    private static void tryProcessBranch(@Nonnull String[] args) {
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
                    }
                    break;
                }
                case "delete": {
                    if (args[2].equals("master")) {
                        System.out.println("Branch master cannot be deleted.");
                    } else {
                        try {
                            VCS.deleteBranch(args[2]);
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
    }

    private static void showLog() {
        try {
            List<VCS.CommitDescription> commits = VCS.getLog();
            String curBranchName = VCS.getCurBranch();
            if (commits.isEmpty()) {
                System.out.printf("No commits in branch %s yet.\n", curBranchName);
            } else {
                System.out.printf("Commits in branch %s:\n", curBranchName);
                System.out.printf("%4s%12s %12s%8s %s\n", "ID", "Author",
                        "Date", "Time", "Message");
                commits.forEach(commit ->
                        System.out.printf("%4d%12s  %3$tF %3$tT %4$s\n",
                                commit.getNumber(), commit.getAuthor(),
                                commit.getTime(), commit.getMessage())
                );
            }
        } catch (VCS.BadRepoException | VCS.NoSuchBranchException e) {
            System.out.println("Incorrect repo");
        }
    }

    private static void checkout(@Nonnull String[] args) {
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
    }

    private static void merge(@Nonnull String[] args) {
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
    }

    private static void login(@Nonnull String[] args) {
        if (args.length < 2) {
            showHelp();
        } else {
            try {
                VCS.setUserName(args[1]);
            } catch (VCS.BadRepoException e) {
                System.out.println("Incorrect repo.");
            }
        }
    }

    private static void rm(@Nonnull String[] args) {
        if (args.length < 2) {
            System.out.println("Specify a file ro remove.");
        } else {
            try {
                VCS.remove(args[1]);
            } catch (VCS.NoSuchFileException e) {
                System.out.println("No file called " + args[1] + " found in working" +
                        " directory or staging zone.");
            } catch (VCS.BadRepoException e) {
                System.out.println("Incorrect repo.");
            }
        }
    }

    private static void reset(@Nonnull String[] args) {
        if (args.length < 2) {
            System.out.println("Specify a file ro reset.");
        } else {
            try {
                VCS.reset(args[1]);
            } catch (VCS.NoSuchFileException e) {
                System.out.println("The current commit does not contain a file called "
                        + args[1]);
            } catch (VCS.BadRepoException e) {
                System.out.println("Incorrect repo.");
            }
        }
    }

    private static void status() {
        try {
            System.out.println("Changed files:");
            VCS.getChanged().forEach(System.out::println);

            System.out.println("\nNew files:");
            VCS.getCreated().forEach(System.out::println);

            System.out.println("\nStaged files:");
            VCS.getStaged().forEach(System.out::println);

            System.out.println("\nRemoved files:");
            VCS.getRemoved().forEach(System.out::println);
        } catch (VCS.BadRepoException e) {
            System.out.println("Incorrect repo");
        }
    }
}
