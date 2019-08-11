package cmp.compile.panels;

/*********************************************
 * 	The Add/Edit Window for the species panel
 *  Plus Database chooser class called from the Add database window
 *  
 *  The object is created in the CompilePanel to be displayed in the main panel
 *  DEFAULTS:
 *  		Prefix is initialized in DatabaseSelectPanel
 *  		Filenames are set in FileSelectTextField, except for PROTEINsTCW is setPeptideDB 
 */
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.methods.Static;
import util.ui.UserPrompt;

import cmp.database.Globals;
import cmp.viewer.panels.DatabaseSelectPanel;

public class EditSpeciesPanel extends JPanel {
	private static final long serialVersionUID = -8431928635947256814L;

	private final int WIDTH = 90; // Globals.CompilePanel.WIDTH;
	
	public EditSpeciesPanel(CompilePanel parentPanel) {
		theCompilePanel = parentPanel;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.CENTER_ALIGNMENT);
		
		mainPanel = createEditSpeciesPanel();
		mainPanel.setMaximumSize(mainPanel.getPreferredSize());
		mainPanel.setMinimumSize(mainPanel.getPreferredSize());
		mainPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		add(mainPanel);
		
		pnlDBSelect = new DatabaseSelectPanel(Globals.Viewer.DB_NAME_PREFIX, this); 
		pnlDBSelect.setVisible(false);
		
