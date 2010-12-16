
package openstim.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.FlowLayout;
import javax.sound.sampled.Line;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import openstim.GUI;
import openstim.Settings;

public class PlaybackSettings extends JDialog implements ActionListener {
	private Settings settings;
	private Mixer.Info currentDevice;
	private AudioFormat currentFormat;
	private boolean canceled = true;

	private JCheckBox defaultCheckBox;
	private JComboBox deviceComboBox;
	private JComboBox sampleRateComboBox;
	private JComboBox sampleSizeComboBox;
	private JComboBox channelsComboBox;

	public PlaybackSettings(Settings settings) {
		super((Frame)null, "OpenStim playback settings", true);
		setIconImage(GUI.APP_ICON);
		this.settings = settings;
		this.currentDevice = settings.playbackDevice;
		this.currentFormat = settings.playbackFormat;

		deviceComboBox = new JComboBox();
		Iterator<Mixer.Info> it = settings.audioHardware.keySet().iterator();
		while (it.hasNext()) {
			Mixer.Info info = it.next();
			if (info == null) continue;
			DeviceItem item = new DeviceItem(info);
			deviceComboBox.addItem(item);
			if (currentDevice == info || currentDevice == null && item.isJavaEngine()) {
				deviceComboBox.setSelectedItem(item);
			}
		}
		deviceComboBox.setActionCommand("deviceChanged");
		deviceComboBox.addActionListener(this);

		defaultCheckBox = new JCheckBox("Use default audio device");
		defaultCheckBox.setActionCommand("deviceChanged");
		defaultCheckBox.addActionListener(this);
		defaultCheckBox.setSelected(currentDevice == null);

		sampleRateComboBox = new JComboBox();
		sampleRateComboBox.setActionCommand("sampleRateChanged");
		sampleRateComboBox.addActionListener(this);

		sampleSizeComboBox = new JComboBox();
		sampleSizeComboBox.setActionCommand("sampleSizeChanged");
		sampleSizeComboBox.addActionListener(this);

		channelsComboBox = new JComboBox();
		channelsComboBox.setActionCommand("channelChanged");
		channelsComboBox.addActionListener(this);

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

		GridBagLayout layout = new GridBagLayout();
		JPanel mainPanel = new JPanel(layout);
		Insets insets = new Insets(2, 4, 2, 4);
		getContentPane().add(mainPanel);

		JComponent[] components = {
			new JLabel("Device:"),
			null,
			new JLabel("Channels:"),
			new JLabel("Sample size:"),
			new JLabel("Sample rate:")
		};

		for (int i = 0; i < components.length; i++) {
			if (components[i] == null) continue;
			layout.setConstraints(components[i], new GridBagConstraints(
				0, i, 1, 1, 0, 0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				insets, 0, 0
			));
			mainPanel.add(components[i]);
		}

		components = new JComponent[] {
			deviceComboBox,
			defaultCheckBox,
			channelsComboBox,
			sampleSizeComboBox,
			sampleRateComboBox
		};

		for (int i = 0; i < components.length; i++) {
			layout.setConstraints(components[i], new GridBagConstraints(
				1, i, 1, 1, 1, 0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				insets, 0, 0
			));
			mainPanel.add(components[i]);
		}

		JSeparator separator = new JSeparator();
		getContentPane().add(separator);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		getContentPane().add(buttonPanel);

		JButton confirm = new JButton("Save");
		confirm.setActionCommand("confirm");
		confirm.addActionListener(this);
		buttonPanel.add(confirm);

		JButton cancel = new JButton("Cancel");
		cancel.setActionCommand("cancel");
		cancel.addActionListener(this);
		buttonPanel.add(cancel);

		pack();
		onDeviceChanged();
		setMinimumSize(new Dimension(400, getPreferredSize().height + 16));
		setResizable(false);
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("deviceChanged")) {
			onDeviceChanged();
		} else if (e.getActionCommand().equals("channelChanged")) {
			onChannelChanged();
		} else if (e.getActionCommand().equals("sampleSizeChanged")) {
			onSampleSizeChanged();
		} else if (e.getActionCommand().equals("sampleRateChanged")) {
			onSampleRateChanged();
		} else if (e.getActionCommand().equals("confirm")) {
			settings.playbackDevice = currentDevice;
			settings.playbackFormat = currentFormat;
			settings.store();
			canceled = false;
			setVisible(false);
		} else if (e.getActionCommand().equals("cancel")) {
			canceled = true;
			setVisible(false);
		}
	}

	private Set<AudioFormat> getAudioFormats() {
		if (defaultCheckBox.isSelected()) {
			return settings.audioHardware.get(null);
		} else {
			DeviceItem device = (DeviceItem)deviceComboBox.getSelectedItem();
			return settings.audioHardware.get(device.value);
		}
	}

	private void onDeviceChanged() {
		if (defaultCheckBox.isSelected()) {
			deviceComboBox.setEnabled(false);
			currentDevice = null;
		} else {
			deviceComboBox.setEnabled(true);
			DeviceItem item = (DeviceItem)deviceComboBox.getSelectedItem();
			currentDevice = (item == null ? null : item.value);
		}

		Set<AudioFormat> formats = getAudioFormats();
		if (formats == null) return;

		boolean[] temp = new boolean[Settings.MAX_CHANNELS];
		Iterator<AudioFormat> it = formats.iterator();
		while (it.hasNext()) {
			AudioFormat fmt = it.next();
			if (fmt.getChannels() > Settings.MAX_CHANNELS) continue;
			temp[fmt.getChannels() - 1] = true;
		}

		channelsComboBox.removeActionListener(this);
		channelsComboBox.removeAllItems();
		boolean selectionDone = false;
		for (int i = 0; i < Settings.MAX_CHANNELS; i++) {
			if (temp[i]) {
				ChannelsItem item = new ChannelsItem(i+1);
				channelsComboBox.addItem(item);
				if (!selectionDone && item.value >= currentFormat.getChannels()) {
					channelsComboBox.setSelectedItem(item);
					selectionDone = true;
				}
			}
		}
		channelsComboBox.addActionListener(this);
		onChannelChanged();
	}

	private void onChannelChanged() {
		Set<AudioFormat> formats = getAudioFormats();
		ChannelsItem channels = (ChannelsItem)channelsComboBox.getSelectedItem();
		if (formats == null || channels == null) return;

		boolean bit8 = false, bit16 = false;
		Iterator<AudioFormat> it = formats.iterator();

		while (it.hasNext()) {
			AudioFormat fmt = it.next();
			bit8 |= (fmt.getSampleSizeInBits() == 8);
			bit16 |= (fmt.getSampleSizeInBits() == 16);
		}

		sampleSizeComboBox.removeActionListener(this);
		sampleSizeComboBox.removeAllItems();
		if (bit8) {
			SampleSizeItem item = new SampleSizeItem(8);
			sampleSizeComboBox.addItem(item);
			if (currentFormat.getSampleSizeInBits() == 8) sampleSizeComboBox.setSelectedItem(item);
		}
		if (bit16) {
			SampleSizeItem item = new SampleSizeItem(16);
			sampleSizeComboBox.addItem(item);
			if (currentFormat.getSampleSizeInBits() == 16) sampleSizeComboBox.setSelectedItem(item);
		}
		sampleSizeComboBox.addActionListener(this);
		onSampleSizeChanged();
	}


	private void onSampleSizeChanged() {
		Set<AudioFormat> formats = getAudioFormats();
		ChannelsItem channels = (ChannelsItem)channelsComboBox.getSelectedItem();
		SampleSizeItem sampleSize = (SampleSizeItem)sampleSizeComboBox.getSelectedItem();
		if (formats == null || sampleSize == null || channels == null) return;

		Set<SampleRateItem> temp = new TreeSet<SampleRateItem>();
		Iterator<AudioFormat> it = formats.iterator();

		while (it.hasNext()) {
			AudioFormat fmt = it.next();
			if (fmt.getChannels() != channels.value) continue;
			if (fmt.getSampleSizeInBits() != sampleSize.value) continue;
			if (fmt.getSampleRate() == AudioSystem.NOT_SPECIFIED) {
				temp.add(new SampleRateItem(11025));
				temp.add(new SampleRateItem(16000));
				temp.add(new SampleRateItem(22050));
				temp.add(new SampleRateItem(32000));
				temp.add(new SampleRateItem(44100));
				temp.add(new SampleRateItem(48000));
			} else if (fmt.getSampleRate() >= 11000) {
				temp.add(new SampleRateItem((int)fmt.getSampleRate()));
			}
		}

		sampleRateComboBox.removeActionListener(this);
		sampleRateComboBox.removeAllItems();
		Iterator<SampleRateItem> it2 = temp.iterator();
		boolean selectionDone = false;
		while (it2.hasNext()) {
			SampleRateItem item = it2.next();
			sampleRateComboBox.addItem(item);
			if (!selectionDone && item.value >= currentFormat.getSampleRate()) {
				sampleRateComboBox.setSelectedItem(item);
				selectionDone = true;
			}
		}
		sampleRateComboBox.addActionListener(this);
		onSampleRateChanged();
	}

	private void onSampleRateChanged() {
		Set<AudioFormat> formats = getAudioFormats();
		ChannelsItem channels = (ChannelsItem)channelsComboBox.getSelectedItem();
		SampleSizeItem sampleSize = (SampleSizeItem)sampleSizeComboBox.getSelectedItem();
		SampleRateItem sampleRate = (SampleRateItem)sampleRateComboBox.getSelectedItem();
		if (formats == null || channels == null || sampleSize == null || sampleRate == null) return;

		Iterator<AudioFormat> it = formats.iterator();

		while (it.hasNext()) {
			AudioFormat fmt = it.next();
			if (fmt.getSampleRate() != AudioSystem.NOT_SPECIFIED && fmt.getSampleRate() != sampleRate.value) continue;
			if (fmt.getSampleSizeInBits() != sampleSize.value) continue;
			if (fmt.getChannels() != channels.value) continue;

			currentFormat = new AudioFormat(
				fmt.getSampleRate() == AudioSystem.NOT_SPECIFIED ? sampleRate.value : fmt.getSampleRate(),
				fmt.getSampleSizeInBits(),
				fmt.getChannels(),
				fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED,
				fmt.isBigEndian()
			);
			break;
		}
	}

	// ----------------------------------------------------------------------

	private static class DeviceItem {
		public Mixer.Info value;
		public DeviceItem(Mixer.Info value) { this.value = value; }
		public boolean isJavaEngine() { return value.getName().contains("Java"); }
		public String toString() { return value.getName(); }
	}

	public static class SampleRateItem implements Comparable<SampleRateItem> {
		public int value;
		public SampleRateItem(int value) { this.value = value; }
		public int compareTo(SampleRateItem other) { return value - other.value; }
		public String toString() { return String.format("%d Hz", value); }
	}

	private static class SampleSizeItem implements Comparable<SampleSizeItem> {
		public int value;
		public SampleSizeItem(int value) { this.value = value; }
		public int compareTo(SampleSizeItem other) { return value - other.value; }
		public String toString() { return String.format("%d bit PCM", value); }
	}

	public static class ChannelsItem implements Comparable<ChannelsItem> {
		public int value;
		public ChannelsItem(int value) { this.value = value; }
		public int compareTo(ChannelsItem other) { return value - other.value; }
		public String toString() {
			switch (value) {
				case 1  : return "Mono";
				case 2  : return "Stereo";
				default : return String.format("%d channels", value);
			}
		}
	}

}


