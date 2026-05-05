package com.assistant.commands;

import com.assistant.services.GeminiService;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationCommand implements Command {
    private static final Pattern TARGET_LANGUAGE_PATTERN = Pattern.compile(
        "(?:^|\\s)(на|по)\\s+([\\p{L}a-zA-Z-]+)(?=\\s|$)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    private final GeminiService geminiService = new GeminiService();

    private record TranslationRequest(String text, String targetLanguage) {
    }

    private record LanguageMention(String language, int start, int end) {
    }

    @Override
    public String execute(String input) {
        Optional<TranslationRequest> request = parseRequest(input);
        if (request.isEmpty()) {
            return "Я поняла, что нужен перевод. Напиши так: «переведи привет на японский».";
        }

        TranslationRequest translation = request.get();
        Optional<String> localAnswer = translateLocally(translation.text(), translation.targetLanguage());
        if (localAnswer.isPresent()) {
            return localAnswer.get();
        }

        return geminiService.ask("""
            Переведи фразу на %s.
            Фраза: %s
            Ответь коротко: сначала перевод, потом в скобках произношение, если оно полезно.
            """.formatted(translation.targetLanguage(), translation.text()));
    }

    private Optional<TranslationRequest> parseRequest(String input) {
        String normalized = normalize(input);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        Optional<LanguageMention> target = findTargetLanguage(normalized);
        if (target.isEmpty()) {
            return Optional.empty();
        }

        LanguageMention mention = target.get();
        String beforeLanguage = normalized.substring(0, mention.start()).trim();
        String afterLanguage = normalized.substring(mention.end()).trim();

        String text = chooseText(beforeLanguage, afterLanguage);
        text = cleanTextToTranslate(text);

        if (text.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new TranslationRequest(text, mention.language()));
    }

    private Optional<LanguageMention> findTargetLanguage(String normalized) {
        Matcher matcher = TARGET_LANGUAGE_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String language = normalizeLanguage(matcher.group(2));
            if (!language.isBlank()) {
                return Optional.of(new LanguageMention(language, matcher.start(1), matcher.end(2)));
            }
        }

        String[] tokens = normalized.split("\\s+");
        int searchStart = 0;
        for (String token : tokens) {
            String language = normalizeLanguage(token);
            if (!language.isBlank()) {
                int start = normalized.indexOf(token, searchStart);
                return Optional.of(new LanguageMention(language, start, start + token.length()));
            }
            searchStart += token.length() + 1;
        }

        return Optional.empty();
    }

    private String chooseText(String beforeLanguage, String afterLanguage) {
        if (!afterLanguage.isBlank() && containsAny(beforeLanguage, "как будет", "как сказать", "скажи", "расскажи")) {
            return afterLanguage;
        }

        if (!afterLanguage.isBlank() && beforeLanguage.matches(".*\\b(переведи|перевод|слово|фраза)\\b.*")) {
            return beforeLanguage;
        }

        return beforeLanguage.isBlank() ? afterLanguage : beforeLanguage;
    }

    private String cleanTextToTranslate(String text) {
        return text
            .replaceFirst("^(расскажи\\s+)?как\\s+будет\\s+", "")
            .replaceFirst("^(расскажи\\s+)?как\\s+сказать\\s+", "")
            .replaceFirst("^перевод\\s+слова\\s+", "")
            .replaceFirst("^перевод\\s+фразы\\s+", "")
            .replaceFirst("^перевод\\s+", "")
            .replaceFirst("^переведи\\s+", "")
            .replaceFirst("^перевести\\s+", "")
            .replaceFirst("^что\\s+значит\\s+", "")
            .replaceFirst("^что\\s+означает\\s+", "")
            .replaceFirst("^слово\\s+", "")
            .replaceFirst("^фразу\\s+", "")
            .replaceFirst("^на\\s+", "")
            .replaceFirst("^по\\s+", "")
            .replaceAll("^['\"«»\\s]+|['\"«»\\s]+$", "")
            .trim();
    }

    private Optional<String> translateLocally(String text, String targetLanguage) {
        String phrase = normalize(text);

        if ("японский".equals(targetLanguage)) {
            return translateToJapanese(text, phrase);
        }
        if ("русский".equals(targetLanguage)) {
            return translateToRussian(text, phrase);
        }
        if ("английский".equals(targetLanguage)) {
            return translateToEnglish(text, phrase);
        }
        if ("казахский".equals(targetLanguage)) {
            return translateToKazakh(text, phrase);
        }

        return Optional.empty();
    }

    private Optional<String> translateToJapanese(String original, String phrase) {
        return switch (phrase) {
            case "привет", "здравствуй", "здравствуйте" -> Optional.of(
                "«" + original.trim() + "» на японском — «こんにちは» (konnichiwa). Ближе всего по смыслу: «здравствуйте» или «добрый день»."
            );
            case "доброе утро" -> Optional.of("«Доброе утро» на японском — «おはようございます» (ohayou gozaimasu).");
            case "добрый вечер" -> Optional.of("«Добрый вечер» на японском — «こんばんは» (konbanwa).");
            case "спасибо" -> Optional.of("«Спасибо» на японском — «ありがとう» (arigatou). Более вежливо: «ありがとうございます» (arigatou gozaimasu).");
            case "пока", "до свидания" -> Optional.of("«Пока» на японском можно сказать «じゃあね» (jaa ne). Более официально: «さようなら» (sayounara).");
            case "да" -> Optional.of("«Да» на японском — «はい» (hai).");
            case "нет" -> Optional.of("«Нет» на японском — «いいえ» (iie).");
            case "как дела" -> Optional.of("«Как дела?» на японском — «元気ですか» (genki desu ka).");
            case "я тебя люблю" -> Optional.of("«Я тебя люблю» на японском — «愛してる» (aishiteru).");
            default -> Optional.empty();
        };
    }

    private Optional<String> translateToRussian(String original, String phrase) {
        return switch (phrase) {
            case "こんにちは", "konnichiwa", "конничива", "коничива", "конитива" -> Optional.of(
                "«" + original.trim() + "» по-русски — «здравствуйте» или «добрый день». В разговоре можно сказать проще: «привет»."
            );
            case "ありがとう", "arigatou", "аригато", "аригатоу" -> Optional.of("«" + original.trim() + "» по-русски — «спасибо».");
            case "さようなら", "sayounara", "sayonara", "сайонара" -> Optional.of("«" + original.trim() + "» по-русски — «до свидания».");
            case "おはようございます", "ohayou gozaimasu", "охайо", "охайо годзаимас" -> Optional.of("«" + original.trim() + "» по-русски — «доброе утро».");
            case "こんばんは", "konbanwa", "конбанва" -> Optional.of("«" + original.trim() + "» по-русски — «добрый вечер».");
            case "сәлем", "салем", "салеметсиз бе", "сәлеметсіз бе" -> Optional.of("«" + original.trim() + "» по-русски — «привет» или «здравствуйте».");
            case "рақмет", "рахмет" -> Optional.of("«" + original.trim() + "» по-русски — «спасибо».");
            case "қайырлы таң", "кайырлы тан" -> Optional.of("«" + original.trim() + "» по-русски — «доброе утро».");
            case "қайырлы күн", "кайырлы кун" -> Optional.of("«" + original.trim() + "» по-русски — «добрый день».");
            case "қалайсың", "калайсын", "қалайсыз", "калайсыз" -> Optional.of("«" + original.trim() + "» по-русски — «как дела?».");
            default -> Optional.empty();
        };
    }

    private Optional<String> translateToEnglish(String original, String phrase) {
        return switch (phrase) {
            case "привет", "здравствуй", "здравствуйте" -> Optional.of("«" + original.trim() + "» на английском — «hello» или проще «hi».");
            case "спасибо" -> Optional.of("«Спасибо» на английском — «thank you».");
            case "пока", "до свидания" -> Optional.of("«Пока» на английском — «bye» или «goodbye».");
            case "да" -> Optional.of("«Да» на английском — «yes».");
            case "нет" -> Optional.of("«Нет» на английском — «no».");
            case "как дела" -> Optional.of("«Как дела?» на английском — «how are you?».");
            case "я тебя люблю" -> Optional.of("«Я тебя люблю» на английском — «I love you».");
            default -> Optional.empty();
        };
    }

    private Optional<String> translateToKazakh(String original, String phrase) {
        return switch (phrase) {
            case "привет", "здравствуй", "здравствуйте" -> Optional.of("«" + original.trim() + "» на казахском — «сәлем». Более вежливо: «сәлеметсіз бе».");
            case "доброе утро" -> Optional.of("«Доброе утро» на казахском — «қайырлы таң».");
            case "добрый день" -> Optional.of("«Добрый день» на казахском — «қайырлы күн».");
            case "добрый вечер" -> Optional.of("«Добрый вечер» на казахском — «қайырлы кеш».");
            case "спасибо" -> Optional.of("«Спасибо» на казахском — «рақмет».");
            case "пожалуйста" -> Optional.of("«Пожалуйста» на казахском — «өтінемін». В ответ на спасибо чаще говорят «оқасы жоқ».");
            case "пока" -> Optional.of("«Пока» на казахском — «сау бол».");
            case "до свидания" -> Optional.of("«До свидания» на казахском — «сау болыңыз».");
            case "да" -> Optional.of("«Да» на казахском — «иә».");
            case "нет" -> Optional.of("«Нет» на казахском — «жоқ».");
            case "как дела" -> Optional.of("«Как дела?» на казахском — «қалайсың?» Нейтрально-вежливо: «қалайсыз?».");
            case "я тебя люблю" -> Optional.of("«Я тебя люблю» на казахском — «мен сені жақсы көремін».");
            default -> Optional.empty();
        };
    }

    private String normalizeLanguage(String value) {
        String language = normalize(value)
            .replace("-", "")
            .replace(" ", "")
            .replace("қ", "к")
            .replace("ә", "а")
            .replace("і", "и")
            .replace("ң", "н")
            .replace("ғ", "г")
            .replace("ү", "у")
            .replace("ұ", "у")
            .replace("ө", "о");

        if (language.contains("япон") || language.contains("japan") || language.contains("nihon")) {
            return "японский";
        }
        if (language.contains("рус") || language.contains("russian")) {
            return "русский";
        }
        if (language.contains("англ") || language.contains("english") || language.equals("en")) {
            return "английский";
        }
        if (language.contains("каз") || language.contains("kazakh") || language.contains("kazak")
            || editDistance(language, "казахский") <= 3 || editDistance(language, "казахском") <= 3) {
            return "казахский";
        }
        if (language.contains("китай") || language.contains("chinese")) {
            return "китайский";
        }
        if (language.contains("коре") || language.contains("korean")) {
            return "корейский";
        }
        if (language.contains("тур") || language.contains("turkish")) {
            return "турецкий";
        }
        if (language.contains("нем") || language.contains("german")) {
            return "немецкий";
        }
        if (language.contains("франц") || language.contains("french")) {
            return "французский";
        }
        if (language.contains("испан") || language.contains("spanish")) {
            return "испанский";
        }

        return "";
    }

    private int editDistance(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[left.length()][right.length()];
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        return input == null
            ? ""
            : input.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[?!.,;:()\\[\\]{}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
