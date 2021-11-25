package io.customrealms.runtime;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Runtime {

    /**
     * The Java plugin this runtime is operating within
     */
    private final JavaPlugin plugin;

    /**
     * The NodeJS runtime
     */
    private final NodeJS nodeJS;

    /**
     * The logger to use for the runtime console and errors
     */
    private final Logger logger;

    /**
     * The task handle for the event loop
     */
    private int task_handle = -1;

    /**
     * Globals inserted to the runtime
     */
    private final ArrayList<Global> globals = new ArrayList<>();

    /**
     * Constructs a new Runtime instance with a custom logger
     * @param logger the logger to use for the runtime console and errors
     */
    public Runtime(JavaPlugin plugin, Logger logger) {

        // Save the plugin and logger
        this.plugin = plugin;
        this.logger = logger;

        // Create the NodeJS runtime
        this.nodeJS = NodeJS.createNodeJS();

        // Add the "use strict" to the beginning
        this.nodeJS.getRuntime().executeVoidScript("'use strict';");

    }

    private static final long TICKS_PER_SECOND = 20;
    private static final long MS_PER_TICK = 1000 / TICKS_PER_SECOND;

    /**
     * Allow one-half tick of time to be spent pumping the NodeJS event loop
     */
    private static final long MAX_MS_PER_PUMP = MS_PER_TICK / 2;

    /**
     * Starts running the NodeJS event loop once per tick
     */
    public void start() {

        this.task_handle = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {

            // Get the start time of this tick
            long start_time = System.currentTimeMillis();

            // Process the NodeJS event loop for up to the maximum amount of time
            while (this.nodeJS.isRunning() && System.currentTimeMillis() < start_time + Runtime.MAX_MS_PER_PUMP) {
                this.nodeJS.handleMessage();
            }

            // Start another event loop pump in 1 tick
            this.start();

        }, 1);

    }

    /**
     * Gets the logger used by this runtime
     * @return the logger instance
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Adds one or more globals to the runtime and initializes them
     * @param globals the globals to add
     */
    public void addGlobal(Global... globals) {

        // Get the V8 runtime
        V8 v8 = this.nodeJS.getRuntime();

        // Loop through all the provided globals
        for (Global global : globals) {

            // Initialize the global on the runtime
            global.init(v8, this.logger);

            // Add the global to the list of all globals
            this.globals.add(global);

        }

    }

    /**
     * Releases the runtime instance and all objects below it
     */
    public void release() {

        // Disable the task
        Bukkit.getScheduler().cancelTask(this.task_handle);
        this.task_handle = -1;

        // Release globals
        this.globals.forEach(Global::release);
        this.globals.clear();

        // Get the runtime
        V8 v8 = this.nodeJS.getRuntime();

        // Release the runtime
        if (v8 != null && !v8.isReleased()) {

            // Get the reference count
            long referenceCount = v8.getObjectReferenceCount();

            // If there is more than just the runtime
            if (referenceCount > 1) this.logger.log(Logger.LogType.WARNING, "Ref count: " + referenceCount);

        }

        try {

            // Release the runtime itself
            this.nodeJS.release();

        } catch (IllegalStateException ignored) {}

    }

    /**
     * Executes a string of JavaScript code in the executor and does not return any result
     * @param script the JavaScript contents to evaluate
     */
    public void execute(String script) {

        try {

            // Create a temporary file for the script on the disk
            File file = File.createTempFile("cr-plugin-temp-", ".js");
            file.deleteOnExit();

            // Write the script code to the file
            FileWriter fw = new FileWriter(file);
            fw.write(script);
            fw.close();

            // Execute the file in the NodeJS context. Note: the file can't be deleted until
            // the NodeJS runtime has read it into memory completely, and we can't be sure this
            // has happened until the entire server has exited. That's why we do "deleteOnExit" above
            this.nodeJS.exec(file);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Executes a script and logs any errors to the runtime logger, protecting the caller
     * from needing to handle exceptions
     * @param script the script to execute
     */
    public void executeSafely(String script) {
        SafeExecutor.executeSafely(() -> this.execute(script), this.logger);
    }

}
