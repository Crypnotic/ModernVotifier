package me.theinfobug.modernvotifier.core.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

import me.theinfobug.modernvotifier.core.ModernVotifier;
import me.theinfobug.modernvotifier.core.objects.Vote;
import me.theinfobug.modernvotifier.core.utils.Encryption;
import me.theinfobug.modernvotifier.core.utils.Streams;

import com.google.common.io.ByteArrayDataInput;

public class VoteReceiver extends Thread {

	private final ModernVotifier votifier;
	private final String host;
	private final int port;
	private ServerSocket server;
	private boolean running = true;

	public VoteReceiver(final ModernVotifier votifier, String host, int port) throws Exception {
		this.votifier = votifier;
		this.host = host;
		this.port = port;
	}

	public void shutdown() {
		running = false;
		if (server == null) {
			return;
		}
		try {
			server.close();
		} catch (Exception ex) {
			ModernVotifier.log(Level.WARNING, "Unable to shut down vote receiver cleanly.");
		}
	}

	@Override
	public void run() {
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
		} catch (Exception ex) {
			ModernVotifier
					.log(Level.SEVERE,
							"Error initializing vote receiver. Please verify that the configured IP address and port are not already in use. This is a common problem with hosting services and, if so, you should check with your hosting provider.");
			return;
		}
		while (running) {
			try {
				Socket socket = server.accept();
				socket.setSoTimeout(5000);
				InputStream input = socket.getInputStream();
				OutputStreamWriter output = new OutputStreamWriter(socket.getOutputStream());

				Streams.write(output, "VOTIFIER " + votifier.getVersion() + "\n");

				ByteArrayDataInput stream = Encryption.parse(votifier.getKeyPair(), input, 0, 256);
				if (stream == null) {
					throw new Exception();
				}

				String prefix = stream.readLine();
				if (!prefix.equals("VOTE")) {
					ModernVotifier
							.log(Level.WARNING,
									"An exception occured whilst attempting to decode an incoming vote. This may be an error of the client sending the vote, or the message was tampered with in transport.");
					return;
				}

				String service = stream.readLine();
				String address = stream.readLine();
				String username = stream.readLine();
				String timestamp = stream.readLine();

				final Vote vote = new Vote(service, address, username, timestamp);

				if (votifier.isDebugging()) {
					ModernVotifier.log(Level.INFO, "Received vote record -> " + vote);
				}

				votifier.getPlatform().runSynchronously(new Runnable() {
					public void run() {
						votifier.getPlatform().callVoteEvent(vote);
					}
				});

				output.close();
				input.close();
				socket.close();
			} catch (SocketException exception) {
				ModernVotifier.log(Level.WARNING,
						"Protocol error. Ignoring packet - " + exception.getLocalizedMessage());
			} catch (IOException exception) {
				ModernVotifier.log(Level.WARNING, "An error occured whilst attempting to read incoming decrypted data");
			} catch (Exception exception) {
				ModernVotifier.log(Level.WARNING, "Exception caught while receiving a vote notification");
			}
		}
	}

	public Boolean isRunning() {
		return running;
	}
}
