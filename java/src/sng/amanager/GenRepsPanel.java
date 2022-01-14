package sng.amanager;
/*****************************************************
 * Define Replicates
 * Panel to define what counts are replicates for the 'Associated Count' table
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;


import sng.database.Globals;
import util.ui.UserPrompt;
import util.methods.Static;

public class GenRepsPanel extends JPanel {
	private static final long serialVersionUID = -1652821001608191481L;
	private static final String helpHTML =  Globals.helpRunDir + "GenRepsPanel.html";
	private static final String [] COL_NAMES = { 
		"SeqID", "Column name from file", "Condition (editable)" };
	private static final int TABLE_HEIGHT = 300;

	public GenRepsPanel(ManagerFrame parentFrame) {
		theParentFrame = parentFrame;
		JPanel mainPanel = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();	
		JLabel title = new JLabel("Define Replicates");
		title.setFont(mainPanel.getFont().deriveFont(Font.BOLD, 18));
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		mainPanel.add(row);
		mainPanel.add(Box.createVerticalStrut(20));
		
		theModel = new TheTableModel();
		theTable = new JTable();
		theTable.setModel(theModel);
		sPane = new JScrollPane(theTable);
		sPane.getViewport().setBackground(Globals.BGCOLOR);
		Dimension d = sPane.getPreferredSize();
		d.height = TABLE_HEIGHT;
		sPane.setPreferredSize(d);
		sPane.setMaximumSize(d);
		sPane.setMinimumSize(d);
		
		JPanel tablePanel = Static.createRowPanel();;
		tablePanel.add(sPane);
		mainPanel.add(tablePanel);
		mainPanel.add(Box.createVerticalStrut(20));
		
		btnKeep = new JButton("Keep");
		btnKeep.setBackground(Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				stopEditing();
				
				if (checkForDups()) {
					keepReps();
					theParentFrame.updateUI();
					
					setVisible(false);
					theParentFrame.setMainPanelVisible(true);
					theParentFrame.saveProject();	
					theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				}
			}
		});
		
		btnDiscard = new JButton("Cancel");
		btnDiscard.setBackground(Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				setVisible(false);
				theParentFrame.setMainPanelVisible(true);
				theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, 
						"Define Replicates", helpHTML);
			}
		});
		
		JPanel buttonPanel = Static.createRowPanel();
		buttonPanel.add(btnKeep);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(btnDiscard);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(btnHelp);

		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel temp = Static.createRowPanel();
		temp.add(Box.createHorizontalGlue());
		temp.add(buttonPanel);
		temp.add(Box.createHorizontalGlue());
		mainPanel.add(temp);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
		mainPanel.setMaximumSize(mainPanel.getPreferredSize());
		mainPanel.setMinimumSize(mainPanel.getPreferredSize());
		mainPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		add(mainPanel);
		setVisible(false);
	}
	/********************************************
	 * called on Define Replicates before making panel visible
	 * create LibIDs from removing digit at end
	 */
	public void setRepList(ManagerData curManData) {
		
		Vector<String> seqList = new Vector<String> ();
		Vector<String> repList = new Vector<String> ();
		Vector<String> condList = new Vector<String> ();
		
		for(int x=0; x<curManData.getNumCountLibs(); x++) {
			ManagerData.CountData cObj = curManData.getCountLibAt(x);
			if(cObj.getNumReps() > 1) { // already processed
				String seqID = cObj.getSeqID();
				String condID = cObj.getCondID();
				Vector <String> reps = cObj.getRepList();
				for (String rep : reps) {
					seqList.add(seqID);
					repList.add(rep);
					condList.add(condID);
				}
			}
			else {
				String seqID = cObj.getSeqID();
				String repID = cObj.getRepList().get(0);
				String condID = repID;
				condID = mkLibID(condID);
				
				seqList.add(seqID);
				repList.add(repID);
				condList.add(condID);
			}
		}
		
		theModel.clearAllData();
		for(int x=0; x<repList.size(); x++) {
			theModel.addRow(seqList.get(x), repList.get(x), condList.get(x));
		}
		theModel.fireTableDataChanged();
	}
	private String mkLibID(String id) {
		String libid = id;
		if(id.matches("[a-z0-9A-Z_]+[0-9]+")) {
			Pattern pattern = Pattern.compile("[a-z0-9A-Z_]*[^0-9]+");
			Matcher matcher = pattern.matcher(id);
			
			if(matcher.find()) {
				String val = matcher.group();
				while(val.endsWith("_"))
					val = val.substring(0, val.length()-1);
				libid = val;
			}
		}
		return libid;
	}
	
	/*****************************************************
	 * creates HashMap of seqID:libID -- rep, rep, rep.....
	 */
	public void keepReps() {
		String [] seq = getAllSeqID();
		String [] cond = getAllCond();
		String [] rep = getAllRepID();
			
		Vector <String> keyOrder = new Vector <String> (); // keep in order
		HashMap <String, String> repMap = new HashMap <String, String> ();
			
		for(int i=0; i<rep.length; i++) {
			String key = seq[i] + ":" + cond[i];
			if (repMap.containsKey(key)) {
				String list = repMap.get(key) + "," + rep[i];
				repMap.put(key, list);
			}
			else {
				repMap.put(key, rep[i]);
				keyOrder.add(key);
			}
		}
		ManagerData curManData = theParentFrame.getCurManData();
		curManData.replaceDefinedReps(keyOrder, repMap);
	}
	private boolean checkForDups() { 
		String [] cond = getAllCond();
		String [] seq = theParentFrame.getCurManData().getSeqID();
		
		for (String c : cond) {
			for (String s: seq) {
				if (s.equals(c)) {
					JOptionPane.showMessageDialog(null, 
						"SeqIDs and conditions must not have the same name\nCondition '" + s + "' is also a SeqID", "Error", JOptionPane.PLAIN_MESSAGE);
					return false;
				}
			}
		}
		return true;
	}
	
	/***************************************************************/
	private void stopEditing() {
		if(theTable.isEditing())
			theTable.getCellEditor().stopCellEditing();
	}
	private String [] getAllCond() {
		String [] retVal = new String[theModel.getRowCount()];
		
		for(int x=0; x<retVal.length; x++) 
			retVal[x] = theModel.getCondAt(x);
		
		return retVal;
	}

	private String [] getAllSeqID() {
		String [] retVal = new String[theModel.getRowCount()];
		
		for(int x=0; x<retVal.length; x++) 
			retVal[x] = theModel.getSeqIDAt(x);
		
		return retVal;
	}

	private String [] getAllRepID() {
		String [] retVal = new String[theModel.getRowCount()];
		
		for(int x=0; x<retVal.length; x++) 
			retVal[x] = theModel.getRepAt(x);
		
		return retVal;
	}

	/*******************************************************************/
	private class TheTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -473598297867947687L;

		public TheTableModel() { theRows = new TableRow[0]; }
		
		public void addRow(String seqID, String repID, String cond) {
			TableRow [] temp = theRows;
			
			theRows = new TableRow[temp.length + 1];
			for(int x=0; x<temp.length; x++)
				theRows[x] = temp[x];
			theRows[theRows.length - 1] = new TableRow(seqID, repID, cond);
		}
		public Object getValueAt(int row, int column) {
			if(column == 0) return 	theRows[row].strSeqID;
			if(column == 1) return 	theRows[row].strRepID;
			return 					theRows[row].strCondID;
		}
		public void setValueAt(Object val, int row, int column) {
			if (column == 0) 		theRows[row].strSeqID = (String)val;
			else if (column == 1) 	theRows[row].strRepID = (String)val;
			else 					theRows[row].strCondID = (String)val;
		}
		
		public String getSeqIDAt(int row) { return theRows[row].strSeqID; }
		public String getRepAt(int row) { return theRows[row].strRepID; }
		public String getCondAt(int row) { return theRows[row].strCondID; }
		
		public void clearAllData() { theRows = new TableRow[0]; }
		public int getRowCount() { return theRows.length; }
		public Class<?> getColumnClass(int column) { return String.class; }
		public String getColumnName(int columnIndex) { return COL_NAMES[columnIndex]; }
		public boolean isCellEditable (int row, int column) { return column == 2; }
		public int getColumnCount() { return COL_NAMES.length; }
		
		private class TableRow {
			public TableRow(String seqid, String repid, String cond) {
				strSeqID = seqid;
				strRepID = repid;
				strCondID = cond;
			}

			private String strSeqID = "";
			private String strRepID = "";	// From File
			private String strCondID = "";
		}
		private TableRow [] theRows = null;
	}
	/************************************************************/
	private JTable theTable = null;
	private TheTableModel theModel = null;
	private JScrollPane sPane = null;
	private JButton btnKeep = null;
	private JButton btnDiscard = null;
	private JButton btnHelp = null;
	private ManagerFrame theParentFrame = null;
}
