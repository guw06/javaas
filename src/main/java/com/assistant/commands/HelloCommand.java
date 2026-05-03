package com.assistant.commands;

import java.time.LocalTime;
import java.util.Random;

/**
 * Приветствие — разнообразные ответы в зависимости от времени суток
 */
public class HelloCommand implements Command {
    private static final Random random = new Random();
    
    @Override
    public String execute(String input) {
        int hour = LocalTime.now().getHour();
        
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = getRandomItem(new String[]{
                "Доброе утро! ☀️ Чем могу помочь?",
                "Доброе утро! Готов к работе. Какие задачи на сегодня?",
                "С добрым утром! Все системы активны и готовы помочь."
            });
        } else if (hour >= 12 && hour < 17) {
            greeting = getRandomItem(new String[]{
                "Добрый день! 🌤️ Чем могу быть полезен?",
                "Добрый день! Слушаю вас внимательно.",
                "Добрый день! Все системы работают исправно."
            });
        } else if (hour >= 17 && hour < 22) {
            greeting = getRandomItem(new String[]{
                "Добрый вечер! 🌆 Как прошёл день?",
                "Добрый вечер! Готов помочь с любым вопросом.",
                "Добрый вечер! Рад вас слышать."
            });
        } else {
            greeting = getRandomItem(new String[]{
                "Доброй ночи! 🌙 Чем могу помочь?",
                "Доброй ночи! Я не сплю — готов помогать.",
                "Поздновато, но я всегда на связи! 🌙"
            });
        }
        
        return greeting;
    }
    
    private String getRandomItem(String[] items) {
        return items[random.nextInt(items.length)];
    }
}
