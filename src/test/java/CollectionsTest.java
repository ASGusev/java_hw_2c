import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CollectionsTest {
    private ArrayList<Integer> numbers = new ArrayList<Integer>();
    private final int TEST_LENGTH = 10;

    @Before
    public void initTests() {
        for (int i = 0; i < TEST_LENGTH; i++) {
            numbers.add(i);
        }
    }

    @Test
    public void filterTest() throws Exception {
        List filtered = Collections.filter(numbers, new Predicate<Number>() {
            @Override
            public Boolean apply(Number x) {
                return (Integer)x % 2 == 0;
            }
        });

        for (int i = 0; i < TEST_LENGTH / 2; i++) {
            Assert.assertEquals("Filter doesn't work correctly",
                    i * 2, filtered.get(i));
        }
    }

    @Test
    public void foldlTest() throws Exception {
        String allNumsFoldl = Collections.foldl(new Function2<String, Integer, String>() {
            @Override
            public String apply(String y, Integer x) {
                return y.concat(x.toString());
            }
        }, "", numbers);

        String allNums = "";
        for (int i = 0; i < TEST_LENGTH; i++) {
            allNums += String.valueOf(i);
        }

        Assert.assertEquals("Foldl doesn't work correctly.", allNums, allNumsFoldl);
    }

    @Test
    public void foldrTest() throws Exception {
        String allNumsFoldr = Collections.foldr(new Function2<Number, String, String>() {
            @Override
            public String apply(Number x, String y) {
                return y.concat(x.toString());
            }
        }, "", numbers);

        String allNums = "";
        for (int i = TEST_LENGTH - 1; i >= 0; i--) {
            allNums += String.valueOf(i);
        }

        Assert.assertEquals("Foldr doesn't work correctly.", allNums, allNumsFoldr);
    }

    @Test
    public void mapTest() throws Exception {
        List<Character> mapped = Collections.map(numbers, new Function1<Integer, Character>() {
            @Override
            public Character apply(Integer x) {
                return (char)((int)'a' + (int)x);
            }
        });

        Character[] answer = new Character[TEST_LENGTH];
        int letter = (int)'a';
        for (int i = 0; i < TEST_LENGTH; i++) {
            answer[i] = (char)(i + (char)'a');
        }
        Assert.assertArrayEquals("Map doesn't work correctly.",
                answer, mapped.toArray());
    }

    @Test
    public void takeUnlessTest() throws Exception {
        List<Integer> taken = Collections.takeUnless(numbers, new Predicate<Integer>() {
            @Override
            public Boolean apply(Integer x) {
                return x >= TEST_LENGTH / 2;
            }
        });

        Integer[] answer = new Integer[TEST_LENGTH / 2];
        for (int i = 0; i < TEST_LENGTH / 2; i++) {
            answer[i] = i;
        }

        Assert.assertArrayEquals("TakeUnless works incorrectly.",
                answer, taken.toArray());
    }

    @Test
    public void takeWhileTest() throws Exception {
        List<Integer> taken = Collections.takeWhile(numbers, new Predicate<Number>() {
            @Override
            public Boolean apply(Number x) {
                return (Integer)x < TEST_LENGTH / 2;
            }
        });

        Integer[] answer = new Integer[TEST_LENGTH / 2];
        for (int i = 0; i < TEST_LENGTH / 2; i++) {
            answer[i] = i;
        }

        Assert.assertArrayEquals("TakeWhile works incorrectly.",
                answer, taken.toArray());
    }
}
