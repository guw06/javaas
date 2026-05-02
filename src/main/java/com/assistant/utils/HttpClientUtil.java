package com.assistant.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import java.io.IOException;

public class HttpClientUtil {
    private static final OkHttpClient client = new OkHttpClient();
    public static final Gson gson = new Gson();

    public static String get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("HTTP ошибка: " + response.code() + " для URL: " + url);
                return null;
            }
            if (response.body() != null) {
                return response.body().string();
            }
            return null;
        } catch (IOException e) {
            System.err.println("Ошибка при выполнении запроса к " + url + ": " + e.getMessage());
            return null;
        }
    }
}
