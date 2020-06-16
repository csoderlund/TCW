package sng.amanager;
/***********************************************
 * Add/Edit annoDB panel
 */
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.util.Vector;


import sng.database.Globals;
import util.methods.BlastArgs;
import util.methods.Static;
import util.methods.Out;
import util.ui.ButtonComboBox;
import util.ui.UserPrompt;

public class EditAnnoPanel extends JPanel {
	private static final long serialVersionUID = 4830755874697242929L;
	
	private static final int COLUMN_WIDTH = 200;
	private static final int TAB_WIDTH = 20;
	private static final int COL_MINUS_TAB = COLUMN_WIDTH-TAB_WIDTH;
	private static final int FIELD_WIDTH_SHORT = 15;
	private static final int FIELD_WIDTH = 30;
	
	public EditAnnoPanel(ManagerFrame parentFrame) {
		theParentFrame = parentFrame;
		
		pnlMain = Static.createPagePanel();
		JLabel title = new JLabel("Add or Edit annoDB");
		title.setFont(pnlMain.getFont().deriveFont(Font.BOLD, 18));
		JPanel row = Static.createRowPanel();
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));
		
		lblTaxonomy = new JLabel("Taxonomy");
		txtTaxonomy = Static.createTextField("",FIELD_WIDTH_SHORT);
		
		lblDBfasta = new JLabel("annoDB FASTA file");
		txtDBfasta = new FileTextField(theParentFrame, FileTextField.ANNO, FileTextField.FASTA);
		
		btnGenHitFile = new JRadioButton("Generate Hit Tabular File");
		btnGenHitFile.setBackground(Globals.BGCOLOR);
		btnGenHitFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				enableFields(true);		
			}
		});
		
		lblSearchPgms = new JLabel("Search program ");
		cmbSearchPgms = new ButtonComboBox();
		cmbSearchPgms.setBackground(Globals.BGCOLOR);
		Vector <String> pgm = BlastArgs.getSearchPgms();
		if (pgm.size()==0) {
			Out.die("singleTCW needs a search program to continue.");
		}
		else if (pgm.size()==1) {
			cmbSearchPgms.addItem(pgm.get(0));
			cmbSearchPgms.setSelectedIndex(0);
		}
		else {
			for (String p: pgm) cmbSearchPgms.addItem(p);
			cmbSearchPgms.setSelectedIndex(0); 
		}
		
		lblParams = new JLabel("Parameters");
		txtParams = new JTextField(FIELD_WIDTH);
		setParamDefaults();
		
		cmbSearchPgms.setMaximumSize(cmbSearchPgms.getPreferredSize());
		cmbSearchPgms.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setParamDefaults();
			}
		});
		
		btnUseExistingHitFile = new JRadioButton("Use Existing Hit Tabular File");
		btnUseExistingHitFile.setBackground(Globals.BGCOLOR);
		btnUseExistingHitFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				enableFields(false);
			}
		});
		lblTabularFile = new JLabel("Hit Tabular File");
		txtTabularFile = new FileTextField(theParentFrame, FileTextField.PROJ, FileTextField.TAB);
		
		lblDate = new JLabel("Date of annoDB download");
		txtDate = Static.createTextField("",FIELD_WIDTH_SHORT);
		
		btnKeep = new JButton("Keep");
		btnKeep.setBackground(Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!dataIsValid()) return;
				if (!keep()) return;
				
				setVisible(false);
				theParentFrame.setMainPanelVisible(true);
				
				theParentFrame.updateUI();
				theParentFrame.saveProject();
				theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		
		btnDiscard = new JButton("Cancel");
		btnDiscard.setBackground(Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(theParentFrame.isRemoveAnnoOnDiscard()) {
					theParentFrame.clearCurrentAnnoDB();
					theParentFrame.updateUI();
				}
				
				setVisible(false);
				theParentFrame.setMainPanelVisible(true);
				theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "AnnoDB Help", 
						"html/runSingleTCW/EditAnnoDBPanel.html");
			}
		});

		/** Edit Panel layout **/
		ButtonGroup group = new ButtonGroup();
		group.add(btnGenHitFile);
		group.add(btnUseExistingHitFile);
		btnGenHitFile.setSelected(true);
		
		// required
		row = Static.createRowPanel();
		row.add(lblTaxonomy);
		if(lblTaxonomy.getPreferredSize().width < COLUMN_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_WIDTH - lblTaxonomy.getPreferredSize().width));
		row.add(txtTaxonomy);
		row.add(new JLabel("- Required (See Help Naming Rules)"));
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		row.add(lblDBfasta);
		if(lblDBfasta.getPreferredSize().width < COLUMN_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_WIDTH - lblDBfasta.getPreferredSize().width));
		row.add(txtDBfasta);
		row.add(new JLabel("- Required"));
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		row.add(lblDate);
		if(lblDate.getPreferredSize().width < COLUMN_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_WIDTH - lblDate.getPreferredSize().width));
		row.add(txtDate);
		row.add(new JLabel("- Defaults to file date"));
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));
		
		// generate
		row = Static.createRowPanel();
		row.add(btnGenHitFile);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(TAB_WIDTH));
		row.add(lblSearchPgms);
		if(lblSearchPgms.getPreferredSize().width < COL_MINUS_TAB)
			row.add(Box.createHorizontalStrut(COL_MINUS_TAB - lblSearchPgms.getPreferredSize().width));
		row.add(cmbSearchPgms);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));

		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(TAB_WIDTH));
		row.add(lblParams);
		if(lblParams.getPreferredSize().width < COL_MINUS_TAB)
			row.add(Box.createHorizontalStrut(COL_MINUS_TAB - lblParams.getPreferredSize().width));
		row.add(txtParams);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));

		// existing
		row = Static.createRowPanel();
		row.add(btnUseExistingHitFile);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));

		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(TAB_WIDTH));
		row.add(lblTabularFile);
		if(lblTabularFile.getPreferredSize().width < COL_MINUS_TAB)
			row.add(Box.createHorizontalStrut(COL_MINUS_TAB - lblTabularFile.getPreferredSize().width));
		row.add(txtTabularFile);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));
	
		row = Static.createRowPanel();
		JPanel buttonPanel = Static.createRowPanel();
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		buttonPanel.add(btnKeep);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(btnDiscard);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(btnHelp);
		
		row.add(Box.createHorizontalGlue());
		row.add(buttonPanel);
		row.add(Box.createHorizontalGlue());
		pnlMain.add(row);
		pnlMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		setBackground(Globals.BGCOLOR);
		add(pnlMain);
		setVisible(false);
	}
	public void updateAnnoDBEditUI(int index, boolean b) {
		nCurrentAnnoIndex = index;
		bAdd = b;
		ManagerData curManData = theParentFrame.getCurManData();
		ManagerData.AnnodbData annoObj = curManData.getAnnoDBAt(index);
		
		boolean isLoaded=annoObj.isLoaded();
		setLoaded(isLoaded);
		
		txtTaxonomy.setText(annoObj.getTaxo());
		curTaxonomy=annoObj.getTaxo();
		
		txtTabularFile.setText(annoObj.getTabularFile());
		txtDBfasta.setText(annoObj.getFastaDB());
		txtDate.setText(annoObj.getDate());
		
		setSearchPgm(annoObj.getSearchPgm());
		String args = annoObj.getParams();
		if (args.equals("-")) setParamDefaults(); 
		else txtParams.setText(annoObj.getParams());
		
		if (bAdd) enableFields(true);
		else {
			if (!isLoaded) { 
				String file = annoObj.getTabularFile();
				if (file == null || file.equals("")) enableFields(true);
				else enableFields(false);
			}
		}
	}
	private boolean keep() {
		ManagerData curManData = theParentFrame.getCurManData();
		ManagerData.AnnodbData annoObj = curManData.getAnnoDBAt(nCurrentAnnoIndex);
		
		String file = txtDBfasta.getText();
		if (bAdd && curManData.hasAnnoFile(file)) return false; 
		
		String tax = txtTaxonomy.getText();
		if (bIsLoaded && !curTaxonomy.equals(tax)) {
			if (UserPrompt.showConfirm("Edit annoDB", "Changed taxonomy from " + curTaxonomy + " to " + tax)) {
				if (!theParentFrame.tcwDBupdateTaxonomyForLoadedAnnoDB(tax, curTaxonomy))
					tax = curTaxonomy;
			}
			else tax = curTaxonomy;
		}
		annoObj.setTaxo(tax);
		
		annoObj.setFastaDB(file);
		annoObj.setDate(txtDate.getText());
		annoObj.setSearchPgm(cmbSearchPgms.getSelectedItem());
		annoObj.setParams(txtParams.getText());
		annoObj.setTabularFile(txtTabularFile.getText());
		return true;
	}
	private void enableFields(boolean gen) {
		boolean g=gen, e=!gen;
		lblDBfasta.setEnabled(true);
		txtDBfasta.setEnabled(true);
		
		btnGenHitFile.setSelected(g);
		lblParams.setEnabled(g);
		txtParams.setEnabled(g);
		lblSearchPgms.setEnabled(g);
		
		btnUseExistingHitFile.setSelected(e);
		lblTabularFile.setEnabled(e);
		txtTabularFile.setEnabled(e);
		if (e==false) txtTabularFile.setText("");
	}
	
	private boolean dataIsValid()
	{
		boolean valid = true;
		String tax = txtTaxonomy.getText().trim();
		if (tax.equals(""))
		{
			UserPrompt.showError("Enter taxonomy string");
			valid = false;
		}
		else {
			if (!theParentFrame.isValidID(tax)) {
				UserPrompt.showError("Taxonomy string " + tax + " must be letters, digits or '_' (underscore)");
				valid = false;
			}
		}
		if (btnUseExistingHitFile.isSelected() && btnUseExistingHitFile.isEnabled()) 
		{
			if (txtDBfasta.getText().trim().equals(""))
			{
				if (!UserPrompt.showConfirm("AnnoDB",
						"Missing information -- Enter annoDB FASTA file"))
					valid = false;
			}
			if (txtTabularFile.getText().trim().equals(""))
			{
				UserPrompt.showError("Missing information -- Enter hit tabular file");
				valid = false;
			}
		}
		else if (btnGenHitFile.isSelected() && btnGenHitFile.isEnabled())
		{
			if (txtDBfasta.getText().trim().equals(""))
			{
				UserPrompt.showError("Missing information -- Enter annoDB FASTA file");
				valid = false;
			}	
		}
		return valid;
	}
	
	private void setParamDefaults () {
		String selected = cmbSearchPgms.getSelectedItem();
		if (selected.equals("blast")) txtParams.setText(BlastArgs.getBlastxOptions());
		else if (selected.equals("diamond")) txtParams.setText(BlastArgs.getDiamondOpDefaults());
		else txtParams.setText("");
	}
	public void setSearchPgm(String pgm) {
		boolean set = cmbSearchPgms.setSelectedItem(pgm);
		if (!set) cmbSearchPgms.setSelectedIndex(0);
	}

	private void setLoaded(boolean loaded) { 
		bIsLoaded = loaded; 
		
		btnGenHitFile.setEnabled(!bIsLoaded);
		lblDBfasta.setEnabled(!bIsLoaded);
		txtDBfasta.setEnabled(!bIsLoaded);
		lblParams.setEnabled(!bIsLoaded);
		txtParams.setEnabled(!bIsLoaded);
		lblDate.setEnabled(!bIsLoaded);
		txtDate.setEnabled(!bIsLoaded);
		
		lblSearchPgms.setEnabled(!bIsLoaded);
		cmbSearchPgms.setEnabled(!bIsLoaded);
		
		btnUseExistingHitFile.setEnabled(!bIsLoaded);
		lblTabularFile.setEnabled(!bIsLoaded);
		txtTabularFile.setEnabled(!bIsLoaded);	
	}
		
	/****************************************************
	 * EditAnnoPanel private variables
	 */
	private ManagerFrame theParentFrame = null;
	private JPanel pnlMain = null;
	
	private JLabel lblTaxonomy = null;
	private JTextField txtTaxonomy = null;
	
	private JLabel lblDBfasta = null;
	private FileTextField txtDBfasta = null;
	
	private JRadioButton btnGenHitFile = null;
	private JLabel lblSearchPgms=null;
	private ButtonComboBox cmbSearchPgms = null;
	private JLabel lblParams = null;
	private JTextField txtParams = null;
	
	private JRadioButton btnUseExistingHitFile = null;
	private JLabel lblTabularFile = null;
	private FileTextField txtTabularFile = null;
	
	private JLabel lblDate = null;
	private JTextField txtDate = null;
	
	private JButton btnKeep = null, btnDiscard = null, btnHelp = null;
	private boolean bIsLoaded = false;
	private boolean bAdd = true;
	private int nCurrentAnnoIndex = -1;
	private String curTaxonomy="";
}
