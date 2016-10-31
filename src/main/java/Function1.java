public abstract class Function1 < ArgType, ValueType > {
    public abstract ValueType apply(ArgType x) throws Exception;

    public < GValue > Function1 < ArgType, GValue > compose(final Function1 < ? super ValueType, GValue > g) {
        return new Function1<ArgType, GValue>() {
            @Override
            public GValue apply(ArgType x) throws Exception{
                return g.apply(Function1.this.apply(x));
            }
        };
    }
}
