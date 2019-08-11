package cmp.compile.panels;
/*********************************************
 * Settings for Run Stats
 *
 * DP alignment  o  [method] or o Blast Similarity [ ] Overlap [ ]
 * 
 * x  Compute stats
 * x  Write KaKs: Maximum #gaps[ ]  x Split output
 * 
 * x Read KaKs results files
 * 
 * x PCC on RPKM
 */
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import util.methods.Static;
import util.ui.UserPrompt;
import cmp.database.DBinfo;
import cmp.database.Globals;

public class EditStatsPanel  extends JPanel  {
	private static final long serialVersionUID = 1L;
	
	public EditStatsPanel(CompilePanel parentPanel) {
		theCompilePanel = parentPanel;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		setAlignmentX(Component.CENTER_ALIGNMENT);
		
		add(Box.createVerticalStrut(5));
		JPanel row;
		
		// PCC
		row = Static.createRowPanel(); row.add(Box.createHorizontalStrut(5));
		chkPCC = Static.createCheckBox("PCC of RPKM for all hit pairs", true);
		row.add(chkPCC);
		add(row);
		add(Box.createVerticalStrut(15));
			
		// Alignment and stats
		row = Static.createRowPanel(); row.add(Box.createHorizontalStrut(5));
		row.add(Static.createLabel("Alignment of hit pairs in clusters:"));
		add(row); add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel(); row.add(Box.createHorizontalStrut(20));
		chkStats = Static.createCheckBox("Compute pair stats", true);
		row.add(chkStats);
		add(row); add(Box.createVerticalStrut(3));
		
		row = Static.createRowPanel(); row.add(Box.createHorizontalStrut(20));
		chkKaKs = Static.createCheckBox("KaKs files",true); 
		chkKaKs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				boolean b = chkKaKs.isSelected();
				kaksRdButton.setEnabled(b); 
				kaksWrButton.setEnabled(b); 
			}
		});
		row.add(chkKaKs); row.add(Box.createHorizontalStrut(5));
		
		kaksWrButton = Static.createRadioButton("Write",false); 
		row.add(kaksWrButton); row.add(Box.createHorizontalStrut(5));
		
		kaksRdButton = Static.createRadioButton("Read",false); 
		row.add(kaksRdButton); row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group = new ButtonGroup();
		group.add(kaksWrButton); group.add(kaksRdButton); 
		
		add(row);  add(Box.createVerticalStrut(25));
		
		row = Static.createRowPanel(); row.add(Box.createHorizontalStrut(5));
		row.add(Static.createLabel("Multiple alignment of clusters:"));
		add(row); add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel(); row.add(Box.createHorizontalStrut(20));
		chkMulti = Static.createCheckBox("Compute cluster scores", true);
		row.add(chkMulti);
		add(row);
		add(Box.createVerticalStrut(25));
		
		// Lower buttons
		btnKeep = Static.createButton("Keep", true,Globals.BGCOLOR);
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				keep();
			}
		});
		
		btnDiscard = Static.createButton("Cancel", true,Globals.BGCOLOR);
		btnDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cancel();
				
			}
		});
		
		btnHelp = Static.createButton("Help", true,Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theCompilePanel.getParentFrame(), "Edit Stats Settings", "html/runMultiTCW/EditStatsPanel.html");
			}
		});
	
		buttonPanel = Static.createRowPanel();
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnKeep);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnDiscard);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnHelp);
		buttonPanel.add(Box.createHorizontalGlue());
		add(buttonPanel);
		add(Box.createVerticalStrut(15));
		
		JPanel page = Static.createPagePanel();
		for (int i=0; i<lblSum.length; i++) {
			lblSum[i] = Static.createLabel(" ");
			page.add(lblSum[i]); page.add(Box.createVerticalStrut(3));
		}
		add(page);
		
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	private void keep() {
		bPCC = chkPCC.isSelected();
		bStats = chkStats.isSelected();
		bWrite = kaksWrButton.isSelected();
		bRead = kaksRdButton.isSelected();
		bKaKs = chkKaKs.isSelected();
		bMulti = chkMulti.isSelected();
		
		setVisible(false);
		theCompilePanel.getStatsPanel().setStatsSummary();
		theCompilePanel.setMainPanelVisible(true);
	}
	private void cancel() {
		chkPCC.setSelected(bPCC);
		chkStats.setSelected(bStats);
		chkKaKs.setSelected(bKaKs);
		kaksWrButton.setSelected(bWrite);
		kaksRdButton.setSelected(bRead);
		chkMulti.setSelected(bMulti);
		if (!bMulti) {
			kaksWrButton.setEnabled(false);
			kaksRdButton.setEnabled(false);
		}
		
		theCompilePanel.getStatsPanel().setStatsSummary();
		theCompilePanel.setMainPanelVisible(true);
		setVisible(false);
	}
	/**************** Return parameters ********************/
	public boolean isFunction() {
		return isStats() || isKaKs()|| isPCC() || isMulti();
	}
	public String getSummary() {
		String msg="";
		if (isFunction()) {
			if (chkPCC.isSelected())   msg = Static.combineSummary(msg, "PCC");
			if (chkStats.isSelected()) msg = Static.combineSummary(msg, "Stats");
			if (chkKaKs.isSelected()) {
				if (kaksWrButton.isSelected())       msg = Static.combineSummary(msg, "Write KaKs");
				else if (kaksRdButton.isSelected())  msg = Static.combineSummary(msg, "Read KaKs");
			}
			if (chkMulti.isSelected())   msg = Static.combineSummary(msg, "Multi");
			return msg;
		}
		DBinfo info = theCompilePanel.getDBInfo();
		if (info.getCntPair()==0) return "No pairs in database";
		if (info.getCntGrp() == 0 && info.isNTonly()) return "No clusters in database"; 
		return "No action selected";
	}
	public boolean isPCC()   { return chkPCC.isSelected(); }
	public boolean isStats() { return chkStats.isSelected(); }
	public boolean isKaKs()  { return chkKaKs.isSelected() && 
						(kaksRdButton.isSelected() || kaksWrButton.isSelected());}
	public boolean isRead()  { return  (chkKaKs.isSelected() && kaksRdButton.isSelected());}
	public boolean isWrite()  { return (chkKaKs.isSelected() && kaksWrButton.isSelected());}
	public boolean isMulti() { return chkMulti.isSelected();}
	
	/******************* Standard interface ********************/
	public void updateDBexists() {
		updateDBnone(); // everything setSelected(false) setEnabled(false)
		
		DBinfo info = theCompilePanel.getDBInfo();
		int cntPairs = info.getCntPair();
		
		// PCC
		int index=0;
		lblSum[index++].setText(html(cntPairs, "Total Hit Pairs", false));
		
		int cntPCC = info.getCntPCC();
		int cntLib = info.getSeqLib().length;
		if (cntLib==0) { 			// PCC for RPKM
			lblSum[index++].setText(html(-1, "No expression levels", true));
		}
		else { 						
			if (cntPairs>0 && cntPCC==0) chkPCC.setSelected(true);
			chkPCC.setEnabled(true);
			lblSum[index++].setText(html(cntPCC, "Pairs with PCC", true));
		}
		bPCC = chkPCC.isSelected();
		
		// Clusters -- want to score regardless of NT-sTCW or AA-sTCW
		int cntGrp =   info.getCntGrp();
		int cntMulti = info.getCntMultiScore();
		lblSum[index++].setText(html(cntGrp,   "Clusters", false));
		lblSum[index++].setText(html(cntMulti, "with scores", true));
		
		if (cntGrp>0) {
			chkMulti.setEnabled(true);
			if (cntGrp>cntMulti) chkMulti.setSelected(true);
		}
		bMulti= chkMulti.isSelected();
		
		// Pairs		
		int nNT = info.nNTdb(); 
		if (nNT<=1) {
			if (nNT==0) lblSum[index++].setText(html(-1, "No  NT-sTCWdb", false));
			else        lblSum[index++].setText(html(-1, "One NT-sTCWdb", false));
			bStats=false; bKaKs=false; bRead=false; bWrite=false;
			return;
		}
		int cntPairGrp = info.getCntPairGrp(); // Pairwise.hasGrp=1
		lblSum[index++].setText(html(cntPairGrp, "Pairs in clusters", false));
		if (cntPairGrp==0) 
			return;
		
	// Pairs
		 
		// Stats
		int cntStats = info.getCntStats();    // Paiwise.align>0 
		lblSum[index++].setText(html(cntStats, "with stats", true));
		
		chkStats.setEnabled(true);
		if (cntStats<cntPairGrp) chkStats.setSelected(true); 
		bStats = chkStats.isSelected();
		
		// KaKs
		int cntKaKs = info.getCntKaKs();     // Pairwise.KaKs>=0 read from file
		lblSum[index++].setText(html(cntKaKs, "with KaKs ", true));
		
		chkKaKs.setEnabled(true);
		kaksWrButton.setEnabled(true); 
		kaksRdButton.setEnabled(true); 
		
		// Read/Write files exists?
		boolean wExists=false, rExists=false;
		String kaksDir = theCompilePanel.getStatsPanel().getKaKsDir();
		String file = kaksDir + "/" + Globals.KaKsOutPrefix + "1" + Globals.KaKsOutSuffix;
		File f = new File(file);
		wExists = f.exists();
		
		if (cntStats<cntPairGrp) { // KaKs need to be written for new pairs
			chkKaKs.setSelected(true);
			kaksWrButton.setSelected(true);
			lblSum[index++].setText(html(-1,     "&nbsp;&nbsp;Write files for KaKs input", true));
			if (wExists)
				lblSum[index++].setText(html(-1, "&nbsp;&nbsp;***Input to KaKs exists", true));
		}	
		else if (cntKaKs<cntPairGrp) { // cntStats all aligned already
			if (wExists) {
				file = kaksDir + "/" + Globals.KaKsInPrefix + "1" + Globals.KaKsInSuffix;
				f = new File(file);
				rExists = f.exists();
			}
			if (rExists) {
				chkKaKs.setSelected(true);
				kaksRdButton.setSelected(true); kaksWrButton.setSelected(false);
				lblSum[index++].setText(html(-1, "KaKs files ready for Read", true));
			}
			else if (wExists) {
				chkKaKs.setSelected(false); 
				kaksWrButton.setEnabled(false); kaksWrButton.setSelected(false);
				kaksRdButton.setEnabled(false); kaksRdButton.setSelected(false);
				lblSum[index++].setText(html(-1, "Files ready for KaKs input", true));
			}
			else lblSum[index++].setText(html(-1, "No KaKs files", true));
		}
		
		bKaKs = chkKaKs.isSelected();
		bWrite = kaksWrButton.isSelected();
		bRead = kaksRdButton.isSelected();
	}
	
	private String html(int n, String msg, boolean indent) {
		String x = "<html><tt><small>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
		if (indent) x += "&nbsp;&nbsp;&nbsp;&nbsp;";
		if (n==-1) return x + msg +"</tt></small><html>";
		return x + msg + ": " + n + "</tt></small><html>";
	}
	public void updateDBnone() {
		chkStats.setSelected(false);
		chkPCC.setSelected(false);
		chkMulti.setSelected(false); 
		chkKaKs.setSelected(false);
		
		chkStats.setEnabled(false);
		chkPCC.setEnabled(false);
		chkMulti.setEnabled(false);
		chkKaKs.setEnabled(false);
		
		kaksWrButton.setEnabled(false);
		kaksRdButton.setEnabled(false);
		
		for (int i=0; i<lblSum.length; i++) lblSum[i].setText("");
	}
	
	/****************************************************/
	private JButton btnKeep = null, btnDiscard = null,  btnHelp = null;
	
	private JRadioButton kaksWrButton, kaksRdButton;
	private JCheckBox chkKaKs = null;
	private JCheckBox	chkStats = null, chkPCC = null, chkMulti = null;
	
	private boolean bStats=false, bWrite=false, bRead=false, bPCC=false, bKaKs=false, bMulti=false;
	
	private JLabel [] lblSum = new JLabel [10];
	private JPanel buttonPanel = null;
	private CompilePanel theCompilePanel = null;
}
