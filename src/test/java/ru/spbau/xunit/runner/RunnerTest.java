package ru.spbau.xunit.runner;

import org.junit.Assert;
import org.junit.Test;
import ru.spbau.xunit.annotations.After;
import ru.spbau.xunit.annotations.AfterClass;
import ru.spbau.xunit.annotations.Before;
import ru.spbau.xunit.annotations.BeforeClass;

public class RunnerTest {
    @Test
    public void simpleSuccessTest() throws IllegalAccessException,
            Runner.InvocationException, InstantiationException {
        Runner.TestScore score = Runner.run(Good.class).get(0);

        Assert.assertEquals(Runner.TestResult.Success, score.getResult());
        Assert.assertTrue(Good.isCalled());
    }

    @Test
    public void simpleFailureTest() throws IllegalAccessException,
            Runner.InvocationException, InstantiationException {
        Runner.TestScore score = Runner.run(Bad.class).get(0);

        Assert.assertEquals(Runner.TestResult.Failure, score.getResult());
    }

    @Test
    public void timeTest() throws IllegalAccessException, Runner.InvocationException,
            InstantiationException {
        Runner.TestScore score = Runner.run(Timed.class).get(0);

        Assert.assertTrue(score.getRunningTime() >= Timed.runningTime);
    }

    @Test
    public void ignoreTest() throws IllegalAccessException, Runner.InvocationException,
            InstantiationException {
        Runner.TestScore score = Runner.run(Ignored.class).get(0);

        Assert.assertEquals(Runner.TestResult.Ignore, score.getResult());
        Assert.assertEquals(Ignored.REASON, score.getIgnoreReason());
        Assert.assertFalse(Ignored.isCalled());
    }

    @Test
    public void expectationSuccessTest() throws IllegalAccessException,
            Runner.InvocationException, InstantiationException {
        Runner.TestScore score = Runner.run(ExpectedThrown.class).get(0);

        Assert.assertEquals(Runner.TestResult.Success, score.getResult());
    }

    @Test
    public void expectationNotThrownTest() throws IllegalAccessException,
            Runner.InvocationException, InstantiationException {
        Runner.TestScore score = Runner.run(ExpectedNotThrown.class).get(0);

        Assert.assertEquals(Runner.TestResult.Failure, score.getResult());
        Assert.assertEquals(null, score.getFailureReason());
    }

    @Test
    public void expectationWrongException() throws IllegalAccessException,
            Runner.InvocationException, InstantiationException {
        Runner.TestScore score = Runner.run(WrongThrown.class).get(0);

        Assert.assertEquals(Runner.TestResult.Failure, score.getResult());
        Assert.assertEquals(WrongThrown.ERROR, score.getFailureReason());
    }

    @Test
    public void beforeTest() throws IllegalAccessException, Runner.InvocationException,
            InstantiationException {
        Runner.TestScore score = Runner.run(BeforeTester.class).get(0);
        Assert.assertEquals(Runner.TestResult.Success, score.getResult());
    }

    @Test
    public void afterTest() throws IllegalAccessException, Runner.InvocationException,
            InstantiationException {
        Runner.TestScore score = Runner.run(AfterTester.class).get(0);
        Assert.assertEquals(Runner.TestResult.Success, score.getResult());
        Assert.assertEquals(2, AfterTester.getCalls());
    }

    @Test
    public void beforeClassTest() throws IllegalAccessException,
            Runner.InvocationException, InstantiationException {
        Runner.TestScore score = Runner.run(BeforeClassTester.class).get(0);
        Assert.assertEquals(Runner.TestResult.Success, score.getResult());
    }

    @Test
    public void afterClassTest() throws IllegalAccessException,
            Runner.InvocationException, InstantiationException {
        Runner.TestScore score = Runner.run(AfterClassTester.class).get(0);
        Assert.assertEquals(Runner.TestResult.Success, score.getResult());
        Assert.assertEquals(3, AfterClassTester.getCalls());
    }

    public static class Good {
        public Good(){}
        private static boolean called = false;

        @ru.spbau.xunit.annotations.Test
        public void test() {
            called = true;
        }

        public static boolean isCalled() {
            return called;
        }
    }

    public static class Bad {
        @ru.spbau.xunit.annotations.Test
        public void test() {
            throw new Error();
        }
    }

    public static class Timed {
        public static final long runningTime = 100;

        @ru.spbau.xunit.annotations.Test
        public void test() {
            try {
                Thread.sleep(runningTime);
            } catch (InterruptedException e) {}
        }
    }

    public static class Ignored {
        private static boolean called = false;
        public final static String REASON = "reason";

        @ru.spbau.xunit.annotations.Test(ignoreReason = REASON)
        public void test() {
            called = true;
        }

        public static boolean isCalled() {
            return called;
        }
    }

    public static class ExpectedThrown {
        @ru.spbau.xunit.annotations.Test(expected = "Exception")
        public void test() throws Exception {
            throw new Exception();
        }
    }

    public static class ExpectedNotThrown {
        @ru.spbau.xunit.annotations.Test(expected = "Error")
        public void test() {}
    }

    public static class WrongThrown {
        public static Error ERROR = new Error();

        @ru.spbau.xunit.annotations.Test(expected = "Exception")
        public void test() throws Exception {
            throw ERROR;
        }
    }

    public static class BeforeTester {
        private int beforeCalls = 0;

        @Before
        public void before() {
            beforeCalls++;
        }

        @ru.spbau.xunit.annotations.Test
        public void test() {
            Assert.assertEquals(1, beforeCalls);
        }
    }

    public static class AfterTester {
        private static int calls = 0;

        @After
        public void after() {
            Assert.assertEquals(calls, 1);
            calls++;
        }

        @ru.spbau.xunit.annotations.Test
        public void test() {
            calls++;
        }

        public static int getCalls() {
            return calls;
        }
    }

    public static class BeforeClassTester {
        private int beforeCalls = 0;

        @BeforeClass
        public void beforeClass() {
            beforeCalls++;
        }

        @ru.spbau.xunit.annotations.Test
        public void test() {
            Assert.assertEquals(1, beforeCalls);
        }

        @ru.spbau.xunit.annotations.Test
        public void test2() {
            Assert.assertEquals(1, beforeCalls);
        }
    }

    public static class AfterClassTester {
        private static int calls = 0;

        @AfterClass
        public void afterClass() {
            Assert.assertEquals(calls, 2);
            calls++;
        }

        @ru.spbau.xunit.annotations.Test
        public void test() {
            calls++;
        }

        @ru.spbau.xunit.annotations.Test
        public void test2() {
            calls++;
        }

        public static int getCalls() {
            return calls;
        }
    }
}
