package cmp.viewer.table;

/****************************************************
 * Renders the table.
 * Provides formatting of columns
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import cmp.database.Globals;
import util.database.Globalx;

public class SortTable extends JTable implements ListSelectionListener {
	private static final long serialVersionUID = 5088980428070407729L;

	private static final String ROWNUM = FieldData.ROWNUM;
	
    public SortTable(TableData tData) {
        theClickListeners = new Vector<ActionListener> ();
        theDoubleClickListeners = new Vector<ActionListener> ();
        
    		theModel = new SortTableModel(tData);
    	
        setAutoCreateColumnsFromModel( true );
       	setColumnSelectionAllowed( false );
       	setCellSelectionEnabled( false );
       	setRowSelectionAllowed( true );
       	setShowHorizontalLines( false );
       	setShowVerticalLines( true );	
       	setIntercellSpacing ( new Dimension ( 1, 0 ) );
       	setOpaque(true);

       	setModel(theModel);
       	
        addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				if(me.getClickCount() > 1) {
				    ActionEvent e = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, 
				    		"DoubleClickSingleRow" );			    
					Iterator<ActionListener> iter = theDoubleClickListeners.iterator();
					while(iter.hasNext()) {
						iter.next().actionPerformed(e);
					}
				}
				else {
				    ActionEvent e = new ActionEvent ( this, ActionEvent.ACTION_PERFORMED, 
				    		"SinlgeClickRow" );
					Iterator<ActionListener> iter = theClickListeners.iterator();
					while(iter.hasNext()) {
						ActionListener l = iter.next();
						l.actionPerformed(e);
					}					
				}
			}
			
        });
    }  
    
    public void removeListeners() {
	    	theClickListeners.clear();
	    	theDoubleClickListeners.clear();
    }
    
    public void addSingleClickListener(ActionListener l) {
    		theClickListeners.add(l);
    }
    
    public void addDoubleClickListener(ActionListener l) {
    		theDoubleClickListeners.add(l);
    }
    
    public void sortAtColumn(int column, boolean ascending) {
	    	if(!theModel.getColumnName(column).equals(ROWNUM))
	    		theModel.sortAtColumn(column, ascending);
    }
   
    private Component getFormattedComponent(Component comp, int Index_row, int Index_col) {
	    	if(!(comp instanceof JLabel)) return comp;
	    	
        	JLabel compLbl = (JLabel)comp;
    
        	Class<?> cl = getColumnClass(Index_col);
        	String colName = getColumnName(Index_col);
      	
        	//even index, selected or not selected
        	if(isRowSelected(Index_row)) {
        		compLbl.setBackground(bgColorHighlight);
        		compLbl.setForeground(bgColor);
        	}
        	else if (Index_row % 2 == 0) {
        		compLbl.setBackground(bgColorAlt);
        		compLbl.setForeground(txtColor);
        } 
        else {
            	compLbl.setBackground(bgColor);
            	compLbl.setForeground(txtColor);
        }
        	
        	//Both fields use numeric representation for their values for speed
        	if(colName.equals(ROWNUM)) {
        		compLbl.setText("" + (Index_row + 1));
        		compLbl.setHorizontalAlignment(SwingConstants.LEFT);
        	}
        	else if (getValueAt(Index_row, Index_col) == null) {
        		compLbl.setText(Globalx.sNoVal);
        	 	compLbl.setHorizontalAlignment(SwingConstants.RIGHT); 
        	}
        	else if((cl == Integer.class || cl == Long.class)) {
        		String x = compLbl.getText();
        		if (x.equals(Globalx.iStrNoVal)) compLbl.setText(Globalx.sNoVal);
        		else if (x.length() == 0)        compLbl.setText(Globalx.sNoVal);
        	 	compLbl.setHorizontalAlignment(SwingConstants.RIGHT); 
        	}
        	else if (cl == Double.class) {
        		boolean showVal = true;
        		String noVal="-"; 
        		try {
        			double val = ((Double)getValueAt(Index_row, Index_col));
          		
        			if (SeqDEs.contains(colName)) {
        				if (Math.abs(val)>=Globalx.dNoDE) showVal=false;  // 3,-3 no display of DE
        			}
        			// distinguish value not computed versus null value
        			else if (val <= Globalx.dNullVal) {//-2 no value, 1.5 = null value
        				showVal = false; 
        				if (val == Globalx.dNullVal)  noVal=Globalx.sNullVal;  
        			}
        			else if(compLbl.getText().length() == 0) showVal=false;
        			
	        		if (showVal)	compLbl.setText(formatDouble(val, false));
	        		else compLbl.setText(noVal);
	        	 	compLbl.setHorizontalAlignment(SwingConstants.RIGHT); 
	        }
        		catch(Exception e) {compLbl.setText("");}
        	}
         else if (cl == Float.class) { 
        		boolean showVal = true;
        		String noVal=Globalx.sNoVal;
        		try {
        			float val = ((Float)getValueAt(Index_row, Index_col));
        			 
        			boolean isPerc = colName.startsWith("%");  
        			
        			if (colName.equals(FieldData.SCORE1)) {
        				if (val <= Globalx.dNoScore) showVal = false;
        			}
        			else if (val <= Globalx.dNullVal) {
        				showVal = false; // -2 no display for everything else
        				if (val == Globalx.dNullVal)  noVal=Globalx.sNullVal;  
        			}
        			else if(compLbl.getText().length() == 0) showVal=false;
        			
	        		if (showVal)	compLbl.setText(formatDouble(val, isPerc));
	        		else compLbl.setText(noVal);
	        	 	compLbl.setHorizontalAlignment(SwingConstants.RIGHT); 
	        }
        		catch(Exception e) {compLbl.setText("");}
        	}
        	else {
        	   	if(compLbl.getText().length() == 0) compLbl.setText(Globalx.sNoVal);
        	 	compLbl.setHorizontalAlignment(SwingConstants.LEFT); 
        	}
        return compLbl;    		
    }
    
    public Component prepareRenderer(TableCellRenderer renderer,int Index_row, int Index_col) {
	    	Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
	    	return getFormattedComponent(comp, Index_row, Index_col);
    }
    
    public int getColumnHeaderIndex(String columnName) {
	    	for(int x=0; x<getColumnModel().getColumnCount(); x++) {    		
	    		if(((String)getColumnModel().getColumn(x).getHeaderValue()).equals(columnName))
	    			return x;
	    	}
	    	return -1;
    }
 
    public void setDEnames(String [] seq) {
		for (int i=0; i<seq.length; i++) SeqDEs.add(seq[i]);
	}
  
    static DecimalFormat df1 = new DecimalFormat("#0.0###;#0.0###");
    static DecimalFormat df2 = new DecimalFormat("0.#E0;0.##E0");
    
    static public String formatDouble(double val, boolean isP) {
	    	if (val == 0) return "0.000"; 
	    	if (isP) return String.format("%.1f", val); 
	    	
	    	double a = Math.abs(val);
	    	if (a>=10.0)    return String.format("%.1f", val); 
	    	if (a>=1.0)     return String.format("%.2f", val); 
	    	if (a>=0.001)  return String.format("%.3f", val);  
	    	return  String.format("%.1E", val);
	}
   
	private static final int MAX_AUTOFIT_COLUMN_WIDTH = 120; // in pixels
    public void autofitColumns() {
        TableColumn column;
        Component comp;
        int headerWidth;
        int cellWidth;
        TableCellRenderer headerRenderer = getTableHeader().getDefaultRenderer();
        
        for (int i = 0;  i < getModel().getColumnCount();  i++) { // for each column
            column = getColumnModel().getColumn(i);
            
            comp = headerRenderer.getTableCellRendererComponent(
                                 this, column.getHeaderValue(),
                                 false, false, 0, i);
            
            headerWidth = comp.getPreferredSize().width + 10;
            
            cellWidth = 0;
            for (int j = 0;  j < getModel().getRowCount();  j++) { // for each row
	            comp = getDefaultRenderer(theModel.getColumnClass(i)).
	                             getTableCellRendererComponent(
	                                 this, theModel.getValueAt(j, i),
	                                 false, false, j, i);

	            comp = getFormattedComponent(comp, j, i);
	             
	            int width = comp.getMinimumSize().width;
	            if(theModel.getColumnClass(i) == String.class) width += 2;
	            else if(theModel.getColumnClass(i) == Double.class) width += 2; 
	            cellWidth = Math.max(cellWidth, width);
	            
	            if (j > 100) break; // only check beginning rows, for performance reasons
            }         
            column.setPreferredWidth(Math.min(Math.max(headerWidth, cellWidth), MAX_AUTOFIT_COLUMN_WIDTH));
        }
    }
    
    public class SortTableModel extends AbstractTableModel {
	    	private static final long serialVersionUID = -2360668369025795459L;
	
	    	public SortTableModel(TableData values) {
	    		theData = values;
	    	}
	
	    	public boolean isCellEditable(int row, int column) { return false; }
	    	public Class<?> getColumnClass(int columnIndex) { return theData.getColumnType(columnIndex); }
	    	public String getColumnName(int columnIndex) { return theData.getColumnName(columnIndex); }
	    	public int getColumnCount() { return theData.getNumColumns(); }
	    	public int getRowCount() { return theData.getNumRows(); }
	    	public Object getValueAt(int rowIndex, int columnIndex) {
	    		return theData.getValueAt(rowIndex, columnIndex); 
	    	}
	    	public void setValueAt(Object obj, int rowIndex, int columnIndex) { 
	    		theData.setValueAt(obj, rowIndex, columnIndex); 
	    	}
	    	public void sortAtColumn(int columnIndex, boolean ascending) {
	    		theData.sortByColumn(columnIndex, ascending);
	    		this.fireTableDataChanged();
	    		theData.sortMasterList(theData.getColumnName(columnIndex), ascending);
	    	}
	
	    	private TableData theData = null;
    }
    private Color bgColor = Globals.BGCOLOR;
    private Color bgColorAlt = new Color(240,240,255);
    private Color bgColorHighlight = Color.GRAY;
    private Color txtColor = Color.BLACK; 
    
    private HashSet <String> SeqDEs = new HashSet <String> (); 
    private SortTableModel theModel = null;
    private Vector<ActionListener> theClickListeners = null;
    private Vector<ActionListener> theDoubleClickListeners = null;
}