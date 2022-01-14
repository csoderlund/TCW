package cmp.compile.panels;

/****************************************************
 * Defines the Add/Edit Panel for Methods
 */
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import util.ui.ButtonComboBox;
import util.ui.UserPrompt;
import util.methods.Out;
import util.methods.Static;
import cmp.database.Globals;

public class EditMethodPanel extends JPanel {
	private static final long serialVersionUID = -3057836165213279944L;
	
	public  static final String LBLPREFIX = "Required (unique, 5 char max)";
	
	private final String helpHTML =  Globals.helpRunDir + "EditMethodPanel.html";
	private final int maxPrefix = 5;

	private final short BB = 0;
	private final short TR = 1;
	private final short HT = 2;
	private final short OM = 3;
	private final short UD = 4;

	public EditMethodPanel(CompilePanel parentPanel) {
		theCompilePanel = parentPanel;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Static.BGCOLOR);
			
		JPanel row1 = Static.createRowPanel();
		lblMode = new JLabel("Method");
		row1.add(lblMode);
		row1.add(Box.createHorizontalStrut(5));
		
		// Dropdown of methods: order must match numbers assigned to OR, TR, BB
		String [] labels = {
				MethodBBHPanel.getMethodType(),
				MethodClosurePanel.getMethodType(),
				MethodHitPanel.getMethodType(),
				MethodOrthoMCLPanel.getMethodType(),
				MethodLoadPanel.getMethodType()};
		
