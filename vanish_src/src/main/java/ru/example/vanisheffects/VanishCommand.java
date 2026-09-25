package ru.example.vanisheffects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.example.vanisheffects.effects.VanishEffects.VanishStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VanishCommand implements CommandExecutor, TabCompleter {

    private final VanishManager vanishManager;

    public VanishCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        VanishStyle style = args.length >= 1 ? parseStyle(args[0]) : null;
        String targetName = args.length >= 2 ? args[1] : null;

        Player target;
        if (targetName != null) {
            if (!sender.hasPermission("vanisheffects.others")) {
                sender.sendMessage("§cУ вас нет прав применять эту команду к другим игрокам.");
                return true;
            }
            target = sender.getServer().getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден: " + targetName);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cУкажите игрока: /v <fire|bats|lightning> <игрок>");
                return true;
            }
            target = (Player) sender;
        }

        boolean self = target.equals(sender);
        if (self && !sender.hasPermission("vanisheffects.use")) {
            sender.sendMessage("§cУ вас нет прав на использование ваниша.");
            return true;
        }

        if (style != null) {
            // Указан конкретный стиль — включаем ваниш с ним (или меняем стиль, если уже спрятан)
            vanishManager.enable(target, style);
            sender.sendMessage("§a" + (self ? "Вы исчезли" : target.getName() + " исчез(ла)")
                    + " со стилем §f" + style.getDisplayName() + "§a.");
        } else {
            // Без аргументов — просто переключаем ваниш вкл/выкл
            boolean nowVanished = vanishManager.toggle(target);
            String who = self ? "Вы" : target.getName();
            sender.sendMessage(nowVanished
                    ? "§a" + who + (self ? " исчезли" : " исчез(ла)") + "."
                    : "§a" + who + (self ? " снова видимы" : " снова видим(а)") + ".");
        }

        return true;
    }

    private VanishStyle parseStyle(String arg) {
        for (VanishStyle style : VanishStyle.values()) {
            if (style.name().equalsIgnoreCase(arg)) {
                return style;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("fire", "bats", "lightning");
        }
        if (args.length == 2 && sender.hasPermission("vanisheffects.others")) {
            List<String> names = new ArrayList<>();
            for (Player p : sender.getServer().getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        return new ArrayList<>();
    }
}
