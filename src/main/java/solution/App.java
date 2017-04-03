package solution;

import models.Wine;
import org.reactivecouchbase.json.Throwables;
import utils.CSV;
import utils.Done;
import utils.Run;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class App {

    public static Map<String, Wine> wines = new HashMap<String, Wine>() {{
        CSV.readCsvJava("wines.csv").forEach(parts -> {
            put(parts.get(0), new Wine(parts.get(0), parts.get(1), parts.get(2), parts.get(3), parts.get(4)));
        });
    }};

    public static Map<String, List<String>> winesByCountry = new HashMap<String, List<String>>() {{
        List<String> filenames = IntStream.rangeClosed(1, 11).boxed().map(idx -> "country-" + idx + ".csv").collect(Collectors.toList());
        filenames.forEach(filename -> {
            CSV.readCsvJava(filename).forEach(parts -> {
                String id = parts.get(0);
                String country = parts.get(1);
                if (!containsKey(country)) {
                    put(country, new ArrayList<>());
                }
                get(country).add(id);
            });
        });
    }};

    public static Set<String> countries = winesByCountry.keySet();

    // @Run
    public static void demo() {

        Set<String> countries2 = new HashSet<>(countries);
        countries2.add("lmkjmlkj");
        countries2.add("lkjm");

        String res = countries2
            .stream()
            .map(country -> Optional.ofNullable(winesByCountry.get(country)))
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get())
            .map(wineIds -> {
                return wineIds
                    .stream()
                    .limit(2)
                    .map(id -> Optional.ofNullable(wines.get(id)))
                    .filter(opt -> opt.isPresent())
                    .map(opt -> opt.get())
                    .map(wine -> wine.toString())
                    .collect(Collectors.joining(", "));
            })
            .collect(Collectors.joining("\n"));

        System.out.println(res);
    }

    public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        try {
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[futures.size()])).thenApply(vd -> {
                return futures
                    .stream()
                    .map(cf -> {
                        try {
                            return cf.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // @Run
    public static void display10Wines() {
        try {
            List<CompletableFuture<Optional<Wine>>> fwines = Wine.javaIds.stream().limit(10).map(Wine::javaFindById).collect(Collectors.toList());
            CompletableFuture.allOf(fwines.toArray(new CompletableFuture<?>[fwines.size()])).thenApply(aVoid -> {
                String winesStr = fwines
                    .stream()
                    .map(fu -> {
                        try {
                            return fu.get();
                        } catch (Exception e) {
                            throw Throwables.propagate(e);
                        }
                    })
                    .filter(opt -> opt.isPresent())
                    .map(opt -> opt.get())
                    .map(wine -> wine.toString())
                    .collect(Collectors.joining("\n"));
                System.out.println(winesStr);
                return Done.Instance;
            }).get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
