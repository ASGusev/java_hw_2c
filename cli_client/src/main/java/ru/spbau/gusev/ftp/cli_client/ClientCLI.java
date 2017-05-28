package ru.spbau.gusev.ftp.cli_client;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import ru.spbau.gusev.ftp.client.FTPClient;

public class ClientCLI {
    private final static String DIR_TYPE = "dir";
    private final static String FILE_TYPE = "file";

    public static void main(String[] args) {
        if (args.length != 4) {
            showHelp();
        } else {
            switch (args[0]) {
                case "list": {
                    FTPClient client = new FTPClient();
                    String address = args[1];
                    int port = Integer.valueOf(args[2]);
                    String path = args[3];
                    try {
                        client.connect(address, port);
                        List<FTPClient.DirEntry> dirContent = client.executeList(path);
                        System.out.printf("%-40s %-5s\n", "Name", "Type");
                        dirContent.forEach(entry -> {
                            String type;
                            if (entry.isDir()) {
                                type = DIR_TYPE;
                            } else {
                                type = FILE_TYPE;
                            }
                            System.out.printf("%-40s %-5s\n", entry.getPath(), type);
                        });
                    } catch (NoSuchFileException e) {
                        System.out.println("No such folder: " + path);
                    }
                    catch (IOException e) {
                        System.out.println("Network error.");
                    }
                    break;
                }
                case "get": {
                    FTPClient client = new FTPClient();
                    String address = args[1];
                    int port = Integer.valueOf(args[2]);
                    String path = args[3];

                    try {
                        client.connect(address, port);
                        client.executeGet(path);
                        client.disconnect();
                    } catch (NoSuchFileException e) {
                        System.out.println("No such file: " + path);
                    } catch (IOException e) {
                        System.out.println("Downloading error: " + e.getMessage());
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
        System.out.println("Commands:\n" +
                "get <server IP> <port> <filePath> - downloads the required file\n" +
                "list <server IP> <port> <dirPath> - lists the required directory on" +
                "server");
    }
}
