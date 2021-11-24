package io.customrealms.runtime.bindgen;

import java.lang.reflect.Executable;
import java.net.Proxy;
import java.util.ArrayList;

public class MethodFinder {

    /**
     * Array of all the primitive types. These primitives map directly to a JavaScript type, so
     * we don't want to wrap them with bindings. We can just pass them directly between the
     * Java and JS runtimes.
     */
    private static final Class<?>[] PRIMITIVES = {
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
    };

    /**
     * Array of all the numeric primitive types
     */
    private static final Class<?>[] NUMERICS = {
            int.class,
            short.class,
            long.class,
            double.class,
            float.class,
            Integer.class,
            Short.class,
            Long.class,
            Double.class,
            Float.class
    };

    /**
     * Checks if a given class is a primitive type
     * @param clazz the class to check
     * @return whether or not the class is primitive
     */
    public static boolean isPrimitiveType(Class<?> clazz) {
        for (Class<?> c : MethodFinder.PRIMITIVES) {
            if (c.equals(clazz)) return true;
        }
        return false;
    }

    /**
     * Checks if a given value is of a primitive type
     * @param value the value to check
     * @return whether or not the value is a primitive
     */
    public static boolean isValuePrimitive(Object value) {
        for (Class<?> clazz : MethodFinder.PRIMITIVES) {
            if (clazz.isInstance(value)) return true;
        }
        return false;
    }

    /**
     * Determines if the provided type is numeric
     * @param clazz the class to test
     */
    public static boolean isNumericType(Class<?> clazz) {
        for (Class<?> numeric : MethodFinder.NUMERICS) {
            if (clazz.equals(numeric)) return true;
        }
        return false;
    }

    /**
     * Determines if the provided type is boolean
     * @param clazz the class to test
     */
    public static boolean isBooleanType(Class<?> clazz) {
        return clazz.equals(Boolean.class) || clazz.equals(boolean.class);
    }

    /**
     * Determines if two types are close enough to pass into a method
     * @param hole the hole type to fill
     * @param peg the peg value to pass in
     */
    public static boolean isTypeUsable(Class<?> hole, Object peg) {

        // If the value is null
        if (peg == null) return true;

        // Get the peg type
        Class<?> pegClass = peg.getClass();

        // Check the hole types
        boolean hole_numeric = MethodFinder.isNumericType(hole);
        boolean hole_boolean = MethodFinder.isBooleanType(hole);

        // Check the peg types
        boolean peg_numeric = MethodFinder.isNumericType(pegClass);
        boolean peg_boolean = MethodFinder.isBooleanType(pegClass);

        // If both types are numeric
        if ((hole_numeric || hole_boolean) && peg_numeric) return true;

        // If both values are boolean
        if (hole_boolean && peg_boolean) return true;

        // If the peg implements the hole type
        if (hole.isAssignableFrom(pegClass)) return true;

        // Return false otherwise
        return false;

    }

    /**
     * Determines if the provided array of peg types can be used to call a method
     * @param holes the method parameter types
     * @param pegs the peg values
     */
    public static boolean isSignatureCallable(Class<?>[] holes, Object[] pegs) {

        // If there are a different number of types
        if (holes.length != pegs.length) return false;

        // Loop through the holes
        for (int i = 0; i < holes.length; i++) {
            if (!MethodFinder.isTypeUsable(holes[i], pegs[i])) return false;
        }

        // Return true if we get here
        return true;

    }

    /**
     * Determines which one, if any, of the methods provided are callable by the provided types
     * @param methods the methods to test
     * @param pegs the peg values
     */
    public static <T extends Executable> T findCallableMethod(T[] methods, Object[] pegs) {

        // Loop through the methods
        for (T method : methods) {

            // Get the parameter types for the method
            Class<?>[] holes = method.getParameterTypes();

            // If it's compatible
            if (MethodFinder.isSignatureCallable(holes, pegs)) return method;

        }

        // Return null if we get here
        return null;

    }

    public static Object[] castArguments(Class<?>[] holes, Object[] pegs) {

        // Create an output array
        Object[] casted = new Object[holes.length];

        // Loop through the length
        for (int i = 0; i < pegs.length; i++) {

            // Cast the argument
            casted[i] = MethodFinder.castArgument(holes[i], pegs[i]);

        }

        // Return the casted values
        return casted;

    }

    private static Object castArgument(Class<?> hole, Object peg) {

        // If the value is null
        if (peg == null) {
            if (MethodFinder.isNumericType(hole)) return 0;
            if (MethodFinder.isBooleanType(hole)) return false;
            return null;
        }

        // Get the peg type
        Class<?> pegClass = peg.getClass();

        // If the types are both numeric
        if (MethodFinder.isNumericType(hole) &&
            MethodFinder.isNumericType(pegClass)) return MethodFinder.castNumeric(hole, peg);

        // If the hole is boolean and the value is numeric
        if (MethodFinder.isBooleanType(hole) &&
            MethodFinder.isNumericType(pegClass)) return MethodFinder.castBoolean(peg);

        // Return the value itself
        return peg;

    }

    private static boolean castBoolean(Object value) {
        return int.class.cast(value) > 0;
    }

    private static Object castNumeric(Class<?> to, Object value) {

        // Define the primitive types
        ArrayList<Class<?>> primitiveTypes = new ArrayList<>();
        primitiveTypes.add(int.class);
        primitiveTypes.add(short.class);
        primitiveTypes.add(long.class);
        primitiveTypes.add(double.class);
        primitiveTypes.add(float.class);

        // Get the class we're coming from
        Class<?> fromClass = value.getClass();

        // Determine if we're going to a primitive type
        boolean toPrimitive = primitiveTypes.contains(to);
        boolean fromPrimitive = primitiveTypes.contains(fromClass);

        // If we're going to and from primitives or non-primitives
        if (toPrimitive == fromPrimitive) return to.cast(fromClass);

        // If we're going to a primitive
        if (toPrimitive) {
            if (to.equals(int.class)) return ((Number)value).intValue();
            if (to.equals(short.class)) return ((Number)value).shortValue();
            if (to.equals(long.class)) return ((Number)value).longValue();
            if (to.equals(double.class)) return ((Number)value).doubleValue();
            if (to.equals(float.class)) return ((Number)value).floatValue();
        }

        // If we're going to a non-primitive
        if (!toPrimitive) {
            if (to.equals(Integer.class)) return Integer.valueOf((int)value);
            if (to.equals(Short.class)) return Short.valueOf((short)value);
            if (to.equals(Long.class)) return Long.valueOf((long)value);
            if (to.equals(Double.class)) return Double.valueOf((double)value);
            if (to.equals(Float.class)) return Float.valueOf((float)value);
        }

        // Return zero by default
        return 0;

    }

}
