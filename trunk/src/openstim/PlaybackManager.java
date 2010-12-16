
package openstim;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;

public class PlaybackManager implements Runnable {
	private Settings settings;
	private SourceDataLine dataLine = null;
	private Thread thread = null;
	private boolean running = false;

	public PlaybackManager(Settings settings) {
		super();
		this.settings = settings;
	}

	public synchronized boolean initialize() {
		try {
			boolean playing = isPlaying();
			if (playing) stop();

			if (dataLine != null) {
				dataLine.drain();
				dataLine.stop();
				dataLine.close();
			}

			dataLine = AudioSystem.getSourceDataLine(settings.playbackFormat, settings.playbackDevice);
			dataLine.open(settings.playbackFormat);
			dataLine.start();

			if (playing) play();
			return true;
		} catch (LineUnavailableException e) {
			JOptionPane.showMessageDialog(
				null,
				e.getMessage(),
				"Error initializing playback",
				JOptionPane.ERROR_MESSAGE
			);
			dataLine = null;
			return false;
		}
	}

	public synchronized boolean isPlaying() {
		return running;
	}

	public synchronized void play() {
		if (dataLine == null) {
			JOptionPane.showMessageDialog(
				null,
				"Audio system is not available for playback.",
				"Error starting playback",
				JOptionPane.ERROR_MESSAGE
			);
			return;
		}

		if (!running) {
			running = true;
			thread = new Thread(this, getClass().getName());
			thread.setDaemon(true);
			thread.start();
		}
	}

	public synchronized void stop() {
		if (running) {
			try {
				running = false;
				thread.join();
			} catch (InterruptedException e) {
				// ignore
			} finally {
				thread = null;
			}
		}
	}

	public void run() {
		final byte[] buffer = new byte[8192];
		final int samples = buffer.length / settings.playbackFormat.getFrameSize();
		final int channels = settings.playbackFormat.getChannels();
		final boolean sign = (settings.playbackFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED);
		final boolean bit16 = (settings.playbackFormat.getSampleSizeInBits() == 16);
		final boolean bigend = (settings.playbackFormat.isBigEndian());
		System.out.println(bigend);
		final float step = (float)(100.0 * 2.0 * Math.PI / settings.playbackFormat.getSampleRate());
		float volume = 0.0f;
		float pos = 0.0f;

		while (running || volume > Float.MIN_VALUE) {
			for (int i = 0, ofs = 0; i < samples; i++) {
				double value = Math.sin(pos);
				int val = Math.max(-32768, Math.min(32767, (int)(volume * 32768.0f * value)));
				if (!sign) val += 32768;
				for (int j = 0; j < channels; j++) {
					if (bit16 && !bigend) buffer[ofs++] = (byte)val;
					buffer[ofs++] = (byte)(val >> 8);
					if (bit16 && bigend) buffer[ofs++] = (byte)val;
				}
				pos += step;
				if (running) {
					volume = Math.min(1.0f, volume + step);
				} else {
					volume = Math.max(0.0f, volume - step);
				}
			}
			dataLine.write(buffer, 0, buffer.length);
		}
	}
}



