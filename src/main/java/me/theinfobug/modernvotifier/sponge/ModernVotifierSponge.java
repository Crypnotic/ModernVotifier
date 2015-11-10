package me.theinfobug.modernvotifier.sponge;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import me.theinfobug.modernvotifier.core.ModernVotifier;
import me.theinfobug.modernvotifier.core.objects.Vote;
import me.theinfobug.modernvotifier.core.objects.connectors.IConfig;
import me.theinfobug.modernvotifier.core.objects.connectors.IPlatform;
import me.theinfobug.modernvotifier.sponge.objects.SpongeConfig;
import me.theinfobug.modernvotifier.sponge.objects.SpongeVoteEvent;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.DefaultConfig;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

@Plugin(id = "modernvotifier", name = "ModernVotifier", version = "${project.version}")
public class ModernVotifierSponge implements IPlatform {

	@Inject
	private Game game;
	
	@Inject
	private Logger logger;
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private File configFile;
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	
	private ModernVotifier votifier;
	private SpongeConfig config;
	
	@Subscribe
	public void onEnable(ServerStartedEvent event){
		this.votifier = new ModernVotifier(this, "$project.version");
		this.config = new SpongeConfig(this, configFile, configManager);
		
		votifier.enable();
	}
	
	@Subscribe
	public void onDisable(ServerStoppingEvent event){
		votifier.disable();
	}
	
	public IConfig getCoreConfig() {
		return config;
	}

	public String getAddress() {
		return game.getServer().getBoundAddress().orElse(new InetSocketAddress("0.0.0.0", 0)).getHostName();
	}

	public void callVoteEvent(Vote vote) {
		game.getEventManager().post(new SpongeVoteEvent(vote));
	}

	public void runSynchronously(Runnable runnable) {
		game.getScheduler().createTaskBuilder().execute(runnable);
	}

	public void log(Level level, String message) {
		if (level == Level.INFO) {
			logger.info(message);
		} else if (level == Level.WARNING) {
			logger.warn(message);
		} else if (level == Level.SEVERE) {
			logger.error(message);
		}
	}
}
