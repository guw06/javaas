package com.assistant.services;

import com.assistant.utils.HttpClientUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewsService {
    public String getNews(String input) {
        String topic = extractTopic(input);
        String url = topic.isBlank()
            ? "https://news.google.com/rss?hl=ru&gl=RU&ceid=RU:ru"
            : "https://news.google.com/rss/search?q=" + URLEncoder.encode(topic, StandardCharsets.UTF_8) + "&hl=ru&gl=RU&ceid=RU:ru";

        String xml = HttpClientUtil.get(url);
        if (xml == null || xml.isBlank()) {
            return "Не удалось получить новости. Проверьте интернет или попробуйте позже.";
        }

        List<String> titles = parseTitles(xml, 5);
        if (titles.isEmpty()) {
            return "Новости не найдены" + (topic.isBlank() ? "." : " по теме: " + topic);
        }

        StringBuilder result = new StringBuilder(topic.isBlank() ? "Последние новости:\n" : "Новости по теме \"" + topic + "\":\n");
        for (int i = 0; i < titles.size(); i++) {
            result.append(i + 1).append(". ").append(titles.get(i)).append("\n");
        }
        return result.toString().trim();
    }

    private List<String> parseTitles(String xml, int limit) {
        List<String> titles = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList items = document.getElementsByTagName("item");

            for (int i = 0; i < items.getLength() && titles.size() < limit; i++) {
                NodeList children = items.item(i).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if ("title".equalsIgnoreCase(children.item(j).getNodeName())) {
                        titles.add(children.item(j).getTextContent().trim());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return titles;
    }

    private String extractTopic(String input) {
        if (input == null) {
            return "";
        }

        String text = input.trim();
        String lower = text.toLowerCase(Locale.ROOT).replace('ё', 'е');
        String[] markers = {"новости про", "новости по", "новости о", "что нового про", "что нового о"};

        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0) {
                return text.substring(index + marker.length()).trim();
            }
        }

        return "";
    }
}
