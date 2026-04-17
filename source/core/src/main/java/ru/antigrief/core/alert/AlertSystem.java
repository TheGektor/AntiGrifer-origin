package ru.antigrief.core.alert;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AlertSystem {
    private final List<AlertChannel> channels = new ArrayList<>();
    private final Logger logger;

    public AlertSystem(Logger logger) {
        this.logger = logger;
    }

    public void registerChannel(AlertChannel channel) {
        channels.add(channel);
    }

    public void dispatch(String message, AlertLevel level) {
        dispatch(message, level, null);
    }

    public void dispatch(String message, AlertLevel level, String suspectName) {
        logger.info("[ALERT] [" + level + "] " + message + (suspectName != null ? " (Suspect: " + suspectName + ")" : ""));
        for (AlertChannel channel : channels) {
            channel.send(message, level, suspectName);
        }
    }

    public enum AlertLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public interface AlertChannel {
        void send(String message, AlertLevel level, String suspectName);
    }
}
