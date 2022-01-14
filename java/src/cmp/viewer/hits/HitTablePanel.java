 package cmp.viewer.hits;

 /**********************************************8
  * Hit table - added CAS310 mdb62
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

import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.ViewerSettings;
import cmp.viewer.seq.SeqsTablePanel;
import cmp.viewer.table.FieldData;
import cmp.viewer.table.SortTable;
import cmp.viewer.table.TableData;
import cmp.viewer.table.TableUtil;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

public class HitTablePanel  extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private static final String TABLE = FieldData.HIT_TABLE;
	private static final String HITID = FieldData.HITID;
	private static final String HITDESC = FieldData.HITDESC;
	private static final String HIT_SQLID = FieldData.HIT_SQLID; // Hidden column containing DB HITid
	private static final String helpHTML = Globals.helpDir + "HitTable.html";

	private final String pSEQ = MTCWFrame.SEQ_PREFIX;
	private final String pHIT = MTCWFrame.HIT_PREFIX;
	private final String pHITs = MTCWFrame.HIT_PREFIX + "s";
	
	public HitTablePanel(MTCWFrame parentFrame, String tab) {
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		
		initData(parentFrame, tab);	
		
		HitQueryPanel theQueryPanel = theViewerFrame.getHitQueryPanel();
		if (theQueryPanel!=null) {
			strSubQuery =     theQueryPanel.getSQLwhere();
			strQuerySummary = theQueryPanel.getQuerySummary();
		}
		else buildShortList();
		
		strQuerySummary = MTCWFrame.FILTER + strQuerySummary;
		buildQueryThread(); 
	}
	
	/** XXX Buttons for Hit table */
    private JPanel createTableButton() {
    	JPanel buttonPanel = Static.createPagePanel();
	    	
    	JPanel topRow = Static.createRowPanel();
	    topRow.add(Static.createLabel(Globals.select));
    	   	
         btnTableSeqs = Static.createButtonTab(MTCWFrame.SEQ_TABLE, false);
         btnTableSeqs.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent arg0) {
 				viewSequences();
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
        
        btnHelp = Static.createButtonHelp("Help", true);
        btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Hit table",  helpHTML);
			}
		});
        topRow.add(btnHelp);

        topRow.setAlignmentX(LEFT_ALIGNMENT);

        buttonPanel.add(topRow);
        buttonPanel.add(Box.createVerticalStrut(5));
        
        return buttonPanel;
    }
    
    private void createBtnCopy() {
    	btnCopy = Static.createButtonMenu("Copy...", false);
 	    final JPopupMenu copyPop = new JPopupMenu();
 	    copyPop.setBackground(Color.WHITE); 
 	    
  		copyPop.add(new JMenuItem(new AbstractAction("HitID") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
  				int row = theTable.getSelectedRow();
  				int idx = theTableData.getColumnHeaderIndex(HITID);
  				String hitID =  ((String)theTableData.getValueAt(row, idx));
  				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
  				cb.setContents(new StringSelection(hitID), null);
  			}
  		}));
  		copyPop.add(new JMenuItem(new AbstractAction("Hit Description") {
  			private static final long serialVersionUID = 1L;
  			public void actionPerformed(ActionEvent e) {
  				int row = theTable.getSelectedRow();
  				int idx = theTableData.getColumnHeaderIndex(HITDESC);
  				String desc =  ((String)theTableData.getValueAt(row, idx));
  				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
  				cb.setContents(new StringSelection(desc), null);
  			}
  		}));
  		copyPop.add(new JMenuItem(new AbstractAction("Hit Sequence") {
  			private static final long serialVersionUID = 1L;
  			public void actionPerformed(ActionEvent e) {
  				int row = theTable.getSelectedRow();
  				int idx = theTableData.getColumnHeaderIndex(HITID);
  				String hitID =  ((String)theTableData.getValueAt(row, idx));
  				String seq = loadSeq(hitID);
  				seq = ">" + hitID + "\n" + seq;
  				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
  				cb.setContents(new StringSelection(seq), null);
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
 			   new TableUtil().statsPopUp("Hits: " + strQuerySummary, theTable);
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
				} catch (Exception ex) {ErrorReport.reportError(ex, "Error generating list");}
 			}
 		}));
        popup.addSeparator();
 		popup.add(new JMenuItem(new AbstractAction("Export table (" + Globalx.CSV_SUFFIX + ")") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportTableTab(btnTable, theTable, Globals.bHIT);
 			}
 		}));
 		
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
		hasGOs =    (theViewerFrame.getInfo().getCntGO()>0);
		totalHits = theViewerFrame.getInfo().getCntHit();

		colSelectChange = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateTable(false);
				displayTable();
				vSettings.getHitSettings().setSelectedColumns(getSelectedColumns());
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
        FieldData theFields = FieldData.getHitFields();
    
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
        String status =  nRow + " of " + totalHits + " " + Static.perText(nRow, totalHits);
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
    	
    	String [] sections = 	FieldData.getHitColumnSections();
    	int [] secIdx = 		FieldData.getHitColumnSectionIdx();
    	int [] secBreak = 		FieldData.getHitColumnSectionBreak();
    	String [] columns = 	FieldData.getHitColumns();
    	String [] descriptions =FieldData.getHitDescript();
    	boolean [] defaults = 	FieldData.getHitSelections();
	    boolean [] selections = getColumnSelections(columns, defaults, 
    					theViewerFrame.getSettings().getHitSettings().getSelectedColumns());    
	    	
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
    
    	lblSummary = Static.createLabel(strQuerySummary, true);
    	lblSummary.setFont(getFont());
    	thePanel.add(lblSummary);
    	
    	return thePanel;
	}
	
	public String getSummary() { return strQuerySummary; }
	private HitTablePanel getInstance() { return this; }
	
	private String buildQueryStr(DBConn mdb) {
        try {
    		FieldData theFields = FieldData.getHitFields();
        	
    		String strQuery = "SELECT " + theFields.getDBFieldQueryList() + " FROM " + TABLE + " ";
        	
    		strQuery += " WHERE " + strSubQuery;
    	
    		int cnt = mdb.executeCount("select count(*) from " + TABLE + " where " + strSubQuery);
    		String per = Static.perText(cnt, theViewerFrame.getInfo().getCntHit());
        	loadStatus.setText("Getting " + cnt + " " + per + " filtered Hits from database");
    	
    		return strQuery;
        } catch(Exception e) {ErrorReport.reportError(e, "Error processing query");return null;}
	}
	private void buildShortList() {
		try {
			DBConn mDB = theViewerFrame.getDBConnection(); // 
			DBinfo info = theViewerFrame.getInfo();
			String x = info.getSampleHit(mDB);
			mDB.close();
			
			if (x==null || x=="") return;
			String [] y = x.split(":");
			strSubQuery = y[1];
			strQuerySummary = y[0];
		 } catch(Exception e) {ErrorReport.reportError(e, "Getting hit sample");}
	}
	private void clearColumns() { 
		for(int x=0; x<chkFields.length; x++) {
			chkFields[x].setSelected(false);
		} 
		chkFields[4].setSelected(true);  
	}
    private String [] getSelectedColumns() {
    	String [] retVal = null;
    	
    	int selectedCount = 0;
    	for(int x=0; x<chkFields.length; x++) 
    		if(chkFields[x].isSelected())
    			selectedCount++;
    	
    	if(selectedCount == 0) {
			for(int x=0; x<FieldData.HIT_DEFAULTS.length; x++) {
				chkFields[x].setSelected(FieldData.HIT_DEFAULTS[x]);
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
    private boolean [] getColumnSelections(String [] col, boolean [] def, String [] selections) {

    	if(selections == null || selections.length == 0) return def;
    	
    	Vector<String> sels = new Vector<String> ();
    	boolean [] retVal = new boolean[col.length];
    	for(int x=0; x<selections.length; x++) sels.add(selections[x]);
    	for(int x=0; x<col.length; x++) retVal[x] = sels.contains(col[x]);
 
    	return retVal;
    }
   
    /***********************************************************************
     * Sequences for selected HITs -- query database for UTid of unitrans_hits for HITid of selected
     ***********************************************************/
    private void viewSequences() {
    	try {						
			int [] sels = theTable.getSelectedRows();
			if (sels.length==0) return;
			
		// Get hitIDs
			int hitIdx =  theTableData.getColumnHeaderIndex(HIT_SQLID);
			int hitNameIdx = theTableData.getColumnHeaderIndex(HITID);
			
			String hitlist = "", sum="", tab="";
			int id1 = (Integer)theTableData.getValueAt(sels[0], hitIdx);
			
			if (sels.length==1) {
				tab = pSEQ  + ":  " + (String)theTableData.getValueAt(sels[0], hitNameIdx);
				hitlist = "unitrans_hits.HITid = " + id1; // CAS340 add unitrans
				sum = getSumLine(sels[0]);
			}
			else {
				tab = pSEQ  + ":  " + pHITs + sels.length; // tab on left
				sum= pHIT + ": HitID=" + (String)theTableData.getValueAt(sels[0], hitNameIdx);
				hitlist = "unitrans_hits.HITid in (" + id1;
				
				for (int i=1; i<sels.length; i++) {
					int id2 = (Integer)theTableData.getValueAt(sels[i], hitIdx);
					hitlist += "," + id2;
					
					if (i<8) sum += ", " + (String)theTableData.getValueAt(sels[i], hitNameIdx);
				}
				hitlist += ")";
				if (sels.length>=8) sum += "...";
			}
			String list="";
			
		// Get SEQids for hitids
			String where = getSeqList(hitlist);
 	    	
			int row = (sels.length == 1) ? row = theTable.getSelectedRow() : -1;
			
			SeqsTablePanel newPanel = new SeqsTablePanel(theViewerFrame, getInstance(), 
					where, tab, sum, row);
			theViewerFrame.addResultPanel(getInstance(), newPanel, newPanel.getName(), sum);
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "View HIT Sequences");}
    }
    private String getSeqList(String hitlist) { // CAS340 shared with getNextHitRowForSeq
    	String seqlist="";
    	try {
    		DBConn mDB = theViewerFrame.getDBConnection();
			ResultSet rs = mDB.executeQuery("select distinct UTid from unitrans_hits where " + hitlist);
			while (rs.next()) {
				int id = rs.getInt(1);
				seqlist += (seqlist=="") ? id : ("," + id);
			}
			return " UTid in (" + seqlist + ")";
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "Get SeqIDs for " + hitlist); return "1";}
    }
    private String loadSeq(String hitStr) {
    	try {
    		DBConn mDB = theViewerFrame.getDBConnection();
			ResultSet rs = mDB.executeQuery("select sequence from unique_hits where HITstr='" + hitStr + "'");
			if (rs.next()) return rs.getString(1);
			else return "Error reading sequence";
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "Load HIT Sequence " + hitStr);}
    	
    	Out.prt("Error reading sequence for " + hitStr);
    	return "Error reading sequence";
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
    	setBackground(Static.BGCOLOR);
    	loadStatus = new JTextField(100);
    	loadStatus.setBackground(Static.BGCOLOR);
    	loadStatus.setMaximumSize(loadStatus.getPreferredSize());
    	loadStatus.setEditable(false);
    	loadStatus.setBorder(BorderFactory.createEmptyBorder());
    	
    	JButton btnStop = Static.createButton("Stop");
    	btnStop.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			//This not only informs the user that it is canceled, but this is also the signal for the thread to stop. 
			if(buildThread != null)
				loadStatus.setText("Cancelled");
		} });
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
    	temp.add(showColumnSelect);		temp.add(Box.createHorizontalStrut(5));
    	temp.add(clearColumn);			temp.add(Box.createHorizontalStrut(5));
    	temp.add(txtStatus);
    	temp.setMaximumSize(temp.getPreferredSize());
    	add(temp);
    	if(theTable != null) {
    	 	tableType.setText("Hit View");
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
		private static final long serialVersionUID = 1L;
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
    /************* Next/Prev from sequence table ***************/
    // Called from SeqTopRow panel
    public String [] getNextHitRowForSeq(int nextRow) {
    	int row = getTranslatedRow(nextRow);
		int hitIDX = theTableData.getColumnHeaderIndex(HITID);
		String name = (String)theTableData.getValueAt(row, hitIDX);
		
		int dbIDX = theTableData.getColumnHeaderIndex(HIT_SQLID);
		int hitID = ((Integer)theTableData.getValueAt(row, dbIDX)); 
		
		String [] retVal = new String[4];
		retVal[0] = getSeqList("HITid = " + hitID);	// query
		retVal[1] = pSEQ + ": " + name;				// tab
		retVal[2] = getSumLine(row);				// summary
		retVal[3] = row +"";						// nParentRow
		return retVal;
    }
    private String getSumLine(int row) {
    	int descIdx = theTableData.getColumnHeaderIndex(HITDESC);
		String desc =  "  " + (String)theTableData.getValueAt(row, descIdx);
		
		int hitNameIdx = theTableData.getColumnHeaderIndex(HITID);
		String name = (String)theTableData.getValueAt(row, hitNameIdx);
		
		return "Row " + (row+1) + "/" + theTable.getRowCount() + "   " +  pHIT + " " + name + desc;	// summary
	}
    private int getTranslatedRow(int row) {
		if (theTable==null) return 0;

    	if (row >= theTable.getRowCount()) 	return 0; // CAS340 add '>' 
    	else if (row < 0) 					return theTable.getRowCount()-1; // last row
    	else								return row;
    }
    /****************************************************************/
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
    private JButton btnTableSeqs = null;
    
    private ActionListener dblClick = null;
    private ActionListener sngClick = null;
    private ListSelectionListener selListener = null;
    private Thread buildThread = null;
    
    private JCheckBox [] chkFields = null;
    private ActionListener colSelectChange = null;
    
	private JPanel tableButtonPanel = null;
	private JPanel tableStatusPanel = null;
	private JPanel fieldSelectPanel = null;
	private JPanel tableSummaryPanel = null;
	
	private JButton showColumnSelect = null;
	private JButton clearColumn = null;
	
	private ViewerSettings vSettings = null;
	private int totalHits=0;
	private boolean isList=false, hasAAdb=false, hasGOs=false;
	
	String tabName;
	String strSubQuery=null, strQuerySummary=null;
	private MTCWFrame theViewerFrame = null;
}
