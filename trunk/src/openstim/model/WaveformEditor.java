
package openstim.model;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import openstim.GUI;

public class WaveformEditor extends DefaultCellEditor {
	public WaveformEditor() {
		super(new JTextField());
		clickCountToStart = 2;
	}

	public Component getTableCellEditorComponent(JTable table, final Object value, boolean isSelected, int row, int column) {
		final JButton editorComponent = new JButton();
		editorComponent.setBackground(Color.WHITE);
		editorComponent.setBorderPainted(false);
		editorComponent.setContentAreaFilled(false);

		final PopupDialog dialog = new PopupDialog((Waveform)value, column > 0);
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
			dialog.value = (Waveform)value;
		}
	}

	private class PopupDialog extends JDialog {
		final JComboBox[] shapeCombobox = new JComboBox[Waveform.NUM_SHAPES];
		final JSlider[] weightSlider = new JSlider[Waveform.NUM_SHAPES];
		final JSlider[] speedSlider = new JSlider[Waveform.NUM_SHAPES];
		final JSlider[] phaseSlider = new JSlider[Waveform.NUM_SHAPES];
		final JTextField spec = new JTextField();
		final WaveformRenderer preview = new WaveformRenderer(true);

		public Waveform value;
		public boolean canceled;

		public PopupDialog(Waveform wf, boolean canClear) {
			super((Frame)null, "Modify waveform", true);
			setIconImage(GUI.APP_ICON);
			value = (wf != null ? new Waveform(wf) : new Waveform());
			spec.setText(value.toString());
			canceled = true;

			GridBagLayout layout = new GridBagLayout();
			getContentPane().setLayout(layout);
			Insets insets = new Insets(2, 4, 2, 4);

			for (int i = 0; i < Waveform.NUM_SHAPES; i++) {
				shapeCombobox[i] = createShapeCombobox(layout, i);
				weightSlider[i] = createWeightSlider(layout, i);
				speedSlider[i] = createSpeedSlider(layout, i);
				phaseSlider[i] = createPhaseSlider(layout, i);
				if (i < Waveform.NUM_SHAPES - 1) createVerticalSeparator(layout, i);

			}

			JSeparator separator = new JSeparator();
			layout.setConstraints(separator, new GridBagConstraints(0, 4, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			getContentPane().add(separator);

			preview.setValue(value);
			layout.setConstraints(preview, new GridBagConstraints(0, 5, GridBagConstraints.REMAINDER, 1, 1, 5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			getContentPane().add(preview);

			separator = new JSeparator();
			layout.setConstraints(separator, new GridBagConstraints(0, 6, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			getContentPane().add(separator);

			spec.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					parseSpec();
				}
			});
			spec.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					if (!e.isTemporary()) {
						parseSpec();
					}
				}
			});
			layout.setConstraints(spec, new GridBagConstraints(0, 7, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			getContentPane().add(spec);

			JPanel panel = new JPanel();
			for (final Waveform.Shape shape : Waveform.Shape.values()) {
				if (shape.equals(Waveform.Shape.NONE)) continue;
				JButton pure = new JButton("Pure");
				pure.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						for (int i = 0; i < Waveform.NUM_SHAPES; i++) {
							shapeCombobox[i].setSelectedItem(i == 0 ? shape : Waveform.Shape.NONE);
							weightSlider[i].setValue(i == 0 ? 100 : 50);
							speedSlider[i].setValue(0);
							phaseSlider[i].setValue(0);
						}
					}
				});
				panel.add(pure);
			}

			if (canClear) {
				JButton clear = new JButton("Clear");
				clear.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						value = null;
						canceled = false;
						setVisible(false);
					}
				});
				panel.add(clear);
			}

			JButton okay = new JButton("Okay");
			okay.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					canceled = false;
					setVisible(false);
				}
			});
			panel.add(okay);

			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					canceled = true;
					setVisible(false);
				}
			});
			panel.add(cancel);

			layout.setConstraints(panel, new GridBagConstraints(0, 8, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			getContentPane().add(panel);

			pack();
			setMinimumSize(new Dimension(600, 400));
			setResizable(false);
		}

		private JComboBox createShapeCombobox(final GridBagLayout layout, final int index) {
			final Insets insets = new Insets(2, 4, 2, 4);
			final JComboBox combo = new JComboBox(Waveform.Shape.values());
			final Waveform.Shape shape = value.getShape(index);
			combo.setSelectedItem(shape == null ? Waveform.Shape.NONE : shape);
			combo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					value.setShape(index, (Waveform.Shape)combo.getSelectedItem());
					spec.setText(value.toString());
					preview.setValue(value);
				}
			});
			layout.setConstraints(combo, new GridBagConstraints(index*4, 0, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			getContentPane().add(combo);
			return combo;
		}

		private JSlider createWeightSlider(final GridBagLayout layout, final int index) {
			final Insets insets = new Insets(2, 4, 2, 4);
			final Insets insets0 = new Insets(0, 0, 0, 0);
			final int min = -100;
			final int max = 100;
			final int val = Math.max(min, Math.min(max, (int)(100.0f * value.getWeight(index))));
			final JLabel label = new JLabel("Weight");
			final JLabel vlabel = new JLabel(String.format("%d", val));
			final JSlider slider = new JSlider(JSlider.VERTICAL, min, max, val);
			slider.setMajorTickSpacing(50);
			slider.setMinorTickSpacing(10);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setFont(new Font(null, Font.PLAIN, 8));
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					vlabel.setText(String.format("%d", slider.getValue()));
					value.setWeight(index, (float)slider.getValue() / 100.0f);
					spec.setText(value.toString());
					preview.setValue(value);
				}
			});
			layout.setConstraints(label,  new GridBagConstraints(index*4+0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets0, 0, 0));
			layout.setConstraints(vlabel, new GridBagConstraints(index*4+0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets0, 0, 0));
			layout.setConstraints(slider, new GridBagConstraints(index*4+0, 3, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, insets, 0, 0));
			getContentPane().add(label);
			getContentPane().add(vlabel);
			getContentPane().add(slider);
			return slider;
		}

		private JSlider createSpeedSlider(final GridBagLayout layout, final int index) {
			final Insets insets = new Insets(2, 4, 2, 4);
			final Insets insets0 = new Insets(0, 0, 0, 0);
			final int min = -100;
			final int max = 100;
			final int val = Math.max(min, Math.min(max, (int)(100.0f * value.getSpeed(index))));
			final JLabel label = new JLabel("Speed");
			final JLabel vlabel = new JLabel(String.format("%+d%%", val));
			final JSlider slider = new JSlider(JSlider.VERTICAL, min, max, val);
			slider.setMajorTickSpacing(50);
			slider.setMinorTickSpacing(10);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setFont(new Font(null, Font.PLAIN, 8));
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					vlabel.setText(String.format("%+d%%", slider.getValue()));
					value.setSpeed(index, (float)slider.getValue() / 100.0f);
					spec.setText(value.toString());
					preview.setValue(value);
				}
			});
			layout.setConstraints(label,  new GridBagConstraints(index*4+1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets0, 0, 0));
			layout.setConstraints(vlabel, new GridBagConstraints(index*4+1, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets0, 0, 0));
			layout.setConstraints(slider, new GridBagConstraints(index*4+1, 3, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, insets, 0, 0));
			getContentPane().add(label);
			getContentPane().add(vlabel);
			getContentPane().add(slider);
			return slider;
		}

		private JSlider createPhaseSlider(final GridBagLayout layout, final int index) {
			final Insets insets = new Insets(2, 4, 2, 4);
			final Insets insets0 = new Insets(0, 0, 0, 0);
			final int min = -100;
			final int max = 100;
			final int val = Math.max(min, Math.min(max, (int)(100.0f * value.getPhase(index))));
			final JLabel label = new JLabel("Phase");
			final JLabel vlabel = new JLabel(String.format("%+d%%", val));
			final JSlider slider = new JSlider(JSlider.VERTICAL, min, max, val);
			slider.setMajorTickSpacing(50);
			slider.setMinorTickSpacing(10);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setFont(new Font(null, Font.PLAIN, 8));
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					vlabel.setText(String.format("%+d%%", slider.getValue()));
					value.setPhase(index, (float)slider.getValue() / 100.0f);
					spec.setText(value.toString());
					preview.setValue(value);
				}
			});
			layout.setConstraints(label,  new GridBagConstraints(index*4+2, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets0, 0, 0));
			layout.setConstraints(vlabel, new GridBagConstraints(index*4+2, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets0, 0, 0));
			layout.setConstraints(slider, new GridBagConstraints(index*4+2, 3, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, insets, 0, 0));
			getContentPane().add(label);
			getContentPane().add(vlabel);
			getContentPane().add(slider);
			return slider;
		}

		private void createVerticalSeparator(final GridBagLayout layout, final int index) {
			final Insets insets = new Insets(2, 4, 2, 4);
			final JSeparator separator = new JSeparator(JSeparator.VERTICAL);
			layout.setConstraints(separator, new GridBagConstraints(
				index*4+3, 0, 1, 4, 0, 1,
				GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
				insets, 0, 0
			));
			getContentPane().add(separator);
		}

		private void parseSpec() {
			try {
				Waveform new_value = new Waveform();
				new_value.assign(spec.getText());
				value = new_value;
				for (int i = 0; i < Waveform.NUM_SHAPES; i++) {
					shapeCombobox[i].setSelectedItem(value.getShape(i));
					weightSlider[i].setValue((int)(value.getWeight(i) * 100.0f));
					speedSlider[i].setValue((int)(value.getSpeed(i) * 100.0f));
					phaseSlider[i].setValue((int)(value.getPhase(i) * 100.0f));
				}
			} catch (Exception exn) {
				JOptionPane.showMessageDialog(
					null,
					exn.getMessage(),
					"Error parsing spec",
					JOptionPane.ERROR_MESSAGE
				);
			}
		}
	}
}


