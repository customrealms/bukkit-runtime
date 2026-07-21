package io.customrealms.runtime.globals;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class JSFunction implements ProxyExecutable {
    public interface Runnable {
        Object call(Value... args);
    }

    private final Runnable runnable;

    public JSFunction(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public Object execute(Value... args) {
        return this.runnable.call(args);
    }
}
