package ru.example.vanisheffects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Хранит список ваниш-игроков и отвечает за их скрытие/показ
 * остальным игрокам на сервере.
 */
public class VanishManager {

    private final JavaPlugin plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void vanish(Player target) {
        vanished.add(target.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(target) && !online.hasPermission("vanisheffects.see")) {
                online.hidePlayer(plugin, target);
            }
        }
        target.setPlayerListName(ChatColor.GRAY + "[V] " + ChatColor.RESET + target.getName());
    }

    public void show(Player target) {
        vanished.remove(target.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, target);
        }
        target.setPlayerListName(target.getName());
    }

    public void showAll() {
        for (UUID uuid : new HashSet<>(vanished)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                show(p);
            }
        }
    }

    /** Скрывает уже ваниш-игроков от только что зашедшего игрока. */
    public void applyVanishTo(Player viewer) {
        if (viewer.hasPermission("vanisheffects.see")) return;
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && !vanishedPlayer.equals(viewer)) {
                viewer.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }
}
