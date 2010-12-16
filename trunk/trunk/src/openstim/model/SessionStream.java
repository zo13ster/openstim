
package openstim.model;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public abstract class SessionStream extends AudioInputStream {
	public SessionStream(int sampleRate, int sampleSize, int channels) {
		super(
			null,
			new AudioFormat(sampleRate, sampleSize, channels, true, false),
			AudioSystem.NOT_SPECIFIED
		);
	}

	@Override
	public int available() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void close() {
		// nothing to do
	}

	@Override
	public void mark(int limit) {
		// mark is not supported
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read() throws IOException {
		throw new IOException("read() is not supported");
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return read(buffer, 0, buffer.length);
	}

	@Override
	public void reset() throws IOException {
		throw new IOException("reset() is not supported");
	}

	@Override
	public long skip(long n) throws IOException {
		throw new IOException("skip(long) is not supported");
	}

	public abstract void rewind();
	public abstract void seek(float t);
	public abstract float currentTime();
	public abstract float totalTime();
	public abstract boolean isFinished();
}

