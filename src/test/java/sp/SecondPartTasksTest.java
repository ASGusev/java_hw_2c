package sp;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SecondPartTasksTest {

    @Test
    public void testFindQuotes() throws IOException {
        final String[][] FILES_CONTENT =  { { "qwerty", "", "foo", "bar", "foobar" },
                { "bar", "baz" }, {}};
        final ArrayList<String> filesList = new ArrayList<String>();
        final String[] EXPECTED_ANSWER = { "foo", "foobar" };

        for (int i = 1; i <= 3; i++) {
            String curFileName = "testFindQuotes" + String.valueOf(i) + ".txt";
            filesList.add(curFileName);

            Files.write(Paths.get(curFileName), Arrays.asList(FILES_CONTENT[i - 1]));
        }

        Assert.assertArrayEquals("FindQuotes works incorrectly.", EXPECTED_ANSWER,
                SecondPartTasks.findQuotes(filesList, "foo").toArray());

        for (String file: filesList) {
            Files.delete(Paths.get(file));
        }
    }

    @Test
    public void testPiDividedBy4() {
        final double EXPECTED = Math.PI / 4;
        final double EPS = 1e-4;

        Assert.assertEquals("The probability of hit is " + String.valueOf(EXPECTED) + ".",
                EXPECTED, SecondPartTasks.piDividedBy4(), EPS);
    }

    @Test
    public void testFindPrinter() {
        final String[] printers = { "p1", "p2", "p3", "p4" };
        final String[][] works = { {"story", "story"},
            {"so", "me", "sho", "rt", "sto", "ri", "es"},
            {"a very-very long story", "and one more"}, {"just tale"} };
        final String EXPECTED = "p3";

        HashMap<String,List<String>> compositions = new HashMap<>();
        for (int i = 0; i < printers.length; i++) {
            compositions.put(printers[i], Arrays.asList(works[i]));
        }

        Assert.assertEquals(EXPECTED, SecondPartTasks.findPrinter(compositions));
    }

    @Test
    public void testCalculateGlobalOrder() {
        final String[][] shopsProductNames = { { "Rice", "Macaroni", "Bread" },
                { "Rice", "Macaroni" }, { "Rice" }, {} };
        final Integer[][] shopsProductAmounts = { {10, 3, 5}, {100, 0}, {1000}, {} };
        final String[] allProductNames = { "Rice", "Macaroni", "Bread" };
        final Integer[] allProductsAmounts = { 1110, 3, 5 };

        ArrayList <Map<String,Integer>> orders = new ArrayList<Map<String,Integer>>();

        for (int i = 0; i < shopsProductAmounts.length; i++) {
            Map<String,Integer> curOrder = new HashMap<String,Integer>();
            for (int j = 0; j < shopsProductAmounts[i].length; j++) {
                curOrder.put(shopsProductNames[i][j], shopsProductAmounts[i][j]);
            }
            orders.add(curOrder);
        }

        Map<String,Integer> result = SecondPartTasks.calculateGlobalOrder(orders);

        Assert.assertEquals("The resulting map should contain all mentioned products once.",
                allProductNames.length, result.size());

        for (int i = 0; i < allProductNames.length; i++) {
            Assert.assertEquals("The global amount of a product odered should match " +
                    "summary amount of all orders.", allProductsAmounts[i],
                    result.get(allProductNames[i]));
        }
    }
}