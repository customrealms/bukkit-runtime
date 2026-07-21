package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import org.graalvm.polyglot.Value;

/**
 * Math implements the JavaScript `Math` global.
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math">MDN Docs</a>
 */
public class Math implements Global {
    public void init(Value bindings) {
        // GraalVM JavaScript already provides the standard Math global.
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

}
