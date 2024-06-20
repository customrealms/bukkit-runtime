package io.customrealms.runtime.globals;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.openjdk.nashorn.api.scripting.JSObject;
import javax.script.Bindings;

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
    private HashMap<Integer, RegisteredHandlerData> handlers = new HashMap<>();

    public BukkitEvents(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void init(Bindings bindings) {
        bindings.put("__events_register", (BiFunction<String, JSObject, Integer>)this::jsRegisterEventHandler);
        bindings.put("__events_unregister", (Consumer<Integer>)this::jsUnregisterEventHandler);
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {
        // Clear the listeners
        this.handlers.values().forEach(registered_handle -> {

            // Unregister the listener
            HandlerList.unregisterAll(registered_handle.listener);

        });

        // Clear the map of handlers
        this.handlers.clear();
    }

    @SuppressWarnings("unchecked")
    public Integer jsRegisterEventHandler(String eventClassName, JSObject handler) {

        // Create the registered handle
        final RegisteredHandlerData registered_handle = new RegisteredHandlerData();
        registered_handle.listener = new Listener() {};
        registered_handle.func = (Event event) -> {
            handler.call(null, event);
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
                (Listener l, Event event) -> {
                    SafeExecutor.executeSafely(() -> {
                        registered_handle.func.accept(event);
                    }, this.logger);
                },
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
