package com.assistant.commands;

public class HelpCommand implements Command {
    @Override
    public String execute(String input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Вот что умеет AURA:\n\n");

        sb.append("Основное:\n");
        sb.append("  - привет, время, дата\n");
        sb.append("  - погода, новости, валюта\n");
        sb.append("  - шутка, факт, посчитай 2 + 2\n\n");

        sb.append("Компьютер:\n");
        sb.append("  - открой калькулятор\n");
        sb.append("  - открой проводник\n");
        sb.append("  - открой chrome\n");
        sb.append("  - закрой блокнот\n");
        sb.append("  - открой диспетчер задач\n\n");

        sb.append("Браузер:\n");
        sb.append("  - новая вкладка\n");
        sb.append("  - закрой вкладку\n");
        sb.append("  - следующая вкладка\n");
        sb.append("  - предыдущая вкладка\n");
        sb.append("  - обнови вкладку\n");
        sb.append("  - открой сайт youtube.com\n\n");
        sb.append("YouTube и музыка:\n");
        sb.append("  - включи песню Love Me Not на YouTube\n");
        sb.append("  - поставь трек Imagine Dragons на ютубе\n\n");
        sb.append("Если говорите \"включи\" или \"поставь\", AURA откроет первый найденный ролик.\n\n");

        sb.append("Новости и спорт:\n");
        sb.append("  - новости\n");
        sb.append("  - новости про искусственный интеллект\n");
        sb.append("  - счет матча Реал\n");
        sb.append("  - результаты NBA\n");
        sb.append("  - как сыграла Барселона\n\n");

        sb.append("Сеть:\n");
        sb.append("  - включи Wi-Fi\n");
        sb.append("  - выключи Wi-Fi\n");
        sb.append("  - включи Bluetooth\n");
        sb.append("  - выключи Bluetooth\n");
        sb.append("  - настройки Wi-Fi\n\n");

        sb.append("Файлы:\n");
        sb.append("  - создай файл notes.txt\n");
        sb.append("  - создай файл notes.txt с текстом привет\n");
        sb.append("  - создай папку project\n");
        sb.append("  - перемести \"notes.txt\" в \"archive\"\n");
        sb.append("  - удали файл notes.txt\n");
        sb.append("Удаление безопасное: файлы уходят в корзину AURA.\n\n");

        sb.append("Word-документы:\n");
        sb.append("  - создай Word документ \"report.docx\" с текстом Привет\n");
        sb.append("  - создай документ Word на тему искусственный интеллект\n\n");

        sb.append("Agent mode:\n");
        sb.append("  - агент найди все PDF в загрузках\n");
        sb.append("  - агент найди все PDF в проекте\n");
        sb.append("  - агент найди все PDF в загрузках и переименуй их по дате\n");
        sb.append("  - агент разложи загрузки по типам\n");
        sb.append("Пока agent mode работает безопасно: только пользовательские папки и без удаления.\n\n");

        sb.append("Мышление и задачи:\n");
        sb.append("  - посчитай 15 процентов от 240\n");
        sb.append("  - посчитай (12 + 8) * 3\n");
        sb.append("  - реши задачу: ...\n");
        sb.append("  - переведи привет на японский\n");
        sb.append("  - переведи привет на казахский\n");
        sb.append("  - как будет на японском спасибо\n");
        sb.append("  - рахмет на русском\n");
        sb.append("  - коничива на русском\n");
        sb.append("AURA понимает больше синонимов и часть ошибок: открой/запусти/стартани, удали/сотри, найди/поищи/загугли, казхсский/казахский.\n\n");

        sb.append("Память:\n");
        sb.append("  - запомни [текст]\n");
        sb.append("  - вспомни\n");
        sb.append("  - история\n\n");

        sb.append("Если это обычный вопрос, AURA ответит через подключенную модель.");
        return sb.toString();
    }
}
