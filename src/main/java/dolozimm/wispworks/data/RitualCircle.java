package dolozimm.wispworks.data;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RitualCircle {
    
    private final UUID id;
    private final Location centerLocation;
    private final List<ItemStack> droppedItems;
    private final Map<EntityType, Integer> sacrificedAnimals;
    private final long creationTime;
    private boolean isActive;
    private UUID owner;

    public RitualCircle(Location centerLocation, UUID owner) {
        this.id = UUID.randomUUID();
        this.centerLocation = centerLocation;
        this.droppedItems = new ArrayList<>();
        this.sacrificedAnimals = new HashMap<>();
        this.creationTime = System.currentTimeMillis();
        this.isActive = false;
        this.owner = owner;
    }

    public UUID getId() {
        return id;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public List<ItemStack> getDroppedItems() {
        return new ArrayList<>(droppedItems);
    }

    public void addDroppedItem(ItemStack item) {
        droppedItems.add(item.clone());
    }

    public void clearDroppedItems() {
        droppedItems.clear();
    }

    public Map<EntityType, Integer> getSacrificedAnimals() {
        return new HashMap<>(sacrificedAnimals);
    }

    public void addSacrificedAnimal(EntityType entityType) {
        sacrificedAnimals.put(entityType, sacrificedAnimals.getOrDefault(entityType, 0) + 1);
    }

    public void clearSacrificedAnimals() {
        sacrificedAnimals.clear();
    }

    public void clearAll() {
        clearDroppedItems();
        clearSacrificedAnimals();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isInCircle(Location location) {
        if (!location.getWorld().equals(centerLocation.getWorld())) {
            return false;
        }

        double distance = location.distance(centerLocation);
        return distance <= 3.5;
    }

    public String getCurrentRecipeString() {
        StringBuilder recipe = new StringBuilder();
        
        if (!droppedItems.isEmpty()) {
            recipe.append("Items: ");
            Map<Material, Integer> itemCounts = new HashMap<>();
            for (ItemStack item : droppedItems) {
                itemCounts.put(item.getType(), itemCounts.getOrDefault(item.getType(), 0) + item.getAmount());
            }
            
            boolean first = true;
            for (Map.Entry<Material, Integer> entry : itemCounts.entrySet()) {
                if (!first) recipe.append(", ");
                recipe.append(entry.getValue()).append("x ").append(entry.getKey().name());
                first = false;
            }
        }

        if (!sacrificedAnimals.isEmpty()) {
            if (recipe.length() > 0) recipe.append(" | ");
            recipe.append("Sacrifices: ");
            
            boolean first = true;
            for (Map.Entry<EntityType, Integer> entry : sacrificedAnimals.entrySet()) {
                if (!first) recipe.append(", ");
                recipe.append(entry.getValue()).append("x ").append(entry.getKey().name());
                first = false;
            }
        }

        return recipe.length() > 0 ? recipe.toString() : "Empty ritual";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RitualCircle that = (RitualCircle) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RitualCircle{" +
                "id=" + id +
                ", centerLocation=" + centerLocation +
                ", itemCount=" + droppedItems.size() +
                ", sacrificeCount=" + sacrificedAnimals.size() +
                ", isActive=" + isActive +
                '}';
    }
}