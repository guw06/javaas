package com.assistant.services;

import com.assistant.utils.SafeFileWalker;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class FileSearchService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Path userHome;
    private final Path desktop;
    private final List<Path> roots;
    private List<SearchResult> lastResults = List.of();

    private record SearchResult(Path path, int score, long modifiedAt) {
    }

    public FileSearchService() {
        this.userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        this.desktop = userHome.resolve("Desktop");
        this.roots = List.of(
            Files.isDirectory(desktop) ? desktop : userHome,
            userHome.resolve("Documents"),
            userHome.resolve("Downloads")
        );
    }

    public String handle(String input) {
        String text = normalize(input);
        boolean open = containsAny(text, "открой", "запусти", "покажи на экране");
        boolean latest = containsAny(text, "последний", "последнюю", "последнее", "новый", "свежий");
        String query = extractQuery(input);

        if (open && containsAny(text, "найденный", "первый результат", "первый файл") && !lastResults.isEmpty()) {
            SearchResult result = lastResults.get(0);
            return openPath(result.path())
                ? "Открыла " + describe(result.path(), result.modifiedAt()) + "."
                : "Нашла " + describe(result.path(), result.modifiedAt()) + ", но Windows не смог открыть автоматически.";
        }

        List<SearchResult> results = search(query, text, latest, 8);
        if (results.isEmpty()) {
            return "Я поискала на рабочем столе, в Документах и Загрузках, но ничего похожего не нашла.";
        }
        lastResults = results;

        SearchResult first = results.get(0);
        if (open) {
            return openPath(first.path())
                ? "Нашла и открыла " + describe(first.path(), first.modifiedAt()) + "."
                : "Нашла " + describe(first.path(), first.modifiedAt()) + ", но Windows не смог открыть автоматически.";
        }

        StringBuilder answer = new StringBuilder("Нашла похожие файлы:\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            answer.append(i + 1).append(". ").append(describe(result.path(), result.modifiedAt())).append("\n");
        }
        answer.append("\nЧтобы открыть первый результат, скажи: открой найденный файл.");
        return answer.toString().trim();
    }

    public List<String> quickSearch(String query, int limit) {
        return search(query, normalize(query), false, limit).stream()
            .map(result -> describe(result.path(), result.modifiedAt()))
            .toList();
    }

    private List<SearchResult> search(String query, String fullText, boolean latest, int limit) {
        Set<String> terms = queryTerms(query);
        Set<String> wantedExtensions = wantedExtensions(fullText);
        boolean includeDirectories = containsAny(fullText, "папк", "директор");
        List<SearchResult> results = new ArrayList<>();

        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }

            try (Stream<Path> stream = SafeFileWalker.walk(root, 5)) {
                stream
                    .filter(path -> includeDirectories || Files.isRegularFile(path))
                    .map(path -> score(path, terms, wantedExtensions, latest))
                    .filter(result -> result.score() > 0 || latest)
                    .forEach(results::add);
            } catch (Exception ignored) {
            }
        }

        Comparator<SearchResult> comparator = Comparator
            .comparingInt(SearchResult::score)
            .thenComparingLong(SearchResult::modifiedAt)
            .reversed();

        if (latest && terms.isEmpty()) {
            comparator = Comparator.comparingLong(SearchResult::modifiedAt).reversed();
        }

        return results.stream()
            .sorted(comparator)
            .limit(limit)
            .toList();
    }

    private SearchResult score(Path path, Set<String> terms, Set<String> wantedExtensions, boolean latest) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        String normalizedName = normalize(fileName);
        String extension = extension(fileName);
        int score = 0;

        if (wantedExtensions.isEmpty() || wantedExtensions.contains(extension)) {
            score += wantedExtensions.isEmpty() ? 4 : 35;
        } else {
            score -= 30;
        }

        for (String term : terms) {
            if (normalizedName.contains(term)) {
                score += 25;
            }
        }

        if (latest) {
            score += 5;
        }

        return new SearchResult(path, score, lastModified(path));
    }

    private Set<String> wantedExtensions(String text) {
        Set<String> extensions = new LinkedHashSet<>();
        if (containsAny(text, "word", "ворд", "docx", "документ")) {
            extensions.add("docx");
            extensions.add("doc");
        }
        if (containsAny(text, "pdf", "пдф")) {
            extensions.add("pdf");
        }
        if (containsAny(text, "таблиц", "excel", "xlsx")) {
            extensions.add("xlsx");
            extensions.add("xls");
        }
        if (containsAny(text, "картин", "фото", "скрин", "изображ")) {
            extensions.add("png");
            extensions.add("jpg");
            extensions.add("jpeg");
        }
        if (containsAny(text, "текст", "txt", "заметк")) {
            extensions.add("txt");
            extensions.add("md");
        }
        return extensions;
    }

    private Set<String> queryTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        for (String part : normalize(query).split("\\s+")) {
            if (part.length() >= 3 && !isStopWord(part)) {
                terms.add(part);
            }
        }
        return terms;
    }

    private String extractQuery(String input) {
        return normalize(input)
            .replaceAll("\\b(найди|поищи|найти|покажи|открой|запусти|последний|последнюю|последнее|свежий|новый|файл|документ|папку|папка|про|о|на|экране|найденный)\\b", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean isStopWord(String value) {
        return Set.of("word", "docx", "pdf", "txt", "ворд", "пдф", "документ", "файл", "папка").contains(value);
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

    private String describe(Path path, long modifiedAt) {
        String name = path.getFileName() == null ? "элемент" : path.getFileName().toString();
        return "«" + name + "» " + describeLocation(path.getParent()) + ", изменен " + formatModified(modifiedAt);
    }

    private String describeLocation(Path parent) {
        if (parent == null) {
            return "в выбранном месте";
        }

        Path normalized = parent.toAbsolutePath().normalize();
        Path documents = userHome.resolve("Documents").toAbsolutePath().normalize();
        Path downloads = userHome.resolve("Downloads").toAbsolutePath().normalize();

        if (samePath(normalized, desktop)) {
            return "на рабочем столе";
        }
        if (startsWith(normalized, desktop)) {
            return "на рабочем столе в папке «" + firstChild(desktop, normalized) + "»";
        }
        if (samePath(normalized, documents)) {
            return "в Документах";
        }
        if (startsWith(normalized, documents)) {
            return "в Документах в папке «" + firstChild(documents, normalized) + "»";
        }
        if (samePath(normalized, downloads)) {
            return "в Загрузках";
        }
        if (startsWith(normalized, downloads)) {
            return "в Загрузках в папке «" + firstChild(downloads, normalized) + "»";
        }
        if (startsWith(normalized, userHome)) {
            return "в вашей папке пользователя";
        }
        return "в найденной папке";
    }

    private String firstChild(Path base, Path value) {
        try {
            Path relative = base.relativize(value);
            return relative.getNameCount() == 0 ? "выбранная" : relative.getName(0).toString();
        } catch (Exception e) {
            return "выбранная";
        }
    }

    private String formatModified(long modifiedAt) {
        if (modifiedAt <= 0) {
            return "недавно";
        }
        return Instant.ofEpochMilli(modifiedAt).atZone(ZoneId.systemDefault()).format(DATE_FORMAT);
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean startsWith(Path value, Path prefix) {
        return value.toString().toLowerCase(Locale.ROOT).startsWith(prefix.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT));
    }

    private boolean samePath(Path first, Path second) {
        return first.toString().equalsIgnoreCase(second.toAbsolutePath().normalize().toString());
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
        return input == null ? "" : input
            .toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .replaceAll("[?!,;:\"'()\\[\\]{}]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
