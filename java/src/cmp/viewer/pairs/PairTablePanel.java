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
import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Vector;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;
import cmp.align.SumStats;
import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.ViewerSettings;
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.seq.SeqsTopRowPanel;
import cmp.viewer.table.*;

public class PairTablePanel extends JPanel {
	private static final long serialVersionUID = -3827610586702639105L;
	private static boolean initFirstAppletPOGRun = true;
	
	private static final String TABLE = FieldData.PAIR_TABLE;
	private static final String PAIRID = FieldData.PAIRID;
	private static final String SEQ_ID = FieldData.SEQ_TABLE + "." + FieldData.SEQ_SQLID;
	private static final String SEQ_ID1 = FieldData.SEQID1; 
	private static final String SEQ_ID2 = FieldData.SEQID2; 
	private static final String ID1_SQL = FieldData.ID1_SQLID; 
	private static final String ID2_SQL = FieldData.ID2_SQLID; 	
	private static final String HITID = FieldData.HITID;
	private static final String HITDESC = FieldData.HITDESC;
	private static final String helpHTML = "PairTable.html";
	
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
	// From groups table
	public PairTablePanel(MTCWFrame parentFrame, GrpTablePanel parentList,
			String tab,  String sum, int grp, String name, int row) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		theGrpTable = parentList;
		nParentRow = row;
		initData(parentFrame, tab);	
		
		pairsForGrpID = grp;
		grpName = name; // name.substring(0, name.indexOf("_")) + " like '" +  name + "'"; // column name like cluster name 
		strQuerySummary = sum;
		
