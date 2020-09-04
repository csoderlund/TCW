package sng.amanager;

/***
 * Sequence Dataset Add/Edit 
 */
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
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import sng.database.Globals;
import util.database.Globalx;
import util.ui.UserPrompt;

public class EditTransLibPanel extends JPanel {
	private static final long serialVersionUID = -2386563315902142612L;
	private final String LIBDIR = Globalx.PROJDIR + "/";
	
	public static final String [] TRANS_LIB_SYMBOLS = { "SeqID", "Sequence File", 
		 "Count File", "Quality File", "   5' suffix", "   3' suffix"};
	
	public static final String [] ATTRIBUTE_SYMBOLS = 
		{ "Title", "Species", "Cultivar", "Strain", "Tissue", "Stage", "Treatment",  
		"Year", "Source" };
	
	private static final int COLUMN_LABEL_WIDTH = 140;
	private static final int TEXTFIELD_WIDTH_SHORT = 6; // CAS304 was 8
	private static final int TEXTFIELD_WIDTH = 30;

	public EditTransLibPanel(ManagerFrame parentFrame) {
		theParentFrame = parentFrame;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		createTransReadPanel();
		pnlBuildCountFile = new BuildRepFilePanel(parentFrame, this);
		pnlBuildCountFile.setVisible(false);
		
		add(pnlMainPanel);
		add(pnlBuildCountFile);
		
		setVisible(false);
	}
	public void setEditTransVisible() { 
		theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_TRANS_LIB_EDIT);
		pnlBuildCountFile.setVisible(false);
		pnlMainPanel.setVisible(true);
	}
	private void createTransReadPanel() {
		pnlMainPanel = createPagePanel(false);
		
		JLabel title = new JLabel("Add or Edit a Sequence Dataset");
		title.setFont(pnlMainPanel.getFont().deriveFont(Font.BOLD, 18));
		JPanel row = createRowPanel();
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(20));
		
		lblTransLibID = new JLabel(TRANS_LIB_SYMBOLS[0]); // Trans/Read ID
		tfTransLibID = createTextField(TEXTFIELD_WIDTH_SHORT);		

		lblSeqFile = new JLabel(TRANS_LIB_SYMBOLS[1]); // Sequence file
		tfSeqFile = new FileTextField(theParentFrame, FileTextField.LIB, FileTextField.FASTA);
		tfSeqFile.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent arg0) {
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
		});

		lblCountFile = new JLabel(TRANS_LIB_SYMBOLS[2]); 
		tfCountFile = new FileTextField(theParentFrame, FileTextField.LIB, FileTextField.COUNT);

		lblQualFile = new JLabel(TRANS_LIB_SYMBOLS[3]); 
		tfQualFile = new FileTextField(theParentFrame, FileTextField.LIB, FileTextField.QUAL);
		
		lbl5pSuffix = new JLabel(TRANS_LIB_SYMBOLS[4]);
		tf5pSuffix = createTextField(4);
		
		lbl3pSuffix = new JLabel(TRANS_LIB_SYMBOLS[5]);
		tf3pSuffix = createTextField(4);
		tf5pSuffix.setText(Globals.def5p); tf3pSuffix.setText(Globals.def3p);
		
		lblAttr = new JLabel[ATTRIBUTE_SYMBOLS.length];
		tfAttr = new JTextField[ATTRIBUTE_SYMBOLS.length];
		for(int x=0; x<ATTRIBUTE_SYMBOLS.length; x++) {
			lblAttr[x] = new JLabel(ATTRIBUTE_SYMBOLS[x]);
			tfAttr[x] = createTextField(TEXTFIELD_WIDTH);
		}
		
		btnGenCountFile = new JButton("Build from multiple count files");
		btnGenCountFile.setBackground(Globalx.MENUCOLOR);
		btnGenCountFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(tfSeqFile.getText().length() > 0) {
					theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_BUILD_REPS);
					String file = tfSeqFile.getText();
					if (!file.contains("/")) 
						file = LIBDIR + theParentFrame.getProjectName() +"/"+file;
					pnlBuildCountFile.setSeqFile(file);
					pnlMainPanel.setVisible(false);
					pnlBuildCountFile.setVisible(true); // see GenerateFileSelector in this file 
				}
				else
					JOptionPane.showMessageDialog(theParentFrame, "Please enter Sequence File first", 
							"Build count file", JOptionPane.PLAIN_MESSAGE);
			}
		});
		
		row = createRowPanel();
		row.add(lblTransLibID);
		if(lblTransLibID.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - 
					lblTransLibID.getPreferredSize().width));
		row.add(tfTransLibID);
		row.add(Box.createHorizontalStrut(10));
		row.add(new JLabel("- Required  (Less than 8 characters, no spaces)"));
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(10));

		row = createRowPanel();
		row.add(lblSeqFile);
		if(lblSeqFile.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblSeqFile.getPreferredSize().width));
		row.add(tfSeqFile);
		row.add(Box.createHorizontalStrut(10));
		row.add(new JLabel("- Required"));
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(20));

		row = createRowPanel();
		row.add(lblCountFile);
		if(lblCountFile.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblCountFile.getPreferredSize().width));
		row.add(tfCountFile);
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(5));
		
		row = createRowPanel();
		row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH));
		row.add(new JLabel("or")); row.add(Box.createHorizontalStrut(5));
		row.add(btnGenCountFile);
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(20));

		row = createRowPanel();
		row.add(lblQualFile);
		if(lblQualFile.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lblQualFile.getPreferredSize().width));
		row.add(tfQualFile);
		pnlMainPanel.add(row);
		pnlMainPanel.add(Box.createVerticalStrut(10));
		
		row = createRowPanel();
		JLabel lbl = new JLabel("Sanger ESTs ");
		row.add(lbl);
		if(lbl.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lbl.getPreferredSize().width));
		row.add(Box.createHorizontalStrut(10));
		row.add(lbl5pSuffix);  row.add(Box.createHorizontalStrut(3));
		row.add(tf5pSuffix);  row.add(Box.createHorizontalStrut(10));
		row.add(lbl3pSuffix); row.add(Box.createHorizontalStrut(3));
		row.add(tf3pSuffix);  row.add(Box.createHorizontalStrut(10)); 
		row.add(new JLabel("(Defaults .f and .r)"));
		pnlMainPanel.add(row);
		
		pnlMainPanel.add(Box.createVerticalStrut(20));
		
		pnlMainPanel.add(new JLabel("ATTRIBUTES:"));
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
		
		pnlMainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		btnKeep = new JButton("Keep");
		btnKeep.setBackground(Globalx.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!transIDValid()) return;
				if (!transIDduplicate()) return;
				String sf = tfSeqFile.getText();
				if (sf.equals("")) {
					UserPrompt.showError("A Sequence File must be entered");
					return;
				}
				
				if(updateKeepThisUI()) {
					theParentFrame.tcwDBupdateLibAttributes(0);
					setVisible(false);
					theParentFrame.setMainPanelVisible(true);
					theParentFrame.updateUI();
					theParentFrame.saveProject();
					theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				}				
			}
		});
		btnDiscard = new JButton("Cancel");
		btnDiscard.setBackground(Globalx.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(theParentFrame.isAddTransLib()) {
					theParentFrame.clearCurrentTransLib();
					theParentFrame.updateUI();
				}
				setVisible(false); 
				theParentFrame.setMainPanelVisible(true);				
				theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globalx.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Sequence Dataset Help", "html/runSingleTCW/EditTransLibPanel.html");
			}
		});

		JPanel buttonRow = createRowPanel();
		buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonRow.add(btnKeep);
		buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnDiscard);
		buttonRow.add(Box.createHorizontalStrut(15));
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
		ManagerData curManData = theParentFrame.getCurManData();
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
	private boolean updateKeepThisUI() {
		ManagerData curManData = theParentFrame.getCurManData();
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
            		if (!outFileName.startsWith("/")) outFileName = LIBDIR + curManData.getProjectName() + "/" + x.getCountFile();
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
		ManagerData curManData = theParentFrame.getCurManData();
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
	private ManagerFrame theParentFrame = null;
	
	private JPanel pnlMainPanel = null;
	
	private JLabel lblTransLibID = null;
	private JTextField tfTransLibID = null;
	
	private JLabel lblSeqFile = null;
	private FileTextField tfSeqFile = null;	
	
	private JLabel lblQualFile = null;
	private FileTextField tfQualFile = null;	
	
	private JLabel lblCountFile = null;
	private FileTextField tfCountFile = null;
	
	private JLabel lbl5pSuffix = null;
	private JTextField tf5pSuffix = null;
	
	private JLabel lbl3pSuffix = null;
	private JTextField tf3pSuffix = null;
	
	private JLabel [] lblAttr = null;
	private JTextField [] tfAttr = null;
	
	private JButton btnGenCountFile = null;

	private JButton btnKeep = null;
	private JButton btnDiscard = null;
	private JButton btnHelp = null;
	
	private BuildRepFilePanel pnlBuildCountFile = null;
	private String strOldTransLibID = "";
	private int nCurTransLibIndex = -1;
	private boolean bAddTransLib=false;
}
