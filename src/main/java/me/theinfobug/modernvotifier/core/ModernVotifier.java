package me.theinfobug.modernvotifier.core;

import java.io.File;
import java.security.KeyPair;
import java.util.logging.Level;

import me.theinfobug.modernvotifier.core.network.VoteReceiver;
import me.theinfobug.modernvotifier.core.objects.connectors.IConfig;
import me.theinfobug.modernvotifier.core.objects.connectors.IPlatform;
import me.theinfobug.modernvotifier.core.utils.Encryption;

public class ModernVotifier {

	private static ModernVotifier instance;

	private IPlatform platform;
	private String version;
	private IConfig config;
	private KeyPair keyPair;
	private VoteReceiver receiver;

	/* Config Values -START- */

	private String host;
	private Integer port;
	private Boolean debug;

	/* Config Values -END- */

	public ModernVotifier(IPlatform platform, String version) {
		this.platform = platform;
		this.version = version;
	}

	public void enable() {
		ModernVotifier.instance = this;

		config = platform.getCoreConfig();

		config.init();

		if (config.get("Listener.host") == null) {
			log(Level.INFO, "Configuring Votifier for the first time...");

			this.host = platform.getAddress();
			this.port = 8192;
			this.debug = false;

			config.set("host", host);
			config.set("port", port);
			config.set("debug", debug);
			config.save();

			log(Level.INFO, "------------------------------------------------------------------------------");
			log(Level.INFO, "Assigning Votifier to listen on port 8192. If you are hosting Craftbukkit on a");
			log(Level.INFO, "shared server please check with your hosting provider to verify that this port");
			log(Level.INFO, "is available for your use. Chances are that your hosting provider will assign");
			log(Level.INFO, "a different port, which you need to specify in config.yml");
			log(Level.INFO, "------------------------------------------------------------------------------");
		}

		try {
			File directory = new File(config.getFolder(), "rsa");
			if (!directory.exists()) {
				directory.mkdir();
				keyPair = Encryption.generate(2048);
				Encryption.saveKeypair(directory, keyPair);
			} else {
				keyPair = Encryption.loadKeypair(directory);
			}
		} catch (Exception exception) {
			log(Level.SEVERE, "An error occured whilst attempting to create the RSA keypairs. Exiting...");
			return;
		}

		try {
			receiver = new VoteReceiver(this, host, port);
			receiver.start();
		} catch (Exception exception) {
			log(Level.SEVERE, "An error occured whilst attempting to start the VoteReceiver. Exiting...");
			return;
		}
	}

	public void disable() {
		if (receiver != null && receiver.isRunning()) {
			receiver.interrupt();
		}
	}

	public IPlatform getPlatform() {
		return platform;
	}

	public VoteReceiver getReceiver() {
		return receiver;
	}

	public KeyPair getKeyPair() {
		return keyPair;
	}

	public String getVersion() {
		return version;
	}

	public Boolean isDebugging() {
		return debug;
	}

	public static void log(Level level, String message) {
		ModernVotifier.getInstance().getPlatform().log(level, message);
	}

	public static ModernVotifier getInstance() {
		return instance;
	}
}
