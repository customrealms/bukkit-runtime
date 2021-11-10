package io.customrealms.runtime.globals;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;

public class Bootstrap implements Global {

    /**
     * The handle to the JavaScript plugin instance
     */
    private V8Object js_handle;

    public void init(V8 runtime, Logger logger) {
        runtime.registerJavaMethod(this::jsBootstrap, "_cr_bootstrap");
    }

    public void release() {}

    public V8Object getHandle() {
        return this.js_handle;
    }

    public void jsBootstrap(V8Object receiver, V8Array args) {

        // If the arguments are zero length, return early
        if (args == null || args.length() < 1) return;

        // If the function has already been called
        if (this.js_handle != null) return;

        // Get the constructor function to call
        this.js_handle = args.getObject(0);

    }

}
