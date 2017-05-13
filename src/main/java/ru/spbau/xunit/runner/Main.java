package ru.spbau.xunit.runner;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String URL_PREFIX = "file:";
    private static final String CP_COMMAND = "-cp";
    private static final String TESTS_FOUND_IN_CLASS = "%d tests found in class %s\n";
    private static final String TEST_PASSED = "Test %s passed.\n";
    private static final String TIME = "Time: %d ms.\n";
    private static final String FAILURE_CAUSE = "Cause: %s.\n";
    private static final String TEST_FAILED = "Test %s failed.\n";
    private static final String TEST_IGNORED = "Test %s ignored.\n";
    private static final String IGNORE_REASON = "Reason: %s.\n";
    private static final String CLASS_NOT_FOUND = "Class %s not found.\n";
    private static final String BAD_CLASS = "Bad class: %s.\n";
    private static final String CLASS_NOT_ACCESSIBLE = "Class %s is not accessible.\n";
    private static final String EXPECTED_NOT_THROWN = "expected exception was not thrown";
    private static final String ERROR_IN_METHOD = "%s in method %s: %s.\n";
    private static final String BAD_WORKING_DIRECTORY = "Bad working directory.";
    private static final String BAD_CP = "Bad classpath: %s\n";
    private static final String HELP_COMMAND = "-h";

    private static URLClassLoader classLoader;

    public static void main(String[] args) {
        List<String> classNames = new ArrayList<>();
        List<URL> classPaths = new ArrayList<>();

        String defaultCP = Paths.get(".").toAbsolutePath().toString();
        defaultCP = defaultCP.substring(0, defaultCP.length() - 1);
        try {
            classPaths.add(new URL(URL_PREFIX + defaultCP));
        } catch (MalformedURLException e) {
            System.out.println(BAD_WORKING_DIRECTORY);
        }

        if (args.length == 0 || args[0].equals(HELP_COMMAND)) {
            showHelp();
        }
        boolean isCP = false;
        for (String arg: args) {
            if (isCP) {
                String cp = Paths.get(arg).toAbsolutePath().toString() + "/";
                try {
                    classPaths.add(new URL(URL_PREFIX + cp));
                } catch (MalformedURLException e) {
                    System.out.printf(BAD_CP, cp);
                }
                isCP = false;
            } else {
                if (arg.equals(CP_COMMAND)) {
                    isCP = true;
                } else {
                    classNames.add(arg);
                }
            }
        }

        URL[] cpURLs = new URL[classPaths.size()];
        classPaths.toArray(cpURLs);
        classLoader = new URLClassLoader(cpURLs);

        classNames.forEach(Main::testClass);
    }

    private static void testClass(@Nonnull String className) {
        try {
            Class classToTest = classLoader.loadClass(className);
            List<Runner.TestScore> testScores = Runner.run(classToTest);
            System.out.printf(TESTS_FOUND_IN_CLASS, testScores.size(), className);
            for (Runner.TestScore score: testScores) {
                switch (score.getResult()) {
                    case Success: {
                        System.out.printf(TEST_PASSED, score.getName());
                        System.out.printf(TIME, score.getRunningTime());
                        break;
                    }

                    case Failure: {
                        System.out.printf(TEST_FAILED, score.getName());
                        String reason;
                        if (score.getFailureReason() == null) {
                            reason = EXPECTED_NOT_THROWN;
                        } else {
                            reason = score.getFailureReason().getMessage();
                        }
                        System.out.printf(FAILURE_CAUSE, reason);
                        System.out.printf(TIME, score.getRunningTime());
                        break;
                    }

                    case Ignore: {
                        System.out.printf(TEST_IGNORED, score.getName());
                        System.out.printf(IGNORE_REASON, score.getIgnoreReason());
                        break;
                    }
                }
                System.out.println("");
            }
        } catch (ClassNotFoundException e) {
            System.out.printf(CLASS_NOT_FOUND, className);
        } catch (InstantiationException e) {
            System.out.printf(BAD_CLASS, className);
        } catch (IllegalAccessException e) {
            System.out.printf(CLASS_NOT_ACCESSIBLE, className);
        } catch (Runner.InvocationException e) {
            System.out.printf(ERROR_IN_METHOD,
                    e.getCause().getClass().getName(),
                    e.getMethod().getName(),
                    e.getCause().getMessage());
        }
    }

    private static void showHelp() {
        System.out.println("List classes to test. Use -cp <dir> to add a directory " +
                "to classpath. Classpath can contain more than one directory. By " +
                "default the classpath contains the working directory.");
    }
}
