package sng.amanager;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.util.Vector;

import sng.database.Globals;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileRead;
import util.methods.BlastArgs;
import util.methods.Static;
import util.methods.Out;
import util.ui.ButtonComboBox;
import util.ui.UserPrompt;

/***********************************************
 * Add/Edit annoDB panel
 */
public class EditAnnoPanel extends JPanel {
	private static final long serialVersionUID = 4830755874697242929L;
	
	private static String helpHTML =  Globals.helpRunDir + "EditAnnoDBPanel.html";
	private static final int COLUMN_WIDTH = 200;
	private static final int TAB_WIDTH = 20;
	private static final int COL_MINUS_TAB = COLUMN_WIDTH-TAB_WIDTH;
	private static final int FIELD_WIDTH_SHORT = 10;
	private static final int FIELD_WIDTH = 30;
	
	// Created once on startup
	public EditAnnoPanel(ManagerFrame parentFrame) {
		theManFrame = parentFrame;
		
		pnlMain = Static.createPagePanel();
		JLabel title = new JLabel("Add or Edit annoDB");
		title.setFont(pnlMain.getFont().deriveFont(Font.BOLD, 18));
		JPanel row = Static.createRowPanel();
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));
		
		row = Static.createRowPanel();
		lblTaxonomy = new JLabel("Taxonomy");
		txtTaxonomy = Static.createTextField("",FIELD_WIDTH_SHORT);
		row.add(lblTaxonomy);
		if(lblTaxonomy.getPreferredSize().width < COLUMN_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_WIDTH - lblTaxonomy.getPreferredSize().width));
		row.add(txtTaxonomy);
		row.add(new JLabel("- Required (See Help Naming Rules)"));
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		lblDBfasta = new JLabel("annoDB FASTA file");
		txtDBfasta = Static.createTextField("", 25);
		row.add(lblDBfasta);
		if(lblDBfasta.getPreferredSize().width < COLUMN_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_WIDTH - lblDBfasta.getPreferredSize().width));
		row.add(txtDBfasta);		row.add(Box.createHorizontalStrut(5));
		
		btnDBfasta = new JButton("...");
		btnDBfasta.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String projName = theManFrame.getProjDir();
				FileRead fc = new FileRead(projName, FileC.bDoVer, FileC.bDoPrt);
				
				if (fc.run(btnDBfasta, "AnnoDB", FileC.dANNO, FileC.fFASTA)) { 
					txtDBfasta.setText(fc.getRemoveFixedPath());	
					if (!fc.isProtein()) {
						String pgm = cmbSearchPgms.getSelectedItem();
						if (pgm.contentEquals("diamond")) {
							Out.PrtSpMsg(0, "NT file must use blast");
							cmbSearchPgms.setSelectedIndex(1);
						}
					}
				}
			}
		});
		row.add(btnDBfasta);	row.add(Box.createHorizontalStrut(5));
		row.add(new JLabel("- Required"));
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		lblDate = new JLabel("Date of annoDB download");
		txtDate = Static.createTextField("",FIELD_WIDTH_SHORT);
		row.add(lblDate);
		if(lblDate.getPreferredSize().width < COLUMN_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_WIDTH - lblDate.getPreferredSize().width));
		row.add(txtDate);
		row.add(new JLabel("- Defaults to file date"));
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));
		
		row = Static.createRowPanel();
		btnGenHitFile = new JRadioButton("Generate Hit Tabular File");
		btnGenHitFile.setBackground(Globals.BGCOLOR);
		btnGenHitFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				enableFields(true);		
			}
		});
		
		row.add(btnGenHitFile);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));
		
		
		lblSearchPgms = new JLabel("Search program ");
		cmbSearchPgms = new ButtonComboBox();
		cmbSearchPgms.setBackground(Globals.BGCOLOR);
		Vector <String> pgm = BlastArgs.getSearchPgms();
		for (String p: pgm) cmbSearchPgms.addItem(p);
		cmbSearchPgms.setSelectedIndex(0); 
		
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(TAB_WIDTH));
		row.add(lblSearchPgms);
		if(lblSearchPgms.getPreferredSize().width < COL_MINUS_TAB)
			row.add(Box.createHorizontalStrut(COL_MINUS_TAB - lblSearchPgms.getPreferredSize().width));
		
		cmbSearchPgms.setMaximumSize(cmbSearchPgms.getPreferredSize());
		cmbSearchPgms.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setParamDefaults();
			}
		});
			
		row.add(cmbSearchPgms);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));

		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(TAB_WIDTH));
		lblParams = new JLabel("Parameters");
		row.add(lblParams);
		if(lblParams.getPreferredSize().width < COL_MINUS_TAB)
			row.add(Box.createHorizontalStrut(COL_MINUS_TAB - lblParams.getPreferredSize().width));
		txtParams = new JTextField(FIELD_WIDTH);
		row.add(txtParams);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));
		setParamDefaults();

		row = Static.createRowPanel();
		btnUseExistingHitFile = new JRadioButton("Use Existing Hit Tabular File");
		btnUseExistingHitFile.setBackground(Globals.BGCOLOR);
		btnUseExistingHitFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				enableFields(false);
			}
		});
		row.add(btnUseExistingHitFile);
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(TAB_WIDTH));
		lblTabFile = new JLabel("Hit Tabular File");
		row.add(lblTabFile);
		if(lblTabFile.getPreferredSize().width < COL_MINUS_TAB)
			row.add(Box.createHorizontalStrut(COL_MINUS_TAB - lblTabFile.getPreferredSize().width));
		
		txtTabFile = Static.createTextField("", 25);
		row.add(txtTabFile);		row.add(Box.createHorizontalStrut(5));
		btnTabFile = new JButton("...");
		btnTabFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String projName = theManFrame.getProjDir();
				FileRead fc = new FileRead(projName, FileC.bDoVer, FileC.bDoPrt);
				
				if (fc.run(btnTabFile, "Hit Tab", FileC.dPROJHIT, FileC.fTAB)) { 
					txtTabFile.setText(fc.getRemoveFixedPath());	
				}
			}
		});
		row.add(btnTabFile);	
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(20));
	
		ButtonGroup group = new ButtonGroup();
		group.add(btnGenHitFile);
		group.add(btnUseExistingHitFile);
		btnGenHitFile.setSelected(true);
		
		// Bottom buttons
		JButton btnKeep = new JButton(Globalx.keepBtn);
		btnKeep.setBackground(Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!keep()) return;
				
				setVisible(false);
				theManFrame.setMainPanelVisible(true);
				
				theManFrame.updateUI();
				theManFrame.saveProject();
				theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		
		JButton btnResetDefaults = new JButton(Globalx.defaultBtn);
		btnResetDefaults.setBackground(Globals.BGCOLOR);
		btnResetDefaults.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setDefaults();
			}
		});
		
		JButton btnDiscard = new JButton(Globalx.cancelBtn);
		btnDiscard.setBackground(Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(theManFrame.isRemoveAnnoOnDiscard()) {
					theManFrame.clearCurrentAnnoDB();
					theManFrame.updateUI();
				}
				
				setVisible(false);
				theManFrame.setMainPanelVisible(true);
				theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theManFrame, "AnnoDB Help", helpHTML);	
			}
		});

		row = Static.createRowPanel();
		JPanel buttonPanel = Static.createRowPanel();
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		buttonPanel.add(btnKeep);			buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(btnDiscard);		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(btnResetDefaults);	buttonPanel.add(Box.createHorizontalStrut(10));
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
	// add new or edit selected; index is for new or selected
	public void updateAnnoDBEditUI(int index, boolean b) { // b=true is add, else edit
		nCurrentAnnoIndex = index;
		bAdd = b;
		ManagerData curManData = theManFrame.getCurManData();
		ManagerData.AnnodbData annoObj = curManData.getAnnoDBAt(index); // selected DB line
		
		boolean isLoaded=annoObj.isLoaded(); // added to db already (should be edit mode)
		setLoaded(isLoaded);
		
		txtTaxonomy.setText(annoObj.getTaxo());
		curTaxonomy=annoObj.getTaxo();
		
		txtTabFile.setText(annoObj.getTabularFile());
		txtDBfasta.setText(annoObj.getFastaDB());
		txtDate.setText(annoObj.getDate());
		
		boolean set = cmbSearchPgms.setSelectedItem(annoObj.getSearchPgm());
		if (!set) cmbSearchPgms.setSelectedIndex(0);
		
		txtParams.setText(annoObj.getParams());
		if (cmbSearchPgms.getSelectedItem().equals("diamond")) {
			saveDiaArgs = annoObj.getParams();
			saveBlaArgs = BlastArgs.getBlastArgsDB(); 
		}
		else {
			saveBlaArgs = annoObj.getParams();
			saveDiaArgs = BlastArgs.getDiamondArgsDB();
		}
		
		if (bAdd) enableFields(true);
		else {
			if (!isLoaded) { 
				String tabfile = annoObj.getTabularFile();
				if (!Globals.hasVal(tabfile)) enableFields(true);
				else enableFields(false);
			}
		}
	}
	private boolean keep() {
		String tax = txtTaxonomy.getText().trim();
		if (tax.equals("")) return rcMsg("Enter taxonomy string");
		else {
			if (!theManFrame.isValidID(tax)) 
				return rcMsg("Taxonomy string " + tax + " must be letters, digits or '_' (underscore)");
		}
		if (btnUseExistingHitFile.isSelected() && btnUseExistingHitFile.isEnabled()) {
			if (txtDBfasta.getText().trim().equals("")) 
				return rcMsg("Missing information -- Enter annoDB FASTA file");
					
			if (txtTabFile.getText().trim().equals("")) 
				return rcMsg("Missing information -- Enter hit tabular file");
		}
		else if (btnGenHitFile.isSelected() && btnGenHitFile.isEnabled()) {
			if (txtDBfasta.getText().trim().equals("")) 
				return rcMsg("Missing information -- Enter annoDB FASTA file");	
		}
		if (txtParams.getText().equals(""))
			return rcMsg("Missing information -- Params cannot be blank"); // CAS314 add	
	
		ManagerData curManData = theManFrame.getCurManData();
		ManagerData.AnnodbData annoObj = curManData.getAnnoDBAt(nCurrentAnnoIndex);
		
		String file = txtDBfasta.getText();
		if (bAdd && curManData.hasAnnoFile(file)) return false; 
		
		if (bIsLoaded && !curTaxonomy.equals(tax)) {
			if (UserPrompt.showConfirm("Edit annoDB", "Changed taxonomy from " + curTaxonomy + " to " + tax)) {
				if (!theManFrame.tcwDBupdateTaxonomyForLoadedAnnoDB(tax, curTaxonomy))
					tax = curTaxonomy;
			}
			else tax = curTaxonomy;
		}
		annoObj.setTaxo(tax);	
	
		annoObj.setFastaDB(file);
		annoObj.setDate(txtDate.getText());
		annoObj.setSearchPgm(cmbSearchPgms.getSelectedItem());
		annoObj.setParams(txtParams.getText());
		annoObj.setTabularFile(txtTabFile.getText());
		return true;
	}
	private boolean rcMsg(String msg) {
		JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.PLAIN_MESSAGE);
		return false;
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
		lblTabFile.setEnabled(e);
		txtTabFile.setEnabled(e);
		if (e==false) txtTabFile.setText("");
	}
	
	private void setParamDefaults () {
		String selected = cmbSearchPgms.getSelectedItem();
		
		if (selected.equals("blast")) {
			saveDiaArgs = txtParams.getText();
			txtParams.setText(saveBlaArgs);
		}
		else if (selected.equals("diamond")) {
			saveBlaArgs = txtParams.getText();
			txtParams.setText(saveDiaArgs);
		}
		else Out.bug("Illegal search program: " + selected);
	}
	
	private void setLoaded(boolean loaded) { 
		bIsLoaded = loaded; 
		
		btnGenHitFile.setEnabled(!bIsLoaded);
		lblDBfasta.setEnabled(!bIsLoaded);
		txtDBfasta.setEnabled(!bIsLoaded);
		btnDBfasta.setEnabled(!bIsLoaded);
		lblParams.setEnabled(!bIsLoaded);
		txtParams.setEnabled(!bIsLoaded);
		lblDate.setEnabled(!bIsLoaded);
		txtDate.setEnabled(!bIsLoaded);
		
		lblSearchPgms.setEnabled(!bIsLoaded);
		cmbSearchPgms.setEnabled(!bIsLoaded);
		
		btnUseExistingHitFile.setEnabled(!bIsLoaded);
		lblTabFile.setEnabled(!bIsLoaded);
		txtTabFile.setEnabled(!bIsLoaded);	
		btnTabFile.setEnabled(!bIsLoaded);
	}
	private void setDefaults() {// CAS314 add
		btnGenHitFile.setSelected(true);
		cmbSearchPgms.setSelectedIndex(0);
		saveDiaArgs=BlastArgs.getDiamondArgsDB();
		saveBlaArgs=BlastArgs.getBlastArgsDB();
		txtParams.setText(saveDiaArgs);
		txtDBfasta.setText("");
		txtTaxonomy.setText("");
		txtDate.setText("");
	}
	/****************************************************
	 * EditAnnoPanel private variables
	 */
	private ManagerFrame theManFrame = null;
	private JPanel pnlMain = null;
	
	private JLabel lblTaxonomy = null;
	private JTextField txtTaxonomy = null;
	
	private JLabel lblDBfasta = null;
	private JTextField txtDBfasta = null;
	private JButton btnDBfasta = null;
	
	private JRadioButton btnGenHitFile = null;
	private JLabel lblSearchPgms=null;
	private ButtonComboBox cmbSearchPgms = null;
	private JLabel lblParams = null;
	private JTextField txtParams = null;
	
	private JRadioButton btnUseExistingHitFile = null;
	private JLabel lblTabFile = null;
	private JTextField txtTabFile = null;
	private JButton btnTabFile = null;
	
	private JLabel lblDate = null;
	private JTextField txtDate = null;
	
	private boolean bIsLoaded = false;
	private boolean bAdd = true;
	private int nCurrentAnnoIndex = -1;
	private String curTaxonomy="";
	
	private String saveDiaArgs="", saveBlaArgs=""; 
}
