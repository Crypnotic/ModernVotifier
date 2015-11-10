package me.theinfobug.modernvotifier.sponge.objects;

import me.theinfobug.modernvotifier.core.objects.Vote;

import org.spongepowered.api.event.Event;

public class SpongeVoteEvent implements Event {

	private Vote vote;

	public SpongeVoteEvent(Vote vote) {
		this.vote = vote;
	}

	public Vote getVote() {
		return vote;
	}
}
