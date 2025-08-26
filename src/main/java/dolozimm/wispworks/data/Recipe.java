package dolozimm.wispworks.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Recipe {

    private final Map<Material, Integer> ingredients;

    public Recipe() {
        this.ingredients = new HashMap<>();
    }

    public void addIngredient(Material material, int amount) {
        ingredients.put(material, ingredients.getOrDefault(material, 0) + amount);
    }

    public Map<Material, Integer> getIngredients() {
        return new HashMap<>(ingredients);
    }

    public boolean matches(List<ItemStack> items) {
        Map<Material, Integer> itemCounts = new HashMap<>();

        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                itemCounts.put(item.getType(), itemCounts.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        if (itemCounts.size() != ingredients.size()) {
            return false;
        }

        for (Map.Entry<Material, Integer> ingredient : ingredients.entrySet()) {
            Integer itemCount = itemCounts.get(ingredient.getKey());
            if (itemCount == null || !itemCount.equals(ingredient.getValue())) {
                return false;
            }
        }

        return true;
    }

    public int getTotalIngredientCount() {
        return ingredients.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return ingredients.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Recipe recipe = (Recipe) obj;
        return ingredients.equals(recipe.ingredients);
    }

    @Override
    public int hashCode() {
        return ingredients.hashCode();
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "ingredients=" + ingredients +
                '}';
    }
}