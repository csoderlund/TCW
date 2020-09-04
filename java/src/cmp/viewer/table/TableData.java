package cmp.viewer.table;
/**********************************************
 * Initialized table per Data type
 * Write fields - FIXME - make -2 blanks
 * Sorts
 */
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.JTextField;

import util.methods.ErrorReport;
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.seq.SeqsTablePanel;
import cmp.viewer.pairs.PairTablePanel;

public class TableData implements Serializable {
    private static final long serialVersionUID = 8279185942173639084L;    
	private static final long DISPLAY_INTERVAL = 100; 
	
	 public TableData(GrpTablePanel parent) {
	    	vData = new Vector<Vector<Object>>();
	    	vHeaders = new Vector<TableDataHeader>();
	    	theGrpTable = parent;
	 }
	 public TableData(PairTablePanel parent) {
	    	vData = new Vector<Vector<Object>>();
	    	vHeaders = new Vector<TableDataHeader>();
	    	thePairTable = parent;
	 }
	
	 public TableData(SeqsTablePanel parent) {
	    	vData = new Vector<Vector<Object>>();
	    	vHeaders = new Vector<TableDataHeader>();
	    	theSeqTable = parent;
	 }
	 
	 public static TableData createModel(String [] columns, TableData source, GrpTablePanel parent) {
		TableData retVal = new TableData(parent);
		createModel(columns, source, retVal);
		return retVal;
	}
	 public static TableData createModel(String [] columns, TableData source, PairTablePanel parent) {
			TableData retVal = new TableData(parent);
			createModel(columns, source, retVal);
			return retVal;
	}

	 public static TableData createModel(String [] columns, TableData source, SeqsTablePanel parent) {
			TableData retVal = new TableData(parent);
			createModel(columns, source, retVal);
			return retVal;
	}
	
	private static void createModel(String [] columns, TableData source, TableData retVal) {
		retVal.arrData = new Object[source.arrData.length][columns.length];
		retVal.arrHeaders = new TableDataHeader[columns.length];
		
		for(int x=0; x<columns.length; x++) {
			int sourceColumnIdx = source.getColumnHeaderIndex(columns[x]);
			retVal.arrHeaders[x] = source.arrHeaders[sourceColumnIdx];
			for(int y=0; y<source.arrData.length; y++) {
				retVal.arrData[y][x] = source.arrData[y][sourceColumnIdx];
			}
		}
		retVal.strCacheName = source.strCacheName;
		retVal.bReadOnly = source.bReadOnly;
	}

	public static String [] orderColumns(JTable sourceTable, String [] selectedColumns) {
		String [] retVal = new String[selectedColumns.length];
		Vector<String> columns = new Vector<String> ();
		for(int x=selectedColumns.length-1; x>=0; x--)
			columns.add(selectedColumns[x]);
		
		int targetIndex = 0;
		for(int x=0; x<sourceTable.getColumnCount(); x++) {
			String columnName = sourceTable.getColumnName(x);
			
			int columnIdx = columns.indexOf(columnName);
			if(columnIdx >= 0) {
				retVal[targetIndex] = columnName;
				targetIndex++;
				columns.remove(columnIdx);
			}
		}		
		while(columns.size() > 0) {
			retVal[targetIndex] = columns.get(0);
			columns.remove(0);
			targetIndex++;
		}		
		return retVal;
	}
	
    public void sortMasterList(String columnName, boolean ascending) {
    		if (theGrpTable!=null) theGrpTable.sortMasterColumn(columnName, ascending);
    		else if (thePairTable!=null) thePairTable.sortMasterColumn(columnName, ascending);
    		else if (theSeqTable!=null) theSeqTable.sortMasterColumn(columnName, ascending);
    }

    public void setColumnHeaders(String [] columnNames, Class<?> [] columnTypes) {
    		vHeaders.clear();

	    	for(int x=0; x<columnNames.length; x++)
	    		addColumnHeader(columnNames[x], columnTypes[x]);
	}
	    
    public void addColumnHeader(String columnName, Class<?> type) {
	    	try {
	    		if(bReadOnly) throw (new Exception());
	    		vHeaders.add(new TableDataHeader(columnName, type));
	    	} 
	    	catch(Exception e) {ErrorReport.reportError(e, "Error adding column");}
	    	catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error adding column", null);}
    }
    
