package me.theinfobug.modernvotifier.core.network;

import java.io.BufferedWriter;
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
		if (server == null)
			return;
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
			ModernVotifier.log(Level.SEVERE, "Error initializing vote receiver. Please verify that the configured");
			ModernVotifier.log(Level.SEVERE, "IP address and port are not already in use. This is a common problem");
			ModernVotifier.log(Level.SEVERE,
					"with hosting services and, if so, you should check with your hosting provider.");
			ex.printStackTrace();
			return;
		}
		while (running) {
			try {
				Socket socket = server.accept();
				socket.setSoTimeout(5000);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				InputStream in = socket.getInputStream();

				writer.write("VOTIFIER " + votifier.getVersion());
				writer.newLine();
				writer.flush();

				byte[] block = new byte[256];
				in.read(block, 0, block.length);

				block = Encryption.decrypt(block, votifier.getKeyPair().getPrivate());
				int position = 0;

				String opcode = readString(block, position);
				position += opcode.length() + 1;
				if (!opcode.equals("VOTE")) {
					throw new Exception("Unable to decode RSA");
				}

				String serviceName = readString(block, position);
				position += serviceName.length() + 1;
				String username = readString(block, position);
				position += username.length() + 1;
				String address = readString(block, position);
				position += address.length() + 1;
				String timeStamp = readString(block, position);
				position += timeStamp.length() + 1;

				final Vote vote = new Vote();
				vote.setServiceName(serviceName);
				vote.setUsername(username);
				vote.setAddress(address);
				vote.setTimeStamp(timeStamp);

				if (votifier.isDebugging())
					ModernVotifier.log(Level.INFO, "Received vote record -> " + vote);

				votifier.getPlatform().runSynchronously(new Runnable() {
					public void run() {
						votifier.getPlatform().callVoteEvent(vote);
					}
				});

				writer.close();
				in.close();
				socket.close();
			} catch (SocketException ex) {
				ModernVotifier.log(Level.WARNING, "Protocol error. Ignoring packet - " + ex.getLocalizedMessage());
			} catch (BadPaddingException ex) {
				ModernVotifier.log(Level.WARNING, "Unable to decrypt vote record. Make sure that that your public key");
				ModernVotifier.log(Level.WARNING, "matches the one you gave the server list.");
				ex.printStackTrace();
			} catch (Exception ex) {
				ModernVotifier.log(Level.WARNING, "Exception caught while receiving a vote notification");
				ex.printStackTrace();
			}
		}
	}

	private String readString(byte[] data, int offset) {
		StringBuilder builder = new StringBuilder();
		for (int i = offset; i < data.length; i++) {
			if (data[i] == '\n')
				break;
			builder.append((char) data[i]);
		}
		return builder.toString();
	}

	public Boolean isRunning() {
		return running;
	}
}
