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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import sng.database.Globals;
import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileRead;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

public class BasicSeqFilterPanel extends JPanel {
	private static final long serialVersionUID = -434438354961245720L;
	private static final Color BGCOLOR = Globals.BGCOLOR;
	
	private final String topHTML = 		Globals.helpDir + "BasicTopSeq.html";
	private final String queryHTML = 	Globals.helpDir + "BasicQuerySeq.html";
	private final String lowerHTML =   	Globals.helpDir + "BasicModify.html"; 
	
	private static final String seqPref= "_seqPrefs"; // save to preferences; same columns for hit&seq
	
	private static final int BUILD=0, ADD=1, SELECT=2; // CAS334 add select
	
	// Any change here will work in this file, but needs to be changed in BasicSeqQueryTab.SeqData class!!!
	// These are columns from database and do not include row#
	// Do not move Seq ID, as it is expected to be index 0
	private static final String rowCol = "Row";
	private static int idxLong = 1;
	private static String [] STATIC_COLUMNS   = { "Seq ID", "Longest", "TCW Remark", "User Remark", "Counts",  "Best HitID"};
	private static final Class<?> [] COLUMN_TYPES =   { String.class, String.class, String.class, String.class , Integer.class,  String.class}; 
	private static final String [] MYSQL_COLUMNS = { // CAS327 moved longest after contigid
		"contig.contigid", "contig.longest_clone ", "contig.notes", "contig.user_notes", 
		"contig.totalexp", "contig.bestmatchid"};
	
