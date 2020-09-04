package sng.viewer.panels.seqTable;
/***
 * Used for the Library Included/Exclude for at least N number of reads
 * Called from QueryTab
 */
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import java.awt.ItemSelectable;

public class UIqueryIncExLib extends JPanel implements ItemSelectable {
	private static final long serialVersionUID = 7617905498520098841L;
	private ItemListener listener;
	private JLabel leadLabel = null, midLabel = null, endLabel = null;
	private JTextField numField = null;
	private JComboBox <String> optionsCtrl = null;
	private double dNumVal = -1;
	private JCheckBox chkSelect = null;
	
	// [] label [default] label [dropdown] label
	public UIqueryIncExLib(String leadText, double numVal, String midText, String [] options1, String endText1, 
			int defaultOpt, Dimension fieldDim, ItemListener l)
	{
		dNumVal = numVal;
		listener = l;
		
		chkSelect = new JCheckBox("");
		chkSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (listener != null) {
					listener.itemStateChanged(new ItemEvent(UIqueryIncExLib.this, e.getID(), null, 0));
				}
			}
		});
		
		if(leadText != null)	leadLabel = new JLabel(leadText);
		if(midText != null)	midLabel = new JLabel(midText);
		if(endText1 != null)	endLabel = new JLabel(endText1);
		
		numField = new JTextField(Double.toString(dNumVal));
		numField.setPreferredSize ( fieldDim );
		numField.setMaximumSize( fieldDim );
		numField.setMinimumSize( fieldDim );
		numField.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
		    	if (listener != null) {
		    		try {
		    			dNumVal = Double.parseDouble(numField.getText());	
		    		}
		    		catch (NumberFormatException nfe) {
		    			dNumVal = 0;
		    		}
		    		listener.itemStateChanged( new ItemEvent(UIqueryIncExLib.this, e.getMark(), null, ItemEvent.ITEM_STATE_CHANGED));
		    	}
			}
		});
		
	
		optionsCtrl = new JComboBox <String> (options1);
		optionsCtrl.setSelectedIndex(defaultOpt);
		optionsCtrl.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (listener != null) {
					listener.itemStateChanged(new ItemEvent(UIqueryIncExLib.this, e.getID(), null, e.getStateChange()));
				}
			}
		});
		
		
		setLayout(new BoxLayout ( this, BoxLayout.X_AXIS ));
		super.setBackground(Color.WHITE);
		if(chkSelect != null) 	add(chkSelect);
		if(leadLabel != null) 	add(leadLabel);
		if(numField != null) 	add(numField);
		if(midLabel != null) 	add(midLabel);
		if(optionsCtrl != null) add(optionsCtrl);
		if(endLabel != null) 	add(endLabel);
		
	}
	
	public boolean isSelected() { 
		if(chkSelect != null)
			return chkSelect.isSelected();
		return false;
	}
	
	public void setSelected(boolean selected) {
		if(chkSelect != null)
			chkSelect.setSelected(selected);
	}
	
	public double getNum() { return dNumVal; }
	public void setNum(double val) { 
		dNumVal = val;
		numField.setText(dNumVal + "");
	}
	public int getOption() { return optionsCtrl.getSelectedIndex(); }
	public String getOptionText() { return (String)optionsCtrl.getSelectedItem(); }
	public void setOption(int val) { optionsCtrl.setSelectedIndex(val); }
	
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
