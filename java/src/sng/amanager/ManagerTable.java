package sng.amanager;
/*************************************************************
 * Displays the three tables on the runSingleTCW main window
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import util.methods.Out;

public class ManagerTable extends JTable {
	private static final long serialVersionUID = -3339710873853787507L;
	
	public static final int EXP_LIB_MODE = 1;
	public static final int TRANS_LIB_MODE = 2;
	public static final int ANNODB_MODE = 3;

	public ManagerTable(int mode) {
		nMode = mode;
		theModel = new ManagerTableModel();
    	
        setAutoCreateColumnsFromModel( true );
       	setColumnSelectionAllowed( false );
       	setCellSelectionEnabled( false );
       	setRowSelectionAllowed( true );
       	setShowHorizontalLines( false );
       	setShowVerticalLines( true );	
       	setIntercellSpacing ( new Dimension ( 1, 0 ) );
       	setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
       	setOpaque(true);

       	setModel(theModel);
       	setDefaultRenderer(String.class, new TheRenderer());
       	
       	String [] col = theModel.colNames;
       	for (int i=0; i<col.length; i++) {
       		if (!col[i].equals("Title") && !col[i].equals("annoDB") ) {
	       		TableColumn column = getColumnModel().getColumn(i);
	       		column.setMaxWidth(column.getPreferredWidth());
	       		column.setMinWidth(column.getPreferredWidth());
	       	}
       	}
	}
	
	public void addRow(boolean selected, boolean loaded, String [] values) {
		theModel.addRow(selected, loaded, values);
	}
	
	public void resetData() {
		theModel.clearAllData();
	}
	
	private ManagerTableModel theModel = null;
	private int nMode = -1;
	
	private class ManagerTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -3635828419937775047L;

		private final String [] COL_NAMES_EXP = { "SeqID", "Condition", "Title", "Reps" };
		private final String [] COL_NAMES_TRANS_LIB = { "SeqID", "Title" };
		private final String [] COL_NAMES_ANNODB = { "Load", "Taxonomy", "Action", "annoDB" };
		
		public ManagerTableModel() {
			if(nMode == EXP_LIB_MODE) {
				colNames = new String[COL_NAMES_EXP.length];
				for(int x=0; x<colNames.length; x++)
					colNames[x] = COL_NAMES_EXP[x];
			}
			else if(nMode == TRANS_LIB_MODE) {
				colNames = new String[COL_NAMES_TRANS_LIB.length];
				for(int x=0; x<colNames.length; x++)
					colNames[x] = COL_NAMES_TRANS_LIB[x];
			}
			else if(nMode == ANNODB_MODE){
				colNames = new String[COL_NAMES_ANNODB.length];
				for(int x=0; x<colNames.length; x++)
					colNames[x] = COL_NAMES_ANNODB[x];
			}
		}
		
		public void addRow(boolean selected, boolean loaded, String [] values) {
			int oldSize = 0;
			TableRow [] oldRows = null;
			
			if(theRows != null && theRows.length > 0) {
				oldSize = theRows.length;
				oldRows = new TableRow[theRows.length];
				for(int x=0; x<theRows.length; x++) {
					oldRows[x] = new TableRow(theRows[x].bSelected, theRows[x].bLoaded, theRows[x].vals);
				}
			}
			
			theRows = new TableRow[oldSize + 1];
			if(oldSize > 0) {
				for(int x=0; x<oldSize; x++) {
					theRows[x] = new TableRow(oldRows[x].bSelected, oldRows[x].bLoaded, oldRows[x].vals);
				}
			}
			theRows[theRows.length - 1] = new TableRow(selected, loaded, values);
		}
		
		public void clearAllData() {
			theRows = new TableRow[0];
		}
		
		public int getColumnCount() {
			return colNames.length;
		}

		public int getRowCount() {
			if(theRows == null || theRows.length == 0)
				return 0;
			return theRows.length;
		}
		
		public Object getValueAt(int row, int column) {
			if(row >= theRows.length)
				return "";
			if(nMode == ANNODB_MODE) {
				if(column == 0)
					return theRows[row].isSelected();
				return theRows[row].getValueAt(column-1);
			}
			else if( nMode == TRANS_LIB_MODE ){
				if(theRows[row].isSelected() && column == 0)
					return theRows[row].getValueAt(column);
				return theRows[row].getValueAt(column);
			}
			return theRows[row].getValueAt(column);
		}
		
		public boolean isLoadedAt(int row) {
			if (row>=theRows.length) {
				Out.debug("*** ManagerTable: Synchronization error -- ignore");
				return false; 
			}
			return theRows[row].bLoaded;
		}

		public void setValueAt(Object val, int row, int column) {
			if(nMode == ANNODB_MODE) {
				if(column == 0)
					theRows[row].setSelected((Boolean)val);
				else
					theRows[row].setValueAt((String)val, column-1);
			}
			else
				theRows[row].setValueAt((String)val, column);
		}
		
		public Class<?> getColumnClass(int column) {
			if(column == 0 && (nMode == ANNODB_MODE))
				return Boolean.class;
			return String.class;
		}
		
	    	public String getColumnName(int columnIndex) {
	    		return colNames[columnIndex];
	    	}

		public boolean isCellEditable (int row, int column) { 
			return (nMode == ANNODB_MODE) && column == 0; 
		}
		private class TableRow {
			public TableRow(boolean selected, boolean loaded, String [] values) {
				bSelected = selected;
				bLoaded = loaded;
				vals = new String[values.length];
				for(int x=0; x<vals.length; x++)
					vals[x] = values[x];
			}
			private boolean isSelected() { return bSelected; }
			private void setSelected(boolean selected) { bSelected = selected; }
			
			private String getValueAt(int column) { return vals[column]; }
			private void setValueAt(String value, int column) { vals[column] = value; }
			
			private boolean bSelected = false;
			private boolean bLoaded = false;
			private String [] vals = null;
		}
		
		private String [] colNames = null;
		private TableRow [] theRows = null;
	}
	
	private class TheRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 6195418129152499449L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			if(nMode == ANNODB_MODE) {
				Font oldFont = c.getFont();
				Font newFont = null;
				
				try { 
					if(theModel.isLoadedAt(row)) {
						newFont = new Font(oldFont.getFontName(), Font.ITALIC, oldFont.getSize());
						c.setFont(newFont);
					}
				}
				catch (Exception e) {}
			}
			return c;
		}
	}
};