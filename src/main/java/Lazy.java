/**
 * An interface providing lazy computation of a Supplier. Computation is
 * started after the first request of the value. All requests return the
 * same object.
 * @param <T> the class of the object returned by the Supplier.
 */

public interface Lazy<T> {
    /**
     * Returns the value returned by the supplier's get method. All calls
     * return the same object.
     * @return the value returned by the supplier's get method.
     */
    T get();
}
