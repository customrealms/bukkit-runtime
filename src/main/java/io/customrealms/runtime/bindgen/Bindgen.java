package io.customrealms.runtime.bindgen;

import com.eclipsesource.v8.*;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;

import java.lang.reflect.*;
import java.net.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Bindgen class is a Global that can be installed to a Runtime (io.customrealms.runtime.Runtime) which
 * exposes Java classes to the JavaScript runtime via the "Java" global namespace.
 *
 * There can be major security implications to using this Global haphazardly in your code. However, this risk
 * is inherent, and perhaps even worse, in traditional Bukkit Java plugins. In other words, this Bindgen
 * doesn't provide any security / sandboxing improvements. On the contrary, it explicitly REMOVES the sandbox
 * behavior of the underlying JavaScript runtime.
 */
public class Bindgen implements Global {

    /**
     * The key to use in bound objects
     */
    public static final String JAVA_SHADOW_KEY = "[[Java]]";

    /**
     * The handle instance object
     */
    private Object currentHandleInstance = null;

    /**
     * The classes bound to the JavaScript runtime
     */
    private final HashMap<String, V8Function> boundClasses = new HashMap<>();

    /**
     * The V8 runtime handle
     */
    private V8 v8;

    /**
     * The logger for the runtime
     */
    private Logger logger;

    /**
     * The root bindings object, accessible in the runtime as Java
     */
    private V8Object bindings_root;

    @Override
    public void init(V8 v8, Logger logger) {

        // Save the V8 handle and logger
        this.logger = logger;
        this.v8 = v8;

        // Generate the root container object for the bindings
        this.bindings_root = new V8Object(v8);
        v8.add("Java", bindings_root);

        // Add a function to generate bindings on-the-fly
        this.bindings_root.registerJavaMethod(this::jsResolve, "resolve");

    }

    private V8Object jsResolve(V8Object receiver, V8Array args) {

        // If the argument is not a string, return
        if (args.getType(0) != V8Value.STRING) return null;

        // Get the name of the class
        String classPath = args.getString(0);

        // Get the binding value
        V8Function jsClassFn = this.boundClasses.get(classPath);

        // If there are no class bindings, or it has been released
        if (jsClassFn == null || jsClassFn.isReleased()) {

            try {

                // Get the class with the name
                Class<?> clazz = Class.forName(classPath);

                // Generate the bindings
                jsClassFn = this.generateClassBindings(clazz, true);

            } catch (ClassNotFoundException ex) {

                // Print the error
                ex.printStackTrace();

            }

        }

        // If there is still no class function
        if (jsClassFn == null || jsClassFn.isReleased()) {
            return null;
        }

        // Return the class
        return jsClassFn.twin();

    }

    /**
     * Generates JavaScript bindings for the provided classes
     * @param classes the class paths to all classes to add
     */
    public void generateBindings(Class<?>[] classes, final boolean trace) {

        // If the array is null, return here
        if (classes == null) return;

        // Loop through the class paths
        for (Class<?> clazz : classes) this.generateClassBindings(clazz, trace);

    }

