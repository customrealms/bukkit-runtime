package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.openjdk.nashorn.api.scripting.JSObject;
import javax.script.Bindings;
import java.util.function.BiFunction;

public class BukkitCommands implements Global {
    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin plugin;

    public BukkitCommands(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(Bindings bindings) {
        bindings.put("__commands_register", (BiFunction<String, JSObject, Boolean>)this::jsRegisterCommandHandler);
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    public boolean jsRegisterCommandHandler(String name, JSObject handler) {
        // Get the command with the provided name. It must be in the plugin.yml file.
        PluginCommand command = this.plugin.getCommand(name);
        if (command == null) {
            return false;
        }

        // Add an executor to the command
        command.setExecutor((sender, cmd, label, args) -> (Boolean)handler.call(null, sender, label, args));
        return true;
    }
}
