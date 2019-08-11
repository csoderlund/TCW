package util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class ButtonOptionDialog extends JDialog {
	private static final long serialVersionUID = 8508408134439829394L;
	private static int MAX_HEIGHT = 100;
	
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
		d.height = Math.min(d.height, MAX_HEIGHT);
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
