package io.customrealms.runtime;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

public class RuntimeExecutor {
    /**
     * The executor service for asynchronous operations
     */
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * The Java plugin we're running within
     */
    private final JavaPlugin plugin;

    /**
     * The context to use for the runtime
     */
    private final Context context;

    /**
     * The logger to output errors to
     */
    private final Logger logger;

    /**
     * The Promise constructor
     */
    private final Value promiseCtor;

    public RuntimeExecutor(JavaPlugin plugin, Context context, Logger logger) {
        this.plugin = plugin;
        this.context = context;
        this.logger = logger;

        // Get the Promise constructor
        this.promiseCtor = this.context.getBindings("js").getMember("Promise");
    }

    public void release() {
        this.ioExecutor.shutdownNow();
    }

    /**
     * Safely executes plugin JavaScript code, and handles uncaught exceptions
     * @param runnable the runnable to execute
     * @param logger the logger to output errors to
     */
    public void executeSafely(Runnable runnable) {
        Runnable task = () -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                if (this.logger != null) {
                    this.logger.logUnhandledException(ex);
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this.plugin, task);
        }
    }

    private Throwable unwrapCompletionException(Throwable error) {
        while (
            (error instanceof CompletionException ||
             error instanceof ExecutionException) &&
            error.getCause() != null
        ) {
            error = error.getCause();
        }
    
        return error;
    }

    public <T> ProxyExecutable promiseFunction(Function<Value[], Supplier<T>> prepare) {
        return args -> {
            // Create the new Promise
            return this.promiseCtor.newInstance((ProxyExecutable) promiseArgs -> {
                // Get the resolve and reject functions
                Value resolve = promiseArgs[0];
                Value reject = promiseArgs[1];

                // Call the function with the args to get the CompletableFuture
                CompletableFuture<T> future = CompletableFuture.supplyAsync(prepare.apply(args), this.ioExecutor);

                // When the future is complete, resolve or reject the promise
                future.whenComplete((result, error) -> {
                    if (error != null) {
                        this.executeSafely(() -> reject.executeVoid(this.unwrapCompletionException(error)));
                    } else {
                        this.executeSafely(() -> resolve.executeVoid(result));
                    }
                });

                return null;
            });
        };
    }
}
