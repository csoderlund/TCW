package sng.util;

/************************************************************
 * Contains table methods for Main Sequence Table
 */
import java.sql.Connection;
import java.util.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListSelectionEvent;
import java.io.File;

import sng.database.Globals;
import sng.viewer.panels.DisplayDecimalTab;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.ui.DisplayFloat;
import util.ui.DisplayInt;
import util.methods.Out;

public class MainTableSort extends JTable implements TableModel
{	
	public static final String ROW_DELIMITER = Globals.ROW_DELIMITER;
	
	public MainTableSort ( 	FieldMapper inIndexer, Vector <String> r,
			int[] inDefaultFields, final ActionListener refreshListener)
	throws Exception
	{	
		fieldObj = inIndexer;
		tableRows = r;
		nTotalRows = tableRows.size();
		visibleColumns = inDefaultFields;
		
		setAutoCreateColumnsFromModel( true );
		setColumnSelectionAllowed( false );
		setCellSelectionEnabled( false );
		setRowSelectionAllowed( true );
		setShowHorizontalLines( false );
		setShowVerticalLines( true );	
		setIntercellSpacing ( new Dimension ( 1, 0 ) );
		setModel ( this );
		
		colListener = new ColumnListener(this, visibleColumns.length);
		bAscend = new boolean[visibleColumns.length];
		for(int x=0; x<bAscend.length; x++) bAscend[x] = true;
		
		getColumnModel().addColumnModelListener(colListener);
				
		JTableHeader header = getTableHeader();
		
		header.setReorderingAllowed( true );
		header.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				// if (e.getClickCount() != 1) return; CAS304 on MacOS 10.15 with Java 14, if click to sort one way, and then other, count>1
				tableHeaderClicked(e);
				
				int[] savedColumnWidths = getColumnWidths(); // kludge to prevent column auto-resizing on sort
				setColumnWidths(savedColumnWidths); 
			}
		});
		headerRenderer.setRenderer( new DefaultTableCellRenderer () );
		header.setDefaultRenderer( headerRenderer );
	
		addMouseListener(new MouseAdapter() { // double click row is same as single click with View Sequence
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 
						&& getSelectedRowCount() == 1
						&& isRowSelected(rowAtPoint(e.getPoint()))
						&& rowAtPoint(e.getPoint()) < getRowCount()) 
					notifyDoubleClick();
			}
		});

		setAutoResizeMode(AUTO_RESIZE_OFF);
		autofitColumns();
	}
	private void notifyDoubleClick ( ){		
	    ActionEvent e = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, "DoubleClickSingleRow" );

	    for ( int i = 0; i < doubleClickListeners.size(); ++i ) {
	        ActionListener curListener = doubleClickListeners.get(i);
	        curListener.actionPerformed( e );
        }				
	}
	private void tableHeaderClicked( MouseEvent e ) 
	{ 
		//if (e.getClickCount() == 1) { // CAS304 same comment as above
		JTableHeader h = null;
		try {
			// Figure out what column was clicked:
			h = (JTableHeader)e.getSource();
			h.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); 
	        TableColumnModel columnModel = h.getColumnModel();
	  
	        String columnName = this.getColumnName(
	        		getMappedColumn(this.getColumnModel().getColumnIndexAtX(e.getX())));
	        int viewColumn = getMappedColumn(columnModel.getColumnIndexAtX(e.getX() + 1)) - 1;
	        viewColumn = getColumnModel().getColumnIndex(columnName);
            if ( viewColumn < 0 )
                return;   
            
	        int nNewColumn = columnModel.getColumn(viewColumn).getModelIndex()-1;
	        if (nNewColumn < 0 ) return;
	        
	        if(SwingUtilities.isLeftMouseButton(e))
	        		bAscend[nNewColumn] = !bAscend[nNewColumn];
	        else
	        		bAscend[nNewColumn] = true;
	        
	        sortByColumn ( nNewColumn, bAscend[nNewColumn] );    
		}
		catch ( Exception err ) {
			ErrorReport.reportError(err, "TCW error: sorting table");
		}
		finally { // ensure that cursor is restored
			if (h != null) 
				h.setCursor(Cursor.getDefaultCursor());
		}
		//}
	}
	private int[] getColumnWidths(){
    	int numColumns = getModel().getColumnCount();
    	int[] widths = new int[numColumns];
    	for (int i = 0;  i < numColumns;  i++)
    		widths[i] = getColumnModel().getColumn(i).getPreferredWidth();
    	
    	return widths;
    }
    private void setColumnWidths(int[] widths) {
	    for (int i = 0;  i < getModel().getColumnCount();  i++)
	    	getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }	
	protected int getMappedColumn(int columnIndex) {
		return colListener.getFieldPositionFromColumn(columnIndex);
	}
	protected int getReverseMappedColumn(int columnIndex) {
		return colListener.getReverseFieldPositionFromColumn(columnIndex);
	}
	protected boolean hasColumnMoved(int columnIndex) {
		return colListener.hasColumnMoved(columnIndex);
	}
	private static final int MAX_AUTOFIT_COLUMN_WIDTH = 90; // in pixels
    
	protected void autofitColumns() {
        TableModel model = getModel();
        TableColumn column;
        Component comp;
        int headerWidth;
        int cellWidth;
        TableCellRenderer headerRenderer = getTableHeader().getDefaultRenderer();
        
        for (int i = 0;  i < getModel().getColumnCount();  i++) { // for each column
            column = getColumnModel().getColumn(i);

            comp = headerRenderer.getTableCellRendererComponent(
                      this, column.getHeaderValue(), false, false, 0, i);
            
            headerWidth = comp.getPreferredSize().width + 10; 
            
            cellWidth = 0;
            for (int j = 0;  j < getModel().getRowCount();  j++) { // for each row
	            comp = getDefaultRenderer(model.getColumnClass(i)).
	                       getTableCellRendererComponent(
	                            this, model.getValueAt(j, i), false, false, j, i);
	            cellWidth = Math.max(cellWidth, comp.getPreferredSize().width);
	            if (j > 100) break; // only check beginning rows, for performance reasons
            }
            column.setPreferredWidth(Math.min(Math.max(headerWidth, cellWidth), MAX_AUTOFIT_COLUMN_WIDTH));
        }
    }
	// XXX
	private void sortByColumn ( int nColumn, final boolean bInAscending ) // override SortableTable
	{	
		final int nNewSortField = getFieldIdxFromColumnIdx ( nColumn );
        if ( nNewSortField < 0 ) return;
		
		// if it is a DE column, sort by abs and then +/-
		final String dbName = fieldObj.getDBNameByID(nNewSortField);
		
		// null column is n-fold, sort it like DE
		final boolean negSort = (dbName == null || dbName.startsWith(Globals.PVALUE)) ? true : false;
		final boolean isPval =  (dbName != null && dbName.startsWith(Globals.PVALUE)) ? true : false;

		Comparator<Object []> sortRows = new Comparator<Object []> () {
			public int compare ( Object [] row1, Object [] row2 ) 
	    	{
				Object o1 = convertNull ( row1[1], isPval );
	            Object o2 = convertNull ( row2[1], isPval );
	            
	            if (negSort) { // CAS330 make NoVal sort to bottom
	            	if (!(o1 instanceof DisplayFloat) || !(o2 instanceof DisplayFloat)) {
	            		if ( o1 == o2 ) 		return 0;
		 	            else if ( o1 == null || o1.toString()==Globalx.sNoVal || o1.toString()==Globalx.sNullVal)	return 1;
		 	            else if ( o2 == null || o2.toString()==Globalx.sNoVal || o2.toString()==Globalx.sNullVal)	return -1;	
	            	}
	            	
	            	double	v1 = ((DisplayFloat)o1).getValue();
	            	double	v2 = ((DisplayFloat)o2).getValue();
	            
	            	Double d1 = Math.abs(v1);
	            	Double d2 = Math.abs(v2);
	            	int nRes = 0;
	            	if (d1 < d2) nRes = -1;
	            	else if (d1 > d2) nRes = 1;
	            	else if (v1>0 && v2<0) nRes=-1; // if equal absolute, positives go first
	            	else if (v1<0 && v2>0) nRes=1;
	            	return  (bInAscending ? nRes : -nRes);
	            }
	    	
	            // Sort nulls to the beginning.  
				int nRes = 0;
	            if ( o1 == o2 ) 		nRes = 0;
	            else if ( o1 == null )	nRes = -1;
	            else if ( o2 == null )	nRes = 1;
	            else {
            		if (o1 instanceof String) { 
            			nRes = ((String) o1).compareToIgnoreCase((String) o2);
            		}
            		else if (o1 instanceof DisplayInt) {
            			nRes = ((DisplayInt) o1).compareTo((DisplayInt)o2);
            		}
            		else if (o1 instanceof DisplayFloat) {
            			nRes = ((DisplayFloat) o1).compareTo((DisplayFloat)o2);
            		}
            		else if (o1 instanceof Integer) {
            			nRes = ((Integer) o1).compareTo((Integer)o2);
            		}
            		else if (o1 instanceof Double) {
            			nRes = ((Double) o1).compareTo((Double)o2);
            		}
            		else if (o1 instanceof Boolean) {
            			nRes = ((Boolean) o1).compareTo((Boolean)o2);
            		}
            		else if (o1 instanceof Float) {
            			nRes = ((Float) o1).compareTo((Float)o2);
            		}
            		else if (o1 instanceof Long) {
            			nRes = ((Long) o1).compareTo((Long)o2);
            		}
            		else { // CAS304 was not checking types
            			//Comparable c1 = (Comparable)o1;
    	                //Comparable c2 = (Comparable)o2;
    	                //nRes = c1.compareTo( c2 );
            			Out.PrtWarn("No type for column: " + dbName + " " + o1.getClass().getName());
            		}
	            }
	            
				// Invert the result if this is descending
	    		if ( !bInAscending )
	    			nRes = -nRes;
	    		
	    		return nRes;
	    	}
		};
		
		// tempRows just has line number && object to sort
		// tempRows2 saves the rows to put in order in rows
		// otherwise, have to keep splitting on line delimiter during sort and finding sorting column
		Vector<Object[]> tempRows = new Vector<Object[]>(nTotalRows);
		String[] tempRows2 = new String[nTotalRows];
		try {
			int lineNum = 0;
			for (String line : tableRows) {
				tempRows.add(new Object[] {lineNum, fieldObj.extractFieldByID(line, nNewSortField)});
				tempRows2[lineNum] = line;
				lineNum++;
			}
		}
		catch (Exception e) {ErrorReport.reportError(e, "Internal error: sorting columns");}
	  	tableRows.clear();
	  	
	    Collections.sort( tempRows, sortRows );
	  
		for (Object[] row : tempRows) {
			String line = tempRows2[(Integer)row[0]];
			tableRows.add(line);
		}
		tempRows = null;	
		tempRows2 = null;	
		
        notifyTableRowsChanged ();
        tableSelectionChanged ();
	}
	private void notifyTableRowsChanged ( ){
        TableModelEvent eventAllRows = new TableModelEvent ( this );
        TableModelListener curListener;
        
        for ( int i = 0; i < tableModelListeners.size(); ++i ) {
        	curListener = tableModelListeners.get(i);
        	curListener.tableChanged(eventAllRows);
        }		
	}	
	private void tableSelectionChanged ( ){
        if ( actionListeners == null ) return;
		
		// Notify listeners:
        ActionEvent e = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, "SelectionChanged" );
        ActionListener curListener;
        
        for ( int i = 0; i < actionListeners.size(); ++i ) {
        	curListener = actionListeners.get(i);
        	curListener.actionPerformed( e );
        }		
	}	
	/*****************************************
	 * Public methods
	 */
	public void setSelectedRows(int start, int end) {
		setRowSelectionInterval(start, end);
	}
	public void addDoubleClickListener(ActionListener l) {
		if ( !doubleClickListeners.contains( l ) )
			doubleClickListeners.add ( l ); 		
	}
	public void addSelectionChangeListener(ActionListener l) { 
		if ( !actionListeners.contains( l ) )
			actionListeners.add ( l ); 
	};
	public TableCellRenderer getCellRenderer(int row, int column) {
		rendererWrapper.setRenderer( super.getCellRenderer ( row, column ) );
		return rendererWrapper;
	}
	/*************************************************************
	 * Sort class
	 */
	private class SortableCellRenderer implements TableCellRenderer{
		void setRenderer ( TableCellRenderer inRenderer ){
			renderer = inRenderer;
		}
		public Component getTableCellRendererComponent (JTable table,
				Object value, boolean isSelected, boolean hasFocus,
				int row, int column )
		{
	
			boolean bHeader = (row == -1);
			boolean bBlueBG = (row % 2 == 1);
			
			JLabel label = (JLabel)renderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );				
			if ( bHeader ) {
				label.setBorder( BorderFactory.createEtchedBorder() );
				label.setBackground ( Globalx.BGCOLOR );
			}
			else {
				label.setBorder( null );
			
				if ( isSelected )
					label.setBackground ( Globalx.selectColor );
				else {
					if ( bBlueBG ) label.setBackground( Globalx.altRowColor );
					else label.setBackground( null );
				}
			}
			/* CAS322 */
			if (!bHeader && DisplayDecimalTab.isHighPval()) {
				int absColumn =  getMappedColumn(column)-1;	// remove Row#
				if (absColumn>0) {
					String dbName = fieldObj.getDBcolumnByIdx(absColumn);
					
					if (dbName!=null && dbName.startsWith(Globals.PVALUE)) { 
						Object obj = getValueAt(row, column);
						
						if (obj!=null) {
				        	if (obj instanceof DisplayFloat) { // Could be '-' or 'NA'
								double theVal = ((DisplayFloat) obj).getValue();
								Color high = DisplayDecimalTab.getPvalColor(theVal);
					        	if (high!=null) label.setBackground(high);
							}
						}
					}
				}
			}
		
			label.setFont( theFont );
			return label;
		}				
		TableCellRenderer renderer = null;
	};
	/************** Implementation of TableModel interface *******************/
	public Class<?> getColumnClass(int columnIndex) { 
		try {
			return Class.forName( "java.lang.String" ); 
		}
		catch ( Exception err ) {
			return null;
		}
	}
	public int getColumnCount() { 
		if ( fieldObj == null ) return 0;
        else if ( visibleColumns != null ) return visibleColumns.length + 1;	
		else return fieldObj.getNumFields() + 1;	
	};   
	public String getColumnName( int columnIndex ){
		if ( fieldObj == null ) return "";
		else {
			if (columnIndex-- == 0) return "Row #"; 
			String strReturn = fieldObj.getFieldNameByID( getFieldIdxFromColumnIdx ( columnIndex ) ); 
			return strReturn;
		}
	}
	
	public int getDataRowCount() { return tableRows.size();};
	
	public int getRowCount() { return tableRows.size();};
	
	public Object getValueAt( int rowIndex, int columnIndex ){	
		
		if (  rowIndex > nTotalRows || nTotalRows == 0) return null;
		
		try {
			int mapcol = getMappedColumn(columnIndex); 
			if (mapcol-- == 0)  return rowIndex+1; // Row#
			
			String line = tableRows.get(rowIndex);
			int fieldIdx = getFieldIdxFromColumnIdx ( mapcol );
	        Object obj = fieldObj.extractFieldByID ( line,  fieldIdx);
	        
	        String dbName = fieldObj.getDBNameByID(fieldIdx); 
	     
	        boolean isPval = (dbName != null && dbName.startsWith(Globals.PVALUE)) ? true : false;
	        
	        obj = convertNull ( obj, isPval );
      
	        return obj;
		}
		catch (Exception e) {return null;}
			
	}
	private int getFieldIdxFromColumnIdx ( int columnIndex ){
        if ( columnIndex < 0 )
            return columnIndex;
        
        if ( visibleColumns != null && columnIndex<visibleColumns.length)
            return visibleColumns [ columnIndex ];     
        else
            return columnIndex;
    }
	private Object convertNull ( Object obj, boolean isPval ){ // CAS330 add isPval
        if ( obj instanceof Integer ) {
            Integer val = (Integer)obj;
            if ( val.intValue() == Integer.MIN_VALUE ) obj = null;
        }
        else if ( obj instanceof DisplayFloat ) { // CAS330 add DisplayFloat check
        	if (isPval) {
        		Double val = Math.abs(((DisplayFloat) obj).getValue());
        		if      (val==Globalx.dNoDE) obj = Globalx.sNoVal;
        		else if (val==Globalx.dNaDE) obj = Globalx.sNullVal;
        	}
        }
        else if ( obj instanceof Float ) {
            Float val = (Float)obj;
            if ( Math.abs( val.floatValue() ) == Float.MAX_VALUE ) obj = null;
        }
        else if ( obj instanceof Double ) {
            Double val = (Double)obj;
            if ( Math.abs( val.doubleValue() ) == Double.MAX_VALUE ) obj = null;
        }
        return obj;
    }
	public Object getRowAt(int nRow) {
		if (tableRows == null || nRow < 0 || nRow >= nTotalRows) return null;
		
		String line = tableRows.get(nRow);
		Object[] row = line.split(ROW_DELIMITER);
		return row;
	}
	public int getSelectedRow() {
		int nRow = super.getSelectedRow();
		if (nRow >= nTotalRows) return -1;
		return nRow;
	}
	public int[] getSelectedRows() {return super.getSelectedRows();}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)  { super.setValueAt(aValue, rowIndex, columnIndex); } // never called
	
	public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
	
	public void removeTableModelListener(TableModelListener l) {
		// tableModelListeners.remove( tableModelListeners ); CAS304
	}
	public void addTableModelListener(TableModelListener l) { 
		if ( !tableModelListeners.contains( l ) )
				tableModelListeners.add ( l ); 
	}
    /******************************************************************
     * EXPORT CODE - code is in MainTable
     **************************************************************/
    public String copyTableToString() {return "";}
    public int saveToFileTabDelim(File f) {return 0;}
    public int saveToFasta(File f, Connection conn) {return 0;}
    public int saveORFToFasta(File f, Connection conn) {return 0;}
    public void saveToFileGO(File f, String level, String eval, Connection conn) { return ;}
    public void saveToFilePCC(File f, String pcc, String [] libraries) {return;}
    public void saveToFilePCC2(File f, String pcc, String [] libraries) {return;}
    public int saveToFileCounts(File f, Connection conn, String [] libraries) {return 0;}
   
    /** Private variables **/
    private static final long serialVersionUID = 1;
	private ColumnListener colListener = null;

	private class ColumnListener implements TableColumnModelListener {
    	public ColumnListener(JTable table, int numColumns) {
    		colMap = new int[numColumns+1];
    		
    		for(int x=0; x<=numColumns; x++)
    			colMap[x] = x;
    	}
		public void columnMoved(TableColumnModelEvent e) {
		    int fromIndex = e.getFromIndex();
		    int toIndex = e.getToIndex();

		    if(fromIndex != toIndex)
		    	swap(fromIndex, toIndex);
		}	
		public int getFieldPositionFromColumn(int column) {
			if (column<0) {
				return colMap[0];
			}
			return colMap[column];
		}	
		public boolean hasColumnMoved(int column) {
			return colMap[column] == column;
		}
		
		public int getReverseFieldPositionFromColumn(int column) {
			int retVal = -1;
			for(int x=0; x<colMap.length && retVal<0; x++)
				if(colMap[x] == column)
					retVal = x;
			return retVal;
		}
		public void columnAdded(TableColumnModelEvent arg0) {}
		public void columnMarginChanged(ChangeEvent arg0) {}
		public void columnRemoved(TableColumnModelEvent arg0) {}
		public void columnSelectionChanged(ListSelectionEvent arg0) {}
		private void swap(int x, int y) {
			colMap[x] ^= colMap[y];
			colMap[y] ^= colMap[x];
			colMap[x] ^= colMap[y];
		}
		private int [] colMap = null;
    }
	public void clearTable() {
		tableRows.clear();
		tableModelListeners.clear();
		doubleClickListeners.clear();
	}
	/************************************************
	 * Private variables
	 */
	private Font theFont = Globalx.textFont;
	private SortableCellRenderer rendererWrapper = new SortableCellRenderer ();
	private SortableCellRenderer headerRenderer = new SortableCellRenderer ();
	private FieldMapper fieldObj = null;
	private ArrayList<TableModelListener> tableModelListeners = new ArrayList<TableModelListener> ();
	private ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener> ();
	private ArrayList<ActionListener> doubleClickListeners = new ArrayList<ActionListener> ();
	 
	protected Vector <String> tableRows = new Vector <String> (); // Strings delimited by "\t" 	
    protected int nTotalRows;
	protected int[] visibleColumns = null;
  
    protected boolean [] colAscend = null;
	protected boolean [] bAscend = null;
}