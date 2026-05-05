package com.assistant.services;

import com.assistant.commands.BrowserTabsCommand;
import com.assistant.commands.CalculatorCommand;
import com.assistant.commands.ComputerAgentCommand;
import com.assistant.commands.FileControlCommand;
import com.assistant.commands.NetworkControlCommand;
import com.assistant.commands.NewsCommand;
import com.assistant.commands.ProblemSolverCommand;
import com.assistant.commands.ProgramControlCommand;
import com.assistant.commands.SportsScoreCommand;
import com.assistant.commands.TaskManagerCommand;
import com.assistant.commands.TranslationCommand;
import com.assistant.commands.WordDocumentCommand;
import com.assistant.commands.YouTubeCommand;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class SmartAssistantService {
    private static final Pattern MATH_PATTERN = Pattern.compile(".*\\d+\\s*([+\\-*/^]|плюс|минус|умнож|дели|процент|%).*\\d+.*", Pattern.CASE_INSENSITIVE);

    private final CalculatorCommand calculator = new CalculatorCommand();
    private final ComputerAgentCommand computerAgent = new ComputerAgentCommand();
    private final ProblemSolverCommand problemSolver = new ProblemSolverCommand();
    private final WordDocumentCommand wordDocument = new WordDocumentCommand();
    private final SportsScoreCommand sports = new SportsScoreCommand();
    private final NewsCommand news = new NewsCommand();
    private final ProgramControlCommand programs = new ProgramControlCommand();
    private final BrowserTabsCommand browser = new BrowserTabsCommand();
    private final NetworkControlCommand network = new NetworkControlCommand();
    private final FileControlCommand files = new FileControlCommand();
    private final TaskManagerCommand taskManager = new TaskManagerCommand();
    private final YouTubeCommand youtube = new YouTubeCommand();
    private final TranslationCommand translation = new TranslationCommand();

    public Optional<String> process(String input) {
        String text = normalize(input);

        if (text.isBlank()) {
            return Optional.empty();
        }

        if (isTranslationIntent(text)) {
            return Optional.of(translation.execute(input));
        }
        if (isAgentIntent(text)) {
            return Optional.of(computerAgent.execute(input));
        }
        if (isYouTubeIntent(text)) {
            return Optional.of(youtube.execute(input));
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
        return buildFallbackPrompt(input, "", "");
    }

    public String buildFallbackPrompt(String input, String previousUserInput, String previousAssistantResponse) {
        return buildFallbackPrompt(input, previousUserInput, previousAssistantResponse, "Профиль пользователя пока пуст.");
    }

    public String buildFallbackPrompt(String input, String previousUserInput, String previousAssistantResponse, String profileContext) {
        String context = previousUserInput == null || previousUserInput.isBlank()
            ? "Контекста прошлого сообщения нет."
            : """
            Предыдущий запрос пользователя: %s
            Предыдущий ответ AURA: %s
            """.formatted(previousUserInput, previousAssistantResponse == null ? "" : previousAssistantResponse);

        return """
            Ты AURA, персональная ассистентка для управления компьютером.
            Общайся естественно, тепло и по-человечески, как внимательная девушка-помощница, а не как сухая нейросеть.
            Отвечай как умный голосовой ассистент: сначала давай полезный ответ, потом короткое пояснение, если нужно.
            Перед ответом тихо определи намерение пользователя: вопрос, перевод, расчет, поиск, управление компьютером, работа с файлом, память или обычный разговор.
            Не цепляйся за одно знакомое слово внутри фразы. Например, если пользователь спрашивает "как будет привет на японском", это перевод, а не приветствие.
            Понимай синонимы, ошибки и разговорные формулировки: "как будет", "что значит", "поставь", "вруби", "найди", "покажи", "сделай", "казхсский" = "казахский".
            Если пользователь спрашивает факт, термин, перевод или объяснение, отвечай содержательно даже если это не команда управления компьютером.
            У тебя уже есть инструменты: программы, браузер, YouTube-поиск, вкладки, Wi-Fi/Bluetooth, файлы, Word-документы, новости, спорт, память, расчеты и agent mode.
            Не говори "я подключу инструмент" или "давай я добавлю", если ты сама не меняешь код проекта прямо сейчас.
            Если действие еще не поддерживается приложением, скажи коротко: "этот режим пока не подключен".
            Если это вопрос, отвечай кратко, понятно, по-русски, без канцелярита и фраз вроде "как ИИ".
            Не выдумывай, что ты выполнила действие, если оно не выполнено.
            Не показывай длинные технические пути к файлам или служебные URL без необходимости.
            Учитывай короткий контекст диалога:
            %s
            Учитывай профиль:
            %s

            Пользователь: %s
            """.formatted(context, profileContext == null ? "Профиль пользователя пока пуст." : profileContext, input);
    }

    private boolean isMathIntent(String text) {
        return containsAny(text, "посчитай", "вычисли", "рассчитай", "сколько будет", "калькулятор")
            || MATH_PATTERN.matcher(text).matches();
    }

    private boolean isTranslationIntent(String text) {
        boolean mentionsLanguage = containsAny(text,
            "на япон", "по япон", "на рус", "по рус", "на англ", "по англ",
            "на каз", "по каз", "қазақ", "казакша", "kazakh",
            "японский", "русский", "английский", "казахский", "казх",
            "китайский", "корейский", "турецкий", "немецкий", "французский", "испанский"
        );
        if (!mentionsLanguage) {
            return false;
        }

        return containsAny(text,
            "переведи", "перевод", "перевести", "как будет", "как сказать",
            "что значит", "что означает", "слово", "фраза", "по русски", "по-японски",
            "на казхсский", "на казхском"
        ) || text.matches("^[\\p{L}\\s'-]{2,40}\\s+(на|по)\\s+[\\p{L}a-zA-Z-]+.*$");
    }

    private boolean isAgentIntent(String text) {
        if (containsAny(text, "агент", "agent mode", "ai agent", "автоматизируй", "сделай сама", "самостоятельно")) {
            return true;
        }
        if (containsAny(text, "переименуй", "переименовать", "разложи", "организуй", "рассортируй")
            && containsAny(text, "файл", "pdf", "пдф", "загрузк", "документ", "рабочий стол")) {
            return true;
        }
        return containsAny(text, "найди все", "покажи все", "список всех")
            && containsAny(text, "pdf", "пдф", "docx", "word", "jpg", "png", "txt", "загрузк", "документ");
    }

    private boolean isYouTubeIntent(String text) {
        return containsAny(text, "youtube", "ютуб", "ютьюб")
            && containsAny(text, "включи", "вруби", "поставь", "запусти", "проиграй", "открой", "найди", "песня", "песню", "трек", "клип", "музык");
    }

    private boolean isProblemSolvingIntent(String text) {
        return containsAny(text, "реши задачу", "решить задачу", "помоги решить", "объясни решение", "найди решение");
    }

    private boolean isWordDocumentIntent(String text) {
        return containsAny(text, "word", "ворд", "docx", "документ", "реферат", "отчет")
            && containsAny(text, "создай", "сделай", "напиши", "сформируй", "подготовь", "составь");
    }

    private boolean isSportsIntent(String text) {
        return containsAny(text,
            "счет матча", "счет игры", "результат матча", "как сыграли", "спорт",
            "nba", "нба", "nfl", "nhl", "нхл", "апл", "лига чемпион", "футбол", "баскетбол", "хоккей");
    }

    private boolean isNewsIntent(String text) {
        return containsAny(text, "новости", "новостная сводка", "что нового", "последние события", "сводка дня", "расскажи новости");
    }

    private boolean isNetworkIntent(String text) {
        return containsAny(text, "wi-fi", "wifi", "вайфай", "вай-фай", "bluetooth", "блютуз", "блютус");
    }

    private boolean isTaskManagerIntent(String text) {
        return containsAny(text, "диспетчер задач", "task manager", "taskmgr");
    }

    private boolean isBrowserIntent(String text) {
        return containsAny(text, "вкладк", "браузер", "сайт", "адресная строка", "перейди на", "зайди на", "открой youtube", "открой google");
    }

    private boolean isFileIntent(String text) {
        return containsAny(text, "файл", "папк", "директор")
            && containsAny(text, "создай", "удали", "сотри", "убери", "перемести", "перенеси", "сделай", "создать", "удалить");
    }

    private boolean isProgramIntent(String text) {
        return containsAny(text, "открой", "запусти", "стартани", "открой ка", "закрой", "выключи приложение", "заверши")
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
