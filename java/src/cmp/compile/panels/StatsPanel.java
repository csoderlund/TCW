package cmp.compile.panels;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import util.database.DBConn;
import util.database.Globalx;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

import cmp.compile.MultiStats;
import cmp.compile.Pairwise;
import cmp.compile.Summary;
import cmp.compile.runMTCWMain;
import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.database.Schema;

public class StatsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	public StatsPanel (CompilePanel parentPanel, EditStatsPanel ep) {
		theCompilePanel = parentPanel;
		editPanel = ep;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		
		statsPanel = Static.createPagePanel();
		
		statsPanel.add(Static.createLabel("4. Statistics"));
		statsPanel.add(Box.createVerticalStrut(5));
		
		JPanel row = Static.createRowPanel();
		btnRunStats = Static.createButtonRun("Run Stats", false); 
		btnRunStats.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runStats();
			}
		});
		row.add(btnRunStats);
		row.add(Box.createHorizontalStrut(5));
		
		btnStatsSettings = Static.createButtonPanel("Settings", false); // CAS310 can't see until exists
		btnStatsSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				editPanelStartup();
			}
		});
		row.add(btnStatsSettings);
		row.add(Box.createHorizontalStrut(5));
		
		lblStatsSummary = Static.createLabel(" ");
		row.add(lblStatsSummary);
		
		statsPanel.add(row);
		statsPanel.add(Box.createVerticalStrut(3));
		statsPanel.add(Static.getSeparator()); 
		statsPanel.add(Box.createVerticalStrut(3));
		
		add(statsPanel);
		setStatsSummary();
	}

	public void runStats() {
		if (!editPanel.isFunction()) return;
		
		try {
			long time = Out.getTime();
			Out.createLogFile(theCompilePanel.getCurProjAbsDir(), Globals.statsFile);
	
			DBinfo info = theCompilePanel.getDBInfo();
			info.clearCntKeys();
			
			Pairwise pairObj = new Pairwise(theCompilePanel);
			
			boolean doStats = 		editPanel.isStats();
			boolean doWriteKaKs = 	editPanel.isWrite();
			boolean doReadKaKs = 	editPanel.isRead();
			boolean doPCC = 		editPanel.isPCC();
			boolean doAlign = 		(doStats || doWriteKaKs);
			boolean doMulti = 		editPanel.isMulti();
			
			int nNT = info.nNTdb();
			if (nNT<=1 && doAlign) {
				UserPrompt.showWarn("Must have at least 2 NT-sTCWdbs to run these functions");
				return;
			}
					
			String msg="";
			if (doPCC && info.hasPCC()) 
				msg = "PCC has been run - will be rerun\n";
			
			if (doWriteKaKs) {
				String dirKaKs = theCompilePanel.getCurProjRelDir() + Globals.KaKsDIR;
				String in = Globals.KaKsOutPrefix + "1" + Globals.KaKsOutSuffix;
				if ((new File(dirKaKs + "/" + in)).exists() ) {
					if (!UserPrompt.showContinue("KaKs files exist", 
							"Replace existing KaKs files for input to KaKs_calculator.\n")) {
						Out.Print("Terminate Run Stats");
						return;
					}
					else {
						Out.Print("Clear directory " + dirKaKs);
						FileHelpers.clearDir(dirKaKs);
					}
				}
			}
			if (doReadKaKs) {
				String dirKaKs = theCompilePanel.getCurProjRelDir() + Globals.KaKsDIR;
				String in = Globals.KaKsInPrefix + "1" + Globals.KaKsInSuffix;
				if (!(new File(dirKaKs + "/" + in)).exists() ) 
					msg += "KaKs input files do not appear to exist, e.g. " + in + "\n";
			}
			
			if (!msg.equals(""))
				if (!UserPrompt.showContinue("Run Stats", msg+"\n")) {
					Out.Print("Terminate Run Stats");
					return;
				}
			
			if (doPCC) 		{
				pairObj.computeAndSavePCC();
				Out.Print("");
			}
			if (doAlign) 	{
				pairObj.saveStatsAndKaKsWrite(doWriteKaKs);
			}
			if (doReadKaKs) 	{
				pairObj.loadKaKsRead();
				Out.Print("");
			}
			if (doMulti) {
				boolean bRedo=false, bSkip=false; // CAS312
				if (editPanel.isDoneMulti()) {
					if (UserPrompt.showYesNo("Run Multi Stats", "Multi-align is complete. Redo scores?\n")) {
						if (runMTCWMain.bRmMSA) {
							if (UserPrompt.showYesNo("Run Multi Stats", "Remove MSAs first?\n")) rmMSA();
							else bSkip=true;
						}
						else {
							bRedo=true;
						}
					} 
					else bSkip=true;
				}
				
				if (!bSkip) new MultiStats(theCompilePanel).scoreAll(bRedo);
				Out.Print("");
			}
			
			Out.PrtSpMsg(0,"Finishing...");
			
			DBConn mDB = theCompilePanel.getDBconn(); // CAS310
			info.updateCntKeys(mDB);
			
			Schema.updateVersion(mDB);
			new Summary(mDB).removeSummary(); // CAS310 
			mDB.close();
			
			theCompilePanel.updateAll();
			Out.PrtSpMsgTime(0, "All done", time);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Running stats");}
		Out.close();
	}
	private void rmMSA() {
		try {
			DBConn mDB = theCompilePanel.getDBconn(); // CAS312
			mDB.executeUpdate("update pog_groups set conLen=0, sdLen=0, "
					+ "score1=" + Globalx.dNoScore +  ", score2=" + Globalx.dNoVal);
			mDB.executeUpdate("update pog_members set alignMap=''");
			mDB.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Remove MSA");}
	}
	public void setStatsSummary() {
		if(theCompilePanel.getProjectName() == null) {
			lblStatsSummary.setText("");
			return;
		}
		else if (!theCompilePanel.dbIsExist()) {
			lblStatsSummary.setText("");
			return;
		}
		String msg=editPanel.getSummary();
		lblStatsSummary.setText(msg);
		if (editPanel.isFunction()) btnRunStats.setEnabled(true);
		else btnRunStats.setEnabled(false);
	}

	private void editPanelStartup() {
		theCompilePanel.setMainPanelVisible(false);
		editPanel.updateDBexists(); // CAS304
		editPanel.setVisible(true);
	}
	public EditStatsPanel getEditPanel() { return editPanel;}
	
	/***************************************************
	 * PCC if only has Pairs
	 * Stats if has Groups
	 * Summary if has BBH
	 */
	public void update(boolean dbExists) {
		if (dbExists) {
			DBinfo info = theCompilePanel.getDBInfo();
			boolean hasPairs = (info!=null && info.getCntPair()>0) ? true : false;
			btnRunStats.setEnabled(hasPairs);
			editPanel.updateDBexists();	
		}
		else {
			editPanel.updateDBnone();
		}
		btnStatsSettings.setEnabled(dbExists); // CAS310 queries database
		setStatsSummary();
	}
	
	public void updateClearInterface() {
		btnRunStats.setEnabled(false);
		btnStatsSettings.setEnabled(false);
		lblStatsSummary.setText("");
	}
	public String getKaKsDir() {
		String dir = theCompilePanel.getCurProjRelDir() + Globals.KaKsDIR;
		File temp = new File(dir);
		if (!temp.exists()) {
			temp.mkdir();
		}
		return dir;
	}
	private JButton btnStatsSettings = null;
	private JLabel lblStatsSummary = null;
	
	private JButton btnRunStats = null;
	
	public EditStatsPanel editPanel = null;
	private JPanel statsPanel = null;
	private CompilePanel theCompilePanel = null;
	
}
