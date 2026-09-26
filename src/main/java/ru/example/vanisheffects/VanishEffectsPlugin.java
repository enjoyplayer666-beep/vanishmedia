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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
    private static final int BAT_COUNT = 38;
    private static final int BAT_CIRCLE_TICKS = 16;
    private static final int BAT_FLYAWAY_TICKS = 22;

    private enum Style {
        FIRE("Огонь"), BATS("Летучие мыши"), LIGHTNING("Молния"), STARFALL("Звездопад"),
        FROST("Иней"), SOUL("Душа"), VORTEX("Вихрь");

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
            sender.sendMessage(vanishStatusMessage(true, target.getName()));
        } else {
            boolean nowVanished = toggle(target);
            sender.sendMessage(vanishStatusMessage(nowVanished, target.getName()));
        }

        return true;
    }

    /**
     * Сообщение о статусе невидимости в формате:
     * "⚑ Невидимость · Невидимость Включено/Отключено для <игрок>"
     * (без пояснительной средней строки).
     */
    private String vanishStatusMessage(boolean enabled, String playerName) {
        org.bukkit.ChatColor statusColor = enabled ? org.bukkit.ChatColor.GREEN : org.bukkit.ChatColor.RED;
        String status = enabled ? "Включено" : "Отключено";

        return "" + org.bukkit.ChatColor.LIGHT_PURPLE + "⚑ " +
                org.bukkit.ChatColor.LIGHT_PURPLE + org.bukkit.ChatColor.BOLD + "Невидимость " +
                org.bukkit.ChatColor.GRAY + "· " +
                statusColor + status + " " +
                org.bukkit.ChatColor.GRAY + "для " +
                org.bukkit.ChatColor.GRAY + playerName;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("fire", "bats", "lightning", "starfall", "frost", "soul", "vortex");
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
    }

    private void disable(Player player) {
        UUID id = player.getUniqueId();
        if (vanished.remove(id)) {
            showToOthers(player);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            Style style = lastStyle.get(id);
            if (style == Style.FIRE) {
                playFireEffect(player);
            } else if (style == Style.LIGHTNING) {
                playLightningEffect(player);
            } else if (style == Style.BATS) {
                playBatsEffect(player);
            } else if (style == Style.STARFALL) {
                playStarfallEffect(player);
            } else if (style == Style.FROST) {
                playFrostEffect(player);
            } else if (style == Style.SOUL) {
                playSoulEffect(player);
            } else if (style == Style.VORTEX) {
                playVortexEffect(player);
            }
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
            case STARFALL:
                playStarfallEffect(player);
                break;
            case FROST:
                playFrostEffect(player);
                break;
            case SOUL:
                playSoulEffect(player);
                break;
            case VORTEX:
                playVortexEffect(player);
                break;
        }
    }

    private void playFireEffect(Player player) {
        World world = player.getWorld();
        Location initialLoc = player.getLocation().add(0, 1, 0);
        world.playSound(initialLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 6;

            @Override
            public void run() {
                if (tick >= totalTicks || !player.isOnline()) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.FLAME, loc, 15, 0.5, 0.9, 0.5, 0.04);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playLightningEffect(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        world.strikeLightningEffect(loc);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0), 200, 1.2, 1.7, 1.2, 0.12);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        int extraStrikes = 8;
        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= extraStrikes) {
                    cancel();
                    return;
                }
                double angle = 2 * Math.PI * i / extraStrikes;
                double radius = 1.8 + RANDOM.nextDouble() * 2.2;
                double x = loc.getX() + radius * Math.cos(angle);
                double z = loc.getZ() + radius * Math.sin(angle);
                Location strikeLoc = new Location(world, x, loc.getY(), z);
                world.strikeLightningEffect(strikeLoc);
                world.spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc.clone().add(0, 1, 0), 50, 0.5, 0.7, 0.5, 0.05);
                i++;
            }
        }.runTaskTimer(plugin, 3L, 3L);
    }

    /** Стая летучих мышей поднимается вокруг игрока, кружит, а затем красиво разлетается в разные стороны. */
    private void playBatsEffect(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 1, 0);

        world.playSound(center, Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.8f);
        world.playSound(center, Sound.ENTITY_BAT_AMBIENT, 1.0f, 1.0f);

        List<Bat> bats = new ArrayList<>();
        List<Vector> flyDirections = new ArrayList<>();
        for (int i = 0; i < BAT_COUNT; i++) {
            double angle = 2 * Math.PI * i / BAT_COUNT;
            double radius = 0.8 + RANDOM.nextDouble() * 0.6;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY() + (RANDOM.nextDouble() - 0.5) * 0.4;

            Bat bat = world.spawn(new Location(world, x, y, z), Bat.class);
            bat.setAwake(true);
            bat.setCustomNameVisible(false);
            bats.add(bat);

            double flyAngle = angle + (RANDOM.nextDouble() - 0.5) * 0.6;
            flyDirections.add(new Vector(Math.cos(flyAngle), 0.3 + RANDOM.nextDouble() * 0.3, Math.sin(flyAngle)));
        }

        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                if (tick <= BAT_CIRCLE_TICKS) {
                    for (Bat bat : bats) {
                        if (!bat.isValid()) continue;
                        Vector outward = bat.getLocation().toVector().subtract(center.toVector());
                        if (outward.lengthSquared() < 1.0E-4) {
                            outward = new Vector(RANDOM.nextDouble() - 0.5, 0, RANDOM.nextDouble() - 0.5);
                        }
                        bat.setVelocity(outward.normalize().multiply(0.15).add(new Vector(0, 0.08, 0)));
                    }
                } else {
                    double progress = (double) (tick - BAT_CIRCLE_TICKS) / BAT_FLYAWAY_TICKS;
                    double speed = 0.25 + progress * 0.55;
                    for (int i = 0; i < bats.size(); i++) {
                        Bat bat = bats.get(i);
                        if (!bat.isValid()) continue;
                        bat.setVelocity(flyDirections.get(i).clone().multiply(speed));
                        bat.getWorld().spawnParticle(Particle.PORTAL, bat.getLocation(), 2, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                if (tick >= BAT_CIRCLE_TICKS + BAT_FLYAWAY_TICKS) {
                    for (Bat bat : bats) {
                        if (bat.isValid()) {
                            bat.getWorld().spawnParticle(Particle.PORTAL, bat.getLocation(), 12, 0.15, 0.15, 0.15, 0.05);
                            bat.remove();
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Светящийся метеор падает с неба на игрока и в момент удара взрывается фейерверком из искр. */
    private void playStarfallEffect(Player player) {
        World world = player.getWorld();
        Location target = player.getLocation().add(0, 1, 0);
        Location start = target.clone().add(0, 14, 0);
        Vector path = target.clone().subtract(start).toVector();

        world.playSound(target, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f);

        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 12;

            @Override
            public void run() {
                if (tick > totalTicks) {
                    cancel();
                    return;
                }
                double progress = (double) tick / totalTicks;
                Location current = start.clone().add(path.clone().multiply(progress));
                world.spawnParticle(Particle.END_ROD, current, 4, 0.08, 0.08, 0.08, 0.01);
                world.spawnParticle(Particle.FLAME, current, 2, 0.05, 0.05, 0.05, 0.01);

                if (tick == totalTicks) {
                    world.spawnParticle(Particle.FIREWORKS_SPARK, target, 90, 0.6, 0.8, 0.6, 0.18);
                    world.spawnParticle(Particle.END_ROD, target, 40, 0.5, 0.6, 0.5, 0.05);
                    world.playSound(target, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.1f);
                    world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Игрока за долю секунды затягивает инеем, после чего ледяная корка со звоном разлетается вдребезги. */
    private void playFrostEffect(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation().add(0, 1, 0);
        world.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);

        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 5;

            @Override
            public void run() {
                if (tick > totalTicks) {
                    world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.9f);
                    world.spawnParticle(Particle.SNOWFLAKE, center, 100, 0.6, 1.0, 0.6, 0.15);
                    world.spawnParticle(Particle.SNOWBALL, center, 30, 0.5, 0.9, 0.5, 0.08);
                    cancel();
                    return;
                }
                double radius = 0.3 + tick * 0.25;
                for (int i = 0; i < 10; i++) {
                    double angle = 2 * Math.PI * i / 10;
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    world.spawnParticle(Particle.SNOWFLAKE, new Location(world, x, center.getY(), z), 2, 0.02, 0.05, 0.02, 0.01);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Тёмная спираль из частиц душ поднимается вокруг игрока и завершается тихим потусторонним всплеском. */
    private void playSoulEffect(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation();
        world.playSound(base, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.6f);

        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 14;

            @Override
            public void run() {
                if (tick > totalTicks) {
                    world.spawnParticle(Particle.SOUL, base.clone().add(0, 1, 0), 60, 0.4, 0.6, 0.4, 0.1);
                    world.playSound(base, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.7f);
                    cancel();
                    return;
                }
                double angle = tick * 0.9;
                double height = tick * 0.15;
                double radius = 0.5;
                double x1 = base.getX() + radius * Math.cos(angle);
                double z1 = base.getZ() + radius * Math.sin(angle);
                double x2 = base.getX() - radius * Math.cos(angle);
                double z2 = base.getZ() - radius * Math.sin(angle);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(world, x1, base.getY() + height, z1), 2, 0.02, 0.02, 0.02, 0.01);
                world.spawnParticle(Particle.SOUL, new Location(world, x2, base.getY() + height, z2), 2, 0.02, 0.02, 0.02, 0.01);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Вокруг игрока закручивается портальный вихрь из двух спиральных рукавов, который стягивается внутрь и лопается. */
    private void playVortexEffect(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation().add(0, 1, 0);
        world.playSound(base, Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.8f);

        JavaPlugin plugin = this;
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 16;

            @Override
            public void run() {
                if (tick > totalTicks) {
                    world.spawnParticle(Particle.PORTAL, base, 80, 0.4, 0.6, 0.4, 0.3);
                    world.spawnParticle(Particle.REVERSE_PORTAL, base, 40, 0.3, 0.5, 0.3, 0.05);
                    world.playSound(base, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
                    cancel();
                    return;
                }
                double progress = (double) tick / totalTicks;
                double radius = 1.2 * (1 - progress) + 0.1;
                double angle = tick * 0.8;
                double height = progress * 1.6;
                for (int arm = 0; arm < 2; arm++) {
                    double a = angle + arm * Math.PI;
                    double x = base.getX() + radius * Math.cos(a);
                    double z = base.getZ() + radius * Math.sin(a);
                    world.spawnParticle(Particle.PORTAL, new Location(world, x, base.getY() + height, z), 3, 0.03, 0.05, 0.03, 0.02);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
