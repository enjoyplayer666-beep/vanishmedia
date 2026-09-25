package ru.example.vanisheffects.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VanishEffects {

    private static final Random RANDOM = new Random();

    /** Сколько летучих мышей поднимается вокруг игрока при стиле BATS. */
    private static final int BAT_COUNT = 14;

    /** Сколько тиков стая кружит вокруг игрока перед тем, как разлететься (20 тиков = 1 сек). */
    private static final int BAT_SWARM_TICKS = 16;

    public enum VanishStyle {
        FIRE("Огонь"),
        BATS("Летучие мыши"),
        LIGHTNING("Молния");

        private final String displayName;

        VanishStyle(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static void playVanishEffect(JavaPlugin plugin, Player player, VanishStyle style) {
        switch (style) {
            case FIRE:
                playFireEffect(player);
                break;
            case BATS:
                playBatsEffect(plugin, player);
                break;
            case LIGHTNING:
                playLightningEffect(player);
                break;
        }
    }

    /** Мягкий эффект при возвращении из ваниша (одинаковый для всех стилей). */
    public static void playRevealEffect(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation().add(0, 1, 0);
        world.spawnParticle(Particle.POOF, loc, 40, 0.5, 0.8, 0.5, 0.02);
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
    }

    private static void playFireEffect(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation().add(0, 1, 0);
        world.spawnParticle(Particle.FLAME, loc, 80, 0.6, 1.0, 0.6, 0.05);
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 40, 0.5, 1.0, 0.5, 0.03);
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
    }

    private static void playLightningEffect(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        world.strikeLightningEffect(loc); // визуальная молния, без урона и поджога
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0), 60, 0.6, 1.0, 0.6, 0.05);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    /**
     * Вокруг игрока поднимается стая летучих мышей, кружит примерно секунду
     * и разлетается в момент исчезновения — как будто игрок пропадает вместе с ними.
     */
    private static void playBatsEffect(JavaPlugin plugin, Player player) {
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

            Location spawnLoc = new Location(world, x, y, z);
            Bat bat = world.spawn(spawnLoc, Bat.class);
            bat.setAwake(true);
            bat.setCustomNameVisible(false);
            bats.add(bat);
        }

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                for (Bat bat : bats) {
                    if (!bat.isValid()) {
                        continue;
                    }
                    Vector outward = bat.getLocation().toVector()
                            .subtract(center.toVector());
                    if (outward.lengthSquared() < 1.0E-4) {
                        outward = new Vector(RANDOM.nextDouble() - 0.5, 0, RANDOM.nextDouble() - 0.5);
                    }
                    Vector velocity = outward.normalize().multiply(0.15).add(new Vector(0, 0.08, 0));
                    bat.setVelocity(velocity);
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
