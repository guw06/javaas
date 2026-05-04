package com.assistant.commands;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeCommand implements Command {
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("\"videoId\":\"([A-Za-z0-9_-]{11})\"");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build();

    @Override
    public String execute(String input) {
        String query = extractQuery(input);
        if (query.isBlank()) {
            return openUrl("https://www.youtube.com", "Открыла YouTube.", "Не смогла открыть YouTube. Проверь, пожалуйста, браузер.");
        }

        String searchUrl = buildSearchUrl(query);
        if (!isPlayIntent(input)) {
            return openUrl(searchUrl, "Открыла YouTube с поиском «" + query + "».", "Не смогла открыть YouTube. Проверь, пожалуйста, браузер.");
        }

        Optional<String> videoId = findFirstVideoId(query);
        if (videoId.isPresent()) {
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId.get() + "&autoplay=1";
            return openUrl(videoUrl, "Включаю на YouTube: «" + query + "».", "Нашла песню, но не смогла открыть ролик.");
        }

        return openUrl(searchUrl, "Не смогла сразу выбрать ролик, поэтому открыла поиск YouTube по «" + query + "».", "Не смогла открыть YouTube. Проверь, пожалуйста, браузер.");
    }

    private String openUrl(String url, String successMessage, String errorMessage) {
        try {
            Desktop.getDesktop().browse(new URI(url));
            return successMessage;
        } catch (Exception e) {
            return errorMessage;
        }
    }

    private Optional<String> findFirstVideoId(String query) {
        Request request = new Request.Builder()
            .url(buildSearchUrl(query))
            .header("User-Agent", "Mozilla/5.0 AURA Assistant")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.7,en;q=0.6")
            .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return Optional.empty();
            }

            String html = response.body().string();
            Matcher matcher = VIDEO_ID_PATTERN.matcher(html);
            while (matcher.find()) {
                String videoId = matcher.group(1);
                if (!videoId.isBlank()) {
                    return Optional.of(videoId);
                }
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private String buildSearchUrl(String query) {
        return "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private boolean isPlayIntent(String input) {
        String text = input == null ? "" : input.toLowerCase(Locale.ROOT).replace('ё', 'е');
        return text.contains("включ")
            || text.contains("постав")
            || text.contains("проигр")
            || text.contains("запусти")
            || text.contains("песня")
            || text.contains("песню")
            || text.contains("трек")
            || text.contains("музык");
    }

    private String extractQuery(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String cleaned = input
            .replaceAll("(?iu)youtube|ютубе?|ютьюбе?", " ")
            .replaceAll("[\"'«»?!,.;:()\\[\\]{}]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        StringBuilder query = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (word.isBlank() || isStopWord(word)) {
                continue;
            }
            if (!query.isEmpty()) {
                query.append(' ');
            }
            query.append(word);
        }

        return query.toString().trim();
    }

    private boolean isStopWord(String word) {
        String normalized = word.toLowerCase().replace('ё', 'е');
        return normalized.equals("аура")
            || normalized.equals("пожалуйста")
            || normalized.equals("можешь")
            || normalized.equals("ли")
            || normalized.equals("этот")
            || normalized.equals("это")
            || normalized.equals("мне")
            || normalized.equals("включи")
            || normalized.equals("включить")
            || normalized.equals("поставь")
            || normalized.equals("поставить")
            || normalized.equals("запусти")
            || normalized.equals("запустить")
            || normalized.equals("проиграй")
            || normalized.equals("проиграть")
            || normalized.equals("открой")
            || normalized.equals("открыть")
            || normalized.equals("найди")
            || normalized.equals("найти")
            || normalized.equals("поищи")
            || normalized.equals("песня")
            || normalized.equals("песню")
            || normalized.equals("трек")
            || normalized.equals("клип")
            || normalized.equals("музыку")
            || normalized.equals("музыка")
            || normalized.equals("на")
            || normalized.equals("в");
    }
}
