package sng.amanager;

/****************************************************
 * Options button on Main Panel opens this window.
 * 1. GO
 * 2. ORFs
 * 3. Similarity
 * It gets all values from sTCW.cfg from ManagerData, which has already loaded it.
 * It writes changes to ManagerData, which subsequently writes to sTCW.cfg
 */
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;


import sng.database.Globals;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.TCWprops;
import util.ui.ButtonComboBox;
import util.ui.UserPrompt;
import util.database.HostsCfg;
import util.database.DBConn;

public class AnnoOptionsPanel extends JPanel {
	private static final long serialVersionUID = -6606995633515724156L;
	/*************************************************
	 * annotation options panel that replaces main window
	 */
	private static final int BLAST_ARGS_TEXTFIELD_WIDTH = 30;
	private static final int NUM_LG_FIELD_WIDTH = 4;
	private static final int NUM_SM_FIELD_WIDTH = 2;
	private static final int INDENT_RADIO = 25;
	
	public AnnoOptionsPanel(ManagerFrame parentFrame) {
		theParentFrame = parentFrame;
		createAnnoDBOptionPanel();
	}
	/**********************************************
	 * The panel is created before its known if it proteins, so we can only disable
	 */
	public void setOptions(ManagerData cd, HostsCfg cf, boolean isP) {
		curManData = cd;
		hostsObj = cf;
		isProteinDB=isP;
		setDefaults();
		
		// GO
		Vector<String> goList = findGODBs();
		cmbGODB.removeAllItems();
		cmbGODB.addItems(goList);				
		
		String godb = curManData.getGODB();
		if(goList.contains(godb)) {
			cmbGODB.setSelectedIndex(goList.indexOf(godb));
			
			String slim = curManData.getSlimSubset();
			if (!slim.equals("")) {
				findSetSlims(slim);
				setGOSlim(true, false);
			}
			else {
				String file = curManData.getSlimFile();
				if (!file.equals("")) {
					txtSlimOBOFile.setText(file);
					setGOSlim(false, true);
				}
			}
		}
		else {
			cmbGODB.setSelectedIndex(0);
			setGOSlim(false, false);
			if (!godb.equals("")) { 
				Out.PrtWarn("No GO database " + godb);
				curManData.setGODB("");
			}
		}
		
		if (isProteinDB) return; /*******************************/
		
		String file="";
		ManagerData.AnnoData annoObj = curManData.getAnnoObj();
		
		// Best Anno
		if (annoObj.getSPpref().equals("1")) chkSPpref.setSelected(true);
		else chkSPpref.setSelected(false);
		
		// ORF
		
		if (annoObj.getORFaltStart().equals("1")) chkAltStart.setSelected(true);
		else chkAltStart.setSelected(false);
		
		txtHitEval.setText(annoObj.getORFhitEval());
		txtHitSim.setText(annoObj.getORFhitSim());
		
		txtLenDiff.setText(annoObj.getORFlenDiff());
		
		txtTrainMinSet.setText(annoObj.getORFtrainMinSet());
		setTrain(true, false, false);
		
		file = annoObj.getORFtrainCDSfile();
		if (file !=null && !file.equals("") && !file.equals("-1")) {
			txtTrainCDSfile.setText(file);
			setTrain(false, true, false);
		}
		
		// Similarity
		file = annoObj.getSelfBlast();
		if(file==null) {
			setSim(false, false);
		}
		else {
			if (file.equals("")) {
				setSim(true, false);
				String opt = annoObj.getSelfBlastParams();
				if (opt!=null && !opt.equals("")) txtSelfBlastArgs.setText(opt);
			}
			else {
				setSim(false, true);
				txtSelfBlastfile.setText(file);
			}
		}
		file = annoObj.getTSelfBlast();
		if(file==null) {
			setTSim(false, false);
		}
		else {
			if (file.equals("")) {
				setTSim(true, false);
				String opt = annoObj.getTSelfBlastParams();
				if (!opt.equals("")) txtTSelfBlastArgs.setText(opt);
			}
			else {
				setTSim(false, true);
				txtTSelfBlastfile.setText(file);
			}
		}
		txtPairsLimit.setText("" + annoObj.getPairsLimit()); 
	}
	private void setDefaults() {
		chkSPpref.setSelected(false);
		cmbGODB.setSelectedIndex(0);
		cmbSlimSubset.setSelectedIndex(0);
	
		// ORF
		chkAltStart.setEnabled(!isProteinDB);
		txtHitEval.setEnabled(!isProteinDB);
		txtHitSim.setEnabled(!isProteinDB);
		
		txtLenDiff.setEnabled(!isProteinDB);
		
		chkTrainHit.setEnabled(!isProteinDB);
		chkTrainCDSfile.setEnabled(!isProteinDB);
		
		txtTrainCDSfile.setEnabled(!isProteinDB);
		
		// Sim
		chkDoSelfBlast.setEnabled(!isProteinDB);
		chkUseSelfBlast.setEnabled(!isProteinDB);
		txtSelfBlastfile.setEnabled(!isProteinDB);
		txtSelfBlastArgs.setEnabled(!isProteinDB);
		
		chkDoTSelfBlast.setEnabled(!isProteinDB);
		chkUseTSelfBlast.setEnabled(!isProteinDB);
		txtTSelfBlastfile.setEnabled(!isProteinDB);
		txtTSelfBlastArgs.setEnabled(!isProteinDB);
		txtPairsLimit.setEnabled(!isProteinDB);
		
		if (isProteinDB) return; /***************/
		
		// ORF - getProperty returns default
		if (mProps==null) mProps = new TCWprops(TCWprops.PropType.Annotate);
		
		String bAlt = mProps.getProperty("Anno_ORF_alt_start");
		boolean check = (bAlt.equals("1")) ? true : false;
		chkAltStart.setSelected(check);
		
		chkTrainHit.setEnabled(true);
		txtHitEval.setText(mProps.getProperty("Anno_ORF_hit_evalue")); 
		txtHitSim.setText(mProps.getProperty("Anno_ORF_hit_sim")); 
		
		txtLenDiff.setText(mProps.getProperty("Anno_ORF_len_diff")); 
		
		txtTrainMinSet.setText(mProps.getProperty("Anno_ORF_train_min_set")); 
		txtTrainCDSfile.setText(""); 
		
		// Sim
		setSim(false, false);
		setTSim(false, false);
		txtPairsLimit.setText(curManData.getAnnoObj().getPairsLimit() + "");
		txtSelfBlastArgs.setText(BlastArgs.getBlastnOptions());
		txtTSelfBlastArgs.setText(BlastArgs.getTblastxOptions());
	}
	private boolean keep() {
		curManData.setGODB("");	
		curManData.setSlimSubset("");
		curManData.setSlimFile("");
		if(cmbGODB.getSelectedIndex() != 0) {
			curManData.setGODB(cmbGODB.getSelectedItem().trim());
			
			if (chkSlimSubset.isSelected() && cmbSlimSubset.getSelectedIndex()!=0) 
				curManData.setSlimSubset(cmbSlimSubset.getSelectedItem());
			else 
				if (chkSlimOBOFile.isSelected() && txtSlimOBOFile.getText().trim()!="") 
					curManData.setSlimFile(txtSlimOBOFile.getText());
		}
		
		String x;
		ManagerData.AnnoData annoObj = curManData.getAnnoObj();
		
		if (chkSPpref.isSelected()) annoObj.setSPpref("1");
		else annoObj.setSPpref("0");
		
		if (chkAltStart.isSelected()) annoObj.setORFaltStart("1");
		else annoObj.setORFaltStart("0");
		
		x = txtHitEval.getText();
		if (Static.isDouble("Hit E-value", x)) annoObj.setORFhitEval(x);
		else return false;
		
		x = txtHitSim.getText();
		if (Static.isInteger("Hit %Similarity", x)) annoObj.setORFhitSim(x);
		else return false;
		
		x = txtLenDiff.getText();
		if (Static.isDouble("Length Difference", x)) annoObj.setORFlenDiff(x);
		else return false;
			
		x = txtTrainMinSet.getText();
		if (Static.isInteger("Minimum number of sequences used for training", x)) annoObj.setORFtrainMinSet(x);
		else return false;
		
		if (!chkTrainCDSfile.isSelected()) annoObj.setORFtrainCDSfile("");
		else annoObj.setORFtrainCDSfile(txtTrainCDSfile.getText());
		
		// Similarity
		// fileName of "" means run blast, fileName of null means no similarity
		boolean bSelf=false;
		annoObj.setSelfBlast(null);
		annoObj.setSelfBlastParams(null);
		if(chkDoSelfBlast.isSelected()) {
			annoObj.setSelfBlast("");
			annoObj.setSelfBlastParams(txtSelfBlastArgs.getText().replace('\n', ' '));
			bSelf=true;
		}
		else if (chkUseSelfBlast.isSelected()) {
			annoObj.setSelfBlast(txtSelfBlastfile.getText());
			annoObj.setSelfBlastParams("");
			bSelf=true;
		}
		
		annoObj.setTSelfBlast(null);
		annoObj.setTSelfBlastParams(null);
		if(chkDoTSelfBlast.isSelected()) {
			annoObj.setTSelfBlast("");
			annoObj.setTSelfBlastParams(txtTSelfBlastArgs.getText().replace('\n', ' '));
			bSelf=true;
		}
		else if (chkUseTSelfBlast.isSelected()) {
			annoObj.setTSelfBlast(txtTSelfBlastfile.getText());
			annoObj.setTSelfBlastParams("");
			bSelf=true;
		}
		if (bSelf) {
			x = txtPairsLimit.getText();
			int limit=0;
			if(x.length() > 0) {
				try {
					limit = Integer.parseInt(x);
					annoObj.setPairsLimit(limit);
				}
				catch(Exception err) {
					JOptionPane.showMessageDialog(this,  "Incorrect pairs limit " + x, "Incorrect parameter", JOptionPane.PLAIN_MESSAGE);
					return false;
				}
			}
			if (limit==0) {
				JOptionPane.showMessageDialog(this,  
						"Cannot have 'Pairs limit' = 0 when a self blast option is selected.\n" +
						"Either set a Pairs limit or uncheck all circles under SIMILAR SEQUENCES.", 
						"Incorrect parameter", JOptionPane.PLAIN_MESSAGE);
				return false;
			}
		}
		return true;
	}
	
