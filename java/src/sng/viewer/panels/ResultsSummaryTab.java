package sng.viewer.panels;

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
import sng.util.MenuTreeNode;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.ui.UserPrompt;

public class ResultsSummaryTab extends Tab
{
	private static final long serialVersionUID = -8340849413126018332L;
	public ResultsSummaryTab ( STCWFrame parentFrame, Tab parentTab, String [] fieldList )
	{
		super(parentFrame, parentTab);
		super.setBackground(Color.WHITE);
		theParentFrame = parentFrame;
		colNames = fieldList;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
		
		btnClearSel = new JButton("Remove Selected Result");
		btnClearSel.setEnabled(false);
		btnClearSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) { 
				int selRow = table.getSelectedRow();
				if (selRow != -1) {	
					String name = rows.get(selRow).get(0);
					theParentFrame.removeResult(name);
					rows.remove(selRow);
					tabs.remove(selRow);
					table.revalidate();
				}
			}
		});
		
		btnClearAll = new JButton("Remove All Results");
		btnClearAll.setEnabled(false);
		btnClearAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) { 
				clearAll();
			}
		});
		
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), "List Filtered Results", "html/viewSingleTCW/ResultsSummaryTab.html");
			}
		});
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout ( buttonPanel, BoxLayout.X_AXIS ));
		buttonPanel.setBackground(Color.WHITE);
		buttonPanel.add(btnClearSel);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnClearAll);
		buttonPanel.add(Box.createHorizontalStrut(5));
		
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnHelp); 
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		lblTitle = new JLabel("List Filtered Results");
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		txtDesc = new JTextArea(
				"Remove a result removes it from left panel and this table.");

		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setEditable(false);
		lblError = new JLabel("The filtered result has been closed.");
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
		rows = new Vector<Vector<String>>();
		tabs = new Vector<Tab>();	
		table.setModel(new ResultsTableModel());
		table.getTableHeader().setBackground(Color.WHITE);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1)
					btnClearSel.setEnabled(true);
				else if (e.getClickCount() == 2) {
					int row = table.getSelectedRow();
					Tab tab = (Tab)tabs.get(row);
					if (tab != null) {
						String name = rows.get(row).get(0);
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
		
		add(buttonPanel);
		add(Box.createVerticalStrut(20));
		add(lblTitle);
		add(txtDesc);
		add(Box.createVerticalStrut(15));
		add(lblError);
		add(scroll);
	}
	
	public int addResultSummary(String name, Tab tab, MenuTreeNode node, String summary) {
		btnClearAll.setEnabled(true);
		lblError.setVisible(false);
		
		Vector<String> newRow = new Vector<String>();
		newRow.add(name);
		newRow.add(summary);
		
		rows.add(newRow);
		tabs.add(tab);
		table.revalidate();
		
		return rows.size()-1;
	}
	
	public void removeResultSummary(Tab tab) {	
		int index = tabs.indexOf(tab);
		if (index >= 0) {
			tab.close();
			rows.remove(index);
			tabs.remove(index);
			lblError.setVisible(false);
			table.revalidate();
		}
	}
	private void clearAll() {
		while(tabs.size()>0) {
			Tab t = tabs.get(0);
			t.close();
			String name = rows.get(0).get(0);
			theParentFrame.removeResult(name);
			rows.remove(0);
			tabs.remove(0);
		}
		table.revalidate();

		btnClearAll.setEnabled(false);
		btnClearSel.setEnabled(false);
		lblError.setVisible(false);
		
		revalidate(); repaint();
	}
	/**
	 * Tab is about to close, free the associated memory
	 */
	public void close()
	{
		table = null;
		if(rows != null) rows.clear();
		rows = null;
		if(tabs != null) tabs.clear();
		tabs = null;
	}
	
	private class ResultsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;

		public int getColumnCount() {
            return colNames.length;
        }

        public int getRowCount() {
            return rows.size();
        }
        
        public Object getValueAt(int row, int col) {
            Vector<String> r = rows.elementAt(row);
            return r.elementAt(col);
        }
        
        public String getColumnName(int col) {
            return colNames[col];
        }
	}
	
	private JTable table = null;
	private JLabel lblTitle = null;
	private JLabel lblError = null;
	private JTextArea txtDesc = null;
	private JScrollPane scroll = null;
	private JButton btnClearSel = null;
	private JButton btnClearAll = null;
	private JButton btnHelp = null;
	private String[] colNames = null;
	private Vector<Vector<String>> rows = null;
	private Vector<Tab> tabs = null;  
	private STCWFrame theParentFrame=null;
}

