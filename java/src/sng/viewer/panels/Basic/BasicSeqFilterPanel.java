package sng.viewer.panels.Basic;

/****************************************************
 * Basic Query Sequence:
 * 	section between the top button row and table.
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import sng.database.Globals;
import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Static;

public class BasicSeqFilterPanel extends JPanel {
	private static final long serialVersionUID = -434438354961245720L;
	private static final Color BGCOLOR = Globals.BGCOLOR;
	
	private static final String seqPref= "_seqPrefs"; // save to preferences; same columns for hit&seq
	
	// Any change here will work in this file, but needs to be changed in BasicSeqQueryTab.SeqData class!!!
	// These are columns from database and do not include row#
	private static final String rowCol = "Row#";
	private static final String [] STATIC_COLUMNS   = { "Seq ID", "TCW Remark", "User Remark", "Counts", "Longest", "Best HitID"};
	private static final Class<?> [] COLUMN_TYPES =   { String.class, String.class, String.class, Integer.class, String.class , String.class}; 
	private static final String [] MYSQL_COLUMNS = {
		"contig.contigid", "contig.notes", "contig.user_notes", 
		"contig.totalexp", "contig.longest_clone ", "contig.bestmatchid"};
	
	private boolean isOnByDefault(int x) {
		if (x!=4) return true;
		return false;
	}
	/**********************************************
	* The top queries, and 4 selection panels 
	*/
	public BasicSeqFilterPanel(STCWFrame frame, BasicSeqQueryTab parentTab) {
		theParentTab = parentTab;
		theMainFrame = frame;
		
		initColumns();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Color.white);
		
		queryPanel = new QueryPanel();
		columnPanel = new ColumnPanel();
		
		add(queryPanel);
		add(columnPanel);
	}
	private void initColumns() {
		int x, numColumns=STATIC_COLUMNS.length;

		staticColNames = new String[numColumns];
		staticColTypes = new Class <?> [numColumns];
		int index=0;
		for(x=0; x<STATIC_COLUMNS.length; x++) {
			staticColNames[index] = STATIC_COLUMNS[x];
			staticColTypes[index] = COLUMN_TYPES[x];
			if (x==0) mysqlCols=MYSQL_COLUMNS[x];
			else mysqlCols += "," + MYSQL_COLUMNS[x];
			index++;
		}	
	}
	private void hideMain() {
		queryPanel.setVisible(false);
		theParentTab.hideAll();
	}
	private void showMain() {
		queryPanel.setVisible(true);
		theParentTab.showAll();
	}
	/****************************************
	 * Called by BasicHitQueryTab
	 */
	public String [] getColNames() { return columnPanel.getColNames(); }
	public boolean [] getColSelect() { return columnPanel.getColSelect(); }
	/***************************************************
	 * The main filter panel
	 */
	private class QueryPanel extends JPanel {
		private static final long serialVersionUID = -5987399873828589062L;
		private static final int DEFAULT_PROMPT_SIZE = 20;
		private static final int MAIN_PANEL_LABEL_WIDTH = 75; // labels on left

		public QueryPanel() {
			setBackground(BGCOLOR);
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setAlignmentX(Component.LEFT_ALIGNMENT);
			
			add(new JSeparator());
			add(Box.createVerticalStrut(3));	
			
			JPanel row1 = Static.createRowPanel();
			row1.add(getRowHeader("Search"));
			
			radSeqID = Static.createRadioButton("Seq ID", true); 
			row1.add(radSeqID); row1.add(Box.createHorizontalStrut(2));
			
			radTCW = Static.createRadioButton("TCW", false);
			row1.add(radTCW);	row1.add(Box.createHorizontalStrut(2));
			
			radUser = Static.createRadioButton("User", false);
			row1.add(radUser);	row1.add(Box.createHorizontalStrut(8));
			
			ButtonGroup allbg = new ButtonGroup();
			allbg.add(radSeqID); 
			allbg.add(radTCW); 
			allbg.add(radUser); 
			
			row1.add(new JLabel("Substring: "));
			txtField  = new JTextField(DEFAULT_PROMPT_SIZE);
			txtField.setMaximumSize(txtField.getPreferredSize());
			row1.add(txtField);
			row1.add(Box.createHorizontalStrut(5));
			
			btnFindFile = new JButton("Load File");
			btnFindFile.setBackground(Globals.BGCOLOR);
			
			btnFindFile.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						JFileChooser fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
						if(fc.showOpenDialog(getInstance()) == JFileChooser.APPROVE_OPTION) {
							loadFile(fc.getSelectedFile().getPath());
						}
					}
					catch(Exception e) {ErrorReport.prtReport(e, "Error finding file");}
				}});
			row1.add(btnFindFile); row1.add(Box.createHorizontalStrut(2));
			
			JButton clearall = new JButton("Clear");
			clearall.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						radSeqID.setSelected(true);
						txtField.setText("");
					}
					catch(Exception e) {ErrorReport.prtReport(e, "GO etc");}
			}});
			clearall.setMargin(new Insets(0, 0, 0, 0));
			clearall.setFont(new Font(clearall.getFont().getName(),Font.PLAIN,10));
			clearall.setAlignmentX(RIGHT_ALIGNMENT);
			row1.add(clearall);
			add(row1);
			add(Box.createVerticalStrut(5));	
			
			// Results
			add(new JSeparator());
			add(Box.createVerticalStrut(3));	
			
			JPanel row3 = Static.createRowPanel();
			row3.add(getRowHeader("Results"));
			btnBuildTable = new JButton("BUILD TABLE");
			btnBuildTable.setBackground(Globals.FUNCTIONCOLOR);
			btnBuildTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					loadDataForTable(true /* build */);
				}
			});
			row3.add(btnBuildTable);
			row3.add(Box.createHorizontalStrut(10));
			
			btnAddTable = new JButton("ADD to TABLE");
			btnAddTable.setBackground(Globals.FUNCTIONCOLOR);
			btnAddTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					loadDataForTable(false /* add */);
				}
			});
			row3.add(btnAddTable);
			row3.add(Box.createHorizontalStrut(10));
			
			
			btnSetColumns = new JButton("Columns");
			btnSetColumns.setBackground(Globals.MENUCOLOR);
			btnSetColumns.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					hideMain();
					columnPanel.setVisible(true);
				}
			});
			row3.add(btnSetColumns);	
			add(row3);
			
			setMinimumSize(getPreferredSize());
			setMaximumSize(getPreferredSize());
			setVisible(true);
		}
		
		private JLabel getRowHeader(String label) {
			JLabel lblHeader = new JLabel(label);
			Dimension dim = lblHeader.getPreferredSize();
			dim.width = MAIN_PANEL_LABEL_WIDTH;
			lblHeader.setPreferredSize(dim);
			lblHeader.setMaximumSize(lblHeader.getPreferredSize());
			return lblHeader;
		}
		private void loadFile(String fileName) {
			try {
				loadList= new Vector <String> ();
				String line;
				BufferedReader file = new BufferedReader(new FileReader(fileName));
				while((line = file.readLine()) != null) {
					line.replace("\n","").trim();
					if (line.equals("")) continue;
					if (line.startsWith("#")) continue;
					loadList.add(line);
				}
				file.close(); 
				if (loadList.size()==0) loadList = null;
				else txtField.setText(loadList.get(0) + ",...");
			}
			catch(Exception e) {ErrorReport.prtReport(e, "Error loading file");}
		}
		private STCWFrame getInstance() {return theMainFrame;}
		
		public String getSearchCol() {
			if (radSeqID.isSelected()) return "contig.contigid";
			else if (radTCW.isSelected()) return "contig.notes";
			else return "contig.user_notes";
		}
		public String getSearchStr() { 
			String x = txtField.getText().trim(); 
			if (x.equals("") || x.equals("...") || x.equals("...")) return ""; 
			
			if (x.contains("...")) return Static.addQuoteDBList(loadList);
			loadList=null;
			
			return Static.addQuoteDB(x);
		}
		public String getStatusCol() {
			if (radSeqID.isSelected()) return "Seq ID";
			else return "Remark";
		}
		public String getStatusStr() { 
			String x = txtField.getText().trim(); 
			if (x.equals("")) return "";
			return Static.addQuote(x); 
		}
		
		public void enableAddToTable(boolean b) {btnBuildTable.setEnabled(b);}
		// Search:
		public JRadioButton radSeqID = null;
		public JRadioButton radTCW = null, radUser = null;
		public JTextField txtField = null;
		public Vector <String> loadList = null;
		
		// Results
		private JButton btnBuildTable = null;
		private JButton btnAddTable = null;
		private JButton btnSetColumns = null;
	} // end QueryPanel
	/// XXX Column Select Panel
	/*************************************************
	 * Column Select Panel
	 */
	private class ColumnPanel extends JPanel
	{
		private static final long serialVersionUID = -49938519942155818L;

		public ColumnPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
			
			JPanel headerPanel = new JPanel();
			headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
			headerPanel.setBackground(Color.white);
			
			JLabel theHeader = new JLabel("<HTML><H2>Select columns to view</H2></HTML>");
			theHeader.setBackground(Color.white);
			theHeader.setAlignmentX(CENTER_ALIGNMENT);
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			headerPanel.setMaximumSize(headerPanel.getPreferredSize());
			headerPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(Box.createVerticalStrut(30));
			add(headerPanel);
			add(Box.createVerticalStrut(10));
			
			createSelectPanel();
			add(selectPanel);
			
			add(Box.createVerticalStrut(30));
			btnOK = new JButton("Accept");
			btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSelections();
					showMain();
					setVisible(false);
					theParentTab.tableRefresh();
				}
			});
			btnCancel = new JButton("Discard");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreSelections();
					showMain();
					setVisible(false);
				}
			});
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(Color.white);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(btnOK);
			buttonPanel.add(Box.createHorizontalStrut(10));
			buttonPanel.add(btnCancel);
			buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
			buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(buttonPanel);
			setVisible(false);
		}
 		private void createSelectPanel() {
 			String [] staticNames = new String [staticColNames.length+1];
 			staticNames[0] = rowCol;
 			for (int i=0; i<staticColNames.length; i++) staticNames[i+1] = staticColNames[i];
 			
 		// initialize
			chkStaticColNames = new JCheckBox[staticNames.length];	
			for(int x=0; x<chkStaticColNames.length; x++) chkStaticColNames[x] = null;
 					
		/** Static columns **/ 
			JPanel staticPanel = Static.createPagePanel();
			staticPanel.setAlignmentY(TOP_ALIGNMENT);
			
			for(int x=0; x<staticNames.length; x++) {
				chkStaticColNames[x] = new JCheckBox(staticNames[x], false);
				chkStaticColNames[x].setAlignmentX(LEFT_ALIGNMENT);
				chkStaticColNames[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setOKEnabled();
					}
				});
				JPanel genRow = Static.createRowPanel();
				genRow.add(chkStaticColNames[x]);
				staticPanel.add(genRow);
			}
			staticPanel.add(Box.createVerticalStrut(10));	
			
			JPanel genCheck = Static.createRowPanel();
			genCheck.setBackground(Color.white);
			
			final JCheckBox sAll = new JCheckBox("check/uncheck all", true);
			sAll.setAlignmentX(LEFT_ALIGNMENT);
			sAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					for(int x=0; x<chkStaticColNames.length; x++)
						chkStaticColNames[x].setSelected(sAll.isSelected());
					setOKEnabled();
				}
			});
			genCheck.add(sAll);
			genCheck.setBorder(BorderFactory.createLineBorder(Color.BLACK));	
			genCheck.setMinimumSize(genCheck.getPreferredSize());
			genCheck.setMaximumSize(genCheck.getPreferredSize());
			staticPanel.add(genCheck);	
			
			staticPanel.setMinimumSize(staticPanel.getPreferredSize());
			staticPanel.setMaximumSize(staticPanel.getPreferredSize());
			staticPanel.setAlignmentX(CENTER_ALIGNMENT);

			selectPanel = Static.createRowPanel();
			selectPanel.add(staticPanel);
			
			selectPanel.setMinimumSize(selectPanel.getPreferredSize());
			selectPanel.setMaximumSize(selectPanel.getPreferredSize());
			selectPanel.setAlignmentX(CENTER_ALIGNMENT);
			
			initSelections();
		
 		}
		
		// Accept is enabled if nothing is selected
		private void setOKEnabled() {
			boolean enable = false;
			for(int x=0; !enable && x<chkStaticColNames.length; x++)
				enable = chkStaticColNames[x].isSelected();
			btnOK.setEnabled(enable);
		}
 		public String [] getColNames() {
 			String [] colName = new String[totalColumns];
 			int index = 0;
 			
 			for(int x=0; x<chkStaticColNames.length; x++)
 				colName[index++] = chkStaticColNames[x].getText();
 			
 			return colName;
 		}
 		
 		public boolean [] getColSelect() {
 			boolean [] isChk = new boolean[totalColumns];
 			int index = 0;
 			
 			for(int x=0; x<chkStaticColNames.length; x++)
 				isChk[index++] = chkStaticColNames[x].isSelected();
 			
 			return isChk;
 		}
				
		private void saveSelections() {
			String prefs="";
			bSaveStaticSelect = new boolean[chkStaticColNames.length];
			for(int x=0; x<bSaveStaticSelect.length; x++) {
				boolean b = chkStaticColNames[x].isSelected();
				bSaveStaticSelect[x] = b;
				if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
			}
			prefsRoot.put(prefLabel, prefs);
		}
		
		private void restoreSelections() {
			if(bSaveStaticSelect != null)
				for(int x=0; x<bSaveStaticSelect.length; x++)
					chkStaticColNames[x].setSelected(bSaveStaticSelect[x]);
		}

		private void initSelections() {
			totalColumns = chkStaticColNames.length;
			
			prefsRoot = theMainFrame.getPreferencesRoot();
			prefLabel = theMainFrame.getdbName() + seqPref; // sTCW database name + _hitCol
			String seqCol = prefsRoot.get(prefLabel, null); // sets to null if not set yet
			int cnt=0;
			
			if (seqCol!=null) {
				String [] list = seqCol.split("\t");	
				for (int i=0; i<list.length; i++) {
					int x = Static.getInteger(list[i]);
					if (x<0 || x>=totalColumns) continue; 
					
					cnt++;
					chkStaticColNames[x].setSelected(true);
				}
			}
			if (cnt==0) {
				for (int x=0; x<chkStaticColNames.length; x++) {
					if (isOnByDefault(x)) chkStaticColNames[x].setSelected(true);
				}
			}
			bSaveStaticSelect = new boolean [totalColumns];
			saveSelections();
		}
		private JPanel selectPanel = null;
		private JButton btnOK = null, btnCancel = null;
		
		// these change based on isGrpView
		private int totalColumns=0;
		private JCheckBox [] chkStaticColNames = null;
		private boolean [] bSaveStaticSelect = null;
		
		private String prefLabel = "";
		private Preferences prefsRoot = null;
	} // end column panel
	
	/****************************************************************
	 * Database query
	 ***************************************************************/
	private void loadDataForTable(boolean isBuild)
	{
		final boolean bBuild = isBuild; 
		Thread thread = new Thread(new Runnable() {
		public void run() {
			try {	
				theParentTab.setStatus("Loading Sequences. Please Wait..");
				queryPanel.enableAddToTable(false);
					
				String searchStr = queryPanel.getSearchStr();
				String statusSearch = queryPanel.getStatusStr();
				
				ArrayList<Object []> results = loadFromDatabase(false);
				if(results.isEmpty() && queryPanel.loadList==null 
						&& !searchStr.equals("") && !searchStr.contains("%")) {
					results = loadFromDatabase(true); // search with wild chars
					statusSearch = queryPanel.getStatusCol() + " contains " + statusSearch;
				}
				else if (!statusSearch.equals(""))
					statusSearch = queryPanel.getStatusCol() + " = " + statusSearch;
				
				statusSearch = "Filter: " + statusSearch;
				
				queryPanel.enableAddToTable(true);
				if (bBuild) theParentTab.tableBuild(results, statusSearch);
				else  theParentTab.tableAdd(results, statusSearch);
			} catch (Exception err) {
				queryPanel.enableAddToTable(true);
				theParentTab.setStatus("Error during query");
				JOptionPane.showMessageDialog(null, "Query failed due to unknown reasons ");
				ErrorReport.reportError(err, "Internal error: building hit table");
			}
		}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	 public ArrayList<Object []> loadFromDatabase (boolean bSecond) throws Exception
	 {
        try {
    	  		String searchStr = queryPanel.getSearchStr();
    	  		if (bSecond) searchStr = Static.addWildDB(searchStr);
    	  		String searchCol = queryPanel.getSearchCol();
    	  		
		    String strQuery = "SELECT " + mysqlCols + " FROM contig";
		  	
	        if (!searchStr.equals("")) {
	        		strQuery += " where " + searchCol;
	        		if (queryPanel.loadList!=null)    strQuery+= " IN ("  + searchStr + ")"; 
	        		else if (searchStr.contains("%")) strQuery+= " LIKE " + searchStr; 
	        		else                              strQuery+= " = "    + searchStr; 
	        		strQuery += " ORDER BY contig.contigid ASC";
	        }
	        else {
	        	 	strQuery += " ORDER BY contig.contigid ASC";       
	        }
	        DBConn dbc = theMainFrame.getNewDBC();
	        ResultSet rset = dbc.executeQuery( strQuery );
	        
	        int numStaticFields= staticColNames.length;
	      
	        ArrayList<Object []> retVal = new ArrayList<Object []> ();
	       
	        while( rset.next() ) {
	        		Object [] buffer = new Object[numStaticFields];
	        	 	for (int i=0; i<numStaticFields; i++) { 
	        	 		if (staticColTypes[i] == String.class) buffer[i] = rset.getString((i+1));
	        	 		else                                   buffer[i] = rset.getInt((i+1));
	        	 	}
	    			retVal.add(buffer);
	    		}
	        rset.close(); dbc.close(); 
	    		return retVal;
	    }
	    catch(Exception e) {
	        	ErrorReport.reportError(e,"Error: reading database loadFromDatabase");
	    }
        return null;
	}
	
	// Columns for table
	private String [] staticColNames = null;
	private Class <?> [] staticColTypes = null;
	private String mysqlCols="";
	
	// main panel
	private QueryPanel queryPanel = null;
	
	// sub panels
	private ColumnPanel columnPanel = null;

	private BasicSeqQueryTab theParentTab = null;
	private STCWFrame theMainFrame = null;
	private JButton btnFindFile = null;
}
