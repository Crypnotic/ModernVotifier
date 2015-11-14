package me.theinfobug.modernvotifier.core.objects;

public class Vote {

	private String service;
	private String username;
	private String address;
	private String timestamp;

	public Vote(String service, String username, String address, String timestamp) {
		this.service = service;
		this.username = username;
		this.address = address;
		this.timestamp = timestamp;
	}

	public String getServiceName() {
		return service;
	}

	public String getUsername() {
		return username;
	}

	public String getAddress() {
		return address;
	}

	public String getTimeStamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "Vote (from=" + service + ", username=" + username + ", address=" + address + ", timestamp=" + timestamp
				+ ")";
	}
}
