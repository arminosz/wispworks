package dolozimm.wispworks.utils;

import dolozimm.wispworks.WispPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class MessageUtils {

    private final WispPlugin plugin;
    private final Map<String, String> messages;

    public MessageUtils(WispPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        loadMessages();
    }

    public void loadMessages() {
        messages.clear();

        if (plugin.getConfig().contains("messages")) {
            for (String key : plugin.getConfig().getConfigurationSection("messages").getKeys(false)) {
                String message = plugin.getConfig().getString("messages." + key, "");
                messages.put(key, ChatColor.translateAlternateColorCodes('&', message));
            }
        }

        setDefaultIfMissing("prefix", "&6[WispWorks] ");
        setDefaultIfMissing("message-not-found", "&cMessage not found: %key%");
    }

    private void setDefaultIfMissing(String key, String defaultMessage) {
        if (!messages.containsKey(key)) {
            messages.put(key, ChatColor.translateAlternateColorCodes('&', defaultMessage));
        }
    }

    public void sendMessage(CommandSender sender, String messageKey) {
        String message = getMessage(messageKey);
        if (!message.isEmpty() && !message.startsWith("§cMessage not found:")) {
            sender.sendMessage(getPrefix() + message);
        } else if (message.startsWith("§cMessage not found:")) {
            sender.sendMessage(getPrefix() + message);
        }
    }

    public void sendMessage(CommandSender sender, String messageKey, String... replacements) {
        String message = getMessage(messageKey);

        if (!message.isEmpty()) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
            
            if (!message.startsWith("§cMessage not found:")) {
                sender.sendMessage(getPrefix() + message);
            } else {
                sender.sendMessage(getPrefix() + message);
            }
        }
    }

    public String getMessage(String key) {
        String message = messages.getOrDefault(key, null);
        if (message == null) {
            String fallback = messages.getOrDefault("message-not-found", "§cMessage not found: %key%");
            return fallback.replace("%key%", key);
        }
        return message;
    }

    public String getPrefix() {
        return messages.getOrDefault("prefix", "§6[WispWorks] ");
    }

    public void reloadMessages() {
        loadMessages();
    }
}