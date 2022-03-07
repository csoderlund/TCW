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

	// from filter or '>Clusters'
	public GrpTablePanel(MTCWFrame parentFrame, String resultName) {
		if (resultName.startsWith(MTCWFrame.MENU_PREFIX)) isSample=true;
		initData(parentFrame, resultName);
		
		GrpQueryPanel theQueryPanel = theViewerFrame.getGrpQueryPanel();
		if (theQueryPanel!=null) {
			strSubQuery =   theQueryPanel.getSubQuery();
			strSummary = 	Globals.trimSum(theQueryPanel.getQuerySummary());
		}
		else {
			loadShortList(); // first time
		}
		
		strSummary = Globals.FILTER +strSummary;
				
		buildQueryThread(); 
	}
	// From Seq Table
	public GrpTablePanel(MTCWFrame vFrame, SeqsTablePanel tPanel, String sql, String tab, String sum,  int row) {
		
		theSeqTable = tPanel;
		nParentRow = row;
		
		initData(vFrame, tab);
		
		strSummary = Globals.trimSum(sum);
		strSubQuery = sql;
				
		buildQueryThread(); 
	}
	// From Pairs table
	public GrpTablePanel(MTCWFrame vFrame, PairTablePanel tPanel, String sql, String tab, String sum,  int row) {
		
		thePairTable = tPanel;
		nParentRow = row;
		
		initData(vFrame, tab);
		
		strSummary = Globals.trimSum(sum);
		strSubQuery = sql;
				
		buildQueryThread(); 
	}
	
	/**********************************
	 * Buttons for Cluster table 
	 *********************************8*/
    private JPanel createTableButton() {
    	JPanel topRow = Static.createRowPanel();
    	JPanel buttonPanel = Static.createPagePanel();
    	topRow.add(Static.createLabel(Globals.select));	topRow.add(Box.createHorizontalStrut(1));
    	
	   	btnShowSeqs = Static.createButtonTab(Globals.SEQ_TABLE, false);
    	btnShowSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewSeqs();	
			} 
		});
    	topRow.add(btnShowSeqs);				topRow.add(Box.createHorizontalStrut(1));
    	
    	btnShowPairs = Static.createButtonTab(Globals.PAIR_TABLE, false);
    	btnShowPairs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewPairs();
			}
		});
        topRow.add(btnShowPairs);				topRow.add(Box.createHorizontalStrut(1));    
        
        AlignButtons msaObj = new AlignButtons(theViewerFrame, getInstance()); // CAS340 new
        btnMSA = msaObj.createBtnMultiAlign();
        topRow.add(btnMSA);						topRow.add(Box.createHorizontalStrut(1)); 
        
        AlignButtons msadbObj = new AlignButtons(theViewerFrame, getInstance()); // CAS340 new
        btnMSAdb = msadbObj.createBtnMultiDB();
        if (hasMSA) {
        	topRow.add(btnMSAdb);				topRow.add(Box.createHorizontalStrut(1)); 
        }  
        
        createBtnCopy();
 		topRow.add(btnCopy);			topRow.add(Box.createHorizontalStrut(nParentRow>=0 ? 5 : 30));
 		
        createBtnTable();
        topRow.add(btnTable);					topRow.add(Box.createHorizontalGlue());
        
        btnHelp = Static.createButtonHelp("Help", true);
        btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Cluster table", helpHTML);
			}
		});
        topRow.add(btnHelp);					topRow.add(Box.createHorizontalStrut(1)); 
        
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
  	 	   
  	 	   topRow.add(btnPrevRow); 
  	 	   topRow.add(btnNextRow);
  	    }
        topRow.setAlignmentX(LEFT_ALIGNMENT);

        buttonPanel.add(topRow);
       
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
   			   new TableUtil().statsPopUp("Clusters: " + strSummary, theTable);
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
	  				new TableUtil(theViewerFrame).exportGrpGO(btnTable,theTableData, strSummary);
	  			}
	  		}));
  		}
  		if (hasCounts) { // CAS305
  			popup.addSeparator();
	  		popup.add(new JMenuItem(new AbstractAction("Export cluster counts (" + Globalx.TSV_SUFFIX + ")...") { 
	  			private static final long serialVersionUID = 4692812516440639008L;
	  			public void actionPerformed(ActionEvent e) {
	  				new TableUtil(theViewerFrame).exportGrpCounts(btnTable,theTableData, strSummary, true);
	  			}
	  		}));
	  		popup.add(new JMenuItem(new AbstractAction("Export cluster TPM (" + Globalx.TSV_SUFFIX + ")...") { 
	  			private static final long serialVersionUID = 4692812516440639008L;
	  			public void actionPerformed(ActionEvent e) {
	  				new TableUtil(theViewerFrame).exportGrpCounts(btnTable,theTableData, strSummary, false);
	  			}
	  		}));
  		}
  		btnTable = Static.createButtonTable("Table...", true);
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
        
        if(!isSample) {
            theViewerFrame.changePanelName(this, tabName + ": " + nRow, strSummary);
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
   
    	JLabel lblSummary = Static.createLabel(strSummary, true);
    	lblSummary.setFont(getFont());
    	thePanel.add(lblSummary);
    	
    	return thePanel;
	}
	
	public String getSummary() { return strSummary; }
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
	private void loadShortList() {
		try {
			DBConn mDB = theViewerFrame.getDBConnection();
			DBinfo info = theViewerFrame.getInfo();
			String x = info.getSampleGrp(mDB);
			mDB.close();
			
			if (x==null || x=="") return;
			String [] y = x.split(":");
			strSubQuery = y[1];
			strSummary = y[0];
		} catch(Exception e) {ErrorReport.reportError(e, "Error processing query"); }
	}
	/**********************************************************
	 * XXX Columns
	 */
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
    
    /**************************************************
     * Table
     */
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
    	add(Box.createVerticalStrut(2));
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
     * XXX: viewPairs
     * where pairwise.CL like CL_xxxx 
     * no DB call
     */
    private void viewPairs() {
    	try{	
    		int [] sels = theTable.getSelectedRows();
    		if (sels.length==0) return;
    		int numSel=sels.length;
    		int grpIdx = theTableData.getColumnHeaderIndex(CLUSTERID);
    		
    		String sum="", where=null;
    		for (int i=0; i<numSel; i++) {
    			int row = sels[i];
			
    			String grpName = (String)theTableData.getValueAt(row, grpIdx);
    			if (where==null) {
    				where = grpLike(grpName);
    				sum = grpName;
    			}
    			else {
    				where += " OR " + grpLike(grpName);
    				if (i<7) sum += ", " + grpName;
    			}
    		}
    		if (numSel>7) sum+= "....(" + numSel + " total)";  
    		
			String summary;
			if (numSel==1) summary = getSumLine(sels[0]);
			else 		   summary = Globals.tagGRP + sum;
			
			String tab;
			if (numSel==1) tab = Globals.tagPAIR + sum;
			else 		   tab = Globals.tagPAIR + "Clus " + numSel;
			
			int row =  (numSel==1) ? sels[0] : -1;
			
			PairTablePanel pairPanel = new PairTablePanel(theViewerFrame, getInstance(),  where, tab, summary,  row);
			theViewerFrame.addResultPanel(getInstance(), pairPanel, pairPanel.getName(), pairPanel.getSummary());
		
		} catch(Exception e) {ErrorReport.reportError(e, "View Selected Pairs");}
    }
   
 // Called from Pair table
    public String [] getNextGrpRowForPair(int nextRow) {
    	int row = getTranslatedRow(nextRow);
		int Nameidx = theTableData.getColumnHeaderIndex(CLUSTERID);
		
		String grpName = (String)theTableData.getValueAt(row, Nameidx);
		
		String [] retVal = new String[4];
		
		retVal[0] = grpLike(grpName);  		// where
		retVal[1] = Globals.tagPAIR + grpName; // tab
		retVal[2] = getSumLine(row);		// summary
		retVal[3] = row+"";					// nParentRow
	
		return retVal;
    }
   
    private String grpLike(String grpName) { // OM_xxx the OM is the column name
    	return "pairwise." + grpName.substring(0, grpName.indexOf("_")) + " like '" +  grpName + "'";
    }
    /*******************************************************
     * viewSeqs
     * where pog_members.PGid =  + grpID;	
     * no DB call
     */
    private void viewSeqs() {
    	try{						
    		int [] sels = theTable.getSelectedRows();
    		if (sels.length==0) return;
			
			int IDidx = 	theTableData.getColumnHeaderIndex(GRP_SQLID);
			int Nameidx = 	theTableData.getColumnHeaderIndex(CLUSTERID);
			
			String sourceTable = "pog_members.PGid";
			String query="", tab = "", summary="";
			
			if(sels.length == 1) {
				String name = (String)theTableData.getValueAt(sels[0], Nameidx);
				tab = Globals.tagSEQ + name;
				
				summary = getSumLine(sels[0]);
				query = sourceTable + " = " + ((Integer)theTableData.getValueAt(sels[0], IDidx));
			}
			else if(sels.length == 2) {
				String  name = 	(String)theTableData.getValueAt(sels[0], Nameidx) + ", " + (String)theTableData.getValueAt(sels[1], Nameidx);							
				
				tab = Globals.tagSEQ + "Clus " + sels.length;
				summary = Globals.tagGRPs + name;
				
				query = sourceTable + " = " + ((Integer)theTableData.getValueAt(sels[0], IDidx));
				query += " OR " + sourceTable + " = " + ((Integer)theTableData.getValueAt(sels[1], IDidx));
			}
			else {
				summary = Globals.tagGRPs  + (String)theTableData.getValueAt(sels[0], Nameidx);
				query = sourceTable + " IN (" + ((Integer)theTableData.getValueAt(sels[0], IDidx));
				for(int x=1; x<sels.length; x++) {
					if (x<7) summary += ", " +(String)theTableData.getValueAt(sels[x], Nameidx);
					query += ", " + ((Integer)theTableData.getValueAt(sels[x], IDidx));
				}
				query += ")";
				
				tab = Globals.tagSEQ +  "Clus " + sels.length ;
				if (sels.length>7) summary += ",...(" + sels.length + " total)";
			}
			
			int row = (theTable.getSelectedRowCount()==1) ? theTable.getSelectedRow() : -1;
			
			SeqsTablePanel seqPanel = new SeqsTablePanel(theViewerFrame, getInstance(), query, tab, summary, row) ; 
			
			theViewerFrame.addResultPanel(getInstance(), seqPanel, seqPanel.getName(), seqPanel.getSummary());
		} catch(Exception e) {ErrorReport.reportError(e, "View Selected Sequences");
		} catch(Error e) {ErrorReport.reportFatalError(e, "View Selected Sequences", theViewerFrame);}
    }
   
    // Called from SeqTopRow panel
    public String [] getNextSeqRowForGrp(int nextRow) {
    	int row = getTranslatedRow(nextRow);
		int nameIdx = theTableData.getColumnHeaderIndex(CLUSTERID);
		String name = (String)theTableData.getValueAt(row, nameIdx);
		
		int IDidx = theTableData.getColumnHeaderIndex(GRP_SQLID);
		int grpID = ((Integer)theTableData.getValueAt(row, IDidx)); // CAS340 null pointer
		
		String [] retVal = new String[5];
		retVal[0] = "pog_members.PGid = " + grpID;	// where
		retVal[1] = Globals.tagSEQ + name;			// tab
		retVal[2] = getSumLine(row);				// summary
		retVal[3] = row +"";						// nParentRow
		retVal[4] = grpID +"";						// grpID
		return retVal;
    }
    /*******************************************
     * for AlignButtons MSA... and MSAdb; 
     */
    public String [] getAlignSeqIDs() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
    	
    	return loadAlignSeqIDs(row);
    }
    public String [] getAlignSeqIDs(int row) {
    	return loadAlignSeqIDs(row);
    }
    private String [] loadAlignSeqIDs(int row) { 
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
    	
    	retVal[0] = ((String)theTableData.getValueAt(row, IDidx));  // tab
    	retVal[1] = getSumLine(row);								// summary
    	retVal[2] = row+"";							
    	return retVal;
    }
    public int getGrpSQLID() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
		int grpid = theTableData.getColumnHeaderIndex(GRP_SQLID);
    	return (Integer) theTableData.getValueAt(row, grpid);
    }
    public String getHitStr() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
		int hitid = theTableData.getColumnHeaderIndex(HITID);
    	return (String) theTableData.getValueAt(row, hitid);
    }
    public boolean selectedHasMSA() {
    	int [] sels = theTable.getSelectedRows();
		int row = (sels.length>0) ? sels[0] : 0;
    	int IDidx = theTableData.getColumnHeaderIndex(SCORE1);
    	float score = ((float)theTableData.getValueAt(row, IDidx));
    	if (score<0) return false;
    	return true;
    }
    /* MSA... and MSAdb Next/Prev */
    public String [] getNextGrpForMSA(int nextRow) {
    	int row = getTranslatedRow(nextRow);
		int grpIdx = theTableData.getColumnHeaderIndex(CLUSTERID);
		String grpName = (String)theTableData.getValueAt(row, grpIdx);
		
		int IDidx = theTableData.getColumnHeaderIndex(GRP_SQLID);
		int grpID = ((Integer)theTableData.getValueAt(row, IDidx)); // CAS340 null pointer
		
		String [] retVal = new String[5];
		retVal[0] = Globals.tagMxx + grpName;		// tab
		retVal[1] = getSumLine(row);				// summary
		retVal[2] = row +"";						// nParentRow
		retVal[3] = grpID +"";						
		retVal[4] = getHitStr();	
		return retVal;
    }
    /**************************************************************/
    private int getTranslatedRow(int row) {
		if (theTable==null) return 0;

    	if (row >= theTable.getRowCount()) 	return 0; // CAS340 add '>' 
    	else if (row < 0) 					return theTable.getRowCount()-1; // last row
    	else								return row;
    }
   
    private String getSumLine(int row) {
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
		String sum = "";
		if (theTable.getRowCount()>1) sum = "Row " + (row+1) + "/" + theTable.getRowCount() + "   ";
		sum +=  Globals.tagGRP + name + "   " + desc + scores;
    	
		return  sum;
	}
    /**************************************************
     * Cluster calls Next/Prev for next sequence or pair row
     * The query is made with the static makeSQLfrom GRPid
     ***************** next/prev *********************************************/
    private void getNextRow(int rowNum) {
    	String [] strVals = null;
    	if (thePairTable!=null) 	strVals = thePairTable.getNextPairRowForGrp(rowNum); 
    	else if (theSeqTable!=null) strVals = theSeqTable.getNextSeqRowForGrp(rowNum);  
    	else Out.die("TCW error on next row for clusters");
    	
    	strSubQuery= 		strVals[0];  // pog_groups.PGid = N 
		tabName = 			strVals[1];
		strSummary = 		Globals.trimSum(strVals[2]);
		nParentRow = 		Integer.parseInt(strVals[3]);
				
		buildQueryThread();
		
		theViewerFrame.changePanelName(this, tabName, strSummary);
    }
    static public String makeSQLfromGRPid(Integer [] grpids) {
		String sourceTable = TABLE + "." +  GRP_SQLID; // "pog_groups.PGid";
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
   
    /***************************************************************/
    private SortTable theTable = null;
    private TableData theTableData = null;
    private JScrollPane sPane = null;
    
    private JPanel tableButtonPanel = null, tableStatusPanel = null, fieldSelectPanel = null, tableSummaryPanel = null;
	
    private JTextField tableHeader = null, tableType = null, loadProgress = null, txtStatus = null;
    
    private JButton btnTable = null, btnCopy=null, btnMSA=null, btnMSAdb=null;
    private JButton btnNextRow = null, btnPrevRow = null, btnHelp = null;
    private JButton btnShowSeqs = null, btnShowPairs = null;
	private JButton showColumnSelect = null, clearColumn = null;
	
    private JCheckBox [] chkFields = null;
    
    private ActionListener dblClick = null, sngClick = null, colSelectChange = null;
    private ListSelectionListener selListener = null;
    
    private Thread buildThread = null;
    
	private MTCWFrame theViewerFrame = null;
	private ViewerSettings vSettings = null;
	private SeqsTablePanel theSeqTable = null;
	private PairTablePanel thePairTable = null;
	
	private String strSubQuery = "", strSummary = null, tabName = "";
	private String [] asmNames;
	private boolean hasGOs=false, hasAAdb=false, hasCounts=false, hasMSA=false, isSample=false;
	private int nParentRow = -1, totalGrp=0;
}
