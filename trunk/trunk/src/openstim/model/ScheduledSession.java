
package openstim.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ProgressMonitor;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import openstim.table.TableModelExt;
import org.w3c.dom.*;

public class ScheduledSession extends AbstractTableModel implements Session,TableModelExt {
	private static final TableCellRenderer defaultRendererExpl = new DefaultRenderer(true);
	private static final TableCellRenderer defaultRendererImpl = new DefaultRenderer(false);
	private static final TableCellRenderer intervalRendererExpl = new IntervalRenderer(true);
	private static final TableCellRenderer intervalRendererImpl = new IntervalRenderer(false);
	private static final TableCellRenderer waveformRendererExpl = new WaveformRenderer(true);
	private static final TableCellRenderer waveformRendererImpl = new WaveformRenderer(false);
	private static final TableCellEditor floatEditor = new FloatEditor();
	private static final TableCellEditor intervalEditor = new IntervalEditor();
	private static final TableCellEditor waveformEditor = new WaveformEditor();

	private static final int FIXED_ROWS = 2;
	private static final int KEYS_PER_CHANNEL = 13;
	private static final int MAX_CHANNELS = 2;
	private static final int INIT_TRACKS = 8;

	private int numChannels;
	private int numTracks;
	private float[] duration;
	private float[] timeIndices;
	private String[] description;
	private Values[] values;

	public ScheduledSession() {
		numChannels = 2;
		numTracks = 2;
		duration = new float[INIT_TRACKS];
		description = new String[INIT_TRACKS];
		values = new Values[INIT_TRACKS * MAX_CHANNELS];

		for (int i = 0; i < numTracks; i++) {
			duration[i] = 60.0f;
		}

		for (int i = 0; i < numTracks * MAX_CHANNELS; i++) {
			values[i] = new Values(i < MAX_CHANNELS);
		}

		updateTimeIndices();
		updateInterpolatedValues();
	}

	public void insertTrack(int index) {
		if (index >= 0 && index < numTracks) {
			if (duration.length == numTracks) {
				float[] temp = new float[duration.length * 2];
				System.arraycopy(duration, 0, temp, 0, duration.length);
				duration = temp;
			}

			if (description.length == numTracks) {
				String[] temp = new String[description.length * 2];
				System.arraycopy(description, 0, temp, 0, description.length);
				description = temp;
			}

			if (values.length == numTracks * MAX_CHANNELS) {
				Values[] temp = new Values[values.length * 2];
				System.arraycopy(values, 0, temp, 0, values.length);
				values = temp;
			}

			System.arraycopy(duration, index+1, duration, index+2, numTracks-index-1);
			duration[index+1] = 60.0f;

			System.arraycopy(description, index+1, description, index+2, numTracks-index-1);
			description[index+1] = null;

			final int idx = (index+1) * MAX_CHANNELS;
			System.arraycopy(values, idx, values, idx+MAX_CHANNELS, (numTracks-index-1)*MAX_CHANNELS);
			for (int i = 0; i < MAX_CHANNELS; i++) values[idx+i] = new Values(false);

			numTracks++;
			updateTimeIndices();
			updateInterpolatedValues();
			fireTableStructureChanged();
		}
	}

	public void deleteTrack(int index) {
		if (index > 0 && index < numTracks && numTracks > 2) {
			System.arraycopy(duration, index+1, duration, index, numTracks-index-1);
			System.arraycopy(description, index+1, description, index, numTracks-index-1);
			System.arraycopy(values, (index+1)*MAX_CHANNELS, values, index*MAX_CHANNELS, (numTracks-index-1)*MAX_CHANNELS);

			numTracks--;
			updateTimeIndices();
			updateInterpolatedValues();
			fireTableStructureChanged();
		}
	}

