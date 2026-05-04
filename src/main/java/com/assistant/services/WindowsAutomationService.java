package com.assistant.services;

import java.awt.Desktop;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WindowsAutomationService {
    private static final int DEFAULT_TIMEOUT_SECONDS = 25;

    private record AppProfile(List<String> aliases, String executable, String processImage) {
    }

    private record CommandResult(boolean success, int exitCode, String output) {
    }

    private final List<AppProfile> apps = List.of(
        new AppProfile(List.of("блокнот", "notepad", "заметки"), "notepad.exe", "notepad.exe"),
        new AppProfile(List.of("калькулятор", "calculator", "calc"), "calc.exe", "CalculatorApp.exe"),
        new AppProfile(List.of("paint", "пэйнт", "рисование"), "mspaint.exe", "mspaint.exe"),
        new AppProfile(List.of("проводник", "explorer", "файлы"), "explorer.exe", "explorer.exe"),
        new AppProfile(List.of("командная строка", "cmd"), "cmd.exe", "cmd.exe"),
        new AppProfile(List.of("powershell", "пауэршелл"), "powershell.exe", "powershell.exe"),
        new AppProfile(List.of("терминал", "terminal"), "wt.exe", "WindowsTerminal.exe"),
        new AppProfile(List.of("chrome", "хром", "google chrome"), "chrome.exe", "chrome.exe"),
        new AppProfile(List.of("edge", "эдж", "microsoft edge"), "msedge.exe", "msedge.exe"),
        new AppProfile(List.of("диспетчер задач", "task manager", "taskmgr"), "taskmgr.exe", "Taskmgr.exe")
    );

    public String openProgram(String input) {
        String target = normalize(input);
        Optional<AppProfile> profile = findApp(target);

        if (profile.isPresent()) {
            return startExecutable(profile.get().executable(), "Открываю: " + profile.get().executable());
        }

        Optional<Path> explicitExe = extractExplicitExecutable(input);
        if (explicitExe.isPresent()) {
            return startExecutable(explicitExe.get().toString(), "Открываю: " + explicitExe.get().getFileName());
        }

        return "Не понял, какую программу открыть. Примеры: открой калькулятор, открой диспетчер задач, открой chrome.";
    }

    public String closeProgram(String input) {
        String target = normalize(input);
        Optional<AppProfile> profile = findApp(target);

        if (profile.isEmpty()) {
            return "Не понял, какую программу закрыть. Пример: закрой блокнот, закрой chrome.";
        }

        String image = profile.get().processImage();
        CommandResult result = runCommand(List.of("taskkill.exe", "/IM", image, "/T", "/F"), DEFAULT_TIMEOUT_SECONDS);
        if (result.success()) {
            return "Закрыл программу: " + image;
        }

        return "Не удалось закрыть " + image + ". Возможно, программа не запущена.";
    }

    public String openTaskManager() {
        return startExecutable("taskmgr.exe", "Открываю диспетчер задач.");
    }

    public String openBrowserTarget(String input) {
        String target = extractBrowserTarget(input);
        if (target.isBlank()) {
            target = "https://google.com";
        } else if (looksLikeUrl(target)) {
            target = ensureUrlScheme(target);
        } else {
            target = "https://www.google.com/search?q=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(target));
                return "Открыл в браузере: " + target;
            }
            return runCommand(List.of("cmd.exe", "/c", "start", "", target), DEFAULT_TIMEOUT_SECONDS).success()
                ? "Открыл в браузере: " + target
                : "Не удалось открыть браузер.";
        } catch (Exception e) {
            return "Не удалось открыть браузер: " + e.getMessage();
        }
    }

    public String browserNewTab() {
        return sendShortcut("Открыл новую вкладку.", KeyEvent.VK_CONTROL, KeyEvent.VK_T);
    }

    public String browserCloseTab() {
        return sendShortcut("Закрыл текущую вкладку.", KeyEvent.VK_CONTROL, KeyEvent.VK_W);
    }

    public String browserNextTab() {
        return sendShortcut("Перешел на следующую вкладку.", KeyEvent.VK_CONTROL, KeyEvent.VK_TAB);
    }

    public String browserPreviousTab() {
        return sendShortcut("Перешел на предыдущую вкладку.", KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
    }

    public String browserRefreshTab() {
        return sendShortcut("Обновил текущую вкладку.", KeyEvent.VK_CONTROL, KeyEvent.VK_R);
    }

    public String browserFocusAddress() {
        return sendShortcut("Адресная строка активна.", KeyEvent.VK_CONTROL, KeyEvent.VK_L);
    }

    public String pasteTextToActiveWindow(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            return sendShortcut("Вставил текст.", KeyEvent.VK_CONTROL, KeyEvent.VK_V);
        } catch (Exception e) {
            return "Не удалось вставить текст: " + e.getMessage();
        }
    }

    public String setWifi(boolean enabled) {
        String action = enabled ? "Enable-NetAdapter" : "Disable-NetAdapter";
        String script = "$adapters = Get-NetAdapter | Where-Object { " +
            "$_.Name -match 'Wi-Fi|Wireless' -or $_.InterfaceDescription -match 'Wi-Fi|Wireless|802.11' }; " +
            "if (!$adapters) { Write-Output 'Wi-Fi adapter not found'; exit 3 }; " +
            "$adapters | " + action + " -Confirm:$false";

        CommandResult result = runPowerShell(script);
        if (result.success()) {
            return enabled ? "Wi-Fi включен." : "Wi-Fi выключен.";
        }

        return "Не удалось изменить Wi-Fi. Часто для этого нужны права администратора. Могу открыть настройки Wi-Fi.";
    }

    public String setBluetooth(boolean enabled) {
        String action = enabled ? "Enable-PnpDevice" : "Disable-PnpDevice";
        String script = "$devices = Get-PnpDevice -Class Bluetooth -ErrorAction SilentlyContinue | " +
            "Where-Object { $_.FriendlyName -match 'Bluetooth|Wireless' }; " +
            "if (!$devices) { Write-Output 'Bluetooth device not found'; exit 3 }; " +
            "$devices | " + action + " -Confirm:$false";

        CommandResult result = runPowerShell(script);
        if (result.success()) {
            return enabled ? "Bluetooth включен." : "Bluetooth выключен.";
        }

        return "Не удалось изменить Bluetooth. Часто для этого нужны права администратора. Могу открыть настройки Bluetooth.";
    }

    public String openWifiSettings() {
        return openSettingsUri("ms-settings:network-wifi", "Открыл настройки Wi-Fi.");
    }

    public String openBluetoothSettings() {
        return openSettingsUri("ms-settings:bluetooth", "Открыл настройки Bluetooth.");
    }

    private String startExecutable(String executable, String successMessage) {
        try {
            new ProcessBuilder(executable).start();
            return successMessage;
        } catch (IOException e) {
            return "Не удалось запустить " + executable + ": " + e.getMessage();
        }
    }

    private Optional<AppProfile> findApp(String input) {
        for (AppProfile app : apps) {
            for (String alias : app.aliases()) {
                if (input.contains(alias.toLowerCase(Locale.ROOT))) {
                    return Optional.of(app);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Path> extractExplicitExecutable(String input) {
        String cleaned = stripQuotes(input.trim());
        if (!cleaned.toLowerCase(Locale.ROOT).endsWith(".exe")) {
            return Optional.empty();
        }

        Path path = Path.of(cleaned).toAbsolutePath().normalize();
        if (Files.isRegularFile(path)) {
            return Optional.of(path);
        }
        return Optional.empty();
    }

    private String sendShortcut(String successMessage, int... keys) {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(35);

            for (int key : keys) {
                robot.keyPress(key);
            }
            for (int i = keys.length - 1; i >= 0; i--) {
                robot.keyRelease(keys[i]);
            }

            return successMessage;
        } catch (Exception e) {
            return "Не удалось отправить клавиши: " + e.getMessage();
        }
    }

    private String openSettingsUri(String uri, String successMessage) {
        CommandResult result = runCommand(List.of("cmd.exe", "/c", "start", "", uri), DEFAULT_TIMEOUT_SECONDS);
        return result.success() ? successMessage : "Не удалось открыть настройки Windows.";
    }

    private CommandResult runPowerShell(String script) {
        return runCommand(List.of(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            script
        ), DEFAULT_TIMEOUT_SECONDS);
    }

    private CommandResult runCommand(List<String> command, int timeoutSeconds) {
        try {
            Process process = new ProcessBuilder(new ArrayList<>(command))
                .redirectErrorStream(true)
                .start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(false, -1, "Timeout");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue() == 0, process.exitValue(), output);
        } catch (Exception e) {
            return new CommandResult(false, -1, e.getMessage());
        }
    }

    private String extractBrowserTarget(String input) {
        String text = normalize(input);
        String[] markers = {
            "открой вкладку",
            "новая вкладка",
            "открой сайт",
            "перейди на",
            "браузер",
            "вкладка"
        };

        for (String marker : markers) {
            int index = text.indexOf(marker);
            if (index >= 0) {
                return stripQuotes(text.substring(index + marker.length()).trim());
            }
        }

        return "";
    }

    private boolean looksLikeUrl(String value) {
        String text = value.toLowerCase(Locale.ROOT);
        return text.startsWith("http://")
            || text.startsWith("https://")
            || text.contains(".com")
            || text.contains(".ru")
            || text.contains(".org")
            || text.contains(".net")
            || text.contains(".kz");
    }

    private String ensureUrlScheme(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }

    private String stripQuotes(String value) {
        String result = value.trim();
        while ((result.startsWith("\"") && result.endsWith("\"")) ||
               (result.startsWith("'") && result.endsWith("'"))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }
}
