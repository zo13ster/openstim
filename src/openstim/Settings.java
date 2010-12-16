
package openstim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Settings {
	public final static int MAX_CHANNELS = 8;
	public final File configDir;
	public final File settingsFile;
	public final Map<Mixer.Info, Set<AudioFormat>> audioHardware;

	public Mixer.Info playbackDevice;
	public AudioFormat playbackFormat;

	public Settings() {
		configDir = new File(System.getProperty("user.home"), ".openstim");
		settingsFile = new File(configDir, "settings.xml");
		audioHardware = scanAudioHardware();
	}

	public boolean load() {
		if (!settingsFile.isFile()) {
			playbackDevice = null;
			playbackFormat = new AudioFormat(44100, 16, 2, true, false);
			return false;
		}

		try {
			DocumentBuilderFactory bFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = bFactory.newDocumentBuilder();

			FileInputStream istream = new FileInputStream(settingsFile);
			Document doc = builder.parse(istream);
			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();
			if (!root.getNodeName().equals("openstim-settings")) {
				throw new Exception("Unexpected root element: " + root.getNodeName());
			}

			int version = Integer.parseInt(root.getAttribute("version"));
			if (version != 1) {
				throw new Exception("Unrecognized version number: " + version);
			}

			boolean approximate = false;
			NodeList nodes = root.getChildNodes();

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.TEXT_NODE) {
					if (node.getTextContent().trim().isEmpty()) continue;
					throw new Exception("Unexpected text node: " + node.getTextContent());
				} else if (node.getNodeName().equals("playback")) {
					NamedNodeMap attributes = node.getAttributes();
					Node attr = attributes.getNamedItem("device");
					playbackDevice = (attr == null ? null : findDevice(attr.getTextContent()));
					if (attr != null && playbackDevice == null) approximate = true;

					attr = attributes.getNamedItem("sampleRate");
					int sampleRate = (attr == null ? 44100 : Integer.parseInt(attr.getTextContent()));
					if (sampleRate < 11000) sampleRate = 44100; else
					if (sampleRate > 48000) sampleRate = 48000;

					attr = attributes.getNamedItem("sampleSize");
					int sampleSize = (attr == null ? 16 : Integer.parseInt(attr.getTextContent()));
					if (sampleSize != 8 && sampleSize != 16) sampleSize = 16;

					attr = attributes.getNamedItem("channels");
					int channels = (attr == null ? 2 : Integer.parseInt(attr.getTextContent()));
					if (channels < 1) channels = 1; else
					if (channels > 8) channels = 8;

					playbackFormat = findFormat(sampleRate, sampleSize, channels);
					if (playbackFormat.getSampleRate() != sampleRate) approximate = true;
					if (playbackFormat.getSampleSizeInBits() != sampleSize) approximate = true;
					if (playbackFormat.getChannels() != channels) approximate = true;
				} else {
					throw new Exception("Unexpected node: " + node.getNodeName());
				}
			}

			if (playbackFormat == null) {
				playbackFormat = new AudioFormat(44100, 16, 2, true, false);
			}

			if (approximate) {
				JOptionPane.showMessageDialog(
					null,
					"OpenStim was not able to restore your previous settings.\n" +
					"This is probably due to changes in your audio hardware and/or driver.\n" +
					"Please review your settings.",
					"Audio system changed",
					JOptionPane.INFORMATION_MESSAGE
				);
				return false;
			}

			return true;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(
				null,
				"Unable to read configuration file:\n" + e.getMessage(),
				"Error reading configuration file",
				JOptionPane.ERROR_MESSAGE
			);
			playbackDevice = null;
			playbackFormat = new AudioFormat(44100, 16, 2, true, false);
			return false;
		}
	}

	public boolean store() {
		if (!configDir.isDirectory()) {
			if (!configDir.mkdir()) {
				JOptionPane.showMessageDialog(
					null,
					"Unable to create directory:\n" + configDir,
					"Error creating configuration directory",
					JOptionPane.ERROR_MESSAGE
				);
				return false;
			}
		}

		try {
			DocumentBuilderFactory bFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = bFactory.newDocumentBuilder();

			Document doc = builder.newDocument();
			doc.setXmlStandalone(true);

			Element root = doc.createElement("openstim-settings");
			root.setAttribute("version", "1");

			Element playbackXML = doc.createElement("playback");
			if (playbackDevice != null) {
				playbackXML.setAttribute("device", playbackDevice.getName());
			}
			playbackXML.setAttribute("sampleSize", Integer.toString(playbackFormat.getSampleSizeInBits()));
			playbackXML.setAttribute("sampleRate", Integer.toString((int)playbackFormat.getSampleRate()));
			playbackXML.setAttribute("channels", Integer.toString(playbackFormat.getChannels()));

			root.appendChild(playbackXML);
			doc.appendChild(root);

			TransformerFactory tFactory = TransformerFactory.newInstance();
			tFactory.setAttribute("indent-number", 4);

			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(doc);
			FileOutputStream ostream = new FileOutputStream(settingsFile);
			StreamResult destination = new StreamResult(new OutputStreamWriter(ostream, "UTF-8"));
			transformer.transform(source, destination);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(
				null,
				"Unable to write configuration file:\n" + e.getMessage(),
				"Error writing configuration file",
				JOptionPane.ERROR_MESSAGE
			);
			return false;
		}
	}

	private Map<Mixer.Info, Set<AudioFormat>> scanAudioHardware() {
		Map<Mixer.Info, Set<AudioFormat>> result = new TreeMap<Mixer.Info, Set<AudioFormat>>(
			new Comparator<Mixer.Info>() {
				public int compare(Mixer.Info o1, Mixer.Info o2) {
					if (o1 == null && o2 == null) return 0;
					if (o1 == null) return -1;
					if (o2 == null) return 1;
					String n1 = o1.getName();
					String n2 = o2.getName();
					if (n1.contains("Java") && !n2.contains("Java")) return -1;
					if (!n1.contains("Java") && n2.contains("Java")) return 1;
					return n1.compareToIgnoreCase(n2);
				}
			}
		);

		Set<AudioFormat> formats = scanAudioFormats(null);
		if (!formats.isEmpty()) result.put(null, formats);

		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
		for (int i = 0; i < mixerInfo.length; i++) {
			formats = scanAudioFormats(mixerInfo[i]);
			if (!formats.isEmpty()) result.put(mixerInfo[i], formats);
		}
		return result;
	}

	private Set<AudioFormat> scanAudioFormats(Mixer.Info mixerInfo) {
		Line.Info[] lineInfo;
		if (mixerInfo == null) {
			lineInfo = AudioSystem.getSourceLineInfo(new Line.Info(SourceDataLine.class));
		} else {
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			lineInfo = mixer.getSourceLineInfo(new Line.Info(SourceDataLine.class));
		}

		Set<AudioFormat> result = new HashSet<AudioFormat>();
		for (int i = 0; i < lineInfo.length; i++) {
			if (!(lineInfo[i] instanceof DataLine.Info)) continue;
			AudioFormat[] fmt = ((DataLine.Info)lineInfo[i]).getFormats();
			for (int j = 0; j < fmt.length; j++) {
				if (fmt[j].getEncoding() != AudioFormat.Encoding.PCM_SIGNED && fmt[j].getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) continue;
				if (fmt[j].getSampleSizeInBits() != 8 && fmt[j].getSampleSizeInBits() != 16) continue;

				boolean found = false;
				Iterator<AudioFormat> it = result.iterator();
				while (it.hasNext()) {
					if (it.next().matches(fmt[j])) {
						found = true;
						break;
					}
				}

				if (!found) {
					result.add(fmt[j]);
				}
			}
		}
		return result;
	}

	private Mixer.Info findDevice(String name) {
		Iterator<Mixer.Info> it = audioHardware.keySet().iterator();
		while (it.hasNext()) {
			Mixer.Info info = it.next();
			if (info == null) continue;
			if (info.getName().compareToIgnoreCase(name) == 0) return info;
		}
		return null;
	}

	private AudioFormat findFormat(int sampleRate, int sampleSize, int channels) {
		AudioFormat bestFormat = null;
		int bestError = Integer.MAX_VALUE;

		Set<AudioFormat> formats = audioHardware.get(playbackDevice);
		Iterator<AudioFormat> it = formats.iterator();
		while (it.hasNext()) {
			int error = 0;
			AudioFormat fmt = it.next();

			if (fmt.getChannels() < channels) error += 8; else
			if (fmt.getChannels() > channels) error += 2;
			if (fmt.getSampleSizeInBits() < sampleSize) error += 4; else
			if (fmt.getSampleSizeInBits() > sampleSize) error += 1;

			if (fmt.getSampleRate() != AudioSystem.NOT_SPECIFIED) {
				if (fmt.getSampleRate() < sampleRate) error += 4; else
				if (fmt.getSampleRate() > sampleRate) error += 1;
			}

			if (bestFormat == null || error < bestError) {
				bestFormat = fmt;
				bestError = error;
			}
		}

		return new AudioFormat(
			bestFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED ? sampleRate : bestFormat.getSampleRate(),
			bestFormat.getSampleSizeInBits(),
			bestFormat.getChannels(),
			bestFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED,
			bestFormat.isBigEndian()
		);
	}
}



