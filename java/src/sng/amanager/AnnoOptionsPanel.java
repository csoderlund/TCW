package sng.amanager;

import java.awt.Component;

/****************************************************
 * Options button on Main Panel opens this window.
 * 1. GO
 * 2. ORFs
 * 3. Similarity
 * It gets all values from sTCW.cfg from ManagerData, which has already loaded it.
 * It writes changes to ManagerData, which subsequently writes to sTCW.cfg
 */

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import sng.database.Globals;
import util.database.HostsCfg;
import util.database.DBConn;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileRead;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.TCWprops;
import util.ui.ButtonComboBox;
import util.ui.UserPrompt;

public class AnnoOptionsPanel extends JPanel {
	private static final long serialVersionUID = -6606995633515724156L;
	/*************************************************
	 * annotation options panel that replaces main window
	 */
	private static final int BLASTARGS_TEXT_WIDTH = 25;
	private static final int NUM_LG_FIELD_WIDTH = 4;
	private static final int NUM_SM_FIELD_WIDTH = 2;
	private static final int INDENT_RADIO = 25;
	private static final int VERT1 = 5, VERT2=10;
	private final String helpHTML = Globals.helpRunDir + "AnnotationOptions.html";
	private static final String searchHelpHTML = Globalx.searchHelp;
	
	public AnnoOptionsPanel(ManagerFrame parentFrame) {
		theManFrame = parentFrame;
		
		try { // created before we know what type sTCW is
		pnlAnnoDBOptions = Static.createPagePanel();
		pnlAnnoDBOptions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel innerPanel = Static.createPageCenterPanel();
		
		JPanel row = Static.createRowPanel();
		JLabel title = new JLabel("Annotation Options");
		title.setFont(pnlAnnoDBOptions.getFont().deriveFont(Font.BOLD, 18));
		
		row.add(Box.createHorizontalGlue());
		row.add(title);
		row.add(Box.createHorizontalGlue());
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT2));
		
		createAnnoPanel(innerPanel);
		createOrfPanel(innerPanel);
		createSelfPanel(innerPanel);
		
		innerPanel.setMaximumSize(innerPanel.getPreferredSize());
		innerPanel.setMinimumSize(innerPanel.getPreferredSize());
		
		pnlAnnoDBOptions.add(innerPanel);
		
