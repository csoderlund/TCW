package cmp.viewer.panels;

/*************************************************
 * List all results with summaries; allows removal
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import util.ui.UserPrompt;
import util.methods.Static;

import cmp.viewer.MTCWFrame;

public class ResultPanel extends JPanel {
	private static final long serialVersionUID = -4532933089334778200L;
	private static final String helpHTML = "html/viewMultiTCW/Results.html";

	public ResultPanel(MTCWFrame parentFrame) {
		theParentFrame = parentFrame;
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Static.BGCOLOR);
		
		theTable = new JTable();
		theTable.getTableHeader().setBackground(Static.BGCOLOR);
		theTable.setColumnSelectionAllowed( false );
		theTable.setCellSelectionEnabled( false );
		theTable.setRowSelectionAllowed( true );
		// theTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); CAS341
		theTable.setShowHorizontalLines( false );
		theTable.setShowVerticalLines( true );	
		theTable.setIntercellSpacing ( new Dimension ( 1, 0 ) );		
		
		theTable.setModel(new ResultsTableModel(resultVec));
		theTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				updateButtons();
				if (e.getClickCount() == 2) {
					int row = theTable.getSelectedRow();
					theParentFrame.setSelection(resultVec.get(row).panel);
				}
			}
		});
		
		scroll = new JScrollPane(theTable);
		scroll.setBorder( null );
		scroll.setPreferredSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize()); // force table to use all space
		scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		scroll.getViewport().setBackground(Static.BGCOLOR);
				
		add(addButtonPanel());
		add(Box.createVerticalStrut(20));
		add(addLabelPanel());
		add(scroll);
	}
	private JPanel addButtonPanel() {
		JPanel row = Static.createRowPanel();
		
		btnRemoveSelectedPanels = Static.createButton("Remove Selected", false);
		btnRemoveSelectedPanels.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (theTable.getSelectedRowCount()>0)  
					removeSelectedRows(theTable.getSelectedRows());
			}
		});
		row.add(btnRemoveSelectedPanels);
		row.add(Box.createHorizontalStrut(5));
		
		btnRemoveAllPanels = Static.createButton("Remove All", false);
		btnRemoveAllPanels.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeAllPanels();
			}
		});
		row.add(btnRemoveAllPanels);
		row.add(Box.createHorizontalGlue());
		
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Results Help", helpHTML);
			}
		});
		row.add(btnHelp);
		
		return row;
	}
	
	private JPanel addLabelPanel() {
		JPanel thePanel = Static.createPagePanel();
		
		JTextArea instructions = new JTextArea(	"Removing results below will also remove them from the left panel.\n" +
												"Double-click a result to display it.");
				
		instructions.setEditable(false);
		instructions.setBackground(getBackground());
		instructions.setAlignmentX(LEFT_ALIGNMENT);
		
		thePanel.add(instructions);
		thePanel.setMaximumSize(thePanel.getPreferredSize());
		thePanel.setAlignmentX(LEFT_ALIGNMENT);
		thePanel.add(Box.createVerticalStrut(10));
		
		return thePanel;
	}
	
	private void removeSelectedRows(int [] sel) {// CAS341 remove all selected
		for (int x : sel) removeMark(x);
		
		Vector <ResultData> rebuild = new Vector <ResultData> ();
		for (int x=0; x<resultVec.size(); x++) {// remove from vector
			if (resultVec.get(x).remove) 
				theParentFrame.removePanelFromMenuOnly(resultVec.get(x).panel);
			else 
				rebuild.add(resultVec.get(x));
		}
		resultVec.clear();
		for (ResultData rd : rebuild) resultVec.add(rd);
		
		theTable.clearSelection();
		theTable.revalidate();
		updateButtons();
	}
	private void removeMark(int x) {// recursive mark for removal
		ResultData rd = resultVec.get(x);
		rd.remove = true;
		JPanel rowP = rd.panel;
		
		for (int i=0; i<resultVec.size(); i++) {
			ResultData rd1 = resultVec.get(i);
			if (rd1.parent==null || rd1.remove) continue;
			
			if (rd1.parent.equals(rowP)) removeMark(i);  
		}
	}
	
	private void removeAllPanels () {
		for(int i=0; i<resultVec.size(); i++) {
			theParentFrame.removePanelFromMenuOnly(resultVec.get(i).panel);
		}
		resultVec.clear();
		theTable.clearSelection();
		theTable.revalidate();
		updateButtons();
	}
	// remove on left of viewMulti by clicking x
	public void removePanel(JPanel targetPanel) {
		for(int x=0; x<resultVec.size(); x++) {
			if(resultVec.get(x).panel.equals(targetPanel)) {
				resultVec.remove(x);
				theTable.revalidate();
				updateButtons();
				return;
			}
		}
	}
	private void updateButtons() {		
		if(theTable.getRowCount() > 0) {
			btnRemoveAllPanels.setEnabled(true);
			btnRemoveSelectedPanels.setEnabled(true);
		}
		else {
			btnRemoveAllPanels.setEnabled(false);
			btnRemoveSelectedPanels.setEnabled(false);
		}
	}

	public void addResult(JPanel parentPanel, JPanel theNewPanel, String name, String summary) {
		ResultData resObj = new ResultData(name, summary, theNewPanel, parentPanel);
		boolean b=true;
		if (parentPanel!=null) {
			for (int i=0; i<resultVec.size(); i++) {
				ResultData obj = resultVec.get(i);
				if (obj.panel.equals(parentPanel)) {
					resObj.level = obj.level+1;
					resultVec.insertElementAt(resObj, i+1);
					b=false;
					
					break;
				}
			}
		}
		if (b) resultVec.add(resObj);
		
		resizeColumns();
		updateButtons();
	}
	
	public void renamePanel(JPanel targetPanel, String newName, String newSummary) {
		for(int x=0; x<theTable.getRowCount(); x++) {
			ResultData d = resultVec.get(x);
			if(d.panel.equals(targetPanel)) {
	        		d.label = newName;
	        		d.summary = newSummary;
				theTable.repaint();
				updateButtons();
				return;
			}
		}
	}
	private void resizeColumns() {
		theTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		for(int x=0; x < theTable.getColumnCount(); x++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) theTable.getColumnModel();
			TableColumn col = colModel.getColumn(x);
			int maxSize = 0;		
			TableCellRenderer renderer = col.getHeaderRenderer();
			if(renderer == null)renderer = theTable.getTableHeader().getDefaultRenderer();
			Component comp = renderer.getTableCellRendererComponent(theTable, col.getHeaderValue(), false, false, 0, 0);
			maxSize = comp.getPreferredSize().width;
			
			for(int y=0; y<theTable.getRowCount(); y++) {
				renderer = theTable.getCellRenderer(y, x);
				comp = renderer.getTableCellRendererComponent(theTable, 
						theTable.getValueAt(y, x), false, false, y, x);
				maxSize = Math.max(maxSize, comp.getPreferredSize().width);
			}
			maxSize += 10;
			if (x>0) maxSize+=10;
			
			col.setPreferredWidth(maxSize);
		}
		((DefaultTableCellRenderer) 
				theTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	}	
	private class ResultsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;

		private String [] theLabels = null;
		private Vector <ResultData> theDisplayValues = null;
		 
		public ResultsTableModel(Vector <ResultData> displayValues) { 
			theDisplayValues = displayValues; 
			theLabels = hitColumnHeadings();
		}
		public int getColumnCount() { return theLabels.length; }
        public int getRowCount() { return theDisplayValues.size(); }
        public Object getValueAt(int row, int col) { return theDisplayValues.get(row).getValueAt(col); }
        public String getColumnName(int col) { return theLabels[col]; }    
	}
	private String [] hitColumnHeadings() { 
		String [] x = {"Level", "Label","Summary"};
		return x;
	}
	private class ResultData {
		public ResultData(String label, String summary, JPanel panel, JPanel parent) {
			this.label=label;
			this.summary=summary;
			this.panel = panel;
			this.parent = parent;
		}
		public Object getValueAt(int pos) {
			switch(pos) {
			case 0: return level;
			case 1: return label;
			case 2: return summary;
			}
			return null;
		}
		int level=1;
		String label="", summary="";
		JPanel panel=null, parent=null;
		boolean remove=false;
	}
	
	//Needed for summary updates
	Thread updateThread = null;

	private JButton btnRemoveSelectedPanels = null;
	private JButton btnRemoveAllPanels = null;
	private JButton btnHelp = null;
		
	private JTable theTable = null;
	private JScrollPane scroll = null;

	private MTCWFrame theParentFrame = null;
	
	private Vector <ResultData> resultVec = new Vector <ResultData> ();
}

