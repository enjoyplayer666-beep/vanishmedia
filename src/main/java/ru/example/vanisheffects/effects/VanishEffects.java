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
 * Эффект исчезновения: настоящие летучие мыши + немного огня в воздухе
 * (по чуть-чуть, не облако пламени) + звук телепортации.
 * При появлении обратно — никаких эффектов, происходит мгновенно.
 */
public class VanishEffects {

    private static final Random RANDOM = new Random();

    public static void playVanishEffect(JavaPlugin plugin, Player target, Runnable onFinish) {
        World world = target.getWorld();
        Location center = target.getLocation();

        // Мягкий звук телепортации вместо звука горения
        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Спавним НАСТОЯЩИХ летучих мышей, которые разлетаются в стороны
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
            final int duration = 25; // чуть больше секунды

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
}
