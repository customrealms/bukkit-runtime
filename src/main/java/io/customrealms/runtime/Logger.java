package io.customrealms.runtime;

public interface Logger {

    /**
     * Types of log outputs
     */
    enum LogType {
        LOG,
        WARNING,
        ERROR
    }
    
    /**
     * Logs an exception that originated in the JavaScript runtime and wasn't handled
     * by the runtime internally
     * @param ex the exception that was thrown
     */
    void logUnhandledException(Exception ex);

    /**
     * Logs a value from a JS plugin runtime
     * @param type the type of log this is: log, warning, error
     * @param value the String value to log
     */
    void log(LogType type, String value);
    
}
