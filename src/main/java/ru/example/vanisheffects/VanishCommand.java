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
import java.util.List;

/**
 * /v [игрок] — включает или выключает ваниш.
 * При включении проигрывается эффект (летучие мыши + огонь + звук телепортации).
 * При выключении — без эффектов, мгновенно.
 */
public class VanishCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final VanishManager vanishManager;

    public VanishCommand(JavaPlugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;

        if (args.length >= 1) {
            if (!sender.hasPermission("vanisheffects.others")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав применять /v к другим игрокам.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден: " + args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "С консоли укажите игрока: /v <игрок>");
                return true;
            }
            target = (Player) sender;
        }

        if (vanishManager.isVanished(target)) {
            vanishManager.show(target);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " теперь виден.");
        } else {
            sender.sendMessage(ChatColor.GREEN + target.getName() + " исчезает...");
            VanishEffects.playVanishEffect(plugin, target, () -> vanishManager.vanish(target));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) result.add(p.getName());
            }
        }
        return result;
    }
}
