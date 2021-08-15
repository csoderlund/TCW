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

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.seq.SeqsTopRowPanel;
import cmp.viewer.table.*;

public class PairTablePanel extends JPanel {
	private static final long serialVersionUID = -3827610586702639105L;
	
	private static final String TABLE = FieldData.PAIR_TABLE;
	private static final String PAIRID = FieldData.PAIRID;
	private static final String SEQ_ID = FieldData.SEQ_SQLID; // CAS310 FieldData.SEQ_TABLE + "." + FieldData.SEQ_SQLID;
	private static final String SEQ_ID1 = FieldData.SEQID1; 
	private static final String SEQ_ID2 = FieldData.SEQID2; 
	private static final String ID1_SQL = FieldData.ID1_SQLID; 
	private static final String ID2_SQL = FieldData.ID2_SQLID; 	
	private static final String HITID = FieldData.HITID;
	private static final String HITDESC = FieldData.HITDESC;
	private static final String helpHTML = Globals.helpDir + "PairTable.html";
	
	// List pairs or Filter pairs
	public PairTablePanel(MTCWFrame parentFrame, String tab) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		
		initData(parentFrame, tab);	
		
		PairQueryPanel theQueryPanel = theViewerFrame.getPairQueryPanel();
		if (theQueryPanel!=null) {
			strSubQuery =     theQueryPanel.getSubQuery();
			strQuerySummary = theQueryPanel.getQuerySummary();
		}
		else buildShortList();
		
