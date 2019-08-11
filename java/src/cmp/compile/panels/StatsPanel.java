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

import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

import cmp.compile.MultiStats;
import cmp.compile.Pairwise;
import cmp.database.DBinfo;
import cmp.database.Globals;

public class StatsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	public StatsPanel (CompilePanel parentPanel, EditStatsPanel ep) {
		theCompilePanel = parentPanel;
		editPanel = ep;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		
		statsPanel = Static.createPagePanel();
		
		statsPanel.add(new JLabel("4. Statistics"));
		statsPanel.add(Box.createVerticalStrut(5));
		
		JPanel row = Static.createRowPanel();
		btnRunStats = new JButton("Run Stats"); 
		btnRunStats.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runStats();
			}
		});
		row.add(btnRunStats);
		row.add(Box.createHorizontalStrut(5));
		
		btnStatsSettings = Static.createButton("Settings", true, Globals.MENUCOLOR);
		btnStatsSettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				editPanelStartup();
			}
		});
		row.add(btnStatsSettings);
		row.add(Box.createHorizontalStrut(5));
		
		lblStatsSummary = new JLabel(" ");
		row.add(lblStatsSummary);
		
		statsPanel.add(row);
		statsPanel.add(Box.createVerticalStrut(3));
		statsPanel.add(Static.getSeparator()); 
		statsPanel.add(Box.createVerticalStrut(3));
		
		add(statsPanel);
		setStatsSummary();
	}

	private void runStats() {
	
		if (!editPanel.isFunction()) return;
		
		try {
			Out.createLogFile(theCompilePanel.getCurProjAbsDir(), Globals.statsFile);
	
			DBinfo info = theCompilePanel.getDBInfo();
			Pairwise pairObj = new Pairwise(theCompilePanel);
			
			boolean doStats = 		editPanel.isStats();
			boolean doWriteKaKs = 	editPanel.isWrite();
			boolean doReadKaKs = 	editPanel.isRead();
			boolean doPCC = 			editPanel.isPCC();
			boolean doAlign = 		(doStats || doWriteKaKs);
			boolean doMulti = 		editPanel.isMulti();
			
			int nNT = info.nNTdb();
			if (nNT<=1 && doAlign) {
				UserPrompt.showWarn("Must have at least 2 NT-sTCWdbs to run these functions");
				return;
			}
					
			String msg="";
			if (doPCC && info.getCntPCC()>0) 
				msg = "PCC has been run - will be rerun\n";
			
			if (doWriteKaKs) {
				String dirKaKs = theCompilePanel.getCurProjRelDir() + Globals.KaKsDIR;
				String in = Globals.KaKsOutPrefix + "1" + Globals.KaKsOutSuffix;
				if ((new File(dirKaKs + "/" + in)).exists() ) {
					if (!UserPrompt.showContinue("KaKs files exist", 
							"Overwrite existing KaKs files for input to KaKs_calculator.\n")) {
						Out.Print("Terminate Run Stats");
						return;
					}
					else Out.Print("Overwrite existing KaKs files");
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
			
			if (doPCC) 		pairObj.computeAndSavePCC();
			
			if (doAlign) 	pairObj.saveStatsAndKaKsWrite(doWriteKaKs);
			
			if (doReadKaKs) 	pairObj.saveKaKsRead();
			
			if (doMulti) new MultiStats(theCompilePanel.getDBconn()).scoreAll();
			
			theCompilePanel.updateAll();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Running stats");}
		Out.close();
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
		btnStatsSettings.setEnabled(true);
		
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
