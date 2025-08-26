package dolozimm.wispworks.listeners;

import dolozimm.wispworks.WispPlugin;
import dolozimm.wispworks.data.MagicCauldron;
import dolozimm.wispworks.managers.CauldronManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;

public class CauldronListener implements Listener {

    private final WispPlugin plugin;
    private final CauldronManager cauldronManager;

    public CauldronListener(WispPlugin plugin) {
        this.plugin = plugin;
        this.cauldronManager = plugin.getCauldronManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if ((clickedBlock.getType() == Material.CAULDRON || clickedBlock.getType() == Material.WATER_CAULDRON)
                && itemInHand.getType() == Material.AMETHYST_SHARD) {

            if (!player.hasPermission("wispworks.use")) {
                plugin.getMessageUtils().sendMessage(player, "no-permission");
                return;
            }

            if (!cauldronManager.isValidCauldronSetup(clickedBlock)) {
                plugin.getMessageUtils().sendMessage(player, "cauldron-not-valid");
                return;
            }

            if (!cauldronManager.isCauldronFull(clickedBlock)) {
                plugin.getMessageUtils().sendMessage(player, "cauldron-not-valid");
                return;
            }

            Location cauldronLocation = clickedBlock.getLocation();

            if (cauldronManager.isMagicCauldron(cauldronLocation)) {
                return;
            }

            player.getWorld().strikeLightning(cauldronLocation.clone().add(0, 1, 0));
            cauldronManager.activateCauldron(cauldronLocation);

            plugin.getMessageUtils().sendMessage(player, "cauldron-activated");

            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.WATER_CAULDRON) {
            Location cauldronLocation = block.getLocation();

            if (cauldronManager.isMagicCauldron(cauldronLocation)) {
                MagicCauldron magicCauldron = cauldronManager.getMagicCauldron(cauldronLocation);

                if (magicCauldron.hasRecipeItems()) {
                    plugin.getRecipeManager().clearCauldronRecipe(magicCauldron, player);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.CAULDRON || block.getType() == Material.WATER_CAULDRON) {
            Location cauldronLocation = block.getLocation();

            if (cauldronManager.isMagicCauldron(cauldronLocation)) {
                MagicCauldron magicCauldron = cauldronManager.getMagicCauldron(cauldronLocation);
                Player player = event.getPlayer();

                if (magicCauldron.hasRecipeItems()) {
                    plugin.getRecipeManager().clearCauldronRecipe(magicCauldron, player);
                }

                cauldronManager.removeMagicCauldron(cauldronLocation);
            }
        }
    }
}