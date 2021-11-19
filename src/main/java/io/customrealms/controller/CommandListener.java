package io.customrealms.controller;

import io.customrealms.jsplugin.JsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    /**
     * The array of plugins that commands should be sent to
     */
    private JsPlugin[] plugins;

    /**
     * Constructs a command listener instance, sending commands to a set of plugins
     * @param plugins the plugins to pipe commands to
     */
    public CommandListener(JsPlugin[] plugins) {
        this.plugins = plugins;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        // Get the command message
        String message = event.getMessage().trim();

        // Get the player
        Player player = event.getPlayer();

        // Attempt to handle the command with the plugins
        boolean handled = this.attemptCommand(player, message);

        // If the event was handled, cancel it. Otherwise we still want to allow other plugins
        // and the default Bukkit / Minecraft commands.
        if (handled) event.setCancelled(true);

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

    public void destroy() {
        this.plugins = null;
    }

}
