package cmp.compile.panels;

/***********************************************************
 * The "Setting" window for blast panel
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import util.database.Globalx;
import util.methods.BlastArgs;
import util.methods.Out;
import util.methods.Static;
import util.methods.ErrorReport;
import util.ui.ButtonComboBox;
import util.ui.UserPrompt;
import cmp.database.Globals;

public class EditBlastPanel extends JPanel {
	private static final long serialVersionUID = -6189127919667748457L;
	
	private final String helpHTML = Globals.helpRunDir + "EditBlastPanel.html";
	private final String BLAST_AA_TAB = Globals.Search.BLAST_AA_TAB;
	private final String BLAST_NT_TAB = Globals.Search.BLAST_NT_TAB;

	private final int AA=Globals.AA;
	private final int NT=Globals.NT;
	private final String fexists = "- file exists";
	
	public EditBlastPanel(CompilePanel parentPanel) {
		theCompilePanel = parentPanel;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		add(Box.createVerticalStrut(15));
		
		add(createBlast(AA)); 
		add(createBlast(NT)); 
		
	// Lower buttons
		btnKeep = new JButton(Globalx.keepBtn);
		btnKeep.setBackground(Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				keep();
				theCompilePanel.mTCWcfgSave();
				theCompilePanel.getBlastPanel().setBlastSummary();
				setVisible(false);
				theCompilePanel.setMainPanelVisible(true);
			}
		});
		
		btnDiscard = new JButton(Globalx.cancelBtn);
		btnDiscard.setBackground(Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				restore();
				theCompilePanel.getBlastPanel().setBlastSummary();
				setVisible(false);
				theCompilePanel.setMainPanelVisible(true);
			}
		});
		btnDef = new JButton(Globalx.defaultBtn);
		btnDef.setBackground(Globals.BGCOLOR);
		btnDef.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setDefaults();
			}
		});
		
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theCompilePanel.getParentFrame(), "Edit Search Settings", helpHTML);
			}
		});
	
		buttonPanel = Static.createRowPanel();
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnKeep);    buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnDiscard); buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnDef); 	 buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnHelp);
		buttonPanel.add(Box.createHorizontalGlue());
		add(buttonPanel);
		add(Box.createVerticalStrut(30));
		
		setMaximumSize(getPreferredSize()); 
		setMinimumSize(getPreferredSize());
	}
	/*******************************************************
	 * AA and NT interface
	 */
	private JPanel createBlast(int tp) {
		JPanel page = Static.createPagePanel();
		int index2=40;
		
		JPanel row = Static.createRowPanel();
		String msg = (tp==AA) ? "AA (protein)" : "NT (nucleotide)";
		chkBlast[tp] = Static.createCheckBox(msg); 
		chkBlast[tp].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePanel();
			}
		});
		if (tp==AA) {
			row.add(Box.createHorizontalStrut(3));
			row.add(new JLabel(msg)); 
		}
		else row.add(chkBlast[tp]);
		
		row.add(Box.createHorizontalStrut(3));
		lblBlastExists[tp] = new JLabel("");
		row.add(lblBlastExists[tp]);
		
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(index2));
		lblBlastFile[tp] = Static.createLabel(""); 
		row.add(lblBlastFile[tp]);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		cmbSearchPgms[tp] = new ButtonComboBox ();
		Vector <String> pgm = BlastArgs.getSearchPgms();
		for (String p: pgm) 
			cmbSearchPgms[tp].addItem(p);
		cmbSearchPgms[tp].setSelectedIndex(0); 

		cmbSearchPgms[tp].setMaximumSize(cmbSearchPgms[tp].getPreferredSize());
		cmbSearchPgms[tp].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String pgm = cmbSearchPgms[AA].getSelectedItem();
				String args = getSearchArgs(pgm);
				txtParams[AA].setText(args);
			}
		});
		if (tp==AA) {
			row = Static.createRowPanel();
			row.add(Box.createHorizontalStrut(index2));
			row.add(Static.createLabel("Search program"));
			row.add(Box.createHorizontalStrut(2));
			row.add(cmbSearchPgms[AA]);
			
			page.add(row);
			page.add(Box.createVerticalStrut(5));
		}
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(index2));	
		row.add(Static.createLabel("Parameters"));
		row.add(Box.createHorizontalStrut(5));
		
		msg = (tp==AA) 	? BlastArgs.getDiamondArgsORF() 
						: BlastArgs.getBlastnArgs(); 		// CAS316 was wrong in 315
		txtParams[tp] = Static.createTextField(msg, 25);
		row.add(Box.createHorizontalStrut(5));
		row.add(txtParams[tp]);
		page.add(row);
		page.add(Box.createVerticalStrut(10));
		
		page.add(Box.createVerticalStrut(20));
		return page;
	}

	private String getSearchArgs(String selected) {
		if (selected.equals("blast")) 			return BlastArgs.getBlastArgsORF();
		else if (selected.equals("diamond")) 	return BlastArgs.getDiamondArgsORF();
		else return "";
	}
	
	/***********************************************************/
	private void keep() {
		for (int tp=0; tp<2; tp++) {
			String blastPath = lblBlastFile[tp].getText().trim();
			File f = new File(blastPath);
			blastExists[tp]=f.exists();
		}
	}
	
	/******************************************************************
	 * Startup of Settings, Read mTCW.cfg, and changes
	 */
	public void updatePanel() {
		save();
	
		boolean dbExists = (theCompilePanel.dbIsExist()); // CAS325 was exiting on !dbExists
		for (int tp=0; tp<2; tp++) {			
			boolean bUse = chkBlast[tp].isSelected();
			if (dbExists && tp==1) {
				if (theCompilePanel.getDBInfo().nNTdb()==0) {
					chkBlast[tp].setSelected(false);
					chkBlast[tp].setEnabled(false); 
					bUse=false;
				}
			}
			lblBlastFile[tp].setEnabled(bUse); 
			lblBlastExists[tp].setEnabled(bUse);
			cmbSearchPgms[tp].setEnabled(bUse);
			txtParams[tp].setEnabled(bUse);
		}
	}
	/***************************************************************/
	public boolean isBlast(int tp) { 
		if (tp==1) {
			if (theCompilePanel!=null) {
				if (theCompilePanel.getDBInfo()!=null) {
					if (theCompilePanel.getDBInfo().nNTdb()==0) return false;
				}
			}
			return chkBlast[tp].isSelected();
		}
		return chkBlast[tp].isSelected();
	}
		
	// called for processing blast file
	public String getBlastFile(int tp) {return lblBlastFile[tp].getText();}
	public boolean isBlastExists(int tp) {return blastExists[tp];}
	
	public String getSearchPgm(int tp) { 
		if (tp==0) 	return cmbSearchPgms[tp].getSelectedItem();
		else 		return "blastn";
	}
	public String getBlastParams(int tp) { return txtParams[tp].getText(); }
	
	public void clearInterface() {
		setDefaults();
		
		resetFile();
		
		save(); // CAS325
	}
	public void resetFile() { // when blast directory removed
		for (int tp=0; tp<2; tp++) {
			blastExists[tp] = false;
			lblBlastExists[tp].setText("");
		}
		updatePanel();
	}
	public void updateBlast(int tp) {
		blastExists[tp] = true;
		lblBlastExists[tp].setText(fexists);
	}
	
	/**********************************************************
	 * cfg interface
	 */
	public void cfgFileNoBlastN() {
		chkBlast[1].setSelected(false);
	}
	public void cfgFileDefaults() { // called at startup of edit panel, when summary is computed, and resetFiles
		String projPath = theCompilePanel.getBlastPanel().getDefaultBlastDirRelPath();
	
		for (int tp=0; tp<2; tp++) {
			if (tp==0) 	fullBlastPath[tp] = projPath + "/"  + BLAST_AA_TAB;
			else 		fullBlastPath[tp] = projPath + "/"  + BLAST_NT_TAB;
			
			lblBlastFile[tp].setText(fullBlastPath[tp]);
			
			boolean exists =  new File(fullBlastPath[tp]).exists();
			blastExists[tp] = exists;
			
			String msg = (exists) ? fexists : "";
			lblBlastExists[tp].setText(msg);
		
			chkBlast[tp].setSelected(true);
		}
		updatePanel(); 
	}

	public void cfgSetParams(int tp, String params) {
		txtParams[tp].setText(params); 
	}
	public void cfgSetPgm(int tp, String pgm) {
		Vector <String> pgms = BlastArgs.getSearchPgms();
		if (pgms.contains(pgm)) {
			cmbSearchPgms[tp].setSelectedItem(pgm);
			txtParams[tp].setText(getSearchArgs(pgm));
		}
		else 
			Out.PrtWarn("mTCW.cfg defined unavailable search program:" + pgm);
	}
	/*********************************************************/
	private void save() {
		for (int i=0; i<2; i++) {
			schkBlast[i] 		= chkBlast[i].isSelected();
			slblBlastFile[i]	= lblBlastFile[i].getText();
			slblBlastExists[i]	= lblBlastExists[i].getText();
			stxtParams[i]		= txtParams[i].getText();
			scmbSearchPgms[i] 	= cmbSearchPgms[i].getSelectedItem();
		}
	}
	private void restore() {
		for (int i=0; i<2; i++) {
			chkBlast[i].setSelected(schkBlast[i]);
			lblBlastFile[i].setText(slblBlastFile[i]);
			lblBlastExists[i].setText(slblBlastExists[i]);
			cmbSearchPgms[i].setSelectedItem(scmbSearchPgms[i]);
			txtParams[i].setText(stxtParams[i]);
		}
	}
	private void setDefaults() { // CAS325 from Defaults and on clearInterface
		chkBlast[AA].setSelected(true);
		cmbSearchPgms[AA].setSelectedIndex(0);
		txtParams[AA].setText(BlastArgs.getDiamondArgsORF());
		
		chkBlast[NT].setSelected(true);
		txtParams[NT].setText(BlastArgs.getBlastnArgs()); 
	}
	private JPanel buttonPanel = null;
	
	// Blast
	private JCheckBox [] chkBlast = new JCheckBox [2];
	private JLabel [] lblBlastFile= new JLabel [2];
	private JLabel [] lblBlastExists= new JLabel [2];
	private ButtonComboBox [] cmbSearchPgms = new ButtonComboBox [2]; // NT is not used at this time
	private JTextField [] txtParams = new JTextField [2];
	private boolean [] blastExists = {false, false};
	
	// Blast save
	private boolean [] schkBlast = 		new boolean [2];
	private String [] slblBlastFile= 	new String [2];
	private String [] slblBlastExists= 	new String [2];
	private String [] scmbSearchPgms = 	new String [2]; // NT is not used at this time
	private String [] stxtParams = 		new String [2]; 
	
	private JButton btnKeep = null, btnDiscard = null,  btnHelp = null, btnDef = null;
	private CompilePanel theCompilePanel = null;
	private String [] fullBlastPath = new String [2];
}
