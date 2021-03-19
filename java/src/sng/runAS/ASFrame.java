package sng.runAS;
/*************************************************
 * Creates interface for Annotation Setup
 */

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.TreeMap;

import util.database.Globalx;

import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.TimeHelpers;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class ASFrame extends JDialog implements WindowListener{
	private static final long serialVersionUID = -3424085221960261295L; 
	private boolean isDemo = ASMain.isDemo; // upDir: UniProt_demo, GO_tmpdemo
	private boolean isOBO = ASMain.isOBO; 
	
	private final String subset = DoUP.subset;
	private final String rootDir = Globalx.ANNODIR; // projects/DBfasta
	private final String upDir =   rootDir + "/UniProt_";
	private final String goDirTar =   rootDir + "/GO_tmp";
	private final String goDirOBO =   rootDir + "/GO_obo";
	private final String goPreDB = Globalx.goPreDB;
	
	private final String cfgPrefix = Globalx.PROJDIR +  "/AnnoDBs_"; // UniProt_date is added; CAS316 remove ./
	private final String cfgSuffix = Globalx.CFG; // CAS315 added
	
	private final String helpDir = "html/runAS/ASFrame.html";
	
	private final String demoSuffix = "demo";
	
	private final int LABEL_WIDTH = 55;
	private final int LABEL_WIDTH2 = 75; 
	private final Color fastaColor = new Color(200, 200, 240);
	private final Color datColor = Color.PINK;
	private final Color dgColor = new Color(169, 204, 143);
	
	private final String strDate = new SimpleDateFormat("MMMyyyy").format(new Date());
	private final String [] options = {"Confirm", "Cancel"};
	
	public ASFrame() {
		addWindowListener(this);
		parent = this;
		
		createMainPanel();

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);	
		setResizable(true);
		add(mainPanel);
		setWindowSettings("TCW Anno Setup " + Globalx.strTCWver);
		setVisible(true);
		
		upObj = new DoUP(this);
		goObj = new DoGO(this);
		oboObj = new DoOBO(this);
		runCheck();
	}
	private void createMainPanel() {	
		mainPanel = Static.createPagePanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		createDIRpanel(); 
		mainPanel.add(new JSeparator());
		mainPanel.add(Box.createVerticalStrut(10));
		
		createLeftpanel(); 
		createRightpanel();
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setContinuousLayout(false);
        
        splitPane.setBorder(new EmptyBorder(2, 2, 2, 2));
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
		mainPanel.add(splitPane);
		
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(new JSeparator());
		mainPanel.add(Box.createVerticalStrut(10));
		createLowerPanel();
		
		mainPanel.setMaximumSize(mainPanel.getPreferredSize());
		mainPanel.setMinimumSize(mainPanel.getPreferredSize());
	}
	private void createDIRpanel() {
		JPanel dirPanel = Static.createPagePanel();
		JPanel row;
		
		row = Static.createRowPanel();
		row.add(new JLabel("TCW Annotation Directories") );
		dirPanel.add(row);
		
		// UniProt
		row = Static.createRowPanel();
		lblUPdir = Static.createLabel("UniProt");
		row.add(lblUPdir);
		row.add(Box.createHorizontalStrut(LABEL_WIDTH-lblUPdir.getPreferredSize().width));
		
		txtUPdir = new JTextField(20);
		txtUPdir.setMaximumSize(txtUPdir.getPreferredSize());
		txtUPdir.setText(getDir(upDir));
		row.add(txtUPdir);
		
		JButton btnGetFile = new JButton("...");
		btnGetFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String fname = fileChooser();
				if (fname!=null && fname!="") {
					txtUPdir.setText(fname);
					runCheck();
				}
			}
		});
		row.add(btnGetFile);
		dirPanel.add(row);
		
		// GO
		row = Static.createRowPanel();
		lblGOdir = Static.createLabel("GO");
		row.add(lblGOdir);
		row.add(Box.createHorizontalStrut(LABEL_WIDTH-lblGOdir.getPreferredSize().width));
		
		txtGOdir = new JTextField(20);
		txtGOdir.setMaximumSize(txtGOdir.getPreferredSize());
		String goDir = (isOBO) ? goDirOBO : goDirTar;
		txtGOdir.setText(getDir(goDir));
		row.add(txtGOdir);
		
		JButton btnGetFile2 = new JButton("...");
		btnGetFile2.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String fname = fileChooser();
				if (fname!=null && fname!="") {
					txtGOdir.setText(fname);
					runCheck();
				}
			}
		});
		row.add(btnGetFile2);
		dirPanel.add(row);
		
		dirPanel.setMaximumSize(dirPanel.getPreferredSize());
		dirPanel.setMinimumSize(dirPanel.getPreferredSize());
		mainPanel.add(dirPanel);
	}
	private void createLeftpanel() {
		leftPanel = Static.createPagePanel();
		leftPanel.add(Box.createVerticalStrut(10));
		JPanel row;
		
		row = Static.createRowPanel();
		row.add(new JLabel("Taxonomic"));
		leftPanel.add(row);
		
		row = Static.createRowPanel();
		row.add(new JLabel("Swiss  TrEMBL"));
		leftPanel.add(row);
		
		for (int i=0; i<upTaxo.length; i++) {
			row = Static.createRowPanel();
			cbSwiss[i] = new JCheckBox();
			cbTrembl[i] = new JCheckBox();
			cbSwiss[i].setBackground(Color.white);
			cbTrembl[i].setBackground(Color.white);
			cbSwiss[i].setOpaque(true);
			cbTrembl[i].setOpaque(true);
			row.add(cbSwiss[i]);
			row.add(Box.createHorizontalStrut(5));
			row.add(cbTrembl[i]);
			row.add(Box.createHorizontalStrut(10));
			row.add(new JLabel(upTaxo[i]));
			
			row.setMaximumSize(row.getPreferredSize());
			row.setMinimumSize(row.getPreferredSize());
			leftPanel.add(row);
		}
		leftPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		JButton doUP = new JButton("1. Build Tax");
		doUP.setEnabled(true);
		doUP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runUP();
			}
		});
		row.add(doUP);
		leftPanel.add(row);
		leftPanel.setMaximumSize(leftPanel.getPreferredSize());
		leftPanel.setMinimumSize(leftPanel.getPreferredSize());
	}
	private void createRightpanel() {
		rightPanel = Static.createPagePanel();
		rightPanel.add(Box.createVerticalStrut(10));
		JPanel row;
		
		JPanel upPanel = Static.createPagePanel();
		row = Static.createRowPanel();
		row.add(new JLabel(" Full UniProt") );
		upPanel.add(row);
		
		row = Static.createRowPanel();
		cbFullSwiss = new JCheckBox();
		cbFullSwiss.setOpaque(true);
		cbFullSwiss.setBackground(Color.white);
		row.add(cbFullSwiss);
		row.add(new JLabel("SwissProt"));
		upPanel.add(row);
		
		row = Static.createRowPanel();
		cbFullTrembl = new JCheckBox();
		cbFullTrembl.setOpaque(true);
		cbFullTrembl.setBackground(Color.white);
		row.add(cbFullTrembl);
		row.add(new JLabel("TrEMBL"));
		upPanel.add(row);
		
		upPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		JButton doUP = new JButton("2. Build Full");
		doUP.setEnabled(true);
		doUP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runFull();
			}
		});
		row.add(doUP);
		upPanel.add(row);
		
		////////////////////////////
	    JPanel goPanel = Static.createPagePanel();
		row = Static.createRowPanel();
		row.add(new JLabel(" GO (Gene Ontology)") );
		goPanel.add(row);
		
		row = Static.createRowPanel();
		lblGOdb = Static.createLabel(" Database");
		row.add(lblGOdb);
		row.add(Box.createHorizontalStrut(LABEL_WIDTH2-lblGOdb.getPreferredSize().width));
		txtGOdb = new JTextField(10);
		txtGOdb.setMaximumSize(txtGOdb.getPreferredSize());
		txtGOdb.setText(getGOdb());
		txtGOdb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runCheck();
			}
		});
		row.add(txtGOdb);
		goPanel.add(row);
		
		goPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		JButton doGO = new JButton("3. Build GO");
		doGO.setEnabled(true);
		doGO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runGO();
			}
		});
		row.add(doGO);
		goPanel.add(row);
		goPanel.setMaximumSize(goPanel.getPreferredSize());
		goPanel.setMinimumSize(goPanel.getPreferredSize());
		
		rightPanel.add(upPanel);
		rightPanel.add(Box.createVerticalStrut(50));
		rightPanel.add(goPanel);
		
		rightPanel.setMaximumSize(rightPanel.getPreferredSize());
		rightPanel.setMinimumSize(rightPanel.getPreferredSize());
	}
	private void createLowerPanel() {
		JPanel panel = Static.createPagePanel();
		JPanel row = Static.createRowPanel();
		
		JButton bc = new JButton("Check");
		bc.setBackground(dgColor);
		bc.setOpaque(true);
		bc.setEnabled(true);
		bc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runCheck();
			}
		});
		row.add(bc);
		panel.add(row);
		
		row.add(Box.createHorizontalStrut(35));
		
		JButton ba = new JButton("AnnoDBs.cfg");
		ba.setEnabled(true);
		ba.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runWriteAnno();
			}
		});
		row.add(ba);
		row.add(Box.createHorizontalStrut(35));
		
		final JButton btnHelp = new JButton("Help");
	    btnHelp.setBackground(Globalx.HELPCOLOR);
	    btnHelp.setOpaque(true);
	    btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(getInstance(), "RunAS Help", helpDir );
			}
	    });
		row.add(btnHelp);
		panel.add(row);
		
		panel.setMaximumSize(panel.getPreferredSize());
		panel.setMinimumSize(panel.getPreferredSize());
		mainPanel.add(panel);
	}
	private ASFrame getInstance() {return this;}
	
	/**************************************************
	 * XXX Actions
	 */
	private void runUP() {
		runCheck(); // make sure spDat/trDat is current
		String upPath = txtUPdir.getText();
		if (upPath.equals("")) {
			JOptionPane.showMessageDialog(this,"UniProt directory is blank" +
					"\nMust enter a string for the new directory name\n");
			return;
		}
		Vector <String> spFiles = new Vector <String> ();
		Vector <Boolean> spDats = new Vector <Boolean> ();
		String spDown="", trDown="", spFasta="", trFasta="";
		for (int i=0; i<cbSwiss.length; i++)
			if (cbSwiss[i].isSelected()) {
				spFiles.add(upTaxo[i]);
				if (spDat.containsKey(upTaxo[i]))  {
					 spFasta= getList(upTaxo[i], spFasta);
					 spDats.add(true);
				}
				else {
					spDown = getList(upTaxo[i], spDown);
					spDats.add(false);
				}
			}
		Vector <String> trFiles = new Vector <String> ();
		Vector <Boolean> trDats = new Vector <Boolean> ();
		for (int i=0; i<cbTrembl.length; i++)
			if (cbTrembl[i].isSelected()) { 
				trFiles.add(upTaxo[i]);
				if (trDat.containsKey(upTaxo[i])) {
					trFasta = getList(upTaxo[i], trFasta);
					trDats.add(true);
				}
				else  {
					if (upTaxo[i].equals("bacteria")) 
						 trDown = getList(upTaxo[i] + "(BIG)", trDown);
					else trDown = getList(upTaxo[i], trDown);
					trDats.add(false);
				}
			}
		if (spFiles.size()==0 && trFiles.size()==0) {
			JOptionPane.showMessageDialog(this,"No files have been selected for download " + 
					"\nYou must select at least one file\n");
			return;
		}
		String msg="";
		if (spDown!="") msg += "Download SP - " + spDown + "\n";
		if (trDown!="") msg += "Download TR - " + trDown + "\n";
		if (spFasta!="") msg += "Create SP Fasta - " + spFasta + "\n";
		if (trFasta!="") msg += "Create TR Fasta - " + trFasta + "\n";
		int ret = JOptionPane.showOptionDialog(this, 
				msg + "Continue?",
				"UniProt Taxonomic", JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return;
		
		upObj.xTaxo(spFiles, trFiles, spDats, trDats, upPath);
		
		runCheck();
	}
	/******************************************************************/
	private void runFull() {
		runCheck();
		String upPath = txtUPdir.getText();
		if (upPath.equals("")) {
			JOptionPane.showMessageDialog(this,"UniProt directory is blank" +
					"\nMust enter a string for the new directory name\n");
			return;
		}
		boolean sp = cbFullSwiss.isSelected();
		boolean tr = cbFullTrembl.isSelected();
		if (!sp && !tr) {
			JOptionPane.showMessageDialog(this,"Select SwissProt or TrEMBL ");
			return;
		}
		String msg="";
		if (sp) {
			boolean e = false;
			if (spDat.containsKey(subset)) {
				e = true;
				msg="Full SwissProt download exists\nCreate Full SwissProt Subset files\n";
				if (spFasta.containsKey(subset)) 
					msg += "SP fullSubset files exists - they will be overwritten\n"; // CAS315
			}
			else msg= "Download Full SwissProt\n";
			
			int n = (e) ? spFasta.size()-1 : spFasta.size();
			msg += n + " Taxonomic files to remove entries from Full\n\n"; // CAS315
		}
		if (tr) {
			boolean e = false;
			if (trDat.containsKey(subset)) {
				e = true;
				msg="Full TrEMBl download exists\nCreate Full Trembl Subset files\n";
				if (trFasta.containsKey(subset)) 
					msg += "TR fullSubset files exists - they will be overwritten\n";
			}
			else msg+= "Download Full TrEMBL (file is VERY LARGE)\n";
			
			int n = (e) ? trFasta.size()-1 : trFasta.size();
			msg += n + " Taxonomic files to remove entries from Full\n\n";
		}
		int ret = JOptionPane.showOptionDialog(this, msg + "Continue?",
			"Full UniProt subsets", JOptionPane.YES_NO_OPTION, 
			JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return;
		
		upObj.xFull(sp, tr, spDat.containsKey(subset), trDat.containsKey(subset), upPath);
		runCheck();
	}
	/******************************************************************/
	private void runGO() {
		runCheck();
		String goPath = txtGOdir.getText();
		if (goPath.equals("")) {
			JOptionPane.showMessageDialog(this,"GO directory is blank" +
					"\nMust enter a string for the new GO directory name\n");
			return;
		}
		int n = (spDat.size()+trDat.size());
		if (n==0) {
			JOptionPane.showMessageDialog(this,"No UniProt .dat files exist.\n" +
					"Download the UniProt files you will be\n" +
					"using for annotation before running this step. ");
			Out.PrtWarn("Cancelling create GO database");
			return;
		}
		
		String msg = "Mapping " + n + " UniProt files to GO database.\n";
		if (!hasGOfile) msg += "Download GO files. Build GO database.\n";
		else msg += "GO files exist. Build GO database.\n";
		
		String goDBname = txtGOdb.getText();
		int rc;
		
		if (isOBO) rc = oboObj.goDBcheck(goDBname);
		else  		rc = goObj.checkGODB(goDBname);
		
		if (rc==2) { // database exists
			int ret = JOptionPane.showOptionDialog(this, 			
				"GO database exists:  " + goDBname + "\n" + "Delete and Continue?",
				"GO database", JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (ret == JOptionPane.NO_OPTION) {
				Out.PrtWarn("Cancelling create GO database");
				return;
			}
			msg += "Delete current database " + goDBname;
		}
		else if (rc==1){
			msg += "Partial database " + goDBname + " exists; it will be deleted.";
		}

		int ret = JOptionPane.showOptionDialog(this, 
				msg + "\nContinue?",
				"GO database", JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null,  options, options[0]);
		if (ret==JOptionPane.NO_OPTION) {
			Out.PrtWarn("Cancelling create GO database");
			return;
		}
		
		if (isOBO)  oboObj.run(txtUPdir.getText(), goPath, txtGOdb.getText(), hasGOfile);
		else 		goObj.run(txtUPdir.getText(), goPath, txtGOdb.getText(), hasGOfile);
		runCheck();
	}
	/******************************************************************/
	// Write Anno_Update_date.cfg
	private void runWriteAnno() {
		try {
			runCheck();
			String upDir = txtUPdir.getText();
			
			String file = cfgPrefix + upDir.substring(upDir.lastIndexOf("/")+1) + cfgSuffix; 
			BufferedWriter out = new BufferedWriter(new FileWriter(file, false));
			Out.PrtSpMsg(0,"");
			Out.PrtSpMsg(0, "Write " + file);
			String go = txtGOdb.getText();
			if (!go.equals("")) out.write("Anno_GO_DB = " + txtGOdb.getText() + "\n\n"); 
			
			upDir += "/";
			String date = TimeHelpers.getDBDate(); 
			
			int nFile=1;								
			for (String tax : spFasta.keySet()) {
				out.write("Anno_DBtaxo_" + nFile + "= " + tax + "\n");
				String fasta = upDir + "sp_" + tax + "/" + spFasta.get(tax);
				out.write("Anno_DBfasta_" + nFile + "= " + fasta + "\n");
				out.write("Anno_DBdate_" + nFile + "= " + date + "\n\n");
				nFile++;
			}
			for (String tax : trFasta.keySet()) {
				out.write("Anno_DBtaxo_" + nFile + "= " + tax + "\n");
				String fasta = upDir + "tr_" + tax + "/" + trFasta.get(tax);
				out.write("Anno_DBfasta_" + nFile + "= " + fasta + "\n");
				out.write("Anno_DBdate_" + nFile + "= " + date + "\n\n");
				nFile++;
			}
			
			out.close();
			Out.PrtSpMsg(1, (nFile-1) + " entries written");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error creating TCW.anno file");}
	}
	/**************************************************
	 * Figures out what resources exist, 
	 * set data structures used by other 'run' methods,
	 * and highlights the interface
	 */
	private void runCheck() {
		lblUPdir.setBackground(Color.white);
		lblGOdir.setBackground(Color.white);
		lblGOdb.setBackground(Color.white);
		cbFullSwiss.setBackground(Color.white);
		cbFullTrembl.setBackground(Color.white);
		for (int i=0; i<upTaxo.length; i++) {
			cbSwiss[i].setBackground(Color.white);
			cbTrembl[i].setBackground(Color.white);
		}
		spFasta.clear(); trFasta.clear(); spDat.clear(); trDat.clear();
		
		String upDir = txtUPdir.getText();  if (!upDir.endsWith("/")) upDir += "/";
		String goDir = txtGOdir.getText();  if (!goDir.endsWith("/")) goDir += "/";
				
		try {
			// GO
			hasGOfile=false;
			String goFullPath = goDir;
			goFullPath += (isOBO) ? DoOBO.goFile : DoGO.goDailyFile; 
			if (new File(goDir).exists()) {
				lblGOdir.setBackground(Color.pink);
				if (new File(goFullPath).exists()) {
					lblGOdir.setBackground(fastaColor);
					hasGOfile=true;
				} 
			}
			int rc;
			if (isOBO)  rc = oboObj.goDBcheck(txtGOdb.getText());
			else 		rc = goObj.checkGODB(txtGOdb.getText());
			
			if (rc>0) {
				if (rc==2) 		lblGOdb.setBackground(fastaColor);
				else if (rc==1) lblGOdb.setBackground(Color.pink);
			}
			
			// UP dir
			File dir = new File(upDir);
			if (dir.exists()) lblUPdir.setBackground(fastaColor);
			else return;
			
			// UP Files
			File [] dirs = dir.listFiles();
			for (File d : dirs) {
				if (!d.isDirectory()) continue;
				String dirName = d.getName();
				
				File [] xfiles = d.listFiles();
				for (File f : xfiles) {
					String fileName=f.getName();
					boolean isFasta = fileName.endsWith(".fasta") || fileName.endsWith(".fasta.gz"); // CAS315
					boolean isDat = (fileName.endsWith(".dat") || fileName.endsWith(".dat.gz"));
					if (isFasta || isDat) {
						runCheckAdd(dirName, fileName, isFasta);
					}
				}	
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Checking directories");}
	}
	private void runCheckAdd(String dirName, String file, boolean isFasta) {
		String [] tok = dirName.split(("_"));
		if (tok[0].equals(Globalx.SP)) {
			if (tok[1].equals(subset)) { // Full
				if (isFasta) {
					spFasta.put(subset, file);
					cbFullSwiss.setBackground(fastaColor);
				}
				else {
					spDat.put(subset, file);
					if (!spFasta.containsKey(subset)) cbFullSwiss.setBackground(datColor);
				}
				return;
			}
			
			for (int i=0; i<upTaxo.length; i++) { // Tax
				if (tok[1].equals(upTaxo[i])) {
					if (isFasta) {
						spFasta.put(tok[1], file);
						cbSwiss[i].setBackground(fastaColor);
					}
					else {
						spDat.put(tok[1], file);
						if (!spFasta.containsKey(tok[1])) cbSwiss[i].setBackground(datColor);
					}
					return;
				}
			}
		}
		if (tok[0].equals("tr")) {
			if (tok[1].equals(subset)) { // Full
				if (isFasta) {
					trFasta.put(subset, file);
					cbFullTrembl.setBackground(fastaColor);
				}
				else {
					trDat.put(subset, file);
					if (!trFasta.containsKey(subset)) cbFullTrembl.setBackground(datColor);
				}
				return;
			}
			
			for (int i=0; i<upTaxo.length; i++) { // Tax
				if (tok[1].equals(upTaxo[i])) {
					if (isFasta) {
						trFasta.put(tok[1], file);
						cbTrembl[i].setBackground(fastaColor);
					}
					else {
						trDat.put(tok[1], file);
						if (!trFasta.containsKey(tok[1])) cbTrembl[i].setBackground(datColor);
					}
					return;
				}
			}
		}
	}
	
	/***************************************************
	 * Utilities
	 */
	private String getList(String x, String list) {
		if (list.equals("")) return x;
		else return list + "," + x;
	}
	// set defaults for startup
	private String getDir(String initDir) {
		if (isDemo) return initDir+demoSuffix;
		return initDir + strDate;
	}
	private String getGOdb() {
		if (isDemo) return goPreDB + demoSuffix;
		return goPreDB + strDate;
	}
	
	private String fileChooser() {
		try {
			final JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setCurrentDirectory(new File(rootDir));
			if(fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
				String filePath = fc.getSelectedFile().getCanonicalPath();
				String cur = System.getProperty("user.dir");
				
				if(filePath.contains(cur)) filePath = filePath.replace(cur, ".");
				
				return filePath;
			}
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems getting file");}
		return null;
	}
	
	private void setWindowSettings(final String title) {
		setTitle(title);
		UIHelpers.centerScreen(this);
		pack();
	}
	public void windowClosed(WindowEvent arg0) {System.exit(0);}
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosing(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	/*************************************************
	 * Variables
	 */
	
	private JTextField txtUPdir = new JTextField();
	private JTextField txtGOdir = new JTextField();
	private JLabel lblUPdir, lblGOdir;

	// Full uniprot - correct set as of Jan-2021
	private String [] upTaxo = {"archaea","bacteria","fungi", 
			"human","invertebrates", "mammals","plants",
			"rodents", "vertebrates", "viruses"};
	private JCheckBox cbFullSwiss = null, cbFullTrembl = null;
	private JCheckBox [] cbSwiss = new JCheckBox [upTaxo.length];
	private JCheckBox [] cbTrembl = new JCheckBox [upTaxo.length];
	
	// Go
	private JTextField txtGOdb = new JTextField();
	private JLabel lblGOdb;
	
	private JPanel mainPanel = null, leftPanel = null, rightPanel = null;
	
	// status
	private boolean hasGOfile=false;
	private TreeMap <String, String> spFasta = new TreeMap <String, String> ();
	private TreeMap <String, String> trFasta  = new TreeMap <String, String> ();
	private TreeMap <String, String> spDat = new TreeMap <String, String> ();
	private TreeMap <String, String> trDat  = new TreeMap <String, String> ();
	
	private ASFrame parent;
	private DoUP upObj=null;
	private DoGO goObj=null;
	private DoOBO oboObj=null;
}
