package cmp.viewer.table;

public class TableDataHeader {
    
    public TableDataHeader(String label, Class<?> type) {
            strName = label;
            columnType = type;
    }

    public TableDataHeader(String label,  Class<?> type, String symbol) {// use this one if sort needs to access dynamic filed
            strName = label;
            columnType = Object.class;
            dbSymbol = symbol;
    }

    public String getColumnName() { return strName; }
    public String getColumnSymbol() {return dbSymbol;}

    public Class<?> getColumnClass() { return columnType; }
    public void setColumnClass(Class<?> type) { columnType = type; }
    
    public boolean isAscending() { return bAscending; }
    public void setAscending(boolean ascending) { bAscending = ascending; } 

    public String toString() { return strName; }

    private String strName = null;
    private Class<?> columnType = null;
    private String dbSymbol = null; // Used as 
    private boolean bAscending = false;
}