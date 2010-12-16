
package openstim.table;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public interface TableModelExt extends TableModel {
	public int getRowHeight(int row);
	public int[] getRowLabelWidths();
	public String[] getRowLabels(int row);
	public TableCellRenderer getRendererAt(int row, int col);
	public TableCellEditor getEditorAt(int row, int col);
	public String getFormattedValueAt(int row, int col);
	public String getToolTipAt(int row, int col);
}



