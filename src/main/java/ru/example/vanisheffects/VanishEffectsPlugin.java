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

/**
 * Три эффекта исчезновения, выбираемые отдельными командами (/v fire, /v bats, /v lightning).
 * При появлении обратно (/v show) — без эффектов, мгновенно.
 */
public class VanishEffects {

    private static final Random RANDOM = new Random();

    // ======================================================
    // /v fire — приглушённый огонь + звук телепортации
    // ======================================================
    public static void playFireEffect(JavaPlugin plugin, Player target, Runnable onFinish) {
        World world = target.getWorld();
        Location center = target.getLocation();

        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 25;

            @Override
            public void run() {
                if (!target.isOnline() || ticks >= duration) {
                    this.cancel();
                    if (onFinish != null) onFinish.run();
                    return;
                }

                // Немного огня в воздухе — редкие одиночные искры, а не облако пламени
                if (ticks % 4 == 0) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    Location fireLoc = center.clone().add(
                            Math.cos(angle) * 0.6,
                            0.5 + RANDOM.nextDouble() * 1.2,
                            Math.sin(angle) * 0.6
                    );
                    world.spawnParticle(Particle.FLAME, fireLoc, 2, 0.05, 0.05, 0.05, 0.005);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ======================================================
    // /v bats — настоящие летучие мыши (реальные сущности)
    // ======================================================
    public static void playBatsEffect(JavaPlugin plugin, Player target, Runnable onFinish) {
        World world = target.getWorld();
        Location center = target.getLocation();

        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        List<Bat> bats = new ArrayList<>();
        int batCount = 5;
        for (int i = 0; i < batCount; i++) {
            double angle = (Math.PI * 2 / batCount) * i;
            Location spawnLoc = center.clone().add(Math.cos(angle) * 0.4, 1.0, Math.sin(angle) * 0.4);
            Bat bat = world.spawn(spawnLoc, Bat.class);
            bat.setAwake(true);
            bat.setRemoveWhenFarAway(true);
            bat.setVelocity(new Vector(
                    Math.cos(angle) * 0.35,
                    0.35 + RANDOM.nextDouble() * 0.25,
                    Math.sin(angle) * 0.35
            ));
            bats.add(bat);
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 25;

            @Override
            public void run() {
                if (!target.isOnline() || ticks >= duration) {
                    for (Bat bat : bats) {
                        if (bat.isValid()) {
                            bat.remove();
                        }
                    }
                    this.cancel();
                    if (onFinish != null) onFinish.run();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ======================================================
    // /v lightning — серия визуальных молний (без урона)
    // ======================================================
    public static void playLightningEffect(JavaPlugin plugin, Player target, Runnable onFinish) {
        World world = target.getWorld();

        new BukkitRunnable() {
            int strikes = 0;
            final int totalStrikes = 6;

            @Override
            public void run() {
                if (!target.isOnline() || strikes >= totalStrikes) {
                    this.cancel();
                    if (onFinish != null) onFinish.run();
                    return;
                }

                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double dist = 0.8 + RANDOM.nextDouble() * 1.5;
                Location strikeLoc = target.getLocation().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // strikeLightningEffect — только визуал и звук, без урона и поджога
                world.strikeLightningEffect(strikeLoc);
                world.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f + RANDOM.nextFloat() * 0.3f);
                world.spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc.clone().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.05);

                strikes++;
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }
}
