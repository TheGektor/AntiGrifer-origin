package ru.antigrief.features.alerts;

import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.antigrief.core.utils.CoordinateFormatter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Сервис для отправки алертов в Discord.
 *
 * @author Antag0nis1
 */
public class DiscordAlertService {

    private final Plugin plugin;
    private final HttpClient httpClient;
    // TODO: Вынести URL в конфиг
    private final String webhookUrl; 

    public DiscordAlertService(Plugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
        this.webhookUrl = plugin.getConfig().getString("discord.alerts-webhook", "");
    }

    public void sendAlert(Player player, String action, Location location) {
        if (webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "⚠️ Grief Attempt Detected");
                embed.addProperty("color", 16711680); // Red
                embed.addProperty("description", "**Player:** " + player.getName() + 
                                               "\n**Action:** " + action + 
                                               "\n**Location:** " + CoordinateFormatter.formatPlain(location));
                
                JsonObject footer = new JsonObject();
                footer.addProperty("text", "AntiGriefSystem • " + plugin.getDescription().getVersion());
                embed.add("footer", footer);

                JsonObject json = new JsonObject();
                // json.addProperty("content", "@here"); // Опционально пинг
                com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
                embeds.add(embed);
                json.add("embeds", embeds);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send Discord alert", e);
            }
        });
    }
}
