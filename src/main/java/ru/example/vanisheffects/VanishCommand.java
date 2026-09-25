package ru.example.vanisheffects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.example.vanisheffects.effects.VanishEffects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /v fire      [игрок]  — вспыхивающий огонь + звук, затем ваниш
 * /v bats      [игрок]  — стая летучих мышей, затем ваниш
 * /v lightning [игрок]  — серия молний, затем ваниш
 * /v show      [игрок]  — вернуть видимость
 *
 * Все три эффекта запускаются одной и той же командой /v,
 * просто с разными аргументами — как и просилось в задаче.
 */
public class VanishCommand implements CommandExecutor, TabCompleter {

    private static final List<String> EFFECTS = Arrays.asList("fire", "bats", "lightning", "show");

    private final JavaPlugin plugin;
    private final VanishManager vanishManager;

    public VanishCommand(JavaPlugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Использование: /v <fire|bats|lightning|show> [игрок]");
            return true;
        }

        String effect = args[0].toLowerCase();
        Player target;

        if (args.length >= 2) {
            if (!sender.hasPermission("vanisheffects.others")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав применять эффект к другим игрокам.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден: " + args[1]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "С консоли укажите игрока: /v " + effect + " <игрок>");
                return true;
            }
            target = (Player) sender;
        }

        switch (effect) {
            case "fire":
                startVanish(sender, target, "огонь", () ->
                        VanishEffects.playFireEffect(plugin, target, () -> vanishManager.vanish(target)));
                break;
            case "bats":
                startVanish(sender, target, "летучие мыши", () ->
                        VanishEffects.playBatsEffect(plugin, target, () -> vanishManager.vanish(target)));
                break;
            case "lightning":
                startVanish(sender, target, "молнии", () ->
                        VanishEffects.playLightningEffect(plugin, target, () -> vanishManager.vanish(target)));
                break;
            case "show":
            case "off":
                if (!vanishManager.isVanished(target)) {
                    sender.sendMessage(ChatColor.YELLOW + target.getName() + " и так виден.");
                    return true;
                }
                vanishManager.show(target);
                VanishEffects.playShowEffect(plugin, target, null);
                sender.sendMessage(ChatColor.GREEN + target.getName() + " снова виден.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Неизвестный эффект. Доступно: fire, bats, lightning, show");
        }

        return true;
    }

    private void startVanish(CommandSender sender, Player target, String effectName, Runnable action) {
        if (vanishManager.isVanished(target)) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " уже в ваниш-режиме. Сначала /v show " + target.getName());
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Запускаю эффект \"" + effectName + "\" для " + target.getName() + "...");
        action.run();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String eff : EFFECTS) {
                if (eff.startsWith(args[0].toLowerCase())) result.add(eff);
            }
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) result.add(p.getName());
            }
        }
        return result;
    }
}
