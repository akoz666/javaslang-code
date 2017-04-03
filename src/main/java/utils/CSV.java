package utils;

import javaslang.Value;
import javaslang.collection.List;
import javaslang.control.Try;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CSV {

    public static List<List<String>> readCsvJavaslang(String filename) {
        return List.ofAll(
            Try.of(() -> Files.lines(new File("src/main/resources/" + filename).toPath()).collect(Collectors.toList())).getOrElseGet(err -> {
                err.printStackTrace();
                return new ArrayList<>();
            })
        ).map(line -> List.of(line.split(";")));
    }

    public static java.util.List<java.util.List<String>> readCsvJava(String filename) {
        return readCsvJavaslang(filename).map(Value::toJavaList).toJavaList();
    }
}
