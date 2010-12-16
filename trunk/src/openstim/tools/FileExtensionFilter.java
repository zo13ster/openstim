
package openstim.tools;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class FileExtensionFilter extends FileFilter {
	private String extension;
	private String description;

	public FileExtensionFilter(String extension, String description) {
		super();
		this.extension = extension;
		this.description = description;
	}

	public boolean accept(File f) {
		return (
			f.isDirectory() ||
			f.isFile() && f.getName().toLowerCase().endsWith("." + extension)
		);
	}

	public File fixExtension(File f) {
		if (f == null || f.getName() == null) return f;
		if (f.getName().toLowerCase().endsWith("." + extension)) return f;
		return new File(f.getParent(), f.getName() + "." + extension);
	}

	public String getDescription() {
		return String.format("%s (*.%s)", description, extension);
	}
}



