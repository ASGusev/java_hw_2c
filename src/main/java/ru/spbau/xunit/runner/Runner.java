package ru.spbau.xunit.runner;

import ru.spbau.xunit.annotations.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class for running xunit tests.
 */
public class Runner {
    /**
     * Runs all the tests in the given class. The class must have a public constructor
     * with no arguments.
     *
     * A test is a method with a @Test annotation. Tests with
     * specified ignoreReason are not launched.
     *
     * Methods annotated with @Before are invoked before every test; methods annotated
     * with @After are invoked after every test. Methods with @BeforeClass annotation
     * are invoked before all the tests; methods with @AfterClass annotation are
     * invoked after all the tests.
     *
     * @param classToTest the class containing the tests.
     * @return a list containing TestScore objects for all the tests in the class.
     * @throws IllegalAccessException if the class or its constructor is not accessible.
     * @throws InstantiationException if the class if impossible to instantiate.
     * @throws InvocationException if a @Before, @After, @BeforeClass or @AfterClass
     * method throws an exception.
     */
    @Nonnull
    public static List<TestScore> run(@Nonnull  Class classToTest)
            throws IllegalAccessException, InstantiationException, InvocationException {
        Object instance = classToTest.newInstance();
        List<TestScore> scores;
        List<Method> beforeMethods;
        List<Method> afterMethods;
        MethodRunner methodRunner = new MethodRunner(instance);

        beforeMethods = Arrays.stream(classToTest.getMethods())
                .filter(method -> method.getAnnotation(Before.class) != null)
                .collect(Collectors.toList());
        afterMethods = Arrays.stream(classToTest.getMethods())
                .filter(method -> method.getAnnotation(After.class) != null)
                .collect(Collectors.toList());

        runAnnotated(classToTest, BeforeClass.class, methodRunner);

        try {
            scores =  Arrays.stream(classToTest.getMethods())
                    .filter(method -> method.getAnnotation(Test.class) != null)
                    .map(method -> {
                        beforeMethods.forEach(methodRunner::run);
                        TestScore score = runTest(method, instance);
                        afterMethods.forEach(methodRunner::run);
                        return score;
                    })
                    .collect(Collectors.toList());
        } catch (InvocationError e) {
            throw new InvocationException(e.getMethod(), e.getCause());
        }

        runAnnotated(classToTest, AfterClass.class, methodRunner);

        return scores;
    }

    /**
     * Runs a single test on the supplied object.
     * @param method the method to run.
     * @param instance the instance of the class that should be used to invoke the method.
     * @return A TestScore object with test result.
     */
    @Nonnull
    private static TestScore runTest(@Nonnull Method method, @Nonnull Object instance) {
        Test testAnnotation = method.getAnnotation(Test.class);
        TestScore score = new TestScore(method.getName());
        String expectedException = testAnnotation.expected();
        if (testAnnotation.ignoreReason().equals(Test.ACTIVE)) {
            long startTime = 0;
            long finishTime = 0;
            try {
                startTime = System.currentTimeMillis();
                method.invoke(instance);
                finishTime = System.currentTimeMillis();
                if (expectedException.equals(Test.NO_EXCEPTIONS)) {
                    score.setResult(TestResult.Success);
                } else {
                    score.setResult(TestResult.Failure);
                }
            } catch (InvocationTargetException e) {
                finishTime = System.currentTimeMillis();
                Throwable t = e.getCause();
                Class errorClass = t.getClass();
                if (expectedException.equals(errorClass.getCanonicalName()) ||
                        expectedException.equals(errorClass.getSimpleName())) {
                    score.setResult(TestResult.Success);
                } else {
                    score.setResult(TestResult.Failure);
                    score.setFailureReason(t);
                }
            } catch (IllegalAccessException e) {
                score.setResult(TestResult.Failure);
                score.setFailureReason(e);
            }
            score.setRunningTime(finishTime - startTime);
        } else {
            score.setResult(TestResult.Ignore);
            score.setIgnoreReason(testAnnotation.ignoreReason());
        }
        return score;
    }

    private static void runAnnotated(@Nonnull Class classToTest,
                                     @Nonnull Class<? extends Annotation> annotationClass,
                                     @Nonnull MethodRunner methodRunner)
            throws InvocationException {
        try {
            Arrays.stream(classToTest.getMethods())
                    .filter(method -> method.getAnnotation(annotationClass) != null)
                    .forEach(methodRunner::run);
        } catch (InvocationError e) {
            throw new InvocationException(e.getMethod(), e.getCause());
        }
    }

    /**
     * A class for running methods with the instance that was provided to the
     * constructor. This class was created to run methods with a little method reference.
     */
    private static class MethodRunner {
        private final Object instance;

        private MethodRunner(@Nullable Object instance) {
            this.instance = instance;
        }

        private void run(@Nonnull Method method) {
            try {
                method.invoke(instance);
            } catch (IllegalAccessException e) {
                throw new InvocationError(method, e);
            } catch (InvocationTargetException e) {
                throw new InvocationError(method, e.getCause());
            }
        }
    }

    /**
     * A class representing score of a test.
     */
    public static class TestScore {
        private final String name;
        private TestResult result;
        private long runningTime;
        private String ignoreReason;
        private Throwable failureReason;

        private TestScore(@Nonnull String name) {
            this.name = name;
        }

        /**
         * Gets the result of the test.
         * @return Result.Success if the test finished successfully, Result.Failure if
         * the test failed, Result.Ignored if the test has been marked to ignore.
         */
        @Nonnull
        public TestResult getResult() {
            return result;
        }

        /**
         * Gets the reason to ignore the test.
         * @return ignoreReason parameter of the test if the test is ignored,
         * null otherwise.
         */
        @Nullable
        public String getIgnoreReason() {
            return ignoreReason;
        }

        /**
         * Gets the reason of the test failure.
         * @return an exception thrown by the test. null, if nothing was thrown or the
         * test was passed.
         */
        @Nullable
        public Throwable getFailureReason() {
            return failureReason;
        }

        /**
         * Gets the time that the test took to run in milliseconds.
         * @return the time taken by the test to run. 0 if the test was marked to ignore.
         */
        public long getRunningTime() {
            return runningTime;
        }

        /**
         * Gets the name of the test.
         * @return the name of the test.
         */
        @Nonnull
        public String getName() {
            return name;
        }

        private void setResult(@Nonnull TestResult result) {
            this.result = result;
        }

        private void setRunningTime(long runningTime) {
            this.runningTime = runningTime;
        }

        private void setIgnoreReason(@Nonnull String ignoreReason) {
            this.ignoreReason = ignoreReason;
        }

        private void setFailureReason(@Nonnull Throwable failureReason) {
            this.failureReason = failureReason;
        }
    }

    /**
     * Enumeration of possible test Results.
     */
    public enum TestResult {
        Success, Failure, Ignore
    }

    /**
     * An exception thrown in case of a @Before/@After/@BeforeClass/@AfterClass method
     * invocation failure.
     */
    public static class InvocationException extends Exception {
        private Method method;

        private InvocationException(@Nonnull Method method, @Nonnull Throwable cause) {
            super(cause);
            this.method = method;
        }

        @Nonnull
        public Method getMethod() {
            return method;
        }
    }

    private static class InvocationError extends Error {
        private Method method;

        private InvocationError(@Nonnull Method method, @Nonnull Throwable cause) {
            super(cause);
            this.method = method;
        }

        @Nonnull
        private Method getMethod() {
            return method;
        }
    }
}
