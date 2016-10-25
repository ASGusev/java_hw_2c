/**
 * Created by Андрей on 12.10.2016.
 */
public abstract class Predicate <ArgType> extends Function1 <ArgType, Boolean> {
    public abstract Boolean apply(ArgType x) throws Exception;

    public Predicate or(final Predicate <? super ArgType> other) {
        return new Predicate<ArgType>() {
            @Override
            public Boolean apply(ArgType x) throws Exception{
                return Predicate.this.apply(x) || other.apply(x);
            }
        };
    }

    public Predicate and(final Predicate <? super ArgType> other) {
        return new Predicate<ArgType>() {
            @Override
            public Boolean apply(ArgType x) throws Exception {
                return Predicate.this.apply(x) && other.apply(x);
            }
        };
    }

    public Predicate not() {
        return new Predicate<ArgType>() {
            @Override
            public Boolean apply(ArgType x) throws Exception {
                return !Predicate.this.apply(x);
            }
        };
    }

    public static final Predicate ALWAYS_TRUE = new Predicate() {
        @Override
        public Boolean apply(Object x) {
            return true;
        }
    };

    public static final Predicate ALWAYS_FALSE = new Predicate() {
        @Override
        public Boolean apply(Object x) {
            return false;
        }
    };
}
