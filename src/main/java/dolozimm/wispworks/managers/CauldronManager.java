package dolozimm.wispworks.managers;

import dolozimm.wispworks.WispPlugin;
import dolozimm.wispworks.data.MagicCauldron;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CauldronManager {

    private final WispPlugin plugin;
    private final Map<Location, MagicCauldron> magicCauldrons;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public CauldronManager(WispPlugin plugin) {
        this.plugin = plugin;
        this.magicCauldrons = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "cauldrons.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe(plugin.getMessageUtils().getMessage("could-not-create-cauldrons-file"));
                e.printStackTrace();
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isValidCauldronSetup(Block cauldron) {
        if (cauldron.getType() != Material.CAULDRON && cauldron.getType() != Material.WATER_CAULDRON) {
            return false;
        }

        Block below = cauldron.getRelative(0, -1, 0);
        return below.getType() == Material.FIRE || below.getType() == Material.CAMPFIRE;
    }

    public boolean isCauldronFull(Block cauldron) {
        if (cauldron.getType() != Material.WATER_CAULDRON) {
            return false;
        }

        Levelled levelled = (Levelled) cauldron.getBlockData();
        return levelled.getLevel() == levelled.getMaximumLevel();
    }

    public void activateCauldron(Location location) {
        MagicCauldron magicCauldron = new MagicCauldron(location);
        magicCauldrons.put(location, magicCauldron);
        saveCauldrons();
    }

    public boolean isMagicCauldron(Location location) {
        return magicCauldrons.containsKey(location);
    }

    public MagicCauldron getMagicCauldron(Location location) {
        return magicCauldrons.get(location);
    }

    public void removeMagicCauldron(Location location) {
        magicCauldrons.remove(location);
        saveCauldrons();
    }

    public void emptyCauldron(Block cauldron) {
        cauldron.setType(Material.CAULDRON);

        Block below = cauldron.getRelative(0, -1, 0);
        if (below.getType() == Material.FIRE) {
            below.setType(Material.AIR);
        } else if (below.getType() == Material.CAMPFIRE) {
            below.setType(Material.CAMPFIRE);
            org.bukkit.block.data.type.Campfire campfireData = (org.bukkit.block.data.type.Campfire) below.getBlockData();
            campfireData.setLit(false);
            below.setBlockData(campfireData);
        }
    }

    public void saveCauldrons() {
        dataConfig.set("cauldrons", null);

        for (Map.Entry<Location, MagicCauldron> entry : magicCauldrons.entrySet()) {
            Location loc = entry.getKey();
            String key = "cauldrons." + UUID.randomUUID().toString();

            dataConfig.set(key + ".world", loc.getWorld().getName());
            dataConfig.set(key + ".x", loc.getBlockX());
            dataConfig.set(key + ".y", loc.getBlockY());
            dataConfig.set(key + ".z", loc.getBlockZ());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe(plugin.getMessageUtils().getMessage("could-not-save-cauldrons-file"));
            e.printStackTrace();
        }
    }

    public void loadCauldrons() {
        magicCauldrons.clear();

        if (!dataConfig.contains("cauldrons")) {
            return;
        }

        for (String key : dataConfig.getConfigurationSection("cauldrons").getKeys(false)) {
            String worldName = dataConfig.getString("cauldrons." + key + ".world");
            int x = dataConfig.getInt("cauldrons." + key + ".x");
            int y = dataConfig.getInt("cauldrons." + key + ".y");
            int z = dataConfig.getInt("cauldrons." + key + ".z");

            if (plugin.getServer().getWorld(worldName) != null) {
                Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);
                MagicCauldron magicCauldron = new MagicCauldron(location);
                magicCauldrons.put(location, magicCauldron);
            }
        }

        plugin.getLogger().info(plugin.getMessageUtils().getMessage("loaded-cauldrons")
                .replace("%count%", String.valueOf(magicCauldrons.size())));
    }

    public int getMagicCauldronCount() {
        return magicCauldrons.size();
    }
}