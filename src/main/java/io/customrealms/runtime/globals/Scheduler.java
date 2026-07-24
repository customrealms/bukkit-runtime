package io.customrealms.runtime.globals;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;

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

        bindings.putMember(
            "__main_thread",
            new JSFunction(args -> {
                if (args.length == 0 || !args[0].canExecute()) {
                    throw new IllegalArgumentException(
                        "__main_thread requires a JavaScript function"
                    );
                }
                return new MainThreadFunction(args[0]);
            })
        );
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

    /**
     * Executes a JavaScript callback on Bukkit's primary thread.
     *
     * If already on the primary thread, it executes immediately.
     *
     * If invoked from another thread, it schedules the callback and blocks only
     * the calling background thread until the callback finishes. This preserves
     * Function/BiFunction return values and CompletableFuture ordering.
     */
    private Object executeOnMainThread(Value callback, Object... arguments) {
        if (Bukkit.isPrimaryThread()) {
            return executeCallback(callback, arguments);
        }

        CompletableFuture<Object> result = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
                result.complete(executeCallback(callback, arguments));
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });

        return result.join();
    }

    /**
     * Invokes the JavaScript function and converts its result into a normal
     * Java value where possible.
     */
    private Object executeCallback(Value callback, Object... arguments) {
        Value result = callback.execute(arguments);

        if (result == null || result.isNull()) {
            return null;
        }

        if (result.isHostObject()) {
            return result.asHostObject();
        }

        if (result.isBoolean()) {
            return result.asBoolean();
        }

        if (result.isString()) {
            return result.asString();
        }

        if (result.fitsInInt()) {
            return result.asInt();
        }

        if (result.fitsInLong()) {
            return result.asLong();
        }

        if (result.fitsInDouble()) {
            return result.asDouble();
        }

        // Graal may convert ordinary JS arrays/objects into List/Map-like
        // values depending on the configured host access.
        return result.as(Object.class);
    }

    private final class MainThreadFunction implements Function<Object, Object> {

        private final Value callback;

        private MainThreadFunction(Value callback) {
            this.callback = callback;
        }

        @Override
        public Object apply(Object value) {
            return executeOnMainThread(this.callback, value);
        }
    }
}