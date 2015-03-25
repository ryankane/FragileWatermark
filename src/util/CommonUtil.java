package util;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CommonUtil {
	public static String byteToHex(byte b) {
		return Integer.toString((b & 0xff) + 0x100, 16).substring(1);
	}

	public static String byteToBin(byte b) {
		return String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(' ', '0');
	}

	public static String hexDump(byte[] bytes) {
		StringBuffer sb = new StringBuffer();
		for (byte b : bytes) {
			sb.append(byteToHex(b)); // sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static String byteArrayToString(byte[] bytes) {
		return new String(bytes);
	}

	public static String getPixelARGB(int pixel) {
		int alpha = (pixel >> 24) & 0xff;
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;
		return String.format("argb(%3d, %3d, %3d, %3d)", alpha, red, green,
				blue);
	}

	public static String getPixelBinaryARGB(int pixel) {
		Color argb = new Color(pixel, true);
		byte a = (byte) argb.getAlpha();
		byte r = (byte) argb.getRed();
		byte g = (byte) argb.getGreen();
		byte b = (byte) argb.getBlue();

		return String.format("b[%s, %s, %s, %s]", CommonUtil.byteToBin(a),
				CommonUtil.byteToBin(r), CommonUtil.byteToBin(g),
				CommonUtil.byteToBin(b));
	}

	public static String hashStrMD5(String input) {
		return CommonUtil.hexDump(hashMD5(input));
	}

	public static byte[] hashMD5(String input) {
		try {
			byte[] bytesOfMessage = input.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");

			return md.digest(bytesOfMessage);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return null;
	}
}
