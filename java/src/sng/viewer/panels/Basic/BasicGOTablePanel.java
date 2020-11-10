package sng.viewer.panels.Basic;

/*************************************************************
 * Creates the Table and 
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

import sng.util.ExportFile;
import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.GOtree;
import util.methods.Static;
import util.methods.Out;
import util.methods.Stats;
import util.ui.DisplayFloat;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class BasicGOTablePanel {
	private static final Color BGCOLOR = Globalx.BGCOLOR;
	private static final Color DETRIMCOLOR = new Color(229, 245, 237);
	private static final String GO_FORMAT = Globalx.GO_FORMAT;
	private static final int MAX_COL = 300;
	
	public static final int GOindex=0;
	public static final int GOdomain=1;
	private static final int GOdesc=3;
	public static final int GOnSeq=5;
	private static final String goPref= "_goPrefs";
	private final String EVALUE = "Best E-val";
	
	private  final Class<?> [] COL_TYPES = 
	 {Integer.class,  String.class, Integer.class, String.class, Double.class, Integer.class }; 
	private  final String [] COL_MYSQL = 
	 {"go_info.gonum","go_info.term_type", "go_info.level","go_info.descr","go_info.bestEval", "go_info.nUnitranHit" };
	
	private final String [] COL_NAMES = 
		 {"GO term", "Domain", "Level",  "Description", EVALUE , "#Seqs"};
	private String []      ecColNames = null;
	private Vector<String> deColNames = null;
	private int endEC=0, endStatic=0;
	
	private boolean []     selectedCol = null;
	private Vector<Object []> theResults = null;    // results in order COL+ecCol+deCol names
	private HashMap<Integer,Integer> rowMap = null; // If Show DEtrim, not all rows shown	
	
	public BasicGOTablePanel(STCWFrame f, BasicGOFilterTab g) {
		theMainFrame=f;
		theGOQuery=g;
		
		theResults = new Vector<Object []> ();
		treeFilterIn = new HashSet<Integer>(); 	// IDs flagged by tree filter
		currentIDs = new HashSet<Integer>(); 	// IDs flagged by tree filter
		rowMap = new HashMap<Integer,Integer>();	// if treeFilter=true, then non-flagged results are hidden (but retained in theResults).
							// rowMap maps the row number to the actual entry in theResults.
		
		columnPanel = new ColumnPanel();
		tablePanel = new TablePanel();
	}
	private class ColumnPanel extends JPanel
	{
		private static final long serialVersionUID = -7235165216064464845L;
		private ColumnPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
		     	  
			loadInitData();
		    	/// General 
		    	JPanel generalColsPanel = Static.createPagePanel();
		    	generalColsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		    	generalColsPanel.add(new JLabel("General"));
		    	generalColsPanel.add(Box.createVerticalStrut(10));
	    	
		    	chkGeneralColumns = new JCheckBox[COL_NAMES.length];
		    	for(int x=0; x < COL_NAMES.length; x++) {
		    		chkGeneralColumns[x] = new JCheckBox(COL_NAMES[x], false);
		    		chkGeneralColumns[x].setBackground(BGCOLOR);
		    		chkGeneralColumns[x].addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							checkAllGen();
						}
					});
		    		generalColsPanel.add(chkGeneralColumns[x]);
		    	}
		    	chkGeneralColumns[GOindex].setEnabled(false);
		    generalColsPanel.add(Box.createVerticalStrut(10));
		    
		    	JPanel checkGenPanel = Static.createRowPanel();
		    	chkSelectAllGen = new JCheckBox("Check/uncheck all");
		    	chkSelectAllGen.setSelected(false);
		    	chkSelectAllGen.setBackground(BGCOLOR);
		    	chkSelectAllGen.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						for(int x=1; x<chkGeneralColumns.length; x++) { // Skip GOindex
							boolean isSel = chkSelectAllGen.isSelected();
							chkGeneralColumns[x].setSelected(isSel);
						}
					}
				});
		    	checkGenPanel.add(chkSelectAllGen);
		    	checkGenPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		    	generalColsPanel.add(checkGenPanel);
	    	
		    int rowBreak=10; // for both ec and de
	//evidence code
		 	JPanel ecColsPanel = Static.createPagePanel();
		 	ecColsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		    	if (ecColNames.length>0) { // pre v1.6.4 did not have EC in go_info   
			    	ecColsPanel.add(new JLabel("Evidence Codes"));
			    	ecColsPanel.add(Box.createVerticalStrut(10));
			    
			    	int ecNum = ecColNames.length;
			    	chkECColumns = new JCheckBox[ecNum];
			    	int nRow = ecNum;
			    	if (ecNum>rowBreak) nRow=(int) ((ecNum/2.0)+0.5);
			  
			    	JPanel subPanel1 = Static.createPagePanel();
			    subPanel1.setAlignmentY(Component.TOP_ALIGNMENT);
			    	JPanel subPanel2 = Static.createPagePanel();
			    	subPanel2.setAlignmentY(Component.TOP_ALIGNMENT);
			    	    	
			    	for (int x=0; x<ecNum; x++) {
			    		chkECColumns[x] = new JCheckBox(ecColNames[x], false);
			    		chkECColumns[x].setBackground(BGCOLOR);
			    		chkECColumns[x].addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							checkAllEC();
						}
					});
			    		if (x<nRow) subPanel1.add(chkECColumns[x]);
			    		else subPanel2.add(chkECColumns[x]);
			    	}
			    	if (nRow!=ecNum) {
			    		JPanel subPanel =  Static.createRowPanel();
				    	subPanel.add(subPanel1);
				    	subPanel.add(Box.createHorizontalStrut(10));
				    	subPanel.add(subPanel2);
				    	ecColsPanel.add(subPanel);
			    	}
			    	else ecColsPanel.add(subPanel1);
			    	ecColsPanel.add(Box.createVerticalStrut(10));
			    	
			    	JPanel checkECPanel = Static.createRowPanel();
			    	chkSelectAllECs = new JCheckBox("Check/uncheck all");
			    	chkSelectAllECs.setSelected(false);
			    	chkSelectAllECs.setBackground(BGCOLOR);
			    	chkSelectAllECs.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							for(int x=0; x<chkECColumns.length; x++) {
								boolean isSel = chkSelectAllECs.isSelected();
								chkECColumns[x].setSelected(isSel);
							}
						}
					});
			    	checkECPanel.add(chkSelectAllECs);
			    	checkECPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			    	ecColsPanel.add(checkECPanel);
		    	}
/////// DE
		    	JPanel deColsPanel = Static.createPagePanel();
		    	deColsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
	   
		    	if (deColNames.size() > 0)
		    	{
		    	 	deColsPanel.add(new JLabel("GO DE P-value"));
			    	deColsPanel.add(Box.createVerticalStrut(10));
			    	
			    	int deNum = deColNames.size();
			    int 	nRow = deNum;
			    	if (deNum>rowBreak) nRow=(int) ((deNum/2.0)+0.5);
			    	
		    		JPanel deSubPanel1 = Static.createPagePanel();
		    		deSubPanel1.setAlignmentY(Component.TOP_ALIGNMENT);
		    		JPanel deSubPanel2 = Static.createPagePanel();
		    		deSubPanel2.setAlignmentY(Component.TOP_ALIGNMENT);
		    		
			    	chkDEColumns = new JCheckBox[deColNames.size()];
			    	for(int x=0; x<chkDEColumns.length; x++) {
			    		chkDEColumns[x] = new JCheckBox(deColNames.get(x), false);
			    		chkDEColumns[x].setBackground(BGCOLOR);
			    		chkDEColumns[x].addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								checkAllDE();
							}
						});
			    		if (x<nRow) deSubPanel1.add(chkDEColumns[x]);
			    		else deSubPanel2.add(chkDEColumns[x]);
			    	}
			    	if (nRow!=deNum) {
			    		JPanel subPanel =  Static.createRowPanel();
				    	subPanel.add(deSubPanel1);
				    	subPanel.add(Box.createHorizontalStrut(10));
				    	subPanel.add(deSubPanel2);
				    	deColsPanel.add(subPanel);
			    	}
			    	else deColsPanel.add(deSubPanel1);
			    	deColsPanel.add(Box.createVerticalStrut(10));  	
		    
			    JPanel checkDEPanel = Static.createRowPanel();
			    	chkSelectAllDEs = new JCheckBox("Check/uncheck all");
			    	chkSelectAllDEs.setSelected(false);
			    	chkSelectAllDEs.setBackground(BGCOLOR);
			    	chkSelectAllDEs.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						boolean isSel = chkSelectAllDEs.isSelected();
						for(int x=0; x<chkDEColumns.length; x++)
							chkDEColumns[x].setSelected(isSel);
					}
				});
			    	checkDEPanel.add(chkSelectAllDEs);
			    	checkDEPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			   
			    	deColsPanel.add(checkDEPanel);
		    	}
	    	
		   	/**** Select puts general and de side by side ****/
		    JPanel sideBySidePanel = Static.createRowPanel();
		    	sideBySidePanel.add(generalColsPanel);
		    	sideBySidePanel.add(Box.createHorizontalStrut(30));
		    sideBySidePanel.add(ecColsPanel);
		    	sideBySidePanel.add(Box.createHorizontalStrut(30));
		    	sideBySidePanel.add(deColsPanel);
		    	
		    	sideBySidePanel.setMaximumSize(sideBySidePanel.getPreferredSize());
		    	sideBySidePanel.setMinimumSize(sideBySidePanel.getPreferredSize());
		    	sideBySidePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	    	
		    	/////// Accept Discard
		    	JPanel buttonPanel = Static.createRowPanel();
		    	buttonPanel.setBackground(BGCOLOR);
		    	JButton keepButton = new JButton("Accept");
		    	keepButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveColumns();
					theTableModel.fireTableStructureChanged();
					tablePanel.tableResizeColumns();
					
					setVisible(false);
					theGOQuery.showMain();
				}
			});
		
			JButton discardButton = new JButton("Discard");
			discardButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					theGOQuery.showMain();
				}
			});
		    	buttonPanel.add(keepButton);
		    	buttonPanel.add(Box.createHorizontalStrut(10));
		    	buttonPanel.add(discardButton);
		    	buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		    	buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
		    	buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		    	
		    	JLabel tmpLabel = new JLabel("<HTML><H2>Select columns to view</H2></HTML>");
		    	tmpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		    	tmpLabel.setMaximumSize(tmpLabel.getPreferredSize());
		    	tmpLabel.setMinimumSize(tmpLabel.getPreferredSize());
		    add(Box.createVerticalStrut(10));
		    	add(tmpLabel);
		    	add(Box.createVerticalStrut(10));
		    	add(sideBySidePanel);
		    	add(Box.createVerticalStrut(20));
		    	add(buttonPanel);
		    	setVisible(false);
		    	
		    	initSelections();
		} // end createColumnPanel
		private void loadInitData() {
			try {
				DBConn mDB = theMainFrame.getNewDBC();
				ResultSet rs=null;
				
				// for DE columns
				deColNames = new Vector<String> ();
				rs = mDB.executeQuery("SHOW COLUMNS FROM go_info LIKE 'P\\_%'");
				while(rs.next()) {
					String deName = rs.getString(1).substring(2);
					deColNames.add(deName);
				}
				rs.close();
				mDB.close();
				
				// init rest of columns
				HashSet <String> ecSet = theMainFrame.getMetaData().getECinDB();
				String [] ecList = theMainFrame.getMetaData().getEClist();
				ecColNames = new String [ecSet.size()];
				int x=0;
				for (String ec : ecList) {
					if (ecSet.contains(ec))
						ecColNames[x++] = ec;
				}
				endStatic = COL_NAMES.length;
				endEC = endStatic + ecColNames.length;
		
				selectedCol = new boolean[COL_NAMES.length + ecColNames.length + deColNames.size()];		
				for(int i=0; i<selectedCol.length; i++) {
					if (i<endStatic) selectedCol[i] = true;
					else selectedCol[i] = false;
				}
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(null, "Query failed getting column names");
				ErrorReport.prtReport(e, "Error getting column names");
			}
		}
		 
		 public void setupVisible() {
			 int x=0;
			for(; x<chkGeneralColumns.length; x++)
				chkGeneralColumns[x].setSelected(selectedCol[x]);
			if (chkECColumns!=null) {
				for(int y=0; y<chkECColumns.length; x++, y++) 
					chkECColumns[y].setSelected(selectedCol[x]);
			}
			if (chkDEColumns!=null) {
				for(int y=0; y<chkDEColumns.length; x++, y++) 
					chkDEColumns[y].setSelected(selectedCol[x]);
			}
			checkAllDE();
			checkAllEC();
			checkAllGen();
			setVisible(true);
		 }
		 private void checkAllGen() {
		    	boolean allSelected = true;
		    	for(int x=0; x<chkGeneralColumns.length && allSelected; x++)
		    			allSelected = chkGeneralColumns[x].isSelected();
		    	chkSelectAllGen.setSelected(allSelected);
		}
	    private void checkAllEC() {
	    		if (ecColNames.length == 0) return;
	    	
		    	boolean allSelected = true;
		    	for(int x=0; x<chkECColumns.length && allSelected; x++)
		    			allSelected = chkECColumns[x].isSelected();
		    	chkSelectAllECs.setSelected(allSelected);
		}
	    private void checkAllDE() {
		    	if (deColNames.size() == 0) return;
		    
		    	boolean allSelected = true;
		    	for(int x=0; x<chkDEColumns.length && allSelected; x++)
		    		allSelected = chkDEColumns[x].isSelected();
		    	
		    	chkSelectAllDEs.setSelected(allSelected);
		}
	    private void saveColumns() {
	    		String prefs="";
	    		int x=0;
			for (; x<chkGeneralColumns.length; x++) {
				boolean b = chkGeneralColumns[x].isSelected();
				selectedCol[x] = b;
				if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
			}
			if (chkECColumns!=null) {
				for (int y=0; y<chkECColumns.length; x++, y++) {
					boolean b = chkECColumns[y].isSelected();
					selectedCol[x] = b;
					if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
				}
			}
			if (chkDEColumns!=null) {
				for (int y=0; y<chkDEColumns.length; x++, y++) {
					boolean b = chkDEColumns[y].isSelected();
					selectedCol[x] = b;
					if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
				}
			}
			prefsRoot.put(prefLabel, prefs);
	    }
	    private void initSelections() {
	    		nStatic = chkGeneralColumns.length;
	    		nEC = (chkECColumns==null ? 0 : chkECColumns.length);
			nDE = (chkDEColumns==null ? 0 : chkDEColumns.length);
			
			totalColumns = nStatic + nDE + nEC;
			
			prefsRoot = theMainFrame.getPreferencesRoot();
			prefLabel = theMainFrame.getdbName() + goPref; // sTCW database name + _hitCol
			String goCol = prefsRoot.get(prefLabel, null); // sets to null if not set yet
			int cnt=0;
			
			if (goCol!=null) {
				int offset = nStatic+nEC;
				String [] list = goCol.split("\t");	
				
				for (int i=0; i<list.length; i++) {
					int x = Static.getInteger(list[i]);
					if (x<0 || x>=totalColumns) continue; 
					
					cnt++;
					if (x<nStatic) chkGeneralColumns[x].setSelected(true);
					else if (x<offset) chkECColumns[x-nStatic].setSelected(true);
					else chkDEColumns[x-offset].setSelected(true);
				}
			}
			if (cnt==0) {
				for (int x=0; x<nStatic; x++) {
					chkGeneralColumns[x].setSelected(true);
				}
			}
			selectedCol = new boolean [totalColumns];
			saveColumns();
	    }
	    int totalColumns=0, nStatic=0, nDE=0, nEC=0;
	    private String prefLabel = "";
		private Preferences prefsRoot = null;
		
		private JCheckBox [] chkGeneralColumns = null, chkDEColumns = null, chkECColumns = null;
		private JCheckBox chkSelectAllGen = null, chkSelectAllDEs = null, chkSelectAllECs = null;

	} // End ColumnPanel
	/*******************************************************
	 * XXX table
	 */
	private class GoTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -314853582139888170L;

		public GoTableModel() {}
		
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
		
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}
		
		public String getColumnName(int columnIndex) {
			int index = getMappedColumn(columnIndex);
			
			if (index < endStatic) 			return COL_NAMES[index];
			if (index < endEC)          		return ecColNames[index - endStatic];
			if (deColNames.size() > 0)   return deColNames.get(index - endEC);
			
			System.err.println("Bad column:" + index + " diff:" + (index - COL_NAMES.length));
			return "NA";
		}
		
		public int getColumnCount() {
			int numCols = 0;
			for(int index = 0; index<selectedCol.length; index++) {
				if(selectedCol[index]) numCols++;
			}
			return numCols;
		}

		public int getRowCount() {
			return rowMap.keySet().size();
		}
		
		// Following two methods are for Export
		// Column relative to static order of results.
		public Object getValue(int rowx, int col) {
			int row = rowMap.get(rowx);
			return theResults.get(row)[col];
		}
		
		public double getDE(int rowx, int deCol) {
			int row = rowMap.get(rowx);
			return ((((Double)theResults.get(row)[deCol]).doubleValue()));
		}
		// rowx and col relative to displayed table
		public Object getValueAt(int rowx, int col) {
			int index = getMappedColumn(col);
			int row = rowMap.get(rowx);
			
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
			
			if (index >= endStatic && index < endEC) {
				int x = ((((Integer)theResults.get(row)[index]).intValue()));
				if (x==1) return "Yes";
				else return " - ";
			}
			if(index >= endEC) { 
				double theVal = ((((Double)theResults.get(row)[index]).doubleValue()));
				return new DisplayFloat(theVal);
			}
			return "??";
		}
 	  	public class ColumnListener extends MouseAdapter {
		    protected JTable table;

		    public ColumnListener(JTable t) {
			    	table = t;
			    	bAscend = new boolean[selectedCol.length];
			    	for(int x=0; x<bAscend.length; x++)
			    		bAscend[x] = true;   
		    }

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
  	  		}
  	  		
  	  		private void sort(final int sortColumn, final boolean ascend) {
  	  			Collections.sort(theResults, new Comparator<Object []>() {

				public int compare(Object[] arg0, Object[] arg1) {
					int sign = 1;
					if(!ascend) sign = -1;
	  	  			int mapCol = getMappedColumn(sortColumn);  	  			
		  	  		if(mapCol >= endStatic && mapCol<endEC) {
		  	  			return sign * ((Integer)arg0[mapCol]).compareTo((Integer)arg1[mapCol]);
	  				}
	  				if(mapCol >= endEC) {
	  	  				return sign * ((Double)arg0[mapCol]).compareTo((Double)arg1[mapCol]);
	  				}
	  	  			
	  				Class<?> theType = COL_TYPES[mapCol];
	  	  			if(theType == String.class)
	  	  				return sign * ((String)arg0[mapCol]).compareTo((String)arg1[mapCol]);
	  	  			if(theType == Integer.class)
	  	  				return sign * ((Integer)arg0[mapCol]).compareTo((Integer)arg1[mapCol]);
	  	  			if(theType == Double.class)
	  	  				return sign * ((Double)arg0[mapCol]).compareTo((Double)arg1[mapCol]);
	  	  			if(theType == Long.class)
	  	  				return sign * ((Long)arg0[mapCol]).compareTo((Long)arg1[mapCol]);
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
			theTable = new JTable(theTableModel)
			{
				private static final long serialVersionUID = -1559706452814091003L;

				public Component prepareRenderer(
				        TableCellRenderer renderer, int row, int column)
				    {
				        Component c = super.prepareRenderer(renderer, row, column);
				       
					    if (theTable.isRowSelected(row)) 	c.setBackground(Globalx.selectColor);
					    else if (isDEtrim) {
					    	 	Integer gonum = (Integer)(theResults.get(rowMap.get(row))[GOindex]);
					    		if (treeFilterIn.contains(gonum)) c.setBackground(DETRIMCOLOR);
					    		else 							 c.setBackground(Color.WHITE);
					    }
					    else {
					    		boolean bBlueBG = ((row % 2) == 1);
					    		if ( bBlueBG ) 					c.setBackground( Globalx.altRowColor );
							else                             c.setBackground( null );
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
					theGOQuery.updateTopButtons();
				}
			});

			JTableHeader header = theTable.getTableHeader();
			header.setUpdateTableInRealTime(true);
			colListener = theTableModel.new ColumnListener(theTable);
			header.addMouseListener(colListener);

			theTable.getTableHeader().setBackground(Color.WHITE);
			tableScroll = new JScrollPane(theTable);
			
			add(tableScroll);
			add(createTableButtonPanel());
		}
		 /*********************
		  *  BOTTOM BUTTONS - 
		  ***/
		 private JPanel createTableButtonPanel() {
			lblHeader = new JLabel("Modify table");
			
			btnUnselectAll = new JButton("Unselect All");
			btnUnselectAll.setMargin(new Insets(0, 0, 0, 0));
			btnUnselectAll.setFont(new Font(btnUnselectAll.getFont().getName(),Font.PLAIN,10));
			btnUnselectAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					theTable.clearSelection();
				}
			});
			
			btnSelectAll = new JButton("Select All");
			btnSelectAll.setMargin(new Insets(0, 0, 0, 0));
			btnSelectAll.setFont(new Font(btnSelectAll.getFont().getName(),Font.PLAIN,10));
			btnSelectAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					theTable.selectAll();
				}
			});
			
			btnDelete = new JButton("Delete selected");
			btnDelete.setMargin(new Insets(0, 0, 0, 0));
			btnDelete.setFont(new Font(btnDelete.getFont().getName(),Font.PLAIN,10));
			btnDelete.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteFromList();
					tableRefresh();
				}
			});
			
			btnKeep = new JButton("Keep selected");
			btnKeep.setMargin(new Insets(0, 0, 0, 0));
			btnKeep.setFont(new Font(btnKeep.getFont().getName(),Font.PLAIN,10));
			btnKeep.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					keepFromList();
					tableRefresh();
				}
			});
			
			JPanel bottomPanel = new JPanel();
			bottomPanel.setLayout(new BoxLayout ( bottomPanel, BoxLayout.X_AXIS ));
			bottomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			bottomPanel.setBackground(Globalx.BGCOLOR);
			bottomPanel.setAlignmentY(LEFT_ALIGNMENT);
			
			bottomPanel.add(lblHeader);
			bottomPanel.add(Box.createHorizontalStrut(5));
			bottomPanel.add(btnDelete);
			bottomPanel.add(Box.createHorizontalStrut(3));
			bottomPanel.add(btnKeep);
			
			bottomPanel.add(Box.createHorizontalStrut(15));
			bottomPanel.add(btnSelectAll);
			bottomPanel.add(Box.createHorizontalStrut(3));
			bottomPanel.add(btnUnselectAll);
			
			bottomPanel.add(Box.createHorizontalGlue());
			bottomPanel.setMaximumSize(bottomPanel.getPreferredSize());
			return bottomPanel;
		}
		 private void keepFromList()
		{
			int numElements = theTable.getRowCount();
			int [] selValues = theTable.getSelectedRows();		

			int [] opposite = new int[numElements - selValues.length];
			int x=0, selPos=0, nextval=0;
			
			for(x=0; x<opposite.length; x++)
			{
				while(selPos<selValues.length && selValues[selPos] == nextval) {
					selPos++;
					nextval++;
				}
				opposite[x] = nextval++;
			}
			deleteFromList(opposite);
		}
			
		private void deleteFromList()
		{
			int [] selValues = theTable.getSelectedRows();
			deleteFromList(selValues);
		}
		private void deleteFromList(int [] sels)
		{
			if (sels == null || sels.length==0) return;
			isDEtrim=showDEtrim=false;
			treeFilterIn.clear();
			
			theTable.clearSelection();
			for(int x=sels.length-1; x>=0; x--) {	
				theResults.remove(sels[x]);
			}
			currentIDs.clear();
			for (Object[] res : theResults)
			{
				Integer id = (Integer)res[GOindex];
				currentIDs.add(id);
			}
			buildRowMap();
			theTableModel.fireTableDataChanged();
			theGOQuery.deleteFinish(getRowCount());
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
		            final JTable table,
		            final Object value,
		            final boolean isSelected,
		            final boolean hasFocus,
		            final int row,
		            final int col) {

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
		private JButton btnDelete = null, btnKeep = null;
		private JButton btnUnselectAll = null, btnSelectAll = null;
		private JLabel lblHeader = null;
	
		private GoTableModel.ColumnListener colListener = null;
		private JScrollPane tableScroll = null;
	} // end TablePanel
	
	/********************************************
	 * Public
	 */
	public void clear() {
		theResults.clear();
		rowMap.clear();
		currentIDs.clear();
		
		treeFilterIn.clear();
		isDEtrim=showDEtrim=false;
		
		theTableModel.fireTableDataChanged();
	}
	public void clearTrim() {
		treeFilterIn.clear();
		isDEtrim=showDEtrim=false;
		
		theTableModel.fireTableDataChanged();
	}
	public String trimTable() {
		isDEtrim=true;
		tablePanel.tableRefresh();
		return + theResults.size() + " GOs; " + treeFilterIn.size() + " in DE-trim";
	}
	
	private String trimStatus() {
		return (isDEtrim) ? "Trim" : "";
	}
	/**********************************************************
	 * building table 
	 */
	public void addResult(Object [] newRow) {
		Integer gonum = (Integer)newRow[GOindex];	
		if (!currentIDs.contains(gonum)) 
		{
			theResults.add(newRow);
			currentIDs.add(gonum);
		}
	}
	public void treeAdd(int index) {treeFilterIn.add(index);}
	public Vector<Object []> getTheResults() {return theResults;} 
	
	public void showColumns() {
		columnPanel.setupVisible();
	}
	public ColumnPanel getColumnPanel() { return columnPanel;}
	public TablePanel getTablePanel() { return tablePanel;}
	
	public int [] getSelectedGOs() {
		int [] sels = theTable.getSelectedRows();
		int [] goIDs = new int[sels.length];
		for(int x=0; x<sels.length; x++) {
			goIDs[x] = Integer.parseInt(((String)theTableModel.getValueAt(sels[x], GOindex)).substring(3));
		}
		return goIDs;
	}
	public String getSelectedGO() {
		int [] sels = theTable.getSelectedRows();
		if (sels!=null && sels.length>0) {
			return ((String)theTableModel.getValueAt(sels[0], GOindex));
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
	
	public int getSelectedRowCount() { return theTable.getSelectedRowCount();}
	public int getRowCount() { return theTable.getRowCount();}
	public int getNumStaticCols() {return COL_NAMES.length;}
	public Class<?> [] getColumnTypes() {return COL_TYPES;}
	public int getEndEC() { return endEC;}
	public Vector <String> getDEcolumns() { return deColNames;}
	
	public void toggleTrimmedView(boolean showDEisSelected) 
	{	
		showDEtrim = (deColNames.size() > 0 ? showDEisSelected : false);
		
		buildRowMap();
		theTableModel.fireTableStructureChanged();
		theTableModel.fireTableDataChanged();
		tablePanel.tableResizeColumns();
	}
	public boolean changeNSeq(int row, int cnt, int cutoff) {
		if (cnt==0 || cnt<cutoff) {
			Integer go = (Integer)theResults.get(row)[GOindex];
			if (currentIDs.contains(go)) currentIDs.remove(go); 
			theResults.remove(row);
			return true;
		}
		
		theResults.get(row)[GOnSeq] = cnt;
		return false; // did not remove
	}
	public void buildRowMap()
	{
		rowMap.clear();
		for (int r1 = 0, r2=0; r1 < theResults.size(); r1++)
		{
			if (showDEtrim)
			{
				Integer go = (Integer)theResults.get(r1)[GOindex];
				if (treeFilterIn.contains(go))
				{
					rowMap.put(r2, r1);
					r2++;
				}
			}
			else
			{
				rowMap.put(r1,r1);
			}	
		}	
		tablePanel.tableRefresh();
	}
	public String getColumns() {
		String theQuery = "SELECT " + COL_MYSQL[GOindex];
		
		for(int x=1; x<COL_MYSQL.length; x++) {
			if(COL_MYSQL[x].length() == 0) theQuery += ", NULL";
			else  theQuery += ", " + COL_MYSQL[x];
		}
		for (String ec : ecColNames)
			theQuery += ", go_info." + ec; 
		
		for(int x=0; x<deColNames.size(); x++)
			theQuery += ", go_info.P_" + deColNames.get(x);
		
		theQuery += " FROM go_info ";
		return theQuery;
	}
	public void exportTable(int type) {
		new ExportGO().run(type);
	}
	public String makeCopyTableString(String delim) {
 		StringBuilder retVal = new StringBuilder();
 	
		for(int x=0; x<theTableModel.getColumnCount()-1; x++) {
			retVal.append(theTableModel.getColumnName(x));
			retVal.append(delim);
		}	
		retVal.append(theTableModel.getColumnName(theTableModel.getColumnCount()-1));
		retVal.append("\n");
		
		for(int x=0; x<theTableModel.getRowCount(); x++) {
			StringBuilder row = new StringBuilder();
			for(int y=0; y<theTableModel.getColumnCount()-1; y++) {
				row.append(theTableModel.getValueAt(x, y));
				row.append(delim);
			}
			retVal.append(row.toString());
			retVal.append(theTableModel.getValueAt(x, theTableModel.getColumnCount()-1));
			retVal.append("\n");
		}
 		return retVal.toString();
	 }
	public void showRelatedFromTable(JButton btnShow) {
		try {
			int [] sels = theTable.getSelectedRows();
			if (sels.length==0) {
				JOptionPane.showMessageDialog(null, "No selected GO ");
				return;		
			}
			int gonum = (Integer)  theResults.get(sels[0])[GOindex];
			String desc = (String) theResults.get(sels[0])[GOdesc];
			
			int [] goIDs = new int[theTable.getRowCount()];
			for(int x=0; x<goIDs.length; x++) {
				goIDs[x] = Integer.parseInt(((String)theTableModel.getValueAt(x, GOindex)).substring(3));
			}
			HashSet <Integer> trimSet = null;
			if (isDEtrim && goIDs.length>treeFilterIn.size())
				trimSet=treeFilterIn;
			new GOtree(theMainFrame).popup(gonum, desc, goIDs, trimSet, btnShow, this);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Hits for Selected"); }
	}
	public String getRowHeading() {
		return "#Seqs";
	}
	public String getRowInfo(int row) {
		String line = String.format("%5d", (Integer) theTableModel.getValue(row, GOnSeq));
		return line;
	}
	/************ GO hits  ***************/
	public void showHitsForSelected(int type, JButton btnShow) {
		try {
			int [] sels = theTable.getSelectedRows();
			if (sels.length==0) {
				JOptionPane.showMessageDialog(null, "No selected GO ");
				return;		
			}
			
			int gonum = (Integer) theResults.get(sels[0])[GOindex];
			String desc = (String) theResults.get(sels[0])[GOdesc];
			new GOtree(theMainFrame).popup(gonum, desc, type, btnShow);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Hits for Selected"); }
	}
	
	/******** GO selected ***********/
	public void showPathsForSelected(int type, JButton btnShow) {
		try {
			int [] sels = theTable.getSelectedRows();
			if (sels.length==0) {
				JOptionPane.showMessageDialog(null, "No selected GO ");
				return;		
			}
			int gonum = (Integer) theResults.get(sels[0])[GOindex];
			String desc = (String) theResults.get(sels[0])[GOdesc];
			
			new GOtree(theMainFrame).popup(gonum, desc, type, btnShow);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Paths for Selected");}
	}

	/*********** GO list ****************/
	public void showExportAllPaths(int type) {
		try {	
			TreeSet <Integer> goMap = new TreeSet <Integer>  ();
			for (Object [] o : theResults) {
				int gonum = (Integer) o[GOindex];
				goMap.add(gonum);
			}
			boolean big = (type==GOtree.LONGEST_PATHS || type==GOtree.ALL_PATHS);
			if (big && goMap.size()>100) {
				String msg = "This feature is only recommend for < 100 GOs.\n";
				msg += 		 "The table can get big and slow to produce (if really large >5 minutes).";
				if (!UserPrompt.showContinue(">100 Paths", msg)) return;
			}
			new GOtree(theMainFrame).popupExportAll(type, goMap);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Show Paths for Selected");}
	}
	/***************************************************************/
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
	private class ExportGO {
		private final String filePrefix="GOtable";
		public ExportGO() {}
		
		public boolean run(int type) {
			tcwid = theMainFrame.getdbID();
			String msg, title, file;
			if (type==0) {
				msg="Append";
				title=" columns of table ";
				file = filePrefix+"Columns" + Globalx.CSV_SUFFIX;
			}
			else if (type==1){
				msg="Append";
				title= " SeqIDs with GOs ";
				file = filePrefix+"BySeq"+ Globalx.CSV_SUFFIX;
			}
			else if (type==2) {
				msg="Merge";
				title = " #Seqs Column ";
				file = filePrefix+"NSeqs"+ Globalx.CSV_SUFFIX;
			}
			else return false;
			
			File out = ExportFile.getFileHandle(msg, file, theMainFrame);
			if (out==null) return false;
				
			boolean bAppend = ExportFile.isAppend();
			if (bAppend) Out.PrtSpMsg(0, msg + title +  out.getAbsolutePath());
			else         Out.PrtSpMsg(0, "Write " + title +  out.getAbsolutePath());
			
			if (type==0) 		return exportTableColumns(bAppend, out);
			else if (type==1)	return exportSeqIDwithGOs(bAppend, out);
			else if (type==2)	return exportDomainMerge(bAppend, out); 
			else return false;
		}
		private boolean exportTableColumns(boolean bAppend, File out) {
			try {
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out, bAppend)));
				headerLine = String.format("### %s %d GOs   %s   %s", 
						tcwid, getRowCount(), theGOQuery.getStatusStr(), trimStatus());
				pw.println(headerLine);
				
				StringBuilder line = new StringBuilder();
	
				int colCnt = theTableModel.getColumnCount();
				int rowCnt = getRowCount();
				Out.prt(1, "Processing " + rowCnt + " rows and " + colCnt + " columns...");
				
				for(int x=0; x<theTableModel.getColumnCount()-1; x++) {
					String colName = theTableModel.getColumnName(x).replaceAll("\\s", "-"); 
					line.append(colName);
					line.append(Globalx.CSV_DELIM);
				}	
				line.append(theTableModel.getColumnName(theTableModel.getColumnCount()-1));
				pw.println(line.toString());
			
				int x, y, cnt=0;
				String val = "";
				for(x=0; x<getRowCount(); x++) {
					line.delete(0, line.length());
					for(y=0; y<theTableModel.getColumnCount()-1; y++) {
						if(theTableModel.getValueAt(x, y) != null) 
							val = theTableModel.getValueAt(x, y).toString();
						else val = "";
						
						line.append(val);
						line.append(Globalx.CSV_DELIM);
					}
					if(theTableModel.getValueAt(x, y) != null) 
						val = theTableModel.getValueAt(x, y).toString();
					else  val = "";
					
					line.append(val);
					
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
				
				String where = theGOQuery.makeNseqClause(false); 
				if (where!=null && where.length()>0) where = " and " + where;
				HashMap <Integer, String> seqID = new HashMap <Integer, String> ();
				DBConn mDB = theMainFrame.getNewDBC();
				
				ResultSet rs = mDB.executeQuery("select CTGid, contigid from contig as c " +
						"where PIDgo>0" + where);
				while (rs.next()) {
					seqID.put(rs.getInt(1), rs.getString(2));
				}
				Out.PrtSpCntMsg(1, seqID.size(), "sequences with at least one GO and pass DE");
				int cnt=0, cntGO=0;
				HashMap <Integer, Vector<Integer>> seqGO = new HashMap <Integer, Vector<Integer>> ();
				String query = "select CTGID from pja_unitrans_go where gonum=";
				for(int row=0; row<rowCnt; row++) {
					int go = (Integer) theTableModel.getValue(row, GOindex);
					
					rs = mDB.executeQuery(query + go);
					while (rs.next()) {
						int ctgid = rs.getInt(1);
						if (seqID.containsKey(ctgid)) {
							Vector <Integer> list;
							if (seqGO.containsKey(ctgid)) list = seqGO.get(ctgid);
							else list = new Vector <Integer> ();
							list.add(go);
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
				
				headerLine = String.format("!!! %s %d Seqs   %s  %s", tcwid, seqGO.size(), theGOQuery.getStatusStr(), trimStatus());
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
		
		/*********************************************************
		 * XXX Get from table: GO-term Domain Description #Seqs
		 * #Seqs is renamed by User
		 */
		private boolean exportDomainMerge(boolean bInMerge, File Infh) {
			try {
				String de = theGOQuery.selectOneDE();
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
									"### %-6s %6d  Column: %-6s  %s   %s", 
									tcwid, nSeqs, colHead,
									theGOQuery.getStatusStr(), trimStatus());
							
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
		private boolean domainAddTable() {
			try {
				Out.PrtSpMsg(1, "Add GOs from table");
				int j=nCol-1, nGO=0;
				
				double score=0.0;
				int count=0;
				
				for (int row=0; row<getRowCount(); row++) {
					String domain = (String)  theTableModel.getValue(row, GOdomain);
					String desc = 	(String)  theTableModel.getValue(row, GOdesc);
					if (isSeq) count = 	(Integer) theTableModel.getValue(row, GOnSeq);
					else score =    theTableModel.getDE(row, colIdx);
					if      (domain.startsWith("b")) domainAdd(biolMap, desc, score, count, j);
	    				else if (domain.startsWith("c")) domainAdd(cellMap, desc, score, count, j);
	    				else if (domain.startsWith("m")) domainAdd(moleMap, desc, score, count, j);
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
	    			if (tok[0].equals("Domain")) {
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
		    				if (count<0) return prtError("Incorrect #Seqs: " + tok[i]);
		    			}
		    			else { // for DE
		    				score = Static.getDouble(tok[i].trim());
		    				if (score<0) return prtError("Incorrect p-value: " + tok[i]);
		    			}
		    			
		    			if (domain.startsWith("b")) 		domainAdd(biolMap, desc, score, count, j);
		    			else if (domain.startsWith("c")) domainAdd(cellMap, desc, score, count, j);
		    			else if (domain.startsWith("m")) domainAdd(moleMap, desc, score, count, j);
		    			else return prtError("Bad domain: " + domain);
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
				domainWriteSet(biolMap, "biological_process");
				domainWriteSet(cellMap, "cellular_component");
				domainWriteSet(moleMap, "molecular_function");
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
		private String tcwid = null, headerLine="", columnLine="Domain\tDescription";
		private int nCol=1;
		
		private PrintWriter outF;
		private TreeMap <String, Count> biolMap = new TreeMap <String, Count>();
		private TreeMap <String, Count> cellMap = new TreeMap <String, Count>();
		private TreeMap <String, Count> moleMap = new TreeMap <String, Count>();
		private int cntOut=0;
		
		private boolean isSeq=false;
		private int colIdx=-1; // column to print out
	}
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
						Out.prt("BasicTablePanel: class? " + (String) theTable.getValueAt(x, y) + " " + dtype);
					}
				}
				double [] results = Stats.averages(colName, dArr, false);
				rows[nRow][0] = colName;
				for (int i=0, c=1; i<results.length; i++, c++) {
					if ((i>=intCol && isInt) || (i==results.length-1)) {
						if (results[i]<0) rows[nRow][c] = "N/A"; // overflow
						else rows[nRow][c] = String.format("%,d", (long) results[i]);
					}
					else rows[nRow][c] = Out.formatDouble(results[i]);
				}
				nRow++;
			}
			String statStr = Out.makeTable(nStat, nRow, fields, justify, rows);
			statStr += "\n" + tableRows + " rows; " + sum;
			return statStr;
		
		} catch(Exception e) {ErrorReport.reportError(e, "Error create column stats"); return "Error"; }
	}
	/*****************************************************/
	private JTable theTable = null;
	private TablePanel tablePanel = null;
	private ColumnPanel columnPanel = null;
	private GoTableModel theTableModel = null;
	
	private STCWFrame theMainFrame;
	private BasicGOFilterTab theGOQuery;
	
	// DEtrim only
	private HashSet<Integer> currentIDs;
	private HashSet<Integer> treeFilterIn = null;
	private boolean isDEtrim = false, showDEtrim=false;
}
