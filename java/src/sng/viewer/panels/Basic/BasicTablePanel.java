package sng.viewer.panels.Basic;
/**********************************************
 * Table for Basic Sequence and Hit. Includes status bar
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import sng.viewer.STCWFrame;
import sng.viewer.panels.DisplayDecimalTab;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileWrite;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.Stats;
import util.ui.UserPrompt;
import util.ui.DisplayFloat;

public class BasicTablePanel extends JPanel {
	private static final long serialVersionUID = 2737912903605181053L;
	private final int MAX_COL = 260;
	private final int idIdx = 0;		// Seq and Hit start with seqID/HitID column (index 0)
	
	private final Color BGCOLOR = 		Globalx.BGCOLOR;
	private final Color HIGHCOLOR =   Globalx.HIGHCOLOR;
	private final Color altRowColor = Globalx.altRowColor;
	private final Color selectColor = Globalx.selectColor;
	
	public BasicTablePanel (STCWFrame mf, BasicHitTab hitTab, String [] columns, boolean [] visible, String [] pvalColNames) {
		this.hitTab = hitTab;
		this.pvalColNames = pvalColNames;
		
		setColumns(columns, visible);
		
		createStatusBar();
		createBasicTable();
		createTableButtonPanel();
	}
	public BasicTablePanel (STCWFrame mf, BasicSeqTab seqTab, String [] columns, boolean [] visible) {
		this.seqTab = seqTab;
		
		setColumns(columns, visible);
		
		createStatusBar();
		createBasicTable();
		createTableButtonPanel();
	}
	private void createStatusBar() {
		txtStatus = new JTextField(400);
		txtStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtStatus.setEditable(false);
		txtStatus.setBackground(Color.WHITE);
		txtStatus.setBorder(BorderFactory.createEmptyBorder());
		Dimension dimStat = txtStatus.getPreferredSize();
		dimStat.width = 500;
		txtStatus.setPreferredSize(dimStat);
		txtStatus.setMaximumSize(txtStatus.getPreferredSize());
	}
	private void createBasicTable () {
		theTablePanel = Static.createPagePanel();
		theTableModel = new TableModel();
		theTable = new JTable(theTableModel) {
			private static final long serialVersionUID = -1559706452814091003L;

			public Component prepareRenderer(
			        TableCellRenderer renderer, int row, int column)
		    {
		        Component c = super.prepareRenderer(renderer, row, column);
		        if (theTable.isRowSelected(row)) {
		        	c.setBackground(selectColor);
		        }
		        else if (highSet.size()>0) { // col=0 is the row number, that gets sorted with the row, but not displayed
	        		String id = (seqTab!=null) ? seqTab.getValueAt(row, 0).toString() : 
	        									 hitTab.getValueAt(row, 0).toString();
	        		if (highSet.contains(id)) 	c.setBackground( HIGHCOLOR );	
	        		else 						c.setBackground( BGCOLOR );
		        }			
		        else {
			        boolean bBlueBG = row % 2 == 1;
			        if ( bBlueBG )	c.setBackground( altRowColor );
					else 			c.setBackground( BGCOLOR );
		        }
		        /* CAS322 added, CAS323 removed because problems, CAS324 fixed */
		        if (hitTab!=null && DisplayDecimalTab.isHighPval() && pvalColNames!=null) { 
		        	String displayCol = theTable.getColumnName(column);
		        	for (String cn : pvalColNames) {
			    		if (cn.contentEquals(displayCol)) {
			    			Object obj = theTable.getValueAt(row,column);
			    			if (obj.toString()==Globalx.sNoVal) break; // CAS330 changed 3 to '-'
			    			
					    	double theVal=0.0;
				        	if (obj instanceof DisplayFloat) {
								theVal = ((DisplayFloat) obj).getValue();
								
								Color high = DisplayDecimalTab.getPvalColor(theVal);
					        	if (high!=null) c.setBackground(high);
							}
							else 
								Out.prt("Cannot read table obj " + obj.toString());
				        	break;
			    		}
			    	}
		        }
		        return c;
		    }
		};
		theTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		theTable.setCellSelectionEnabled(false);
		theTable.setRowSelectionAllowed(true);
		theTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); 
		theTable.setDefaultRenderer( Object.class, new BorderLessTableCellRenderer() );
		theTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				enableAllButtons(); // CAS336
			}
		});
		JTableHeader header = theTable.getTableHeader();
		header.setUpdateTableInRealTime(true);
		colListener = theTableModel.new ColumnListener(theTable);
		header.addMouseListener(colListener);

		theTable.getTableHeader().setBackground(Color.WHITE);
		tableScroll = new JScrollPane(theTable);
		
		theTablePanel.add(tableScroll);
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Color.white);
	
		add(txtStatus);		add(Box.createVerticalStrut(5));
		add(theTablePanel);	add(Box.createVerticalStrut(5));
	}
	
	/*************************************************************
	 * Table methods
	 */
	public void tableResizeColumns() {
		if(theTable.getRowCount() > 0) theTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		else theTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		int margin = 4;
		
		for(int x=0; x < theTable.getColumnCount(); x++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) theTable.getColumnModel();
			TableColumn col = colModel.getColumn(x);
			int maxSize = 0;
			
			TableCellRenderer renderer = col.getHeaderRenderer();
			if(renderer == null) renderer = theTable.getTableHeader().getDefaultRenderer();
			
			Component comp = renderer.getTableCellRendererComponent(theTable, 
					col.getHeaderValue(), false, false, 0, 0);
			maxSize = comp.getPreferredSize().width;
	 	
			for(int y=0; y<theTable.getRowCount() && y<20; y++) {
				renderer = theTable.getCellRenderer(y, x);
				comp = renderer.getTableCellRendererComponent(theTable, 
						theTable.getValueAt(y, x), false, false, y, x);
				maxSize = Math.max(maxSize, comp.getPreferredSize().width);
			}
			maxSize += margin;
			col.setPreferredWidth(Math.min(maxSize, MAX_COL)); 
		}
		((DefaultTableCellRenderer) 
				theTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
		 theTableModel.fireTableDataChanged();
	}
	public void tableRefresh() {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					if(theTableModel != null) {
						JTableHeader header = theTable.getTableHeader();
						header.removeMouseListener(colListener);
					}
					theTableModel = new TableModel();
					theTable.setModel(theTableModel);
					tableResizeColumns();
					
					JTableHeader header = theTable.getTableHeader();
					header.setUpdateTableInRealTime(true);
					colListener = theTableModel.new ColumnListener(theTable);
					header.addMouseListener(colListener);
					theTable.getTableHeader().setBackground(Color.WHITE);
					theTable.setDefaultRenderer( Object.class, new BorderLessTableCellRenderer() );
					
					enableAllButtons();
				}
			}
		);
	}
	 // XXX Table model
	private class TableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;
		
		public TableModel() {
			changeColumns(columns, visible);
		}
		public void changeColumns(String [] columns, boolean [] visible) {
			colNamesList = new String[columns.length];
			for(int x=0; x<columns.length; x++)	colNamesList[x] = columns[x];
			
			colIsVisList = new boolean[visible.length];
			for(int x=0; x<visible.length; x++)	colIsVisList[x] = visible[x];	
		}
		public int getColumnCount() {
        	int count = 0;
   
        	for(int x=0; x<colIsVisList.length; x++)
        		if(colIsVisList[x]) count++;        	
        	return count;
        }
        public int getRowCount() {
        	return getNumRow();
        }
        public Object getValueAt(int row, int col) {
        	int index = getMappedColumn(col);
        	return getValue(row, index);
        }
        public String getColumnName(int col) { 
        	int index = getMappedColumn(col);
        	return colNamesList[index]; 
        }
        // return index that includes invisible columns
        private int getMappedColumn(int tableIndex) {
        	int iTable = 0;
        	int absIndex = -1;
        	
        	for(int iAbs=0; iAbs<colIsVisList.length && absIndex < 0; iAbs++) {
        		if(colIsVisList[iAbs] && (tableIndex == iTable)) absIndex = iAbs;
        		if(colIsVisList[iAbs]) iTable++;
        	}
        	return absIndex;
        }        
  	  	public class ColumnListener extends MouseAdapter {
		    protected JTable table;

		    public ColumnListener(JTable t) {
	    		table = t;
		    	bAscend = new boolean[table.getColumnCount()];
		    	for(int x=0; x<bAscend.length; x++) bAscend[x] = true;
		    }
		    public void mouseClicked(MouseEvent e) {
		    	sortTable(e.getX(), SwingUtilities.isLeftMouseButton(e));						
		    }
  	  		private void sortTable(int xLoc, boolean leftclick) {
  	  			TableColumnModel colModel = table.getColumnModel();
  	  			int columnModelIndex = colModel.getColumnIndexAtX(xLoc);
  	  			if (columnModelIndex==-1) return;
  	  			
  	  			int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();
  	  			if (modelIndex < 0) return;
  	  			
  	  			int mappedColumn = getMappedColumn(modelIndex);
  	  			  	  			
  	  			if(leftclick) bAscend[modelIndex] = !bAscend[modelIndex];
  	  			else bAscend[modelIndex] = true;
  	  			
  	  			tableSort(bAscend[modelIndex], mappedColumn);
  	  			
  	  			table.tableChanged(new TableModelEvent(TableModel.this));
  	  			table.repaint();
  	  		}
  	  		private boolean [] bAscend = null;
  	  	}
        private String [] colNamesList = null;
		private boolean [] colIsVisList = null;		
	} // end TableModelClass
		
	 private static class BorderLessTableCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 6447910004356379503L;

			public Component getTableCellRendererComponent(
		            final JTable table,
		            final Object value,
		            final boolean isSelected,
		            final boolean hasFocus,
		            final int row,
		            final int col) {

		        final boolean showFocusedCellBorder = false; // change this to see the behavior change

		        final Component c = super.getTableCellRendererComponent(
		                table, value, isSelected,
		                showFocusedCellBorder, // && hasFocus, // shall obviously always evaluate to false in this example
		                row, col
		        );
		        return c;
		    }
	}
	 /*************************************************************/
	// Parent specific
	private Object getValue(int row, int index) {
		if (index == 0)  return row+1;
		if (hitTab!=null) 		return hitTab.getValueAt(row, index);
		else if (seqTab!=null) 	return seqTab.getValueAt(row, index);
		else return null;
	}
	private void tableSort(boolean sortAsc, int mode) {
		if (hitTab!=null) 		hitTab.tableSort(sortAsc, mode);
		else if (seqTab!=null) 	seqTab.tableSort(sortAsc, mode);
	}
	private int getNumRow() {
		if (hitTab!=null) 		return hitTab.getNumRow();
		else if (seqTab!=null) 	return seqTab.getNumRow();
		else return 0;
	}
	
	// called from parent
	public void setStatus(String status) {
		txtStatus.setText(status);
	}
	public void setColumns(String [] col, boolean [] b) { // when columns change
		columns = new String [col.length];
		visible = new boolean [b.length];
		for (int i=0; i<col.length; i++) {
			columns[i] = col[i];
			visible[i] = b[i];
		}
	};
	public int getSelectedRow() {
		if (getSelectedRowCount()==0) return -1;
		
		return theTable.getSelectedRows()[0];
	}
	public int [] getSelectedRows() {
		return theTable.getSelectedRows();
	}
	public int getSelectedRowCount() {
		try {
			int cnt = theTable.getSelectedRowCount();
			return cnt;
		}
		catch (Exception e) {return 0;}
	}
	
	// XXX CAS334 the following were added for the new Basic Sequence select option
	public void selectRow(int row) {
		theTable.addRowSelectionInterval(row, row);
	}
	 /*********************
	  *  BOTTOM BUTTONS - 
	  ***/
	 private void createTableButtonPanel() {
		lblHeader = new JLabel("Modify");
		
		btnDelete = Static.createButtonPlain("Delete selected", false);
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteFromList();
				enableAllButtons();
				theTableModel.fireTableDataChanged(); // CAS338 was not displaying right w/o this
			}
		});	
		btnKeep = Static.createButtonPlain("Keep selected", false);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				keepFromList();
				enableAllButtons();
				theTableModel.fireTableDataChanged(); // CAS338 was not displaying right w/o this
			}
		});
		btnUnselectAll = Static.createButtonPlain("Unselect All", false);
		btnUnselectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theTable.clearSelection();
				enableAllButtons();
			}
		});
		btnSelectAll = Static.createButtonPlain("Select All", false);
		btnSelectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theTable.selectAll();
				enableAllButtons();
			}
		});
		btnSort = Static.createButtonPlain("Sort by other list", false);
		btnSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hitTab.convertToOrder();
				enableAllButtons();
				theTableModel.fireTableDataChanged(); // is not automatically triggered
			}
		});
		btnQuery = Static.createButtonPlain("Select Query", false); // CAS336 move from beside BUILD
		btnQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				seqTab.getFilterPanel().loadSelect();
			}
		});
		btnHighSelect = Static.createButtonPlain("Highlight", false); // CAS336 new
		btnHighSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				highSelect();
				theTable.clearSelection();
				enableAllButtons();
			}
		});
		btnHighClear = Static.createButtonPlain("Clear", false); // CAS336 new
		btnHighClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				highClear();
				theTableModel.fireTableDataChanged(); // is not automatically triggered
				enableAllButtons();
			}
		});
		
		JPanel bottomPanel = Static.createRowPanel();
		bottomPanel.add(lblHeader); 	bottomPanel.add(Box.createHorizontalStrut(4));
		bottomPanel.add(btnDelete);		bottomPanel.add(Box.createHorizontalStrut(2));
		bottomPanel.add(btnKeep);		bottomPanel.add(Box.createHorizontalStrut(14));
		
		bottomPanel.add(btnSelectAll);	bottomPanel.add(Box.createHorizontalStrut(2));
		bottomPanel.add(btnUnselectAll);bottomPanel.add(Box.createHorizontalStrut(2));
		
		if (hitTab!=null) {
			bottomPanel.add(Box.createHorizontalStrut(12));
			bottomPanel.add(btnSort); bottomPanel.add(Box.createHorizontalStrut(14));
		}
		else {
			bottomPanel.add(btnQuery);	bottomPanel.add(Box.createHorizontalStrut(14));
		}
		bottomPanel.add(btnHighSelect);	bottomPanel.add(Box.createHorizontalStrut(2));
		bottomPanel.add(btnHighClear);
		
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.setMaximumSize(bottomPanel.getPreferredSize());
		add(bottomPanel);
	}
	
	/******************************************************
	 * Keep Selected and Remove Selected
	 */
	private void keepFromList(){
		int numElements = theTable.getRowCount();
		int [] selValues = theTable.getSelectedRows();		

		int [] opposite = new int[numElements - selValues.length];
		int x=0, selPos=0, nextval=0;
		
		for(x=0; x<opposite.length; x++) {
			while(selPos<selValues.length && selValues[selPos] == nextval) {
				selPos++;
				nextval++;
			}
			opposite[x] = nextval++;
		}
		deleteFromList(opposite);
	}
	private void deleteFromList(){
		int [] selValues = theTable.getSelectedRows();
		deleteFromList(selValues);
	}
	// CAS335 change to send the list instead of row by row; CAS336 the status is set in hitTab and seqTab
	private void deleteFromList(int [] selValues) {
		theTable.clearSelection(); 
		if (hitTab!=null) 	hitTab.deleteFromList(selValues);
		else 				seqTab.deleteFromList(selValues);
	}
	// CAS336 adds selected to highSet, which is highlighted on display. 
	private void highClear() {
		highSet.clear();
		if (hitTab!=null) 	hitTab.appendStatus("");
		else 				seqTab.appendStatus("");
	}
	private void highSelect() {
		int [] selRow = theTable.getSelectedRows();	
		
		int cntOrig = highSet.size(), cntNew=0;
		for(int x=0; x<selRow.length; x++)  {
			String id = (seqTab!=null) ? seqTab.getValueAt(selRow[x], idIdx).toString() :
										 hitTab.getValueAt(selRow[x], idIdx).toString();
			if (!highSet.contains(id)) {
				 highSet.add(id);
				 cntNew++;
			}
		}
		String msg = (cntOrig==0) ? "Highlight " + highSet.size() : "Highlight " + cntOrig + "+" + cntNew;
		if (seqTab!=null) 	seqTab.appendStatus(msg);	
		else				hitTab.appendStatus(msg);
	}
	
	public void enableLowButtons(boolean b) {// CAS334 new - called when database query starts
		btnDelete.setEnabled(b);
		btnKeep.setEnabled(b);
		btnUnselectAll.setEnabled(b);
		btnSelectAll.setEnabled(b);
		btnSort.setEnabled(b);
		btnQuery.setEnabled(b);
		btnHighSelect.setEnabled(b);
		btnHighClear.setEnabled(b);
	}
	public void enableAllButtons() { // called from lower buttons, valueChanged, tableRefresh CAS337
		int sel = theTable.getSelectedRowCount();
		int row = theTable.getRowCount();
		
		if (hitTab!=null) 	hitTab.enableTopButtons(sel, row);
		else  				seqTab.enableTopButtons(sel, row);
		
		boolean bSel = (sel>0);
		btnDelete.setEnabled(bSel);
		btnKeep.setEnabled(bSel);
		
		btnUnselectAll.setEnabled(bSel);
		btnHighSelect.setEnabled(bSel);
		
		boolean bRow = (row>0);
		btnSort.setEnabled(bRow);
		
		btnSelectAll.setEnabled(bRow);
		btnQuery.setEnabled(bRow);
		
		if (!bRow) highSet.clear();
		btnHighClear.setEnabled(highSet.size()>0);
	}
	
	/****************************************************************
	 * Popup/export/copy
	 */
	public void statsPopUp(final String title) {
		String info = statsPopUpCompute(title);
		UserPrompt.displayInfoMonoSpace(null, title, info);		
	}
	private String statsPopUpCompute(String sum) {
		try {
			String [] fields = 
				{"Column", "Average", "StdDev", "Median", "Range", "Low", "High", "Sum", "Count"};
			int [] justify =   {1,   0,  0, 0, 0, 0, 0, 0, 0};
			int intCol=3;
			
			int tableRows=theTable.getRowCount(), tableCols = theTable.getColumnCount();
			int nStat=  fields.length, nRow=0;
			
		    String [][] rows = new String[tableCols][nStat]; 
			double [] dArr = new double [tableRows];

		    for(int y=0; y < tableCols; y++) {
		    	String colName = theTable.getColumnName(y);
		    	if (colName.equals("Row")) continue;
		    		
			 	Object obj = theTable.getValueAt(0,y);
			 	if (obj==null) continue; 
				Class <?> dtype = obj.getClass();
				if (dtype==String.class) continue;
				
			 	for (int i=0; i<tableRows; i++) dArr[i]=Globalx.dNoVal;
			 	boolean isInt=false;
			 	
				for (int x=0, c=0; x<tableRows; x++) {
					if (theTable.getValueAt(x,y) == null) continue;
					
					obj = theTable.getValueAt(x,y);
					
					if (dtype == Integer.class) {
						int j = (Integer) theTable.getValueAt(x, y);
						dArr[c++] = (double) j;
						isInt=true;
					}
					else if (obj instanceof DisplayFloat) {
						dArr[c++] = ((DisplayFloat) obj).getValue();
					}
					else if (dtype == Float.class) {
						dArr[c++] = (Float)  theTable.getValueAt(x, y);
					}
					else if (dtype == Double.class) {
						dArr[c++] = (Double) theTable.getValueAt(x, y);
					}
					else if ((String) theTable.getValueAt(x, y)==Globalx.sNoVal) {
						dArr[c++]=Globalx.dNoDE; // CAS330 DE can be '-'
					}
					else { 
						Out.prtToErr("BasicTablePanel: class? " + (String) theTable.getValueAt(x, y) + " " + dtype);
					}
				}
				double [] results = Stats.averages(colName, dArr, false);
				rows[nRow][0] = colName;
				for (int i=0, c=1; i<results.length; i++, c++) {
					if ((i>=intCol && isInt) || (i==results.length-1)) {
						if (results[i]<0) rows[nRow][c] = "N/A"; // overflow
						else rows[nRow][c] = String.format("%,d", (long) results[i]);
					}
					else rows[nRow][c] = DisplayDecimalTab.formatDouble(results[i]);
				}
				nRow++;
			}
			String statStr = Out.makeTable(nStat, nRow, fields, justify, rows);
			statStr += "\n" + tableRows + " rows; " + sum;
			return statStr;
		
		} catch(Exception e) {ErrorReport.reportError(e, "Error create column stats"); return "Error"; }
	}
	public String tableCopyString(String delim) {
 		StringBuilder retVal = new StringBuilder();
 	
		for(int x=0; x<theTable.getColumnCount()-1; x++) {
			retVal.append(theTable.getColumnName(x));
			retVal.append(delim);
		}
		retVal.append(theTable.getColumnName(theTable.getColumnCount()-1));
		retVal.append("\n");
		
		for(int x=0; x<theTable.getRowCount(); x++) {
			StringBuilder row = new StringBuilder();
			for(int y=0; y<theTable.getColumnCount()-1; y++) {
				row.append(theTable.getValueAt(x, y));
				row.append(delim);
			}
			retVal.append(row.toString());
			retVal.append(theTable.getValueAt(x, theTable.getColumnCount()-1));
			retVal.append("\n");
		}
		return retVal.toString();
	}
	// CAS324 was in TableModel, which exported in original order
	public void tableExportToFile(Component btnC, String type) {
		String prefix = (type.equals("Seq")) ? "SeqBasicTable" : "HitBasicTable";
		String fileName = prefix + Globalx.CSV_SUFFIX;
		
		FileWrite fw = new FileWrite(FileC.bNoVer, FileC.bNoPrt);
		PrintWriter pw = fw.getWriter(btnC, "table", fileName, FileC.fTSV, FileC.wAPPEND);
		if (pw==null) return;
		
		String delim = Globalx.CSV_DELIM;
		String line = "", val="";
		
		int colCnt = theTable.getColumnCount();
		int rowCnt = theTable.getRowCount();
		String msg = (fw.isAppend()) ? "Append " : "Write "; // CAS342
		Out.prtSp(0, msg + rowCnt + " rows and " + colCnt + " columns to " + fileName);
		
		int x, y;
		
		pw.print("#"); // CAS314
		for(y=0; y<colCnt; y++) {
			if (y>0) line += delim;
			line += theTable.getColumnName(y).replaceAll("\\s", "-"); 
		}
		pw.print(line + "\n");
		line="";
		
		for(x=0; x<rowCnt; x++) {
			for(y=0; y<colCnt; y++) {
				if(theTable.getValueAt(x, y) != null) val = theTable.getValueAt(x, y).toString();
				else val = "";
				if (y>0) line += delim;
				
				String outVal;
				if (val==null) outVal="";
				else outVal = val.toString();
				
				line += outVal;
			}
			pw.print(line + "\n");
			line="";
			if (x%1000==0) Out.r("Wrote " + x); 
		}
		pw.close();
		Out.prtSp(0, "Complete writing " + theTable.getRowCount() + " rows");
	}
	// Table
	// if a column is invisible, it is not accessible from this file
	// seqTab.getValueAt(row, col) and hitTab.getValueAt(row, col) are sorted rows
	//		with all columns except row - hence, the seqID and hitID are index 0
	private TableModel theTableModel = null;	// rows sorted, but visible columns in order
	private JTable theTable = null;				// what is viewed
	private String [] columns = null;			// same as theTableModel.colNamesList
	private boolean [] visible = null;
	
	private JPanel theTablePanel = null;
	private TableModel.ColumnListener colListener = null;
	private JScrollPane tableScroll = null;
	private JTextField txtStatus = null;
	
	//Table button panel
	private JLabel lblHeader = null;
	private JButton btnDelete = null, btnKeep = null;
	private JButton btnUnselectAll = null, btnSelectAll = null, btnSort=null;
	private JButton btnHighSelect=null, btnHighClear=null, btnQuery=null;

	private BasicHitTab hitTab=null;
	private BasicSeqTab seqTab=null;
	private String [] pvalColNames;
	private boolean hitView=true; // if hitTab, is the highSet HitID or SeqID
	private HashSet <String> highSet = new HashSet <String> ();
}
