package io.customrealms.runtime;

import com.eclipsesource.v8.V8ScriptExecutionException;

public class DefaultLogger implements Logger {

    /**
     * Logs an exception that originated in the JavaScript runtime and wasn't handled
     * by the runtime internally
     * @param ex the exception that was thrown
     */
    public void logUnhandledException(V8ScriptExecutionException ex) {
        System.err.println(ex.getMessage());
    }

    /**
     * Logs a value from a JS plugin runtime
     * @param type the type of log this is: log, warning, error
     * @param value the String value to log
     */
    public void log(LogType type, String value) {
        switch (type) {
            case WARNING:
            case LOG:
                System.out.println(value);
                break;
            case ERROR:
                System.err.println(value);
        }
    }

}
