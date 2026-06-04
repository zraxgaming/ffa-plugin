package xyz.zcraft.studios.zffa.update;

import org.bukkit.Bukkit;
import xyz.zcraft.studios.zffa.ZFfaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JAR_URL_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
    private final ZFfaPlugin plugin;

    public UpdateChecker(ZFfaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkOnce() {
        if (!plugin.getConfig().getBoolean("settings.update-check.enabled", true)) return;
        plugin.scheduler().runAsync(() -> {
            try {
                ReleaseInfo latest = fetchLatestRelease();
                if (latest == null || latest.tag() == null || latest.tag().isBlank()) return;
                String current = plugin.getDescription().getVersion();
                if (normalize(latest.tag()).equalsIgnoreCase(normalize(current))) {
                    plugin.getLogger().info("Z-FFA is up to date (" + current + ").");
                } else {
                    plugin.getLogger().warning("A newer Z-FFA version is available: " + latest.tag() + " (current: " + current + ").");
                    plugin.getLogger().warning("Download: https://github.com/zraxgaming/ffa-plugin/releases/latest");
                    if (plugin.getConfig().getBoolean("settings.update-check.auto-download", true)) {
                        downloadLatest(latest);
                    }
                }
            } catch (Exception exception) {
                if (plugin.getConfig().getBoolean("settings.debug-enabled", false)) {
                    plugin.getLogger().warning("Update check failed: " + exception.getMessage());
                }
            }
        });
    }

    private ReleaseInfo fetchLatestRelease() throws Exception {
        String url = plugin.getConfig().getString("settings.update-check.url", "https://api.github.com/repos/zraxgaming/ffa-plugin/releases/latest");
        String body = requestText(url);
        Matcher tagMatcher = TAG_PATTERN.matcher(body);
        Matcher jarMatcher = JAR_URL_PATTERN.matcher(body);
        String tag = tagMatcher.find() ? tagMatcher.group(1) : null;
        String jarUrl = jarMatcher.find() ? jarMatcher.group(1).replace("\\/", "/") : null;
        return new ReleaseInfo(tag, jarUrl);
    }

    private String requestText(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "Z-FFA/" + plugin.getDescription().getVersion());
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return body.toString();
        }
    }

    private void downloadLatest(ReleaseInfo release) throws Exception {
        if (release.jarUrl() == null || release.jarUrl().isBlank()) {
            plugin.getLogger().warning("Auto-update is enabled, but no release jar was found.");
            return;
        }
        Path updateDir = plugin.getServer().getUpdateFolderFile().toPath();
        Files.createDirectories(updateDir);
        Path output = updateDir.resolve("Z-FFA-" + normalize(release.tag()) + ".jar");
        if (Files.exists(output)) {
            plugin.getLogger().info("Update jar already downloaded: " + output);
            return;
        }
        HttpURLConnection connection = (HttpURLConnection) URI.create(release.jarUrl()).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "Z-FFA/" + plugin.getDescription().getVersion());
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            plugin.getLogger().warning("Unable to download update jar. HTTP " + connection.getResponseCode());
            return;
        }
        try (InputStream input = connection.getInputStream()) {
            Files.copy(input, output);
        }
        plugin.getLogger().warning("Downloaded Z-FFA update to " + output + ". Restart the server to apply it.");
    }

    private String normalize(String version) {
        return version == null ? "" : version.trim().replaceFirst("^[vV]", "");
    }

    private record ReleaseInfo(String tag, String jarUrl) {
    }
}
