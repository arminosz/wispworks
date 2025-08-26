package dolozimm.wispworks.listeners;

import dolozimm.wispworks.WispPlugin;
import dolozimm.wispworks.managers.CauldronManager;
import dolozimm.wispworks.utils.ItemUtils;
import dolozimm.wispworks.utils.MutationUtils;
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
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;

public class ItemListener implements Listener {
    
    private final WispPlugin plugin;
    private final CauldronManager cauldronManager;
    
    public ItemListener(WispPlugin plugin) {
        this.plugin = plugin;
        this.cauldronManager = plugin.getCauldronManager();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        
        new BukkitRunnable() {
            int checks = 0;
            final int maxChecks = 60;
            
            @Override
            public void run() {
                if (!item.isValid() || item.isDead() || checks >= maxChecks) {
                    cancel();
                    return;
                }
                
                Location itemLocation = item.getLocation();
                Block currentBlock = itemLocation.getBlock();
                
                if (currentBlock.getType() == Material.WATER_CAULDRON &&
                    cauldronManager.isMagicCauldron(currentBlock.getLocation()) &&
                    cauldronManager.isCauldronFull(currentBlock)) {
                    
                    Player nearestPlayer = getNearestPlayer(itemLocation, 10.0);
                    if (nearestPlayer != null && nearestPlayer.hasPermission("wispworks.craft")) {
                        
                        itemLocation.getWorld().playSound(itemLocation, Sound.ENTITY_GENERIC_SPLASH, 1.0f, 1.2f);
                        
                        plugin.getRecipeManager().addItemToCauldron(
                            currentBlock.getLocation(), 
                            item.getItemStack(), 
                            nearestPlayer
                        );
                        
                        item.remove();
                        cancel();
                        return;
                    }
                }
                
                Block belowBlock = itemLocation.getBlock().getRelative(0, -1, 0);
                if (belowBlock.getType() == Material.WATER_CAULDRON &&
                    cauldronManager.isMagicCauldron(belowBlock.getLocation()) &&
                    cauldronManager.isCauldronFull(belowBlock) &&
                    itemLocation.getY() - belowBlock.getY() < 1.5) {
                    
                    Player nearestPlayer = getNearestPlayer(itemLocation, 10.0);
                    if (nearestPlayer != null && nearestPlayer.hasPermission("wispworks.craft")) {
                        
                        itemLocation.getWorld().playSound(itemLocation, Sound.ENTITY_GENERIC_SPLASH, 1.0f, 1.2f);
                        
                        plugin.getRecipeManager().addItemToCauldron(
                            belowBlock.getLocation(), 
                            item.getItemStack(), 
                            nearestPlayer
                        );
                        
                        item.remove();
                        cancel();
                        return;
                    }
                }
                
                checks++;
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Entity target = event.getRightClicked();

        if (ItemUtils.isMutationApple(itemInHand) && target instanceof LivingEntity) {
            if (!player.hasPermission("wispworks.use")) {
                plugin.getMessageUtils().sendMessage(player, "no-permission");
                return;
            }
            LivingEntity livingEntity = (LivingEntity) target;
            if (MutationUtils.mutateMob(plugin, livingEntity, player)) {
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        
        if (ItemUtils.isMutationBonemeal(itemInHand)) {
            if (!player.hasPermission("wispworks.use")) {
                plugin.getMessageUtils().sendMessage(player, "no-permission");
                return;
            }
            if (MutationUtils.mutatePlant(plugin, clickedBlock, player)) {
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                event.setCancelled(true);
            }
        }
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