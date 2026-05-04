package com.assistant.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class GeminiService {
    private static final String CONFIG_FILE = "aura.properties";
    private static final String DEFAULT_PROVIDER = "gemini";
    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash-lite";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-5.4-mini";
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final Properties localConfig;
    private final String provider;
    private final String geminiApiKey;
    private final String geminiModel;
    private final String openAiApiKey;
    private final String openAiModel;

    private record AiResponse(boolean success, boolean fallbackAllowed, String text) {
        static AiResponse ok(String text) {
            return new AiResponse(true, false, text);
        }

        static AiResponse fail(String text) {
            return new AiResponse(false, false, text);
        }

        static AiResponse fallback(String text) {
            return new AiResponse(false, true, text);
        }
    }

    public GeminiService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.localConfig = loadLocalConfig();
        this.provider = resolveProvider();
        this.geminiApiKey = resolveGeminiApiKey();
        this.geminiModel = resolveGeminiModel();
        this.openAiApiKey = resolveOpenAiApiKey();
        this.openAiModel = resolveOpenAiModel();
    }

    public String ask(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Пожалуйста, задайте вопрос.";
        }

        AiResponse response = switch (provider) {
            case "openai" -> askOpenAi(prompt);
            case "gemini", "auto" -> askWithGeminiFallback(prompt);
            default -> AiResponse.fail("Неизвестный AI-провайдер: " + provider + ". В aura.properties поставьте ai.provider=gemini, ai.provider=openai или ai.provider=auto.");
        };

        return response.text();
    }

    public boolean isAvailable() {
        String response = ask("Ответь одним словом: ok");
        String normalized = response.toLowerCase(Locale.ROOT);
        return !normalized.contains("ключ")
            && !normalized.contains("недоступ")
            && !normalized.contains("ошибка")
            && !normalized.contains("лимит")
            && !normalized.contains("unknown")
            && !normalized.contains("unauthorized");
    }

    private AiResponse askWithGeminiFallback(String prompt) {
        AiResponse geminiResponse = askGemini(prompt);
        if (geminiResponse.success() || !geminiResponse.fallbackAllowed()) {
            return geminiResponse;
        }

        if (!hasValidKey(openAiApiKey, "YOUR_OPENAI_API_KEY")) {
            return AiResponse.fail("""
                Сейчас основной AI недоступен, а запасной OpenAI ключ не настроен.
                Добавьте в aura.properties:
                openai.api.key=ВАШ_OPENAI_КЛЮЧ
                openai.model=gpt-5.4-mini
                После этого перезапустите AURA.
                """.trim());
        }

        AiResponse openAiResponse = askOpenAi(prompt);
        if (openAiResponse.success()) {
            return openAiResponse;
        }

        return AiResponse.fail(openAiResponse.text());
    }

    private AiResponse askGemini(String prompt) {
        if (!hasValidKey(geminiApiKey, "YOUR_GEMINI_API_KEY")) {
            return AiResponse.fallback("""
                Gemini API ключ не настроен.
                Добавьте его в aura.properties:
                ai.provider=gemini
                gemini.api.key=ВАШ_GEMINI_КЛЮЧ
                gemini.model=gemini-2.5-flash-lite
                После этого перезапустите AURA.
                """.trim());
        }

        try {
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();

            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            Request request = new Request.Builder()
                .url(buildGeminiApiUrl())
                .post(RequestBody.create(gson.toJson(requestBody), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    return buildGeminiErrorResponse(response.code(), responseBody);
                }

                return parseGeminiResponse(responseBody);
            }
        } catch (IOException e) {
            return AiResponse.fallback("Не удалось подключиться к Gemini API. Проверьте интернет и попробуйте ещё раз.");
        } catch (Exception e) {
            return AiResponse.fallback("Произошла ошибка при обращении к Gemini: " + e.getMessage());
        }
    }

    private AiResponse askOpenAi(String prompt) {
        if (!hasValidKey(openAiApiKey, "YOUR_OPENAI_API_KEY")) {
            return AiResponse.fail("""
                OpenAI API ключ не настроен.
                Добавьте его в aura.properties:
                ai.provider=openai
                openai.api.key=ВАШ_OPENAI_КЛЮЧ
                openai.model=gpt-5.4-mini
                После этого перезапустите AURA.
                """.trim());
        }

        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", openAiModel);
            requestBody.addProperty("input", prompt);

            Request request = new Request.Builder()
                .url(OPENAI_RESPONSES_URL)
                .post(RequestBody.create(gson.toJson(requestBody), JSON))
                .addHeader("Authorization", "Bearer " + openAiApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    return AiResponse.fail(buildOpenAiErrorMessage(response.code(), responseBody));
                }

                return parseOpenAiResponse(responseBody);
            }
        } catch (IOException e) {
            return AiResponse.fail("Не удалось подключиться к OpenAI API. Проверьте интернет и попробуйте ещё раз.");
        } catch (Exception e) {
            return AiResponse.fail("Произошла ошибка при обращении к OpenAI: " + e.getMessage());
        }
    }

    private AiResponse parseGeminiResponse(String responseBody) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            if (jsonResponse == null || !jsonResponse.has("candidates")) {
                return AiResponse.fallback("Gemini вернул пустой ответ. Попробуйте переформулировать вопрос.");
            }

            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return AiResponse.fallback("Gemini не дал текстового ответа. Попробуйте другой запрос.");
            }

            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray parts = content != null ? content.getAsJsonArray("parts") : null;
            if (parts == null || parts.isEmpty()) {
                return AiResponse.fallback("Gemini вернул пустой текст.");
            }

            JsonObject firstPart = parts.get(0).getAsJsonObject();
            if (!firstPart.has("text")) {
                return AiResponse.fallback("Gemini не вернул текст.");
            }

            String text = firstPart.get("text").getAsString().trim();
            return text.isBlank()
                ? AiResponse.fallback("Gemini вернул пустой текст.")
                : AiResponse.ok(text);
        } catch (Exception e) {
            return AiResponse.fallback("Не удалось разобрать ответ Gemini.");
        }
    }

    private AiResponse parseOpenAiResponse(String responseBody) {
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            if (root == null) {
                return AiResponse.fail("OpenAI вернул пустой ответ.");
            }

            if (root.has("output_text") && !root.get("output_text").isJsonNull()) {
                String directText = root.get("output_text").getAsString().trim();
                if (!directText.isBlank()) {
                    return AiResponse.ok(directText);
                }
            }

            JsonArray output = root.getAsJsonArray("output");
            if (output == null || output.isEmpty()) {
                return AiResponse.fail("OpenAI не дал текстового ответа. Попробуйте другой запрос.");
            }

            StringBuilder text = new StringBuilder();
            for (JsonElement outputElement : output) {
                JsonObject outputItem = outputElement.getAsJsonObject();
                JsonArray content = outputItem.getAsJsonArray("content");
                if (content == null) {
                    continue;
                }

                for (JsonElement contentElement : content) {
                    JsonObject contentItem = contentElement.getAsJsonObject();
                    if (contentItem.has("text") && !contentItem.get("text").isJsonNull()) {
                        text.append(contentItem.get("text").getAsString());
                    }
                }
            }

            return text.isEmpty()
                ? AiResponse.fail("OpenAI вернул ответ без текста.")
                : AiResponse.ok(text.toString().trim());
        } catch (Exception e) {
            return AiResponse.fail("Не удалось разобрать ответ OpenAI.");
        }
    }

    private String buildGeminiApiUrl() {
        String encodedModel = URLEncoder.encode(geminiModel, StandardCharsets.UTF_8);
        String encodedKey = URLEncoder.encode(geminiApiKey, StandardCharsets.UTF_8);
        return GEMINI_API_BASE_URL + encodedModel + ":generateContent?key=" + encodedKey;
    }

    private AiResponse buildGeminiErrorResponse(int code, String responseBody) {
        String apiMessage = extractApiErrorMessage(responseBody);

        String message = switch (code) {
            case 400 -> "Gemini отклонил запрос. Возможно, неверное имя модели: " + geminiModel + messageSuffix(apiMessage);
            case 401, 403 -> "Gemini API ключ неправильный, отключён или без доступа. Создайте новый ключ и обновите aura.properties.";
            case 404 -> "Модель Gemini не найдена: " + geminiModel + ". Укажите другую модель через gemini.model.";
            case 429 -> "Основной AI сейчас не отвечает. Попробуйте ещё раз немного позже.";
            case 500, 502, 503, 504 -> "Сервис Gemini временно недоступен. Попробуйте ещё раз через минуту.";
            default -> "Gemini API вернул ошибку " + code + messageSuffix(apiMessage);
        };

        return switch (code) {
            case 400, 401, 403, 404, 429, 500, 502, 503, 504 -> AiResponse.fallback(message);
            default -> AiResponse.fail(message);
        };
    }

    private String buildOpenAiErrorMessage(int code, String responseBody) {
        String apiMessage = extractApiErrorMessage(responseBody);

        return switch (code) {
            case 400 -> "OpenAI отклонил запрос. Возможно, неверное имя модели: " + openAiModel + messageSuffix(apiMessage);
            case 401, 403 -> "OpenAI API ключ неправильный, отключён или без доступа. Обновите openai.api.key в aura.properties.";
            case 404 -> "Модель OpenAI не найдена: " + openAiModel + ". Укажите другую модель через openai.model.";
            case 429 -> "Запасной AI сейчас тоже не ответил. Проверьте доступ OpenAI или попробуйте немного позже.";
            case 500, 502, 503, 504 -> "Сервис OpenAI временно недоступен. Попробуйте ещё раз через минуту.";
            default -> "OpenAI API вернул ошибку " + code + messageSuffix(apiMessage);
        };
    }

    private String extractApiErrorMessage(String responseBody) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                return "";
            }

            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            if (root != null && root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message")) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private String messageSuffix(String apiMessage) {
        return apiMessage == null || apiMessage.isBlank() ? "" : " Детали: " + apiMessage;
    }

    private String resolveProvider() {
        String configured = firstNonBlank(
            System.getenv("AURA_AI_PROVIDER"),
            System.getProperty("ai.provider"),
            localConfig.getProperty("ai.provider")
        );
        return configured == null ? DEFAULT_PROVIDER : configured.toLowerCase(Locale.ROOT);
    }

    private String resolveGeminiApiKey() {
        return firstNonBlank(
            System.getenv("GEMINI_API_KEY"),
            System.getenv("GOOGLE_API_KEY"),
            System.getProperty("gemini.api.key"),
            localConfig.getProperty("gemini.api.key")
        );
    }

    private String resolveGeminiModel() {
        String configured = firstNonBlank(
            System.getenv("GEMINI_MODEL"),
            System.getProperty("gemini.model"),
            localConfig.getProperty("gemini.model")
        );
        return configured == null ? DEFAULT_GEMINI_MODEL : configured;
    }

    private String resolveOpenAiApiKey() {
        return firstNonBlank(
            System.getenv("OPENAI_API_KEY"),
            System.getProperty("openai.api.key"),
            localConfig.getProperty("openai.api.key")
        );
    }

    private String resolveOpenAiModel() {
        String configured = firstNonBlank(
            System.getenv("OPENAI_MODEL"),
            System.getProperty("openai.model"),
            localConfig.getProperty("openai.model")
        );
        return configured == null ? DEFAULT_OPENAI_MODEL : configured;
    }

    private Properties loadLocalConfig() {
        Path config = Path.of(CONFIG_FILE).toAbsolutePath().normalize();
        Properties properties = new Properties();
        if (!Files.isRegularFile(config)) {
            return properties;
        }

        try (InputStream input = Files.newInputStream(config)) {
            properties.load(input);
        } catch (IOException ignored) {
            return properties;
        }
        return properties;
    }

    private boolean hasValidKey(String value, String placeholder) {
        return value != null
            && !value.isBlank()
            && !value.equalsIgnoreCase(placeholder)
            && !value.equalsIgnoreCase("YOUR_API_KEY")
            && !value.equalsIgnoreCase("ВАШ_КЛЮЧ");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
