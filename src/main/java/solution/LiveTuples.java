package solution;

import javaslang.Tuple;
import javaslang.Tuple4;
import org.joda.time.DateTime;
import org.reactivecouchbase.json.JsObject;
import org.reactivecouchbase.json.Json;
import utils.Console;

public class LiveTuples {

    public static void tuples() {
        Console.log(
            Tuple.of(2, 2)
        );

        Tuple.of(2, 2).map(
            a -> a + 3,
            b -> b + 12
        );

        Tuple4<String, Integer, DateTime, JsObject> tuple =
                Tuple.of("a", 1, DateTime.now(), Json.obj());
    }
}
