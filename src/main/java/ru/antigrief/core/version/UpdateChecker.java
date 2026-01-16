package ru.antigrief.core.version;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Асинхронная проверка обновлений через GitHub API.
 * Использует Java 11 HttpClient.
 *
 * @author Antag0nis1
 */
public class UpdateChecker {

    private final Plugin plugin;
    private final String repoOwner;
    private final String repoName;
    private final HttpClient httpClient;

    public UpdateChecker(@NotNull Plugin plugin, @NotNull String repoOwner, @NotNull String repoName) {
        this.plugin = plugin;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Асинхронно проверяет наличие обновлений.
     */
    public CompletableFuture<VersionInfo> checkUpdates() {
        String url = String.format("https://api.github.com/repos/%s/%s/releases/latest", repoOwner, repoName);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse)
                .exceptionally(throwable -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to check for updates: " + throwable.getMessage());
                    return null;
                });
    }

    private VersionInfo parseResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            plugin.getLogger().warning("Update check returned status code: " + response.statusCode());
            return null;
        }

        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String tagName = json.get("tag_name").getAsString();
            String body = json.has("body") ? json.get("body").getAsString() : "";
            String htmlUrl = json.get("html_url").getAsString();

            // Удаляем 'v' если есть (v1.0.0 -> 1.0.0)
            if (tagName.startsWith("v")) {
                tagName = tagName.substring(1);
            }

            VersionInfo version = VersionInfo.parse(tagName);
            if (version != null) {
                // Создаем новый инстанс с URL и changelog, так как parse() возвращает базовый
                return new VersionInfo(
                        version.getMajor(), 
                        version.getMinor(), 
                        version.getPatch(), 
                        htmlUrl, 
                        body
                );
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing GitHub response", e);
        }
        return null;
    }
}
