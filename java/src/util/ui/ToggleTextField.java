package util.ui;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import java.awt.ItemSelectable;

import util.methods.Static;

public class ToggleTextField extends JPanel implements ItemSelectable {
	private static final long serialVersionUID = 1L;
	private JCheckBox checkBox;
	private JTextField textField;
	private JLabel trailingLabel;
	ItemListener listener;
	
	public ToggleTextField(String leadingText, String fieldText, String trailingText, 
			int width, ItemListener l) 
	{
		listener = l;
		
		checkBox = Static.createCheckBox(leadingText);
		checkBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (listener != null)
					listener.itemStateChanged(
		    			new ItemEvent(ToggleTextField.this, e.getID(), null, e.getStateChange()));
			}
		});
		checkBox.addActionListener(new ActionListener() { 
		      public void actionPerformed(ActionEvent ae) {
		    	  		textField.setEnabled(checkBox.isSelected());
		      }
		 });
		textField = Static.createTextField(fieldText, width, false);
		textField.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
		    	if (listener != null) 
		    		listener.itemStateChanged(
		    			new ItemEvent(ToggleTextField.this, e.getMark(), null, ItemEvent.ITEM_STATE_CHANGED));
			}
		});
		
		trailingLabel = Static.createLabel(trailingText);
		
		setLayout(new BoxLayout ( this, BoxLayout.X_AXIS ));
		setBackground(Color.WHITE);
		add(checkBox);
		add(textField);
		add(trailingLabel);
	}
	
	public void setEnabled(boolean enabled) {
		checkBox.setEnabled(enabled);
		textField.setEnabled(enabled);
		trailingLabel.setEnabled(enabled);
	}
	
	public void addItemListener(ItemListener l) {
		listener = l;
	}
	
	public Object[] getSelectedObjects() {
		return null;
	}
	
	public void removeItemListener(ItemListener l) {
		listener = null;
	}
	
	public String getText() {
		return textField.getText();
	}
	
	public void setText(String s) {
		textField.setText(s);
	}
	
	public boolean isSelected() {
		return checkBox.isSelected();
	}
	
	public void setSelected(boolean b) {
		checkBox.setSelected(b);
	}
	
	public String toString() {
		return checkBox.getText() + textField.getText() + trailingLabel.getText();
	}
}
