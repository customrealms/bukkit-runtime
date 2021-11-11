package io.customrealms.runtime.globals;

import com.eclipsesource.v8.*;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

enum ScheduledTaskType {
    TIMEOUT,
    INTERVAL
}

class ScheduledTaskHandle {
    public ScheduledTaskType type;
    public int handle;
    public V8Function func = null;
    public int milliseconds = 0;
    public V8Array args = null;
}

/**
 * Timeout provides an override of the V8 default "setTimeout" and other related global functions.
 * The reason we need to provide this is that the V8 event loop doesn't run automatically, and scheduled
 * events are effectively ignored. Because of this, this class allows the "setTimeout" and other functions
 * to tap into the JavaPlugin (Spigot) scheduler.
 */
public class Timeout implements Global {

    /**
     * The Bukkit plugin instance
     */
    private JavaPlugin plugin;

    /**
     * Output destination for error logs
     */
    private Logger logger;

    /**
     * Task handles currently outstanding
     */
    private final ArrayList<ScheduledTaskHandle> handles = new ArrayList<>();

    /**
     * Converts a number of milliseconds to a number of ticks
     * @param milliseconds the number of milliseconds for the timeout
     * @return the number of ticks
     */
    private static long msToTicks(long milliseconds) {
        return (long)((float)milliseconds / 1000f * 20);
    }

    /**
     * Constructs the timeout global, which adds support for "setTimeout" and other related functions.
     * @param plugin the plugin whose scheduler to use for the timing
     */
    public Timeout(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static ScheduledTaskHandle createTask(ScheduledTaskType type, V8Object receiver, V8Array args) {

        // If there are no arguments, do nothing
        if (args.length() == 0) return null;

        // Create the handle object
        ScheduledTaskHandle handle = new ScheduledTaskHandle();
        handle.type = type;

        // Get the function
        if (args.getType(0) != V8Value.V8_FUNCTION) return null;
        handle.func = (V8Function) args.getObject(0);

        // The other arguments
        if (args.length() > 1 && args.getType(1) == V8Value.INTEGER) handle.milliseconds = args.getInteger(1);

        // The array of arguments
        if (args.length() > 2) {
            handle.args = new V8Array(receiver.getRuntime());
            for (int i = 2; i < args.length(); i++) {

                // Get the value at the index
                Object value = args.get(i);

                // Add the value based on its type
                if (value == null) handle.args.pushNull();
                else if (value instanceof String) handle.args.push((String)value);
                else if (value instanceof V8Value) handle.args.push((V8Value)value);
                else if (value instanceof Boolean) handle.args.push((Boolean)value);
                else if (value instanceof Double) handle.args.push((Double)value);
                else if (value instanceof Integer) handle.args.push((Integer)value);

                // Release the value
                if (value instanceof V8Value) ((V8Value)value).release();

            }
        }

        // Return the handle
        return handle;

    }

    /**
     * Releases a handle object for a scheduled task
     * @param handle the handle to release
     */
    private void releaseHandle(ScheduledTaskHandle handle) {

        // Cancel the task
        Bukkit.getScheduler().cancelTask(handle.handle);

        // Release the values
        if (handle.func != null) handle.func.release();
        if (handle.args != null) handle.args.release();

        // Remove it from the array
        this.handles.remove(handle);

    }

    public Integer jsSetTimeout(V8Object receiver, V8Array args) {

        // Create a handle
        ScheduledTaskHandle handle = Timeout.createTask(ScheduledTaskType.TIMEOUT, receiver, args);
        if (handle == null) return -1;

        // Return the handle
        handle.handle = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {

            // Run the code safely
            SafeExecutor.executeSafely(() -> {

                // If the handle or its function are null
                if (handle.func == null) return;

                // Execute the task function
                handle.func.call(null, handle.args);

            }, this.logger);

            // Release the handle
            this.releaseHandle(handle);

        }, Timeout.msToTicks(handle.milliseconds));

        // Add the handle to the list
        this.handles.add(handle);

        // Return the handle number value
        return handle.handle;

    }

    public Integer jsSetInterval(V8Object receiver, V8Array args) {

        // Create a handle
        ScheduledTaskHandle handle = Timeout.createTask(ScheduledTaskType.INTERVAL, receiver, args);
        if (handle == null) return -1;

        // Calculate the number of ticks
        long ticks = Timeout.msToTicks(handle.milliseconds);

        // Return the handle
        handle.handle = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> SafeExecutor.executeSafely(() -> {

            // If the handle or its function are null
            if (handle.func == null) return;

            // Execute the task function
            handle.func.call(null, handle.args);

        }, this.logger), ticks, ticks);

        // Add the handle to the list
        this.handles.add(handle);

        // Return the handle number value
        return handle.handle;

    }

    private ScheduledTaskHandle getTaskHandleById(int handle_id) {
        for (ScheduledTaskHandle h : this.handles) {
            if (h.handle == handle_id) return h;
        }
        return null;
    }

    public void jsClearTimeout(V8Object receiver, V8Array args) {

        // Get the handle value
        if (args.getType(0) != V8Value.INTEGER) return;
        int handle_id = args.getInteger(0);

        // Find the handle with the identifier
        ScheduledTaskHandle handle = this.getTaskHandleById(handle_id);
        if (handle == null) return;
        if (handle.type != ScheduledTaskType.TIMEOUT) return;

        // Clear the handle
        this.releaseHandle(handle);

    }

    public void jsClearInterval(V8Object receiver, V8Array args) {

        // Get the handle value
        if (args.getType(0) != V8Value.INTEGER) return;
        int handle_id = args.getInteger(0);

        // Find the handle with the identifier
        ScheduledTaskHandle handle = this.getTaskHandleById(handle_id);
        if (handle == null) return;
        if (handle.type != ScheduledTaskType.INTERVAL) return;

        // Clear the handle
        this.releaseHandle(handle);

    }

    /**
     * Called when the module is initialized
     * @param runtime the V8 runtime to load into
     * @param logger the logger to use for runtime stdout and stderr
     */
    public void init(V8 runtime, Logger logger) {

        // Save the logger
        this.logger = logger;

        // Register all the globals
        runtime.registerJavaMethod(this::jsSetTimeout, "setTimeout");
        runtime.registerJavaMethod(this::jsSetInterval, "setInterval");
        runtime.registerJavaMethod(this::jsClearTimeout, "clearTimeout");
        runtime.registerJavaMethod(this::jsClearInterval, "clearInterval");

    }

    /**
     * Called when the module is released
     */
    public void release() {

        // Loop through all of the issued handles
        while (this.handles.size() > 0) {

            // Get the first handle in the list
            ScheduledTaskHandle handle = this.handles.get(0);

            // Release the handle
            this.releaseHandle(handle);

        }

        // Set all the members to null
        this.logger = null;
        this.plugin = null;

    }

}
