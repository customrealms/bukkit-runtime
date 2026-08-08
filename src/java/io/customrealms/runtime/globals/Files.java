package io.customrealms.runtime.globals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import io.customrealms.runtime.Global;
import io.customrealms.runtime.RuntimeExecutor;

public class Files implements Global {
    /**
     * The executor to use for the runtime
     */
    private final RuntimeExecutor executor;

    public Files(RuntimeExecutor executor) {
        this.executor = executor;
    }

    public void init(Value bindings) {
        HashMap<String, Object> files = new HashMap<>();
        files.put("read", this.executor.promiseFunction(this::jsReadFile));
        files.put("readdir", this.executor.promiseFunction(this::jsReadDir));
        files.put("exists", this.executor.promiseFunction(this::jsExists));
        files.put("remove", this.executor.promiseFunction(this::jsRemove));
        files.put("mkdir", this.executor.promiseFunction(this::jsMkdir));
        files.put("write", this.executor.promiseFunction(this::jsWriteFile));

        bindings.putMember("__fs", ProxyObject.fromMap(files));
    }

    /**
     * Releases all the values tying the runtime to the plugin
     */
    public void release() {}

    private Supplier<String> jsReadFile(Value... args) {
        String path = args[0].asString();
        return () -> {
            try {
                Path file = Path.of(path);
                if (!java.nio.file.Files.isRegularFile(file)) {
                    return null;
                }
                return java.nio.file.Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        };
    }

    private Supplier<String[]> jsReadDir(Value... args) {
        String path = args[0].asString();
        return () -> {
            File file = new File(path);
            if (!file.exists() || !file.isDirectory()) return null;
            return file.list();
        };
    }

    private Supplier<Boolean> jsExists(Value... args) {
        String path = args[0].asString();
        return () -> {
            File file = new File(path);
            return file.exists();
        };
    }

    private Supplier<Void> jsRemove(Value... args) {
        String path = args[0].asString();
        return () -> {
            File file = new File(path);
            if (!file.exists()) return null;
            file.delete();
            return null;
        };
    }

    private Supplier<Boolean> jsMkdir(Value... args) {
        String path = args[0].asString();
        boolean recursive = args[1].asBoolean();
        return () -> {
            File dir = new File(path);
            if (recursive) {
                return dir.mkdirs();
            } else {
                return dir.mkdir();
            }
        };
    }

    private Supplier<Void> jsWriteFile(Value... args) {
        String path = args[0].asString();
        String content = args[1].asString();
        return () -> {  
            try {
                File file = new File(path);
                FileWriter writer = new FileWriter(file);
                writer.write(content);
                writer.close();
                return null;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        };
    }
}
