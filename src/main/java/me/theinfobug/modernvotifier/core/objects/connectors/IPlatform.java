package me.theinfobug.modernvotifier.core.objects.connectors;

import java.util.logging.Level;

import me.theinfobug.modernvotifier.core.objects.Vote;

public interface IPlatform {

	IConfig getCoreConfig();

	String getAddress();

	void callVoteEvent(Vote vote);
	
	void runSynchronously(Runnable runnable);

	void log(Level level, String message);
}
