package io.customrealms.controller;

import io.customrealms.jsplugin.JsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A Controller instance makes it possible to run one or more JavaScript plugins
 * by binding into the Bukkit events (commands, etc) and making them interact
 * with the JavaScript plugin runtimes.
 */
public class Controller {

    /**
     * Event listener instances for the controller
     */
    private CommandListener command_listener;

    /**
     * The JavaPlugin in which we're running (since all Java plugins are operating in the memory space
     * of the CraftBukkit / Spigot / etc. process)
     */
    private JavaPlugin java_plugin;

    /**
     * Array of plugins being operated by this Controller
     */
    private JsPlugin[] plugins;

    /**
     * Constructs a Controller instance that operates a specific set of plugins within the given JavaPlugin
     * @param java_plugin the Java plugin we're operating within
     * @param plugins the JavaScript plugins to operate
     */
    public Controller(
            JavaPlugin java_plugin,
            JsPlugin... plugins
    ) {

        // Save the passed-in values on the object
        this.java_plugin = java_plugin;
        this.plugins = plugins;

        // Create the event listeners
        this.command_listener = new CommandListener(plugins);

    }

    /**
     * Called when the plugin / server is enabled
     */
    public void enable() {

        // Register all the event listeners
        PluginManager plugin_manager = Bukkit.getServer().getPluginManager();
        plugin_manager.registerEvents(this.command_listener, this.java_plugin);

        // Enable all of the plugins
        for (JsPlugin plugin : this.plugins) {
            plugin.enable();
        }

    }

    /**
     * Called when the plugin / server is disabled
     */
    public void disable() {

        // Destroy the command listener
        this.command_listener.destroy();
        this.command_listener = null;

        // Disable all of the plugins
        for (JsPlugin plugin : this.plugins) {
            plugin.disable();
            plugin.destroy();
        }

        // Let go of references
        this.plugins = null;
        this.java_plugin = null;

    }

}
