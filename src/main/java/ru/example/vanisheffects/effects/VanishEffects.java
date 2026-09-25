package ru.example.vanisheffects.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Три визуальных эффекта, проигрываемых перед тем как игрок пропадает
 * из виду (становится vanished), плюс один эффект "появления" обратно.
 *
 * Все эффекты работают через частицы и звуки — они безопасны
 * (не наносят урона, молнии визуальные и не поджигают блоки).
 */
public class VanishEffects {

    private static final Random RANDOM = new Random();

    // ======================================================
    // ЭФФЕКТ 1: Исчезающий огонь (со звуком)
    // ======================================================
    public static void playFireEffect(JavaPlugin plugin, Player target, Runnable onFinish) {
        World world = target.getWorld();

        world.playSound(target.getLocation(), Sound.ENTITY_GENERIC_BURN, 1.0f, 1.0f);
        world.playSound(target.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 30; // ~1.5 секунды

            @Override
            public void run() {
                if (!target.isOnline() || ticks >= duration) {
                    this.cancel();
                    if (onFinish != null) onFinish.run();
                    return;
                }

                double progress = ticks / (double) duration;
                double radius = 1.2 * (1 - progress) + 0.3; // огонь сужается
                double height = progress * 2.2;              // и поднимается вверх

                Location base = target.getLocation();
                for (int i = 0; i < 10; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double x = Math.cos(angle) * radius * RANDOM.nextDouble();
                    double z = Math.sin(angle) * radius * RANDOM.nextDouble();
                    Location particleLoc = base.clone().add(x, height + RANDOM.nextDouble() * 0.5, z);
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0.02, 0, 0.01);
                    if (ticks % 3 == 0) {
                        world.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                    }
                }
                world.spawnParticle(Particle.SMOKE_LARGE, base.clone().add(0, height, 0), 2, 0.3, 0.2, 0.3, 0.01);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ======================================================
    // ЭФФЕКТ 2: Стая летучих мышей
    // ======================================================
    public static void playBatsEffect(JavaPlugin plugin, Player target, Runnable onFinish) {
        World world = target.getWorld();
        world.playSound(target.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 35;
            final int batCount = 8;

            @Override
            public void run() {
                if (!target.isOnline() || ticks >= duration) {
                    this.cancel();
                    if (onFinish != null) onFinish.run();
                    return;
                }

                if (ticks % 4 == 0) {
                    world.playSound(target.getLocation(), Sound.ENTITY_BAT_AMBIENT, 0.6f, 1.2f);
                }

                Location base = target.getLocation();
                double t = ticks / (double) duration;
                double radius = 1.5 + t * 1.5;

                for (int i = 0; i < batCount; i++) {
                    double angle = (Math.PI * 2 / batCount) * i + ticks * 0.35;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = 1 + Math.sin(ticks * 0.5 + i) * 0.9 + t * 1.5;
                    Location wingLoc = base.clone().add(x, y, z);
                    world.spawnParticle(Particle.ASH, wingLoc, 2, 0.1, 0.1, 0.1, 0.001);
                    world.spawnParticle(Particle.SMOKE_NORMAL, wingLoc, 1, 0, 0, 0, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ======================================================
    // ЭФФЕКТ 3: Много молний (визуальных, без урона)
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
        }.runTaskTimer(plugin, 0L, 6L); // молния примерно каждые 0.3 сек
    }

    // ======================================================
    // Мягкое появление обратно (для /v show)
    // ======================================================
    public static void playShowEffect(JavaPlugin plugin, Player target, Runnable onFinish) {
        World world = target.getWorld();
        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 15;

            @Override
            public void run() {
                if (!target.isOnline() || ticks >= duration) {
                    this.cancel();
                    if (onFinish != null) onFinish.run();
                    return;
                }
                world.spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 6, 0.4, 0.6, 0.4, 0.02);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
