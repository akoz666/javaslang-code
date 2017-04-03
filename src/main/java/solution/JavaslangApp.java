package solution;

import javaslang.Lazy;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Seq;
import javaslang.concurrent.Future;
import javaslang.control.Option;
import javaslang.control.Try;
import models.Wine;
import org.reactivecouchbase.json.Json;
import utils.CSV;
import utils.Done;
import utils.ES;
import utils.Run;

import static javaslang.API.*;
import static javaslang.Predicates.*;
import static javaslang.Patterns.*;

public class JavaslangApp {

    /*
     * Only 3 functional control structures
     * No memoization
     * No lifting
     * No pattern matching
     * No tuples
     * Optional not Serializable/Iterable
     * Lack of Stream/Optional in APIs
     * Checked exceptions in functions
     * "Type pollution"
     * list.stream().map(...).collect(toList())
     * ...
     */

    /**
     |
     | * Function
     |   * Memoization ???
     |   * lifting ???
     |   * curry ???
     | * Tuple => OK
     | * Option => OK
     | * Try : OkHttp => Try.of(...).getOrElseGet(e -> ...) => OK
     | * Lazy : CSV reader ??? => OK
     | * Future : Future.sequence
     | * Collections :-)
     |   * list.stream().map(a -> a).collect(Collectors.toList()) OK
     |   * Stream !!!
     |   * List.flatMap(option -> option) => retour de findById == Option<...> OK
     |   * Range int ;-) Ok
     | * Pattern Matching !!!!
     |   * Try
     |   * Option
     |   * Class
     | * Either ???
     | * Validation ???
     |
     */

    public static Lazy<Map<String, Wine>> _wines = Lazy.of(() -> {
        return HashMap.ofEntries(CSV.readCsvJavaslang("wines.csv").map(parts ->
                Tuple.of(parts.apply(0), new Wine(parts.apply(0), parts.apply(1), parts.apply(2), parts.apply(3), parts.apply(4)))));
    });

    public static Lazy<Map<String, List<String>>> _winesByCountry = Lazy.of(() -> {
        return HashMap.ofEntries(List.rangeClosed(1, 11).map(idx -> "country-" + idx + ".csv").map(filename -> {
            List<Tuple2<String, String>> lines = CSV.readCsvJavaslang(filename).map(parts -> Tuple.of(parts.apply(1), parts.apply(0)));
            return Tuple.of(lines.head()._1, lines.map(i -> i._2));
        }));
    });

    public static Lazy<List<String>> _countries = Lazy.of(() -> _winesByCountry.get().keySet().toList());

    public static Future<Option<Wine>> fetchWine(String id) {
        return ES.fetch("/wines/wine/" + id).map(res -> {
            if (res.code() == 200) {
                return Try.of(() -> res.body().string())
                    .map(body -> Json.parse(body).field("_source"))
                    .map(body -> Wine.fromJson(body.asObject().add(Json.obj().with("id", id))).asOpt())
                    .getOrElse(Option.none());
            } else {
                return Option.none();
            }
        });
    }

    // @Run
    public static void demo() {
        Map<String, Wine> wines = _wines.get();
        Map<String, List<String>> winesByCountry = _winesByCountry.get();
        List<String> countries = _countries.get();

        String res = countries
            .append("lkjqslkj")
            .append("lkjlmkj")
            .map(country -> winesByCountry.get(country))
            .flatMap(o -> o)
            .map(ids -> ids.take(2).map(id -> wines.get(id)).flatMap(o -> o))
            .mkString("\n");

        System.out.println(res);
    }

    // @Run
    public static void demoAsync() {

        Map<String, Wine> wines = _wines.get();
        Map<String, List<String>> winesByCountry = _winesByCountry.get();
        List<String> countries = _countries.get();

        Future.sequence(
            countries
                .append("lkjqslkj")
                .append("lkjlmkj")
                .map(country -> winesByCountry.get(country))
                .flatMap(o -> o)
                .map(ids -> {
                    return Future.sequence(ids.take(3).map(id -> wines.get(id)).flatMap(o -> o).map(wine -> {
                        return fetchWine(wine.id);
                    }))
                    .map(seq -> seq.flatMap(o -> o));
                })
        ).andThen(ttry -> Match(ttry).option(
            Case(Success($()), ssw -> ssw.mkString("\n")),
            Case(Failure($()), err -> err.getMessage())
        ).forEach(System.out::println)).await();
    }

    // @Run
    public static void display10Wines() {
        Future.sequence(Wine.javaslangIds.take(10).map(Wine::findById)).map(wineOpts -> {
            String winesStr = wineOpts.flatMap(o -> o).mkString("\n");
            System.out.println(winesStr);
            return Done.Instance;
        }).await();
    }

    public static void demoOptions() {

    }

    public static void demoTry() {

    }

    public static void demoFuture() {

    }

    public static void demoRange() {

    }

    public static void options() {

    }
}
