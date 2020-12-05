package cmp.viewer.hits;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.panels.CollapsiblePanel;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

public class HitQueryPanel  extends JPanel {
	private static final long serialVersionUID = 1L;
	public static final String tag = MTCWFrame.HIT_PREFIX;
	public static final String helpHTML = Globals.helpDir + "HitQuery.html";
	
	private static final String [] SECTIONS = { "Basic"};
	private static final String [] SECTIONS_DESC = 	{ ""}; 

	public HitQueryPanel(MTCWFrame parentFrame) {
		theViewerFrame = parentFrame;
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
		btnSearch = Static.createButton("View Filtered Hits", true, Globals.FUNCTIONCOLOR);
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hasError=false;
				hitSQLwhere();
				if (!hasError) {
					HitTablePanel newPanel = new HitTablePanel(theViewerFrame, tag + theViewerFrame.getNextLabelNum(tag));
					theViewerFrame.addResultPanel(MTCWFrame.HIT_PREFIX, newPanel, newPanel.getName(), getQuerySummary());
				}
			}
		});
		
		btnExpand = new JButton("Expand All");
		btnExpand.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		btnExpand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<nSec; x++)theSections[x].expand();
			}
		});
		
		btnCollapse = new JButton("Collapse All");
		btnCollapse.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
		btnCollapse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int x=0; x<nSec; x++) theSections[x].collapse();
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
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Hit Filter Help", helpHTML);
			}
		});
		
		row.add(btnSearch); 	row.add(Box.createHorizontalStrut(30));
		row.add(btnExpand); 	row.add(Box.createHorizontalStrut(5));
		row.add(btnCollapse); 	row.add(Box.createHorizontalStrut(5));
		row.add(btnClear);		row.add(Box.createHorizontalStrut(30));
		
		row.add(Box.createHorizontalGlue());
		row.add(btnHelp);
		
		buttonPanel.add(row);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
	}

	private JPanel createBasicPanel() {
		JPanel page = Static.createPagePanel();
	
		JPanel row = Static.createRowPanel();
		txtHitID = new Substring("HitID", "HitID", 	"unique_hits.HITstr", 
				"Show all hits with the substring in their hitIDs");
		row.add(txtHitID);
		page.add(row);	page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		txtDesc = new Substring("Descript", "Descript", "unique_hits.description", 
				"Show all hits with the substring in their best description");
		row.add(txtDesc);
		page.add(row);	page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		rgNseq = new Range("nSeq", "1", "", "nSeq", 
				"All Hits where the number of hit sequences is >=N, with optional <M sequences");
		row.add(rgNseq);
		page.add(row);	page.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		rgNbest = new Range("nBest", "1", "", "nBest", 
				"All Hits where the number of best hits is >=N, with optional <M sequences");
		row.add(rgNbest);
		page.add(row);
		
		return page;
	}
	
	private void createSections() {
		nSec = SECTIONS.length;
		theSections = new CollapsiblePanel[nSec];
		
		for(int x=0; x<nSec; x++) {
			theSections[x] = new CollapsiblePanel(SECTIONS[x], SECTIONS_DESC[x]);
			theSections[x].setAlignmentX(Component.LEFT_ALIGNMENT);
			theSections[x].expand();
		}
		theSections[0].add(createBasicPanel());
	}
	private void clearFilters() {
		txtHitID.clear(); txtDesc.clear();
		rgNseq.clear(); rgNbest.clear();
	}
	
	private void hitSQLwhere() {
		sqlWhere = "";
		
		sqlWhere = Static.combineBool(sqlWhere, txtHitID.getSQL());
		sqlWhere = Static.combineBool(sqlWhere, txtDesc.getSQL());
		
		sqlWhere = Static.combineBool(sqlWhere, rgNseq.getSQL());
		sqlWhere = Static.combineBool(sqlWhere, rgNbest.getSQL());
		
		if (sqlWhere==null || sqlWhere.equals("")) {
			if (theViewerFrame.getInfo().getCntHit()>100000) {
				if (UserPrompt.showContinue("Slow query", 
						"There is more than 100,000 hits.\nThis will be slow.")) {
					summary="All hits";
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
		
		summary = Static.combineSummary(summary, txtHitID.getSum());
		summary = Static.combineSummary(summary, txtDesc.getSum());
		summary = Static.combineSummary(summary, rgNseq.getSum());
		summary = Static.combineSummary(summary, rgNbest.getSum());
		
		if (summary==null || summary.length()==0) summary = "All hits";
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
	
	private MTCWFrame theViewerFrame = null;
	
	//Top button panel
	private JPanel buttonPanel = null;
	private JButton btnSearch = null, btnClear = null, btnHelp = null;
	private JButton btnExpand = null, btnCollapse = null;
	
	private JPanel mainPanel = null;
	private CollapsiblePanel [] theSections = null;
	
	// General
	private Substring txtHitID = null, txtDesc = null;
	private Range rgNseq=null, rgNbest=null;
		
	private String summary="", sqlWhere="";
	private boolean hasError=false;
	private int nSec=0;
}
