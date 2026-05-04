package com.assistant.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ComputerAgentService {
    private static final int MAX_BATCH_FILES = 80;
    private static final int MAX_PREVIEW_FILES = 8;
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private final Path userHome;
    private final Path desktop;
    private final Path downloads;
    private final Path documents;
    private final Path projectDir;

    public ComputerAgentService() {
        this.userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        this.desktop = userHome.resolve("Desktop").toAbsolutePath().normalize();
        this.downloads = userHome.resolve("Downloads").toAbsolutePath().normalize();
        this.documents = userHome.resolve("Documents").toAbsolutePath().normalize();
        this.projectDir = Path.of("").toAbsolutePath().normalize();
    }

    public String execute(String input) {
        String text = normalize(input);
        if (text.isBlank() || containsAny(text, "что умеешь", "помощь", "help", "возможности")) {
            return capabilities();
        }

        if (isBrowserAutonomyRequest(text)) {
            return """
                Я поняла задачу как браузерного агента.
                Сейчас я могу открыть сайт, искать информацию и управлять вкладками. Полное заполнение форм, бронирования и покупки лучше добавить отдельным модулем на Playwright или Selenium, чтобы я видела страницу и нажимала элементы надежно.
                Можем следующим шагом подключить такой browser-agent.
                """.trim();
        }

        List<String> extensions = extractExtensions(text);
        Path location = extractLocation(text).orElse(downloads);

        if (!isUserDirectory(location)) {
            return "Я работаю только с пользовательскими папками: Рабочий стол, Загрузки и Документы.";
        }

        if (containsAny(text, "разложи", "организуй", "сортируй", "рассортируй") && containsAny(text, "загрузк", "downloads")) {
            return organizeDownloadsByType();
        }

        if (containsAny(text, "переимен", "переименуй", "rename") && containsAny(text, "дат", "дате", "date")) {
            String extension = extensions.isEmpty() ? "pdf" : extensions.get(0);
            return renameFilesByDate(location, extension);
        }

        if (containsAny(text, "найди", "покажи", "список", "найти", "find") && !extensions.isEmpty()) {
            return findFiles(location, extensions);
        }

        return """
            Я могу выполнить агентную задачу, если она относится к файлам или простому браузеру.
            Примеры:
            агент найди все PDF в загрузках
            агент найди все PDF в загрузках и переименуй их по дате
            агент разложи загрузки по типам
            """.trim();
    }

    private String findFiles(Path location, List<String> extensions) {
        try {
            List<Path> files = findMatchingFiles(location, extensions);
            if (files.isEmpty()) {
                return "Не нашла такие файлы " + describeLocation(location) + ".";
            }

            StringBuilder result = new StringBuilder();
            result.append("Нашла ").append(files.size()).append(" файл").append(wordEnding(files.size()))
                .append(" ").append(describeLocation(location)).append(":\n");

            for (int i = 0; i < Math.min(files.size(), MAX_PREVIEW_FILES); i++) {
                result.append(i + 1).append(". ").append(files.get(i).getFileName()).append("\n");
            }
            if (files.size() > MAX_PREVIEW_FILES) {
                result.append("И еще ").append(files.size() - MAX_PREVIEW_FILES).append(" файл").append(wordEnding(files.size() - MAX_PREVIEW_FILES)).append(".");
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Не смогла просмотреть файлы. Возможно, нет доступа к папке.";
        }
    }

    private String renameFilesByDate(Path location, String extension) {
        try {
            List<Path> files = findMatchingFiles(location, List.of(extension));
            if (files.isEmpty()) {
                return "Не нашла ." + extension + " файлы " + describeLocation(location) + ".";
            }

            int renamed = 0;
            int skipped = 0;
            List<String> examples = new ArrayList<>();

            for (Path file : files.subList(0, Math.min(files.size(), MAX_BATCH_FILES))) {
                String name = file.getFileName().toString();
                if (name.matches("^\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}_.+")) {
                    skipped++;
                    continue;
                }

                String prefix = formatModifiedTime(file);
                Path target = uniquePath(file.getParent().resolve(prefix + "_" + name));
                Files.move(file, target);
                renamed++;

                if (examples.size() < 3) {
                    examples.add(target.getFileName().toString());
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("Готово, переименовала ").append(renamed).append(" .").append(extension)
                .append(" файл").append(wordEnding(renamed)).append(" ").append(describeLocation(location)).append(".");
            if (skipped > 0) {
                result.append(" ").append(skipped).append(" уже были с датой, их не трогала.");
            }
            if (!examples.isEmpty()) {
                result.append("\nПример: ").append(String.join(", ", examples));
            }
            if (files.size() > MAX_BATCH_FILES) {
                result.append("\nОстановилась на первых ").append(MAX_BATCH_FILES).append(" файлах, чтобы не менять слишком много за раз.");
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Не смогла переименовать файлы. Возможно, один из них открыт или нет доступа.";
        }
    }

    private String organizeDownloadsByType() {
        if (!Files.isDirectory(downloads)) {
            return "Папка Загрузки не найдена.";
        }

        Map<String, Integer> movedByGroup = new LinkedHashMap<>();
        int moved = 0;

        try (Stream<Path> stream = Files.list(downloads)) {
            List<Path> files = stream
                .filter(Files::isRegularFile)
                .limit(MAX_BATCH_FILES)
                .toList();

            for (Path file : files) {
                String folder = groupFor(file);
                Path destinationDir = downloads.resolve(folder);
                Files.createDirectories(destinationDir);
                Path destination = uniquePath(destinationDir.resolve(file.getFileName()));
                Files.move(file, destination);
                moved++;
                movedByGroup.merge(folder, 1, Integer::sum);
            }
        } catch (Exception e) {
            return "Не смогла разложить загрузки. Возможно, часть файлов открыта или нет доступа.";
        }

        if (moved == 0) {
            return "В Загрузках не нашла файлов для сортировки.";
        }

        StringBuilder result = new StringBuilder("Готово, разложила ").append(moved)
            .append(" файл").append(wordEnding(moved)).append(" в Загрузках:\n");
        movedByGroup.forEach((group, count) -> result.append("- ").append(group).append(": ").append(count).append("\n"));
        return result.toString().trim();
    }

    private List<Path> findMatchingFiles(Path location, List<String> extensions) throws IOException {
        if (!Files.isDirectory(location)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.walk(location, 5)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> matchesExtension(path, extensions))
                .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                .limit(MAX_BATCH_FILES)
                .toList();
        }
    }

    private List<String> extractExtensions(String text) {
        List<String> extensions = new ArrayList<>();
        addExtensionIfMentioned(extensions, text, "pdf", "pdf", "пдф");
        addExtensionIfMentioned(extensions, text, "docx", "docx", "word", "ворд");
        addExtensionIfMentioned(extensions, text, "txt", "txt", "текстов");
        addExtensionIfMentioned(extensions, text, "jpg", "jpg", "jpeg");
        addExtensionIfMentioned(extensions, text, "png", "png");
        addExtensionIfMentioned(extensions, text, "zip", "zip", "архив");
        addExtensionIfMentioned(extensions, text, "xlsx", "xlsx", "excel", "эксель");
        return extensions;
    }

    private void addExtensionIfMentioned(List<String> extensions, String text, String extension, String... markers) {
        for (String marker : markers) {
            if (text.contains(marker)) {
                extensions.add(extension);
                return;
            }
        }
    }

    private Optional<Path> extractLocation(String text) {
        if (containsAny(text, "загрузк", "downloads")) {
            return Optional.of(downloads);
        }
        if (containsAny(text, "документ", "documents")) {
            return Optional.of(documents);
        }
        if (containsAny(text, "рабоч", "desktop")) {
            return Optional.of(desktop);
        }
        if (containsAny(text, "проект", "project", "текущей папк", "текущая папк")) {
            return Optional.of(projectDir);
        }
        return Optional.empty();
    }

    private boolean matchesExtension(Path path, List<String> extensions) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (name.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }

    private String groupFor(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.matches(".*\\.(pdf)$")) return "PDF";
        if (name.matches(".*\\.(doc|docx|txt|rtf|odt)$")) return "Documents";
        if (name.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|svg)$")) return "Images";
        if (name.matches(".*\\.(zip|rar|7z|tar|gz)$")) return "Archives";
        if (name.matches(".*\\.(mp3|wav|flac|m4a|ogg)$")) return "Audio";
        if (name.matches(".*\\.(mp4|mov|avi|mkv|webm)$")) return "Video";
        if (name.matches(".*\\.(exe|msi|bat|cmd)$")) return "Apps";
        return "Other";
    }

    private boolean isBrowserAutonomyRequest(String text) {
        return containsAny(text, "заполни", "форма", "форму", "бронируй", "забронируй", "билет", "купи", "оформи заказ");
    }

    private boolean isUserDirectory(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return normalized.startsWith(userHome) && Files.isDirectory(normalized);
    }

    private Path uniquePath(Path path) {
        if (!Files.exists(path)) {
            return path;
        }

        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot > 0 ? fileName.substring(dot) : "";
        Path parent = path.getParent();
        int counter = 1;
        Path candidate;
        do {
            candidate = parent.resolve(base + "_" + counter + extension);
            counter++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private String formatModifiedTime(Path file) throws IOException {
        FileTime time = Files.getLastModifiedTime(file);
        LocalDateTime local = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
        return FILE_DATE.format(local);
    }

    private Instant lastModifiedSafe(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private String describeLocation(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (equalsIgnoreCase(normalized, downloads)) return "в Загрузках";
        if (equalsIgnoreCase(normalized, documents)) return "в Документах";
        if (equalsIgnoreCase(normalized, desktop)) return "на рабочем столе";
        if (equalsIgnoreCase(normalized, projectDir)) return "в папке проекта";
        if (normalized.startsWith(downloads)) return "в папке Загрузок";
        if (normalized.startsWith(documents)) return "в папке Документов";
        if (normalized.startsWith(desktop)) return "в папке рабочего стола";
        if (normalized.startsWith(projectDir)) return "в папке проекта";
        return "в выбранной папке";
    }

    private String wordEnding(int count) {
        int last = Math.abs(count) % 10;
        int lastTwo = Math.abs(count) % 100;
        if (last == 1 && lastTwo != 11) return "";
        if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) return "а";
        return "ов";
    }

    private boolean equalsIgnoreCase(Path first, Path second) {
        return first.toString().equalsIgnoreCase(second.toString());
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        return input == null
            ? ""
            : input.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[?!,;:()\\[\\]{}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String capabilities() {
        return """
            Включила безопасный agent mode.
            Уже умею:
            - найти файлы по типу в Загрузках, Документах, проекте или на рабочем столе;
            - переименовать PDF по дате изменения;
            - разложить Загрузки по типам: PDF, Documents, Images, Archives, Audio, Video, Apps.

            Примеры:
            агент найди все PDF в загрузках
            агент найди все PDF в загрузках и переименуй их по дате
            агент разложи загрузки по типам
            """.trim();
    }
}
