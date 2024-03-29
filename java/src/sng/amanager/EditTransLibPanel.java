package sng.amanager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import sng.database.Globals;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileRead;
import util.ui.UserPrompt;
import util.methods.Static;

/***
 * Sequence Dataset Add/Edit 
 * CAS316 change from FileTextField to FileChooser
 */

public class EditTransLibPanel extends JPanel {
	private static final long serialVersionUID = -2386563315902142612L;
	private final String helpHTML = Globals.helpRunDir + "EditTransLibPanel.html";
	private final String PROJDIR = Globalx.PROJDIR + "/";
	
	public static final String [] TRANS_LIB_SYMBOLS = { "SeqID", "Sequence File", 
		 "Count File", "Quality File", "   5' suffix", "   3' suffix"};
	
	public static final String [] ATTRIBUTE_SYMBOLS = 
		{ "Title", "Species", "Cultivar", "Strain", "Tissue", "Stage", "Treatment",  
		"Year", "Source" };
	
	private static final int COLUMN_LABEL_WIDTH = 140;
	private static final int TEXTFIELD_WIDTH_SHORT = 6; // CAS304 was 8
	private static final int TEXTFIELD_WIDTH = 30;

	public EditTransLibPanel(ManagerFrame parentFrame) {
		theManFrame = parentFrame;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		createTransReadPanel();
		pnlBuildCountFile = new BuildRepFilePanel(parentFrame, this);
		pnlBuildCountFile.setVisible(false);
		
		add(pnlMainPanel);
		add(pnlBuildCountFile);
		
		setVisible(false);
	}
	public void setEditTransVisible() { 
		theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_TRANS_LIB_EDIT);
		pnlBuildCountFile.setVisible(false);
		pnlMainPanel.setVisible(true);
	}
	private void createTransReadPanel() {
		pnlMainPanel = createPagePanel(false);
		pnlMainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
		JPanel row = createRowPanel();
		JLabel title = Static.createLabel("Add or Edit a Sequence Dataset");
		title.setFont(pnlMainPanel.getFont().deriveFont(Font.BOLD, 18));
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(20));
		
		row = createRowPanel();
		lblTransLibID = Static.createLabel(TRANS_LIB_SYMBOLS[0]); // Trans/Read ID
		tfTransLibID = createTextField(TEXTFIELD_WIDTH_SHORT);		
		row.add(lblTransLibID);
		if (lblTransLibID.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblTransLibID.getPreferredSize().width));
		row.add(tfTransLibID);
		row.add(Box.createHorizontalStrut(10));
		row.add(Static.createLabel("- Required  (Less than 8 characters, no spaces)"));
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(10));
		
		row = createRowPanel();
		lblSeqFile = Static.createLabel(TRANS_LIB_SYMBOLS[1]); // Sequence file
		tfSeqFile = createTextField(TEXTFIELD_WIDTH);
		row.add(tfSeqFile);
		row.add(Box.createHorizontalStrut(1));
		btnSeqFile = Static.createButtonFile("...", true);
		btnSeqFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnSeqFile, "Seq File", FileC.dPROJ, FileC.fFASTA)) { // CAS316 was in-file choooser
					tfSeqFile.setText(fc.getRemoveFixedPath());
					
					String title = tfAttr[0].getText();
					if (title==null || title.equals("")) {
						title = tfSeqFile.getText();
						if (!title.equals("")) { // CAS304
							title = title.substring(title.lastIndexOf("/")+1);
							title = title.substring(0, title.indexOf("."));
							tfAttr[0].setText(title);
						}
					}
				}
			}
		});
		row.add(lblSeqFile);
		if(lblSeqFile.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblSeqFile.getPreferredSize().width));
		row.add(tfSeqFile);  row.add(Box.createHorizontalStrut(1));
		row.add(btnSeqFile); row.add(Box.createHorizontalStrut(10));
		row.add(Static.createLabel("- Required"));
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(20));
		
		row = createRowPanel();
		lblCountFile = Static.createLabel(TRANS_LIB_SYMBOLS[2]); 	// Count file
		tfCountFile = createTextField(TEXTFIELD_WIDTH);
		btnCountFile = Static.createButtonFile("...", true);
		btnCountFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnCountFile, "Count File", FileC.dPROJ, FileC.fTXT)) { 
					tfCountFile.setText(fc.getRemoveFixedPath());
				}
			}
		});
		row.add(lblCountFile);
		if(lblCountFile.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblCountFile.getPreferredSize().width));
		row.add(tfCountFile);row.add(Box.createHorizontalStrut(1));
		row.add(btnCountFile); 
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(5));
		
		row = createRowPanel();
		btnGenCountFile = Static.createButtonPanel("Build from multiple count files", true);
		btnGenCountFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(tfSeqFile.getText().length() > 0) {
					theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_BUILD_REPS);
					String file = tfSeqFile.getText();
					if (!file.contains("/")) 
						file = PROJDIR + theManFrame.getProjDir() +"/"+file;
					pnlBuildCountFile.setSeqFile(file);
					pnlMainPanel.setVisible(false);
					pnlBuildCountFile.setVisible(true); // see GenerateFileSelector in this file 
				}
				else
					JOptionPane.showMessageDialog(theManFrame, "Please enter Sequence File first", 
							"Build count file", JOptionPane.PLAIN_MESSAGE);
			}
		});
		row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH));
		row.add(Static.createLabel("or")); row.add(Box.createHorizontalStrut(5));
		row.add(btnGenCountFile);
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(20));

		row = createRowPanel();
		lblQualFile = Static.createLabel(TRANS_LIB_SYMBOLS[3]); 	// Qual file
		tfQualFile = createTextField(TEXTFIELD_WIDTH);
		btnQualFile = Static.createButtonFile("...", true);;
		btnQualFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnCountFile, "Qual File", FileC.dPROJ, FileC.fQUAL)) { 
					tfQualFile.setText(fc.getRemoveFixedPath());
				}
			}
		});
		row.add(lblQualFile);
		if(lblQualFile.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblQualFile.getPreferredSize().width));
		row.add(tfQualFile);row.add(Box.createHorizontalStrut(1));
		row.add(btnQualFile); 
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(10));
		
		row = createRowPanel();					// 5' 3'
		lbl5pSuffix = Static.createLabel(TRANS_LIB_SYMBOLS[4]);
		tf5pSuffix = createTextField(4);
		
		lbl3pSuffix = Static.createLabel(TRANS_LIB_SYMBOLS[5]);
		tf3pSuffix = createTextField(4);
		tf5pSuffix.setText(Globals.def5p); tf3pSuffix.setText(Globals.def3p);
		
		lblAttr = new JLabel[ATTRIBUTE_SYMBOLS.length];
		tfAttr = new JTextField[ATTRIBUTE_SYMBOLS.length];
		for(int x=0; x<ATTRIBUTE_SYMBOLS.length; x++) {
			lblAttr[x] = Static.createLabel(ATTRIBUTE_SYMBOLS[x]);
			tfAttr[x] = createTextField(TEXTFIELD_WIDTH);
		}
		JLabel lbl = Static.createLabel("Sanger ESTs ");
		row.add(lbl);
		if(lbl.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lbl.getPreferredSize().width));
		row.add(Box.createHorizontalStrut(10));
		row.add(lbl5pSuffix);  row.add(Box.createHorizontalStrut(3));
		row.add(tf5pSuffix);  row.add(Box.createHorizontalStrut(10));
		row.add(lbl3pSuffix); row.add(Box.createHorizontalStrut(3));
		row.add(tf3pSuffix);  row.add(Box.createHorizontalStrut(10)); 
		row.add(Static.createLabel("(Defaults .f and .r)"));
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(25));
		
	// Attributes
		pnlMainPanel.add(Static.createLabel("ATTRIBUTES:"));
		pnlMainPanel.add(Box.createVerticalStrut(10));
		
		for(int x=0; x<lblAttr.length; x++) {
			row = createRowPanel();
			row.add(lblAttr[x]);
			if(lblAttr[x].getPreferredSize().width < COLUMN_LABEL_WIDTH)
				row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblAttr[x].getPreferredSize().width));
			new JTextField(TEXTFIELD_WIDTH);
			row.add(tfAttr[x]);
			pnlMainPanel.add(row);
			if(x< (lblAttr.length - 1))
				pnlMainPanel.add(Box.createVerticalStrut(10));
		}

	// Row of buttons
		btnKeep = Static.createButton("Keep");
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!transIDValid()) return;
				if (!transIDduplicate()) return;
				String sf = tfSeqFile.getText();
				if (sf.equals("")) {
					UserPrompt.showError("A Sequence File must be entered");
					return;
				}
				
				if(keep()) {
					theManFrame.tcwDBupdateLibAttributes(0);
					setVisible(false);
					theManFrame.setMainPanelVisible(true);
					theManFrame.updateUI();
					theManFrame.saveProject();
					theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				}				
			}
		});
		btnDiscard = Static.createButton("Cancel");
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(theManFrame.isAddTransLib()) {
					theManFrame.clearCurrentTransLib();
					theManFrame.updateUI();
				}
				setVisible(false); 
				theManFrame.setMainPanelVisible(true);				
				theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theManFrame, "Sequence Dataset Help", helpHTML);
			}
		});

		JPanel buttonRow = createRowPanel();
		buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonRow.add(btnKeep);		buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnDiscard);	buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnHelp);
		buttonRow.setMaximumSize(buttonRow.getPreferredSize());
		buttonRow.setMinimumSize(buttonRow.getPreferredSize());
		
		JPanel outerPanel = createPagePanel(true);
		outerPanel.add(buttonRow);

		pnlMainPanel.add(Box.createVerticalStrut(20));
		pnlMainPanel.add(outerPanel);
		pnlMainPanel.setMaximumSize(pnlMainPanel.getPreferredSize());
		pnlMainPanel.setMinimumSize(pnlMainPanel.getPreferredSize());
		
		Dimension d = outerPanel.getPreferredSize();
		d.width = pnlMainPanel.getPreferredSize().width;
		outerPanel.setPreferredSize(d);
		outerPanel.setMaximumSize(outerPanel.getPreferredSize());
		outerPanel.setMinimumSize(outerPanel.getPreferredSize());
	}
	
	/*******************************************************************
	 * called by ManagerFrame add or edit -- 
	 * 		its already created curManData.SeqData object for Add
	 */
	public void updateTransLibEditUI(int index, boolean dbExists, boolean bAdd) {
		nCurTransLibIndex = index;
		bAddTransLib = bAdd;
		
		ManagerData curManData = theManFrame.getCurManData();
		projDirName = theManFrame.getProjDir();
		
		ManagerData.SeqData x = curManData.getTransLibraryAt(index);
		strOldTransLibID = x.getSeqID();
		tfTransLibID.setText(x.getSeqID());
		
		tfSeqFile.setText(x.getSeqFile());
		tfQualFile.setText(x.getQualFile());
		tfCountFile.setText(x.getCountFile());
		tf5pSuffix.setText(x.getFivePrimeSuffix());
		tf3pSuffix.setText(x.getThreePrimeSuffix());
		tfAttr[0].setText(x.getAttr().getTitle());
		tfAttr[1].setText(x.getAttr().getOrganism());
		tfAttr[2].setText(x.getAttr().getCultivar());
		tfAttr[3].setText(x.getAttr().getStrain());
		tfAttr[4].setText(x.getAttr().getTissue());
		tfAttr[5].setText(x.getAttr().getStage());
		tfAttr[6].setText(x.getAttr().getTreatment());
		tfAttr[7].setText(x.getAttr().getYear());
		tfAttr[8].setText(x.getAttr().getSource());
		
		// If add, everything is editable
		// If edit, if the !dbExists, can edit ID but not files (need to make then update right to be all editable); 
		boolean doEdit=true;
		if (!bAddTransLib && !dbExists) doEdit=false;
		tfTransLibID.setEnabled(doEdit);
		
		tfSeqFile.setEnabled(bAddTransLib); // Seqfile 
		tfQualFile.setEnabled(bAddTransLib); // qualfile
		tfCountFile.setEnabled(bAddTransLib); // countfile
		btnGenCountFile.setEnabled(bAddTransLib);
		tf5pSuffix.setEnabled(doEdit); // 5'
		tf3pSuffix.setEnabled(doEdit); //3'
	}
	/**********************************************
	 * Keep
	 */
	private boolean keep() {
		ManagerData curManData = theManFrame.getCurManData();
		String transID = tfTransLibID.getText();		
	
		ManagerData.SeqData x = curManData.getTransLibraryAt(nCurTransLibIndex);
		x.setSeqID(transID);
		x.setSeqFile(tfSeqFile.getText());
		x.setQualFile(tfQualFile.getText());
		x.setExpFile(tfCountFile.getText());
		x.setFivePrimeSuffix(tf5pSuffix.getText());
		x.setThreePrimeSuffix(tf3pSuffix.getText());
		
		// The expression level file(s) were just defined, so need to read 
		if(bAddTransLib) { 
            if(x.getCountFile().length() > 0) {
            	String outFileName = x.getCountFile();
            	if (!outFileName.startsWith("/")) 
            		outFileName = PROJDIR + curManData.getProjDir() + "/" + x.getCountFile();
                File temp = new File(outFileName);
            
                String [] countLibs = curManData.readLibCountsFromFile(temp);
                if (countLibs==null) return false;
                
                boolean rc = curManData.addLibsToCount(x.getSeqID(), countLibs);
                if (!rc) return false;
            }
        }
		else { // seqID changed
			String oldName = getOldTransLibID();
			for(int i=0; i<curManData.getNumCountLibs(); i++) {
				if(curManData.getCountLibAt(i).getSeqID().equals(oldName)) {
					curManData.getCountLibAt(i).setSeqID(transID);
				}
			}
		}
		x.getAttr().setTitle(tfAttr[0].getText());
		x.getAttr().setOrganism(tfAttr[1].getText());
		x.getAttr().setCultivar(tfAttr[2].getText());
		x.getAttr().setStrain(tfAttr[3].getText());
		x.getAttr().setTissue(tfAttr[4].getText());
		x.getAttr().setStage(tfAttr[5].getText());
		x.getAttr().setTreatment(tfAttr[6].getText());	
		x.getAttr().setYear(tfAttr[7].getText());
		x.getAttr().setSource(tfAttr[8].getText());
		
		return true;
	}
	/** Utilty checks and interface **/
	private boolean transIDValid() {
		String id = tfTransLibID.getText();
		if (id.length()==0 || id.equals("")) {
			UserPrompt.showError("Please enter a seqID \n");
			return false;
		}
		if (id.length()>8) {
			UserPrompt.showError("seqID must be <= 8 character \n");
			return false;
		}
		if (!id.matches("[A-Za-z]+[A-Za-z0-9_]*")) { 
			UserPrompt.showError("seqID must start with a letter, and be composed of letters, digits or underscore\n");
			return false;
		}
		return true;
	}
	public boolean transIDduplicate() {
		ManagerData curManData = theManFrame.getCurManData();
		String transID = tfTransLibID.getText();

		for(int x=0; x<curManData.getNumSeqLibs(); x++) {
			if(x != nCurTransLibIndex) {
				if(curManData.getTransLibraryAt(x).getSeqID().equals(transID)) {
					UserPrompt.showError(transID + " seqID is a duplicate, please select another");
					return false;
				}
			}
		}
		return true;
	}
	private JPanel createRowPanel() {
		JPanel retVal = new JPanel();
		retVal.setLayout(new BoxLayout(retVal, BoxLayout.LINE_AXIS));
		retVal.setAlignmentX(Component.LEFT_ALIGNMENT);
		retVal.setBackground(Globalx.BGCOLOR);
		
		return retVal;
	}
	private JPanel createPagePanel(boolean left) {
		JPanel retVal = new JPanel();
		retVal.setLayout(new BoxLayout(retVal, BoxLayout.PAGE_AXIS));
		if (left) retVal.setAlignmentX(Component.LEFT_ALIGNMENT);
		else retVal.setAlignmentX(Component.CENTER_ALIGNMENT);
		retVal.setBackground(Globalx.BGCOLOR);
		
		return retVal;
	}
	private JTextField createTextField(int w) {
		JTextField retVal = new JTextField(w);
		retVal.setMaximumSize(retVal.getPreferredSize());
		retVal.setMinimumSize(retVal.getPreferredSize());
		return retVal;
	}
	
	public EditTransLibPanel getInstance() { return this; }

	public String getOldTransLibID() { return strOldTransLibID; }
	public void setCountFile(String name) {tfCountFile.setText(name);} 
	public void clearCountFileList() { pnlBuildCountFile.clearCountFileList(); }
	
	/****************************************************
	 * private variables
	 */
	private ManagerFrame theManFrame = null;
	
	private JPanel pnlMainPanel = null;
	
	private JLabel lblTransLibID = null;
	private JTextField tfTransLibID = null;
	
	private JLabel lblSeqFile = null;
	private JButton btnSeqFile = null;
	private JTextField tfSeqFile = null;
	
	private JLabel lblQualFile = null;
	private JButton btnQualFile = null;	
	private JTextField tfQualFile = null;
	
	private JLabel lblCountFile = null;
	private JButton btnCountFile = null;
	private JTextField tfCountFile = null;
	
	private JLabel lbl5pSuffix = null;
	private JTextField tf5pSuffix = null;
	
	private JLabel lbl3pSuffix = null;
	private JTextField tf3pSuffix = null;
	
	private JLabel [] lblAttr = null;
	private JTextField [] tfAttr = null;
	
	private JButton btnGenCountFile = null;

	private JButton btnKeep = null, btnDiscard = null, btnHelp = null;
	
	private BuildRepFilePanel pnlBuildCountFile = null;
	private String strOldTransLibID = "";
	private int nCurTransLibIndex = -1;
	private boolean bAddTransLib=false;
	private String projDirName=null;
}
