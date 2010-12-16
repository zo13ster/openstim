
package openstim.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class IntervalRenderer extends Component implements TableCellRenderer {
	private static final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
	private boolean explicit;
	private JTable table;
	private Interval value;
	private boolean isSelected;
	private boolean hasFocus;

	public IntervalRenderer(boolean explicit) {
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

		if (value == null) return;
		g.translate(2, 2);
		g.setFont(font);

		final int w = size.width - 4;
		final int h = size.height - 4;
		final Color red = (explicit ? Color.RED : new Color(0xffaaaa));
		final Color blue = (explicit ? Color.BLUE : new Color(0xaaaaff));
		final FontMetrics fm = g.getFontMetrics();
		final float s = w / (value.t_attack + value.t_on + value.t_release + value.t_off);
		final int x1 = (int)(s * (value.t_attack));
		final int x2 = (int)(s * (value.t_attack + value.t_on));
		final int x3 = (int)(s * (value.t_attack + value.t_on + value.t_release));
		final int y0 = h - fm.getHeight();
		final int y1 = 0;
		final byte[] on = "always on".getBytes();
		final byte[] off = "always off".getBytes();
		final byte[] s1 = String.format("%.2fs", value.t_attack + value.t_on).getBytes();
		final byte[] s2 = String.format("%.2fs", value.t_release + value.t_off).getBytes();

		g.setColor(Color.LIGHT_GRAY);
		g.drawLine(0, y0, w, y0);

		if (value.t_attack < Float.MIN_VALUE && value.t_on < Float.MIN_VALUE) {
			g.setColor(blue);
			g.drawLine(0, y0, w, y0);
			g.drawBytes(off, 0, off.length, (w - fm.bytesWidth(off, 0, off.length)) / 2, y1 + (y0 + fm.getAscent()) / 2);
		} else if (value.t_release < Float.MIN_VALUE && value.t_off < Float.MIN_VALUE) {
			g.setColor(red);
			g.drawLine(0, y1, w, y1);
			g.drawBytes(on, 0, on.length, (w - fm.bytesWidth(on, 0, on.length)) / 2, y1 + (y0 + fm.getAscent()) / 2);
		} else {
			g.setColor(red);
			g.drawLine(0, y0, x1, y1);
			g.drawLine(x1, y1, x2, y1);
			g.setColor(blue);
			g.drawLine(x2, y1, x3, y0);
			g.drawLine(x3, y0, w, y0);
		}

		g.setColor(red);
		g.drawBytes(s1, 0, s1.length, 0, h);
		g.setColor(blue);
		g.drawBytes(s2, 0, s2.length, w - fm.bytesWidth(s2, 0, s2.length), h);
	}

	public void setValue(Interval value) {
		this.value = value;
		repaint();
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		this.table = table;
		this.value = (Interval)value;
		this.isSelected = isSelected;
		this.hasFocus = hasFocus;
		return this;
	}
}



