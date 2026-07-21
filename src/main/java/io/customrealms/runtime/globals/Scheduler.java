package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;

public class Scheduler implements Global {
    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin plugin;

    /**
     * The logger for the runtime
     */
    private final Logger logger;

    private static final long TICKS_PER_SECOND = 20;
    private static final long MS_PER_TICK = 1000 / TICKS_PER_SECOND;

    public Scheduler(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void init(Value bindings) {
        bindings.putMember("setTimeout", new JSFunction(args ->
                this.jsSetTimeout(args[0], args[1].asInt())));
        bindings.putMember("clearTimeout", new JSFunction(args -> {
            this.jsClearTimeout(args[0].asInt());
            return null;
        }));
        bindings.putMember("setInterval", new JSFunction(args ->
                this.jsSetInterval(args[0], args[1].asInt())));
        bindings.putMember("clearInterval", new JSFunction(args -> {
            this.jsClearInterval(args[0].asInt());
            return null;
        }));
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    public Integer jsSetTimeout(Value handler, Integer milliseconds) {
        long ticks = milliseconds / MS_PER_TICK;
        return Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            SafeExecutor.executeSafely(() -> handler.executeVoid(), this.logger);
        }, ticks);
    }

    public void jsClearTimeout(int handle) {
        Bukkit.getScheduler().cancelTask(handle);
    }

    public Integer jsSetInterval(Value handler, Integer milliseconds) {
        long ticks = milliseconds / MS_PER_TICK;
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            SafeExecutor.executeSafely(() -> handler.executeVoid(), this.logger);
        }, ticks, ticks);
    }

    public void jsClearInterval(int handle) {
        Bukkit.getScheduler().cancelTask(handle);
    }

}
