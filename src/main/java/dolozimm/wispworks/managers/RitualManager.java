package dolozimm.wispworks.managers;

import dolozimm.wispworks.WispPlugin;
import dolozimm.wispworks.data.RitualCircle;
import dolozimm.wispworks.data.RitualRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RitualManager {
    
    private final WispPlugin plugin;
    private final Map<Location, RitualCircle> activeRituals;
    private final List<RitualRecipe> ritualRecipes;
    private final Map<UUID, Location> playerActiveRituals;
    private final Map<Location, BukkitRunnable> waitingEffectTasks = new HashMap<>();

    public RitualManager(WispPlugin plugin) {
        this.plugin = plugin;
        this.activeRituals = new HashMap<>();
        this.ritualRecipes = new ArrayList<>();
        this.playerActiveRituals = new HashMap<>();
        loadRitualRecipes();
    }

    public void loadRitualRecipes() {
        ritualRecipes.clear();
        
        if (!plugin.getConfig().contains("rituals")) {
            plugin.getLogger().warning(plugin.getMessageUtils().getMessage("no-rituals-section"));
            return;
        }

        ConfigurationSection ritualsSection = plugin.getConfig().getConfigurationSection("rituals");
        
        for (String ritualName : ritualsSection.getKeys(false)) {
            try {
                ConfigurationSection ritualSection = ritualsSection.getConfigurationSection(ritualName);
                RitualRecipe recipe = new RitualRecipe(ritualName, 
                    ritualSection.getString("effect-type", "NONE"));

                if (ritualSection.contains("items")) {
                    List<String> items = ritualSection.getStringList("items");
                    for (String itemStr : items) {
                        String[] parts = itemStr.split(":");
                        if (parts.length == 2) {
                            try {
                                Material material = Material.valueOf(parts[0].toUpperCase());
                                int amount = Integer.parseInt(parts[1]);
                                recipe.addRequiredItem(material, amount);
                            } catch (Exception e) {
                                plugin.getLogger().warning(plugin.getMessageUtils().getMessage("invalid-material-recipe")
                                        .replace("%recipe%", ritualName)
                                        .replace("%material%", itemStr));
                            }
                        }
                    }
                }

                if (ritualSection.contains("sacrifices")) {
                    List<String> sacrifices = ritualSection.getStringList("sacrifices");
                    for (String sacrificeStr : sacrifices) {
                        String[] parts = sacrificeStr.split(":");
                        if (parts.length == 2) {
                            try {
                                EntityType entityType = EntityType.valueOf(parts[0].toUpperCase());
                                int amount = Integer.parseInt(parts[1]);
                                recipe.addRequiredSacrifice(entityType, amount);
                            } catch (Exception e) {
                                plugin.getLogger().warning(plugin.getMessageUtils().getMessage("invalid-entity-type")
                                        .replace("%entity%", sacrificeStr));
                            }
                        }
                    }
                }

                if (ritualSection.contains("properties")) {
                    ConfigurationSection propsSection = ritualSection.getConfigurationSection("properties");
                    for (String key : propsSection.getKeys(false)) {
                        recipe.setEffectProperty(key, propsSection.get(key));
                    }
                }

                ritualRecipes.add(recipe);
                plugin.getLogger().info(plugin.getMessageUtils().getMessage("loaded-ritual")
                        .replace("%ritual%", ritualName));
                
            } catch (Exception e) {
                plugin.getLogger().severe(plugin.getMessageUtils().getMessage("failed-to-load-ritual")
                        .replace("%ritual%", ritualName));
                e.printStackTrace();
            }
        }

        plugin.getLogger().info(plugin.getMessageUtils().getMessage("loaded-rituals-count")
                .replace("%count%", String.valueOf(ritualRecipes.size())));
    }

    public boolean isValidRitualCircle(Location centerLocation) {
        Block centerBlock = centerLocation.getBlock();
        if (centerBlock.getType() != Material.GOLD_BLOCK) {
            return false;
        }

        return checkRitualCircleStructure(centerLocation);
    }

    private boolean checkRitualCircleStructure(Location center) {
        Material[][] grid = {
            {null, null, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, null, null},
            {null, Material.MAGMA_BLOCK, null, null, null, null, null, Material.MAGMA_BLOCK, null},
            {Material.MAGMA_BLOCK, null, null, Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK, null, null, Material.MAGMA_BLOCK},
            {Material.MAGMA_BLOCK, null, Material.NETHERRACK, null, null, null, Material.NETHERRACK, null, Material.MAGMA_BLOCK},
            {Material.MAGMA_BLOCK, null, Material.NETHERRACK, null, Material.GOLD_BLOCK, null, Material.NETHERRACK, null, Material.MAGMA_BLOCK},
            {Material.MAGMA_BLOCK, null, Material.NETHERRACK, null, null, null, Material.NETHERRACK, null, Material.MAGMA_BLOCK},
            {Material.MAGMA_BLOCK, null, null, Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK, null, null, Material.MAGMA_BLOCK},
            {null, Material.MAGMA_BLOCK, null, null, null, null, null, Material.MAGMA_BLOCK, null},
            {null, null, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, null, null}
        };
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int gridX = dx + 4;
                int gridZ = dz + 4;
                Material expected = grid[gridZ][gridX];
                Location loc = new Location(center.getWorld(), centerX + dx, centerY, centerZ + dz);
                Block block = loc.getBlock();
                if (expected == null) {
                    Material type = block.getType();
                    if (type == Material.GOLD_BLOCK || type == Material.NETHERRACK || type == Material.MAGMA_BLOCK) {
                        return false;
                    }
                } else {
                    if (block.getType() != expected) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean startRitual(Location centerLocation, Player player) {
        if (!isValidRitualCircle(centerLocation)) {
            return false;
        }
        if (activeRituals.containsKey(centerLocation)) {
            return false;
        }
        if (playerActiveRituals.containsKey(player.getUniqueId())) {
            plugin.getMessageUtils().sendMessage(player, "ritual-already-active");
            return false;
        }
        RitualCircle ritual = new RitualCircle(centerLocation, player.getUniqueId());
        ritual.setActive(true);
        activeRituals.put(centerLocation, ritual);
        playerActiveRituals.put(player.getUniqueId(), centerLocation);
        startRitualWaitingEffect(centerLocation);
        plugin.getMessageUtils().sendMessage(player, "ritual-started");
        return true;
    }

    public boolean cancelRitual(Location centerLocation, Player player) {
        RitualCircle ritual = activeRituals.get(centerLocation);
        if (ritual == null || !ritual.getOwner().equals(player.getUniqueId())) {
            return false;
        }

        BukkitRunnable waitingTask = waitingEffectTasks.remove(centerLocation);
        if (waitingTask != null) waitingTask.cancel();
        activeRituals.remove(centerLocation);
        playerActiveRituals.remove(player.getUniqueId());
        returnItemsToPlayer(ritual, player);
        plugin.getMessageUtils().sendMessage(player, "ritual-cancelled");
        return true;
    }

    private void returnItemsToPlayer(RitualCircle ritual, Player player) {
        Location dropLocation = ritual.getCenterLocation().clone().add(0.5, 1, 0.5);
        
        for (ItemStack item : ritual.getDroppedItems()) {
            player.getWorld().dropItem(dropLocation, item).setVelocity(
                player.getLocation().toVector().subtract(dropLocation.toVector()).normalize().multiply(0.2)
            );
        }
    }

    public void addItemToRitual(Location centerLocation, ItemStack item) {
        RitualCircle ritual = activeRituals.get(centerLocation);
        if (ritual != null && ritual.isActive()) {
            ritual.addDroppedItem(item);
            centerLocation.getWorld().playSound(centerLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
            centerLocation.getWorld().spawnParticle(Particle.CLOUD, centerLocation, 20, 0.5, 0.5, 0.5, 0.05);
            checkForRecipeMatch(ritual);
        }
    }

    public void addSacrificeToRitual(Location location, EntityType entityType) {
        for (RitualCircle ritual : activeRituals.values()) {
            if (ritual.isInCircle(location) && ritual.isActive()) {
                ritual.addSacrificedAnimal(entityType);
                location.getWorld().playSound(location, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.0f);
                
                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 2.0f);
                location.getWorld().spawnParticle(Particle.DUST, location, 30, 0.5, 0.5, 0.5, dustOptions);
                
                checkForRecipeMatch(ritual);
                break;
            }
        }
    }

    private void checkForRecipeMatch(RitualCircle ritual) {
        for (RitualRecipe recipe : ritualRecipes) {
            if (recipe.matches(ritual)) {
                executeRitual(ritual, recipe);
                return;
            }
        }
    }

    private void executeRitual(RitualCircle ritual, RitualRecipe recipe) {
        ritual.setActive(false);
        BukkitRunnable waitingTask = waitingEffectTasks.remove(ritual.getCenterLocation());
        if (waitingTask != null) waitingTask.cancel();
        Player owner = Bukkit.getPlayer(ritual.getOwner());
        if (owner != null) {
            plugin.getMessageUtils().sendMessage(owner, "ritual-executing", 
                "%ritual%", recipe.getName());
        }
        startRitualExecution(ritual, recipe, owner);
    }

    private void startRitualExecution(RitualCircle ritual, RitualRecipe recipe, Player owner) {
        Location center = ritual.getCenterLocation();
        
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 60;

            @Override
            public void run() {
                if (ticks >= duration) {
                    performRitualEffect(ritual, recipe, owner);
                    
                    activeRituals.remove(ritual.getCenterLocation());
                    if (owner != null) {
                        playerActiveRituals.remove(owner.getUniqueId());
                    }
                    
                    cancel();
                    return;
                }

                createRitualExecutionEffect(center, ticks, duration);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void performRitualEffect(RitualCircle ritual, RitualRecipe recipe, Player owner) {
        Location center = ritual.getCenterLocation();
        String effectType = recipe.getEffectType();

        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, 
            center.clone().add(0.5, 1, 0.5), 3);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.5f);

        switch (effectType.toUpperCase()) {
            case "FLYING_TORCH":
                if (owner != null) {
                    createFlyingTorch(owner, recipe);
                }
                break;
                
            case "GUARDIAN":
                summonGuardian(center, owner, recipe);
                break;
                
            case "HARVEST":
                performHarvestRitual(center, recipe);
                break;
                
            case "BLOOD_MOON":
                if (owner != null) {
                    performBloodMoonRitual(center, owner, recipe);
                }
                break;
                
            case "COMMAND":
                executeCommandRitual(recipe, owner);
                break;

            case "CRAFTING":
                if (owner != null) {
                    String result = (String) recipe.getEffectProperty("result");
                    String[] parts = result.split(":");
                    if (parts.length == 2) {
                        try {
                            Material mat = Material.valueOf(parts[0].toUpperCase());
                            int amount = Integer.parseInt(parts[1]);
                            ItemStack itemStack = new ItemStack(mat, amount);
                            owner.getWorld().dropItem(owner.getLocation().add(0.5, 1, 0.5), itemStack);
                            owner.sendMessage(plugin.getMessageUtils().getMessage("ritual-crafted")
                                    .replace("%amount%", String.valueOf(amount))
                                    .replace("%item%", mat.name().replace('_', ' ').toLowerCase()));
                        } catch (Exception e) {
                            owner.sendMessage(plugin.getMessageUtils().getMessage("unknown-crafting-result")
                                    .replace("%result%", result));
                        }
                    } else {
                        owner.sendMessage(plugin.getMessageUtils().getMessage("unknown-crafting-result")
                                .replace("%result%", result));
                    }
                }
                break;
                
            default:
                plugin.getLogger().warning(plugin.getMessageUtils().getMessage("unknown-ritual-effect")
                        .replace("%effect%", effectType));
        }
    }

    private void createFlyingTorch(Player player, RitualRecipe recipe) {
        int duration = (int) recipe.getEffectProperty("duration", 600);
        new BukkitRunnable() {
            int remainingTicks = duration * 20;
            Location lastLightLoc = null;

            @Override
            public void run() {
                if (!player.isOnline() || remainingTicks <= 0) {
                    if (lastLightLoc != null) {
                        Block block = lastLightLoc.getBlock();
                        if (block.getType() == Material.LIGHT) {
                            block.setType(Material.AIR);
                        }
                    }
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation().add(0, 2.5, 0);
                player.getWorld().spawnParticle(Particle.FLAME, playerLoc, 5, 0.3, 0.3, 0.3, 0.02);

                if (lastLightLoc != null) {
                    Block block = lastLightLoc.getBlock();
                    if (block.getType() == Material.LIGHT) {
                        block.setType(Material.AIR);
                    }
                }

                Location lightLoc = player.getLocation();
                Block lightBlock = lightLoc.getBlock();
                if (lightBlock.getType() == Material.AIR) {
                    lightBlock.setType(Material.LIGHT);
                }
                lastLightLoc = lightLoc.clone();

                remainingTicks -= 20;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void summonGuardian(Location center, Player owner, RitualRecipe recipe) {
        org.bukkit.entity.IronGolem golem = center.getWorld().spawn(
            center.clone().add(0.5, 1, 0.5), org.bukkit.entity.IronGolem.class);

        if (owner != null) {
            golem.setPlayerCreated(true);
        }

        Object healthObj = recipe.getEffectProperty("health", 80.0);
        double health = healthObj instanceof Number ? ((Number) healthObj).doubleValue() : 80.0;
        golem.setHealth(Math.max(1.0, Math.min(health, golem.getMaxHealth())));
    }

    private void performHarvestRitual(Location center, RitualRecipe recipe) {
        int radius = (int) recipe.getEffectProperty("radius", 15);
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -5; y <= 5; y++) {
                    Location cropLoc = center.clone().add(x, y, z);
                    Block block = cropLoc.getBlock();
                    
                    if (isCrop(block.getType())) {
                        growCrop(block);
                    }
                }
            }
        }
    }

    private void performBloodMoonRitual(Location center, Player owner, RitualRecipe recipe) {
        center.getWorld().setTime(18000);
        
        String bloodMoonTitle = plugin.getMessageUtils().getMessage("blood-moon-rises");
        String bloodMoonSubtitle = plugin.getMessageUtils().getMessage("blood-moon-subtitle");
        
        for (Player player : center.getWorld().getPlayers()) {
            if (player.equals(owner)) continue;
            player.sendTitle(bloodMoonTitle, bloodMoonSubtitle, 10, 70, 20);
        }

        int duration = (int) recipe.getEffectProperty("duration", 300);
        
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration * 20) {
                    cancel();
                    return;
                }

                if (ticks % 100 == 0) {
                    for (Player player : center.getWorld().getPlayers()) {
                        if (!player.equals(owner)) {
                            player.damage(2.0);
                            Particle.DustTransition dustTransition = new Particle.DustTransition(
                                Color.RED, Color.MAROON, 1.5f);
                            player.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, 
                                player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, dustTransition);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void executeCommandRitual(RitualRecipe recipe, Player owner) {
        @SuppressWarnings("unchecked")
        List<String> commands = (List<String>) recipe.getEffectProperty("commands", new ArrayList<>());
        
        for (String command : commands) {
            if (owner != null) {
                command = command.replace("%player%", owner.getName());
            }
            
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private boolean isCrop(Material material) {
        switch (material) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case COCOA:
            case NETHER_WART:
            case SWEET_BERRY_BUSH:
                return true;
            default:
                return false;
        }
    }

    private void growCrop(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.Ageable) {
            org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) block.getBlockData();
            ageable.setAge(ageable.getMaximumAge());
            block.setBlockData(ageable);
        }
    }

    private void startRitualWaitingEffect(Location center) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Color fromColor = Color.fromRGB(255, 215, 0);
                Color toColor = Color.fromRGB(255, 255, 255);
                float size = 1.2f + (float)(Math.sin(System.currentTimeMillis() / 350.0) * 0.3f);
                Particle.DustTransition dust = new Particle.DustTransition(fromColor, toColor, size);
                double radius = 3.5;
                int points = 18;
                double y = center.getY() + 1.0;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = center.getX() + 0.5 + Math.cos(angle) * radius;
                    double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
                    center.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, x, y, z, 2, 0.1, 0.1, 0.1, dust);
                    center.getWorld().spawnParticle(Particle.END_ROD, x, y + 0.2, z, 1, 0.05, 0.1, 0.05, 0.01);
                }
            }
        };
        task.runTaskTimer(plugin, 0, 10);
        waitingEffectTasks.put(center, task);
    }

    private void createRitualExecutionEffect(Location center, int currentTick, int totalTicks) {
        double progress = (double) currentTick / totalTicks;
        int swirlCount = 7;
        double swirlRadius = 2.8 * (1.0 - progress) + 0.5;
        double swirlHeight = 1.0 + (progress * 1.5);
        Color fromColor = Color.fromRGB(255, 215, 0);
        Color toColor = Color.fromRGB(255, 255, 255);
        float size = 1.2f;
        Particle.DustTransition dust = new Particle.DustTransition(fromColor, toColor, size);
        for (int i = 0; i < swirlCount; i++) {
            double angle = (currentTick * 0.25) + (i * 2 * Math.PI / swirlCount);
            double x = center.getX() + 0.5 + Math.cos(angle) * swirlRadius;
            double z = center.getZ() + 0.5 + Math.sin(angle) * swirlRadius;
            double y = center.getY() + swirlHeight + Math.sin(angle + currentTick * 0.1) * 0.3;
            center.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, x, y, z, 2, 0.1, 0.1, 0.1, dust);
            center.getWorld().spawnParticle(Particle.END_ROD, x, y + 0.2, z, 1, 0.05, 0.1, 0.05, 0.01);
        }
        if (currentTick % 15 == 0) {
            center.getWorld().spawnParticle(Particle.SCRAPE,
                center.clone().add(0.5, 1.5, 0.5), 25, 1.0, 0.5, 1.0, 0.2);
        }
        if (currentTick % 20 == 0) {
            center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.0f + (float) progress);
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.3f, 1.2f);
        }
    }

    public RitualCircle getRitual(Location location) {
        return activeRituals.get(location);
    }

    public String getCurrentRecipe(Player player) {
        Location ritualLoc = playerActiveRituals.get(player.getUniqueId());
        if (ritualLoc == null) return null;
        
        RitualCircle ritual = activeRituals.get(ritualLoc);
        return ritual != null ? ritual.getCurrentRecipeString() : null;
    }

    public boolean isPlayerInActiveRitual(Player player, Location location) {
        for (RitualCircle ritual : activeRituals.values()) {
            if (ritual.isInCircle(location) && ritual.isActive()) {
                return true;
            }
        }
        return false;
    }

    public java.util.Set<Location> getActiveRitualLocations() {
        return new java.util.HashSet<>(activeRituals.keySet());
    }

    public int getActiveRitualCount() {
        return activeRituals.size();
    }

    public int getRecipeCount() {
        return ritualRecipes.size();
    }
}