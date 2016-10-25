import org.junit.Assert;
import org.junit.Test;
public class Function2Test {
    private Function2 difference = new Function2<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer x, Integer y) {
            return x - y;
        }
    };

    private Function1 triple = new Function1<Integer, Integer>() {
        @Override
        public Integer apply(Integer x) {
            return 3 * x;
        }
    };


    @Test
    public void composeTest() throws Exception {
        Assert.assertEquals("Compose works incorectlly.", 6,
                difference.compose(triple).apply(10, 8));
    }

    @Test
    public void bind1Test() throws Exception {
        Assert.assertEquals("Binding first argument works incorrectly.",
                3, difference.bind1(10).apply(7));
    }

    @Test
    public void bind2Test() throws Exception {
        Assert.assertEquals("Binding second arguement doesn't work corrrectly.",
                4, difference.bind2(6).apply(10));
    }

    @Test
    public void carryTest() throws Exception {
        Assert.assertEquals("Carry works incorrectly.",
                5, ((Function1)difference.carry().apply(10)).apply(5));
        //Function1<Integer,Integer> tenMinus = difference.carry().apply(10);
    }
}
