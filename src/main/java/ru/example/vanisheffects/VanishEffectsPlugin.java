package ru.example.vanisheffects;

import org.bukkit.plugin.java.JavaPlugin;

public class VanishEffectsPlugin extends JavaPlugin {

    private VanishManager vanishManager;

    @Override
    public void onEnable() {
        this.vanishManager = new VanishManager(this);

        VanishCommand commandExecutor = new VanishCommand(this, vanishManager);
        getCommand("v").setExecutor(commandExecutor);
        getCommand("v").setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(new JoinListener(vanishManager), this);

        getLogger().info("VanishEffects включён! Команда: /v <fire|bats|lightning|show> [игрок]");
    }

    @Override
    public void onDisable() {
        if (vanishManager != null) {
            vanishManager.showAll();
        }
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }
}
