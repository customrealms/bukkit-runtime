package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;

public class Console implements Global {
    /**
     * The logger for the runtime
     */
    private final Logger logger;

    public Console(Logger logger) {
        this.logger = logger;
    }

    public void init(Value bindings) {
        HashMap<String, Object> console = new HashMap<>();
        console.put("log", (ProxyExecutable) this::jsConsoleLog);
        console.put("warn", (ProxyExecutable) this::jsConsoleWarn);
        console.put("error", (ProxyExecutable) this::jsConsoleError);

        bindings.putMember("console", ProxyObject.fromMap(console));
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    private static String joinLogArgs(Value[] args) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                str.append("\t");
            }
            str.append(args[i].isHostObject() ? args[i].asHostObject() : args[i].toString());
        }
        return str.toString();
    }

    private Object jsConsoleLog(Value... args) {
        this.logger.log(Logger.LogType.LOG, Console.joinLogArgs(args));
        return null;
    }

    private Object jsConsoleWarn(Value... args) {
        this.logger.log(Logger.LogType.WARNING, Console.joinLogArgs(args));
        return null;
    }

    private Object jsConsoleError(Value... args) {
        this.logger.log(Logger.LogType.ERROR, Console.joinLogArgs(args));
        return null;
    }
}
