package me.theinfobug.modernvotifier.core.objects.synchronizers;

import me.theinfobug.modernvotifier.core.ModernVotifier;

public abstract class SyncScheduler implements Runnable {

	protected static ModernVotifier votifier = ModernVotifier.getInstance();

	public abstract void run();

	public void execute() {
		votifier.getPlatform().runSynchronously(this);
	}
}
