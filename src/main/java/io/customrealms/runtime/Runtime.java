package io.customrealms.runtime;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess;
import java.util.ArrayList;
import java.util.Arrays;

public class Runtime {
    /**
     * The GraalVM JavaScript context
     */
    private final Context context;

    /**
     * Globals inserted to the runtime
     */
    private final ArrayList<Global> globals = new ArrayList<>();

    /**
     * Constructs a new Runtime instance with the given globals
     * @param globals the globals to insert into the runtime
     */
    public Runtime(Global... globals) {
        // Configure the host access for the runtime
        HostAccess hostAccess = HostAccess.newBuilder(HostAccess.ALL)
            .targetTypeMapping(
                Double.class,
                Float.class,
                d -> d >= -Float.MAX_VALUE && d <= Float.MAX_VALUE,
                Double::floatValue
            )
            .build();

        // Create the GraalVM JavaScript runtime
        this.context = Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowHostAccess(hostAccess)
                .allowHostClassLookup(className -> true)
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        // Add all the globals
        this.globals.addAll(Arrays.asList(globals));

        // Create the bindings for the context
        Value bindings = this.context.getBindings("js");

        // Initialize all the globals
        for (Global global : this.globals) {
            global.init(bindings);
        }

        // Provide a Nashorn-style Java.resolve helper for existing scripts.
        this.context.eval("js", "if (typeof Java !== 'undefined' && typeof Java.resolve === 'undefined') { Java.resolve = Java.type; }");
    }

    /**
     * Releases the runtime instance and all objects below it
     */
    public void release() {
        // Release globals
        this.globals.forEach(Global::release);
        this.globals.clear();
        this.context.close();
    }

    /**
     * Executes a string of JavaScript code in the executor and does not return any result
     * @param script the JavaScript contents to evaluate
     */
    public void execute(String script) {
        this.context.eval("js", script);
    }
}