		cmbMode =  new ButtonComboBox();
		cmbMode.addItems(labels);
		cmbMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateMode();
			}
		});
		row1.add(cmbMode);
		row1.setMaximumSize(row1.getPreferredSize());
		row1.setMinimumSize(row1.getPreferredSize());
		
		add(row1);
		add(Box.createVerticalStrut(15));
		
		// One of these is listed here.
		// The Prefix, Name, Remark and parameters are created in the following 4 classes
		
		pnlBBH =   new MethodBBHPanel(theCompilePanel);
		pnlTrans = new MethodClosurePanel(theCompilePanel);
		pnlHit = new MethodHitPanel(theCompilePanel);
		pnlOrtho = new MethodOrthoMCLPanel(theCompilePanel);
		pnlLoad  = new MethodLoadPanel(theCompilePanel);

		add(pnlBBH);
		add(pnlTrans);
		add(pnlHit);
		add(pnlOrtho);
		add(pnlLoad);
		
		// button row of buttons
		JPanel buttonPanel = Static.createRowPanel();
		btnKeep = Static.createButton("Keep");
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				keep();
			}
		});
		buttonPanel.add(btnKeep);
		buttonPanel.add(Box.createHorizontalStrut(10));
		
		btnDiscard = Static.createButton("Cancel");
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
				theCompilePanel.setMainPanelVisible(true);
			}
		});
		buttonPanel.add(btnDiscard);
		buttonPanel.add(Box.createHorizontalStrut(10));
		
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theCompilePanel.getParentFrame(), "Edit method",helpHTML);
			}
		});
		buttonPanel.add(btnHelp);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
		
		JPanel row3 = Static.createRowPanel();
		row3.add(Box.createHorizontalGlue());
		row3.add(buttonPanel);
		row3.add(Box.createHorizontalGlue());
		
		add(Box.createVerticalStrut(15));
		add(row3);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
		updateMode();
	}
	private void keep() {
		boolean isValid = true;
		String prefix = getPrefix();
		String comment = getComment();
		int nMethod = cmbMode.getSelectedIndex();
		
		isValid = checkBBH();
		
	// Prefix  - for all
		if(prefix.length() > maxPrefix) {
			String msg = "Prefix too long. Must be less than " + maxPrefix + " characters";
			JOptionPane.showMessageDialog(theCompilePanel, msg,
							"Invalid prefix", JOptionPane.PLAIN_MESSAGE);
			Out.PrtError(msg);
			isValid = false;
		}
		if (!prefix.matches("[\\w\\d]+"))
		{
			String msg = "Prefix contains invalid characters.\nUse letters,numbers,underscores.";
			JOptionPane.showMessageDialog(theCompilePanel, msg,
					"Invalid prefix", JOptionPane.PLAIN_MESSAGE);
			Out.PrtError(msg);
			isValid = false;
		}
		
		if (theCompilePanel.isReservedWords(prefix)) { // CAS310 moved from DBinfo to CompilePanel
			String msg = "Prefix '" + prefix + "' is a MySQL resevered word. Please select a different prefix.";
			JOptionPane.showMessageDialog(theCompilePanel, msg,
					"Invalid prefix", JOptionPane.PLAIN_MESSAGE);
			Out.PrtError(msg);
			isValid = false;
		}
		
	// Prefix unique
		int numRow= theCompilePanel.getMethodPanel().getNumRows();
		if(bEditMode) {
			int row = theCompilePanel.getMethodPanel().getSelectedRow();
			MethodPanel mp = theCompilePanel.getMethodPanel();
			for(int x=0; x<numRow && isValid; x++) {
				if(x != row && mp.getMethodPrefixAt(x).equalsIgnoreCase(prefix)) {
					String msg= "Edit: Duplicate prefix '" + prefix + "' at row # " + (x+1);
					JOptionPane.showMessageDialog(theCompilePanel, msg, "Must be unique", JOptionPane.PLAIN_MESSAGE);
					Out.PrtError(msg);
					isValid = false;
				}
			}
			if(isValid) {
				theCompilePanel.getMethodPanel().setRow(row, getMethodType(), prefix, comment, getSettings());
				if(bLoadedInDB) 
					theCompilePanel.dbAddMethodRemark(prefix, comment);
			}
		}
		else {
			for(int x=0; x<numRow && isValid; x++) {
				MethodPanel mp = theCompilePanel.getMethodPanel();
				if (mp.getMethodPrefixAt(x).equalsIgnoreCase(prefix)) {
					String msg= "Add: Duplicate prefix '" + prefix + "' at row # " + (x+1);
					JOptionPane.showMessageDialog(theCompilePanel, msg, "Must be unique", JOptionPane.PLAIN_MESSAGE);
					Out.PrtError(msg);
					isValid = false;
				}
			}
		}
	// Comment
		if (comment.contains("'") || comment.contains("\"") || comment.contains("="))
		{
			String msg = "Remark cannot contain quotes or the '=' sign.";
			JOptionPane.showMessageDialog(theCompilePanel, msg,
					"Invalid remark", JOptionPane.PLAIN_MESSAGE);
			Out.PrtError(msg);
			isValid = false;					
		}
				
	// Amino acid or nucleotide
		String type = getSearchType();
		if (theCompilePanel.dbIsExist() && type.equals("NT")) {
			int n = theCompilePanel.getNumNTdb();
			
			if (n<2 &&  nMethod==BB) {
				JOptionPane.showMessageDialog(theCompilePanel, 
						"Must have at least two nucleotide singleTCW databases", 
						"Nucleotide BBH", JOptionPane.PLAIN_MESSAGE);
				isValid = false;
			}
			if (n==0 &&  nMethod==TR) {
				JOptionPane.showMessageDialog(theCompilePanel, 
						"No nucleotide singleTCW databases loaded -- must have at one", 
						"Nucleotide TR", JOptionPane.PLAIN_MESSAGE);
				isValid = false;
			}
			if (n<2 &&  nMethod==TR) {
				if(JOptionPane.showConfirmDialog(theCompilePanel, 
						"Only one Nucleotide singleTCW loaded.\nContinue?", 
						"Nucleotide TR", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) 
						!= JOptionPane.YES_OPTION)
				isValid = false;
			}
		}
		
	// User defined
		if (nMethod==UD) {
			if (!pnlLoad.hasValidFile()) {
				JOptionPane.showMessageDialog(theCompilePanel, "Must enter an existing file name",
						"Invalid file", JOptionPane.PLAIN_MESSAGE);
				isValid = false;
			}
		}
		if(isValid) {
			if(!bEditMode) {
				MethodPanel mPnl = theCompilePanel.getMethodPanel();
				String method =  cmbMode.getSelectedItem();
				mPnl.addRow(method, prefix, comment, getSettings());
			}
			theCompilePanel.getMethodPanel().updateTable();
			theCompilePanel.mTCWcfgSave();
			
			setVisible(false);
			theCompilePanel.setMainPanelVisible(true);
		}
	}
	
	// User selected the Edit button
	public void setEditMode(boolean mode) { bEditMode = mode; }
	
	// User selected a Method row - may or may not already be in the database
	public void setLoaded(boolean loaded) { 
		bLoadedInDB = loaded;
		cmbMode.setEnabled(!loaded);
		int sel = cmbMode.getSelectedIndex();
		
		if(sel == TR) pnlTrans.setLoaded(bLoadedInDB);
		else if(sel == BB) pnlBBH.setLoaded(bLoadedInDB);
		else if(sel == HT) pnlHit.setLoaded(bLoadedInDB);
		else if(sel == OM) pnlOrtho.setLoaded(bLoadedInDB);
		else pnlLoad.setLoaded(bLoadedInDB);
	}

	private void updateMode() {
		pnlBBH.setVisible(false);
		pnlTrans.setVisible(false);
		pnlHit.setVisible(false);
		pnlOrtho.setVisible(false);
		pnlLoad.setVisible(false);
		
		int sel = cmbMode.getSelectedIndex();
		
		if (sel == BB)      pnlBBH.setVisible(true);
		else if (sel == TR) pnlTrans.setVisible(true);
		else if (sel == HT) pnlHit.setVisible(true);
		else if (sel == OM) pnlOrtho.setVisible(true);
		else                pnlLoad.setVisible(true);
	}
	
	public String getMethodType() { return  cmbMode.getSelectedItem(); }
	public void setMethodType(String method) { 
		cmbMode.setSelectedItem(method);
		updateMode(); 
	}
	private boolean checkBBH() {
		int sel = cmbMode.getSelectedIndex();
		
		if (sel==BB) {
			if (!pnlBBH.checkSet()) {
				JOptionPane.showMessageDialog(theCompilePanel, 
					"Choose two or more datasets for BBH creation", 
					"BBH set", JOptionPane.PLAIN_MESSAGE);
				return false;
			}
		}
		return true;
	}
	private String getSearchType() {
		int sel = cmbMode.getSelectedIndex();
		
		if (sel == OM) return "AA";
		if (sel == TR) return pnlTrans.getSearchType();
		if (sel == BB) return pnlBBH.getSearchType();
		return "AA";
	}
	private String getPrefix() { 
		int sel = cmbMode.getSelectedIndex();
		
		if (sel == TR) return pnlTrans.getPrefix();
		if (sel == BB) return pnlBBH.getPrefix();
		if (sel == HT) return pnlHit.getPrefix();
		if (sel == OM) return pnlOrtho.getPrefix();
		return pnlLoad.getPrefix();
	}
	
	public void setPrefix(String prefix) { 
		int sel = cmbMode.getSelectedIndex();
		
		if (sel == HT) pnlHit.setPrefix(prefix);
		else if (sel == TR) pnlTrans.setPrefix(prefix);
		else if (sel == BB) pnlBBH.setPrefix(prefix);
		else if (sel == OM) pnlOrtho.setPrefix(prefix);
		else pnlLoad.setPrefix(prefix);
	}
	
	private String getComment() {
		int sel = cmbMode.getSelectedIndex();
		
		if (sel == TR) return pnlTrans.getComment();
		if (sel == BB) return pnlBBH.getComment();
		if (sel == HT) return pnlHit.getComment();
		if (sel == OM) return pnlOrtho.getComment();
		return pnlLoad.getComment();
	}

	private String getSettings() {
		int sel = cmbMode.getSelectedIndex();
		
		if(sel == TR) return pnlTrans.getSettings();
		if(sel == BB) return pnlBBH.getSettings();
		if(sel == HT) return pnlHit.getSettings();
		if(sel == OM) return pnlOrtho.getSettings();
		
		return pnlLoad.getSettings();
	}
	
	public void setSettings(String settings) {
		int sel = cmbMode.getSelectedIndex();
		
		if(sel == OM) pnlOrtho.setSettings(settings);
		else if(sel == TR) pnlTrans.setSettings(settings);
		else if(sel == BB) pnlBBH.setSettings(settings);
		else if (sel == HT) pnlHit.setSettings(settings);
		else pnlLoad.setSettings(settings);
	}
	
	public void resetSettings() {
		pnlOrtho.resetSettings();
		pnlTrans.resetSettings();
		pnlBBH.resetSettings();
		pnlLoad.resetSettings();
		pnlHit.resetSettings();
	}
	
	private CompilePanel theCompilePanel = null;
	private ButtonComboBox cmbMode = null;
	private JLabel lblMode = null;
	private MethodBBHPanel pnlBBH = null;
	private MethodOrthoMCLPanel pnlOrtho = null;
	private MethodClosurePanel pnlTrans = null;
	private MethodHitPanel pnlHit = null;
	private MethodLoadPanel pnlLoad = null;
	private JButton btnKeep = null;
	private JButton btnDiscard = null;
	private JButton btnHelp = null;
	
	private boolean bEditMode = false;
	private boolean bLoadedInDB = false;
}