	/**********************************************
	* The top queries, and 4 selection panels 
	*/
	public BasicSeqFilterPanel(STCWFrame frame, BasicSeqTab parentTab) {
		theParentTab = parentTab;
		theMainFrame = frame;
		
		bUseOrigName = theMainFrame.getMetaData().bUseOrigName(); // CAS311
		longLabel = theMainFrame.getMetaData().getLongLabel(); // CAS311
		STATIC_COLUMNS[idxLong] = longLabel;
		
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
		private static final int MAIN_PANEL_LABEL_WIDTH = 70; // labels on left

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
			
			radLong = Static.createRadioButton(longLabel, true); // CAS311 add, CAS326 create even if not use
			if (!bUseOrigName) {
				row1.add(radLong); row1.add(Box.createHorizontalStrut(2));
			}
			radTCW = Static.createRadioButton("TCW", false);
			row1.add(radTCW);	row1.add(Box.createHorizontalStrut(2));
			
			radUser = Static.createRadioButton("User", false);
			row1.add(radUser);	row1.add(Box.createHorizontalStrut(8));
			
			ButtonGroup allbg = new ButtonGroup();
			allbg.add(radSeqID); 
			allbg.add(radLong);
			allbg.add(radTCW); 
			allbg.add(radUser); 
			
			row1.add(new JLabel("Substring: "));
			txtField  = Static.createTextField("", 20, true);
			row1.add(txtField); row1.add(Box.createHorizontalStrut(5));
			
			btnFindFile = Static.createButton("Load File", true);
			btnFindFile.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						FileRead fr = new FileRead("Seq", FileC.bNoVer, FileC.bNoPrt); // CAS316
						if (fr.run(btnFindFile, "Seq File", FileC.dRESULTEXP, FileC.fTXT)) {
							loadFile(fr.getRelativeFile());
						}
					}
					catch(Exception e) {ErrorReport.prtReport(e, "Error finding file");}
				}});
			row1.add(btnFindFile); row1.add(Box.createHorizontalStrut(3));
			
			JButton clearall = new JButton("Clear");
			clearall.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
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
			
			btnBuildTable = Static.createButton("BUILD", true, Globals.FUNCTIONCOLOR);
			btnBuildTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					loadDataForTable(BUILD);
				}
			});
			btnAddTable = Static.createButton("ADD", true, Globals.FUNCTIONCOLOR);
			btnAddTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					loadDataForTable(ADD);
				}
			});
			btnSetColumns = Static.createButton("Columns", true, Globals.MENUCOLOR);
			btnSetColumns.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					hideMain();
					columnPanel.setVisible(true);
				}
			});
			createHelp();
			
			JPanel row3 = Static.createRowPanel(); // CAS336 added glue and dropdown help
			Box hzBox = Box.createHorizontalBox();
			
			 hzBox.add(Static.createLabel("Table", true)); hzBox.add(Box.createHorizontalStrut(5));
			 hzBox.add(btnBuildTable);					   hzBox.add(Box.createHorizontalStrut(5));
			 hzBox.add(btnAddTable);
			 
			 hzBox.add(Box.createGlue());
			 hzBox.add(btnSetColumns);
			
			 hzBox.add(Box.createGlue());
			 hzBox.add(btnHelp);
			 row3.add(hzBox);
			
			add(row3);
			
			setMinimumSize(getPreferredSize());
			setMaximumSize(getPreferredSize());
			setVisible(true);
		}
		private void createHelp() {
			final JPopupMenu popup = new JPopupMenu();
			
			popup.add(new JMenuItem(new AbstractAction("Top buttons") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					try {
						UserPrompt.displayHTMLResourceHelp(theMainFrame, "Top Buttons", topHTML);
					} catch (Exception er) {ErrorReport.reportError(er, "Error copying gonum"); }
				}
			}));
			popup.add(new JMenuItem(new AbstractAction("Search and Table") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					try {
						UserPrompt.displayHTMLResourceHelp(theMainFrame, "Search, Filter and Table", queryHTML);
					} catch (Exception er) {ErrorReport.reportError(er, "Error copying gonum"); }
				}
			}));
			popup.add(new JMenuItem(new AbstractAction("Modify Buttons") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					try {
						UserPrompt.displayHTMLResourceHelp(theMainFrame, "Modify Buttons", lowerHTML);
					} catch (Exception er) {ErrorReport.reportError(er, "Error copying gonum"); }
				}
			}));
			
			
			btnHelp = Static.createButton("Help...", true, Globalx.HELPCOLOR);
			btnHelp.addMouseListener(new MouseAdapter() {
	            public void mousePressed(MouseEvent e) {
	                popup.show(e.getComponent(), e.getX(), e.getY());
	            }
	        });
			btnHelp.setAlignmentX(Component.RIGHT_ALIGNMENT);
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
				boolean isName = isName();
				int cnt=0;
				String line;
				BufferedReader file = new BufferedReader(new FileReader(fileName));
				while((line = file.readLine()) != null) {
					line = line.replace("\n","").trim();
					if (line.equals("")) continue;
					if (line.startsWith("#")) continue;
					
					if (isName) {
						String [] tok = line.split("\\s+"); // CAS334 ignore rest of stuff on line
						if (tok.length==0 || tok[0].trim()=="") 
							Out.PrtWarn("Bad Line: " + line);
						else {
							loadList.add(tok[0]);
							cnt++;
						}
					}
					else {
						loadList.add(line);
						cnt++;
					}
				}
				file.close(); 
				if (loadList.size()==0) loadList = null;
				else txtField.setText(loadList.get(0) + ",... (" + cnt + ")");
			}
			catch(Exception e) {ErrorReport.prtReport(e, "Error loading file");}
		}
		public boolean isName() {
			return (radSeqID.isSelected() || radLong.isSelected());
		}
		public String getSearchCol() {
			if (radSeqID.isSelected()) return "contig.contigid";
			else if (radLong!=null && radLong.isSelected()) return "contig.longest_clone"; // CAS326
			else if (radTCW.isSelected()) return "contig.notes";
			else return "contig.user_notes";
		}
		public String getSearchStr() { 
			String x = txtField.getText().trim(); 
			
			if (x.equals("") || x.equals("...") || x.equals("...")) return ""; 
			
			if (x.contains("...") && loadList!=null) {
				return Static.addQuoteDBList(loadList);
			}
			else {
				loadList=null;
				
				x = Static.addQuoteDB(x);
				if (!x.contains("%")) 	x = Static.addWildDB(x);
				
				return x;
			}
		}
		public String getStatusCol() {
			if (radSeqID.isSelected()) return "Seq ID";
			else if (radLong!=null && radLong.isSelected()) return longLabel;
			else if (radTCW.isSelected()) return "TCW Remark";
			else return "User Remark";
		}
		public boolean isLoadFile() { return loadList!=null;}
		public String getStatusStr() { 
			String x = txtField.getText().trim(); 
			if (x.equals("")) return "";
			return Static.addQuote(x); 
		}
		
		// Search:
		public JRadioButton radSeqID = null, radLong=null;
		public JRadioButton radTCW = null, radUser = null;
		public JTextField txtField = null;
		public Vector <String> loadList = null;
		
		// Results
		private JButton btnBuildTable = null, btnAddTable = null;
		private JButton btnSetColumns = null, btnHelp = null;
	} // end QueryPanel
	
	/*************************************************
	 * Column Select Panel
	 */
	private class ColumnPanel extends JPanel {
		private static final long serialVersionUID = -49938519942155818L;

		public ColumnPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			
			JPanel headerPanel = Static.createRowCenterPanel();
			JLabel theHeader = new JLabel("<HTML><H2>Select columns to view</H2></HTML>");
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			Static.center(headerPanel);
			
			add(Box.createVerticalStrut(30));
			add(headerPanel);
			add(Box.createVerticalStrut(10));
			
			createSelectPanel();
			add(selectPanel);
			add(Box.createVerticalStrut(20));
			
			btnOK = Static.createButton("Accept", true);
			btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSelections();
					showMain();
					setVisible(false);
					theParentTab.tableRefresh();
				}
			});
			btnCancel = Static.createButton("Discard", true);
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreSelections();
					showMain();
					setVisible(false);
				}
			});
			JPanel buttonPanel = Static.createRowCenterPanel();
			buttonPanel.add(btnOK);		buttonPanel.add(Box.createHorizontalStrut(10));
			buttonPanel.add(btnCancel);
			Static.center(buttonPanel);
			add(buttonPanel);
			
			setVisible(false);
		}
 		private void createSelectPanel() {
 			String [] staticNames = new String [staticColNames.length+1];
 			staticNames[0] = rowCol;
 			for (int i=0; i<staticColNames.length; i++) staticNames[i+1] = staticColNames[i];
 					
		/** Static columns **/ 
			JPanel staticPanel = Static.createPageCenterPanel();
			chkStaticColNames = new JCheckBox[staticNames.length];	
			for(int x=0; x<staticNames.length; x++) {
				chkStaticColNames[x] = Static.createCheckBox(staticNames[x], false);
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
			final JCheckBox sAll = Static.createCheckBox("check/uncheck all", true);
			sAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					for(int x=0; x<chkStaticColNames.length; x++)
						chkStaticColNames[x].setSelected(sAll.isSelected());
					setOKEnabled();
				}
			});
			genCheck.add(sAll);
			Static.border(genCheck);
			staticPanel.add(genCheck);	
			
			selectPanel = Static.createRowCenterPanel();
			selectPanel.add(staticPanel);
			Static.center(selectPanel);
			
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
					chkStaticColNames[x].setSelected(true);
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
	public void loadSelect() { // called from BasicTablePanel for "Select Query"
		loadDataForTable(SELECT);
	};
	private void loadDataForTable(int type){
		final int iType = type; 
		Thread thread = new Thread(new Runnable() {
		public void run() {
			try {	
				theParentTab.setStatus("Loading Sequences. Please Wait..");
				theParentTab.enableButtons(false);
					
				ArrayList<Object []> results = loadFromDatabase();
				
				// CAS327 changed rules to always use contains (was doing =, and if failed, do contain)
				String statusSearch = queryPanel.getStatusStr();
				if (!statusSearch.equals("")) {
					String op = (queryPanel.isLoadFile()) ? " = " : " contains ";
					statusSearch = queryPanel.getStatusCol() + op + statusSearch;
					statusSearch = "Search: " + statusSearch;
				}
				
				if (iType==0) 		theParentTab.tableBuild(results, statusSearch);
				else if (iType==1)  theParentTab.tableAdd(results, statusSearch);
				else 				theParentTab.tableSelect(results, statusSearch);
				
				theParentTab.enableButtons(true); // CAS334 moved after Build/Add/Select
				
			} catch (Exception err) {
				theParentTab.enableButtons(true);
				theParentTab.setStatus("Error during query");
				JOptionPane.showMessageDialog(null, "Query failed due to unknown reasons ");
				ErrorReport.reportError(err, "Internal error: building hit table");
			}
		}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	private ArrayList<Object []> loadFromDatabase () { // called from above
        try {
		    String strQuery = "SELECT " + mysqlCols + " FROM contig";
		  	
		    String searchStr = queryPanel.getSearchStr();
		    if (!searchStr.equals("")) {
	        	strQuery += " where " + queryPanel.getSearchCol();
	        	
	        	if (queryPanel.isLoadFile())  strQuery+= " IN ("  + searchStr + ")"; 
	        	else 						  strQuery+= " LIKE " + searchStr; 
	        }
	        strQuery += " ORDER BY contig.contigid ASC"; 
	        
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
	    catch(Exception e) {ErrorReport.reportError(e,"Reading database loadFromDatabase");}
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

	private BasicSeqTab theParentTab = null;
	private STCWFrame theMainFrame = null;
	private JButton btnFindFile = null;
	
	private String longLabel = "";
	private boolean bUseOrigName=false;
}
