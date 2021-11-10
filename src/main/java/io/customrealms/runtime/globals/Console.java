package io.customrealms.runtime.globals;

import com.eclipsesource.v8.*;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import io.customrealms.runtime.bindgen.Bindgen;
import org.bukkit.ChatColor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Console is a Global which can be installed on a Runtime, which provides a global "console" object
 * with three functions: console.log, console.warn, console.error
 *
 * The behavior of these globals mirrors that of the browser-based and NodeJS APIs for "console"
 */
public class Console implements Global {

    /**
     * The JavaScript plugin instance
     */
    private Logger logger;

    @Override
    public void init(V8 runtime, Logger logger) {

        // Save the logger instance
        this.logger = logger;

        // Create the "console" global object
        V8Object consoleObj = new V8Object(runtime);

        // Register all the functions (console.log, console.warn, console.error)
        consoleObj.registerJavaMethod((receiver, args) -> {
            this.performLog(Logger.LogType.LOG, args);
        }, "log");
        consoleObj.registerJavaMethod((receiver, args) -> {
            this.performLog(Logger.LogType.WARNING, args);
        }, "warn");
        consoleObj.registerJavaMethod((receiver, args) -> {
            this.performLog(Logger.LogType.ERROR, args);
        }, "error");

        // Add the "console" object to the runtime and release it
        runtime.add("console", consoleObj);
        consoleObj.release();

    }

    public void release() {}

    /**
     * Performs a generic log function, to any of the log types (log, warning, error)
     * @param type the type of log being sent
     * @param args the arguments passed into the logger
     */
    private void performLog(Logger.LogType type, V8Array args) {

        // Create an output stream
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // And create a print stream to write to the output stream
        PrintStream out;
        try {
            out = new PrintStream(os, true, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return;
        }

        // Log all of the arguments
        for (int i = 0; i < args.length(); i++) {

            // If this is not the first, put a tab here
            if (i > 0) out.print("\t");

            // Get the value from the array
            Object value = args.get(i);

            // Print the string representation of the value
            out.print(Console.toString(value, 0));

        }

        // End it with a newline
        // out.println();

        try {

            // Get the string representation of the output stream
            String output = os.toString("UTF-8");

            // Send the output to the logger
            this.logger.log(type, output.replace("" + ChatColor.COLOR_CHAR, "&"));

        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        // Close the print stream
        out.close();

    }

    /**
     * Gets the string representation for a value
     */
    private static String toString(Object value, int depth) {
        if (value == null) return "null";
        try {
            if (value instanceof Boolean) return ((Boolean)value).booleanValue() ? "true" : "false";
            if (value instanceof String) return (String)value;
            if (value instanceof Integer) return Integer.toString((Integer)value);
            if (value instanceof Short) return Short.toString((Short)value);
            if (value instanceof Long) return Long.toString((Long)value);
            if (value instanceof Float) return Float.toString((Float)value);
            if (value instanceof Double) return Double.toString((Double)value);
            if (value instanceof V8Array) return Console.arrayToString((V8Array)value, depth);
            if (value instanceof V8Object) return Console.objectToString((V8Object)value, depth);
            return "unknown";
        } finally {

            // If the value is a V8Value, release it
            if (value instanceof V8Value) ((V8Value)value).release();

        }
    }

    /**
     * Converts an array of V8 values to string
     * @param array the array to convert
     * @param depth the depth of the value
     * @return the string representation of the array
     */
    private static String arrayToString(V8Array array, int depth) {

        // If the value is null, bail out here
        if (array == null) return "null";

        // If we're past level 5, summarize it
        if (depth >= 5) return "[Array of " + array.length() + "]";

        // Decide on a separator for the array, based on its length
        final String separator = array.length() >= 10 ? ",\n" : ", ";

        // Create the output string
        StringBuilder output = new StringBuilder();

        // Loop through the values in the array
        for (int i = 0; i < array.length(); i++) {

            // Get the value from the array
            Object value = array.get(i);

            // If this is not the first, add the separator
            if (i > 0) output.append(separator);

            // Add the string representation to the output
            output.append(Console.toString(value, depth + 1));

        }

        // Return the wrapped output value
        return "[ " + output.toString() + " ]";

    }

    /**
     * Converts an object of V8 values to string
     * @param object the object to convert
     * @param depth the depth of the value
     * @return the string representation of the object
     */
    private static String objectToString(V8Object object, int depth) {

        // If the value is null, bail out here
        if (object == null) return "null";

        // If the value is a function
        if (object instanceof V8Function) {

            try {

                // Get the name of the function
                String fn_name = object.getString("name");
                if (fn_name != null) return "[Function " + fn_name + "]";

            } catch (Exception ignored) {}

            // Return a default function name
            return "[Function fn]";

        }

        // Get all of the keys in the object
        ArrayList<String> keys = new ArrayList<String>(Arrays.asList(object.getKeys()));

        // Remove keys that should be hidden
        keys.remove(Bindgen.JAVA_SHADOW_KEY);
        keys.remove("_java");

        // If we're past level 5, summarize it
        if (depth >= 5) return "{Object of " + keys.size() + "}";

        // Decide on a separator for the object, based on its length
        final String separator = keys.size() >= 10 ? ",\n" : ", ";

        // Create the output string
        StringBuilder output = new StringBuilder("");

        try {

            // Get the constructor, if there is one
            V8Function ctor = (V8Function)object.getObject("constructor");
            if (ctor != null) {

                // Convert it to a function
                String ctor_name = ctor.getString("name");
                if (ctor_name != null && !ctor_name.equals("Object")) {
                    output.append(ctor_name);
                    output.append(" ");
                }

                // Release the constructor
                ctor.release();

            }

        } catch (Exception ignored) {}

        // Add the curly brace
        output.append("{ ");

        // Loop through the keys in the object
        for (int i = 0; i < keys.size(); i++) {

            // Get the key at the index
            String key = keys.get(i);

            // Get the value in the object for the key
            Object value = object.get(key);

            // If this is not the first, add the separator
            if (i > 0) output.append(separator);

            // Add the string representation to the output
            output.append(key);
            output.append(": ");
            output.append(Console.toString(value, depth + 1));

        }

        // Return the wrapped output value
        output.append(" }");
        return output.toString();

    }

}
