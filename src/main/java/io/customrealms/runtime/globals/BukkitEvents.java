package io.customrealms.runtime.globals;

import java.util.HashMap;
import java.util.function.Consumer;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.RuntimeExecutor;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

class RegisteredHandlerData {
    public Listener listener;
    public Consumer<Event> func;
}

public class BukkitEvents implements Global {
    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin plugin;

    /**
     * The runtime executor for this plugin.
     */
    private final RuntimeExecutor executor;

    /**
     * The logger for the runtime
     */
    private final Logger logger;

    /**
     * The next handle to issue for an event listener
     */
    private int nextListenerHandle = 0;

    /**
     * Each event handler registered spawns a separate Bukkit listener. They are
     * all stored in this map, associated to the issued listener handle integer.
     */
    private final HashMap<Integer, RegisteredHandlerData> handlers = new HashMap<>();

    public BukkitEvents(JavaPlugin plugin, RuntimeExecutor executor, Logger logger) {
        this.plugin = plugin;
        this.executor = executor;
        this.logger = logger;
    }

    public void init(Value bindings) {
        bindings.putMember("__events_register", (ProxyExecutable) args -> {
            return this.jsRegisterEventHandler(args[0].asString(), args[1]);
        });
        bindings.putMember("__events_unregister", (ProxyExecutable) args -> {
            this.jsUnregisterEventHandler(args[0].asInt());
            return null;
        });
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {
        // Clear the listeners
        this.handlers.values().forEach(registered_handle -> {
            HandlerList.unregisterAll(registered_handle.listener);
        });

        // Clear the map of handlers
        this.handlers.clear();
    }

    @SuppressWarnings("unchecked")
    public Integer jsRegisterEventHandler(String eventClassName, Value handler) {
        // Create the registered handle
        final RegisteredHandlerData registered_handle = new RegisteredHandlerData();
        registered_handle.listener = new Listener() {};
        registered_handle.func = (Event event) -> {
            handler.executeVoid(event);
        };

        // Resolve the class for the event type classpath
        Class<Event> eventClass;
        try {
            eventClass = (Class<Event>) Class.forName(eventClassName);
        } catch (ClassNotFoundException ex) {
            this.logger.log(Logger.LogType.ERROR, "Unrecognized event class: " + eventClassName);
            return null;
        }

        // Create the listener handle instance
        int handle = this.nextListenerHandle;
        this.nextListenerHandle++;

        // Store the handler in the map
        this.handlers.put(handle, registered_handle);

        // Register the event handler
        Bukkit.getPluginManager().registerEvent(
            eventClass,
            registered_handle.listener,
            EventPriority.NORMAL,
            (Listener l, Event event) -> this.executor.executeSafely(() -> registered_handle.func.accept(event)),
            this.plugin
        );

        // Return the handle
        return handle;
    }

    public void jsUnregisterEventHandler(int handle) {
        // Get the handle index
        if (!this.handlers.containsKey(handle)) return;

        // Get the registered handle
        RegisteredHandlerData registered_handle = this.handlers.get(handle);

        // Remove the handle from the map
        this.handlers.remove(handle);

        // Unregister the Bukkit listener
        HandlerList.unregisterAll(registered_handle.listener);
    }

}
