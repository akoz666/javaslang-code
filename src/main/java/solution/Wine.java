package solution;

import javaslang.Lazy;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Either;
import javaslang.control.Option;
import javaslang.control.Try;
import okhttp3.*;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import utils.CSV;
import utils.ES;

import java.io.IOException;

import static javaslang.API.*;
import static javaslang.Patterns.*;

@SuppressWarnings(value = { "Duplicates", "unchecked" })
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
    public static List<String> testIds = List.of(
            "wineries-ausone-wines-saint-emilion-1er-grand-cru-class-1995",
            "wineries-henri-boillot-wines-clos-de-la-mouchre-monopole-puligny-montrachet-1er-cru-2012",
            "wineries-chateau-magdelaine-wines-saint-milion-1er-grand-cru-class-2010",
            "wineries-deutz-wines-amour-de-deutz-champagne-brut-millesimi-2002",
            "wineries-suduiraut-wines-sauternes-1er-cru-class-2001",
            "wineries-la-mission-haut-brion-wines-pessac-leognan-cru-class-des-graves-1999",
            "wineries-chateau-clinet-wines-1196580-pomerol-2005",
            "wineries-paul-jaboulet-aine-1387-wines-la-chapelle-hermitage-1990",
            "wineries-tertre-roteboeuf-wines-saint-milion-grand-cru-2005",
            "wineries-e-guigal-wines-cte-rtie-la-landonne-2010",
            "wineries-haut-brion-wines-graves-premier-grand-cru-class-1959",
            "wineries-armand-rousseau-wines-chambertin-grand-cru-2002"
    );

    private static Lazy<Map<String, Wine>> localWines = Lazy.of(() ->
            HashMap.ofEntries(
                    CSV.readCsvJavaslang("wines.csv")
                            .map(parts -> Tuple.of(parts.head(), new Wine(parts.get(0), parts.get(1), parts.get(2), parts.get(3), parts.get(4))))
            )
    );

    public static Either<Throwable, Wine> fromJson(JsValue value) {
        return Try.of(() -> Either.<Throwable, Wine>right(new Wine(
                value.field("id").asOptString().getOrElse("--"),
                value.field("name").asOptString().getOrElse("--"),
                value.field("year").asOptString().getOrElse("--"),
                value.field("color").asOptString().getOrElse("--"),
                value.field("region").asOptString().getOrElse("--"),
                value.field("country").asOptString().getOrElse("--"),
                value.field("externalPhotoUrl").asOptString().getOrElse("--")
        ))).getOrElseGet(err -> Either.left(err));
    }

    public static Option<Wine> localFindById(String id) {
        return localWines.get().get(id);
    }

    public static List<Wine> localFindAllById(List<String> ids) {
        return ids.map(id -> localFindById(id)).flatMap(o -> o);
    }

    public static void displayLocalTestWines() {
        System.out.println(
                testIds
                        .remove("wineries-ausone-wines-saint-emilion-1er-grand-cru-class-1995")
                        .append("aa")
                        .append("bb")
                        .append("cc")
                        .map(id -> localFindById(id))
                        .flatMap(o -> o)
                        .mkString("\n")
        );
    }

    public static Future<Response> httpGet(String url) {
        Promise<Response> promise = Promise.make();
        okHttpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                promise.tryFailure(e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                promise.trySuccess(response);
            }
        });
        return promise.future();
    }

    public static Future<Option<Wine>> findById(String id) {
        return httpGet(ES.esUrl("/wines/wine/" + id))
                .filter(r -> r.code() == 200)
                .map(response ->
                        Try.of(() -> response.body().string())
                                .mapTry(body -> Json.parse(body))
                                .mapTry(body -> body.field("_source").asObject().add(Json.obj().with("id", body.field("_id").asString())))
                                .mapTry(body -> fromJson(body))
                                .map(either -> Match(either).of(
                                        Case(Left($()), err -> Option.<Wine>none()),
                                        Case(Right($()), value -> Option.some(value))
                                ))
                ).map(ttry -> Match(ttry).of(
                        Case(Success($()), value -> value),
                        Case(Failure($()), err -> Option.none())
                ));
    }

    public static Future<List<Wine>> findAllById(List<String> ids) {
        return Future.sequence(ids.map(id -> findById(id))).map(futures -> futures.flatMap(o -> o).toList());
    }
}
