package me.theinfobug.modernvotifier.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;

import javax.crypto.Cipher;

import me.theinfobug.modernvotifier.core.ModernVotifier;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class Encryption {

	private static final char[] encodeMap = initEncodeMap();
	private static final byte[] decodeMap = initDecodeMap();
	private static final byte PADDING = 127;

	public static ByteArrayDataInput parse(KeyPair keys, InputStream input, int offset, int bits) {
		try {
			byte[] block = Streams.read(input, offset, bits);
			return ByteStreams.newDataInput(decrypt(block, keys.getPrivate()));
		} catch (Exception exception) {
			return null;
		}
	}

	public static byte[] encrypt(byte[] data, PublicKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	public static byte[] decrypt(byte[] data, PrivateKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data);
	}

	public static void saveKeypair(File directory, KeyPair keyPair) throws Exception {
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKey.getEncoded());
		FileOutputStream out = new FileOutputStream(directory + "/public.key");
		out.write(printBase64Binary(publicSpec.getEncoded()).getBytes());
		out.close();

		PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
		out = new FileOutputStream(directory + "/private.key");
		out.write(printBase64Binary(privateSpec.getEncoded()).getBytes());
		out.close();
	}

	public static KeyPair loadKeypair(File directory) throws Exception {
		File publicKeyFile = new File(directory, "/public.key");
		FileInputStream in = new FileInputStream(publicKeyFile);
		byte[] encodedPublicKey = new byte[(int) publicKeyFile.length()];
		in.read(encodedPublicKey);
		encodedPublicKey = parseBase64Binary(new String(encodedPublicKey));
		in.close();

		File privateKeyFile = new File(directory + "/private.key");
		in = new FileInputStream(privateKeyFile);
		byte[] encodedPrivateKey = new byte[(int) privateKeyFile.length()];
		in.read(encodedPrivateKey);
		encodedPrivateKey = parseBase64Binary(new String(encodedPrivateKey));
		in.close();

		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
		return new KeyPair(publicKey, privateKey);
	}

	public static KeyPair generate(int bits) throws Exception {
		ModernVotifier.log(Level.INFO, "ModernVotifier is generating an RSA key pair...");
		KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(bits, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);
		return keygen.generateKeyPair();
	}

	public static String printBase64Binary(byte[] input) {
		int length = input.length;
		char[] buf = new char[((length + 2) / 3) * 4];
		int remaining = length;
		int ptr = 0;
		int i = 0;
		for (; remaining >= 3; remaining -= 3, i += 3) {
			buf[ptr++] = encode(input[i] >> 2);
			buf[ptr++] = encode(((input[i] & 0x3) << 4) | ((input[i + 1] >> 4) & 0xF));
			buf[ptr++] = encode(((input[i + 1] & 0xF) << 2) | ((input[i + 2] >> 6) & 0x3));
			buf[ptr++] = encode(input[i + 2] & 0x3F);
		}
		if (remaining == 1) {
			buf[ptr++] = encode(input[i] >> 2);
			buf[ptr++] = encode(((input[i]) & 0x3) << 4);
			buf[ptr++] = '=';
			buf[ptr++] = '=';
		}
		if (remaining == 2) {
			buf[ptr++] = encode(input[i] >> 2);
			buf[ptr++] = encode(((input[i] & 0x3) << 4) | ((input[i + 1] >> 4) & 0xF));
			buf[ptr++] = encode((input[i + 1] & 0xF) << 2);
			buf[ptr++] = '=';
		}
		assert ptr == buf.length;
		return new String(buf);
	}

	public static byte[] parseBase64Binary(String text) {
		final int buflen = guessLength(text);
		final byte[] out = new byte[buflen];
		int o = 0;

		final int len = text.length();
		int i;

		final byte[] quadruplet = new byte[4];
		int q = 0;

		for (i = 0; i < len; i++) {
			char ch = text.charAt(i);
			byte v = decodeMap[ch];

			if (v != -1) {
				quadruplet[q++] = v;
			}

			if (q == 4) {
				out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
				if (quadruplet[2] != PADDING) {
					out[o++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
				}
				if (quadruplet[3] != PADDING) {
					out[o++] = (byte) ((quadruplet[2] << 6) | (quadruplet[3]));
				}
				q = 0;
			}
		}

		if (buflen == o) {
			return out;
		}

		byte[] nb = new byte[o];
		System.arraycopy(out, 0, nb, 0, o);
		return nb;
	}

	private static int guessLength(String text) {
		final int len = text.length();

		int j = len - 1;
		for (; j >= 0; j--) {
			byte code = decodeMap[text.charAt(j)];
			if (code == PADDING) {
				continue;
			}
			if (code == -1) {
				return text.length() / 4 * 3;
			}
			break;
		}

		j++;
		int padSize = len - j;
		if (padSize > 2) {
			return text.length() / 4 * 3;
		}
		return text.length() / 4 * 3 - padSize;
	}

	public static char encode(int i) {
		return encodeMap[i & 0x3F];
	}

	private static char[] initEncodeMap() {
		char[] map = new char[64];
		int i;
		for (i = 0; i < 26; i++) {
			map[i] = (char) ('A' + i);
		}
		for (i = 26; i < 52; i++) {
			map[i] = (char) ('a' + (i - 26));
		}
		for (i = 52; i < 62; i++) {
			map[i] = (char) ('0' + (i - 52));
		}
		map[62] = '+';
		map[63] = '/';

		return map;
	}

	private static byte[] initDecodeMap() {
		byte[] map = new byte[128];
		int i;
		for (i = 0; i < 128; i++) {
			map[i] = -1;
		}

		for (i = 'A'; i <= 'Z'; i++) {
			map[i] = (byte) (i - 'A');
		}
		for (i = 'a'; i <= 'z'; i++) {
			map[i] = (byte) (i - 'a' + 26);
		}
		for (i = '0'; i <= '9'; i++) {
			map[i] = (byte) (i - '0' + 52);
		}
		map['+'] = 62;
		map['/'] = 63;
		map['='] = PADDING;

		return map;
	}
}
