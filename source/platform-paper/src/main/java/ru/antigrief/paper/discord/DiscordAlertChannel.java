package ru.antigrief.paper.discord;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.alert.AlertTracker;

import java.util.UUID;

/**
 * AlertChannel реализация для Discord.
 * Создаёт тред в Forum-канале на первый алерт игрока,
 * последующие алерты постит в уже существующий тред.
 */
public class DiscordAlertChannel implements AlertSystem.AlertChannel {

    private final DiscordWebhookClient client;
    private final AlertTracker alertTracker;
    private final boolean forumChannel;

    public DiscordAlertChannel(DiscordWebhookClient client, AlertTracker alertTracker,
                                boolean forumChannel) {
        this.client = client;
        this.alertTracker = alertTracker;
        this.forumChannel = forumChannel;
    }

    @Override
    public void send(String message, AlertSystem.AlertLevel level, String suspectName) {
        if (suspectName == null) suspectName = "Неизвестно";
        final String name = suspectName;

        UUID suspectUUID = resolveUUID(suspectName);
        long existingThread = suspectUUID != null ? alertTracker.getDiscordThreadId(suspectUUID) : -1L;

        if (existingThread > 0) {
            // Уже есть тред — постим обновление туда
            client.sendToThread(existingThread,
                    "🔄 **Новый алерт** | `" + name + "`\n" +
                    "**Причина:** " + message + "\n" +
                    "**Уровень:** " + level.name());
        } else {
            // Создаём новый тред / embed
            String threadName = "🚨 Гриф: " + suspectName;
            long ts = System.currentTimeMillis();
            final UUID uid = suspectUUID;

            client.sendAlert(threadName, name, message, level.name(), ts, forumChannel)
                    .thenAccept(threadId -> {
                        if (uid != null && threadId > 0) {
                            alertTracker.setDiscordThreadId(uid, threadId);
                        }
                    });
        }
    }

    /**
     * Вызывается из AntiGriferCommand при ACK или TP.
     * Отправляет embed в тред с информацией кто обработал алерт.
     */
    public void notifyAction(UUID suspectUUID, String suspectName,
                              String adminName, String action, boolean resolved) {
        long threadId = alertTracker.getDiscordThreadId(suspectUUID);
        if (threadId <= 0) return;
        client.sendStatusToThread(threadId, adminName, action, suspectName, resolved);
        // Если resolved — можно сбросить thread ID чтобы следующий алерт создал новый тред
        if (resolved) {
            alertTracker.setDiscordThreadId(suspectUUID, -1L);
        }
    }

    private UUID resolveUUID(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online.getUniqueId() : null;
    }
}
