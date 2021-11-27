package io.customrealms.runtime.bindgen;

import com.eclipsesource.v8.*;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import io.customrealms.runtime.Logger;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassBindingGenerator {

    /**
     * The key to use in bound objects
     */
    public static final String JAVA_SHADOW_KEY = "[[Java]]";

    /**
     * The V8 runtime handle
     */
    private final V8 v8;

    /**
     * The logger for the runtime
     */
    private final Logger logger;

    /**
     * Cached class bindings for the runtime
     */
    private final HashMap<String, V8Function> cached_class_bindings = new HashMap<>();

    /**
     * The handle instance object
     */
    private Object active_receiver = null;

    /**
     * Constructs a class binding generator that operates on a given runtime and logs results
     * @param v8 the V8 runtime the binding generator operates within
     * @param logger the logger to which errors will be logged
     */
    public ClassBindingGenerator(V8 v8, Logger logger) {
        this.v8 = v8;
        this.logger = logger;
    }

    /**
     * Releases the class bindings
     */
    public void release() {

        // Release all of the map values
        this.cached_class_bindings.values().forEach(V8Function::release);

        // Empty the map
        this.cached_class_bindings.clear();

    }

    /**
     * Resolves a Java classpath string into a V8Function that acts as the class constructor
     * within the JavaScript runtime
     * @param classpath the classpath string for the Java class to bind
     * @return the class constructor function in the JavaScript runtime
     * @throws ClassNotFoundException thrown if the given classpath doesn't map to an actual Java class
     */
    public V8Function resolve(String classpath) throws ClassNotFoundException {

        // Check for the class function in the cache, and return it if it exists
        if (this.cached_class_bindings.containsKey(classpath)) {
            return this.cached_class_bindings.get(classpath);
        }

        // Get the class with the provided classpath
        Class<?> clazz = Class.forName(classpath);

        // Generate the bindings for the class
        return this.generate_class(clazz);

    }

    /**
     * Generates JavaScript bindings for the provided class
     * @param c the class path to the class to add
     */
    private V8Function generate_class(Class<?> c) {

        //================================================================================
        // RESOLVE TYPE – Here we do some filtering and narrowing down of the actual
        // type that needs to be resolved.
        //================================================================================

        // If the class is null, there's nothing we can do here
        if (c == null) return null;

        // If the class is an array type, we need to unwrap it to get the actual type
        // stored within the array
        while (c.isArray()) c = c.getComponentType();

        // Primitive types don't get wrapped in any special bindings since the JS runtime
        // already inherently understands them.
        if (MethodFinder.isPrimitiveType(c)) return null;

        // For now, we're just treating enums as classes, and it seems to work fine
        // if (c.isEnum()) return;

        // Keep a final reference to the class, so we can reference it
        final Class<?> clazz = c;


        //================================================================================
        // CACHE CHECK – Check the cache to make sure we don't generate bindings that
        // have already been generated.
        //================================================================================

        // Get the full class path
        String classpath = clazz.getCanonicalName();

        // If the class path is in the map already
        if (this.cached_class_bindings.containsKey(classpath)) return this.cached_class_bindings.get(classpath);
        this.cached_class_bindings.put(classpath, null);


        //================================================================================
        // CONSTRUCTOR – In JavaScript, constructors are functions. When called with the
        // keyword 'new' in front of the function call, the expression returns a new instance
        // of the class type. Here' we create that constructor function and cache it.
        //================================================================================

        // Generate the class constructor function
        V8Function jsClassFn = this.generate_class_constructor(clazz);

        // Register this binding
        this.cached_class_bindings.put(classpath, jsClassFn);


        //================================================================================
        // STATIC – Here we add all the static fields and methods as properties of the
        // constructor function in the JavaScript runtime.
        //================================================================================

        // Add all of the static fields
        Arrays
                .stream(clazz.getFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> Modifier.isPublic(f.getModifiers()))
                .forEach(f -> this.addFieldToObject(f, jsClassFn, null));

        // Add all of the static methods
        Arrays
                .stream(clazz.getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .forEach(m -> {

                    // Register the static function
                    jsClassFn.registerJavaMethod(
                            (V8Object receiver, V8Array args) -> this.invokeJavaMethod(
                                    clazz,
                                    m.getName(),
                                    null,
                                    args
                            ),
                            m.getName()
                    );

                });


        //================================================================================
        // PROTOTYPE – Here we generate the class prototype, and apply it to the class
        // function so that all instances of the class will inherit the member functions.
        //================================================================================

        // Generate the class prototype (the object containing all the member methods for all instances
        // of the class within the JavaScript runtime). This is very frequently used (ie. any time a method
        // is called on any Bukkit value in the core library).
        V8Object jsClassProto = this.generate_class_prototype(clazz);
        jsClassFn.add("prototype", jsClassProto);
        jsClassProto.release();

        // Return the function
        return jsClassFn;

    }

    /**
     * Creates a JavaScript class constructor function that creates instances of the given class
     * type within the JavaScript runtime.
     *
     * The job of a JavaScript class constructor function is to assume a `this` receiver internally
     * and apply all of the instance values (fields / properties) to `this`.
     *
     * @param clazz the Java class being represented in the JavaScript runtime by the constructor function
     * @return the JavaScript class constructor function
     */
    private V8Function generate_class_constructor(final Class<?> clazz) {

        // Create and return a function that will act as the constructor for the bound class
        // when the constructor is called within the JavaScript runtime
        V8Function jsClassFn = new V8Function(this.v8, (V8Object receiver, V8Array args) -> {

            // Convert the arguments to Java
            Object[] javaArgs = this.getJavaArgs(args);

            // Find an underlying constructor that can be called with the arguments passed in
            Constructor<?> ctor = MethodFinder.findCallableMethod(
                    clazz.getConstructors(),
                    javaArgs
            );

            // If there is no callable constructor
            if (ctor == null) {
                this.logger.log(Logger.LogType.ERROR, "Matching constructor not found!");
                return null;
            }

            // Cast the arguments so that they can be passed into the constructor. This mostly means
            // converting numeric arguments to the exact correct numeric type of the parameter.
            Object[] castedArgs = MethodFinder.castArguments(
                    ctor.getParameterTypes(),
                    javaArgs
            );

            try {

                // Create a new instance with the casted arguments
                Object instance = ctor.newInstance(castedArgs);

                // Convert the instance to JavaScript
                return this.wrap(instance);

            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {

                // Log the error
                this.logger.log(Logger.LogType.ERROR, "Constructor exists, but failed to construct instance.");
                return null;

            }

        });

        // Set the name for the class constructor
        jsClassFn.add("name", clazz.getName());

        // Return the class constructor
        return jsClassFn;

    }

    /**
     * Creates a JavaScript class prototype, which is an object containing all of the non-static instance methods
     * belonging to the corresponding class. The returned prototype contains methods mapping to each of the
     * instance methods on the provided Java class.
     * @param clazz the class for which to create a prototype
     * @return the JavaScript class prototype for the provided Java class
     */
    private V8Object generate_class_prototype(Class<?> clazz) {

        // Create the class prototype
        V8Object jsClassProto = new V8Object(this.v8);

        // Add the inspect method, which returns a string representation of the Java class
        // underlying a JavaScript wrapped instance.
        jsClassProto.registerJavaMethod((receiver, args) -> "[" + clazz.getSimpleName() + "]", "inspect");

        // Add all of the instance methods
        Arrays
                .stream(clazz.getMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .forEach(m -> {

                    // Add the method to the prototype
                    jsClassProto.registerJavaMethod(
                            (V8Object receiver, V8Array args) -> this.invokeJavaMethod(
                                    clazz,
                                    m.getName(),
                                    this.getJavaShadowInstance(receiver),
                                    args
                            ),
                            m.getName()
                    );

                });

        // Return the prototype
        return jsClassProto;

    }

    /**
     * Finds a method within a class which is callable with the provided arguments
     * @param clazz the class to look in
     * @param methodName the name of the method
     * @param staticMethod whether or not to look for a static method
     * @param javaArgs the arguments we intend to pass into the method
     * @return the matching method, or null
     */
    private Method findJavaMethod(Class<?> clazz, String methodName, boolean staticMethod, Object[] javaArgs) {

        // Get the methods with the name
        Method[] methods = Arrays
                .stream(clazz.getMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Modifier.isStatic(m.getModifiers()) == staticMethod)
                .filter(m -> m.getName().equals(methodName))
                .toArray(Method[]::new);

        // Find which is callable by the argument types
        return MethodFinder.findCallableMethod(methods, javaArgs);

    }

    /**
     * Converts an array of V8 arguments to Java values
     * @param args the V8 arguments
     * @return the Java values
     */
    private Object[] getJavaArgs(V8Array args) {
        Object[] javaArgs = new Object[args.length()];
        for (int i = 0; i < args.length(); i++) {
            javaArgs[i] = this.unwrap(args.get(i));
        }
        return javaArgs;
    }

    private Object invokeJavaMethod(Class<?> clazz, String methodName, Object javaInstance, V8Array args) {

        // Convert the arguments to Java
        Object[] javaArgs = this.getJavaArgs(args);

        // Find the matching method to call
        Method m = this.findJavaMethod(clazz, methodName, javaInstance == null, javaArgs);

        // If there is no matching method
        if (m == null) {
            this.logger.log(Logger.LogType.ERROR, "NO METHOD FOUND:");
            this.logger.log(Logger.LogType.ERROR, "\t" + clazz.getCanonicalName());
            this.logger.log(Logger.LogType.ERROR, "\t" + methodName);
            for (Object arg : javaArgs) {
                if (arg == null) this.logger.log(Logger.LogType.ERROR, "\t - null");
                else this.logger.log(Logger.LogType.ERROR, "\t - " + arg.getClass().getCanonicalName());
            }
            this.logger.log(Logger.LogType.ERROR, "");
            return null;
        }

        // Convert the arguments
        Object[] castedArgs = MethodFinder.castArguments(
                m.getParameterTypes(),
                javaArgs
        );

        try {

            // Invoke the method
            Object returnValue = m.invoke(javaInstance, castedArgs);

            // If the return type is void, return nothing
            if (m.getReturnType().equals(Void.TYPE)) return null;

            // Otherwise, convert the returned value
            return this.wrap(returnValue);

        } catch (IllegalAccessException | InvocationTargetException ex) {

            // Print the stack trace and return
            ex.printStackTrace();
            return null;

        }

    }

    /**
     * Creates a wrapper instance for a given Java value. The wrapper instance is a JavaScript object existing
     * within the JavaScript runtime (using the TypeScript type Java.Value) that maps to a Java instance.
     * @param javaInstance the Java instance to wrap in a JavaScript object
     * @param clazz the class to use for the bindings
     * @return the JavaScript wrapper object
     */
    private V8Object create_wrapper(final Object javaInstance, Class<?> clazz) {

        // If the Java instance is null
        if (javaInstance == null) return null;

        // If the class provided is null, use the instance's class. We do this to allow
        // a class like Player (interface) to be used even though the object is a subclass like CraftPlayer.
        if (clazz == null) clazz = javaInstance.getClass();

        // Get the class path
        String classpath = clazz.getCanonicalName();

        // If the class is not yet cached, generate the binding for it
        if (!this.cached_class_bindings.containsKey(classpath)) {
            this.generate_class(clazz);
        }

        // Get the class function for the object type
        V8Object jsClassFn = this.cached_class_bindings.get(classpath);
        if (jsClassFn == null) return null;
        if (jsClassFn.isReleased()) {
            this.logger.log(Logger.LogType.ERROR, "Error constructing bound Java instance. Class function has already been released: " + classpath);
             return null;
        }

        // Create the JavaScript instance of the object
        V8Object jsInstance = new V8Object(this.v8);

        // Add a function to the object which, when called, sets the 'active_receiver' member of this
        // ClassBindingGenerator instance to the underlying Java instance represented by the JS instance.
        jsInstance.registerJavaMethod((receiver, args) -> {
            this.active_receiver = javaInstance;
            return null;
        }, ClassBindingGenerator.JAVA_SHADOW_KEY);

        // Copy the prototype from the class onto this instance
        if (!jsClassFn.isReleased()) {
            V8Object jsProto = jsClassFn.getObject("prototype");
            jsInstance.setPrototype(jsProto);
            jsProto.release();
        }

        // Return the JavaScript instance
        return jsInstance;

    }

    /**
     * Utility function that pushes a value into a V8Array. We do this in a couple places
     * and it's kinda verbose, plus we need to be sure to release in the correct spot, so
     * it's better off in its own function here than being duplicated.
     * @param output the output array to push the value to
     * @param val the value to wrap and push into the array
     */
    private void push_jsvalue(V8Array output, Object val) {

        // Convert the value
        Object jsVal = this.wrap(val);

        // Assign the value to the key
        if (jsVal == null) output.pushNull();
        else if (jsVal instanceof Integer) output.push((Integer)jsVal);
        else if (jsVal instanceof Boolean) output.push((Boolean)jsVal);
        else if (jsVal instanceof Double) output.push((Double)jsVal);
        else if (jsVal instanceof String) output.push((String)jsVal);
        else if (jsVal instanceof V8Value) {
            output.push((V8Value)jsVal);
            ((V8Value)jsVal).release();
        }

    }

    /**
     * Wraps a Java value into a JavaScript instance. This effectively just passes off to
     * the `create_wrapper`, but provides support for container types (arrays, lists, maps).
     * @param value the value to wrap into a JavaScript instance
     * @return the wrapped JavaScript value for the Java value
     */
    public Object wrap(Object value) {

        // If the value is null or a primitive, return it as-is
        if (value == null || MethodFinder.isValuePrimitive(value)) return value;

        // If the class is null, get the class of the value
        Class<?> clazz = value.getClass();

        // If the value is an array, we need to fill a V8 array with the values
        if (clazz != null && clazz.isArray()) {
            V8Array output = new V8Array(this.v8);
            for (Object val : (Object[])value) this.push_jsvalue(output, val);
            return output;
        }

        // If the value is a list, do the same thing as with arrays
        if (value instanceof List) {
            V8Array output = new V8Array(this.v8);
            for (Object val : (List<?>)value) this.push_jsvalue(output, val);
            return output;
        }

        // If the value is a map, we need to create a V8 hash map with the values
        if (value instanceof Map) {
            V8Object output = new V8Object(this.v8);
            ((Map<?, ?>)value).keySet().forEach(k -> {
                if (!(k instanceof String)) return;
                Object v = ((Map<?, ?>)value).get(k);
                Object jsVal = this.wrap(v);
                if (jsVal == null) output.addNull((String)k);
                else if (jsVal instanceof Integer) output.add((String)k, (Integer)jsVal);
                else if (jsVal instanceof Boolean) output.add((String)k, (Boolean)jsVal);
                else if (jsVal instanceof Double) output.add((String)k, (Double)jsVal);
                else if (jsVal instanceof String) output.add((String)k, (String)jsVal);
                else if (jsVal instanceof V8Value) {
                    output.add((String)k, (V8Value)jsVal);
                    ((V8Value)jsVal).release();
                }
            });
            return output;
        }

        // If we get here, the value is a Java object that is not a container type (array, map)
        // so we need to create a wrapper instance for it
        return this.create_wrapper(value, clazz);

    }

    /**
     * Unwraps a JavaScript value into the underlying Java value
     * @param value the wrapped or primitive JavaScript value
     * @return the Java value wrapped inside the JavaScript one
     */
    public Object unwrap(Object value) {

        // If the value is null or a primitive, return it as-is
        if (value == null || MethodFinder.isValuePrimitive(value)) return value;

        // If it's an array, we need to construct a new array and unwrap all the contained values
        if (value instanceof V8Array) {
            Object[] output = new Object[((V8Array)value).length()];
            for (int i = 0; i < ((V8Array) value).length(); i++) {
                output[i] = this.unwrap(((V8Array) value).get(i));
            }
            return output;
        }

        // If it's an object, it could be either a hash map or some actual Java object
        if (value instanceof V8Object) {

            // Check for the underlying Java instance, if it exists
            Object underlying = this.getJavaShadowInstance((V8Object) value);
            if (underlying != null) return underlying;

            // There was no Java instance, so it's a hash map
            return V8ObjectUtils
                    .toMap((V8Object)value)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> this.unwrap(entry.getValue())
                    ));
        }

        // If we get here, we don't know what to do with the value, so return null
        return null;

    }

    private void addFieldToObject(Field f, V8Object jsInstance, Object receiver) {

        // The value of the field
        Object obj;

        try {

            // Get the value for the field
            obj = f.get(receiver);

        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return;
        }

        // Translate the value to JavaScript
        Object jsObj = this.wrap(obj);

        // Assign the value to the key
        if (jsObj == null) jsInstance.addNull(f.getName());
        else if (jsObj instanceof Integer) jsInstance.add(f.getName(), (Integer)jsObj);
        else if (jsObj instanceof Boolean) jsInstance.add(f.getName(), (Boolean)jsObj);
        else if (jsObj instanceof Double) jsInstance.add(f.getName(), (Double)jsObj);
        else if (jsObj instanceof String) jsInstance.add(f.getName(), (String)jsObj);
        else if (jsObj instanceof V8Value) {
            jsInstance.add(f.getName(), (V8Value)jsObj);
            ((V8Value)jsObj).release();
        }

    }

    /**
     * Gets the shadow Java object underlying a JavaScript object
     * @param obj the JavaScript object
     * @return the underlying Java object
     */
    private Object getJavaShadowInstance(V8Object obj) {
        this.active_receiver = null;
        if (obj.contains(ClassBindingGenerator.JAVA_SHADOW_KEY)) {
            obj.executeVoidFunction(ClassBindingGenerator.JAVA_SHADOW_KEY, null);
        }
        return this.active_receiver;
    }

}