	private void createAnnoDBOptionPanel() {
		try {
		pnlAnnoDBOptions = new JPanel();
		pnlAnnoDBOptions.setLayout(new BoxLayout(pnlAnnoDBOptions, BoxLayout.PAGE_AXIS));
		pnlAnnoDBOptions.setBackground(Globals.BGCOLOR);
		pnlAnnoDBOptions.setAlignmentX(Component.LEFT_ALIGNMENT);
		pnlAnnoDBOptions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.PAGE_AXIS));
		innerPanel.setBackground(Globals.BGCOLOR);
		innerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JPanel row = Static.createRowPanel();
		JLabel title = new JLabel("Annotation Options");
		title.setFont(pnlAnnoDBOptions.getFont().deriveFont(Font.BOLD, 18));
		
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(20));
		
		createAnnoPanel(innerPanel);
		if (!isProteinDB) createOrfPanel(innerPanel);
		if (!isProteinDB) createSimPanel(innerPanel);
		
		innerPanel.setMaximumSize(innerPanel.getPreferredSize());
		innerPanel.setMinimumSize(innerPanel.getPreferredSize());
		
		pnlAnnoDBOptions.add(innerPanel);
		
		// buttons
		JButton btnResetDefaults = new JButton("Reset To Default");
		btnResetDefaults.setBackground(Globals.BGCOLOR);
		btnResetDefaults.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setDefaults();
			}
		});

		JButton btnKeep = new JButton("Keep");
		btnKeep.setBackground(Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (keep()) {
					setVisible(false);
					theParentFrame.setMainPanelVisible(true);
					theParentFrame.updateUI();
					theParentFrame.saveProject();
					theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				}
			}
		});
		
		JButton btnDiscard = new JButton("Cancel");
		btnDiscard.setBackground(Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				theParentFrame.setMainPanelVisible(true);
				theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, 
						"Annotation Options Help", "html/runSingleTCW/AnnotationOptions.html");
			}
		});
		
		JPanel buttonRow = new JPanel();
		buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.LINE_AXIS));
		buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonRow.setBackground(Globals.BGCOLOR);
				
		buttonRow.add(btnKeep);
		buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnDiscard);
		buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnResetDefaults);
		buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnHelp);
		
		buttonRow.setMaximumSize(buttonRow.getPreferredSize());
		buttonRow.setMinimumSize(buttonRow.getPreferredSize());
		
		pnlAnnoDBOptions.add(buttonRow);
		
		setBackground(Globals.BGCOLOR);
		add(pnlAnnoDBOptions);
		setVisible(false);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Retry - this happens occasionally on updates");}
	}
	// ORF
	private void createOrfPanel(JPanel innerPanel) {
		innerPanel.add(new JLabel("ORF FINDER: (See Help)"));
		innerPanel.add(Box.createVerticalStrut(10));
		
		JPanel row = Static.createRowPanel();
		chkAltStart = new JCheckBox("Use Alternative starts");
		chkAltStart.setBackground(Globals.BGCOLOR);
		row.add(chkAltStart);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		// Rule 1
		row = Static.createRowPanel();		
		row.add(new JLabel("Rule 1: Use best hit frame if E-value <="));
		row.add(Box.createHorizontalStrut(1));
		txtHitEval  = Static.createTextField("0", NUM_LG_FIELD_WIDTH);
		row.add(txtHitEval);
		row.add(Box.createHorizontalStrut(1));
		
		row.add(new JLabel(" or %HitSim>="));
		row.add(Box.createHorizontalStrut(1));
		txtHitSim  = Static.createTextField("0", NUM_LG_FIELD_WIDTH);
		row.add(txtHitSim);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		// Rule 2
		row = Static.createRowPanel();		
		row.add(new JLabel("Rule 2: Else use longest ORF frame if the log length ratio >"));
		row.add(Box.createHorizontalStrut(1));
		txtLenDiff = Static.createTextField("0", NUM_SM_FIELD_WIDTH);
		row.add(txtLenDiff);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		// Training
		row = Static.createRowPanel();
		row.add(new JLabel("Rule 3: Else use the best Markov score"));
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		
		chkTrainHit = new JRadioButton("Train with Best Hits (Rule 1)");
		chkTrainHit.setBackground(Globals.BGCOLOR);
		chkTrainHit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setTrain(true, false, false);
			}	
		});
		row.add(chkTrainHit);
		row.add(Box.createHorizontalStrut(1));
		
		
		row.add(Box.createHorizontalStrut(5));
		row.add(new JLabel("Minimum Set"));
		row.add(Box.createHorizontalStrut(1));
		txtTrainMinSet  = Static.createTextField("0", NUM_SM_FIELD_WIDTH);
		row.add(txtTrainMinSet);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		// 1.2
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		chkTrainCDSfile = new JRadioButton("Train with CDS file");
		chkTrainCDSfile.setBackground(Globals.BGCOLOR);
		chkTrainCDSfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setTrain(false, true, false);
			}	
		});
		row.add(chkTrainCDSfile);

		txtTrainCDSfile = new FileTextField(theParentFrame, FileTextField.PROJ, FileTextField.FASTA);
		txtTrainCDSfile.setMaximumSize(txtTrainCDSfile.getPreferredSize());
		txtTrainCDSfile.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent arg0) {
				if(txtTrainCDSfile.getText().length() > 0) {
					setTrain(false, true, false);
					if (txtTrainCDSfile.isProtein()) 
						UserPrompt.showError("CDS file appears to be protein. It needs to be nucleotide.");
				}
			}
		});
		row.add(txtTrainCDSfile);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		innerPanel.add(new JSeparator());
		innerPanel.add(Box.createVerticalStrut(10));
	}
	// Similarity
	private void createSimPanel(JPanel innerPanel) {
		innerPanel.add(new JLabel("SIMILAR SEQUENCES:"));
		innerPanel.add(Box.createVerticalStrut(10));

		JPanel row = Static.createRowPanel();
		row.add(new JLabel("Nucleotide Self Blast (Megablast or blastn)"));
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();	
		chkDoSelfBlast = new JRadioButton("Execute");
		chkDoSelfBlast.setBackground(Globals.BGCOLOR);
		chkDoSelfBlast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setSim(chkDoSelfBlast.isSelected(), false);
			}	
		});
		row.add(chkDoSelfBlast);
		
		txtSelfBlastArgs = new JTextArea(1, BLAST_ARGS_TEXTFIELD_WIDTH);
		txtSelfBlastArgs.setText(BlastArgs.getBlastnOptions());
		txtSelfBlastArgs.setLineWrap(true);
		txtSelfBlastArgs.setWrapStyleWord(true);
		JScrollPane tempPane = new JScrollPane(txtSelfBlastArgs);
		row.add(tempPane);	
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		chkUseSelfBlast = new JRadioButton("Or use existing file");
		chkUseSelfBlast.setBackground(Globals.BGCOLOR);
		chkUseSelfBlast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setSim(false, chkUseSelfBlast.isSelected());
			}	
		});
		row.add(chkUseSelfBlast);
		row.add(Box.createHorizontalStrut(5));
		txtSelfBlastfile = new FileTextField(theParentFrame, FileTextField.PROJ, FileTextField.TAB);
		txtSelfBlastfile.setMaximumSize(txtSelfBlastfile.getPreferredSize());
		row.add(txtSelfBlastfile);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(10));
				
		///
		row = Static.createRowPanel();
		row.add(new JLabel("Translated Self Blast (tblastx)"));
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();	
		chkDoTSelfBlast = new JRadioButton("Execute");
		chkDoTSelfBlast.setBackground(Globals.BGCOLOR);
		chkDoTSelfBlast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setTSim(chkDoTSelfBlast.isSelected(), false);
			}	
		});
		row.add(chkDoTSelfBlast);
		
		txtTSelfBlastArgs = new JTextArea(1, BLAST_ARGS_TEXTFIELD_WIDTH);
		txtTSelfBlastArgs.setText(BlastArgs.getTblastxOptions());
		txtTSelfBlastArgs.setLineWrap(true);
		txtTSelfBlastArgs.setWrapStyleWord(true);
		tempPane = new JScrollPane(txtTSelfBlastArgs);
		row.add(tempPane);	
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		chkUseTSelfBlast = new JRadioButton("Or use existing file");
		chkUseTSelfBlast.setBackground(Globals.BGCOLOR);
		chkUseTSelfBlast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setTSim(false, chkUseTSelfBlast.isSelected());
			}	
		});
		row.add(chkUseTSelfBlast);
		row.add(Box.createHorizontalStrut(5));
		txtTSelfBlastfile = new FileTextField(theParentFrame, FileTextField.PROJ, FileTextField.TAB);
		txtTSelfBlastfile.setMaximumSize(txtTSelfBlastfile.getPreferredSize());
		row.add(txtTSelfBlastfile);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		txtPairsLimit  = Static.createTextField("0", NUM_LG_FIELD_WIDTH);
			
		JLabel pairsLimit = new JLabel("Pairs limit"); 
		row.add(pairsLimit);
		row.add(Box.createHorizontalStrut(10));
		row.add(txtPairsLimit);
		innerPanel.add(row);
		innerPanel.add(new JSeparator());
		innerPanel.add(Box.createVerticalStrut(10));
	}
		
	// GO
	private void createAnnoPanel(JPanel innerPanel) {
		innerPanel.add(new JLabel("ANNOTATION:"));
		innerPanel.add(Box.createVerticalStrut(10));
		JPanel row = Static.createRowPanel();
				
		chkSPpref = Static.createCheckBox("Best Anno - SwissProt preference");
		row.add(chkSPpref);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		row.add(new JLabel("GO Database"));
		row.add(Box.createHorizontalStrut(11));
		
		cmbGODB = new ButtonComboBox();
		cmbGODB.addItem("         None         ");
		cmbGODB.setBackground(Globals.BGCOLOR);
		cmbGODB.setMaximumSize(cmbGODB.getPreferredSize());
		cmbGODB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (cmbGODB.getSelectedIndex()!=0) {
					setGOSlim(true, false);
					findSetSlims("");
				}
				else setGOSlim(false, false);
			}
		});
		row.add(cmbGODB);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		
		chkSlimSubset = new JRadioButton("Slims from GO database");
		chkSlimSubset.setBackground(Globals.BGCOLOR);
		chkSlimSubset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setGOSlim(true, false);
				findSetSlims("");
			}	
		});
		
		row.add(chkSlimSubset);
		row.add(Box.createHorizontalStrut(10));
		
		cmbSlimSubset = new ButtonComboBox();
		cmbSlimSubset.addItem("         None         ");
		cmbSlimSubset.setBackground(Globals.BGCOLOR);
		cmbSlimSubset.setMaximumSize(cmbSlimSubset.getPreferredSize());
		cmbSlimSubset.setEnabled(false);
		row.add(cmbSlimSubset);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		chkSlimOBOFile = new JRadioButton("Slims from OBO File");
		chkSlimOBOFile.setBackground(Globals.BGCOLOR);
		chkSlimOBOFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setGOSlim(false, true);
			}	
		});
		row.add(chkSlimOBOFile);
		txtSlimOBOFile = new FileTextField(theParentFrame, FileTextField.PROJ, FileTextField.OBO);
		txtSlimOBOFile.setMaximumSize(txtSlimOBOFile.getPreferredSize());
		row.add(txtSlimOBOFile);
		
		innerPanel.add(row);
		innerPanel.add(new JSeparator());
		innerPanel.add(Box.createVerticalStrut(10));
	}
	private Vector<String> findGODBs() {
		Vector<String> retVal = new Vector<String> ();
		retVal.add("   None   ");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String dbstr = "jdbc:mysql://" + hostsObj.host() + "/";
			Connection con = null; 

			con = DriverManager.getConnection(dbstr, hostsObj.user(), hostsObj.pass());
			Statement st = con.createStatement();
				
			ResultSet rset = st.executeQuery("show databases LIKE 'go_%'");
			while(rset.next()) {
				retVal.add(rset.getString(1));
			}
			rset.close();						
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting GO databases");
		}
		return retVal;
	}
	private void findSetSlims(String slim) {
		Vector<String> subsets = new Vector<String> ();
		if (cmbGODB.getSelectedIndex()==0) return;
		String godbName = cmbGODB.getSelectedItem();
		if (godbName==null) return;
		
		godbName = godbName.trim();
		try {
			DBConn goDB = hostsObj.getDBConn(godbName);
	
			ResultSet rset = goDB.executeQuery("select acc from term where term_type='subset'");
			while(rset.next()) {
				String acc = rset.getString(1);
				if (acc.startsWith("goslim"))
					subsets.add(rset.getString(1));
			}
			rset.close();	
			
			cmbSlimSubset.removeAllItems();
			cmbSlimSubset.addItem("   None   ");
			int index=0;
			for(int x=0; x<subsets.size(); x++) {
				String sub = subsets.get(x);
				cmbSlimSubset.addItem(sub);
				if (sub.equals(slim)) index = x+1;
			}
			cmbSlimSubset.setSelectedIndex(index); 
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting GO slims from " + godbName);
		}
	}
	
	private void setTrain(boolean bHits, boolean bCDS, boolean bUsage) {
		chkTrainHit.setSelected(bHits);
		
		chkTrainCDSfile.setSelected(bCDS);
		txtTrainCDSfile.setEnabled(bCDS);
	}
	private void setGOSlim(boolean x, boolean y) {
		
		if (x==true || y==true) {
			chkSlimSubset.setEnabled(true);
			cmbSlimSubset.setEnabled(x);
			chkSlimSubset.setSelected(x);
			
			chkSlimOBOFile.setEnabled(true);
			txtSlimOBOFile.setEnabled(y);
			chkSlimOBOFile.setSelected(y);
		}
		else {
			chkSlimSubset.setEnabled(false);
			cmbSlimSubset.setEnabled(false);
			chkSlimSubset.setSelected(false);
			
			chkSlimOBOFile.setEnabled(false);
			txtSlimOBOFile.setEnabled(false);
			chkSlimOBOFile.setSelected(false);
		}
	}
	private void setSim(boolean x, boolean y) {
		chkDoSelfBlast.setSelected(x);
		txtSelfBlastArgs.setEnabled(x);
		
		chkUseSelfBlast.setSelected(y);
		txtSelfBlastfile.setEnabled(y);
		
		if (x || y) txtPairsLimit.setEnabled(true);
		else {
			if (!chkDoTSelfBlast.isSelected() && !chkUseTSelfBlast.isSelected()) txtPairsLimit.setEnabled(false);
		}
	}
	private void setTSim(boolean x, boolean y) {
		chkDoTSelfBlast.setSelected(x);
		txtTSelfBlastArgs.setEnabled(x);
		
		chkUseTSelfBlast.setSelected(y);
		txtTSelfBlastfile.setEnabled(y);
		
		if (x || y) txtPairsLimit.setEnabled(true);
		else {
			if (!chkDoSelfBlast.isSelected() && !chkUseSelfBlast.isSelected()) txtPairsLimit.setEnabled(false);
		}
	}
	
	private TCWprops mProps=null;
	private HostsCfg hostsObj = null;
	private ManagerData curManData = null;
	private ManagerFrame theParentFrame=null;
	private boolean isProteinDB=false;
	
	//Anno controls
	private JPanel pnlAnnoDBOptions = null;
	
	// Best Anno
	private JCheckBox chkSPpref = null;
	
	// GO
	private ButtonComboBox cmbGODB = null;
	
	private JRadioButton chkSlimSubset=null;
	private ButtonComboBox cmbSlimSubset = null;
	
	private JRadioButton chkSlimOBOFile=null;
	private FileTextField txtSlimOBOFile = null;
	
	// ORF
	private JCheckBox chkAltStart = null;
	
	private JTextField txtHitEval=null, txtHitSim=null, txtLenDiff=null;
	
	private JRadioButton  chkTrainHit=null, chkTrainCDSfile=null;
	private JTextField 	  txtTrainMinSet=null;
	private FileTextField txtTrainCDSfile = null;
	
	// Similarity
	private JRadioButton chkDoSelfBlast = null, chkUseSelfBlast = null;
	private JTextArea txtSelfBlastArgs = null;
	private FileTextField txtSelfBlastfile = null;
	
	private JRadioButton chkDoTSelfBlast = null, chkUseTSelfBlast = null;
	private JTextArea txtTSelfBlastArgs = null; 
	private FileTextField txtTSelfBlastfile = null;
	private JTextField txtPairsLimit = null;
}
