package io.customrealms.runtime;

import javax.script.Bindings;

public interface Global {

    /**
     * Initializes the global with the runtime it will exist in
     * @param bindings the global bindings object to append to
     */
    void init(Bindings bindings);

    /**
     * Releases the global from its runtime
     */
    void release();

}
