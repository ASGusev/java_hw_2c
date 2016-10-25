import org.junit.Assert;
import org.junit.Test;

public class Function1Test {
    @Test
    public void composeTest() throws Exception {
        Function1 square = new Function1<Integer, Integer>() {
            @Override
            public Integer apply(Integer x) {
                return x * x;
            }
        };

        Function1 duplicate = new Function1<Integer, Integer>() {
            @Override
            public Integer apply(Integer x) {
                return 2 * x;
            }
        };

        Assert.assertEquals("Compose works incorrectly.", 18,
                square.compose(duplicate).apply(3));
        Assert.assertEquals("Compose works incorrectly.", 36,
                duplicate.compose(square).apply(3));
    }
}
