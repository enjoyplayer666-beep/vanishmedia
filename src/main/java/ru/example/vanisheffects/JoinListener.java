package ru.example.vanisheffects;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final VanishManager vanishManager;

    public JoinListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Чтобы новый игрок сразу не видел тех, кто уже в ваниш-режиме
        vanishManager.applyVanishTo(event.getPlayer());
    }
}
