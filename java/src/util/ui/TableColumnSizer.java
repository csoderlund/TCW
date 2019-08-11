package util.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class TableColumnSizer {


	    /**
	     * resizes the columns in a JTable based on the data in that table. data
	     * scanned is limited to the first 25 rows.
	     *
	     * @param table the table with the columns to resize
	     */
	    public static void resizeColumns(JTable table) 
	    {

	        final TableModel model = table.getModel();
	        final int columnCount = model.getColumnCount();
	        final int rowCount = model.getRowCount();
	        final List charactersPerColumn = new ArrayList();

	        // initiazlize character counts
	        final Integer integerZero = Integer.valueOf(0);
	        for (int col = 0; col < columnCount; col++) {
	            charactersPerColumn.add(integerZero);
	        }

	        // scan first 25 rows
	        final int rowsToScan = model.getRowCount();
	        for (int row = 0; row < rowsToScan; row++) {
	            for (int col = 0; col < columnCount; col++) {

	                // character counts for comparison
	                final int existingCharacterCount = ((Integer)charactersPerColumn.get(col)).intValue();
	                final Object cellValue = model.getValueAt(row, col);
	                if (cellValue != null) {
	                    final Integer newCharacterCount = Integer.valueOf(cellValue.toString().length());

	                    // do we need to increase the character count?
	                    if (newCharacterCount.intValue() > existingCharacterCount) {
	                        charactersPerColumn.set(col, newCharacterCount);
	                    }
	                }

	            }
	        }

	        // prepare the table for column resizing
	        final TableColumnModel columnModel = table.getColumnModel();
	        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	        
	        // set maximum character counts
	        final Integer maximumCharacterCount = Integer.valueOf(24);
	        for (int col = 0; col < columnCount; col++) {
	            final int existingCharacterCount = ((Integer)charactersPerColumn.get(col)).intValue();
	            if (existingCharacterCount > maximumCharacterCount.intValue()) {
	                charactersPerColumn.set(col, maximumCharacterCount);
	            }
	        }

	        // set column widths
	        for (int col = 0; col < columnCount; col++) {
	            final int existingCharacterCount = ((Integer)charactersPerColumn.get(col)).intValue();
	            final int columnWidth = 18 + (existingCharacterCount * 7);
	            columnModel.getColumn(col).setPreferredWidth(columnWidth);
	        }        
	        
	    }
	
}
