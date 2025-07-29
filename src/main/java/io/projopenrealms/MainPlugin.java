package io.projopenrealms;

import io.projopenrealms.resource.Resource;
import io.projopenrealms.runtime.DefaultLogger;
import io.projopenrealms.runtime.Logger;
import io.projopenrealms.runtime.Runtime;
import io.projopenrealms.runtime.globals.BukkitCommands;
import io.projopenrealms.runtime.globals.BukkitEvents;
import io.projopenrealms.runtime.globals.Console;
import io.projopenrealms.runtime.globals.Math;
import io.projopenrealms.runtime.globals.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * MainPlugin is the main JavaPlugin instance which serves as the entrypoint that wraps around
 * a CustomRealms JavaScript plugin
 */
public class MainPlugin extends JavaPlugin {
    /**
     * The JavaScript runtime for this plugin.
     */
    private Runtime runtime;

    @Override
    public void onEnable() {
        // Create a logger instance that will be used within the JavaScript runtime
        Logger logger = new DefaultLogger(this.getLogger());

        // Create the runtime
        this.runtime = new Runtime(logger,
                // Add globals to the runtime
                new BukkitCommands(this),
                new BukkitEvents(this, logger),
                new Scheduler(this, logger),
                new Console(logger),
                new Math()
        );

        // Load the code bundled into the JAR file
        String source_code = new Resource("plugin.js").getStringContents();
        if (source_code == null) {
            this.getLogger().log(Level.SEVERE, "JavaScript source code has not been loaded!");
            return;
        }

        // Execute the source code
        this.runtime.executeSafely(source_code);
    }

    @Override
    public void onDisable() {
        // Release the runtime
        if (this.runtime != null) {
            this.runtime.release();
            this.runtime = null;
        }
    }
}
