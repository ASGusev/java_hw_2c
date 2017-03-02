import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * An abstract class providing factories of Lazy implementations with
 * different properties.
 */

public abstract class LazyFactory {
    private static final Object EMPTY = new Object();

    /**
     * Constructs a non thread-safe Lazy implementation calculating the given supplier.
     * The supplier's get() method is only called once.
     * @param supplier the supplier calculating the required value.
     * @param <T> the class of the returned value.
     * @return A non thread-safe Lazy implementation with the given supplier
     */
    public static <T> Lazy<T> createLazySingleThread(Supplier<T> supplier) {
        return new Lazy<T>() {
            private T result = (T)EMPTY;

            @Override
            public T get() {
                if (result == EMPTY) {
                    result = supplier.get();
                }
                return result;
            }
        };
    }

    /**
     * Constructs a thread-safe Lazy implementation with the given supplier.
     * get() call may lead to locks. The calculation can only be called once.
     * @param supplier the supplier calculating the required value.
     * @param <T> the class of the returned value.
     * @return A thread-safe implementation of Lazy interface with locks possible.
     */
    public static <T> Lazy<T> createLazyMultiThread(Supplier<T> supplier) {
        return new Lazy<T>() {
            private volatile T result = (T)EMPTY;

            @Override
            public T get() {
                T res = result;
                if (res == EMPTY) {
                    synchronized (this) {
                        res = result;
                        if (res == EMPTY) {
                            result = res = supplier.get();
                        }
                    }
                }
                return res;
            }
        };
    }

    /**
     * Creates a thread-safe implementation of Lazy interface. A call of get() method
     * does not lead to locks. Supplier calculation can be called more than once.
     * The object returned is always the same, though.
     * @param supplier the supplier calculating the required value.
     * @param <T> the class of the returned value.
     * @return A thread-safe Lazy implementation without locks.
     */
    public static <T> Lazy<T> createLazyLockFree(Supplier<T> supplier) {
        return new Lazy<T>() {
            private AtomicReference<T> result = new AtomicReference<T>((T)EMPTY);

            @Override
            public T get() {
                if (result.get() == EMPTY) {
                    result.compareAndSet((T)EMPTY, supplier.get());
                }
                return result.get();
            }
        };
    }
}
