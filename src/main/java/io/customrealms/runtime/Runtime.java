package io.customrealms.runtime;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;

public class Runtime {
    /**
     * The Nashorn script engine
     */
    private final NashornScriptEngine engine;

    /**
     * The logger to use for the runtime console and errors
     */
    private final Logger logger;

    /**
     * Globals inserted to the runtime
     */
    private final ArrayList<Global> globals = new ArrayList<>();

    /**
     * Constructs a new Runtime instance with a custom logger
     * @param logger the logger to use for the runtime console and errors
     */
    public Runtime(Logger logger, Global... globals) {
        // Save the logger
        this.logger = logger;

        // Create the Nashorn runtime
        ScriptEngineManager manager = new ScriptEngineManager();
        manager.registerEngineName("nashorn", new NashornScriptEngineFactory());
        this.engine = (NashornScriptEngine)manager.getEngineByName("nashorn");

        // Add all the globals
        this.globals.addAll(Arrays.asList(globals));

        // Create the bindings for the engine
        Bindings bindings = this.engine.createBindings();

        // Initialize all the globals
        for (Global global : this.globals) {
            global.init(bindings);
        }

        // Set the engine scope bindings
        this.engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    }

    /**
     * Releases the runtime instance and all objects below it
     */
    public void release() {
        // Release globals
        this.globals.forEach(Global::release);
        this.globals.clear();
    }

    /**
     * Executes a string of JavaScript code in the executor and does not return any result
     * @param script the JavaScript contents to evaluate
     */
    private void execute(String script) {
        try {
            this.engine.eval(script);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a script and logs any errors to the runtime logger, protecting the caller
     * from needing to handle exceptions
     * @param script the script to execute
     */
    public void executeSafely(String script) {
        SafeExecutor.executeSafely(() -> this.execute(script), this.logger);
    }

}
