package sng.viewer.panels.Basic;

/*************************************************************
 * Creates the Table 
 * CAS324 remove Trim code and add code to know re-order
 * NOTE: trim still exists but needs updating, at which point, re-add to this code
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import sng.viewer.STCWFrame;
import sng.viewer.panels.DisplayDecimalTab;
import util.database.DBConn;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileWrite;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.methods.Stats;
import util.ui.DisplayFloat;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class BasicGOTablePanel {
	private final Color BGCOLOR = 		Globalx.BGCOLOR;
	private final Color HIGHCOLOR = 	Globalx.HIGHCOLOR;
	private final Color altRowColor = 	Globalx.altRowColor;
	private final Color selectColor = 	Globalx.selectColor;
	
	private final Color EvCOLOR = new Color(255, 102, 102);
	private final String GO_FORMAT = Globalx.GO_FORMAT;
	private final int MAX_COL = 300;
	
	private final String goPref= "_goPrefs";
	private final String EVALUE = "Best E-val";
	
	// IF CHANGE COLUMNS, FIX THESE TOO!!! use theResults with these indices
	private static final int GOrow=0;   
	public  static final int GOindex=1;
	public  static final int GOdomain=2;
	private static final int GOdesc=3; 
	private static final int GOnSeq=5; // CAS334 was 5 (wasn't fixed after column change), which broke #Seqs DE
	
	// CAS336 add row
	private  final Class<?> [] COL_TYPES = 
	 {Integer.class, Integer.class,  String.class, String.class, Integer.class, Integer.class, Integer.class,  Double.class }; 
	private  final String [] COL_MYSQL = 
	 {null, "go_info.gonum","go_info.term_type", "go_info.descr", "go_info.level", 
			 "go_info.nUnitranHit", "go_info.nDirectHit","go_info.bestEval" }; // CAS330 change column order
	
	private final String [] COL_NAMES = 
		 {"Row", Globalx.goID, Globalx.goOnt, Globalx.goTerm , "Level",   "#Seqs", "#Assign", EVALUE};
	private String []      evColNames = null;
	private String []	 pvalColNames = null; // No P_	// CAS324 changed from Vector
	private int endEvC=0, endStatic=0;
	
	public BasicGOTablePanel(STCWFrame f, BasicGOFilterTab g) {
		theMainFrame=f;
		filterPanel=g;
		projName = theMainFrame.getdbID();
		
		theResults = 	new Vector<Object []> ();
		
		columnPanel = new ColumnPanel();
		tablePanel = new TablePanel();
	}
	/**************************************************
	 * Column Panel
	 */
	private class ColumnPanel extends JPanel {
		private static final long serialVersionUID = -7235165216064464845L;
		private ColumnPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			 	  
			initColumns();
			
			JPanel headerPanel = Static.createRowCenterPanel();
			JLabel theHeader = new JLabel("<HTML><H2>Select columns to view</H2></HTML>");
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			Static.center(headerPanel);
			
	    	JPanel generalColsPanel = 	createGenPanel();
		    JPanel evcColsPanel =  		createEvcPanel(); 
	    	JPanel pvalColsPanel = 		createPvalPanel(); 
	    	
		    JPanel sideBySidePanel = Static.createRowPanel();
		    sideBySidePanel.add(generalColsPanel);	sideBySidePanel.add(Box.createHorizontalStrut(30));
		    sideBySidePanel.add(evcColsPanel);		sideBySidePanel.add(Box.createHorizontalStrut(30));
	    	sideBySidePanel.add(pvalColsPanel);
	    	Static.center(sideBySidePanel);
    	
	    	/////// Accept Discard
	    	JPanel buttonPanel = Static.createRowCenterPanel();
	    	JButton keepButton = Static.createButton("Accept", true);
	    	keepButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveColumns();
					theTableModel.fireTableStructureChanged();
					tablePanel.tableResizeColumns();
					
					setVisible(false);
					filterPanel.showMain();
				}
			});
			JButton discardButton = Static.createButton("Discard", true);
			discardButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					filterPanel.showMain();
				}
			});
	    	buttonPanel.add(keepButton);	buttonPanel.add(Box.createHorizontalStrut(10));
	    	buttonPanel.add(discardButton);
	    	Static.center(buttonPanel);
			
		    add(Box.createVerticalStrut(10));
	    	add(headerPanel);
	    	add(Box.createVerticalStrut(10));
	    	add(sideBySidePanel);
	    	add(Box.createVerticalStrut(20));
	    	add(buttonPanel);
	    	setVisible(false);
	    	
	    	initSelections();
		} // end createColumnPanel
		private JPanel createGenPanel() {
			JPanel generalColsPanel = Static.createPageTopPanel();
	    	generalColsPanel.add(Static.createLabel("General"));
	    	generalColsPanel.add(Box.createVerticalStrut(10));
    	
	    	chkStaticColumns = new JCheckBox[COL_NAMES.length];
	    	for(int x=0; x < COL_NAMES.length; x++) {
	    		chkStaticColumns[x] = Static.createCheckBox(COL_NAMES[x], false);
	    		chkStaticColumns[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						checkAllGen();
					}
				});
	    		generalColsPanel.add(chkStaticColumns[x]);
	    	}
		    generalColsPanel.add(Box.createVerticalStrut(10));
		    
	    	JPanel checkGenPanel = Static.createRowPanel();
	    	chkSelectAllGen = Static.createCheckBox("Check/uncheck all", false);
	    	chkSelectAllGen.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					for(int x=0; x<chkStaticColumns.length; x++) { 
						boolean isSel = chkSelectAllGen.isSelected();
						chkStaticColumns[x].setSelected(isSel);
					}
				}
			});
	    	checkGenPanel.add(chkSelectAllGen);
	    	Static.border(checkGenPanel);
	    	generalColsPanel.add(checkGenPanel);
	    	
			return generalColsPanel;
		}
		private JPanel createEvcPanel() {
			JPanel evcColsPanel = Static.createPageTopPanel();
	    	  
	    	evcColsPanel.add(Static.createLabel("Evidence Categories")); 
	    	evcColsPanel.add(Box.createVerticalStrut(10));
	    
	    	int ecNum = evColNames.length;
	    	chkEvCColumns = new JCheckBox[ecNum];
	    	
	    	JPanel subPanel1 = Static.createPageTopPanel();    	
	    	for (int x=0; x<ecNum; x++) {
	    		chkEvCColumns[x] = Static.createCheckBox(evColNames[x], false);
	    		chkEvCColumns[x].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					checkAllEvC();
				}});
	    		subPanel1.add(chkEvCColumns[x]);
	    	}
	    	evcColsPanel.add(subPanel1);
	    	evcColsPanel.add(Box.createVerticalStrut(10));
	    	
	    	JPanel checkEvCRow = Static.createRowPanel();
	    	chkSelectAllECs = Static.createCheckBox("Check/uncheck all", false);
	    	chkSelectAllECs.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					for(int x=0; x<chkEvCColumns.length; x++) {
						boolean isSel = chkSelectAllECs.isSelected();
						chkEvCColumns[x].setSelected(isSel);
					}
				}
			});
	    	checkEvCRow.add(chkSelectAllECs); 
	    	Static.border(checkEvCRow);
	    	
	    	evcColsPanel.add(checkEvCRow);
	    	evcColsPanel.add(Box.createVerticalStrut(10));
	    	
	    	JPanel checkDisplayRow = Static.createRowPanel();
			JRadioButton chkEvClong = Static.createRadioButton("Long", false);
			chkEvClong.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent ae) {
			    	  	isEvCcolor=false;
			      }
			 });
			checkDisplayRow.add(chkEvClong); checkDisplayRow.add(Box.createHorizontalStrut(1));
			
			JRadioButton chkEvCshort = Static.createRadioButton("Short", true);
			chkEvCshort.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent ae) {
			    	  isEvCcolor=true;
			      }
			 });
			checkDisplayRow.add(chkEvCshort); 
			evcColsPanel.add(checkDisplayRow);
	    	
			ButtonGroup grpLevel = new ButtonGroup();
			grpLevel.add(chkEvClong);
			grpLevel.add(chkEvCshort);
			
			Static.setSize(evcColsPanel);
			return evcColsPanel;
		}
		private JPanel createPvalPanel(){
			int rowBreak=10; 
			JPanel pvalColsPanel = Static.createPageTopPanel();
			
			if (pvalColNames==null || pvalColNames.length == 0) return pvalColsPanel;
		
    		int nPval = pvalColNames.length;
    	 	pvalColsPanel.add(Static.createLabel("P-value"));
	    	pvalColsPanel.add(Box.createVerticalStrut(10));
	    	
		    int nRow = nPval;
	    	if (nPval>rowBreak) nRow=(int) ((nPval/2.0)+0.5);
	    	
    		JPanel deSubPanel1 = Static.createPageTopPanel();
    		JPanel deSubPanel2 = Static.createPageTopPanel(); 
    		
	    	chkPvalColumns = new JCheckBox[nPval];
	    	for(int x=0; x<chkPvalColumns.length; x++) {
	    		chkPvalColumns[x] = Static.createCheckBox(pvalColNames[x], false);
	    		chkPvalColumns[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						checkAllPval();
					}
				});
	    		if (x<nRow) deSubPanel1.add(chkPvalColumns[x]);
	    		else        deSubPanel2.add(chkPvalColumns[x]);
	    	}
	    	if (nRow!=nPval) {
	    		JPanel subPanel =  Static.createRowPanel();
		    	subPanel.add(deSubPanel1);		subPanel.add(Box.createHorizontalStrut(10));
		    	subPanel.add(deSubPanel2);
		    	pvalColsPanel.add(subPanel);
	    	}
	    	else pvalColsPanel.add(deSubPanel1);
	    	pvalColsPanel.add(Box.createVerticalStrut(10));  	
	    
		    JPanel checkPvalPanel = Static.createRowPanel();
	    	chkSelectAllPvals = Static.createCheckBox("Check/uncheck all", false);
	    	chkSelectAllPvals.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					boolean isSel = chkSelectAllPvals.isSelected();
					for(int x=0; x<chkPvalColumns.length; x++)
						chkPvalColumns[x].setSelected(isSel);
			}});
	    	checkPvalPanel.add(chkSelectAllPvals);
	    	Static.border(checkPvalPanel);
	    	pvalColsPanel.add(checkPvalPanel);
    	
			return pvalColsPanel;
		}
		/************************************************/
		private void initColumns() {
			try {
				pvalColNames = theMainFrame.getMetaData().getGoPvalCols(); // CAS326 was using TreeMap
				
				// for evidence codes - no EV__
				evColNames = theMainFrame.getMetaData().getEvClist();
				
				// delimit column sections
				endStatic = COL_NAMES.length;
				endEvC = endStatic + evColNames.length;
		
				selectedCol = new boolean[COL_NAMES.length + evColNames.length + pvalColNames.length];		
				for(int i=0; i<selectedCol.length; i++) {
					if (i<endStatic) 	selectedCol[i] = true;
					else 				selectedCol[i] = false;
				}
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(null, "Query failed getting column names");
				ErrorReport.prtReport(e, "Error getting column names");
			}
		}
		public void setupVisible() {
			int x=0;
			for(; x<chkStaticColumns.length; x++)
				chkStaticColumns[x].setSelected(selectedCol[x]);
			if (chkEvCColumns!=null) {
				for(int y=0; y<chkEvCColumns.length; x++, y++) 
					chkEvCColumns[y].setSelected(selectedCol[x]);
			}
			if (chkPvalColumns!=null) {
				for(int y=0; y<chkPvalColumns.length; x++, y++) 
					chkPvalColumns[y].setSelected(selectedCol[x]);
			}
			checkAllPval();
			checkAllEvC();
			checkAllGen();
			setVisible(true);
		 }
		 private void checkAllGen() {
	    	boolean allSelected = true;
	    	for(int x=0; x<chkStaticColumns.length && allSelected; x++)
	    			allSelected = chkStaticColumns[x].isSelected();
	    	chkSelectAllGen.setSelected(allSelected);
		}
	    private void checkAllEvC() {
    		if (evColNames.length == 0) return;
    	
	    	boolean allSelected = true;
	    	for(int x=0; x<chkEvCColumns.length && allSelected; x++)
	    			allSelected = chkEvCColumns[x].isSelected();
	    	chkSelectAllECs.setSelected(allSelected);
		}
	    private void checkAllPval() {
	    	if (pvalColNames==null || pvalColNames.length == 0) return;
	    
	    	boolean allSelected = true;
	    	for(int x=0; x<chkPvalColumns.length && allSelected; x++)
	    		allSelected = chkPvalColumns[x].isSelected();
	    	
	    	chkSelectAllPvals.setSelected(allSelected);
		}
	    private void saveColumns() {
	    	String prefs="";
	    	int x=0;
			for (; x<chkStaticColumns.length; x++) {
				boolean b = chkStaticColumns[x].isSelected();
				selectedCol[x] = b;
				if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
			}
			if (chkEvCColumns!=null) {
				for (int y=0; y<chkEvCColumns.length; x++, y++) {
					boolean b = chkEvCColumns[y].isSelected();
					selectedCol[x] = b;
					if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
				}
			}
			if (chkPvalColumns!=null) {
				for (int y=0; y<chkPvalColumns.length; x++, y++) {
					boolean b = chkPvalColumns[y].isSelected();
					selectedCol[x] = b;
					if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
				}
			}
			prefsRoot.put(prefLabel, prefs);
	    }
	    private void initSelections() {
	    	nStatic = chkStaticColumns.length;
	    	nEvC =  (chkEvCColumns==null ? 0 : chkEvCColumns.length);
			nPval = (chkPvalColumns==null ? 0 : chkPvalColumns.length);
			
			totalColumns = nStatic + nPval + nEvC;
			
			prefsRoot = theMainFrame.getPreferencesRoot();
			prefLabel = theMainFrame.getdbName() + goPref; // sTCW database name + _hitCol
			String goCol = prefsRoot.get(prefLabel, null); // sets to null if not set yet
			int cnt=0;
			
			if (goCol!=null) {
				int offset = nStatic+nEvC;
				String [] list = goCol.split("\t");	
				
				for (int i=0; i<list.length; i++) {
					int x = Static.getInteger(list[i]);
					if (x<0 || x>=totalColumns) continue; 
					
					cnt++;
					if (x<nStatic) 		chkStaticColumns[x].setSelected(true);
					else if (x<offset) 	chkEvCColumns[x-nStatic].setSelected(true);
					else 				chkPvalColumns[x-offset].setSelected(true);
				}
			}
			if (cnt==0) {
				chkStaticColumns[GOindex].setSelected(true);
				chkStaticColumns[GOdomain].setSelected(true);
				chkStaticColumns[GOdesc].setSelected(true);
				chkStaticColumns[GOdomain].setSelected(true);
			}
			// CAS324 initialized in selectedCol = new boolean [totalColumns];
			saveColumns(); // sets selectedCol
	    }
	    int totalColumns=0, nStatic=0, nPval=0, nEvC=0;
	    private String prefLabel = "";
		private Preferences prefsRoot = null;
		
		private JCheckBox [] chkStaticColumns = null, chkPvalColumns = null, chkEvCColumns = null;
		private JCheckBox chkSelectAllGen = null, chkSelectAllPvals = null, chkSelectAllECs = null;

	} // End ColumnPanel
	/*******************************************************
	 * XXX table
	 */
	private class GoTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -314853582139888170L;

		public GoTableModel() {}
		
	/* required */
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}
		public int getColumnCount() {
			int numCols = 0;
			for(int index = 0; index<selectedCol.length; index++) {
				if(selectedCol[index]) numCols++;
			}
			return numCols;
		}
		public int getRowCount() {
			return theResults.size();
		}
		// Mapped from Results to Displayed table; row and col relative to displayed table
		public Object getValueAt(int row, int col) {
			int index = getMappedColumn(col);
			
			if (index < endStatic) {
				if(index == GOindex) {
					Integer go = (Integer)theResults.get(row)[GOindex];
					return String.format(GO_FORMAT, go);
				}
				if (COL_NAMES[index].equals(EVALUE)) {
					double eval = (Double) theResults.get(row)[index];
					return new DisplayFloat(eval);
				}
				return theResults.get(row)[index];
			}
			
			if (index >= endStatic && index < endEvC) { // CAS323 
				String result = ((((String)theResults.get(row)[index]))).trim();
				if (isEvCcolor) {
					if (result.equals("")) return "";
					else return "Yes";
				}
				else return result;
			}
			if(index >= endEvC) { 
				double theVal = ((((Double)theResults.get(row)[index]).doubleValue()));
				return new DisplayFloat(theVal);
			}
			return "??";
		}
	/* not required by AbstractTableModel */
		// Called by Jtable
		public String getColumnName(int columnIndex) { // this is original order
			int index = getMappedColumn(columnIndex);			
			
			if (index < endStatic) 			return COL_NAMES[index];
			if (index < endEvC)          	return evColNames[index - endStatic];
			if (pvalColNames.length > 0)  	return pvalColNames[index - endEvC];
			
			Out.PrtWarn("Bad column:" + index + " diff:" + (index - COL_NAMES.length));
			return "NA";
		}
		
		// Takes the column number on the visible table, which may have some hidden columns,
		// and maps it to the column number that would be seen on the table with *all* columns showing
		private int getMappedColumn(int col) {
			int counter = -1;
			for(int x=0; x<selectedCol.length; x++) {
				if(selectedCol[x]) counter++;
				if(counter == col) return x;
			}
			Out.PrtError("\nMappedColumn Error: col index " + col + ", #selected columns=" 
						+ counter + " len=" + selectedCol.length);
			return -1;
		}
		
		/***********************************************
		 * Sort and display
		 */
 	  	public class ColumnListener extends MouseAdapter {
		    protected JTable table;

		    public ColumnListener(JTable t) {
		    	table = t;
		    	bAscend = new boolean[selectedCol.length];
		    	for(int x=0; x<bAscend.length; x++)
		    		bAscend[x] = true;   
		    }

		    // invoked on mouseClick and mouseRelease - detects column being moved (if needed)
  	  		public void mouseReleased(MouseEvent e) {}
  	  		
		    public void mouseClicked(MouseEvent e) {
		    	sortTable(e.getX(), SwingUtilities.isLeftMouseButton(e));
		    	
		    	theTableModel.fireTableDataChanged();
		    }
		 	   
  	  		private void sortTable(int xLocation, boolean leftClick) {
  	  			TableColumnModel colModel = table.getColumnModel();
  	  			int columnModelIndex = (colModel.getColumnIndexAtX(xLocation));
  	  			if (columnModelIndex<0) return;  
  	  			
  	  			int modelIndex = (colModel.getColumn(columnModelIndex).getModelIndex());
  	  
  	  			if (modelIndex < 0) return; 
  	  		
  	  			if(leftClick)
   	  				bAscend[modelIndex] = !bAscend[modelIndex];
  	  			else
  	  				bAscend[modelIndex] = true;
  	  			
   	  			sort(modelIndex, bAscend[modelIndex]);  
   	  			
   	  			for (int row=0; row<theResults.size(); row++) // CAS336  - row gets sorted too
   	  				theResults.get(row)[GOrow] = (row+1);
  	  		}
  	  		
  	  		private void sort(final int sortColumn, final boolean ascend) {
  	  			Collections.sort(theResults, new Comparator<Object []>() {

				public int compare(Object[] arg0, Object[] arg1) {
					int sign = 1;
					if(!ascend) sign = -1;
	  	  			int absCol = getMappedColumn(sortColumn); 
	  	  			if (absCol==0) return 0;		// row#
	  	  			
		  	  		if (absCol >= endStatic && absCol<endEvC) { //evidence code
		  	  			return sign * ((String)arg0[absCol]).compareTo((String)arg1[absCol]);
	  				}
	  				if(absCol >= endEvC) { // P-value
	  	  				return sign * ((Double)arg0[absCol]).compareTo((Double)arg1[absCol]);
	  				}
	  	  			
	  				Class<?> theType = COL_TYPES[absCol];
	  	  			if(theType == String.class)
	  	  				return sign * ((String)arg0[absCol]).compareTo((String)arg1[absCol]);
	  	  			if(theType == Integer.class)
	  	  				return sign * ((Integer)arg0[absCol]).compareTo((Integer)arg1[absCol]);
	  	  			if(theType == Double.class)
	  	  				return sign * ((Double)arg0[absCol]).compareTo((Double)arg1[absCol]);
	  	  			if(theType == Long.class)
	  	  				return sign * ((Long)arg0[absCol]).compareTo((Long)arg1[absCol]);
					return 0;
				}
  	  			});
  	  		}
  	  	}
 	  	private boolean [] bAscend = null;
	}
	/*************************************
	 * XXX Table Panel
	 */
	private class TablePanel extends JPanel
	{
		private static final long serialVersionUID = -3195592554322606106L;
		public TablePanel () {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			
			theTableModel = new GoTableModel();
			theTable = new JTable(theTableModel) {
				private static final long serialVersionUID = -1559706452814091003L;

				public Component prepareRenderer(
				        TableCellRenderer renderer, int row, int column)
			    {
			        Component c = super.prepareRenderer(renderer, row, column);
			       
				    if (theTable.isRowSelected(row)) 	{
				    	c.setBackground(selectColor);
				    }
				    else if (highSet.size()>0) {
				    	int gonum = (Integer) theResults.get(row)[GOindex];
			    		if (highSet.contains(gonum)) c.setBackground(HIGHCOLOR);
			    		else 						 c.setBackground(BGCOLOR);
				    }
				    else {
				    	boolean bBlueBG = ((row % 2) == 1);
				    	if ( bBlueBG ) 	c.setBackground( altRowColor );
						else            c.setBackground( null );
				    }
				    /* XXX CAS322 added, CAS324 fixed so works with moving columns */
				    if (DisplayDecimalTab.isHighPval()) {
				    	String displayCol = theTable.getColumnName(column);
				    	for (String cn : pvalColNames) {
				    		if (cn.contentEquals(displayCol)) {
				    			Object obj = theTable.getValueAt(row,column);
				    			
						    	double theVal=0.0;
					        	if (obj instanceof DisplayFloat) {
									theVal = ((DisplayFloat) obj).getValue();
									
									Color high = DisplayDecimalTab.getPvalColor(theVal);
						        	if (high!=null) c.setBackground(high);
								}
								else 
									Out.prt("Cannot read table obj " + obj.toString());
					        	
					        	break;
				    		}
				    	}
				    }
				    if (isEvCcolor) {
				    	String displayCol = theTable.getColumnName(column);
				    	for (String cn : evColNames) {
				    		if (cn.contentEquals(displayCol)) {
				    			Object obj = theTable.getValueAt(row,column);
								String val = ((String) obj);
								if (val.contentEquals("Yes")) c.setBackground(EvCOLOR);
								break;
				    		}
				    	}
				    }
			        return c;
			    }
			};
			theTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			theTable.setCellSelectionEnabled(false);
			theTable.setRowSelectionAllowed(true);
			theTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); 
			//theTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION); //  can only select one
			theTable.setDefaultRenderer( Object.class, new BorderLessTableCellRenderer() );
			theTable.setDefaultRenderer( Object.class, new BorderLessTableCellRenderer() );
			theTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent arg0) {
					enableAllButtons();
				}
			});

			JTableHeader header = theTable.getTableHeader();
			header.setUpdateTableInRealTime(true);
			colListener = theTableModel.new ColumnListener(theTable);
			header.addMouseListener(colListener);

			theTable.getTableHeader().setBackground(Color.WHITE);
			tableScroll = new JScrollPane(theTable);
			
			add(tableScroll);
			add(createLowButtonPanel());
		}
		 /*********************
		  *  BOTTOM BUTTONS - 
		  ***/
		 private JPanel createLowButtonPanel() {
			lblHeader = new JLabel("Modify");
			
			btnDelete = Static.createButtonPlain("Delete selected", false);
			btnDelete.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteFromList();
					tableRefresh();
				}
			});
			btnKeep = Static.createButtonPlain("Keep selected", false);
			btnKeep.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					keepFromList();
					tableRefresh();
				}
			});
			btnUnselectAll = Static.createButtonPlain("Unselect All", false);
			btnUnselectAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					theTable.clearSelection();
				}
			});
			btnSelectAll = Static.createButtonPlain("Select All", false);
			btnSelectAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					theTable.selectAll();
				}
			});
			btnQuery = Static.createButtonPlain("Select Query", false); // CAS336 new
			btnQuery.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					theTable.clearSelection();
					filterPanel.loadQueryStart(BasicGOLoadFromDB.SELECT);
				}
			});	
			btnHighSelect = Static.createButtonPlain("Highlight", false); // CAS336 new
			btnHighSelect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					highSelect();
					theTable.clearSelection();
				}
			});
			btnHighClear = Static.createButtonPlain("Clear", false); // CAS336 new
			btnHighClear.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					highClear();
				}
			});
			
			JPanel bottomPanel = Static.createRowPanel();
			
			bottomPanel.add(lblHeader);		bottomPanel.add(Box.createHorizontalStrut(3));
			bottomPanel.add(btnDelete);		bottomPanel.add(Box.createHorizontalStrut(2));
			bottomPanel.add(btnKeep);		bottomPanel.add(Box.createHorizontalStrut(10));
			bottomPanel.add(btnSelectAll);	bottomPanel.add(Box.createHorizontalStrut(2));
			bottomPanel.add(btnUnselectAll);bottomPanel.add(Box.createHorizontalStrut(2));
			bottomPanel.add(btnQuery);		bottomPanel.add(Box.createHorizontalStrut(10));
			bottomPanel.add(btnHighSelect); bottomPanel.add(Box.createHorizontalStrut(2));
			bottomPanel.add(btnHighClear);
			
			bottomPanel.add(Box.createHorizontalGlue());
			bottomPanel.setMaximumSize(bottomPanel.getPreferredSize());
			return bottomPanel;
		}
		// CAS336 adds in selected to highSet, which is highlighted on display. Clear 
		private void highSelect() {
			int [] selRow = theTable.getSelectedRows();	
			
			int cntOrig = highSet.size(), cntNew=0;
			for(int x=0; x<selRow.length; x++)  {
				int gonum = (Integer) theResults.get(selRow[x])[GOindex];
				if (!highSet.contains(gonum)) {
					highSet.add(gonum);
					cntNew++;
				}
			}
			String msg = (cntOrig==0) ? "Highlight " + highSet.size() : "Highlight " + cntOrig + "+" + cntNew;
			
			filterPanel.appendStatus(msg);
		}
		private void keepFromList() {
			int numElements = theTable.getRowCount();
			int [] selValues = theTable.getSelectedRows();		

			int [] opposite = new int[numElements - selValues.length];
			int x=0, selPos=0, nextval=0;
			
			for(x=0; x<opposite.length; x++) {
				while(selPos<selValues.length && selValues[selPos] == nextval) {
					selPos++;
					nextval++;
				}
				opposite[x] = nextval++;
			}
			deleteFromList(opposite);
		}
			
		private void deleteFromList(){
			int [] selValues = theTable.getSelectedRows();
			deleteFromList(selValues);
		}
		private void deleteFromList(int [] sels){
			if (sels == null || sels.length==0) return;
			
			theTable.clearSelection();
			for(int x=sels.length-1; x>=0; x--) {	
				theResults.remove(sels[x]);
			}
			
			theTableModel.fireTableDataChanged();
			filterPanel.deleteFinish(getRowCount());
		}
		public void tableRefresh() {
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						if(theTableModel != null) {
							JTableHeader header = theTable.getTableHeader();
							header.removeMouseListener(colListener);
						}
						theTableModel = new GoTableModel();
						theTable.setModel(theTableModel);
						tableResizeColumns();
						
						JTableHeader header = theTable.getTableHeader();
						header.setUpdateTableInRealTime(true);
						colListener = theTableModel.new ColumnListener(theTable);
						header.addMouseListener(colListener);
						theTable.getTableHeader().setBackground(Color.WHITE);
						theTable.setDefaultRenderer( Object.class, new BorderLessTableCellRenderer() );
						
						enableAllButtons();
					}
				}
			);
		}
		public void tableResizeColumns() {
			theTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			int margin = 2;
			
			for(int x=0; x < theTable.getColumnCount(); x++) {
				DefaultTableColumnModel colModel = (DefaultTableColumnModel) 
					theTable.getColumnModel();
				TableColumn col = colModel.getColumn(x);
				int maxSize = 0;
				
				TableCellRenderer renderer = col.getHeaderRenderer();
				
				if(renderer == null)
					renderer = theTable.getTableHeader().getDefaultRenderer();
				
				Component comp = renderer.getTableCellRendererComponent(theTable, 
						col.getHeaderValue(), false, false, 0, 0);
				maxSize = comp.getPreferredSize().width;
				
				for(int y=0; y<theTable.getRowCount(); y++) {
					renderer = theTable.getCellRenderer(y, x);
					comp = renderer.getTableCellRendererComponent(theTable, 
							theTable.getValueAt(y, x), false, false, y, x);
					maxSize = Math.max(maxSize, comp.getPreferredSize().width);
				}
				maxSize += 2 * margin;
				
				col.setPreferredWidth(Math.min(maxSize, MAX_COL));
			}
			((DefaultTableCellRenderer) 
					theTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
		}
		private class BorderLessTableCellRenderer extends DefaultTableCellRenderer {
		    private static final long serialVersionUID = 1L;
		    public Component getTableCellRendererComponent(
		            final JTable table, final Object value,
		            final boolean isSelected, final boolean hasFocus,
		            final int row, final int col) 
		    {
		        final boolean showFocusedCellBorder = false; // change this to see the behavior change

		        final Component c = super.getTableCellRendererComponent(
		                table,
		                value,
		                isSelected,
		                //showFocusedCellBorder && hasFocus, // always false
		                showFocusedCellBorder, 
		                row,
		                col
		        );
		        return c;
		    }
		}
		
		private JLabel lblHeader = null;
	
		private GoTableModel.ColumnListener colListener = null;
		private JScrollPane tableScroll = null;
	} // end TablePanel
	
	/********************************************
	 * Public
	 */
	public void clear() {
		theResults.clear();
		highSet.clear();
		theTableModel.fireTableDataChanged();
	}
	
	/**********************************************************
	 * building table 
	 */
	public void addResult(Object [] newRow) {
		theResults.add(newRow);	
	}
	
	public Vector<Object []> getTheResults() {return theResults;} // used by Trim
	
	public void showColumns() { // Called when Column button selected
		columnPanel.setupVisible();
	}
	public ColumnPanel getColumnPanel() { return columnPanel;}
	public TablePanel getTablePanel() { return tablePanel;}
	
	public int [] getSelectedGOnums() {
		int [] sels = theTable.getSelectedRows();
		int [] gonums = new int[sels.length];
		for(int x=0; x<sels.length; x++) {
			gonums[x] = (Integer) theResults.get(sels[x])[GOindex]; //CAS336 Results instead of theTableModel
		}
		return gonums;
	}
	public String getSelectedGOid() {
		int [] sels = theTable.getSelectedRows();
		if (sels!=null && sels.length>0) {
			int gonum = (Integer) theResults.get(sels[0])[GOindex];//CAS336 Results instead of theTableModel
			return String.format(GO_FORMAT, gonum);
		}
		return null;
	}
	public String getSelectedGOdesc() {
		int [] sels = theTable.getSelectedRows();
		if (sels!=null && sels.length>0) {
			String desc =  (String) theResults.get(sels[0])[GOdesc];
			return desc; 
		}
		return null;
	}
	
	public int getSelectedRowCount() 		{return theTable.getSelectedRowCount();}
	public int getRowCount() 				{return theTable.getRowCount();}
	
	// Static table
	public int getNumStaticCols() 			{return COL_NAMES.length;}
	public Class<?> [] getColumnTypes() 	{return COL_TYPES;}
	public int getEndEvC() 					{return endEvC;}		// load all columns
	
	public void tableRefresh() { // CAS324 was in trim's buildRowMap
		tablePanel.tableRefresh();
	}
	public boolean changeNSeq(int row, int cnt, int cutoff) {
		if (cnt==0 || cnt<cutoff) {
			theResults.remove(row);
			return true;
		}
		
		theResults.get(row)[GOnSeq] = cnt;
		return false; // did not remove
	}
	public String getColumns() {
		String theQuery = "SELECT " + COL_MYSQL[GOindex];
		
		for(int x=1; x<COL_MYSQL.length; x++) {
			if(COL_MYSQL[x].length() == 0) theQuery += ", NULL";
			else  theQuery += ", " + COL_MYSQL[x];
		}
		for (String ec : evColNames)
			theQuery += ", go_info." + Globalx.GO_EvC + ec; 
		
		for(int x=0; x<pvalColNames.length; x++)
			theQuery += ", go_info." + Globalx.PVALUE + pvalColNames[x];
		
		theQuery += " FROM go_info ";
		return theQuery;
	}
	/*******************************************************************
	 * XXX  Show and Table buttons to end of file
	 */
	public void tableExport(Component btnC, int type) {
		new ExportGO().run(btnC, type);
	}
	// CAS324 change theTableModel to theTable
	public String tableCopyString(String delim) {
 		StringBuilder retVal = new StringBuilder();
 	
 		retVal.append("#"); // CAS334 column headings is a comment
		for(int x=0; x<theTable.getColumnCount()-1; x++) {
			retVal.append(theTable.getColumnName(x));
			retVal.append(delim);
		}	
		retVal.append(theTable.getColumnName(theTable.getColumnCount()-1));
		retVal.append("\n");
		
		for(int x=0; x<theTable.getRowCount(); x++) {
			StringBuilder row = new StringBuilder();
			for(int y=0; y<theTable.getColumnCount()-1; y++) {
				row.append(theTable.getValueAt(x, y));
				row.append(delim);
			}
			retVal.append(row.toString());
			retVal.append(theTable.getValueAt(x, theTable.getColumnCount()-1));
			retVal.append("\n");
		}
 		return retVal.toString();
	 }
	// XXX CAS336 for automatic select
	public void selectRows(String msg, HashSet <Integer> rowSet) {
		int cnt=0;	
		for(int x=0; x<theTable.getRowCount(); x++)  {
			int gonum = (Integer) theResults.get(x)[GOindex];
			
			if (rowSet.contains(gonum)) {
				theTable.addRowSelectionInterval(x, x);
				cnt++;
			}
		}
		filterPanel.appendStatus(String.format("%s: %,d", msg, cnt));
	}
	public void selectRelatedFromTable(int type) { // XXX CAS336
		try {
			int [] sels = theTable.getSelectedRows();
			if (sels.length==0) {
				JOptionPane.showMessageDialog(null, "No selected GO ");
				return;		
			}
			int gonum = (Integer)  theResults.get(sels[0])[GOindex];
			
			int [] goIDs = new int[theTable.getRowCount()];
			for(int x=0; x<goIDs.length; x++) {
				goIDs[x] = (Integer) theResults.get(x)[GOindex];
			}
			HashSet <Integer> inTab = new GOtree(theMainFrame).computeRelatedForSet(gonum, goIDs, type);
			
			int cnt=0;
			for(int x=0; x<goIDs.length; x++)  {
				if (inTab.contains(goIDs[x])) {
					theTable.addRowSelectionInterval(x, x);
					cnt++;
				}
			}
			filterPanel.appendStatus("Select " + cnt + " for " + String.format(GO_FORMAT, gonum));
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Hits for Selected"); }
	}
	
	public void showRelatedFromTable(int mode, JButton btnShow) {
		try {
			int [] sels = theTable.getSelectedRows();
			if (sels.length==0) {
				JOptionPane.showMessageDialog(null, "No selected GO ");
				return;		
			}
			int gonum = (Integer)  theResults.get(sels[0])[GOindex];
			String desc = (String) theResults.get(sels[0])[GOdesc];
			
			int [] allGonums = new int[theTable.getRowCount()];
			for(int x=0; x<allGonums.length; x++) {
				allGonums[x] = (Integer) theResults.get(x)[GOindex];
			}
			new GOtree(theMainFrame).goRelatedPopup(gonum, desc, allGonums, mode, btnShow, this);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Hits for Selected"); }
	}
	
	public String getRowHeading() {
		return "#Seqs";
	}
	
	/************ GOtree options  ***************/
	public void showExportGOtreeSelected(int actionType, int outType, JButton btnFrom) {
		try {
			int [] sels = theTable.getSelectedRows();
			if (sels.length==0) {
				JOptionPane.showMessageDialog(null, "No selected GO ");
				return;		
			}
			int gonum =  (Integer) theResults.get(sels[0])[GOindex];
			String desc = (String) theResults.get(sels[0])[GOdesc];
			
			new GOtree(theMainFrame).computeSelected(gonum, desc, actionType, outType, btnFrom);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Hits for Selected"); }
	}
	
	/*********** GO Lists/Paths ****************/
	public void showExportGOtreeTable(Component c, int type, int mode) {
		try {	
			TreeSet <Integer> goMap = new TreeSet <Integer>  ();
			for (Object [] o : theResults) {
				int gonum = (Integer) o[GOindex];
				goMap.add(gonum);
			}
			boolean path = (type==GOtree.LONGEST_PATHS || type==GOtree.ALL_PATHS);
			
			if (path) { // Request info: GO name or GO id; FileName if Export; .tsv or html
				if (goMap.size()>100) {
					String msg = "This feature is only recommend for < 100 GOs.\n";
					msg += 		 "The table can get big and slow to produce (if really large >5 minutes).";
					if (!UserPrompt.showContinue(">100 Paths", msg)) return;
				}
				new GOtree(theMainFrame).goPopupExportPath(c, type, mode, goMap);
			}
			else { // FileName if Export
				new GOtree(theMainFrame).goPopupExportList(c, type, mode, goMap);
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Paths for Selected");}
	}
	/***************************************************************
	 * Export Column
	 */
	private class ExportColumn extends JDialog {
		private static final long serialVersionUID = 1L;
		public ExportColumn(String tcwid) {
    		setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		setTitle("Column heading.... ");
        
    		JPanel page = Static.createPagePanel();
	        page.add(Static.createLabel("Column heading for #Seqs"));
	        page.add(Box.createVerticalStrut(10));
	       
	        JPanel row = Static.createRowPanel();
			row.add(Static.createLabel("Column:"));
	        txtColName  = new JTextField(10);
	        txtColName.setMaximumSize(txtColName.getPreferredSize());
	        txtColName.setText(tcwid);
	        row.add(txtColName);
	        page.add(row);
				    
	        page.add(new JSeparator());
	        
	        // bottom buttons
	        row = Static.createRowPanel();
    		btnOK = new JButton("OK");
    		btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode=1;
					setVisible(false);
				}
			});
    		row.add(btnOK);
    		row.add(Box.createHorizontalStrut(10));
    		btnCancel = new JButton("Cancel");
    		btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode=0;
					setVisible(false);
				}
			});
    		
    		btnOK.setPreferredSize(btnCancel.getPreferredSize());
    		btnOK.setMaximumSize(btnCancel.getPreferredSize());
    		btnOK.setMinimumSize(btnCancel.getPreferredSize());
    		row.add(btnCancel);
    		page.add(row);
    		
    		page.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    		add(page);
    		
    		pack();
    		this.setResizable(false);
    		UIHelpers.centerScreen(this);
    	}
		public String getColumn() {return txtColName.getText();}
		public int getMode() {return nMode;}
		
		private int nMode;
		private JButton btnOK, btnCancel;
		private JTextField txtColName;
	}
	/***********************************************************
	 * Export GO - Table  Export only
	 */
	private class ExportGO {
		private final String filePrefix="GOtable";
		public ExportGO() {}
		
		public boolean run(Component btnC, int type) {
			if (type==4) {
				exportGOids(btnC);
				return true;
			}
			tcwid = theMainFrame.getdbID();
			String msg, title, file;
			int wtype=FileC.wAPPEND;
			if (type==0) {
				msg="Append";
				title=" columns of table ";
				file = filePrefix+"Columns_" + projName + FileC.TSV_SUFFIX;
			}
			else if (type==5) {
				msg="Append";
				title=" columns of table with log10(Pval)";
				file = filePrefix+"ColPval_"+ projName  + FileC.TSV_SUFFIX;
			}
			else if (type==1){
				msg="Append";
				title= " SeqIDs with GOs ";
				file = filePrefix+"BySeq_"+ projName + FileC.TSV_SUFFIX;
			}
			else if (type==2) {
				wtype=FileC.wMERGE;
				msg="Merge";
				title = " #Seqs Column ";
				file = filePrefix+"NSeqs_" + projName  + FileC.TSV_SUFFIX;
			}
			else return false;
			
			FileWrite fw = new FileWrite(FileC.bNoVer, FileC.bDoPrt);
			File out = fw.run(btnC, file, FileC.fTSV, wtype);
			
			if (out==null) return false;
				
			boolean bAppend = fw.isAppend();
			if (bAppend) Out.PrtSpMsg(0, msg + title +  out.getAbsolutePath());
			else         Out.PrtSpMsg(0, "Write " + title +  out.getAbsolutePath());
			
			if (type==0) 		return exportTableColumns(bAppend, out, false);
			else if (type==5) 	return exportTableColumns(bAppend, out, true);
			else if (type==1)	return exportSeqIDwithGOs(bAppend, out);
			else if (type==2)	return exportDomainMerge(bAppend, out); 
			else return false;
		}
		
		// CAS324 changed theTableModel to theTable - outputs the table order
		// CAS326 add bLog
		private boolean exportTableColumns(boolean bAppend, File out, boolean bLog) {
			try {
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out, bAppend)));
				headerLine = String.format("### %s %d GOs   %s ", 
						tcwid, getRowCount(), filterPanel.getBuildFilter());
				pw.println(headerLine);
				
				StringBuilder line = new StringBuilder();
	
				int colCnt = theTable.getColumnCount();
				int rowCnt = getRowCount();
				Out.prtSp(1, "Processing " + rowCnt + " rows and " + colCnt + " columns...");
				
				// Column headings
				line.append("#"); // CAS334 column headings are a comment
				boolean [] isDE = new boolean [theTable.getColumnCount()];
				for(int x=0; x<theTable.getColumnCount(); x++) {
					String colName = theTable.getColumnName(x).replaceAll("\\s", "-"); 
					line.append(colName);
					if (x!=theTable.getColumnCount()-1) 
						line.append(Globalx.TSV_DELIM);
					isDE[x] = false;
					
					if (bLog) {
						String displayCol = theTable.getColumnName(x);
					    for (String cn : pvalColNames) {
					    	if (displayCol.equals(cn)) {
					    		isDE[x] = true;
					    		break;
					    	}
						}
					}
				}	
				pw.println(line.toString());
			
				// rest of table
				int x, y, cnt=0, nCol=theTable.getColumnCount();
				String val = "";
				for(x=0; x<getRowCount(); x++) {
					line.delete(0, line.length());
					
					for(y=0; y<nCol; y++) {
						if(theTable.getValueAt(x, y) != null) 
							val = theTable.getValueAt(x, y).toString();
						else val = "";
						
						if (isDE[y]) {
							try {
								double d = Double.parseDouble(val);
								double l = -Math.log10(d);
								val = l+"";
							}
							catch (Exception e) {Out.prt("Not double " + val);}
						}
						line.append(val);
						if (y!=nCol-1) line.append(Globalx.TSV_DELIM);
					}
					pw.println(line.toString());
					cnt++;
				}
				pw.close();
				Out.PrtSpMsg(0, "Complete writing " + cnt + " lines");
				return true;
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Exporting GO table");}
			return false;
		}
		
		private boolean exportSeqIDwithGOs(boolean bAppend, File out) {
			try {
				int rowCnt = getRowCount();
				Out.PrtSpCntMsg(1, rowCnt, "rows to process");
				
				String where = filterPanel.makeNseqClause(false); 
				if (where!=null && where.length()>0) where = " and " + where;
				HashMap <Integer, String> seqID = new HashMap <Integer, String> ();
				DBConn mDB = theMainFrame.getNewDBC();
				
				ResultSet rs = mDB.executeQuery("select CTGid, contigid from contig as c " +
						"where PIDgo>0" + where);
				while (rs.next()) {
					seqID.put(rs.getInt(1), rs.getString(2));
				}
				Out.PrtSpCntMsg(1, seqID.size(), "sequences with at least one GO");
				int cnt=0, cntGO=0;
				HashMap <Integer, Vector<Integer>> seqGO = new HashMap <Integer, Vector<Integer>> ();
				String query = "select CTGID from pja_unitrans_go where gonum=";
				for(int row=0; row<rowCnt; row++) {
					int gonum = (Integer) theResults.get(row)[GOindex]; // CAS336 was theTableModel
					
					rs = mDB.executeQuery(query + gonum);
					while (rs.next()) {
						int ctgid = rs.getInt(1);
						if (seqID.containsKey(ctgid)) {
							Vector <Integer> list;
							if (seqGO.containsKey(ctgid)) list = seqGO.get(ctgid);
							else list = new Vector <Integer> ();
							list.add(gonum);
							seqGO.put(ctgid, list);
						}
						cnt++;
					}
					rs.close(); 
					if (cnt%1000==0) Out.r("Processed " + cnt);
				}
				mDB.close();
				
				cnt=cntGO=0;
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out, bAppend)));
				StringBuilder line = new StringBuilder();
				String goFormat = Globalx.GO_FORMAT;
				
				headerLine = String.format("!!! %s %d Seqs   %s", tcwid, seqGO.size(), filterPanel.getBuildFilter());
				pw.println(headerLine);
				
				for (int id : seqGO.keySet()) {
					line.delete(0, line.length());
					line.append(seqID.get(id));
					Vector <Integer> list = seqGO.get(id);
					for (int gonum : list) {
						String goStr = String.format(goFormat, gonum);
						line.append("\t" + goStr);
						cntGO++;
					}
					pw.println(line.toString());
					cnt++;
				}
				pw.close();
				Out.PrtSpMsg(0, "Complete writing " + cnt + " sequences with " + cntGO + " GOs");
				return true;
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Exporting GO table");}
			return false;
		}
		// CAS324 add GO ids - use theTableModel so no worry about GO column being moved; CAS336 - theResults
		private boolean exportGOids(Component btnC) {
			try {
				FileWrite fw = new FileWrite(FileC.bNoVer, FileC.bDoPrt);
				PrintWriter pw = fw.getWriter(btnC, "Basic GOs", "GOtableIDs", FileC.fTXT, FileC.wAPPEND);
	         	if (pw==null) return false;
				
				for(int x=0; x< theTableModel.getRowCount(); x++) { 
					int gonum = (Integer) theResults.get(x)[GOindex];
					pw.write(String.format(GO_FORMAT, gonum) + "\n");
				}
				pw.close();
				
				return true;
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Exporting GO IDs");}
			return false;
		}
		/*********************************************************
		 * Get from table: GO-term Domain Description #Seqs
		 * #Seqs is renamed by User
		 */
		private boolean exportDomainMerge(boolean bInMerge, File Infh) {
			try {
				String de = filterPanel.selectOneDE();
				String name = (de!=null) ? de : tcwid;
				final ExportColumn et = new ExportColumn(name);
				et.setVisible(true);
				final int saveMode = et.getMode();
				if(saveMode == 0) return false;
				
				final String colHead=et.getColumn();
				final boolean bMerge=bInMerge;
				final File fh = Infh;
			
				Thread theThread = new Thread(new Runnable() {
					public void run() {
						try {
							isSeq=true;
							
							if (bMerge) domainParseFile(fh);
						
							columnLine += "\t" + colHead;
							
							int nSeqs = theMainFrame.getMetaData().nContigs();
							headerLine += String.format(
									"### %-6s %6d  Column: %-6s  %s ", 
									tcwid, nSeqs, colHead, filterPanel.getBuildFilter());
							
							domainAddTable();
							domainWriteFile(fh);
						}
						catch(Exception e) {ErrorReport.prtReport(e, "Error creating export file");}
					}
				});
				theThread.start();
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Exporting GO table");}
			return false;
		}
		private boolean domainAddTable() {//Export/Merge #Seq, to use P-value column
			try {
				Out.PrtSpMsg(1, "Add GOs from table");
				int j=nCol-1, nGO=0;
				
				double score=0.0;
				int count=0;
				
				for (int row=0; row<getRowCount(); row++) {
					String domain = (String)  theResults.get(row)[GOdomain];
					String desc = 	(String)  theResults.get(row)[GOdesc];
					if (isSeq) count = 	(Integer) theResults.get(row)[GOnSeq];
					else score =  ((((Double)theResults.get(row)[colIdx]).doubleValue()));
					
					String [] abbr = Globalx.GO_TERM_ABBR;
					if      (domain.equals(abbr[0])) domainAdd(biolMap, desc, score, count, j);
	    			else if (domain.equals(abbr[1])) domainAdd(cellMap, desc, score, count, j);
	    			else if (domain.equals(abbr[2])) domainAdd(moleMap, desc, score, count, j);
	    			else Out.PrtWarn("Invalid " + domain + " "  + abbr[0]);
					nGO++;
				}
				Out.PrtSpMsg(2,"Table Total GOs: " + nGO);
				return true;
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Creating current info"); return false;}
		}
		private boolean domainParseFile(File f) {
			String line="";
			try {
				Out.PrtSpMsg(1, "Read " + f.getName());
				BufferedReader reader = new BufferedReader ( new FileReader(f));
				
				String last="";
				int nGO=0, nCnts=0;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("#")) {
						headerLine += line + "\n";
						continue;
					}
	    			String [] tok = line.split("\t");
	    			if (tok[0].equals(Globalx.goOnt)) {
	    				columnLine = line;
	    				nCol = (tok.length-startNseq)+1;
	    				continue;
	    			}
	    			double score=0.0;
	    			int count=0;
	    			
	    			String domain = tok[0];
	    			if (domain.trim().equals("")) domain=last;
	    			else last=domain;
	    			
	    			String desc = tok[1];
	    			for (int i=startNseq, j=0; i<tok.length; i++, j++) {
		    			if (isSeq) {
		    				count = Static.getInteger((tok[i].trim()));
		    				if (count<0) {
		    					reader.close();
		    					return prtError("Incorrect #Seqs: " + tok[i]);
		    				}
		    			}
		    			else { // for DE
		    				score = Static.getDouble(tok[i].trim());
		    				if (score<0) {
		    					reader.close();
		    					return prtError("Incorrect p-value: " + tok[i]);
		    				}
		    			}
		    			String [] list = Globalx.GO_TERM_LIST;
		    			if (domain.equals(list[0])) 	 domainAdd(biolMap, desc, score, count, j);
		    			else if (domain.equals(list[1])) domainAdd(cellMap, desc, score, count, j);
		    			else if (domain.equals(list[2])) domainAdd(moleMap, desc, score, count, j);
		    			else {
		    				reader.close();
		    				return prtError("Bad domain: " + domain);
		    			}
		    			nCnts++;
	    			}
		    		nGO++;
				}
				Out.PrtSpMsg(2, "Read Total GOs: " + nGO + " Total counts: " + nCnts);
				reader.close();
				return true;
			}
			catch (Exception e) {ErrorReport.prtReport(e, "reading " + line); return false;}
		}
		
		private void domainAdd(TreeMap <String, Count> map, String desc, double score, int count, int index) {
			if (map.containsKey(desc)) {
				map.get(desc).score[index]=score;
				map.get(desc).count[index]=count;
			}
			else {
				Count c = new Count();
				for (int i=0; i<nCol; i++) {
					c.count[i] = 0;
					c.score[i] = 0.0;
				}
				c.score[index]=score;
				c.count[index]=count;
				map.put(desc, c);
			}
		}
		private void domainWriteFile(File f) {
			try {
				Out.PrtSpMsg(1, "Start writing " + f.getName());
				outF = new PrintWriter(new FileWriter(f));
				outF.println(headerLine);
				outF.println(columnLine);
				String [] terms = Globalx.GO_TERM_LIST;
				domainWriteSet(biolMap, terms[0]);
				domainWriteSet(cellMap, terms[1]);
				domainWriteSet(moleMap, terms[2]);
				outF.close();
				Out.PrtSpMsg(0, "Write " + cntOut + " GOs to " + f.getName());
			}
			catch (Exception e) {e.printStackTrace();}
		}
		// This was written to take care of different scenerios, but only is being
		// used for writing GO followed by #Seqs, isSet=True and one column besides the GO
		private void domainWriteSet(TreeMap <String, Count> map, String domain) {
			String o;
			int cnt=0, sum=0;
			for (String desc : map.keySet()) {
				if (cnt==0) o = domain + "\t" + desc;
				else        o =          "\t" + desc;
				Count c = map.get(desc);
				  
				for (int i=0; i<nCol; i++) { // only one column for exportDomainGOnSeq
					if (isSeq) o += String.format("\t%d",  c.count[i]);
					else       o += String.format("\t%.0E",c.score[i]);
					sum += c.count[i];
				}
				outF.println(o);
				cntOut++;
				cnt++;
			}
			Out.PrtSpMsg(2, String.format("%-20s %3d %6d", domain, cnt, sum));
		}
		
		private boolean prtError(String msg) {
			Out.PrtError(msg);
			return false;
		}
		
		private class Count {
			int [] count = new int [nCol];
			double [] score = new double [nCol];
		}
		private int startNseq=2;
		private String tcwid = null, headerLine="", columnLine= Globalx.goOnt + "\t" + Globalx.goTerm;
		private int nCol=1;
		
		private PrintWriter outF;
		private TreeMap <String, Count> biolMap = new TreeMap <String, Count>();
		private TreeMap <String, Count> cellMap = new TreeMap <String, Count>();
		private TreeMap <String, Count> moleMap = new TreeMap <String, Count>();
		private int cntOut=0;
		
		private boolean isSeq=false;
		private int colIdx=-1; // column to print out
	}// End Class ExportGO
	/*************************************************************
	 * Copied from BasicTablePanel
	 */
	public void statsPopUp(final String title) {
		String info = statsPopUpCompute(title);
		UserPrompt.displayInfoMonoSpace(null, title, info);		
	}
	private String statsPopUpCompute(String sum) {
		try {
			String [] fields = 
				{"Column", "Average", "StdDev", "Median", "Range", "Low", "High", "Sum", "Count"};
			int [] justify =   {1,   0,  0, 0, 0, 0, 0, 0, 0};
			int intCol=3;
			
			int tableRows=theTable.getRowCount(), tableCols = theTable.getColumnCount();
			int nStat=  fields.length, nRow=0;
			
		    String [][] rows = new String[tableCols][nStat]; 
			double [] dArr = new double [tableRows];

		    for(int y=0; y < tableCols; y++) {
		    	String colName = theTable.getColumnName(y);
		    	if (colName.equals("Row")) continue;
		    		
			 	Object obj = theTable.getValueAt(0,y);
			 	if (obj==null) continue; 
				Class <?> dtype = obj.getClass();
				if (dtype==String.class) continue;
				
			 	for (int i=0; i<tableRows; i++) dArr[i]=Globalx.dNoVal;
			 	boolean isInt=false;
			 	
				for (int x=0, c=0; x<tableRows; x++) {
					if (theTable.getValueAt(x,y) == null) continue;
					
					obj = theTable.getValueAt(x,y);
					
					if (dtype == Integer.class) {
						int j = (Integer) theTable.getValueAt(x, y);
						dArr[c++] = (double) j;
						isInt=true;
					}
					else if (obj instanceof DisplayFloat) {
						dArr[c++] = ((DisplayFloat) obj).getValue();
					}
					else if (dtype == Float.class) {
						dArr[c++] = (Float)  theTable.getValueAt(x, y);
					}
					else if (dtype == Double.class) {
						dArr[c++] = (Double) theTable.getValueAt(x, y);
					}
					else { 
						Out.prtToErr("BasicTablePanel: class? " + (String) theTable.getValueAt(x, y) + " " + dtype);
					}
				}
				double [] results = Stats.averages(colName, dArr, false);
				rows[nRow][0] = colName;
				for (int i=0, c=1; i<results.length; i++, c++) {
					if ((i>=intCol && isInt) || (i==results.length-1)) {
						if (results[i]<0) rows[nRow][c] = "N/A"; // overflow
						else rows[nRow][c] = String.format("%,d", (long) results[i]);
					}
					else rows[nRow][c] = DisplayDecimalTab.formatDouble(results[i]);
				}
				nRow++;
			}
			String statStr = Out.makeTable(nStat, nRow, fields, justify, rows);
			statStr += "\n" + tableRows + " rows; " + sum;
			return statStr;
		
		} catch(Exception e) {ErrorReport.reportError(e, "Error create column stats"); return "Error"; }
	}

	public void enableLowButtons(boolean b) { // called when query starts
		btnDelete.setEnabled(b);
		btnKeep.setEnabled(b);
		btnUnselectAll.setEnabled(b);
		btnHighSelect.setEnabled(b);
		btnSelectAll.setEnabled(b);
		btnQuery.setEnabled(b);
		btnHighClear.setEnabled(b);
	}
	public void enableAllButtons() { // CAS336 new - called on select and tableRefresh
		boolean b = (theTable.getSelectedRows().length>0);
		btnDelete.setEnabled(b);
		btnKeep.setEnabled(b);
		
		btnUnselectAll.setEnabled(b);
		btnHighSelect.setEnabled(b);
		
		b = (theTable.getRowCount()>0);
		btnSelectAll.setEnabled(b);
		btnQuery.setEnabled(b);
		
		btnHighClear.setEnabled(highSet.size()>0);
		
		filterPanel.enableAllButtons();
	}
	public void highClear() {
		highSet.clear();
		filterPanel.appendStatus("");
		theTableModel.fireTableDataChanged();
	}
	/*****************************************************/
	// CAS336 changed everything to use theResults for static indices (at top)
	// Sorted rows: all theResults, theTable, theTableModel
	// Columns:     theResults has all columns. theTable and theTableModel have selectedCol
	//              theTable has columns re-arranged - it is what you see
	// Gonum/goID:  theResults has gonum. theTable and theTableModel have goID.
	// theTable is what is shown. theResults maintains the row indices (e.g. GOindex). 
	private boolean []       selectedCol = null;	// full set - true are displayed
	private Vector<Object []> theResults = null;    
		
	private JTable theTable = null;				// exactly what is displayed
	private GoTableModel theTableModel = null;  // what is displayed but columns not rearranged
	
	private TablePanel tablePanel = null;
	private ColumnPanel columnPanel = null;
	
	private STCWFrame theMainFrame;
	private BasicGOFilterTab filterPanel;
	private boolean isEvCcolor=true;
	private String projName="";
	
	// created in TablePanel
	private JButton btnDelete = null, btnKeep = null;
	private JButton btnUnselectAll = null, btnSelectAll = null, btnQuery=null;
	private JButton btnHighSelect=null, btnHighClear=null;
	
	private HashSet <Integer> highSet = new HashSet <Integer> ();
}
