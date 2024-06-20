package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.openjdk.nashorn.api.scripting.JSObject;
import javax.script.Bindings;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

    public void init(Bindings bindings) {
        bindings.put("setTimeout", (BiFunction<JSObject, Integer, Integer>)this::jsSetTimeout);
        bindings.put("clearTimeout", (Consumer<Integer>)this::jsClearTimeout);
        bindings.put("setInterval", (BiFunction<JSObject, Integer, Integer>)this::jsSetInterval);
        bindings.put("clearInterval", (Consumer<Integer>)this::jsClearInterval);
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    public Integer jsSetTimeout(JSObject handler, Integer milliseconds) {
        long ticks = milliseconds / MS_PER_TICK;
        return Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            SafeExecutor.executeSafely(() -> handler.call(null), this.logger);
        }, ticks);
    }

    public void jsClearTimeout(int handle) {
        Bukkit.getScheduler().cancelTask(handle);
    }

    public Integer jsSetInterval(JSObject handler, Integer milliseconds) {
        long ticks = milliseconds / MS_PER_TICK;
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            SafeExecutor.executeSafely(() -> handler.call(null), this.logger);
        }, ticks, ticks);
    }

    public void jsClearInterval(int handle) {
        Bukkit.getScheduler().cancelTask(handle);
    }

}
