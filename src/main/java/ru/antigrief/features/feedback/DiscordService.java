package ru.antigrief.features.feedback;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DiscordService {

    // IMPORTANT: Run SecurityUtil.main() to generate this string!
    // Example: "Base64String..."
    private static final String ENCRYPTED_WEBHOOK_URL = "oPiV1ssQhoW2ykoREml94W07Ud0+cqpTSERYHybPQDuydbOGz8XebLZe8G00LLSV5W8kRm8n9+uUdU9u6s0N99W0/k3RjhZwu+l2at7tZqtWi7hDtJxgtU1nHKvPnUeaZ3YX+qPjC//rp0EiTSN8LAs2JRaOHpEgLy4dMKYt5Wi1ncb2ZHFq34rpsX1xg+QtLDcSDUI=";

    private final HttpClient httpClient;

    public DiscordService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendFeedback(String playerName, String message, String serverIp) {
        String url = SecurityUtil.decrypt(ENCRYPTED_WEBHOOK_URL);
        
        if (url == null || url.isEmpty() || url.contains("INSERT_ENCRYPTED")) {
            Bukkit.getLogger().warning("[AntiGriefSystem] Webhook URL is not configured or decryption failed.");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("username", "System Feedback");
        
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        embed.addProperty("title", "New Feedback / Bug Report");
        // Green color
        embed.addProperty("color", 5763719); 
        
        JsonArray fields = new JsonArray();
        
        fields.add(createField("Player", playerName, true));
        fields.add(createField("Message", message, false));
        if (serverIp != null) {
            fields.add(createField("Server IP", serverIp, true));
        }

        embed.add("fields", fields);
        embeds.add(embed);
        json.add("embeds", embeds);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        Bukkit.getLogger().warning("[AntiGriefSystem] Failed to send feedback to Discord: " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    Bukkit.getLogger().severe("[AntiGriefSystem] Error sending feedback: " + e.getMessage());
                    return null;
                });
    }

    private JsonObject createField(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        return field;
    }
}
