package com.assistant.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiService {
    private static final String API_KEY = "AIzaSyA2m5Z3tN8S-kaaTO-i4lHNYgsiLdhmOcY";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public GeminiService() {
        // Настраиваем клиент с таймаутами
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * Отправляет запрос к Gemini API и возвращает ответ
     * @param prompt текст запроса к нейросети
     * @return ответ от Gemini или сообщение об ошибке
     */
    public String ask(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Пожалуйста, задайте вопрос";
        }
        
        try {
            // Формируем JSON запрос программно через Gson
            JsonObject requestBody = buildRequestBody(prompt);
            String jsonBody = gson.toJson(requestBody);
            
            // Создаем POST запрос
            Request request = new Request.Builder()
                    .url(API_URL + API_KEY)
                    .post(RequestBody.create(jsonBody, JSON))
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            // Выполняем запрос
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Gemini API error: " + response.code());
                    if (response.body() != null) {
                        System.err.println("Response body: " + response.body().string());
                    }
                    return "Извините, нейросеть временно недоступна";
                }
                
                // Парсим ответ
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    return parseGeminiResponse(responseBody);
                } else {
                    return "Извините, нейросеть временно недоступна";
                }
            }
            
        } catch (IOException e) {
            System.err.println("Ошибка при обращении к Gemini API: " + e.getMessage());
            e.printStackTrace();
            return "Извините, нейросеть временно недоступна";
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка: " + e.getMessage());
            e.printStackTrace();
            return "Извините, нейросеть временно недоступна";
        }
    }
    
    /**
     * Формирует тело запроса в формате Gemini API
     * Структура: {"contents":[{"parts":[{"text": "ПРОМПТ"}]}]}
     */
    private JsonObject buildRequestBody(String prompt) {
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
        
        return requestBody;
    }
    
    /**
     * Извлекает текст ответа из JSON структуры Gemini
     * Путь: candidates[0].content.parts[0].text
     */
    private String parseGeminiResponse(String responseBody) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // Проверяем наличие candidates
            if (!jsonResponse.has("candidates")) {
                System.err.println("Ответ не содержит candidates");
                return "Извините, нейросеть временно недоступна";
            }
            
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() == 0) {
                System.err.println("Массив candidates пуст");
                return "Извините, нейросеть временно недоступна";
            }
            
            // Получаем первого кандидата
            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            
            // Проверяем наличие content
            if (!firstCandidate.has("content")) {
                System.err.println("Кандидат не содержит content");
                return "Извините, нейросеть временно недоступна";
            }
            
            JsonObject content = firstCandidate.getAsJsonObject("content");
            
            // Проверяем наличие parts
            if (!content.has("parts")) {
                System.err.println("Content не содержит parts");
                return "Извините, нейросеть временно недоступна";
            }
            
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts.size() == 0) {
                System.err.println("Массив parts пуст");
                return "Извините, нейросеть временно недоступна";
            }
            
            // Получаем текст из первой части
            JsonObject firstPart = parts.get(0).getAsJsonObject();
            if (!firstPart.has("text")) {
                System.err.println("Part не содержит text");
                return "Извините, нейросеть временно недоступна";
            }
            
            String text = firstPart.get("text").getAsString();
            
            // Очищаем текст от лишних пробелов
            return text.trim();
            
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге ответа Gemini: " + e.getMessage());
            e.printStackTrace();
            return "Извините, нейросеть временно недоступна";
        }
    }
    
    /**
     * Проверяет доступность Gemini API
     * @return true если API доступен
     */
    public boolean isAvailable() {
        try {
            String testResponse = ask("Привет");
            return !testResponse.equals("Извините, нейросеть временно недоступна");
        } catch (Exception e) {
            return false;
        }
    }
}
