package ru.spbau.test2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {
    private final int n;
    private final List<List<Cell>> field;
    private Cell opened;

    public Game(int n) {
        if (n % 2 != 0) {
            throw new IllegalArgumentException("Odd field size.");
        }

        this.n = n;

        List<List<Integer>> values = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Integer> line = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                line.add(0);
            }
            values.add(line);
        }
        int ones = ((int)(Math.random() * (n * n / 2))) * 2;
        ArrayList<Integer> cellsList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                cellsList.add(i * n + j);
            }
        }
        Collections.shuffle(cellsList);
        cellsList.stream()
                .mapToInt(cell -> cell)
                .limit(ones)
                .forEach(cell -> values.get(cell / n).set(cell % n, 1));

        field = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Cell> line = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                line.add(new Cell(i, j, values.get(i).get(j)));
            }
            field.add(line);
        }
    }

    public int getN() {
        return n;
    }

    public Cell getCell(int x, int y) {
        return field.get(x).get(y);
    }

    public void open(Cell cell) {
        opened = cell;
    }

    public void close() {
        opened = null;
    }

    public Cell getOpened() {
        return opened;
    }

    public boolean hasOpened() {
        return opened != null;
    }

    public class Cell {
        private final int x;
        private final int y;
        private final int value;

        private Cell(int x, int y, int value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Cell && ((Cell)obj).x == x && ((Cell)obj).y == y;
        }
    }
}
