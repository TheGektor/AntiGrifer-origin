package ru.antigrief.integrations;

import ru.antigrief.AntiGriefSystem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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

    public void sendNotification(String title, String description) {
        String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        // Simple JSON payload. For production, a proper JSON builder (like Gson) is
        // better.
        // Assuming simple text without special escaping for now to keep it
        // dependency-free-ish
        // or simple enough.
        // Let's do basic escaping for double quotes.
        String safeTitle = escapeJson(title);
        String safeDesc = escapeJson(description);

        String json = String.format("{\"embeds\": [{\"title\": \"%s\", \"description\": \"%s\", \"color\": 16711680}]}",
                safeTitle, safeDesc);

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

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