    public void insertColumnHeader(int pos, String columnName, Class<?> type) {
	    	try {
	    		if(bReadOnly) throw (new Exception());
	    		vHeaders.insertElementAt(new TableDataHeader(columnName, type), pos);
	    	} 
	    	catch(Exception e) {ErrorReport.reportError(e, "Error inserting column");}
	    	catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error inserting column", null);}
    }
    
    public int getColumnHeaderIndex(String columnName) {
	    	int retVal = -1, x=0;
	    	
	    	if(bReadOnly) {
	    		for(;x<arrHeaders.length && retVal<0;x++) {
	    			if(arrHeaders[x].getColumnName().equals(columnName))
	    				retVal = x;
	    		}
	    	}
	    	else {
	    		Iterator<TableDataHeader> iter = vHeaders.iterator();
	    		for(;iter.hasNext() && retVal<0; x++) 
	    			if(iter.next().getColumnName().equals(columnName))
	    				retVal = x;
	    	}
	    	
	    	return retVal;
    }
    
	public void addRowsWithProgress(ResultSet rset, FieldData theFields, JTextField progress) {
		 try {
		 	progress.setText("Start displaying rows....");
    		String [] symbols = theFields.getDisplayFieldSymbols();
    		boolean firstRow = true;
    		boolean cancelled = false;
    		
    		while(rset.next() && !cancelled) {	
    			
        		Vector<Object> rowData = new Vector<Object> ();
    			rowData.setSize(symbols.length);
    			if(firstRow)
    				vHeaders.get(0).setColumnClass(Integer.class);
    			
    			for(int x=0; x<symbols.length; x++) {
					Object tempVal = rset.getObject(symbols[x]);
        				
					if(firstRow && tempVal != null) {
						vHeaders.get(x).setColumnClass(tempVal.getClass());
					}
	        		rowData.set(x, tempVal);
	    		}
    			
    			firstRow = false;
    				
    			if(progress != null && (vData.size() % DISPLAY_INTERVAL == 0)) {
    				if (progress.getText().equals("Cancelled")) 
    					cancelled = true;
    				else 
    					progress.setText("Loaded " + vData.size() + " rows...");
    			}
        		if (rowData.size() > 0) vData.add(rowData);
    		}
    		progress.setText("");
    	} 
	 	catch (Exception e) {ErrorReport.reportError(e, "Error building table");}
	    catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error building table", null);}
    }
    
    public boolean isReadOnly() { return bReadOnly; }

    public void showTable() { // CAS304 was finalize, which apparently means something to java that is depreciated
            arrHeaders = new TableDataHeader[vHeaders.size()];
            vHeaders.copyInto(arrHeaders);
            vHeaders.clear();

            arrData = new Object[vData.size()][];
            Iterator<Vector<Object>> iter = vData.iterator();
            int x = 0;
            Vector<Object> tempV;
            while(iter.hasNext()) {
                arrData[x] = new Object[arrHeaders.length];
                tempV = iter.next();
                tempV.copyInto(arrData[x]);
                tempV.clear();
                x++;
            }
            vData.clear();

            bReadOnly = true;
    }

    public Object getValueAt(int row, int column) {
	    	try {
	    		if(!bReadOnly) throw (new Exception());
	    		if (row== -1 || column == -1) System.err.println("Problem with indexes: " + row + " " + column);
	    		return arrData[row][column];
	    	}
	    	catch(Exception e) {ErrorReport.reportError(e, "Error getting table value");}
	    	catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error getting table value", null);}
	    	return null;
    }
    
    public void setValueAt(Object obj, int row, int column) {
	    	try {
	    		if(!bReadOnly) throw (new Exception());
	    		arrData[row][column] = obj;
	    	}
	    	catch(Exception e) {ErrorReport.reportError(e, "Error setting table value");}
	    	catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error setting table value", null);}
    }

    public Object [] getRowAt(int row) {
        try {
             if(!bReadOnly) throw (new Exception());
             return arrData[row];
        }
        catch(Exception e) {ErrorReport.reportError(e, "Error getting table row");}
        catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error getting table row", null);}
        return null;
    }

    public String getColumnName(int column) {
        try {
            if(!bReadOnly) throw (new Exception());
            return arrHeaders[column].getColumnName();
        } 
        catch(Exception e) {ErrorReport.reportError(e, "Error getting table column name");}
        catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error getting table column name", null);}
        return null;
    }

