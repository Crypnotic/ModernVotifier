package me.theinfobug.modernvotifier.core.utils;

import java.io.InputStream;
import java.io.Writer;
import java.util.logging.Level;

import me.theinfobug.modernvotifier.core.ModernVotifier;

public class Streams {

	public static void write(Writer writer, String... messages) {
		try {
			if (writer == null || messages == null || messages.length == 0) {
				return;
			}
			for (String message : messages) {
				writer.write(message);
			}
			writer.flush();
		} catch (Exception exception) {
			ModernVotifier.log(Level.WARNING, "An error occured whilst attempting to write to an input/output stream");
		}
	}

	public static byte[] read(InputStream stream, int offset, int bits) {
		try {
			byte[] block = new byte[bits];
			stream.read(block, offset, block.length);
			return block;
		} catch (Exception exception) {
			return null;
		}
	}
}
