package ru.antigrief.core.utils;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Утилиты для работы с MiniMessage.
 *
 * @author Antag0nis1
 */
public class MiniMessageUtils {

    private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();

    /**
     * Парсит строку MiniMessage в Component.
     */
    public static @NotNull Component parse(@NotNull String message) {
        return MINIMESSAGE.deserialize(message);
    }

    /**
     * Отправляет сообщение отправителю (плееру или консоли).
     */
    public static void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(parse(message));
    }

    /**
     * Отправляет компонент отправителю.
     */
    public static void sendMessage(@NotNull CommandSender sender, @NotNull Component component) {
        sender.sendMessage(component);
    }

    /**
     * Отправляет список сообщений.
     */
    public static void sendMessage(@NotNull CommandSender sender, @NotNull List<String> messages) {
        for (String msg : messages) {
            sendMessage(sender, msg);
        }
    }

    /**
     * Возвращает глобальный инстанс MiniMessage.
     */
    public static MiniMessage getMiniMessage() {
        return MINIMESSAGE;
    }
}
