package sng.viewer.panels.seqDetail;

import java.util.Vector;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

import javax.swing.table.TableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;

import sng.database.Globals;
import sng.dataholders.ContigData;
import sng.dataholders.SNPData;
import util.methods.ErrorReport;

public class SNPTable extends JTable implements TableModel
{
	public SNPTable ( ContigData inTheContig )
	{
		theContig = inTheContig;
		fontMetrics = getFontMetrics ( theFont );
		listSNPs = theContig.getSNPCandidates ();
				
		// Make sure the sort orders of the contig/SNPS are consistent with what the menus show
		theContig.setGroupSortOrder( ContigData.SORT_BY_NAME );
		SNPData.sortByPosition( listSNPs );
		
		// Setup the table	
		setModel ( this );
		setCellSelectionEnabled( false );
		setRowSelectionAllowed( false );
		setColumnSelectionAllowed( true );
		
		setShowHorizontalLines( false );
		setShowVerticalLines( true );		
		setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		setSelectionMode ( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		
		setIntercellSpacing ( new Dimension ( 1, 0 ) );
		
		getColumnModel ().addColumnModelListener
		(	new TableColumnModelListener ()
			{
				public void columnAdded(TableColumnModelEvent e) 
				{
					// JTable seems to re-add all of the columns when the
					// data changed event is sent
					if ( getColumnModel().getColumnCount() == getColumnCount () )
						sizeColumns ();	
				};
				public void columnMarginChanged(ChangeEvent e) {};
				public void columnMoved(TableColumnModelEvent e) {};
				public void columnRemoved(TableColumnModelEvent e) {};
				public void columnSelectionChanged(ListSelectionEvent e) {};
			}	
		);
		
		JTableHeader header = getTableHeader();
		header.setReorderingAllowed( false );
		header.setResizingAllowed( false );
		header.addMouseListener( new MouseAdapter ()
				{
					public void mouseClicked(MouseEvent e) 
					{
							tableHeaderClicked ( e );
					}
				}
		);	
		headerRederer.setRederer( new DefaultTableCellRenderer () );
		header.setDefaultRenderer( headerRederer );
		header.setBackground( null );
		header.setOpaque( false );
				
		sizeColumns ();	
	}
	
	public void addSortChangeListener ( ActionListener l )
	{
		sortChangeListeners.add( l );
	}
	
	public void changeESTSort ( int nNewSort )
	{
		bGrouping = nNewSort == ContigData.SORT_BY_ALLELE; 
		theContig.setGroupSortOrder( nNewSort );		
		nSortColumn = Integer.MIN_VALUE;
		notifyTableChange ();
	}
	
	public void changeSNPSort ( int nNewSort )
	{
		switch ( nNewSort )
		{
			case SORT_SNPS_BY_POSITION :
				SNPData.sortByPosition( listSNPs );
				break;
			case SORT_SNPS_BY_COSEGREGATION :
				SNPData.sortByCoSegregationGroup( listSNPs );
				break;
		}	
		
		sortByColumn ( nSortColumn ); // Just for consistency
		
	}
	
	void sortByColumn ( int nNewColumn )
	{
		int nActualColumn = Math.abs(nNewColumn) - 1;
		
		if ( nActualColumn >= 0 && nActualColumn < listSNPs.size() )
		{
			boolean bAscending = nNewColumn > 0;
			theContig.sortGroupBySNPVector( listSNPs, nActualColumn, bAscending );
			bGrouping = false;
		}
		
    	notifyTableChange ();
	}
	
	public Font getFont ( )
	{
		return theFont;
	}
	
	public void paint ( Graphics g )
	{
		super.paint( g );

		int nTableXStart = 0;
		int nTableXEnd = nTableXStart + getWidth ();
		int nTableYStart = 0;
		int nTableYEnd = nTableYStart + getHeight ();
		
		// Draw divider lines
		g.setColor( Color.BLACK );
		g.drawLine( nTableXStart, nTableYStart, nTableXEnd, nTableYStart );				
		Rectangle rectDivide = getCellRect( HEADER_ROWS - 1, 0, true );
		g.drawLine( (int)rectDivide.getMaxX() - 1, nTableYStart, 
				(int)rectDivide.getMaxX() - 1, nTableYEnd );
		g.drawLine( nTableXStart, (int)rectDivide.getMaxY(), 
				nTableXEnd, (int)rectDivide.getMaxY() );

		// Dividers between groups (if sorted by allele):
		int nLastGroup = -1;
		if ( bGrouping )		
			for ( int i = 0; i < theContig.getNumGroups(); ++i )
			{
				int nCurGroup = theContig.getGroupGroupAt( i );
				if ( nCurGroup != nLastGroup )
				{
					rectDivide = getCellRect( HEADER_ROWS + i - 1, 0, true );
					g.drawLine( nTableXStart, (int)rectDivide.getMaxY(), 
							nTableXEnd, (int)rectDivide.getMaxY() );				
				}
				
				nLastGroup = nCurGroup;
			}
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		// Note: the if statement solves a chick-egg problem 
		// in constructor with JRE 1.5.0  This gets called before the columns
		// are populated causing an array to go out of bounds in the column model.
		if ( getColumnModel().getColumnCount() > 0 )
		{
			super.valueChanged(e); 
		}
	}

	private void tableHeaderClicked( MouseEvent e ) 
	{ 
		try
		{
			// Figure out what column was clicked:
	        JTableHeader h = (JTableHeader)e.getSource();
	        TableColumnModel columnModel = h.getColumnModel();
	        boolean leftClick = SwingUtilities.isLeftMouseButton(e);
	        int viewColumn = columnModel.getColumnIndexAtX( e.getX() );
	        int nNewColumn = columnModel.getColumn(viewColumn).getModelIndex();
	        if ( nNewColumn != 0 )
	        {
				if ( nNewColumn == Math.abs (nSortColumn) ) {
					if(leftClick) {
						nSortColumn = -nSortColumn; // Toggle ascending/descending
					}
					else {
						nSortColumn = Math.abs(nSortColumn) * -1;
					}
				}
				else
					nSortColumn = nNewColumn;
	        	sortByColumn ( nSortColumn );
	        		        	
	        	// Notify listeners
	        	notifySortChange ();
	        }
		}
		catch ( Exception err )
		{
			String s = "Internal error: table header clicked";
			System.err.println(s);
			ErrorReport.reportError(err, s);
		}
	}
	
	
	/************** Implementation of TableModel interface *******************/
	public Class<?> getColumnClass(int columnIndex) 
	{ 
		try 
		{
			return Class.forName( "java.lang.String" ); 
		}
		catch ( Exception err )
		{
			return null;
		}
	}
  
	public int getColumnCount() 
	{ 
		if ( theContig == null )
			return 0;
		else
			return theContig.getSNPCount() + 1;
	};
  
	public String getColumnName( int columnIndex )
	{
		if ( columnIndex == nSortColumn )
			return "\u2227";
		else if ( columnIndex == Math.abs ( nSortColumn ) )
			return "\u2228";
		else
			return " ";		
	}
  
	public int getRowCount() 
	{ 
		if ( theContig == null )
			return 0;
		else
			return theContig.getNumGroups () + HEADER_ROWS; 
	};
	
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		int nSNPIdx = columnIndex - 1;
		
		if ( rowIndex < HEADER_ROWS )
		{
			// SNP header cell 
			switch ( rowIndex )
			{
				case 0:		
					if ( columnIndex == 0 )
						return " Co-segregation Score";
					else
						return String.valueOf ( 
								getSNPData ( nSNPIdx ).getCoSegregationScore () );
				case 1:	
					if ( columnIndex == 0 )
						return " Co-segregation Group";
					else
						return String.valueOf ( 
								getSNPData ( nSNPIdx ).getCoSegregationGroup () );
				case 2:		
					if ( columnIndex == 0 )
						return " SNP Redundancy";
					else
						return String.valueOf ( 
								getSNPData ( nSNPIdx ).getSNPRedundancy( theContig.getThresholds() ) );
				case 3:		
					if ( columnIndex == 0 )
						return " Base Position";
					else
						return String.valueOf( 
								getSNPData ( columnIndex - 1 ).getPosition() );		
				default:	
					return "Error!";
			}
		}
			
		// Get the sequence
		int nGroupIdx = rowIndex - HEADER_ROWS;
		if ( columnIndex == 0 )
			return " " + theContig.getGroupNameAt( nGroupIdx );
		else
		{
			// Get the base
			int nPosition = getSNPData ( nSNPIdx ).getPosition();
			return String.valueOf( theContig.safeGetGroupBaseAt ( nGroupIdx, nPosition ) );
		}
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)  { }
	
