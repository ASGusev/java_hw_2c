public abstract class Function2<ArgOneType, ArgTwoType, ValueType> {
    public abstract ValueType apply(ArgOneType x, ArgTwoType y) throws Exception;

    public < GValueType > Function2<ArgOneType, ArgTwoType, GValueType> compose(final Function1 < ? super ValueType, GValueType > g) {
        return new Function2<ArgOneType, ArgTwoType, GValueType>() {
            @Override
            public GValueType apply(ArgOneType x, ArgTwoType y) throws Exception {
                return g.apply(Function2.this.apply(x, y));
            }
        };
    }

    public Function1<ArgTwoType, ValueType> bind1(final ArgOneType argOne) {
        return new Function1 < ArgTwoType, ValueType > () {
            @Override
            public ValueType apply(ArgTwoType x) throws Exception {
                return Function2.this.apply(argOne, x);
            }
        };
    }

    public Function1<ArgOneType,ValueType> bind2(final ArgTwoType argTwo) {
        return new Function1 < ArgOneType, ValueType > () {
            @Override
            public ValueType apply(ArgOneType x) throws Exception {
                return Function2.this.apply(x, argTwo);
            }
        };
    }

    public Function1<ArgOneType,Function1<ArgTwoType, ValueType>> carry() {
        return new Function1<ArgOneType, Function1<ArgTwoType, ValueType>> () {
            @Override
            public Function1<ArgTwoType, ValueType> apply(final ArgOneType x) {
                return new Function1<ArgTwoType, ValueType> () {
                    @Override
                    public ValueType apply(ArgTwoType y) throws Exception{
                        return Function2.this.apply(x, y);
                    }
                };
            }
        };
    }
}