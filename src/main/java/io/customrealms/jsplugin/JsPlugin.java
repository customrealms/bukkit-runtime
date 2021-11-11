package io.customrealms.jsplugin;

import io.customrealms.runtime.globals.*;
import io.customrealms.runtime.Runtime;
import io.customrealms.runtime.bindgen.Bindgen;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class JsPlugin {

    private JavaPlugin java_plugin;
    private JsPluginDescriptor descriptor;
    private String source_code;

    private Runtime runtime;
    private ServerCommands server_commands;

    public JsPlugin(
            JavaPlugin java_plugin,
            JsPluginDescriptor descriptor,
            String source_code
    ) {

        // Save the values
        this.java_plugin = java_plugin;
        this.descriptor = descriptor;
        this.source_code = source_code;

        // Initialize the plugin
        this.setup();

    }

    private void setup() {

        // Create the runtime
        this.runtime = new Runtime(/* logger */);

        // Create some globals separately, since we need to reference them later
        Bindgen bindgen = new Bindgen(null);
        this.server_commands = new ServerCommands();

        // Register all the globals
        this.runtime.addGlobal(

                // Allow the JavaScript runtime to reach into the Java world
                // through the "Java" global.
                bindgen,

                // setTimeout, setInterval, etc
                new Timeout(this.java_plugin),

                // console.log, console.warn, console.error
                new Console(),

                // Allow the JavaScript runtime to listen for server events
                new ServerEvents(this.java_plugin, bindgen),

                // Allow the JavaScript runtime to listen for commands
                this.server_commands

        );

    }

    /**
     * Enables the plugin to start running
     */
    public void enable() {

        // Run the source code of the plugin
        this.runtime.executeSafely(
                "'use strict';\n" + this.source_code,
                this.descriptor.name,
                1
        );

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
        this.descriptor = null;
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
