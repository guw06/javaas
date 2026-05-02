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
            // Получаем доступ к системному буферу обмена
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            
            // Проверяем, есть ли в буфере текстовые данные
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return "Буфер обмена пуст или не содержит текста";
            }
            
            // Получаем текст из буфера обмена
            String clipboardText = (String) clipboard.getData(DataFlavor.stringFlavor);
            
            // Проверяем, не пустой ли текст
            if (clipboardText == null || clipboardText.trim().isEmpty()) {
                return "Буфер обмена пуст";
            }
            
            // Ограничиваем длину текста для вывода (максимум 200 символов)
            if (clipboardText.length() > 200) {
                clipboardText = clipboardText.substring(0, 200) + "...";
            }
            
            System.out.println("Содержимое буфера обмена: " + clipboardText);
            
            return "В буфере обмена сейчас: " + clipboardText;
            
        } catch (UnsupportedFlavorException e) {
            System.err.println("Неподдерживаемый формат данных в буфере обмена: " + e.getMessage());
            return "Буфер обмена содержит данные в неподдерживаемом формате";
        } catch (IOException e) {
            System.err.println("Ошибка при чтении буфера обмена: " + e.getMessage());
            return "Не удалось прочитать буфер обмена";
        } catch (IllegalStateException e) {
            System.err.println("Буфер обмена недоступен: " + e.getMessage());
            return "Буфер обмена временно недоступен";
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при работе с буфером обмена: " + e.getMessage());
            e.printStackTrace();
            return "Произошла ошибка при чтении буфера обмена";
        }
    }
}
