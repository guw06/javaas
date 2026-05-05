package com.assistant.services;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.net.URI;

public class SystemTrayService {
    private TrayIcon trayIcon;

    public void install(String url) {
        try {
            if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
                return;
            }

            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Open AURA");
            open.addActionListener(event -> openUrl(url));
            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(event -> System.exit(0));
            menu.add(open);
            menu.add(exit);

            trayIcon = new TrayIcon(createIcon(), "AURA assistant", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(event -> openUrl(url));
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            System.out.println("System tray недоступен: " + e.getMessage());
        }
    }

    public void remove() {
        try {
            if (trayIcon != null && SystemTray.isSupported()) {
                SystemTray.getSystemTray().remove(trayIcon);
            }
        } catch (Exception ignored) {
        }
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    private Image createIcon() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(18, 24, 21));
        graphics.fillOval(2, 2, 28, 28);
        graphics.setColor(new Color(143, 227, 193));
        graphics.fillOval(7, 7, 18, 18);
        graphics.setColor(new Color(18, 24, 21));
        graphics.drawString("A", 12, 21);
        graphics.dispose();
        return image;
    }
}
