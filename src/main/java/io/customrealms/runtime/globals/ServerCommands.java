package io.customrealms.runtime.globals;

import com.eclipsesource.v8.*;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerCommands implements Global {

    private V8 runtime;
    private Logger logger;

    /**
     * The next handle to issue for an event listener
     */
    private int nextListenerHandle = 0;

    /**
     * Each event handler registered spawns a separate Bukkit listener. They are
     * all stored in this map, associated to the issued listener handle integer.
     */
    private HashMap<Integer, V8Function> handlers = new HashMap<>();

    public void init(V8 runtime, Logger logger) {

        // Save the runtime and logger for later use
        this.runtime = runtime;
        this.logger = logger;

        // Create the global "ServerCommands" variable
        V8Object serverEventsObj = new V8Object(runtime);

        // Register global methods to allow the JavaScript code to manage event handlers
        serverEventsObj.registerJavaMethod(this::jsRegisterCommandHandler, "register");
        serverEventsObj.registerJavaMethod(this::jsUnregisterCommandHandler, "unregister");

        // Add the object to the runtime as a global
        runtime.add("BukkitCommands", serverEventsObj);
        serverEventsObj.release();

    }

    /**
     * Releases all of the values tying the runtime to the plugin
     */
    public void release() {

        // Clear the listeners
        this.handlers.values().forEach(registered_handle -> {

            // Release the handler function
            if (!registered_handle.isReleased()) {
                registered_handle.release();
            }

        });

        // Clear the map of handlers
        this.handlers.clear();

    }

    public Integer jsRegisterCommandHandler(V8Object receiver, V8Array args) {

        // Get the handler function passed in
        V8Function handler = (V8Function)args.getObject(0);

        // Create the listener handle instance
        int handle = this.nextListenerHandle;
        this.nextListenerHandle++;

        // Store the handler in the map
        this.handlers.put(handle, handler);

        // Return something to make J2V8 happy
        return null;

    }

    public Object jsUnregisterCommandHandler(V8Object receiver, V8Array args) {

        // Get the handle index
        int handle = args.getInteger(0);
        if (!this.handlers.containsKey(handle)) return null;

        // Get the registered handle
        V8Function handler = this.handlers.get(handle);

        // Remove the handle from the map
        this.handlers.remove(handle);

        // Release the function
        if (handler != null && !handler.isReleased()) handler.release();

        // Return null
        return null;

    }

    /**
     * Attempts to execute a command in this plugin. Returns true if the command was
     * handled, regardless of the result of the command.
     * @param player the player sending the command
     * @param message the full string of the command
     * @return true if the message was handled
     */
    public boolean attemptCommand(Player player, String message) {

        // Get the list of handlers
        List<V8Function> handlers = new ArrayList<>(this.handlers.values());
        if (handlers.size() == 0) return false;

        // Create the arguments array. Note that creating it once and reusing means
        // handler functions are able to mutate it.
        V8Array args = new V8Array(this.runtime);
        args.push(player.getUniqueId().toString());
        args.push(message);

        // Loop through the registered handlers
        for (V8Function handler : handlers) {

            // If the handler is null or released somehow, skip it
            if (handler == null || handler.isReleased()) continue;

            // Attempt to handle it with this handler
            Boolean handled = SafeExecutor.executeSafely(() -> {
                try {
                    // Execute the handler and do a "truthy" check on the result
                    Object value = handler.call(null, args);
                    if (value == null) return false;
                    if (value instanceof Number) return ((Number) value).intValue() > 0;
                    if (value instanceof Boolean) return (Boolean) value;
                    if (!(value instanceof V8Value)) return false;
                    return !((V8Value) value).isUndefined();
                } catch (V8ResultUndefined ex) {
                    return false;
                }
            }, this.logger);

            // If it was handled, return early here
            if (handled != null && handled) {
                if (!args.isReleased()) args.release();
                return true;
            }

        }

        // Release the arguments
        if (!args.isReleased()) args.release();

        // It wasn't handled
        return false;

    }

}
