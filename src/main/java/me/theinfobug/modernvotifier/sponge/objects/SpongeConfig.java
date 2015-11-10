package me.theinfobug.modernvotifier.sponge.objects;

import java.io.File;

import me.theinfobug.modernvotifier.core.objects.connectors.IConfig;
import me.theinfobug.modernvotifier.sponge.ModernVotifierSponge;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class SpongeConfig implements IConfig {

	public ModernVotifierSponge plugin;
	private File folder;
	private File file;
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private CommentedConfigurationNode config;

	public SpongeConfig(ModernVotifierSponge plugin, File file,
			ConfigurationLoader<CommentedConfigurationNode> configManager) {
		this.plugin = plugin;
		this.file = file;
		this.configManager = configManager;
	}

	public void init() {
		try {
			this.folder = file.getParentFile();
			if (!folder.isDirectory()) {
				folder.delete();
				folder.mkdirs();
			}
			this.file = new File(folder, "config.yml");
			if (!folder.exists()) {
				folder.mkdirs();
			}
			if (!file.exists()) {
				file.createNewFile();
			}
			configManager.load();
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
		config.getNode(path).setValue(value);
	}

	public void save() {
		try {
			configManager.save(config);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
}
