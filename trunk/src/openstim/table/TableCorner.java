
package openstim.table;

import javax.swing.JLabel;
import javax.swing.LookAndFeel;

public class TableCorner extends JLabel {
	public void updateUI() {
		super.updateUI();
		setHorizontalAlignment(CENTER);
		LookAndFeel.installBorder(this, "TableHeader.cellBorder");
		LookAndFeel.installColorsAndFont(this, "TableHeader.background", "TableHeader.foreground", "TableHeader.font");
	}
}


