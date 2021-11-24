package io.customrealms.runtime.globals;

import com.eclipsesource.v8.*;
import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import org.bukkit.Bukkit;

/**
 * Server is a Global which can be installed on a Runtime, which provides a global "server" object
 * with functions to interact easily with the Minecraft server.
 */
public class Server implements Global {

    /**
     * The JavaScript logger instance
     */
    private Logger logger;

    @Override
    public void init(V8 runtime, Logger logger) {

        // Save the logger instance
        this.logger = logger;

        // Create the "server" global object
        V8Object serverObj = new V8Object(runtime);

        // Register properties to the server
        serverObj.add("port", Bukkit.getServer().getPort());

        // Register global functions to the server object
        serverObj.registerJavaMethod(this::jsGetMotd, "getMotd");
        serverObj.registerJavaMethod(this::jsShutdown, "shutdown");

        // Add the "server" object to the runtime and release it
        runtime.add("server", serverObj);
        serverObj.release();

    }

    @Override
    public void release() {}

    private Object jsGetMotd(V8Object receiver, V8Array args) {
        return Bukkit.getMotd();
    }

    private void jsShutdown(V8Object receiver, V8Array args) {
        Bukkit.shutdown();
    }

}