	public boolean isCellEditable(int rowIndex, int columnIndex) { return false; };
	
	public void removeTableModelListener(TableModelListener l)  
	{ 
		tableModelListeners.remove(l);
	};

	public void addTableModelListener(TableModelListener l) 
	{ 
		tableModelListeners.add(l);
	};
	
	public TableCellRenderer getCellRenderer(int row, int column) 
	{
		redererWrapper.setRederer( super.getCellRenderer ( row, column ) );
		return redererWrapper;
	}
	
	private SNPData getSNPData ( int i )
	{
		return (SNPData)listSNPs.get(i);
	}
	
	private class SNPCellRederer implements TableCellRenderer
	{
		void setRederer ( TableCellRenderer inRenderer )
		{
			rederer = inRenderer;
		}
		
		public Component getTableCellRendererComponent (JTable table,
				Object value,
				boolean isSelected,
                boolean hasFocus,
                int row,
                int column )
		{
			boolean bCenter = column > 0;
			boolean bBlueBG = Math.abs (row % 2) == 1;
						
			JLabel label = (JLabel)rederer.getTableCellRendererComponent( table, 
					value, isSelected, hasFocus, row, column );				
			label.setBorder( null );
			if ( bCenter )
				label.setHorizontalAlignment( JLabel.CENTER );
			else
				label.setHorizontalAlignment( JLabel.LEFT );
			
			if ( isSelected )
				label.setBackground ( Globals.selectColor );
			else
			{
				if ( bBlueBG )
					label.setBackground( Globals.altRowColor );
				else
					label.setBackground( null );
			}
			label.setFont( theFont );
			
			String str = label.getText();
			if ( str.equals( "A" ) )
				label.setForeground( aColor );			
			else if ( str.equals( "G" ) )
				label.setForeground( gColor );			
			else if ( str.equals( "C" ) )
				label.setForeground( cColor );			
			else if ( str.equals( "T" ) )
				label.setForeground( tColor );			
			else
				label.setForeground( Color.BLACK );			
			return label;
		}		
		
