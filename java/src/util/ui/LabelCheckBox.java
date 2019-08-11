package util.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class LabelCheckBox extends JPanel implements ItemSelectable {
	private static final long serialVersionUID = -796901669126126986L;
	private JCheckBox checkBox;
	private JTextField textField;
	ItemListener listener;
	
	public LabelCheckBox(String leadingText, boolean isChecked, String trailingText, 
			Dimension fieldDim, ItemListener l) 
	{
		listener = l;
		
		setLayout(new BoxLayout ( this, BoxLayout.X_AXIS ));
		setBackground(Color.WHITE);
		
		checkBox = new JCheckBox(leadingText);
		checkBox.setBackground(Color.WHITE);
		checkBox.setSelected(isChecked);
		checkBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (listener != null)
					listener.itemStateChanged(
		    			new ItemEvent(LabelCheckBox.this, e.getID(), null, e.getStateChange()));
			}
		});
		
		textField = new JTextField(trailingText);
		textField.setPreferredSize ( fieldDim );
		textField.setMaximumSize( fieldDim );
		textField.setMinimumSize( fieldDim );
		textField.setEditable(false);
		textField.setBorder(null);
		textField.setBackground(Color.WHITE);
		
		textField.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				//checkBox.setSelected(true);
		    	if (listener != null) 
		    		listener.itemStateChanged(
		    			new ItemEvent(LabelCheckBox.this, e.getMark(), null, ItemEvent.ITEM_STATE_CHANGED));
			}
		});

		
		setAlignmentX(Component.LEFT_ALIGNMENT);
		add(checkBox);
		add(textField);
	
	}
	
	public String getLabel() { return checkBox.getText(); }
	public void setLabel(String label) { checkBox.setText(label); } 
	public String getText() { return textField.getText(); }
	public void setText(String val) { textField.setText(val); }
	public boolean isSelected() { return checkBox.isSelected(); }
	public void setSelected(boolean val) { checkBox.setSelected(val); }
	
	public void addItemListener(ItemListener l) {
		listener = l;
	}
	
	public Object[] getSelectedObjects() {
		return null;
	}
	
	public void removeItemListener(ItemListener l) {
		listener = null;
	}
}

