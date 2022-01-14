package cmp.viewer.groups;
/******************************************************
 * Cluster table
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
import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.ViewerSettings;
import cmp.viewer.align.AlignButtons;
import cmp.viewer.pairs.PairTablePanel;
import cmp.viewer.seq.SeqsTablePanel;
import cmp.viewer.table.*;

public class GrpTablePanel extends JPanel {
	private static final long serialVersionUID = -3827610586702639105L;
 
	private static final String TABLE = FieldData.GRP_TABLE;
	private static final String GRP_SQLID = FieldData.GRP_SQLID;
	private static final String CLUSTERID = FieldData.CLUSTERID;
	private static final String CLUSTERCOUNT = FieldData.CLUSTERCOUNT;
	private static final String HITID = FieldData.HITID;
	private static final String HITDESC = FieldData.HITDESC;
	private static final String SCORE1 = FieldData.SCORE1;
	private static final String SCORE2 = FieldData.SCORE2;
	private static final String helpHTML = Globals.helpDir + "GrpTable.html";

	private final String pSEQ = MTCWFrame.SEQ_PREFIX;
	private final String pGRP = MTCWFrame.GRP_PREFIX;
	private final String pPAIR = MTCWFrame.PAIR_PREFIX;
	private final String pGRPs = MTCWFrame.GRP_PREFIX + "s";
	
	// from filter or '>Clusters'
	public GrpTablePanel(MTCWFrame parentFrame, String resultName) {
		if (resultName.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		initData(parentFrame, resultName);
		
		GrpQueryPanel theQueryPanel = theViewerFrame.getGrpQueryPanel();
		if (theQueryPanel!=null) {
			strSubQuery =     theQueryPanel.getSubQuery();
			strQuerySummary = theQueryPanel.getQuerySummary();
		}
		else {
			buildShortList(); // first time
		}
		
		strQuerySummary = MTCWFrame.FILTER +strQuerySummary;
				
		buildQueryThread(); 
	}
	// From Seq Table
	public GrpTablePanel(MTCWFrame parentFrame, SeqsTablePanel parentList, 
			String sql, String tab, String summary,  int row) {
		
		theSeqTable = parentList;
		nParentRow = row;
		
		initData(parentFrame, tab);
		
		strQuerySummary = summary;
		strSubQuery = sql;
				
		buildQueryThread(); 
	}
	// From Pairs table
	public GrpTablePanel(MTCWFrame parentFrame, PairTablePanel parentList, 
			String sql, String tab, String summary,  int row) {
		
		thePairTable = parentList;
		nParentRow = row;
		
		initData(parentFrame, tab);
		
		strQuerySummary = summary;
		strSubQuery = sql;
				
		buildQueryThread(); 
	}
	
	/** XXX Buttons for Cluster table */
    private JPanel createTableButton() {
    	JPanel topRow = Static.createRowPanel();
    	JPanel buttonPanel = Static.createPagePanel();
    	topRow.add(Static.createLabel(Globals.select));	topRow.add(Box.createHorizontalStrut(2));
    	
	   	btnShowSeqs = Static.createButtonTab(MTCWFrame.SEQ_TABLE, false);
    	btnShowSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewSequences();	
			} 
		});
    	topRow.add(btnShowSeqs);				topRow.add(Box.createHorizontalStrut(2));
    	
    	btnShowPairs = Static.createButtonTab(MTCWFrame.PAIR_TABLE, false);
    	btnShowPairs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewPairs();
			}
		});
        topRow.add(btnShowPairs);				topRow.add(Box.createHorizontalStrut(2));    
        
        AlignButtons msaObj = new AlignButtons(theViewerFrame, getInstance()); // CAS340 new
        btnMSA = msaObj.createBtnMultiAlign();
        topRow.add(btnMSA);						topRow.add(Box.createHorizontalStrut(2)); 
        
        AlignButtons msadbObj = new AlignButtons(theViewerFrame, getInstance()); // CAS340 new
        btnMSAdb = msadbObj.createBtnMultiDB();
        if (hasMSA) {
        	topRow.add(btnMSAdb);				topRow.add(Box.createHorizontalStrut(2)); 
        }  
        
        createBtnCopy();
 		topRow.add(btnCopy);					topRow.add(Box.createHorizontalStrut(8));
 		
        createBtnTable();
        topRow.add(btnTable);					topRow.add(Box.createHorizontalGlue());
        
        btnHelp = Static.createButtonHelp("Help", true);
        btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Cluster table", helpHTML);
			}
		});
        topRow.add(btnHelp);					topRow.add(Box.createHorizontalStrut(2)); 
        
        if (nParentRow >= 0) { // if -1, then showing members from multiple clusters or ">", and no Next/Prev
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
  	 	   
  	 	   topRow.add(btnPrevRow);topRow.add(Box.createHorizontalStrut(1)); 
  	 	   topRow.add(btnNextRow);
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
	    copyPop.add(new JMenuItem(new AbstractAction("Cluster ID") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				int row = theTable.getSelectedRow();
 				int idx = theTableData.getColumnHeaderIndex(CLUSTERID); 
 				String seqID =  ((String)theTableData.getValueAt(row, idx));
 				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
 				cb.setContents(new StringSelection(seqID), null);
 			}
 		}));
		copyPop.add(new JMenuItem(new AbstractAction("Hit ID") {
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
   			   new TableUtil().statsPopUp("Clusters: " + strQuerySummary, theTable);
   		   }
   	   }));
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
  		popup.add(new JMenuItem(new AbstractAction("Export table (" + Globalx.TSV_SUFFIX + ")") {
  			private static final long serialVersionUID = 4692812516440639008L;
  			public void actionPerformed(ActionEvent e) {
  				new TableUtil(theViewerFrame).exportTableTab(btnTable, theTable, Globals.bGRP);
  			}
  		}));
  		popup.add(new JMenuItem(new AbstractAction("Export all cluster AA sequences (" + Globalx.FASTA_SUFFIX + ")") { 
  			private static final long serialVersionUID = 4692812516440639008L;
  			public void actionPerformed(ActionEvent e) {
  				new TableUtil(theViewerFrame).exportGrpSeqFa(btnTable,theTableData, FieldData.AASEQ_SQL);
  			}
  		}));
  		if (!hasAAdb) {
	  		popup.add(new JMenuItem(new AbstractAction("Export all cluster NT sequences (" + Globalx.FASTA_SUFFIX + ")") { 
	  			private static final long serialVersionUID = 4692812516440639008L;
	  			public void actionPerformed(ActionEvent e) {
	  				new TableUtil(theViewerFrame).exportGrpSeqFa(btnTable,theTableData, FieldData.NTSEQ_SQL);
	  			}
	  		}));
  		}
  		if (hasGOs) {
  			popup.addSeparator();
	  		popup.add(new JMenuItem(new AbstractAction("Export cluster GOs (" + Globalx.TSV_SUFFIX + ")...") { 
	  			private static final long serialVersionUID = 4692812516440639008L;
	  			public void actionPerformed(ActionEvent e) {
	  				new TableUtil(theViewerFrame).exportGrpGO(btnTable,theTableData, strQuerySummary);
	  			}
	  		}));
  		}
  		if (hasCounts) { // CAS305
  			popup.addSeparator();
	  		popup.add(new JMenuItem(new AbstractAction("Export cluster counts (" + Globalx.TSV_SUFFIX + ")...") { 
	  			private static final long serialVersionUID = 4692812516440639008L;
	  			public void actionPerformed(ActionEvent e) {
	  				new TableUtil(theViewerFrame).exportGrpCounts(btnTable,theTableData, strQuerySummary, true);
	  			}
	  		}));
	  		popup.add(new JMenuItem(new AbstractAction("Export cluster TPM (" + Globalx.TSV_SUFFIX + ")...") { 
	  			private static final long serialVersionUID = 4692812516440639008L;
	  			public void actionPerformed(ActionEvent e) {
	  				new TableUtil(theViewerFrame).exportGrpCounts(btnTable,theTableData, strQuerySummary, false);
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
	private void initData(MTCWFrame parentFrame, String tab) {
		theViewerFrame = parentFrame;
		vSettings = parentFrame.getSettings();
		tabName = tab;
		
		totalGrp = theViewerFrame.getInfo().getCntGrp();
		asmNames = theViewerFrame.getInfo().getASM();
		hasGOs = (theViewerFrame.getInfo().getCntGO()>0);
		hasAAdb = (theViewerFrame.getnAAdb()>0);
		hasCounts = theViewerFrame.getInfo().hasCounts();
		hasMSA	= theViewerFrame.getInfo().hasMultiScore();
		
		colSelectChange = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateTable(false);
				displayTable();
				
				vSettings.getGrpSettings().setSelectedColumns(getSelectedColumns());
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
					DBConn conn = theViewerFrame.getDBConnection();
					String sql = buildQueryStr(conn);
					ResultSet rset = MTCWFrame.executeQuery(conn, sql, loadProgress);
					if(rset != null) {
						buildTable(rset);
						MTCWFrame.closeResultSet(rset); //Thread safe way of closing the resultSet
						updateTable(true);
					}
					conn.close();
					
					displayTable();
					
					if(isVisible()) {//Makes the table appear (A little hacky, but fast)
						setVisible(false);
						setVisible(true);
					}
				} catch (Exception e) {ErrorReport.reportError(e, "build cluster table");
				} catch (Error e) {ErrorReport.reportFatalError(e, "build clustertable", theViewerFrame);}
			}
		});
		buildThread.setPriority(Thread.MIN_PRIORITY);
		buildThread.start();
	}
	/*****************************************************************
	 * Build interface
	 */
	private void buildTable(ResultSet rset) {
        FieldData theFields = null;
       
        theFields = FieldData.getGrpFields(asmNames);
    
        tableButtonPanel = createTableButton();    
		tableStatusPanel = createTableStatusPanel();	
		tableSummaryPanel = createFilterSummaryPanel();
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
		clearColumn = Static.createButton("Clear Columns");
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
        theTableData.addRowsWithProgress(rset, theFields, loadProgress);
        theTableData.showTable();

        int nRow = theTableData.getNumRows();
        String status = nRow + " of " + totalGrp + " " + Static.perText(nRow, totalGrp);
        tableHeader.setText(status);
        
        if(!isList) {
            theViewerFrame.changePanelName(this, tabName + ": " + nRow, strQuerySummary);
         }
    }
	private void setTopEnabled() {
		int selCount = theTable.getSelectedRowCount();
		
		btnShowSeqs.setEnabled(selCount>0);
		btnShowPairs.setEnabled(selCount>0);
		
		boolean b = selCount==1;
		btnCopy.setEnabled(b);
		btnMSA.setEnabled(b);
		btnMSAdb.setEnabled(b);
	}
    private JPanel createFieldSelectPanel() {
    	JPanel page = Static.createPagePanel();
    	
    	String [] sections = FieldData.getGrpColumnSections();;
    	int [] secIdx = FieldData.getGrpColumnSectionIdx(asmNames.length);
    	int [] secBreak = FieldData.getGrpColumnSectionBreak();
    	String [] columns = FieldData.getGrpColumns(asmNames);
    	String [] descriptions = FieldData.getGrpDescript(asmNames);
    	boolean [] defaults = FieldData.getGrpSelections(asmNames.length);
    	
    	boolean [] selections = getColumnSelections(columns, defaults, 
							theViewerFrame.getSettings().getGrpSettings().getSelectedColumns());    
    	
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
    	 	int space=3;
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
    private JPanel createFilterSummaryPanel() {
    	JPanel thePanel = Static.createRowPanel();
   
    	JLabel lblSummary = Static.createLabel(strQuerySummary, true);
    	lblSummary.setFont(getFont());
    	thePanel.add(lblSummary);
    	
    	return thePanel;
	}
	
	public String getSummary() { return strQuerySummary; }
	private GrpTablePanel getInstance() { return this; }
	
	private String buildQueryStr(DBConn mdb) {
        try {
    		FieldData theFields = FieldData.getGrpFields(asmNames);
    		
        	String strQuery = "SELECT " + theFields.getDBFieldQueryList() + " FROM " + TABLE + " ";
        	if(theFields.hasJoins())
        		strQuery += theFields.getJoins();
        	
        	if(strSubQuery != null && strSubQuery.length() > 0)
        		strQuery += " WHERE " + strSubQuery;
        	else strSubQuery = " 1 ";
        	
        	if (strSubQuery.contains("unique_hits")) { 
        		 loadProgress.setText("Getting filtered clusters from database" );
        	}
        	else {
	        	int cnt = mdb.executeCount("select count(*) from " + TABLE + " WHERE " + strSubQuery);
	        	String per = Static.perText(cnt, theViewerFrame.getInfo().getCntGrp());
	        	loadProgress.setText("Getting " + cnt + " " + per + " filtered clusters from database" );
        	}
        	return strQuery;
        } catch(Exception e) {ErrorReport.reportError(e, "Error processing query"); return null;}
	}
	private void buildShortList() {
		try {
			DBConn mDB = theViewerFrame.getDBConnection();
			DBinfo info = theViewerFrame.getInfo();
			String x = info.getSampleGrp(mDB);
			mDB.close();
			
			if (x==null || x=="") return;
			String [] y = x.split(":");
			strSubQuery = y[1];
			strQuerySummary = y[0];
		} catch(Exception e) {ErrorReport.reportError(e, "Error processing query"); }
	}
	private void clearColumns() {
		chkFields[0].setSelected(false);
		chkFields[1].setSelected(true);  // GrpID
		for(int x=2; x<chkFields.length; x++) {
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
			for(int x=0; x<FieldData.GRP_DEFAULT.length; x++) {
				chkFields[x].setSelected(FieldData.GRP_DEFAULT[x]);
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
    
    
  //When the view table gets sorted, sort the master table to match (Called by TableData)
    public void sortMasterColumn(String columnName, boolean ascending) {
	    	int index = theTableData.getColumnHeaderIndex(columnName);
	    	theTableData.sortByColumn(index, ascending);
    }
    
    private void showProgress() {
    	removeAll();
    	repaint();
    	setBackground(Static.BGCOLOR);
    	loadProgress = new JTextField(100);
    	loadProgress.setBackground(Static.BGCOLOR);
    	loadProgress.setMaximumSize(loadProgress.getPreferredSize());
    	loadProgress.setEditable(false);
    	loadProgress.setBorder(BorderFactory.createEmptyBorder());
    	
    	JButton btnStop = Static.createButton("Stop");
    	btnStop.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			//This not only informs the user that it is canceled, 
			//but this is also the signal for the thread to stop. 
			if(buildThread != null)
				loadProgress.setText("Cancelled");
		}
		});
        add(loadProgress);
        add(btnStop);
        validateTable();
    }
    
    //Take built view table and build the panel
    private void displayTable() {
    	removeAll();
    	repaint();
    	loadProgress = null;
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
    	temp.setBackground(Static.BGCOLOR);
    	temp.add(showColumnSelect);
    	temp.add(Box.createHorizontalStrut(5));
    	temp.add(clearColumn);
    	temp.add(Box.createHorizontalStrut(5));
    	temp.add(txtStatus);
    	temp.setMaximumSize(temp.getPreferredSize());
    	add(temp);
    	if(theTable != null) {
    		if (theSeqTable!=null) 			tableType.setText("Cluster View for Sequences");
    		else if (thePairTable!=null) 	tableType.setText("Cluster View for Pairs");
    		else 							tableType.setText("Cluster View");
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
			//Sync the orders of the selected columns with the order in the table
			String [] columns = TableData.orderColumns(theTable, getSelectedColumns());
			//Copy the new model from the master table
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
    /*******************************************************
     * viewPairs
     */
    private void viewPairs() {
    	try{	
    		int [] sels = theTable.getSelectedRows();
    		if (sels.length==0) return;
    		int num=sels.length;
    		int Nameidx = theTableData.getColumnHeaderIndex(CLUSTERID);
    		
    		String sum="", where=null;
    		for (int i=0; i<num; i++) {
    			int row = sels[i];
			
    			String grpName = (String)theTableData.getValueAt(row, Nameidx);
    			if (where==null) {
    				where = grpLike(grpName);
    				sum = grpName;
    			}
    			else {
    				where += " OR " + grpLike(grpName);
    				if (i<7) sum += ", " + grpName;
    			}
    		}
    		if (num>7) sum+= "....(" + num + " total)";  
    		
			String summary;
			if (num==1) summary = getSumLine(sels[0]);
			else 		summary = pGRPs + sum;
			
			String tab = pPAIR + ": ";
			if (num==1) tab += sum;
			else 		tab += pGRP + " " + num;
			
			int row =  (num==1) ? sels[0] : -1;
			
			PairTablePanel pairPanel = new PairTablePanel(theViewerFrame, getInstance(),  where, tab, summary,  row);
			theViewerFrame.addResultPanel(getInstance(), pairPanel, pairPanel.getName(), pairPanel.getSummary());
		
		} catch(Exception e) {ErrorReport.reportError(e, "View Selected Pairs");}
    }
 
    private String grpLike(String grpName) {
    	return grpName.substring(0, grpName.indexOf("_")) + " like '" +  grpName + "'";
    }
    /*******************************************************
     * viewSequences
     */
    private void viewSequences() {
    	try{						
			String [] strVals = getSeqsQueryList(); // query, tab, summary, grpid, hitStr
			if (strVals==null) return;
			
			int row = (theTable.getSelectedRowCount()==1) ? theTable.getSelectedRow() : -1;
			int grpID = Static.getInteger(strVals[3]);
			
			SeqsTablePanel seqPanel = new SeqsTablePanel(theViewerFrame, getInstance(), 
				strVals[0], strVals[1], strVals[2], row, grpID , strVals[4]) ; // CAS305 add [4] hitStr
			
			theViewerFrame.addResultPanel(getInstance(), seqPanel, seqPanel.getName(), seqPanel.getSummary());
		} catch(Exception e) {ErrorReport.reportError(e, "View Selected Sequences");
		} catch(Error e) {ErrorReport.reportFatalError(e, "View Selected Sequences", theViewerFrame);}
    }
    private String [] getSeqsQueryList() {
    	int [] sels = theTable.getSelectedRows();
		if (sels.length==0) return null;
		
    		// return the same numbers for IDidx and Nameidx everytime, e.g. 26 and 1
		int IDidx = theTableData.getColumnHeaderIndex(GRP_SQLID);
		int Nameidx = theTableData.getColumnHeaderIndex(CLUSTERID);
		int Hitidx = theTableData.getColumnHeaderIndex(HITID);
		
		String sourceTable = "pog_members.PGid";
		String tab = "";
		String summary = "";
		String subquery = "";
		String grpID = "";
		String hitID = null;
		
		if(sels.length == 1) {
			String name = (String)theTableData.getValueAt(sels[0], Nameidx);
			tab = pSEQ + ": " + name;
			
			summary = getSumLine(sels[0]);
			subquery = sourceTable + " = " + ((Integer)theTableData.getValueAt(sels[0], IDidx));
			grpID = ((Integer)theTableData.getValueAt(sels[0], IDidx)) + "";
			hitID = (String)  theTableData.getValueAt(sels[0], Hitidx); // CAS305
		}
		else if(sels.length == 2) {
			String  name = 	(String)theTableData.getValueAt(sels[0], Nameidx) + ", " + (String)theTableData.getValueAt(sels[1], Nameidx);							
			
			tab = pSEQ + ": " + pGRP + " "  + sels.length;
			summary = "Clusters " + name;
			
			subquery = sourceTable + " = " + ((Integer)theTableData.getValueAt(sels[0], IDidx));
			subquery += " OR " + sourceTable + " = " + ((Integer)theTableData.getValueAt(sels[1], IDidx));
		}
		else {
			summary = "Clusters " + (String)theTableData.getValueAt(sels[0], Nameidx);
			subquery = sourceTable + " IN (" + ((Integer)theTableData.getValueAt(sels[0], IDidx));
			for(int x=1; x<sels.length; x++) {
				if (x<7) summary += ", " +(String)theTableData.getValueAt(sels[x], Nameidx);
				subquery += ", " + ((Integer)theTableData.getValueAt(sels[x], IDidx));
			}
			subquery += ")";
			
			tab = pSEQ + ": " + pGRP + " " + sels.length ;
			if (sels.length>7) summary += ",...(" + sels.length + " total)";
		}
				
		String [] retVal = new String[5];
		retVal[0] = subquery;
		retVal[1] = tab;
		retVal[2] = summary;
		retVal[3] = grpID;
		retVal[4] = hitID; // CAS305

		return retVal;
    }
    
    static public String makeSQLfromGRPid(Integer [] grpids) {
		String sourceTable = "pog_groups.PGid";
		String subquery = "";

		if (grpids.length==0) {
			subquery = sourceTable + " = 'xxx'"; // will return no clusters
		}
		else if(grpids.length == 1) {
			subquery = sourceTable + " = " + grpids[0];
		}
		else if(grpids.length == 2) {
			subquery = sourceTable + " = " + grpids[0];
			subquery += " OR " + sourceTable + " = " + grpids[1];
		}
		else {
			subquery = sourceTable + " IN (" + grpids[0];
			for(int x=1; x<grpids.length; x++) {
				subquery += ", " + grpids[x];
			}
			subquery += ")";
		}
		return subquery;    	
	}
    /***************** next/prev *********************************************/
    private void getNextRow(int rowNum) {
    	String [] strVals = null;
    	if (thePairTable!=null) {
    		strVals = thePairTable.getNextPairRowForGrp(rowNum); // pog_groups.PGid = 78 OR pog_groups.PGid = 292
    	}
    	else if (theSeqTable!=null) {
    		strVals = theSeqTable.getNextSeqRowForGrp(rowNum); //pog_groups.PGid = 212 OR pog_groups.PGid = 427
    	}
    	else Out.die("TCW error on next row for clusters");
    	
    	strSubQuery= 		strVals[0];
		tabName = 			strVals[1];
		strQuerySummary = 	strVals[2];
		nParentRow = 		Integer.parseInt(strVals[3]);
				
		buildQueryThread();
		
		theViewerFrame.changePanelName(this, tabName, strQuerySummary);
    }
   
 // Called from Pair table
    public String [] getNextGrpRowForPair(int nextRow) {
    	int row = getTranslatedRow(nextRow);
		int Nameidx = theTableData.getColumnHeaderIndex(CLUSTERID);
		
		String grpName = (String)theTableData.getValueAt(row, Nameidx);
		
		String [] retVal = new String[4];
		
		retVal[0] = grpLike(grpName);  		// where
		retVal[1] = pPAIR + ": " + grpName; // tab
		retVal[2] = getSumLine(row);		// summary
		retVal[3] = row+"";					// nParentRow
	
		return retVal;
    }
    // Called from SeqTopRow panel
    public String [] getSeqNextGrpRow(int nextRow) {
    	int row = getTranslatedRow(nextRow);
		int nameIdx = theTableData.getColumnHeaderIndex(CLUSTERID);
		String name = (String)theTableData.getValueAt(row, nameIdx);
		
		int IDidx = theTableData.getColumnHeaderIndex(GRP_SQLID);
		int grpID = ((Integer)theTableData.getValueAt(row, IDidx)); // CAS340 null pointer
		
		String [] retVal = new String[5];
		retVal[0] = "pog_members.PGid = " + grpID;	// where
		retVal[1] = pSEQ + ": " + name;				// tab
		retVal[2] = getSumLine(row);				// summary
		retVal[3] = row +"";						// nParentRow
		retVal[4] = grpID +"";						// grpID
		return retVal;
    }
    private int getTranslatedRow(int row) {
		if (theTable==null) return 0;

    	if (row >= theTable.getRowCount()) 	return 0; // CAS340 add '>' 
    	else if (row < 0) 					return theTable.getRowCount()-1; // last row
    	else								return row;
    }
    private String getSummary(int row) { // CAS313 shared between Next and getting Cluster from table
    	int clusIDX = theTableData.getColumnHeaderIndex(CLUSTERID);
		String name = (String)theTableData.getValueAt(row, clusIDX);
		
		int descIDX = theTableData.getColumnHeaderIndex(HITDESC);
		String desc = (String)theTableData.getValueAt(row, descIDX);
		if (desc==null) desc = Globals.uniqueDesc;
		
		String scores="";
		if (hasMSA) {	// CAS312
			int score1 = theTableData.getColumnHeaderIndex(SCORE1);
			float s1 = (Float)theTableData.getValueAt(row, score1);
			
			int score2 = theTableData.getColumnHeaderIndex(SCORE2);
			float s2 = (Float)theTableData.getValueAt(row, score2);
			scores = String.format("   (%.3f, %.3f)", s1, s2); 
		}
		return name + "   " + desc + scores;
    }
    private String getSumLine(int row) {
		return  "Row " + (row+1) + "/" + theTable.getRowCount() + "   " + pGRP + " " + getSummary(row);	// summary
	}
    /*******************************************
     * for AlignButtons MSA... and MSAdb; 
     */
    public String [] getSelectedSeqIDs() { 
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
    	
		int grpidx = theTableData.getColumnHeaderIndex(GRP_SQLID);
    	int grpid = (Integer) theTableData.getValueAt(row, grpidx);
    	
		int cntidx = theTableData.getColumnHeaderIndex(CLUSTERCOUNT); 
		int cnt = (Integer) theTableData.getValueAt(row, cntidx);
		String [] seqIDs = new String [cnt];
		try {
			DBConn mDB = theViewerFrame.getDBConnection();
			ResultSet rs = mDB.executeQuery("select UTstr from pog_members where PGid=" + grpid);
			int i=0;
			while (rs.next())  seqIDs[i++] = rs.getString(1);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting cluster seqIDs");}
		
		return seqIDs;
    }
    public String [] getTabSum() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
		
    	int IDidx = theTableData.getColumnHeaderIndex(CLUSTERID);
    	
    	String [] retVal = new String[3];
    	
    	retVal[0] = ((String)theTableData.getValueAt(row, IDidx));
    	retVal[1] = getSumLine(row);
    	retVal[2] = row+"";
    	return retVal;
    }
    public int getGrpSQLID() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
		int grpid = theTableData.getColumnHeaderIndex(GRP_SQLID);
    	return (Integer) theTableData.getValueAt(row, grpid);
    }
    public boolean selectedHasMSA() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
    	int IDidx = theTableData.getColumnHeaderIndex(SCORE1);
    	float score = ((float)theTableData.getValueAt(row, IDidx));
    	if (score<0) return false;
    	return true;
    }
    /***************************************************************/
    private SortTable theTable = null;
    private TableData theTableData = null;
    private JScrollPane sPane = null;
    
    private JTextField tableHeader = null;
    private JTextField tableType = null;
    private JTextField loadProgress = null;
    private JTextField txtStatus = null;
   
    //Function buttons
    private JButton btnTable = null, btnCopy=null, btnMSA=null, btnMSAdb=null;
    private JButton btnHelp = null;
    private JButton btnNextRow = null, btnPrevRow = null;
    
    private JButton btnShowSeqs = null;
    private JButton btnShowPairs = null;
    
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
	private ViewerSettings vSettings = null;
	private int totalGrp=0;
	private boolean isList=false;
	private String [] asmNames;
	private boolean hasGOs=false, hasAAdb=false, hasCounts=false, hasMSA=false;
	
	private SeqsTablePanel theSeqTable = null;
	private PairTablePanel thePairTable = null;
	private int nParentRow = -1;
}
