package io.customrealms.runtime.globals;

import java.util.HashMap;

import com.eclipsesource.v8.*;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.SafeExecutor;
import io.customrealms.runtime.bindgen.Bindgen;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

class RegisteredHandlerData {
    public Listener listener;
    public V8Function func;
}

public class ServerEvents implements Global {

    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin java_plugin;

    /**
     * The bindgen instance for the plugin's JavaScript runtime. It's used in order
     * to convert server event types between the Java and JavaScript runtimes.
     */
    private final Bindgen bindgen;

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
    private HashMap<Integer, RegisteredHandlerData> handlers = new HashMap<>();

    public ServerEvents(
            JavaPlugin java_plugin,
            Bindgen bindgen
    ) {
        this.java_plugin = java_plugin;
        this.bindgen = bindgen;
    }

    public void init(V8 runtime, Logger logger) {

        // Save the runtime and logger for later use
        this.runtime = runtime;
        this.logger = logger;

        // Create the global "ServerEvents" variable
        V8Object serverEventsObj = new V8Object(runtime);

        // Register global methods to allow the JavaScript code to manage event handlers
        serverEventsObj.registerJavaMethod(this::jsRegisterEventHandler, "register");
        serverEventsObj.registerJavaMethod(this::jsUnregisterEventHandler, "unregister");

        // Add the object to the runtime as a global
        runtime.add("ServerEvents", serverEventsObj);
        serverEventsObj.release();

    }

    /**
     * Releases all of the values tying the runtime to the plugin
     */
    public void release() {

        // Clear the listeners
        this.handlers.values().forEach(registered_handle -> {

            // Unregister the listener
            HandlerList.unregisterAll(registered_handle.listener);

            // Release the V8 function
            registered_handle.func.release();

        });

        // Clear the map of handlers
        this.handlers.clear();

    }

    @SuppressWarnings("unchecked")
    public Integer jsRegisterEventHandler(V8Object receiver, V8Array args) {

        // The function should take a string (class name of the event)
        String eventClassName = args.getString(0);

        // Create the registered handle
        final RegisteredHandlerData registered_handle = new RegisteredHandlerData();
        registered_handle.listener = new Listener() {};
        registered_handle.func = (V8Function) args.get(1);

        try {

            // Get the event class
            Class eventClass = Class.forName(eventClassName);

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

                    // If there is no runtime
                    if (this.bindgen == null) return;

                    // Get the value for the event
                    Object jsEvent = this.bindgen.valueToJavaScript(event, eventClass);

                    // Create the array of arguments
                    V8Array argsIn = new V8Array(this.runtime);
                    if (jsEvent == null) argsIn.pushNull();
                    else if (jsEvent instanceof Integer) argsIn.push((Integer)jsEvent);
                    else if (jsEvent instanceof Boolean) argsIn.push((Boolean)jsEvent);
                    else if (jsEvent instanceof Double) argsIn.push((Double)jsEvent);
                    else if (jsEvent instanceof String) argsIn.push((String)jsEvent);
                    else if (jsEvent instanceof V8Value) {
                        argsIn.push((V8Value)jsEvent);
                        ((V8Value)jsEvent).release();
                    }

                    try {

                        // Run the code safely
                        SafeExecutor.executeSafely(() -> {

                            // If the handle or its function are null
                            if (registered_handle.func == null) return;

                            // Call the event
                            registered_handle.func.call(null, argsIn);

                        }, this.logger);

                    } catch (V8RuntimeException ex) {

                        // Log the error
                        ex.printStackTrace();

                    }

                    // Release the arguments
                    argsIn.release();

                },
                this.java_plugin
            );

            // Return the handle
            return handle;

        } catch (ClassNotFoundException ex) {

            // Print the exception
            ex.printStackTrace();

        }

        // Return something to make J2V8 happy
        return null;

    }

    public Object jsUnregisterEventHandler(V8Object receiver, V8Array args) {

        // Get the handle index
        int handle = args.getInteger(0);
        if (!this.handlers.containsKey(handle)) return null;

        // Get the registered handle
        RegisteredHandlerData registered_handle = this.handlers.get(handle);

        // Remove the handle from the map
        this.handlers.remove(handle);

        // Unregister the Bukkit listener
        HandlerList.unregisterAll(registered_handle.listener);

        // Release the function
        registered_handle.func.release();

        // Return null
        return null;

    }

}
