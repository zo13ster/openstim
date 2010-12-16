
package openstim.tools;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class JFileChooserExt extends JFileChooser {
	private static File workingDir = new File(System.getProperty("user.dir"));

	public JFileChooserExt() {
		super(workingDir);
	}

	public void approveSelection() {
		File f = getSelectedFile();
		if (f.exists() && getDialogType() == SAVE_DIALOG) {
			int result = JOptionPane.showConfirmDialog(
				getTopLevelAncestor(),
				String.format("The file '%s' already exists.\nDo you want to overwrite it?", f),
				"File already exists",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE
			);

			switch(result)  {
				case JOptionPane.YES_OPTION:
					workingDir = getCurrentDirectory();
					super.approveSelection();
					return;

				case JOptionPane.NO_OPTION:
					return;

				case JOptionPane.CANCEL_OPTION:
					cancelSelection();
					return;
			}
		} else {
			workingDir = getCurrentDirectory();
			super.approveSelection();
		}
	}

	public void setSelectedFile(File file) {
		if (getFileFilter() instanceof FileExtensionFilter) {
			FileExtensionFilter filter = (FileExtensionFilter)getFileFilter();
			file = filter.fixExtension(file);
		}
		super.setSelectedFile(file);
	}
}



