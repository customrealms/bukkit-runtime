package io.customrealms.controller;

import io.customrealms.jsplugin.JsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    /**
     * The chat prefix to send with commands
     */
    private static final String PREFIX = ChatColor.BLACK + "[" + ChatColor.BLUE + ChatColor.BOLD + "SERVER" + ChatColor.RESET + ChatColor.BLACK + "] " + ChatColor.RESET;
    private static final String ERROR_PREFIX = ChatColor.BLACK + "[" + ChatColor.RED + ChatColor.BOLD + "ERROR" + ChatColor.RESET + ChatColor.BLACK + "] " + ChatColor.RESET;

    private final JsPlugin[] plugins;

    public CommandListener(JsPlugin[] plugins) {
        this.plugins = plugins;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        // Cancel the default Bukkit / Minecraft events
        event.setCancelled(true);

        // Get the command message
        String message = event.getMessage().trim();

        // Get the player
        Player player = event.getPlayer();

        // Attempt to handle the command with the plugins
        boolean handled = this.attemptCommand(player, message);

        // If it was not handled
        if (!handled) {

            // Send the player the message
            player.sendMessage(CommandListener.ERROR_PREFIX + ChatColor.RED + "Unhandled command: " + ChatColor.GOLD + event.getMessage());

        }

    }

    /**
     * Attempts to execute a command on the installed plugins
     * @param player the player executing the command
     * @param message the string value of the command
     * @return true if the command was handled by one or more plugins
     */
    private boolean attemptCommand(Player player, String message) {

        // If there are no plugins, always return false
        if (this.plugins == null) return false;

        // Track if it was handled
        boolean handled = false;

        // Loop through all of the plugins
        for (JsPlugin plugin : this.plugins) {

            // Attempt the command on the plugin
            if (plugin.attemptCommand(player, message)) handled = true;

        }

        // Return the handled status
        return handled;

    }

}
