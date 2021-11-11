package io.customrealms.v8wrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class V8Wrap {

    private V8ClassLoader class_loader = new V8ClassLoader();

    private Class<?> v8Class;
    private Object v8;

    public V8Wrap() {
        try {

            // Get the v8 class
            this.v8Class = this.class_loader.findClass("com.eclipsesource.v8.V8");

            // Create the v8 instance
            Constructor<?> ctor = this.v8Class.getDeclaredConstructor(String.class);
            this.v8 = ctor.newInstance("global");

        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void executeVoidScript(String script) {
        try {
            Method method = this.v8Class.getDeclaredMethod("executeVoidScript", String.class);
            method.invoke(this.v8, script);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public Object executeScript(String script, String scriptName, int lineNumber) {
        try {
            Method method = this.v8Class.getDeclaredMethod("executeScript", String.class, String.class, int.class);
            return method.invoke(this.v8, script, scriptName, lineNumber);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void release(boolean reportMemoryLeaks) {
        try {
            Method method = this.v8Class.getDeclaredMethod("release", boolean.class);
            method.invoke(this.v8, reportMemoryLeaks);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        this.v8 = null;
        this.class_loader = null;
    }

    public boolean isReleased() {
        try {
            Method method = this.v8Class.getDeclaredMethod("isReleased");
            return (Boolean)method.invoke(this.v8);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getObjectReferenceCount() {
        try {
            Method method = this.v8Class.getDeclaredMethod("getObjectReferenceCount");
            return (Integer)method.invoke(this.v8);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
