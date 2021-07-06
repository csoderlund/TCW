package util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.basic.BasicArrowButton;

import util.methods.Out;

/*************************************************
 * Drop-down selection with arrows on end
 */
public class ButtonComboBox extends JPanel {
	private static final long serialVersionUID = -2875371221678221795L;

	public ButtonComboBox() {
		setBackground(Color.WHITE);
		btnSelection = new JButton("");
		btnSelection.setBackground(Color.WHITE);
		btnSelection.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ButtonOptionDialog theOptions = new ButtonOptionDialog(theSelections.toArray(new String[0]));
				String theSelection = theOptions.getSelection(MouseInfo.getPointerInfo().getLocation());
				if(theSelection != null)
					setSelectedIndex(theSelections.indexOf(theSelection));
			}
		});
		theListeners = new Vector<ActionListener> ();
		theSelections = new Vector<String> ();
		
		btnDecrement = new BasicArrowButton(BasicArrowButton.WEST);
		btnDecrement.setBackground(Color.WHITE);
		btnDecrement.setMaximumSize(btnDecrement.getPreferredSize());
		btnDecrement.setMinimumSize(btnDecrement.getPreferredSize());
		btnDecrement.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				decrementSelection();
			}
		});
		btnIncrement = new BasicArrowButton(BasicArrowButton.EAST);
		btnIncrement.setBackground(Color.WHITE);
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
		
		theButtonPrefSize = btnSelection.getPreferredSize();
		thePanePrefSize = btnSelection.getPreferredSize();
		thePanePrefSize.width += 
				(btnDecrement.getPreferredSize().width) + (btnIncrement.getPreferredSize().width);
	}
	
	public void addActionListener(ActionListener l) {
		theListeners.add(l);
	}
	
	public void removeAllListeners() { theListeners.removeAllElements(); }
	
	public void removeAllItems() {
		theSelections.clear();
		btnSelection.setText("");
		nCurrentSelection = -1;
	}
	public String[] getColumns() { return theSelections.toArray(new String[0]);}
	public int getItemCount() { return theSelections.size(); }
	
	public void setSelectedIndex(int index) {
		if (index>=theSelections.size()) {
			Out.prt("TCW error on ButtonComboBox.setSelectedIndex: " + index + " " + theSelections.size());
			for (String x : theSelections) Out.prt("   " + x);
			return;
		}
		nCurrentSelection = index;
		btnSelection.setText(theSelections.get(nCurrentSelection));
		for(int x=0;x<theListeners.size(); x++)
			theListeners.get(x).actionPerformed(new ActionEvent(btnSelection, ActionEvent.ACTION_PERFORMED, "Selection Change"));
		btnSelection.setPreferredSize(theButtonPrefSize);
	}
	
	public int getSelectedIndex() { return nCurrentSelection; }
	
	public void addItems(Vector <String> items) {
		for (String s : items) theSelections.add(s);
		if(nCurrentSelection == -1) nCurrentSelection = 0;
		updateButton();
		finish();
	}
	public void addItems(String [] items) {
		for (String s : items) theSelections.add(s);
		if(nCurrentSelection == -1) nCurrentSelection = 0;
		updateButton();
		finish();
	}
	public void addItem(String item) {
		theSelections.add(item);
		if(nCurrentSelection == -1) nCurrentSelection = 0;
		updateButton();
	}
	public void finish() {
		setSelectedIndex(nCurrentSelection);
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	public String getItemAt(int pos) {
		if(pos < 0 || pos >= theSelections.size()) return null;
		return theSelections.get(pos);
	}
	
	public void setEnabled(boolean enabled) { 
		btnSelection.setEnabled(enabled);
		btnDecrement.setEnabled(enabled);
		btnIncrement.setEnabled(enabled);
	}
	public boolean isEnabled() { return btnSelection.isEnabled(); }
	
	public String getSelectedItem() {
		return theSelections.get(nCurrentSelection);
	}
	
	public boolean setSelectedItem(String item) {
		int index = theSelections.indexOf(item);
		if(index < 0)
			return false;
		nCurrentSelection = index;
		btnSelection.setText(theSelections.get(nCurrentSelection));
		return true;
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
			String temp = btnSelection.getText();
			btnSelection.setText(theSelections.get(maxIndex));
			Dimension d = btnSelection.getPreferredSize();
			btnSelection.setMaximumSize(d);
			btnSelection.setMinimumSize(d);
			theButtonPrefSize = btnSelection.getPreferredSize();
			thePanePrefSize = btnSelection.getPreferredSize();
			thePanePrefSize.width += (btnDecrement.getPreferredSize().width) + (btnIncrement.getPreferredSize().width);
			btnSelection.setText(temp);
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
	public void print () {
		for (int i=0; i<theSelections.size(); i++) 
			System.err.println(i + ". " + theSelections.get(i));
	}
	private int nCurrentSelection = -1;
	private JButton btnSelection = null;
	private Vector<String> theSelections = null;
	private BasicArrowButton btnDecrement = null;
	private BasicArrowButton btnIncrement = null;
	private Dimension thePanePrefSize = null, theButtonPrefSize = null;
	private Vector<ActionListener> theListeners = null;
	
	/*************************************************************************
	 * CAS326 was in separate file, but only used by ButtonCombobox
	 */
	private class ButtonOptionDialog extends JDialog {
		private static final long serialVersionUID = 8508408134439829394L;
		
		public ButtonOptionDialog(String [] theOptions) {
			setModal(true);
			setUndecorated(true);
			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
			
			JPanel selectionPanel = new JPanel();
			selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.PAGE_AXIS));
			
			theSelections = new JButton[theOptions.length];
			for(int x=0; x<theSelections.length; x++) {
				theSelections[x] = new JButton(theOptions[x]);
				final int pos = x;
				theSelections[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						strClickedItem = theSelections[pos].getText();
						setVisible(false);
					}
				});
			}
			btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					strClickedItem = null;
					setVisible(false);
				}
			});
			btnCancel.setAlignmentX(Component.LEFT_ALIGNMENT);
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(btnCancel);
			
			Dimension maxSize = btnCancel.getPreferredSize();
			for(int x=0; x<theSelections.length; x++) {
				maxSize.height = Math.max(maxSize.height, theSelections[x].getPreferredSize().height);
				maxSize.width = Math.max(maxSize.width, theSelections[x].getPreferredSize().width);
			}
			for(int x=0; x<theSelections.length; x++) {
				theSelections[x].setPreferredSize(maxSize);
			}
			
			for(int x=0; x<theSelections.length; x++) {
				JPanel row = new JPanel();
				row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
				row.add(Box.createHorizontalGlue());
				row.add(theSelections[x]);
				row.add(Box.createHorizontalGlue());
				selectionPanel.add(row);
			}
			
			JScrollPane sPane = new JScrollPane(selectionPanel);
			Dimension d = sPane.getPreferredSize();
			d.height = Math.min(d.height, 100);
			sPane.setPreferredSize(d);
			sPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			mainPanel.add(sPane);
			mainPanel.add(Box.createVerticalStrut(10));
			mainPanel.add(buttonPanel);
			mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
			add(mainPanel);
			
			pack();
		}
		
		public void windowLostFocus(WindowEvent e) {
		    System.out.println("lost focus");
		  }
		
		public String getSelection(Point center) {
			setLocation(center);
			setVisible(true);
			return strClickedItem;
		}
		
		private JButton [] theSelections = null;
		private JButton btnCancel = null;
		private String strClickedItem = "";
	}

}
