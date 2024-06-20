package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;
import org.bukkit.entity.Player;
import org.openjdk.nashorn.api.scripting.JSObject;
import javax.script.Bindings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class BukkitCommands implements Global {
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
    private HashMap<Integer, BiFunction<Player, String, Boolean>> handlers = new HashMap<>();

    public BukkitCommands(Logger logger) {
        this.logger = logger;
    }

    public void init(Bindings bindings) {
        bindings.put("__commands_register", (Function<JSObject, Integer>)this::jsRegisterCommandHandler);
        bindings.put("__commands_unregister", (Consumer<Integer>)this::jsUnregisterCommandHandler);
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {
        // Clear the map of handlers
        this.handlers.clear();
    }

    public Integer jsRegisterCommandHandler(JSObject handler) {
        // Create the listener handle instance
        int handle = this.nextListenerHandle;
        this.nextListenerHandle++;

        // Store the handler in the map
        this.handlers.put(handle, (Player player, String message) -> {
            return (Boolean)handler.call(null, player, message);
        });

        // Return the handle integer
        return handle;
    }

    public void jsUnregisterCommandHandler(int handle) {
        this.handlers.remove(handle);
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
        List<BiFunction<Player, String, Boolean>> handlers = new ArrayList<>(this.handlers.values());
        if (handlers.isEmpty()) return false;

        // Loop through the registered handlers
        for (BiFunction<Player, String, Boolean> handler : handlers) {

            // If the handler is null or released somehow, skip it
            if (handler == null) continue;

            // Attempt to handle it with this handler
            Boolean handled = SafeExecutor.executeSafely(() -> {
                // Execute the handler and do a "truthy" check on the result
                return handler.apply(player, message);
            }, this.logger);

            // If it was handled, return early here
            if (handled != null && handled) {
                return true;
            }
        }

        // It wasn't handled
        return false;

    }

}
