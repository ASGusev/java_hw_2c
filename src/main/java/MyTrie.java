import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Андрей on 21.09.2016.
 */
public class MyTrie implements Trie, StreamSerializable {
    private Node root = new Node();

    public boolean add(String element) {
        boolean contained = contains(element);
        if (!contained) {
            Node pos = root;
            pos.incSize();
            for (int i = 0; i < element.length(); i++) {
                char symbol = element.charAt(i);
                if (!pos.hasKid(symbol)) {
                    pos.addKid(symbol);
                }
                pos = pos.getKid(symbol);
                pos.incSize();
            }
            pos.makeTerminal();
        }
        return !contained;
    }

    public boolean contains(String element) {
        Node pos = root;
        boolean exists = true;
        for (int i = 0; ((i < element.length()) && (exists)); i++) {
            if (pos.hasKid(element.charAt(i))) {
                pos = pos.getKid(element.charAt(i));
            } else {
                exists = false;
            }
        }
        if (exists && !pos.isTerminal()) {
            exists = false;
        }
        return exists;
    }

    public boolean remove(String element) {
        boolean contained = contains(element);
        if (contained) {
            Node pos = root;
            for (int i = 0; i < element.length(); i++) {
                pos.decSize();
                pos = pos.getKid(element.charAt(i));
            }
            pos.makeNotTerminal();
            pos = root;
            int i = 0;
            while (i < element.length() && pos.getKid(element.charAt(i)).
                    getSize() != 0) {
                pos = pos.getKid(element.charAt(i));
                i++;
            }
            if (i < element.length()) {
                pos.delKid(element.charAt(i));
            }
        }
        return contained;
    }

    public int size() {
        return root.getSize();
    }

    public int howManyStartsWithPrefix(String prefix) {
        Node pos = root;
        int ans = 0;
        boolean exists = true;
        for (int i = 0; ((i < prefix.length()) && (exists)); i++) {
            if (pos.hasKid(prefix.charAt(i))) {
                pos = pos.getKid(prefix.charAt(i));
            } else {
                exists = false;
            }
        }
        if (exists) {
            ans = pos.getSize();
        }
        return ans;
    }

    public void serialize(OutputStream out) throws IOException {
        root.boxToStream(out);
        out.flush();
    }

    public void deserialize(InputStream in) throws IOException {
        root.unboxFromStream(in);
    }

    private class Node {
        private static final int basicRepSize = 9;
        private boolean terminal = false;
        private int subtreeSize = 0;
        private Node[] kids = new Node[52];

        public int getSize() {
            return subtreeSize;
        }

        public void incSize() {
            subtreeSize++;
        }

        public void decSize() {
            subtreeSize--;
        }

        public void addKid(char c) {
            kids[makeIndex(c)] = new Node();
        }

        public Node getKid(char c) {
            return kids[makeIndex(c)];
        }

        public boolean hasKid(char c) {
            return kids[makeIndex(c)] != null;
        }

        public void delKid(char c) {
            kids[makeIndex(c)] = null;
        }

        public boolean isTerminal() {
            return terminal;
        }

        public void makeTerminal() {
            terminal = true;
        }

        public void makeNotTerminal() {
            terminal = false;
        }

        public void boxToStream(OutputStream stream) throws IOException {
            int kidsNumber = 0;
            byte[] representation = null;
            for (int i = 0 ; i < 52; i++) {
                if (kids[i] != null) {
                    kidsNumber++;
                }
            }
            representation = new byte[basicRepSize + kidsNumber];

            if (terminal) {
                representation[0] = 1;
            } else {
                representation[0] = 0;
            }
            disassembleInt(subtreeSize, representation, 1);
            disassembleInt(kidsNumber, representation, 5);

            byte curKid = 0;
            for (int i = 0; i < kidsNumber; i++) {
                while (kids[curKid] == null) {
                    curKid++;
                }
                representation[basicRepSize + i] = curKid;
                curKid++;
            }

            stream.write(representation);

            for (int i = 0; i < 52; i++) {
                if (kids[i] != null) {
                    kids[i].boxToStream(stream);
                }
            }
        }

        public void unboxFromStream(InputStream stream) throws IOException {
            byte[] basicData = new byte[basicRepSize];
            int kidsNumber = 0;
            byte[] kidsNumbers = null;
            stream.read(basicData, 0, basicRepSize);
            terminal = (basicData[0] == 1);
            subtreeSize = assembleInt(basicData, 1);
            kidsNumber = assembleInt(basicData, 5);

            for (int i = 0; i < 52; i++) {
                kids[i] = null;
            }
            kidsNumbers = new byte[kidsNumber];
            stream.read(kidsNumbers, 0, kidsNumber);
            for (int i = 0; i < kidsNumber; i++) {
                kids[kidsNumbers[i]] = new Node();
                kids[kidsNumbers[i]].unboxFromStream(stream);
            }
        }

        private int makeIndex(char c) {
            int index = -1;
            if (Character.isLowerCase(c)) {
                index = c - 'a';
            } else {
                index = c - 'A' + 26;
            }
            return index;
        }

        private void disassembleInt(int val, byte []arr, int beginning) {
            for (int i = 0; i < 4; i++) {
                arr[beginning + i] = (byte)(val >>> (8 * (3 - i)));
            }
        }

        private int assembleInt(byte[] arr, int beginning) {
            int ans = 0;
            for (int i = 0; i < 4; i++) {
                ans |= ((int)arr[beginning + i]) >>> (8 * (3 - i));
            }
            return ans;
        }
    }
}
