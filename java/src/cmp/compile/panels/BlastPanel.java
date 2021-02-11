package cmp.compile.panels;

/**************************************************************
 * This defines the Compare Sequence panel for the Main window
 * It also has the action for RunSearch and executes it.
 * EditBlastPanel defines the Setting panel. 
 */
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cmp.compile.DoBlast;
import cmp.compile.Pairwise;
import cmp.compile.Summary;
import cmp.database.DBinfo;
import cmp.database.Globals;

import util.database.DBConn;
import util.file.FileHelpers;
import util.methods.Static;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.UserPrompt;

public class BlastPanel extends JPanel {
	private static final long serialVersionUID = 312139563542324851L;
	public static final int AA=Globals.AA;
	public static final int NT=Globals.NT;
	private static final String [] tpStr = {"AA", "NT"};
	
	public BlastPanel(CompilePanel parentPanel, EditBlastPanel ep) {
		theCompilePanel = parentPanel;
		editPanel = ep;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		
		blastPanel = Static.createPagePanel();
		
		blastPanel.add(new JLabel("2. Compare sequences"));
		blastPanel.add(Box.createVerticalStrut(5));
		
		JPanel row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(10));
		lblBlastSummary = new JLabel(" ");
		row.add(lblBlastSummary);
		blastPanel.add(row);
		blastPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		btnRunBlast = new JButton("Run Search");
		btnRunBlast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runBlast();
			}
		});
		row.add(btnRunBlast);
		row.add(Box.createHorizontalStrut(10));
		
		JLabel lblCPUs = new JLabel("#CPUs");
		txtCPUs = new JTextField(3);
		row.add(lblCPUs);
		row.add(Box.createHorizontalStrut(5));
		
		int cores = Runtime.getRuntime().availableProcessors(); 
		txtCPUs.setText(cores+"");
		txtCPUs.setMaximumSize(txtCPUs.getPreferredSize());
		txtCPUs.setMinimumSize(txtCPUs.getPreferredSize());
		row.add(txtCPUs);
		row.add(Box.createHorizontalStrut(10));
		
		btnBlastSettings = Static.createButton("Settings", true, Globals.MENUCOLOR);
		btnBlastSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				editPanelStartup();
			}
		});
		row.add(btnBlastSettings);
		blastPanel.add(row);
		
		// pairs
		row = Static.createRowPanel();
		btnAddPairs = Static.createButton("Add Pairs from Hits", false);
		btnAddPairs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				createPairsFromBlast();
			}
		});
		row.add(btnAddPairs);
		
		row.add(Box.createHorizontalStrut(5));
		lblPairsSummary = new JLabel(" ");
		row.add(lblPairsSummary);
		
		blastPanel.add(row);
		
		add(Box.createVerticalStrut(10));
		add(blastPanel);
		
		setBlastSummary();
	}
	/*******************************************************
	 * Add blast pairs
	 */
	private void createPairsFromBlast() {
		Out.createLogFile(theCompilePanel.getCurProjAbsDir(), Globals.searchFile);
	
		if (theCompilePanel.getDBInfo().getCntPair()>0) {
			UserPrompt.showMsg("Pairs exist already");
			return;
		}
		try {
			Out.PrtDateMsg("Create pairs from search result files");
			long time = Out.getTime();
			
			Pairwise pw = new Pairwise(theCompilePanel);
			if (editPanel.isBlast(AA)) {
				String blastFile = editPanel.getBlastFileToProcess(AA);
				pw.savePairsFromHitFile(blastFile, true);
			}
			else Out.PrtSpMsg(2, "No AA (protein) results file to load");
			
			if (editPanel.isBlast(NT)) {
				String blastFile = editPanel.getBlastFileToProcess(NT);
				pw.savePairsFromHitFile(blastFile, false);
			}
			else Out.PrtSpMsg(2, "No NT (nucleotide) results file to load");
				
			Out.PrtMsgTimeMem("Complete creating pairs", time);
			theCompilePanel.updateAll(); // updates DBinfo, then calls the blast updatedDBexist
		}
		catch (Exception e) {ErrorReport.prtReport(e, "adding pairs");}
		Out.close();
	}
	
	private void runBlast() { 
		Out.createLogFile(theCompilePanel.getCurProjAbsDir(), Globals.searchFile);
	
		String blastDir = theCompilePanel.getCurProjAbsDir() +  "/" + Globals.Search.BLASTDIR; 
		FileHelpers.createDir(blastDir);
		String [] sql = new String [2];
		sql[0]=sql[1]=null; // actual program run
		
		for (int tp=0; tp<2; tp++) {
			if (!editPanel.isBlast(tp)) continue;
			
			if (!editPanel.isBlastExists(tp)) {
				String file = editPanel.getBlastFile(tp);
				String params = editPanel.getBlastParams(tp);
				String pgm = editPanel.getSearchPgm(tp);
				
				if (tp==AA) sql[tp] = DoBlast.runBlastp(theCompilePanel, pgm, file, params);
				else        sql[tp] = DoBlast.runBlastn(theCompilePanel, file, params); 
				if (sql[tp]!=null) editPanel.updateBlast(tp);
			}
		}
		try {
			DBConn mDB = theCompilePanel.getDBconn();
			
			new Summary(mDB).removeSummary();
			mDB.executeUpdate("update info set aaInfo='', ntInfo=''");
			
			if (sql[0]!=null) 
				mDB.executeUpdate("update info set aaPgm='" + sql[0] + "'");
			if (sql[1]!=null) 
				mDB.executeUpdate("update info set ntPgm='" + sql[1] + "'");
			
			mDB.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Entering blast info to database");}
		setBlastSummary();
		Out.close();
	}
	private void editPanelStartup() {
		editPanel.updatePanel();
		
		theCompilePanel.setMainPanelVisible(false);
		editPanel.setVisible(true);
	}
	
	
	public void setBlastSummary() {
		btnRunBlast.setEnabled(false);
		btnAddPairs.setEnabled(false); 
		btnBlastSettings.setEnabled(true); 
		txtCPUs.setEnabled(true);
		
		if(theCompilePanel.getProjectName() == null) {
			lblBlastSummary.setText("No project selected");
			lblPairsSummary.setText("");
			return;
		}
		boolean bDB = theCompilePanel.dbIsExist();
		if (!bDB) {
			lblBlastSummary.setText("Database does not exist");
			lblPairsSummary.setText("");
			return;
		}
		
		// only hit this code if database exists so we know where there is an AAdb
		txtCPUs.setEnabled(true);
		DBinfo dbinfo = theCompilePanel.getDBInfo();
		boolean bNoNT = dbinfo.nNTdb()==0;
		String [] msg = {"", ""};
		boolean doSearch=false;
		
		for (int tp=0; tp<2; tp++) {
			if (!editPanel.isBlast(tp)) continue;
			if (tp==1 && bNoNT) continue;
			
			if (editPanel.isBlastExists(tp)) msg[tp] = "Use existing " + tpStr[tp] + " file";
			else  {
				String pgm = editPanel.getSearchPgm(tp);
				if (pgm.startsWith("TCW")) pgm = "generate";
				msg[tp] = pgm + " " + tpStr[tp] + " file";
				doSearch=true;
			}
		}
		
		if (bNoNT)  {
			 lblBlastSummary.setText(msg[0]);
			 msg[1] = "";
		}
		else lblBlastSummary.setText(msg[0] + "; " + msg[1]); 
			
		btnRunBlast.setEnabled(doSearch);
		
		if (doSearch) {
			lblPairsSummary.setText("Run search before add pairs");
			btnAddPairs.setEnabled(false); 
		}
		else {
			boolean hasPairs = (dbinfo.getCntPair()>0) ? true : false;
			
			if (!hasPairs) lblPairsSummary.setText("Pairs need to be added");
			else lblPairsSummary.setText("Pairs exist in database");
			btnAddPairs.setEnabled(!hasPairs);
			// CAS316 - btnBlastSettings.setEnabled(!hasPairs); 
			txtCPUs.setEnabled(!hasPairs);
		}
	}
	
	/****  Gets ********/
	public CompilePanel getParentPanel() { return theCompilePanel; }
	
	// Used by orthoMCL (BBH & Closure read pairs from database)
	public String getBlastFileForMethods(int type) { // 0 AA, 1 NT
		try {
			String blastFile;
			blastFile = editPanel.getBlastFileToProcess(type);
			if ( new File(blastFile).exists()) return blastFile;
			
			String msg = (type==0) ? "AA" : "NT";
			String msg2 = msg + " search file '" + blastFile + "' does not exist.\n";
			msg2 += "  Remove pairs (use Remove button at top)\n";
			msg2 += "  Open search settings and select Use " + msg +"...\n";
			msg2 += "  Then select Run Search\n";
			UserPrompt.showMsg(msg2);
			Out.PrtWarn(msg2);
			return null;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error getting search file");}
		return null;
	}
	
	public int getCPUs() {
		try {
			String n = txtCPUs.getText();
			nCPUs = Integer.parseInt(n);
			return nCPUs;
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this,  
					"#CPUs must be an integer", "Error", JOptionPane.PLAIN_MESSAGE);
			return 0;
		}
	}
	
	// build the full path to default blast location so we can tell if the user-selected
	// path starts with this or not.
	public String getDefaultBlastDirFullPath()
	{
		String sdir = getDefaultBlastDirRelPath();
		File dir = new File(sdir);
		return dir.getAbsolutePath();
	}
	public String getDefaultBlastDirRelPath()
	{
		String projDir = theCompilePanel.getCurProjRelDir();
		if (!projDir.endsWith("/")) projDir += "/"; 
		return  projDir + Globals.Search.BLASTDIR;
	}	
	/***************************************************/
	public void update(boolean dbExists) {
		setBlastSummary();
		
		lblBlastSummary.setEnabled(true);
		lblPairsSummary.setEnabled(true);
		
		if (!dbExists) { // otherwise setBlastSummary sets based on state
			lblPairsSummary.setText("");
			btnRunBlast.setEnabled(false);
			btnAddPairs.setEnabled(false);
			btnBlastSettings.setEnabled(true);
			txtCPUs.setEnabled(true);
		}
	}
	
	public void updateClearInterface() {
		editPanel.clearInterface();
		
		lblBlastSummary.setText("No project selected");
		lblPairsSummary.setText("");
		
		btnBlastSettings.setEnabled(true);
		btnRunBlast.setEnabled(false);
		btnAddPairs.setEnabled(false);
	}
	public void removedBlastDir() {
		editPanel.resetFile();
		setBlastSummary();
	}
	
	public EditBlastPanel getEditPanel() { return editPanel;}
	private JButton btnBlastSettings = null;
	private JLabel lblBlastSummary = null;
	private JLabel lblPairsSummary = null;
	
	private JButton btnRunBlast = null;
	private JTextField txtCPUs = null;
	private JButton btnAddPairs = null;

	public EditBlastPanel editPanel = null;
	private JPanel blastPanel = null;
	private CompilePanel theCompilePanel = null;
	
	private int nCPUs=1;
}
