package me.theinfobug.modernvotifier.core.objects.synchronizers;

import java.util.logging.Level;

import me.theinfobug.modernvotifier.core.ModernVotifier;

public class SyncLogger implements Runnable {

	protected static ModernVotifier votifier = ModernVotifier.getInstance();

	private Level level;
	private String message;

	public SyncLogger(Level level, String message) {
		this.level = level;
		this.message = message;
	}

	public void run() {
		ModernVotifier.log(level, message);
	}

	public void execute() {
		votifier.getPlatform().runSynchronously(this);
	}
}
