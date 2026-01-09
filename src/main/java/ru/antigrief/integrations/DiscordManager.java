package ru.antigrief.integrations;

import ru.antigrief.AntiGriefSystem;
import org.bukkit.configuration.file.YamlConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class DiscordManager {

    private final AntiGriefSystem plugin;
    private final HttpClient httpClient;

    public DiscordManager(AntiGriefSystem plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendWebhook(String key, Map<String, String> placeholders) {
        String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        YamlConfiguration config = plugin.getConfigManager().getDiscordConfig();
        if (!config.contains("messages." + key)) {
            plugin.getLogger().warning("Discord webhook key not found: " + key);
            return;
        }

        String path = "messages." + key;
        String title = applyPlaceholders(config.getString(path + ".title", ""), placeholders);
        String description = applyPlaceholders(config.getString(path + ".description", ""), placeholders);
        String colorHex = config.getString(path + ".color", "#FFFFFF");
        String footerText = config.getString(path + ".footer", "AntiGriefSystem");
        boolean timestamp = config.getBoolean(path + ".timestamp", true);

        int colorDecimal = parseColor(colorHex);

        String json = buildJson(title, description, colorDecimal, footerText, timestamp);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(e -> {
                    plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
                    return null;
                });
    }

    // Deprecated method shim for backward compatibility if any straightforward
    // calls exist
    public void sendNotification(String title, String description) {
        // Fallback or use a generic template if needed, but for now we won't use it.
        // Or we can just log a warning.
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null)
            return "";
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    private int parseColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    private String buildJson(String title, String description, int color, String footer, boolean timestamp) {
        String safeTitle = escapeJson(title);
        String safeDesc = escapeJson(description);
        String safeFooter = escapeJson(footer);

        String timestampJson = "";
        if (timestamp) {
            timestampJson = ", \"timestamp\": \"" + Instant.now().toString() + "\"";
        }

        return String.format(
                "{" +
                        "  \"embeds\": [{" +
                        "    \"title\": \"%s\"," +
                        "    \"description\": \"%s\"," +
                        "    \"color\": %d," +
                        "    \"footer\": {\"text\": \"%s\"}" +
                        "%s" +
                        "  }]" +
                        "}",
                safeTitle, safeDesc, color, safeFooter, timestampJson);
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