	public void load(File file) throws IOException {
		try {
			DocumentBuilderFactory bFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = bFactory.newDocumentBuilder();

			FileInputStream istream = new FileInputStream(file);
			Document doc = builder.parse(istream);
			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();
			System.out.println(root.getNodeName() + " " + root.getAttribute("version"));
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Internal error.", e);
		}
	}

	public void store(File file) throws IOException {
		try {
			DocumentBuilderFactory bFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = bFactory.newDocumentBuilder();

			Document doc = builder.newDocument();
			doc.setXmlStandalone(true);

			Element root = doc.createElement("openstim-session");
			root.setAttribute("version", "1");

			Element authorXML = doc.createElement("author");
			authorXML.appendChild(doc.createTextNode("willibald"));
			root.appendChild(authorXML);

			Element licenseXML = doc.createElement("license");
			licenseXML.appendChild(doc.createTextNode("creative commons"));
			root.appendChild(licenseXML);

			Element descriptionXML = doc.createElement("description");
			descriptionXML.appendChild(doc.createTextNode("Lorem ipsum dolor sit amet, consectetuer sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\nDuis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat.\nUt wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi."));
			root.appendChild(descriptionXML);

			Element channelsXML = doc.createElement("channels");
			for (int channel = 0; channel < numChannels; channel++) {
				Element channelXML = doc.createElement("channel");
				channelXML.setAttribute("description", channel == 0 ? "left" : "right"); // TODO
				channelsXML.appendChild(channelXML);
			}
			root.appendChild(channelsXML);

			Element tracksXML = doc.createElement("tracks");
			for (int track = 0; track < numTracks; track++) {
				Element trackXML = doc.createElement("track");
				if (track == 0) {
					trackXML.setAttribute("initial", "true");
				} else {
					trackXML.setAttribute("duration", Float.toString(duration[track]));
					if (description[track] != null && !description[track].isEmpty()) {
						trackXML.setAttribute("description", description[track]);
					}
				}

				for (int channel = 0; channel < numChannels; channel++) {
					Element channelXML = doc.createElement("channel");
					Values v = values[track*MAX_CHANNELS+channel];
					if (v.intervalExplicit) {
						channelXML.appendChild(v.interval.toXML(doc));
					}
					if (v.baseWaveExplicit || v.baseFreqExplicit || v.baseAmplExplicit) {
						Element baseXML = doc.createElement("base-wave");
						if (v.baseWaveExplicit) baseXML.appendChild(v.baseWave.toXML(doc));
						if (v.baseFreqExplicit) baseXML.appendChild(float2XML(doc, "frequency", v.baseFreq));
						if (v.baseAmplExplicit) baseXML.appendChild(float2XML(doc, "amplitude", v.baseAmpl));
						channelXML.appendChild(baseXML);
					}
					if (v.fmodWaveExplicit || v.fmodFreqExplicit || v.fmodAmplExplicit) {
						Element fmodXML = doc.createElement("frequency-mod");
						if (v.fmodWaveExplicit) fmodXML.appendChild(v.fmodWave.toXML(doc));
						if (v.fmodFreqExplicit) fmodXML.appendChild(float2XML(doc, "frequency", v.fmodFreq));
						if (v.fmodAmplExplicit) fmodXML.appendChild(float2XML(doc, "amplitude", v.fmodAmpl));
						channelXML.appendChild(fmodXML);
					}
					if (v.amod1WaveExplicit || v.amod1FreqExplicit || v.amod1AmplExplicit) {
						Element amod1XML = doc.createElement("amplitude-mod1");
						if (v.amod1WaveExplicit) amod1XML.appendChild(v.amod1Wave.toXML(doc));
						if (v.amod1FreqExplicit) amod1XML.appendChild(float2XML(doc, "frequency", v.amod1Freq));
						if (v.amod1AmplExplicit) amod1XML.appendChild(float2XML(doc, "amplitude", v.amod1Ampl));
						channelXML.appendChild(amod1XML);
					}
					if (v.amod2WaveExplicit || v.amod2FreqExplicit || v.amod2AmplExplicit) {
						Element amod2XML = doc.createElement("amplitude-mod2");
						if (v.amod2WaveExplicit) amod2XML.appendChild(v.amod2Wave.toXML(doc));
						if (v.amod2FreqExplicit) amod2XML.appendChild(float2XML(doc, "frequency", v.amod2Freq));
						if (v.amod2AmplExplicit) amod2XML.appendChild(float2XML(doc, "amplitude", v.amod2Ampl));
						channelXML.appendChild(amod2XML);
					}
					trackXML.appendChild(channelXML);
				}

				tracksXML.appendChild(trackXML);
			}

			root.appendChild(tracksXML);
			doc.appendChild(root);

			TransformerFactory tFactory = TransformerFactory.newInstance();
			tFactory.setAttribute("indent-number", 4);

			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(doc);
			FileOutputStream ostream = new FileOutputStream(file);
			StreamResult destination = new StreamResult(new OutputStreamWriter(ostream, "UTF-8"));
			transformer.transform(source, destination);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Internal error.", e);
		}
	}

