/**
 * 
 */
package sng.viewer.panels.seqTable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.ItemSelectable;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;


import java.util.Iterator;
import java.util.Vector;

/**
 * A custom UI widget that gives three options for the selected item:
 *    1) Include when true
 *    2) Exclude when true
 *    3) Ignore (optional)
 */

public class UIqueryIncEx extends JPanel implements ItemSelectable, ItemListener
{
	private static final long serialVersionUID = -8714286499322636798L;

	public final static int INCLUDE = FieldContigData.FILTER_INCLUDE;
	public final static int EXCLUDE = FieldContigData.FILTER_EXCLUDE;
	public final static int IGNORE  = FieldContigData.FILTER_NONE;
	
	private JLabel titleLabel;
	private JRadioButton includeButton;
	private JRadioButton excludeButton;
	private JRadioButton ignoreButton;
	private ButtonGroup group;
	private Vector<ItemListener> listeners;
	
	public UIqueryIncEx ( String titleText, String strIncludeText, 
									String strExcludeText, String strIgnoreText, 
									int nInitialValue, int width )
	{
		titleLabel    = new JLabel(titleText);
		includeButton = new JRadioButton(strIncludeText);
		excludeButton = new JRadioButton(strExcludeText);
		ignoreButton  = new JRadioButton(strIgnoreText);
		
	    group = new ButtonGroup();
	    group.add(includeButton);
	    group.add(excludeButton);
	    group.add(ignoreButton);
		
		setLayout( new BoxLayout ( this, BoxLayout.X_AXIS ) );
		super.setBackground(Color.WHITE);
		add(titleLabel);
		if (width>0 && width > titleLabel.getPreferredSize().width) 
			add(Box.createHorizontalStrut(width-titleLabel.getPreferredSize().width));
		if (strIncludeText != null) { includeButton.setBackground(Color.WHITE);	add(includeButton); }
		if (strExcludeText != null)	{ excludeButton.setBackground(Color.WHITE);	add(excludeButton); }
		if (strIgnoreText  != null)	{ ignoreButton.setBackground(Color.WHITE);	add(ignoreButton); }
		
		listeners = new Vector<ItemListener>();
		includeButton.addItemListener(this);
		excludeButton.addItemListener(this);
		ignoreButton.addItemListener(this);

		setValue ( nInitialValue );
	}
	
	public void setEnabled(boolean enabled) {
		titleLabel.setEnabled(enabled);
		includeButton.setEnabled(enabled);
		excludeButton.setEnabled(enabled);
		ignoreButton.setEnabled(enabled);
	}
	
	public void itemStateChanged(ItemEvent e) 
	{
	    	Iterator<ItemListener> iter = listeners.iterator();
	    	while ( iter.hasNext() ) {
	    		JRadioButton b = (JRadioButton)e.getItem();
	    		iter.next().itemStateChanged(
	    				new ItemEvent(this, e.getID(), b.getText(), e.getStateChange()));
	    	}
	}
	
	public int getValue()
	{
		if (includeButton.isSelected()) return FieldContigData.FILTER_INCLUDE;
		if (excludeButton.isSelected()) return FieldContigData.FILTER_EXCLUDE;
		return FieldContigData.FILTER_NONE;
	}
	
	public void setValue( int nVal )
	{
		switch ( nVal )
		{
		case FieldContigData.FILTER_INCLUDE:
			group.setSelected(includeButton.getModel(), true);
			break;
		case FieldContigData.FILTER_EXCLUDE:
			group.setSelected(excludeButton.getModel(), true);
			break;		
		case FieldContigData.FILTER_NONE:
			group.setSelected(ignoreButton.getModel(), true);
			break;		
		}
	}
	
	public void addItemListener(ItemListener l) {
		listeners.add(l);
	}
	
	public Object[] getSelectedObjects() {
		return null;
	}
	
	public void removeItemListener(ItemListener l) {
		listeners.remove(l);
	}
	
	public String toString() {
		if (includeButton.isSelected()) return titleLabel.getText() + " " + includeButton.getText();
		else if (excludeButton.isSelected()) return titleLabel.getText() + " " + excludeButton.getText();
		else if (ignoreButton.isSelected()) return titleLabel.getText() + " " + ignoreButton.getText();
		else return "<blank>";
	}
}
