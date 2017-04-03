package solution;

import javaslang.Lazy;
import javaslang.collection.List;
import javaslang.control.Option;
import javaslang.control.Try;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import utils.Console;
import utils.Important;
import utils.Run;

import java.io.NotActiveException;
import java.net.URI;
import java.nio.file.NotLinkException;
import java.util.UUID;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Patterns.*;
import static javaslang.Predicates.instanceOf;

public class LiveValues {

    public static void options() {
        Option<String> option0 =  Option.of("Hello");
        Option<String> option1 = Option.none();
        Option<String> option2 = Option.some("Hello");
        Console.log(Option.of(null).equals(Option.none())); // true

        String result = Option.of(null)
                .map(Object::toString)
                .map(String::toLowerCase)
                .getOrElse(() -> "Default value"); // Default value

        option0.toJavaList(); // java.util.List()
        option1.toList();     // javaslang.collection.List("Hello")
    }

    @Run @Important
    public static void tryValue() {
        // TODO
    }

    public static void tryValueSolution() {

        Try.success("hello");
        Try.failure(new RuntimeException("..."));

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url("http://www.google.fr").build();
        String body = Try.of(() -> client.newCall(req).execute())
                .mapTry(res -> res.body().string())
                .getOrElseGet(err -> err.getMessage());

        Console.log(body);

        Console.log(
            Try.of(() -> new URI("not an URI"))
                .map(URI::getScheme)
                .getOrElse(() -> "unknown scheme")
        );
    }

    @Run
    public static void lazy() {
        // TODO
    }

    public static void lazySolution() {
        Lazy<List<UUID>> uuids = Lazy.of(() ->
             List.rangeClosed(1, 100000).map(none -> UUID.randomUUID())
        );

        uuids.get().mkString("\n");
    }

    @Run @Important
    public static void patternMatching() {
        // TODO
    }

    public static void patternMatchingSolution() {
        Try<String> scheme = Try.of(() -> new URI("notanuri")).map(URI::getScheme);
        Integer i = 3;
        Console.log(Match(i).of(
                Case(v -> v > 2, "plus grand que deux"),
                Case(v -> v < 2, "plus petit que 2"),
                Case($(), "what ???")
        ));
        Object value = "hello";
        Match(value).of(
                Case(instanceOf(String.class), str -> str),
                Case(instanceOf(Integer.class), integ -> integ.toString()),
                Case($(), integ -> "--")
        );
        Match(scheme).of(
                Case(Success($()), sch -> Console.log("Scheme is :", sch)),
                Case(Failure($()), err -> Console.log("Unknown scheme :("))
        );
        Match(Option.of(null)).of(
                Case(Some($()), v -> Console.log("Option value is :", value)),
                Case(None(), err -> Console.log("not value"))
        );
        Try.of(() -> "")
                .recover(x -> Match(x).of(
                        Case(instanceOf(NotActiveException.class), "NotActiveException"),
                        Case(instanceOf(NotLinkException.class), "NotLinkException"),
                        Case($(), "Exception")
                ));
    }
}
