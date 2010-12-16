
package openstim.model;

import javax.swing.ProgressMonitor;

public interface Session {
	public SessionStream getStream(int sampleRate, int sampleSize, int channels, ProgressMonitor monitor);
}



