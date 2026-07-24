package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;

public class Plugin implements Global {
    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin plugin;

    public Plugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(Value bindings) {
        bindings.putMember("Plugin", this.plugin);
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}
}
