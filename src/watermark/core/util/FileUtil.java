package watermark.core.util;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

/**
 * This class is responsible for handling common file operations.
 *
 * @author Ryan M. Kane
 */
public class FileUtil {
	// System independent new-line/line-feed.
	public static final String ENDL = System.getProperty("line.separator");

	/**
	 * Loads a Properties object for a given file.
	 *
	 * @param filename - the properties File filename.
	 * @return a Properties object.
	 */
	public static Properties loadProperties(String filename) {
		Properties props = new Properties();
		InputStream is = loadFileStream(filename);

		// Try loading properties from the file (if found)
		try {
			props.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			is.close();
		} catch (IOException e) {
		}

		return props;
	}

	public static boolean fileExists(String filename) {
		File f = new File("./" + filename);

		return f.isFile() && f.exists();
	}

	public static InputStream loadFileStream(String filename) {
		// First try loading from the file system.
		try {
			return new FileInputStream(new File("./" + filename));
		} catch (Exception e) {
			// Try loading from classpath.
			return FileUtil.class.getResourceAsStream("/resources/" + filename);
		}
	}

	/**
	 * Saves a map of configuration properties to a properties file.
	 *
	 * @param propMap - key-value pairs of properties
	 * @param filename - the filename to save the properties to.
	 * @param description - a description of the property list.
	 */
	public static void saveProperties(Map<String, String> propMap, String filename, String description) {
		PrintWriter writer = null;
		Properties props = new Properties();

		try {
			writer = new PrintWriter(new File(FileUtil.class.getResource(filename).getPath()));

			for (Map.Entry<String, String> entry : propMap.entrySet()) {
				props.setProperty(entry.getKey(), entry.getValue());
			}

			props.store(writer, description);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	/**
	 * Saves text to a resource file.
	 *
	 * @param text - text to save to the file.
	 * @param filename - the filename to save the text to.
	 */
	public static void writeConfig(String text, String filename) {
		PrintWriter writer = null;

		try {
			writer = new PrintWriter(new File("./" + filename).getPath());
			writer.print(text);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	public static String loadResourceFileText(String resourcePath) {
		StringBuilder fileContents = new StringBuilder();
		InputStream inStream = null;
		InputStreamReader streamReader = null;
		BufferedReader reader = null;
		String line = null;

		try {
			inStream = loadFileStream(resourcePath);
			streamReader = new InputStreamReader(inStream);
			reader = new BufferedReader(streamReader);

			while ((line = reader.readLine()) != null) {
				fileContents.append(line + ENDL);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
			try {
				streamReader.close();
			} catch (IOException e) {
			}
			try {
				inStream.close();
			} catch (IOException e) {
			}
		}

		return fileContents.toString();
	}

	// ========================================================================
	// Unused
	// ========================================================================

	/**
	 * Opens a file in the User's default file editor.
	 *
	 * @param file - the file to edit.
	 * @return whether the action succeeded without error.
	 */
	public static boolean editFile(File file) {
		try {
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				String cmd = "rundll32 url.dll,FileProtocolHandler " + file.getCanonicalPath();
				Runtime.getRuntime().exec(cmd);
			} else {
				Desktop.getDesktop().edit(file);
			}

			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