	private Element float2XML(Document doc, String name, float value) {
		Element result = doc.createElement(name);
		result.setAttribute("value", Float.toString(value));
		return result;
	}

	private boolean isExplicit(int channel, int track, int key) {
		if (track == 0) return true;
		return values[track*MAX_CHANNELS+channel].isExplicit(key);
	}

	private void setExplicit(int channel, int track, int key, boolean value) {
		if (track == 0) value = true;
		values[track*MAX_CHANNELS+channel].setExplicit(key, value);
	}

	private Object getValue(int channel, int track, int key) {
		return values[track*MAX_CHANNELS+channel].getValue(key);
	}

	private void setValue(int channel, int track, int key, Object value) {
		values[track*MAX_CHANNELS+channel].setValue(key, value);
	}

	public int getColumnCount() {
		return numTracks;
	}

	public String getColumnName(int col) {
		if (col == 0) return "0:00.0";
		int tenth = Math.round(10.0f * timeIndices[col]);
		return String.format("- %d:%02d.%d", tenth/600, (tenth/10) % 60, tenth % 10);
	}

	public int getRowCount() {
		return FIXED_ROWS + numChannels * KEYS_PER_CHANNEL;
	}

	public int getRowHeight(int row) {
		if (row < FIXED_ROWS) return 16;
		final int key = (row - FIXED_ROWS) % KEYS_PER_CHANNEL;
		if (key == 0 || key % 3 == 1) return 32;
		return 16;
	}

	public int[] getRowLabelWidths() {
		return new int[] { 50, 50, 100 };
	}

	public String[] getRowLabels(int row) {
		if (row == 0) return new String[] { "Transition Time" };
		if (row == 1) return new String[] { "Description" };
		final int channel = (row - FIXED_ROWS) / KEYS_PER_CHANNEL;
		final int key = (row - FIXED_ROWS) % KEYS_PER_CHANNEL;
		switch (key) {
			case 0  : return new String[] { getChannelName(channel), "On/Off Interval" };
			case 1  : return new String[] { null, "Base\nWave", "Waveform" };
			case 2  : return new String[] { null, null, "Frequency" };
			case 3  : return new String[] { null, null, "Volume" };
			case 4  : return new String[] { null, "Freq.\nMod", "Waveform" };
			case 5  : return new String[] { null, null, "Frequency" };
			case 6  : return new String[] { null, null, "Depth" };
			case 7  : return new String[] { null, "Ampl.\nMod 1", "Waveform" };
			case 8  : return new String[] { null, null, "Frequency" };
			case 9  : return new String[] { null, null, "Depth" };
			case 10 : return new String[] { null, "Ampl.\nMod 2", "Waveform" };
			case 11 : return new String[] { null, null, "Frequency" };
			case 12 : return new String[] { null, null, "Depth" };
			default : return null;
		}
	}

