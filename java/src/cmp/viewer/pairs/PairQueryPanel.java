package cmp.viewer.pairs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import util.database.Globalx;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.panels.CollapsiblePanel;
import cmp.viewer.table.FieldData;

public class PairQueryPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final String PAIR_TABLE = FieldData.PAIR_TABLE;
	private static final String UNIQUE_HITS = FieldData.HIT_TABLE;
	
	private final String [] SECTIONS = { "Basic", "Hit", "Statistics", "Datasets", "Cluster Sets"};
	private final String [] SECTIONS_DESC = {"", "", "", "", ""};// display wrong as applet
	private final String helpHTML = Globals.helpDir + "PairQuery.html";
	private final int width=100;
	
	public PairQueryPanel (MTCWFrame parentFrame) {
		theViewerFrame = parentFrame;			
		hasStats = (theViewerFrame.getInfo().hasStats());
		hasNTblast = (theViewerFrame.getInfo().hasNTblast());
		cntPair = theViewerFrame.getInfo().getCntPair();
		
		createButtonPanel();
		createSections();
		
		buildPanel();
	}
	private void buildPanel() {
		setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		mainPanel = Static.createPagePanel();
		
		add(buttonPanel);
		add(Box.createVerticalStrut(5));
				
		for(int x=0; x<theSections.length; x++) {
			mainPanel.add(theSections[x]);
		}

		JScrollPane sPane = new JScrollPane(mainPanel);
		add(sPane);
	}
	private void createSections() {
		theSections = new CollapsiblePanel[SECTIONS.length];
		
		for(int x=0; x<theSections.length; x++) {
			theSections[x] = new CollapsiblePanel(SECTIONS[x], SECTIONS_DESC[x]);
			theSections[x].setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		theSections[0].add(createBasicPanel());
		theSections[1].add(createBlastPanel());
		theSections[2].add(createStatsPanel());
		theSections[3].add(createDatasetPanel());
		theSections[4].add(createClusterPanel());
	}
	private JPanel createBasicPanel() {
		JPanel page = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();	
		txtPairID = new Value("Pair #", "", "PAIRid", "=", false, "Pair number (see corresponding column)");
		row.add(txtPairID);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();	
		txtSeqID = new Substring("SeqID", PAIR_TABLE + ".UTstr1", PAIR_TABLE + ".UTstr2", 
				"All pairs with the substring in either seqID1 or seqID2", false);
		row.add(txtSeqID);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		txtHitID = new Substring("Shared HitID", PAIR_TABLE + ".HITstr", null,
				"All pairs with the substring in the identifier of shared hit", true);
		row.add(txtHitID);
		page.add(row);	
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();	
		txtDesc = new Substring("Shared Descript", UNIQUE_HITS + ".description", null, 
				"All pairs with the substring in the shared description of the pair", true);
		row.add(txtDesc);
		
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		rgPCC = new Range("PCC", "0.8", "", "pcc", 
				"All pairs with the PCC within the range, where PCC is the " +
				"Pearson Correlation Coefficient of TPM values");
		row.add(rgPCC);
		page.add(row);
		
		return page;
	}
	private JPanel createBlastPanel() {
		JPanel page = Static.createPagePanel();
		
		// Has Search Hit Yes No Either
		JPanel row = Static.createRowPanel();
		aaLabel = toolTipLabel("AA pairs", "Pairs with protein blast hit", true);
		row.add(aaLabel);
		Static.addHorzBox(row, aaLabel, width);
		aaIncButton = Static.createRadioButton("Yes",false);
		
		row.add(aaIncButton); row.add(Box.createHorizontalStrut(5));
		aaExcButton = Static.createRadioButton("No",false);
		
		row.add(aaExcButton); row.add(Box.createHorizontalStrut(5));
		aaIgnButton = Static.createRadioButton("Either",true);
		
		row.add(aaIgnButton); row.add(Box.createHorizontalStrut(5));
		ButtonGroup group = new ButtonGroup();
		group.add(aaIncButton); group.add(aaExcButton); group.add(aaIgnButton);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		aaEval = new Value("E-value", "1E-10", "aaEval", "<=", true, "Hit E-value for protein alignment");
		row.add(aaEval);					row.add(Box.createHorizontalStrut(5));
		
		aaSim = new Value("%Similarity", "50", "aaSim", ">=", false, "Hit %Similarity for protein alignment");
		row.add(aaSim);		
		page.add(row);
		page.add(Box.createVerticalStrut(15));
		
		if (hasNTblast) {
			row = Static.createRowPanel();
			ntLabel = toolTipLabel("NT pairs", "Pairs with DNA blast hit", true);
			row.add(ntLabel);
			Static.addHorzBox(row, ntLabel, width);
			ntIncButton = Static.createRadioButton("Yes",false);
			
			row.add(ntIncButton); row.add(Box.createHorizontalStrut(5));
			ntExcButton = Static.createRadioButton("No",false);
			
			row.add(ntExcButton); row.add(Box.createHorizontalStrut(5));
			ntIgnButton = Static.createRadioButton("Either",true);
			
			row.add(ntIgnButton); row.add(Box.createHorizontalStrut(5));
			ButtonGroup group1 = new ButtonGroup();
			group1.add(ntIncButton); group1.add(ntExcButton); group1.add(ntIgnButton);
			page.add(row);
			page.add(Box.createVerticalStrut(5));
			
			row = Static.createRowPanel();
			ntEval = new Value("E-value", "1E-10", "ntEval", "<=", true, "Hit E-value for DNA alignment");
			row.add(ntEval);					row.add(Box.createHorizontalStrut(5));
			
			ntSim = new Value("%Similarity", "50", "ntSim", ">=", false, "Hit %Similarity for DNA alignment");
			row.add(ntSim);		
			page.add(row);
		}
		return page;
	}
	/************************************************************/
	private JPanel createStatsPanel() {
		JPanel page = Static.createPagePanel();
		if (!theViewerFrame.getInfo().hasStats()) {
			page.add(new JLabel("No statistics available"));
			return page;
		}
		int width=100;
		
		// Has Stats Yes No Either
		JPanel row = Static.createRowPanel();
		statLabel = toolTipLabel("Has Stats", "Only pairs in clusters have statistics", true);
		row.add(statLabel);
		Static.addHorzBox(row, statLabel, width);
		statIncButton = Static.createRadioButton("Yes",false);
		row.add(statIncButton); row.add(Box.createHorizontalStrut(5));
		statExcButton = Static.createRadioButton("No",false);
		row.add(statExcButton); row.add(Box.createHorizontalStrut(5));
		statIgnButton = Static.createRadioButton("Either",true);
		row.add(statIgnButton); row.add(Box.createHorizontalStrut(5));
		ButtonGroup group = new ButtonGroup();
		group.add(statIncButton); group.add(statExcButton); group.add(statIgnButton);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
	
		int rsep=5;
		page.add(new JLabel("  Codon and Amino acid"));
		page.add(Box.createVerticalStrut(rsep));
		//
		row = Static.createRowPanel();
		rgStatCexact = new Range("%Cexact", "0.0", "", "pCmatch", "Percent of codons that are exact matches");
		row.add(rgStatCexact);
		row.add(Box.createHorizontalStrut(5));
		
		rgStatCnonsyn = new Range("%CnonSyn", "0.0", "", "pCnonsyn", "Percent of codons that are nonsynonymous (different codon, different amino acid)");
		row.add(rgStatCnonsyn);
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));
		
		//
		row = Static.createRowPanel();
		rgStatCsyn = new Range("%Csyn", "0.0", "", "pCsyn", "Percent of codons that are synonymous (different codon, same amino acid)");
		row.add(rgStatCsyn);
		row.add(Box.createHorizontalStrut(5));
		
		rgStatC4d = new Range("%4d", "0.0", "", "pC4d", "Percent of codons that are 4-fold degenerate (synonymous: four possible bases in ith position)");
		row.add(rgStatC4d);
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));
		
		row = Static.createRowPanel();
		rgStatAexact = new Range("%Aexact", "0.0", "", "pAmatch", "Percent of amino acids that are matches");
		row.add(rgStatAexact);
		row.add(Box.createHorizontalStrut(5));
		
		rgStatApos = new Range("%Apos", "0.0", "", "pAsub", "Percent of amino acid substitutions that have positive Blosum62 score, i.e. more likely substitution");
		row.add(rgStatApos);
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));
		
		row = Static.createRowPanel();
		rgStatAneg = new Range("%Aneg", "0.0", "", "pAmis", "Percent of amino acid substitutions that have negative or zero Blosum62 score, i.e. less likely substitution");
		row.add(rgStatAneg);
		row.add(Box.createHorizontalStrut(5));
		
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));
			
		page.add(new JLabel("  Nucleotide"));
		page.add(Box.createVerticalStrut(rsep));
		
		row = Static.createRowPanel();
		rgStatAlign = new Range("Align", "0", "", "nAlign", "Number of aligned bases in the CDS alignment - excludes gaps");
		row.add(rgStatAlign);
		row.add(Box.createHorizontalStrut(5));
		
		rgStatCdiff = new Range("%Cdiff", "0.0", "", "pDiffCDS", "CDS %Difference   (#bases different/#bases align) - includes gaps");
		row.add(rgStatCdiff);
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));
		
		row = Static.createRowPanel();
		rgStat5diff = new Range("%5diff", "0.0", "", "pDiffUTR5", "5'UTR %Difference (#bases different/#bases align) - includes gaps");
		row.add(rgStat5diff);
		row.add(Box.createHorizontalStrut(5));
		
		rgStat3diff = new Range("%3diff", "0.0", "", "pDiffUTR3", "3'UTR %Difference (#bases different/#bases align) - includes gaps");
		row.add(rgStat3diff);
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));
		
		row = Static.createRowPanel();
		rgStatOlap1 = new Range("%Cov1", "0.0", "", "pOlap1", "Percent coverage of alignment for the 1st CDS (Calign/CDSlen1)%"); 
		row.add(rgStatOlap1);
		row.add(Box.createHorizontalStrut(5));
		
		rgStatOlap2 = new Range("%Cov2", "0.0", "", "pOlap2", "Percent coverage of alignment for the 2nd CDS (Calign/CDSlen2)%");
		row.add(rgStatOlap2);
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));
		
		row = Static.createRowPanel();
		rgStatGap = new Range("Gap", "0", "", "nGap", "Number of gaps in the alignment");
		row.add(rgStatGap);
		row.add(Box.createHorizontalStrut(5));
		
		rgStatTsTv = new Range("ts/tv", "0.0", "", "tstv", "ts/tv of CDS, where ts=Transistion, tv=Transversion");
		row.add(rgStatTsTv);
		page.add(row);
		page.add(Box.createVerticalStrut(rsep));	
		
		
		row = Static.createRowPanel();
		rgKaKs = new Range("KaKs", "0.0", "", "kaks", "Selective strength");
		row.add(rgKaKs); row.add(Box.createHorizontalStrut(20));
		
		naCheck = Static.createCheckBox("KaKs=NA",false);
		final String desc = "Include pairs with KaKs=NA";
		naCheck.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus(desc);
			}
			public void mouseExited(MouseEvent e) {
			    theViewerFrame.setStatus("");
			}
		});
		row.add(naCheck); 
	
		if ((theViewerFrame.getInfo().hasKaKs())) {
			page.add(new JLabel("  KaKs"));
			page.add(Box.createVerticalStrut(rsep));
			page.add(row);
		}
		return page;
	}
	private JPanel createDatasetPanel() {
		JPanel page = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();
		radDiffDS = toolTipRadioButton("Different sets", "Hit pairs from two different datasets", false);
		row.add(radDiffDS); 		row.add(Box.createHorizontalStrut(5));
		
		radSameDS = toolTipRadioButton("Same sets","Hit pairs from the same dataset",false);
		row.add(radSameDS); 		row.add(Box.createHorizontalStrut(5));
		
		radIgnoreDS = Static.createRadioButton("Either",true);
		row.add(radIgnoreDS); 		row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group = new ButtonGroup();
		group.add(radDiffDS); group.add(radSameDS); group.add(radIgnoreDS);
			
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		String [] sets = theViewerFrame.getAsmList();
		nSets = sets.length;
		if (nSets==2) return page;
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(3));
		row.add(toolTipLabel("Pairs:", "Only pairs with the two datasets", true));
		row.add(Box.createHorizontalStrut(3));
		ButtonGroup grpPair = new ButtonGroup();
		int n=((nSets*(nSets-1))/2)+1;
		radPairDS = new JRadioButton [n];
		radPairDS[0] = Static.createRadioButton("Any", true);
		grpPair.add(radPairDS[0]);
		row.add(radPairDS[0]); row.add(Box.createHorizontalStrut(3));
		
		int x=0;
		for (int i=0, k=1; i<nSets-1; i++) {
			for (int j=i+1; j<nSets; j++) {
				String label = sets[i]+":"+sets[j];
				x += label.length();
				if (x>54) {
					x = label.length();
					page.add(row);
					page.add(Box.createVerticalStrut(3));
					row = Static.createRowPanel();
					row.add(Box.createHorizontalStrut(30));
				}
				radPairDS[k] = Static.createRadioButton(label, false);
				grpPair.add(radPairDS[k]);
				row.add(radPairDS[k]); row.add(Box.createHorizontalStrut(3));
				k++;
			}
		}
		page.add(row);
		return page;
	}
	private JPanel createClusterPanel() {
		JPanel page = Static.createPagePanel();
		methods = theViewerFrame.getInfo().getMethodPrefix();
		nMethods = methods.length;
		
		if (nMethods==0) {
			page.add(new JLabel("No cluster sets"));
			return page;
		}
		
		JPanel row = Static.createRowPanel();
		radAnd = Static.createRadioButton("All (&)", true);
		radOr = Static.createRadioButton("Any (|)", true);
		ButtonGroup group1 = new ButtonGroup();
		group1.add(radAnd);
		group1.add(radOr);
		if (nMethods>1) {
			row.add(radAnd); 
			row.add(Box.createHorizontalStrut(5));
			row.add(radOr); 
			page.add(row);
			page.add(Box.createVerticalStrut(5));
		}
		
		radClSetHas = new JRadioButton[nMethods];
		radClSetNot = new JRadioButton[nMethods];
		radClSetDont = new JRadioButton[nMethods];
		
		for (int i=0; i<nMethods; i++) {
			row = Static.createRowPanel();
			JLabel name = toolTipLabel(methods[i], 
					"Contain pairs from cluster set " + methods[i], true);
			row.add(name);
			row.add(Box.createHorizontalStrut(60 - name.getPreferredSize().width));
			
			radClSetHas[i] = Static.createRadioButton("In",  false);
			row.add(radClSetHas[i]);
			row.add(Box.createHorizontalStrut(5));
			
			radClSetNot[i] = Static.createRadioButton("Not in",  false);
			row.add(radClSetNot[i]);
			row.add(Box.createHorizontalStrut(5));
			
			radClSetDont[i] = Static.createRadioButton("Don't care", true);
			row.add(radClSetDont[i]);
			row.add(Box.createHorizontalStrut(5));
			
			ButtonGroup group = new ButtonGroup();
			group.add(radClSetHas[i]);
			group.add(radClSetNot[i]);
			group.add(radClSetDont[i]);
			
			page.add(row);
			page.add(Box.createVerticalStrut(5));
		}
		return page;
	}
	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();
		
		btnSearch = Static.createButtonTab("View Filtered Pairs", true);
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hasError=false;
				setSubQuery();
				if (!hasError) {
					String tab = MTCWFrame.PAIR_PREFIX + theViewerFrame.getNextLabelNum(MTCWFrame.PAIR_PREFIX);
					PairTablePanel newPanel = new PairTablePanel(theViewerFrame, tab);
					theViewerFrame.addResultPanel(MTCWFrame.PAIR_PREFIX, newPanel, newPanel.getName(), newPanel.getSummary());
				}
			}
		});
		btnSearch.setEnabled(cntPair>0);
		
		btnExpand = Static.createButton("Expand All");
		btnExpand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<theSections.length; x++)
					theSections[x].expand();
			}
		});
		
		btnCollapse = Static.createButton("Collapse All");
		btnCollapse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<theSections.length; x++)
					theSections[x].collapse();
			}
		});
		
		btnClear = Static.createButton("Clear", true);
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearFilters();
			}
		});	
		
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Pair Filter Help", helpHTML);
			}
		});
		
		row.add(btnSearch);
		row.add(Box.createHorizontalStrut(30));
		row.add(btnExpand);
		row.add(Box.createHorizontalStrut(5));
		row.add(btnCollapse);
		row.add(Box.createHorizontalStrut(5));
		row.add(btnClear);
		row.add(Box.createHorizontalStrut(30));
		
		row.add(Box.createHorizontalGlue());
		row.add(btnHelp);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE,(int)row.getPreferredSize ().getHeight()));
		
		buttonPanel.add(row);
	}
	
	private void clearFilters() {
		txtPairID.clear(); txtSeqID.clear(); txtDesc.clear(); txtHitID.clear(); rgPCC.clear();
		
		aaIgnButton.setSelected(true); aaEval.clear(); aaSim.clear();
		if (hasNTblast) {
			ntIgnButton.setSelected(true); ntEval.clear(); ntSim.clear();
		}
		
		// dataset
		radIgnoreDS.setSelected(true); 
		if (nSets>2) radPairDS[0].setSelected(true); 
		
		// cluster
		for (int i=0; i<radClSetDont.length; i++) radClSetDont[i].setSelected(true); 
		
		if (hasStats) {
			statIgnButton.setSelected(true);
			rgKaKs.clear(); 
			naCheck.setSelected(false);
			rgStatAlign.clear();
			rgStatGap.clear(); rgStatAneg.clear(); rgStatTsTv.clear(); rgStatCdiff.clear();  rgStat5diff.clear(); rgStat3diff.clear(); rgStatOlap1.clear(); rgStatOlap2.clear();
			rgStatCexact.clear(); rgStatCsyn.clear(); rgStatC4d.clear(); rgStatCnonsyn.clear(); rgStatAexact.clear(); rgStatApos.clear();
		}	
	}
	private void setSubQuery() {
		query="";
		if (cntPair==0) return;
		
		// general
		query = Static.combineBool(query, txtPairID.getSQL());
		query = Static.combineBool(query, txtSeqID.getSQL());
		query = Static.combineBool(query, txtDesc.getSQL());
		query = Static.combineBool(query, txtHitID.getSQL());
		query = Static.combineBool(query, rgPCC.getSQL());
		
		// blast
		if (aaIncButton.isSelected())      query = Static.combineBool(query, "pairwise.aaAlign>1", true);
		else if (aaExcButton.isSelected()) query = Static.combineBool(query, "pairwise.aaAlign<=0", true);
		query = Static.combineBool(query, aaEval.getSQL());
		query = Static.combineBool(query, aaSim.getSQL());
		
		if (hasNTblast) {
			if (ntIncButton.isSelected())      query = Static.combineBool(query, "pairwise.ntAlign>1", true);			
			else if (ntExcButton.isSelected()) query = Static.combineBool(query, "pairwise.ntAlign<=0", true);
			query = Static.combineBool(query, ntEval.getSQL());
			query = Static.combineBool(query, ntSim.getSQL());
		}
		
		// stats
		if (hasStats) {
			if (statIncButton.isSelected())      query = Static.combineBool(query, "pairwise.nAlign>1", true);
			else if (statExcButton.isSelected()) query = Static.combineBool(query, "pairwise.nAlign<=0", true);
			
			query = Static.combineBool(query, rgStatCexact.getSQL());
			query = Static.combineBool(query, rgStatCnonsyn.getSQL());
			query = Static.combineBool(query, rgStatCsyn.getSQL());
			query = Static.combineBool(query, rgStatC4d.getSQL());
			query = Static.combineBool(query, rgStatAexact.getSQL());
			query = Static.combineBool(query, rgStatApos.getSQL());
			query = Static.combineBool(query, rgStatAneg.getSQL());
			query = Static.combineBool(query, rgStatGap.getSQL());
			
			query = Static.combineBool(query, rgStatAlign.getSQL());
			query = Static.combineBool(query, rgStatCdiff.getSQL());
			query = Static.combineBool(query, rgStat5diff.getSQL());
			query = Static.combineBool(query, rgStat3diff.getSQL());
			query = Static.combineBool(query, rgStatTsTv.getSQL());
			query = Static.combineBool(query, rgStatOlap1.getSQL());
			query = Static.combineBool(query, rgStatOlap2.getSQL());
			
			String sub=""; // CAS327
			String kk = rgKaKs.getSQL();
			if (kk!="" && !kk.contains(">=")) kk = "(kaks>=0.0 && " + kk + ")"; // CAS343 -2.0(-) and -1.5(NA) should never show from this
			String na = "";
			if (naCheck.isSelected()) na = "pairwise.kaks=" + Globalx.dNullVal; 
			if (kk!="" && na !="") 	sub = "( " + Static.combineBool(kk, na, false) + ")"; // >=0.0 or =-1.5
			else if (kk!="") 		sub = kk;
			else if (na!="") 		sub = na;
			query = Static.combineBool(query, sub);
		}
		// datasets
		if (radDiffDS.isSelected()) query = Static.combineBool(query, "pairwise.ASMid1!=pairwise.ASMid2", true);
		if (radSameDS.isSelected()) query = Static.combineBool(query, "pairwise.ASMid1=pairwise.ASMid2", true);
		
		if (nSets>2 && !radPairDS[0].isSelected()) {
			int k=0;
			for (int i=1; i<radPairDS.length; i++)
				if (radPairDS[i].isSelected()) {
					k=i;
					break;
				}
			if (k==0) Out.die("Pairs error on setSubQuery");
			String [] pair = radPairDS[k].getText().split(":");
			int idx1 = theViewerFrame.getInfo().getAsmIdx(pair[0]);
			int idx2 = theViewerFrame.getInfo().getAsmIdx(pair[1]);
			// have to ask both ways because prefixes are not necessarily
			//   the sequence names, and pairs go in name1<name2 (not asm1<asm2)
			String tmp = 
				"((pairwise.ASMid1=" + idx1 + " and pairwise.ASMid2=" + idx2 + ") or " 
			   +" (pairwise.ASMid2=" + idx1 + " and pairwise.ASMid1=" + idx2 + "))";
			query = Static.combineBool(query, tmp, true);
		}
		// clusters
		if (radAnd!=null) { // CAS343 otherwise, no clusters
			boolean isAnd = (radAnd.isSelected());
			String subquery = "";
			for (int i=0; i<nMethods; i++) {
				if (radClSetHas[i].isSelected()) 
					subquery = Static.combineBool(subquery, "pairwise." + methods[i] + " is not null", isAnd);
				else if (radClSetNot[i].isSelected()) 
					subquery = Static.combineBool(subquery, "pairwise." + methods[i] + " is null", isAnd);
			}
			if (subquery!="") {
				subquery = "(" + subquery + ")";
				query = Static.combineBool(query, subquery);
			}
			if (query==null || query.equals("")) {
				if (theViewerFrame.getInfo().getCntPair()>100000) {
					if (UserPrompt.showContinue("Slow query", 
							"There is more than 100,000 pairs.\nThis will be slow.")) {
						summary="All sequences";
					}
					else hasError=true;
				}
			}
		}
		if (hasError) query=" 1 ";
		else {
			if (query=="") query = " 1 ";
			setSummary();
		}
	}
	
	private void setSummary() {
		summary = "";
		if (cntPair==0) return;
		
		summary = Static.combineSummary(summary, txtPairID.getSum(""));
		summary = Static.combineSummary(summary, txtSeqID.getSum());
		summary = Static.combineSummary(summary, txtHitID.getSum());
		summary = Static.combineSummary(summary, txtDesc.getSum());
		summary = Static.combineSummary(summary, rgPCC.getSum());
					
		if (aaIncButton.isSelected()) summary = Static.combineSummary(summary, "AA pairs");
		else if (aaExcButton.isSelected()) summary = Static.combineSummary(summary, "No AA pairs");
		summary = Static.combineSummary(summary, aaEval.getSum("AA-"));
		summary = Static.combineSummary(summary, aaSim.getSum("AA-"));
		
		if (hasNTblast) {
			if (ntIncButton.isSelected()) summary = Static.combineSummary(summary, "NT pairs");
			else if (ntExcButton.isSelected()) summary = Static.combineSummary(summary, "No NT pairs");
			summary = Static.combineSummary(summary, ntEval.getSum("NT-"));
			summary = Static.combineSummary(summary, ntSim.getSum("NT-"));	
		}
		if (hasStats) {
			if (statIncButton.isSelected()) summary = Static.combineSummary(summary, "Has Stats");
			else if (statExcButton.isSelected()) summary = Static.combineSummary(summary, "No Stats");
			
			summary = Static.combineSummary(summary, rgStatCexact.getSum());
			summary = Static.combineSummary(summary, rgStatCnonsyn.getSum());
			summary = Static.combineSummary(summary, rgStatCsyn.getSum());
			summary = Static.combineSummary(summary, rgStatC4d.getSum());
			summary = Static.combineSummary(summary, rgStatAexact.getSum());
			summary = Static.combineSummary(summary, rgStatApos.getSum());
			summary = Static.combineSummary(summary, rgStatAneg.getSum());
			
			summary = Static.combineSummary(summary, rgStatAlign.getSum());
			summary = Static.combineSummary(summary, rgStatCdiff.getSum());
			summary = Static.combineSummary(summary, rgStat5diff.getSum());
			summary = Static.combineSummary(summary, rgStat3diff.getSum());
			summary = Static.combineSummary(summary, rgStatTsTv.getSum());
			summary = Static.combineSummary(summary, rgStatGap.getSum());
		
			summary = Static.combineSummary(summary, rgStatOlap1.getSum());
			summary = Static.combineSummary(summary, rgStatOlap2.getSum());
		
			summary = Static.combineSummary(summary, rgKaKs.getSum());
			if (naCheck.isSelected()) summary = Static.combineSummary(summary, "KaKs=NA");
		}
		// datasets
		if (radDiffDS.isSelected()) summary = Static.combineSummary(summary, "Different sets");
		if (radSameDS.isSelected()) summary = Static.combineSummary(summary, "Same set");
		
		if (nSets>2 && !radPairDS[0].isSelected()) {
			int k=0;
			for (int i=1; i<radPairDS.length; i++) 
				if (radPairDS[i].isSelected()) {
					k=i;
					break;
				}
			if (k>0) {
				String pair = radPairDS[k].getText();
				summary = Static.combineSummary(summary, pair);
			}
		}	
		
		// Methods
		if (radAnd!=null) { // CAS343 pairs&clusters added, clusters removed, this crashes
			String op = (radAnd.isSelected()) ? "&" : "|";
			String in="", out="";
			for (int i=0; i<nMethods; i++) {
				if (radClSetHas[i].isSelected()) {
					if (in=="") in = methods[i];
					else in += op + methods[i];
				}
				else if (radClSetNot[i].isSelected()) {
					if (out=="") out = methods[i];
					else out += op + methods[i];
				}
			}
			if (!in.equals("") || !out.equals("")) {
				if (!in.equals("")) in = "In(" + in + ")";
				if (!out.equals("")) out = "Not(" + out + ")";
				String tmp = Static.combineBool(in, out, radAnd.isSelected());
				summary = Static.combineBool(summary, tmp);
			}
		}
		if (summary==null || summary.equals("")) summary="Show All";
	}
	public String getQuerySummary() { return summary;}
	public String getSubQuery() { return query;}
	/************************************************************************/
	private class Range extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final int colLen = 5;
		
		public Range(String l, String min, String max, String field, String descript) {
			label = l; defMin=min; defMax=max; sqlField="pairwise."+ field;
			if (defMin.contains(".")) isInt=false;
			
			setLayout(new BoxLayout ( this, BoxLayout.LINE_AXIS ));
			super.setBackground(Color.WHITE);
			
			checkOn = Static.createCheckBox(label,false);
			checkOn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean b = checkOn.isSelected();	
					txtMin.setEnabled(b);
					txtMax.setEnabled(b);
				}
			});
			txtMin = Static.createTextField(defMin, colLen, false);
			txtMax = Static.createTextField(defMax, colLen, false);
			
			final String desc = descript;
			checkOn.addMouseListener(new MouseAdapter() 
			{
				public void mouseEntered(MouseEvent e) {
				    theViewerFrame.setStatus(desc);
				}
				public void mouseExited(MouseEvent e) {
				    theViewerFrame.setStatus("");
				}
			});
			add(Box.createHorizontalStrut(15));
			add(checkOn);
			if (width > checkOn.getPreferredSize().width) 
				add(Box.createHorizontalStrut(width-checkOn.getPreferredSize().width));
			add(new JLabel(">="));
			add(txtMin);
			add(Box.createHorizontalStrut(5));
			add(new JLabel("<"));
			add(txtMax);
		}
		public String getSQL() {
			checkValues();
			if (!checkOn.isSelected()) return "";
			
			String min = txtMin.getText().trim(); // check if proper value
			String max = txtMax.getText().trim();
			
			if (min.contentEquals("") && max.contentEquals("")) {
				checkOn.setSelected(false);
				return "";
			}
			if (!min.contentEquals("") && max.contentEquals(""))  // CAS330 rewrite
				return sqlField + ">=" + min;
			
			if (min.contentEquals("") && !max.contentEquals(""))  // CAS330 rewrite
				return sqlField + "<" + max;
			
			if (min.contentEquals(max))	// CAS340 (was specifically checking for min=0 and max=0)
				return sqlField + "=" + max;
			
			return "(" + sqlField + ">=" + min + " and " + sqlField + "<" + max + ")";
		}
		// CAS310 had weird test that would not display >=0.0, even though query would work
		public String getSum() {
			if (!checkOn.isSelected()) return "";
			
			String min = txtMin.getText(); 
			String max = txtMax.getText();
			
			if (min.equals("")) return label + "<"  + max;
			if (max.equals("")) return label + ">=" + min;
			
			if (min.contentEquals("0")  && max.contentEquals("0"))  return  label + "=0";
			if (min.contentEquals("0.0")&& max.contentEquals("0.0"))return  label + "=0.0";
			
			return "(" + label + ">=" + min + " and " + label + "<" + max + ")";
		}
		private void checkValues() {
			if (!checkOn.isSelected()) return;
			String min = txtMin.getText(); // check if proper value
			String max = txtMax.getText();
			if (min.equals(defMin) && max.equals(defMax)) return;
			if (min.equals("") && max.equals("") ) {
				checkOn.setSelected(false);
				return;
			}
			if (isInt) {
				if (min.contentEquals("0.0")) txtMin.setText("0");
				if (max.contentEquals("0.0")) txtMax.setText("0");
			}
			else {
				if (min.contentEquals("0")) txtMin.setText("0.0");
				if (max.contentEquals("0")) txtMax.setText("0.0");
			}
			if (!min.equals("") && !Static.isDouble(min) && !Static.isInteger(min)) {
				UserPrompt.showWarn("Incorrect minimum  '" + min + "' for " + label );
				hasError=true;
			}
			if (!max.equals("") && !Static.isDouble(max) && !Static.isInteger(max)) {
				UserPrompt.showWarn("Incorrect maximum  '" + max + "' for " + label );
				hasError=true;
			}
		}
		public void clear() {
			txtMax.setText("");
			if (isInt) txtMin.setText("0");
			else  txtMin.setText("0.0");
			
			checkOn.setSelected(false);
			txtMin.setEnabled(false);
			txtMax.setEnabled(false);
		}
		private JCheckBox checkOn = null;
		private JTextField txtMin = null, txtMax=null;
		private String defMin, defMax, sqlField, label;
		private boolean isInt=true;
		private int width=90;
	}
	/************************************************************************/
	private class Value extends JPanel {
		private static final long serialVersionUID = 1L;
		
		public Value(String lab, String val, String sqlfield, String operator, boolean noVal,
				String descript) {
			label = lab; defVal=val; sqlField="pairwise."+ sqlfield; op=operator;
			bNoVal =  noVal;
			
			setLayout(new BoxLayout ( this, BoxLayout.LINE_AXIS ));
			super.setBackground(Color.WHITE);
			
			String x = label + " " + op; 
			checkOn = Static.createCheckBox(x,false);
			checkOn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean b = checkOn.isSelected();	
					txtVal.setEnabled(b);
				}
			});
			final String desc = descript;
			checkOn.addMouseListener(new MouseAdapter() 
			{
				public void mouseEntered(MouseEvent e) {
				    theViewerFrame.setStatus(desc);
				}
				public void mouseExited(MouseEvent e) {
				    theViewerFrame.setStatus("");
				}
			});
			txtVal = Static.createTextField("", 5, false);
			
			add(Box.createHorizontalStrut(15));
			add(checkOn);
			if (width > checkOn.getPreferredSize().width) 
				add(Box.createHorizontalStrut(width-checkOn.getPreferredSize().width));
			add(txtVal);
		}
		public String getSQL() {
			checkValue();
			if (!checkOn.isSelected()) return "";
			String val = txtVal.getText().trim(); 
			if (val.equals("")) return "";
			if (bNoVal) return "(" + sqlField + "!=" + Globalx.dNoVal 
					+ " and " + sqlField + op + val + ")";
			return "(" + sqlField + op + val + ")";
		}
		public String getSum(String prefix) {
			if (!checkOn.isSelected()) return "";
			String val = txtVal.getText().trim(); 
			if (val.equals("")) return "";
			return prefix + label + op + val;
		}
		private void checkValue() {
			if (!checkOn.isSelected()) return;
			String min = txtVal.getText().trim();
			if (min.equals(defVal)) return;
			if (min.equals("")) {
				checkOn.setSelected(false);
				return;
			}
			if (!min.equals("") && !Static.isDouble(min) && !Static.isInteger(min)) {
				UserPrompt.showWarn("Incorrect minimum  '" + min + "' for " + label );
				hasError=true;
			}	
		}
		
		public void clear() {
			txtVal.setText(defVal);
			checkOn.setSelected(false);
			txtVal.setEnabled(false);
		}
		private JCheckBox checkOn = null;
		private JTextField txtVal;
		private String defVal, sqlField, label, op;
		private boolean bNoVal;
		private int width=75;
	}
	/************************************************************************/
	private class Substring extends JPanel {
		private static final long serialVersionUID = 1L;
		public Substring(String lab, String field, String field2, String descript, boolean yesNo) {
			label = lab;  
			sqlField= field;
			sqlField2 = field2;
			
			setLayout(new BoxLayout ( this, BoxLayout.LINE_AXIS ));
			super.setBackground(Color.WHITE);
			
			checkOn = Static.createCheckBox(label + " (substring)",false);
			checkOn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean b = checkOn.isSelected();	
					txtVal.setEnabled(b);
				}
			});
			final String desc = descript;
			checkOn.addMouseListener(new MouseAdapter() 
			{
				public void mouseEntered(MouseEvent e) {theViewerFrame.setStatus(desc);}
				public void mouseExited(MouseEvent e)  {theViewerFrame.setStatus("");}
			});
			txtVal = Static.createTextField("", 15, false);
			add(Box.createHorizontalStrut(15));
			add(checkOn);
			if (width > checkOn.getPreferredSize().width) 
				add(Box.createHorizontalStrut(width-checkOn.getPreferredSize().width));
			add(txtVal);
			
			if (yesNo) {
				yesButton = Static.createRadioButton("Has",true);
				add(yesButton); add(Box.createHorizontalStrut(5));
				noButton =  Static.createRadioButton("Not",false);
				add(noButton); 
				ButtonGroup group1 = new ButtonGroup();
				group1.add(yesButton); group1.add(noButton);
			}
		}
		public String getSQL() {
			if (!checkOn.isSelected()) return "";
			String val = txtVal.getText().trim(); // check if proper value
			if (sqlField2!=null && !sqlField2.equals("")) {
				return "(" + sqlField +  " LIKE '%" + val + "%' or " 
			               + sqlField2 + " LIKE '%" + val + "%')";
			}
			if (noButton!=null && noButton.isSelected())
				return "(" + sqlField + " NOT LIKE '%" + val + "%')";
			return "(" + sqlField + " LIKE '%" + val + "%')";
		}
		public String getSum() {
			if (!checkOn.isSelected()) return "";
			String val = txtVal.getText().trim(); // check if proper value
			
			if (noButton!=null && noButton.isSelected())
				return label + " not contains '" + val + "'";
			
			return label + " contains '" + val +"'";
		}
		public void set(boolean b) {
			if (checkOn.isSelected() && !b) checkOn.setSelected(false);
			txtVal.setEnabled(b);
		}
		public void clear() {
			txtVal.setText("");
			set(false);
		}
		private JCheckBox checkOn = null;
		private JTextField txtVal;
		private String sqlField, sqlField2="", label;
		private int width=210;
		private JRadioButton yesButton= null, noButton= null;
	}
	private JRadioButton toolTipRadioButton(String label, String descript, boolean enable) {
		JRadioButton radio = new JRadioButton(label);
		radio.setBackground(Color.white);
		radio.setSelected(enable);
		
		final String desc = descript;
		radio.addMouseListener(new MouseAdapter() 
		{
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus(desc);
			}
			public void mouseExited(MouseEvent e) {
			    theViewerFrame.setStatus("");
			}
		});
		return radio;
	}
	private JLabel toolTipLabel(String label, String descript, boolean enable) {
		JLabel tmp = new JLabel(label);
		tmp.setBackground(Color.white);
		tmp.setEnabled(enable);
		final String desc = descript;
		tmp.addMouseListener(new MouseAdapter() 
		{
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus(desc);
			}
			public void mouseExited(MouseEvent e) {
			    theViewerFrame.setStatus("");
			}
		});
		return tmp;
	}
	/************************************************************/
	//Top button panel
	private JPanel buttonPanel = null;
	private JButton btnSearch = null, btnClear = null, btnHelp = null;
	private JButton btnExpand = null, btnCollapse = null;
	
	//Main panel
	private JPanel mainPanel = null;
	private CollapsiblePanel [] theSections = null;
	
	// basic
	private Substring txtSeqID = null, txtHitID = null, txtDesc = null;
	private Value txtPairID = null;
	private Range rgPCC= null;
	
	// blast
	private JLabel aaLabel;
	private JRadioButton aaIncButton, aaExcButton, aaIgnButton;
	private Value aaEval, aaSim;
	
	private JLabel ntLabel;
	private JRadioButton ntIncButton= null, ntExcButton= null, ntIgnButton= null;
	private Value ntEval= null, ntSim= null;
	
	private JLabel statLabel;
	private JRadioButton statIncButton= null, statExcButton= null, statIgnButton= null;
	private Range rgStatGap, rgStatTsTv, rgStatAlign, rgStatCdiff, rgStat5diff, rgStat3diff, rgStatOlap1, rgStatOlap2;
	private Range rgStatCexact, rgStatCsyn, rgStatC4d, rgStatCnonsyn, rgStatAexact, rgStatApos, rgStatAneg;
	
	private Range rgKaKs; // CAS310 had unused KaKs 
	private JCheckBox naCheck = null; // CAS330 change from Yes/No/Either to checkbox
	
	// dataset
	private JRadioButton radDiffDS, radSameDS, radIgnoreDS;
	private JRadioButton [] radPairDS = null;
	
	// Has Method
	private int nMethods=0;
	private String [] methods;
	private JRadioButton [] radClSetHas, radClSetNot, radClSetDont;
	private JRadioButton radAnd, radOr;
	
	private MTCWFrame theViewerFrame = null;
	private String summary="", query="";
	private boolean hasError=false;
	private int nSets=2, cntPair=0;
	private boolean hasStats=false, hasNTblast=false;
}
