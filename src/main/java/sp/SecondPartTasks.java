package sp;

import javax.annotation.processing.Completion;
import java.awt.geom.Point2D;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.*;

public final class SecondPartTasks {

    private SecondPartTasks() {}

    // Найти строки из переданных файлов, в которых встречается указанная подстрока.
    public static List<String> findQuotes(List<String> paths, CharSequence sequence) {
        return paths
                .stream()
                .flatMap(filename -> {
                    try {
                        return Files.lines(Paths.get(filename));
                    } catch (Exception e) {
                        System.out.println(filename);
                        return Stream.empty();
                    }
                })
                .filter((line) -> line.contains(sequence))
                .collect(Collectors.toList());
    }

    // В квадрат с длиной стороны 1 вписана мишень.
    // Стрелок атакует мишень и каждый раз попадает в произвольную точку квадрата.
    // Надо промоделировать этот процесс с помощью класса java.util.Random и посчитать, какова вероятность попасть в мишень.
    public static double piDividedBy4() {
        final Point2D.Double CENTER = new Point2D.Double(0.5d, 0.5d);
        final double RADIUS = 0.5d;
        final double EPS = 1e-9d;
        final long NUMBER_OF_SHOTS = 30_000_000;

        Random random = new Random();
        return Stream
                .generate(() -> new Point2D.Double(random.nextDouble(), random.nextDouble()))
                .limit(NUMBER_OF_SHOTS)
                .mapToInt(
                        point -> CENTER.distance(point) < RADIUS + EPS ? 1 : 0)
                .average()
                .orElse(0);
    }

    // Дано отображение из имени автора в список с содержанием его произведений.
    // Надо вычислить, чья общая длина произведений наибольшая.
    public static String findPrinter(Map<String, List<String>> compositions) {
        return compositions
                .keySet()
                .stream()
                .max(Comparator.comparing(author -> compositions
                        .get(author)
                        .stream()
                        .collect(Collectors.summingLong(String::length))
                ))
                .orElse(null);
    }

    // Вы крупный поставщик продуктов. Каждая торговая сеть делает вам заказ в виде Map<Товар, Количество>.
    // Необходимо вычислить, какой товар и в каком количестве надо поставить.
    public static Map<String, Integer> calculateGlobalOrder(List<Map<String, Integer>> orders) {
        return orders
                .stream()
                .flatMap(map -> StreamSupport.stream(map.entrySet().spliterator(), false))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingInt(Map.Entry::getValue)));
    }
}
