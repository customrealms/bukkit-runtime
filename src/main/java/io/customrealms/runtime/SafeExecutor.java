package io.customrealms.runtime;

import com.eclipsesource.v8.V8ScriptExecutionException;

/**
 * SafeExecutor is a class that enables us to easily run JavaScript code in a V8 instance without
 * allowing Exceptions to escape into the Java runtime.
 */
public class SafeExecutor {

    /**
     * Safely executes plugin JavaScript code, and handles uncaught exceptions
     * @param runnable the runnable to execute
     * @param logger the logger to output errors to
     */
    public static <T> T executeSafely(SafeRunnable<T> runnable, Logger logger) {

        try {

            // Run the runnable
            return runnable.run();

        } catch (V8ScriptExecutionException ex) {

            // Pass the exception to the logger
            if (logger != null) {
                logger.logUnhandledException(ex);
            }

        }

        // Return null by default
        return null;

    }

    /**
     * Safely executes plugin JavaScript code, and handles uncaught exceptions
     * @param runnable the runnable to execute
     * @param logger the logger to output errors to
     */
    public static void executeSafely(Runnable runnable, Logger logger) {

        try {

            // Run the runnable
            runnable.run();

        } catch (V8ScriptExecutionException ex) {

            // Pass the exception to the logger
            if (logger != null) {
                logger.logUnhandledException(ex);
            }

        }

    }

}
