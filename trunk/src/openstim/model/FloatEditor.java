
package openstim.model;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;

public class FloatEditor extends DefaultCellEditor {
	public FloatEditor() {
		super(new JTextField());
		clickCountToStart = 2;
	}

	@Override
	public Object getCellEditorValue() {
		Object v = super.getCellEditorValue();
		return Float.parseFloat(v.toString());
	}

	@Override
	public boolean stopCellEditing() {
		try {
			JTextField tf = (JTextField)editorComponent;
			Float.parseFloat(tf.getText());
			return super.stopCellEditing();
		} catch (Exception e) {
			return false;
		}
	}


}



