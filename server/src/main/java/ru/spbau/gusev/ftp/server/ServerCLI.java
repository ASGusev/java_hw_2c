package ru.spbau.gusev.ftp.server;

import java.io.IOException;
import java.util.Scanner;

public class ServerCLI {
    public static void main(String[] args) {
        if (args.length == 1) {
            int port = Integer.valueOf(args[0]);
            FTPServer server = new FTPServer(port);
            try {
                server.start();
            } catch (IOException e) {
                System.out.println("Error during server start " + e.getMessage());
            }
            System.out.println("Server is running. Press enter to stop.");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            try {
                server.stop();
            } catch (IOException e) {
                System.out.println("Error during server stop " + e.getMessage());
            }
        } else {
            showHelp();
        }
    }

    private static void showHelp() {
        System.out.println("You should specify port to start the server.");
    }
}
