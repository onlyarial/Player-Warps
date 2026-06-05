package com.oncearial.playerwarps.update;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements Listener {
    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final PlayerWarps plugin;
    private final HttpClient client;
    private volatile UpdateInfo updateInfo;

    public UpdateChecker(PlayerWarps plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void checkAsync() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UpdateInfo checked = checkLatestRelease();
                updateInfo = checked;
                if (checked.newer()) {
                    plugin.getServer().getScheduler().runTask(plugin, this::notifyOnlineAdmins);
                }
            } catch (Exception ex) {
                if (plugin.getConfig().getBoolean("update-checker.log-failures", true)) {
                    plugin.getLogger().warning("Could not check for updates: " + ex.getMessage());
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> notifyIfNeeded(event.getPlayer()), 40L);
    }

    private UpdateInfo checkLatestRelease() throws IOException, InterruptedException {
        String apiUrl = plugin.getConfig().getString("update-checker.github-api-url", "https://api.github.com/repos/onlyarial/Player-Warps/releases/latest");
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "PlayerWarps/" + plugin.getDescription().getVersion())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub returned HTTP " + response.statusCode());
        }

        String tagName = match(response.body(), TAG_NAME);
        String releaseUrl = match(response.body(), HTML_URL);
        if (tagName == null || releaseUrl == null) {
            throw new IOException("GitHub release response did not include tag_name or html_url");
        }

        String currentVersion = plugin.getDescription().getVersion();
        return new UpdateInfo(tagName, releaseUrl, compareVersions(tagName, currentVersion) > 0);
    }

    private void notifyOnlineAdmins() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            notifyIfNeeded(player);
        }
    }

    private void notifyIfNeeded(Player player) {
        UpdateInfo checked = updateInfo;
        if (checked == null || !checked.newer()) return;
        if (!player.hasPermission("playerwarps.admin")) return;

        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String message = plugin.getConfig().getString(
                "messages.update-available",
                "&eA newer PlayerWarps version is available: &f%version%&e. Download: &f%url%"
        );

        Text.send(player, prefix, message
                .replace("%version%", checked.version())
                .replace("%url%", checked.url()));
    }

    private String match(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private int compareVersions(String latest, String current) {
        int[] latestParts = versionParts(latest);
        int[] currentParts = versionParts(current);
        int length = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < length; i++) {
            int left = i < latestParts.length ? latestParts[i] : 0;
            int right = i < currentParts.length ? currentParts[i] : 0;
            if (left != right) return Integer.compare(left, right);
        }
        return 0;
    }

    private int[] versionParts(String version) {
        String cleaned = version == null ? "" : version.trim();
        cleaned = cleaned.replaceFirst("^[vV]", "");
        cleaned = cleaned.split("-", 2)[0];
        if (cleaned.isBlank()) return new int[] {0};

        String[] pieces = cleaned.split("\\.");
        int[] parts = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            try {
                parts[i] = Integer.parseInt(pieces[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ex) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    private record UpdateInfo(String version, String url, boolean newer) {}
}
