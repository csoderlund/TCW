package sng.viewer.panels.seqTable;
/****************************************************
 * The actual query is in FieldMapper method getContigObjFromDB 
 * This does not fit into the FieldMapper paradigm, so is complex
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;

import sng.util.FieldMapper;
import util.ui.ButtonComboBox;
import util.methods.Static;

public class UIfieldNFold extends JPanel {
	private static final long serialVersionUID = 5636465225630988236L;
	final int BASE = FieldContigData.N_FOLD_LIB;
	
	public UIfieldNFold( JCheckBox [] allLibraries, boolean bF) {
		final int THEIGHT = 100;
		final int TWIDTH = 100;
		
		bFilter = bF;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Color.white);
		
		selectedFolds = new Vector<String> ();
		strLibNames = new String[allLibraries.length];
		for(int x=0; x<strLibNames.length; x++) {
			strLibNames[x] = allLibraries[x].getText();
		}
		
		// For filter only (not columns)
		JPanel row = Static.createRowPanel();
		
		chkFilter = new JCheckBox();
		chkFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b = (chkFilter.isSelected());
				txtNfold.setEnabled(b);
				cmbBool.setEnabled(b);
				btnAdd.setEnabled(b);
				btnRemove.setEnabled(b);
				btnChange.setEnabled(b);
				if (theTable.getSelectedRowCount() == 0) {
					btnRemove.setEnabled(false);
					btnChange.setEnabled(false);
				}
			}
		});
		row.add(chkFilter); row.add(Box.createHorizontalStrut(2));
		
		row.add(new JLabel("N-fold")); row.add(Box.createHorizontalStrut(5));
		txtNfold = Static.createTextField("2", 3);
		row.add(txtNfold); row.add(Box.createHorizontalStrut(15));
		
		cmbBool = new JComboBox(); cmbBool.setBackground(Color.WHITE);
		cmbBool.addItem("EVERY");
		cmbBool.addItem("ANY");
		cmbBool.setSelectedIndex(0);
		cmbBool.setMaximumSize(cmbBool.getPreferredSize());
		cmbBool.setMinimumSize(cmbBool.getPreferredSize());
		cmbBool.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmbBool.repaint();
			}
		});
		
		row.add(new JLabel("for")); row.add(Box.createHorizontalStrut(2));
		row.add(cmbBool);row.add(Box.createHorizontalStrut(2));
		row.add(new JLabel("N-fold pair"));
		
		if (bFilter) {
			chkFilter.setSelected(false);
			txtNfold.setEnabled(false);
			cmbBool.setEnabled(false);
			add(row);
			add(Box.createVerticalStrut(10));
		}
		
		// Select panel
		JPanel lowerPanel = Static.createRowPanel();
		JPanel selectPanel = Static.createPagePanel();
		cmbRefLib = new ButtonComboBox();
		cmbLib = new ButtonComboBox();
		cmbRefLib.addItem("Select 1");
		cmbLib.addItem("Select 2");
		for(int x=0; x<strLibNames.length; x++) {
			cmbRefLib.addItem(strLibNames[x]);
			cmbLib.addItem(strLibNames[x]);
		}
		cmbRefLib.setSelectedIndex(0);
		cmbLib.setSelectedIndex(0);
		
		cmbRefLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnAdd.setEnabled((chkFilter.isSelected() 
						&& cmbRefLib.getSelectedIndex() > 0 
						&& cmbLib.getSelectedIndex() > 0 
						&& cmbRefLib.getSelectedIndex() != cmbLib.getSelectedIndex()));
			}
		});
		
		cmbLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnAdd.setEnabled((chkFilter.isSelected() 
						&& cmbRefLib.getSelectedIndex() > 0 
						&& cmbLib.getSelectedIndex() > 0 
						&& cmbRefLib.getSelectedIndex() != cmbLib.getSelectedIndex()));
			}
		});

		selectPanel.add(cmbRefLib);
		selectPanel.add(Box.createVerticalStrut(10));
		selectPanel.add(cmbLib);
		
		selectPanel.setMinimumSize(selectPanel.getPreferredSize());
		selectPanel.setMaximumSize(selectPanel.getPreferredSize());
		
		lowerPanel.add(selectPanel);
		lowerPanel.add(Box.createHorizontalStrut(10));
		
		// Regulation
		JPanel regPanel = Static.createPagePanel();
		cmbReg = new JComboBox(); cmbReg.setBackground(Color.WHITE);
		cmbReg.setAlignmentX(Component.LEFT_ALIGNMENT);
		cmbReg.addItem("Up");
		cmbReg.addItem("Down");
		cmbReg.addItem("Either");
		cmbReg.setSelectedIndex(0);
		cmbReg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmbReg.repaint(); cmbReg.revalidate();
			}
		});
		btnChange = Static.createButton("Change", false);
		btnChange.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeItemForFilter();
			}
		});
		cmbReg.setMinimumSize(btnChange.getMinimumSize());
		cmbReg.setMaximumSize(btnChange.getMaximumSize());
		cmbReg.setPreferredSize(btnChange.getPreferredSize());
		
		if (bFilter) {
			regPanel.add(cmbReg);
			regPanel.add(Box.createVerticalStrut(10));
			regPanel.add(btnChange);
			regPanel.setMaximumSize(regPanel.getPreferredSize());
			regPanel.setMinimumSize(regPanel.getPreferredSize());
			
			lowerPanel.add(regPanel);
			lowerPanel.add(Box.createHorizontalStrut(10));
		}
		else chkFilter.setSelected(true);
		
		// Add/Remove
		JPanel buttonPanel = Static.createPagePanel();
		btnAdd = Static.createButton("Add", false);
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (bFilter) addItemForFilter();
				else addItemForColumns();
			}
		});
		btnRemove = Static.createButton("Remove", false);
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int nrow = theTable.getSelectedRow();
				if (nrow>=0) {
					selectedFolds.remove(nrow);
					theTableModel.removeRow(nrow);
				}
			}
		});
		
		btnAdd.setMinimumSize(btnRemove.getMinimumSize());
		btnAdd.setMaximumSize(btnRemove.getMaximumSize());
		btnAdd.setPreferredSize(btnRemove.getPreferredSize());
		
		buttonPanel.add(btnAdd);
		buttonPanel.add(Box.createVerticalStrut(10));
		buttonPanel.add(btnRemove);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
		
		lowerPanel.add(buttonPanel);
		lowerPanel.add(Box.createHorizontalStrut(10));
		
		// table
		String headers[] = {"test"};
		theTableModel = new DefaultTableModel(headers,0){
			private static final long serialVersionUID = 2691505287422437591L;

			public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		theTable = new JTable(theTableModel);
		sPane = new JScrollPane(theTable);
		
		theTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				if (chkFilter.isSelected()) {
					boolean b = theTable.getSelectedRowCount() > 0;
					btnRemove.setEnabled(b);
					btnChange.setEnabled(b);
				}
			}
		});
		theTable.setTableHeader(null);
		theTable.setAlignmentX(Component.LEFT_ALIGNMENT);
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		theTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
		
		Dimension temp = sPane.getPreferredSize();
		temp.height = THEIGHT;
		temp.width = TWIDTH;
		sPane.setPreferredSize(temp);
		sPane.setAlignmentX(Component.LEFT_ALIGNMENT);

		lowerPanel.add(sPane);
		
		lowerPanel.setMaximumSize(lowerPanel.getPreferredSize());
		lowerPanel.setMinimumSize(lowerPanel.getPreferredSize());
		
		add(lowerPanel);
	}
	private void addItemForColumns() {
		String newItem = cmbRefLib.getSelectedItem() + "/" + cmbLib.getSelectedItem();
		
		if(!selectedFolds.contains(newItem)) {
			selectedFolds.add(newItem);
			Vector<String> newRow = new Vector<String>();
			newRow.add(newItem);
			theTableModel.addRow(newRow);
			if (!foldMap.containsKey(newItem)) {
				foldMap.put(newItem, BASE+foldIndex);
				foldIndex++;
			}
		}
	}
	private void changeItemForFilter() {
		int nrow = theTable.getSelectedRow();
		if (nrow == -1) return;
		
		String item = selectedFolds.get(nrow);
		String [] tok = item.split(" ");
		if (tok.length==0) return;
		item = tok[0].trim();
		
		int index = cmbReg.getSelectedIndex();
		String abbr = " E";
		if (index==0) abbr=" U";
		else if (index==1) abbr=" D";
	
		String newItem = item+abbr;
		selectedFolds.set(nrow, newItem);
		theTableModel.setValueAt(newItem, nrow, 0);
		repaint();
	}
	private void addItemForFilter() {
		String newItem = cmbRefLib.getSelectedItem() + "/" + cmbLib.getSelectedItem();
		
		int index = cmbReg.getSelectedIndex();
		String abbr = " E";
		if (index==0) abbr = " U";
		else if (index==1) abbr = " D";
		
		newItem += abbr;
		for (String oldItem : selectedFolds) {
			if (oldItem.equals(newItem)) return;
		}
		selectedFolds.add(newItem);
		Vector<String> newRow = new Vector<String>();
		newRow.add(newItem);
		theTableModel.addRow(newRow);
	}
	/*********************************************
	 * Filter (QueryContigTab) and columns(FieldContigTab)
	 */
	public String [] getColumnNames() {
		return selectedFolds.toArray(new String[0]);
	}
	/********************************************
	 * Filter
	 */
	public boolean isNfold() {return chkFilter.isSelected();}
	public int getNfold() {
		String n = txtNfold.getText();
		try {
			int i = Integer.parseInt(n);
			return i;
		}
		catch (Exception e) { return -1;}
	}
	public String getAndOr() {
		return (cmbBool.getSelectedIndex() == 0) ? " & " : " | ";
	}

	/*****************************************
	 * Columns 
	 */
	public int getIDForName(String name) {
		if(!selectedFolds.contains(name)) return -1;
		if (!foldMap.containsKey(name)) {
			System.err.println("TCW Sequence column error getting index for " + name);
			return -1;
		}
		return foldMap.get(name);
	}
	// Called on startup to set from preferences
	public void initFoldColumn(String foldName) {
		if(selectedFolds.contains(foldName)) return;

		String [] libs  = foldName.split("/");
		int cnt=0;

		for(int x=0; x<strLibNames.length && cnt!=2; x++) {
			if(libs[0].equals(strLibNames[x])) cnt++;
			else if(libs[1].equals(strLibNames[x])) cnt++;
		}	
		if(cnt!=2) return; // shouldn't happen
			
		selectedFolds.add(foldName);
		foldMap.put(foldName, BASE+foldIndex);
		foldIndex++;
		
		Vector<String> newRow = new Vector<String>();
		newRow.add(foldName);
		theTableModel.addRow(newRow);
	
	}
	// called on startup to set mapper fields -- 
	public void addFoldColumns(FieldMapper mapper) {
		for (String foldCol : selectedFolds) {
			mapper.addFloatField(foldMap.get(foldCol), foldCol, null, null, null, null, null);
		}
	}
	
	// called on Restore Defaults. Do not clear foldMap so will reuse index if fold selected again
	public void clear() {
		cmbRefLib.setSelectedIndex(0);
		cmbLib.setSelectedIndex(0);
		
		if (theTableModel.getRowCount()==0) return;
		selectedFolds.clear();
		
		while(theTableModel.getRowCount() > 0)
		{
		    theTableModel.removeRow(0);
		}
		theTableModel.fireTableDataChanged();
	}
	private JScrollPane sPane = null;
	private JTable theTable = null;
	private DefaultTableModel theTableModel = null;
	
	private ButtonComboBox cmbRefLib = null, cmbLib = null;
	private JButton btnAdd = null, btnRemove = null, btnChange = null;
	
	private boolean bFilter = false;
	private JCheckBox chkFilter = null;
	private JTextField txtNfold = null;
	private JComboBox cmbBool = null;
	private JComboBox cmbReg = null;
	
	private String [] strLibNames = null;
	private Vector<String> selectedFolds = null;
	private HashMap <String, Integer> foldMap = new HashMap <String, Integer> ();
	private int foldIndex=0;
}
