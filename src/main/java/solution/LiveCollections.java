package solution;

import javaslang.collection.List;
import javaslang.collection.Stream;
import models.Wine;
import utils.Console;
import utils.Important;
import utils.Run;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LiveCollections {

    public static void collections() {

        java.util.List<String> jlist = java.util.stream.Stream.of("a", "b", "c").collect(Collectors.toList());

        jlist.stream().map(i -> i.toUpperCase()).collect(Collectors.toList());   // PPPFFFF

        jlist.stream().filter(i -> i.length() > 3).collect(Collectors.toList()); // PPPFFFF

        List.of("a", "b", "c")
                .map(i -> i.toUpperCase())
                .map(i -> i.toLowerCase());

        List.of(Wine.random())
                .mkString(", ");

        java.util.stream.Stream.of(Wine.random())
                .map(w -> w.toString()) // WAT ???
                .collect(Collectors.joining(", "));

        List.of("a", "b", "c")
                .append("d")
                .remove("a")
                .map(i -> i.toUpperCase())
                .intersperse("|")
                .filter(i -> !i.equals("d"))
                .mkString(" "); // B | C

        java.util.List<String> list = java.util.stream.Stream.of("a", "b", "c").collect(Collectors.toList());
        list.add("d");
        list.remove("a");
        list
            .stream()
            .map(i -> i.toUpperCase())
            .flatMap(i -> java.util.stream.Stream.of("|", i))
            .skip(1)
            .filter(i -> !i.equals("d"))
            .collect(Collectors.joining(" ")); // B | C
    }

    @Run @Important
    public static void listOfOptions() {
        // TODO
    }

    public static void listOfOptionsSolution() {

        Console.log(
            Wine.javaslangIds
                .take(10)
                .map(id -> Wine.localFindById(id))
                .flatMap(o -> o)
        );

        Console.log(
            Wine.javaIds
                .stream()
                .map(id -> Optional.ofNullable(Wine.javaLocalFindById(id)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList())
        );
    }

    @Run
    public static void infiniteStreams() {
        // TODO
    }

    public static void infiniteStreamsSolution() {
        Stream<Integer> stream = Stream.from(1).filter(i -> i % 2 == 0);
        Console.log(stream.take(10).mkString(", "));
    }

    @Run @Important
    public static void integerRange() {
        // TODO
    }

    public static void integerRangeSolution() {
        List<Integer> ints =
                List.rangeClosed(1, 10);
    }

    @Run @Important
    public static void javaIntegerRange() {
        // TODO
    }

    public static void javaIntegerRangeSolution() {
        java.util.List<Integer> ints = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
    }

}
