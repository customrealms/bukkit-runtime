package io.customrealms.runtime;

import com.eclipsesource.v8.V8ScriptExecutionException;

import java.util.logging.Level;

public class DefaultLogger implements Logger {

    /**
     * The underlying logger instance to log into
     */
    private java.util.logging.Logger underlying_logger;

    /**
     * Constructs a new logger that uses the default global logger instance
     */
    public DefaultLogger() {
        this.underlying_logger = java.util.logging.Logger.getGlobal();
    }

    /**
     * Constructs a new logger that uses a specific underlying logger instance
     * @param underlying_logger the logger instance to call into
     */
    public DefaultLogger(java.util.logging.Logger underlying_logger) {
        this.underlying_logger = underlying_logger;
    }

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
                this.underlying_logger.log(Level.WARNING, value);
                break;
            case LOG:
                this.underlying_logger.log(Level.INFO, value);
                break;
            case ERROR:
                this.underlying_logger.log(Level.SEVERE, value);
        }
    }

}
