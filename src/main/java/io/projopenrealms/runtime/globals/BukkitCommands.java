package io.projopenrealms.runtime.globals;

import io.projopenrealms.runtime.Global;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class BukkitCommands implements Global {
    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin plugin;

    public BukkitCommands(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init(Context context) {
        Value bindings = context.getBindings("js");
        bindings.putMember("__commands_register", (ProxyExecutable) args -> {
            if (args.length < 2) return false;

            String name = args[0].asString();
            Value handler = args[1];
            return jsRegisterCommandHandler(name, handler);
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

        command.setExecutor((sender, cmd, label, args) -> {
            if (!handler.canExecute())
                Bukkit.getLogger().log(Level.SEVERE, "NO EXECUTE??");
            Value result = handler.execute(sender, label, args);
            if (!result.isBoolean()) return true;
            return result.asBoolean();
        });
        return true;
    }
}
