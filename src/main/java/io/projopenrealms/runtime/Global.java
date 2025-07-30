package io.projopenrealms.runtime;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public interface Global {

    /**
     * Initializes the global with the runtime it will exist in
     * @param context the GraalJS context
     */
    void init(Context context);

    /**
     * Releases the global from its runtime
     */
    void release();

}
