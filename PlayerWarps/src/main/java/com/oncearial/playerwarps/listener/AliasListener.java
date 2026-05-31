package com.oncearial.playerwarps.listener;

import com.oncearial.playerwarps.PlayerWarps;
import com.oncearial.playerwarps.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public class AliasListener implements Listener {
    private final PlayerWarps plugin;

    public AliasListener(PlayerWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String command = message.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);

        List<String> aliases = plugin.getConfig().getStringList("settings.gui-aliases");
        for (String alias : aliases) {
            String cleaned = alias.replace("/", "").toLowerCase(Locale.ROOT);
            if (!command.equals(cleaned)) continue;

            event.setCancelled(true);
            if (!player.hasPermission("playerwarps.gui")) {
                Text.send(player, msg("prefix"), msg("no-permission"));
                return;
            }
            plugin.warpGui().openBrowse(player, 0);
            return;
        }
    }

    private String msg(String key) { return plugin.getConfig().getString("messages." + key, key); }
}
