package io.customrealms.runtime.globals;

import java.util.ArrayList;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Scheduler implements Global {

    /**
     * The root Bukkit plugin instance
     */
    private final JavaPlugin plugin;

    public Scheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * List of scheduled task handles
     */
    private ArrayList<Integer> task_handles = new ArrayList<>();

    public void init(V8 runtime, Logger logger) {
        // TODO: Do this within a Scheduler global in JavaScript: Scheduler.after(...), Scheduler.repeat(...)
        runtime.registerJavaMethod(this::jsScheduleTask, "schedule_task");
        runtime.registerJavaMethod(this::jsScheduleRepeatingTask, "schedule_repeating_task");
    }

    /**
     * Releases all of the values tying the runtime to the plugin
     */
    public void release() {

        // Clear the scheduled task handles
        this.task_handles.forEach(Bukkit.getScheduler()::cancelTask);
        this.task_handles.clear();

    }

    public int jsScheduleTask(V8Object receiver, V8Array args) {

        // Get the function to call
        final V8Function fn = (V8Function)args.getObject(0);

        // Get the number of ticks
        int delay_ticks = args.getInteger(1);

        // Schedule the task
        int handle = Bukkit.getScheduler().scheduleSyncDelayedTask(
            this.plugin,
            () -> {

                // If the function is released
                if (fn.isReleased()) return;

                // Call the function
                fn.call(null, null);

                // Cleanup
                fn.release();

            },
            delay_ticks
        );

        // Add the handle to the list
        this.task_handles.add(handle);

        // Return the handle
        return handle;

    }

    public int jsScheduleRepeatingTask(V8Object receiver, V8Array args) {

        // Get the function to call
        final V8Function fn = (V8Function)args.getObject(0);

        // Get the number of ticks
        int delay_ticks = args.getInteger(1);
        int interval_ticks = args.getInteger(2);

        // Schedule the task
        int handle = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this.plugin,
            () -> {

                // If the function is released
                if (fn.isReleased()) return;

                // Call the function
                fn.call(null, null);

            },
            delay_ticks,
            interval_ticks
        );

        // Add the handle to the list
        this.task_handles.add(handle);

        // Return the handle
        return handle;

    }

}
