package sng.viewer.panels;

/************************************************************
 * Results tab on left of main panel. Used by Results (Sequence) and Pair Results
 * CAS334 	Sequence Detail nodes are added and removed from Summary.
 * 			The data structure was changed to RowData class
 */
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.util.Vector;

import sng.database.Globals;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.ui.UserPrompt;
import util.methods.Out;
import util.methods.Static;

public class ResultsSummaryTab extends Tab
{
	private final String helpHTML =  Globals.helpDir + "ResultsSummaryTab.html";
	private final String [] colNames = new String [] {"Label", "Summary"}; 
	
	private static final long serialVersionUID = -8340849413126018332L;
	public ResultsSummaryTab (String name, STCWFrame parentFrame)
	{
		super(parentFrame, null);
		super.setBackground(Color.WHITE);
		title = name; 
		theMainFrame = parentFrame;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
		
		btnClearSel = Static.createButton("Remove Selected Result", false);
		btnClearSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) { 
;				removeSelected();
			}
		});
		btnClearAll = Static.createButton("Remove All Results", false);
		btnClearAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) { 
				removeAllRows();
			}
		});	
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), title + " Results", helpHTML);
			}
		});
		
		JPanel buttonPanel = Static.createRowPanel();
		buttonPanel.add(btnClearSel);	buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnClearAll);	buttonPanel.add(Box.createHorizontalStrut(5));
		
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnHelp); 
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		lblTitle = Static.createLabel(title + " Results");
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		txtDesc = new JTextArea("Remove a label removes it from left panel and this table.");

		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setEditable(false);
		lblError = Static.createLabel("The filtered result has been closed.");
		lblError.setVisible(false);
		lblError.setForeground(Color.RED);
		
		table = new JTable();
		table.setColumnSelectionAllowed( false );
		table.setCellSelectionEnabled( false );
		table.setRowSelectionAllowed( true );
		table.setShowHorizontalLines( false );
		table.setShowVerticalLines( true );	
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
		table.setIntercellSpacing ( new Dimension ( 1, 0 ) );		
		rowsVec = new Vector<RowData>();
		table.setModel(new ResultsTableModel());
		table.getTableHeader().setBackground(Color.WHITE);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1)
					btnClearSel.setEnabled(true);
				else if (e.getClickCount() == 2) {
					int row = table.getSelectedRow();
					Tab tab = rowsVec.get(row).tab;
					if (tab != null) {
						String name = rowsVec.get(row).name;
						getParentFrame().menuTree.setSelectedNode(name);
						getParentFrame().tabbedPane.setSelectedTab(tab);
						lblError.setVisible(false);
					}
					else
						lblError.setVisible(true);
				}
			}
		});
		
		scroll = new JScrollPane(table);
		scroll.setBorder( null );
		scroll.setPreferredSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize()); // force table to use all space
		scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		scroll.getViewport().setBackground(Color.WHITE);
		
		add(buttonPanel);	add(Box.createVerticalStrut(20));
		add(lblTitle);
		add(txtDesc);		add(Box.createVerticalStrut(15));
		add(lblError);
		add(scroll);
	}	
	public int addResultSummary(String name, String summary, Tab tab, Tab parentTab) {
		btnClearAll.setEnabled(true);
		lblError.setVisible(false);
		
		RowData newRow = new RowData(name, summary, tab, parentTab);
		
		boolean badd=false;
		if (parentTab!=null) {
			for (int i=0; i<rowsVec.size(); i++) {
				if (rowsVec.get(i).tab==parentTab) {
					rowsVec.insertElementAt(newRow, i+1);
					badd=true;
					break;
				}
			}
		}
		if (!badd) rowsVec.add(newRow);
		table.revalidate();
		
		return rowsVec.size()-1;
	}
	public void removeResultSummary(Tab tab) {	
		for (int idx=0; idx<rowsVec.size(); idx++) {
			if (rowsVec.get(idx).tab == tab) {
				removeChildren(tab);		
				
				rowsVec.remove(idx);	
	
				lblError.setVisible(false);
				table.revalidate();
				return;
			}
		}
	}
	private void removeSelected() {
		int selRow = table.getSelectedRow();
		if (selRow == -1) return;
		
		theMainFrame.removeTab(rowsVec.get(selRow).tab); // remove from left panel
		
		removeChildren(rowsVec.get(selRow).tab);		  // remove children from both
		
		rowsVec.remove(selRow);						  // remove from result table
		
		table.revalidate();
	}
	/************************************************
	 * CAS334: The Summary contains the prefix of the parent node, e.g Filter1: 
	 * so all children with that prefix are removed if the parent is removed.
	 */
	private void removeChildren(Tab ptab) {
		Vector <RowData> remove = new Vector <RowData> ();
		for (RowData rd : rowsVec) {
			if (rd.ptab==ptab) remove.add(rd);
		}
		for (RowData rd : remove) {
			theMainFrame.removeTab(rd.tab);
			rowsVec.remove(rd);
		}
	}
	private void removeAllRows() {
		for (RowData rd : rowsVec) {
			theMainFrame.removeTab(rd.tab);
		}
		rowsVec.clear();
		table.revalidate();

		btnClearAll.setEnabled(false);
		btnClearSel.setEnabled(false);
		lblError.setVisible(false);
		
		revalidate(); repaint();
	}
	/** Tab is about to close, free the associated memory */
	public void close() {
		table = null;
		if(rowsVec != null) rowsVec.clear();
		rowsVec = null;
	}
	
	private class ResultsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;

		public int getColumnCount() {
            return colNames.length;
        }
        public int getRowCount() {
            return rowsVec.size();
        }
        public Object getValueAt(int row, int col) {
            RowData r = rowsVec.elementAt(row);
            if (col==0) return r.name;
            if (col==1) return r.summary;
            return r.tab;
        }
        public String getColumnName(int col) {
            return colNames[col];
        }
	}
	private class RowData { // CAS334 changed from two vectors to a class
		public RowData(String name, String summary, Tab tab, Tab parentTab) {
			this.name = name;
			this.summary = summary;
			this.tab = tab;
			this.ptab = parentTab;
		}
		String name;
		String summary;
		Tab tab, ptab;
	}
	private JTable table = null;
	private JLabel lblTitle = null, lblError = null;
	private JTextArea txtDesc = null;
	private JScrollPane scroll = null;
	private JButton btnClearSel = null, btnClearAll = null, btnHelp = null;
	
	private String title;
	private Vector<RowData> rowsVec = null;
	private STCWFrame theMainFrame=null;
}

