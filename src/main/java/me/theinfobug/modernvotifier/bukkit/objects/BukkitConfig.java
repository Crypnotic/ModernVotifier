package me.theinfobug.modernvotifier.bukkit.objects;

import java.io.File;

import me.theinfobug.modernvotifier.bukkit.ModernVotifierBukkit;
import me.theinfobug.modernvotifier.core.objects.connectors.IConfig;

import org.bukkit.configuration.file.YamlConfiguration;

public class BukkitConfig implements IConfig {

	public ModernVotifierBukkit plugin;
	private File folder;
	private File file;
	private YamlConfiguration config;

	public BukkitConfig(ModernVotifierBukkit plugin) {
		this.plugin = plugin;
	}

	public void init() {
		try {
			this.folder = plugin.getDataFolder();
			this.file = new File(plugin.getDataFolder(), "config.yml");
			if (!folder.exists()) {
				folder.mkdirs();
			}
			if (!file.exists()) {
				file.createNewFile();
			}
			this.config = YamlConfiguration.loadConfiguration(file);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public File getFolder() {
		return folder;
	}

	public File getFile() {
		return file;
	}

	public String get(String path) {
		return config.getString(path);
	}

	public void set(String path, Object value) {
		config.set(path, value);
	}

	public void save() {
		try {
			config.save(file);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
}
