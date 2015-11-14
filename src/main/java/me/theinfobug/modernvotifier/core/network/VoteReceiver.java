package me.theinfobug.modernvotifier.core.network;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

import javax.crypto.BadPaddingException;

import me.theinfobug.modernvotifier.core.ModernVotifier;
import me.theinfobug.modernvotifier.core.objects.Vote;
import me.theinfobug.modernvotifier.core.utils.Encryption;

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
				OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());

				writer.write("VOTIFIER " + votifier.getVersion() + "\n");
				writer.flush();

				byte[] block = new byte[256];
				input.read(block, 0, block.length);

				block = Encryption.decrypt(block, votifier.getKeyPair().getPrivate());
				int position = 0;

				String prefix = read(block, position);
				if (!prefix.equals("VOTE")) {
					ModernVotifier
							.log(Level.WARNING,
									"An exception occured whilst attempting to decode an incoming vote. This may be an error of the client sending the vote, or the message was tampered with in transport.");
					return;
				}

				String service = read(block, position += prefix.length() + 1);
				String username = read(block, position += service.length() + 1);
				String address = read(block, position += username.length() + 1);
				String timestamp = read(block, position += address.length() + 1);

				final Vote vote = new Vote(service, username, address, timestamp);

				if (votifier.isDebugging()) {
					ModernVotifier.log(Level.INFO, "Received vote record -> " + vote);
				}

				votifier.getPlatform().runSynchronously(new Runnable() {
					public void run() {
						votifier.getPlatform().callVoteEvent(vote);
					}
				});

				writer.close();
				input.close();
				socket.close();
			} catch (SocketException ex) {
				ModernVotifier.log(Level.WARNING, "Protocol error. Ignoring packet - " + ex.getLocalizedMessage());
			} catch (BadPaddingException ex) {
				ModernVotifier
						.log(Level.WARNING,
								"Unable to decrypt vote record. Make sure that that your public key matches the one you gave to the server list.");
			} catch (Exception ex) {
				ModernVotifier.log(Level.WARNING, "Exception caught while receiving a vote notification");
			}
		}
	}

	private String read(byte[] data, int offset) {
		StringBuilder builder = new StringBuilder();
		for (int i = offset; i < data.length; i++) {
			if (data[i] == '\n') {
				break;
			}
			builder.append((char) data[i]);
		}
		return builder.toString();
	}

	public Boolean isRunning() {
		return running;
	}
}
