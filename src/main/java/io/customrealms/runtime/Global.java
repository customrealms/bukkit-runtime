package io.customrealms.runtime;

import com.eclipsesource.v8.V8;

public interface Global {

    /**
     * Initializes the global with the runtime it will exist in
     * @param v8 the handle to the runtime
     * @param logger the logger to use for runtime stdout and stderr
     */
    void init(V8 v8, Logger logger);

    /**
     * Releases the global from its runtime
     */
    void release();

}
