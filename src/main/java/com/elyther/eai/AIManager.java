package com.elyther.eai;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AIManager {

    private final EAI plugin;

    private final HttpClient httpClient;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public AIManager(EAI plugin) {

        this.plugin = plugin;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public void ask(Player player, String question) {

        long now = System.currentTimeMillis();

        long cooldown =
                plugin.getConfig()
                        .getLong(
                                "ai.cooldown-seconds",
                                3
                        );

        Long last =
                cooldowns.get(player.getUniqueId());

        if (last != null) {

            long remaining =
                    (cooldown * 1000L)
                            - (now - last);

            if (remaining > 0) {

                long seconds =
                        (long) Math.ceil(
                                remaining / 1000.0
                        );

                player.sendMessage(
                        color(
                                "&cAI ilə yenidən danışmaq üçün "
                                        + seconds
                                        + " saniyə gözlə."
                        )
                );

                return;
            }
        }

        cooldowns.put(
                player.getUniqueId(),
                now
        );

        player.sendMessage(
                color("&8[&bEAI&8] &7Düşünürəm...")
        );

        CompletableFuture
                .supplyAsync(
                        () -> askProviders(question)
                )
                .thenAccept(answer -> {

                    plugin.getServer()
                            .getScheduler()
                            .runTask(
                                    plugin,
                                    () -> {

                                        if (!player.isOnline()) {
                                            return;
                                        }

                                        player.sendMessage(
                                                color(
                                                        "&8[&bEAI&8] &f"
                                                                + answer
                                                )
                                        );
                                    }
                            );
                });
    }

    private String askProviders(String question) {

        String[] order =
                plugin.getConfig()
                        .getStringList("ai.order")
                        .toArray(new String[0]);

        if (order.length == 0) {

            return "AI provider konfiqurasiya edilməyib.";
        }

        String lastError =
                "Bütün AI providerləri uğursuz oldu.";

        for (String provider : order) {

            try {

                if (!isEnabled(provider)) {
                    continue;
                }

                String key =
                        getEnvironmentKey(provider);

                if (key == null || key.isBlank()) {

                    lastError =
                            provider
                                    + " üçün API key tapılmadı.";

                    continue;
                }

                String answer;

                switch (provider.toLowerCase()) {

                    case "gemini" ->

                            answer =
                                    askGemini(
                                            key,
                                            question
                                    );

                    case "groq" ->

                            answer =
                                    askOpenAICompatible(
                                            key,
                                            "https://api.groq.com/openai/v1/chat/completions",
                                            plugin.getConfig()
                                                    .getString(
                                                            "ai.providers.groq.model"
                                                    ),
                                            question
                                    );

                    case "openrouter" ->

                            answer =
                                    askOpenAICompatible(
                                            key,
                                            "https://openrouter.ai/api/v1/chat/completions",
                                            plugin.getConfig()
                                                    .getString(
                                                            "ai.providers.openrouter.model"
                                                    ),
                                            question
                                    );

                    default -> {
                        continue;
                    }
                }

                if (answer != null
                        && !answer.isBlank()) {

                    return answer;
                }

            } catch (Exception exception) {

                lastError =
                        provider
                                + " xətası: "
                                + exception.getMessage();

                plugin.getLogger()
                        .warning(
                                "EAI "
                                        + provider
                                        + " uğursuz oldu: "
                                        + exception.getMessage()
                        );
            }
        }

        return "&cAI hazırda cavab verə bilmir. "
                + "Provider limitlərini yoxla.";
    }

    private boolean isEnabled(String provider) {

        return plugin.getConfig()
                .getBoolean(
                        "ai.providers."
                                + provider
                                + ".enabled",
                        false
                );
    }

    private String getEnvironmentKey(String provider) {

        String environmentVariable =
                plugin.getConfig()
                        .getString(
                                "ai.providers."
                                        + provider
                                        + ".environment-variable"
                        );

        if (environmentVariable == null) {
            return null;
        }

        return System.getenv(environmentVariable);
    }

    private String askGemini(
            String apiKey,
            String question
    ) throws IOException, InterruptedException {

        String model =
                plugin.getConfig()
                        .getString(
                                "ai.providers.gemini.model",
                                "gemini-2.5-flash"
                        );

        String system =
                plugin.getConfig()
                        .getString(
                                "ai.system-prompt",
                                ""
                        );

        int maxTokens =
                plugin.getConfig()
                        .getInt(
                                "ai.max-tokens",
                                300
                        );

        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/"
                        + model
                        + ":generateContent?key="
                        + apiKey;

        String prompt =
                system
                        + "\n\nOyunçu:\n"
                        + question;

        String json =
                "{"
                        + "\"contents\":["
                        + "{"
                        + "\"parts\":["
                        + "{"
                        + "\"text\":\""
                        + escapeJson(prompt)
                        + "\""
                        + "}"
                        + "]"
                        + "}"
                        + "],"
                        + "\"generationConfig\":{"
                        + "\"maxOutputTokens\":"
                        + maxTokens
                        + "}"
                        + "}";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                json,
                                                StandardCharsets.UTF_8
                                        )
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString(
                                        StandardCharsets.UTF_8
                                )
                );

        if (response.statusCode() == 429) {

            throw new RuntimeException(
                    "Gemini rate limit"
            );
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new RuntimeException(
                    "HTTP "
                            + response.statusCode()
            );
        }

        return extractGeminiText(
                response.body()
        );
    }

    private String askOpenAICompatible(
            String apiKey,
            String url,
            String model,
            String question
    ) throws IOException, InterruptedException {

        String system =
                plugin.getConfig()
                        .getString(
                                "ai.system-prompt",
                                ""
                        );

        int maxTokens =
                plugin.getConfig()
                        .getInt(
                                "ai.max-tokens",
                                300
                        );

        double temperature =
                plugin.getConfig()
                        .getDouble(
                                "ai.temperature",
                                0.7
                        );

        String json =
                "{"
                        + "\"model\":\""
                        + escapeJson(model)
                        + "\","
                        + "\"messages\":["
                        + "{"
                        + "\"role\":\"system\","
                        + "\"content\":\""
                        + escapeJson(system)
                        + "\""
                        + "},"
                        + "{"
                        + "\"role\":\"user\","
                        + "\"content\":\""
                        + escapeJson(question)
                        + "\""
                        + "}"
                        + "],"
                        + "\"temperature\":"
                        + temperature
                        + ","
                        + "\"max_tokens\":"
                        + maxTokens
                        + "}"
                        ;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                json,
                                                StandardCharsets.UTF_8
                                        )
                        )
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString(
                                        StandardCharsets.UTF_8
                                )
                );

        if (response.statusCode() == 429) {

            throw new RuntimeException(
                    "Rate limit"
            );
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new RuntimeException(
                    "HTTP "
                            + response.statusCode()
            );
        }

        return extractOpenAIText(
                response.body()
        );
    }

    private String extractGeminiText(
            String json
    ) {

        String marker =
                "\"text\":\"";

        int start =
                json.indexOf(marker);

        if (start == -1) {

            return "Gemini cavabı oxunmadı.";
        }

        start += marker.length();

        return readJsonString(
                json,
                start
        );
    }

    private String extractOpenAIText(
            String json
    ) {

        String marker =
                "\"content\":\"";

        int start =
                json.indexOf(marker);

        if (start == -1) {

            return "AI cavabı oxunmadı.";
        }

        start += marker.length();

        return readJsonString(
                json,
                start
        );
    }

    private String readJsonString(
            String json,
            int start
    ) {

        StringBuilder result =
                new StringBuilder();

        boolean escaped = false;

        for (
                int i = start;
                i < json.length();
                i++
        ) {

            char c =
                    json.charAt(i);

            if (escaped) {

                switch (c) {

                    case 'n' ->
                            result.append('\n');

                    case 'r' ->
                            result.append('\r');

                    case 't' ->
                            result.append('\t');

                    case '"' ->
                            result.append('"');

                    case '\\' ->
                            result.append('\\');

                    case '/' ->
                            result.append('/');

                    default ->
                            result.append(c);
                }

                escaped = false;

                continue;
            }

            if (c == '\\') {

                escaped = true;

                continue;
            }

            if (c == '"') {

                break;
            }

            result.append(c);
        }

        return result.toString();
    }

    private String escapeJson(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\t",
                        "\\t"
                );
    }

    private String color(
            String text
    ) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}
