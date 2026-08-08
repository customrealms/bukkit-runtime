package io.customrealms;

import io.customrealms.resource.Resource;
import io.customrealms.runtime.DefaultLogger;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.Runtime;
import io.customrealms.runtime.RuntimeExecutor;
import io.customrealms.runtime.globals.BukkitCommands;
import io.customrealms.runtime.globals.BukkitEvents;
import io.customrealms.runtime.globals.Console;
import io.customrealms.runtime.globals.Files;
import io.customrealms.runtime.globals.Plugin;
import io.customrealms.runtime.globals.Scheduler;
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

    /**
     * The runtime executor for this plugin.
     */
    private RuntimeExecutor executor;

    @Override
    public void onEnable() {
        // Create a logger instance that will be used within the JavaScript runtime
        Logger logger = new DefaultLogger(this.getLogger());

        // Create the runtime executor
        this.executor = new RuntimeExecutor(this, logger);

        // Create the runtime
        this.runtime = new Runtime(
            // Add globals to the runtime
            new BukkitCommands(this),
            new BukkitEvents(this, this.executor, logger),
            new Scheduler(this, this.executor),
            new Console(logger),
            new Plugin(this),
            new Files(this.executor)
        );

        // Load the code bundled into the JAR file
        String sourceCode = new Resource("plugin.js").getStringContents();
        if (sourceCode == null) {
            this.getLogger().log(Level.SEVERE, "JavaScript source code has not been loaded!");
            return;
        }

        // Execute the source code
        this.executor.executeSafely(() -> this.runtime.execute(sourceCode));
    }

    @Override
    public void onDisable() {
        // Release the runtime executor
        if (this.executor != null) {
            this.executor.release();
            this.executor = null;
        }

        // Release the runtime
        if (this.runtime != null) {
            this.runtime.release();
            this.runtime = null;
        }
    }
}
