package me.theinfobug.modernvotifier.bukkit.objects;

import me.theinfobug.modernvotifier.core.objects.Vote;
import me.theinfobug.modernvotifier.core.objects.events.IVoteEvent;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitVoteEvent extends Event implements IVoteEvent {

	private static final HandlerList handlers = new HandlerList();
	private Vote vote;

	public BukkitVoteEvent(Vote vote) {
		this.vote = vote;
	}

	public Vote getVote() {
		return vote;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
