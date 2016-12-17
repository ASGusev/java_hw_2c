import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class ThreadPoolImplTest {
    private final double EPS = 1e-9;

    @Test
    public void simpleTest() throws LightExecutionException, InterruptedException {
        ThreadPoolImpl pool = new ThreadPoolImpl(1);
        LightFuture<Double> pi = pool.submit(() -> Math.PI);
        assertEquals(Math.PI, pi.get(), EPS);
    }

    @Test (expected = LightExecutionException.class)
    public void exceptionTest() throws LightExecutionException, InterruptedException {
        ThreadPoolImpl pool = new ThreadPoolImpl(1);
        LightFuture future = pool.submit(() -> {
            throw new UnsupportedOperationException();
        });
        future.get();
    }

    @Test
    public void thenApplyTest() throws LightExecutionException, InterruptedException {
        ThreadPoolImpl pool = new ThreadPoolImpl(1);
        LightFuture<Double> firstFuture = pool.submit(() -> 2.0);
        LightFuture<Double> secondFuture = firstFuture.thenApply(Math::sqrt);
        assertEquals(Math.sqrt(2.0), (double)secondFuture.get(), EPS);
    }

    @Test (expected = InterruptedException.class)
    public void shutdownTest() throws LightExecutionException, InterruptedException {
        ThreadPoolImpl pool = new ThreadPoolImpl(1);
        pool.shutdown();
        pool.submit(() -> 0).get();
    }

    @Test
    public void threadsNumberTest() throws LightExecutionException,
            InterruptedException {
        final int threadsNumber = 16;

        ThreadPoolImpl pool = new ThreadPoolImpl(threadsNumber);
        Set<Long> threadIds = new HashSet<Long>();
        LightFuture<Long> futures[] = new LightFuture[threadsNumber * 4];

        for (int i = 0; i < threadsNumber * 4; i++) {
            futures[i] = pool.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                return Thread.currentThread().getId();
            });
        }
        for (LightFuture<Long> future: futures) {
            threadIds.add(future.get());
        }

        assertEquals(threadsNumber, threadIds.size());
    }
}
