package util.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class ToggleTextComboField extends JPanel implements ItemSelectable{
	private static final long serialVersionUID = 1L;
	private JCheckBox checkBox;
	private JTextField textField;
	private JComboBox <String> optionList;
	private JLabel trailingLabel;
	ItemListener listener;
	
	public ToggleTextComboField(
			String leadLabel, 
			String fieldText, 
			String trailLabel, 
			String [] options, int defaultOption, 
			Dimension fieldDim, 
			ItemListener l) 
	{
		listener = l;
		
		checkBox = new JCheckBox(leadLabel);
		checkBox.setBackground(Color.WHITE);
		checkBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (listener != null)
					listener.itemStateChanged(
		    			new ItemEvent(ToggleTextComboField.this, e.getID(), null, e.getStateChange()));
			}
		});
		checkBox.addActionListener(new ActionListener() {  
		      public void actionPerformed(ActionEvent ae) {
		    	  		textField.setEnabled(checkBox.isSelected());
		    	  		optionList.setEnabled(checkBox.isSelected());
		      }
		 });
		add(checkBox);
		add(Box.createHorizontalStrut(5));
		
		optionList = new JComboBox <String> (options);
		optionList.setSelectedIndex(defaultOption);
		optionList.setMaximumSize(optionList.getPreferredSize());
		optionList.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(listener != null) {
					listener.itemStateChanged(
							new ItemEvent(ToggleTextComboField.this, e.getID(), null, e.getStateChange()));
				}
			}
		});
		optionList.setEnabled(false);
		add(optionList);
		add(Box.createHorizontalStrut(10));
		
		if (fieldText==null) textField = new JTextField("dummy");
		else {
			textField = new JTextField(fieldText);
			textField.setPreferredSize ( fieldDim );
			textField.setMaximumSize( fieldDim );
			textField.setMinimumSize( fieldDim );
			textField.addCaretListener(new CaretListener() {
				public void caretUpdate(CaretEvent e) {
			    	if (listener != null) 
			    		listener.itemStateChanged(
			    			new ItemEvent(ToggleTextComboField.this, e.getMark(), null, ItemEvent.ITEM_STATE_CHANGED));
				}
			});
			textField.setEnabled(false);
			Dimension optionDim = optionList.getPreferredSize();
			Dimension textDim = textField.getMaximumSize();
			
			textDim.height = Math.max(textDim.height, optionDim.height);
			textField.setMaximumSize(textDim);
			
			add(textField);
		}
		trailingLabel = new JLabel(trailLabel);
		add(trailingLabel);
		
		setLayout(new BoxLayout ( this, BoxLayout.X_AXIS ));
		setBackground(Color.WHITE);
	}
	
	public void setEnabled(boolean enabled) {
		checkBox.setEnabled(enabled);
		optionList.setEnabled(enabled);
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
	
	public int getOptionSelection() {
		return optionList.getSelectedIndex();
	}
	
	public String getOptionText() {
		return (String) optionList.getSelectedItem();
	}
	
	public void setOptionSelection(int item) {
		optionList.setSelectedIndex(item);
	}
	
	public String toString() {
		return checkBox.getText() + optionList.getSelectedItem() + textField.getText() + trailingLabel.getText();
	}

}
