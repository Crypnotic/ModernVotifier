package me.theinfobug.modernvotifier.bukkit;

import java.util.logging.Level;

import me.theinfobug.modernvotifier.bukkit.objects.BukkitConfig;
import me.theinfobug.modernvotifier.bukkit.objects.BukkitVoteEvent;
import me.theinfobug.modernvotifier.core.ModernVotifier;
import me.theinfobug.modernvotifier.core.objects.Vote;
import me.theinfobug.modernvotifier.core.objects.connectors.IConfig;
import me.theinfobug.modernvotifier.core.objects.connectors.IPlatform;

import org.bukkit.plugin.java.JavaPlugin;

public class ModernVotifierBukkit extends JavaPlugin implements IPlatform {

	private ModernVotifier votifier;
	private BukkitConfig config;

	@Override
	public void onEnable() {
		votifier = new ModernVotifier(this, getDescription().getVersion());
		config = new BukkitConfig(this);

		votifier.enable();
	}

	@Override
	public void onDisable() {
		votifier.disable();
	}

	@Override
	public IConfig getCoreConfig() {
		return config;
	}

	@Override
	public String getAddress() {
		return getServer().getIp() == null ? "0.0.0.0" : getServer().getIp();
	}

	@Override
	public void callVoteEvent(Vote vote) {
		getServer().getPluginManager().callEvent(new BukkitVoteEvent(vote));
	}

	@Override
	public void runSynchronously(Runnable runnable) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, runnable);
	}

	@Override
	public void log(Level level, String message) {
		getLogger().log(level, message);
	}
}
