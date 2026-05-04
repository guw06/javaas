package com.assistant.services;

import com.assistant.utils.HttpClientUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SportsScoreService {
    private record League(String name, String url) {
    }

    private record Team(String name, String shortName, String score) {
    }

    private final List<League> leagues = List.of(
        new League("NBA", "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard"),
        new League("NFL", "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard"),
        new League("NHL", "https://site.api.espn.com/apis/site/v2/sports/hockey/nhl/scoreboard"),
        new League("MLB", "https://site.api.espn.com/apis/site/v2/sports/baseball/mlb/scoreboard"),
        new League("Premier League", "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard"),
        new League("LaLiga", "https://site.api.espn.com/apis/site/v2/sports/soccer/esp.1/scoreboard"),
        new League("Serie A", "https://site.api.espn.com/apis/site/v2/sports/soccer/ita.1/scoreboard"),
        new League("Bundesliga", "https://site.api.espn.com/apis/site/v2/sports/soccer/ger.1/scoreboard"),
        new League("Ligue 1", "https://site.api.espn.com/apis/site/v2/sports/soccer/fra.1/scoreboard"),
        new League("Champions League", "https://site.api.espn.com/apis/site/v2/sports/soccer/uefa.champions/scoreboard")
    );

    public String getScores(String input) {
        String teamFilter = extractTeam(input);
        List<String> matches = new ArrayList<>();

        for (League league : selectLeagues(input)) {
            String json = HttpClientUtil.get(league.url());
            if (json == null || json.isBlank()) {
                continue;
            }

            matches.addAll(parseEvents(json, league.name(), teamFilter));
            if (!teamFilter.isBlank() && !matches.isEmpty()) {
                break;
            }
        }

        if (matches.isEmpty()) {
            return teamFilter.isBlank()
                ? "Сейчас не нашел свежие матчи. Попробуйте указать лигу или команду: счет матча Реал."
                : "Не нашел свежий матч для запроса: " + teamFilter + ". Иногда у ESPN нет матча на сегодня или команда называется иначе.";
        }

        StringBuilder result = new StringBuilder(teamFilter.isBlank() ? "Свежие матчи:\n" : "Матчи по запросу \"" + teamFilter + "\":\n");
        for (int i = 0; i < Math.min(matches.size(), 8); i++) {
            result.append(i + 1).append(". ").append(matches.get(i)).append("\n");
        }
        return result.toString().trim();
    }

    private List<League> selectLeagues(String input) {
        String lower = normalize(input);
        if (containsAny(lower, "nba", "нба", "баскет")) {
            return only("NBA");
        }
        if (containsAny(lower, "nfl", "американский футбол")) {
            return only("NFL");
        }
        if (containsAny(lower, "nhl", "нхл", "хоккей")) {
            return only("NHL");
        }
        if (containsAny(lower, "mlb", "бейсбол")) {
            return only("MLB");
        }
        if (containsAny(lower, "апл", "premier", "англия", "эвертон", "манчестер", "ливерпуль", "арсенал", "челси")) {
            return only("Premier League");
        }
        if (containsAny(lower, "лалиг", "la liga", "испания", "реал", "барсел")) {
            return only("LaLiga");
        }
        if (containsAny(lower, "серия а", "serie", "италия", "ювентус", "интер", "милан")) {
            return only("Serie A");
        }
        if (containsAny(lower, "бундеслиг", "bundesliga", "германия", "бавария")) {
            return only("Bundesliga");
        }
        if (containsAny(lower, "лига чемпион", "champions")) {
            return only("Champions League");
        }
        return leagues;
    }

    private List<League> only(String name) {
        return leagues.stream().filter(league -> league.name().equals(name)).toList();
    }

    private List<String> parseEvents(String json, String leagueName, String teamFilter) {
        List<String> result = new ArrayList<>();
        try {
            JsonObject root = HttpClientUtil.gson.fromJson(json, JsonObject.class);
            JsonArray events = root.getAsJsonArray("events");
            if (events == null) {
                return result;
            }

            for (JsonElement eventElement : events) {
                JsonObject event = eventElement.getAsJsonObject();
                JsonObject competition = event.getAsJsonArray("competitions").get(0).getAsJsonObject();
                JsonArray competitors = competition.getAsJsonArray("competitors");
                if (competitors == null || competitors.size() < 2) {
                    continue;
                }

                Team away = readTeam(competitors.get(0).getAsJsonObject());
                Team home = readTeam(competitors.get(1).getAsJsonObject());
                String allNames = normalize(away.name() + " " + away.shortName() + " " + home.name() + " " + home.shortName());

                if (!teamFilter.isBlank() && !matchesTeamFilter(allNames, teamFilter)) {
                    continue;
                }

                result.add("%s: %s %s - %s %s (%s)".formatted(
                    leagueName,
                    away.shortName(),
                    away.score(),
                    home.score(),
                    home.shortName(),
                    readStatus(competition)
                ));
            }
        } catch (Exception e) {
            return List.of();
        }
        return result;
    }

    private Team readTeam(JsonObject competitor) {
        JsonObject team = competitor.getAsJsonObject("team");
        String name = getString(team, "displayName", "Team");
        String shortName = getString(team, "shortDisplayName", name);
        String score = getString(competitor, "score", "0");
        return new Team(name, shortName, score);
    }

    private String readStatus(JsonObject competition) {
        if (!competition.has("status")) {
            return "status unknown";
        }
        JsonObject statusObject = competition.getAsJsonObject("status");
        if (!statusObject.has("type")) {
            return "status unknown";
        }
        JsonObject type = statusObject.getAsJsonObject("type");
        if (type.has("shortDetail")) {
            return type.get("shortDetail").getAsString();
        }
        if (type.has("description")) {
            return type.get("description").getAsString();
        }
        return "status unknown";
    }

    private String extractTeam(String input) {
        if (input == null) {
            return "";
        }

        String text = input.trim();
        String lower = normalize(text);
        String[] markers = {"счет матча", "счет игры", "результат матча", "как сыграли", "матч", "команда"};
        for (String marker : markers) {
            int index = lower.indexOf(marker);
            if (index >= 0) {
                return text.substring(Math.min(text.length(), index + marker.length())).trim();
            }
        }
        return "";
    }

    private boolean matchesTeamFilter(String allNames, String teamFilter) {
        String normalizedFilter = normalizeTeamQuery(teamFilter);
        if (normalizedFilter.isBlank()) {
            return true;
        }
        if (allNames.contains(normalizedFilter)) {
            return true;
        }

        String[] parts = normalizedFilter.split("[\\s\\-–—]+");
        int meaningful = 0;
        int matched = 0;
        for (String part : parts) {
            if (part.length() < 3 || isStopWord(part)) {
                continue;
            }
            meaningful++;
            if (allNames.contains(part)) {
                matched++;
            }
        }
        return meaningful > 0 && matched == meaningful;
    }

    private String normalizeTeamQuery(String value) {
        return normalize(value)
            .replace("манчестер сити", "manchester city")
            .replace("ман сити", "manchester city")
            .replace("манчестер юнайтед", "manchester united")
            .replace("ман юнайтед", "manchester united")
            .replace("эвертон", "everton")
            .replace("ливерпуль", "liverpool")
            .replace("арсенал", "arsenal")
            .replace("челси", "chelsea")
            .replace("тоттенхэм", "tottenham")
            .replace("тоттенхем", "tottenham")
            .replace("реал мадрид", "real madrid")
            .replace("реал", "real madrid")
            .replace("барселона", "barcelona")
            .replace("бавария", "bayern munich")
            .replace("псж", "paris saint-germain")
            .replace("ювентус", "juventus")
            .replace("интер", "inter")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean isStopWord(String value) {
        return value.equals("and")
            || value.equals("vs")
            || value.equals("матча")
            || value.equals("матч")
            || value.equals("счет")
            || value.equals("сегодня")
            || value.equals("сегодняшний");
    }

    private String getString(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
            ? object.get(key).getAsString()
            : fallback;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435').trim();
    }
}