	private String getChannelName(int channel) {
		switch (channel) {
			case 0  : return "#1\nLeft";
			case 1  : return "#2\nRight";
			default : return String.format("#%i", channel);
		}
	}

	public TableCellRenderer getRendererAt(int row, int col) {
		if (row < FIXED_ROWS) return defaultRendererExpl;
		final int channel = (row - FIXED_ROWS) / KEYS_PER_CHANNEL;
		final int key = (row - FIXED_ROWS) % KEYS_PER_CHANNEL;
		final boolean explicit = isExplicit(channel, col, key);
		if (key == 0) return (explicit ? intervalRendererExpl : intervalRendererImpl);
		if (key % 3 == 1) return (explicit ? waveformRendererExpl : waveformRendererImpl);
		return (explicit ? defaultRendererExpl : defaultRendererImpl);
	}

	public TableCellEditor getEditorAt(int row, int col) {
		if (row == 0) return floatEditor;
		if (row < FIXED_ROWS) return null;
		final int key = (row - FIXED_ROWS) % KEYS_PER_CHANNEL;
		if (key == 0) return intervalEditor;
		if (key % 3 == 1) return waveformEditor;
		return floatEditor;
	}

	public String getFormattedValueAt(int row, int col) {
		if (row == 0 && col > 0) return String.format("%.1f sec", duration[col]);
		if (row < FIXED_ROWS) return null;
		final int channel = (row - FIXED_ROWS) / KEYS_PER_CHANNEL;
		final int key = (row - FIXED_ROWS) % KEYS_PER_CHANNEL;
		final Values v = values[col*MAX_CHANNELS+channel];
		switch (key) {
			case 2: return String.format("%.1f Hz", v.baseFreq);
			case 3: return String.format("%.1f %%", v.baseAmpl);
			case 5: return String.format("%.2f Hz", v.fmodFreq);
			case 6: return String.format("%.1f Hz", v.fmodAmpl);
			case 8: return String.format("%.2f Hz", v.amod1Freq);
			case 9: return String.format("%.1f %%", v.amod1Ampl);
			case 11: return String.format("%.2f Hz", v.amod2Freq);
			case 12: return String.format("%.1f %%", v.amod2Ampl);
			default: return null;
		}
	}

	public String getToolTipAt(int row, int col) {
		if (row != 1) return null;
		return description[col];
	}

	public Object getValueAt(int row, int col) {
		if (row < FIXED_ROWS && col == 0) return "<html><center>&mdash;</center></html>";
		if (row == 0) return duration[col];
		if (row == 1) return description[col];

		final int channel = (row - FIXED_ROWS) / KEYS_PER_CHANNEL;
		final int key = (row - FIXED_ROWS) % KEYS_PER_CHANNEL;
		return getValue(channel, col, key);
	}

	public boolean isCellEditable(int row, int col) {
		return (row >= FIXED_ROWS || col != 0);
	}

	/*private void fireTableHeaderUpdated() {
		for (int col = 0; col < getColumnCount(); col++) {
			int viewCol = table.convertColumnIndexToView(col);
			TableColumn column = table.getColumnModel().getColumn(viewCol);
			column.setHeaderValue(getColumnName(col));
		}
		table.getTableHeader().repaint();
	}*/

	public void setValueAt(Object value, int row, int col) {
		if (col == 0 && value == null) {
			// the first column is always explicit,
			// deleting is not allowed there
			return;
		}

		if (row == 0) {
			// duration, deleting is not allowed here,
			// column headers have to be updated
			if (value == null) return;
			float v = ((Float)value).floatValue();
			duration[col] = Math.max(0.0f, v);
			updateTimeIndices();
			updateInterpolatedValues();
			fireTableCellUpdated(row, col);
			//fireTableHeaderUpdated();
			fireTableStructureChanged();
			return;
		}

		if (row == 1) {
			String s = (String)value;
			if (s == null || s.isEmpty()) s = null;
			if (s != null && s.length() > 80) s = s.substring(0, 80);
			description[col] = s;
			fireTableCellUpdated(row, col);
			return;
		}

		final int channel = (row - FIXED_ROWS) / KEYS_PER_CHANNEL;
		final int key = (row - FIXED_ROWS) % KEYS_PER_CHANNEL;
		setExplicit(channel, col, key, value != null);
		if (value != null) setValue(channel, col, key, value);
		fireTableCellUpdated(row, col);
		updateInterpolatedValues();
	}

