package watermark.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import watermark.core.util.FileUtil;
import watermark.core.util.GuiUtils;
import watermark.gui.AppConfig;
import watermark.gui.AppIcons;

public class EditFileDialog extends JDialog {
	private static final long serialVersionUID = 6629613323587008928L;

	private static final int PAD;
	private static final Color BORDER_COLOR;
	private static final Border PAD_BORDER;
	private static final Font FONT;
	private static final boolean LINE_WRAP;

	private static final float FONT_SIZE = 12f;
	private static final float FONT_SIZE_MIN = 10f;
	private static final float FONT_SIZE_MAX = 24f;

	static {
		PAD = 8;
		BORDER_COLOR = new Color(0xFFD7D7D7, true);
		PAD_BORDER = BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD);
		FONT = new Font("courier new", Font.PLAIN, (int) FONT_SIZE);
		LINE_WRAP = true;
	}

	private int width;
	private int height;
	private String[] reqProps;
	private String resourceName;

	private JTextArea txtArea;
	private JScrollPane txtAreaScroll;
	private TextLineNumber textLineNumber;
	private JToolBar toolBar;
	private JButton saveButton;
	private JButton undoButton;
	private JButton redoButton;
	private JButton zoomInButton;
	private JButton zoomOutButton;
	private JButton zoomDefButton;

	private TextAreaUndoManager undoManager;

	public EditFileDialog(Frame frameOwner, int width, int height, String resourceName, String[] reqProps) {
		this(frameOwner, width, height);

		// Load the resource.
		this.loadFile(resourceName);
		this.reqProps = reqProps;
	}

	public EditFileDialog(Frame frameOwner, int width, int height) {
		super(frameOwner);

		this.width = width;
		this.height = height;
		this.undoManager = new TextAreaUndoManager(60);

		this.initComponent();
		this.addChildren();
		this.addListeners();
		this.setIconImage(AppIcons.getAppImage());
		this.setTitle("No file loaded");
		this.setLocationByPlatform(true);
		this.pack();
	}

	protected void initComponent() {
		toolBar = new JToolBar("Still draggable");
		saveButton = createButton(new SaveAction(), AppIcons.getSaveIcon(), "Save");
		undoButton = createButton(new UndoAction(), AppIcons.getUndoIcon(), "Undo");
		redoButton = createButton(new RedoAction(), AppIcons.getRedoIcon(), "Redo");
		zoomInButton = createButton(new ZoomInAction(), AppIcons.getZoomInIcon(), "Zoom In (Ctrl + Mouse Wheel Up)");
		zoomOutButton = createButton(new ZoomOutAction(), AppIcons.getZoomOutIcon(), "Zoom Out (Ctrl + Mouse Wheel Down)");
		zoomDefButton = createButton(new ZoomDefaultAction(), AppIcons.getZoomDefaultIcon(), "Zoom Default");

		txtArea = new JTextArea();
		txtAreaScroll = new JScrollPane();
		textLineNumber = new TextLineNumber(txtArea);

		txtArea.setAutoscrolls(true);
		txtArea.setPreferredSize(new Dimension(width, height));
		txtArea.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
		txtArea.setFont(FONT);
		txtArea.setLineWrap(LINE_WRAP);
		txtArea.addMouseWheelListener(new ZoomMouseWheelListener());

		txtAreaScroll.setViewportView(txtArea);
		txtAreaScroll.setAutoscrolls(true);

		txtAreaScroll.setBorder(PAD_BORDER);
		txtAreaScroll.setRowHeaderView(textLineNumber);

		// Attach the undo manager to the text area.
		undoManager.attach(txtArea);
	}

	private void addListeners() {
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (saveButton.isEnabled()) {
					int option = JOptionPane.showConfirmDialog(null,
							"Do you wish to save your changes", "Document Modified",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE);

					if (option == JOptionPane.YES_OPTION) {
						// If save was selected, but the save failed, prevent
						// the window from closing. Else, allow the window to be
						// closed.
						if (!handleSave()) {
							setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
						} else {
							setDefaultCloseOperation(DISPOSE_ON_CLOSE);
						}
					} else {
						setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					}
				}
			}
		});

		txtArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				handleDocumentUpdate(e);
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				handleDocumentUpdate(e);
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				handleDocumentUpdate(e);
			}
		});
	}

	private void handleDocumentUpdate(DocumentEvent e) {
		if (!this.saveButton.isEnabled()) {
			this.saveButton.setEnabled(true);
		}

		if (this.getUndoManager().canUndo()) {
			this.undoButton.setEnabled(true);
		}

		if (this.getUndoManager().canRedo()) {
			this.redoButton.setEnabled(true);
		}
	}

	protected void addChildren() {
		this.setLayout(new BorderLayout());

		toolBar.add(saveButton);
		toolBar.add(undoButton);
		toolBar.add(redoButton);
		toolBar.add(zoomInButton);
		toolBar.add(zoomOutButton);
		toolBar.add(zoomDefButton);

		this.add(toolBar, BorderLayout.PAGE_START);
		this.add(txtAreaScroll, BorderLayout.CENTER);
	}

	public void loadFile(String resourceName) {
		if (resourceName == this.resourceName) {
			GuiUtils.showErrorMessage(String.format("The file is already loaded: %s%n", resourceName));
			return;
		}

		this.loadResource(resourceName);
	}

	public void reloadResource() {
		loadResource(this.resourceName);
	}

	public void loadResource(String resourceName) {
		this.resourceName = resourceName;
		this.txtArea.setText(FileUtil.loadResourceFileText(resourceName));
		this.setTitle("Editing: " + resourceName);
	}

	public void launch() {
		// If not already visible, make visible and reload the file.
		if (!this.isVisible()) {
			this.reloadResource();
			this.setVisible(true);
			this.undoManager.resetEdits();
			this.saveButton.setEnabled(false);
			this.undoButton.setEnabled(false);
			this.redoButton.setEnabled(false);
		}

		// Request focus on text area.
		this.txtArea.requestFocusInWindow();
	}

	public void setReqProps(String[] reqProps) {
		this.reqProps = reqProps;
	}

	private JButton createButton(Action action, Icon icon, String toolTipText) {
		JButton button = new JButton(action);

		button.setIcon(icon);
		button.setToolTipText(toolTipText);
		button.setBorderPainted(false);
		button.setFocusPainted(false);

		return button;
	}

	public boolean validateProperties(String text) {
		try {
			Properties props = new Properties();
			InputStream stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
			props.load(stream);

			if (props == null || props.isEmpty()) {
				GuiUtils.showErrorMessage("Properties are missing!");
				return false;
			}

			for (String prop : reqProps) {
				if (!props.containsKey(prop)) {
					GuiUtils.showErrorMessage("Property is missing: " + prop);
					return false;
				}
			}

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private UndoManager getUndoManager() {
		return this.undoManager.getUndoManager();
	}

	// ========================================================================
	// Action handlers
	// ========================================================================
	private boolean handleSave() {
		String rawText = txtArea.getText();

		if (!validateProperties(rawText)) {
			GuiUtils.showErrorMessage("Properties invalid. Please check for errors.");
			return false;
		}

		if (!FileUtil.fileExists(resourceName)) {
			GuiUtils.showMessage(null, AppConfig.APP_TITLE,
					"File does not exist, creating new file.",
					JOptionPane.INFORMATION_MESSAGE);
		}

		FileUtil.writeConfig(rawText, resourceName);

		if (!FileUtil.fileExists(resourceName)) {
			GuiUtils.showErrorMessage("Save failed. Please check directory permissions.");
			return false;
		} else {
			GuiUtils.showSuccessMessage("Wrote configuration to: " + resourceName);
			return true;
		}
	}

	private void handleUndo() {
		if (getUndoManager().canUndo()) {
			getUndoManager().undo();

			if (getUndoManager().canRedo() && !redoButton.isEnabled()) {
				redoButton.setEnabled(true);
			}
		}

		if (!getUndoManager().canUndo()) {
			undoButton.setEnabled(false);
		}
	}

	private void handleRedo() {
		if (getUndoManager().canRedo()) {
			getUndoManager().redo();

			if (!undoButton.isEnabled()) {
				undoButton.setEnabled(true);
			}
		}

		if (getUndoManager().canUndo() && !getUndoManager().canRedo()) {
			redoButton.setEnabled(false);
		}
	}

	private void handleZoomIn() {
		float size = txtArea.getFont().getSize();

		if (size < FONT_SIZE_MAX) {
			txtArea.setFont(txtArea.getFont().deriveFont(size += 1f));

			if (!zoomOutButton.isEnabled()) {
				zoomOutButton.setEnabled(true);
			}
		}

		if (size == FONT_SIZE_MAX) {
			zoomInButton.setEnabled(false);
		}
	}

	private void handleZoomOut() {
		float size = txtArea.getFont().getSize();

		if (size > FONT_SIZE_MIN) {
			txtArea.setFont(txtArea.getFont().deriveFont(size -= 1f));

			if (!zoomInButton.isEnabled()) {
				zoomInButton.setEnabled(true);
			}
		}

		if (size == FONT_SIZE_MIN) {
			zoomOutButton.setEnabled(false);
		}
	}

	private void handleZoomDefault() {
		txtArea.setFont(txtArea.getFont().deriveFont(FONT_SIZE));

		if (!zoomInButton.isEnabled()) {
			zoomInButton.setEnabled(true);
		}

		if (!zoomOutButton.isEnabled()) {
			zoomOutButton.setEnabled(true);
		}
	}

	// ========================================================================
	// Toolbar Actions
	// ========================================================================

	private class SaveAction extends AbstractAction {
		private static final long serialVersionUID = -779576692147872538L;

		public SaveAction() {
			super();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (handleSave()) {
				saveButton.setEnabled(false);
			}
		}
	}

	private class UndoAction extends AbstractAction {
		private static final long serialVersionUID = 3148924981951371406L;

		public UndoAction() {
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			handleUndo();
		}
	}

	private class RedoAction extends AbstractAction {
		private static final long serialVersionUID = 5509505828576734702L;

		public RedoAction() {
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			handleRedo();
		}
	}

	private class ZoomInAction extends AbstractAction {
		private static final long serialVersionUID = -4077929805352920410L;

		public ZoomInAction() {
			super();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			handleZoomIn();
		}
	}

	private class ZoomOutAction extends AbstractAction {
		private static final long serialVersionUID = 318723677338959431L;

		public ZoomOutAction() {
			super();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			handleZoomOut();
		}
	}

	private class ZoomDefaultAction extends AbstractAction {
		private static final long serialVersionUID = 318723677338959431L;

		public ZoomDefaultAction() {
			super();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			handleZoomDefault();
		}
	}

	private class ZoomMouseWheelListener implements MouseWheelListener {
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (e.isControlDown()) {
				if (e.getWheelRotation() < 0)  {
					handleZoomIn();
				} else{
					handleZoomOut();
				}
			}
		}
	}

	/**
	 * Driver.
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final JFrame mainFrame = new JFrame("Resource Editor");
				final JTextField txtField = new JTextField();
				JButton btn = new JButton("Open Dialog");
				String resourceName = "appconfig.properties";

				txtField.setText(resourceName);

				mainFrame.setVisible(true);
				mainFrame.setSize(300, 180);
				mainFrame.setLayout(new BorderLayout());
				mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				mainFrame.add(btn, BorderLayout.SOUTH);
				mainFrame.add(txtField, BorderLayout.NORTH);

				btn.setPreferredSize(new Dimension(0, 50));
				btn.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						String resourceName = txtField.getText().trim();
						String[] reqProps = new String[] {};
						EditFileDialog dialog = new EditFileDialog(mainFrame, 900, 500, resourceName, reqProps);

						dialog.launch();
					}
				});
			}
		});
	}
}