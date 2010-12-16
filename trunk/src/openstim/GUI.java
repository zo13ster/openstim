
package openstim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import openstim.dialogs.PlaybackSettings;
import openstim.model.*;
import openstim.table.*;
import openstim.tools.*;

public class GUI extends JFrame implements ActionListener {
	public static final Image APP_ICON = new ImageIcon(GUI.class.getResource("images/appicon.png")).getImage();

	private JTable table;
	private PlaybackManager playbackManager;
	private ScheduledSession session;
	private Settings settings;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					new GUI();
				}
			}
		);
	}

	public GUI() {
		super(String.format("OpenStim %s", Version.version));

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				doExit();
			}
		});

		settings = new Settings();
		if (!settings.load()) {
			PlaybackSettings dlg = new PlaybackSettings(settings);
			dlg.setVisible(true);
			if (dlg.isCanceled()) System.exit(1);
		}

		playbackManager = new PlaybackManager(settings);
		while (!playbackManager.initialize()) {
			PlaybackSettings dlg = new PlaybackSettings(settings);
			dlg.setVisible(true);
			if (dlg.isCanceled()) System.exit(1);
		}

		JPanel content = new JPanel(new BorderLayout());
		content.setOpaque(true);

		/*try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(this);
		} catch (Exception e) {
			System.err.println("Internal Look And Feel Setting Error.");
			System.err.println(e);
		}*/

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setRollover(true);
		toolbar.add(createToolbarButton("quit", "system-log-out", "Leave program"));
		toolbar.addSeparator();
		toolbar.add(createToolbarButton("prefs", "preferences-system", "Edit personal preferences"));
		toolbar.addSeparator();
		toolbar.add(createToolbarButton("new", "document-new", "Create a new session"));
		toolbar.add(createToolbarButton("open", "document-open", "Load a session from disk"));
		toolbar.add(createToolbarButton("save", "document-save", "Save session to disk"));
		toolbar.add(createToolbarButton("export", "document-export", "Export session to an audio file"));
		toolbar.addSeparator();
		toolbar.add(createToolbarButton("insertLeft", "object-rotate-left", "Insert new track before current track"));
		toolbar.add(createToolbarButton("insertRight", "object-rotate-right", "Insert new track after current track"));
		toolbar.add(createToolbarButton("delete", "edit-delete", "Remove current track"));
		toolbar.addSeparator();
		toolbar.add(createToolbarButton("play", "media-playback-start", "Start sound generation"));
		toolbar.add(createToolbarButton("pause", "media-playback-pause", "Pause sound generation"));
		toolbar.addSeparator();
		toolbar.add(createToolbarButton("first", "go-first", "Jump to first slot"));
		toolbar.add(createToolbarButton("previous", "go-previous", "Jump to previous slot"));
		toolbar.add(createToolbarButton("next", "go-next", "Jump to next slot"));
		content.add(toolbar, BorderLayout.NORTH);

		session = new ScheduledSession();
		table = new JTable(session) {
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				Dimension size = super.getPreferredScrollableViewportSize();
				return new Dimension(Math.min(getPreferredSize().width, size.width), size.height);
			}

			@Override
			public boolean getScrollableTracksViewportHeight() {
				if (getParent() instanceof JViewport) {
					JViewport parent = (JViewport)getParent();
					return (parent.getHeight() > getPreferredSize().height);
				} else {
					return false;
				}
			}

			@Override
			public TableCellRenderer getCellRenderer(int row, int column) {
				TableModelExt m = (TableModelExt)getModel();
				TableCellRenderer tcr = m.getRendererAt(row, column);
				if (tcr != null) return tcr;
				return super.getCellRenderer(row, column);
			}

			@Override
			public TableCellEditor getCellEditor(int row, int column) {
				TableModelExt m = (TableModelExt)getModel();
				TableCellEditor tce = m.getEditorAt(row, column);
				if (tce != null) return tce;
				return super.getCellEditor(row, column);
			}

			@Override
			public void tableChanged(TableModelEvent event) {
				super.tableChanged(event);
				if (event == null || event.getFirstRow() == TableModelEvent.HEADER_ROW || event.getType() == TableModelEvent.INSERT) {
					TableModelExt m = (TableModelExt)getModel();
					for (int i = 0; i < getRowCount(); i++) {
						setRowHeight(i, m.getRowHeight(i));
					}
				}
			}

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
				Component c = super.prepareRenderer(renderer, row, col);
				if (c instanceof JComponent) {
					TableModelExt m = (TableModelExt)getModel();
					JComponent jc = (JComponent)c;
					jc.setToolTipText(m.getToolTipAt(row, col));
				}
				return c;
			}

			@Override
			protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					final int[] rows = getSelectedRows();
					final int[] cols = getSelectedColumns();
					for (int r = 0; r < rows.length; r++) {
						for (int c = 0; c < cols.length; c++) {
							setValueAt(null, rows[r], cols[c]);
						}
					}
					return false;
				} else {
					return super.processKeyBinding(ks, e, condition, pressed);
				}
			}
		};

		table.setCellSelectionEnabled(true);
		table.setColumnSelectionAllowed(true);
		table.setRowSelectionAllowed(false);
		table.setSurrendersFocusOnKeystroke(true);
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		table.tableChanged(null);

		JScrollPane pane = new JScrollPane(table);
		pane.setRowHeader(new TableRowHeader(table, pane));
		pane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new TableCorner());
		pane.getViewport().setBackground(table.getBackground());
		content.add(pane, BorderLayout.CENTER);

		setContentPane(content);
		pack();

		setIconImage(APP_ICON);
		setVisible(true);
		setMinimumSize(new Dimension(800, 600));
		setSize(new Dimension(1024, 768));
		//setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
	}

	private JComponent createToolbarButton(String action, String icon, String text) {
		ImageIcon img = new ImageIcon(getClass().getResource(String.format("images/%s.png", icon)));
		JButton btn = new JButton(img);
		btn.setActionCommand(action);
		btn.setToolTipText(text);
		btn.setRolloverEnabled(true);
		btn.addActionListener(this);
		return btn;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("quit")) {
			doExit();
		} else if (e.getActionCommand().equals("prefs")) {
			while (true) {
				PlaybackSettings dlg = new PlaybackSettings(settings);
				dlg.setVisible(true);
				if (dlg.isCanceled()) break;
				if (playbackManager.initialize()) break;
			}
		} else if (e.getActionCommand().equals("new")) {
			doTest();
		} else if (e.getActionCommand().equals("open")) {
			doOpen();
		} else if (e.getActionCommand().equals("save")) {
			doSave();
		} else if (e.getActionCommand().equals("export")) {
			doExport();
		} else if (e.getActionCommand().equals("insertLeft")) {
			session.insertTrack(table.getSelectedColumn() - 1);
		} else if (e.getActionCommand().equals("insertRight")) {
			session.insertTrack(table.getSelectedColumn());
		} else if (e.getActionCommand().equals("delete")) {
			session.deleteTrack(table.getSelectedColumn());
		} else if (e.getActionCommand().equals("play")) {
			playbackManager.play();
		} else if (e.getActionCommand().equals("pause")) {
			playbackManager.stop();
		}
	}

	private void doExit() {
		int result = JOptionPane.showConfirmDialog(
			this,
			"Do you really want to exit OpenStim?",
			"Close OpenStim",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE
		);

		if (result == JOptionPane.YES_OPTION) {
			playbackManager.stop();
			System.exit(0);
		}
	}

	private void doOpen() {
		JFileChooserExt dlg = new JFileChooserExt();
		dlg.setDialogTitle("Load session from disk");
		dlg.setFileHidingEnabled(true);
		dlg.setMultiSelectionEnabled(false);
		dlg.setAcceptAllFileFilterUsed(true);
		dlg.setFileFilter(new FileExtensionFilter("stim", "OpenStim session files"));
		int result = dlg.showOpenDialog(this);
		if (result != JFileChooser.APPROVE_OPTION) return;

		try {
			session.load(dlg.getSelectedFile());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
				this,
				e.getMessage(),
				"Error loading session from disk",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}

	private void doSave() {
		JFileChooserExt dlg = new JFileChooserExt();
		dlg.setDialogTitle("Save session to disk");
		dlg.setFileHidingEnabled(true);
		dlg.setMultiSelectionEnabled(false);
		dlg.setAcceptAllFileFilterUsed(true);
		dlg.setFileFilter(new FileExtensionFilter("stim", "OpenStim session files"));
		int result = dlg.showSaveDialog(this);
		if (result != JFileChooser.APPROVE_OPTION) return;

		try {
			session.store(dlg.getSelectedFile());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
				this,
				e.getMessage(),
				"Error saving session to disk",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}

	private void doExport() {
		JFileChooserExt dlg = new JFileChooserExt();
		dlg.setDialogTitle("Export session to audio file");
		dlg.setFileHidingEnabled(true);
		dlg.setMultiSelectionEnabled(false);
		dlg.setAcceptAllFileFilterUsed(true);
		dlg.setFileFilter(new FileExtensionFilter("wav", "PCM wave file"));
		int result = dlg.showSaveDialog(this);
		if (result != JFileChooser.APPROVE_OPTION) return;

		// Important:
		// Export uses regular playback infrastructure which is not reentrant.
		// There lock GUI to ensure that we get no problems here.
		setEnabled(false);

		final ProgressMonitor monitor = new ProgressMonitor(null, "Exporting session to audio file", "", 0, 100);
		final AudioInputStream audioStream = session.getStream(44100, 16, 2, monitor);
		final File file = dlg.getSelectedFile();

		final Thread thread = new Thread() {
			public void run() {
				try {
					AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, file);
					if (monitor.isCanceled()) {
						file.delete();
						throw new IOException("Canceled by user.");
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(
						GUI.this,
						e.getMessage(),
						"Error exporting to audio file",
						JOptionPane.ERROR_MESSAGE
					);
				} finally {
					// unlock GUI in all cases
					monitor.close();
					setEnabled(true);
				}
			}
		};

		thread.start();
	}

	private void doTest() {
		final byte[] buf = new byte[4096];
		SessionStream ais = session.getStream(44100, 16, 2, null);
		long start = System.nanoTime();
		while (true) {
			try { if (ais.read(buf) <= 0) break; }
			catch (Exception e) { e.printStackTrace(); break; }
		}
		double time = (double)(System.nanoTime() - start) / 1e9;
		System.out.println(String.format("%.2f sec (%.2f %%)", time, 100.0f * time / ais.totalTime()));
		ais = session.getStream(22050, 16, 2, null);
		start = System.nanoTime();
		while (true) {
			try { if (ais.read(buf) <= 0) break; }
			catch (Exception e) { e.printStackTrace(); break; }
		}
		time = (double)(System.nanoTime() - start) / 1e9;
		System.out.println(String.format("%.2f sec (%.2f %%)", time, 100.0f * time / ais.totalTime()));
	}



}


