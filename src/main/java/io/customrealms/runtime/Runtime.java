package io.customrealms.runtime;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Value;

import java.util.ArrayList;

public class Runtime {

    /**
     * The V8 runtime instance
     */
    private V8 v8;

    /**
     * The logger to use for the runtime console and errors
     */
    private final Logger logger;

    /**
     * Globals inserted to the runtime
     */
    private final ArrayList<Global> globals = new ArrayList<>();

    /**
     * Constructs a new Runtime instance using the default logger
     */
    public Runtime() {
        this(null);
    }

    /**
     * Constructs a new Runtime instance with a custom logger
     * @param logger the logger to use for the runtime console and errors
     */
    public Runtime(Logger logger) {

        // Save the logger. If the logger provided was null, construct
        // a default logger instance instead
        this.logger = logger == null ? new DefaultLogger() : logger;

        // Create the new V8 runtime instance
        this.v8 = V8.createV8Runtime("global");

        // Add the "use strict" to the beginning
        this.v8.executeVoidScript("'use strict';");

    }

    /**
     * Gets the logger used by this runtime
     * @return the logger instance
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Adds one or more globals to the runtime and initializes them
     * @param globals the globals to add
     */
    public void addGlobal(Global... globals) {

        // Loop through all the provided globals
        for (Global global : globals) {

            // Initialize the global on the runtime
            global.init(this.v8, this.logger);

            // Add the global to the list of all globals
            this.globals.add(global);

        }

    }

    /**
     * Releases the runtime object itself
     */
    protected void releaseRuntime() {

        // Release it and report leaks
        if (this.v8 != null) {
            this.v8.release(true);
            this.v8 = null;
        }

    }

    /**
     * Releases the runtime instance and all objects below it
     */
    public void release() {

        // Release globals
        this.globals.forEach(Global::release);

        // Release the runtime
        if (this.v8 != null && !this.v8.isReleased()) {

            // Get the reference count
            long referenceCount = this.v8.getObjectReferenceCount();

            // If there is more than just the runtime
            if (referenceCount > 1) System.out.println("Ref count: " + referenceCount);

            try {

                // Release the runtime itself
                this.releaseRuntime();

            } catch (IllegalStateException ignored) {}

        }

        // Set the runtime to null
        this.v8 = null;

    }

    /**
     * Executes a string of JavaScript code in the executor and does not return any result
     * @param script the JavaScript contents to evaluate
     */
    public void execute(String script) {
        this.execute(script, "<cmd>", 1);
    }

    /**
     * Executes a string of JavaScript code in the executor and does not return any result
     * @param script the JavaScript contents to evaluate
     * @param scriptName the name of the "file" the code comes from
     * @param lineNumber the line number of the evaluated code
     */
    public void execute(String script, String scriptName, int lineNumber) {

        // Evaluate the script and get a result
        Object result = this.evaluate(script, scriptName, lineNumber);

        // If the result is a V8Value, free it
        if (result instanceof V8Value) {

            // Cast it for simplicity
            V8Value val = (V8Value) result;

            // If it needs to be released, release it
            if (!val.isReleased()) val.release();

        }

    }

    public void executeSafely(String script, String scriptName, int lineNumber) {
        SafeExecutor.executeSafely(() -> {
            this.execute(script, scriptName, lineNumber);
        }, this.logger);
    }

    /**
     * Evaluates a string of JavaScript code in the executor and returns the result. This result
     * needs to be freed in most cases
     * @param script the JavaScript contents to evaluate
     * @return the resultant value from the evaluation
     */
    public Object evaluate(String script) {
        return this.evaluate(script, "<cmd>", 1);
    }

    /**
     * Evaluates a string of JavaScript code in the executor and returns the result. This result needs to be freed
     * in most cases.
     * @param script the JavaScript contents to evaluate
     * @param scriptName the name of the "file" the code comes from
     * @param lineNumber the line number of the evaluated code
     * @return the resultant value from the evaluation
     */
    public Object evaluate(String script, String scriptName, int lineNumber) {

        // Return the result
        return this.v8.executeScript(script, scriptName, lineNumber);

    }

    /**
     * Evaluates a string of JavaScript code and returns the result. If an error occurs, the error will
     * not throw an exception, and will instead just log it to the logger
     * @param script the JavaScript contents to evaluate
     * @param scriptName the name of the "file" the code comes from
     * @param lineNumber the line number of the evaluated code
     * @return the result value from evaluation
     */
    public Object evaluateSafely(String script, String scriptName, int lineNumber) {
        return SafeExecutor.executeSafely(() -> this.evaluate(script, scriptName, lineNumber), this.logger);
    }

}
