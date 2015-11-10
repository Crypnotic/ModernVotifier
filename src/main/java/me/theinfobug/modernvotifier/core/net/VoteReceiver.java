/*
 * Copyright (C) 2012 Vex Software LLC
 * This file is part of Votifier.
 * 
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.theinfobug.modernvotifier.core.net;

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
import me.theinfobug.modernvotifier.core.crypto.RSA;
import me.theinfobug.modernvotifier.core.objects.Vote;

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

		initialize();
	}

	private void initialize() throws Exception {
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
		} catch (Exception ex) {
			ModernVotifier.log(Level.SEVERE, "Error initializing vote receiver. Please verify that the configured");
			ModernVotifier.log(Level.SEVERE, "IP address and port are not already in use. This is a common problem");
			ModernVotifier.log(Level.SEVERE,
					"with hosting services and, if so, you should check with your hosting provider.");
			ex.printStackTrace();
			throw new Exception(ex);
		}
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
		while (running) {
			try {
				Socket socket = server.accept();
				socket.setSoTimeout(5000); // Don't hang on slow connections.
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				InputStream in = socket.getInputStream();

				// Send them our version.
				writer.write("VOTIFIER " + votifier.getVersion());
				writer.newLine();
				writer.flush();

				// Read the 256 byte block.
				byte[] block = new byte[256];
				in.read(block, 0, block.length);

				// Decrypt the block.
				block = RSA.decrypt(block, votifier.getKeyPair().getPrivate());
				int position = 0;

				// Perform the opcode check.
				String opcode = readString(block, position);
				position += opcode.length() + 1;
				if (!opcode.equals("VOTE")) {
					// Something went wrong in RSA.
					throw new Exception("Unable to decode RSA");
				}

				// Parse the block.
				String serviceName = readString(block, position);
				position += serviceName.length() + 1;
				String username = readString(block, position);
				position += username.length() + 1;
				String address = readString(block, position);
				position += address.length() + 1;
				String timeStamp = readString(block, position);
				position += timeStamp.length() + 1;

				// Create the vote.
				final Vote vote = new Vote();
				vote.setServiceName(serviceName);
				vote.setUsername(username);
				vote.setAddress(address);
				vote.setTimeStamp(timeStamp);

				if (votifier.isDebugging())
					ModernVotifier.log(Level.INFO, "Received vote record -> " + vote);

				// Call event in a synchronized fashion to ensure that the
				// custom event runs in the
				// the main server thread, not this one.
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
				break; // Delimiter reached.
			builder.append((char) data[i]);
		}
		return builder.toString();
	}

	public Boolean isRunning() {
		return running;
	}
}
