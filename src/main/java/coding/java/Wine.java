package coding.java;

import okhttp3.*;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.json.mapping.JsResult;
import utils.CSV;
import utils.ES;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("Duplicates")
public class Wine {

    public final String id;
    public final String name;
    public final String year;
    public final String color;
    public final String region;
    public final String country;
    public final String externalPhotoUrl;

    public Wine(String id, String name, String year, String color, String region, String country, String externalPhotoUrl) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.color = color;
        this.region = region;
        this.country = country;
        this.externalPhotoUrl = externalPhotoUrl;
    }

    public Wine(String id, String name, String region, String year, String color) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.color = color;
        this.region = region;
        this.country = "--";
        this.externalPhotoUrl = "--";
    }

    public String toString() {
        return "Wine " + toJson().pretty();
    }

    public JsValue toJson() {
        return Json.obj()
            .with("id", this.id)
            .with("name", this.name)
            .with("year", this.year)
            .with("color", this.color)
            .with("region", this.region)
            .with("country", this.country)
            .with("externalPhotoUrl", this.externalPhotoUrl);
    }

    private static OkHttpClient okHttpClient = new OkHttpClient();

    // collection utils List.of
    public static List<String> testIds = new ArrayList<String>() {{
        add("wineries-ausone-wines-saint-emilion-1er-grand-cru-class-1995");
        add("wineries-henri-boillot-wines-clos-de-la-mouchre-monopole-puligny-montrachet-1er-cru-2012");
        add("wineries-chateau-magdelaine-wines-saint-milion-1er-grand-cru-class-2010");
        add("wineries-deutz-wines-amour-de-deutz-champagne-brut-millesimi-2002");
        add("wineries-suduiraut-wines-sauternes-1er-cru-class-2001");
        add("wineries-la-mission-haut-brion-wines-pessac-leognan-cru-class-des-graves-1999");
        add("wineries-chateau-clinet-wines-1196580-pomerol-2005");
        add("wineries-paul-jaboulet-aine-1387-wines-la-chapelle-hermitage-1990");
        add("wineries-tertre-roteboeuf-wines-saint-milion-grand-cru-2005");
        add("wineries-e-guigal-wines-cte-rtie-la-landonne-2010");
        add("wineries-haut-brion-wines-graves-premier-grand-cru-class-1959");
        add("wineries-armand-rousseau-wines-chambertin-grand-cru-2002");
    }};

    // Lazy + collection utils List.of
    private static List<String> localIds = new ArrayList<String>() {{
        addAll(
                CSV.readCsvJava("wines.csv")
                        .stream()
                        .map(parts -> parts.get(0))
                        .collect(Collectors.toList())
        );
    }};

    // Lazy + collection utils Map.ofEntries + tuples
    private static Map<String, Wine> wines = new HashMap<String, Wine>() {{
        for (Wine wine : CSV.readCsvJava("wines.csv")
                .stream()
                .map(parts -> new Wine(parts.get(0), parts.get(1), parts.get(2), parts.get(3), parts.get(4)))
                .collect(Collectors.toList())) {
            put(wine.id, wine);
        }
    }};

    // Lazy + collection utils Map.ofEntries + List.rangeClosed
    private static Map<String, List<String>> winesByCountry = new HashMap<String, List<String>>() {{
        IntStream.rangeClosed(1, 11).mapToObj(i -> "country-" + i + ".csv").forEach(filename -> {
            List<List<String>> lines = CSV.readCsvJava(filename);
            List<String> ids = lines.stream().map(parts -> {
                return parts.get(1);
            }).collect(Collectors.toList());
            put(lines.get(0).get(1), ids);
        });
    }};

    public static JsResult<Wine> fromJson(JsValue value) {
        try {
            return JsResult.success(
                new Wine(
                    value.field("id").asOptString().getOrElse("--"),
                    value.field("name").asOptString().getOrElse("--"),
                    value.field("year").asOptString().getOrElse("--"),
                    value.field("color").asOptString().getOrElse("--"),
                    value.field("region").asOptString().getOrElse("--"),
                    value.field("country").asOptString().getOrElse("--"),
                    value.field("externalPhotoUrl").asOptString().getOrElse("--")
                )
            );
        } catch (Exception e) {
            return JsResult.error(e);
        }
    }

    // use option from map
    public static Optional<Wine> localFindById(String id) {
        return Optional.ofNullable(wines.get(id));
    }

    public static List<Wine> localFindAllById(List<String> ids) {
        // list.stream().map(...).collect(Collectors.toList())
        // option flatmap
        return ids
            .stream()
            .map(id -> localFindById(id))
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get())
            .collect(Collectors.toList());
    }

    public static CompletableFuture<Optional<Wine>> findById(String id) {
        return httpGet(ES.esUrl("/wines/wine/" + id)).thenCompose(response -> {
            // filter
            if (response.code() == 200) {
                return CompletableFuture.completedFuture(response);
            } else {
                // future.failed
                CompletableFuture<Response> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("bad status"));
                return failed;
            }
        }).thenApply(response -> {
            // Try
            // Pattern matching
            try {
                String respBody = response.body().string();
                JsValue body = Json.parse(respBody);
                JsValue jsonWine = body.field("_source").asObject().add(Json.obj().with("id", body.field("_id").asString()));
                return Optional.ofNullable(fromJson(jsonWine).getValueOrNull());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static CompletableFuture<List<Wine>> findAllById(List<String> ids) {
        // Future.sequence
        List<CompletableFuture<Optional<Wine>>> futureWines = ids.stream().map(id -> findById(id)).collect(Collectors.toList());
        return CompletableFuture.allOf(futureWines.toArray(new CompletableFuture<?>[futureWines.size()])).thenApply(empty -> {
            return futureWines
                .stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .collect(Collectors.toList());
        });
    }

    public static CompletableFuture<Response> httpGet(String url) {
        // Promise
        CompletableFuture<Response> future = new CompletableFuture<>();
        okHttpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                future.complete(response);
            }
        });
        return future;
    }

    public static void displayLocalTestWines() {
        List<String> ids = new ArrayList<>(testIds);
        ids.remove("wineries-ausone-wines-saint-emilion-1er-grand-cru-class-1995");
        ids.add("aa");
        ids.add("bb");
        ids.add("cc");
        System.out.println(
            testIds
                .stream()
                .map(id -> localFindById(id))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .map(wine -> wine.toString())
                .collect(Collectors.joining("\n"))
        );
    }
}
