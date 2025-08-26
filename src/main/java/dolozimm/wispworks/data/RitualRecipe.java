package dolozimm.wispworks.data;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RitualRecipe {
    
    private final String name;
    private final Map<Material, Integer> requiredItems;
    private final Map<EntityType, Integer> requiredSacrifices;
    private final String effectType;
    private final Map<String, Object> effectProperties;

    public RitualRecipe(String name) {
        this.name = name;
        this.requiredItems = new HashMap<>();
        this.requiredSacrifices = new HashMap<>();
        this.effectType = "NONE";
        this.effectProperties = new HashMap<>();
    }

    public RitualRecipe(String name, String effectType) {
        this.name = name;
        this.requiredItems = new HashMap<>();
        this.requiredSacrifices = new HashMap<>();
        this.effectType = effectType;
        this.effectProperties = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Map<Material, Integer> getRequiredItems() {
        return new HashMap<>(requiredItems);
    }

    public void addRequiredItem(Material material, int amount) {
        requiredItems.put(material, requiredItems.getOrDefault(material, 0) + amount);
    }

    public Map<EntityType, Integer> getRequiredSacrifices() {
        return new HashMap<>(requiredSacrifices);
    }

    public void addRequiredSacrifice(EntityType entityType, int amount) {
        requiredSacrifices.put(entityType, requiredSacrifices.getOrDefault(entityType, 0) + amount);
    }

    public String getEffectType() {
        return effectType;
    }

    public Map<String, Object> getEffectProperties() {
        return new HashMap<>(effectProperties);
    }

    public void setEffectProperty(String key, Object value) {
        effectProperties.put(key, value);
    }

    public Object getEffectProperty(String key) {
        return effectProperties.get(key);
    }

    public Object getEffectProperty(String key, Object defaultValue) {
        return effectProperties.getOrDefault(key, defaultValue);
    }

    public boolean matches(RitualCircle circle) {
        Map<Material, Integer> circleItems = new HashMap<>();
        for (org.bukkit.inventory.ItemStack item : circle.getDroppedItems()) {
            if (item != null && item.getType() != Material.AIR) {
                circleItems.put(item.getType(), circleItems.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        Map<EntityType, Integer> circleSacrifices = circle.getSacrificedAnimals();

        if ("CRAFTING".equalsIgnoreCase(effectType)) {
            for (Map.Entry<Material, Integer> required : requiredItems.entrySet()) {
                Integer circleAmount = circleItems.get(required.getKey());
                if (circleAmount == null || circleAmount < required.getValue()) {
                    return false;
                }
            }
            for (Map.Entry<EntityType, Integer> required : requiredSacrifices.entrySet()) {
                Integer circleAmount = circleSacrifices.get(required.getKey());
                if (circleAmount == null || circleAmount < required.getValue()) {
                    return false;
                }
            }
            return true;
        } else {
            if (circleItems.size() != requiredItems.size() || 
                circleSacrifices.size() != requiredSacrifices.size()) {
                return false;
            }
            for (Map.Entry<Material, Integer> required : requiredItems.entrySet()) {
                Integer circleAmount = circleItems.get(required.getKey());
                if (circleAmount == null || !circleAmount.equals(required.getValue())) {
                    return false;
                }
            }
            for (Map.Entry<EntityType, Integer> required : requiredSacrifices.entrySet()) {
                Integer circleAmount = circleSacrifices.get(required.getKey());
                if (circleAmount == null || !circleAmount.equals(required.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public String toString() {
        return "RitualRecipe{" +
                "name='" + name + '\'' +
                ", requiredItems=" + requiredItems +
                ", requiredSacrifices=" + requiredSacrifices +
                ", effectType='" + effectType + '\'' +
                '}';
    }
}