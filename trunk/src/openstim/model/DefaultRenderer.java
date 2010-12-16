
package openstim.model;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import openstim.table.TableModelExt;

public class DefaultRenderer extends DefaultTableCellRenderer {
	private boolean explicit;

	public DefaultRenderer(boolean explicit) {
		super();
		this.explicit = explicit;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		TableModelExt m = (TableModelExt)table.getModel();
		String s = m.getFormattedValueAt(row, column);
		if (s != null) value = s;
		if (explicit && value != null) value = String.format("<html><b>%s</b></html>", value.toString());
		Component renderer = super.getTableCellRendererComponent(
			table, value, isSelected, hasFocus, row, column
		);
		setForeground(explicit ? Color.BLACK : isSelected ? Color.YELLOW : Color.ORANGE);
		setHorizontalAlignment(SwingConstants.CENTER);
		return renderer;
	}
}



