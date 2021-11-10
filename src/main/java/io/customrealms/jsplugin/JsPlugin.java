package io.customrealms.jsplugin;

import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import io.customrealms.runtime.globals.*;
import io.customrealms.runtime.Runtime;
import io.customrealms.runtime.SafeExecutor;
import io.customrealms.runtime.bindgen.Bindgen;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class JsPlugin {

    private final JavaPlugin java_plugin;
    private final JsPluginDescriptor descriptor;
    private final String source_code;

    private Runtime runtime;
    private V8Object js_handle;

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

        // Create the bindgen global separately, since we need to reference it later
        Bindgen bindgen = new Bindgen(/* new Class<?>[]{
                Player.class,
                Server.class,
                Plugin.class,
                Bukkit.class,
                World.class,
                Location.class,
                Note.class,
                Note.Tone.class
        }*/ null);

        // Create a bootstrap global
        Bootstrap bootstrap = new Bootstrap();

        // Register all the globals
        this.runtime.addGlobal(

                // Allow the JavaScript runtime to reach into the Java world
                // through the "Java" global.
                bindgen,

                // setTimeout, setInterval, etc
                new Timeout(this.java_plugin),

                // console.log, console.warn, console.error
                new Console(),

                bootstrap,
                new Commands(this.descriptor.command_prefix),
                new Scheduler(this.java_plugin),
                new ServerEvents(this.java_plugin, bindgen)

        );

        // Run the source code of the plugin
        this.runtime.executeSafely(
                "'use strict';\n" + this.source_code,
                this.descriptor.name,
                1
        );

        // Check if the bootstrap global yielded a handle
        this.js_handle = bootstrap.getHandle();

    }

    /**
     * Enables the plugin to start running
     */
    public void enable() {

        // If there is no handle, bail out here
        if (this.js_handle == null) return;

        // Safely execute the function
        SafeExecutor.executeSafely(() -> {

            // Execute the enable function on the handle
            this.js_handle.executeVoidFunction("enable", null);

        }, this.runtime.getLogger());

    }

    /**
     * Stops running the plugin, if it is currently running
     */
    public void disable() {

        // If there is no handle, bail out here
        if (this.js_handle == null) return;

        // Safely execute the function
        SafeExecutor.executeSafely(() -> {

            // Execute the enable function on the handle
            this.js_handle.executeVoidFunction("disable", null);

        }, this.runtime.getLogger());

    }

    /**
     * Destroys the plugin and releases its memory
     */
    public void destroy() {

        // Release the plugin handle
        if (this.js_handle != null) {
            this.js_handle.release();
            this.js_handle = null;
        }

        // Release the runtime
        if (this.runtime != null) {
            this.runtime.release();
            this.runtime = null;
        }

    }

    /**
     * Attempts to execute a command in this plugin. Returns true if the command was
     * handled, regardless of the result of the command.
     * @param player the player sending the command
     * @param message the full string of the command
     * @return true if the message was handled
     */
    public boolean attemptCommand(Player player, String message) {

        // If there is no runtime handle
        if (this.js_handle == null) return false;

        // Create the arguments
        V8Array args = new V8Array(this.js_handle.getRuntime());
        args.push(player.getUniqueId().toString());
        args.push(message);

        // Safely execute the function
        boolean handled = SafeExecutor.executeSafely(() -> {

            // Attempt the command in the runtime
            return this.js_handle.executeBooleanFunction("attempt_command", args);

        }, this.runtime.getLogger());

        // Release the arguments
        if (!args.isReleased()) args.release();

        // Return the handled status
        return handled;

    }

}