	/**
	* Update the array with the time indices.
	* This array is used for the column headers and the interpolation.
	*/

	private void updateTimeIndices() {
		if (timeIndices == null || timeIndices.length != duration.length) {
			timeIndices = new float[duration.length];
			timeIndices[0] = 0.0f;
		}
		for (int i = 1; i < numTracks; i++) {
			timeIndices[i] = timeIndices[i-1] + duration[i];
		}
	}

	/**
	* Update all non-explicit values by interpolating them their
	* respective explit left and right neighbours.
	*/

	private void updateInterpolatedValues() {
		final int[] next = new int[numTracks + 1];

		for (int channel = 0, row = 0; channel < numChannels; channel++) {
			for (int key = 0; key < KEYS_PER_CHANNEL; key++, row++) {
				// for every track determine the next explicit track on the right

				next[numTracks] = -1;
				for (int track = numTracks-1; track >= 0; track--) {
					next[track] = isExplicit(channel, track, key) ? track : next[track+1];
				}

				// now interpolate all non-explicit tracks, we continously update
				// last seen explicit track which is used for the left side of the
				// interpolation, the very first track is always explicit by convention

				for (int track = 1, last = 0; track < numTracks; track++) {
					if (isExplicit(channel, track, key)) {
						last = track;
						continue;
					}

					if (next[track] < 0) {
						setValue(channel, track, key, getValue(channel, last, key));
						setExplicit(channel, track, key, false);
						continue;
					}

					final float s1 = (timeIndices[next[track]] - timeIndices[track]) / (timeIndices[next[track]] - timeIndices[last]);
					final float s2 = (timeIndices[track] - timeIndices[last]) / (timeIndices[next[track]] - timeIndices[last]);
					final Object value = getValue(channel, track, key);

					if (value instanceof Float) {
						float a = ((Float)getValue(channel, last, key)).floatValue();
						float b = ((Float)getValue(channel, next[track], key)).floatValue();
						setValue(channel, track, key, s1 * a + s2 * b);
					} else if (value instanceof Interval) {
						Interval a = (Interval)getValue(channel, last, key);
						Interval b = (Interval)getValue(channel, next[track], key);
						setValue(channel, track, key, new Interval(s1, a, s2, b));
					} else if (value instanceof Waveform) {
						Waveform a = (Waveform)getValue(channel, last, key);
						Waveform b = (Waveform)getValue(channel, next[track], key);
						setValue(channel, track, key, new Waveform(s1, a, s2, b));
					}

					fireTableCellUpdated(row, track);
				}
			}
		}
	}

	public SessionStream getStream(int sampleRate, int sampleSize, int channels, ProgressMonitor monitor) {
		return new Stream(sampleRate, sampleSize, channels, monitor);
	}

	private class Stream extends SessionStream {
		private ProgressMonitor monitor;
		private float stepSize;
		private int outChannels;
		private boolean bit16;
		private int trackIndex = 1;
		private float trackOffset = 0.0f;
		private boolean finished = false;
		private final Interval[] interval = new Interval[MAX_CHANNELS];
		private final Waveform[] baseWave = new Waveform[MAX_CHANNELS];
		private final Waveform[] fmodWave = new Waveform[MAX_CHANNELS];
		private final Waveform[] amod1Wave = new Waveform[MAX_CHANNELS];
		private final Waveform[] amod2Wave = new Waveform[MAX_CHANNELS];

