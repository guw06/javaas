package com.assistant.services;

import com.assistant.commands.BrowserTabsCommand;
import com.assistant.commands.CalculatorCommand;
import com.assistant.commands.FileControlCommand;
import com.assistant.commands.NetworkControlCommand;
import com.assistant.commands.NewsCommand;
import com.assistant.commands.ProblemSolverCommand;
import com.assistant.commands.ProgramControlCommand;
import com.assistant.commands.SportsScoreCommand;
import com.assistant.commands.TaskManagerCommand;
import com.assistant.commands.WordDocumentCommand;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class SmartAssistantService {
    private static final Pattern MATH_PATTERN = Pattern.compile(".*\\d+\\s*([+\\-*/^]|плюс|минус|умнож|дели|процент|%).*\\d+.*", Pattern.CASE_INSENSITIVE);

    private final CalculatorCommand calculator = new CalculatorCommand();
    private final ProblemSolverCommand problemSolver = new ProblemSolverCommand();
    private final WordDocumentCommand wordDocument = new WordDocumentCommand();
    private final SportsScoreCommand sports = new SportsScoreCommand();
    private final NewsCommand news = new NewsCommand();
    private final ProgramControlCommand programs = new ProgramControlCommand();
    private final BrowserTabsCommand browser = new BrowserTabsCommand();
    private final NetworkControlCommand network = new NetworkControlCommand();
    private final FileControlCommand files = new FileControlCommand();
    private final TaskManagerCommand taskManager = new TaskManagerCommand();

    public Optional<String> process(String input) {
        String text = normalize(input);

        if (text.isBlank()) {
            return Optional.empty();
        }

        if (isWordDocumentIntent(text)) {
            return Optional.of(wordDocument.execute(input));
        }
        if (isSportsIntent(text)) {
            return Optional.of(sports.execute(input));
        }
        if (isNewsIntent(text)) {
            return Optional.of(news.execute(input));
        }
        if (isMathIntent(text)) {
            return Optional.of(calculator.execute(input));
        }
        if (isProblemSolvingIntent(text)) {
            return Optional.of(problemSolver.execute(input));
        }
        if (isNetworkIntent(text)) {
            return Optional.of(network.execute(input));
        }
        if (isTaskManagerIntent(text)) {
            return Optional.of(taskManager.execute(input));
        }
        if (isBrowserIntent(text)) {
            return Optional.of(browser.execute(input));
        }
        if (isFileIntent(text)) {
            return Optional.of(files.execute(input));
        }
        if (isProgramIntent(text)) {
            return Optional.of(programs.execute(input));
        }

        return Optional.empty();
    }

    public String buildFallbackPrompt(String input) {
        return """
            Ты AURA, персональный ассистент для управления компьютером.
            У тебя есть инструменты: программы, вкладки браузера, Wi-Fi/Bluetooth, файлы, Word-документы, новости, спорт, память, расчеты.
            Если пользователь просит действие, но инструмент еще не подключен, скажи честно, что именно нужно добавить.
            Если это вопрос, отвечай как умный личный ассистент: кратко, понятно, по-русски.
            Не выдумывай, что ты выполнила действие, если оно не выполнено.

            Пользователь: %s
            """.formatted(input);
    }

    private boolean isMathIntent(String text) {
        return containsAny(text, "посчитай", "вычисли", "рассчитай", "сколько будет", "калькулятор")
            || MATH_PATTERN.matcher(text).matches();
    }

    private boolean isProblemSolvingIntent(String text) {
        return containsAny(text, "реши задачу", "решить задачу", "помоги решить", "объясни решение", "найди решение");
    }

    private boolean isWordDocumentIntent(String text) {
        return containsAny(text, "word", "ворд", "docx", "документ")
            && containsAny(text, "создай", "сделай", "напиши", "сформируй", "подготовь");
    }

    private boolean isSportsIntent(String text) {
        return containsAny(text,
            "счет матча", "счет игры", "результат матча", "как сыграли", "спорт",
            "nba", "нба", "nfl", "nhl", "нхл", "апл", "лига чемпион", "футбол", "баскетбол", "хоккей");
    }

    private boolean isNewsIntent(String text) {
        return containsAny(text, "новости", "новостная сводка", "что нового", "последние события", "сводка дня");
    }

    private boolean isNetworkIntent(String text) {
        return containsAny(text, "wi-fi", "wifi", "вайфай", "вай-фай", "bluetooth", "блютуз", "блютус");
    }

    private boolean isTaskManagerIntent(String text) {
        return containsAny(text, "диспетчер задач", "task manager", "taskmgr");
    }

    private boolean isBrowserIntent(String text) {
        return containsAny(text, "вкладк", "браузер", "сайт", "адресная строка", "перейди на", "открой youtube", "открой google");
    }

    private boolean isFileIntent(String text) {
        return containsAny(text, "файл", "папк", "директор")
            && containsAny(text, "создай", "удали", "перемести", "перенеси", "сделай", "создать", "удалить");
    }

    private boolean isProgramIntent(String text) {
        return containsAny(text, "открой", "запусти", "стартани", "закрой", "выключи приложение", "заверши")
            && containsAny(text, "калькулятор", "проводник", "блокнот", "paint", "chrome", "edge", "cmd", "powershell", "терминал");
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
                .replaceAll("[?!,;:()\\[\\]{}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
