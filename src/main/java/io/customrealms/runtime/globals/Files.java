package io.customrealms.runtime.globals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import io.customrealms.runtime.Global;

public class Files implements Global {
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

    public void init(Value bindings) {
        HashMap<String, Object> files = new HashMap<>();
        files.put("read", new JSFunction(this::jsReadFile));
        files.put("readdir", new JSFunction(this::jsReadDir));
        files.put("exists", new JSFunction(this::jsExists));
        files.put("remove", new JSFunction(this::jsRemove));
        files.put("mkdir", new JSFunction(this::jsMkdir));
        files.put("write", new JSFunction(this::jsWriteFile));

        bindings.putMember("__fs", ProxyObject.fromMap(files));
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    private Object jsReadFile(Value... args) {
        String path = args[0].asString();
        Function<String, Void> resolve = args[1].asHostObject();
        Function<Throwable, Void> reject = args[2].asHostObject();

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    Path file = Path.of(path);
                    if (!java.nio.file.Files.isRegularFile(file)) {
                        return null;
                    }
                    return java.nio.file.Files.readString(file, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, this.ioExecutor)
            .thenAccept(resolve::apply)
            .exceptionally(t -> {
                reject.apply(t.getCause() != null ? t.getCause() : t);
                return null;
            });

        return null;
    }

    private Object jsReadDir(Value... args) {
        String path = args[0].asString();
        Function<String[], Void> resolve = args[1].asHostObject();
        Function<Throwable, Void> reject = args[2].asHostObject();

        CompletableFuture
            .supplyAsync(() -> {
                File file = new File(path);
                if (!file.exists() || !file.isDirectory()) return null;
                return file.list();
            }, this.ioExecutor)
            .thenAccept(resolve::apply)
            .exceptionally(t -> {
                reject.apply(t.getCause() != null ? t.getCause() : t);
                return null;
            });

        return null;
    }

    private Object jsExists(Value... args) {
        String path = args[0].asString();
        Function<Boolean, Void> resolve = args[1].asHostObject();
        Function<Throwable, Void> reject = args[2].asHostObject();

        CompletableFuture
            .supplyAsync(() -> {
                File file = new File(path);
                return file.exists();
            }, this.ioExecutor)
            .thenAccept(resolve::apply)
            .exceptionally(t -> {
                reject.apply(t.getCause() != null ? t.getCause() : t);
                return null;
            });

        return null;
    }

    private Object jsRemove(Value... args) {
        String path = args[0].asString();
        Function<Object, Void> resolve = args[1].asHostObject();
        Function<Throwable, Void> reject = args[2].asHostObject();

        CompletableFuture
            .supplyAsync(() -> {
                File file = new File(path);
                if (!file.exists()) return null;
                file.delete();
                return null;
            }, this.ioExecutor)
            .thenAccept(resolve::apply)
            .exceptionally(t -> {
                reject.apply(t.getCause() != null ? t.getCause() : t);
                return null;
            });

        return null;
    }

    private Object jsMkdir(Value... args) {
        String path = args[0].asString();
        boolean recursive = args[1].asBoolean();
        Function<Boolean, Void> resolve = args[2].asHostObject();
        Function<Throwable, Void> reject = args[3].asHostObject();

        CompletableFuture
            .supplyAsync(() -> {
                File dir = new File(path);
                if (recursive) {
                    return dir.mkdirs();
                } else {
                    return dir.mkdir();
                }
            }, this.ioExecutor)
            .thenAccept(resolve::apply)
            .exceptionally(t -> {
                reject.apply(t.getCause() != null ? t.getCause() : t);
                return null;
            });

        return null;
    }

    private Object jsWriteFile(Value... args) {
        String path = args[0].asString();
        String content = args[1].asString();
        Function<Object, Void> resolve = args[2].asHostObject();
        Function<Throwable, Void> reject = args[3].asHostObject();

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    File file = new File(path);
                    FileWriter writer = new FileWriter(file);
                    writer.write(content);
                    writer.close();
                    return null;
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, this.ioExecutor)
            .thenAccept(resolve::apply)
            .exceptionally(t -> {
                reject.apply(t.getCause() != null ? t.getCause() : t);
                return null;
            });

        return null;
    }
}
