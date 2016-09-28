import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

public class MyHashMapTest {
    private MyHashMap map = new MyHashMap();

    @Before
    public void fill() {
        map.put("a", "one");
        map.put("b", "two");
        map.put("c", "three");
    }

    @Test
    public void testSize() {
        org.junit.Assert.assertTrue("Size method " +
                "returned incorrect value.", map.size() == 3);
    }

    @Test
    public void testClear() {
        map.clear();
        org.junit.Assert.assertTrue("Clear method did't " +
                "work correctly.", map.size() == 0);
        org.junit.Assert.assertFalse("Clear method did't " +
                "work correctly.", map.contains("a"));
    }

    @Test
    public void testPut() {
        org.junit.Assert.assertNull("Put method did't return " +
                "null for a brand new key",
                map.put("d", "Four"));
        org.junit.Assert.assertTrue("Incorrect size after " +
                "putting a brand new pair.", map.size() == 4);
        org.junit.Assert.assertEquals("Put method didn't " +
                "return old value for an existent key.",
                map.put("d", "four"), "Four");
        org.junit.Assert.assertTrue("Incorrect size after " +
                "putting a new value for an old key.",
                map.size() == 4);
    }

    @Test
    public void testContains() {
        org.junit.Assert.assertTrue("Contains method " +
                "returned false for an existent key",
                map.contains("a"));
        org.junit.Assert.assertFalse("Contains method " +
                "returned true for a nonexistent key.",
                map.contains("d"));
    }

    @Test
    public void testGet() {
        org.junit.Assert.assertEquals("Get method returned " +
                "wrong value.", map.get("a"), "one");
        map.put("a", "One");
        org.junit.Assert.assertEquals("Get method worked " +
                "incorrectly with a changed value.",
                map.get("a"), "One");
        org.junit.Assert.assertNull("Get methods didn't " +
                "return null for a nonexistent key.",
                map.get("d"));
    }

    @Test
    public void testRemove() {
        org.junit.Assert.assertEquals("Remove method didn't" +
                " return correct value for removed key.",
                "one", map.remove("a"));
        org.junit.Assert.assertNull("Remove method didn't " +
                "return null value for nonexistent key.",
                map.remove("a"));
        org.junit.Assert.assertFalse("Remove method didn't" +
                " work properly.", map.contains("a"));
        org.junit.Assert.assertNull("Remove method didn't" +
                " work properly.", map.get("a"));
    }
}