		public Stream(int sampleRate, int sampleSize, int channels, ProgressMonitor monitor) {
			super(sampleRate, sampleSize, channels);
			rewind();
			stepSize = 1.0f / (float)sampleRate;
			outChannels = channels;
			bit16 = (sampleSize == 16);
			this.monitor = monitor;
		}

		public void rewind() {
			trackIndex = 1;
			trackOffset = 0.0f;
			finished = false;
			for (int i = 0; i < MAX_CHANNELS; i++) {
				interval[i] = new Interval();
				baseWave[i] = new Waveform();
				fmodWave[i] = new Waveform();
				amod1Wave[i] = new Waveform();
				amod2Wave[i] = new Waveform();
			}
		}

		public void seek(float t) {
			rewind();
			while (trackIndex <= numTracks && timeIndices[trackIndex - 1] <= t) {
				trackIndex++;
			}
			if (trackIndex > numTracks) {
				finished = true;
			} else {
				finished = false;
				trackOffset = t - timeIndices[trackIndex - 1];
			}
		}

		public float currentTime() {
			return (finished ? totalTime() : timeIndices[trackIndex - 1] + trackOffset);
		}

		public float totalTime() {
			return timeIndices[numTracks-1];
		}

		public boolean isFinished() {
			return finished;
		}

		public int read(byte[] buffer, int offset, int length) {
			if (finished) {
				// we already reached the end of session
				return -1;
			}

			if (monitor != null) {
				float current = currentTime();
				float total = totalTime();
				monitor.setProgress((int)Math.round(current / total * 100.0f));
				monitor.setNote(String.format(
					"Completed %d:%02d of %d:%02d...",
					((int)current) / 60, ((int)current) % 60,
					((int)total) / 60, ((int)total) % 60
				));
				if (monitor.isCanceled()) {
					return -1;
				}
			}

			for (int frame = 0; frame < length/frameSize; frame++) {
				for (int channel = 0; channel < outChannels; channel++) {
					if (channel > numChannels) {
						buffer[offset++] = 0;
						if (bit16) buffer[offset++] = 0;
						continue;
					}

					float s2 = trackOffset / duration[trackIndex];
					float s1 = 1.0f - s2;

					int idx = trackIndex * MAX_CHANNELS + channel;
					Values v1 = values[idx - MAX_CHANNELS];
					Values v2 = values[idx];

					float freq = s1 * v1.baseFreq + s2 * v2.baseFreq;
					float ampl = s1 * v1.baseAmpl + s2 * v2.baseAmpl;

					float modFreq = s1 * v1.fmodFreq + s2 * v2.fmodFreq;
					float modAmpl = s1 * v1.fmodAmpl + s2 * v2.fmodAmpl;
					fmodWave[channel].interpolate(s1, v1.fmodWave, s2, v2.fmodWave);
					//freq += modAmpl * fmodWave[channel].nextValue(modFreq, stepSize);

					modFreq = s1 * v1.amod1Freq + s2 * v2.amod1Freq;
					modAmpl = s1 * v1.amod1Ampl + s2 * v2.amod1Ampl;
					amod1Wave[channel].interpolate(s1, v1.amod1Wave, s2, v2.amod2Wave);
					//ampl += modAmpl * amod1Wave[channel].nextValue(modFreq, stepSize);

					modFreq = s1 * v1.amod2Freq + s2 * v2.amod2Freq;
					modAmpl = s1 * v1.amod2Ampl + s2 * v2.amod2Ampl;
					amod2Wave[channel].interpolate(s1, v1.amod2Wave, s2, v2.amod2Wave);
					//ampl += modAmpl * amod2Wave[channel].nextValue(modFreq, stepSize);

					interval[channel].interpolate(s1, v1.interval, s2, v2.interval);
					//ampl *= interval[channel].nextValue(stepSize);

					ampl = (float)Math.max(-1.0, Math.min(1.0, 0.01 * ampl));
					baseWave[channel].interpolate(s1, v1.baseWave, s2, v2.baseWave);
					float value = 0.0f;//ampl * baseWave[channel].nextValue(freq, stepSize);

					if (bit16) {
						int iv = Math.max(-32768, Math.min(32767, (int)Math.round(value * 32768.0f)));
						buffer[offset++] = (byte)iv;
						buffer[offset++] = (byte)(iv >> 8);
					} else {
						buffer[offset++] = (byte)Math.max(-128, Math.min(127, (int)Math.round(value * 128.0f)));
					}
				}

				trackOffset += stepSize;

				if (trackOffset > duration[trackIndex]) {
					trackOffset -= duration[trackIndex];
					trackIndex++;
					if (trackIndex >= numTracks) {
						finished = true;
						return (frame+1) * frameSize;
					}
				}
			}

			return length - (length % frameSize);
		}

	}