		TableCellRenderer rederer = null;
	};
	
	private void notifySortChange ()
	{
	    // Notify listeners:
	    ActionEvent event = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, "SortChanged" );
	    
	    for ( int i = 0; i < sortChangeListeners.size(); ++i )
	    {
	    	ActionListener curListener = (ActionListener)sortChangeListeners.get(i);
	    	curListener.actionPerformed( event );
	    }	
	}
	
	private void notifyTableChange ()
	{
	    // Notify listeners:
	    TableModelEvent eventAllRows = new TableModelEvent ( this );
	    TableModelEvent eventColumnHeader = new TableModelEvent ( this, TableModelEvent.HEADER_ROW );
	    TableModelListener curListener;
	    
	    for ( int i = 0; i < tableModelListeners.size(); ++i )
	    {
	    	curListener = (TableModelListener)tableModelListeners.get(i);
	    	curListener.tableChanged(eventAllRows);
	    	curListener.tableChanged(eventColumnHeader);
	    }	
	}
	
	private void setColumnWidth ( int nColumn, int nTextWidth )
	{
    	TableColumn col = getColumnModel().getColumn( nColumn );
    	col.setPreferredWidth( nTextWidth + 7 );
    	col.setMaxWidth ( nTextWidth + 7 );
    }
	
	private int calcColumnWidth ( int nCol, int nMaxRowsToCheck )
	{
		int nMaxWidth = 0;
		for ( int nRow = 0; nRow < getRowCount () && nRow < nMaxRowsToCheck; ++ nRow )
		{
			nMaxWidth = Math.max ( nMaxWidth, fontMetrics.stringWidth( (String) getValueAt (nRow, nCol) ) );
		}
		return nMaxWidth;
	}
	
	private void sizeColumns ()
	{			
		// Setup width of first column based on maximum length of
		// EST name
     	int width = calcColumnWidth ( 0, Integer.MAX_VALUE );
    	setColumnWidth ( 0, width );
    	
    	// Setup other columns based on maximum width of 
    	// the header fields.
    	width = 0;
	    for ( int i = 1; i <  getColumnCount(); ++i )
	    {
	    	int nRowsToCheck = HEADER_ROWS;
	    	width = Math.max( width, calcColumnWidth ( i, nRowsToCheck ) );
	    }
	    
	    for ( int i = 1; i <  getColumnCount(); ++i )
	    {
	    	setColumnWidth ( i, width );
	    }
	}
	
	final public static Color aColor = new Color ( 0xF50029 );
	final public static Color gColor = new Color ( 0x12B600 );
	final public static Color cColor = new Color ( 0xC69E00 );
	final public static Color tColor = new Color ( 0x330099 );
	
	final public static int SORT_SNPS_BY_POSITION = 1;
	final public static int SORT_SNPS_BY_COSEGREGATION = 2;
	
	private Vector<ActionListener> sortChangeListeners = new Vector<ActionListener> ();
	private Vector<TableModelListener> tableModelListeners = new Vector<TableModelListener> ();
	private int nSortColumn = Integer.MIN_VALUE;
	private boolean bGrouping = false;
	private SNPCellRederer redererWrapper = new SNPCellRederer ();
	private SNPCellRederer headerRederer = new SNPCellRederer ();
	private Font theFont = Globals.textFont;
	static private final int HEADER_ROWS = 4;
	private Vector<SNPData> listSNPs;
	private FontMetrics fontMetrics;
	private ContigData theContig;
    
    private static final long serialVersionUID = 1;
}