		add(pnlDBSelect);
	}

	/************************************ 
	 * Add/Edit window for Species 
	 ***/
	private JPanel createEditSpeciesPanel() {
		JPanel page = Static.createPageCenterPanel();

		lblDatabase = new JLabel("singleTCW");
		btnSelectDB = new JButton("Select " + Globals.Viewer.DB_NAME_PREFIX + " Database");
		btnSelectDB.setBackground(Globals.MENUCOLOR);
		btnSelectDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mainPanel.setVisible(false);
				pnlDBSelect.setVisible(true);
			}
		});					
		JPanel row = Static.createRowPanel();
		row.add(lblDatabase);
		row.add(Box.createHorizontalStrut(WIDTH -lblDatabase.getPreferredSize().width));
		row.add(btnSelectDB);
		page.add(row);
		page.add(Box.createVerticalStrut(10));
		
		page.add(createSummaryPanel());
		page.add(Box.createVerticalStrut(10));
		
		lblPrefix = new JLabel("Prefix");
		txtPrefix = Static.createTextField("", 4);
		row = Static.createRowPanel();
		row.add(lblPrefix);
		row.add(Box.createHorizontalStrut(WIDTH - lblPrefix.getPreferredSize().width));
		row.add(txtPrefix);
		row.add(new JLabel("-- required, unique, 3 char max"));
		page.add(row);
		page.add(Box.createVerticalStrut(20));
	
	// Remark	
		lblRemark = new JLabel("Remark");
		txtRemark = Static.createTextField("", 15);
		row = Static.createRowPanel();
		row.add(lblRemark);
		row.add(Box.createHorizontalStrut(WIDTH - lblRemark.getPreferredSize().width));
		row.add(txtRemark);
		page.add(row);
		page.add(Box.createVerticalStrut(10));

	// buttons
		btnKeep = new JButton("Keep");
		btnKeep.setBackground(Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				keep();
			}
		});
		btnDiscard = new JButton("Cancel");
		btnDiscard.setBackground(Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				theCompilePanel.setMainPanelVisible(true);
			}
		});
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theCompilePanel.getParentFrame(), 
						"Edit species", "html/runMultiTCW/EditSpeciesPanel.html");
			}
		});
		row = Static.createRowPanel();
		row.add(Box.createHorizontalGlue());
		row.add(btnKeep);
		row.add(Box.createHorizontalStrut(10));
		row.add(btnDiscard);
		row.add(Box.createHorizontalStrut(10));
		row.add(btnHelp);
		row.add(Box.createHorizontalGlue());
		row.setMaximumSize(row.getPreferredSize());
		row.setMinimumSize(row.getPreferredSize());
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		JPanel buttonRow = Static.createRowPanel();
		buttonRow.add(Box.createHorizontalGlue());
		buttonRow.add(row);
		buttonRow.add(Box.createHorizontalGlue());
		page.add(buttonRow);
		
		page.setMaximumSize(page.getPreferredSize());
		page.setMinimumSize(page.getPreferredSize());
		
		return page;
	}
	// Keep
	private void keep() {
		SpeciesPanel theSpeciesPanel = theCompilePanel.getSpeciesPanel();
		boolean isValid = true;
		
		String dbName = lblDBname.getText();
		String prefix = txtPrefix.getText();
		String remark = txtRemark.getText();
		
		// initial checks
		if (stcwid==null || stcwid.trim().equals("")) {
			JOptionPane.showMessageDialog(theCompilePanel, "Selected sTCW database has blank sTCW ID.",
					"Invalid sTCW id", JOptionPane.PLAIN_MESSAGE);
			isValid=false;
		}
		if (dbName==null || dbName.trim().equals("")) {
			JOptionPane.showMessageDialog(theCompilePanel, "Selected sTCW database name is blank.",
					"Invalid database", JOptionPane.PLAIN_MESSAGE);
			isValid=false;
		}
		if (prefix==null || prefix.trim().equals("")) {
			JOptionPane.showMessageDialog(theCompilePanel, "Prefix is blank.",
					"Invalid prefix", JOptionPane.PLAIN_MESSAGE);
			isValid=false;
		}
		if (remark.contains("'") || remark.contains("\"") || remark.contains("="))
		{
			JOptionPane.showMessageDialog(theCompilePanel, "Remark cannot contain quotes or the '=' sign.",
					"Invalid comment", JOptionPane.PLAIN_MESSAGE);
			System.err.println("Error: comment contains quotes or '=' sign");
			isValid = false;					
		}
		// check prefix
		if(prefix.trim().length() > 3) {
			JOptionPane.showMessageDialog(theCompilePanel, "Prefix too long (limit 3 characters).",
					"Invalid prefix", JOptionPane.PLAIN_MESSAGE);

			System.err.println("Error: Prefix too long (limit 3 characters)");
			JOptionPane.showMessageDialog(theCompilePanel, "Prefix too long (limit 3 characters");
			isValid = false;
		}
		if (!prefix.matches("[\\w\\d]+")) {
			JOptionPane.showMessageDialog(theCompilePanel, "Prefix contains invalid characters." +
					"\nUse letters, numbers, and underscores.",
					"Invalid prefix", JOptionPane.PLAIN_MESSAGE);
			System.err.println("Error: prefix contains invalid characters");
			isValid = false;
		}
		if (isValid) { // prefix duplicate?
			int row = -1;
			if(bEditMode) row = theSpeciesPanel.getSelectedRow();
			
			for(int x=0; x<theSpeciesPanel.getNumRows() && isValid; x++) {
				if(x != row && theSpeciesPanel.getPrefixAt(x).equals(prefix)) {
					System.err.println("Error: duplicate prefix " + prefix);
					JOptionPane.showMessageDialog(theCompilePanel,
							"Duplicate prefix '" + prefix + "'");
					isValid = false;
				}
			}
		}
		// sTCW id duplicate
		if (isValid) { 
			int row = -1;
			if (bEditMode) row = theSpeciesPanel.getSelectedRow();
			
			for(int x=0; x<theSpeciesPanel.getNumRows() && isValid; x++) {
				if(x != row && theSpeciesPanel.getSTCWidAt(x).equals(stcwid)) {
					System.err.println("Error: duplicate sTCW id " + stcwid);
					JOptionPane.showMessageDialog(theCompilePanel,
							"Duplicate sTCW id '" + stcwid + "'");
					isValid = false;
				}
			}
		}
		if (isValid) {
			if(bEditMode) {
				int row = theSpeciesPanel.getSelectedRow();
				theSpeciesPanel.setRow(row, dbName, stcwid, prefix, remark, bIsProtein);
			}
			else
				theSpeciesPanel.addRow(dbName, stcwid, prefix, remark, bIsProtein);
			
			theSpeciesPanel.updateTable();
			theCompilePanel.dbAddSpeciesRemark(prefix, remark); // update to database if exists
			theCompilePanel.mTCWcfgSave();
			
			setVisible(false);
			theCompilePanel.setMainPanelVisible(true);
		}
	}
	/************************************************
	 * Summary information about sTCW database after selected
	 * This is shown under "Select sTCW Database"
	 */
	private JPanel createSummaryPanel() {
		JPanel page = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();
		JLabel lblDBHeader = new JLabel("Database");
		lblDBHeader.setMaximumSize(lblDBHeader.getPreferredSize());
		lblDBHeader.setMinimumSize(lblDBHeader.getPreferredSize());
		lblDBname = new JLabel("");
		row.add(lblDBHeader);
		if(lblDBHeader.getMaximumSize().width < WIDTH)
			row.add(Box.createHorizontalStrut(WIDTH - lblDBHeader.getMaximumSize().width));
		row.add(lblDBname);
		page.add(row);
		
		row = Static.createRowPanel();
		JLabel lblTypeHeader = new JLabel("Type");
		lblTypeHeader.setMaximumSize(lblTypeHeader.getPreferredSize());
		lblTypeHeader.setMinimumSize(lblTypeHeader.getPreferredSize());
		lblDBtype = new JLabel("");
		row.add(lblTypeHeader);
		if(lblTypeHeader.getMaximumSize().width < WIDTH)
			row.add(Box.createHorizontalStrut(WIDTH - lblTypeHeader.getMaximumSize().width));
		row.add(lblDBtype);
		page.add(row);
					
		return page;
	}
	
	public void setMainPanelVisible(boolean visible) {
		mainPanel.setVisible(visible);
	}
	
	public void enableAll(boolean loaded) { 
		lblDatabase.setEnabled(!loaded);
		btnSelectDB.setEnabled(!loaded);
		
		lblPrefix.setEnabled(!loaded);
		txtPrefix.setEnabled(!loaded);
		
		lblDBname.setEnabled(!loaded);
		lblDBtype.setEnabled(!loaded);
	}
	
	public void setFromDBselect(String dbName, String id, String prefix, boolean isPep) {
		lblDBname.setText(dbName);
		stcwid = id;
		txtPrefix.setText(prefix);
		txtRemark.setText(dbName.substring(Globals.MTCW.length()+1));
		bIsProtein = isPep;
	
		lblDBtype.setText(isPep ? Globals.TypeAA : Globals.TypeNT);
	} 
	public void setValues(String id, String dbName, String prefix, 
			String remark, boolean isAA, String type) {
		stcwid=id;
		lblDBname.setText(dbName);
		bIsProtein = isAA;
		lblDBtype.setText(type);
		
		txtPrefix.setText(prefix);	
		
		txtRemark.setText(remark);
		bEditMode=true;
	}
	public void clear() {
		lblDBname.setText("");
		lblDBtype.setText("");
		txtPrefix.setText("");
		txtRemark.setText("");
		bIsProtein=bEditMode=false;
		stcwid = "";
	}

	private JPanel mainPanel = null;
	private JLabel lblDatabase = null;
	private JButton btnSelectDB = null;
	private DatabaseSelectPanel pnlDBSelect = null;
	
	private JButton btnKeep = null;
	private JButton btnDiscard = null;
	private JButton btnHelp = null;
	
	private JLabel lblPrefix = null;
	private JTextField txtPrefix = null;
	
	private JLabel lblRemark = null;
	private JTextField txtRemark = null;
	
	//Summary JLabels for species database
	private JLabel lblDBname = null;
	private JLabel lblDBtype = null;
	
	private boolean bEditMode = false;
	private boolean bIsProtein = false;
	private String stcwid = "";
	
	private CompilePanel theCompilePanel = null;
}
