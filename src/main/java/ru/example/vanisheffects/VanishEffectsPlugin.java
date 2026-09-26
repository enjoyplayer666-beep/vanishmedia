package ru.example.vanisheffects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * VanishEffects — весь плагин в одном файле, чтобы не было путаницы с несколькими классами.
 *
 * /v                     — включить/выключить ваниш (со стилем, который использовался последним)
 * /v <style> [игрок]     — включить ваниш с конкретным стилем (или сменить стиль на лету)
 * /v menu                — открыть меню выбора эффекта (иконки + градиентные названия)
 */
public class VanishEffectsPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private static final Random RANDOM = new Random();
    private static final int BAT_COUNT = 38;
    private static final int BAT_CIRCLE_TICKS = 16;
    private static final int BAT_FLYAWAY_TICKS = 22;

    /**
     * Название меню. Само вычисляется через градиент, поэтому объявлено после метода hexGradient
     * по смыслу, но т.к. это статический метод — порядок объявления полей значения не имеет.
     */
    private static final String MENU_TITLE = hexGradient("Меню эффектов ваниша", "#8E2DE2", "#4A00E0", true, true);

    private enum Style {
        FIRE("Огонь", "#FF416C", "#FF4B2B", Material.BLAZE_POWDER),
        BATS("Летучие мыши", "#654EA3", "#1F1C2C", Material.BAT_SPAWN_EGG),
        LIGHTNING("Молния", "#2D1BDE", "#28B3F6", Material.TRIDENT),
        STARFALL("Звездопад", "#F7971E", "#FFE259", Material.NETHER_STAR),
        FROST("Иней", "#00C6FB", "#005BEA", Material.BLUE_ICE),
        SOUL("Душа", "#0BAB64", "#3BE4D3", Material.SOUL_LANTERN),
        VORTEX("Вихрь", "#360033", "#0B8793", Material.ENDER_EYE);

        final String displayName;
        final String colorFrom;
        final String colorTo;
        final Material icon;

        Style(String displayName, String colorFrom, String colorTo, Material icon) {
            this.displayName = displayName;
            this.colorFrom = colorFrom;
            this.colorTo = colorTo;
            this.icon = icon;
        }

        /** Название стиля, окрашенное собственным градиентом (жирный + курсив). */
        String gradientName() {
            return hexGradient(displayName, colorFrom, colorTo, true, true);
        }
    }

    private final Set<UUID> vanished = new HashSet<>();
    private final Map<UUID, Style> lastStyle = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("v").setExecutor(this);
        getCommand("v").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("========================================");
        getLogger().info("VanishEffects включён! Команда /v");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        for (UUID id : new HashSet<>(vanished)) {
            Player p = getServer().getPlayer(id);
            if (p != null) {
                showToOthers(p);
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
        vanished.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        if (joined.hasPermission("vanisheffects.see")) {
            return;
        }
        for (UUID id : vanished) {
            Player vp = getServer().getPlayer(id);
            if (vp != null && !vp.equals(joined)) {
                joined.hidePlayer(this, vp);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Меню доступно только игрокам.");
                return true;
            }
            Player menuPlayer = (Player) sender;
            if (!menuPlayer.hasPermission("vanisheffects.use")) {
                menuPlayer.sendMessage(ChatColor.RED + "У вас нет прав на использование ваниша.");
                return true;
            }
            menuPlayer.openInventory(buildMenu(menuPlayer));
            return true;
        }

        Style style = args.length >= 1 ? parseStyle(args[0]) : null;
        String targetName = args.length >= 2 ? args[1] : null;

        Player target;
        if (targetName != null) {
            if (!sender.hasPermission("vanisheffects.others")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав применять эту команду к другим игрокам.");
                return true;
            }
            target = getServer().getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден: " + targetName);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Укажите игрока: /v <стиль> <игрок>");
                return true;
            }
            target = (Player) sender;
        }

        boolean self = target.equals(sender);
        if (self && !sender.hasPermission("vanisheffects.use")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на использование ваниша.");
            return true;
        }

        if (style != null) {
            enable(target, style);
            sender.sendMessage(vanishStatusMessage(true, target.getName()));
        } else if (args.length == 0) {
            boolean nowVanished = toggle(target);
            sender.sendMessage(vanishStatusMessage(nowVanished, target.getName()));
        } else {
            sender.sendMessage(ChatColor.RED + "Неизвестный стиль. Доступно: menu, fire, bats, lightning, starfall, frost, soul, vortex");
        }

        return true;
    }

    /**
     * Сообщение о статусе невидимости в формате
     * ⚑ Невидимость · Включено/Отключено для игрок
     * (без пояснительной средней строки).
     */
    private String vanishStatusMessage(boolean enabled, String playerName) {
        ChatColor statusColor = enabled ? ChatColor.GREEN : ChatColor.RED;
        String status = enabled ? "Включено" : "Отключено";

        return "" + ChatColor.LIGHT_PURPLE + "⚑ " +
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Невидимость " +
                ChatColor.GRAY + "· " +
                statusColor + status + " " +
                ChatColor.GRAY + "для " +
                ChatColor.GRAY + playerName;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("menu", "fire", "bats", "lightning", "starfall", "frost", "soul", "vortex");
        }
        if (args.length == 2 && sender.hasPermission("vanisheffects.others")) {
            List<String> names = new ArrayList<>();
            for (Player p : getServer().getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        return new ArrayList<>();
    }

    private Style parseStyle(String arg) {
        for (Style s : Style.values()) {
            if (s.name().equalsIgnoreCase(arg)) {
                return s;
            }
        }
        return null;
    }

    //
