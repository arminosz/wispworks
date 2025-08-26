package dolozimm.wispworks.utils;

import dolozimm.wispworks.WispPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ItemUtils {
    
    private static WispPlugin plugin;
    
    public static void setPlugin(WispPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static ItemStack createMutationBonemeal() {
        ItemStack item = new ItemStack(Material.BONE_MEAL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getMessageUtils().getMessage("mutation-bonemeal-name"));
            meta.setLore(Arrays.asList(
                    plugin.getMessageUtils().getMessage("mutation-bonemeal-lore-1"),
                    plugin.getMessageUtils().getMessage("mutation-bonemeal-lore-2"),
                    plugin.getMessageUtils().getMessage("mutation-bonemeal-lore-3"),
                    plugin.getMessageUtils().getMessage("mutation-bonemeal-lore-4"),
                    plugin.getMessageUtils().getMessage("mutation-bonemeal-lore-5")
            ));

            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createMutationApple() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getMessageUtils().getMessage("mutation-apple-name"));
            meta.setLore(Arrays.asList(
                    plugin.getMessageUtils().getMessage("mutation-apple-lore-1"),
                    plugin.getMessageUtils().getMessage("mutation-apple-lore-2"),
                    plugin.getMessageUtils().getMessage("mutation-apple-lore-3"),
                    plugin.getMessageUtils().getMessage("mutation-apple-lore-4"),
                    plugin.getMessageUtils().getMessage("mutation-apple-lore-5")
            ));

            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }

        return item;
    }

    public static boolean isMutationBonemeal(ItemStack item) {
        if (item == null || item.getType() != Material.BONE_MEAL) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String expectedName = plugin.getMessageUtils().getMessage("mutation-bonemeal-name");
        return expectedName.equals(meta.getDisplayName()) &&
                meta.hasEnchant(Enchantment.UNBREAKING) &&
                meta.getEnchantLevel(Enchantment.UNBREAKING) == 3;
    }

    public static boolean isMutationApple(ItemStack item) {
        if (item == null || item.getType() != Material.APPLE) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String expectedName = plugin.getMessageUtils().getMessage("mutation-apple-name");
        return expectedName.equals(meta.getDisplayName()) &&
                meta.hasEnchant(Enchantment.UNBREAKING) &&
                meta.getEnchantLevel(Enchantment.UNBREAKING) == 3;
    }

    public static boolean isSpecialItem(ItemStack item) {
        return isMutationBonemeal(item) || isMutationApple(item);
    }
}