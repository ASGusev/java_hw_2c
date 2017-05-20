import java.util.List;
import ru.spbau.gusev.vcs.*;

import javax.annotation.Nonnull;

public class Main {
    public static void main(String[] args) {
        Command requestedCommand;
        if (args.length == 0) {
            requestedCommand = Command.help;
        } else {
            try {
                requestedCommand = Command.valueOf(args[0]);
            } catch (EnumConstantNotPresentException | IllegalArgumentException e) {
                requestedCommand = Command.help;
            }
        }

        try {
            requestedCommand.exec(args);
        } catch (VCS.FileSystemError e) {
            System.out.println("File system error:" + e.getMessage());
        } catch (Throwable t) {
            System.out.println("Unknown error:" + t.getMessage());
        }
    }

    private enum Command{
        init {
            @Override
            protected void exec(String[] args) {
                if (args.length == 1) {
                    help.exec(args);
                } else {
                    try {
                        VCS.createRepo(args[1]);
                    } catch (VCS.RepoAlreadyExistsException e) {
                        System.out.println("Repo already exists.");
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "init <username> - initialises repo with the given username";
            }
        },

        commit {
            @Override
            protected void exec(String[] args) {
                if (args.length == 1) {
                    System.out.println("Commit message required.");
                } else {
                    try {
                        VCS.commit(args[1]);
                    } catch (VCS.BadRepoException e) {
                        System.out.println("Incorrect repo: " + e.getMessage());
                    } catch (VCS.BadPositionException e) {
                        System.out.println("You must be in the head of a branch to commit.");
                    } catch (VCS.NothingToCommitException e) {
                        System.out.println("Nothing to commit.");
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "commit <message> - commits changes";
            }
        },

        add {
            @Override
            protected void exec(String[] args) {
                if (args.length == 1) {
                    help.exec(args);
                } else {
                    for (int i = 1; i < args.length; i++) {
                        try {
                            VCS.addFile(args[i]);
                        } catch (VCS.BadRepoException e) {
                            System.out.println("Incorrect repo: " + e.getMessage());
                        } catch (VCS.NoSuchFileException e) {
                            System.out.println("File " + args[i] + " " +
                                    "cannot be found.");
                        }
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "add <file> - adds the file to the next commit";
            }
        },

        checkout {
            @Override
            protected void exec(String[] args) {
                if (args.length == 3) {
                    try {
                        switch (args[1]) {
                            case "branch": {
                                VCS.checkoutBranch(args[2]);
                                break;
                            }
                            case "commit": {
                                VCS.checkoutCommit(Integer.valueOf(args[2]));
                                break;
                            }
                            default: {
                                help.exec(args);
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Commit ID must be a number.");
                    } catch (VCS.BadRepoException e) {
                        System.out.println("Incorrect repo: " +
                                e.getMessage());
                    } catch (VCS.NoSuchBranchException e) {
                        System.out.println("A branch called " +
                                args[2] + " does not exist.");
                    } catch (VCS.NoSuchCommitException e) {
                        System.out.println("A commit with number " +
                                args[2] + " does not exist.");
                    }
                } else {
                    help.exec(args);
                }
            }

            @Override
            protected String getDesc() {
                return "checkout commit <number> - returns the repo to the state of " +
                        "the specified commit\n" +
                        "checkout branch <name> - checks out the head of the given " +
                        "branch";
            }
        },

        login {
            @Override
            protected void exec(@Nonnull String[] args) {
                if (args.length < 2) {
                    help.exec(args);
                } else {
                    try {
                        VCS.setUserName(args[1]);
                    } catch (VCS.BadRepoException e) {
                        System.out.println("Incorrect repo: " + e.getMessage());
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "login <new username> - changes current username to the given one";
            }
        },

        merge {
            @Override
            protected void exec(@Nonnull String[] args) {
                if (args.length < 2) {
                    System.out.println("Specify a branch to merge.");
                } else {
                    try {
                        VCS.merge(args[1]);
                    } catch (VCS.NoSuchBranchException e) {
                        System.out.println("Branch " + args[1] + " does not exist.");
                    } catch (VCS.BadRepoException e) {
                        System.out.println("Incorrect repo:" + e.getMessage());
                    } catch (VCS.BadPositionException e) {
                        System.out.println("You must be in the head of a branch " +
                                "to merge another one into it.");
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "merge <branch name> - merges the given branch into current";
            }
        },

        reset {
            @Override
            protected void exec(String[] args) {
                if (args.length < 2) {
                    System.out.println("Specify a file ro reset.");
                } else {
                    try {
                        VCS.reset(args[1]);
                    } catch (VCS.NoSuchFileException e) {
                        System.out.println("The current commit does not contain a file called "
                                + args[1]);
                    } catch (VCS.BadRepoException e) {
                        System.out.println("Incorrect repo: " + e.getMessage());
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "reset <filename> - resets the file to its state in the " +
                        "last commit(if the file is not present in the last commit" +
                        ", it is removed)";
            }
        },

        rm {
            @Override
            protected void exec(String[] args) {
                if (args.length < 2) {
                    System.out.println("Specify a file ro remove.");
                } else {
                    try {
                        VCS.remove(args[1]);
                    } catch (VCS.NoSuchFileException e) {
                        System.out.println("No file called " + args[1] + " found " +
                                "in staging zone.");
                    } catch (VCS.BadRepoException e) {
                        System.out.println("Incorrect repo: " + e.getMessage());
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "rm <filename> - removes the file with given name from the " +
                        "working directory and stage";
            }
        },

        help {
            @Override
            protected void exec(String[] args) {
                System.out.println("Commands include:");
                for (Command command: Command.values()) {
                    System.out.println(command.getDesc());
                }
            }

            @Override
            protected String getDesc() {
                return "help - show commands descriptions";
            }
        },

        log {
            @Override
            protected void exec(String[] args) {
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
                    System.out.println("Incorrect repo: " + e.getMessage());
                }

            }


            @Override
            protected String getDesc() {
                return "log - shows commit history in current branch";
            }
        },

        status {
            @Override
            protected void exec(String[] args) {
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
                    System.out.println("Incorrect repo: " + e.getMessage());
                }
            }

            @Override
            protected String getDesc() {
                return "status - to show information about new, changed, deleted and" +
                        " staged files";
            }
        },

        branch {
            @Override
            protected void exec(String[] args) {
                if (args.length > 1) {
                    switch (args[1]) {
                        case "create": {
                            try {
                                if (args.length > 2) {
                                    VCS.createBranch(args[2]);
                                }
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
                                    if (args.length > 2) {
                                        VCS.deleteBranch(args[2]);
                                    }
                                } catch (VCS.BadRepoException e) {
                                    System.out.println("Incorrect repo: " +
                                            e.getMessage());
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
                        case "list": {
                            try {
                                String currentBranch = VCS.getCurBranch();
                                VCS.getBranchNames().forEach(branchName -> {
                                    if (branchName.equals(currentBranch)) {
                                        System.out.println("* " + branchName);
                                    } else {
                                        System.out.println("  " + branchName);
                                    }
                                });
                            } catch (VCS.BadRepoException e) {
                                System.out.println("Incorrect repo: " +
                                        e.getMessage());
                            }
                            break;
                        }
                        default: {
                            help.exec(args);
                        }
                    }
                }
            }

            @Override
            protected String getDesc() {
                return "branch create <name> - creates a branch with provided name\n" +
                        "branch delete <name> - deletes specified branch";
            }
        },

        clean {
            @Override
            protected void exec(String[] args) {
                try {
                    VCS.clean();
                } catch (VCS.BadRepoException e) {
                    System.out.println("Incorrect repo: " + e.getMessage());
                }
            }

            @Override
            protected String getDesc() {
                return "clean - removes all untracked files";
            }
        };

        protected abstract void exec(String[] args);

        protected abstract String getDesc();
    }
}
