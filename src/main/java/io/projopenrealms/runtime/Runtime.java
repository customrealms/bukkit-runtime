package io.projopenrealms.runtime;

import org.graalvm.polyglot.*;

import javax.script.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

public class Runtime {
    /**
     * The Nashorn script engine
     */
    private final Context context;

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

        // Create the GraalJS runtime
        Engine polyglotEngine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        this.context = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowAllAccess(true)
                .engine(polyglotEngine)
                .build();

        // Add all the globals
        this.globals.addAll(Arrays.asList(globals));

        // Initialize all the globals
        for (Global global : this.globals) {
            global.init(context);
        }
    }

    /**
     * Releases the runtime instance and all objects below it
     */
    public void release() {
        // Release globals
        this.globals.forEach(Global::release);
        this.globals.clear();
        // Close the engine
        this.context.close();
    }

    /**
     * Executes a string of JavaScript code in the executor and does not return any result
     * @param script the JavaScript contents to evaluate
     */
    private void execute(String script) {
        try {
            this.context.eval("js", script);
        } catch (PolyglotException e) {
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
