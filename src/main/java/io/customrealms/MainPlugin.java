package io.customrealms;

import io.customrealms.jsplugin.JsPlugin;
import io.customrealms.resource.Resource;
import org.bukkit.plugin.java.JavaPlugin;

import io.customrealms.controller.Controller;

/**
 * MainPlugin is the main JavaPlugin instance which serves as the entrypoint that wraps around
 * a CustomRealms JavaScript plugin
 */
public class MainPlugin extends JavaPlugin {

    /**
     * The controller runtime. JavaScript runtime which handles first-party CustomRealms API
     * management of the server.
     */
    private Controller controller;

    @Override
    public void onLoad() {

        // Load the code bundled into the JAR file
        Resource source_code = new Resource("plugin.js");

        // Create the JavaScript plugin instance
        JsPlugin plugin = new JsPlugin(this, source_code.getStringContents());

        // Create the plugin controller
        this.controller = new Controller(this, plugin);

    }

    @Override
    public void onEnable() {

        // Enable the controller
        if (this.controller != null) this.controller.enable();

    }

    @Override
    public void onDisable() {

        // Disable the controller
        if (this.controller != null) {
            this.controller.disable();
            this.controller = null;
        }

    }

}
