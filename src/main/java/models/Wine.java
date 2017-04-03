package models;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import javaslang.Function1;
import javaslang.Lazy;
import javaslang.Tuple;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Seq;
import javaslang.concurrent.Future;
import javaslang.control.Option;
import javaslang.control.Try;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.json.mapping.JsResult;
import utils.CSV;
import utils.ES;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Patterns.Failure;
import static javaslang.Patterns.Success;

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

    public static Wine random() {
        return localFindById(javaslangIds.head()).get();
    }

    public static JsResult<Wine> fromJson(JsValue value) {
        return Try.of(() ->
            JsResult.success(
                new Wine(
                    value.field("id").asOptString().getOrElse("--"),
                    value.field("name").asOptString().getOrElse("--"),
                    value.field("year").asOptString().getOrElse("--"),
                    value.field("color").asOptString().getOrElse("--"),
                    value.field("region").asOptString().getOrElse("--"),
                    value.field("country").asOptString().getOrElse("--"),
                    value.field("externalPhotoUrl").asOptString().getOrElse("--")
                )
            )
        ).getOrElseGet(e -> JsResult.error(e));
    }

    public static Future<Seq<String>> countries() {
        return ES.search("/wines/_search", Json.obj()
            .with("size", 0)
            .with("aggs", Json.obj()
                .with("countries", Json.obj()
                    .with("terms", Json.obj()
                        .with("field", "country.keyword")
                        .with("size", 30)
                        .with("order", Json.obj()
                            .with("_count", "desc")
                        )
                    )
                )
            )
        ).map(res -> Try.of(() -> res.body().string())
            .map(body -> Json.parse(body))
            .map(body -> body.field("aggregations").field("countries").field("buckets").asArray())
            .map(arr  -> arr.values.map(jsv -> jsv.field("key").asString()))
            .getOrElse(List.empty())
        );
    }

    public static Future<Seq<Wine>> findByCountry(String country) {
        return ES.search("/wines/_search", Json.obj()
            .with("size", 200)
            .with("query", Json.obj()
                .with("bool", Json.obj()
                    .with("must", Json.arr(
                        Json.obj().with("match", Json.obj()
                            .with("country.keyword", country)
                        )
                    ))
                )
            )
        ).map(res -> Try.of(() -> res.body().string())
            .map(body -> Json.parse(body))
            .map(body -> body.field("hits").field("hits").asArray())
            .map(arr  -> arr.values.map(jsv -> fromJson(jsv.field("_source").asObject().add(Json.obj().with("id", jsv.field("_id").asString()))).asOpt()).flatMap(Function.identity()))
            .getOrElse(List.empty())
        );
    }

    public static Future<Option<Wine>> findById(String id) {
        return ES.fetch("/wines/wine/" + id).map(res -> {
            if (res.code() == 200) {
                return Try.of(() -> res.body().string())
                    .map(body -> Json.parse(body))
                    .map(body -> body.field("_source").asObject().add(Json.obj().with("id", body.field("_id").asString())))
                    .map(body -> fromJson(body).asOpt())
                    .getOrElse(Option.none());
            } else {
                return Option.none();
            }
        });
    }

    public static CompletableFuture<Optional<Wine>> javaFindById(String id) {
        CompletableFuture<Optional<Wine>> cf = new CompletableFuture<>();
        findById(id).andThen(t -> Match(t).of(
            Case(Success($()), response -> cf.complete(response.toJavaOptional())),
            Case(Failure($()), err -> cf.completeExceptionally(err))
        ));
        return cf;
    }

    public static void generateCSV() {
        AtomicInteger counter = new AtomicInteger(0);
        countries().flatMap(countries -> {
            return Future.sequence(countries.map(country -> {
                Integer idx = counter.incrementAndGet();
                return findByCountry(country).map(wines -> {
                    return wines.map(wine -> {
                        return Try.of(() -> {
                            String line = List.of(wine.id, wine.name, wine.region, wine.year, wine.color).mkString(";") + "\n";
                            String countryLine = List.of(wine.id, wine.country).mkString(";") + "\n";
                            Files.append(line, new File("src/main/resources/wines.csv"), Charsets.UTF_8);
                            Files.append(countryLine, new File("src/main/resources/country-" + idx + ".csv"), Charsets.UTF_8);
                            return null;
                        }).getOrElseGet(err -> {
                            err.printStackTrace();
                            return null;
                        });
                    });
                });
            }));
        }).await();
    }

    public static final List<String> javaslangIds = List.of(
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
        "wineries-armand-rousseau-wines-chambertin-grand-cru-2002",
        "wineries-champagne-philipponnat-wines-le-clos-des-goisses-champagne-brut-2001",
        "wineries-armand-rousseau-wines-pre-et-fils-chambertin-2006",
        "wineries-dom-perignon-wines-rose-champagne-brut-vintage-1990",
        "wineries-armand-rousseau-wines-1779113-chambertin-grand-cru-2000",
        "wineries-domaine-laurent-perrier-wines-grand-sicle-grande-cuve-champagne-1990",
        "wineries-suduiraut-wines-sauternes-1er-cru-class-1985",
        "wineries-joseph-drouhin-wines-marquis-de-laguiche-montrachet-grand-cru-2007",
        "wineries-fr-domaine-tempier-wines-la-tourtine-cuve-spciale-bandol-2013",
        "wineries-champagne-gosset-wines-champagne-brut-15-ans-8888",
        "wineries-charles-heidsieck-wines-blanc-des-millnaires-millesim-8888",
        "wineries-ausone-wines-saint-emilion-1er-grand-cru-class-2001",
        "wineries-fr-chateau-latour-wines-grand-vin-de-premier-cru-classe-pauillac-1955",
        "wineries-chateau-lafleur-wines-1518607-pomerol-1993",
        "wineries-e-guigal-wines-cte-rtie-la-landonne-1997",
        "wineries-e-guigal-wines-cte-rtie-la-turoue-1999",
        "wineries-leoville-las-cases-wines-grand-vin-de-loville-marquis-de-las-cases-saint-julien-2009",
        "wineries-domaine-de-la-janasse-wines-chateauneuf-du-pape-vieilles-vignes-2007",
        "wineries-champagne-krug-wines-champagne-grande-cuve-brut-2002",
        "wineries-domaine-jean-louis-chave-wines-hermitage-2007",
        "wineries-ausone-wines-saint-emilion-1er-grand-cru-class-2002",
        "wineries-chateau-d-yquem-wines-y-bordeaux-2011",
        "wineries-fr-perrier-jouet-wines-champagne-sparkling-rose-belle-epoque-2002",
        "wineries-e-guigal-wines-la-mouline-cte-rtie-2006",
        "wineries-chateau-trotanoy-wines-pomerol-1982",
        "wineries-chateau-de-beaucastel-wines-chteauneuf-du-pape-vieilles-vignes-2013",
        "wineries-e-guigal-wines-cte-rtie-la-landonne-2004",
        "wineries-domaine-leflaive-wines-les-pucelles-puligny-montrachet-1er-cru-2005",
        "wineries-e-guigal-wines-cte-rtie-la-turoue-2009",
        "wineries-haut-brion-wines-graves-premier-grand-cru-class-1975",
        "wineries-clos-leglise-wines-pomerol-2005",
        "wineries-chateau-rabaud-promis-wines-premier-cru-classe-sauternes-2006",
        "wineries-champagne-bruno-paillard-wines-champagne-nec-plus-ultra-1996",
        "wineries-m-chapoutier-wines-le-pavillon-ermitage-2003",
        "wineries-m-chapoutier-wines-m-chapoutier-de-loree-2004",
        "wineries-domaine-leflaive-wines-batard-montrachet-grand-cru-2013",
        "wineries-domaine-leflaive-wines-les-combettes-puligny-montrachet-2010",
        "wineries-chateau-palmer-wines-xix-century-2007",
        "wineries-m-chapoutier-wines-m-chapoutier-le-pavillon-2009",
        "wineries-m-chapoutier-wines-m-chapoutier-de-loree-2007",
        "wineries-chateau-la-negly-wines-la-porte-du-ciel-coteaux-du-languedoc-2012",
        "wineries-leoville-las-cases-wines-saint-julien-1985",
        "wineries-clos-saint-jean-wines-deus-ex-machina-chteauneuf-du-pape-2011",
        "wineries-domaine-du-chateau-de-puligny-montrachet-wines-chassagne-montrachet-2013",
        "wineries-domaine-comte-georges-de-vogue-wines-domaine-comte-georges-de-vogue-cuve-vieilles-vigne-musigny-grand-cru-2005",
        "wineries-fr-chateau-haut-bages-liberal-wines-pauillac-grand-cru-class-1989",
        "wineries-gracia-wines-saint-milion-grand-cru-2010",
        "wineries-fr-louis-jadot-wines-btard-montrachet-2012",
        "wineries-groffier-pere-et-fils-wines-7068021-les-amoureuses-chambolle-musigny-1er-cru-2012",
        "wineries-m-chapoutier-wines-lermite-red-2007",
        "wineries-chateau-pavie-macquin-wines-saint-milion-premier-grand-cru-class-2000",
        "wineries-chateau-lafite-rothschild-wines-pauillac-lafite-rothschild-1970",
        "wineries-chateau-angelus-wines-saint-emilion-grand-cru-class-2002",
        "wineries-chateau-de-beaucastel-wines-chteauneuf-du-pape-rouge-1990",
        "wineries-chateau-troplong-mondot-wines-saint-emilion-grand-cru-class-2009",
        "wineries-roses-de-jeanne-wines-avril-champagne-lieu-dit-la-boloree-millesime-8888",
        "wineries-mommessin-wines-clos-de-tart-grand-cru-8888",
        "wineries-chateau-de-beaucastel-wines-chteauneuf-du-pape-rouge-2007",
        "wineries-leoville-barton-wines-loville-barton-saint-julien-2000",
        "wineries-fr-chateau-leoville-poyferre-wines-saint-julien-2003-1",
        "wineries-champagne-billecart-salmon-wines-cuvee-nicolas-francois-billecart-champagne-brut-1999",
        "wineries-chateau-guiraud-wines-sauternes-1er-grand-cru-class-2009",
        "wineries-cos-d-estournel-wines-saint-stephe-rouge-1989",
        "wineries-clos-fourtet-wines-saint-milion-1er-grand-cru-class-2003",
        "wineries-chateau-malescot-st-exupery-wines-1304870-margaux-2001",
        "wineries-chateau-de-beaucastel-wines-chteauneuf-du-pape-vieilles-vignes-2008",
        "wineries-chateau-latour-a-pomerol-wines-latour--pomerol-1998",
        "wineries-champagne-ruinart-wines-champagne-reims-brut-2006",
        "wineries-tertre-roteboeuf-wines-saint-milion-grand-cru-2008",
        "wineries-clos-saint-jean-wines-la-combe-des-fous-chteauneuf-du-pape-2011",
        "wineries-le-dome-wines-chteau-le-dome-saint-milion-grand-cru-2011",
        "wineries-e-guigal-wines-cte-rtie-la-turoue-1997",
        "wineries-gruaud-larose-wines-chateau-gruaud-larose-1989",
        "wineries-chateau-talbot-wines-saint-julien-grand-cru-class-1986",
        "wineries-montrose-115806-wines-jl-charmole-grand-cru-class-saint-estphe-2010",
        "wineries-mouton-rothschild-wines-3566768-pauillac-1991",
        "wineries-chateau-troplong-mondot-wines-saint-emilion-grand-cru-class-2000",
        "wineries-pol-roger-wines-epernay-champagne-brut-reserve-2002",
        "wineries-la-mission-haut-brion-wines-pessac-lognan-cru-class-des-graves-2003",
        "wineries-montrose-115806-wines-jl-charmole-grand-cru-class-saint-estphe-2009",
        "wineries-chateau-palmer-wines-1131866-chateau-palmer-2008",
        "wineries-chateau-margaux-wines-pavillon-rouge-1996",
        "wineries-chateau-lafite-rothschild-wines-pauillac-lafite-rothschild-2011",
        "wineries-bonneau-du-martray-wines-corton-charlemagne-grand-cru-2011",
        "wineries-pavie-wines-saint-milion-1er-grand-cru-class-1999",
        "wineries-valandraud-wines-1494467-saint-emilion-grand-cru-2004",
        "wineries-chateau-petit-village-wines-1689148-pomerol-2000",
        "wineries-louis-roederer-wines-120288-cristal-1993",
        "wineries-suduiraut-wines-sauternes-1er-cru-class-1982",
        "wineries-moet-and-chandon-wines-grand-vintage-brut-ros-2000",
        "wineries-domaine-weinbach-wines-cuvee-ste-catherine-alsace-grand-cru-schlossberg-riesling-2014",
        "wineries-valandraud-wines-saint-emilion-grand-cru-2001",
        "wineries-laville-wines-sauternes-2009",
        "wineries-chateau-angelus-wines-saint-emilion-grand-cru-class-1993",
        "wineries-chateau-l-evangile-wines-pomerol-2003",
        "wineries-chateau-lafite-rothschild-wines-dbr-lafite-carruades-de-lafite-2000",
        "wineries-chateau-angelus-wines-saint-emilion-grand-cru-class-1995",
        "wineries-leoville-las-cases-wines-chateau-leoville-las-cases-1989",
        "wineries-chateau-guiraud-wines-2936590-sauternes-1er-grand-cru-class-2011",
        "wineries-champagne-duval-leroy-wines-femme-de-champagne-2000",
        "wineries-gros-nore-wines-bandol-rouge-2014",
        "wineries-clos-saint-jean-wines-deus-ex-machina-chteauneuf-du-pape-2009",
        "wineries-domaine-de-la-janasse-wines-chteauneuf-du-pape-chaupin-2010",
        "wineries-champagne-drappier-wines-grande-sendree-champagne-brut-ros-2008",
        "wineries-champagne-bruno-paillard-wines-assemblage-brut-1999",
        "wineries-hubert-paulet-wines-cuve-risleus-brut-rilly-la-montagne-premier-cru-2002",
        "wineries-deiss-marcel-vins-wines-bergheim-altenberg-grand-cru-2008",
        "wineries-domaine-chantal-lescure-wines-les-bertins-pommard-1er-cru-2010",
        "wineries-domaine-weinbach-wines-cuvee-ste-catherine-alsace-grand-cru-schlossberg-riesling-2009",
        "wineries-chateau-hosanna-wines-pomerol-2001",
        "wineries-chateau-lafite-rothschild-wines-pauillac-lafite-rothschild-1971",
        "wineries-tertre-roteboeuf-wines-saint-milion-grand-cru-1995",
        "wineries-domaine-leflaive-wines-les-pucelles-puligny-montrachet-1er-cru-2009",
        "wineries-fr-domaine-georges-vernay-wines-les-chailles-de-lenfer-condrieu-2009",
        "wineries-fombrauge-wines-grand-bordeaux-saint-emilion-grand-cru-2002",
        "wineries-clos-saint-jean-wines-la-combe-des-fous-chteauneuf-du-pape-2012",
        "wineries-e-guigal-wines-ermitage-red-ex-voto-2006",
        "wineries-egly-ouriet-wines-champagne-millsime-brut-grand-cru-2004",
        "wineries-clos-leglise-wines-pomerol-1999",
        "wineries-maison-louis-latour-wines-1292184-corton-charlemagne-grand-cru-2009",
        "wineries-chateau-rieussec-wines-chteau-rieussec-sauternes-1er-grand-cru-class-1996",
        "wineries-gerard-bertrand-wines-le-viala-2013",
        "wineries-domaine-rossignol-trapet-wines-chambertin-grand-cru-2011",
        "wineries-joseph-drouhin-wines-2350266-charmes-chambertin-grand-cru-2008",
        "wineries-valandraud-wines-virginie-de-valandraud-saint-emilion-grand-cru-1996",
        "wineries-romanee-conti-wines-2893849-richebourg-2012",
        "wineries-chateau-margaux-wines-1225996-premier-grand-cru-class-2000",
        "wineries-cheval-blanc-wines-st-emilion-grand-cru-2000",
        "wineries-chateau-d-yquem-wines-1750566-sauternes-2007",
        "wineries-romanee-conti-wines-romanee-st-vivant-2009",
        "wineries-petrus-wines-pomerol-2002-1",
        "wineries-gruaud-larose-wines-chateau-gruaud-larose-1982",
        "wineries-chateau-margaux-wines-premier-grand-cru-classe-1994",
        "wineries-petrus-wines-pomerol-2001",
        "wineries-petrus-wines-pomerol-2007-1",
        "wineries-chateau-margaux-wines-1374776-premier-grand-cru-class-2003",
        "wineries-chateau-d-yquem-wines-1487737-sauternes-1983",
        "wineries-chateau-lafite-rothschild-wines-lafite-rothschild-2005",
        "wineries-domaine-grand-veneur-wines-domaine-grand-veneur-vieilles-vignes-chteauneuf-du-pape-2009",
        "wineries-fontaine-gagnard-wines-criots-btard-montrachet-grand-cru-2011",
        "wineries-delas-freres-wines-les-bessards-hermitage-2011",
        "wineries-raveneau-wines-chablis-premier-cru-butteaux-2013",
        "wineries-domaine-comte-georges-de-vogue-wines-domaine-comte-georges-de-vogue-cuve-vieilles-vignes-musigny-grand-cru-1993",
        "wineries-chateau-l-eglise-clinet-wines-pomerol-2009",
        "wineries-m-chapoutier-wines-lermite-red-2011",
        "wineries-le-pin-wines-2141481-pomerol-2007",
        "wineries-mouton-rothschild-wines-chateaux-pauillac-2000",
        "wineries-chateau-margaux-wines-1238211-premier-grand-cru-class-1996",
        "wineries-chateau-lafite-rothschild-wines-lafite-rothschild-1986",
        "wineries-petrus-wines-pomerol-1990-1",
        "wineries-fr-chateau-latour-wines-grand-vin-de-premier-cru-classe-pauillac-1985",
        "wineries-fr-chateau-latour-wines-grand-vin-de-premier-cru-classe-pauillac-1989",
        "wineries-haut-brion-wines-graves-premier-grand-cru-class-1998",
        "wineries-fr-chateau-latour-wines-grand-vin-de-premier-cru-classe-pauillac-1994",
        "wineries-champagne-krug-wines-champagne-vintage-reims-brut-2000",
        "wineries-pol-roger-wines-2664609-champagne-cuve-sir-winston-churchill-1999",
        "wineries-lynch-bages-wines-grand-cru-class-pauillac-1989",
        "wineries-de-sousa-and-fils-wines-cuve-des-caudalies-millesim-champagne-brut-grand-cru-2006",
        "wineries-valandraud-wines-saint-emilion-grand-cru-2009",
        "wineries-champagne-krug-wines-krug-clos-du-mesnil-1995",
        "wineries-e-guigal-wines-la-mouline-cte-rtie-2011",
        "wineries-chateau-d-yquem-wines-1441235-sauternes-2000",
        "wineries-chateau-l-evangile-wines-pomerol-1990",
        "wineries-tertre-roteboeuf-wines-saint-milion-grand-cru-2006",
        "wineries-chateau-lafite-rothschild-wines-pauillac-lafite-rothschild-1983",
        "wineries-chateau-rayas-wines-chteauneuf-du-pape-2001-1",
        "wineries-chateau-angelus-wines-angelus-cabernet-franc-2003",
        "wineries-bollinger-wines-champagne-rd-extra-brut-1996",
        "wineries-chateau-lafite-rothschild-wines-lafite-rothschild-2006",
        "wineries-champagne-ruinart-wines-dom-brut-champagne-1998",
        "wineries-chateau-d-yquem-wines-2631066-sauternes-2011",
        "wineries-henri-boillot-wines-corton-charlemagne-grand-cru-2012",
        "wineries-chateau-hosanna-wines-pomerol-2009",
        "wineries-domaine-du-vicomte-liger-belair-wines-la-colombire-vosne-romane-2008",
        "wineries-champagne-henriot-wines-brut-souverain-2000",
        "wineries-fr-perrier-jouet-wines-belle-epoque-champagne-brut-1990",
        "wineries-olivier-leflaive-wines-3216756-montrachet-grand-cru-2011",
        "wineries-henri-bonneau-wines-rserve-des-clestins-chteauneuf-du-pape-2009",
        "wineries-champagne-krug-wines-clos-du-mesnil-brut-blanc-de-blancs-2003",
        "wineries-romanee-conti-wines-la-tche-2002",
        "wineries-mouton-rothschild-wines-6048054-pauillac-1959",
        "wineries-romanee-conti-wines-domaine-de-la-romanee-conti-grands-echezeau-2005",
        "wineries-pavie-wines-saint-milion-1er-grand-cru-class-2010",
        "wineries-bollinger-wines-champagne-rd-extra-brut-1997",
        "wineries-pavie-decesse-wines-chteau-pavie-decesse-saint-emillion-grand-cru-classe-1990",
        "wineries-romanee-conti-wines-1486114-richebourg-1999",
        "wineries-egly-ouriet-wines-champagne-millsime-brut-grand-cru-2005",
        "wineries-fr-domaine-trapet-wines-chambertin-grand-cru-1996"
    );

    public static final java.util.List<String> javaIds = javaslangIds.toJavaList();

    public static Lazy<Map<String, Wine>> _wines = Lazy.of(() -> {
        return HashMap.ofEntries(CSV.readCsvJavaslang("wines.csv").map(parts ->
            Tuple.of(parts.apply(0), new Wine(parts.apply(0), parts.apply(1), parts.apply(2), parts.apply(3), parts.apply(4)))));
    });

    public static Option<Wine> localFindById(String id) {
        return _wines.get().get(id);
    }

    public static Optional<Wine> javaLocalFindById(String id) {
        return _wines.get().get(id).toJavaOptional();
    }

}
