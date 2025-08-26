package dolozimm.wispworks.utils;

import dolozimm.wispworks.WispPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class MutationUtils {

    private static final Random random = new Random();

    public static boolean mutatePlant(WispPlugin plugin, Block block, Player player) {
        Material blockType = block.getType();
        String materialName = blockType.name().toLowerCase();

        List<String> possibleTransforms = plugin.getConfig().getStringList("mutations.plants." + materialName);

        if (possibleTransforms.isEmpty()) {
            plugin.getMessageUtils().sendMessage(player, "plant-cannot-mutate");
            return false;
        }

        String newPlantName = possibleTransforms.get(random.nextInt(possibleTransforms.size()));
        Material newMaterial;

        try {
            newMaterial = Material.valueOf(newPlantName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(plugin.getMessageUtils().getMessage("invalid-plant-material")
                .replace("%material%", newPlantName));
            return false;
        }

        createMutationEffect(plugin, block.getLocation().clone().add(0.5, 0.5, 0.5), player);

        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(newMaterial);
                player.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
                player.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0f, 1.2f);
            }
        }.runTaskLater(plugin, 40L);

        return true;
    }

    public static boolean mutateMob(WispPlugin plugin, LivingEntity entity, Player player) {
        if (entity instanceof Player) {
            plugin.getMessageUtils().sendMessage(player, "cannot-mutate-players");
            return false;
        }

        String entityName = entity.getType().name().toLowerCase();
        List<String> possibleTransforms = plugin.getConfig().getStringList("mutations.animals." + entityName);

        if (possibleTransforms.isEmpty()) {
            plugin.getMessageUtils().sendMessage(player, "creature-cannot-mutate");
            return false;
        }

        String newMobName = possibleTransforms.get(random.nextInt(possibleTransforms.size()));
        EntityType newEntityType;

        try {
            newEntityType = EntityType.valueOf(newMobName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(plugin.getMessageUtils().getMessage("invalid-entity-type")
                .replace("%entity%", newMobName));
            return false;
        }

        Location location = entity.getLocation();

        createMutationEffect(plugin, location, player);

        new BukkitRunnable() {
            @Override
            public void run() {
                double health = entity.getHealth();
                boolean baby = false;

                if (entity instanceof Ageable) {
                    baby = !((Ageable) entity).isAdult();
                }

                entity.remove();

                Entity newEntity = location.getWorld().spawnEntity(location, newEntityType);

                if (newEntity instanceof LivingEntity) {
                    LivingEntity newLivingEntity = (LivingEntity) newEntity;

                    double maxHealth = newLivingEntity.getMaxHealth();
                    newLivingEntity.setHealth(Math.min(health, maxHealth));

                    if (baby && newLivingEntity instanceof Ageable) {
                        ((Ageable) newLivingEntity).setBaby();
                    }
                }

                player.getWorld().playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.5f);

                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location.clone().add(0, 1, 0), 1);
            }
        }.runTaskLater(plugin, 40L);

        return true;
    }

    private static void createMutationEffect(WispPlugin plugin, Location location, Player player) {
        player.getWorld().playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                double angle = (ticks * 0.3) % (2 * Math.PI);
                double radius = 1.0 + Math.sin(ticks * 0.1) * 0.5;

                for (int i = 0; i < 3; i++) {
                    double currentAngle = angle + (i * 2 * Math.PI / 3);
                    double x = location.getX() + Math.cos(currentAngle) * radius;
                    double z = location.getZ() + Math.sin(currentAngle) * radius;
                    double y = location.getY() + Math.sin(ticks * 0.2) * 0.5;

                    player.getWorld().spawnParticle(
                            Particle.ENCHANTED_HIT,
                            x, y, z,
                            1, 0, 0, 0, 0.02
                    );
                }
                if (ticks % 10 == 0) {
                    player.getWorld().spawnParticle(
                            Particle.PORTAL,
                            location,
                            5, 0.5, 0.5, 0.5, 0.02
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}