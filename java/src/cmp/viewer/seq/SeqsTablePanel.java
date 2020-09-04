package cmp.viewer.seq;
/*****************************************************
 * Sequence table: called from SeqsTopRowPanel to display the table.
 */

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
import util.methods.ErrorReport;
import util.methods.Static;

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.ViewerSettings;
import cmp.viewer.table.FieldData;
import cmp.viewer.table.SortTable;
import cmp.viewer.table.TableData;

public class SeqsTablePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public  static final int MAX_SELECT=20;
	
	private static final int bSEQ=Globals.bSEQ;
	private static final int bPAIR=Globals.bPAIR;
	private static final int bGRP=Globals.bGRP;
	
	private static final String TABLE = FieldData.SEQ_TABLE;
	private static final String AALEN = FieldData.AALEN;
	private static final String NTLEN = FieldData.NTLEN;
	private static final String SEQID = FieldData.SEQID;
	private static final String SEQINDEX = FieldData.SEQ_SQLID;
	
	public SeqsTablePanel(MTCWFrame parentFrame, SeqsTopRowPanel parentList, 
			String tab, String summary, String subQuery, int vType) {
		topRowPanel = parentList;
		viewType = vType;
		totalSeq = parentFrame.getInfo().getCntSeq();
		if (tab.startsWith(MTCWFrame.MENU_PREFIX)) isList=true;
		
		initData(parentFrame, tab, summary, subQuery);
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
					DBConn conn = theViewerFrame.getDBConnection();
					String query = buildQueryStr(conn);
					ResultSet rset = MTCWFrame.executeQuery(conn, query, loadStatus);
					if(rset != null) {
						buildTable(rset);
						MTCWFrame.closeResultSet(rset); //Thread safe way of closing the resultSet
						updateTable(true);
					}
					conn.close();
					
					displayTable();
					topRowPanel.buildPanel();
					
					if(isVisible()) {//Makes the table appear (A little hacky, but fast)
						setVisible(false);
						setVisible(true);
						topRowPanel.setTopEnabled(0, theTable.getRowCount());
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
				int selCount = theTable.getSelectedRowCount();
				if(selCount > MAX_SELECT) {
					txtStatus.setText("Cannot pair align more than " + MAX_SELECT + "  rows at a time");
				}
				else {
					txtStatus.setText("");	
				}
				topRowPanel.setTopEnabled(selCount, theTable.getRowCount());
			}
		};

        theTableData = new TableData(this);
        FieldData theFields = FieldData.getSeqFields(theViewerFrame.getSeqLibList(), 
        		theViewerFrame.getSeqDEList(), theViewerFrame.getMethodPrefixes());
        theTableData.setColumnHeaders(theFields.getDisplayFields(), theFields.getDisplayTypes());
        
        theTableData.addRowsWithProgress(rset, theFields, loadStatus);
        theTableData.showTable();
       
        int nRow = theTableData.getNumRows(); 
        String status =  nRow + " of " + totalSeq + " " + Static.perText(nRow, totalSeq);
        tableHeader.setText(status);
        
        if(!isList) {
        	    nRow = theTableData.getNumRows();
            theViewerFrame.changePanelName(topRowPanel, tabName + ": " + nRow, strQuerySummary);
        }
    }
	
    private JPanel createFieldSelectPanel() {
	    	JPanel retVal = Static.createPagePanel();
	    	
    		String [] lib = theViewerFrame.getSeqLibList();
    		String [] de  = theViewerFrame.getSeqDEList();
    		String [] met = theViewerFrame.getMethodPrefixes();
    		
    		String [] headers = 		FieldData.getSeqColumnSections();
    		int [] headerIdx = 		FieldData.getSeqColumnSectionIdx(lib.length, de.length, met.length); 
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
	        	if(Globals.COLUMN_SELECT_WIDTH - chkFields[x].getPreferredSize().width > 0) {
	        		newWidth = Globals.COLUMN_SELECT_WIDTH;
	        		space = Globals.COLUMN_SELECT_WIDTH - chkFields[x].getPreferredSize().width;
	        	}
	        	else {
	        		space = 0;
	        		newWidth = chkFields[x].getPreferredSize().width;
	        	}
	        	
	        	if(rowWidth + newWidth >= Globals.COLUMN_PANEL_WIDTH || 
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
	
	    	lblSummary = Static.createLabel(strQuerySummary, true);
	    	lblSummary.setFont(getFont());
	    	
	    	JLabel header = Static.createLabel(MTCWFrame.FILTER, true);
	    	thePanel.add(header);
	    	thePanel.add(lblSummary);
	    	
	    	return thePanel;
	}
	private void initData(MTCWFrame parentFrame, String tab, String querySummary, String subQuery) {
		theViewerFrame = parentFrame;
		vSettings = parentFrame.getSettings();
		tabName = tab;
		
		if (querySummary!=null) strQuerySummary = querySummary;
		if (subQuery!=null)     strSubQuery = subQuery;

		colSelectChange = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateTable(false);
				displayTable();
				vSettings.getSeqSettings().setSelectedColumns(getSelectedColumns());
			}
		};
	}
		
	public String getSummary() { return strQuerySummary; }

	private SeqsTablePanel getInstance() { return this; }
	 
    private void validateTable() {validate();} //Called from a thread
    
    public boolean correctType(String type) {
	    	boolean clearSel = false;
	    	if(theTable.getSelectedRowCount() == 0) {
	    		theTable.selectAll();
	    		clearSel = true;
	    	}
    	
     	int [] sels = theTable.getSelectedRows();
     	int colIndex = 0;
     	String msg="";
      	if (type.equals("NT")) {
      		colIndex = theTableData.getColumnHeaderIndex(NTLEN);
      		msg = "nucleotide";
      	}
      	else {
      		colIndex = theTableData.getColumnHeaderIndex(AALEN);
      		msg = "amino acid ";
      	}
      	if (clearSel) msg = "Not all selected have " + msg + " sequences.";
      	else msg = "Not all selected have " + msg + " sequences.";
      	boolean rc=true;
      	
      	// if len=0, no associated NT/AA sequence
      	for(int x=0; x<sels.length; x++) {
  			int len = ((Integer)theTableData.getValueAt(sels[x], colIndex));
  			if (len==0) {
  				JOptionPane.showMessageDialog(theViewerFrame, 
  							msg, "Warning", JOptionPane.PLAIN_MESSAGE);
  				rc = false;
  				break;
  			}
  		} 
      	
     	if(clearSel) theTable.clearSelection();
		return rc;
	}
    /****************** gets *****************************/
   
    public String [] getSelectedSeqIDs() {  
	    	boolean clearSel = false;
	    	if(theTable.getSelectedRowCount() == 0) {
	    		theTable.selectAll();
	    		clearSel = true;
	    	}
	    int colIndex =	theTableData.getColumnHeaderIndex(SEQID);
	    int [] sels = theTable.getSelectedRows();
	 	String [] seqIDs = new String [sels.length];
	 	
	  	for(int x=0; x<sels.length; x++)
	  		seqIDs[x] =  ((String)theTableData.getValueAt(sels[x], colIndex));
	    	
	    	if(clearSel) theTable.clearSelection();
	    	return seqIDs;
    }
 
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
	private String buildQueryStr(DBConn mdb) {
        try {
    		FieldData theFields = FieldData.getSeqFields(theViewerFrame.getSeqLibList(), 
				theViewerFrame.getSeqDEList(), theViewerFrame.getMethodPrefixes());
       
        	String from, strQuery;
        	if (strSubQuery==null || strSubQuery.equals("")) strSubQuery= " 1 ";
        	if (viewType==bGRP) { 
        		from = 	" FROM " + TABLE + " " + theFields.getJoins() + 
        				" LEFT JOIN pog_members ON pog_members.UTid = unitrans.UTid" +
        				" WHERE " + strSubQuery;
        		strQuery = "SELECT " + theFields.getDBFieldQueryList() +  from +
        				" order by unitrans.UTstr"; // CAS303 group  -> order
        	}
        	else {  // join on unitrans.HITid=unique_hits.HITid
        	    from = 	" FROM " + TABLE + " " + theFields.getJoins() + 
        				" WHERE " + strSubQuery;
        	    strQuery = "SELECT " + theFields.getDBFieldQueryList() + from;
        	}
        	int cnt  = mdb.executeCount("select count(*) " + from);
	        String per = Static.perText(cnt, theViewerFrame.getInfo().getCntSeq());
	        loadStatus.setText("Getting " + cnt + " " + per + " filtered sequences from database" );
       	
        	return strQuery;
        } catch(Exception e) {ErrorReport.reportError(e, "Error processing query");return null;}
	}
	/**********************************************************
	 * len[0] is AAlen for pair[0], len[1] is AAlen for pair[1]
	 * len[2] is NTlen for pair[0], len[3] is NTlen for pair[1]
	 */
	public int [] getPairLens() {
		int [] len = new int [4];
		int [] sels = theTable.getSelectedRows();
		if (sels.length==0) {
			sels = new int [2];
			sels[0]=0;
			sels[1]=1;
		}
		
		int idx = theTableData.getColumnHeaderIndex(AALEN);
		len[0]=(Integer)theTableData.getValueAt(sels[0], idx);
		len[1]=(Integer)theTableData.getValueAt(sels[1], idx);
		
		idx = theTableData.getColumnHeaderIndex(NTLEN);
		len[2]=(Integer)theTableData.getValueAt(sels[0], idx);
		len[3]=(Integer)theTableData.getValueAt(sels[1], idx);
		return len;
	}
	public SortTable getTable() { return theTable;}
	public TableData getTableData() { return theTableData;}

	public int getSelectedSQLid() {
     	if(theTable.getSelectedRowCount() == 0) return -1;
     	
     	int [] sels = theTable.getSelectedRows();
     	int idx = theTableData.getColumnHeaderIndex(SEQINDEX);
     	return (Integer)theTableData.getValueAt(sels[0], idx);
    }
	public int [] getSelectedSQLids() {
     	if(theTable.getSelectedRowCount() == 0) return null;
     	
     	int [] sels = theTable.getSelectedRows();
     	int idx = theTableData.getColumnHeaderIndex(SEQINDEX);
     	
     	int [] ids = new int [sels.length];
		for (int i=0; i<sels.length; i++) 
			ids[i] = (Integer)theTableData.getValueAt(sels[i], idx);
		return ids;
    }
	public String [] getSelectedSEQIDs() {
		if(theTable.getSelectedRowCount() == 0) return null;
     	
     	int [] sels = theTable.getSelectedRows();
     	int idx = theTableData.getColumnHeaderIndex(SEQID);
     	String [] list = new String [sels.length];
     	for (int i=0; i<sels.length; i++) 
     		list[i] = (String)theTableData.getValueAt(sels[i], idx);
     	return list;
	}
	public int [] getTableSQLid() {
		int idx = theTableData.getColumnHeaderIndex(SEQINDEX);
		int nRow = theTable.getRowCount();
		int [] ids = new int [nRow];
		for (int i=0; i<nRow; i++) 
			ids[i] = (Integer)theTableData.getValueAt(i, idx);
		return ids;
	}
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
    public String getSelectedAASeq() {
    	if(theTable.getSelectedRowCount() == 0) return null;
     	
     	return loadSelectedSeq(FieldData.AASEQ_SQL);
    }
    private String loadSelectedSeq(String column) {
		if(theTable.getSelectedRowCount() == 0) return null;
		try {
			DBConn mdb = theViewerFrame.getDBConnection();
			int seqSQL = getSelectedSQLid();
			String seq=null;
			ResultSet rs = mdb.executeQuery("select " + column + " from unitrans where UTid=" + seqSQL);
			if (rs.next()) seq=rs.getString(1);
			else System.err.println("Could not get " + column + " for " + seqSQL);
			return seq;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "loading sequence from database ");}
		return null;
    }
    public void clearSelection() {theTable.clearSelection();}
    
    public int getRowCount() { return theTable.getRowCount();}
	public int getSelectedRowCount() {return theTable.getSelectedRowCount();}
	public int getSelectedRow() { return theTable.getSelectedRows()[0];}
	public int [] getSelectedRows() { return theTable.getSelectedRows();}
	public String getGroupQueryList(Integer [] UTids) {
		String sourceTable = "pog_groups.PGid";
		
		String subquery = "";

		if(UTids.length == 1) {
			subquery = sourceTable + " = " + UTids[0];
		}
		else if(UTids.length == 2) {
			subquery = sourceTable + " = " + UTids[0];
			subquery += " OR " + sourceTable + " = " + UTids[1];
		}
		else {
			subquery = sourceTable + " IN (" + UTids[0];
			for(int x=1; x<UTids.length; x++) {
				subquery += ", " + UTids[x];
			}
			subquery += ")";
		}
		return subquery;    	
	}
	 /* SeqTopRowPanel next and prev */
    public int getTranslatedRow(int row) {
  		if (theTable==null) return 0;

  		if(row == theTable.getRowCount()) return 0;
  		if(row < 0) return theTable.getRowCount() - 1;
  		return row;
    }
    public int getRowSeqIndex(int row) {
    		return (Integer)theTableData.getValueAt(row, 
				theTableData.getColumnHeaderIndex(SEQINDEX));
    }
    public String getRowSeqName(int row) {
		return (String)theTableData.getValueAt(row, 
			theTableData.getColumnHeaderIndex(SEQID));
    }
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
    	setBackground(Globals.BGCOLOR);
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
	    	add(Box.createVerticalStrut(5));
	    	add(tableSummaryPanel);
	    	add(Box.createVerticalStrut(5));
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
	    		 if (viewType==bGRP) tableType.setText("Sequence View for Cluster");
	    		 else if (viewType==bPAIR) tableType.setText("Sequence View for Pair");
	    		 else if (viewType==bSEQ) tableType.setText("Sequence View");
	    		 else tableType.setText("Sequence ???");
	    		 
	    		 if (topRowPanel!=null)
	    			 topRowPanel.setTopEnabled(theTable.getSelectedRowCount(), theTable.getRowCount());
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
   
    
    private SortTable theTable = null;
    private TableData theTableData = null;
    private JScrollPane sPane = null;
  
    private JTextField tableHeader = null;
    private JTextField tableType = null;
    private JTextField loadStatus = null;
    private JTextField txtStatus = null;
    private JLabel lblSummary = null;
    
    private ActionListener dblClick = null;
    private ActionListener sngClick = null;
    private ListSelectionListener selListener = null;
    
    private JCheckBox [] chkFields = null;
    private ActionListener colSelectChange = null;
    
	private JPanel tableButtonPanel = null;
	private JPanel tableStatusPanel = null;
	private JPanel fieldSelectPanel = null;
	private JPanel tableSummaryPanel = null;
	
	private JButton showColumnSelect = null;
	private JButton clearColumn = null;
	private Thread buildThread = null;
	    
	private String strSubQuery = "";
    private String strQuerySummary = null;
	private String tabName = "";
	private boolean isList=false;
	private int totalSeq=0;
	
	private MTCWFrame theViewerFrame = null;
	private ViewerSettings vSettings = null;
	private SeqsTopRowPanel topRowPanel = null;
	
	private int viewType=0;
}
