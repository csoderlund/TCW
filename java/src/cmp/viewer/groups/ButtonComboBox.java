package cmp.viewer.groups;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicArrowButton;

import cmp.database.Globals;

public class ButtonComboBox extends JPanel {
	private static final long serialVersionUID = -2875371221678221795L;

	public ButtonComboBox() {
		setBackground(Globals.BGCOLOR);
		btnSelection = new JButton("");
		btnSelection.setBackground(Globals.BGCOLOR);
		theSelections = new Vector<String> ();
		
		btnDecrement = new BasicArrowButton(BasicArrowButton.WEST);
		btnDecrement.setBackground(Globals.BGCOLOR);
		btnDecrement.setMaximumSize(btnDecrement.getPreferredSize());
		btnDecrement.setMinimumSize(btnDecrement.getPreferredSize());
		btnDecrement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				decrementSelection();
			}
		});
		btnIncrement = new BasicArrowButton(BasicArrowButton.EAST);
		btnIncrement.setBackground(Globals.BGCOLOR);
		btnIncrement.setMaximumSize(btnIncrement.getPreferredSize());
		btnIncrement.setMinimumSize(btnIncrement.getPreferredSize());
		btnIncrement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				incrementSelection();
			}
		});
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(btnDecrement);
		add(btnSelection);
		add(btnIncrement);
		
		Dimension temp = btnDecrement.getPreferredSize();
		temp.width *= 2;
		btnDecrement.setPreferredSize(temp);
		btnDecrement.setMaximumSize(temp);
		btnDecrement.setMinimumSize(temp);
		
		theButtonPrefSize = btnSelection.getPreferredSize();
		thePanePrefSize = btnSelection.getPreferredSize();
		thePanePrefSize.width += (btnDecrement.getPreferredSize().width) + (btnIncrement.getPreferredSize().width);
	}
	
	public void setSelectedIndex(int index) {
		nCurrentSelection = index;
		btnSelection.setText(theSelections.get(nCurrentSelection));
		btnSelection.setPreferredSize(theButtonPrefSize);
	}
	
	public int getSelectedIndex() { return nCurrentSelection; }
	
	public void addItem(String item) {
		theSelections.add(item);
		if(nCurrentSelection == -1) {
			nCurrentSelection = 0;
		}
		updateButton();
	}
	
	public void setEnabled(boolean enabled) { btnSelection.setEnabled(enabled); }
	public boolean isEnabled() { return btnSelection.isEnabled(); }
	
	public String getSelectedItem() {
		return theSelections.get(nCurrentSelection);
	}
	
	private void updateButton() {
		int maxSize = 0;
		int maxIndex = -1;
		for(int x=0; x<theSelections.size(); x++) {
			if(theSelections.get(x).length() > maxSize) {
				maxSize = theSelections.get(x).length();
				maxIndex = x;
			}
		}
		if(maxIndex >= 0) {
			btnSelection.setText(theSelections.get(maxIndex));
			btnSelection.setMaximumSize(btnSelection.getPreferredSize());
			btnSelection.setMinimumSize(btnSelection.getPreferredSize());
			theButtonPrefSize = btnSelection.getPreferredSize();
			thePanePrefSize = btnSelection.getPreferredSize();
			thePanePrefSize.width += (btnDecrement.getPreferredSize().width) + (btnIncrement.getPreferredSize().width);
		}
	}
	
	public Dimension getPreferredSize() { return thePanePrefSize; }
	
	private void incrementSelection() {
		nCurrentSelection++;
		nCurrentSelection = nCurrentSelection % theSelections.size();
		setSelectedIndex(nCurrentSelection);
	}
	
	private void decrementSelection() {
		nCurrentSelection--;
		if(nCurrentSelection < 0)
			nCurrentSelection = theSelections.size() - 1;
		setSelectedIndex(nCurrentSelection);
	}
	
	private int nCurrentSelection = -1;
	private JButton btnSelection = null;
	private Vector<String> theSelections = null;
	private BasicArrowButton btnDecrement = null;
	private BasicArrowButton btnIncrement = null;
	private Dimension thePanePrefSize = null, theButtonPrefSize = null;
}
