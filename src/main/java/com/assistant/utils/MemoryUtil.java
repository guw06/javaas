package com.assistant.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MemoryUtil {
    private static final String MEMORY_FILE = "memory.txt";

    public static void saveNote(String text) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MEMORY_FILE, true))) {
            writer.println(text);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении заметки: " + e.getMessage());
        }
    }

    public static String readAllNotes() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(MEMORY_FILE));
            if (lines.isEmpty()) {
                return "Я ничего не помню";
            }
            return String.join("\n", lines);
        } catch (IOException e) {
            return "Не удалось прочитать память";
        }
    }
}
