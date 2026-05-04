package com.assistant.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WordDocumentService {
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"|'([^']+)'");
    private static final Pattern TEXT_MARKER = Pattern.compile("(?iu)\\s+с\\s+текстом\\s+(.+)$");
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final GeminiService geminiService = new GeminiService();
    private final Path defaultDirectory;

    public WordDocumentService() {
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path desktop = home.resolve("Desktop");
        this.defaultDirectory = Files.isDirectory(desktop) ? desktop : home;
    }

    public String createFromCommand(String input) {
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
            return "Word-документ создан: " + output;
        } catch (Exception e) {
            return "Не удалось создать Word-документ: " + e.getMessage();
        }
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
