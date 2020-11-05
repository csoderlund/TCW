package cmp.viewer.groups;

/********************************************
 * The Filters for Clusters
 */
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
import util.methods.Static;

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.panels.CollapsiblePanel;
import cmp.viewer.table.FieldData;

public class GrpQueryPanel extends JPanel {
	private static final long serialVersionUID = 1672776836742705318L;
	private static final String UNIQUE_HITS = FieldData.HIT_TABLE;
	private static final String GRP_TABLE = FieldData.GRP_TABLE;
	private static final String helpHTML = "GrpQuery.html";
	public static final String ALLRadio = "All";
	
	private static final String [] SECTIONS = { "Basic", "Dataset", "Cluster Sets"};
	private static final String [] SECTIONS_DESC = 	{ "", "", ""};
	public GrpQueryPanel(MTCWFrame parentFrame) {
		theViewerFrame = parentFrame;
		cntGrp = theViewerFrame.getInfo().getCntGrp();
		
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
	
	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();
		
		btnSearch = Static.createButton("View Filtered Clusters", true, Globals.FUNCTIONCOLOR);
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hasError=false;
				setSubQuery();
				if (!hasError) {
					tabPrefix = MTCWFrame.GRP_PREFIX;
					if (!radMethods[0].isSelected()) {
						for (int i=1; i<=nMethods; i++) {
							if (radMethods[i].isSelected()) {
								tabPrefix = radMethods[i].getText();
								break;
							}
						}
					}
					
					String tab = tabPrefix + theViewerFrame.getNextLabelNum(tabPrefix);
					GrpTablePanel grpPanel = new GrpTablePanel(theViewerFrame, tab);
					theViewerFrame.addResultPanel(MTCWFrame.GRP_PREFIX, grpPanel, grpPanel.getName(), grpPanel.getSummary());
				}
			}
		});
		btnSearch.setEnabled(cntGrp>0);
		
		btnExpand = new JButton("Expand All");
		btnExpand.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		btnExpand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<theSections.length; x++)
					theSections[x].expand();
			}
		});
		
		btnCollapse = new JButton("Collapse All");
		btnCollapse.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		btnCollapse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<theSections.length; x++)
					theSections[x].collapse();
			}
		});
		
		btnClear = Static.createButton("Clear", true);
		btnClear.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearFilters();
			}
		});	
		
		btnHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Cluster Filter Help", 
						"html/viewMultiTCW/" + helpHTML);
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
	private void createSections() {
		int num = SECTIONS.length;
		theSections = new CollapsiblePanel[num];
		
		for(int x=0; x<num; x++) {
			theSections[x] = new CollapsiblePanel(SECTIONS[x], SECTIONS_DESC[x]);
			theSections[x].setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		theSections[0].add(createBasicPanel());
		theSections[1].add(createDataSetPanel());
		theSections[2].add(createGrpPanel()); 
	}
	private JPanel createBasicPanel() {
		JPanel page = Static.createPagePanel();
		
		JPanel row = Static.createRowPanel();	
		txtGrpID = new Substring("Cluster ID", GRP_TABLE + ".PGstr", 
				"All clusters with the substring in their Cluster ID (see corresponding column)", false);
		row.add(txtGrpID);
		page.add(row);	
		page.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		txtHitID = new Substring("Majority Hit ID", GRP_TABLE + ".HITstr", 
				"All clusters with the substring in the identifier of majority hit", true);
		row.add(txtHitID);
		page.add(row);	
		page.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		txtDesc = new Substring("Majority Descript", UNIQUE_HITS + ".description", 
				"All clusters with the substring in the description of majority hit", true);
		row.add(txtDesc);
		page.add(row);	
		page.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		rgPerAnno = new Range("%Hit", "100", "", GRP_TABLE + ".perAnno", 
				"Percent of the sequences in the cluster with the majority hit");
		row.add(rgPerAnno);
		page.add(row);
		return page;
	}
	
	private JPanel createGrpPanel() {
		JPanel page = Static.createPagePanel();
		String [] methodNames = theViewerFrame.getInfo().getMethodPrefix();;
		nMethods = methodNames.length;
		if (nMethods==0) {
			page.add(new JLabel("No cluster sets"));
			return page;
		}
		
		JPanel row = Static.createRowPanel();		
		row.add(toolTipLabel("Taxa:", "Composition of clusters (see Help)", true));
		String [] taxa = theViewerFrame.getTaxaList();
		int nTax = taxa.length + 1;
		
		ButtonGroup group1 = new ButtonGroup();
		radTaxa = new JRadioButton [nTax];
		radTaxa[0] = Static.createRadioButton(ALLRadio, true);
		group1.add(radTaxa[0]);
		for(int x=1; x<=taxa.length; x++) {
			radTaxa[x] = Static.createRadioButton(taxa[x-1], true);
			group1.add(radTaxa[x]);	
		}
		
		// N datasets, N*2 taxa
		int nItem=9;
		for(int x=0, i=1; x<=taxa.length; x++, i++) {
			row.add(radTaxa[x]); row.add(Box.createHorizontalStrut(3));
			if (i==nItem && x<taxa.length) {
				page.add(row); 
				page.add(Box.createVerticalStrut(3));
				
				row = Static.createRowPanel();
				row.add(Box.createHorizontalStrut(34));
				i=1;
				nItem=8; 
			}
		}
		page.add(row);
		page.add(Box.createVerticalStrut(15));
		
		// All methods
		row =  Static.createRowPanel();		
		row.add(toolTipLabel("Sets:", "Show all clusters or only those from the selected set.", true));
		
		ButtonGroup group2 = new ButtonGroup();
		radMethods = new JRadioButton [nMethods+1];
		radMethods[0] = Static.createRadioButton(ALLRadio, true);
		group2.add(radMethods[0]);
		
		for(int x=1; x<=nMethods; x++) {
			radMethods[x] = Static.createRadioButton(methodNames[x-1], true);
			group2.add(radMethods[x]);
		}
		
		nItem=8;
		if (nMethods>nItem) {
			for (int i=2; i<20; i++) 
				if (nMethods <= (nItem*i)) {
					nItem = (nMethods/i)+1;
					break;
				}
		}
		for(int x=0, i=1; x<=nMethods; x++, i++) {
			row.add(radMethods[x]); 
			row.add(Box.createHorizontalStrut(3));
			if (i==nItem && x<nMethods) {
				page.add(row); 
				page.add(Box.createVerticalStrut(3));
				
				row = Static.createRowPanel();
				row.add(Box.createHorizontalStrut(33));
				i=0;
			}
		}
		page.add(row);
		page.add(Box.createVerticalStrut(10));
		
		page.add(row);
				
		return page;
	}
	
	private JPanel createDataSetPanel() {
		JPanel retVal = Static.createPagePanel();

		theDataSetPanel = new DatasetSubPanel(theViewerFrame); 
		retVal.add(theDataSetPanel);
		return retVal;
	}
	
	private void clearFilters() {
		txtGrpID.clear();
		txtHitID.clear();
		txtDesc.clear();
		rgPerAnno.clear();
		radTaxa[0].setSelected(true);
		radMethods[0].setSelected(true);
		theDataSetPanel.setIncludeLimit("1");
		theDataSetPanel.setExcludeLimit("1");
		theDataSetPanel.setAllSelectedInc(false);
		theDataSetPanel.setAllSelectedEx(false);
	}
	
	//Called by main frame to query the database
	public void setSubQuery() {
		query = "";
		if (cntGrp==0) return;
		
		query = Static.combineBool(query, setBasicQuery(), true);
		query = Static.combineBool(query, setGrpQuery(), true);
		query = Static.combineBool(query, setDataSetQuery(), true);
		
		if (query==null || query.equals("")) {
			if (theViewerFrame.getInfo().getCntGrp()>100000) {
				if (UserPrompt.showContinue("Slow query", 
						"There is more than 100,000 clusters.\nThis will be slow.")) {
					summary="All sequences";
				}
				else hasError=true;
			}
		}
		if (hasError)query="";
		else setSummary();
	}
	public String getSubQuery() { return query;}
	public String getQuerySummary() {return summary;}
	
	private void setSummary() {
		summary = "";
		if (cntGrp==0) return;
		
		summary = Static.combineSummary(summary, getBasicSummary());
		summary = Static.combineSummary(summary, getGrpSummary());
		summary = Static.combineSummary(summary, getDataSetSummary());
		
		if (summary==null || summary.length() == 0) summary = "Show All";
	}
	
	private String setDataSetQuery() {
		String retVal = "";
		if(theDataSetPanel.getIncludedNames() != null) {
			String [] labels = theDataSetPanel.getIncludedNames();
			String boolLabel = theDataSetPanel.getIncAnd()?"AND":"OR";
			int range = theDataSetPanel.getIncludeLimit();
			
			String query = "(pog_groups.A__" + labels[0] + " >= " + range;
			for(int x=1; x<labels.length; x++) {
				query += " " + boolLabel + " pog_groups.A__" + labels[x] + " >= " + range;
			}
			query += ")";
			retVal = Static.combineBool(retVal, query, true);
		}
		if(theDataSetPanel.getExcludedNames() != null) {
			String [] labels = theDataSetPanel.getExcludedNames();
			String boolLabel = theDataSetPanel.getExAnd()?"AND":"OR";
			int range = theDataSetPanel.getExcludeLimit();
			
			String query = "(pog_groups.A__" + labels[0] + " <= " + range;
			for(int x=1; x<labels.length; x++) {
				query += " " + boolLabel + " pog_groups.A__" + labels[x] + " <= " + range;
			}
			query += ")";
			retVal = Static.combineBool(retVal, query, true);
		}
		return retVal;
	}

	private String getDataSetSummary() {
		String retVal = "";
		if(theDataSetPanel.getIncludedNames() != null) {
			String [] labels = theDataSetPanel.getIncludedNames();
			String boolLabel = theDataSetPanel.getIncAnd()?"AND":"OR";
			int range = theDataSetPanel.getIncludeLimit();
			
			String query = "(" + labels[0] + " >= " + range;
			for(int x=1; x<labels.length; x++) {
				query += " " + boolLabel + " " + labels[x] + " >= " + range;
			}
			query += ")";
			retVal = Static.combineSummary(retVal, query);
		}
		if(theDataSetPanel.getExcludedNames() != null) {
			String [] labels = theDataSetPanel.getExcludedNames();
			String boolLabel = theDataSetPanel.getExAnd()?"AND":"OR";
			int range = theDataSetPanel.getExcludeLimit();
			
			String query = "(" + labels[0] + " <= " + range;
			for(int x=1; x<labels.length; x++) {
				query += " " + boolLabel + " " + labels[x] + " <= " + range;
			}
			query += ")";
			retVal = Static.combineSummary(retVal, query);
		}
		return retVal;
	}

	private String setBasicQuery() {
		String sql = "";
		sql = Static.combineBool(sql, txtGrpID.getSQL());
		sql = Static.combineBool(sql, txtHitID.getSQL());
		sql = Static.combineBool(sql, txtDesc.getSQL());
		sql = Static.combineBool(sql, rgPerAnno.getSQL());
		return sql;
	}
	private String getBasicSummary() {
		String summary = "";
		summary = Static.combineSummary(summary, txtGrpID.getSum());
		summary = Static.combineSummary(summary, txtHitID.getSum());
		summary = Static.combineSummary(summary, txtDesc.getSum());
		summary = Static.combineSummary(summary, rgPerAnno.getSum());
		return summary;
	}
	
	private String setGrpQuery() {
		String retVal = "";
		if (!radTaxa[0].isSelected()) {
			for (int i=1; i<=radTaxa.length; i++) {
				if (radTaxa[i].isSelected()) {
					retVal = Static.combineBool(retVal, "pog_groups.taxa = '" 
								+ radTaxa[i].getText() + "'", true);
					break;
				}
			}
		}
		
		if (!radMethods[0].isSelected()) {
			for (int i=1; i<=nMethods; i++) {
				if (radMethods[i].isSelected()) {
					String name = radMethods[i].getText();
					int id = theViewerFrame.getMethodIDForName(name);
					retVal = Static.combineBool(retVal, "pog_groups.PMid = " + id, true);
					break;
				}
			}
		}
		return retVal;
	}
	private String getGrpSummary() {
		String retVal = "";
		if (!radTaxa[0].isSelected()) {
			for (int i=1; i<radTaxa.length; i++) {
				if (radTaxa[i].isSelected()) {
					retVal = Static.combineSummary(retVal, "Taxa=" + radTaxa[i].getText());
				}
			}
		}
		if (!radMethods[0].isSelected()) {
			for (int i=1; i<=nMethods; i++) {
				if (radMethods[i].isSelected()) {
					retVal = Static.combineSummary(retVal, "Set=" + radMethods[i].getText());
				}
			}
		}
		return retVal;
	}
	
	/************************************************************************/
	private class Substring extends JPanel {
		private static final long serialVersionUID = 1L;
		public Substring(String lab, String field, String descript, boolean yesNo) {
			label = lab;  
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
			String val = txtVal.getText().trim(); 
			if (val.equals("")) return "";
			
			if (noButton!=null && noButton.isSelected())
				return "(" + sqlField + " NOT LIKE '%" + val + "%')";
			return "(" + sqlField + " LIKE '%" + val + "%')";
		}
		public String getSum() {
			if (!checkOn.isSelected()) return "";
			String val = txtVal.getText().trim(); 
			if (val.equals("")) return "";
			
			if (noButton!=null && noButton.isSelected())
				return label + " not contains '" + val + "'";
			return label + " contains '" + val + "'";
		}
		public void clear() {
			txtVal.setText("");
			checkOn.setSelected(false);
			txtVal.setEnabled(false);
		}
		private JCheckBox checkOn = null;
		private JTextField txtVal;
		private String sqlField, label;
		private JRadioButton yesButton= null, noButton= null;
		private int width=220;
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
			
			if (min.equals("")) { // CAS301
				if (max.contentEquals("")) {
					checkOn.setSelected(false);
					return "";
				}
				return sqlField + "<"  + max;
			}
			if (max.equals("")) {
				if (min.contentEquals("")) {
					min="0";
					txtMin.setText(min);
				}
				return sqlField + ">=" + min;
			}
			return "(" + sqlField + ">=" + min + " and " + sqlField + "<" + max + ")";
		}
		public String getSum() {
			if (!checkOn.isSelected()) return "";
			String min = txtMin.getText().trim(); // check if proper value
			String max = txtMax.getText().trim();
			
			if (min.equals("")) return label + "<"  + max;
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
	//Top button panel
	private JPanel buttonPanel = null;
	private JButton btnSearch = null, btnClear = null, btnHelp = null;
	private JButton btnExpand = null, btnCollapse = null;
	private JPanel mainPanel = null;
	private CollapsiblePanel [] theSections = null;
	
	// Basic
	private Substring txtGrpID = null, txtHitID = null, txtDesc = null;
	private Range rgPerAnno = null;
	
	// Methods
	private int nMethods=0;
	private JRadioButton [] radMethods = null;
	private JRadioButton [] radTaxa = null;
	
	// Assembly
	private DatasetSubPanel theDataSetPanel = null;
	
	private MTCWFrame theViewerFrame = null;
	private String summary="", query="", tabPrefix="";;
	private boolean hasError=false;
	private int cntGrp=0;
}
