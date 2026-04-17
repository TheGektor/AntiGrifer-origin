# AntiGriefSystem (AGS) 🛡️

**AntiGriefSystem** is a lightweight yet powerful protection plugin for Minecraft (Paper 1.21.1) servers. It introduces a "Trust" system where new players are restricted from using dangerous items (like TNT, Lava, Crystals) until they have played for a configurable amount of time.

It also features a robust **Discord integration** to notify administrators about suspicious activities, trust changes, and potential grief attempts in real-time.

---

## ✨ Features

*   **⏱️ Automatic Trust System**: Players must play for a certain time (default: 6 hours) to become "Trusted" and use restricted items.
*   **🚫 Restricted Items**: customizable list of items (TNT, Lava, etc.) that untrusted players cannot place, use, or craft.
*   **⚙️ Mechanism Protection**: Prevents Redstone mechanisms (Dispensers/Droppers) from dispensing restricted items.
*   **📢 Discord Webhooks**: Beautiful, fully customizable Discord notifications for:
    *   Trust given/revoked.
    *   Automatic promotion.
    *   Suspicious activity (interaction with restricted items).
    *   Mechanism alerts (with coordinates).
*   **🌍 Multi-Language Support**: Out-of-the-box support for **English** (`en`) and **Russian** (`ru`).
*   **🎨 Modern Formatting**: Full support for **MiniMessage** (RGB gradients, hover effects) in all messages.
*   **🗄️ SQLite Database**: Efficiently stores player data and playtime.

## 📥 Installation

1.  Download the latest `AntiGriefSystem-x.x.x.jar` from the [Releases](https://github.com/your-repo/AntiGriefSystem/releases) page.
2.  Place the JAR file into your server's `plugins` folder.
3.  Restart your server.
4.  Configure `config.yml` and `discord.yml` in the `plugins/AntiGriefSystem` folder.

## 💬 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/ags trust <player>` | `ags.admin` | Manually trust a player. |
| `/ags untrust <player>` | `ags.admin` | Remove trust from a player. |
| `/ags check <player>` | `ags.admin` | Check a player's trust status and playtime. |
| `/ags reload` | `ags.admin` | Reload configuration and locale files. |
| **Bypass** | `ags.bypass` | Allow a player to bypass all restrictions. |

## ⚙️ Configuration

### `config.yml`
Main configuration file.
```yaml
# Language: en or ru
language: en

# Time in minutes needed to become trusted automatically
trusted-playtime-needed-minutes: 360

# List of items restricted for untrusted players
restricted-items:
  - TNT
  - LAVA_BUCKET
  - END_CRYSTAL
  # ... add more items
  
# Discord Webhook URL (required for notifications)
discord-webhook-url: "https://discord.com/api/webhooks/..."
```

### `discord.yml`
Customize your Discord notifications. Supports JSON embed structures.
```yaml
trust-given:
  title: "Trust Granted"
  color: "#2ECC71" # Hex color
  description: "Administrator **{admin}** trusted **{player}**."
  # ...
```

## 🏗️ Building from Source

To build this project, you need JDK 21 installed.

1.  Clone the repository:
    ```sh
    git clone https://github.com/your-username/AntiGriefSystem.git
    ```
2.  Navigate to the project directory:
    ```sh
    cd AntiGriefSystem
    ```
3.  Build with Gradle:
    ```sh
    ./gradlew build
    ```
4.  The output JAR will be in `build/libs/`.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
