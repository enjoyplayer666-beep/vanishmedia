package ru.example.vanisheffects;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.example.vanisheffects.effects.VanishEffects;
import ru.example.vanisheffects.effects.VanishEffects.VanishStyle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final JavaPlugin plugin;
    private final Set<UUID> vanished = new HashSet<>();
    private final Map<UUID, VanishStyle> lastStyle = new HashMap<>();

    public VanishManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public VanishStyle getStyle(Player player) {
        return lastStyle.getOrDefault(player.getUniqueId(), VanishStyle.FIRE);
    }

    /**
     * Переключает ваниш вкл/выкл, используя последний выбранный стиль игрока.
     * Возвращает true, если игрок теперь спрятан.
     */
    public boolean toggle(Player player) {
        if (isVanished(player)) {
            disable(player);
            return false;
        } else {
            enable(player, getStyle(player));
            return true;
        }
    }

    /**
     * Включает ваниш с указанным стилем. Если игрок уже спрятан — просто
     * проигрывает эффект нового стиля и запоминает его для следующего /v.
     */
    public void enable(Player player, VanishStyle style) {
        UUID id = player.getUniqueId();
        lastStyle.put(id, style);

        boolean wasVanished = vanished.contains(id);
        vanished.add(id);

        VanishEffects.playVanishEffect(plugin, player, style);

        if (!wasVanished) {
            hideFromOthers(player);
        }
    }

    public void disable(Player player) {
        UUID id = player.getUniqueId();
        if (vanished.remove(id)) {
            showToOthers(player);
            VanishEffects.playRevealEffect(player);
        }
    }

    public void disableAll() {
        for (UUID id : new HashSet<>(vanished)) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) {
                showToOthers(player);
            }
        }
        vanished.clear();
    }

    private void hideFromOthers(Player player) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(player) && !online.hasPermission("vanisheffects.see")) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    private void showToOthers(Player player) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    /** Прячет уже спрятанных игроков от только что зашедшего (см. JoinListener). */
    public void hideVanishedFrom(Player joined) {
        if (joined.hasPermission("vanisheffects.see")) {
            return;
        }
        for (UUID id : vanished) {
            Player vanishedPlayer = plugin.getServer().getPlayer(id);
            if (vanishedPlayer != null && !vanishedPlayer.equals(joined)) {
                joined.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }
}
