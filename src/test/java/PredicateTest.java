import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

public class PredicateTest {
    @Test
    public void testConstants() throws Exception {
        Assert.assertTrue("ALWAYS_TRUE should return true for any input.",
                Predicate.ALWAYS_TRUE.apply(null));
        Assert.assertFalse("ALWAYS_FALSE should return false for any input",
                Predicate.ALWAYS_FALSE.apply(null));
    }

    @Test
    public void testNot() throws Exception {
        Assert.assertFalse("Not should invert the predicate value.",
                Predicate.ALWAYS_TRUE.not().apply(null));
        Assert.assertTrue("Not should invert the predicate value.",
                Predicate.ALWAYS_FALSE.not().apply(null));
    }

    @Test
    public void testAnd() throws Exception {
        Assert.assertTrue("And should perform conjunction.",
                Predicate.ALWAYS_TRUE.and(Predicate.ALWAYS_TRUE).apply(null));
        Assert.assertFalse("And should perform conjunction.",
                Predicate.ALWAYS_TRUE.and(Predicate.ALWAYS_FALSE).apply(null));
        Assert.assertFalse("And should perform conjunction.",
                Predicate.ALWAYS_FALSE.and(Predicate.ALWAYS_TRUE).apply(null));
        Assert.assertFalse("And should perform conjunction.",
                Predicate.ALWAYS_FALSE.and(Predicate.ALWAYS_FALSE).apply(null));

        try {
            Predicate.ALWAYS_FALSE.and(new Predicate<Object>() {
                @Override
                public Boolean apply(Object x) throws Exception {
                    throw new Exception();
                }
        }).apply(new Object());
        } catch (Exception e) {
            fail("And should be lazy.");
        }
    }

    @Test
    public void testOr() throws Exception {
        Assert.assertTrue("Or should perform disjunction.",
                Predicate.ALWAYS_TRUE.or(Predicate.ALWAYS_TRUE).apply(null));
        Assert.assertTrue("Or should perform disjunction.",
                Predicate.ALWAYS_TRUE.or(Predicate.ALWAYS_FALSE).apply(null));
        Assert.assertTrue("Or should perform disjunction.",
                Predicate.ALWAYS_FALSE.or(Predicate.ALWAYS_TRUE).apply(null));
        Assert.assertFalse("Or should perform disjunction.",
                Predicate.ALWAYS_FALSE.or(Predicate.ALWAYS_FALSE).apply(null));

        try {
            Predicate.ALWAYS_TRUE.or(new Predicate<Object>() {
                @Override
                public Boolean apply(Object x) throws Exception {
                    throw new Exception();
                }
            }).apply(null);
        } catch (Exception e) {
            fail("Or should be lazy.");
        }
    }
}
