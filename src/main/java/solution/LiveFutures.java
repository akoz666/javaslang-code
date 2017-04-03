package solution;

import javaslang.Function1;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Option;
import okhttp3.*;
import utils.Console;
import utils.Important;
import utils.Run;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Patterns.Failure;
import static javaslang.Patterns.Success;

public class LiveFutures {

    public static void future() {
        Future.successful("Hello");  // Future(Success(Hello))
        Future.failed(new RuntimeException("...")); // Future(Failure(RuntimeException()))
        Future.of(() -> {
            Thread.sleep(1000); // YOLO
            return "hello";
        });
        Future.of(Executors.newCachedThreadPool(), () -> {
            Thread.sleep(1000); // YOLO
            return "hello";
        });

        // @formatter:off
        Future.successful("Hello")
            .map(hlo -> hlo.toUpperCase())
            .flatMap(hlo -> {
                return Future.successful("World").map(wld -> hlo + " " + wld);
            }).flatMap(hlwld -> {
                return Future.successful("!").map(dot -> hlwld + " " + dot);
            }).onComplete(ttry -> Match(ttry).of(
                Case(Success($()), value -> Console.log(value)),
                Case(Failure($()), err -> Console.log("got error", err.getMessage()))
        )   ).await();
        // @formatter:on
    }

    public static void promise() {
        Promise<String> p = Promise.make();
        OkHttpClient client = new OkHttpClient();
        client.newCall(new Request.Builder().url("http://www.google.fr").get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                p.tryFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                p.trySuccess(response.body().string());
            }
        });
        p.future();
    }

    @Run @Important
    public static void futureAll() {
        // TODO
    }

    public static void futureAllSolution() {
        Map<String, String> values = HashMap.ofEntries(
                Tuple.of("id1", "value1"),
                Tuple.of("id2", "value2")
        );
        java.util.Map<String, String> javaValues = values.toJavaMap();

        List<String> ids = List.of("id1", "id2", "id3");
        java.util.List<String> javaIds = ids.toJavaList();

        Function1<String, Future<Option<String>>> findById = (id) -> Future.successful(values.get(id));

        Future.sequence(ids.map(id -> findById.apply(id))).map(vals -> vals.flatMap(o -> o)).map(vals -> {
            return Console.log("values are", vals.mkString(", "));
        }).await();
    }

    @Run @Important
    public static void futureAllJava() {
        // TODO
    }

    public static void futureAllJavaSolution() {
        java.util.Map<String, String> values = HashMap.ofEntries(
                Tuple.of("id1", "value1"),
                Tuple.of("id2", "value2")
        ).toJavaMap();

        java.util.List<String> ids = List.of("id1", "id2", "id3").toJavaList();

        Function<String, CompletableFuture<Optional<String>>> findById = (id) -> {
            CompletableFuture<Optional<String>> fu = new CompletableFuture<>();
            fu.complete(Optional.ofNullable(values.get(id)));
            return fu;
        };

        try {
            java.util.List<CompletableFuture<Optional<String>>> futures = ids.stream().map(id -> findById.apply(id)).collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).thenApply(voidd -> {
                String res = futures.stream().map(fu -> {
                    try {
                        return fu.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                        .filter(opt -> opt.isPresent())
                        .map(opt -> opt.get())
                        .map(obj -> obj.toString()) // si pas string
                        .collect(Collectors.joining("\n"));
                return Console.log(res);
            }).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
