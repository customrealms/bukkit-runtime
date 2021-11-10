package io.customrealms.runtime.globals;

import java.lang.reflect.Field;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

public class Commands implements Global {

    public final String command_prefix;

    public Commands(String command_prefix) {
        this.command_prefix = command_prefix;
    }

    public void init(V8 runtime, Logger logger) {
        runtime.registerJavaMethod(this::jsRegisterCommandName, "register_command_name");
    }

    public void release() {}

    public void jsRegisterCommandName(V8Object receiver, V8Array args) {

        // Get the name of the command
        String name = args.getString(0);
        String usage = args.getString(1);
        String permission = args.getString(2);

        try {

            // Get the command map field, and make it accessible, since it's private
            final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);

            // Get the command map object
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            // Register a noop command with the name
            commandMap.register(this.command_prefix, new Command(name) {
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return false;
                }
            });

            // Register it to the plugin
            // this.plugin.registerCommandName(name, usage, permission);

        } catch (NoSuchFieldException  | IllegalArgumentException | IllegalAccessException ex) {

            // Print the error
            ex.printStackTrace();

        }

    }

}
