package dolozimm.wispworks.managers;

import dolozimm.wispworks.WispPlugin;
import dolozimm.wispworks.data.MagicCauldron;
import dolozimm.wispworks.data.Recipe;
import dolozimm.wispworks.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RecipeManager {
    
    private final WispPlugin plugin;
    private final Map<Recipe, ItemStack> recipes;
    
    public RecipeManager(WispPlugin plugin) {
        this.plugin = plugin;
        this.recipes = new HashMap<>();
        loadRecipes();
    }
    
    public void loadRecipes() {
        recipes.clear();
        
        if (!plugin.getConfig().contains("recipes")) {
            plugin.getLogger().warning(plugin.getMessageUtils().getMessage("no-recipes-section"));
            return;
        }
        
        Set<String> recipeKeys = plugin.getConfig().getConfigurationSection("recipes").getKeys(false);
        
        for (String resultItemName : recipeKeys) {
            String ingredientsString = plugin.getConfig().getString("recipes." + resultItemName);
            
            if (ingredientsString == null || ingredientsString.trim().isEmpty()) {
                plugin.getLogger().warning(plugin.getMessageUtils().getMessage("empty-recipe")
                    .replace("%recipe%", resultItemName));
                continue;
            }
            
            String[] ingredientNames = ingredientsString.split(",");
            Recipe recipe = new Recipe();
            boolean validRecipe = true;
            
            for (String ingredientName : ingredientNames) {
                ingredientName = ingredientName.trim().toLowerCase();
                
                try {
                    Material material = Material.valueOf(ingredientName.toUpperCase());
                    recipe.addIngredient(material, 1);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(plugin.getMessageUtils().getMessage("invalid-material-recipe")
                        .replace("%recipe%", resultItemName)
                        .replace("%material%", ingredientName));
                    validRecipe = false;
                    break;
                }
            }
            
            if (!validRecipe) {
                continue;
            }
            
            ItemStack resultItem = createResultItem(resultItemName.toLowerCase());
            if (resultItem != null) {
                recipes.put(recipe, resultItem);
                plugin.getLogger().info(plugin.getMessageUtils().getMessage("loaded-recipe")
                    .replace("%recipe%", resultItemName)
                    .replace("%ingredients%", ingredientsString));
            }
        }
        
        plugin.getLogger().info(plugin.getMessageUtils().getMessage("loaded-recipes-count")
            .replace("%count%", String.valueOf(recipes.size())));
    }
    
    private ItemStack createResultItem(String itemName) {
        ItemStack result;
        
        switch (itemName) {
            case "mutation_bonemeal":
                return ItemUtils.createMutationBonemeal();
                
            case "mutation_apple":
                return ItemUtils.createMutationApple();
                
            default:
                try {
                    Material material = Material.valueOf(itemName.toUpperCase());
                    result = new ItemStack(material, 1);
                    return result;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(plugin.getMessageUtils().getMessage("invalid-result-material")
                        .replace("%material%", itemName));
                    return null;
                }
        }
    }
    
    public void addItemToCauldron(Location cauldronLocation, ItemStack item, Player player) {
        MagicCauldron cauldron = plugin.getCauldronManager().getMagicCauldron(cauldronLocation);
        if (cauldron == null) return;
        
        int maxItems = plugin.getConfig().getInt("cauldron.max-recipe-items", 10);
        if (cauldron.getRecipeItems().size() >= maxItems) {
            plugin.getMessageUtils().sendMessage(player, "cauldron-full");
            return;
        }
        
        cauldron.addRecipeItem(item);
        plugin.getMessageUtils().sendMessage(player, "item-added");
        
        checkForRecipeMatch(cauldron, player);
    }
    
    private void checkForRecipeMatch(MagicCauldron cauldron, Player player) {
        List<ItemStack> cauldronItems = cauldron.getRecipeItems();
        
        for (Map.Entry<Recipe, ItemStack> entry : recipes.entrySet()) {
            Recipe recipe = entry.getKey();
            
            if (recipe.matches(cauldronItems)) {
                startCraftingProcess(cauldron, entry.getValue().clone(), player);
                return;
            }
        }
    }
    
    private void startCraftingProcess(MagicCauldron cauldron, ItemStack result, Player player) {
        Location location = cauldron.getLocation();
        Block cauldronBlock = location.getBlock();
        
        int swirlDuration = plugin.getConfig().getInt("cauldron.particles.swirl-duration", 3);
        int explosionCount = plugin.getConfig().getInt("cauldron.particles.explosion-count", 20);
        
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = swirlDuration * 20;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    for (int i = 0; i < explosionCount; i++) {
                        double offsetX = (Math.random() - 0.5) * 2.0;
                        double offsetY = Math.random() * 2.0;
                        double offsetZ = (Math.random() - 0.5) * 2.0;
                        
                        player.getWorld().spawnParticle(
                            org.bukkit.Particle.EXPLOSION,
                            location.clone().add(0.5 + offsetX, 1 + offsetY, 0.5 + offsetZ),
                            1
                        );
                    }
                    
                    Item droppedItem = player.getWorld().dropItem(location.clone().add(0.5, 2, 0.5), result);
                    droppedItem.setVelocity(droppedItem.getVelocity().multiply(0.1));
                    
                    player.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    
                    cauldron.clearRecipe();
                    plugin.getCauldronManager().emptyCauldron(cauldronBlock);
                    
                    plugin.getMessageUtils().sendMessage(player, "crafting-complete");
                    
                    cancel();
                    return;
                }
                
                double angle = (ticks * 0.1) % (2 * Math.PI);
                double radius = 0.8;
                double x = location.getX() + 0.5 + Math.cos(angle) * radius;
                double z = location.getZ() + 0.5 + Math.sin(angle) * radius;
                double y = location.getY() + 1 + Math.sin(ticks * 0.05) * 0.3;
                
                player.getWorld().spawnParticle(
                    org.bukkit.Particle.SCRAPE,
                    x, y, z,
                    2, 0.1, 0.1, 0.1, 0.02
                );
                
                if (ticks % 10 == 0) {
                    player.getWorld().spawnParticle(
                        org.bukkit.Particle.PORTAL,
                        location.clone().add(0.5, 1, 0.5),
                        5, 0.3, 0.3, 0.3, 0.02
                    );
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    public void clearCauldronRecipe(MagicCauldron cauldron, Player player) {
        Location location = cauldron.getLocation();
        
        for (ItemStack item : cauldron.getRecipeItems()) {
            Item droppedItem = player.getWorld().dropItem(location.clone().add(0.5, 2, 0.5), item);
            droppedItem.setVelocity(droppedItem.getVelocity().multiply(0.1));
        }
        
        cauldron.clearRecipe();
        plugin.getMessageUtils().sendMessage(player, "recipe-cleared");
    }
    
    public int getRecipeCount() {
        return recipes.size();
    }
    
    public Set<String> getRecipeNames() {
        Set<String> names = new HashSet<>();
        for (Map.Entry<Recipe, ItemStack> entry : recipes.entrySet()) {
            ItemStack result = entry.getValue();
            if (ItemUtils.isMutationBonemeal(result)) {
                names.add("mutation_bonemeal");
            } else if (ItemUtils.isMutationApple(result)) {
                names.add("mutation_apple");
            } else {
                names.add(result.getType().name().toLowerCase());
            }
        }
        return names;
    }
}