    /**
     * Generates JavaScript bindings for the provided class
     * @param c the class path to the class to add
     */
    private V8Function generateClassBindings(Class<?> c, final boolean trace) {

        // If the class is null or an array type
        if (c == null) return null;
        while (c.isArray()) c = c.getComponentType();
        // if (c.isEnum()) return;
        final Class<?> clazz = c;

        // Define primitive types
        Class<?>[] primitives = {
            String.class,
            Integer.class,
            int.class,
            Double.class,
            double.class,
            Float.class,
            float.class,
            Long.class,
            long.class,
            Short.class,
            short.class,
            Boolean.class,
            boolean.class,
            Byte.class,
            byte.class,
            Character.class,
            char.class,
            Void.class,
            void.class,
            Object.class,
            Proxy.class
        };

        // If it's a primitive, skip
        for (Class<?> x : primitives) if (clazz.equals(x)) return null;

        // Get the full class path
        final String classPath = clazz.getCanonicalName();

        // If the class path is in the map already
        if (this.boundClasses.containsKey(classPath)) return this.boundClasses.get(classPath);
        this.boundClasses.put(classPath, null);

        // Generate the class constructor function
        V8Function jsClassFn = this.generateClassConstructor(clazz);

        // Register this binding
        this.boundClasses.put(classPath, jsClassFn);

        // Add all of the static fields
        Arrays
            .stream(clazz.getFields())
            .filter(f -> Modifier.isStatic(f.getModifiers()))
            .filter(f -> Modifier.isPublic(f.getModifiers()))
            .forEach(f -> {

                // System.out.println("STATIC FIELD: " + classPath + " - " + f.getName() + " type: " + f.getType());

                // Register the type of the field
                if (trace) this.generateClassBindings(f.getType(), false);

                // Add the field to the object
                this.addFieldToObject(f, jsClassFn, null);

            });

        // Add all of the static methods
        Arrays
            .stream(clazz.getMethods())
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .forEach(m -> {

                // System.out.println("STATIC METHOD: " + classPath + " - " + m.getName());

                // Register the types on the method
                if (trace) {
                    this.generateClassBindings(m.getReturnType(), false);
                    this.generateBindings(m.getParameterTypes(), false);
                }

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

        // Add the prototype to the class
        V8Object jsClassProto = this.generateClassProto(clazz, trace);
        jsClassFn.add("prototype", jsClassProto);
        jsClassProto.release();

        // Return the function
        return jsClassFn;

    }

    private V8Function generateClassConstructor(Class<?> clazz) {

        // Create the function for the class
        return new V8Function(this.v8, (V8Object receiver, V8Array args) -> {

            // Convert the arguments to Java
            Object[] javaArgs = this.getJavaArgs(args);

            // Find which is callable by the argument types
            Constructor<?> ctor = MethodFinder.findCallableMethod(
                clazz.getConstructors(),
                javaArgs
            );

            // If there is no callable constructor
            if (ctor == null) {
                this.logger.log(Logger.LogType.ERROR, "Matching constructor not found!");
                return null;
            }

            // Cast the arguments
            Object[] castedArgs = MethodFinder.castArguments(
                ctor.getParameterTypes(),
                javaArgs
            );

            try {

                // Create a new instance
                Object instance = ctor.newInstance(castedArgs);

                // Convert the instance to JavaScript
                return this.valueToJavaScript(instance, clazz);

            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {

                // Log the error
                this.logger.log(Logger.LogType.ERROR, "Constructor exists, but failed to construct instance.");

            }

            // Return null if we get here
            return null;

        });

    }

    private V8Object generateClassProto(Class<?> clazz, boolean trace) {

        // Create the class prototype
        V8Object jsClassProto = new V8Object(this.v8);

        // Add the inspect function
        jsClassProto.registerJavaMethod((receiver, args) -> "[" + clazz.getSimpleName() + "]", "inspect");

        // Add all of the instance methods
        Arrays
            .stream(clazz.getMethods())
            .filter(m -> !Modifier.isStatic(m.getModifiers()))
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .forEach(m -> {

                // Register the types on the method
                if (trace) {
                    this.generateClassBindings(m.getReturnType(), false);
                    this.generateBindings(m.getParameterTypes(), false);
                }

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
            javaArgs[i] = this.valueToJava(args.get(i));
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
            return this.valueToJavaScript(returnValue, m.getReturnType());

        } catch (IllegalAccessException | InvocationTargetException ex) {

            // Print the stack trace and return
            ex.printStackTrace();
            return null;

        }

    }

    private V8Object constructBoundJavaInstance(final Object javaInstance, Class<?> clazz) {

        // If the Java instance is null
        if (javaInstance == null) return null;

        // If the class provided is null, use the instance's class. We do this to allow
        // a class like Player (interface) to be used even though the object is a subclass like CraftPlayer.
        if (clazz == null) clazz = javaInstance.getClass();

        // Get the class path
        String classPath = clazz.getCanonicalName();

        // If there is no class
        if (!this.boundClasses.containsKey(classPath)) {

            // Register the class
            this.generateClassBindings(clazz, false);

        }

        // Get the class function for the object type
        V8Object jsClassFn = this.boundClasses.get(classPath);
        if (jsClassFn == null) return null;
        if (jsClassFn.isReleased()) {
            this.logger.log(Logger.LogType.ERROR, "Error constructing bound Java instance. Class function has already been released: " + classPath);
            // return null;
        }

        // Create the JavaScript instance of the object
        V8Object jsInstance = new V8Object(this.v8);

        // Attach a function to the object
        jsInstance.registerJavaMethod((receiver, args) -> {
            this.currentHandleInstance = javaInstance;
            return null;
        }, Bindgen.JAVA_SHADOW_KEY);

        if (!jsClassFn.isReleased()) {

            // Assign the prototype to the instance
            V8Object jsProto = jsClassFn.getObject("prototype");
            jsInstance.setPrototype(jsProto);
            jsProto.release();

        }

        // Return the JavaScript instance
        return jsInstance;

    }

    public Object valueToJavaScript(Object value) { return this.valueToJavaScript(value, null); }

    public Object valueToJavaScript(Object value, Class<?> clazz) {
        if (value == null ||
            (value instanceof Integer) ||
            (value instanceof Double) ||
            (value instanceof Boolean) ||
            (value instanceof String)) return value;
        else if (clazz != null && clazz.isArray()) {
            V8Array output = new V8Array(this.v8);
            Object[] valueAsArray = (Object[])value;
            for (Object val : valueAsArray) {

                // Convert the value
                Object jsVal = this.valueToJavaScript(val);

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
            return output;
        } else if (value instanceof List) {
            V8Array output = new V8Array(this.v8);
            List<?> valueAsList = (List<?>)value;
            for (Object val : valueAsList) {

                // Convert the value
                Object jsVal = this.valueToJavaScript(val);

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
            return output;
        } else if (value instanceof Map) {
            V8Object output = new V8Object(this.v8);
            ((Map<?, ?>)value).keySet().forEach(k -> {
                if (!(k instanceof String)) return;
                Object v = ((Map<?, ?>)value).get(k);
                Object jsVal = this.valueToJavaScript(v);
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
        } else {
            // System.out.println("valuetoJS");
            return this.constructBoundJavaInstance(value, clazz);
        }
    }

    private Object valueToJava(Object value) {
        if (value == null ||
            (value instanceof Integer) ||
            (value instanceof Double) ||
            (value instanceof Boolean) ||
            (value instanceof String)) return value;
        if (value instanceof V8Array) {
            Object[] output = new Object[((V8Array)value).length()];
            for (int i = 0; i < ((V8Array) value).length(); i++) {
                output[i] = this.valueToJava(((V8Array) value).get(i));
            }
            return output;
        }
        if (value instanceof V8Object) {
            Object underlying = this.getJavaShadowInstance((V8Object) value);
            if (underlying != null) return underlying;
            return V8ObjectUtils
                .toMap((V8Object)value)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> this.valueToJava(entry.getValue())
                ));
        }
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
        Object jsObj = this.valueToJavaScript(obj);

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
        this.currentHandleInstance = null;
        if (obj.contains(Bindgen.JAVA_SHADOW_KEY)) {
            obj.executeVoidFunction(Bindgen.JAVA_SHADOW_KEY, null);
        }
        return this.currentHandleInstance;
    }

    /**
     * Releases the bound classes
     */
    public void release() {

        // Release all of the map values
        this.boundClasses.values().forEach(V8Object::release);

        // Empty the map
        this.boundClasses.clear();

        // Release the bindings root object
        this.bindings_root.release();

    }

}
