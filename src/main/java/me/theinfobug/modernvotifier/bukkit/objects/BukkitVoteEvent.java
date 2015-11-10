package me.theinfobug.modernvotifier.bukkit.objects;

import me.theinfobug.modernvotifier.core.objects.Vote;
import me.theinfobug.modernvotifier.core.objects.events.VoteEvent;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitVoteEvent extends Event implements VoteEvent {

	private static HandlerList handlers = new HandlerList();
	private Vote vote;

	public BukkitVoteEvent(Vote vote) {
		this.vote = vote;
	}

	@Override
	public Vote getVote() {
		return vote;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
