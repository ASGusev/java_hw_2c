import org.junit.Assert;
import org.junit.Test;

public class Function2Test {
    private Function2<Number, Number, Integer> difference = new Function2<Number, Number, Integer>() {
        @Override
        public Integer apply(Number x, Number y) {
            return (Integer)x - (Integer)y;
        }
    };

    private Function1<Number, Integer> triple = new Function1<Number, Integer>() {
        @Override
        public Integer apply(Number x) {
            return 3 * (Integer) x;
        }
    };


    @Test
    public void composeTest() throws Exception {
        Assert.assertEquals("Compose works incorrectly.", (Integer)6,
                difference.<Integer>compose(triple).apply(10, 8));
    }

    @Test
    public void bind1Test() throws Exception {
        Assert.assertEquals("Binding first argument works incorrectly.",
                (Integer)3, difference.<Integer>bind1(10).apply(7));
    }

    @Test
    public void bind2Test() throws Exception {
        Assert.assertEquals("Binding second argument doesn't work correctly.",
                (Integer) 4, difference.<Integer>bind2(6).apply(10));
    }

    @Test
    public void carryTest() throws Exception {
        Assert.assertEquals("Carry works incorrectly.",
                (Integer)5, difference.carry().apply(10).apply(5));
    }
}
