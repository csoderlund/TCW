package cmp.compile.panels;
/************************************************
 * The Methods section of the runMultiTCW
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.sql.ResultSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import cmp.compile.runMTCWMain;
import cmp.compile.Summary;
import cmp.compile.Pairwise;
import cmp.database.Globals;

import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;

public class MethodPanel extends JPanel {
	private static final long serialVersionUID = -8287618156963989136L;

	public MethodPanel(CompilePanel parentPanel, EditMethodPanel editPanel) {
		theCompilePanel = parentPanel;
		theEditMethodPanel = editPanel;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		
		mainPanel = Static.createPagePanel();
		JPanel row = Static.createRowPanel();
		
		JLabel lblMethods = new JLabel("3. Cluster Sets");
		mainPanel.add(lblMethods);
		mainPanel.add(Box.createVerticalStrut(10));
		
	// Table on left
		theTable = new MethodTable();
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
		
	// actions on right
		addActionPanel = Static.createPagePanel();
		btnAdd = Static.createButton("Add", true, Globals.MENUCOLOR);
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				theEditMethodPanel.resetSettings();
				theEditMethodPanel.setEditMode(false);
				theEditMethodPanel.setLoaded(false);
				
				theCompilePanel.setMainPanelVisible(false);
				theEditMethodPanel.setVisible(true);
			}
		});
		addActionPanel.add(btnAdd);
		
		btnEdit = Static.createButton("Edit", false, Globals.MENUCOLOR);
		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int row = theTable.getSelectedRow();
				
				if (row<=-1) JOptionPane.showMessageDialog(null, 
						"Please select a row to edit", "Edit", JOptionPane.PLAIN_MESSAGE);
				else if (theTable.getLoadedAt(row)) JOptionPane.showMessageDialog(null, 
						"Method in database, cannot be edited", "Edit", JOptionPane.PLAIN_MESSAGE);
				else { 
					theEditMethodPanel.setMethodType(theTable.getMethodTypeAt(row));
					theEditMethodPanel.setPrefix(theTable.getPrefixAt(row));
					theEditMethodPanel.setSettings(theTable.getSettingsAt(row));
					theEditMethodPanel.setLoaded(theTable.getLoadedAt(row));
					theEditMethodPanel.setEditMode(true);
					
					theCompilePanel.setMainPanelVisible(false);
					theEditMethodPanel.setVisible(true);
				}
			}
		});
		addActionPanel.add(Box.createVerticalStrut(3));
		addActionPanel.add(btnEdit);
		
		btnRemove = Static.createButton("Remove", false);
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				removeMethod();
				theCompilePanel.updateAll();
			}
		});
		addActionPanel.add(Box.createVerticalStrut(3));
		addActionPanel.add(btnRemove);
		addActionPanel.setMaximumSize(addActionPanel.getPreferredSize());
		addActionPanel.setMinimumSize(addActionPanel.getPreferredSize());
		
		Dimension d = btnRemove.getPreferredSize();
		btnRemove.setMaximumSize(d); btnRemove.setMinimumSize(d);
		btnEdit.setMaximumSize(d);   btnEdit.setMinimumSize(d);
		btnAdd.setMaximumSize(d);    btnAdd.setMinimumSize(d);
		
		row.add(addActionPanel);
		d = row.getMaximumSize();
		d.height = addActionPanel.getMaximumSize().height;
		row.setMaximumSize(d);
		mainPanel.add(row);
		
		add(Box.createVerticalStrut(10));
		add(mainPanel);	
		add(Box.createVerticalStrut(10));
		
	// Main function
		row = Static.createRowPanel();
		btnAddGroups = Static.createButton("Add New Clusters", true, null);
		btnAddGroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(theCompilePanel.dbIsExist()) {
					theCompilePanel.execAddMethods();
					
					theTable.methodsInDB(true);
					((AbstractTableModel)theTable.getModel()).fireTableDataChanged();
				}
				else System.err.println("Error: Database not loaded");
			}
		});
		row.add(btnAddGroups);
		
		add(row);
		add(Box.createVerticalStrut(5));
	}
	private void removeMethod() {
		try {
			Object [] optionsLoaded = { "Remove from database", 
					"Remove from database and table" };
			
			int row = theTable.getSelectedRow();
			if (row<0) return;
			
			String prefixName = theTable.getPrefixAt(row);

			String result="Remove from table";
			if (theTable.getLoadedAt(row))
				result = (String)JOptionPane.showInputDialog(theCompilePanel, "Select Remove Option", "", 
						JOptionPane.PLAIN_MESSAGE, null, optionsLoaded, "Remove Method");
			
			if(result != null) {
				if(result.equals("Remove from database") || result.equals("Remove from database and table")) {
					Out.PrtSpMsg(1, "Removing Method '" + prefixName + "' from database...");
					removeFromDB(prefixName);
					
					Out.PrtSpMsg(1, "Method removed");
					theTable.methodsInDB(true);
				}
				if(result.equals("Remove from database and table") || result.equals("Remove from table")) {
					theTable.removeRow(theTable.getSelectedRow());
					theCompilePanel.mTCWcfgSave();
				}
				((AbstractTableModel)theTable.getModel()).fireTableDataChanged();
			}
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error removing method");}
	}
	// Cluster removal is also in ProjectPanel
	private void removeFromDB(String methodPrefix) {
		try {
			DBConn mDB = runMTCWMain.getDBConnection(theCompilePanel);

			ResultSet rs = mDB.executeQuery("SELECT PMid, PMtype FROM pog_method " +
					" WHERE prefix='" + methodPrefix + "'");
			if(!rs.next()) {
				Out.PrtWarn("Method does not exist");
				rs.close(); mDB.close();
				return;
			}
			int PMid = rs.getInt(1);
			rs.close();
			
			int nSeq = mDB.executeCount("select count(*) from unitrans");
			Out.PrtSpMsg(2, "Remove column from " + nSeq + " sequence table rows...");
			mDB.tableCheckDropColumn("unitrans", methodPrefix);
			
			int nPair = mDB.executeCount("select count(*) from pairwise");
			Out.PrtSpMsg(2, "Remove column from " + nPair + " pair table rows...");
			mDB.tableCheckDropColumn("pairwise", methodPrefix);// also done in ProjectPanel on remove all pairs and methods
			
			int count = mDB.executeCount("SELECT count(*) FROM pog_method");
			if (count==0) return;
			
			// alignments would have been done for all clusters pairs.
			// PairMap will still the alignments, which is okay....
			if (count==1) { 
				Out.PrtSpMsg(2, "Truncate method, group, member tables...");
				mDB.executeUpdate("TRUNCATE TABLE pog_method");
				mDB.executeUpdate("TRUNCATE TABLE pog_groups");
				mDB.executeUpdate("TRUNCATE TABLE pog_members");
				mDB.executeUpdate("TRUNCATE TABLE pog_scores"); // CAS326 add
				Out.PrtSpMsg(2, "Set flags to zero for all pairs...");
				mDB.executeUpdate("update pairwise set hasGrp=0, hasBBH=0");
			}
			else {
				Out.PrtSpMsg(2, "Remove method...");
				mDB.executeUpdate("DELETE FROM pog_method where PMid = " + PMid);
				count = mDB.executeCount("SELECT MAX(PMid) FROM pog_method");
				mDB.executeUpdate("ALTER TABLE pog_method AUTO_INCREMENT = " + (count+1));
				
				Out.PrtSpMsg(2, "Remove cluster members...");
				mDB.openTransaction(); 
				mDB.executeUpdate("DELETE pog_members.* " +
						"FROM pog_members, pog_groups " +
						"WHERE pog_groups.pgid=pog_members.pgid AND pog_groups.pmid=" + PMid);
				mDB.closeTransaction();
				count = mDB.executeCount("SELECT MAX(MEMid) FROM pog_members");
				mDB.executeUpdate("ALTER TABLE pog_members AUTO_INCREMENT = " + (count+1));
				
				Out.PrtSpMsg(2, "Remove cluster scores..."); // CAS326 add
				mDB.openTransaction(); 
				mDB.executeUpdate("DELETE pog_scores.* " +
						"FROM pog_scores, pog_groups " +
						"WHERE pog_groups.pgid=pog_scores.pgid AND pog_groups.pmid=" + PMid);
				mDB.closeTransaction();
				count = mDB.executeCount("SELECT MAX(PGid) FROM pog_scores");
				mDB.executeUpdate("ALTER TABLE pog_scores AUTO_INCREMENT = " + (count+1));
				
				Out.PrtSpMsg(2, "Remove clusters...");
				mDB.openTransaction(); 
				mDB.executeUpdate("DELETE FROM pog_groups where PMid = " + PMid);
				mDB.closeTransaction();
				count = mDB.executeCount("SELECT MAX(PGid) FROM pog_groups");
				mDB.executeUpdate("ALTER TABLE pog_groups AUTO_INCREMENT = " + (count+1));	
				
				new Pairwise(theCompilePanel).fixFlagsPairwise(mDB); 
			}
			
			new Summary(mDB).removeSummary();
			mDB.close();
		} catch (Exception e) {ErrorReport.die(e, "removing method");}
	}
	
	/*************************************************************/
	public void update(boolean dbExists) {
		if (dbExists) {
			
			if (theCompilePanel.getDBInfo().getCntPair()>0) btnAddGroups.setEnabled(true);
			else btnAddGroups.setEnabled(false);
			
			theTable.methodsInDB(true);  // adds italized
			updateTable();
		}
		else {
			btnAddGroups.setEnabled(false);
			theTable.clearMethodsInDB(); // remove italized
			updateTable();
		}
		btnAdd.setEnabled(true);
		if (theTable.getRowCount()>0) {
			btnEdit.setEnabled(true);
			btnRemove.setEnabled(true);
			theTable.setEnabled(true);
		}
	}
	
	public void updateClearInterface() {
		theTable.clearTable();
		
		btnAddGroups.setEnabled(false);
		
		btnAdd.setEnabled(false);
		btnEdit.setEnabled(false);
		btnRemove.setEnabled(false);
		theTable.setEnabled(false);
	}
	/*************************************************************/
	public int getSelectedRow() { return theTable.getSelectedRow(); }	
	public int getNumRows() { return theTable.getRowCount(); }
	public boolean isMethodLoadedAt(int row) { return theTable.getLoadedAt(row); }
	public String getMethodPrefixAt(int row) { return (String)theTable.getValueAt(row, 0); }
	public String getMethodTypeAt(int row) { return (String)theTable.getValueAt(row, 1); }
	public String getCommentAt(int row) { return (String)theTable.getValueAt(row, 2); }
	public String getSettingsAt(int row) { return (String)theTable.getValueAt(row, 3); }
	
	public void setRow(int row, String methodType, String prefix, String comment, String settings) {
		theTable.setRow(row, methodType, prefix, comment, settings);
	}
	
	public void addRow(String methodType, String prefix, String comment, String settings) {
		theTable.addRow(methodType, prefix, comment, settings);
	}
	
	public void updateTable() {
		((AbstractTableModel)theTable.getModel()).fireTableDataChanged();
		
		if (theTable.getRowCount()>0) { 
			btnEdit.setEnabled(true);
			btnRemove.setEnabled(true);
			theTable.setEnabled(true);
		}
	}
	
	public void resizeEvent(ComponentEvent e) {}

	private class MethodTable extends JTable {
		private static final long serialVersionUID = -4484804841714567031L;

		public MethodTable() {
	    		theModel = new MethodTableModel();
	    	
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
	       	
	       	for (int i=0; i<2; i++ ) {
	       		TableColumn column = getColumnModel().getColumn(i);
	       		if (i==0) {
	       			column.setMaxWidth(50);
	       			column.setMinWidth(50);
	       		}
	       		else if (i==1) {
	       			column.setMaxWidth(column.getPreferredWidth());
	       			column.setMinWidth(column.getPreferredWidth());
	       		}
	       	}
		}
		
		public Object getValueAt(int row, int col) {return theModel.getValueAt(row, col); }
		
		public boolean getLoadedAt(int row) { return theModel.getIsLoadedAt(row); }
		public String getPrefixAt(int row) { return theModel.getPrefixAt(row); }
		public String getMethodTypeAt(int row) { return theModel.getMethodTypeAt(row); }
		public String getSettingsAt(int row) { return theModel.getSettingsAt(row); }
		
		public void addRow(String method, String prefix, String comment, String settings) {
			theModel.addRow(method, prefix, comment, settings);
		}
		
		public void setRow(int row, String method, String prefix, String comment, String settings) {
			theModel.setRow(row, method, prefix, comment, settings);
		}
		
		public void removeRow(int row) {theModel.removeRow(row);}
		
		public void methodsInDB(boolean reload) { theModel.methodsInDB(reload); }
		
		public void clearMethodsInDB() {theModel.clearMethodsInDB();}
		
		public void clearTable() {
			theModel.clearTable();
			theModel.fireTableDataChanged();
		}
			
		private MethodTableModel theModel = null;
		
		private class MethodTableModel extends AbstractTableModel {
			private static final long serialVersionUID = 1757554491364875137L;
			
			public int getColumnCount() { return COLUMN_NAMES.length; }
			public int getRowCount() { return rows.length; }
			public String getColumnName(int pos) { return COLUMN_NAMES[pos]; }

			public boolean getIsLoadedAt(int row) { return rows[row].isLoaded(); }
			public String getMethodTypeAt(int row) { return rows[row].getMethodType(); }
			public String getPrefixAt(int row) { return rows[row].getPrefix(); }
			public String getSettingsAt(int row) { return rows[row].getSettings(); }
			
			public Object getValueAt(int row, int column) {
				if(column == 0) return rows[row].getPrefix();
				if(column == 1) return rows[row].getMethodType();
				if(column == 2) return rows[row].getComment();
				return rows[row].getSettings();
			}
			
			public Class<?> getColumnClass(int col) {
				return String.class;
			}
			
			public void addRow(String methodType, String prefix, String comment, String settings) {
				RowData [] oldTable = rows;
				
				rows = new RowData[oldTable.length + 1];
				for(int x=0; x<oldTable.length; x++)
					rows[x] = oldTable[x];
				rows[rows.length - 1] = new RowData(methodType, prefix, comment, settings);
			}
			
			public void setRow(int row, String methodType, String prefix, String comment, String settings) {
				if (row == -1) return; 
				rows[row].setMethodType(methodType);
				rows[row].setPrefix(prefix);
				rows[row].setComment(comment);
				rows[row].setSettings(settings);
			}
			
			public void clearMethodsInDB() {
				for(int row=0; row<rows.length; row++) {
					rows[row].setLoaded(false);
				}
				theLoadedMethods=null;
			}
			
			public void methodsInDB(boolean loadFromDB) {
				try {
					if (!theCompilePanel.dbIsExist()) return;
					
					if(theLoadedMethods == null || loadFromDB) {
						String dbName = theCompilePanel.getProjectPanel().getDBName();
						if(dbName != null && dbName.length() > 0 ) {
							theLoadedMethods = theCompilePanel.dbLoadedMethodPrefixes(dbName);
						}
					}
					for(int row=0; row<rows.length; row++) {
						boolean isLoaded = theLoadedMethods.contains(rows[row].getPrefix());
						rows[row].setLoaded(isLoaded);
					}
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Error validating method");}
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
			
			public void clearTable() {rows = new RowData[0];}
			
			private final String [] COLUMN_NAMES = { "Prefix", "Method", "Parameters" };
			private RowData [] rows = new RowData[0];
			
			private class RowData {
				public RowData(String methodType, String prefix, String comment, String settings) {
					strMethod = methodType;
					strPrefix = prefix;
					strComment = comment;
					strSettings = settings;
				}
				
				public String getMethodType() { return strMethod; }
				public String getComment() { return strComment; }
				public String getPrefix() { return strPrefix; }
				public String getSettings() { return strSettings; }
				public boolean isLoaded() { return bLoaded; }
				
				public void setMethodType(String methodType) { strMethod = methodType; }
				public void setPrefix(String prefix) { strPrefix = prefix; }
				public void setComment(String comment) { strComment = comment; }
				public void setSettings(String settings) { strSettings = settings; }
				public void setLoaded(boolean isLoaded) { bLoaded = isLoaded; }
				
				private String strPrefix = "";
				private String strMethod = "";
				private String strComment = "";
				private String strSettings = "";
				private boolean bLoaded = false;
			}
		}
	}
	
	private class TheRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 6195418129152499449L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			Font oldFont = c.getFont();
			Font newFont = null;
				
			if(theTable.getLoadedAt(row)) {
				newFont = new Font(oldFont.getFontName(), Font.ITALIC, oldFont.getSize());
				c.setFont(newFont);
			}
			return c;
		}
	}

	private CompilePanel theCompilePanel = null;
	private EditMethodPanel theEditMethodPanel = null;
	private Vector<String> theLoadedMethods = null;
	private JScrollPane sPane = null;
	private JPanel addActionPanel = null;
	private JPanel mainPanel = null;
	private MethodTable theTable = null;
	private JButton btnAdd = null;
	private JButton btnEdit = null;
	private JButton btnRemove = null;
	private JButton btnAddGroups = null;
}
