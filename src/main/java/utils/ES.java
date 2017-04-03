package utils;

import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Try;
import okhttp3.*;
import org.reactivecouchbase.json.JsValue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import static javaslang.API.*;
import static javaslang.Patterns.*;

public class ES {

    private static final String url = "https://5cbc9812b19caf87d4a758ac3cb882ed.eu-west-1.aws.found.io:9243";
    private static final String login = "elastic";
    private static final String password = "MbN8NoUXjt053uzeYhBITSbq";

    private static final OkHttpClient client = new OkHttpClient();

    public static String esUrl(String path) {
        return url + path;
    }

    public static Future<Response> search(String path, JsValue query) {
        // System.out.println(query.pretty());
        Promise<Response> promise = Promise.make();
        Request request = new Request.Builder()
            .url(url + path)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((login + ":" + password).getBytes(Charset.forName("UTF-8"))))
            .post(RequestBody.create(MediaType.parse("application/json"), query.stringify()))
            .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                promise.tryFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Try.of(() -> {
                    //System.out.println(response.body().string());
                    return null;
                });
                promise.trySuccess(response);
            }
        });
        return promise.future();
    }

    public static Future<Response> fetch(String path) {
        // System.out.println("fetch " + url + path);
        Promise<Response> promise = Promise.make();
        Request request = new Request.Builder()
                .url(url + path)
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + Base64.getUrlEncoder().encodeToString((login + ":" + password).getBytes(Charset.forName("UTF-8"))))
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                promise.tryFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Try.of(() -> {
                    // System.out.println(response.body().string());
                    return null;
                });
                promise.trySuccess(response);
            }
        });
        return promise.future();
    }

    public static CompletableFuture<Response> javaFetch(String path) {
        CompletableFuture<Response> completableFuture = new CompletableFuture<>();
        fetch(path).andThen(t -> Match(t).of(
           Case(Success($()), response -> completableFuture.complete(response)),
           Case(Failure($()), err -> completableFuture.completeExceptionally(err))
        ));
        return completableFuture;
    }
}
