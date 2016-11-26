package sp;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.*;

public class SecondPartTasksTest {

    @Test
    public void testFindQuotes() throws IOException {
        final List <List<String>> FILES_CONTENT = new ArrayList<>();
        FILES_CONTENT.add(Arrays.asList("qwerty", "", "foo", "bar", "foobar"));
        FILES_CONTENT.add(Arrays.asList("bar", "baz"));
        FILES_CONTENT.add(Collections.emptyList());

        final List <String> EXPECTED_ANSWER = new ArrayList<>();
        EXPECTED_ANSWER.add("foo");
        EXPECTED_ANSWER.add("foobar");

        final ArrayList<String> filesList = new ArrayList<String>();

        for (int i = 1; i <= 3; i++) {
            String curFileName = "testFindQuotes" + i + ".txt";
            filesList.add(curFileName);

            Files.write(Paths.get(curFileName), FILES_CONTENT.get(i - 1));
        }

        Assert.assertEquals("FindQuotes works incorrectly.", EXPECTED_ANSWER,
                SecondPartTasks.findQuotes(filesList, "foo"));

        for (String file: filesList) {
            Files.delete(Paths.get(file));
        }
    }

    @Test
    public void testPiDividedBy4() {
        final double EXPECTED = Math.PI / 4;
        final double EPS = 1e-4;

        Assert.assertEquals("The probability of hit is " + EXPECTED + ".",
                EXPECTED, SecondPartTasks.piDividedBy4(), EPS);
    }

    @Test
    public void testFindPrinter() {
        final Map<String,List<String>> COMPOSITIONS = ImmutableMap.of(
                "p1", Arrays.asList("story", "story"),
                "p2", Arrays.asList("so", "me", "sho", "rt", "sto", "ri", "es"),
                "p3", Arrays.asList("a very-very long story", "and one more"),
                "p4", Arrays.asList("just tale")
        );

        final String EXPECTED = "p3";

        Assert.assertEquals(EXPECTED, SecondPartTasks.findPrinter(COMPOSITIONS));
    }

    @Test
    public void testCalculateGlobalOrder() {
        final Map<String,Integer> EXPECTED = ImmutableMap.of(
                "Rice", 1110,
                "Macaroni", 3,
                "Bread", 5
        );

        List <Map<String,Integer>> orders = new ArrayList<>();
        orders.add(ImmutableMap.of(
                "Rice", 10,
                "Macaroni", 3,
                "Bread", 5
        ));
        orders.add(ImmutableMap.of(
                "Rice", 100,
                "Macaroni", 0
        ));
        orders.add(ImmutableMap.of(
                "Rice", 1000
        ));
        
        Assert.assertEquals("Calculate global order works incorrectly.", EXPECTED,
                SecondPartTasks.calculateGlobalOrder(orders));
    }
}