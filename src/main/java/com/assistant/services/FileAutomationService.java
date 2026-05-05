package com.assistant.services;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileAutomationService {
    private static final Pattern QUOTED_VALUE = Pattern.compile("\"([^\"]+)\"|'([^']+)'");
    private static final Pattern TEXT_MARKER = Pattern.compile("(?iu)\\s+с\\s+текстом\\s+(.+)$");
    private static final DateTimeFormatter TRASH_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path userHome;
    private final Path defaultBase;
    private final Path trashDir;
    private final List<Path> protectedRoots;

    public FileAutomationService() {
        this.userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path desktop = userHome.resolve("Desktop");
        this.defaultBase = Files.isDirectory(desktop) ? desktop : userHome;
        this.trashDir = userHome.resolve(".aura-trash").toAbsolutePath().normalize();
        this.protectedRoots = buildProtectedRoots();
    }

    public String execute(String input) {
        String lower = normalize(input);

        if (lower.contains("открой") || lower.contains("покажи") || lower.contains("запусти")) {
            return open(input);
        }
        if (lower.contains("перемести") || lower.contains("перенеси") || lower.contains("move")) {
            return move(input);
        }
        if (lower.contains("удали") || lower.contains("удалить") || lower.contains("сотри") || lower.contains("убери")) {
            return deleteToAuraTrash(input);
        }
        if (lower.contains("создай") || lower.contains("создать") || lower.contains("сделай") || lower.contains("новый") || lower.contains("новую")) {
            return create(input);
        }

        return "Команды файлов: создай файл notes.txt, создай папку project, удали файл notes.txt, перемести \"a.txt\" в \"folder\".";
    }

    private String create(String input) {
        String lower = normalize(input);
        boolean folder = lower.contains("папк") || lower.contains("директор");
        String targetText = input.replaceFirst("(?iu)^.*?(создай|создать|сделай|новый|новую)\\s+(файл|папку|папка|директорию)?\\s*", "").trim();
        String content = "";

        Matcher textMatcher = TEXT_MARKER.matcher(targetText);
        if (textMatcher.find()) {
            content = stripQuotes(textMatcher.group(1).trim());
            targetText = targetText.substring(0, textMatcher.start()).trim();
        }

        String pathText = firstQuotedValue(targetText).orElse(targetText);
        if (pathText.isBlank()) {
            return folder
                ? "Укажите имя папки. Пример: создай папку project."
                : "Укажите имя файла. Пример: создай файл notes.txt.";
        }

        try {
            Path path = resolveUserPath(pathText);
            if (!isSafeForWrite(path)) {
                return "Не буду создавать файлы в системных папках Windows.";
            }

            if (folder) {
                Files.createDirectories(path);
                return openPath(path)
                    ? "Готово, создала папку " + describePath(path) + " и открыла её на экране."
                    : "Готово, создала папку " + describePath(path) + ".";
            }

            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(path)) {
                return "Файл " + describePath(path) + " уже есть.";
            }

            Files.writeString(path, content, StandardCharsets.UTF_8);
            return openPath(path)
                ? "Готово, создала файл " + describePath(path) + " и открыла его на экране."
                : "Готово, создала файл " + describePath(path) + ".";
        } catch (Exception e) {
            return "Не смогла создать. Проверь имя и доступ к папке.";
        }
    }

    private String open(String input) {
        String targetText = input.replaceFirst("(?iu)^.*?(открой|покажи|запусти)\\s+(файл|папку|папка|директорию|документ)?\\s*", "").trim();
        String pathText = firstQuotedValue(targetText).orElse(targetText);
        pathText = pathText.replaceAll("(?iu)\\b(на\\s+экране|на\\s+главном\\s+экране|пожалуйста)\\b", " ").trim();

        if (pathText.isBlank()) {
            return "Скажи, что открыть. Например: открой файл notes.txt или открой папку Документы.";
        }

        try {
            Path path = resolveUserPath(pathText);
            if (!Files.exists(path)) {
                path = findByName(pathText).orElse(path);
            }
            if (!Files.exists(path)) {
                return "Не нашла " + describePath(path) + ". Проверь имя файла или папки.";
            }
            if (!isSafeForWrite(path)) {
                return "Не буду открывать системные папки Windows.";
            }

            return openPath(path)
                ? "Открыла " + describePath(path) + " на экране."
                : "Нашла " + describePath(path) + ", но Windows не смог открыть автоматически.";
        } catch (Exception e) {
            return "Не смогла открыть. Проверь имя и доступ к файлу.";
        }
    }

    private String deleteToAuraTrash(String input) {
        String targetText = input.replaceFirst("(?iu)^.*?(удали|удалить|сотри|убери)\\s+(файл|папку|папка|директорию)?\\s*", "").trim();
        String pathText = firstQuotedValue(targetText).orElse(targetText);

        if (pathText.isBlank()) {
            return "Укажите файл или папку для удаления. Пример: удали файл notes.txt.";
        }

        try {
            Path path = resolveUserPath(pathText);
            if (!Files.exists(path)) {
                return "Не нашла " + describePath(path) + ". Проверь имя файла или папки.";
            }
            if (!isSafeForDelete(path)) {
                return "Не буду удалять системные папки, корневые папки и важные пользовательские директории.";
            }

            Files.createDirectories(trashDir);
            Path trashTarget = uniqueTrashTarget(path);
            Files.move(path, trashTarget, StandardCopyOption.REPLACE_EXISTING);
            return "Готово, перенесла " + describePath(path) + " в безопасную корзину AURA.";
        } catch (Exception e) {
            return "Не смогла удалить. Возможно, файл открыт или нет доступа.";
        }
    }

    private String move(String input) {
        List<String> quoted = quotedValues(input);
        String sourceText;
        String destinationText;

        if (quoted.size() >= 2) {
            sourceText = quoted.get(0);
            destinationText = quoted.get(1);
        } else {
            String rest = input.replaceFirst("(?iu)^.*?(перемести|перенеси|move)\\s+", "").trim();
            Matcher matcher = Pattern.compile("(?iu)^(.+?)\\s+(?:в|to)\\s+(.+)$").matcher(rest);
            if (!matcher.find()) {
                return "Укажите откуда и куда переместить. Пример: перемести \"a.txt\" в \"archive\".";
            }
            sourceText = matcher.group(1).trim();
            destinationText = matcher.group(2).trim();
        }

        try {
            Path source = resolveUserPath(sourceText);
            Path destination = resolveUserPath(destinationText);

            if (!Files.exists(source)) {
                return "Не нашла " + describePath(source) + ". Проверь имя исходного файла.";
            }
            if (!isSafeForWrite(source) || !isSafeForWrite(destination)) {
                return "Не буду перемещать файлы в системные папки Windows.";
            }
            if (Files.isDirectory(destination)) {
                destination = destination.resolve(source.getFileName()).normalize();
            }
            if (Files.exists(destination)) {
                return "Там уже есть " + describePath(destination) + ".";
            }

            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.move(source, destination);
            return "Готово, перенесла " + describePath(source) + " " + describeLocation(destination.getParent()) + ".";
        } catch (Exception e) {
            return "Не смогла переместить. Проверь, что файл не открыт и есть доступ к папке.";
        }
    }

    private Path resolveUserPath(String rawValue) {
        String raw = stripQuotes(rawValue.trim());
        raw = raw.replace("%USERPROFILE%", userHome.toString());
        raw = raw.replace("%userprofile%", userHome.toString());

        String lower = normalize(raw);
        if (lower.startsWith("рабочий стол")) {
            raw = raw.substring("рабочий стол".length()).replaceFirst("^[\\\\/ ]+", "");
            return defaultBase.resolve(raw).toAbsolutePath().normalize();
        }
        if (lower.startsWith("desktop")) {
            raw = raw.substring("desktop".length()).replaceFirst("^[\\\\/ ]+", "");
            return defaultBase.resolve(raw).toAbsolutePath().normalize();
        }
        if (lower.startsWith("документы")) {
            raw = raw.substring("документы".length()).replaceFirst("^[\\\\/ ]+", "");
            return userHome.resolve("Documents").resolve(raw).toAbsolutePath().normalize();
        }
        if (lower.startsWith("загрузки")) {
            raw = raw.substring("загрузки".length()).replaceFirst("^[\\\\/ ]+", "");
            return userHome.resolve("Downloads").resolve(raw).toAbsolutePath().normalize();
        }
        if (raw.startsWith("~")) {
            raw = userHome + raw.substring(1);
        }

        Path path = Path.of(raw);
        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        return defaultBase.resolve(path).toAbsolutePath().normalize();
    }

    private boolean isSafeForWrite(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.getParent() == null) {
            return false;
        }
        return protectedRoots.stream().noneMatch(root -> startsWithIgnoreCase(normalized, root));
    }

    private boolean isSafeForDelete(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!isSafeForWrite(normalized)) {
            return false;
        }
        if (equalsIgnoreCase(normalized, userHome) || equalsIgnoreCase(normalized, defaultBase) || equalsIgnoreCase(normalized, trashDir)) {
            return false;
        }
        if (equalsIgnoreCase(normalized, userHome.resolve("Documents")) || equalsIgnoreCase(normalized, userHome.resolve("Downloads"))) {
            return false;
        }
        return normalized.getNameCount() >= userHome.getNameCount() + 1;
    }

    private Path uniqueTrashTarget(Path original) throws IOException {
        String timestamp = LocalDateTime.now().format(TRASH_FORMAT);
        String fileName = original.getFileName().toString();
        Path target = trashDir.resolve(timestamp + "_" + fileName);
        int counter = 1;
        while (Files.exists(target)) {
            target = trashDir.resolve(timestamp + "_" + counter + "_" + fileName);
            counter++;
        }
        return target;
    }

    private java.util.Optional<Path> findByName(String query) {
        String normalizedQuery = normalizeFileQuery(query);
        if (normalizedQuery.isBlank()) {
            return java.util.Optional.empty();
        }

        Path[] roots = {
            defaultBase,
            userHome.resolve("Documents"),
            userHome.resolve("Downloads")
        };

        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }

            try (Stream<Path> stream = Files.walk(root, 3)) {
                java.util.Optional<Path> found = stream
                    .filter(path -> normalizeFileQuery(path.getFileName() == null ? "" : path.getFileName().toString()).contains(normalizedQuery))
                    .max(Comparator.comparingLong(this::lastModifiedSafe));
                if (found.isPresent()) {
                    return found;
                }
            } catch (Exception ignored) {
            }
        }

        return java.util.Optional.empty();
    }

    private boolean openPath(Path path) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile());
                return true;
            }

            new ProcessBuilder("cmd.exe", "/c", "start", "", path.toAbsolutePath().toString()).start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Path> buildProtectedRoots() {
        List<Path> roots = new ArrayList<>();
        addIfPresent(roots, System.getenv("SystemRoot"));
        addIfPresent(roots, "C:\\Windows");
        addIfPresent(roots, "C:\\Program Files");
        addIfPresent(roots, "C:\\Program Files (x86)");
        addIfPresent(roots, "C:\\ProgramData");
        return roots;
    }

    private void addIfPresent(List<Path> roots, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        roots.add(Path.of(value).toAbsolutePath().normalize());
    }

    private java.util.Optional<String> firstQuotedValue(String input) {
        List<String> values = quotedValues(input);
        return values.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(values.get(0));
    }

    private List<String> quotedValues(String input) {
        List<String> values = new ArrayList<>();
        Matcher matcher = QUOTED_VALUE.matcher(input);
        while (matcher.find()) {
            values.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }
        return values;
    }

    private boolean startsWithIgnoreCase(Path value, Path prefix) {
        return value.toString().toLowerCase(Locale.ROOT).startsWith(prefix.toString().toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(Path a, Path b) {
        return a.toString().equalsIgnoreCase(b.toString());
    }

    private String describePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path fileName = normalized.getFileName();
        String name = fileName == null ? "элемент" : fileName.toString();
        return "«" + name + "» " + describeLocation(normalized.getParent());
    }

    private String describeLocation(Path parent) {
        if (parent == null) {
            return "в выбранном месте";
        }

        Path normalized = parent.toAbsolutePath().normalize();
        Path documents = userHome.resolve("Documents").toAbsolutePath().normalize();
        Path downloads = userHome.resolve("Downloads").toAbsolutePath().normalize();

        if (equalsIgnoreCase(normalized, defaultBase)) {
            return "на рабочем столе";
        }
        if (startsWithIgnoreCase(normalized, defaultBase)) {
            return "в папке " + describeChildFolder(defaultBase, normalized);
        }
        if (equalsIgnoreCase(normalized, documents)) {
            return "в Документах";
        }
        if (startsWithIgnoreCase(normalized, documents)) {
            return "в папке " + describeChildFolder(documents, normalized);
        }
        if (equalsIgnoreCase(normalized, downloads)) {
            return "в Загрузках";
        }
        if (startsWithIgnoreCase(normalized, downloads)) {
            return "в папке " + describeChildFolder(downloads, normalized);
        }
        if (equalsIgnoreCase(normalized, trashDir) || startsWithIgnoreCase(normalized, trashDir)) {
            return "в безопасной корзине AURA";
        }
        if (startsWithIgnoreCase(normalized, userHome)) {
            return "в вашей папке пользователя";
        }
        return "в выбранной папке";
    }

    private String describeChildFolder(Path base, Path value) {
        try {
            Path relative = base.relativize(value);
            if (relative.getNameCount() > 0) {
                return "«" + relative.getName(0) + "»";
            }
        } catch (Exception ignored) {
        }
        return "«выбранная»";
    }

    private String stripQuotes(String value) {
        String result = value.trim();
        while ((result.startsWith("\"") && result.endsWith("\"")) ||
               (result.startsWith("'") && result.endsWith("'"))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private String normalizeFileQuery(String value) {
        return value == null ? "" : value
            .toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .replaceAll("[^a-zа-я0-9._-]+", "")
            .trim();
    }

    private long lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }
}
