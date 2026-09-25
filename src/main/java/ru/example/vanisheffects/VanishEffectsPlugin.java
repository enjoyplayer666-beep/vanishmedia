package ru.example.vanisheffects;

import org.bukkit.plugin.java.JavaPlugin;

public class VanishEffectsPlugin extends JavaPlugin {

    private VanishManager vanishManager;

    @Override
    public void onEnable() {
        this.vanishManager = new VanishManager(this);

        VanishCommand vanishCommand = new VanishCommand(this, vanishManager);
        getCommand("v").setExecutor(vanishCommand);
        getCommand("v").setTabCompleter(vanishCommand);

        getServer().getPluginManager().registerEvents(new JoinListener(vanishManager), this);

        getLogger().info("VanishEffects включён.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VanishEffects выключен.");
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }
}
