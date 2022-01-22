package cmp.viewer.seq;
/*****************************************************
 * Sequence table: called from SeqsTopRowPanel to display the table.
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
import util.methods.Static;
import util.methods.Out;
import util.ui.UserPrompt;
import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.database.Load;
import cmp.viewer.MTCWFrame;
import cmp.viewer.ViewerSettings;
import cmp.viewer.align.AlignButtons;
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.hits.HitTablePanel;
import cmp.viewer.pairs.PairTablePanel;
import cmp.viewer.seqDetail.SeqTopRowPanel;
import cmp.viewer.table.FieldData;
import cmp.viewer.table.SortTable;
import cmp.viewer.table.TableData;
import cmp.viewer.table.TableUtil;

public class SeqsTablePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private static final String helpHTML = Globals.helpDir + "SeqTable.html";
	private static final int MAX_SELECT=20;
	private static final int COLUMN_SELECT_WIDTH = 75, COLUMN_PANEL_WIDTH = 900;

	private static final String TABLE = FieldData.SEQ_TABLE;
	private static final String SEQID = FieldData.SEQID;
	private static final String SEQ_SQLID = FieldData.SEQ_SQLID;
	private static final String HITID = FieldData.HITID;
	private static final String HITDESC = FieldData.HITDESC;
	private static final String seqTAG = "Seqs ";
	
	// Filter or >Sequences
	public SeqsTablePanel(MTCWFrame parentFrame, String tab) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true; // >Sequences

		initData(parentFrame, tab, -1);
		
		SeqsQueryPanel theQueryPanel = parentFrame.getSeqsQueryPanel();
		if (theQueryPanel!=null) {
			strSubQuery =     theQueryPanel.getSubQuery();
			strSummary = Globals.trimSum(theQueryPanel.getQuerySummary());
		}
		else loadShortList();
		
		strSummary = Globals.FILTER + strSummary;
		
		buildQueryThread(); 
	}
	
	// from Cluster table 
	public SeqsTablePanel(MTCWFrame parentFrame, GrpTablePanel tablePanel, 
			String subQuery, String tab, String summary, int row) {
		
		theGrpTable = tablePanel;
		
		initData(parentFrame, tab, row);	
		
		strSubQuery = subQuery;
		strSummary = Globals.trimSum(summary);
		
		buildQueryThread(); 
	}
	
	// from Pair table
	public SeqsTablePanel(MTCWFrame parentFrame, PairTablePanel tablePanel, 
			String subQuery, String tab, String summary,  int row) {
		thePairTable = tablePanel;
	
		strSubQuery = subQuery;
		strSummary = Globals.trimSum(summary);
		initData(parentFrame, tab, row);	
		
		buildQueryThread(); 
	}
	
	// from Hit table
	public SeqsTablePanel(MTCWFrame parentFrame, HitTablePanel tablePanel, 
			String subQuery, String tab, String summary,  int row) {
		theHitTable = tablePanel;
		
		strSubQuery = subQuery;
		strSummary = Globals.trimSum(summary);
		initData(parentFrame, tab, row);	
		
		buildQueryThread(); 
	}
	private void initData(MTCWFrame parentFrame, String tab, int row) {
		theViewerFrame = parentFrame;
		vSettings = parentFrame.getSettings();
		totalSeq = theViewerFrame.getInfo().getCntSeq();

		tabName = tab;
		nParentRow = row;
		
		colSelectChange = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateTable(false); 
				displayTable();
				vSettings.getSeqSettings().setSelectedColumns(getSelectedColumns());
			}
		};
	}
	/** XXX Buttons for Seq table */
    private JPanel createTableButton() {
    	JPanel buttonPanel = Static.createPagePanel();
	    	
    	JPanel topRow = Static.createRowPanel();
	    topRow.add(Static.createLabel(Globals.select));	topRow.add(Box.createHorizontalStrut(1)); 
    	   	 
	    btnViewDetails = Static.createButtonTab(Globals.SEQ_DETAIL, false);
        btnViewDetails.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewDetails();
			}
		});
       topRow.add(btnViewDetails);				topRow.add(Box.createHorizontalStrut(1)); 
      
        btnViewGroups = Static.createButtonTab(Globals.GRP_TABLE, false);
        btnViewGroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewGrps();
			}
		});
       topRow.add(btnViewGroups);
       topRow.add(Box.createHorizontalStrut(2)); 
       
       btnViewPairs = Static.createButtonTab(Globals.PAIR_TABLE, false);
       btnViewPairs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewPairs();
			}
		});
       topRow.add(btnViewPairs);				topRow.add(Box.createHorizontalStrut(1)); 
       
       AlignButtons msaObj = new AlignButtons(theViewerFrame, getInstance()); // CAS341 new
       btnMSA = msaObj.createBtnMultiAlign();
       topRow.add(btnMSA);						topRow.add(Box.createHorizontalStrut(1)); 
       
        createBtnCopy();
 		topRow.add(btnCopy);					topRow.add(Box.createHorizontalGlue());  
		
        createBtnTable();
        topRow.add(btnTable);					topRow.add(Box.createHorizontalStrut(1));
        
        btnHelp = Static.createButtonHelp("Help", true);
        btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Pair table",  helpHTML);
			}
		});
        topRow.add(btnHelp);					topRow.add(Box.createHorizontalStrut(1)); 

        if(nParentRow >= 0) { // if -1, then showing members from multiple clusters, and no Next/Prev
 	 	   JPanel rowChangePanel = Static.createRowPanel();
 	 	   rowChangePanel.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
 	 	   
 	 	   btnPrevRow = Static.createButton(Globals.prev, true);
 	 	   btnPrevRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   getNextRow(nParentRow-1);
 	 		   }
 	 	   }); 
 	 	   btnNextRow = Static.createButton(Globals.next, true);
 	 	   btnNextRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   getNextRow(nParentRow+1);
 	 		   }
 	 	   });
 	 	   rowChangePanel.add(btnPrevRow);
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
	    
 		copyPop.add(new JMenuItem(new AbstractAction(SEQID) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				String seqid = getSelectedColumn(SEQID);
 				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
 				cb.setContents(new StringSelection(seqid), null);
 			}
 		}));
		copyPop.add(new JMenuItem(new AbstractAction("AA  Sequence") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				int index = getSelectedSQLid();
				Load lObj = new Load(theViewerFrame);
				String id = lObj.loadSeq(index);
				if (id!=null) {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(">" + id + "\n" + lObj.getAAseq()), null);
				}
			}
		}));
		if (theViewerFrame.getInfo().nNTdb()>0) { // CAS310
			copyPop.add(new JMenuItem(new AbstractAction("CDS Sequence") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					int index = getSelectedSQLid();
					Load lObj = new Load(theViewerFrame);
					String id = lObj.loadSeq(index);
					if (id!=null) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						cb.setContents(new StringSelection(">" + id + "\n" + lObj.getCDSseq()), null);
					}
				}
			}));
			copyPop.add(new JMenuItem(new AbstractAction("NT  Sequence") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					int index = getSelectedSQLid();
					Load lObj = new Load(theViewerFrame);
					String id = lObj.loadSeq(index);
					if (id!=null) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						cb.setContents(new StringSelection(">" + id + "\n" + lObj.getNTseq()), null);
					}
				}
			}));
		}
		copyPop.addSeparator();  
		copyPop.add(new JMenuItem(new AbstractAction("Best Hit ID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String hitid = getSelectedColumn(HITID);
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(hitid), null);
			}
		}));
		copyPop.add(new JMenuItem(new AbstractAction("Best Hit Description") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String desc = getSelectedColumn(HITDESC);
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(desc), null);
			}
		}));
		copyPop.add(new JMenuItem(new AbstractAction("Best Hit Sequence") { // CAS310 add; move Hit to end
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String hitID = getSelectedColumn(HITID);
				Load lObj = new Load(theViewerFrame);
				String id = lObj.loadHit(hitID);
				if (id!=null) {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(">" + id + "\n" + lObj.getHitseq()), null);
				}
				
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
 				new TableUtil().statsPopUp("Sequence: " + strSummary, getTable());	
 			}
 		}));
 		popup.addSeparator();
 		popup.add(new JMenuItem(new AbstractAction("Copy Table") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				Thread copyThread = new Thread(new Runnable() {
					public void run() {
						try {											
							Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
							String table = new TableUtil(theViewerFrame).createTableString(getTable());
							cb.setContents(new StringSelection(table), null);
						} catch (Exception e) {ErrorReport.reportError(e, "Error copy table"); }
					}
				});
				copyThread.setPriority(Thread.MIN_PRIORITY);
				copyThread.start();
 			}
 		}));
 		popup.addSeparator();
 		popup.add(new JMenuItem(new AbstractAction("Export table (" + Globalx.CSV_SUFFIX + ")") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportTableTab(btnTable, getTable(), Globals.bSEQ);
 			}
 		}));
 		
 		popup.add(new JMenuItem(new AbstractAction("Export AA sequences (" + Globalx.FASTA_SUFFIX + ")") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportSeqFa(btnTable,getTableSQLid(), FieldData.AASEQ_SQL, false);
 			}
 		}));
 		if (hasNTdbOnly) {
	 		popup.add(new JMenuItem(new AbstractAction("Export CDS sequences (" + Globalx.FASTA_SUFFIX + ")") {
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportSeqFa(btnTable,getTableSQLid(), FieldData.NTSEQ_SQL, true);
	 			}
	 		}));
 		
	 		popup.add(new JMenuItem(new AbstractAction("Export NT sequences (" + Globalx.FASTA_SUFFIX + ")") {
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportSeqFa(btnTable,getTableSQLid(), FieldData.NTSEQ_SQL, false);
	 			}
	 		}));
 		}
 		// XXX
 		if (hasGOs) {
 			popup.addSeparator();
	 		popup.add(new JMenuItem(new AbstractAction("Export all GOs (" + Globalx.CSV_SUFFIX + ")...") {
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportSeqGO(btnTable,getTableData(), strSummary);
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
   
	/**************************************************************
	 * Perform query and build the panel with results
	 */
	public void buildQueryThread() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setAlignmentY(LEFT_ALIGNMENT);

        showProgress();
		buildThread = new Thread(new Runnable() {
			public void run() {
				try {
					DBConn mDB = theViewerFrame.getDBConnection();
					String query = buildQueryStr(mDB);
					ResultSet rset = MTCWFrame.executeQuery(mDB, query, loadStatus);
					if(rset != null) {
						buildTable(rset);
						MTCWFrame.closeResultSet(rset); //Thread safe way of closing the resultSet
						updateTable(true);
					}
					mDB.close();
					
					displayTable();
					
					if(isVisible()) {//Makes the table appear (A little hacky, but fast)
						setVisible(false);
						setVisible(true);
						setTopEnabled(0, theTable.getRowCount());
					}
				} catch (Exception e) {ErrorReport.reportError(e, "Error generating list");
				} catch (Error e) {ErrorReport.reportFatalError(e, "Fatal error generating lisr", theViewerFrame);}
			}
		});
		buildThread.setPriority(Thread.MIN_PRIORITY);
		buildThread.start();
	}
	/******************************************************************/
	private void buildTable(ResultSet rset) {
       
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
				int selCount = theTable.getSelectedRowCount();
				if(selCount > MAX_SELECT) {
					txtStatus.setText("Cannot pair align more than " + MAX_SELECT + "  rows at a time");
				}
				else {
					txtStatus.setText("");	
				}
				setTopEnabled(selCount, theTable.getRowCount());
			}
		};

        theTableData = new TableData(this);
        FieldData theFields = FieldData.getSeqFields(theViewerFrame.getSeqLibList(), 
        		theViewerFrame.getSeqDEList(), theViewerFrame.getMethodPrefixes());
        theTableData.setColumnHeaders(theFields.getDisplayNames(), theFields.getDisplayTypes(), theFields.getDisplaySymbols());
        
        theTableData.addRowsWithProgress(rset, theFields, loadStatus);
        theTableData.showTable();
       
        int nRow = theTableData.getNumRows(); 
        String status =  nRow + " of " + totalSeq + " " + Static.perText(nRow, totalSeq);
        tableHeader.setText(status);
        
        if(!isList) {
        	nRow = theTableData.getNumRows();
            theViewerFrame.changePanelName(this, tabName + ": " + nRow, strSummary);
        }
    }
	private void setTopEnabled(int selCount, int rowCnt) {
		boolean b = (selCount == 1) ? true : false;
		btnViewDetails.setEnabled(b);
		btnCopy.setEnabled(b);
		
		b = (selCount > 0) ? true : false;
		btnViewPairs.setEnabled(b);
		btnViewGroups.setEnabled(b);
		
		btnMSA.setEnabled(selCount>1);
	}
    private JPanel createFieldSelectPanel() {
    	JPanel retVal = Static.createPagePanel();
    	
		String [] lib = theViewerFrame.getSeqLibList();
		String [] de  = theViewerFrame.getSeqDEList();
		String [] met = theViewerFrame.getMethodPrefixes();
		
		String [] headers = 		FieldData.getSeqColumnSections();
		int [] headerIdx = 			FieldData.getSeqColumnSectionIdx(lib.length, de.length, met.length); 
		String [] columns = 		FieldData.getSeqColumns(lib, de, met);
		String []  descriptions = 	FieldData.getSeqDescrip(lib, de, met);
		boolean [] defaults = 		FieldData.getSeqSelections(lib.length, de.length, met.length);
		
	 	boolean [] selections = getColumnSelections(columns, defaults,
							theViewerFrame.getSettings().getSeqSettings().getSelectedColumns());   		
    	
    	chkFields = new JCheckBox[columns.length];

    	JPanel row = Static.createRowPanel();
    	int rowWidth = 0;
    	int hIdx = 0;
		
		retVal.add(new JLabel(headers[0]));
	
    	for(int x=0; x<columns.length; x++) {
    		int newWidth = 0;
    		int space = 0;
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
        	if(COLUMN_SELECT_WIDTH - chkFields[x].getPreferredSize().width > 0) {
        		newWidth = COLUMN_SELECT_WIDTH;
        		space = COLUMN_SELECT_WIDTH - chkFields[x].getPreferredSize().width;
        	}
        	else {
        		space = 0;
        		newWidth = chkFields[x].getPreferredSize().width;
        	}
        	
        	if(rowWidth + newWidth >= COLUMN_PANEL_WIDTH || 
        			(hIdx < headerIdx.length && headerIdx[hIdx] == x)) {
        		retVal.add(row);
        		if(hIdx < headerIdx.length && headerIdx[hIdx] == x) {
        			if(headers[hIdx+1].length() > 0) {
        				retVal.add(Box.createVerticalStrut(10));
        				retVal.add(new JLabel(headers[hIdx+1]));
        			}
        			hIdx++;
        		}
        		row = Static.createRowPanel();
            	rowWidth = 0;
        	}
    		row.add(chkFields[x]);
    		row.add(Box.createHorizontalStrut(space));
    		row.add(Box.createHorizontalStrut(10));	
    		rowWidth += newWidth + 10;
    	}
    	
    	if(row.getComponentCount() > 0) retVal.add(row);
    	retVal.setBorder(BorderFactory.createTitledBorder("Columns"));
    	retVal.setMaximumSize(retVal.getPreferredSize());

    	return retVal;
	}
    private JPanel createTableStatusPanel() {
    	JPanel thePanel = Static.createRowPanel();

    	tableType = Static.createTextFieldNoEdit(20);
    	Font f = tableType.getFont();
    	tableType.setFont(new Font(f.getFontName(),Font.BOLD, f.getSize()));
    	tableType.setMaximumSize(tableType.getPreferredSize());

    	tableHeader = Static.createTextFieldNoEdit(30);
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
		
	public String getSummary() { return strSummary; }

	private SeqsTablePanel getInstance() { return this; }
	 
    private void validateTable() {validate();} //Called from a thread
    
    /******************************************************/
    private void loadShortList() {
		try {
			DBConn mDB =  theViewerFrame.getDBConnection(); 
			DBinfo info = theViewerFrame.getInfo();
			String x = info.getSampleSeq(mDB);
			mDB.close();
			
			if (x==null || x=="") return;
			String [] y = x.split(":");
			if (y.length!=2) Out.PrtError("seq Build short list '" + x + "'");
			strSubQuery = y[1];
			strSummary  = y[0];
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting seq sample");}
	}
    /****************** gets *****************************/
  
 
	public String [] getSelectedColumns() {
	    String [] retVal = null;
	    	
	    int selectedCount = 0;
	    for(int x=0; x<chkFields.length; x++) 
	    	if(chkFields[x].isSelected())
	    		selectedCount++;
	    	
	    if(selectedCount == 0) {
 			for(int x=0; x<FieldData.SEQ_DEFAULTS.length; x++) {
 				chkFields[x].setSelected(FieldData.SEQ_DEFAULTS[x]);
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

    	if(selections == null || selections.length == 0) return defaultSelections;
    	
    	Vector<String> sels = new Vector<String> ();
    	boolean [] retVal = new boolean[columns.length];
    	
    	for(int x=0; x<selections.length; x++) sels.add(selections[x]);
    	for(int x=0; x<columns.length; x++) retVal[x] = sels.contains(columns[x]);
    	return retVal;
	}
	// XXX
	private String buildQueryStr(DBConn mdb) {
    try {
		FieldData theFields = FieldData.getSeqFields(theViewerFrame.getSeqLibList(), 
				theViewerFrame.getSeqDEList(), theViewerFrame.getMethodPrefixes());
   
    	String from, strQuery;
    	if (strSubQuery==null || strSubQuery.equals("")) strSubQuery= " 1 ";
    	
    	from = 	" FROM " + TABLE + " " + theFields.getJoins(); // join on unitrans.HITid=unique_hits.HITid
    	
    	if (theGrpTable!=null) { 
    		from += " LEFT JOIN pog_members ON pog_members.UTid = unitrans.UTid" +
    				" WHERE " + strSubQuery;
    		strQuery = "SELECT " + theFields.getDBFieldQueryList() +  from +
    				" order by unitrans.UTstr"; // CAS303 group  -> order
    	}
    	else {  
    	    from += " WHERE " + strSubQuery;
    	    strQuery = "SELECT " + theFields.getDBFieldQueryList() + from;
    	}
    	int cnt  = mdb.executeCount("select count(*) " + from);
        String per = Static.perText(cnt, theViewerFrame.getInfo().getCntSeq());
        loadStatus.setText("Getting " + cnt + " " + per + " filtered sequences from database" );
   	
    	return strQuery;
    } catch(Exception e) {ErrorReport.reportError(e, "Error processing query");return null;}
	}
	
	public SortTable getTable() { return theTable;}
	public TableData getTableData() { return theTableData;}

	
	private void clearColumns() {
		chkFields[0].setSelected(false);
		chkFields[1].setSelected(true);  // GrpID
		for(int x=2; x<chkFields.length; x++) {
			chkFields[x].setSelected(false);
		}
	}
	public String getSelectedColumn(String column) {
     	if(theTable.getSelectedRowCount() == 0) return null;
     	
     	int colIndex =	theTableData.getColumnHeaderIndex(column);
     	int [] sels = theTable.getSelectedRows();
     	String seqID =  ((String)theTableData.getValueAt(sels[0], colIndex));
     	return seqID;
    }
	public String getSelectedColumn(String column, int rowIdx) { // CAS305 for displaying if multiple rows selected
     	if(theTable.getSelectedRowCount() <= 1) return null;
     	
     	int colIndex =	theTableData.getColumnHeaderIndex(column);
     	int [] sels = theTable.getSelectedRows();
     	String seqID =  ((String)theTableData.getValueAt(sels[rowIdx], colIndex));
     	return seqID;
	}
   
    public void clearSelection() {theTable.clearSelection();}
    
    public int getRowCount() { return theTable.getRowCount();}
	public int getSelectedRowCount() {return theTable.getSelectedRowCount();}
	public int getSelectedRow() { return theTable.getSelectedRows()[0];}
	public int [] getSelectedRows() { return theTable.getSelectedRows();}
	
	 /***************************************************************
	  * Table stuff
	  */
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
		
		theTable.setDEnames(theViewerFrame.getSeqDEList());
        theTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        theTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        theTable.autofitColumns();
        	
        theTable.getSelectionModel().addListSelectionListener(selListener);
		theTable.addSingleClickListener(sngClick);
        theTable.addDoubleClickListener(dblClick);

        theTable.setTableHeader(new SortHeader(theTable.getColumnModel()));
        
        /* CAS304 If a header contains a '\n' multiple lines will appear using this renderer
         * the Sequence table has headings like 'UTR5 CpG' which may want to use this for.
         * but didn't work anyway
        MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer();
        Enumeration<TableColumn> en = theTable.getColumnModel().getColumns();
        while (en.hasMoreElements()) {
          (en.nextElement()).setHeaderRenderer(renderer);
        } 
        */
	}
   
    //When the view table gets sorted, sort the master table to match (Called by TableData)
    public void sortMasterColumn(String columnName, boolean ascending) {
	    int index = theTableData.getColumnHeaderIndex(columnName);
	    theTableData.sortByColumn(index, ascending);
    }
    
    private void showProgress() {
    	removeAll();
    	repaint();
    	setBackground(Static.BGCOLOR);
    	loadStatus = Static.createTextFieldNoEdit(100);
    	JButton btnStop = Static.createButton("Stop", true);
    	btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(buildThread != null) loadStatus.setText("Cancelled");
			}
		});
        add(loadStatus);
        add(btnStop);
        validateTable();
    }
    
    //Take built view table and build the panel
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
    	add(Box.createVerticalStrut(5));
    	add(tableSummaryPanel); add(Box.createVerticalStrut(5));
    	add(tableStatusPanel);
    	add(sPane);
    	add(fieldSelectPanel);  add(Box.createVerticalStrut(10));
    
    	JPanel temp = Static.createRowPanel();
    	temp.add(showColumnSelect);	temp.add(Box.createHorizontalStrut(5));
    	temp.add(clearColumn);		temp.add(Box.createHorizontalStrut(5));
    	temp.add(txtStatus);
    	temp.setMaximumSize(temp.getPreferredSize());
    	add(temp);
    	if(theTable != null) {
    		 if (theGrpTable!=null) 		tableType.setText("Sequence View for Clusters");
    		 else if (thePairTable!=null) 	tableType.setText("Sequence View for Pairs");
    		 else if (theHitTable!=null) 	tableType.setText("Sequence View for Hits");
    		 else 							tableType.setText("Sequence View");
    		 
    		 setTopEnabled(theTable.getSelectedRowCount(), theTable.getRowCount());
    	}
    	invalidate();
    	validateTable();
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
    /***************************************************************
     * Methods for getting info for various displays
     */
    private int getSelectedSQLid() { // copy, viewDetail, loadSelecteSeq
     	if(theTable.getSelectedRowCount() == 0) return -1;
     	
     	int [] sels = theTable.getSelectedRows();
     	int idx = theTableData.getColumnHeaderIndex(SEQ_SQLID);
     	return (Integer)theTableData.getValueAt(sels[0], idx);
    }
	
	private int [] getTableSQLid() { // Exports
		int idx = theTableData.getColumnHeaderIndex(SEQ_SQLID);
		int nRow = theTable.getRowCount();
		int [] ids = new int [nRow];
		for (int i=0; i<nRow; i++) 
			ids[i] = (Integer)theTableData.getValueAt(i, idx);
		return ids;
	}
    /**********************************************************
     * viewCluster - query DB for grpIDs
     * where pog_groups.PGid = grpID
     * DB call for grpID from pog_members
     *******************************************************/
	private void viewGrps() {
		if(getSelectedRowCount() ==0) { 
			JOptionPane.showMessageDialog(theViewerFrame, "Select a Sequence");
			return;
		}
		try{
			int [] sels = theTable.getSelectedRows();
	     	int sqlIdx = theTableData.getColumnHeaderIndex(SEQ_SQLID);
	     	
	     	int [] seqid = new int [sels.length];
			for (int i=0; i<sels.length; i++) 
				seqid[i] = (Integer)theTableData.getValueAt(sels[i], sqlIdx);
			
			String query = loadGrpIDs(seqid);				// query - list of seqids
			if (query==null) return;
			
			int numSeqs=seqid.length;
			String seqstr = getSelectedColumn(SEQID);
			int row = (numSeqs==1) ? getSelectedRow() : -1; // nParentRow
			
			String summary, tab;							// Summary, tab
			if (numSeqs==1) {
				tab = Globals.tagGRP + seqstr;
				summary =  getSumLine(row, seqstr);
			}
			else {
				tab = Globals.tagGRP + seqTAG + numSeqs;
				summary =  Globals.tagSEQs + " " + seqstr;
				for (int i=1; i<numSeqs && i<7; i++) {
					String seqstr2=getSelectedColumn(SEQID, i);
					summary += ", " + seqstr2;
				}
				if (numSeqs>7) summary += "... (" + numSeqs + " total)";
			}
			
			GrpTablePanel grpPanel = new GrpTablePanel(theViewerFrame, getInstance(), query, tab, summary, row); // CAS310 change in args
			theViewerFrame.addResultPanel(getInstance(), grpPanel, grpPanel.getName(), grpPanel.getSummary());

		} catch(Exception e) {ErrorReport.reportError(e, "Error viewing groups");
		} catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error viewing groups", theViewerFrame);}
	}
	  // Called from GrpTable for Next/Prev sequence - clusters for sequence (could be none)
 	public String [] getNextSeqRowForGrp(int nextRow) {
     	int row = getTranslatedRow(nextRow);
     		
 		String name = getRowSeqName(row);
 		
 		int [] seqid = new int [1];
 		int idx = 	theTableData.getColumnHeaderIndex(SEQ_SQLID);
		seqid[0] =  (Integer)theTableData.getValueAt(row, idx);
 		
 		String [] retVal = new String[4];
 		retVal[0] = loadGrpIDs(seqid);		// query
 		retVal[1] = Globals.tagGRP + name;	// tab
 		retVal[2] = getSumLine(row, name);	// summary
 		retVal[3] = row + ""; 				// nParentRow
 	
 		return retVal;
     } 
	private String loadGrpIDs(int [] utid) { // viewGrps and getNextSeqRowForGrp
	try {
		DBConn mDB = theViewerFrame.getDBConnection();
		Vector<Integer> grpIDs = new Vector<Integer> ();
		ResultSet rset=null;
		for (int id : utid) {
			rset = mDB.executeQuery("SELECT PGid FROM pog_members WHERE UTid=" + id + " GROUP BY PGid");
			while(rset.next()) {
				int grpid = rset.getInt(1);
				if (!grpIDs.contains(grpid)) grpIDs.add(grpid);
			}
		}
		if (rset!=null) rset.close(); 
		mDB.close();
		
		if (grpIDs.size()==0) {
			UserPrompt.showMsg("No clusters for selected sequences");
			return null;
		}
		return GrpTablePanel.makeSQLfromGRPid(grpIDs.toArray(new Integer[0]));
		
	} catch(Exception e) {ErrorReport.reportError(e, "Error viewing groups"); return null;}
	}
	/************************************************************************
	 * viewPairs for selected sequences
	 */
	private void viewPairs() {
		if(getSelectedRowCount() ==0 ) { 
			JOptionPane.showMessageDialog(theViewerFrame, "Select one or more sequences");
			return;
		}
		try{	
			// Get sequence names and sql index
			int [] sels = theTable.getSelectedRows();
	     	int sqlIdx = theTableData.getColumnHeaderIndex(SEQ_SQLID);
	     	
	     	int idIdx = theTableData.getColumnHeaderIndex(SEQID);
	     	String [] seqstr = new String [sels.length];
	     	Integer [] seqSQL = new Integer [sels.length];
	     	for (int i=0; i<sels.length; i++) {
	     		seqstr[i] = (String)theTableData.getValueAt(sels[i], idIdx);
	     		seqSQL[i] = (Integer)theTableData.getValueAt(sels[i], sqlIdx);
	     	}
			
			int cnt=0, numSeqs=seqSQL.length;
			
			int row = -1;	// nParentRow;
			String query = null, summary=null;								// summary, query
			String tab;	
			
			if (numSeqs==1) {
				row = getSelectedRow();
				query = "(" + seqSQL[0] + ")";
				tab = Globals.tagPAIR +  seqstr[0];
				summary =  getSumLine(row, seqstr[0]);
			}
			else {
				tab = Globals.tagPAIR + seqTAG + numSeqs;
				summary = Globals.tagSEQs + " " + seqstr[0];
				for (int i=0; i<numSeqs; i++) { // make list
					if (query==null) query = "(" + seqSQL[i];
					else {
						query +=  "," + seqSQL[i];
						if (cnt<7) summary += ", " + seqstr[i];
					}
					cnt++;
				}
				query += ")";
				if (numSeqs>7) summary += "... (" + numSeqs + " total)";
			}
			
			PairTablePanel pairPanel = new PairTablePanel(theViewerFrame, getInstance(),  query, tab, summary, row);
			theViewerFrame.addResultPanel(getInstance(), pairPanel, pairPanel.getName(), pairPanel.getSummary());
			
		} catch(Exception e) {ErrorReport.reportError(e, "Error viewing groups");
		} catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error viewing groups", theViewerFrame);}
	}
	// Pair Table Next/Prev to get next sequence row
    public String [] getNextSeqRowForPair(int nextRow) {
    	int row = getTranslatedRow(nextRow);
    		
		String name = getRowSeqName(row);
		int id = (Integer)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(SEQ_SQLID));
	
		String [] retVal = new String[4];
		retVal[0] = "(" + id + ")";					// query
		retVal[1] = Globals.tagPAIR +  name;		// tab
		retVal[2] = getSumLine(row, name);			// summary
		retVal[3] = row + "";						// nParentRow
	
		return retVal;
    }
	/*********************************************************************/
	private void viewDetails() {
		if (getSelectedRowCount() != 1) { 
			JOptionPane.showMessageDialog(theViewerFrame, "Select a Sequence");
			return;
		}
		try {
			int row = getSelectedRow();
			int seqid = (Integer) getSelectedSQLid();
			String seqname =      getSelectedColumn(SEQID);
			
			SeqTopRowPanel newPanel = new SeqTopRowPanel(theViewerFrame, this, seqname, seqid, row);
			
			String tab = Globals.tagDETAIL + seqname + "-" + (row+1);
			String sum = getSumLine(row, seqname);
			theViewerFrame.addResultPanel(getInstance(), newPanel, tab, sum);
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error viewing details");}
	}
	public int getNextSeqForDetail(int row) {
	   	return (Integer)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(SEQ_SQLID));
	}
	/***************************************************************************
	 * Align - 
	 */
    public String [] getAlignSeqIDs() { // Align 
    	boolean clearSel = false;
    	if(theTable.getSelectedRowCount() == 0) {
    		theTable.selectAll();
    		clearSel = true;
    	}
	    int colIndex = theTableData.getColumnHeaderIndex(SEQID);
	    int [] sels  = theTable.getSelectedRows();
	 	String [] seqIDs = new String [sels.length];
	 	
	  	for(int x=0; x<sels.length; x++)
	  		seqIDs[x] =  ((String)theTableData.getValueAt(sels[x], colIndex));
	    	
	    if(clearSel) theTable.clearSelection();
	    return seqIDs;
    }
    
	 public String [] getTabSum() {
    	int [] sels = theTable.getSelectedRows();
    	
    	String [] retVal = new String[3];
    	retVal[0] = "Seqs " + sels.length; // puts MSA: on in AlignButtons
    	retVal[1] = "Seqs " + sels.length;
    	retVal[2] = "-1";		// have to select multiple rows, so never a next
    	return retVal;
    }
	/*********************************************
	 * SeqTable Next/Prev button
	 */
	private void getNextRow(int rowNum) {
		String [] strVals = null;
		if (theGrpTable!=null) {
			strVals = theGrpTable.getNextSeqRowForGrp(rowNum); // pog_members.PGid = 2
		}
		else if (thePairTable!=null) {
			strVals = thePairTable.getNextPairRowForSeq(rowNum); // (UTID = 1 or UTID = 350)
		}
		else if (theHitTable!=null) {
			strVals = theHitTable.getNextHitRowForSeq(rowNum);  // unitrans.HITid = 2712
		}
		else Out.die("TCW error on table type");
		
		strSubQuery = 		strVals[0];
		tabName = 			strVals[1];
		strSummary = 		Globals.trimSum(strVals[2]);
		nParentRow = 		Integer.parseInt(strVals[3]);	
		buildQueryThread();
		
        theViewerFrame.changePanelName(this, tabName, strSummary);
	}
		
 	private String getSumLine(int row,  String name) {
		return "Row " + (row+1) + "/" + getRowCount() + "   " +  Globals.tagSEQ + name;	// summary
	}
 	 /* SeqTopRowPanel next and prev */
    public int getTranslatedRow(int row) {
  		if (theTable==null) return 0;

  		if(row >= theTable.getRowCount()) return 0; // CAS340 add '>'
  		if(row < 0) return theTable.getRowCount() - 1;
  		return row;
    }
   
    public String getRowSeqName(int row) {
		return (String)theTableData.getValueAt(row, theTableData.getColumnHeaderIndex(SEQID));
    }
   
   /*********************************************************************************/
    private JPanel tableButtonPanel = null, tableStatusPanel = null, fieldSelectPanel = null, tableSummaryPanel = null;
	
    private JButton btnNextRow = null, btnPrevRow = null;
	private JButton btnCopy = null, btnTable = null, btnHelp = null;
	private JButton btnViewDetails = null, btnViewGroups = null, btnViewPairs = null, btnMSA=null;
	private JButton showColumnSelect = null, clearColumn = null;
	
    private SortTable theTable = null;
    private TableData theTableData = null;
    private JScrollPane sPane = null;
  
    private JTextField tableHeader = null, tableType = null, loadStatus = null, txtStatus = null;
    private JLabel lblSummary = null;
    
    private ActionListener dblClick = null;
    private ActionListener sngClick = null;
    private ListSelectionListener selListener = null;
    
    private JCheckBox [] chkFields = null;
    private ActionListener colSelectChange = null;
    
	private Thread buildThread = null;
	
	private MTCWFrame theViewerFrame = null;
	private ViewerSettings vSettings = null;
	
	private GrpTablePanel theGrpTable = null;
	private PairTablePanel thePairTable = null;
	private HitTablePanel theHitTable = null;
	
	private String strSummary = null, strSubQuery = null, tabName = "";
	private int nParentRow = -1, totalSeq=0;
	private boolean hasNTdbOnly=false, hasGOs=false, isList=false;
}