	/*static public void main(String[] args) throws Exception {
		DeterminedSession s = new DeterminedSession();
		System.out.println(s.totalTime());
		s.rewind();
		s.store(new FileOutputStream("foo.xml"));
		s.load(new FileInputStream("foo.xml"));
		System.exit(0);

		AudioFormat afmt = new AudioFormat(44100, 16, 2, true, false);
		SourceDataLine sdl = AudioSystem.getSourceDataLine(afmt);
		sdl.open(afmt);
		sdl.start();
		System.out.println(afmt);

		final float[] buf = new float[44100*2];
		final byte[] outbuf = new byte[44100*2*2];
		while (!s.isFinished()) {
			int n = s.render(buf, 0, 44100);
			for (int i = 0; i < n; i++) {
				int left = Math.max(-32768, Math.min(32767, (int)Math.round(buf[i*2+0] * 32768.0f)));
				int right = Math.max(-32768, Math.min(32767, (int)Math.round(buf[i*2+1] * 32768.0f)));
				outbuf[i*4+0] = (byte)left;
				outbuf[i*4+1] = (byte)(left >> 8);
				outbuf[i*4+2] = (byte)right;
				outbuf[i*4+3] = (byte)(right >> 8);
			}
			System.out.println(s.currentTime());
			sdl.write(outbuf, 0, n*4);
		}

		sdl.drain();
		sdl.stop();
		sdl.close();
	}*/

	private static class Values {
		public Interval interval;
		public boolean intervalExplicit;

		public Waveform baseWave;
		public float baseFreq;
		public float baseAmpl;
		public boolean baseWaveExplicit;
		public boolean baseFreqExplicit;
		public boolean baseAmplExplicit;

		public Waveform fmodWave;
		public float fmodFreq;
		public float fmodAmpl;
		public boolean fmodWaveExplicit;
		public boolean fmodFreqExplicit;
		public boolean fmodAmplExplicit;

		public Waveform amod1Wave;
		public float amod1Freq;
		public float amod1Ampl;
		public boolean amod1WaveExplicit;
		public boolean amod1FreqExplicit;
		public boolean amod1AmplExplicit;

		public Waveform amod2Wave;
		public float amod2Freq;
		public float amod2Ampl;
		public boolean amod2WaveExplicit;
		public boolean amod2FreqExplicit;
		public boolean amod2AmplExplicit;

		public Values(boolean explicit) {
			interval = new Interval();
			intervalExplicit = explicit;

			baseWave = new Waveform();
			baseFreq = 500.0f;
			baseAmpl = 50.0f;
			baseWaveExplicit = explicit;
			baseFreqExplicit = explicit;
			baseAmplExplicit = explicit;

			fmodWave = new Waveform();
			fmodFreq = 1.0f;
			fmodAmpl = 0.0f;
			fmodWaveExplicit = explicit;
			fmodFreqExplicit = explicit;
			fmodAmplExplicit = explicit;

			amod1Wave = new Waveform();
			amod1Freq = 1.0f;
			amod1Ampl = 0.0f;
			amod1WaveExplicit = explicit;
			amod1FreqExplicit = explicit;
			amod1AmplExplicit = explicit;

			amod2Wave = new Waveform();
			amod2Freq = 1.0f;
			amod2Ampl = 0.0f;
			amod2WaveExplicit = explicit;
			amod2FreqExplicit = explicit;
			amod2AmplExplicit = explicit;
		}

