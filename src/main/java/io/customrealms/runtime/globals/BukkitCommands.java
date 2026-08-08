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
        bindings.putMember("__commands_register", (ProxyExecutable) args -> {
            return this.jsRegisterCommandHandler(args[0].asString(), args[1]);
        });
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    public boolean jsRegisterCommandHandler(String name, Value handler) {
        // Get the command with the provided name. It must be in the plugin.yml file.
        PluginCommand command = this.plugin.getCommand(name);
        if (command == null) {
            return false;
        }

        // Add an executor to the command
        command.setExecutor((sender, cmd, label, args) -> handler.execute(sender, label, args).asBoolean());
        return true;
    }
}
