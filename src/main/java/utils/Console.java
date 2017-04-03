package utils;

import javaslang.collection.List;

public class Console {

    public static Done log(Object... args) {
        System.out.println(List.of(args).mkString(" "));
        return Done.Instance;
    }
}
