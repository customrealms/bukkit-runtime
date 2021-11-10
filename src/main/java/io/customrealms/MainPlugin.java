package io.customrealms;

import io.customrealms.jsplugin.JsPlugin;
import io.customrealms.jsplugin.JsPluginDescriptor;
import io.customrealms.resource.Resource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import io.customrealms.controller.Controller;

import java.io.File;

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



    }

    @Override
    public void onEnable() {

        // Define the descriptor of the plugin
        JsPluginDescriptor descriptor = new JsPluginDescriptor();
        descriptor.name = "My CustomRealms Plugin";
        descriptor.version = "0.0.1";
        descriptor.command_prefix = "myplugin";

        // Load the code bundled into the JAR file
        Resource source_code = new Resource("plugin.js");

        // Create the JavaScript plugin instance
        JsPlugin plugin = new JsPlugin(
                this,
                descriptor,
                source_code.getStringContents()
        );

        // Create the plugin controller
        this.controller = new Controller(
                this,
                plugin
        );

        // Enable the controller
        this.controller.enable();

    }

    @Override
    public void onDisable() {

        // Disable the controller
        this.controller.disable();

    }

}
