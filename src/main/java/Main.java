import java.io.IOException;

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
                                System.out.println("File " + args[i] + " cannot be found.");
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

                default: {
                    showHelp();
                }
            }
        }
    }

    private static void showHelp() {
        System.out.println("help)");
    }
}
