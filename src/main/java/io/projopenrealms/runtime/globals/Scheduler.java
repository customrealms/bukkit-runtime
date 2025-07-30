package io.projopenrealms.runtime.globals;

import io.projopenrealms.runtime.Global;
import io.projopenrealms.runtime.Logger;
import io.projopenrealms.runtime.SafeExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

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

    public void init(Context context) {
        Value bindings = context.getBindings("js");

        bindings.putMember("setTimeout", (ProxyExecutable) args -> jsSetTimeout(args[0], args[1].asInt()));

        bindings.putMember("clearTimeout", (ProxyExecutable) args -> {jsClearTimeout(args[0].asInt());return null;});

        bindings.putMember("setInterval", (ProxyExecutable) args -> jsSetInterval(args[0], args[1].asInt()));

        bindings.putMember("clearInterval", (ProxyExecutable) args -> {jsClearInterval(args[0].asInt()); return null;});
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    public Integer jsSetTimeout(Value handler, Integer milliseconds) {
        long ticks = milliseconds / MS_PER_TICK;
        return Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            SafeExecutor.executeSafely(() -> handler.execute(), this.logger);
        }, ticks);
    }

    public void jsClearTimeout(int handle) {
        Bukkit.getScheduler().cancelTask(handle);
    }

    public Integer jsSetInterval(Value handler, Integer milliseconds) {
        long ticks = milliseconds / MS_PER_TICK;
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            SafeExecutor.executeSafely(() -> handler.execute(), this.logger);
        }, ticks, ticks);
    }

    public void jsClearInterval(int handle) {
        Bukkit.getScheduler().cancelTask(handle);
    }

}
