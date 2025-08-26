package dolozimm.wispworks.commands;

import dolozimm.wispworks.WispPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WispWorksCommand implements CommandExecutor, TabCompleter {
    
    private final WispPlugin plugin;
    
    public WispWorksCommand(WispPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.getMessageUtils().sendMessage(sender, "plugin-info");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("wispworks.admin")) {
                    plugin.getMessageUtils().sendMessage(sender, "no-permission");
                    return true;
                }
                
                plugin.reloadPlugin();
                plugin.getMessageUtils().sendMessage(sender, "reload-complete");
                return true;
                
            case "info":
                plugin.getMessageUtils().sendMessage(sender, "plugin-info");
                plugin.getMessageUtils().sendMessage(sender, "active-cauldrons", 
                    "%count%", String.valueOf(plugin.getCauldronManager().getMagicCauldronCount()));
                plugin.getMessageUtils().sendMessage(sender, "loaded-recipes", 
                    "%count%", String.valueOf(plugin.getRecipeManager().getRecipeCount()));
                plugin.getMessageUtils().sendMessage(sender, "ritual-recipes", 
                    "%count%", String.valueOf(plugin.getRitualManager().getRecipeCount()));
                
                if (sender.hasPermission("wispworks.admin")) {
                    plugin.getMessageUtils().sendMessage(sender, "available-recipes", 
                        "%recipes%", String.join(", ", plugin.getRecipeManager().getRecipeNames()));
                }
                return true;
                
            case "give":
                if (!sender.hasPermission("wispworks.admin")) {
                    plugin.getMessageUtils().sendMessage(sender, "no-permission");
                    return true;
                }
                
                if (args.length < 2) {
                    plugin.getMessageUtils().sendMessage(sender, "give-usage");
                    return true;
                }
                
                return handleGiveCommand(sender, args);
                
            default:
                plugin.getMessageUtils().sendMessage(sender, "unknown-command");
                return true;
        }
    }
    
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        String itemType = args[1].toLowerCase();
        Player targetPlayer;
        
        if (args.length >= 3) {
            targetPlayer = plugin.getServer().getPlayer(args[2]);
            if (targetPlayer == null) {
                plugin.getMessageUtils().sendMessage(sender, "player-not-found", "%player%", args[2]);
                return true;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
        } else {
            plugin.getMessageUtils().sendMessage(sender, "console-needs-player");
            return true;
        }
        
        switch (itemType) {
            case "mutation-bonemeal":
                targetPlayer.getInventory().addItem(dolozimm.wispworks.utils.ItemUtils.createMutationBonemeal());
                plugin.getMessageUtils().sendMessage(sender, "gave-mutation-bonemeal", "%player%", targetPlayer.getName());
                plugin.getMessageUtils().sendMessage(targetPlayer, "received-mutation-bonemeal");
                return true;
            case "mutation-apple":
                targetPlayer.getInventory().addItem(dolozimm.wispworks.utils.ItemUtils.createMutationApple());
                plugin.getMessageUtils().sendMessage(sender, "gave-mutation-apple", "%player%", targetPlayer.getName());
                plugin.getMessageUtils().sendMessage(targetPlayer, "received-mutation-apple");
                return true;
            default:
                plugin.getMessageUtils().sendMessage(sender, "unknown-item-type");
                return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            for (String subCommand : Arrays.asList("reload", "info", "give")) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
            
            return completions;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> completions = new ArrayList<>();
            String partial = args[1].toLowerCase();
            for (String itemType : Arrays.asList("mutation-bonemeal", "mutation-apple")) {
                if (itemType.startsWith(partial)) {
                    completions.add(itemType);
                }
            }
            return completions;
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> completions = new ArrayList<>();
            String partial = args[2].toLowerCase();
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}