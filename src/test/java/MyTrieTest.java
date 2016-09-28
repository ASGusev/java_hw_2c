/**
 * Created by andy on 25.09.16.
 */
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MyTrieTest {
    MyTrie trie = null;
    @Before
    public void fill() {
        trie = new MyTrie();
        Assert.assertTrue("Add should return true for a " +
                "new key.", trie.add("abc"));
        Assert.assertFalse("Add should return false for " +
                "an old key.", trie.add("abc"));
        Assert.assertTrue("Add should return true for a " +
                "new key.", trie.add("abcd"));
        Assert.assertTrue("Add should return true for a " +
                "new key.", trie.add("def"));
    }

    @Test
    public void testSize() {
        Assert.assertEquals("Wrong size after addition.",
                trie.size(), 3);
    }

    @Test
    public void testContains() {
        Assert.assertTrue("Contains method should return true" +
                " for an existing key.", trie.contains("abc"));
        Assert.assertFalse("Contains method should return false" +
                " for a nonexent key", trie.contains("ab"));
        Assert.assertFalse("Contains method should return false" +
                " for a nonexent key", trie.contains("DEF"));
    }

    @Test
    public void testRemove() {
        Assert.assertTrue("Remove should return true for " +
                "an existent key", trie.remove("abc"));
        Assert.assertTrue("Remove should return true for " +
                "an existent key", trie.remove("abcd"));
        Assert.assertEquals("Size should decrease with " +
                "every present key deletion.", 1, trie.size());
        Assert.assertFalse("Remove should return false for" +
                " a nonexistent key", trie.remove("abc"));
        Assert.assertTrue("Remove should return true for " +
                "an existent key", trie.remove("def"));

        Assert.assertEquals("Wrong size after removal", 0,
                trie.size());

        Assert.assertFalse("Contains should return false " +
                "for a deleted key", trie.contains("abc"));
        Assert.assertFalse("Contains should return false " +
                "for a deleted key", trie.contains("abcd"));

        Assert.assertEquals("Prefix should disappear after" +
                        " remove call.", 0,
                trie.howManyStartsWithPrefix("abc"));
        Assert.assertEquals("Prefix should disappear after" +
                        " remove call.", 0,
                trie.howManyStartsWithPrefix("ab"));
    }

    @Test
    public void testPrefix() {
        Assert.assertEquals("Wrong subtree size", 2,
                trie.howManyStartsWithPrefix("abc"));
        Assert.assertEquals("Wrong subtree size", 1,
                trie.howManyStartsWithPrefix("abcd"));
        Assert.assertEquals("Wrong subtree size", 2,
                trie.howManyStartsWithPrefix("ab"));
        Assert.assertEquals("No elements should start " +
                "with a new prefix.", 0,
                trie.howManyStartsWithPrefix("q"));
        Assert.assertEquals("No elemennts should start " +
                "with a longer prefix.", 0,
                trie.howManyStartsWithPrefix("defg"));
    }

    @Test
    public void testSerialisation() {
        final String FILENAME = "trieStream.bin";
        try (FileOutputStream fout =
                     new FileOutputStream(FILENAME)) {
            trie.serialize(fout);
        } catch (java.io.IOException e) {
            Assert.fail("Serialization failed.");
        }

        MyTrie newTrie = new MyTrie();
        try (FileInputStream fin =
                     new FileInputStream(FILENAME)) {
            newTrie.deserialize(fin);
        } catch (java.io.IOException e) {
          Assert.fail("Deserialization failed.");
        }

        Assert.assertEquals("Deserialized trie should have the" +
                " same size with serialized.", 3, newTrie.size());

        Assert.assertTrue("Deserialized trie should contain all" +
                " the strings that serialized contained",
                newTrie.contains("abc"));
        Assert.assertTrue("Deserialized trie should contain all" +
                " the strings that serialized contained",
                newTrie.contains("abcd"));
        Assert.assertTrue("Deserialized trie should contain all" +
                " the strings that serialized contained",
                newTrie.contains("def"));

        Assert.assertEquals("Deserialized trie should be " +
                    "completely same as serialized.", 2,
                newTrie.howManyStartsWithPrefix("abc"));
        Assert.assertEquals("Deserialized trie should be " +
                        "completely same as serialized.", 2,
                newTrie.howManyStartsWithPrefix("ab"));
        Assert.assertEquals("Deserialized trie should be " +
                        "completely same as serialized.", 1,
                newTrie.howManyStartsWithPrefix("abcd"));
        Assert.assertEquals("Deserialized trie should be " +
                        "completely same as serialized.", 0,
                newTrie.howManyStartsWithPrefix("bc"));
    }
}