		buildQueryThread(); 
	}
	
	// from seq table
	public PairTablePanel(MTCWFrame parentFrame, SeqsTopRowPanel parentList, String tab,  String sum, String list, int row) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		
		theSeqTable = parentList;
		nParentRow = row;
		seqList = list;
		strQuerySummary = sum;
		
		initData(parentFrame, tab);	
		
		buildQueryThread(); 
	}
	
	// From groups table
	public PairTablePanel(MTCWFrame parentFrame, GrpTablePanel parentList, String where, String tab,  String sum,  int row) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		
		theGrpTable = parentList;
		nParentRow = row;
		grpWhere = where;
		strQuerySummary = sum;
		
		initData(parentFrame, tab);	
		
		buildQueryThread(); 
	}
	
	/** XXX Buttons for Pair table */
    private JPanel createTableButton() {
    	JPanel buttonPanel = Static.createPagePanel();
	    	
    	JPanel topRow = Static.createRowPanel();
	    topRow.add(new JLabel("Selected:"));
    	   	
         btnTableSeqs = Static.createButton("Sequences", false, Globals.FUNCTIONCOLOR);
         btnTableSeqs.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent arg0) {
 				viewSequences();
 			}
 		});
        topRow.add(btnTableSeqs);
        topRow.add(Box.createHorizontalStrut(3));  
        
        // CAS310 add
        btnTableGrps = Static.createButton("Clusters", false, Globals.FUNCTIONCOLOR);
        btnTableGrps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewClusters();
			}
		});
       topRow.add(btnTableGrps);
       topRow.add(Box.createHorizontalStrut(3)); 
         	
        createBtnCopy();
 		topRow.add(btnCopy);
        topRow.add(Box.createHorizontalStrut(50));  
		
        createBtnTable();
        topRow.add(btnTable);
        topRow.add(Box.createHorizontalGlue());
        
        btnHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
        btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Pair table",  helpHTML);
			}
		});
        topRow.add(btnHelp);

        if(nParentRow >= 0) { // if -1, then showing members from multiple clusters, and no Next/Prev
 	 	   JPanel rowChangePanel = Static.createRowPanel();
 	 	   rowChangePanel.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
 	 	   
 	 	   btnPrevRow = Static.createButton("<< Prev", true);
 	 	   btnPrevRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   if (seqList!=null) getNextSeqRow(-1);
 	 			   else getNextGrpRow(nParentRow - 1);
 	 		   }
 	 	   });
 	 	  
 	 	   btnNextRow = Static.createButton("Next >>", true);
 	 	   btnNextRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   if (seqList!=null) getNextSeqRow(1);
	 			   else  getNextGrpRow(nParentRow + 1);
 	 		   }
 	 	   });
 	 	   
 	 	   rowChangePanel.add(btnPrevRow);
 	 	   rowChangePanel.add(Box.createHorizontalStrut(1));
 	 	   rowChangePanel.add(btnNextRow);
 	 	   
 	 	   topRow.add(rowChangePanel);
 	    }
        topRow.setAlignmentX(LEFT_ALIGNMENT);

        buttonPanel.add(topRow);
        buttonPanel.add(Box.createVerticalStrut(5));
        
        return buttonPanel;
    }
    
    
    private void createBtnCopy() {
    	btnCopy = Static.createButton("Copy...", false);
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
					new TableUtil().statsPopUp("Pairs: " + strQuerySummary, theTable);
					
	 		   }
	 	}));
		if (!hasAAdb) {
	 	    popup.add(new JMenuItem(new AbstractAction("Show Table Stats (Slow)") {
				private static final long serialVersionUID = 1L;
					public void actionPerformed(ActionEvent e) {
	 	    			try{			
	 					Vector <Integer> ids = getSeqQueryAll();
	 					DBConn mDB = theViewerFrame.getDBConnection();
	 					String summary = "Pairs: " + strQuerySummary;
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
				} catch (Exception ex) {ErrorReport.reportError(ex, "Error generating list");}
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
	 				new TableUtil(theViewerFrame).exportPairGO(btnTable,theTableData, strQuerySummary);
	 			}
	 		}));
 		}
 	
 		btnTable = Static.createButton("Table...", true);
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
				} catch (Exception e) {ErrorReport.reportError(e, "Error generating list");
				} catch (Error e) {ErrorReport.reportFatalError(e, "Fatal error generating lisr", theViewerFrame);}
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
	
		showColumnSelect = Static.createButton("Select Columns", true, Globals.MENUCOLOR);
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
		clearColumn = Static.createButton("Clear Columns", true, Globals.MENUCOLOR);
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
        theTableData.setColumnHeaders(theFields.getDisplayFields(), theFields.getDisplayTypes());
        theTableData.addRowsWithProgress(rs, theFields, loadStatus);
        theTableData.showTable();

        int nRow = theTableData.getNumRows();
        String status =  nRow + " of " + totalPairs + " " + Static.perText(nRow, totalPairs);
        tableHeader.setText(status);
        
        if(!isList) {
           theViewerFrame.changePanelName(this, tabName + ": " + nRow, strQuerySummary);
        }
    }
	private void setTopEnabled() {
		int selCount = theTable.getSelectedRowCount();
		int rowCount = theTable.getRowCount();
		boolean b = (selCount == 1 || rowCount==1) ? true : false;
		
		btnTableSeqs.setEnabled(selCount>0); 
		btnTableGrps.setEnabled(selCount>0); 
		btnCopy.setEnabled(b);
	}
    private JPanel createFieldSelectPanel() {
    	JPanel page = Static.createPagePanel();
    	
    	String [] sections = FieldData.getPairColumnSections();
    	int [] secIdx = FieldData.getPairColumnSectionIdx(methods.length);
    	int [] secBreak = FieldData.getPairColumnSectionBreak();
    	String [] columns = FieldData.getPairColumns(methods);
    	String [] descriptions = FieldData.getPairDescript(methods);
    	boolean [] defaults = FieldData.getPairSelections(methods.length);
	    boolean [] selections = getColumnSelections(columns, defaults, 
    					theViewerFrame.getSettings().getPairSettings().getSelectedColumns());    
	    	
	    chkFields = new JCheckBox[columns.length];
	
		int maxWidth=0;	
    	for(int x=0; x<columns.length; x++) {
    		final String desc = descriptions[x];
    		
        	chkFields[x] = new JCheckBox(columns[x]);
        	chkFields[x].setBackground(Globals.BGCOLOR);
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
    
    	JLabel header = Static.createLabel(MTCWFrame.FILTER, true);
    	thePanel.add(header);
    	
    	lblSummary = Static.createLabel(strQuerySummary, true);
    	lblSummary.setFont(getFont());
    	thePanel.add(lblSummary);
    	
    	return thePanel;
	}
	
	public String getSummary() { return strQuerySummary; }
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
	private void buildShortList() {
		try {
			DBConn mDB = theViewerFrame.getDBConnection(); // CAS310
			DBinfo info = theViewerFrame.getInfo();
			String x = info.getSamplePair(mDB);
			mDB.close();
			
			if (x==null || x=="") return;
			String [] y = x.split(":");
			strSubQuery = y[1];
			strQuerySummary = y[0];
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
    
 // selection of multiple pairs
    private Vector <Integer> getSeqQueryAll() {
		Vector <Integer> pairids = new Vector <Integer> ();
		int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
		for (int row=0; row<theTableData.getNumRows(); row++) {
			int pairid = ((Integer)theTableData.getValueAt(row, IDidx));	
			pairids.add(pairid);
		}
		return pairids;
    }
    /***********************************************************************
     * Sequences for selected pairs -- build list of seqIdx from table
     ***********************************************************/
    private void viewSequences() {
    	try {						
			String [] strVals; // [0] tab, [1] summary, [2] where
			int [] sels = theTable.getSelectedRows();
			
			if (sels==null || sels.length==0) strVals = getPairQueryForSeq(0);
			else if (sels.length==1)          strVals = getPairQueryForSeq(sels[0]);
			else                              strVals = getPairQueryForSeq(sels);
			
			int row = (sels.length == 1) ? row = theTable.getSelectedRow() : -1;
			SeqsTopRowPanel newPanel = new SeqsTopRowPanel(theViewerFrame, getInstance(), 
					strVals[0], strVals[1], strVals[2], row);
			theViewerFrame.addResultPanel(getInstance(), newPanel, newPanel.getName(), strVals[1]);
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "View Pair Clusters");}
    }
  
    // This is used for Next/Prev from Pair and Cluster sequence tables; and used for viewSequences
    public String [] getPairQueryForSeq(int row) {
		int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
		int IDidx1 = theTableData.getColumnHeaderIndex(ID1_SQL); // index
		int IDidx2 = theTableData.getColumnHeaderIndex(ID2_SQL);
		int id1 = (Integer)theTableData.getValueAt(row, IDidx1); // CAS328 got a ArrayIndexOutOfBoundsException: 12 - why?
		int id2 = (Integer)theTableData.getValueAt(row, IDidx2);
		int pairid = ((Integer)theTableData.getValueAt(row, IDidx));			
		
		String [] retVal = new String[3];
		retVal[0] = MTCWFrame.SEQ_PREFIX + ": pair #" + pairid; 	                   // tab on left
		retVal[1] = "Pair #" + pairid;							       					// summary on table
		retVal[2] = " (" + SEQ_ID + " = " + id1 + " or " + SEQ_ID + " = " + id2 + ")"; // Where

		return retVal;
    }
    private String [] getPairQueryForSeq(int [] row) {
    	String [] retVal = new String[3];
    		
    	retVal[0]= MTCWFrame.SEQ_PREFIX  + ": pairs " + row.length; // tab on left
    	retVal[1]= "Pairs ";										// summary
    	retVal[2]= " UTid in (";								     // where
    	HashSet <Integer> added = new HashSet <Integer> ();
    		
    	for (int i=0; i<row.length; i++) {
    		int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
    		int pairid = ((Integer)theTableData.getValueAt(row[i], IDidx));
			int IDidx1 = theTableData.getColumnHeaderIndex(ID1_SQL);
			int IDidx2 = theTableData.getColumnHeaderIndex(ID2_SQL);
			int id1 = (Integer)theTableData.getValueAt(row[i], IDidx1);
			int id2 = (Integer)theTableData.getValueAt(row[i], IDidx2);
			
			if (i==0) {
				retVal[1] += "#" + pairid;
				retVal[2] += id1 + "," + id2;
				added.add(id1);
				added.add(id2);
			}
			else {
				if (i<11) retVal[1] += ", #" + pairid;
				if (!added.contains(id1)) {
					retVal[2] += "," + id1;	
					added.add(id1);
				}
				if (!added.contains(id2)) {
					retVal[2] += "," + id2;	
					added.add(id2);
				}
			}
    	}
    	if (row.length>11) retVal[1] += "...(" + row.length + " total)";
    	retVal[2] += ")";
		return retVal;
    }
    // Called from Seq Table for next pair row
    public String [] getNextPairRowForSeq(int nextRow) {
    	int row = getTranslatedRow(nextRow);
    	return getPairQueryForSeq(row);
    }
    // Pair Table Next/Prev  through Seq table
    private void getNextSeqRow(int inc) {
		int nextRow = (inc<0) ? nParentRow - 1 : nParentRow + 1;
		
		String [] strVals = theSeqTable.getNextSeqRowForPair(nextRow);
		seqList = strVals[0];
		tabName = strVals[1];
		strQuerySummary = strVals[2];
		nParentRow = Integer.parseInt(strVals[3]);
		
		buildQueryThread();
	}
    /******************************************************
     * CAS310 Clusters for selected pairs -- need to query database for GrpIDs
     */
    private void viewClusters() {
    	try {
    		int [] sels = theTable.getSelectedRows();
        	if (sels==null || sels.length==0) return;
    			
    	// grpId for where (build Where string in GrpTablePanel)
			Vector <Integer> grpIDs = getPairQueryForGrp(sels, true);
			if (grpIDs==null) return;
			String sql = GrpTablePanel.makeSQLfromGRPid(grpIDs.toArray(new Integer[0]));
			
		// summary
			int num = sels.length;
	    	int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
	    	String sum = "#" + ((Integer)theTableData.getValueAt(sels[0], IDidx));
	    	
	    	for (int i=1; i<num && i<11; i++) {
	    		sum += ", #" + ((Integer)theTableData.getValueAt(sels[i], IDidx));
	    	}
	    	if (num>11) sum += "... (" + sels.length + " total)";
	    	String summary = (num==1) ? "Pair " : "Pairs ";
	    	summary += sum;
	    	
	    // tab
			String tab = MTCWFrame.GRP_PREFIX +  ": ";
			if (num==1) tab += "pair " + sum;
			else        tab += "pairs " + num;
			
			int row = (sels.length == 1) ? row = theTable.getSelectedRow() : -1;
			
			GrpTablePanel grpPanel = new GrpTablePanel(theViewerFrame, getInstance(), tab, summary, sql, row); 
			theViewerFrame.addResultPanel(getInstance(), grpPanel, grpPanel.getName(), grpPanel.getSummary());
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "View Pair Clusters");}
    }
    private Vector <Integer> getPairQueryForGrp(int [] sels, boolean pMsg) {
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
			
			if (ids.size()==0 && pMsg) {
				JOptionPane.showMessageDialog(null, 
						"No Clusters for selected pairs ", "Warning", JOptionPane.PLAIN_MESSAGE); 
				return null;
			}
	    	
			return ids;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Loading Cluster IDs"); return null;}
    }
    // Pairs table Next/Prev through Cluster table
    private void getNextGrpRow(int nextRow) {
		String [] strVals = theGrpTable.getNextGrpRowForPair(nextRow);
		grpWhere = strVals[0];
		tabName = strVals[1];
		strQuerySummary = strVals[2];
		nParentRow = Integer.parseInt(strVals[3]);
		
		buildQueryThread();
	}
    // Cluster Table needs next row
    public String [] getNextPairRowForGrp(int nextRow) {
    	int row = getTranslatedRow(nextRow);
    	int [] sels = new int [1];
    	sels[0]=row;
    	Vector <Integer> grpIDs = getPairQueryForGrp(sels, false);
	
    	String [] strVals = new String [4];
    	strVals[0] = GrpTablePanel.makeSQLfromGRPid(grpIDs.toArray(new Integer[0]));
    	
    	int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
    	String pairNum = "Pair #" + ((Integer)theTableData.getValueAt(sels[0], IDidx));
    	
    	strVals[1] = pairNum;
    	
    	strVals[2] =  MTCWFrame.GRP_PREFIX +  ": " +  pairNum;
    	
    	strVals[3] = row+"";
    	
    	return strVals;
    }
    /**********************************************************************************/
  //When the view table gets sorted, sort the master table to match (Called by TableData)
    public void sortMasterColumn(String columnName, boolean ascending) {
	    	int index = theTableData.getColumnHeaderIndex(columnName);
	    	theTableData.sortByColumn(index, ascending);
    }
    
    private void showProgress() {
	    	removeAll();
	    	repaint();
	    	setBackground(Globals.BGCOLOR);
	    	loadStatus = new JTextField(100);
	    	loadStatus.setBackground(Globals.BGCOLOR);
	    	loadStatus.setMaximumSize(loadStatus.getPreferredSize());
	    	loadStatus.setEditable(false);
	    	loadStatus.setBorder(BorderFactory.createEmptyBorder());
	    	JButton btnStop = new JButton("Stop");
	    	btnStop.setBackground(Globals.BGCOLOR);
	    	btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//This not only informs the user that it is canceled, 
				//but this is also the signal for the thread to stop. 
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
	    	theTable.getTableHeader().setBackground(Globals.BGCOLOR);
	    	sPane.setColumnHeaderView(theTable.getTableHeader());
	    	sPane.setAlignmentX(Component.LEFT_ALIGNMENT);
	    	
	    	sPane.getViewport().setBackground(Globals.BGCOLOR);
	    	sPane.getHorizontalScrollBar().setBackground(Globals.BGCOLOR);
	    	sPane.getVerticalScrollBar().setBackground(Globals.BGCOLOR);
	    	sPane.getHorizontalScrollBar().setForeground(Globals.BGCOLOR);
	    	sPane.getVerticalScrollBar().setForeground(Globals.BGCOLOR);
	    	
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
	    	 	if (grpWhere!=null) tableType.setText("Pair View for Cluster");
	    	 	else if (seqList!=null) tableType.setText("Pair View for Sequence");
	    	 	else tableType.setText("Pair View");
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
    private void validateTable() {
    		validate();
    }
   
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
    
    private int getTranslatedRow(int row) {
		if (theTable==null) return 0;

    		if (row == theTable.getRowCount()) return 0;
    		if (row < 0) return theTable.getRowCount() - 1;
    		return row;
    }
    private SortTable theTable = null;
    private TableData theTableData = null;
    private JScrollPane sPane = null;
    
    private JTextField tableHeader = null;
    private JTextField tableType = null;
    private JTextField loadStatus = null;
    private JTextField txtStatus = null;
    private JLabel lblSummary = null;
   
    //Function buttons
    private JButton btnCopy = null, btnTable = null, btnHelp = null;
    private JButton btnNextRow = null, btnPrevRow = null;
    private int nParentRow = -1;
    private JButton btnTableSeqs = null, btnTableGrps = null;
    
    private ActionListener dblClick = null;
    private ActionListener sngClick = null;
    private ListSelectionListener selListener = null;
    private String strSubQuery = "";
    private String strQuerySummary = null;
	private String tabName = "";
    private Thread buildThread = null;
    
    private JCheckBox [] chkFields = null;
    private ActionListener colSelectChange = null;
    
	private JPanel tableButtonPanel = null;
	private JPanel tableStatusPanel = null;
	private JPanel fieldSelectPanel = null;
	private JPanel tableSummaryPanel = null;
	
	private JButton showColumnSelect = null;
	private JButton clearColumn = null;
	
	private MTCWFrame theViewerFrame = null;
	
	private GrpTablePanel theGrpTable = null;
	private String grpWhere = null;
	
	private SeqsTopRowPanel theSeqTable = null;
	private String seqList=null;
	
	private ViewerSettings vSettings = null;
	private int totalPairs=0;
	private String [] methods;
	private boolean isList=false, hasAAdb=false, hasGOs=false;
	
}