		// buttons
		JButton btnResetDefaults = Static.createButton(Globalx.defaultBtn);
		btnResetDefaults.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setDefaults();
			}
		});

		JButton btnKeep = Static.createButton(Globalx.keepBtn);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (keep()) {
					setVisible(false);
					theManFrame.setMainPanelVisible(true);
					theManFrame.updateUI();
					theManFrame.saveProject();
					theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				}
			}
		});
		
		JButton btnDiscard = Static.createButton(Globalx.cancelBtn);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				theManFrame.setMainPanelVisible(true);
				theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
			}
		});
		
		// CAS339 add two level help
		final JPopupMenu popup = new JPopupMenu();
		popup.add(new JMenuItem(new AbstractAction("Options") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theManFrame, "Options", helpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error on Annotation Options help"); }
			}
		}));
		popup.add(new JMenuItem(new AbstractAction("Search Parameters") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theManFrame, "Search Parameters", searchHelpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error on search parameters help"); }
			}
		}));
		JButton btnHelp = Static.createButtonHelp("Help...", true);
		btnHelp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		btnHelp.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JPanel buttonRow = Static.createRowCenterPanel();		
		buttonRow.add(btnKeep);				buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnDiscard);			buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnResetDefaults);	buttonRow.add(Box.createHorizontalStrut(15));
		buttonRow.add(btnHelp);
		
		buttonRow.setMaximumSize(buttonRow.getPreferredSize());
		buttonRow.setMinimumSize(buttonRow.getPreferredSize());
		
		pnlAnnoDBOptions.add(buttonRow);
		
		setBackground(Globalx.BGCOLOR);
		add(pnlAnnoDBOptions);
		setVisible(false);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Retry - this happens occasionally on updates");}
	}
	// ORF
	private void createOrfPanel(JPanel innerPanel) {
		innerPanel.add(new JLabel("ORF FINDER:"));
		innerPanel.add(Box.createVerticalStrut(10));
		
		JPanel row = Static.createRowPanel();
		chkAltStart = new JCheckBox("Use Alternative starts");
		chkAltStart.setBackground(Globalx.BGCOLOR);
		row.add(chkAltStart);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		// Rule 1
		row = Static.createRowPanel();		
		ntJLabel[0] = new JLabel("Rule 1: Use best hit frame if E-value <=");
		row.add(ntJLabel[0]);
		row.add(Box.createHorizontalStrut(1));
		txtHitEval  = Static.createTextField("0", NUM_LG_FIELD_WIDTH);
		row.add(txtHitEval);	row.add(Box.createHorizontalStrut(1));
		
		ntJLabel[1] = new JLabel(" or %Sim>="); // CAS318 %Identity from file, was HitSim;
		row.add(ntJLabel[1]);	row.add(Box.createHorizontalStrut(1));
		txtHitSim  = Static.createTextField("0", NUM_LG_FIELD_WIDTH);
		row.add(txtHitSim);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
	// Rule 2
		row = Static.createRowPanel();	
		ntJLabel[2] = new JLabel("Rule 2: Else use the longest ORF if the log ratio >");
		row.add(ntJLabel[2]);	row.add(Box.createHorizontalStrut(1));
		txtLenDiff = Static.createTextField("0", NUM_SM_FIELD_WIDTH);
		row.add(txtLenDiff);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
	// Rule 3 Training
		row = Static.createRowPanel();
		ntJLabel[3] = new JLabel("Rule 3: Else use the best Markov score if the log ratio >");
		row.add(ntJLabel[3]);	
		row.add(Box.createHorizontalStrut(1));
		txtMkDiff = Static.createTextField("0", NUM_SM_FIELD_WIDTH);
		row.add(txtMkDiff);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		// Train From best hit 
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		
		radTrainHit = new JRadioButton("Train with Best Hits (Rule 1)");
		radTrainHit.setBackground(Globalx.BGCOLOR);
		radTrainHit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setTrain(true, false);
			}	
		});
		row.add(radTrainHit);
		row.add(Box.createHorizontalStrut(5));
		ntJLabel[4] = new JLabel("Minimum Set");
		row.add(ntJLabel[4]);	row.add(Box.createHorizontalStrut(1));
		txtTrainMinSet  = Static.createTextField("0", NUM_LG_FIELD_WIDTH);
		row.add(txtTrainMinSet);
	
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		// Train from CDS
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		radTrainCDSfile = new JRadioButton("Train with CDS file");
		radTrainCDSfile.setBackground(Globalx.BGCOLOR);
		radTrainCDSfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setTrain(false, true);
			}	
		});
		row.add(radTrainCDSfile);						row.add(Box.createHorizontalStrut(5));

		txtTrainCDSfile = Static.createTextField("", 25);
		row.add(txtTrainCDSfile);						row.add(Box.createHorizontalStrut(5));
		
		btnTrainCDSfile = Static.createButtonFile("...", false);
		btnTrainCDSfile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String projName = theManFrame.getProjDir();
				FileRead fc = new FileRead(projName, FileC.bDoVer, FileC.bDoPrt);
				
				if (fc.run(btnTrainCDSfile, "CDS", FileC.dPROJ, FileC.fFASTA)) { 
					if (fc.isProtein())
						UserPrompt.showError("CDS file appears to be protein. It needs to be nucleotide.");
					else {
						txtTrainCDSfile.setText(fc.getRemoveFixedPath());
						setTrain(false, true);
					}
				}
			}
		});
		row.add(btnTrainCDSfile);
		
		ButtonGroup group = new ButtonGroup(); // CAS327
		group.add(radTrainHit);
		group.add(radTrainCDSfile);
		radTrainHit.setSelected(true);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		innerPanel.add(new JSeparator());
		innerPanel.add(Box.createVerticalStrut(VERT2));
	}
	// Similarity
	private void createSelfPanel(JPanel innerPanel) {
		innerPanel.add(new JLabel("SIMILAR PAIRS: (see Help on how to supply tab files)"));
		innerPanel.add(Box.createVerticalStrut(VERT2));

		// BlastN
		JPanel row = Static.createRowPanel();
		chkSelfN = Static.createCheckBox("BlastN (NT-NT)", false);
		row.add(chkSelfN);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		row = Static.createRowPanel();	
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		ntJLabel[5] = new JLabel("Params ");
		row.add(ntJLabel[5]);
		txtSelfNargs = new JTextField(BLASTARGS_TEXT_WIDTH);
		row.add(txtSelfNargs);	
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT2));
		
		/// Tblast
		row = Static.createRowPanel();
		chkSelfX = Static.createCheckBox("Tblastx (6-frame)", false);
		row.add(chkSelfX);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		row = Static.createRowPanel();	
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		row.add(ntJLabel[5]);
		txtSelfXargs = new JTextField(BLASTARGS_TEXT_WIDTH);
		row.add(txtSelfXargs);	
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT2));
		
		// Blastp for AA-ORFs or AA-stcw CAS314 add
		row = Static.createRowPanel();
		chkSelfP = Static.createCheckBox("BlastP (AA-AA: AA-ORFs or AAsTCW)", false);
		row.add(chkSelfP);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		row = Static.createRowPanel();	
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		row.add(new JLabel("Search program: "));
		cmbSearchPgms = new ButtonComboBox();
		cmbSearchPgms.setBackground(Globalx.BGCOLOR);
		Vector <String> pgm = BlastArgs.getSearchPgms();
		for (String p: pgm) cmbSearchPgms.addItem(p);
		cmbSearchPgms.setSelectedIndex(0); // diamond
		cmbSearchPgms.setMaximumSize(cmbSearchPgms.getPreferredSize());
		cmbSearchPgms.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setParamDefaults();
			}
		});
		row.add(cmbSearchPgms);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		row = Static.createRowPanel();	
		row.add(Box.createHorizontalStrut(INDENT_RADIO));
		row.add(new JLabel("Params "));
		txtSelfPargs = new JTextField(BLASTARGS_TEXT_WIDTH);
		row.add(txtSelfPargs);	
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT2));
		
		row = Static.createRowPanel();
		txtPairsLimit  = Static.createTextField("0", NUM_LG_FIELD_WIDTH);
			
		JLabel pairsLimit = new JLabel("Pairs limit"); 
		row.add(pairsLimit);
		row.add(Box.createHorizontalStrut(10));
		row.add(txtPairsLimit);
		innerPanel.add(row);
		innerPanel.add(new JSeparator());
		innerPanel.add(Box.createVerticalStrut(VERT2));
	}
		
	// GO
	private void createAnnoPanel(JPanel innerPanel) {
		innerPanel.add(new JLabel("ANNOTATION:"));
		innerPanel.add(Box.createVerticalStrut(VERT2));
		
		JPanel row = Static.createRowPanel();	
		chkSPpref = Static.createCheckBox("Best Anno - SwissProt preference", false);
		row.add(chkSPpref);
		row.add(Box.createHorizontalStrut(11));
		chkRmECO = Static.createCheckBox("Remove {ECO...} from UniProt descripts", true);
		row.add(chkRmECO); 
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT2));
		
		row = Static.createRowPanel();	// CAS331 add prune; if change - change in Overview
		row.add(Static.createLabel("Prune hits")); 	row.add(Box.createHorizontalStrut(5));
		radPrNone = Static.createRadioButton("None", false);
		row.add(radPrNone);
		radPrAlign = Static.createRadioButton("Alignment", false);
		row.add(radPrAlign);					row.add(Box.createHorizontalStrut(5));
		radPrDesc = Static.createRadioButton("Description", false);
		row.add(radPrDesc);						row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group = new ButtonGroup(); // CAS327
		group.add(radPrDesc); group.add(radPrAlign); group.add(radPrNone);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT2+5));
		
		row = Static.createRowPanel();
		row.add(new JLabel("GO Database"));		row.add(Box.createHorizontalStrut(11));
		
		cmbGODB = new ButtonComboBox();
		cmbGODB.addItem("         None         ");
		cmbGODB.setBackground(Globalx.BGCOLOR);
		cmbGODB.setMaximumSize(cmbGODB.getPreferredSize());
		cmbGODB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b=false;
				if (cmbGODB.getSelectedIndex()!=0) {
					b=true;
					setGOSlim(true, false);
					findSetSlims("");
				}
				else setGOSlim(false, false);
				chkNoGO.setEnabled(b);
			}
		});
		row.add(cmbGODB);	row.add(Box.createHorizontalStrut(35)); 
		
		chkNoGO = Static.createCheckBox("Ignore on Annotate", false);// CAS331 add 
		row.add(chkNoGO);
		
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		row = Static.createRowPanel();
		
		// Slims from GO
		radSlimSubset = new JRadioButton("Slims from GO database");
		radSlimSubset.setBackground(Globalx.BGCOLOR);
		radSlimSubset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setGOSlim(true, false);
				findSetSlims("");
			}	
		});
		row.add(radSlimSubset);	row.add(Box.createHorizontalStrut(10));
		
		cmbSlimSubset = new ButtonComboBox();
		cmbSlimSubset.addItem("         None         ");
		cmbSlimSubset.setBackground(Globalx.BGCOLOR);
		cmbSlimSubset.setMaximumSize(cmbSlimSubset.getPreferredSize());
		cmbSlimSubset.setEnabled(false);
		row.add(cmbSlimSubset);
		innerPanel.add(row);
		innerPanel.add(Box.createVerticalStrut(VERT1));
		
		// Slims from file
		row = Static.createRowPanel();
		radSlimOBOFile = new JRadioButton("Slims from OBO File");
		radSlimOBOFile.setBackground(Globalx.BGCOLOR);
		radSlimOBOFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setGOSlim(false, true);
			}	
		});
		row.add(radSlimOBOFile);
		
		txtSlimOBOFile = Static.createTextField("", 25);
		row.add(txtSlimOBOFile);						row.add(Box.createHorizontalStrut(5));
		
		btnSlimOBOFile = Static.createButtonFile("...", false);
		btnSlimOBOFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String projName = theManFrame.getProjDir();
				FileRead fc = new FileRead(projName, FileC.bDoVer, FileC.bDoPrt);
				
				if (fc.run(btnSlimOBOFile, "Slim OBO", FileC.dPROJ, FileC.fOBO)) { 
					txtSlimOBOFile.setText(fc.getRemoveFixedPath());	
				}
			}
		});
		row.add(btnSlimOBOFile);						
		
		innerPanel.add(row);
		innerPanel.add(new JSeparator());
		innerPanel.add(Box.createVerticalStrut(VERT2));
	}
	private Vector<String> findGODBs() {
		Vector<String> retVal = new Vector<String> ();
		retVal.add("   None   ");
		try {
			Class.forName(DBConn.driver);
			String dbstr = DBConn.createDBstr(hostsObj.host() + "/", null); // CAS303

			Connection con = DriverManager.getConnection(dbstr, hostsObj.user(), hostsObj.pass());
			Statement st = con.createStatement();	
			
			ResultSet rset = st.executeQuery("show databases LIKE '" + Globalx.goPreDB + "%'");
			while(rset.next()) {
				retVal.add(rset.getString(1));
			}
			rset.close();						
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error getting GO databases");}
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
	
			// pre-v318 will not get any, as 'name' is not prefixed with goslim_
			ResultSet rset = goDB.executeQuery("select name from term where term_type='subset'");
			while(rset.next()) {
				String acc = rset.getString(1);
				if (acc.startsWith("goslim_"))
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
	
	private void setTrain(boolean bHits, boolean bCDS) {
		radTrainHit.setSelected(bHits);
		radTrainCDSfile.setSelected(bCDS);
		txtTrainCDSfile.setEnabled(bCDS);
		btnTrainCDSfile.setEnabled(bCDS);
	}
	private void setGOSlim(boolean x, boolean y) {
		if (x==true || y==true) {
			radSlimSubset.setEnabled(true);
			cmbSlimSubset.setEnabled(x);
			radSlimSubset.setSelected(x);
			
			radSlimOBOFile.setEnabled(true);
			txtSlimOBOFile.setEnabled(y);
			btnSlimOBOFile.setEnabled(y);
			radSlimOBOFile.setSelected(y);	
		}
		else {
			radSlimSubset.setEnabled(false);
			cmbSlimSubset.setEnabled(false);
			radSlimSubset.setSelected(false);
			
			radSlimOBOFile.setEnabled(false);
			txtSlimOBOFile.setEnabled(false);
			btnSlimOBOFile.setEnabled(false);
			radSlimOBOFile.setSelected(false);	
		}
	}
	private void setParamDefaults () {
		String pgm = cmbSearchPgms.getSelectedItem();
		
		if (pgm.equals("blast")) {
			 saveDiaArgs = txtSelfPargs.getText();
			 txtSelfPargs.setText(saveBlaArgs);
		}
		else if (pgm.equals("diamond")) {
			saveBlaArgs = txtSelfPargs.getText();
			txtSelfPargs.setText(saveDiaArgs);
		}
		else Out.bug("Illegal search program: " + pgm);
	}
	/******************************************************************************/
	public void updateAnnoOptions(ManagerData cd, HostsCfg cf, boolean isP) {
		curManData = cd;
		hostsObj = cf;
		isAAdb=isP;
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
		if (curManData.getNoGO().contentEquals("0")) chkNoGO.setSelected(false); //CAS331
		else 										 chkNoGO.setSelected(true);
		
		String file="";
		ManagerData.AnnoData annoObj = curManData.getAnnoObj();
		
		// Best Anno
		if (annoObj.getSPpref().equals("1")) 	chkSPpref.setSelected(true);
		else 									chkSPpref.setSelected(false);
		
		if (annoObj.getRmECO().equals("1")) 	chkRmECO.setSelected(true);
		else 									chkRmECO.setSelected(false);
		
		String pruneType = annoObj.getPruneType();
		if (pruneType.contentEquals("1")) 		radPrAlign.setSelected(true); // CAS331
		else if (pruneType.contentEquals("2")) 	radPrDesc.setSelected(true);
		else 									radPrNone.setSelected(true);
		
		// Similarity; 
		chkSelfN.setSelected(annoObj.getSelfBlastn());
		chkSelfX.setSelected(annoObj.getSelfTblastx());
		chkSelfP.setSelected(annoObj.getSelfBlastp());
		
		txtSelfNargs.setText(annoObj.getSelfBlastnParams());
		txtSelfXargs.setText(annoObj.getSelfTblastxParams());
		txtSelfPargs.setText(annoObj.getSelfBlastpParams());
		
		boolean set = cmbSearchPgms.setSelectedItem(annoObj.getSelfBlastpPgm());
		if (!set) cmbSearchPgms.setSelectedIndex(0);
		
		if (cmbSearchPgms.getSelectedItem().equals("diamond")) {
			saveDiaArgs = annoObj.getSelfBlastpParams();
			saveBlaArgs = BlastArgs.getBlastArgsORF();
		}
		else {
			saveBlaArgs = annoObj.getSelfBlastpParams();
			saveDiaArgs = BlastArgs.getDiamondArgsORF();
		}	
		
		txtPairsLimit.setText("" + annoObj.getPairsLimit()); 
		
		// ORF
		if (isAAdb) return; /*******************************/
		if (annoObj.getORFaltStart().equals("1")) chkAltStart.setSelected(true);
		else chkAltStart.setSelected(false);
		
		txtHitEval.setText(annoObj.getORFhitEval());
		txtHitSim.setText(annoObj.getORFhitSim());
		
		txtLenDiff.setText(annoObj.getORFlenDiff());
		txtMkDiff.setText(annoObj.getORFmkDiff());
		
		txtTrainMinSet.setText(annoObj.getORFtrainMinSet());
		setTrain(true, false);
		
		file = annoObj.getORFtrainCDSfile();
		if (file !=null && !file.equals("") && !file.equals("-1")) {
			txtTrainCDSfile.setText(file);
			setTrain(false, true);
		}
	}
	
	private boolean keep() {
		curManData.setGODB("");	
		curManData.setSlimSubset("");
		curManData.setSlimFile("");
		if(cmbGODB.getSelectedIndex() != 0) {
			curManData.setGODB(cmbGODB.getSelectedItem().trim());
			
			if (radSlimSubset.isSelected() && cmbSlimSubset.getSelectedIndex()!=0) 
				curManData.setSlimSubset(cmbSlimSubset.getSelectedItem());
			else 
				if (radSlimOBOFile.isSelected() && txtSlimOBOFile.getText().trim()!="") 
					curManData.setSlimFile(txtSlimOBOFile.getText());
			
			if (chkNoGO.isSelected()) 	curManData.setNoGO("1"); //CAS331
			else 						curManData.setNoGO("0");
		}
		else if (!radPrNone.isSelected()) { // CAS331
			String msg = "If you have created the GO database, please define it; it can help with pruning";
			JOptionPane.showMessageDialog(this, msg, "No GOdb", JOptionPane.PLAIN_MESSAGE);
		}
		
		String x;
		ManagerData.AnnoData annoObj = curManData.getAnnoObj();
		
		if (chkSPpref.isSelected()) annoObj.setSPpref("1");
		else annoObj.setSPpref("0");
		
		if (chkRmECO.isSelected()) annoObj.setRmECO("1"); 
		else annoObj.setRmECO("0");
		
		
		if (radPrAlign.isSelected()) 		annoObj.setPruneType("1");// CAS331
		else if (radPrDesc.isSelected()) 	annoObj.setPruneType("2"); 
		else 								annoObj.setPruneType("0");
		
		if (chkAltStart.isSelected()) annoObj.setORFaltStart("1");
		else annoObj.setORFaltStart("0");
		
		x = txtHitEval.getText();
		if (Static.isDouble("Hit E-value", x)) annoObj.setORFhitEval(x);
		else return rcMsg("Hit E-value", "Must be double '" + x + "'");
		
		x = txtHitSim.getText();
		if (Static.isInteger("Hit %Similarity", x)) annoObj.setORFhitSim(x);
		else return rcMsg("Hit %Similarity", "Must be integer '" + x + "'");
		
		x = txtLenDiff.getText();
		if (Static.isDouble("Length Difference", x)) annoObj.setORFlenDiff(x);
		else return rcMsg("Length Difference", "Must be double '" + x + "'");
		
		x = txtMkDiff.getText();
		if (Static.isDouble("Markov Difference", x)) annoObj.setORFmkDiff(x);
		else return rcMsg("Markov Difference", "Must be double '" + x + "'");
			
		x = txtTrainMinSet.getText();
		if (Static.isInteger("Minimum number of sequences used for training", x)) annoObj.setORFtrainMinSet(x);
		else return rcMsg("Minimum Set", "Must be integer '" + x + "'");
		
		if (!radTrainCDSfile.isSelected()) annoObj.setORFtrainCDSfile("");
		else {
			x = txtTrainCDSfile.getText();
			if (x.contentEquals("") || x.contentEquals("-")) 
				return rcMsg("Train CDS File", "Cannot be blank with this option selected");
			annoObj.setORFtrainCDSfile(x);
		}
		
		// Similarity
		if (chkSelfN.isSelected() && txtSelfNargs.getText().length()==0) 
			return rcMsg("BlastN Params", "Cannot be blank");
		annoObj.setSelfBlastn(chkSelfN.isSelected());
		annoObj.setSelfBlastnParams(txtSelfNargs.getText());
		
		if (chkSelfX.isSelected() && txtSelfXargs.getText().length()==0) 
			return rcMsg("TBlastX Params", "Cannot be blank");
		annoObj.setSelfTblastx(chkSelfX.isSelected());
		annoObj.setSelfTblastxParams(txtSelfXargs.getText());
		
		if (chkSelfP.isSelected() && txtSelfPargs.getText().length()==0) 
			return rcMsg("BlastP Params", "Cannot be blank");
		annoObj.setSelfBlastp(chkSelfP.isSelected());
		annoObj.setSelfBlastpParams(txtSelfPargs.getText());
		annoObj.setSelfBlastpPgm(cmbSearchPgms.getSelectedItem());
		
		x = txtPairsLimit.getText();
		int limit=0; //CAS331 set even if no selected
		if(x.length() > 0) {
			try {
				limit = Integer.parseInt(x);
				annoObj.setPairsLimit(limit);
			}
			catch (Exception e) {return rcMsg("Pair Limit", "Must be integer '" + x + "'");}
		}
		if (chkSelfN.isSelected() || chkSelfX.isSelected() || chkSelfP.isSelected()) {
			if (limit==0) return rcMsg("Pair Limit",
				"Cannot have 'Pairs limit' = 0 when a self blast option is selected.\n" +
				"Either set a Pairs limit or uncheck all circles under SIMILAR PAIRS.");
		}
		return true;
	}
	private boolean rcMsg(String title, String msg) {
		JOptionPane.showMessageDialog(this,title + "\n" + msg, "Error", JOptionPane.PLAIN_MESSAGE);
		return false;
	}
	private void setDefaults() {
		chkSPpref.setSelected(false);
		chkRmECO.setSelected(true);
		
		radPrNone.setSelected(true); //CAS331
		
		cmbGODB.setSelectedIndex(0); 
		chkNoGO.setSelected(false); chkNoGO.setEnabled(false);//CAS331
		cmbSlimSubset.setSelectedIndex(0);
		
		boolean bNTdb = !isAAdb; // everything is disabled
		if (mProps==null) mProps = new TCWprops(TCWprops.PropType.Annotate);
		
		// ORF
		chkAltStart.setEnabled(bNTdb);
		txtHitEval.setEnabled(bNTdb);
		txtHitSim.setEnabled(bNTdb);
		
		txtLenDiff.setEnabled(bNTdb);
		txtMkDiff.setEnabled(bNTdb);
		
		txtTrainMinSet.setEnabled(bNTdb);
		radTrainHit.setEnabled(bNTdb); 
		radTrainCDSfile.setEnabled(bNTdb);
		
		// Sim
		chkSelfN.setSelected(false);  						chkSelfN.setEnabled(bNTdb);
		txtSelfNargs.setText(BlastArgs.getBlastnArgs());	txtSelfNargs.setEnabled(bNTdb); 
		
		chkSelfX.setSelected(false);  						chkSelfX.setEnabled(bNTdb);
		txtSelfXargs.setText(BlastArgs.getTblastxArgs());	txtSelfXargs.setEnabled(bNTdb);
		
		chkSelfP.setSelected(false);  						chkSelfP.setEnabled(true); //CAS330
		txtSelfPargs.setText(BlastArgs.getDiamondArgsORF());txtSelfPargs.setEnabled(true);
		
		chkSelfP.setSelected(false);  						chkSelfP.setEnabled(true);
		cmbSearchPgms.setSelectedIndex(0); 					// before set save
		saveDiaArgs = BlastArgs.getDiamondArgsORF();
		saveBlaArgs = BlastArgs.getBlastArgsORF();
		txtSelfPargs.setText(saveDiaArgs);					txtSelfPargs.setEnabled(true);
		
		txtPairsLimit.setText(mProps.getProperty("Anno_pairs_limit"));
		
		for (int i=0; i<6; i++) ntJLabel[i].setEnabled(bNTdb);
		
		if (isAAdb) return; /***************/
		
		// ORF - getProperty returns default
		String bAlt = mProps.getProperty("Anno_ORF_alt_start");
		boolean check = (bAlt.equals("1")) ? true : false;
		chkAltStart.setSelected(check);
		
		txtHitEval.setText(mProps.getProperty("Anno_ORF_hit_evalue")); 
		txtHitSim.setText(mProps.getProperty("Anno_ORF_hit_sim")); 
		
		txtLenDiff.setText(mProps.getProperty("Anno_ORF_len_diff")); 
		txtMkDiff.setText(mProps.getProperty("Anno_ORF_mk_diff")); 
		
		radTrainHit.setSelected(true);  // CAS327 was enabled
		txtTrainMinSet.setText(mProps.getProperty("Anno_ORF_train_min_set")); 
		txtTrainCDSfile.setText(""); 
	}
	/***********************************************************************/
	private TCWprops mProps=null;
	private HostsCfg hostsObj = null;
	private ManagerData curManData = null;
	private ManagerFrame theManFrame=null;
	private boolean isAAdb=false;
	
	//Anno controls
	private JPanel pnlAnnoDBOptions = null;
	
	// Best Anno
	private JCheckBox chkSPpref = null;
	private JCheckBox chkRmECO = null; // CAS305
	private JRadioButton radPrDesc = null, radPrAlign = null, radPrNone = null;
	
	// GO
	private ButtonComboBox cmbGODB = null;
	private JCheckBox chkNoGO = null;
	
	private JRadioButton radSlimSubset=null;
	private ButtonComboBox cmbSlimSubset = null;
	
	private JRadioButton radSlimOBOFile=null;
	private JTextField txtSlimOBOFile = null;
	private JButton   btnSlimOBOFile = null;
	
	// ORF
	private JCheckBox chkAltStart = null;
	
	private JTextField txtHitEval=null, txtHitSim=null, txtLenDiff=null, txtMkDiff= null;
	
	private JRadioButton  radTrainHit=null, radTrainCDSfile=null;
	private JTextField 	  txtTrainMinSet=null;
	
	private JTextField txtTrainCDSfile = null;
	private JButton btnTrainCDSfile = null;
	
	// Similarity
	private JCheckBox     chkSelfN = null, chkSelfX=null, chkSelfP=null;
	private JTextField     txtSelfNargs = null, txtSelfXargs = null, txtSelfPargs = null; // CAS314 was JTextArea
	private ButtonComboBox cmbSearchPgms = null;
	private JTextField    txtPairsLimit = null;
	
	private String saveDiaArgs="", saveBlaArgs="";
	private JLabel [] ntJLabel = new JLabel [10];
}
