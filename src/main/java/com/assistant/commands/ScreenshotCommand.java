package com.assistant.commands;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScreenshotCommand implements Command {
    @Override
    public String execute(String input) {
        try {
            // Создаем экземпляр Robot для захвата экрана
            Robot robot = new Robot();
            
            // Получаем размер экрана
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRect = new Rectangle(screenSize);
            
            // Делаем снимок экрана
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            
            // Сохраняем изображение в корень проекта
            File outputFile = new File("screenshot.png");
            ImageIO.write(screenshot, "png", outputFile);
            
            System.out.println("Скриншот сохранен: " + outputFile.getAbsolutePath());
            
            return "Скриншот успешно сохранен в " + outputFile.getAbsolutePath();
            
        } catch (AWTException e) {
            System.err.println("Ошибка AWT при создании скриншота: " + e.getMessage());
            return "Не удалось сделать скриншот: ошибка доступа к экрану";
        } catch (Exception e) {
            System.err.println("Ошибка при создании скриншота: " + e.getMessage());
            e.printStackTrace();
            return "Не удалось сделать скриншот";
        }
    }
}