    public Class<?> getColumnType(int column) {
	    	try {
	    		if(!bReadOnly) throw (new Exception());
	    		return arrHeaders[column].getColumnClass();
	    	}
	    	catch(Exception e) {ErrorReport.reportError(e, "Error getting table column type");}
	    	catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error getting table column type", null);}
	    	return null;
    }
    
    public boolean isAscending(int column) {
	    	try {
	    		if(!bReadOnly) throw (new Exception());
	    		return arrHeaders[column].isAscending();
	    	}
	    	catch(Exception e) {ErrorReport.reportError(e, "Error table is not finalized");}
	    	catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error table is not finalized", null);}
	    	return false;
    }

    public int getNumColumns() {
	    	if(bReadOnly)
	    		return arrHeaders.length;
	    	return vHeaders.size();
    }

    public int getNumRows() {
            if(bReadOnly)
                    return arrData.length;
            return vData.size();
    }

    public void sortByColumn(int column, boolean ascending) {
	    	arrHeaders[column].setAscending(ascending);
	    	Arrays.sort(arrData, new ColumnComparator(column));
    }
    
    public void clear() {
	    	arrHeaders = null;
	    	if(arrData != null) {
	    		for(int x=0; x<arrData.length; x++) 
	    			arrData[x] = null;
	    		arrData = null;
	    	}
	    	
	    	vHeaders.clear();
	    	for(int x=0; x<vData.size(); x++)
	    		vData.get(x).clear();
	    	vData.clear();
    }
	
    private class ColumnComparator implements Comparator<Object []> {
	    	public ColumnComparator(int column) {
	    		nColumn = column;
	    	}
		public int compare(Object [] o1, Object [] o2) {
			int retval = 0;
			boolean bInAscending = arrHeaders[nColumn].isAscending();
			boolean isBlank = false;
			
			if (o1[nColumn] == null || o2[nColumn] == null) {
				if (o1[nColumn] == null && o2[nColumn] == null) return 0;
				if (o1[nColumn] == null) return 1;
				if (o2[nColumn] == null) return -1;
			}
			if(o2[nColumn] == null || o2[nColumn].equals("")) {
				isBlank = true;
				retval = -1;
			}
			else if(o2[nColumn].equals("") || o2[nColumn] == null) { 
				isBlank = true;
				retval = 1;
			}
			else if(o1[nColumn].equals(o2[nColumn])) {
				retval = 0;
			}
			else {
				if(arrHeaders[nColumn].getColumnClass() == Integer.class)
					retval = ((Integer)o1[nColumn]).compareTo((Integer)o2[nColumn]);
				else if(arrHeaders[nColumn].getColumnClass() == Long.class) {
					if(o1[nColumn] instanceof String)
						retval = ((Long)o1[nColumn]).compareTo((Long)o2[nColumn]);
					else
						retval = ((Long)o1[nColumn]).compareTo((Long)o2[nColumn]);
				}
				else if(arrHeaders[nColumn].getColumnClass() == Float.class)
					retval = ((Float)o1[nColumn]).compareTo((Float)o2[nColumn]);
				
				else if(arrHeaders[nColumn].getColumnClass() == Double.class) 
					retval = ((Double)o1[nColumn]).compareTo((Double)o2[nColumn]);
				
				else if(arrHeaders[nColumn].getColumnClass() == String.class) 
					retval = ((String)o1[nColumn]).compareToIgnoreCase((String)o2[nColumn]);
				
				else if(arrHeaders[nColumn].getColumnClass() == BigDecimal.class)
					retval = ((BigDecimal)o1[nColumn]).compareTo((BigDecimal)o2[nColumn]);
				
				else if(arrHeaders[nColumn].getColumnClass() == Boolean.class)
					retval = ((Boolean)o1[nColumn]).compareTo((Boolean)o2[nColumn]);
			}

			if(isBlank || bInAscending)
				return retval;
			else
				return retval * -1;
		}
		
		private int nColumn;
    }
    
    private boolean bReadOnly = false;
    private String strCacheName = null;
    
    private TableDataHeader [] arrHeaders = null;
    private Object [][] arrData = null;
   
    private Vector<TableDataHeader> vHeaders = null; //Dynamic data structures
    private Vector<Vector<Object>> vData = null; 
   
    private GrpTablePanel theGrpTable = null;
    private PairTablePanel thePairTable = null;
    private SeqsTablePanel theSeqTable = null;

}