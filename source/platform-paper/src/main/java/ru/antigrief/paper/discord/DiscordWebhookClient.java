package ru.antigrief.paper.discord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Отправляет embed-сообщения и создаёт треды через Discord Webhook API.
 * Все HTTP-запросы выполняются асинхронно в отдельном потоке.
 */
public class DiscordWebhookClient {

    private final String webhookUrl;
    private final Logger logger;
    private final HttpClient http;
    private final ExecutorService executor;

    public DiscordWebhookClient(String webhookUrl, Logger logger) {
        this.webhookUrl = webhookUrl;
        this.logger = logger;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AntiGrifer-Discord");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Отправляет embed алерта.
     * Если forumChannel=true — создаёт тред с именем threadName (Forum channel).
     * Возвращает CompletableFuture<Long> с channel_id треда или -1.
     */
    public CompletableFuture<Long> sendAlert(String threadName, String playerName,
                                              String reason, String level,
                                              long timestamp, boolean forumChannel) {
        return CompletableFuture.supplyAsync(() -> {
            int color = levelColor(level);
            String embed = buildAlertEmbed(playerName, reason, level, color, timestamp);

            String body = forumChannel
                    ? "{\"thread_name\":" + quote(threadName) + ",\"embeds\":[" + embed + "]}"
                    : "{\"embeds\":[" + embed + "]}";

            return post(webhookUrl + "?wait=true", body, forumChannel);
        }, executor);
    }

    /**
     * Отправляет сообщение в существующий тред (ACK / TP уведомление).
     */
    public void sendToThread(long threadId, String content) {
        executor.execute(() -> {
            String body = "{\"content\":" + quote(content) + "}";
            post(webhookUrl + "?thread_id=" + threadId + "&wait=false", body, false);
        });
    }

    /**
     * Отправляет embed статуса (ACK/TP) в тред с цветным оформлением.
     */
    public void sendStatusToThread(long threadId, String adminName, String action,
                                   String playerName, boolean resolved) {
        executor.execute(() -> {
            String emoji = resolved ? "✅" : "👀";
            int color = resolved ? 0x57F287 : 0x5865F2;
            String embed = "{" +
                    "\"title\":\"" + emoji + " " + action + "\"," +
                    "\"description\":\"**Администратор:** `" + escape(adminName) + "`\\n" +
                    "**Игрок:** `" + escape(playerName) + "`\"," +
                    "\"color\":" + color + "," +
                    "\"timestamp\":\"" + Instant.now() + "\"" +
                    "}";
            String body = "{\"embeds\":[" + embed + "]}";
            post(webhookUrl + "?thread_id=" + threadId + "&wait=false", body, false);
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    // ── internal ──────────────────────────────────────────────────────────────

    /** Выполняет POST, возвращает channel_id если extractThread=true, иначе -1. */
    private long post(String url, String body, boolean extractThread) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                if (extractThread) return extractLong(resp.body(), "channel_id");
            } else {
                logger.warning("[AntiGrifer/Discord] HTTP " + resp.statusCode() + " | " + resp.body().substring(0, Math.min(200, resp.body().length())));
            }
        } catch (Exception e) {
            logger.warning("[AntiGrifer/Discord] Ошибка запроса: " + e.getMessage());
        }
        return -1L;
    }

    private String buildAlertEmbed(String player, String reason, String level, int color, long ts) {
        return "{" +
                "\"title\":\"" + levelEmoji(level) + " Подозрение на гриф\"," +
                "\"description\":\"**Игрок:** `" + escape(player) + "`\\n**Причина:** " + escape(reason) + "\"," +
                "\"color\":" + color + "," +
                "\"timestamp\":\"" + Instant.ofEpochMilli(ts) + "\"," +
                "\"fields\":[" +
                "{\"name\":\"Уровень\",\"value\":\"" + escape(level) + "\",\"inline\":true}," +
                "{\"name\":\"Статус\",\"value\":\"🔴 Ожидает\",\"inline\":true}" +
                "]" +
                "}";
    }

    private int levelColor(String level) {
        return switch (level.toUpperCase()) {
            case "LOW"      -> 0x808080;
            case "MEDIUM"   -> 0xFFAA00;
            case "HIGH"     -> 0xFF4400;
            case "CRITICAL" -> 0xFF0000;
            default         -> 0xFF4400;
        };
    }

    private String levelEmoji(String level) {
        return switch (level.toUpperCase()) {
            case "LOW"      -> "ℹ️";
            case "MEDIUM"   -> "⚠️";
            case "HIGH"     -> "🚨";
            case "CRITICAL" -> "🔥";
            default         -> "⚠️";
        };
    }

    /** Извлекает числовое значение поля из простого JSON. */
    private long extractLong(String json, String field) {
        String key = "\"" + field + "\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return -1L;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end <= start) return -1L;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return -1L; }
    }

    private String quote(String s)  { return "\"" + escape(s) + "\""; }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
