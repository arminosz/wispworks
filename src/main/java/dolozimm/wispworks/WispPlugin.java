package dolozimm.wispworks;

import dolozimm.wispworks.commands.WispWorksCommand;
import dolozimm.wispworks.listeners.CauldronListener;
import dolozimm.wispworks.listeners.ItemListener;
import dolozimm.wispworks.listeners.RitualListener;
import dolozimm.wispworks.managers.CauldronManager;
import dolozimm.wispworks.managers.RecipeManager;
import dolozimm.wispworks.utils.MessageUtils;
import dolozimm.wispworks.managers.RitualManager;
import dolozimm.wispworks.utils.ItemUtils;
import org.bukkit.plugin.java.JavaPlugin;


public class WispPlugin extends JavaPlugin {
    
    private static WispPlugin instance;
    private CauldronManager cauldronManager;
    private RecipeManager recipeManager;
    private RitualManager ritualManager;
    private MessageUtils messageUtils;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        this.messageUtils = new MessageUtils(this);
        ItemUtils.setPlugin(this);
        this.cauldronManager = new CauldronManager(this);
        this.recipeManager = new RecipeManager(this);
        this.ritualManager = new RitualManager(this);
        
        cauldronManager.loadCauldrons();
        
        getServer().getPluginManager().registerEvents(new CauldronListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new RitualListener(this), this);
        
        getCommand("wispworks").setExecutor(new WispWorksCommand(this));
        
        getLogger().info(messageUtils.getMessage("plugin-enabled"));
    }
    
    @Override
    public void onDisable() {
        if (cauldronManager != null) {
            cauldronManager.saveCauldrons();
        }
        
        getLogger().info(messageUtils.getMessage("plugin-disabled"));
    }
    
    public static WispPlugin getInstance() {
        return instance;
    }
    
    public CauldronManager getCauldronManager() {
        return cauldronManager;
    }
    
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public RitualManager getRitualManager() {
        return ritualManager;
    }
    
    public MessageUtils getMessageUtils() {
        return messageUtils;
    }
    
    public void reloadPlugin() {
        java.io.File configFile = new java.io.File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        } else {
            reloadConfig();
        }
        messageUtils.reloadMessages();
        cauldronManager.loadCauldrons();
        recipeManager.loadRecipes();
        ritualManager.loadRitualRecipes();
    }
}