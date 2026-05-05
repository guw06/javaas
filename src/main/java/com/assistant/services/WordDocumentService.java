package com.assistant.services;

import com.assistant.utils.SafeFileWalker;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WordDocumentService {
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"|'([^']+)'");
    private static final Pattern TEXT_MARKER = Pattern.compile("(?iu)\\s+с\\s+текстом\\s+(.+)$");
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final GeminiService geminiService = new GeminiService();
    private final Path userHome;
    private final Path defaultDirectory;

    public WordDocumentService() {
        this.userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path desktop = userHome.resolve("Desktop");
        this.defaultDirectory = Files.isDirectory(desktop) ? desktop : userHome;
    }

    public String createFromCommand(String input) {
        if (isOpenIntent(input) && !isCreateIntent(input)) {
            return openFromCommand(input);
        }

        DocumentRequest request = parse(input);

        if (request.content().isBlank()) {
            if (!request.topic().isBlank()) {
                request = new DocumentRequest(
                    request.fileName(),
                    generateContent(request.topic()),
                    request.topic()
                );
            } else {
                request = new DocumentRequest(
                    request.fileName(),
                    "Документ создан AURA.\n\nДобавьте текст командой: создай Word документ ... с текстом ...",
                    request.topic()
                );
            }
        }

        try {
            Path output = uniquePath(defaultDirectory.resolve(request.fileName()).toAbsolutePath().normalize());
            createDocx(output, request.content());
            boolean opened = openPath(output);
            return opened
                ? "Готово, создала Word-документ " + describePath(output) + " и открыла его на экране."
                : "Готово, создала Word-документ " + describePath(output) + ", но не смогла открыть его автоматически.";
        } catch (Exception e) {
            return "Не смогла создать Word-документ. Проверь, пожалуйста, доступ к папке.";
        }
    }

    private String openFromCommand(String input) {
        Optional<Path> document = findRequestedDocument(input);
        if (document.isEmpty()) {
            return "Не нашла Word-документ для открытия. Скажи имя файла, например: открой документ \"report.docx\".";
        }

        Path path = document.get();
        if (openPath(path)) {
            return "Открыла Word-документ " + describePath(path) + ".";
        }

        return "Нашла Word-документ " + describePath(path) + ", но Windows не смог открыть его автоматически.";
    }

    private Optional<Path> findRequestedDocument(String input) {
        String fileName = firstQuoted(input);
        if (fileName.isBlank()) {
            fileName = extractOpenTarget(input);
        }

        if (fileName.isBlank()) {
            return findMostRecentDocx();
        }

        String normalizedName = fileName.toLowerCase(Locale.ROOT).endsWith(".docx") ? fileName : fileName + ".docx";
        Path rawPath = Path.of(stripQuotes(fileName));
        if (rawPath.isAbsolute() && Files.isRegularFile(rawPath)) {
            return Optional.of(rawPath.toAbsolutePath().normalize());
        }

        Path[] candidates = {
            defaultDirectory.resolve(normalizedName),
            userHome.resolve("Documents").resolve(normalizedName),
            userHome.resolve("Downloads").resolve(normalizedName),
            defaultDirectory.resolve(fileName),
            userHome.resolve("Documents").resolve(fileName),
            userHome.resolve("Downloads").resolve(fileName)
        };

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                return Optional.of(normalized);
            }
        }

        return searchDocxByName(fileName);
    }

    private Optional<Path> searchDocxByName(String query) {
        String normalizedQuery = normalizeFileQuery(query);
        Path[] roots = {defaultDirectory, userHome.resolve("Documents"), userHome.resolve("Downloads")};

        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }

            try (Stream<Path> stream = SafeFileWalker.walk(root, 3)) {
                Optional<Path> found = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".docx"))
                    .filter(path -> normalizeFileQuery(path.getFileName().toString()).contains(normalizedQuery))
                    .max(Comparator.comparingLong(this::lastModifiedSafe));
                if (found.isPresent()) {
                    return found;
                }
            } catch (Exception ignored) {
            }
        }

        return Optional.empty();
    }

    private Optional<Path> findMostRecentDocx() {
        Path[] roots = {defaultDirectory, userHome.resolve("Documents"), userHome.resolve("Downloads")};
        Optional<Path> best = Optional.empty();

        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }

            try (Stream<Path> stream = SafeFileWalker.walk(root, 2)) {
                Optional<Path> candidate = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".docx"))
                    .max(Comparator.comparingLong(this::lastModifiedSafe));
                if (candidate.isPresent() && (best.isEmpty() || lastModifiedSafe(candidate.get()) > lastModifiedSafe(best.get()))) {
                    best = candidate;
                }
            } catch (Exception ignored) {
            }
        }

        return best;
    }

    private String extractOpenTarget(String input) {
        String cleaned = input == null ? "" : input
            .replaceFirst("(?iu)^.*?(открой|покажи|запусти)\\s+", "")
            .replaceAll("(?iu)(^|\\s)(word|ворд|вордовский|документ|файл)(?=\\s|$)", " ")
            .replaceAll("(?iu)на\\s+главном\\s+экране|на\\s+экране", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return stripQuotes(cleaned);
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

    private boolean isOpenIntent(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT).replace('ё', 'е');
        return lower.contains("открой") || lower.contains("покажи") || lower.contains("запусти");
    }

    private boolean isCreateIntent(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT).replace('ё', 'е');
        return lower.contains("создай")
            || lower.contains("создать")
            || lower.contains("сделай")
            || lower.contains("сформируй")
            || lower.contains("подготовь")
            || lower.contains("напиши");
    }

    private DocumentRequest parse(String input) {
        String text = input == null ? "" : input.trim();
        String fileName = firstQuoted(text);
        String content = "";

        Matcher textMatcher = TEXT_MARKER.matcher(text);
        if (textMatcher.find()) {
            content = stripQuotes(textMatcher.group(1).trim());
            text = text.substring(0, textMatcher.start()).trim();
        }

        String topic = extractTopic(text);
        if (fileName.isBlank()) {
            fileName = topic.isBlank() ? "aura_document_" + LocalDateTime.now().format(NAME_TIME) : sanitizeFileName(topic);
        }

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            fileName += ".docx";
        }

        return new DocumentRequest(fileName, content, topic);
    }

    private String generateContent(String topic) {
        String prompt = """
            Напиши аккуратный текст для Word-документа на русском языке.
            Тема: %s
            Структура: заголовок, 3-5 коротких абзацев, вывод.
            Без markdown-символов.
            """.formatted(topic);
        return geminiService.ask(prompt);
    }

    private String extractTopic(String input) {
        String lower = input.toLowerCase(Locale.ROOT).replace('ё', 'е');
        String[] markers = {"на тему", "по теме", "про", "о "};
        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0) {
                return stripQuotes(input.substring(index + marker.length()).trim());
            }
        }
        return "";
    }

    private String firstQuoted(String input) {
        Matcher matcher = QUOTED.matcher(input);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private void createDocx(Path output, String content) throws IOException {
        Files.createDirectories(output.getParent());

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            put(zip, "[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
                """);
            put(zip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
                """);
            put(zip, "word/document.xml", buildDocumentXml(content));
        }
    }

    private String buildDocumentXml(String content) {
        StringBuilder body = new StringBuilder();
        for (String paragraph : content.split("\\R+")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            body.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                .append(escapeXml(trimmed))
                .append("</w:t></w:r></w:p>");
        }

        if (body.isEmpty()) {
            body.append("<w:p><w:r><w:t>Документ создан AURA.</w:t></w:r></w:p>");
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                %s
                <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>
              </w:body>
            </w:document>
            """.formatted(body);
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            input.transferTo(zip);
        }
        zip.closeEntry();
    }

    private Path uniquePath(Path path) {
        if (!Files.exists(path)) {
            return path;
        }

        String fileName = path.getFileName().toString();
        String base = fileName.replaceFirst("(?i)\\.docx$", "");
        Path parent = path.getParent();
        int counter = 1;
        Path candidate;
        do {
            candidate = parent.resolve(base + "_" + counter + ".docx");
            counter++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private String sanitizeFileName(String value) {
        String name = value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-zа-я0-9._ -]", "")
            .replaceAll("\\s+", "_")
            .replaceAll("_+", "_")
            .trim();

        if (name.isBlank()) {
            return "aura_document_" + LocalDateTime.now().format(NAME_TIME);
        }
        return name.length() > 50 ? name.substring(0, 50) : name;
    }

    private String normalizeFileQuery(String value) {
        return value == null ? "" : value
            .toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .replaceFirst("(?i)\\.docx$", "")
            .replaceAll("[^a-zа-я0-9]+", "")
            .trim();
    }

    private long lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private String describePath(Path path) {
        String name = path.getFileName() == null ? "документ" : path.getFileName().toString();
        return "«" + name + "» " + describeLocation(path.getParent());
    }

    private String describeLocation(Path parent) {
        if (parent == null) {
            return "в выбранной папке";
        }

        Path normalized = parent.toAbsolutePath().normalize();
        Path desktop = Path.of(System.getProperty("user.home")).resolve("Desktop").toAbsolutePath().normalize();
        Path documents = Path.of(System.getProperty("user.home")).resolve("Documents").toAbsolutePath().normalize();

        if (normalized.toString().equalsIgnoreCase(desktop.toString()) || normalized.startsWith(desktop)) {
            return "на рабочем столе";
        }
        if (normalized.toString().equalsIgnoreCase(documents.toString()) || normalized.startsWith(documents)) {
            return "в Документах";
        }
        return "в выбранной папке";
    }

    private String stripQuotes(String value) {
        String result = value.trim();
        while ((result.startsWith("\"") && result.endsWith("\"")) ||
               (result.startsWith("'") && result.endsWith("'"))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private record DocumentRequest(String fileName, String content, String topic) {
    }
}
