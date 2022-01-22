package cmp.viewer.pairs;
/********************************************
 * Shows Pairs.
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.Vector;
import java.util.HashSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;
import cmp.align.PairSumStats;
import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.ViewerSettings;
import cmp.viewer.align.AlignButtons;
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.seq.SeqsTablePanel;
import cmp.viewer.table.*;

public class PairTablePanel extends JPanel {
	private static final long serialVersionUID = -3827610586702639105L;
	private static final String helpHTML = Globals.helpDir + "PairTable.html";
	
	private static final String TABLE =   FieldData.PAIR_TABLE;
	private static final String PAIRID =  FieldData.PAIRID;
	private static final String SEQ_ID =  FieldData.SEQ_SQLID; // CAS310 FieldData.SEQ_TABLE + "." + FieldData.SEQ_SQLID;
	private static final String SEQ_ID1 = FieldData.SEQID1; 
	private static final String SEQ_ID2 = FieldData.SEQID2; 
	private static final String ID1_SQL = FieldData.ID1_SQLID; 
	private static final String ID2_SQL = FieldData.ID2_SQLID; 	
	private static final String HITID =   FieldData.HITID;
	private static final String HITDESC = FieldData.HITDESC;
	
	// Filter pairs or >Pairs
	public PairTablePanel(MTCWFrame parentFrame, String tab) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true; // >Pairs
		
		initData(parentFrame, tab);	
		
		PairQueryPanel theQueryPanel = theViewerFrame.getPairQueryPanel();
		if (theQueryPanel!=null) {
			strSubQuery = theQueryPanel.getSubQuery();
			strSummary = Globals.trimSum(theQueryPanel.getQuerySummary());
		}
		else loadShortList();
		strSummary = Globals.FILTER + strSummary;
		
		buildQueryThread(); 
	}
	
	// from seq table
	public PairTablePanel(MTCWFrame parentFrame, SeqsTablePanel parentList, String list, String tab,  String sum,  int row) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		
		theSeqTable = parentList;
		seqList = list;
		strSummary =  Globals.trimSum(sum);
		nParentRow = row;
		
		initData(parentFrame, tab);	
		
		buildQueryThread(); 
	}
	
	// From groups table
	public PairTablePanel(MTCWFrame parentFrame, GrpTablePanel parentList, String where, String tab,  String sum,  int row) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		
		theGrpTable = parentList;
		nParentRow = row;
		grpWhere = where;
		strSummary = Globals.trimSum(sum);
		
		initData(parentFrame, tab);	
		
		buildQueryThread(); 
	}
	
	/** XXX Buttons for Pair table */
    private JPanel createTableButton() {
    	JPanel buttonPanel = Static.createPagePanel();
	    	
    	JPanel topRow = Static.createRowPanel();
	    topRow.add(Static.createLabel(Globals.select));	topRow.add(Box.createHorizontalStrut(2));
    	   	
         btnTableSeqs = Static.createButtonTab(Globals.SEQ_TABLE, false);
         btnTableSeqs.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent arg0) {
 				viewSeqs();
 			}
 		});
        topRow.add(btnTableSeqs);				topRow.add(Box.createHorizontalStrut(2));  
        
        // CAS310 add
        btnTableGrps = Static.createButtonTab(Globals.GRP_TABLE, false);
        btnTableGrps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewGrps();
			}
		});
       topRow.add(btnTableGrps);				topRow.add(Box.createHorizontalStrut(2)); 
       
       AlignButtons bObj = new AlignButtons(theViewerFrame, getInstance()); // CAS340 new
       btnPairwise = bObj.createBtnPairAlign();
       topRow.add(btnPairwise);					topRow.add(Box.createHorizontalStrut(2)); 
         	
        createBtnCopy();
 		topRow.add(btnCopy);					topRow.add(Box.createHorizontalGlue());  
		
        createBtnTable();
        topRow.add(btnTable);					topRow.add(Box.createHorizontalStrut(2));
        
        btnHelp = Static.createButtonHelp("Help", true);
        btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Pair table",  helpHTML);
			}
		});
        topRow.add(btnHelp);					topRow.add(Box.createHorizontalStrut(2)); 

        if(nParentRow >= 0) { // if -1, then showing members from multiple clusters, and no Next/Prev
 	 	   JPanel rowChangePanel = Static.createRowPanel();
 	 	   rowChangePanel.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
 	 	   
 	 	   btnPrevRow = Static.createButton(Globals.prev, true);
 	 	   btnPrevRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   getNextRow(nParentRow - 1); // CAS340 nParentRow was missing
 	 		   }
 	 	   }); 
 	 	   btnNextRow = Static.createButton(Globals.next, true);
 	 	   btnNextRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   getNextRow(nParentRow + 1); // CAS340 nParentRow was missing
 	 		   }
 	 	   });
 	 	   rowChangePanel.add(btnPrevRow);		rowChangePanel.add(Box.createHorizontalStrut(1));
 	 	   rowChangePanel.add(btnNextRow);
 	 	   
 	 	   topRow.add(rowChangePanel);
 	    }
        topRow.setAlignmentX(LEFT_ALIGNMENT);

        buttonPanel.add(topRow);
        buttonPanel.add(Box.createVerticalStrut(5));
        
        return buttonPanel;
    }
    
    private void createBtnCopy() {
    	btnCopy = Static.createButtonMenu("Copy...", false);
 	    final JPopupMenu copyPop = new JPopupMenu();
 	    copyPop.setBackground(Color.WHITE); 
 	    
 	    copyPop.add(new JMenuItem(new AbstractAction("SeqID1") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				int row = theTable.getSelectedRow();
 				int idx = theTableData.getColumnHeaderIndex(SEQ_ID1);
 				String seqID =  ((String)theTableData.getValueAt(row, idx));
 				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
 				cb.setContents(new StringSelection(seqID), null);
 			}
 		}));
 	   copyPop.add(new JMenuItem(new AbstractAction("SeqID2") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				int row = theTable.getSelectedRow();
				int idx = theTableData.getColumnHeaderIndex(SEQ_ID2);
				String seqID =  ((String)theTableData.getValueAt(row, idx));
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(seqID), null);
			}
		}));
  		copyPop.add(new JMenuItem(new AbstractAction("HitID") {
  			private static final long serialVersionUID = 4692812516440639008L;
  			public void actionPerformed(ActionEvent e) {
  				int row = theTable.getSelectedRow();
  				int idx = theTableData.getColumnHeaderIndex(HITID);
  				String seqID =  ((String)theTableData.getValueAt(row, idx));
  				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
  				cb.setContents(new StringSelection(seqID), null);
  			}
  		}));
  		copyPop.add(new JMenuItem(new AbstractAction("Hit Description") {
  			private static final long serialVersionUID = 4692812516440639008L;
  			public void actionPerformed(ActionEvent e) {
  				int row = theTable.getSelectedRow();
  				int idx = theTableData.getColumnHeaderIndex(HITDESC);
  				String desc =  ((String)theTableData.getValueAt(row, idx));
  				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
  				cb.setContents(new StringSelection(desc), null);
  			}
  		}));
  		btnCopy.addMouseListener(new MouseAdapter() {
  			public void mousePressed(MouseEvent e) {
  				copyPop.show(e.getComponent(), e.getX(), e.getY());
  			}
 	    });
    }
    private void createBtnTable() {
		final JPopupMenu popup = new JPopupMenu();
		popup.setBackground(Color.WHITE);
		
		popup.add(new JMenuItem(new AbstractAction("Show Column Stats") {
 		   private static final long serialVersionUID = 1L;
 		   public void actionPerformed(ActionEvent e) {
				new TableUtil().statsPopUp("Pairs: " + strSummary, theTable);
 		   }
	 	}));
		if (!hasAAdb) {
	 	    popup.add(new JMenuItem(new AbstractAction("Show Table Stats (Slow)") {
				private static final long serialVersionUID = 1L;
					public void actionPerformed(ActionEvent e) {
	 	    			try{			
	 					Vector <Integer> ids = getPairIDforShowTable();
	 					DBConn mDB = theViewerFrame.getDBConnection();
	 					String summary = "Pairs: " + strSummary;
	 					new PairSumStats(mDB).fromView(ids, summary);
	 					// close in SumStats
	 				} catch(Exception ee) {ErrorReport.reportError(ee,  "View stats");
	 				} catch(Error ee) {Out.prt("Error");ErrorReport.reportFatalError(ee, "View stats", theViewerFrame);}
	 	    	}
	 	   }));
		}
		popup.addSeparator();      
        popup.add(new JMenuItem(new AbstractAction("Copy Table") { 
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				try {
					String table = new TableUtil(theViewerFrame).createTableString(theTable);
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(table), null);
				} catch (Exception ex) {ErrorReport.reportError(ex, "Copy table");}
 			}
 		}));
        popup.addSeparator();
 		popup.add(new JMenuItem(new AbstractAction("Export table (" + Globalx.CSV_SUFFIX + ")") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportTableTab(btnTable, theTable, Globals.bPAIR);
 			}
 		}));
 		popup.add(new JMenuItem(new AbstractAction("Export both AA sequences of pairs (" + Globalx.FASTA_SUFFIX + ")") { 
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportPairSeqFa(btnTable,theTableData, 0, FieldData.AASEQ_SQL);
 			}
 		}));
 		if (!hasAAdb) {
	 		popup.add(new JMenuItem(new AbstractAction("Export both NT sequences of pairs ("+ Globalx.FASTA_SUFFIX + ")") { 
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportPairSeqFa(btnTable,theTableData, 0, FieldData.NTSEQ_SQL);
	 			}
	 		}));
 		}
 		popup.add(new JMenuItem(new AbstractAction("Export 1st AA sequence of pairs (" + Globalx.FASTA_SUFFIX + ")") { 
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportPairSeqFa(btnTable,theTableData, 1, FieldData.AASEQ_SQL);
 			}
 		}));
 		
 		if (!hasAAdb) {
	 		popup.add(new JMenuItem(new AbstractAction("Export 1st NT sequence of pairs (" + Globalx.FASTA_SUFFIX + ")") { 
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportPairSeqFa(btnTable,theTableData, 1, FieldData.NTSEQ_SQL);
	 			}
	 		}));
 		}
 
 		if (hasGOs) {
 			popup.addSeparator();
	 		popup.add(new JMenuItem(new AbstractAction("Export Pairs GOs (" + Globalx.CSV_SUFFIX + ")...") { 
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportPairGO(btnTable,theTableData, strSummary);
	 			}
	 		}));
 		}
 	
 		btnTable = Static.createButtonMenu("Table...", true);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }
	private void initData(MTCWFrame parentFrame, String resultName) {
		theViewerFrame = parentFrame;
		tabName = resultName;
		
		hasAAdb = (theViewerFrame.getnAAdb()>0);
		vSettings = parentFrame.getSettings();
		
		totalPairs = theViewerFrame.getInfo().getCntPair();
		methods =    theViewerFrame.getInfo().getMethodPrefix();
		hasGOs =    (theViewerFrame.getInfo().getCntGO()>0);

		colSelectChange = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateTable(false);
				displayTable();
				vSettings.getPairSettings().setSelectedColumns(getSelectedColumns());
			}
		};
	}
	
	/**************************************************************
	 * Perform query and build the panel with results
	 */
	private void buildQueryThread() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setAlignmentY(LEFT_ALIGNMENT);

        showProgress();
		buildThread = new Thread(new Runnable() {
			public void run() {
				try {
					DBConn mdb = theViewerFrame.getDBConnection();
					String sql = buildQueryStr(mdb);
					ResultSet rs = MTCWFrame.executeQuery(mdb, sql, loadStatus);
					if(rs != null) {
						buildTable(rs);
						MTCWFrame.closeResultSet(rs); //Thread safe way of closing the resultSet
						updateTable(true);
					}
					mdb.close();
					
					displayTable();
					
					if(isVisible()) {//Makes the table appear (A little hacky, but fast)
						setVisible(false);
						setVisible(true);
					}
				} catch (Exception e) {ErrorReport.reportError(e, "generating pairs table");
				} catch (Error e) {ErrorReport.reportFatalError(e, "generating pairs table", theViewerFrame);}
			}
		});
		buildThread.setPriority(Thread.MIN_PRIORITY);
		buildThread.start();
	}
	/*****************************************************************
	 * Build interface
	 */
	private void buildTable(ResultSet rs) {
        FieldData theFields = FieldData.getPairFields(methods);
    
        tableButtonPanel = createTableButton();    
		tableStatusPanel = createTableStatusPanel();	
		tableSummaryPanel = createTableSummaryPanel();
		fieldSelectPanel = createFieldSelectPanel();
		fieldSelectPanel.setVisible(false);
	
		showColumnSelect = Static.createButtonPanel("Select Columns", true);
		showColumnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(fieldSelectPanel.isVisible()) {
					showColumnSelect.setText("Select Columns");
					fieldSelectPanel.setVisible(false);
				}
				else {
					showColumnSelect.setText("Hide Columns");
					fieldSelectPanel.setVisible(true);
				}
				displayTable();
			}
		});
		clearColumn = Static.createButton("Clear Columns", true);
		clearColumn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearColumns();
				updateTable(false);
				displayTable();
			}
		});
		
		txtStatus = Static.createTextFieldNoEdit(100);
        dblClick = new ActionListener() {
			public void actionPerformed(ActionEvent e) {	}
		};
		sngClick = new ActionListener() {
			public void actionPerformed(ActionEvent e) {}
		};
		selListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				int rowCount = theTable.getSelectedRowCount();
				if(rowCount >= 1000)
					txtStatus.setText("Cannot select more than 1000 rows at a time");
				else {
					txtStatus.setText("");
					setTopEnabled();
				}
			}
		};

        theTableData = new TableData(this);
        theTableData.setColumnHeaders(theFields.getDisplayNames(), theFields.getDisplayTypes());
        theTableData.addRowsWithProgress(rs, theFields, loadStatus);
        theTableData.showTable();

        int nRow = theTableData.getNumRows();
        String status =  nRow + " of " + totalPairs + " " + Static.perText(nRow, totalPairs);
        tableHeader.setText(status);
        
        if(!isList) {
           theViewerFrame.changePanelName(this, tabName + ": " + nRow, strSummary);
        }
    }
	private void setTopEnabled() {
		int selCount = theTable.getSelectedRowCount();
	
		boolean b = (selCount>0);
		btnTableSeqs.setEnabled(b); 
		btnTableGrps.setEnabled(b); 
		btnPairwise.setEnabled(b);
		
		btnCopy.setEnabled(selCount==1);
	}
    private JPanel createFieldSelectPanel() {
    	JPanel page = Static.createPagePanel();
    	
    	String [] sections =     FieldData.getPairColumnSections();
    	int [] secIdx = 	     FieldData.getPairColumnSectionIdx(methods.length);
    	int [] secBreak = 	     FieldData.getPairColumnSectionBreak();
    	String [] columns = 	 FieldData.getPairColumns(methods);
    	String [] descriptions = FieldData.getPairDescript(methods);
    	boolean [] defaults =    FieldData.getPairSelections(methods.length);
	    boolean [] selections = getColumnSelections(columns, defaults, 
    					theViewerFrame.getSettings().getPairSettings().getSelectedColumns());    
	    	
	    chkFields = new JCheckBox[columns.length];
	
		int maxWidth=0;	
    	for(int x=0; x<columns.length; x++) {
    		final String desc = descriptions[x];
    		
        	chkFields[x] = Static.createCheckBox(columns[x]);
        	chkFields[x].addActionListener(colSelectChange);
        	chkFields[x].addMouseListener(new MouseAdapter() 
        	{
        		public void mouseEntered(MouseEvent e) {
        			theViewerFrame.setStatus(desc);
        		}
        		public void mouseExited(MouseEvent e) {
        			theViewerFrame.setStatus("");
        		}
        	});
        	chkFields[x].setSelected(selections[x]);
        	int w = chkFields[x].getPreferredSize().width;
        	if (w>maxWidth) maxWidth=w;
    	}
	    	
	    JPanel row = Static.createRowPanel();
		page.add(new JLabel(sections[0]));
		int hIdx = 0, hBreak = 0;
		
	    for(int x=0; x<columns.length; x++) {   	
        	if (hBreak < secBreak.length && secBreak[hBreak] == x) {
        		page.add(row);
        		row = Static.createRowPanel();
        		hBreak++;
        	}
        	else if (hIdx < secIdx.length && secIdx[hIdx] == x) {
    			page.add(row);
        		row = Static.createRowPanel();
        		
    			if(sections[hIdx+1].length() > 0) {
    				page.add(Box.createVerticalStrut(10));
    				page.add(new JLabel(sections[hIdx+1]));
    			}
    			hIdx++;
        	}
        	
    		row.add(chkFields[x]);
    	 	int colwidth = maxWidth - chkFields[x].getPreferredSize().width;
    	 	int space=2; 
        	if(colwidth > 0) space += colwidth;
    		row.add(Box.createHorizontalStrut(space));
    	}
    	
    	if(row.getComponentCount() > 0) page.add(row);
	
    	page.setBorder(BorderFactory.createTitledBorder("Columns"));
    	page.setMaximumSize(page.getPreferredSize());

    	return page;
	}
    private JPanel createTableStatusPanel() {
    	JPanel thePanel = Static.createRowPanel();
    	tableType = Static.createTextFieldNoEdit(20);
    	Font f = tableType.getFont();
    	tableType.setFont(new Font(f.getFontName(),Font.BOLD, f.getSize()));
    	tableType.setMaximumSize(tableType.getPreferredSize());
    	tableType.setAlignmentX(LEFT_ALIGNMENT);

    	tableHeader =  Static.createTextFieldNoEdit(30);
    	thePanel.add(tableType);
    	thePanel.add(tableHeader);
    	thePanel.setMaximumSize(thePanel.getPreferredSize());
    	
    	return thePanel;
	}
    private JPanel createTableSummaryPanel() {
    	JPanel thePanel = Static.createRowPanel();
    
    	lblSummary = Static.createLabel(strSummary, true);
    	lblSummary.setFont(getFont());
    	thePanel.add(lblSummary);
    	
    	return thePanel;
	}
	/*********************************************************************************/
	public String getSummary() { return strSummary; }
	private PairTablePanel getInstance() { return this; }
	
	private String buildQueryStr(DBConn mdb) {
        try {
    		FieldData theFields = FieldData.getPairFields(methods);
        	
    		String strQuery = "SELECT " + theFields.getDBFieldQueryList() + " FROM " + TABLE + " ";
        	
        	strQuery += theFields.getJoins();
        	
        	String cntSQL=null;
      
        	if (grpWhere!=null) {// list pairs in cluster; called from group table e.g. CL like CL_00001
        		strQuery += " WHERE " + grpWhere;
        		cntSQL = "select count(*) from " + TABLE + " WHERE " + grpWhere;
        	}
        	else if (seqList!=null) { // seqList example "(1,2,3)"
        		String clause = "UTid1 IN " + seqList + " or UTid2 IN " + seqList;
        		strQuery += " WHERE " + clause;
        		cntSQL = "select count(*) from " + TABLE + " WHERE " + clause;
        	}
        	else {
        		strQuery += " WHERE " + strSubQuery;
        		cntSQL = "select count(*) from " + TABLE + " where " + strSubQuery;
        	}
        	if (cntSQL.contains("unique_hits") || (strSubQuery==null || strSubQuery.equals(""))) {
        		loadStatus.setText("Getting filtered pairs from database");
        	}
        	else {
        		int cnt = mdb.executeCount("select count(*) from " + TABLE + " where " + strSubQuery);
        		String per = Static.perText(cnt, theViewerFrame.getInfo().getCntPair());
	        	loadStatus.setText("Getting " + cnt + " " + per + " filtered pairs from database");
        	}
        	
    		return strQuery;
        } catch(Exception e) {ErrorReport.reportError(e, "Error processing query");return null;}
	}
	private void loadShortList() {
		try {
			DBConn mDB = theViewerFrame.getDBConnection(); // CAS310
			DBinfo info = theViewerFrame.getInfo();
			String x = info.getSamplePair(mDB);
			mDB.close();
			
			if (x==null || x=="") return;
			String [] y = x.split(":");
			strSubQuery = y[1];
			strSummary = y[0];
		} catch(Exception e) {ErrorReport.reportError(e, "Error processing sample for Pairs");}
	}
	private void clearColumns() {
		chkFields[0].setSelected(false);
		chkFields[1].setSelected(false);  
		chkFields[2].setSelected(true);  // SeqID1
		chkFields[3].setSelected(true);  // SeqID2
		for(int x=4; x<chkFields.length; x++) {
			chkFields[x].setSelected(false);
		}
	}
    private String [] getSelectedColumns() {
    	String [] retVal = null;
    	
    	int selectedCount = 0;
    	for(int x=0; x<chkFields.length; x++) 
    		if(chkFields[x].isSelected())
    			selectedCount++;
    	
    	if(selectedCount == 0) {
			for(int x=0; x<FieldData.PAIR_DEFAULT.length; x++) {
				chkFields[x].setSelected(FieldData.PAIR_DEFAULT[x]);
				if(chkFields[x].isSelected())
					selectedCount++;
			}
    	}
    	retVal = new String[selectedCount];
    	int targetIndex = 0;
    	for(int x=0; x<chkFields.length; x++) 
    		if(chkFields[x].isSelected()) {
    			retVal[targetIndex] = chkFields[x].getText();
    			targetIndex++;
    		}
    	
    	return retVal;
    }
    private boolean [] getColumnSelections(String [] columns, 
		boolean [] defaultSelections, String [] selections) {

    	if(selections == null || selections.length == 0)
    		return defaultSelections;
    	Vector<String> sels = new Vector<String> ();
    	boolean [] retVal = new boolean[columns.length];
    	for(int x=0; x<selections.length; x++)
    		sels.add(selections[x]);
    	for(int x=0; x<columns.length; x++) {
    		retVal[x] = sels.contains(columns[x]);
    	}
    	return retVal;
    }
    
    /**********************************************************************************
    * Table: When the view table gets sorted, sort the master table to match (Called by TableData)
    ******************************************************************************/
    public void sortMasterColumn(String columnName, boolean ascending) {
	    int index = theTableData.getColumnHeaderIndex(columnName);
	    theTableData.sortByColumn(index, ascending);
    }
    private void showProgress() {
    	removeAll();
    	repaint();
    	setBackground(Static.BGCOLOR);
    	loadStatus = new JTextField(100);
    	loadStatus.setBackground(Static.BGCOLOR);
    	loadStatus.setMaximumSize(loadStatus.getPreferredSize());
    	loadStatus.setEditable(false);
    	loadStatus.setBorder(BorderFactory.createEmptyBorder());
    	
    	JButton btnStop = Static.createButton("Stop");
    	btnStop.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			//This informs the user that it is canceled AND the signal for the thread to stop. 
			if(buildThread != null)
				loadStatus.setText("Cancelled");
		}
		});
        add(loadStatus);
        add(btnStop);
        validateTable();
    }
    
    // Build table and refresh with column change
    private void displayTable() {
    	removeAll();
    	repaint();
    	loadStatus = null;
    	sPane = new JScrollPane();
    	sPane.setViewportView(theTable);
    	theTable.getTableHeader().setBackground(Static.BGCOLOR);
    	sPane.setColumnHeaderView(theTable.getTableHeader());
    	sPane.setAlignmentX(Component.LEFT_ALIGNMENT);
    	
    	sPane.getViewport().setBackground(Static.BGCOLOR);
    	sPane.getHorizontalScrollBar().setBackground(Static.BGCOLOR);
    	sPane.getVerticalScrollBar().setBackground(Static.BGCOLOR);
    	sPane.getHorizontalScrollBar().setForeground(Static.BGCOLOR);
    	sPane.getVerticalScrollBar().setForeground(Static.BGCOLOR);
    	
    	if(tableButtonPanel != null) { 	//Is null if a sample table 
    		add(tableButtonPanel);
    	}
    	add(Box.createVerticalStrut(10));
    	add(tableSummaryPanel);
    	add(Box.createVerticalStrut(10));
    	add(tableStatusPanel);
    	add(sPane);
    	add(fieldSelectPanel);
    	add(Box.createVerticalStrut(10));
    
    	JPanel temp = Static.createRowPanel();
    	temp.add(showColumnSelect);
    	temp.add(Box.createHorizontalStrut(5));
    	temp.add(clearColumn);
    	temp.add(Box.createHorizontalStrut(5));
    	temp.add(txtStatus);
    	temp.setMaximumSize(temp.getPreferredSize());
    	add(temp);
    	if(theTable != null) {
    	 	if (theGrpTable!=null) 		tableType.setText("Pair View for Cluster");
    	 	else if (theSeqTable!=null) tableType.setText("Pair View for Sequence");
    	 	else 						tableType.setText("Pair View");
    	 	setTopEnabled();
    	}
    	invalidate();
    	validateTable();
    }
  //When a column is selected/removed this is called to set the new model
  	private void updateTable(boolean loadMaster) {
  		if(theTable != null) {
  			theTable.removeListeners(); //If this is not done, the old table stays in memory
  		}
		if(!loadMaster) {
			String [] columns = TableData.orderColumns(theTable, getSelectedColumns());
			theTable = new SortTable(TableData.createModel(columns, theTableData, getInstance()));
		}
		else 
			theTable = new SortTable(TableData.createModel(getSelectedColumns(), theTableData, this));
  		
      theTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      theTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      theTable.autofitColumns();
      	
      theTable.getSelectionModel().addListSelectionListener(selListener);
      theTable.addSingleClickListener(sngClick);
      theTable.addDoubleClickListener(dblClick);

      theTable.setTableHeader(new SortHeader(theTable.getColumnModel()));
      
      /* CAS304 If a header contains a '\n' multiple lines will appear using this renderer
      MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer();
      Enumeration<TableColumn> en = theTable.getColumnModel().getColumns();
      while (en.hasMoreElements()) {
        (en.nextElement()).setHeaderRenderer(renderer);
      } 
      */
  	}
  	
    //Called from a thread
    private void validateTable() {validate();}
   
    private class SortHeader extends JTableHeader {
		private static final long serialVersionUID = -2417422687456468175L;
    	public SortHeader(TableColumnModel model) {
    		super(model);
    		bColumnAscending = new boolean[model.getColumnCount()];
    		for(int x=0; x<bColumnAscending.length; x++)
    			bColumnAscending[x] = true;
    		
    		addMouseListener(new MouseAdapter() {
            	public void mouseClicked(MouseEvent evt) 
            	{ 
            		try {
	            		SortTable table = (SortTable)((JTableHeader)evt.getSource()).getTable(); 
	            		TableColumnModel colModel = table.getColumnModel(); 
	            		int vColIndex = colModel.getColumnIndexAtX(evt.getX()); 
	            		int mColIndex = table.convertColumnIndexToModel(vColIndex);
	            		if (mColIndex>=0) { 
		            		if(SwingUtilities.isLeftMouseButton(evt)) {
		            			table.sortAtColumn(mColIndex, bColumnAscending[mColIndex]);
		            			bColumnAscending[mColIndex] = !bColumnAscending[mColIndex];
		            		}
		            		else {
		            			bColumnAscending[mColIndex] = false;
		            			table.sortAtColumn(mColIndex, false);
		            		}
	            		}
            		}
	            	catch (Exception e) { ErrorReport.prtReport(e, "Internal mouse click error");}
             }   
    		});
    	}
    	private boolean [] bColumnAscending = null;
    }
    /********************************************
     * From table... dropdown
     */
    private Vector <Integer> getPairIDforShowTable() {
		Vector <Integer> pairids = new Vector <Integer> ();
		int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
		for (int row=0; row<theTableData.getNumRows(); row++) {
			int pairid = ((Integer)theTableData.getValueAt(row, IDidx));	
			pairids.add(pairid);
		}
		return pairids;
    }
    /***********************************************************************
     * Sequences for selected pairs 
     * where unitrans.UTid in (seqid1,...)
     ***********************************************************/
    private void viewSeqs() {
    	try {						
    		int [] sels = theTable.getSelectedRows();
    		
    		String where="", tab="", summary="";
    		
    		int pairId = theTableData.getColumnHeaderIndex(PAIRID);
        	int id1Idx = theTableData.getColumnHeaderIndex(ID1_SQL);
    		int id2Idx = theTableData.getColumnHeaderIndex(ID2_SQL);
    		
    		if (sels.length==1) {
    			int pairid = ((Integer)theTableData.getValueAt(sels[0], pairId));
				int id1 = (Integer)theTableData.getValueAt(sels[0], id1Idx);
				int id2 = (Integer)theTableData.getValueAt(sels[0], id2Idx);
        		
        		where = " (" + SEQ_ID + " = " + id1 + " or " + SEQ_ID + " = " + id2 + ")"; 
        		summary = getSumLine(sels[0], pairid+"");
        		tab = Globals.tagSEQ  + "Pair #" + pairid;
    		}
    		else {
    			HashSet <Integer> added = new HashSet <Integer> ();
    	    	where =  " " + SEQ_ID + " in (";
    	    	for (int i=0; i<sels.length; i++) {
    	    		int pairid = ((Integer)theTableData.getValueAt(sels[i], pairId));
    				int id1 = (Integer)theTableData.getValueAt(sels[i], id1Idx);
    				int id2 = (Integer)theTableData.getValueAt(sels[i], id2Idx);
    				
    				if (i==0) {
    					where += id1 + "," + id2;
    					summary = "Pairs #" + pairid;
    					added.add(id1);
    					added.add(id2);
    				}
    				else {
    					if (i<11) summary += ", #" + pairid;
    					if (!added.contains(id1)) {
    						where += "," + id1;	
    						added.add(id1);
    					}
    					if (!added.contains(id2)) {
    						where += "," + id2;	
    						added.add(id2);
    					}
    				}
    	    	}
    	    	where += ")";
    	    	if (sels.length>11) summary += "...(" + sels.length + " total)";
        		tab = Globals.tagSEQ  + "Pairs " + sels.length; // tab on left
    		}
			int row = (sels.length == 1) ? row = theTable.getSelectedRow() : -1;
			SeqsTablePanel newPanel = new SeqsTablePanel(theViewerFrame, getInstance(), 
					where, tab, summary, row);
			theViewerFrame.addResultPanel(getInstance(), newPanel, newPanel.getName(), tab);
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "View Pair Clusters");}
    }
    
    // Seq Table for next pair row
    public String [] getNextPairRowForSeq(int nextRow) {
    	int row = getTranslatedRow(nextRow);
    	int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
		int IDidx1 = theTableData.getColumnHeaderIndex(ID1_SQL); // index
		int IDidx2 = theTableData.getColumnHeaderIndex(ID2_SQL);
		
		int id1 =     (Integer)theTableData.getValueAt(row, IDidx1); 
		int id2 =     (Integer)theTableData.getValueAt(row, IDidx2);
		int pairid =  (Integer)theTableData.getValueAt(row, IDidx);			
		
		String [] retVal = new String[4];
		retVal[0] = " (" + SEQ_ID + " = " + id1 + " or " + SEQ_ID + " = " + id2 + ")"; // Where
		retVal[1] = Globals.tagSEQ + "Pair #" + pairid; 	        // tab on left
		retVal[2] = getSumLine(row, pairid+"");						// summary on table
		retVal[3] = row+"";
		return retVal;
    }
    /******************************************************
     * CAS310 Clusters for selected pairs -- need to query database for GrpIDs
     * where pog_groups.PGid = grpID
     * DB call for grpIDs from grpStr in pairwise
     */
    private void viewGrps() {
    	try {
    		int [] sels = theTable.getSelectedRows();
        	if (sels==null || sels.length==0) return;
    			
    	// grpId for where (build Where string in GrpTablePanel)
			Vector <Integer> grpIDs = loadGrpsForWhere(sels); // empty table if none
			String sql = GrpTablePanel.makeSQLfromGRPid(grpIDs.toArray(new Integer[0]));
			
		// summary and tab
			int num = sels.length;
	    	int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
	    	int row = -1;
	    	
	    	String summary;
	    	String tab;
	    	String firstPair = ""+ (Integer)theTableData.getValueAt(sels[0], IDidx);
	    	
	    	if (num==1) {
	    		row = theTable.getSelectedRow();
	    		tab = Globals.tagGRP + "Pair " + firstPair;
	    		summary = getSumLine(row, firstPair);
	    	}
	    	else {
	    		tab = Globals.tagGRP + "Pairs " + num;
	    		summary = Globals.tagPAIRs + "#" + firstPair;
	    		
	    		for (int i=1; i<num && i<11; i++) {
		    		summary += ", #" + ((Integer)theTableData.getValueAt(sels[i], IDidx));
		    	}
		    	if (num>11) summary += "... (" + sels.length + " total)";
	    	}
	  
			GrpTablePanel grpPanel = new GrpTablePanel(theViewerFrame, getInstance(), sql, tab, summary, row); 
			
			theViewerFrame.addResultPanel(getInstance(), grpPanel, grpPanel.getName(), grpPanel.getSummary());
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "View Pair Clusters");}
    }
    // Cluster Table needs next row
    public String [] getNextPairRowForGrp(int nextRow) {
    	int row = getTranslatedRow(nextRow);
    	int [] sels = new int [1];
    	sels[0]=row;
    	Vector <Integer> grpIDs = loadGrpsForWhere(sels); 
	
    	int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
    	String pairNum  = ""+((Integer)theTableData.getValueAt(sels[0], IDidx));
    	
    	String [] strVals = new String [4];
    	
    	strVals[0] = GrpTablePanel.makeSQLfromGRPid(grpIDs.toArray(new Integer[0])); // where
    	strVals[1] = Globals.tagGRP + "Pair #" + pairNum;			// tab
    	strVals[2] = getSumLine(row, pairNum);			// summary
    	strVals[3] = row+"";
    	
    	return strVals;
    }
    private Vector <Integer> loadGrpsForWhere(int [] sels) {
		try{	
			// For each selected line, get the column methods
			int methodStart = FieldData.getPairMethodStart();
			
			Vector <String> grpNames = new Vector <String> ();
			for (int row : sels) {
				for (int col=0; col<methods.length; col++) {
					String name = (String)theTableData.getValueAt(row, col+methodStart);
					if (name!=null && !grpNames.contains(name)) {
						grpNames.add(name);
					}
				}
			}
			DBConn mDB = theViewerFrame.getDBConnection();
			Vector<Integer> ids = new Vector<Integer> ();
			ResultSet rset = null;
			for (String name : grpNames) {
				rset = mDB.executeQuery("SELECT PGid FROM pog_groups WHERE PGstr='" + name + "'");
				if(rset.next()) {
					int grpid = rset.getInt(1);
					if (!ids.contains(grpid)) ids.add(grpid);
				}
			}
			if (rset!=null) rset.close(); 
			mDB.close();
	    	
			return ids;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Loading Cluster IDs"); return null;}
    }
  
    /*******************************************
     * for AlignButtons pairwise; 
     */
    public String [] getAlignSeqIDs(int row) { // next/prev
    	int [] sels = {row};
    	return getAlignSeqIDs(sels);
    }
    public String [] getAlignSeqIDs() {
    	int [] sels = theTable.getSelectedRows();
    	return getAlignSeqIDs(sels);
    }
    public String [] getAlignSeqIDs(int [] sels) { // SeqIDs (names)
		int seqID1 = theTableData.getColumnHeaderIndex(SEQ_ID1); // index
		int seqID2 = theTableData.getColumnHeaderIndex(SEQ_ID2);
		
		TreeSet <String> added = new TreeSet <String> ();
    	
    	for (int i=0; i<sels.length; i++) {
			String id1 = (String)theTableData.getValueAt(sels[i], seqID1);
			String id2 = (String)theTableData.getValueAt(sels[i], seqID2);
			if (!added.contains(id1)) added.add(id1);
			if (!added.contains(id2)) added.add(id2);
    	}
    	String [] retVal = new String [added.size()];
    	int i=0;
    	for (String id : added) retVal[i++] = id;
		return retVal;
    }
    public String [] getTabSum() {
    	int [] sels = theTable.getSelectedRows();
		int row1 = sels[0], numSel=sels.length;
		
    	int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
    	String pairNum = ((Integer)theTableData.getValueAt(row1, IDidx))+"";
    		
    	String [] retVal = new String[3];
    	if (numSel==1) {
    		retVal[0] = "Pair #" + pairNum;
    		retVal[1] = getSumLine(row1, pairNum);
    		retVal[2] = row1+"";
    	}
    	else {
    		retVal[0] = "Pair #" + pairNum;
    		retVal[1] = "Pair #" + pairNum;
    		
    		for (int i=1; i<numSel && i<11; i++) {
    			retVal[1] += ", #" + ((Integer)theTableData.getValueAt(sels[i], IDidx));
	    	}
	    	if (numSel>11) retVal[1] += "... (" + sels.length + " total)";
	    	
    		retVal[2] = "-1";
    	}
    	return retVal;
    }
    public int getSelectedCount() {
    	return theTable.getSelectedRowCount();
    }
    public String getHitStr() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
		int IDidx = theTableData.getColumnHeaderIndex(HITID);
    	return (String) theTableData.getValueAt(row, IDidx);
    }  
    public String [] getNextPairForPW(int nextRow) {
    	int row = getTranslatedRow(nextRow);
    	
    	String pairNum = ((Integer)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(PAIRID)))+"";
    	String hitID =   ((String) theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(HITID)));
    	
		String [] retVal = new String[4];
		retVal[0] = Globals.tagPW + "Pair #" + pairNum;		// tab
		retVal[1] = getSumLine(row, pairNum);				// summary
		retVal[2] = row +"";						// nParentRow
		retVal[3] = hitID;
		
		return retVal;
    }
    /************** next/prev **************************/
    // Pair table Next/Prev
    private void getNextRow(int nextRow) {
    	String [] strVals = null;
    	if (theGrpTable!=null) {
    		strVals = theGrpTable.getNextGrpRowForPair(nextRow);
    		grpWhere = 			strVals[0];	//HTd1 like 'HTd1_000002   
    	}
    	else if (theSeqTable!=null) {
    		strVals = theSeqTable.getNextSeqRowForPair(nextRow);
    		seqList = 			strVals[0];	//(3)		seqID list
    	}
    	else Out.die("TCW error on next for pairs");
    	
    	tabName = 		strVals[1];
		strSummary = 	Globals.trimSum(strVals[2]);
		nParentRow = 	Integer.parseInt(strVals[3]);
		
		buildQueryThread();
    }
    private int getTranslatedRow(int row) {
		if (theTable==null) return 0;

    	if (row >= theTable.getRowCount()) 	return 0; // CAS340 add '>'
    	else if (row < 0) 					return theTable.getRowCount() - 1;
    	else 								return row;
    }
    private String getSumLine(int row, String name) {
		String id1 = (String)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(SEQ_ID1));
		String id2 = (String)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(SEQ_ID2));	
		String desc = (String)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(HITDESC));
    	if (desc==null) {
    		desc = (String)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(HITID));
    		desc +=  " (no hit sequence)";
    	}
    	
    	String sum =  "Row " + (row+1) + "/" + theTable.getRowCount();
    	sum += "   " + Globals.tagPAIR +  " #" +   name + "  " + id1 + " " + id2;
    	sum += "   " + Globals.tagHIT +  desc;	
   
    	return sum;
    }
   
    /**************************************************************/
    private SortTable theTable = null;
    private TableData theTableData = null;
    private JScrollPane sPane = null;
    
	private JPanel tableButtonPanel = null, tableStatusPanel = null, fieldSelectPanel = null, tableSummaryPanel = null;
	
    private JTextField tableHeader = null, tableType = null, loadStatus = null, txtStatus = null;
    private JLabel lblSummary = null;
   
    //Function buttons
    private JButton btnCopy = null, btnTable = null, btnHelp = null;
    private JButton btnNextRow = null, btnPrevRow = null;
    private JButton btnTableSeqs = null, btnTableGrps = null, btnPairwise = null;
    private JButton showColumnSelect = null, clearColumn = null;
	
    private ActionListener dblClick = null, sngClick = null, colSelectChange = null;
    private ListSelectionListener selListener = null;
    
    private JCheckBox [] chkFields = null;
   
    private Thread buildThread = null; 
	
	private MTCWFrame theViewerFrame = null;
	private GrpTablePanel theGrpTable = null;
	private SeqsTablePanel theSeqTable = null;
	private ViewerSettings vSettings = null;

	private String strSubQuery = "", strSummary = null, tabName = "";
	private String grpWhere = null, seqList=null;
	private String [] methods;
	private boolean isList=false, hasAAdb=false, hasGOs=false;
	private int nParentRow = -1, totalPairs=0;
	
}
