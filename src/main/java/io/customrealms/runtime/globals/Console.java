package io.customrealms.runtime.globals;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.Logger;

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

    public void init(Bindings bindings) {
        HashMap<String, Object> console = new HashMap<>();
        console.put("log", new JSFunction(this::jsConsoleLog));
        console.put("warn", new JSFunction(this::jsConsoleWarn));
        console.put("error", new JSFunction(this::jsConsoleError));

        bindings.put("console", console);
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

    private Object jsConsoleLog(Object thiz, Object... args) {
        this.logger.log(Logger.LogType.LOG, Console.joinLogArgs(args));
        return null;
    }

    private Object jsConsoleWarn(Object thiz, Object... args) {
        this.logger.log(Logger.LogType.WARNING, Console.joinLogArgs(args));
        return null;
    }

    private Object jsConsoleError(Object thiz, Object... args) {
        this.logger.log(Logger.LogType.ERROR, Console.joinLogArgs(args));
        return null;
    }
}
