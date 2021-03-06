package watermark.core.util;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.EventListener;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * This class contains factory methods for creating graphical components.
 *
 * @author Ryan M. Kane
 */
public class GuiUtils {
	// =========================================================================
	// Menu Creation
	// =========================================================================
	public static JMenu createMenu(String label, int mnemonic, String description) {
		return createMenuItem(new JMenu(label), mnemonic, description, null);
	}

	/**
	 * @TODO: This does not currently work.
	 *
	 * @param actionClass
	 * @return
	 */
	public static <T extends Action> JMenuItem createMenu(Class<T> actionClass) {
		try {
			return new JMenuItem(actionClass.newInstance());
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static JMenuItem createMenuItem(String label, int mnemonic, String description, ActionListener action) {
		return createMenuItem(new JMenuItem(label), mnemonic, description, action);
	}

	public static JCheckBoxMenuItem createCheckBoxMenuItem(String label, int mnemonic, String description, ItemListener action) {
		return createMenuItem(new JCheckBoxMenuItem(label), mnemonic, description, action);
	}

	public static <T extends JMenuItem, E extends EventListener> T createMenuItem(T source, int mnemonic, String description, E action) {
		source.setMnemonic(mnemonic);
		source.getAccessibleContext().setAccessibleDescription(description);

		if (action instanceof ActionListener) {
			source.addActionListener((ActionListener) action);
		} else if (action instanceof ItemListener) {
			source.addItemListener((ItemListener) action);
		}

		return source;
	}

	// =========================================================================
	// Alerts/Message Dialogs
	// =========================================================================
	public static void showErrorMessage(Component parentComponent, String message, String title) {
		showMessage(parentComponent, title, message, JOptionPane.ERROR_MESSAGE);
	}

	public static void showErrorMessage(Component parentComponent, String message) {
		showErrorMessage(parentComponent, message, "Error");
	}

	public static void showErrorMessage(String message) {
		showErrorMessage(null, message);
	}

	public static void showSuccessMessage(Component parentComponent, String message, String title) {
		showMessage(parentComponent, title, message, JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showSuccessMessage(Component parentComponent, String message) {
		showSuccessMessage(parentComponent, message, "Success");
	}

	public static void showSuccessMessage(String message) {
		showSuccessMessage(null, message);
	}

	public static void showMessage(Component parentComponent, String title, String message, int messageType) {
		JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
	}
}
