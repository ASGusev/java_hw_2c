public abstract class Predicate <ArgType> extends Function1 <ArgType, Boolean> {
    public abstract Boolean apply(ArgType x) throws Exception;

    public Predicate<ArgType> or(final Predicate <? super ArgType> other) {
        return new Predicate<ArgType>() {
            @Override
            public Boolean apply(ArgType x) throws Exception{
                return Predicate.this.apply(x) || other.apply(x);
            }
        };
    }

    public Predicate<ArgType> and(final Predicate <? super ArgType> other) {
        return new Predicate<ArgType>() {
            @Override
            public Boolean apply(ArgType x) throws Exception {
                return Predicate.this.apply(x) && other.apply(x);
            }
        };
    }

    public Predicate<ArgType> not() {
        return new Predicate<ArgType>() {
            @Override
            public Boolean apply(ArgType x) throws Exception {
                return !Predicate.this.apply(x);
            }
        };
    }

    public static final Predicate<Object> ALWAYS_TRUE = new Predicate<Object>() {
        @Override
        public Boolean apply(Object x) {
            return true;
        }
    };

    public static final Predicate<Object> ALWAYS_FALSE = new Predicate<Object>() {
        @Override
        public Boolean apply(Object x) {
            return false;
        }
    };
}
