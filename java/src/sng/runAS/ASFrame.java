package sng.runAS;
/*************************************************
 * Creates interface for Annotation Setup
 * CAS339 lots of cleanup while allowing different fasta subset files. 
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
import java.util.HashSet;

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
	
	private final String helpDir = "html/runAS/ASFrame.html";
	
	private final String rootDir =  Globalx.ANNODIR; 	// projects/DBfasta
	private final String upDir =   	GlobalAS.upDir;  	// rootDir + "/UniProt_";
	private final String goDirOBO = GlobalAS.goDirOBO; 	// rootDir + "/GO_obo";
	private final String goPreDB =  Globalx.goPreDB;	// "go_"
	
	private final String cfgPrefix = Globalx.PROJDIR +  "/AnnoDBs_"; // UniProt_date is added; CAS316 remove ./
	private final String cfgSuffix = Globalx.CFG; // CAS315 added
	
	private final String SP = Globalx.SP, TR=Globalx.TR;
	private final static String fullTaxo = GlobalAS.fullTaxo;	// full
	private final static String faSuffix = GlobalAS.faSuffix, datSuffix = GlobalAS.datSuffix;	// .fasta, .dat
	private final static String faGzSuffix = GlobalAS.faGzSuffix, datGzSuffix = GlobalAS.datGzSuffix;	// .fasta.gz, dat.gz
	
	private final static char subsetX = 'x';
	private final String demoSuffix = GlobalAS.demoSuffix; // demo
	
	private final int LABEL_WIDTH = 55;
	private final int LABEL_WIDTH2 = 75; 
	private final Color fastaColor = new Color(200, 200, 240);
	private final Color datColor = Color.PINK;
	private final Color dgColor = new Color(169, 204, 143);
	
	private final String strDate = new SimpleDateFormat("MMMyyyy").format(new Date());
	
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
		
		JButton btnGetFile = Static.createButtonFile("...", true);
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
		txtGOdir.setText(getDir(goDirOBO));
		row.add(txtGOdir);
		
		JButton btnGetFile2 = Static.createButtonFile("...", true);
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
		row.add(Static.createLabel("Taxonomic"));
		leftPanel.add(row);
		
		row = Static.createRowPanel();
		row.add(Static.createLabel("Swiss  TrEMBL"));
		leftPanel.add(row);
		
		for (int i=0; i<upTaxo.length; i++) {
			row = Static.createRowPanel();
			spTaxCheckBox[i] = new JCheckBox();
			trTaxCheckBox[i] = new JCheckBox();
			spTaxCheckBox[i].setBackground(Color.white);
			trTaxCheckBox[i].setBackground(Color.white);
			spTaxCheckBox[i].setOpaque(true);
			trTaxCheckBox[i].setOpaque(true);
			row.add(spTaxCheckBox[i]);
			row.add(Box.createHorizontalStrut(5));
			row.add(trTaxCheckBox[i]);
			row.add(Box.createHorizontalStrut(10));
			row.add(new JLabel(upTaxo[i]));
			
			row.setMaximumSize(row.getPreferredSize());
			row.setMinimumSize(row.getPreferredSize());
			leftPanel.add(row);
		}
		leftPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		JButton doUP = Static.createButtonRun("1. Build Tax", true);
		doUP.setEnabled(true);
		doUP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				buildTax();
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
		row.add(Static.createLabel(" Full UniProt") );
		upPanel.add(row);
		
		row = Static.createRowPanel();
		spFullCheckBox = new JCheckBox();
		spFullCheckBox.setOpaque(true);
		spFullCheckBox.setBackground(Color.white);
		row.add(spFullCheckBox);
		row.add(Static.createLabel("SwissProt"));
		upPanel.add(row);
		
		row = Static.createRowPanel();
		trFullCheckBox = new JCheckBox();
		trFullCheckBox.setOpaque(true);
		trFullCheckBox.setBackground(Color.white);
		row.add(trFullCheckBox);
		row.add(Static.createLabel("TrEMBL"));
		upPanel.add(row);
		
		upPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		JButton doUP = Static.createButtonRun("2. Build Full", true);
		doUP.setEnabled(true);
		doUP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				buildFull();
			}
		});
		row.add(doUP);
		upPanel.add(row);
		
		////////////////////////////
	    JPanel goPanel = Static.createPagePanel();
		row = Static.createRowPanel();
		row.add(Static.createLabel(" GO (Gene Ontology)") );
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
		JButton doGO = Static.createButtonRun("3. Build GO", true);
		doGO.setEnabled(true);
		doGO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				buildGO();
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
		
		JButton bc = Static.createButton("Check");
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
		
		JButton ba = Static.createButton("AnnoDBs.cfg");
		ba.setEnabled(true);
		ba.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runWriteAnno();
			}
		});
		row.add(ba);
		row.add(Box.createHorizontalStrut(35));
		
		final JButton btnHelp = Static.createButtonHelp("Help", true);
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
	private void buildTax() {
		runCheck(); // make sure spDat/trDat is current
		String upPath = txtUPdir.getText();
		if (upPath.equals("")) {
			UserPrompt.showMsg(this,"UniProt directory is blank" +
					"\nMust enter a string for the new directory name\n");
			return;
		}
		
		String spDownList="", trDownList="", spFastaList="", trFastaList="";
		Vector <String>  spTaxo = new Vector <String> ();
		Vector <Boolean> spHasDat = new Vector <Boolean> ();
		Vector <String>  trTaxo = new Vector <String> ();
		Vector <Boolean> trHasDat = new Vector <Boolean> ();
		
		for (int i=0; i<spTaxCheckBox.length; i++) {
			if (spTaxCheckBox[i].isSelected()) {
				spTaxo.add(upTaxo[i]);
				if (spDatSet.contains(upTaxo[i]))  {
					 spFastaList= mkTaxoList(upTaxo[i], spFastaList);
					 spHasDat.add(true);
				}
				else {
					spDownList = mkTaxoList(upTaxo[i], spDownList);
					spHasDat.add(false);
				}
			}
		}
		
		for (int i=0; i<trTaxCheckBox.length; i++) {
			if (trTaxCheckBox[i].isSelected()) { 
				trTaxo.add(upTaxo[i]);
				if (trDatSet.contains(upTaxo[i])) {
					trFastaList = mkTaxoList(upTaxo[i], trFastaList);
					trHasDat.add(true);
				}
				else  {
					if (upTaxo[i].equals("bacteria")) 
						 trDownList = mkTaxoList(upTaxo[i] + "(BIG)", trDownList);
					else trDownList = mkTaxoList(upTaxo[i], trDownList);
					trHasDat.add(false);
				}
			}
		}
		if (spTaxo.size()==0 && trTaxo.size()==0) {
			UserPrompt.showMsg(this,"No files have been selected for download " + 
					"\nYou must select at least one file\n");
			return;
		}
		String msg="";
		if (spDownList!="") msg += "Download SP - " + spDownList + "\n";
		if (trDownList!="") msg += "Download TR - " + trDownList + "\n";
		if (spFastaList!="") msg += "Create SP Fasta - " + spFastaList + "\n";
		if (trFastaList!="") msg += "Create TR Fasta - " + trFastaList + "\n";
		
		if (!UserPrompt.showConfirm(this, "UniProt Taxonomic", msg)) {
			Out.prt("User cancel Build Tax");
			return;
		}
		
		upObj.xTaxo(spTaxo, trTaxo, spHasDat, trHasDat, upPath);
		
		runCheck();
	}
	/******************************************************************/
	private void buildFull() {
	// Check
		runCheck();
		String upPath = txtUPdir.getText();
		if (upPath.equals("")) {
			UserPrompt.showMsg(this,"UniProt directory is blank" +
					"\nMust enter a string for the new directory name\n");
			return;
		}
		boolean bSP = spFullCheckBox.isSelected();
		boolean bTR = trFullCheckBox.isSelected();
		if (!bSP && !bTR) {
			UserPrompt.showMsg(this,"Select SwissProt or TrEMBL ");
			return;
		}
		
	// Prepare state msg and get subsets
		GlobalAS fileObj = new GlobalAS(upPath);
		
		HashSet <String> spTaxoSet=null, trTaxoSet=null;
		String msg="", spSuffix="", trSuffix="";
		if (bSP) {
			if (spDatSet.contains(fullTaxo)) msg="Full SwissProt download exists\n";
			else 							 msg="Download Full SwissProt\n";
			
			TaxoType spTax = new TaxoType(true, "SwissProt", msg);
			spTax.setVisible(true);
			
			spTaxoSet = spTax.getTaxoSet();
			if (spTaxoSet==null) {
				Out.prt("User cancel Build Full");
				return;
			}
			spSuffix =  spTax.getTaxoSuffix();
			
			String path = fileObj.mkNameDir(SP, fullTaxo) + fileObj.mkNameFa(SP, spSuffix);
			if (!checkFile(path)) {
				Out.prt("User cancel Build Full");
				return;
			}
		}
		if (bTR) {
			if (trDatSet.contains(fullTaxo)) msg= "Full TrEMBl download exists\n";
			else 							 msg= "Download Full TrEMBL (file is VERY LARGE)\n";
			
			TaxoType trTax = new TaxoType(false, "TrEMBL", msg);
			trTax.setVisible(true);
			
			trTaxoSet = trTax.getTaxoSet();
			if (trTaxoSet==null) {
				Out.prt("User cancel Build Full");
				return;
			}
			trSuffix =  trTax.getTaxoSuffix();
			
			String path = fileObj.mkNameDir(TR, fullTaxo) + fileObj.mkNameFa(TR, trSuffix);
			if (!checkFile(path)) {
				Out.prt("User cancel Build Full");
				return;
			}
		}
	// Download
		boolean noSPdat=false, noTRdat=false;
		if (bSP && !spDatSet.contains(fullTaxo)) noSPdat=true;
		if (bTR && !trDatSet.contains(fullTaxo)) noTRdat=true;
		if (noSPdat || noTRdat)
			if (!upObj.xFullDown(noSPdat, noTRdat, upPath)) 
				return; 
			
	// Create Fasta file
		upObj.xFullFasta(spSuffix, spTaxoSet, trSuffix, trTaxoSet, upPath);
		runCheck();
	}
	private class TaxoType extends JDialog {
		private static final long serialVersionUID = 1L;
		
	    HashSet <String> taxoSet = new HashSet <String> ();
	    JCheckBox [] taxoCheckBox = new JCheckBox [upTaxo.length];
	    boolean bCancel=false;
	    
	    TaxoType (boolean isSP, String type, String msg) {
	    	boolean isTR = !isSP;
	    	
			setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		setTitle("Full " + type); // sp, tr
    		
    		JPanel selectPanel = Static.createPagePanel();
    		JLabel msgLabel = Static.createLabel(msg);
    		selectPanel.add(msgLabel);
    		selectPanel.add(Box.createVerticalStrut(5));
    		
    		for (int i=0; i<upTaxo.length; i++) {
    			if (isSP && spFaMap.containsKey(upTaxo[i]) || (isTR && trFaMap.containsKey(upTaxo[i]))) {
	    			taxoCheckBox[i] = Static.createCheckBox(upTaxo[i], true);
	    			selectPanel.add(taxoCheckBox[i]);
	    			selectPanel.add(Box.createVerticalStrut(3));
    			}
    			else taxoCheckBox[i]=null;
    		}
	       
	        JButton btnOK = Static.createButton("OK", true);
        	btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
    		JButton btnCancel = Static.createButton("Cancel", true);
    		btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bCancel=true;
					setVisible(false);
				}
			});
    		
    		JPanel buttonPanel = Static.createRowPanel();
    		buttonPanel.add(btnOK);			buttonPanel.add(Box.createHorizontalStrut(20));
    		buttonPanel.add(btnCancel);		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		
    		JPanel mainPanel = Static.createPagePanel();
    		mainPanel.add(selectPanel); 	mainPanel.add(Box.createVerticalStrut(15));
    		mainPanel.add(buttonPanel);
    		
    		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    		add(mainPanel);
    		
    		pack();
    		this.setResizable(false);
    		UIHelpers.centerScreen(this);
		}
		public HashSet <String> getTaxoSet() { 
			if (bCancel) return null;
			
			for (int i=0; i<taxoCheckBox.length; i++) {
				if (taxoCheckBox[i]!=null && taxoCheckBox[i].isSelected()) 
					taxoSet.add(upTaxo[i]);
			}
			 return taxoSet; 
		}
		public String getTaxoSuffix() {
			if (taxoSet.size()==0) return "full";
			
			String suffix="";
			for (String tax : upTaxo) {
				suffix += (taxoSet.contains(tax)) ? tax.substring(0,1).toUpperCase() : subsetX;
			}
			return suffix;
		}
	}
	/******************************************************************/
	private void buildGO() {
		runCheck();
		String goPath = txtGOdir.getText();
		if (goPath.equals("")) {
			UserPrompt.showMsg(this,"GO directory is blank" +
					"\nMust enter a string for the new GO directory name\n");
			return;
		}
		int n = (spDatSet.size()+trDatSet.size());
		if (n==0) {
			UserPrompt.showMsg(this,"No UniProt .dat files exist.\n" +
					"Download the UniProt files you will be\n" +
					"using for annotation before running this step. ");
			Out.PrtWarn("Cancelling Build GO database");
			return;
		}
		String msg;
		if (!hasGOfile) msg = "Download GO files and build GO database.\n";
		else            msg = "GO files exist. Build GO database only.\n";
		msg += "Mapping " + n + " UniProt .dat files to GO database.\n";
		
		
		String goDBname = txtGOdb.getText();
		int rc = oboObj.goDBcheck(goDBname);
		
		if (rc==2) { // database exists
			String msgx = "GO database exists:  " + goDBname + "\n" + "Delete and Continue?";
			if (!UserPrompt.showContinue2(this, "GO database", msgx)) {
				Out.PrtWarn("Cancelling Build GO database");
				return;
			}
			msg += "Delete current database " + goDBname;
		}
		else if (rc==1){
			msg += "Partial database " + goDBname + " exists; it will be deleted.";
		}

		if (!UserPrompt.showContinue(this, "GO database", msg)) {
			Out.PrtWarn("Cancelling Build GO database");
			return;
		}
		
		oboObj.run(txtUPdir.getText(), goPath, txtGOdb.getText(), hasGOfile);
		
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
			
			GlobalAS fileObj = new GlobalAS(upDir);
		
		// SP
			int nFile=1;								
			for (String tax : spFaMap.keySet()) {
				if (!tax.contentEquals(fullTaxo)) {
					String fasta = fileObj.mkNameDir(SP, tax) + spFaMap.get(tax);
					
					out.write("Anno_DBtaxo_" + nFile + "  = " + tax + "\n");
					out.write("Anno_DBfasta_" + nFile + " = " + fasta + "\n");
					out.write("Anno_DBdate_" + nFile + "  = " + date + "\n\n");
					nFile++;
				}
			}
			if (spFaMap.containsKey(fullTaxo)) {
				String fname = spFaMap.get(fullTaxo);
				String taxo = fullTaxo + getCaps(fname);
				String fasta = fileObj.mkNameDir(SP, fullTaxo) + fname;
				
				out.write("Anno_DBtaxo_" + nFile + "  = " + taxo + "\n");
				out.write("Anno_DBfasta_" + nFile + " = " + fasta + "\n");
				out.write("Anno_DBdate_" + nFile + "  = " + date + "\n\n");
				nFile++;
			}
		// TR
			for (String tax : trFaMap.keySet()) {
				if (!tax.contentEquals(fullTaxo)) {
					String fasta = fileObj.mkNameDir(TR, tax) + trFaMap.get(tax);
					
					out.write("Anno_DBtaxo_" + nFile + "  = " + tax + "\n");
					out.write("Anno_DBfasta_" + nFile + " = " + fasta + "\n");
					out.write("Anno_DBdate_" + nFile +  "  = " + date + "\n\n");
					nFile++;
				}
			}
			if (trFaMap.containsKey(fullTaxo)) {
				String fname = trFaMap.get(fullTaxo);
				String taxo = fullTaxo + getCaps(fname);
				String fasta = fileObj.mkNameDir(TR, fullTaxo) + fname;
				
				out.write("Anno_DBtaxo_" + nFile + "  = " + taxo + "\n");
				out.write("Anno_DBfasta_" + nFile + " = " + fasta + "\n");
				out.write("Anno_DBdate_" + nFile + "  = " + date + "\n\n");
				nFile++;
			}
			out.close();
			Out.PrtSpMsg(1, (nFile-1) + " entries written");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error creating TCW.anno file");}
	}
	private String getCaps(String fname) { // CAS339 add informative suffix
		String caps="";
		if (!fname.contains(""+subsetX)) return "";
		
		String suf = fname.substring(fname.lastIndexOf("_")+1);
		suf = suf.substring(0, suf.indexOf("."));
		
		for (int i=0; i<suf.length(); i++) {
			char y = suf.charAt(i);
			if (y!=subsetX) caps += y+"";
		}
		if (caps.length()>0) caps = "_" + caps;
		return caps;
	}
	/**************************************************
	 * Figures out what resources exist, 
	 * set data structures used by other 'run' methods, and highlights the interface
	 */
	private void runCheck() {
		lblUPdir.setBackground(Color.white); lblGOdir.setBackground(Color.white); lblGOdb.setBackground(Color.white);
		spFullCheckBox.setBackground(Color.white);
		trFullCheckBox.setBackground(Color.white);
		for (int i=0; i<upTaxo.length; i++) {
			spTaxCheckBox[i].setBackground(Color.white);
			trTaxCheckBox[i].setBackground(Color.white);
		}
		spFaMap.clear(); trFaMap.clear(); spDatSet.clear(); trDatSet.clear();
		
		String upDir = txtUPdir.getText();  if (!upDir.endsWith("/")) upDir += "/";
		String goDir = txtGOdir.getText();  if (!goDir.endsWith("/")) goDir += "/";
				
		try {
			// GO OBO file downloaded?
			hasGOfile=false;
			String goPath = goDir + GlobalAS.goFile;
			if (new File(goPath).exists()) {
				lblGOdir.setBackground(Color.pink);
				if (new File(goPath).exists()) {
					lblGOdir.setBackground(fastaColor);
					hasGOfile=true;
				} 
			}
			// GOdb exists? (should if OBO downloaded)
			int rc = oboObj.goDBcheck(txtGOdb.getText());
			if (rc>0) {
				if (rc==2) 		lblGOdb.setBackground(fastaColor);
				else if (rc==1) lblGOdb.setBackground(Color.pink);
			}
			
			// UP dir - if not exists, then no files to check
			File dir = new File(upDir);
			if (dir.exists()) lblUPdir.setBackground(fastaColor);
			else return;
			
			// UP tax and full directories
			File [] dirs = dir.listFiles();
			for (File d : dirs) {
				if (!d.isDirectory()) continue;
				String dirName = d.getName();
				
				File [] xfiles = d.listFiles();
				for (File f : xfiles) {
					String fileName=f.getName();
					boolean isFasta = fileName.endsWith(faSuffix) || fileName.endsWith(faGzSuffix); // CAS315
					boolean isDat = (fileName.endsWith(datSuffix) || fileName.endsWith(datGzSuffix));
					if (isFasta || isDat) {
						runCheckAdd(dirName, fileName, isFasta);
					}
				}	
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Checking directories");}
	}
	private void runCheckAdd(String dirName, String file, boolean isFasta) {
		String [] tok = dirName.split(("_")); // e.g. sp_plants or sp_fullSubset
		if (tok[0].equals(SP)) {
			if (tok[1].equals(fullTaxo)) { // Full
				if (isFasta) {			   // If there is multiple, get one with most taxos
					if (spFaMap.containsKey(fullTaxo)) {
						if (useFile(spFaMap.get(fullTaxo), file)) spFaMap.put(fullTaxo, file);
					}
					else spFaMap.put(fullTaxo, file);
					
					spFullCheckBox.setBackground(fastaColor);
				}
				else {
					spDatSet.add(fullTaxo);
					if (!spFaMap.containsKey(fullTaxo)) spFullCheckBox.setBackground(datColor);
				}
				return;
			}
			
			for (int i=0; i<upTaxo.length; i++) { // Tax
				if (tok[1].equals(upTaxo[i])) {
					if (isFasta) {
						spFaMap.put(tok[1], file);	// taxo, file
						spTaxCheckBox[i].setBackground(fastaColor);
					}
					else {
						spDatSet.add(tok[1]);
						if (!spFaMap.containsKey(tok[1])) spTaxCheckBox[i].setBackground(datColor);
					}
					return;
				}
			}
		}
		if (tok[0].equals(TR)) {
			if (tok[1].equals(fullTaxo)) { // Full
				if (isFasta) {
					if (trFaMap.containsKey(fullTaxo)) {
						if (useFile(trFaMap.get(fullTaxo), file)) trFaMap.put(fullTaxo, file);
					}
					else trFaMap.put(fullTaxo, file);
					trFullCheckBox.setBackground(fastaColor);
				}
				else {
					trDatSet.add(fullTaxo);
					if (!trFaMap.containsKey(fullTaxo)) trFullCheckBox.setBackground(datColor);
				}
				return;
			}
			for (int i=0; i<upTaxo.length; i++) { // Tax
				if (tok[1].equals(upTaxo[i])) {
					if (isFasta) {
						trFaMap.put(tok[1], file);
						trTaxCheckBox[i].setBackground(fastaColor);
					}
					else {
						trDatSet.add(tok[1]);
						if (!trFaMap.containsKey(tok[1])) trTaxCheckBox[i].setBackground(datColor);
					}
					return;
				}
			}
		}
	}
	
	private boolean useFile(String file1, String file2) { // true is replace with file2
		if (file1.contains(fullTaxo)) return true;
		if (file2.contains(fullTaxo)) return false;
		
		int cnt1=0, cnt2=0;
		for (int i=0; i<file1.length(); i++) if (file1.charAt(i)==subsetX) cnt1++;
		for (int i=0; i<file2.length(); i++) if (file2.charAt(i)==subsetX) cnt2++;
		return cnt1<cnt2; // use the one with the least removed
	}
	/***************************************************
	 * Utilities
	 */
	private String mkTaxoList(String x, String list) {
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
	private boolean checkFile(String name) {
		try {
			File f = new File(name);
	        if (f.exists()) {
	        	String msg =  "File exists: " + name + "\nConfirm will overwrite it.";
        		if (UserPrompt.showConfirm("File Exists", msg)) return true;
        		else 											return false;
	        }
	        return true;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Checking " + name);return false;}
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

	// Taxonomic categories verified in Jan-2021
	private String [] upTaxo = {"archaea","bacteria","fungi", 
			"human","invertebrates", "mammals","plants",
			"rodents", "vertebrates", "viruses"};
	
	private JCheckBox spFullCheckBox = null, trFullCheckBox = null;
	private JCheckBox [] spTaxCheckBox = new JCheckBox [upTaxo.length];
	private JCheckBox [] trTaxCheckBox = new JCheckBox [upTaxo.length];
	
	// GO
	private JTextField txtGOdb = new JTextField();
	private JLabel lblGOdb;
	
	private JPanel mainPanel = null, leftPanel = null, rightPanel = null;
	
	// status
	private boolean hasGOfile=false;
	
	// map taxo to file - created in runCheck and runCheckAdd
	// (1) the fa file is output to AnnoDB_Dec2021.cfg
	// (2) the key is used for creating the full subset list.
	// (3) the key is used for the full subset popup list
	private TreeMap <String, String> spFaMap = new TreeMap <String, String> ();
	private TreeMap <String, String> trFaMap  = new TreeMap <String, String> ();
	private HashSet <String> spDatSet = new HashSet <String> ();
	private HashSet <String> trDatSet = new HashSet <String> ();
	
	private ASFrame parent;
	private DoUP upObj=null;
	private DoOBO oboObj=null;
}
