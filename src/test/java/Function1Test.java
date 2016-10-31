import org.junit.Assert;
import org.junit.Test;

public class Function1Test {
    @Test
    public void composeTest() throws Exception {
        Function1<Number,Integer> square = new Function1<Number, Integer>() {
            @Override
            public Integer apply(Number x) {
                return (Integer) x * (Integer)x;
            }
        };

        Function1<Number,Integer> duplicate = new Function1<Number, Integer>() {
            @Override
            public Integer apply(Number x) {
                return 2 * (Integer)x;
            }
        };

        Assert.assertEquals("Compose works incorrectly.", (Integer) 18,
                square.compose(duplicate).apply(3));
        Assert.assertEquals("Compose works incorrectly.", (Integer)36,
                duplicate.compose(square).apply(3));
    }
}
