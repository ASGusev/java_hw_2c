import java.util.function.Function;

public interface LightFuture<T> {
    public boolean isReady();

    public T get() throws LightExecutionException, InterruptedException;

    public <U> LightFuture<U> thenApply(Function<T,U> f);
}
