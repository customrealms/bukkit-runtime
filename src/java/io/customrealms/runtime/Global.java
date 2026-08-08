package io.customrealms.runtime;

import org.graalvm.polyglot.Value;

public interface Global {

    /**
     * Initializes the global with the runtime it will exist in
     * @param bindings the global bindings object to append to
     */
    void init(Value bindings);

    /**
     * Releases the global from its runtime
     */
    void release();

}
