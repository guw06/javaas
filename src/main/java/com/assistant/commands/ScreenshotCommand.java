package com.assistant.commands;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScreenshotCommand implements Command {
    @Override
    public String execute(String input) {
        try {
            Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRect = new Rectangle(screenSize);
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            File outputFile = new File("screenshot.png");
            ImageIO.write(screenshot, "png", outputFile);
            
            System.out.println("Скриншот сохранен: " + outputFile.getAbsolutePath());
            
            return "Готово, сохранила скриншот как screenshot.png.";
            
        } catch (AWTException e) {
            System.err.println("Ошибка AWT при создании скриншота: " + e.getMessage());
            return "Не смогла сделать скриншот: Windows не дала доступ к экрану.";
        } catch (Exception e) {
            System.err.println("Ошибка при создании скриншота: " + e.getMessage());
            e.printStackTrace();
            return "Не смогла сделать скриншот.";
        }
    }
}
