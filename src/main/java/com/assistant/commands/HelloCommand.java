package com.assistant.commands;

import java.time.LocalTime;
import java.util.Random;

public class HelloCommand implements Command {
    private static final Random random = new Random();
    
    @Override
    public String execute(String input) {
        int hour = LocalTime.now().getHour();
        
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = getRandomItem(new String[]{
                "Доброе утро. Я рядом, чем займемся?",
                "Доброе утро. Готова помочь, говори обычными словами.",
                "С добрым утром. Я на связи."
            });
        } else if (hour >= 12 && hour < 17) {
            greeting = getRandomItem(new String[]{
                "Добрый день. Слушаю тебя.",
                "Добрый день. Что нужно сделать?",
                "Я здесь. Чем помочь?"
            });
        } else if (hour >= 17 && hour < 22) {
            greeting = getRandomItem(new String[]{
                "Добрый вечер. Как прошел день?",
                "Добрый вечер. Готова помочь.",
                "Я на связи. Что делаем?"
            });
        } else {
            greeting = getRandomItem(new String[]{
                "Доброй ночи. Я рядом, говори.",
                "Поздно, но я на связи.",
                "Я не сплю. Чем помочь?"
            });
        }
        
        return greeting;
    }
    
    private String getRandomItem(String[] items) {
        return items[random.nextInt(items.length)];
    }
}
