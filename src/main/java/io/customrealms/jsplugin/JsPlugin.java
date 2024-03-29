package io.customrealms.jsplugin;

import io.customrealms.runtime.DefaultLogger;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.globals.*;
import io.customrealms.runtime.Runtime;
import io.customrealms.runtime.bindgen.Bindgen;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class JsPlugin {

    private JavaPlugin java_plugin;
    private String source_code;

    private Runtime runtime;
    private BukkitCommands server_commands;

    public JsPlugin(
            JavaPlugin java_plugin,
            String source_code
    ) {

        // Save the values
        this.java_plugin = java_plugin;
        this.source_code = source_code;

        // Initialize the plugin
        this.setup();

    }

    private void setup() {

        // Create a logger instance that will be used within the JavaScript runtime
        Logger logger = new DefaultLogger(this.java_plugin.getLogger());

        // Create the runtime
        this.runtime = new Runtime(this.java_plugin, logger);

        // Create some globals separately, since we need to reference them later
        Bindgen bindgen = new Bindgen();
        this.server_commands = new BukkitCommands(bindgen);

        // Register all the globals
        this.runtime.addGlobal(

                // Allow the JavaScript runtime to reach into the Java world
                // through the "Java" global.
                bindgen,

                // Allow the JavaScript runtime to listen for server events
                new BukkitEvents(this.java_plugin, bindgen),

                // Allow the JavaScript runtime to listen for commands
                this.server_commands

        );

    }

    /**
     * Enables the plugin to start running
     */
    public void enable() {

        // Run the source code of the plugin.
        this.runtime.executeSafely(this.source_code);

        // Start the runtime event loop
        this.runtime.start();

    }

    /**
     * Stops running the plugin, if it is currently running
     */
    public void disable() {}

    /**
     * Destroys the plugin and releases its memory
     */
    public void destroy() {

        // Release the runtime
        if (this.runtime != null) {
            this.runtime.release();
            this.runtime = null;
            this.server_commands = null;
        }

        // Release all the values
        this.java_plugin = null;
        this.source_code = null;

    }

    /**
     * Attempts to execute a command in this plugin. Returns true if the command was
     * handled, regardless of the result of the command.
     * @param player the player sending the command
     * @param message the full string of the command
     * @return true if the message was handled
     */
    public boolean attemptCommand(Player player, String message) {
        if (this.server_commands == null) return false;
        return this.server_commands.attemptCommand(player, message);
    }

}
