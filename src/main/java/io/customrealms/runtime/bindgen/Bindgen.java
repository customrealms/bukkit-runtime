package io.customrealms.runtime.bindgen;

import com.eclipsesource.v8.*;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;

/**
 * The Bindgen class is a Global that can be installed to a Runtime (io.customrealms.runtime.Runtime) which
 * exposes Java classes to the JavaScript runtime via the "Java" global namespace.
 *
 * The actual magic of wrapping Java classes in JavaScript wrappers is found in the 'ClassBindingGenerator' class.
 *
 * There can be major security implications to using this Global haphazardly in your code. However, this risk
 * is inherent, and perhaps even worse, in traditional Bukkit Java plugins. In other words, this Bindgen
 * doesn't provide any security / sandboxing improvements. On the contrary, it explicitly REMOVES the sandbox
 * behavior of the underlying JavaScript runtime.
 */
public class Bindgen implements Global {

    /**
     * The logger for the runtime
     */
    private Logger logger;

    /**
     * The class binding generator contains all the functions to actually creating
     */
    public ClassBindingGenerator class_binding_generator;

    @Override
    public void init(V8 v8, Logger logger) {

        // Save the V8 handle and logger
        this.logger = logger;

        // Create the class binding generator
        this.class_binding_generator = new ClassBindingGenerator(v8, logger);

        // Create the root Java instance that will be inserted as a global to the runtime
        V8Object java_global = new V8Object(v8);

        // Register functions within the Java global namespace
        java_global.registerJavaMethod(this::jsResolve, "resolve");

        // Add the Java global to the runtime
        v8.add("Java", java_global);

    }

    private V8Object jsResolve(V8Object receiver, V8Array args) {

        // If the argument is not a string, return
        if (args.getType(0) != V8Value.STRING) return null;

        // Get the name of the class
        String classpath = args.getString(0);

        try {

            // Resolve the class function
            V8Function jsClassFn = this.class_binding_generator.resolve(classpath);

            // Return a twin of the class function. We return a twin because values returned from
            // JS functions like this one are automatically released. If we return the bound class
            // function, we will not be able to return it a second time, but will still believe it is
            // bound, so we'll also never regenerate the bindings.
            return jsClassFn.twin();

        } catch (ClassNotFoundException ex) {

            // Log the error and return null
            this.logger.log(Logger.LogType.ERROR, ex.getMessage());
            return null;

        }

    }

    /**
     * Releases the bound classes
     */
    public void release() {

        // Clean up the class bindings
        this.class_binding_generator.release();
        this.class_binding_generator = null;

    }

}
