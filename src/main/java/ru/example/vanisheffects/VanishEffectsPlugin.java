package ru.example.vanisheffects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
 * /v                    — включить/выключить ваниш (со стилем, который использовался последним)
 * /v fire|bats|lightning [игрок] — включить ваниш с конкретным стилем (или сменить стиль на лету)
 */
public class VanishEffectsPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private static final Random RANDOM = new Random();
    private static final int BAT_COUNT = 14;
    private static final int BAT_SWARM_TICKS = 16;

    private enum Style {
        FIRE("Огонь"), BATS("Летучие мыши"), LIGHTNING("Молния");

        final String displayName;

        Style(String displayName) {
            this.displayName = displayName;
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
        getLogger().info(" VanishEffects включён! Команда: /v");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        for (UUID id : new HashSet<>(vanished)) {
            Player p = getServer().getPlayer(id);
            if (p != null) {
                showToOthers(p);
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
        Style style = args.length >= 1 ? parseStyle(args[0]) : null;
        String targetName = args.length >= 2 ? args[1] : null;

        Player target;
        if (targetName != null) {
            if (!sender.hasPermission("vanisheffects.others")) {
                sender.sendMessage("§cУ вас нет прав применять эту команду к другим игрокам.");
                return true;
            }
            target = getServer().getPlayerExact(targetName);
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
            enable(target, style);
            sender.sendMessage("§a" + (self ? "Вы исчезли" : target.getName() + " исчез(ла)")
                    + " со стилем §f" + style.displayName + "§a.");
        } else {
            boolean nowVanished = toggle(target);
            String who = self ? "Вы" : target.getName();
            sender.sendMessage(nowVanished
                    ? "§a" + who + (self ? " исчезли" : " исчез(ла)") + "."
                    : "§a" + who + (self ? " снова видимы" : " снова видим(а)") + ".");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("fire", "bats", "lightning");
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

    // ================= Ваниш-логика =================

    private boolean toggle(Player player) {
        if (vanished.contains(player.getUniqueId())) {
            disable(player);
            return false;
        } else {
            enable(player, lastStyle.getOrDefault(player.getUniqueId(), Style.FIRE));
            return true;
        }
    }

    private void enable(Player player, Style style) {
        UUID id = player.getUniqueId();
        lastStyle.put(id, style);
        boolean wasVanished = vanished.contains(id);
        vanished.add(id);

        playVanishEffect(player, style);

        if (!wasVanished) {
            hideFromOthers(player);
        }
    }

    private void disable(Player player) {
        UUID id = player.getUniqueId();
        if (vanished.remove(id)) {
            showToOthers(player);
            World world = player.getWorld();
            Location loc = player.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.POOF, loc, 40, 0.5, 0.8, 0.5, 0.02);
            world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        }
    }

    private void hideFromOthers(Player player) {
        for (Player online : getServer().getOnlinePlayers()) {
            if (!online.equals(player) && !online.hasPermission("vanisheffects.see")) {
                online.hidePlayer(this, player);
            }
        }
    }

    private void showToOthers(Player player) {
        for (Player online : getServer().getOnlinePlayers()) {
            online.showPlayer(this, player);
        }
    }

    // ================= Визуальные эффекты =================

    private void playVanishEffect(Player player, Style style) {
        switch (style) {
            case FIRE:
                playFireEffect(player);
                break;
            case BATS:
                playBatsEffect(player);
                break;
            case LIGHTNING:
                playLightningEffect(player);
                break;
        }
    }

    private void playFireEffect(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation().add(0, 1, 0);
        world.spawnParticle(Particle.FLAME, loc, 80, 0.6, 1.0, 0.6, 0.05);
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 40, 0.5, 1.0, 0.5, 0.03);
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
    }

    private void playLightningEffect(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        world.strikeLightningEffect(loc);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0), 60, 0.6, 1.0, 0.6, 0.05);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    /** Стая летучих мышей поднимается вокруг игрока, кружит и разлетается в момент исчезновения. */
    private void playBatsEffect(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 1, 0);

        world.spawnParticle(Particle.POOF, center, 20, 0.4, 0.6, 0.4, 0.02);
        world.playSound(center, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.8f);
        world.playSound(center, Sound.ENTITY_BAT_AMBIENT, 1.0f, 1.0f);

        List<Bat> bats = new ArrayList<>();
        for (int i = 0; i < BAT_COUNT; i++) {
            double angle = 2 * Math.PI * i / BAT_COUNT;
            double radius = 0.8 + RANDOM.nextDouble() * 0.6;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY() + RANDOM.nextDouble() * 1.2;

            Bat bat = world.spawn(new Location(world, x, y, z), Bat.class);
            bat.setAwake(true);
            bat.setCustomNameVisible(false);
            bats.add(bat);
        }

        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                for (Bat bat : bats) {
                    if (!bat.isValid()) continue;
                    Vector outward = bat.getLocation().toVector().subtract(center.toVector());
                    if (outward.lengthSquared() < 1.0E-4) {
                        outward = new Vector(RANDOM.nextDouble() - 0.5, 0, RANDOM.nextDouble() - 0.5);
                    }
                    bat.setVelocity(outward.normalize().multiply(0.15).add(new Vector(0, 0.08, 0)));
                }
                if (tick >= BAT_SWARM_TICKS) {
                    for (Bat bat : bats) {
                        if (bat.isValid()) {
                            bat.getWorld().spawnParticle(Particle.POOF, bat.getLocation(), 5, 0.1, 0.1, 0.1, 0.01);
                            bat.remove();
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
