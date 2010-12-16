
package openstim.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import openstim.GUI;

public class IntervalEditor extends DefaultCellEditor {
	public IntervalEditor() {
		super(new JTextField());
		clickCountToStart = 2;
	}

	public Component getTableCellEditorComponent(JTable table, final Object value, boolean isSelected, int row, int column) {
		final JButton editorComponent = new JButton();
		editorComponent.setBackground(Color.WHITE);
		editorComponent.setBorderPainted(false);
		editorComponent.setContentAreaFilled(false);

		final PopupDialog dialog = new PopupDialog((Interval)value, column > 0);
		delegate = new Delegate(dialog);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dialog.setLocationRelativeTo(editorComponent);
				dialog.setVisible(true);

				if (dialog.canceled) {
					fireEditingCanceled();
				} else {
					fireEditingStopped();
				}
			}
		});

		return editorComponent;
	}

	private class Delegate extends EditorDelegate {
		private PopupDialog dialog;

		public Delegate(PopupDialog dlg) {
			super();
			dialog = dlg;
		}

		public Object getCellEditorValue() {
			return dialog.value;
		}

		public void setValue(Object value) {
			dialog.value = (Interval)value;
		}
	}

	private class PopupDialog extends JDialog {
		final JScrollBar[] sliders = new JScrollBar[Interval.params.length];
		final JSpinner[] spinners = new JSpinner[Interval.params.length];
		final IntervalRenderer preview = new IntervalRenderer(true);

		public Interval value;
		public boolean canceled;

		public PopupDialog(Interval iv, boolean canClear) {
			super((Frame)null, "Modify interval", true);
			setIconImage(GUI.APP_ICON);
			value = (iv != null ? new Interval(iv) : new Interval());
			canceled = true;

			GridBagLayout layout = new GridBagLayout();
			getContentPane().setLayout(layout);
			Insets insets = new Insets(2, 4, 2, 4);

			for (int i = 0; i < Interval.params.length; i++) {
				JLabel label = new JLabel(Interval.params[i]);
				layout.setConstraints(label, new GridBagConstraints(
					0, i, 1, 1, 0, 0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					insets, 0, 0
				));
				getContentPane().add(label);
			}

			for (int i = 0; i < Interval.params.length; i++) {
				sliders[i] = createSlider(i);
				layout.setConstraints(sliders[i], new GridBagConstraints(
					1, i, 2, 1, 1, 0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					insets, 0, 0
				));
				getContentPane().add(sliders[i]);
			}

			for (int i = 0; i < Interval.params.length; i++) {
				spinners[i] = createSpinner(i);
				layout.setConstraints(spinners[i], new GridBagConstraints(
					3, i, 1, 1, 0, 0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					insets, 0, 0
				));
				getContentPane().add(spinners[i]);
			}

			preview.setValue(value);
			layout.setConstraints(preview, new GridBagConstraints(
				0, Interval.params.length, GridBagConstraints.REMAINDER, 1, 1, 1,
				GridBagConstraints.WEST, GridBagConstraints.BOTH,
				insets, 0, 0
			));
			getContentPane().add(preview);

			JSeparator separator = new JSeparator();
			layout.setConstraints(separator, new GridBagConstraints(
				0, Interval.params.length+1, GridBagConstraints.REMAINDER, 1, 1, 0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				insets, 0, 0
			));
			getContentPane().add(separator);

			if (canClear) {
				JButton clear = new JButton("Clear");
				clear.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						value = null;
						canceled = false;
						setVisible(false);
					}
				});
				layout.setConstraints(clear, new GridBagConstraints(
					0, Interval.params.length+2, 1, 1, 0, 0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					insets, 0, 0
				));
				getContentPane().add(clear);
			}

			JButton okay = new JButton("Okay");
			okay.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					canceled = false;
					setVisible(false);
				}
			});
			layout.setConstraints(okay, new GridBagConstraints(
				2, Interval.params.length+2, 1, 1, 0, 0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				insets, 0, 0
			));
			getContentPane().add(okay);

			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					canceled = true;
					setVisible(false);
				}
			});
			layout.setConstraints(cancel, new GridBagConstraints(
				3, Interval.params.length+2, 1, 1, 0, 0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				insets, 0, 0
			));
			getContentPane().add(cancel);

			pack();
			setMinimumSize(new Dimension(400, 350));
			setResizable(false);
		}

		private JScrollBar createSlider(final int index) {
			final int v = (int)(10.0f * value.getParam(index));
			final JScrollBar slider = new JScrollBar(JScrollBar.HORIZONTAL, v, 100, 0, 700);
			slider.addAdjustmentListener(new AdjustmentListener() {
				public void adjustmentValueChanged(AdjustmentEvent e) {
					spinners[index].setValue(0.1f * slider.getValue());
					updatePreview();
				}
			});
			return slider;
		}

		private JSpinner createSpinner(final int index) {
			final double v = value.getParam(index);
			final JSpinner spinner = new JSpinner(new SpinnerNumberModel(v, 0.0f, 60.0f, 0.01f));
			spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.00"));
			spinner.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					Number val = (Number)spinner.getValue();
					sliders[index].setValue((int)(10.0f * val.floatValue()));
					updatePreview();
				}
			});
			return spinner;
		}

		private void updatePreview() {
			for (int i = 0; i < Interval.params.length; i++) {
				Number val = (Number)spinners[i].getValue();
				value.setParam(i, val.floatValue());
			}
			value.normalize();
			preview.setValue(value);
		}
	}
}


