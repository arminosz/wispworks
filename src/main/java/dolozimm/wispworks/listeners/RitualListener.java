package dolozimm.wispworks.listeners;

import dolozimm.wispworks.WispPlugin;
import dolozimm.wispworks.managers.RitualManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RitualListener implements Listener {
    
    private final WispPlugin plugin;
    private final RitualManager ritualManager;
    private final Set<UUID> sacrificeDeaths;

    public RitualListener(WispPlugin plugin) {
        this.plugin = plugin;
        this.ritualManager = plugin.getRitualManager();
        this.sacrificeDeaths = new HashSet<>();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.GOLD_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() != Material.STICK) {
            return;
        }

        if (!player.hasPermission("wispworks.ritual")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
            return;
        }

        Location centerLocation = clickedBlock.getLocation();

        if (!ritualManager.isValidRitualCircle(centerLocation)) {
            plugin.getMessageUtils().sendMessage(player, "ritual-invalid-circle");
            return;
        }

        if (ritualManager.getRitual(centerLocation) != null) {
            if (ritualManager.cancelRitual(centerLocation, player)) {
                event.setCancelled(true);
            }
        } else {
            if (ritualManager.startRitual(centerLocation, player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Location deathLocation = entity.getLocation();
        Player killer = entity.getKiller();

        if (isInActiveRitualCircle(deathLocation)) {
            sacrificeDeaths.add(entity.getUniqueId());
            
            if (killer != null) {
                ritualManager.addSacrificeToRitual(deathLocation, entity.getType());
                plugin.getMessageUtils().sendMessage(killer, "ritual-sacrifice-added", 
                    "%sacrifice%", entity.getType().name());
                
                if (entity instanceof Player) {
                    Player killedPlayer = (Player) entity;
                    plugin.getMessageUtils().sendMessage(killedPlayer, "ritual-sacrifice-message");
                }
            }
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    sacrificeDeaths.remove(entity.getUniqueId());
                }
            }.runTaskLater(plugin, 100L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        Entity sourceEntity = findSourceEntity(item);
        
        String metadataKey = plugin.getMessageUtils().getMessage("ritual-sacrifice-metadata-key");
        
        if (sourceEntity != null && sacrificeDeaths.contains(sourceEntity.getUniqueId())) {
            item.setMetadata(metadataKey, new FixedMetadataValue(plugin, true));
            ejectItemFromRitual(item);
            return;
        }
        
        new BukkitRunnable() {
            int checks = 0;
            final int maxChecks = 60;
            
            @Override
            public void run() {
                if (!item.isValid() || item.isDead() || checks >= maxChecks) {
                    cancel();
                    return;
                }
                
                if (item.hasMetadata(metadataKey)) {
                    cancel();
                    return;
                }
                
                Location itemLocation = item.getLocation();
                if (isInActiveRitualCircle(itemLocation)) {
                    Player nearestPlayer = getNearestPlayer(itemLocation, 10.0);
                    if (nearestPlayer != null && nearestPlayer.hasPermission("wispworks.ritual")) {
                        Location ritualCenter = findNearestRitualCenter(itemLocation);
                        if (ritualCenter != null) {
                            ritualManager.addItemToRitual(ritualCenter, item.getItemStack());
                            item.remove();
                            cancel();
                            return;
                        }
                    }
                }
                checks++;
            }
        }.runTaskTimer(plugin, 10L, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String recipe = ritualManager.getCurrentRecipe(player);
        
        if (recipe != null) {
            String actionBarMessage = plugin.getMessageUtils().getMessage("ritual-action-bar")
                .replace("%recipe%", recipe);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMessage));
        }
    }

    private Entity findSourceEntity(Item item) {
        Location itemLoc = item.getLocation();
        for (Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 3, 3, 3)) {
            if (entity instanceof LivingEntity && entity.isDead()) {
                if (entity.getLocation().distance(itemLoc) < 2.0) {
                    return entity;
                }
            }
        }
        return null;
    }

    private void ejectItemFromRitual(Item item) {
        Location itemLocation = item.getLocation();
        Location ritualCenter = findNearestRitualCenter(itemLocation);
        
        if (ritualCenter != null) {
            Vector direction = itemLocation.toVector().subtract(ritualCenter.toVector()).setY(0);
            if (direction.lengthSquared() < 0.1) {
                direction = new Vector(1, 0, 0);
            }
            direction.normalize();
            final Vector finalDirection = direction;
            Location ejectionTarget = ritualCenter.clone().add(finalDirection.multiply(6.5)).add(0, 1, 0);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (item.isValid() && !item.isDead()) {
                        item.teleport(ejectionTarget);
                        Vector velocity = finalDirection.clone().multiply(0.4);
                        velocity.setY(0.3);
                        item.setVelocity(velocity);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private boolean isInActiveRitualCircle(Location location) {
        for (Location ritualCenter : ritualManager.getActiveRitualLocations()) {
            if (ritualCenter.getWorld().equals(location.getWorld()) && 
                ritualCenter.distance(location) <= 4.5) {
                return true;
            }
        }
        return false;
    }

    private Location findNearestRitualCenter(Location location) {
        Location nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Location ritualCenter : ritualManager.getActiveRitualLocations()) {
            if (ritualCenter.getWorld().equals(location.getWorld())) {
                double distance = ritualCenter.distance(location);
                if (distance <= 4.5 && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = ritualCenter;
                }
            }
        }
        
        return nearest;
    }
    
    private Player getNearestPlayer(Location location, double radius) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Player) {
                double distance = entity.getLocation().distance(location);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = (Player) entity;
                }
            }
        }
        
        return nearest;
    }
}