		buildQueryThread(); 
	}
	// from seq table
	public PairTablePanel(MTCWFrame parentFrame, SeqsTopRowPanel parentList,
			String tab,  String sum, String list, int row) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		theSeqTable = parentList;
		nParentRow = row;
		initData(parentFrame, tab);	
		
		seqList = list;
		strQuerySummary = sum;
		
		buildQueryThread(); 
	}
	private void loadNewGrpRow(int inc) {
		if (inc<0) nParentRow = theGrpTable.getTranslatedRow(nParentRow - 1);
		else nParentRow = theGrpTable.getTranslatedRow(nParentRow + 1);
		
		String [] strVals = theGrpTable.getPairsQueryNext(nParentRow);
		pairsForGrpID = Integer.parseInt(strVals[0]);
		grpName = strVals[1];
		tabName = strVals[2];
		strQuerySummary = strVals[3];
		
		buildQueryThread();
	}
	private void loadNewPairRow(int inc) {
		int nextRow = (inc<0) ? nParentRow - 1 : nParentRow + 1;
		
		String [] strVals = theSeqTable.getPairsQueryNext(nextRow);
		seqList = strVals[0];
		tabName = strVals[1];
		strQuerySummary = strVals[2];
		nParentRow = Integer.parseInt(strVals[3]);
		
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
 				try{						
 					String [] strVals = getSeqQueryList();//tab, summary, Where
 					
 					int row = -1;
 					if (theTable.getSelectedRowCount() == 1) row = theTable.getSelectedRow();
 					SeqsTopRowPanel newPanel = new SeqsTopRowPanel(theViewerFrame, getInstance(), 
 							strVals[0], strVals[1], strVals[2], row);
 					theViewerFrame.addResultPanel(getInstance(), newPanel, newPanel.getName(), 
 							strVals[1]);
 				
 				} catch(Exception e) {ErrorReport.reportError(e,  "View Selected Sequence");
 				} catch(Error e) {ErrorReport.reportFatalError(e, "View Selected Sequence", theViewerFrame);}
 			}
 		});
        topRow.add(btnTableSeqs);
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
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, 
						"Pair table", "html/viewMultiTCW/" + helpHTML);
			}
		});
        topRow.add(btnHelp);

        if(nParentRow >= 0) { // if -1, then showing members from multiple clusters, and no Next/Prev
 	 	   JPanel rowChangePanel = Static.createRowPanel();
 	 	   rowChangePanel.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
 	 	   
 	 	   btnPrevRow = Static.createButton("<< Prev", true);
 	 	   btnPrevRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   if (seqList!=null) loadNewPairRow(-1);
 	 			   else loadNewGrpRow(-1);
 	 		   }
 	 	   });
 	 	  
 	 	   btnNextRow = Static.createButton("Next >>", true);
 	 	   btnNextRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   if (seqList!=null) loadNewPairRow(1);
	 			   else  loadNewGrpRow(1);
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
    	 					new SumStats(mDB).fromView(ids, theViewerFrame.isApplet(), summary);
    	 					// close in SumStats
    	 				} catch(Exception ee) {ErrorReport.reportError(ee,  "View stats");
    	 				} catch(Error ee) {ErrorReport.reportFatalError(ee, "View stats", theViewerFrame);}
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
 				new TableUtil(theViewerFrame).exportTableTab(theTable, Globals.bPAIR);
 			}
 		}));
 		
 		popup.add(new JMenuItem(new AbstractAction("Export both AA sequences of pairs (" + Globalx.FASTA_SUFFIX + ")") { 
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportPairSeqFa(theTableData, 0, FieldData.AASEQ_SQL);
 			}
 		}));
 		if (!hasAAdb) {
	 		popup.add(new JMenuItem(new AbstractAction("Export both NT sequences of pairs ("+ Globalx.FASTA_SUFFIX + ")") { 
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportPairSeqFa(theTableData, 0, FieldData.NTSEQ_SQL);
	 			}
	 		}));
 		}
 		popup.add(new JMenuItem(new AbstractAction("Export 1st AA sequence of pairs (" + Globalx.FASTA_SUFFIX + ")") { 
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportPairSeqFa(theTableData, 1, FieldData.AASEQ_SQL);
 			}
 		}));
 		
 		if (!hasAAdb) {
	 		popup.add(new JMenuItem(new AbstractAction("Export 1st NT sequence of pairs (" + Globalx.FASTA_SUFFIX + ")") { 
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportPairSeqFa(theTableData, 1, FieldData.NTSEQ_SQL);
	 			}
	 		}));
 		}
 
 		if (hasGOs) {
 			popup.addSeparator();
	 		popup.add(new JMenuItem(new AbstractAction("Export Pairs GOs (" + Globalx.CSV_SUFFIX + ")...") { 
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportPairGO(theTableData, strQuerySummary);
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
		hasAAdb = (theViewerFrame.getnAAdb()>0);
		vSettings = parentFrame.getSettings();
		tabName = resultName;
		totalPairs = theViewerFrame.getInfo().getCntPair();
		methods = theViewerFrame.getInfo().getMethodPrefix();
		hasGOs = (theViewerFrame.getInfo().getCntGO()>0);

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
        theTableData.finalize();

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
	    boolean [] selections = null;
    		
    		if(theViewerFrame.isApplet() && initFirstAppletPOGRun) {
    			initFirstAppletPOGRun = false;
    			selections = getColumnSelections(columns, defaults, null);
    		}
    		else
    			selections = getColumnSelections(columns, defaults, 
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
	      
	        	if (pairsForGrpID>0) {// list pairs in cluster; called from group table e.g. CL like CL_00001
	        		String grp = grpName.substring(0, grpName.indexOf("_")) + " like '" +  grpName + "'"; 
	        		strQuery += " WHERE " + grp;
	        		cntSQL = "select count(*) from " + TABLE + " where " + grp;
	        	}
	        	else if (seqList!=null) { // seqList example "(1,2,3)"
	        		String clause = "UTid1 IN " + seqList + " or UTid2 IN " + seqList;
	        		strQuery += " WHERE " + clause;
	        		cntSQL = "select count(*) from " + TABLE + " where " + clause;
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
		DBinfo info = theViewerFrame.getInfo();
		String x = info.getSamplePair();
		if (x==null || x=="") return;
		String [] y = x.split(":");
		strSubQuery = y[1];
		strQuerySummary = y[0];
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
    
 // selection of multiple pairs
    private String [] getSeqQueryList() {
    		int [] sels = theTable.getSelectedRows();
    		if (sels==null || sels.length==0) return getSeqQueryNext(0);
    		if (sels.length==1) return getSeqQueryNext(sels[0]);
    		else return getSeqQueryList(sels);
    }
    // This is used for Next/Prev and for currect selected
    public String [] getSeqQueryNext(int row) {
		int IDidx = theTableData.getColumnHeaderIndex(PAIRID);
		int IDidx1 = theTableData.getColumnHeaderIndex(ID1_SQL); // index
		int IDidx2 = theTableData.getColumnHeaderIndex(ID2_SQL);
		int id1 = (Integer)theTableData.getValueAt(row, IDidx1);
		int id2 = (Integer)theTableData.getValueAt(row, IDidx2);
		int pairid = ((Integer)theTableData.getValueAt(row, IDidx));			
		
		String [] retVal = new String[3];
		retVal[0] = MTCWFrame.SEQ_PREFIX + ": Pair#" + pairid; 	// tab on left
		retVal[1] = "Pair#" + pairid;							// display as query on table
		retVal[2] = " (" + SEQ_ID + " = " + id1 + " or " + SEQ_ID + " = " + id2 + ")"; // Where

		return retVal;
    }
    // select multiple pairs select UTstr from unitrans where UTid in (1,2,3)
    public String [] getSeqQueryList(int [] row) {
    		String [] retVal = new String[3];
    		
    		retVal[0]="Seq: " + row.length + " pairs";
    		retVal[1]="Pair ";
    		retVal[2]= " UTid in (";
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
				retVal[1] += ", #" + pairid;
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
    		retVal[2] += ")";
		return retVal;
    }
    
   
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
	    	 	tableType.setText("Pair View");
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
      
      //If a header contains a '\n' multiple lines will appear using this renderer
      MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer();
      Enumeration<TableColumn> en = theTable.getColumnModel().getColumns();
      while (en.hasMoreElements()) {
        ((TableColumn)en.nextElement()).setHeaderRenderer(renderer);
      } 
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
    
    public class MultiLineHeaderRenderer extends JList implements TableCellRenderer {
		private static final long serialVersionUID = 3118619652018757230L;

		public MultiLineHeaderRenderer() {
    	    setOpaque(true);
    	    setBorder(BorderFactory.createLineBorder(Color.BLACK));
    	    setBackground(Globals.BGCOLOR);
    	    ListCellRenderer renderer = getCellRenderer();
    	    ((JLabel)renderer).setHorizontalAlignment(JLabel.CENTER);
    	    setCellRenderer(renderer);
    	  }
    	 
    	  public Component getTableCellRendererComponent(JTable table, Object value,
    	                   boolean isSelected, boolean hasFocus, int row, int column) {
    	    setFont(table.getFont());
    	    String str = (value == null) ? "" : value.toString();
    	    BufferedReader br = new BufferedReader(new StringReader(str));
    	    String line;
    	    Vector<String> v = new Vector<String>();
    	    try {
    	      while ((line = br.readLine()) != null) {
    	        v.addElement(line);
    	      }
    	      br.close(); 
    	    } catch (Exception e) {ErrorReport.reportError(e, "Error rendering table cells");}
    	    
    	    setListData(v);
    	    return this;
    	  }
    	}
    public int getTranslatedRow(int row) {
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
    private JButton btnShow = null, btnCopy = null, btnTable = null, btnHelp = null;
    private JButton btnNextRow = null, btnPrevRow = null;
    private int nParentRow = -1;
    private JButton btnTableSeqs = null;
    
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
	private int pairsForGrpID=0;
	private String grpName="";
	
	private SeqsTopRowPanel theSeqTable = null;
	private String seqList=null;
	
	private ViewerSettings vSettings = null;
	private int totalPairs=0;
	private String [] methods;
	private boolean isList=false, hasAAdb=false, hasGOs=false;
	
}
