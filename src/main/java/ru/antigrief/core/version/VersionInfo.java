package ru.antigrief.core.version;

import java.time.Instant;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Модель версии плагина.
 * Поддерживает семантическое версионирование MAJOR.MINOR.PATCH.
 *
 * @author Antag0nis1
 */
public class VersionInfo implements Comparable<VersionInfo> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String fullVersion;
    private final String downloadUrl;
    private final String changelog;
    private final Date releaseDate;

    public VersionInfo(int major, int minor, int patch, @Nullable String downloadUrl, @Nullable String changelog) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.fullVersion = String.format("%d.%d.%d", major, minor, patch);
        this.downloadUrl = downloadUrl;
        this.changelog = changelog;
        this.releaseDate = Date.from(Instant.now());
    }

    /**
     * Парсит строку версии (например, "1.2.3")
     */
    public static @Nullable VersionInfo parse(@NotNull String versionString) {
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (matcher.find()) {
            try {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                int patch = Integer.parseInt(matcher.group(3));
                return new VersionInfo(major, minor, patch, null, null);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public boolean isNewerThan(@NotNull VersionInfo other) {
        return this.compareTo(other) > 0;
    }

    @Override
    public int compareTo(@NotNull VersionInfo other) {
        if (this.major != other.major) return Integer.compare(this.major, other.major);
        if (this.minor != other.minor) return Integer.compare(this.minor, other.minor);
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return fullVersion;
    }

    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }
    public String getFullVersion() { return fullVersion; }
    public @Nullable String getDownloadUrl() { return downloadUrl; }
    public @Nullable String getChangelog() { return changelog; }
}
