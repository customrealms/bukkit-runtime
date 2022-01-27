package io.customrealms.runtime.globals;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.bindgen.Bindgen;
import org.bukkit.plugin.java.JavaPlugin;

public class Config implements Global {

    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin java_plugin;

    /**
     * The bindgen instance for the plugin's JavaScript runtime. It's used in order
     * to convert server event types between the Java and JavaScript runtimes.
     */
    private final Bindgen bindgen;

    /**
     * The runtime this global is operating within
     */
    private V8 runtime;

    public Config(
            JavaPlugin java_plugin,
            Bindgen bindgen
    ) {
        this.java_plugin = java_plugin;
        this.bindgen = bindgen;
    }

    @Override
    public void init(V8 runtime, Logger logger) {

        // Save the runtime for later use
        this.runtime = runtime;

        // Create the global "Config" variable
        V8Object pluginObj = new V8Object(runtime);

        // Register global methods to allow the JavaScript code to manage event handlers
        pluginObj.registerJavaMethod(this::jsSaveConfig, "save");

        // Add the object to the runtime as a global
        runtime.add("Config", pluginObj);
        pluginObj.release();

    }

    @Override
    public void release() {

    }

    public V8Object jsSaveConfig(V8Object receiver, V8Array args) {

        // getting
        String file = args.getString(0);

        // Creating custom config
        FileManager config = new FileManager(this.java_plugin, file + ".yml");

        // Returns config for Javascript
        V8Object jsFileConfiguration = (V8Object) this.bindgen.class_binding_generator.wrap(config);

        return jsFileConfiguration;

    }

}
