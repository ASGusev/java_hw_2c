import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CollectionsTest {
    private ArrayList<Integer> nums = new ArrayList<Integer>();
    private final int TEST_LENGTH = 10;

    @Before
    public void initTests() {
        for (int i = 0; i < TEST_LENGTH; i++) {
            nums.add(i);
        }
    }

    @Test
    public void filterTest() throws Exception {
        List filtered = Collections.filter(nums, new Predicate<Integer>() {
            @Override
            public Boolean apply(Integer x) {
                return x % 2 == 0;
            }
        });

        for (int i = 0; i < TEST_LENGTH / 2; i++) {
            Assert.assertEquals("Filter doesn't work correctly",
                    i * 2, filtered.get(i));
        }
    }

    @Test
    public void foldlTest() throws Exception {
        Integer foldlSum = Collections.foldl(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer x, Integer y) {
                return x + y;
            }
        }, 0, nums);

        Integer sum = 0;
        for (Integer i: nums) {
            sum += i;
        }

        Assert.assertEquals("Foldl doesn't work correctly.", sum, foldlSum);
    }

    @Test
    public void foldrTest() throws Exception {
        String allNumsFoldr = Collections.foldr(new Function2<Integer, String, String>() {
            @Override
            public String apply(Integer x, String y) {
                return y.concat(x.toString());
            }
        }, "", nums);

        String allNums = "";
        for (int i = TEST_LENGTH - 1; i >= 0; i--) {
            allNums += String.valueOf(i);
        }

        Assert.assertEquals("Foldr doesn't work correctly.", allNums, allNumsFoldr);
    }

    @Test
    public void mapTest() throws Exception {
        List<Character> mapped = Collections.map(nums, new Function1<Integer, Character>() {
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
        List<Integer> taken = Collections.takeUnless(nums, new Predicate<Integer>() {
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
        List<Integer> taken = Collections.takeWhile(nums, new Predicate<Integer>() {
            @Override
            public Boolean apply(Integer x) {
                return x < TEST_LENGTH / 2;
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
