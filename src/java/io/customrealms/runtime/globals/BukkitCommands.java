package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class BukkitCommands implements Global {
    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin plugin;

    public BukkitCommands(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(Value bindings) {
        bindings.putMember("__commands_register", (ProxyExecutable) this::jsRegister);
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    public Boolean jsRegister(Value... args) {
        String name = args[0].asString();
        Value handler = args[1];

        // Get the command with the provided name. It must be in the plugin.yml file.
        PluginCommand command = this.plugin.getCommand(name);
        if (command == null) {
            return false;
        }

        // Add an executor to the command
        command.setExecutor((sender, cmd, label, commandArgs) -> handler.execute(sender, label, commandArgs).asBoolean());
        return true;
    }
}
