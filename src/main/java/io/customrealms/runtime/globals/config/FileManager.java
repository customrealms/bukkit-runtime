package io.customrealms.runtime.globals;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class FileManager {

    private final JavaPlugin java_plugin;

    private final String config_name;

    private FileConfiguration config;

    private File config_file;

    public  FileManager(JavaPlugin java_plugin, String config_name) {
        this.java_plugin = java_plugin;
        this.config_name = config_name;

        saveDefaultConfig();

    }

    public void reloadConfig() {
        if (this.config_file == null) this.config_file = new File(this.java_plugin.getDataFolder(), this.config_name);

        this.config = YamlConfiguration.loadConfiguration(this.config_file);

        InputStream stream = this.java_plugin.getResource(this.config_name);
        if (stream != null) {
            YamlConfiguration default_config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
            this.config.setDefaults(default_config);

        }

    }

    public FileConfiguration getConfig() {
        if (this.config == null) this.reloadConfig();
        return this.config;

    }

    public void saveConfig() {

        try {

            if (this.config == null || this.config_file == null) return;

            this.getConfig().save(this.config_file);

        } catch (IOException error) {
            this.java_plugin.getLogger().log(Level.SEVERE, "Error while trying to save" + this.config_name + " config", error.getMessage());

        }

    }

    public void saveDefaultConfig() {
        if (this.config_file == null) {
            this.config_file = new File(this.java_plugin.getDataFolder(), this.config_name);
            if (!this.config_file.exists()) this.java_plugin.saveResource(this.config_name, false);

        }

    }

}
