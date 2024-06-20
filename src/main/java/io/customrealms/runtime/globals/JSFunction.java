package io.customrealms.runtime.globals;

import org.openjdk.nashorn.api.scripting.JSObject;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JSFunction implements JSObject {
    public interface Runnable {
        Object call(Object thiz, Object... args);
    }

    private final Runnable runnable;

    public JSFunction(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public Object call(Object thiz, Object... args) {
        return this.runnable.call(thiz, args);
    }

    @Override
    public Object newObject(Object... args) {
        return null;
    }

    @Override
    public Object eval(String s) {
        return null;
    }

    @Override
    public Object getMember(String name) {
        return null;
    }

    @Override
    public Object getSlot(int index) {
        return null;
    }

    @Override
    public boolean hasMember(String name) {
        return false;
    }

    @Override
    public boolean hasSlot(int slot) {
        return false;
    }

    @Override
    public void removeMember(String name) {

    }

    @Override
    public void setMember(String name, Object value) {

    }

    @Override
    public void setSlot(int index, Object value) {

    }

    @Override
    public Set<String> keySet() {
        return Set.of();
    }

    @Override
    public Collection<Object> values() {
        return List.of();
    }

    @Override
    public boolean isInstance(Object instance) {
        return false;
    }

    @Override
    public boolean isInstanceOf(Object clazz) {
        return false;
    }

    @Override
    public String getClassName() {
        return "Function";
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public boolean isStrictFunction() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }
}
