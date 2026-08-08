package io.customrealms.runtime;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
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
     * The logger to output errors to
     */
    private final Logger logger;

    public RuntimeExecutor(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
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

    private <T> void executePromise(
        Supplier<T> supplier,
        Consumer<? super T> resolve,
        Consumer<? super Throwable> reject
    ) {
        CompletableFuture
            .supplyAsync(supplier, this.ioExecutor)
            .whenComplete((result, error) -> {
                if (error != null) {
                    this.executeSafely(() -> reject.accept(this.unwrapCompletionException(error)));
                } else {
                    this.executeSafely(() -> resolve.accept(result));
                }
            });
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

    public <T> ProxyExecutable wrapPromise(Function<Value[], Supplier<T>> prepare) {
        return args -> {
            if (args.length < 2) {
                throw new IllegalArgumentException("Promise must have at least 2 arguments");
            }

            Value resolve = args[args.length - 2];
            Value reject = args[args.length - 1];

            if (!resolve.canExecute() || !reject.canExecute()) {
                throw new IllegalArgumentException("Resolve and reject must be functions");
            }

            // Everything involving Graal Values happens NOW, while we're
            // still executing JavaScript on the main thread.
            Value[] operationArgs = Arrays.copyOf(args, args.length - 2);

            Supplier<T> task = prepare.apply(operationArgs);

            // From this point onward the background executor only sees Java values.
            this.executePromise(
                task,
                resolve::executeVoid,
                reject::executeVoid
            );

            return null;
        };
    }
}