		public boolean isExplicit(int i) {
			switch (i) {
				case 0  : return intervalExplicit;
				case 1  : return baseWaveExplicit;
				case 2  : return baseFreqExplicit;
				case 3  : return baseAmplExplicit;
				case 4  : return fmodWaveExplicit;
				case 5  : return fmodFreqExplicit;
				case 6  : return fmodAmplExplicit;
				case 7  : return amod1WaveExplicit;
				case 8  : return amod1FreqExplicit;
				case 9  : return amod1AmplExplicit;
				case 10 : return amod2WaveExplicit;
				case 11 : return amod2FreqExplicit;
				case 12 : return amod2AmplExplicit;
				default : return true;
			}
		}

		public void setExplicit(int i, boolean v) {
			switch (i) {
				case 0  : intervalExplicit = v; break;
				case 1  : baseWaveExplicit = v; break;
				case 2  : baseFreqExplicit = v; break;
				case 3  : baseAmplExplicit = v; break;
				case 4  : fmodWaveExplicit = v; break;
				case 5  : fmodFreqExplicit = v; break;
				case 6  : fmodAmplExplicit = v; break;
				case 7  : amod1WaveExplicit = v; break;
				case 8  : amod1FreqExplicit = v; break;
				case 9  : amod1AmplExplicit = v; break;
				case 10 : amod2WaveExplicit = v; break;
				case 11 : amod2FreqExplicit = v; break;
				case 12 : amod2AmplExplicit = v; break;
			}
		}

		public Object getValue(int i) {
			switch (i) {
				case 0  : return interval;
				case 1  : return baseWave;
				case 2  : return baseFreq;
				case 3  : return baseAmpl;
				case 4  : return fmodWave;
				case 5  : return fmodFreq;
				case 6  : return fmodAmpl;
				case 7  : return amod1Wave;
				case 8  : return amod1Freq;
				case 9  : return amod1Ampl;
				case 10 : return amod2Wave;
				case 11 : return amod2Freq;
				case 12 : return amod2Ampl;
				default : return null;
			}
		}

		public void setValue(int i, Object v) {
			switch (i) {
				case 0  : interval = (Interval)v; break;
				case 1  : baseWave = (Waveform)v; break;
				case 2  : baseFreq = Math.max(0.0f, Math.min(10000.0f, ((Float)v).floatValue())); break;
				case 3  : baseAmpl = Math.max(0.0f, Math.min(100.0f, ((Float)v).floatValue())); break;
				case 4  : fmodWave = (Waveform)v; break;
				case 5  : fmodFreq = Math.max(0.0f, Math.min(10000.0f, ((Float)v).floatValue())); break;
				case 6  : fmodAmpl = Math.max(-10000.0f, Math.min(10000.0f, ((Float)v).floatValue())); break;
				case 7  : amod1Wave = (Waveform)v; break;
				case 8  : amod1Freq = Math.max(0.0f, Math.min(10000.0f, ((Float)v).floatValue())); break;
				case 9  : amod1Ampl = Math.max(-100.0f, Math.min(100.0f, ((Float)v).floatValue())); break;
				case 10 : amod2Wave = (Waveform)v; break;
				case 11 : amod2Freq = Math.max(0.0f, Math.min(10000.0f, ((Float)v).floatValue())); break;
				case 12 : amod2Ampl = Math.max(-100.0f, Math.min(100.0f, ((Float)v).floatValue())); break;
			}
		}
	}
}



