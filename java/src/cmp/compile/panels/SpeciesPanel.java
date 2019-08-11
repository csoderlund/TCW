package cmp.compile.panels;

/*****************************************************
 * Species panel of the main window including the table, which contains all species variables
 * (even if not displayed)
 * The object is created in the CompilePanel
 * The buttons Add, Edit, Remove, Create Database are created 
 *              and actions attached (all but Create Database).
 * Create Database action is called in CompileMain.compileSelectListener
 */
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.sql.ResultSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import cmp.compile.runMTCWMain;
import cmp.database.Globals;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;
import util.database.DBConn;

public class SpeciesPanel extends JPanel {
	private static final long serialVersionUID = -7854401073530999670L;
	
	public SpeciesPanel(CompilePanel parentPanel, EditSpeciesPanel editPanel) {
		theCompilePanel = parentPanel;
		theEditSpeciesPanel = editPanel;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		mainPanel = Static.createPagePanel();
		
		JLabel lblSpecies = new JLabel("1. sTCWdbs (single TCW databases)");
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(lblSpecies);
		
		JPanel row = Static.createRowPanel();
		
		// table
		theTable = new SpeciesTable();
		theTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				//updateTableButtons();
			}
		});
		sPane = new JScrollPane(theTable);
		sPane.setBackground(Globals.BGCOLOR);
		sPane.getViewport().setBackground(Globals.BGCOLOR);
		theTable.getTableHeader().setBackground(Globals.BGCOLOR);
		ScrollBarUI tUI = new BasicScrollBarUI() {
		    protected JButton createDecreaseButton(int orientation) {
		        JButton button = super.createDecreaseButton(orientation);
		        button.setBackground(Globals.BGCOLOR);
		        return button;
		    }

		    protected JButton createIncreaseButton(int orientation) {
		        JButton button = super.createIncreaseButton(orientation);
		        button.setBackground(Globals.BGCOLOR);
		        return button;
		    }
		};
		sPane.getHorizontalScrollBar().setBackground(Globals.BGCOLOR);
		sPane.getVerticalScrollBar().setBackground(Globals.BGCOLOR);
		sPane.getHorizontalScrollBar().setUI(tUI);
		sPane.getVerticalScrollBar().setUI(tUI);
		row.add(sPane);
		row.add(Box.createHorizontalStrut(5));

		// action buttons
		buttonPanel = Static.createPagePanel();
		
		btnAdd = Static.createButton("Add", true, Globals.MENUCOLOR);
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theEditSpeciesPanel.clear();
				theEditSpeciesPanel.enableAll(false);
				
				theEditSpeciesPanel.setVisible(true);
				theCompilePanel.setMainPanelVisible(false);
			}
		});
		buttonPanel.add(btnAdd);
		
		btnEdit = Static.createButton("Edit", false, Globals.MENUCOLOR);
		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row = theTable.getSelectedRow();
				if (row<=-1) JOptionPane.showMessageDialog(null, 
						"Please select a row to edit", "Edit", JOptionPane.PLAIN_MESSAGE);
				else {
					theEditSpeciesPanel.setValues(theTable.getSTCWidAt(row),
							theTable.getDBNameAt(row),
							theTable.getPrefixAt(row), 
							theTable.getRemarkAt(row),
							theTable.isAAdb(row),
							theTable.getTypeAt(row));
					theEditSpeciesPanel.enableAll(theCompilePanel.dbIsExist());
					
					theEditSpeciesPanel.setVisible(true);
					theCompilePanel.setMainPanelVisible(false);
				}
			}
		});
		buttonPanel.add(Box.createVerticalStrut(3));
		buttonPanel.add(btnEdit);
		
		btnRemove = Static.createButton("Remove", false, Globals.MENUCOLOR);
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int row = theTable.getSelectedRow();
				if (row<=-1) JOptionPane.showMessageDialog(null, 
						"Please select a row to edit", "Edit", JOptionPane.PLAIN_MESSAGE);
				else {
					theTable.removeRow(row);
					((AbstractTableModel)theTable.getModel()).fireTableDataChanged();
					theCompilePanel.mTCWcfgSave();
				}
			}
		});
		
		Dimension d = btnRemove.getPreferredSize();
		btnRemove.setMaximumSize(d); btnRemove.setMinimumSize(d);
		btnEdit.setMaximumSize(d);   btnEdit.setMinimumSize(d);
		btnAdd.setMaximumSize(d);    btnAdd.setMinimumSize(d);
		buttonPanel.add(Box.createVerticalStrut(3));
		buttonPanel.add(btnRemove);
		
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
		row.add(buttonPanel);
		d = row.getMaximumSize();
		d.height = buttonPanel.getMaximumSize().height;
		row.setMaximumSize(d);
		
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(row);
		add(mainPanel);
		add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		btnBuildDatabase = Static.createButton("Build Database", true);
		btnBuildDatabase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (checkInput())
					theCompilePanel.execBuildDatabase(); // build code is in CompileMain.buildDatabase
			}
		});	
		row.add(btnBuildDatabase);
		
		btnBuildGO = Static.createButton("Add GOs", true);
		btnBuildGO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				theCompilePanel.execBuildGO(); // build code is in CompileMain.buildDatabase
			}
		});	
		row.add(Box.createHorizontalStrut(10));
		row.add(btnBuildGO);	
		
		add(row);
	}
	// Called after mTCW.cfg load, and on Build Database
	public boolean checkInput() {
		// check sTCW databases exists
		
		// check if protein file is correct.
		return true;
	}
	public String getPrefixForAssembly(String assembly) {
		for(int x=0; x<theTable.getRowCount(); x++) {
			if(((String)theTable.getValueAt(x, 0)).equals(assembly)) {
				return (String)theTable.getValueAt(x, 1);
			}
		}
		return null;
	}
	
	public int getSelectedRow() { return theTable.getSelectedRow(); }
	public void resizeEvent(ComponentEvent e) {}
	
	public int getNumRows() { return theTable.getRowCount(); }
	public String getDBNameAt(int row) { return Globals.STCW + "_" + theTable.getDBNameAt(row); }
	public String getSTCWidAt(int row) { return theTable.getSTCWidAt(row); }
	public String getPrefixAt(int row) { return theTable.getPrefixAt(row); }
	public String getRemarkAt(int row) { return theTable.getRemarkAt(row); }
	public boolean isPeptideDBAt(int row) { return theTable.isAAdb(row); }
	
	public void addRow(String dbName, String stcw, String prefix, 
			String remark, boolean isAA) {
		dbName = dbName.replace(Globals.STCW + "_", "");
		theTable.addRow(dbName, stcw, prefix,  remark,  isAA);
	}
	
	public void setRow(int row, String dbName, String stcw, String prefix, 
			String remark, boolean isAA) {
		theTable.setRow(row, dbName, stcw, prefix, remark, isAA);
	}
	
	public void updateTable() {
		((AbstractTableModel)theTable.getModel()).fireTableDataChanged();
	}
	
	/***********************************************************/
	public void update(boolean exists) {
		boolean notExists = !exists;
		
		btnAdd.setEnabled(notExists);
		btnRemove.setEnabled(notExists);
		
		btnBuildDatabase.setEnabled(notExists);
		if (exists) {
			if (theCompilePanel.getDBInfo().hasGOs()) btnBuildGO.setEnabled(false);
			else 	btnBuildGO.setEnabled(true);
		}
		else btnBuildGO.setEnabled(false);
		
		theTable.setEnabled(true);
		btnEdit.setEnabled(true);
	}
	
	public void updateClearInterface() {
		theTable.clearTable();
		theTable.setEnabled(false);
		
		btnAdd.setEnabled(false);
		btnEdit.setEnabled(false);
		btnRemove.setEnabled(false);
		btnBuildDatabase.setEnabled(false);
		btnBuildGO.setEnabled(false);
	}
	public void updateSTCWtype(DBConn mDB, runMTCWMain theCompileMain) {
		try {
			for (int i=0; i<theTable.getRowCount(); i++) {
				
				boolean isAAdb=false;
				
				if (mDB!=null) {
					String stcwid = theTable.getSTCWidAt(i);
					ResultSet rs = mDB.executeQuery("select isPep from assembly where ASMstr = '" + stcwid + "'");
					if (rs.next()) {
						isAAdb = rs.getBoolean(1);
					}
					else {
						String msg = "sTCW database with STCWid '" + stcwid + "' not in mTCWdb";
						UserPrompt.showWarn(msg);
						Out.PrtError(msg);
						continue;
					}
				}
				else {
					String dbName = theTable.getDBNameAt(i);
					String stcwdb = Globals.STCW + "_" + dbName;
					DBConn sDB = theCompileMain.getDBCstcw(stcwdb);
					if (sDB==null) {
						String msg = "sTCW database does not exist: " + stcwdb;
						UserPrompt.showWarn(msg);
						Out.PrtError(msg);
						continue;
					}
					
					if (sDB.tableColumnExists("assem_msg", "peptide")) isAAdb=true;
					sDB.close();	
				}
				theTable.setType(i, isAAdb);
			}
			if (mDB!=null) mDB.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting stcw database types"); }
	}
	/***************************************************************************
	 * XXX The Table class
	 */
	private class SpeciesTable extends JTable {
		private static final long serialVersionUID = -4484804841714567031L;

		private final String [] COLUMN_NAMES = { "singleTCW", "STCWid", "Type", "Remark" };

		public SpeciesTable() {
	    		theModel = new SpeciesTableModel();
	    	
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
	       	
	       	for (int i=0; i<3; i++ ) {
		       	TableColumn column = getColumnModel().getColumn(i);
		       	column.setMaxWidth(column.getPreferredWidth());
		       	column.setMinWidth(column.getPreferredWidth());
	       	}
		}
		
		public int getRowCount() {return theModel.getRowCount(); }
		
		public void addRow(String database, String stcwid, String prefix,  String remark, 
				boolean isAA) {
			theModel.addRow(database, stcwid, prefix,  remark, isAA);
		}
		
		public void setRow(int row, String database, String stcwid, String prefix, String remark, boolean isPeptide) {
			theModel.setRow(row, database, stcwid, prefix, remark, isPeptide);
		}
		
		public void setType(int row, boolean isAAdb) {
			theModel.setType(row, isAAdb);
		}
		public void removeRow(int row) {theModel.removeRow(row);}
		
		public void clearTable() {
			theModel.clearTable();
			theModel.fireTableDataChanged();
		}
		
		public Object getValueAt(int row, int column) {return theModel.getValueAt(row, column);}
		
		public String getDBNameAt(int row) { return theModel.getDBNameAt(row); }
		public String getSTCWidAt(int row) { return theModel.getSTCWidAt(row); }
		public String getPrefixAt(int row) { return theModel.getPrefixAt(row); }
		public String getRemarkAt(int row) { return theModel.getRemark(row); }
		public String getTypeAt(int row) { return theModel.getType(row); }
		public boolean isAAdb(int row) { return theModel.isAAdb(row); }
		
		private SpeciesTableModel theModel = null;
		
		private class SpeciesTableModel extends AbstractTableModel {
			private static final long serialVersionUID = 1757554491364875137L;
			
			public int getColumnCount() { return COLUMN_NAMES.length; }
			public int getRowCount() { return rows.length; }
			public String getColumnName(int pos) { return COLUMN_NAMES[pos]; }

			public Object getValueAt(int row, int column) {
				if (column == 0) return rows[row].strDBname;
				if (column == 1) return rows[row].strSTCWid;
				if (column == 2) return rows[row].strType;
				if (column == 3) return rows[row].strRemark;
				return "";
			}
			
			public void addRow(String database, String stcwid, String prefix, 
					String remark,  boolean isAA) {
				RowData [] oldTable = rows;
				
				rows = new RowData[oldTable.length + 1];
				for(int x=0; x<oldTable.length; x++)
					rows[x] = new RowData(oldTable[x].strDBname, oldTable[x].strSTCWid, oldTable[x].strPrefix,
							oldTable[x].strRemark, oldTable[x].bProteinDB);
				rows[rows.length - 1] = new RowData(database, stcwid, prefix, remark, isAA);
			}
			
			public void setRow(int row, String database, String stcw, String prefix, 
					String remark, boolean isAAdb) {
				rows[row].strDBname = database;
				rows[row].strSTCWid = stcw;
				rows[row].strPrefix = prefix;
				rows[row].strRemark = remark;
				rows[row].bProteinDB = isAAdb;
				rows[row].strType = (isAAdb) ? Globals.TypeAA : Globals.TypeNT;
			}
			
			public void setType(int row, boolean isAAdb) {
				rows[row].bProteinDB = isAAdb;
				rows[row].strType = (isAAdb) ? Globals.TypeAA : Globals.TypeNT;
			}
			public void clearTable() {
				rows = new RowData[0];
			}
			
			public void removeRow(int row) {
				RowData [] oldTable = rows;
				int index = 0;
				
				rows = new RowData[oldTable.length - 1];
				for(int x=0; x<oldTable.length; x++) {
					if(x != row) {
						rows[index] = oldTable[x];
						index++;
					}
				}
			}
			public String getDBNameAt(int row) { return rows[row].strDBname; }
			public String getSTCWidAt(int row) { return rows[row].strSTCWid; }
			public String getPrefixAt(int row) { return rows[row].strPrefix; }
			public String getRemark(int row) { return rows[row].strRemark; }
			public String getType(int row) {return rows[row].strType;}
			public boolean isAAdb(int row) { return rows[row].bProteinDB; }
			private RowData [] rows = new RowData[0];
			
			private class RowData {
				public RowData(String database, String id, String prefix, String remark, boolean isPeptideDB) {
					strDBname = database;
					strSTCWid = id;
					strPrefix = prefix;
					strRemark = remark;
					bProteinDB = isPeptideDB;
					strType = (bProteinDB) ? Globals.TypeAA : Globals.TypeNT;
				}
				private String strDBname = "";
				private String strPrefix = "";
				private String strSTCWid = "";
				private String strRemark = "";
				private String strType="";
				private boolean bProteinDB = false;
			}
		}
	}
	private CompilePanel theCompilePanel = null; 
    private EditSpeciesPanel theEditSpeciesPanel;
	private SpeciesTable theTable = null;
	
	private JScrollPane sPane = null;
	private JPanel mainPanel = null, buttonPanel = null;
	private JButton btnAdd = null, btnEdit = null, btnRemove = null;
	private JButton btnBuildDatabase = null, btnBuildGO = null;
}
