package dolozimm.wispworks.data;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MagicCauldron {

    private final UUID id;
    private final Location location;
    private final List<ItemStack> recipeItems;
    private final long creationTime;

    public MagicCauldron(Location location) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.recipeItems = new ArrayList<>();
        this.creationTime = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public List<ItemStack> getRecipeItems() {
        return new ArrayList<>(recipeItems);
    }

    public void addRecipeItem(ItemStack item) {
        recipeItems.add(item.clone());
    }

    public void clearRecipe() {
        recipeItems.clear();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public int getRecipeItemCount() {
        return recipeItems.size();
    }

    public boolean hasRecipeItems() {
        return !recipeItems.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MagicCauldron that = (MagicCauldron) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "MagicCauldron{" +
                "id=" + id +
                ", location=" + location +
                ", recipeItems=" + recipeItems.size() +
                ", creationTime=" + creationTime +
                '}';
    }
}