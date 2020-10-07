package cmp.viewer.seq;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

import util.ui.UserPrompt;
import util.methods.Out;
import util.methods.Static;

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.panels.CollapsiblePanel;

public class SeqsQueryPanel extends JPanel {
	private static final long serialVersionUID = 3740105753786330459L;
	public static final String tag = MTCWFrame.SEQ_PREFIX;
	public static final String helpHTML = "SeqQuery.html";
	
	private static final String [] SECTIONS = { "Basic", "Datasets", "Cluster Sets", "TPM" };
	private static final String [] SECTIONS_DESC = 	{ "","", "","" }; // description doesn't look good on applet

	public SeqsQueryPanel(MTCWFrame parentFrame) {
		theViewerFrame = parentFrame;
		hasRPKM = theViewerFrame.getInfo().hasRPKM();
		nSec = (hasRPKM) ? SECTIONS.length : SECTIONS.length-1; 
	
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
		
		
		for(int x=0; x<nSec; x++) {
			mainPanel.add(theSections[x]);
		}

		JScrollPane sPane = new JScrollPane(mainPanel);
		add(sPane);
	}
	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();
		btnSearch = Static.createButton("View Filtered Sequences", true, Globals.FUNCTIONCOLOR);
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hasError=false;
				seqSQLwhere();
				if (!hasError) {
					SeqsTopRowPanel newPanel = new SeqsTopRowPanel(theViewerFrame, 
						tag + theViewerFrame.getNextLabelNum(tag));
					theViewerFrame.addResultPanel(MTCWFrame.SEQ_PREFIX, newPanel, newPanel.getName(), getQuerySummary());
				}
			}
		});
		
		btnExpand = new JButton("Expand All");
		btnExpand.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		btnExpand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<nSec; x++)
					theSections[x].expand();
			}
		});
		
		btnCollapse = new JButton("Collapse All");
		btnCollapse.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		btnCollapse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<nSec; x++)
					theSections[x].collapse();
			}
		});
		
		btnClear = Static.createButton("Clear", true);
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearFilters();
			}
		});
		
		btnHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, 
						"Sequence Filter Help", "html/viewMultiTCW/SeqQuery.html");
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
		
		buttonPanel.add(row);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
	}
	private JPanel createBasicPanel() {
		JPanel page = Static.createPagePanel();
	
		JPanel row = Static.createRowPanel();
		txtSeqID = new Substring("Seq ID", "Seq ID", 	"unitrans.UTstr", 
				"Show all sequences with the substring in their seqIDs");
		row.add(txtSeqID);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		txtHitID = new Substring("Best Hit ID", "Hit ID", 	"unitrans.HITstr", 
				"Show all sequences with the substring in their best HitID");
		row.add(txtHitID);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		txtDesc = new Substring("Description", "Descript", 	"unique_hits.description", 
				"Show all sequences with the substring in their best description");
		row.add(txtDesc);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		// Diff set
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(15));
		JLabel lhit = toolTipLabel("Hits to different set",
				"Has hit to one or more sequences in a different dataset", true);
		row.add(lhit);
		row.add(Box.createHorizontalStrut(5));
		
		radYesAA = Static.createRadioButton("Yes", false);
		row.add(radYesAA);
		row.add(Box.createHorizontalStrut(5));
		
		radNoAA = Static.createRadioButton("No", false);
		row.add(radNoAA);
		row.add(Box.createHorizontalStrut(5));
		
		radDcAA = Static.createRadioButton("Either", true);
		row.add(radDcAA);
		row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group = new ButtonGroup();
		group.add(radYesAA); group.add(radNoAA); group.add(radDcAA);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		// Anno
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(15));
		JLabel lanno = toolTipLabel("Has Annotation",
				"Sequences that are annotated with at least one hit", true);
		row.add(lanno);
		int width = lhit.getPreferredSize().width - lanno.getPreferredSize().width;
		row.add(Box.createHorizontalStrut(width+5));
		
		radYesAnno = Static.createRadioButton("Yes", false);
		row.add(radYesAnno);
		row.add(Box.createHorizontalStrut(5));
		
		radNoAnno = Static.createRadioButton("No", false);
		row.add(radNoAnno);
		row.add(Box.createHorizontalStrut(5));
		
		radDcAnno = Static.createRadioButton("Either", true);
		row.add(radDcAnno);
		row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group2 = new ButtonGroup();
		group2.add(radYesAnno); group2.add(radNoAnno); group2.add(radDcAnno);
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		// GO
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(15));
		JLabel lgo = toolTipLabel("Has GO",
				"Sequences that have at least one GO (Gene Ontology)", true);
		row.add(lgo);
		width = lhit.getPreferredSize().width - lgo.getPreferredSize().width;
		row.add(Box.createHorizontalStrut(width+5));
		
		radYesGO = Static.createRadioButton("Yes", false);
		row.add(radYesGO);
		row.add(Box.createHorizontalStrut(5));
		
		radNoGO = Static.createRadioButton("No", false);
		row.add(radNoGO);
		row.add(Box.createHorizontalStrut(5));
		
		radDcGO = Static.createRadioButton("Either", true);
		row.add(radDcGO);
		row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group3 = new ButtonGroup();
		group3.add(radYesGO); group3.add(radNoGO); group3.add(radDcGO);
		if (theViewerFrame.getInfo().getCntGO()>0) page.add(row); 
		
		return page;
	}
	private JPanel createDatasetPanel() {
		JPanel page = Static.createPagePanel();
		String [] sets = theViewerFrame.getAsmList();
		int n = sets.length;
		
		JPanel row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(3));
		row.add(toolTipLabel("Sets:", "Only sequences from selected dataset", true));
		row.add(Box.createHorizontalStrut(3));
		
		ButtonGroup grpPair = new ButtonGroup();
		radDS = new JRadioButton [n+1];
		radDS[0] = Static.createRadioButton("Any", true);
		grpPair.add(radDS[0]);
		row.add(radDS[0]); row.add(Box.createHorizontalStrut(3));
		
		for (int i=0, k=1; i<n; i++, k++) {
			radDS[k] = Static.createRadioButton(sets[i], false);
			row.add(radDS[k]); row.add(Box.createHorizontalStrut(3));
			grpPair.add(radDS[k]);
		}
		
		page.add(row);
		return page;
	}
	private JPanel createClusterPanel() {
		JPanel page = Static.createPagePanel();
		methods = theViewerFrame.getInfo().getMethodPrefix();
		nMethods = methods.length;
		
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
		
		radInGrp = new JRadioButton[nMethods];
		radNotGrp = new JRadioButton[nMethods];
		radDcGrp = new JRadioButton[nMethods];
		
		for (int i=0; i<nMethods; i++) {
			row = Static.createRowPanel();
			JLabel name = toolTipLabel(methods[i],
					"Sequences that are in a cluster from set " + methods[i], true);
			row.add(name);
			row.add(Box.createHorizontalStrut(60 - name.getPreferredSize().width));
			
			radInGrp[i] = Static.createRadioButton("In", false);
			row.add(radInGrp[i]);
			row.add(Box.createHorizontalStrut(5));
			
			radNotGrp[i] = Static.createRadioButton("Not in", false);
			row.add(radNotGrp[i]);
			row.add(Box.createHorizontalStrut(5));
			
			radDcGrp[i] = Static.createRadioButton("Don't care", true);
			row.add(radDcGrp[i]);
			row.add(Box.createHorizontalStrut(5));
			
			ButtonGroup group = new ButtonGroup();
			group.add(radInGrp[i]); group.add(radNotGrp[i]); group.add(radDcGrp[i]);
			
			page.add(row);
			page.add(Box.createVerticalStrut(5));
		}
		return page;
	}
	private JPanel createRPKMpanel() {
		JPanel page = Static.createPagePanel();
		JPanel row = Static.createRowPanel();
		
		radAndPKM = Static.createRadioButton("And", true);
		row.add(radAndPKM);
		row.add(Box.createHorizontalStrut(5));
		radOrPKM = Static.createRadioButton("Or", false);
		row.add(radOrPKM);
		ButtonGroup group = new ButtonGroup();
		group.add(radAndPKM); group.add(radOrPKM);
		page.add(row);
		page.add(Box.createVerticalStrut(3));
		
		String [] libs = theViewerFrame.getSeqLibList();
		rgPKM = new Range [libs.length];
		for (int i=0; i<libs.length; i++) {
			row = Static.createRowPanel();
			rgPKM[i] = new Range(libs[i], "0", "", Globals.PRE_LIB + libs[i],
					"Upper and/or lower limit of the RPKM of " + libs[i]);
			row.add(rgPKM[i]);
			page.add(row);
			page.add(Box.createVerticalStrut(3));
		}
		return page;
	}
	private void createSections() {
		theSections = new CollapsiblePanel[nSec];
		
		for(int x=0; x<nSec; x++) {
			theSections[x] = new CollapsiblePanel(SECTIONS[x], SECTIONS_DESC[x]);
			theSections[x].setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		theSections[0].add(createBasicPanel());
		theSections[1].add(createDatasetPanel());
		theSections[2].add(createClusterPanel());
		JPanel rpkm = createRPKMpanel();
		if (hasRPKM) theSections[3].add(rpkm);
	}
	private void clearFilters() {
		radDcAnno.setSelected(true);
		radDcAA.setSelected(true);
		radDcGO.setSelected(true);
		txtSeqID.clear(); txtHitID.clear(); txtDesc.clear();
		for (int i=0; i<nMethods; i++) radDcGrp[i].setSelected(true);
		radAndPKM.setSelected(true);
		for (int i=0; i<rgPKM.length; i++) rgPKM[i].clear();
	}
	
	/*****************************************
	 * SeqTablePanel.buildQueryStr adds join on unitrans.HITid=unique_hits.HITid
	 */
	private void seqSQLwhere() {
		sqlWhere = "";
		
		// Basic
		sqlWhere = Static.combineBool(sqlWhere, txtSeqID.getSQL());
		sqlWhere = Static.combineBool(sqlWhere, txtHitID.getSQL());
		sqlWhere = Static.combineBool(sqlWhere, txtDesc.getSQL());
		
		if (radYesAA.isSelected()) {
			sqlWhere = Static.combineBool(sqlWhere, "unitrans.nPairs>0");
		}
		else if (radNoAA.isSelected()) {
			sqlWhere = Static.combineBool(sqlWhere, "unitrans.nPairs=0");
		}
		
		if (radYesAnno.isSelected()) {
			sqlWhere = Static.combineBool(sqlWhere, "unitrans.hitID>0");
		}
		else if (radNoAnno.isSelected()) {
			sqlWhere = Static.combineBool(sqlWhere, "unitrans.hitID=0");
		}
		
		if (radYesGO.isSelected()) {
			sqlWhere = Static.combineBool(sqlWhere, "unique_hits.nGO>0");
		}
		else if (radNoGO.isSelected()) {
			sqlWhere = Static.combineBool(sqlWhere, "unique_hits.nGO=0");
		}
		// Dataset
		if (!radDS[0].isSelected()) {
			int k=0;
			for (int i=1; i<radDS.length; i++)
				if (radDS[i].isSelected()) {
					k=i;
					break;
				}
			if (k==0) Out.die("Sequence error on setSubQuery");
			
			String seqID = radDS[k].getText();
			int idx = theViewerFrame.getInfo().getAsmIdx(seqID);
			
			String tmp = "unitrans.ASMid=" + idx;
			sqlWhere = Static.combineBool(sqlWhere, tmp, true);
		}
		// Method
		boolean isAnd = (radAnd.isSelected());
		String subquery="";
		for (int i=0; i<nMethods; i++) {
			if (radInGrp[i].isSelected()) 
				subquery = Static.combineBool(subquery, "unitrans." + methods[i] + " is not null", isAnd);
			else if (radNotGrp[i].isSelected()) 
				subquery = Static.combineBool(subquery, "unitrans." + methods[i] + " is null", isAnd);
		}
		if (subquery!="") {
			subquery = "(" + subquery + ")";
			sqlWhere = Static.combineBool(sqlWhere, subquery);
		}
		// PKM
		String tmp="";
		isAnd = (radAndPKM.isSelected()) ? true : false;
		for (int i=0; i<rgPKM.length; i++) {
			tmp = Static.combineBool(tmp, rgPKM[i].getSQL(), isAnd);
		}
		if (!tmp.equals(""))
			sqlWhere = Static.combineBool(sqlWhere, "(" + tmp + ")");	
		
		if (sqlWhere==null || sqlWhere.equals("")) {
			if (theViewerFrame.getInfo().getCntSeq()>100000) {
				if (UserPrompt.showContinue("Slow query", 
						"There is more than 100,000 sequences.\nThis will be slow.")) {
					summary="All sequences";
				}
				else hasError=true;
			}
			sqlWhere = " 1 ";
		}
		if (hasError) sqlWhere="";
		else setSummary();
	}
	public String getSQLwhere() { return sqlWhere;}
	public String getQuerySummary() { return summary;}
	private void setSummary() {
		summary = "";
		
		//Basic
		summary = Static.combineSummary(summary, txtSeqID.getSum());
		summary = Static.combineSummary(summary, txtHitID.getSum());
		summary = Static.combineSummary(summary, txtDesc.getSum());
		
		if (radYesAA.isSelected()) {
			summary = Static.combineBool(summary, "Has Hit Pairs to different set");
		}
		else if (radNoAA.isSelected()) {
			summary = Static.combineBool(summary, "No Hit Pairs to different set");
		}
		
		if (radYesAnno.isSelected()) {
			summary = Static.combineBool(summary, "Has Annotation");
		}
		else if (radNoAnno.isSelected()) {
			summary = Static.combineBool(summary, "No Annotation");
		}
		
		if (radYesGO.isSelected()) {
			summary = Static.combineBool(summary, "Has GO");
		}
		else if (radNoGO.isSelected()) {
			summary = Static.combineBool(summary, "No GO");
		}
		// Datasets
		if (!radDS[0].isSelected()) {
			for (int i=1; i<radDS.length; i++)
				if (radDS[i].isSelected()) {
					summary = Static.combineBool(summary, "Dataset " + radDS[i].getText());
					break;
				}
		}
		// Methods
		String in="", out="";
		String op = (radAnd.isSelected()) ? "&" : "|";
		for (int i=0; i<nMethods; i++) {
			if (radInGrp[i].isSelected()) {
				if (in=="") in = methods[i];
				else in += op + methods[i];
			}
			else if (radNotGrp[i].isSelected()) {
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
		
		// PKM
		String tmp="";
		boolean isAnd = (radAndPKM.isSelected()) ? true : false;
		for (int i=0; i<rgPKM.length; i++) {
			tmp = Static.combineBool(tmp, rgPKM[i].getSum(), isAnd);
		}
		if (!tmp.equals("")) 
			summary = Static.combineBool(summary, "(" + tmp + ")");
		
		if (summary==null || summary.length()==0) summary = "All sequences";
	}
	/************************************************************************/
	private class Substring extends JPanel {
		private static final long serialVersionUID = 1L;
		public Substring(String lab, String sum, String field, String descript) {
			label = lab; 
			sumLabel=sum; 
			sqlField= field;
			
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
				public void mouseExited(MouseEvent e) {theViewerFrame.setStatus("");}
			});
			txtVal = Static.createTextField("", 15, false);
			
			add(Box.createHorizontalStrut(15));
			add(checkOn);
			if (width > checkOn.getPreferredSize().width) 
				add(Box.createHorizontalStrut(width-checkOn.getPreferredSize().width));
			add(txtVal);
		}
		public String getSQL() {
			if (!checkOn.isSelected()) return "";
			String val = txtVal.getText().trim(); 
			if (val.equals("")) return "";
			return "(" + sqlField + " LIKE '%" + val + "%')";
		}
		public String getSum() {
			if (!checkOn.isSelected()) return "";
			String val = txtVal.getText().trim(); 
			if (val.equals("")) return "";
			return sumLabel + " contains '" + val + "'";
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
		private String sumLabel, sqlField, label;
		private int width=200;
	}
	/************************************************************************/
	private class Range extends JPanel {
		private static final long serialVersionUID = 1L;
		public Range(String l, String min, String max, String field, String descript) {
			label = l; defMin=min; defMax=max; sqlField=field;
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
			txtMin = Static.createTextField(defMin, 4, false);
			txtMax = Static.createTextField(defMax, 4, false);
			
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
			if ((min.equals("") || min.equals("0")) && max.equals("")) {
				checkOn.setSelected(false);
				return "";
			}
			
			if (min.equals("")) return sqlField + "<"  + max;
			if (max.equals("")) return sqlField + ">=" + min;
			return "(" + sqlField + ">=" + min + " and " + sqlField + "<" + max + ")";
		}
		public String getSum() {
			if (!checkOn.isSelected()) return "";
			String min = txtMin.getText().trim(); // check if proper value
			String max = txtMax.getText().trim();
			boolean nomin = (min.equals("") || min.equals("0") || min.equals("0.0"));
			if (nomin && max.equals("")) return "";
			if (nomin) return label + "<"  + max;
			if (max.equals("")) return label + ">=" + min;
			return "(" + label + ">=" + min + " and " + label + "<" + max + ")";
		}
		private void checkValues() {
			if (!checkOn.isSelected()) return;
			String min = txtMin.getText().trim(); // check if proper value
			String max = txtMax.getText().trim();
			if (min.equals(defMin) && max.equals(defMax)) return;
			if (min.equals("") && max.equals("") ) {
				checkOn.setSelected(false);
				return;
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
	private MTCWFrame theViewerFrame = null;
	
	//Top button panel
	private JPanel buttonPanel = null;
	private JButton btnSearch = null, btnClear = null, btnHelp = null;
	private JButton btnExpand = null, btnCollapse = null;
	
	private JPanel mainPanel = null;
	private CollapsiblePanel [] theSections = null;
	
	// General
	private Substring txtSeqID = null, txtHitID = null, txtDesc = null;
	private JRadioButton radYesAA, radNoAA, radDcAA;
	private JRadioButton radYesAnno, radNoAnno, radDcAnno;
	private JRadioButton radYesGO, radNoGO, radDcGO;
	
	// RPKM
	private JRadioButton radAndPKM, radOrPKM;
	private Range [] rgPKM = null;
	
	// Datasets
	private JRadioButton [] radDS = null;
	
	// Clusters methods
	private int nMethods=0;
	private String [] methods;
	private JRadioButton [] radInGrp, radNotGrp, radDcGrp;
	private JRadioButton radAnd, radOr;
	
	private String summary="", sqlWhere="";
	private boolean hasError=false, hasRPKM=false;
	private int nSec=0;
}
