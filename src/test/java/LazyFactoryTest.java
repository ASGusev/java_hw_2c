import org.junit.Assert;
import org.junit.Test;

/**
 * A class for testing Lazy implementations created by LazyFactory.
 */

public class LazyFactoryTest {
    /**
     * A test to check that the single-thread implementation returns the required value.
     */
    @Test
    public void singleThreadSimpleTest() {
        final Integer ret = 42;
        Lazy <Integer> lazy = LazyFactory.createLazySingleThread(() -> ret);
        Assert.assertEquals(ret, lazy.get());
    }

    /**
     * A test that checks if any calls to single-thread Lazy return different objects.
     */
    @Test
    public void singleThreadCoincidenceTest() {
        Lazy lazy = LazyFactory.createLazySingleThread(Object::new);
        Assert.assertTrue(lazy.get() == lazy.get());
    }

    /**
     * A test that checks if single-thread Lazy calls supplier more than once.
     */
    @Test
    public void singleThreadAppealTest() {
        AppealCounter counter = new AppealCounter();
        Lazy lazy = LazyFactory.createLazySingleThread(counter::appeal);
        for (int i = 0; i < 5; i++) {
            lazy.get();
        }
        Assert.assertEquals(1, counter.getCount());
    }

    /**
     * A test that checks if multi-thread Lazy calls supplier more than once.
     */
    @Test
    public void multiThreadAppealTest() {
        AppealCounter counter = new AppealCounter();
        final Lazy lazy = LazyFactory.createLazyMultiThread(counter::appeal);
        final int THREADS_NUMBER = 5;
        Thread threads[] = new Thread[THREADS_NUMBER];
        for (int i = 0; i < THREADS_NUMBER; i++) {
            threads[i] = new Thread(lazy::get);
        }

        for (Thread thread: threads) {
            thread.start();
        }
        for (Thread thread: threads) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {}
            }
        }
        Assert.assertEquals(1, counter.getCount());
    }

    /**
     * A test that checks if the lock-free implementation returns
     * different objects after different calls.
     */
    @Test
    public void multiThreadLockFreeCoincidenceTest() {
        final int THREADS_NUMBER = 5;
        Thread threads[] = new Thread[THREADS_NUMBER];
        Object returned[] = new Object[THREADS_NUMBER];
        final Lazy lazy = LazyFactory.createLazyLockFree(Object::new);
        for (int i = 0; i < THREADS_NUMBER; i++) {
            final int pos = i;
            threads[i] = new Thread(() -> returned[pos] = lazy.get());
        }

        for (Thread thread: threads) {
            thread.start();
        }
        for (Thread thread: threads) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {}
            }
        }

        for (int i = 1; i < THREADS_NUMBER; i++) {
            Assert.assertSame(returned[i - 1], returned[i]);
        }
    }

    private static class AppealCounter {
        private volatile int appeals = 0;

        public Object appeal() {
            appeals++;
            return new Object();
        }

        public int getCount() {
            return appeals;
        }
    }
}
