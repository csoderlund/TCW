package sng.viewer.panels.Basic;
/**********************************************
 * Table for Basic Sequence and Hit. Includes status bar
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.Stats;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import util.ui.DisplayFloat;

public class BasicTablePanel extends JPanel {
	private static final long serialVersionUID = 2737912903605181053L;
	private final int MAX_COL = 260;
	private final Color altRowColor = Globalx.altRowColor;
	private final Color selectColor = Globalx.selectColor;
	
	public BasicTablePanel (STCWFrame mf, BasicHitQueryTab t, String [] col, boolean [] b) {
		theMainFrame = mf;
		hitTab = t;
		setColumns(col, b);
		
		createBasicTable();
	}
	public BasicTablePanel (STCWFrame mf, BasicSeqQueryTab t, String [] col, boolean [] b) {
		theMainFrame = mf;
		seqTab = t;
		setColumns(col, b);
		
		createBasicTable();
	}
	private void createBasicTable () {
		createStatusBar();
		
		theTablePanel = Static.createPagePanel();
		theTableModel = new TableModel();
		theTable = new JTable(theTableModel)
		{
			private static final long serialVersionUID = -1559706452814091003L;

			public Component prepareRenderer(
			        TableCellRenderer renderer, int row, int column)
			    {
			        Component c = super.prepareRenderer(renderer, row, column);
			        if (theTable.isRowSelected(row)) c.setBackground(selectColor);
			        else {
				        boolean bBlueBG = row % 2 == 1;
				        if ( bBlueBG )c.setBackground( altRowColor );
						else c.setBackground( Globalx.BGCOLOR );
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
				updateTopButtons();
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
	
		add(txtStatus);
		add(Box.createVerticalStrut(5));
		add(theTablePanel);
		add(Box.createVerticalStrut(5));
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
	// Parent specific
	private void updateTopButtons() {
		if (hitTab!=null) hitTab.updateTopButtons(getSelectedRowCount());
		else if (seqTab!=null) seqTab.updateTopButtons(getSelectedRowCount());
	}
	private Object getValue(int row, int index) {
		if (index == 0)  return row+1;
		if (hitTab!=null) return hitTab.getValueAt(row, index);
		else if (seqTab!=null) return seqTab.getValueAt(row, index);
		else return null;
	}
	private void tableSort(boolean sortAsc, int mode) {
		if (hitTab!=null) hitTab.tableSort(sortAsc, mode);
		else if (seqTab!=null) seqTab.tableSort(sortAsc, mode);
	}
	private int getNumRow() {
		if (hitTab!=null) return hitTab.getNumRow();
		else if (seqTab!=null) return seqTab.getNumRow();
		else return 0;
	}
	// called from parent
	public void setStatus(String status) {
		txtStatus.setText(status);
	}
	public void setColumns(String [] col, boolean [] b) {
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
	/****************************************************************
	 * Shared table functions
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
					else { 
						Out.prt("BasicTablePanel: class? " + (String) theTable.getValueAt(x, y) + " " + dtype);
					}
				}
				double [] results = Stats.averages(colName, dArr, false);
				rows[nRow][0] = colName;
				for (int i=0, c=1; i<results.length; i++, c++) {
					if ((i>=intCol && isInt) || (i==results.length-1)) {
						if (results[i]<0) rows[nRow][c] = "N/A"; // overflow
						else rows[nRow][c] = String.format("%,d", (long) results[i]);
					}
					else rows[nRow][c] = Out.formatDouble(results[i]);
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
	public void tableExportToFile(String type) {
		String prefix = (type.equals("Seq")) ? "BasicSeqTableColumns" : "HitTableColumns";
		String fileName = prefix + Globalx.CSV_SUFFIX;
		
		PrintWriter pw = getTextToFile(fileName);
		if (pw!=null) {
			theTableModel.exportTableColumns(prefix, pw); 
			if (UIHelpers.isApplet()) {
				String [] msg = {"Export to " + fileName + " complete"};
				UserPrompt.displayInfo("Export", msg);
			}
		}
	}
	private PrintWriter getTextToFile(String fileName) {
		PrintWriter pw = null;
		final JFileChooser fc = new JFileChooser(theMainFrame.lastSaveFilePath);
		fc.setSelectedFile(new File(fileName));
	
		if (fc.showSaveDialog(theMainFrame) == JFileChooser.APPROVE_OPTION) {
			final File f = fc.getSelectedFile();
			final File d = f.getParentFile();
			if (!d.canWrite()) { 
				JOptionPane.showMessageDialog(null, 
						"You do not have permission to write to " + d.getName(), 
						"Warning", JOptionPane.PLAIN_MESSAGE);
			}
			else {
				int writeOption = JOptionPane.YES_OPTION;
				if (f.exists()) {
					writeOption = JOptionPane.showConfirmDialog(theMainFrame,
						    "The file already exists, overwrite it?", "Save to File",
						    JOptionPane.YES_NO_OPTION);
				}
				if (writeOption == JOptionPane.YES_OPTION) {
					theMainFrame.setLastPath(f.getPath());
					try {
				    		Out.prt("Writing to file " + f.getPath());
				    		pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
					}
					catch(Exception e) { ErrorReport.prtReport(e, "Writing to file " + f.getPath());}
				}
			}
		}
		return pw;
	}
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
				}
			}
		);
	}
	 // Table model
	private class TableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;
		
		public TableModel() {
			changeColumns(columns, visible);
		}
		public void changeColumns(String [] columns, boolean [] visible) {
			colNamesList = new String[columns.length];
			for(int x=0; x<columns.length; x++)
				colNamesList[x] = columns[x];
			
			colIsVisList = new boolean[visible.length];
			for(int x=0; x<visible.length; x++)
				colIsVisList[x] = visible[x];	
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
        /************************
         * 1 2 3 4 5 6
         * a b c x y z
         * t f t f t f
         * 1   2   3
         * if relIndex=3 from columns shown in table, absIndex=5 for index into 
         */
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
  	  	private String convertToCSV(Object val) {
			if(val == null) return "";
			if(val.getClass() == String.class) {
				if(((String)val).indexOf(',') >= 0) {
					return "\"" + ((String)val) + "\"";
				}
			}
			return val.toString();
		}
		private void exportTableColumns(String type, PrintWriter pw) {
			String delim = Globalx.CSV_DELIM;
			String line = "", val="";
			
			int colCnt = getColumnCount();
			int rowCnt = getRowCount();
			Out.prt(1, "Processing " + rowCnt + " rows and " + colCnt + " columns");
			
			int x, y;
			boolean isApplet = UIHelpers.isApplet();
			for(y=0; y<colCnt; y++) {
				if (y>0) line += delim;
				line += getColumnName(y).replaceAll("\\s", "-"); 
			}
			pw.print(line + "\n");
			line="";
			
			for(x=0; x<rowCnt; x++) {
				for(y=0; y<colCnt; y++) {
					if(getValueAt(x, y) != null) val = getValueAt(x, y).toString();
					else val = "";
					if (y>0) line += delim;
					line += convertToCSV(val);
				}
				pw.print(line + "\n");
				line="";
				if (!isApplet && x%1000==0) Out.r("Wrote " + x); 
			}
			pw.close();
			Out.prt(1, "Finish export of " + getRowCount() + " rows");
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
	 /*********************
	  *  BOTTOM BUTTONS - 
	  ***/
	 private void createTableButtonPanel() {
		lblHeader = new JLabel("Modify table");
		
		btnUnselectAll = new JButton("Unselect All");
		btnUnselectAll.setMargin(new Insets(0, 0, 0, 0));
		btnUnselectAll.setFont(new Font(btnUnselectAll.getFont().getName(),Font.PLAIN,10));
		btnUnselectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theTable.clearSelection();
			}
		});
		
		btnSelectAll = new JButton("Select All");
		btnSelectAll.setMargin(new Insets(0, 0, 0, 0));
		btnSelectAll.setFont(new Font(btnSelectAll.getFont().getName(),Font.PLAIN,10));
		btnSelectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theTable.selectAll();
			}
		});
		
		btnDelete = new JButton("Delete selected");
		btnDelete.setMargin(new Insets(0, 0, 0, 0));
		btnDelete.setFont(new Font(btnDelete.getFont().getName(),Font.PLAIN,10));
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteFromList();
				tableRefresh();
			}
		});
		
		btnKeep = new JButton("Keep selected");
		btnKeep.setMargin(new Insets(0, 0, 0, 0));
		btnKeep.setFont(new Font(btnKeep.getFont().getName(),Font.PLAIN,10));
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				keepFromList();
				tableRefresh();
			}
		});
		
		btnSort = new JButton("Sort by other list");
		btnSort.setMargin(new Insets(0, 0, 0, 0));
		btnSort.setFont(new Font(btnSort.getFont().getName(),Font.PLAIN,10));
		btnSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hitTab.convertToOrder();
				tableRefresh();
			}
		});
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout ( bottomPanel, BoxLayout.X_AXIS ));
		bottomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		bottomPanel.setBackground(Globalx.BGCOLOR);
		bottomPanel.setAlignmentY(LEFT_ALIGNMENT);
		
		bottomPanel.add(lblHeader);
		bottomPanel.add(Box.createHorizontalStrut(5));
		bottomPanel.add(btnDelete);
		bottomPanel.add(Box.createHorizontalStrut(3));
		bottomPanel.add(btnKeep);
		
		bottomPanel.add(Box.createHorizontalStrut(15));
		bottomPanel.add(btnSelectAll);
		bottomPanel.add(Box.createHorizontalStrut(3));
		bottomPanel.add(btnUnselectAll);
		
		bottomPanel.add(Box.createHorizontalStrut(15));
		if (hitTab!=null) bottomPanel.add(btnSort);

		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.setMaximumSize(bottomPanel.getPreferredSize());
		add(bottomPanel);
	}
	private void keepFromList()
	{
		int numElements = theTable.getRowCount();
		int [] selValues = theTable.getSelectedRows();		

		int [] opposite = new int[numElements - selValues.length];
		int x=0, selPos=0, nextval=0;
		
		for(x=0; x<opposite.length; x++)
		{
			while(selPos<selValues.length && selValues[selPos] == nextval) {
				selPos++;
				nextval++;
			}
			opposite[x] = nextval++;
		}
		deleteFromList(opposite);
	}
		
	private void deleteFromList()
	{
		int [] selValues = theTable.getSelectedRows();
		deleteFromList(selValues);
	}
	private void deleteFromList(int [] selValues)
	{
		theTable.clearSelection();
		for(int x=selValues.length-1; x>=0; x--) {
			if (hitTab!=null) hitTab.removeFromList(selValues[x]);
			else seqTab.removeFromList(selValues[x]);
		}
		if (hitTab!=null) {
			setStatus(hitTab.getStatus());
			hitTab.tableHitRecalc();
		}
		else setStatus("Results: " + seqTab.getNumRow());
	}
	
	//Table button panel
	private JButton btnDelete = null, btnKeep = null;
	private JButton btnUnselectAll = null, btnSelectAll = null, btnSort=null;
	private JLabel lblHeader = null;

	private String [] columns = null;
	private boolean [] visible = null;
	
	private JPanel theTablePanel = null;
	private TableModel theTableModel = null;
	private JTable theTable = null;
	private TableModel.ColumnListener colListener = null;
	private JScrollPane tableScroll = null;
	private JTextField txtStatus = null;
	
	private STCWFrame theMainFrame=null;
	private BasicHitQueryTab hitTab=null;
	private BasicSeqQueryTab seqTab=null;
}
