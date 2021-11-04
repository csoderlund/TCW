package sng.viewer.panels.seqTable;
/****************************************************
 * Used by both Columns and Filter
 * 
 * The column:
 * 		created in FieldSeqTab
 * 		all other columns get added on startup; these get added when created, and re-used if removed and re-added
 * 		formated in FieldMapper.getObjFromSeqResultSet 
 * The filter:
 * 		created in SeqQueryTab
 * 		filtered in SeqQueryTab.getWhereNfold 
 * 
 * This does not fit into the FieldMapper paradigm, so is complex
 * 
 * All calls for data and computations are made directly here, so can easily trace where accessed
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;

import sng.util.FieldMapper;
import util.ui.ButtonComboBox;
import util.database.Globalx;
import util.methods.Out;
import util.methods.Static;

public class UIfieldNFold extends JPanel {
	private static final long serialVersionUID = 5636465225630988236L;
	
	private final String notDelim="/", logDelim=":"; // this are hard-coded in util.MainTable for stats
	
	private final int BASE = FieldSeqData.N_FOLD_LIB;
	private final int LIMIT = FieldSeqData.N_FOLD_LIB_LIMIT;
	private final int THEIGHT = 100, TWIDTH = 100;
	
	public UIfieldNFold( JCheckBox [] allLibraries, FieldMapper fMapObj) {
		this.bFilter = (fMapObj==null);
		this.fMapObj = fMapObj;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Color.white);
		
		selectedFolds = new Vector<String> ();
		strLibNames = new String[allLibraries.length];
		for(int x=0; x<strLibNames.length; x++) {
			strLibNames[x] = allLibraries[x].getText();
		}
		
	// Top line for Filter
		JPanel row = Static.createRowPanel();
		
		chkFilter = Static.createCheckBox("N-fold", false);
		chkFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { // Will not exist for columns
				boolean b = (chkFilter.isSelected());
				txtNfold.setEnabled(b);
				cmbBool.setEnabled(b);
				cmbReg.setEnabled(b);
				chkLog.setEnabled(b);
				btnRemove.setEnabled(b);
				btnAdd.setEnabled(b);
				
				if (theTable.getSelectedRowCount() == 0) {
					btnRemove.setEnabled(false);
				}
			}
		});
		row.add(chkFilter); row.add(Box.createHorizontalStrut(2));
		
		txtNfold = Static.createTextField("1", 3, false);
		row.add(txtNfold); row.add(Box.createHorizontalStrut(15));
	
		String [] action2 = {"Up-Down E", "Up U", "Down D"}; // CAS335 moved from lowerPage
		cmbReg = Static.createCombo(action2, false);
		
		cmbReg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmbReg.repaint(); 
				cmbReg.revalidate();
			}
		});
		row.add(cmbReg); row.add(Box.createHorizontalStrut(15));
		
		
		String [] action = {"Every", "Any"};
		cmbBool = Static.createCombo(action, false); 
		cmbBool.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmbBool.repaint();
			}
		});
		row.add(Static.createLabel("for")); 	row.add(Box.createHorizontalStrut(2));
		row.add(cmbBool);						row.add(Box.createHorizontalStrut(2));
		row.add(Static.createLabel("Pair"));	
		
		if (bFilter) {
			add(row);
			add(Box.createVerticalStrut(10));
		}
		else chkFilter.setSelected(true);

	// Select panel
		JPanel lowerRow  = Static.createRowPanel();
		
		JPanel selectPage = Static.createPagePanel();
		cmbRefLib = new ButtonComboBox();		cmbRefLib.addItem("Select A");
		cmbLib = new ButtonComboBox();			cmbLib.addItem("Select B");
		for(int x=0; x<strLibNames.length; x++) {
			cmbRefLib.addItem(strLibNames[x]);
			cmbLib.addItem(strLibNames[x]);
		}
		cmbRefLib.setSelectedIndex(0);
		cmbLib.setSelectedIndex(0);
		
		cmbRefLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnAdd.setEnabled((chkFilter.isSelected() 
						&& cmbRefLib.getSelectedIndex() > 0 
						&& cmbLib.getSelectedIndex() > 0 
						&& cmbRefLib.getSelectedIndex() != cmbLib.getSelectedIndex()));
			}
		});
		
		cmbLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnAdd.setEnabled((chkFilter.isSelected() 
						&& cmbRefLib.getSelectedIndex() > 0 
						&& cmbLib.getSelectedIndex() > 0 
						&& cmbRefLib.getSelectedIndex() != cmbLib.getSelectedIndex()));
			}
		});

		selectPage.add(cmbRefLib);
		selectPage.add(Box.createVerticalStrut(10));
		selectPage.add(cmbLib);
		
		selectPage.setMinimumSize(selectPage.getPreferredSize());
		selectPage.setMaximumSize(selectPage.getPreferredSize());
		
		lowerRow.add(selectPage);
		lowerRow.add(Box.createHorizontalStrut(10));
		
		// Add/Remove
		JPanel buttonPage = Static.createPagePanel();
		btnAdd = Static.createButton("Add", false);
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (bFilter) addFilter();
				else         addColumn();
			}
		});
		btnRemove = Static.createButton("Remove", false);
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int nrow = theTable.getSelectedRow();
				if (nrow>=0) {
					selectedFolds.remove(nrow);
					theTableModel.removeRow(nrow);
				}
			}
		});
		
		btnAdd.setMinimumSize(btnRemove.getMinimumSize());
		btnAdd.setMaximumSize(btnRemove.getMaximumSize());
		btnAdd.setPreferredSize(btnRemove.getPreferredSize());
		
		buttonPage.add(btnAdd);
		buttonPage.add(Box.createVerticalStrut(10));
		buttonPage.add(btnRemove);
		buttonPage.setMaximumSize(buttonPage.getPreferredSize());
		buttonPage.setMinimumSize(buttonPage.getPreferredSize());
		
		lowerRow.add(buttonPage);  lowerRow.add(Box.createHorizontalStrut(10));
		
		// table
		String headers[] = {"test"};
		theTableModel = new DefaultTableModel(headers,0){
			private static final long serialVersionUID = 2691505287422437591L;

			public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		theTable = new JTable(theTableModel);
		sPane = new JScrollPane(theTable);
		
		theTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				if (chkFilter.isSelected()) {
					boolean b = theTable.getSelectedRowCount() > 0;
					btnRemove.setEnabled(b);
				}
			}
		});
		theTable.setTableHeader(null);
		theTable.setAlignmentX(Component.LEFT_ALIGNMENT);
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		theTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
		
		Dimension temp = sPane.getPreferredSize();
		temp.height = THEIGHT;
		temp.width = TWIDTH;
		sPane.setPreferredSize(temp);
		sPane.setAlignmentX(Component.LEFT_ALIGNMENT);

		lowerRow.add(sPane); lowerRow.add(Box.createHorizontalStrut(10));
		
		JPanel logPage = Static.createPagePanel(); // CAS335 added
		chkLog = Static.createCheckBox("Log2", true);
		chkLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				replaceFold();
			}
		});
		logPage.add(chkLog);
		lowerRow.add(logPage);
		
		lowerRow.setMaximumSize(lowerRow.getPreferredSize());
		lowerRow.setMinimumSize(lowerRow.getPreferredSize());
		
		add(lowerRow);
	}
	private void addColumn() {
		String delim = (chkLog.isSelected()) ? logDelim : notDelim;
		String newItem = cmbRefLib.getSelectedItem() + delim + cmbLib.getSelectedItem();
		
		if(!selectedFolds.contains(newItem)) {
			selectedFolds.add(newItem);
			
			Vector<String> newRow = new Vector<String>();
			newRow.add(newItem);
			theTableModel.addRow(newRow);
			
			if (!foldMap.containsKey(newItem)) {
				if (BASE+foldIndex<=LIMIT) {
					int nID = BASE+foldIndex;
					foldIndex++;
					
					// All columns are added at startup except for these, which are added as created
					fMapObj.addFloatField(nID, newItem, null, null, null, null, null);
					
					foldMap.put(newItem, nID);
				}
				else Out.PrtError("Exceeded number of N-Fold columns");
			}
		}
	}
	/**************************************************
	 * Change all fold columns to/from logDelim/notDelim
	 * Both the FildMapper and foldMap can have both lib1/lib2 and lib1:lib2 with different nIDs
	 */
	private void replaceFold() {
		String delim =  chkLog.isSelected()  ? logDelim : notDelim;
		String other = !chkLog.isSelected()  ? logDelim : notDelim;

		for (int nrow=0; nrow<selectedFolds.size(); nrow++) { 
			String oldCol = selectedFolds.get(nrow);
			
			if (oldCol.contains(other)) {
				
				String newCol = oldCol.replace(other, delim);
				
				if (!bFilter && !foldMap.containsKey(newCol)) {
					if (BASE+foldIndex<=LIMIT) {
						int nID = BASE+foldIndex;
						foldIndex++;
						
						foldMap.put(newCol, nID);
						
						fMapObj.addFloatField(nID, newCol, null, null, null, null, null);
					}
					else Out.PrtError("Exceeded number of N-Fold columns");
				}
				selectedFolds.set(nrow, newCol);		// replaces
				theTableModel.setValueAt(newCol, nrow, 0);
				
				repaint();
			}
		}
	}
	// CAS335 changed to replace existing pair (use to be Change button)
	private void addFilter() {
		String delim = chkLog.isSelected() ? logDelim : notDelim;
		
		String newPair = cmbRefLib.getSelectedItem() + delim + cmbLib.getSelectedItem();
		
		String newFold = newPair;
		int index = cmbReg.getSelectedIndex();
		if (index==0) newFold += " E";
		else if (index==1) newFold += " U";
		else if (index==2) newFold += " D";
		
		for (String oldFold : selectedFolds) {
			if (oldFold.equals(newFold)) return;
		}
		
		for (int nrow=0; nrow<selectedFolds.size(); nrow++) {
			String [] tok = selectedFolds.get(nrow).split(" ");
			if (tok[0].trim().equals(newPair)) { // replace
				selectedFolds.set(nrow, newFold);
				theTableModel.setValueAt(newFold, nrow, 0);
				repaint();
				
				return;
			}
		}
		selectedFolds.add(newFold);
		
		Vector<String> newRow = new Vector<String>();
		newRow.add(newFold);
		theTableModel.addRow(newRow);
	}
	
	/*********************************************
	 * Filter (SeqQueryTab) and Columns (FieldSeqTab)
	 * CAS335 moved all Nfold stuff to here; just moved the nfoldObj around
	 */
	public Vector <String> getFoldColsVec() {	// Return column name only
		Vector <String> retVal = new Vector <String> ();
		
		for (String fold : selectedFolds) {
			String [] tok = fold.split(" "); 
		
			retVal.add(tok[0]);	
		}
		return retVal;
	}
	public String [] getFoldLibs() {			// return libs from columns
		Vector <String> libs = new Vector <String> ();
		
		Vector <String> toks = new Vector <String> ();
		for (String fold : selectedFolds) {
			if (fold.contains(logDelim)) toks.add(fold.replace(logDelim, " "));
			else 						 toks.add(fold.replace(notDelim, " "));
		}
		
		for (String fold : toks) {
			String [] vals = fold.split(" ");
			if(!libs.contains(vals[0])) libs.add(vals[0]);
			if(!libs.contains(vals[1])) libs.add(vals[1]);
		} 	
		return libs.toArray(new String[0]);
	}
	
	public boolean isNfoldFilter() {
		return chkFilter.isSelected() && selectedFolds.size()>0;
	}	
	public String getDelim() {
		return (chkLog.isSelected()) ? logDelim : notDelim;
	}
	
	/********************************************
	 * Filter
	 */
	
	

	/*****************************************
	 * Columns 
	 */
	public int getIDForName(String name) {
		if(!selectedFolds.contains(name)) return -1;
		if (!foldMap.containsKey(name)) {
			Out.PrtErr("TCW Sequence column error getting index for " + name);
			return -1;
		}
		return foldMap.get(name);
	}
	// Columns only: Called on startup to set from preferences
	public void initColumnFromPref(String foldName) {
		if(selectedFolds.contains(foldName)) return;

		String [] libs;
		if (foldName.contains(notDelim)) {
			libs  = foldName.split(notDelim);
			chkLog.setSelected(false);
		}
		else if (foldName.contains(logDelim)) {
			libs  = foldName.split(logDelim);
			chkLog.setSelected(true);
		}
		else return; // Something changed
			
		// sanity check
		int cnt=0;
		for(int x=0; x<strLibNames.length && cnt!=2; x++) {
			if      (libs[0].equals(strLibNames[x])) cnt++;
			else if (libs[1].equals(strLibNames[x])) cnt++;
		}	
		if(cnt!=2) {
			Out.PrtErr("Incorrect fold: " + foldName);
			return; // shouldn't happen
		}
			
		selectedFolds.add(foldName);
		foldMap.put(foldName, BASE+foldIndex);
		foldIndex++;
		
		Vector<String> newRow = new Vector<String>();
		newRow.add(foldName);
		theTableModel.addRow(newRow);
	}
	
	// called on startup to set mapper fields -- 
	public void addColumnsToMapper(FieldMapper fMapObj) {
		for (String foldCol : selectedFolds) {
			fMapObj.addFloatField(foldMap.get(foldCol), foldCol, null, null, null, null, null);
		}
	}
	
	// called on Restore Defaults. Do not clear foldMap so will reuse index if fold selected again
	public void clear() {
		cmbRefLib.setSelectedIndex(0);
		cmbLib.setSelectedIndex(0);
		
		if (theTableModel.getRowCount()==0) return;
		selectedFolds.clear();
		
		while(theTableModel.getRowCount() > 0) {
		    theTableModel.removeRow(0);
		}
		theTableModel.fireTableDataChanged();
	}
	
	/******************************************************
	 * FC pre-335 changed 0 in denominator to 0.1
	 * After reading many posts and trying different approaches, I settled on adding 0.1 which shifts everything
	 */
	/*******************************************************
	 * compute column
	 */
	private final double inc=0.1;
	
	public double getFC(double A, double B) {
		double result=0.0;
		
		A += inc; 
		B += inc;
		if (chkLog.isSelected()) { 
			result = Math.log(A/B)/Math.log(2);
		}
		else {
			if(A>=B) { 
				result = A/B;
			}
			else     {
				result = -1 * (B/A);
			}
		}
		return result;
	}
	/******************************************************
	 * filter - SeqQueryTab SQL statement for filtering
	 */
	public String getFoldSQL() {
		String clause="", sql="";
		
		String op = (cmbBool.getSelectedIndex() == 0) ? "&" : "|";
		double n  = getNfold();
		boolean isLog = chkLog.isSelected();
		String delim = getDelim();
		String sDir="";
		summary="";
		
		Vector <String> colPairs = new Vector <String> ();// remove delimiter after adding to summary
		for (String fold : selectedFolds) { 
			if (summary!="") summary += op;
			String [] tok = fold.split(" ");
			summary += tok[0];
			sDir    += tok[1];
			
			colPairs.add(fold.replace(delim, " "));
		}
		
		String fc = (isLog) ? "Log2FC " : "FC ";
		summary = fc + "N=" + n + ": " + summary + " (" + sDir + ")";
		
		for (String p : colPairs) {
			String [] tok = p.split(" ");
			if (tok.length!=3) {Out.bug("Bad fold: " + p);continue;}
			
			String l1 = Globalx.LIBRPKM + tok[0];
			String l2 = Globalx.LIBRPKM + tok[1];
			String lib1 = "(" + l1 + "+" + inc + ")";
			String lib2 = "(" + l2 + "+" + inc + ")";
			
			int dir = 0;
			if (tok[2].trim().equals("E"))      dir =  0;
			else if (tok[2].trim().equals("U")) dir =  1;
			else if (tok[2].trim().equals("D")) dir = -1;
			else Out.bug("Bad dir: '" + tok[2] + "' " + p);
			
			clause="";
			if (isLog) {
				String log2 = "LOG(" + lib1 + "/" + lib2 + ")/LOG(2)";
				if (dir>=0) clause  = log2 + " >= " + n;
				if (dir==0) clause += " or ";
				if (dir<=0) clause += log2 + " <= -" +  n;
			}
			else {
				if (dir>=0) clause += lib1 + "/" + lib2 + " >= " + n;
				if (dir==0) clause += " or "; 
				if (dir<=0) clause += lib2 + "/" + lib1 + " >= " + n;
			}
			String zero = l1 + ">0 or " + l2 + ">0"; 
			clause = " ((" + zero + ") and (" + clause + ")) ";
			
			if (sql!= "") sql += op;
			sql += clause;
		}

		return sql;
	}
	public String getFoldSum() {
		return summary;
	}
	private double getNfold() {
		String n = txtNfold.getText();
		try {
			double i = Double.parseDouble(n);
			return i;
		}
		catch (Exception e) { 
			int def = (chkLog.isSelected()) ? 1 : 2;
			Out.PrtErr("Incorrect FC cutoff '" + n + "'; using " + def);
			txtNfold.setText(def+"");
			return  (double) def;
		}
	}
	private JScrollPane sPane = null;
	private JTable theTable = null;
	private DefaultTableModel theTableModel = null;
	
	private ButtonComboBox cmbRefLib = null, cmbLib = null;
	private JButton btnAdd = null, btnRemove = null;
	
	private boolean bFilter = false;
	private JCheckBox chkFilter = null;
	private JTextField txtNfold = null;
	private JComboBox <String> cmbBool = null, cmbReg = null;
	
	private JCheckBox chkLog = null;
	
	private String [] strLibNames = null;
	private Vector<String> selectedFolds = null;
	private HashMap <String, Integer> foldMap = new HashMap <String, Integer> ();
	private int foldIndex=0;
	private FieldMapper fMapObj=null;
	
	private String summary=""; //created during getFoldSL, return on getFoldSum
}
