
package openstim.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class WaveformRenderer extends Component implements TableCellRenderer {
	private boolean explicit;
	private JTable table;
	private Waveform value;
	private boolean isSelected;
	private boolean hasFocus;

	public WaveformRenderer(boolean explicit) {
		super();
		this.explicit = explicit;
	}

	public void paint(Graphics g) {
		final Dimension size = getSize();

		if (table == null) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, size.width, size.height);
		} else {
			if (isSelected) {
				g.setColor(table.getSelectionBackground());
				g.fillRect(0, 0, size.width, size.height);
			}
			if (hasFocus) {
				g.setColor(table.getGridColor());
				g.drawRect(0, 0, size.width-1, size.height-1);
			}
		}

		final int w = size.width - 4;
		final int h = size.height - 4;
		final int freq = (table == null ? 3 : 1);
		final int[] xx = new int[w];
		final int[] yy = new int[w];

		g.translate(2, 2);
		if (table == null) {
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(0, h/2, w, h/2);
			g.drawLine(w/3, 0, w/3, h);
			g.drawLine(2*w/3, 0, 2*w/3, h);
		}

		if (value != null) {
			float[] wave = value.render();
			for (int i = 0; i < w; ++i) {
				xx[i] = i;
				yy[i] = (int)((0.5f - wave[(i*freq*wave.length/w)%wave.length] * 0.5f) * h);
			}

			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(explicit ? Color.BLACK : isSelected ? Color.YELLOW : Color.ORANGE);
			g2.drawPolyline(xx, yy, w);
		}
	}

	public void setValue(Waveform value) {
		this.value = value;
		repaint();
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		this.table = table;
		this.value = (Waveform)value;
		this.isSelected = isSelected;
		this.hasFocus = hasFocus;
		return this;
	}
}



