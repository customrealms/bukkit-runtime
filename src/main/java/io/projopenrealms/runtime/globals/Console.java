package io.projopenrealms.runtime.globals;

import io.projopenrealms.runtime.Global;
import io.projopenrealms.runtime.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import javax.script.Bindings;
import java.util.*;

public class Console implements Global {
    /**
     * The logger for the runtime
     */
    private final Logger logger;

    public Console(Logger logger) {
        this.logger = logger;
    }

    public void init(Context context) {
        HashMap<String, Object> console = new HashMap<>();

        console.put("log", (ProxyExecutable) this::jsConsoleLog);
        console.put("warn", (ProxyExecutable) this::jsConsoleWarn);
        console.put("error", (ProxyExecutable) this::jsConsoleError);

        Value bindings = context.getBindings("js");
        bindings.putMember("console", ProxyObject.fromMap(console));
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    private static String joinLogArgs(Object[] args) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                str.append("\t");
            }
            str.append(args[i]);
        }
        return str.toString();
    }

    private Value jsConsoleLog(Value... args) {
        this.logger.log(Logger.LogType.LOG, Console.joinLogArgs(args));
        return null;
    }

    private Value jsConsoleWarn(Value... args) {
        this.logger.log(Logger.LogType.WARNING, Console.joinLogArgs(args));
        return null;
    }

    private Value jsConsoleError(Value... args) {
        this.logger.log(Logger.LogType.ERROR, Console.joinLogArgs(args));
        return null;
    }
}
