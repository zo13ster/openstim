
package openstim.table;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class TableRowHeader extends JViewport implements AdjustmentListener {
	private JTable table;
	private Cell cell;

	public TableRowHeader(JTable table, JScrollPane parent) {
		this.table = table;
		cell = new Cell();
		setView(new Dummy());
		parent.getVerticalScrollBar().addAdjustmentListener(this);
		parent.getHorizontalScrollBar().addAdjustmentListener(this);

		final TableModelExt m = (TableModelExt)table.getModel();
		final int[] widths = m.getRowLabelWidths();
		int width = 0;
		for (int i = 0; i < widths.length; i++) width += widths[i];
		setMinimumSize(new Dimension(width, 0));
		setPreferredSize(new Dimension(width, 0));
		setMaximumSize(new Dimension(width, 0));
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		repaint();
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		final TableModelExt m = (TableModelExt)table.getModel();
		final int rows = table.getRowCount();
		if (rows == 0) return;

		// find first row to draw
		Rectangle rect = getViewRect();
		int index = 0, y = 0;
		while (index < rows) {
			final int height = table.getRowHeight(index);
			if (y + height >= rect.y) break;
			y += height;
			index++;
		}

		final int maxy = rect.y + rect.height;
		final int[] widths = m.getRowLabelWidths();
		final int width = getWidth();

		while (index < rows && y < maxy) {
			final int height = table.getRowHeight(index);
			final String[] labels = m.getRowLabels(index);
			for (int i = 0, x = 0; i < labels.length && i < widths.length; x+=widths[i], i++) {
				if (labels[i] == null) continue;
				cell.setText(labels[i]);
				int labelHeight = height;
				for (int j = index+1; j < rows; j++) {
					final String[] labels2 = m.getRowLabels(j);
					if (i >= labels2.length || labels2[i] != null) break;
					labelHeight += table.getRowHeight(j);
				}
				SwingUtilities.paintComponent(
					g, cell, this, x, y-rect.y,
					i == labels.length-1 ? width - x : widths[i],
					labelHeight
				);
			}
			y += height;
			index++;
		}

		if (y < maxy) {
			cell.setText(null);
			SwingUtilities.paintComponent(g, cell, this, 0, y-rect.y, width, maxy-y);
		}
	}

	private static class Dummy extends JComponent {
		public void paint(Graphics g) {}
		public void update(Graphics g) {}
	}

	private class Cell extends JLabel {
		public void updateUI() {
			super.updateUI();
			setHorizontalAlignment(CENTER);
			LookAndFeel.installBorder(this, "TableHeader.cellBorder");
			LookAndFeel.installColorsAndFont(this, "TableHeader.background", "TableHeader.foreground", "TableHeader.font");
		}

		public void setText(String s) {
			if (s != null) {
				s = s.replace("\n", "<br>");
				s = "<html><center>" + s + "</center></html>";
			}
			super.setText(s);
		}
	}
}


