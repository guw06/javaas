package com.assistant.commands;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ClipboardCommand implements Command {
    @Override
    public String execute(String input) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return "В буфере обмена сейчас нет текста.";
            }
            
            String clipboardText = (String) clipboard.getData(DataFlavor.stringFlavor);
            
            if (clipboardText == null || clipboardText.trim().isEmpty()) {
                return "Буфер обмена пуст.";
            }
            
            if (clipboardText.length() > 200) {
                clipboardText = clipboardText.substring(0, 200) + "...";
            }
            
            System.out.println("Содержимое буфера обмена: " + clipboardText);
            
            return "В буфере обмена: " + clipboardText;
            
        } catch (UnsupportedFlavorException e) {
            System.err.println("Неподдерживаемый формат данных в буфере обмена: " + e.getMessage());
            return "В буфере обмена есть данные, но это не текст.";
        } catch (IOException e) {
            System.err.println("Ошибка при чтении буфера обмена: " + e.getMessage());
            return "Не смогла прочитать буфер обмена.";
        } catch (IllegalStateException e) {
            System.err.println("Буфер обмена недоступен: " + e.getMessage());
            return "Буфер обмена сейчас занят. Попробуй еще раз.";
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при работе с буфером обмена: " + e.getMessage());
            e.printStackTrace();
            return "Не смогла прочитать буфер обмена.";
        }
    }
}
