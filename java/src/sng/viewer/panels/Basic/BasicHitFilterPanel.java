package sng.viewer.panels.Basic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.BlastHitData;
import sng.dataholders.ContigData;
import sng.dataholders.SequenceData;
import sng.viewer.STCWFrame;
import sng.viewer.panels.MainToolAlignPanel;
import sng.viewer.panels.seqDetail.LoadFromDB;
import util.align.AlignCompute;
import util.align.AlignData;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.ButtonComboBox;
import util.ui.UserPrompt;

public class BasicHitFilterPanel extends JPanel {
	private static final long serialVersionUID = 6371517539623729378L;
	public static final int VIEW_BY_GRP = 1; 
	public static final int VIEW_BY_SEQ = 2; 
	private static final Color BGCOLOR = Globals.BGCOLOR;
	private static final String GO_FORMAT = Globals.GO_FORMAT;
	private static final double DEFAULT_EVAL = 1e-30;
	private static final String DEFAULT_SIM = "50";
	private static final String DEFAULT_ALIGN = "50";
	private static final double DEFAULT_DE = 0.05;
	private static final double DEFAULT_RPKM = 1;
	private static final NumberFormat formatd = new DecimalFormat("0E0");
	private static final int DOUBLE_SIZE=4;
	private static final int INT_SIZE=2;
	private static final int LABEL_CHK_WIDTH1 = 60;
	private static final int LABEL_WIDTH2 = 35;
	
	private static final String hitPref= "_hitPrefs"; // save to preferences; same columns for hit&seq
	
	// If these are changed: 
	// (1) Change in loadFromDatabase
	// (2) Change in BasicHitQueryTab: getValueAt and compareTo
	// Keep SEQ and GRP columns the same, just different interpretations.
	private static final String [] SEQ_STATIC_COLUMNS = { 
		"Row", "Seq ID",  "Length", "Hit ID", "Best", "Rank", 
		"E-value+",  "%Sim+", "%SeqCov+", "%HitCov+", "Align+",  
		"Description", "Species", "DBtype", "Taxonomy"};
	private static final String [] GRP_STATIC_COLUMNS = { 
		"Row", "Hit ID", "Length", "#Seqs", "#Best", "#Rank1", 
		"E-value*", "%Sim*", "%SeqCov*", "%HitCov*", "Align*", 
		"Description", "Species", "DBtype", "Taxonomy"}; 
	private static final String [] GO_STATIC_COLUMNS = {"nGO", "GO", "InterPro", "KEGG", "Pfam", "EC (enzyme)"};
	public final int NUM_SEQ_COL = SEQ_STATIC_COLUMNS.length;
	public final int NUM_HIT_COL = GRP_STATIC_COLUMNS.length;
	
	// this expects Seq and Hit columns to have columns in same order
	private boolean isOnByDefault(int x) {
		if (x==0 || x==1 || x==3 || x==8 || x==9 || x==10) return true;
		return false;
	}
	/**********************************************
	* The top queries, and 4 selection panels 
	*/
	public BasicHitFilterPanel(STCWFrame frame, BasicHitQueryTab parentTab) {
		theParentTab = parentTab;
		theMainFrame = frame;
		metaData = frame.getMetaData();
		hasGO = metaData.hasGOs();
		totalSeqHitPairs = metaData.totalSeqHitPairs();
		initColumns();
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Color.white);
		
		queryPanel = new MainQueryPanel();
		annoDBPanel = new AnnoDBPanel();
		speciesPanel = new SpeciesPanel();
		goPanel = new GOetcPanel();
		countPanel = new CountPanel();
		
		colSeqPanel = new ColumnPanel(seqStaticColNames, false);
		colGrpPanel = new ColumnPanel(grpStaticColNames, true);
		alignPanel = Static.createPagePanel();
		
		add(queryPanel);
		add(annoDBPanel);
		add(colGrpPanel);
		add(colSeqPanel);
		add(goPanel); 
		add(speciesPanel);
		add(countPanel);
		add(alignPanel);
	}
	private void initColumns() {
		
		if(metaData.hasExpLevels()) libColNames = metaData.getLibNames();
		if(metaData.hasDE()) pvalColNames = metaData.getDENames();
			
		int x, y;
// SEQUENCE
		int totalColumns = SEQ_STATIC_COLUMNS.length;
		if (hasGO) totalColumns+=GO_STATIC_COLUMNS.length;
		
		seqStaticColNames = new String[totalColumns];
		for(x=0; x<SEQ_STATIC_COLUMNS.length; x++)         seqStaticColNames[x] = SEQ_STATIC_COLUMNS[x];
		if (hasGO) {
			for(y=0; y<GO_STATIC_COLUMNS.length; x++, y++) seqStaticColNames[x] = GO_STATIC_COLUMNS[y];
		}
// GROUPED HIT
		totalColumns = GRP_STATIC_COLUMNS.length;
		if (hasGO) totalColumns+=GO_STATIC_COLUMNS.length;
		
		grpStaticColNames = new String[totalColumns];
		for(x=0; x<GRP_STATIC_COLUMNS.length; x++)         grpStaticColNames[x] = GRP_STATIC_COLUMNS[x];
		if (hasGO) {
			for(y=0; y<GO_STATIC_COLUMNS.length; x++, y++) grpStaticColNames[x] = GO_STATIC_COLUMNS[y];
		}
	}
	private void hideMain() {
		queryPanel.setVisible(false);
		theParentTab.hideAll();
	}
	private void showMain() {
		queryPanel.setVisible(true);
		theParentTab.showAll();
	}
	/****************************************
	 * Called by BasicHitQueryTab
	 */
	public String getFilters() {
		return filters;
	}
	public String [] getColNames() { 
		if (isGrpView) return colGrpPanel.getColNames(); 
		else return colSeqPanel.getColNames();
	}
	public boolean [] getColSelect() { 
		if (isGrpView) return colGrpPanel.getColSelect(); 
		else return colSeqPanel.getColSelect();
	}
	public void displayAlign(Vector <String> seqList, Vector <String> hitList) {
		HashMap <String, ContigData> seqObj = loadFromDatabaseSeqObj(seqList);
		HashMap <String, SequenceData> hitObj = loadFromDatabaseHitObj(hitList);
		Vector <BlastHitData> blastObj = loadFromDatabaseBlastObj(seqList, hitList);
		
		Vector<AlignData> hitSeqList =  
				AlignCompute.DBhitsAlignDisplay(seqList, hitList, blastObj, seqObj, hitObj, 
						AlignCompute.frameResult, metaData.isProteinDB());
		
		alignPanel.removeAll();
		
		JPanel page = Static.createPagePanel();
		JPanel row = Static.createRowPanel();
		JButton btnReturn = new JButton("Back to annoDB hits");
		btnReturn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alignPanel.setVisible(false);
				showMain();
			}
		});
		btnReturn.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.add(btnReturn);
		row.setMaximumSize(new Dimension (Integer.MAX_VALUE, (int)row.getPreferredSize ().getHeight()));
		page.add(row, BorderLayout.WEST);
		alignPanel.add(page);
		alignPanel.add(Box.createVerticalStrut(3));
		
		JPanel page2 = Static.createPagePanel();
		MainToolAlignPanel drawPanel = 
				MainToolAlignPanel.createPairAlignPanel (true, false, hitSeqList );
		page2.add(drawPanel);
		alignPanel.add( page2, BorderLayout.WEST);
		
		hideMain();
		alignPanel.setVisible(true);
	}
	
	/***************************************************
	 * The main filter panel
	 */
	private class MainQueryPanel extends JPanel {
		private static final long serialVersionUID = -5987399873828589062L;
	
		public MainQueryPanel() {
			setBackground(BGCOLOR);
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setAlignmentX(Component.LEFT_ALIGNMENT);
			
			add(new JSeparator());
			add(Box.createVerticalStrut(3));	// need for Linux or line too close
			
// Search
			JPanel rowSearch = Static.createRowPanel();
			chkUseSearch = Static.createCheckBox("", false);
			chkUseSearch.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent ae) {
			    	  	enableSections();
			      }
			 });
			lblSearch = createLabel("Search", LABEL_CHK_WIDTH1);
			rowSearch.add(chkUseSearch);
			rowSearch.add(lblSearch);
			
			lblID = Static.createLabel("Hit IDs", false);
			radHitID = Static.createRadioButton("", false);
			radHitID.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent ae) {
			    	  	boolean enable = radHitID.isSelected();
			    	  	lblID.setEnabled(enable);
			    	  	lblDesc.setEnabled(!enable);
			      }
			 });
			rowSearch.add(radHitID);
			rowSearch.add(lblID);
			
			lblDesc = Static.createLabel("Description", true);
			radDesc = Static.createRadioButton("", false);
			radDesc.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent ae) {
			    	  	boolean enable = radDesc.isSelected();
			    	  	lblID.setEnabled(!enable);
			    	  	lblDesc.setEnabled(enable);
			      }
			 });
			radDesc.setSelected(true);
			rowSearch.add(radDesc);
			rowSearch.add(lblDesc);
			rowSearch.add(Box.createHorizontalStrut(5));
			
			ButtonGroup allbg = new ButtonGroup();
			allbg.add(radHitID); 
			allbg.add(radDesc); 
			
			
			rowSearch.add(new JLabel("  Substring: "));
			txtField  = Static.createTextField("", 20, false);
			txtField.setMaximumSize(txtField.getPreferredSize());
			rowSearch.add(txtField);
			rowSearch.add(Box.createHorizontalStrut(5));
			
			btnFindFile = Static.createButton("Load File", false);
			btnFindFile.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						JFileChooser fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
						if(fc.showOpenDialog(getInstance()) == JFileChooser.APPROVE_OPTION) {
							loadFile(fc.getSelectedFile().getPath());
						}
					}
					catch(Exception e) {ErrorReport.prtReport(e, "Error finding file");}
				}});
			rowSearch.add(btnFindFile);
			rowSearch.add(Box.createHorizontalStrut(30)); // increase separator line
			
			add(rowSearch);
			add(Box.createVerticalStrut(2));	
			add(new JSeparator());
			add(Box.createVerticalStrut(2));	
			
// Filters
			// Seq: [] E-value [ ]  [] #Seq [  ]  [] Exp
			JPanel rowFilter1 = Static.createRowPanel();
			chkUseFilters = Static.createCheckBox("", true);
			chkUseFilters.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent ae) {
			    	  	enableSections();
			      }
			 });
			lblFilters = createLabel("Filters", LABEL_CHK_WIDTH1);
			rowFilter1.add(chkUseFilters);
			rowFilter1.add(lblFilters);
			
			JLabel seqLabel = createLabel("Seq:", LABEL_WIDTH2);
			rowFilter1.add(seqLabel);
			
			boxBest = new ButtonComboBox(); 
			boxBest.addItems(bestOpt);
			boxBest.setSelectedIndex(1);
		 	boxBest.setMaximumSize(boxBest.getPreferredSize());
		 	boxBest.setMinimumSize(boxBest.getPreferredSize());
		 	rowFilter1.add(boxBest);
			rowFilter1.add(Box.createHorizontalStrut(4));
			
			boolean isActive=true;
			chkUseEval = Static.createCheckBox("E-val<=", isActive);
		 	chkUseEval.addActionListener(new ActionListener() {
	    			public void actionPerformed(ActionEvent e) {
	    				enableEvalSim();
	    			}
	    		});   
	    		rowFilter1.add(chkUseEval);
			txtEval = Static.createTextField(formatd.format(DEFAULT_EVAL), DOUBLE_SIZE, isActive);
			rowFilter1.add(txtEval);
			rowFilter1.add(Box.createHorizontalStrut(2));
			
			isActive = false;
			chkUseSim = Static.createCheckBox("%Sim>=", isActive);
			chkUseSim.addActionListener(new ActionListener() {
	    			public void actionPerformed(ActionEvent e) {
	    				enableEvalSim();
	    			}
	    		});   
	    		rowFilter1.add(chkUseSim);
			txtSim = Static.createTextField( DEFAULT_SIM, INT_SIZE,isActive);
			rowFilter1.add(txtSim);
			rowFilter1.add(Box.createHorizontalStrut(2));
			
			isActive = false;
			chkUseAlign = Static.createCheckBox("%Hcov", isActive);
			chkUseAlign.addActionListener(new ActionListener() {
	    			public void actionPerformed(ActionEvent e) {
	    				enableEvalSim();
	    			}
	    		});   
	    		rowFilter1.add(chkUseAlign);
			txtAlign = Static.createTextField( DEFAULT_SIM, INT_SIZE,isActive);
			rowFilter1.add(txtAlign);
			rowFilter1.add(Box.createHorizontalStrut(8));
			
			// RPKM and DE
			isActive = false;
			chkCount = Static.createCheckBox("", isActive);
			chkCount.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean check = chkCount.isSelected();
					btnCount.setEnabled(check);
	    			}
	    		});  
			btnCount = new JButton("Exp");
			btnCount.setBackground(Globals.MENUCOLOR);
			btnCount.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						hideMain();
						countPanel.setVisible(true);
					}
					catch(Exception e) {ErrorReport.prtReport(e, "Count");}
				}});
			btnCount.setEnabled(isActive);
			
			if (libColNames!=null && libColNames.length>0) {
				rowFilter1.add(chkCount);
				rowFilter1.add(btnCount);
				rowFilter1.add(Box.createHorizontalStrut(6));
			}
			add(rowFilter1);
			add(Box.createVerticalStrut(5));	
			
	// Hit : [] AnnoDB  [] Species   [] GOetc
			JPanel rowFilter2 = Static.createRowPanel();
			rowFilter2.add(createLabel("", lblSearch.getPreferredSize().width + 
										chkUseFilters.getPreferredSize().width));
			rowFilter2.add(createLabel("Hit:", seqLabel.getPreferredSize().width));
			
			isActive = false;
			chkAnnoDBs = Static.createCheckBox("", isActive);
			chkAnnoDBs.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean check = chkAnnoDBs.isSelected();
					btnAnnoDBs.setEnabled(check);
	    			}
	    		});  
			rowFilter2.add(chkAnnoDBs);
			btnAnnoDBs = new JButton("AnnoDBs"); 
			btnAnnoDBs.setBackground(Globals.MENUCOLOR);
			btnAnnoDBs.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						hideMain();
						annoDBPanel.setVisible(true);
					}
					catch(Exception e) {ErrorReport.prtReport(e, "AnnoDB");}
				}});
			btnAnnoDBs.setEnabled(isActive);
			rowFilter2.add(btnAnnoDBs);
			rowFilter2.add(Box.createHorizontalStrut(20));
			
			chkSpecies = Static.createCheckBox("", isActive);
			chkSpecies.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean check = chkSpecies.isSelected();
					btnSpecies.setEnabled(check);
	    			}
	    		});  
			rowFilter2.add(chkSpecies);
			btnSpecies = new JButton("Species"); 
			btnSpecies.setBackground(Globals.MENUCOLOR);
			btnSpecies.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						hideMain();
						speciesPanel.setVisible(true);
					}
					catch(Exception e) {ErrorReport.prtReport(e, "Species");}
				}});
			btnSpecies.setEnabled(isActive);
			rowFilter2.add(btnSpecies);
			rowFilter2.add(Box.createHorizontalStrut(20));
			
			chkGOetc = Static.createCheckBox("", isActive);
			chkGOetc.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean check = chkGOetc.isSelected();
					btnGOetc.setEnabled(check);
	    			}
	    		});  
			btnGOetc = new JButton("GO, etc");
			btnGOetc.setBackground(Globals.MENUCOLOR);
			btnGOetc.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						hideMain();
						goPanel.setVisible(true);
					}
					catch(Exception e) {ErrorReport.prtReport(e, "GO etc");}
				}});
			btnGOetc.setEnabled(isActive);
			if (metaData.hasGOs()) {
				rowFilter2.add(chkGOetc);
				rowFilter2.add(btnGOetc);
			}
			rowFilter2.add(Box.createHorizontalStrut(2));
			rowFilter2.add(Box.createHorizontalGlue());
			
			JButton clearall = new JButton("Clear All");
			clearall.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						annoDBPanel.clear();
						speciesPanel.clear();
						goPanel.clear();
						countPanel.clear();
						queryPanel.clear();
					}
					catch(Exception e) {ErrorReport.prtReport(e, "GO etc");}
			}});
			clearall.setMargin(new Insets(0, 0, 0, 0));
			clearall.setFont(new Font(clearall.getFont().getName(),Font.PLAIN,10));
			clearall.setAlignmentX(RIGHT_ALIGNMENT);
			rowFilter2.add(clearall);
			add(rowFilter2);
			add(Box.createVerticalStrut(5));	
			
			// Results
			add(new JSeparator());
			add(Box.createVerticalStrut(3));	
			
			JPanel rowResults = Static.createRowPanel();
			
			rowResults.add(createLabel("  Results", lblSearch.getPreferredSize().width + 
									chkUseFilters.getPreferredSize().width));
			btnBuildTable = new JButton("BUILD TABLE");
			btnBuildTable.setBackground(Globals.FUNCTIONCOLOR);
			btnBuildTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (checkFilters())
						loadDataForTable(true /* isBuild */);
				}
			});
			rowResults.add(btnBuildTable);
			rowResults.add(Box.createHorizontalStrut(10));
			
			btnAddTable = new JButton("ADD to TABLE");
			btnAddTable.setBackground(Globals.FUNCTIONCOLOR);
			btnAddTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (checkFilters())
						loadDataForTable(false /* isAdd */);
				}
			});
			rowResults.add(btnAddTable);
			rowResults.add(Box.createHorizontalStrut(10));
			
			btnSetColumns = new JButton("Hit Columns");
			btnSetColumns.setBackground(Globals.MENUCOLOR);
			btnSetColumns.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					hideMain();
					if (isGrpView) colGrpPanel.setVisible(true);
					else colSeqPanel.setVisible(true);
				}
			});
			rowResults.add(btnSetColumns);
			rowResults.add(Box.createHorizontalStrut(10));
			
			showGrouped = new JCheckBox("Group by Hit ID", true);
			showGrouped.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isGrpView = showGrouped.isSelected();
					if (isGrpView) btnSetColumns.setText("Hit Columns");
					else  btnSetColumns.setText("Seq Columns");
					theParentTab.tableRefresh(isGrpView);
				}
			});
			rowResults.add(showGrouped);
			add(rowResults);
			
			enableSections();
			setMinimumSize(getPreferredSize());
			setMaximumSize(getPreferredSize());
			setVisible(true);
		}
		
		private JLabel createLabel(String label, int width) {
			JLabel tmp = new JLabel(label);
			Dimension dim = tmp.getPreferredSize();
			dim.width = width;
			tmp.setPreferredSize(dim);
			tmp.setMaximumSize(tmp.getPreferredSize());
			return tmp;
		}
			
		private void loadFile(String fileName) {
			try {
				loadList= new Vector <String> ();
				String line;
				BufferedReader file = new BufferedReader(new FileReader(fileName));
				while((line = file.readLine()) != null) {
					line.replace("\n","").trim();
					if (line.equals("")) continue;
					if (line.startsWith("#")) continue;
					
					loadList.add(line);
				}
				file.close(); 
				if (loadList.size()==0) loadList=null;
				else txtField.setText(loadList.get(0) + ",...");
			}
			catch(Exception e) {ErrorReport.prtReport(e, "Error loading file");}
		}
		private STCWFrame getInstance() {return theMainFrame;}
		private double getEvalLimit() { 
			if (!chkUseEval.isSelected()) return -1;
			
			try {
				return Double.parseDouble(txtEval.getText());
			}
			catch (Exception e){
				txtEval.setText(formatd.format(DEFAULT_EVAL));
				return DEFAULT_EVAL;
			}
		}
		public boolean useSearch() {
			String x = txtField.getText().trim(); 
			return (chkUseSearch.isSelected() && !x.equals(""));
		}
		public boolean useFilter() {
			return chkUseFilters.isSelected();
		}	
		
		public boolean isDesc() {
			return radDesc.isSelected();
		}
		
		public int getTypeSearch() {
			String x = txtField.getText().trim(); 
			if (loadList!=null || x.contains("%")) return 1; // 1st search only
			else if (radDesc.isSelected()) return 2;			// 2nd search only
			else return 3;									// 2nd search with % added
		}
		public int getAlign() {
			if (!chkUseAlign.isSelected()) return 0;
				
			try {
				return Integer.parseInt(txtAlign.getText());
			}
			catch (Exception e){
				return 0;
			}
		}
		public String getWhere(boolean bSecond) {
			if (!chkUseSearch.isSelected()) return "";
			
			String searchStr = txtField.getText().trim();
			if (searchStr.equals("")) return ""; 

			if (searchStr.endsWith("...") && loadList==null)       searchStr = searchStr.replace("...", "");
			else if (!searchStr.endsWith("...") && loadList!=null) loadList = null;
			
			if (loadList!=null) searchStr = Static.addQuoteDBList(loadList);
			else {
				searchStr = Static.addQuoteDB(searchStr);
				if (bSecond) searchStr = Static.addWildDB(searchStr);
			}
		      
            String col = "";
            if (radHitID.isSelected()) col = "tn.uniprot_id";
			else col = "uq.description";
             
        		String strQuery = " AND " + col;
        		if (loadList!=null)    strQuery+=	" IN ("  + searchStr + ")";
        		else if (searchStr.contains("%")) strQuery+=	" LIKE " + searchStr; 
        		else                              strQuery+=	" = "    + searchStr; 
            
			return strQuery;
		}
		public String getWhere() {
			String strQuery="";
			double eVal = getEvalLimit();
			if(eVal >= 0) strQuery= " AND  tn.e_value <= " + eVal;
			if (chkUseSim.isSelected()) strQuery  += " AND tn.percent_id>=" + txtSim.getText();
			// chkUseAlign  filter is in loadFromDatbase
			
			int x = boxBest.getSelectedIndex();
			if (x!=0) {
				if (x==1) strQuery += " AND tn.blast_rank=1";
				else if (x==2) strQuery += " AND tn.filter_best=1"; 
				else if (x==3) strQuery += " AND tn.filter_ovbest=1 ";
				else if (x==4) strQuery += " AND (tn.filter_ovbest=1 AND tn.filter_best=1)";
				else if (x==5) strQuery += " AND (tn.filter_ovbest=1 OR tn.filter_best=1)";
			}	
			return strQuery;
		}
		public String getSummaryColumn() {
			if (radHitID.isSelected()) return "Hit ID";
			else return "Desc";
		}
		public String getSummaryText() { 
			if (!chkUseSearch.isSelected()) return "";
			String x = txtField.getText().trim(); 
			if (x.equals("")) return "";
			return Static.addQuote(x); 
		}
		public String getSummary () { 
			String sum="";
			double eval = getEvalLimit();
			if (eval>=0) sum = "E-value " + formatd.format(eval);
			
			String sim = (chkUseSim.isSelected()) ? txtSim.getText() : "";
			if (!sim.equals("")) sum = strMerge(sum, "Sim " + sim + "%");
			
			String aln = (chkUseAlign.isSelected()) ? txtAlign.getText() : "";
			if (!aln.equals("")) sum = strMerge(sum, "Align " + aln + "%");
			
			int x = boxBest.getSelectedIndex();
			if (x!=0) {
				if (x==1) sum = strMerge(sum, "Rank=1");
				else if (x==2) sum = strMerge(sum, "Best Eval");
				else if (x==3) sum = strMerge(sum, "Best Anno");
				else if (x==4) sum = strMerge(sum, "Best Eval & Anno");
				else if (x==5) sum = strMerge(sum, "Best Eval | Anno");
			}	
			return sum;
		}
		public void clear() {
			chkUseSearch.setSelected(false); lblSearch.setEnabled(false);
			chkUseFilters.setSelected(true); lblFilters.setEnabled(true);
			
			txtField.setText("");
			radDesc.setSelected(true); 
			
			lblDesc.setEnabled(true);
			lblID.setEnabled(false);
			
			chkUseAlign.setSelected(false);
			txtAlign.setText(DEFAULT_SIM); txtAlign.setEnabled(false);
			
			chkUseEval.setSelected(false); 
			txtEval.setText(formatd.format(DEFAULT_EVAL)); txtEval.setEnabled(false); 
			
			chkUseSim.setSelected(false);
			txtSim.setText(DEFAULT_SIM); txtSim.setEnabled(false); 
			
			boxBest.setSelectedIndex(0);
			chkCount.setSelected(false); btnCount.setEnabled(false);
			
			chkAnnoDBs.setSelected(false); chkSpecies.setSelected(false); chkGOetc.setSelected(false);
			btnAnnoDBs.setEnabled(false);  btnSpecies.setEnabled(false);  btnGOetc.setEnabled(false);
		}
		
		private void enableEvalSim() {
			txtEval.setEnabled(chkUseEval.isSelected());
			txtSim.setEnabled(chkUseSim.isSelected());
			txtAlign.setEnabled(chkUseAlign.isSelected());
		}
		private void enableSections() {
			boolean sec1 = chkUseSearch.isSelected();
			boolean sec2 = chkUseFilters.isSelected();
			
			lblSearch.setEnabled(sec1);
			lblID.setEnabled(sec1);     lblDesc.setEnabled(sec1);
			radHitID.setEnabled(sec1);  radDesc.setEnabled(sec1);
			txtField.setEnabled(sec1);  btnFindFile.setEnabled(sec1);
			if (sec1) {
				if (!radHitID.isSelected()) lblID.setEnabled(false);
				if (!radDesc.isSelected()) lblDesc.setEnabled(false);
			}
			
			lblFilters.setEnabled(sec2);
			boxBest.setEnabled(sec2);    
			chkCount.setEnabled(sec2);btnCount.setEnabled(sec2);
			
			txtEval.setEnabled(sec2);    txtSim.setEnabled(sec2);     txtAlign.setEnabled(sec2);
			chkUseEval.setEnabled(sec2); chkUseSim.setEnabled(sec2);  chkUseAlign.setEnabled(sec2);
	
			chkAnnoDBs.setEnabled(sec2); chkSpecies.setEnabled(sec2); chkGOetc.setEnabled(sec2);
			btnAnnoDBs.setEnabled(sec2); btnSpecies.setEnabled(sec2); btnGOetc.setEnabled(sec2);
			
			if (sec2) {// only enable if corresponding check box
				if (!chkCount.isSelected())   	btnCount.setEnabled(false);
				if (!chkAnnoDBs.isSelected()) 	btnAnnoDBs.setEnabled(false);
				if (!chkSpecies.isSelected()) 	btnSpecies.setEnabled(false);
				if (!chkGOetc.isSelected())   	btnGOetc.setEnabled(false);	
				if (!chkCount.isSelected())   	btnCount.setEnabled(false);	
				if (chkUseEval.isSelected()) 	{
					txtEval.setEnabled(sec2);
				}
				if (chkUseSim.isSelected())  	{
					txtSim.setEnabled(sec2);
				}
				if (chkUseAlign.isSelected())  	{
					txtAlign.setEnabled(sec2);
				}
			}
		}
		
		public void enableAddToTable(boolean b) {btnBuildTable.setEnabled(b);}
		
		private JCheckBox chkUseSearch = null, chkUseFilters = null;
		private JLabel lblSearch=null, lblFilters=null;
		
		// Search:
		private JLabel lblID=null, lblDesc=null;
		
		public JRadioButton radHitID = null, radDesc = null;
		public JTextField txtField = null;
		public Vector <String> loadList = null;
		
		// Seq: first filter line
		private JCheckBox  chkUseEval = null, chkUseSim = null, chkUseAlign=null;
		private JTextField txtEval = null,    txtSim = null,    txtAlign=null;
		
		private String [] bestOpt = {"None(slow)", "Rank=1", "Best Eval", "Best Anno", 
				"Eval&Anno", "Eval|Anno"};
		private ButtonComboBox boxBest = null;
		
		private JCheckBox chkCount = null;
		private JButton btnCount = null;
				
		// Hit: second filter line 
		public JCheckBox chkAnnoDBs = null, chkSpecies = null, chkGOetc = null;
		private JButton  btnAnnoDBs = null, btnSpecies = null, btnGOetc = null;
				
		// Results
		private JButton btnBuildTable = null;
		private JButton btnAddTable = null;
		private JButton btnSetColumns = null;
		private JCheckBox showGrouped = null;
	} // end QueryPanel
	
	/**************************************************
	 * AnnoSelect
	 */
	private class AnnoDBPanel extends JPanel
	{
		private static final long serialVersionUID = 1L;
		private static final int ANNODB_NUM_COLUMNS = 4;
		private static final int ANNODB_COLUMN_WIDTH = 150;
		
		public AnnoDBPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
			
			JPanel headerPanel = Static.createPagePanel();
			JLabel theHeader = new JLabel("<HTML><H2>Filter on annoDB</H2></HTML>");
			theHeader.setBackground(Color.white);
			theHeader.setAlignmentX(CENTER_ALIGNMENT);
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			headerPanel.setMaximumSize(headerPanel.getPreferredSize());
			headerPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(Box.createVerticalStrut(30));
			add(headerPanel);
			add(Box.createVerticalStrut(10));
			
			add(createSelectPanel());
			
			add(Box.createVerticalStrut(30));
			btnOK = new JButton("Accept");
			btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSelections();
					showMain();
					setVisible(false);
				}
			});
			btnCancel = new JButton("Discard");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreSelections();
					showMain();
					setVisible(false);
				}
			});
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(Color.white);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(btnOK);
			buttonPanel.add(Box.createHorizontalStrut(10));
			buttonPanel.add(btnCancel);
			buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
			buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(buttonPanel);
			setVisible(false);
		}
		
		private JPanel createSelectPanel() {
			String [] annoDBs = metaData.getAnnoDBs();
			if (annoDBs==null) annoDBs = new String [0];
			
			JPanel panel = new JPanel();
			panel.setBackground(BGCOLOR);
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			panel.setAlignmentX(Component.CENTER_ALIGNMENT);
			
			chkAnnoDB = new JCheckBox[annoDBs.length/2];
			lblTaxo = new JLabel[annoDBs.length/2];
			
			JPanel row = new JPanel();
			row.setBackground(BGCOLOR);
			row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			panel.add(new JSeparator());
			int index = 0;
			for(int x=0; x<chkAnnoDB.length; x++) {
				String db = annoDBs[index++].toUpperCase();
				chkAnnoDB[x] = new JCheckBox(db);
				chkAnnoDB[x].setBackground(BGCOLOR);
				chkAnnoDB[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if(isAllAnnoSelected())
							selectAll.setSelected(true);
						else
							selectAll.setSelected(false);
					}
				});
				chkAnnoDB[x].setSelected(true);
				lblTaxo[x] = new JLabel(annoDBs[index++]);
				JPanel selection = new JPanel();
				selection.setBackground(BGCOLOR);
				selection.setLayout(new BoxLayout(selection, BoxLayout.LINE_AXIS));
				selection.setAlignmentX(Component.LEFT_ALIGNMENT);
				selection.add(chkAnnoDB[x]);
				selection.add(new JLabel("--"));
				selection.add(lblTaxo[x]);
				if(selection.getPreferredSize().width < ANNODB_COLUMN_WIDTH)
					selection.add(Box.createHorizontalStrut(ANNODB_COLUMN_WIDTH-selection.getPreferredSize().width));
				selection.setMaximumSize(selection.getPreferredSize());
				row.add(selection);
				if(row.getComponentCount() == ANNODB_NUM_COLUMNS) {
					panel.add(row);
					row = new JPanel();
					row.setBackground(BGCOLOR);
					row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
					row.setAlignmentX(Component.LEFT_ALIGNMENT);
				}
			}
			if(row.getComponentCount() > 0)
				panel.add(row);
			panel.add(Box.createVerticalStrut(5));
			panel.add(new JSeparator());
			selectAll = new JCheckBox("Check/Uncheck All");
			selectAll.setBackground(BGCOLOR);
			selectAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for(int x=0; x<chkAnnoDB.length; x++)
						chkAnnoDB[x].setSelected(selectAll.isSelected());
				}
			});
			selectAll.setAlignmentX(Component.LEFT_ALIGNMENT);
			selectAll.setSelected(true);
			row = new JPanel();
			row.setBackground(BGCOLOR);
			row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			row.add(selectAll);
			row.setMaximumSize(row.getPreferredSize());
			panel.add(row);
			panel.add(new JSeparator());
			JLabel lbl = new JLabel("The query will be on 'any' selected annoDB");
			lbl.setFont(new Font("Verdana",Font.PLAIN,12));
			panel.add(lbl);
			
			panel.setMaximumSize(panel.getPreferredSize());
			
			// needed to do this to get it centered
			JPanel selectPanel = new JPanel();
			selectPanel.setBackground(Color.white);
			selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.LINE_AXIS));
			selectPanel.add(panel);
			selectPanel.setMaximumSize(selectPanel.getPreferredSize());
			selectPanel.setAlignmentX(CENTER_ALIGNMENT);
			
			saveSelections();
			return selectPanel;
		}
		
		public String getWhere() {
			int num = getNumSelectedAnnoDB();
			if (num==0 || num==chkAnnoDB.length) return "";
			
			String [] annoTypes = new String[num];
			int index = 0;
			for(int x=0; x<chkAnnoDB.length; x++)
				if(chkAnnoDB[x].isSelected()) annoTypes[index++] = chkAnnoDB[x].getText();
	
			String [] annoTaxo = new String[num];
			index = 0;
			for(int x=0; x<lblTaxo.length; x++)
				if(chkAnnoDB[x].isSelected()) annoTaxo[index++] = lblTaxo[x].getText();
			
			String annoDBsearch = "";
            
            	for(int x=0; x<annoTypes.length; x++) {
            		String temp = "tn.dbtype='" + annoTypes[x] 
            				+ "' AND tn.taxonomy='" + annoTaxo[x] + "'";
            		
            		if(annoDBsearch.length() == 0) annoDBsearch = "(" + temp + ")";
            		else annoDBsearch += " OR (" + temp + ")";
            	}    
            	if (annoTypes.length>1) annoDBsearch = "(" + annoDBsearch + ")";
            	
            return " AND " + annoDBsearch;
		}
		public String getSummary() { 
			int num = getNumSelectedAnnoDB();
			if (num==0 || num==chkAnnoDB.length) return "";
			
			if (num==1) {
				for(int x=0; x<chkAnnoDB.length; x++) 
					if(chkAnnoDB[x].isSelected()) {
						String tax = lblTaxo[x].getText();
						if (tax.length()>3) tax = tax.substring(0,3);
						return "AnnoDB " + chkAnnoDB[x].getText() + tax;
					}
			}
			
			return num + " annoDBs selected"; 
		}
		public void clear() {
			selectAll.setSelected(true);
			for(int x=0; x<chkAnnoDB.length; x++) {
				chkAnnoDB[x].setSelected(true);
			}
			saveSelections();
		}
		private void saveSelections() {
			if (bSaveArray==null)
				bSaveArray = new boolean[chkAnnoDB.length];
			for(int x=0; x<chkAnnoDB.length; x++) {
				bSaveArray[x] = chkAnnoDB[x].isSelected();
			}
		}
		
		private void restoreSelections() {
			for(int x=0; x<chkAnnoDB.length; x++)
				chkAnnoDB[x].setSelected(bSaveArray[x]);
		}
		private boolean isAllAnnoSelected() {
			boolean retVal = true;
			
			for(int x=0; retVal && x<chkAnnoDB.length; x++)
				retVal = chkAnnoDB[x].isSelected();
			
			return retVal;
		}
		
		private int getNumSelectedAnnoDB() {
			int retVal = 0;
			for(int x=0; x<chkAnnoDB.length; x++)
				if(chkAnnoDB[x].isSelected()) retVal++;
			return retVal;
		}
		
		private boolean [] bSaveArray = null; 
		private JCheckBox [] chkAnnoDB = null;
		private JCheckBox selectAll = null;
		private JLabel [] lblTaxo = null;
		private JButton btnOK = null;
		private JButton btnCancel = null;
	}// end AnnoSelectionPanel
	/****************************
	 *  GO, KEGG, etc Panel
	 */
	private class GOetcPanel extends JPanel {
		private static final long serialVersionUID = 8503076748387692948L;
		
		public GOetcPanel() {			
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
			
			JPanel headerPanel = new JPanel();
			headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
			headerPanel.setBackground(Color.white);
			
			JLabel theHeader = new JLabel("<HTML><H2>Filter on attributes</H2></HTML>");
			theHeader.setBackground(Color.white);
			theHeader.setAlignmentX(CENTER_ALIGNMENT);
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			headerPanel.setMaximumSize(headerPanel.getPreferredSize());
			headerPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(Box.createVerticalStrut(30));
			add(headerPanel);
			add(Box.createVerticalStrut(10));
			
			add(createSelectPanel());
			
			add(Box.createVerticalStrut(30));
			btnOK = new JButton("Accept");
			btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSelections();
					showMain();
					setVisible(false);
				}
			});
			btnCancel = new JButton("Discard");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreSelections();
					showMain();
					setVisible(false);
				}
			});
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(Color.white);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(btnOK);
			buttonPanel.add(Box.createHorizontalStrut(10));
			buttonPanel.add(btnCancel);
			buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
			buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(buttonPanel);
			setVisible(false);
		}
		
		private JPanel createSelectPanel() {
			JPanel panel = new JPanel();
			panel.setBackground(BGCOLOR);
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			panel.setAlignmentX(Component.CENTER_ALIGNMENT);
			
			chkHasGO = new JCheckBox("Has GO");
			chkHasKEGG = new JCheckBox("Has KEGG");
			chkHasPFAM = new JCheckBox("Has Pfam");
			chkHasEC = new JCheckBox("Has EC (enzyme)");
			chkHasIP = new JCheckBox("Has InterPro");
			
			txtGO = new JTextField(15);
			txtKEGG = new JTextField(15);
			txtPFAM = new JTextField(15);
			txtEC = new JTextField(15);
			txtIP = new JTextField(15);
			
			JPanel row = new JPanel();
			row.setBackground(BGCOLOR);
			row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			int width = chkHasIP.getPreferredSize().width;
			if (metaData.hasGOs()) {
				
				row = Static.createRowPanel();
				row.add(new JLabel("Assigned to Hit:"));
				panel.add(Box.createVerticalStrut(10));
				panel.add(row);
				
				row = Static.createRowPanel();
				row.add(chkHasGO);
				row.add(Box.createHorizontalStrut(width - chkHasGO.getPreferredSize().width));
				row.add(Box.createHorizontalStrut(30));
				row.add(txtGO);
				row.add(new JLabel("(exact)"));
				row.setMaximumSize(row.getPreferredSize());
				panel.add(Box.createVerticalStrut(10));
				panel.add(row);
				
				row = Static.createRowPanel();
				row.add(chkHasIP);
				row.add(Box.createHorizontalStrut(30));
				row.add(txtIP);
				row.add(new JLabel("(contains)"));
				row.setMaximumSize(row.getPreferredSize());
				panel.add(Box.createVerticalStrut(10));
				panel.add(row);
				
				row = Static.createRowPanel();
				row.add(chkHasKEGG);
				row.add(Box.createHorizontalStrut(width - chkHasKEGG.getPreferredSize().width));
				row.add(Box.createHorizontalStrut(30));
				row.add(txtKEGG);
				row.add(new JLabel("(contains)"));
				row.setMaximumSize(row.getPreferredSize());
				panel.add(Box.createVerticalStrut(10));
				panel.add(row);
				
				row = Static.createRowPanel();
				row.add(chkHasPFAM);
				row.add(Box.createHorizontalStrut(width - chkHasPFAM.getPreferredSize().width));
				row.add(Box.createHorizontalStrut(30));
				row.add(txtPFAM);
				row.add(new JLabel("(contains)"));
				row.setMaximumSize(row.getPreferredSize());
				panel.add(Box.createVerticalStrut(10));
				panel.add(row);
				
				row = Static.createRowPanel();
				row.add(chkHasEC);
				row.add(Box.createHorizontalStrut(width - chkHasEC.getPreferredSize().width));
				row.add(Box.createHorizontalStrut(30));
				row.add(txtEC);
				row.add(new JLabel("(contains)"));
				row.setMaximumSize(row.getPreferredSize());
				panel.add(Box.createVerticalStrut(10));
				panel.add(row);
			}
			// needed to do this to get it centered
			JPanel selectPanel = new JPanel();
			selectPanel.setBackground(Color.white);
			selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.LINE_AXIS));
			selectPanel.add(panel);
			selectPanel.setMaximumSize(selectPanel.getPreferredSize());
			selectPanel.setAlignmentX(CENTER_ALIGNMENT);
			
			saveSelections();
			return selectPanel;
		}
		public String getWhere() {
			String strQuery="";
	       
	        if(chkHasGO.isSelected()) {
	        		String txt = txtGO.getText(); // get rid of GO:
	        		if (txt.length()>0) {
		        		String goterm= getGO(txt);
		        		if (goterm!=null) 
		        			strQuery += " AND uq.goList LIKE '%" + goterm + "%'";
		        		else 
		        			JOptionPane.showMessageDialog(this, 
		    	    				"Incorrect GO " + txt + "\nmust contain number\nignoring...", "Error", JOptionPane.PLAIN_MESSAGE);
	        		}
	        		else strQuery += " AND uq.goBrief <> ''"; 
	        }
	        if(chkHasIP.isSelected()) {
	        		String txt = txtIP.getText();
	            	if(txt.length() == 0) strQuery += " AND uq.interpro <> ''";
	            	else strQuery += " AND uq.interpro LIKE '%" + txt + "%'";
	        }
	        if(chkHasKEGG.isSelected()) {
	        		String txt = txtKEGG.getText();
	            	if(txt.length() == 0) strQuery += " AND uq.kegg <> ''";
	            	else strQuery += " AND uq.kegg LIKE '%" + txt + "%'";
            }
            if(chkHasPFAM.isSelected()) {
            		String txt = txtPFAM.getText();
	            	if(txt.length() == 0) strQuery += " AND uq.pfam <> ''";
	            	else strQuery += " AND uq.pfam LIKE '%" + txt + "%'";
            }
            if(chkHasEC.isSelected()) {
            		String txt = txtEC.getText();
	            	if(txt.length() == 0) strQuery += " AND uq.ec <> ''";
	            	else strQuery += " AND uq.ec LIKE '%" + txt + "%'";
            }  
			return strQuery;
		}
		private String getGO(String txt) {
			try {
	    			if (txt.startsWith("GO:")) txt = txt.substring(3);
	    			int gonum = Integer.parseInt(txt);
	    			String goterm = String.format(GO_FORMAT, gonum);
	    			return goterm;
	    		}
	    		catch (Exception e) { return null;}
		}
		public String getSummary() { 
			String ret = "";
			
			if (chkHasGO.isSelected()) {
				String txt = txtGO.getText();
				if(txt.length() > 0) {
					String goterm= getGO(txt);
	        			if (goterm!=null)
	        				ret = strMerge(ret, "GO = '" + goterm + "'");
				}
				else ret = strMerge(ret, "Has GO result");
			}
			if (chkHasIP.isSelected()) {
				String txt = txtIP.getText();
				if (txt.length() > 0) ret = strMerge(ret, "InterPro contains '" + txt + "'");
				else ret = strMerge(ret, "Has InterPro");
			}
			if (chkHasKEGG.isSelected()) {
				String txt = txtKEGG.getText();
				if (txt.length() > 0) ret = strMerge(ret, "KEGG contains '" + txt + "'");
				else ret = strMerge(ret, "Has KEGG");
			}
			if (chkHasPFAM.isSelected()) {
				String txt = txtPFAM.getText();
				if(txt.length() > 0) ret = strMerge(ret, "Pfam contains '" + txt + "'");
				else ret = strMerge(ret, "Has Pfam");
			}
			if (chkHasEC.isSelected()) {
				String txt = txtEC.getText();
				if(txt.length() > 0) ret = strMerge(ret, "EC contains '" + txt + "'");
				else ret = strMerge(ret, "Has EC (enzyme)");
			}
			return ret;
		}
		public void clear() {
			chkHasGO.setSelected(false);
			chkHasIP.setSelected(false);
			chkHasPFAM.setSelected(false);
			chkHasEC.setSelected(false);
			chkHasIP.setSelected(false);
			txtKEGG.setText("");
			txtGO.setText("");
			txtPFAM.setText("");
			txtIP.setText("");
			txtEC.setText("");
			saveSelections();
		}
		private void saveSelections() {
			strOldKEGG = txtKEGG.getText();
			strOldPFAM = txtPFAM.getText();
			strOldEC = txtEC.getText();
			strOldGO = txtGO.getText();
			strOldIP = txtIP.getText();
			
			bOldKEGGSel = chkHasKEGG.isSelected();
			bOldPFAMSel = chkHasPFAM.isSelected();
			bOldECSel = chkHasEC.isSelected();
			bOldGOSel = chkHasGO.isSelected();
			bOldIPSel = chkHasIP.isSelected();
		}
		
		private void restoreSelections() {	
			txtKEGG.setText(strOldKEGG);
			chkHasKEGG.setSelected(bOldKEGGSel);
			txtPFAM.setText(strOldPFAM);
			chkHasPFAM.setSelected(bOldPFAMSel);
			txtEC.setText(strOldEC);
			chkHasEC.setSelected(bOldECSel);
			txtEC.setText(strOldGO);
			chkHasEC.setSelected(bOldGOSel);
			txtEC.setText(strOldIP);
			chkHasEC.setSelected(bOldIPSel);
		}
	
		private JButton btnOK = null;
		private JButton btnCancel = null;
	
		private JCheckBox chkHasKEGG = null;
		private JCheckBox chkHasPFAM = null;
		private JCheckBox chkHasEC = null;
		private JCheckBox chkHasGO = null;
		private JCheckBox chkHasIP = null;
		
		private JTextField txtKEGG = null;
		private JTextField txtPFAM = null;
		private JTextField txtEC = null;
		private JTextField txtGO = null;
		private JTextField txtIP = null;
		
		private String strOldKEGG = "";
		private String strOldPFAM = "";
		private String strOldEC = "";
		private String strOldGO = "";
		private String strOldIP = "";
		
		private boolean bOldKEGGSel = false;
		private boolean bOldPFAMSel = false;
		private boolean bOldECSel = false;
		private boolean bOldGOSel = false;
		private boolean bOldIPSel = false;
	} // end attributePanel
	
	/**********************************************************
	 * SpeciesSelectionPanel
	 */
	private class SpeciesPanel extends JPanel {
		private static final long serialVersionUID = 3047222803816003422L;
		public SpeciesPanel() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
			
			JPanel headerPanel = new JPanel();
			headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
			headerPanel.setBackground(Color.white);
			
			JLabel theHeader = new JLabel("<HTML><H2>Filter on species</H2></HTML>");
			theHeader.setBackground(Color.white);
			theHeader.setAlignmentX(CENTER_ALIGNMENT);
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			headerPanel.setMaximumSize(headerPanel.getPreferredSize());
			headerPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(Box.createVerticalStrut(20));
			add(headerPanel);
			add(Box.createVerticalStrut(10));
			
			add(createSelectPanel());
			
			btnOK = new JButton("Accept");
			btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSelections();
					showMain();
					setVisible(false);
				}
			});
			btnCancel = new JButton("Discard");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreSelections();
					showMain();
					setVisible(false);
				}
			});
			JButton btnHelp = new JButton("Help");
			btnHelp.setBackground(Globals.HELPCOLOR);
			btnHelp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
						UserPrompt.displayHTMLResourceHelp(theMainFrame, 
						"Basic DB Hits Query", "html/viewSingleTCW/BasicQueryHitSpecies.html");
				}
			});
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(Color.white);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(btnOK);
			buttonPanel.add(Box.createHorizontalStrut(10));
			buttonPanel.add(btnCancel);
			buttonPanel.add(Box.createHorizontalStrut(10));
			
			buttonPanel.add(Box.createHorizontalGlue());
			buttonPanel.add(btnHelp);
			buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
			buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(buttonPanel);
			setVisible(false);
			
			saveSelections();
		}
		private JPanel createSelectPanel() {
			sl = new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					update();
				}
			};
			lInc = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					incExcList.removeSelectedValues();
					update();
				}
			};
			lEx = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					incExcList.addValues(speciesList.isAllWords(), speciesList.getSelectedValues());
					update();
				}
			};
			lUpdate = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					update();
				}
			};
			
			JPanel panel = new JPanel();			
			panel.setBackground(BGCOLOR);
			
			speciesList = new SpeciesListPanel(SpeciesListPanel.LIST_ONLY, "Species List");
			incExcList = new SpeciesListPanel(SpeciesListPanel.MODE_SEL, "");
			
			speciesList.addChangeListener(sl);
			incExcList.addChangeListener(sl);
			incExcList.addActionListenerForModeSelect(lUpdate);
			
			movePanel = new SpeciesMovePanel();
			movePanel.addActionListenerForDel(lInc);
			movePanel.addActionListenerForAdd(lEx);
			
			panel.add(speciesList);
			panel.add(movePanel);
			panel.add(incExcList);
			panel.setMaximumSize(panel.getPreferredSize());
			panel.setMinimumSize(panel.getPreferredSize());
			
			speciesList.addValues(true, metaData.getSpecies());
			update();
			
			return panel;
		}
		// Find
		private void refreshListFind(String str) {
			int type = speciesList.getWordType();
			if (str.trim().equals("")) {
				refreshListWord(type); // If subset shown, can blank textfield and Find to get full set
				return;
			}
			speciesList.setStatus("Building list for prefix " + str);
		
			Vector <String> species = metaData.getSpecies(); // always start with full list
			if (type==2 && twoList!=null) species = twoList;
			else if (type==1 && oneList!=null) species = oneList;
			
			Vector <String> list = new Vector <String> ();
		
			for (String s : species) {
				if (s.startsWith(str)) list.add(s);  //.matches("(?i)" + s + ".*")
			}
			if (list.size()>0) {
				speciesList.replaceValues(list);
				update();
			}
			else speciesList.setStatus("No species starting with '" + str + "'");
		}
		// change word type
		private void refreshListWord(int type) {
			Vector <String> species = metaData.getSpecies();
			if (type==3) {
				speciesList.replaceValues(species);
				update();
				return;
			}
			if (type==2 && twoList!=null) {
				speciesList.replaceValues(twoList);
				update();
				return;
			}
			if (type==1 && oneList!=null) {
				speciesList.replaceValues(oneList);
				update();
				return;
			}
			speciesList.setStatus("Building list...");
			HashMap <String, Integer> sub = new HashMap <String, Integer> ();
			for (String s : species) {
				String [] tok = s.split(" ");
				if (tok.length==0) continue;
				
				String key;
				if (type==1 || tok.length==1) key = tok[0];
				else key = tok[0] + " " + tok[1];
				if (!sub.containsKey(key)) sub.put(key, 1);
				else sub.put(key, sub.get(key)+1);
			}
			Vector <String> list = new Vector <String> ();
			for (String s : sub.keySet()) {
				String x = s + " (" + sub.get(s) + ")";
				list.add(x);
			}
			speciesList.replaceValues(list);
			if (type==1) oneList = list;
			else twoList = list;
			update();
		}
		public String getWhere() {
			String [] species = incExcList.getValAsArray();
			if (species==null || species.length==0) return "";
			
		    boolean speciesInclude = incExcList.isInclude();
		    String speciesSearch = "";
           
        	 	String boolStr = speciesInclude ? "" : "NOT ";
        	 	if (species.length==1) 
        	 		speciesSearch = boolStr + " uq.species=" + '"' + species[0] + '"';
        	 	else {
	            	speciesSearch = boolStr + "(uq.species IN (" + '"'+ species[0] + '"';
	            	for(int x=1; x<species.length; x++)
	            		speciesSearch += ", " + '"' + species[x] + '"';
            		speciesSearch += "))";
        	 	}
            
	        return " AND " + speciesSearch;
		}
		public String getSummary() {
			String [] species = incExcList.getValAsArray();
			if (species==null || species.length==0) return "";
			
			int num = species.length;
			String incex = (incExcList.isInclude()? "Include ":"Exclude ");
			if (num==1) return incex + species[0];
			return incex + num + " species";
		}
		public void clear() {
			incExcList.clear();
		}
		private void update() {
			int incNumSel = speciesList.getNumSelectedElements();
			int exNumSel = incExcList.getNumSelectedElements();

			int incNum = speciesList.getNumSpecies();
			int exNum = incExcList.getNumSpecies();
			
			if(incNumSel > 0) movePanel.setAddEnabled(true);
			else movePanel.setAddEnabled(false);
				
			if(exNumSel > 0) movePanel.setDelEnabled(true);
			else movePanel.setDelEnabled(false);
			
			String statusText = "";
			if(incNum == 0) statusText = "No Species";
			else  statusText = incNum + " Species";
			
			speciesList.setStatus(statusText);
			String modeStr = "";
			if(incExcList.isInclude()) modeStr = "Included";
			else modeStr = "Excluded";
			
			if(exNum == 0) statusText = "No Species " + modeStr;
			else  statusText = exNum + " Species " + modeStr;
			
			incExcList.setStatus(statusText);
		}
		
		public int getNumSelectedSpecies() { return incExcList.getValAsArray().length; }
		
		private void saveSelections() {
			oldSpeList = speciesList.getValAsArray();
			oldInExList = incExcList.getValAsArray();
			oldModeInc = incExcList.isInclude();
			oldWord=speciesList.getWordType();
		}
		private void restoreSelections() {
			speciesList.setAllValues(oldSpeList);
			speciesList.setWordType(oldWord);
			incExcList.setAllValues(oldInExList);
			incExcList.setInclude(oldModeInc);
		}
		
		private JButton btnOK = null, btnCancel = null;
		private SpeciesListPanel speciesList = null, incExcList = null;
		private SpeciesMovePanel movePanel = null;
		private String [] oldSpeList = null;
		private String [] oldInExList = null;
		private boolean oldModeInc = false;
		private int oldWord=3;
		private ListSelectionListener sl = null;
		private ActionListener lInc = null;
		private ActionListener lEx = null;
		private ActionListener lUpdate = null;
		
		private Vector <String> oneList = null;
		private Vector <String> twoList = null;
		
		// class within the species class
		private class SpeciesListPanel extends JPanel {
			private static final long serialVersionUID = -7638735027037420167L;
			public static final int LIST_ONLY = 0;
			public static final int MODE_SEL = 1;
			
			public SpeciesListPanel(int mode, String label) {
				setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
				setBackground(Color.white);
				
				txtCount = new JTextField(10);
				txtCount.setEnabled(false);
				Dimension dim = txtCount.getPreferredSize();
				dim.width = WIDTH;
				dim.height = HEIGHT;
				theModel = new DefaultListModel();
				theList = new JList(theModel);
				listScroll = new JScrollPane(theList);
				listScroll.setPreferredSize(dim);
				listScroll.setMaximumSize(dim);
				
				add(createRadioPanel(mode, label));
				add(txtCount);
				add(Box.createVerticalStrut(5));
				add(listScroll);
				add(Box.createVerticalStrut(5));
				if (mode==SpeciesListPanel.MODE_SEL) add(createClearPanel());
				else add(createHighlightPanel());
				
				setMaximumSize(getPreferredSize());
			}
			private JPanel createRadioPanel(int mode, String label) {
				JPanel retVal = new JPanel();
				retVal.setBackground(BGCOLOR);
				retVal.setLayout(new BoxLayout(retVal, BoxLayout.LINE_AXIS));
				retVal.setAlignmentX(Component.CENTER_ALIGNMENT);
				
				if(mode == LIST_ONLY) {
					chkOne = new JRadioButton("First word");
					chkOne.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							refreshListWord(1);
						}
					});
					chkTwo = new JRadioButton("Two words");
					chkTwo.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							refreshListWord(2);
						}
					});
					chkAll = new JRadioButton("All");
					chkAll.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							refreshListWord(3);
						}
					});
					chkAll.setSelected(true);
					ButtonGroup group = new ButtonGroup();
					group.add(chkOne);
					group.add(chkTwo);
					group.add(chkAll);
					retVal.add(chkOne);
					retVal.add(chkTwo);
					retVal.add(chkAll);
				} else if(mode == MODE_SEL) {
					chkInclude = new JRadioButton("Include");
					chkExclude = new JRadioButton("Exclude");
					chkInclude.setSelected(true);
					ButtonGroup incExGroup = new ButtonGroup();
					incExGroup.add(chkInclude);
					incExGroup.add(chkExclude);
					retVal.add(chkInclude);
					retVal.add(chkExclude);
				}
				Dimension dim = retVal.getPreferredSize();
				dim.width = WIDTH;
				
				retVal.setPreferredSize(dim);
				retVal.setMaximumSize(dim);
				
				return retVal;
			}
			private JPanel createClearPanel() {
				JPanel panel = new JPanel();
				panel.setBackground(BGCOLOR);
				panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				
				btnClearOrFind = new JButton("Clear");
				btnClearOrFind.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						theModel.removeAllElements();
					}
				});
				btnClearOrFind.setMargin(new Insets(0, 0, 0, 0));
				btnClearOrFind.setFont(new Font(btnClearOrFind.getFont().getName(),Font.PLAIN,10));
				
				panel.add(btnClearOrFind);
				
				panel.setMaximumSize(panel.getPreferredSize());
				
				return panel;
			}
			private JPanel createHighlightPanel() {
				JPanel panel = new JPanel();
				panel.setBackground(BGCOLOR);
				panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				
				btnClearOrFind = new JButton("Find");
				btnClearOrFind.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						refreshListFind(txtFilter.getText());
					}
				});
				btnClearOrFind.setMargin(new Insets(0, 0, 0, 0));
				btnClearOrFind.setFont(new Font(btnClearOrFind.getFont().getName(),Font.PLAIN,10));
				
				txtFilter = new JTextField(20);
				panel.add(btnClearOrFind);
				panel.add(Box.createHorizontalStrut(3));
				panel.add(txtFilter);
				
				panel.setMaximumSize(panel.getPreferredSize());
				
				return panel;
			}
			public int getNumSelectedElements() {return theList.getSelectedIndices().length;}
			public int getNumElements() {return theList.getModel().getSize();	}
			public int getNumSpecies() {return theModel.size();}
			public void setStatus(String text) { txtCount.setText(text); }
			public void clear() {theModel.removeAllElements();}
			
			public boolean isInclude() { 
				if(chkInclude != null) return chkInclude.isSelected();
				return false;
			}	
			public void setInclude(boolean include) { 
				if(chkInclude != null) {
					chkInclude.setSelected(include);
					chkExclude.setSelected(!include);
				}
			}
			public void addActionListenerForModeSelect(ActionListener l) {
				if(chkInclude != null) {
					chkInclude.addActionListener(l);
					chkExclude.addActionListener(l);
				}
			}
			public String []  getValAsArray() {
				String [] retVal = new String[theModel.getSize()];
				for(int x=0; x< theModel.getSize(); x++)
					retVal[x] = (String)theModel.getElementAt(x);
				return retVal;
			}
			public void setAllValues(String [] values) {
				theModel.clear();
				for(int x=0; x<values.length; x++) theModel.addElement(values[x]);
			}
			public Vector<String> getSelectedValues() {
				Vector<String> list = new Vector<String> ();
				int [] vals = theList.getSelectedIndices();
				for(int x=0; x<vals.length; x++)
					list.add((String)theModel.getElementAt(vals[x]));
				return list;
			}
			public void removeSelectedValues() {
				Vector<String> saveVals = new Vector<String> ();
				for(int x=0; x<theModel.getSize(); x++) {
					if(!theList.isSelectedIndex(x))
						saveVals.add((String)theModel.elementAt(x));
				}
				theModel.clear();
				for (String v : saveVals) theModel.addElement(v);
			}
			public void replaceValues(Vector<String> theList) {
				theModel.removeAllElements();
				Collections.sort(theList);
				
				for (String v : theList) theModel.addElement(v);
			}
			public void addValues(boolean bAll, Vector<String> vals) {
				Vector<String> list = new Vector<String> ();
				for(int x=0; x < theModel.getSize(); x++) 
					list.add((String)theModel.getElementAt(x));
				theModel.clear();
				
				if (bAll) list.addAll(vals);
				else expand(vals, list);
				
				// remove dups
				HashSet<String> hashSet = new HashSet<String>(list);
				list = new Vector<String> (hashSet);
				hashSet.clear();
				
				Collections.sort(list);
				for (String v : list) theModel.addElement(v);
				list.clear();
			}
			private Vector <String> expand( Vector<String> vals, Vector <String> list) {
				Pattern pat =  Pattern.compile("(.*)\\((\\d+)\\)$"); 
				Vector <String> species = metaData.getSpecies();
				for (String v : vals) {
					Matcher x = pat.matcher(v);
					int d=0;
					String f="";
					if (x.find()) {
						f = x.group(1).trim();
						d = Integer.parseInt(x.group(2));
					}
					else System.err.println("TCW error -- " + v);
				
					int cnt=0;
					for (String s : species) {
						if (d==cnt) break;
						if (s.startsWith(f)) {
							list.add(s);
							cnt++;
						}
					}
				}
				return list;
			}
			public boolean isAllWords() { return chkAll.isSelected();}
			public int getWordType() { 
				if (chkAll.isSelected()) return 3;
				if (chkTwo.isSelected()) return 2;
				return 1;
			}
			public void setWordType(int x) {
				chkOne.setSelected(x==1);
				chkTwo.setSelected(x==2);
				chkAll.setSelected(x==3);
			}
			public void addChangeListener(ListSelectionListener l) { theList.addListSelectionListener(l); }
			
			private final static int WIDTH = 300;
			private final static int HEIGHT = 400;
			private JTextField txtCount = null;
			private JList theList = null;
			private DefaultListModel theModel = null;
			private JScrollPane listScroll = null;
			
			private JButton btnClearOrFind = null;
			private JTextField txtFilter = null;
			private JRadioButton chkInclude = null, chkExclude = null;
			private JRadioButton chkOne=null, chkTwo=null, chkAll=null;
		} // end SpeciesListPanel
	
		private class SpeciesMovePanel extends JPanel {
			private static final long serialVersionUID = 6922755071089126369L;
			
			public SpeciesMovePanel() {
				setBackground(Color.white);
				setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
				btnAdd = new JButton("Add");
				btnAdd.setBackground(Color.white);
				btnDel = new JButton("Del");
				btnDel.setBackground(Color.white);
				add(new JLabel("Right-->"));
				add(btnAdd);
				add(Box.createVerticalStrut(20));
				add(new JLabel("Left"));
				add(btnDel);
			}
			
			public void setAddEnabled(boolean enabled) { btnAdd.setEnabled(enabled); }
			public void setDelEnabled(boolean enabled) { btnDel.setEnabled(enabled); }
			
			public void addActionListenerForAdd(ActionListener l) { btnAdd.addActionListener(l); }
			public void addActionListenerForDel(ActionListener l) { btnDel.addActionListener(l); }
			
			private JButton btnAdd = null, btnDel = null;
		}
	} // End Species 
	
	/****************************
	 *  CountPanel: RPKM and DE p-values
	 */
	private class CountPanel extends JPanel {
		private static final long serialVersionUID = -1413393170982001945L;
		public CountPanel() {			
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
			
			JPanel headerPanel = new JPanel();
			headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
			headerPanel.setBackground(Color.white);
			
			String header = "<HTML><H2>Filter on RPKM";
			header += (pvalColNames==null) ? "</H2>" : 
				" and/or DE p-values</H2>"  ;
			header += (libColNames.length <=1) ? "</HTML>"  :
				"<p>Applies to at least one sequence with the associated Hit</HTML>";
			JLabel theHeader = new JLabel(header);
			theHeader.setBackground(Color.white);
			theHeader.setAlignmentX(CENTER_ALIGNMENT);
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			headerPanel.setMaximumSize(headerPanel.getPreferredSize());
			headerPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(Box.createVerticalStrut(30));
			add(headerPanel);
			add(Box.createVerticalStrut(30));
			
			add(createCntSelectPanel());
			
			add(Box.createVerticalStrut(30));
			btnOK = new JButton("Accept");
			btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSelections();
					showMain();
					setVisible(false);
				}
			});
			btnCancel = new JButton("Discard");
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreSelections();
					showMain();
					setVisible(false);
				}
			});
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(Color.white);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(btnOK);
			buttonPanel.add(Box.createHorizontalStrut(10));
			buttonPanel.add(btnCancel);
			buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
			buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(buttonPanel);
			setVisible(false);
		}
		
		private JPanel createCntSelectPanel() {
			JPanel panel = new JPanel();
			panel.setBackground(BGCOLOR);
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			panel.setAlignmentX(Component.CENTER_ALIGNMENT);
			
		/** Make adjacent  RPKM, DE panels **/
			JPanel libPanel = createCntLibPanel();
			JPanel pvalPanel = createCntPvalPanel();
			
			// Add  panels side by side in columnSelectPanel
			selectPanel = Static.createRowPanel();
			
			JPanel subP = Static.createRowPanel();
			subP.add(libPanel);
			if (pvalPanel!=null) {
				subP.add(Box.createHorizontalStrut(60));
				subP.add(pvalPanel);
			}
			
			subP.setMinimumSize(subP.getPreferredSize());
			subP.setMaximumSize(subP.getPreferredSize());
			subP.setAlignmentX(CENTER_ALIGNMENT);
		
			if ((libColNames != null && libColNames.length > 20) ||  
				(pvalColNames != null && pvalColNames.length > 20))
			{
				JScrollPane sp = new JScrollPane(subP);
				selectPanel.add(sp);
			}
			else
			{
				selectPanel.add(subP);
			}
			selectPanel.setMaximumSize(selectPanel.getPreferredSize());
			selectPanel.setAlignmentX(CENTER_ALIGNMENT);
		
			saveSelections();
			return selectPanel;
		}
		private JPanel createCntLibPanel() {
			JPanel libPanel = Static.createPagePanel();
			libPanel.setAlignmentY(TOP_ALIGNMENT);
			
		// RPKM >= [] for [Every/Any] selected
			JPanel libRow = Static.createRowPanel();
			libRow.add(new JLabel("RPKM"));
			libRow.add(new JLabel(">="));
			
			txtRPKM  = new JTextField(DOUBLE_SIZE);
			txtRPKM.setMaximumSize(txtRPKM.getPreferredSize());
			txtRPKM.setText(Double.toString(DEFAULT_RPKM));
			libRow.add(txtRPKM);
			
			libRow.add(new JLabel("from"));
			String [] libLabels = {"EVERY", "ANY"};
			libCombo = new JComboBox(libLabels);
			libRow.add(libCombo);
			libRow.add(new JLabel("selected"));
			
			libPanel.add(libRow);
			libPanel.add(new JSeparator());
			
			chkLibColNames = new JCheckBox[libColNames.length];
			for(int x=0; x<chkLibColNames.length; x++) chkLibColNames[x] = null;
			
			// List of RPKM value
			for(int x=0; x<libColNames.length; x++) {
				chkLibColNames[x] = new JCheckBox(libColNames[x], false); // RPKM not on by default
				chkLibColNames[x].setAlignmentX(LEFT_ALIGNMENT);
				
				JPanel libx = Static.createRowPanel();
				libx.add(chkLibColNames[x]);
				libPanel.add(libx);
			}
			libPanel.add(Box.createVerticalStrut(10));
			
			// Check/uncheck
			JPanel libChkRow = Static.createRowPanel();
			final JCheckBox chkAllLib = new JCheckBox("check/uncheck all", false);
			
			chkAllLib.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					boolean check = chkAllLib.isSelected();
					for(int x=0; x<chkLibColNames.length; x++)
						chkLibColNames[x].setSelected(check);
				}
			});
			libChkRow.add(chkAllLib);
			libChkRow.setBorder(BorderFactory.createLineBorder(Color.BLACK));	
			libChkRow.setMinimumSize(libChkRow.getPreferredSize());
			libChkRow.setMaximumSize(libChkRow.getPreferredSize());
			
			libPanel.add(libChkRow);
			libPanel.add(Box.createVerticalStrut(5));
			
			libPanel.setMinimumSize(libPanel.getPreferredSize());
			libPanel.setMaximumSize(libPanel.getPreferredSize());
			libPanel.setAlignmentX(CENTER_ALIGNMENT); 
			
			return libPanel;
		}
		private JPanel createCntPvalPanel() {
			if(pvalColNames == null || pvalColNames.length == 0) return null;
			
			chkPvalColNames = new JCheckBox[pvalColNames.length];
			for(int x=0; x<chkPvalColNames.length; x++) chkPvalColNames[x] = null;
			
			JPanel pvalPanel = Static.createPagePanel();
			pvalPanel.setAlignmentY(TOP_ALIGNMENT);
				
			// DE < [0.05] for [Every/Any] selected
			JPanel row = Static.createRowPanel();
			row.add(new JLabel("P-val"));
			row.add(new JLabel("<"));
			
			txtDE  = new JTextField(DOUBLE_SIZE);
			txtDE.setMaximumSize(txtDE.getPreferredSize());
			txtDE.setText(Double.toString(DEFAULT_DE));
			row.add(txtDE);
			
			row.add(new JLabel("from"));
			String [] labels = {"EVERY", "ANY"};
			pvalCombo = new JComboBox(labels);
			row.add(pvalCombo);
			row.add(new JLabel("selected"));
			
			pvalPanel.add(row);
			
			row = Static.createRowPanel();
			chkUpDE = new JRadioButton("Up"); chkUpDE.setBackground(Color.white);
			chkDownDE = new JRadioButton("Down"); chkDownDE.setBackground(Color.white);
			chkEitherDE = new JRadioButton("Either"); chkEitherDE.setBackground(Color.white);
			ButtonGroup allbg = new ButtonGroup();
			allbg.add(chkUpDE); 
			allbg.add(chkDownDE); 
			allbg.add(chkEitherDE);
			chkEitherDE.setSelected(true);
			row.add(chkUpDE);
			row.add(chkDownDE);
			row.add(chkEitherDE);
		
			pvalPanel.add(row);
			pvalPanel.add(new JSeparator());
			
			// List of DE p-values
			for(int x=0; x<pvalColNames.length; x++) {
				chkPvalColNames[x] = new JCheckBox(pvalColNames[x], false); 
				chkPvalColNames[x].setAlignmentX(LEFT_ALIGNMENT);
				chkPvalColNames[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
					}
				});
			
				JPanel rowx = Static.createRowPanel();
				rowx.add(chkPvalColNames[x]);
				pvalPanel.add(rowx);
			}
			pvalPanel.add(Box.createVerticalStrut(10));
			
			// check/uncheck
			JPanel pvalChkRow = Static.createRowPanel();
			final JCheckBox chkAllPval = new JCheckBox("check/uncheck all", false);
			
			chkAllPval.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					boolean check = chkAllPval.isSelected();
					for(int x=0; x<chkPvalColNames.length; x++)
						chkPvalColNames[x].setSelected(check);
					;
				}
			});
			pvalChkRow.add(chkAllPval);
			pvalChkRow.setBorder(BorderFactory.createLineBorder(Color.BLACK));	
			pvalChkRow.setMinimumSize(pvalChkRow.getPreferredSize());
			pvalChkRow.setMaximumSize(pvalChkRow.getPreferredSize());
			
			pvalPanel.add(pvalChkRow);
			pvalPanel.add(Box.createVerticalStrut(5));
			
			pvalPanel.setMinimumSize(pvalPanel.getPreferredSize());
			pvalPanel.setMaximumSize(pvalPanel.getPreferredSize());
			pvalPanel.setAlignmentX(CENTER_ALIGNMENT); 
			
			return pvalPanel;
		}
		public String getWhere() {
			int cntR = countRPKM();
			int cntD = countDE();
			if (cntR==0 && cntD==0) return "";
			String query="";
			
			if (cntR>0) {
	      		String col = " ct." + Globals.LIBRPKM;
	      		
	      		String cutoff = txtRPKM.getText();
	      		int index = libCombo.getSelectedIndex();
	      		String op = (index==0) ? " AND " : " OR ";
	      		
	            	for(int x=0; x<chkLibColNames.length; x++) 
	            		if (chkLibColNames[x].isSelected()) {
	            			if (!query.equals("")) query+= op; 
	            			query +=  col + libColNames[x] + ">=" + cutoff;
	            		}
	            	query = " AND (" + query + ")";
			}
			if (cntD>0) {
				String query2="";
	          	
	          	String cutoff = txtDE.getText();
	          	String prefix = "ct. "+ Globals.PVALUE;
	          	String colStart = " (abs(" + prefix;
	          	String colEnd =                         ") <" + cutoff;
	          
	      		int index = pvalCombo.getSelectedIndex();
	      		String op = (index==0) ? " AND " : " OR ";
	      		
	          	for(int x=0; x<pvalColNames.length; x++) {
	          		if (chkPvalColNames[x].isSelected()) {
	          			if (!query2.equals("")) query2+= op; 
	          			query2 += colStart + pvalColNames[x] + colEnd;
	          			
	          			if (chkUpDE.isSelected()) 
	    	          			query2 += " and " + prefix + pvalColNames[x] + ">0 )";
	    	          		else if (chkDownDE.isSelected()) 
	    	          			query2 += " and " + prefix + pvalColNames[x] +"<0 )";
	    	          		else 
	    	          			query2 += " )";
	          		}
	          	}
	          	query += " AND (" + query2 + ")";
			}   
			return query;
		}
		public String getSummary() { 
			String ret = "";
			int cntR = countRPKM();
			int cntD = countDE();
			if (cntR>0) {
				String list="";
				int index = libCombo.getSelectedIndex();
	      		String op = (index==0) ? "&" : "|";
	      		
				for(int x=0; x<libColNames.length; x++) 
	          		if (chkLibColNames[x].isSelected()) {
	          			if (!list.equals("")) list += op;
	          			list += libColNames[x];
	          		}
				ret = "RPKM>=" + txtRPKM.getText() + " " + list;
			}
			if (cntD>0) {
				String list="";
				int index = pvalCombo.getSelectedIndex();
	      		String op = (index==0) ? "&" : "|";
	      		
				for(int x=0; x<pvalColNames.length; x++) 
	          		if (chkPvalColNames[x].isSelected()) {
	          			if (!list.equals("")) list += op;
	          			list += pvalColNames[x];
	          		}
				if (!ret.equals("")) ret += "; ";
				String reg = "";
				if (chkUpDE.isSelected())  reg = " (Up)";
				else if (chkDownDE.isSelected())  reg = " (Down)";
				ret += " P-val<" + txtDE.getText() + " " + list + reg;
			}
			return ret;
		}
		public void clear() {
			for (int x=0; x<libColNames.length; x++)
				chkLibColNames[x].setSelected(false);
			if (pvalColNames!=null) {
				for (int x=0; x<pvalColNames.length; x++)
					chkPvalColNames[x].setSelected(false);
			}
			if (chkEitherDE!=null) chkEitherDE.setSelected(true);
			if (txtRPKM!=null) txtRPKM.setText(Double.toString(DEFAULT_RPKM));
			if (txtDE!=null) txtDE.setText(Double.toString(DEFAULT_DE));
			saveSelections();
		}
		private void saveSelections() {
			if(chkLibColNames==null) bSaveLibSelect = null;
			else {
				if (bSaveLibSelect==null)
					bSaveLibSelect = new boolean[chkLibColNames.length];
				for(int x=0; x<bSaveLibSelect.length; x++)
					bSaveLibSelect[x] = chkLibColNames[x].isSelected();
			}
			if(chkPvalColNames==null) bSavePvalSelect = null;
			else {
				if (bSavePvalSelect==null)
					bSavePvalSelect = new boolean[chkPvalColNames.length];
				for(int x=0; x<bSavePvalSelect.length; x++)
					bSavePvalSelect[x] = chkPvalColNames[x].isSelected();
			}
		}
		
		private void restoreSelections() {	
			if(bSaveLibSelect != null)
				for(int x=0; x<bSaveLibSelect.length; x++)
					chkLibColNames[x].setSelected(bSaveLibSelect[x]);
			if(bSavePvalSelect != null)
				for(int x=0; x<bSavePvalSelect.length; x++)
					chkPvalColNames[x].setSelected(bSavePvalSelect[x]);
		}
		private int countRPKM() {
			int cnt=0;
			for(int x=0; x<chkLibColNames.length; x++) 
        			if (chkLibColNames[x].isSelected()) cnt++;
			return cnt;
		}
		private int countDE() {
			if (chkPvalColNames==null) return 0;
			int cnt=0;
			for(int x=0; x<chkPvalColNames.length; x++) 
        			if (chkPvalColNames[x].isSelected()) cnt++;
			return cnt;
		}
		private JButton btnOK = null;
		private JButton btnCancel = null;
	
		private JPanel selectPanel;
		
		private JCheckBox [] chkLibColNames = null;
		private JTextField txtRPKM = null;
		private JComboBox libCombo=null;
		
		private JCheckBox [] chkPvalColNames = null;
		private JTextField txtDE = null;
		private JComboBox pvalCombo=null;
		private JRadioButton chkUpDE=null, chkDownDE=null, chkEitherDE=null;
		
		private boolean [] bSaveLibSelect = null;
		private boolean [] bSavePvalSelect = null;	
	} // end CountPanel
	/*************************************************
	 * Column Select Panel
	 */
	private class ColumnPanel extends JPanel
	{
		private static final long serialVersionUID = -49938519942155818L;
		
		public ColumnPanel(String [] staticNames, boolean isGrp) {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(Color.white);
			setAlignmentX(Component.CENTER_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
			
			JPanel headerPanel = new JPanel();
			headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
			headerPanel.setBackground(Color.white);
			String head="";
			if (isGrp) 
				head = "<HTML><H2>Hit columns</H2></HTML>";
			else 
				head = "<HTML><H2>Sequence columns</H2></HTML>";
			JLabel theHeader = new JLabel(head);
			theHeader.setBackground(Color.white);
			theHeader.setAlignmentX(CENTER_ALIGNMENT);
			headerPanel.add(theHeader);
			headerPanel.add(Box.createVerticalStrut(10));
			headerPanel.setMaximumSize(headerPanel.getPreferredSize());
			headerPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(Box.createVerticalStrut(30));
			add(headerPanel);
			add(Box.createVerticalStrut(10));
			
			createColSelectPanel(staticNames, isGrp);
			add(selectPanel);
			
			add(Box.createVerticalStrut(30));
			btnAccept = new JButton("Accept");
			btnAccept.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSelections();
					showMain();
					setVisible(false);
					theParentTab.tableRefresh(isGrpView);
				}
			});
			btnDiscard = new JButton("Discard");
			btnDiscard.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreSelections();
					showMain();
					setVisible(false);
				}
			});
				
			String label = (isGrp) ? "Accept Seq Columns" : "Accept Hit Columns";
			btnSyncThis = new JButton(label);
			btnSyncThis.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					syncThis();
					
					saveSelections();
					showMain();
					setVisible(false);
					theParentTab.tableRefresh(isGrpView);
				}
			});
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(Color.white);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			
			buttonPanel.add(btnAccept);
			buttonPanel.add(Box.createHorizontalStrut(10));
		
			buttonPanel.add(btnSyncThis);
			buttonPanel.add(Box.createHorizontalStrut(10));
			
			buttonPanel.add(btnDiscard);
				
			buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
			buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
			add(buttonPanel);
			setVisible(false);
		}
		
 		private void createColSelectPanel(String [] staticNames, boolean is) {
 		// initialize check boxes
 			isGrp = is;
			chkStaticColNames = new JCheckBox[staticNames.length];	
			for(int x=0; x<chkStaticColNames.length; x++) chkStaticColNames[x] = null;
			
		/** Static columns -- left panel **/ 
			JPanel staticPanel = Static.createPagePanel();
			staticPanel.setAlignmentY(TOP_ALIGNMENT);
			
			JPanel genRow = Static.createRowPanel();
			genRow.add(new JLabel("General"));
			staticPanel.add(genRow);
			staticPanel.add(Box.createVerticalStrut(5));
			
			for(int x=0; x<staticNames.length; x++) {
				chkStaticColNames[x] = new JCheckBox(staticNames[x], false);
				chkStaticColNames[x].setAlignmentX(LEFT_ALIGNMENT);
				chkStaticColNames[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setOKEnabled();
					}
				});
				genRow = Static.createRowPanel();
				genRow.add(chkStaticColNames[x]);
				if (x==SEQ_STATIC_COLUMNS.length) staticPanel.add(Box.createVerticalStrut(10));
				staticPanel.add(genRow);
			}
			
			JPanel genChkRow = Static.createRowPanel();
			
			final JCheckBox chkGeneral = new JCheckBox("check/uncheck all", false);
			chkGeneral.setAlignmentX(LEFT_ALIGNMENT);
			chkGeneral.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					for(int x=0; x<chkStaticColNames.length; x++)
						chkStaticColNames[x].setSelected(chkGeneral.isSelected());
					setOKEnabled();
				}
			});
			genChkRow.add(chkGeneral);
			genChkRow.setBorder(BorderFactory.createLineBorder(Color.BLACK));	
			genChkRow.setMinimumSize(genChkRow.getPreferredSize());
			genChkRow.setMaximumSize(genChkRow.getPreferredSize());
			staticPanel.add(genChkRow);
			staticPanel.add(Box.createVerticalStrut(2));
			if (isGrp) staticPanel.add(new JLabel("*Hit values of best"));
			else  staticPanel.add(new JLabel("+Hit values or computed from the hit values"));
			staticPanel.setMinimumSize(staticPanel.getPreferredSize());
			staticPanel.setMaximumSize(staticPanel.getPreferredSize());
			staticPanel.setAlignmentX(CENTER_ALIGNMENT);

		/** Make adjacent General, RPKM, DE panels **/
			JPanel libPanel = createColLibPanel();
			JPanel pvalPanel = createColPvalPanel();
			
			// Add three column panels side by side in columnSelectPanel
			selectPanel = Static.createRowPanel();
			
			JPanel subP = Static.createRowPanel();
			subP.add(staticPanel);
			subP.add(Box.createHorizontalStrut(60));
			if (libPanel!=null) {
				subP.add(libPanel);
				if (pvalPanel!=null) {
					subP.add(Box.createHorizontalStrut(60));
					subP.add(pvalPanel);
				}
			}
			subP.setMinimumSize(subP.getPreferredSize());
			subP.setMaximumSize(subP.getPreferredSize());
			subP.setAlignmentX(CENTER_ALIGNMENT);
			
			if ((libColNames != null && libColNames.length > 20) ||  
				(pvalColNames != null && pvalColNames.length > 20))
			{
				JScrollPane sp = new JScrollPane(subP);
				selectPanel.add(sp);
			}
			else
			{
				selectPanel.add(subP);
			}
			selectPanel.setMaximumSize(selectPanel.getPreferredSize());
			selectPanel.setAlignmentX(CENTER_ALIGNMENT);
			
			initSelections();
 		}
		private JPanel createColLibPanel() {
			if(libColNames == null || libColNames.length == 0) return null;
			
			chkLibColNames = new JCheckBox[libColNames.length];
			for(int x=0; x<chkLibColNames.length; x++) chkLibColNames[x] = null;
			
			JPanel libPanel = Static.createPagePanel();
			libPanel.setAlignmentY(TOP_ALIGNMENT);
			
			JPanel libRow = Static.createRowPanel();
			String l = (isGrp) ? "Best RPKM" : "RPKM";
			libRow.add(new JLabel(l));
			libPanel.add(libRow);		
					
			// List of RPKM value
			for(int x=0; x<libColNames.length; x++) {
				chkLibColNames[x] = new JCheckBox(libColNames[x], false); // RPKM not on by default
				chkLibColNames[x].setAlignmentX(LEFT_ALIGNMENT);
				chkLibColNames[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setOKEnabled();
					}
				});
			
				JPanel libx = Static.createRowPanel();
				libx.add(chkLibColNames[x]);
				libPanel.add(libx);
			}
			
			// Check/uncheck
			JPanel libChkRow = Static.createRowPanel();
			final JCheckBox chkAllLib = new JCheckBox("check/uncheck all", false);
			
			chkAllLib.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					boolean check = chkAllLib.isSelected();
					for(int x=0; x<chkLibColNames.length; x++)
						chkLibColNames[x].setSelected(check);
					setOKEnabled();
				}
			});
			libChkRow.add(chkAllLib);
			libChkRow.setBorder(BorderFactory.createLineBorder(Color.BLACK));	
			libChkRow.setMinimumSize(libChkRow.getPreferredSize());
			libChkRow.setMaximumSize(libChkRow.getPreferredSize());
			
			libPanel.add(libChkRow);
			libPanel.add(Box.createVerticalStrut(5));
			
			libPanel.setMinimumSize(libPanel.getPreferredSize());
			libPanel.setMaximumSize(libPanel.getPreferredSize());
			libPanel.setAlignmentX(CENTER_ALIGNMENT); 
			
			return libPanel;
		}
		private JPanel createColPvalPanel() {
			if(pvalColNames == null || pvalColNames.length == 0) return null;
			
			chkPvalColNames = new JCheckBox[pvalColNames.length];
			for(int x=0; x<chkPvalColNames.length; x++) chkPvalColNames[x] = null;
			
			JPanel pvalPanel = Static.createPagePanel();
			pvalPanel.setAlignmentY(TOP_ALIGNMENT);
				
			JPanel row = Static.createRowPanel();
			String l = (isGrp) ? "Best DE" : "DE";
			row.add(new JLabel(l));
			pvalPanel.add(row);		
					
			// List of DE values
			for(int x=0; x<pvalColNames.length; x++) {
				chkPvalColNames[x] = new JCheckBox(pvalColNames[x], false); 
				chkPvalColNames[x].setAlignmentX(LEFT_ALIGNMENT);
				chkPvalColNames[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setOKEnabled();
					}
				});
			
				JPanel rowx = Static.createRowPanel();
				rowx.add(chkPvalColNames[x]);
				pvalPanel.add(rowx);
			}
			
			// check/uncheck
			JPanel pvalChkRow = Static.createRowPanel();
			final JCheckBox chkAllPval = new JCheckBox("check/uncheck all", false);
			
			chkAllPval.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					boolean check = chkAllPval.isSelected();
					for(int x=0; x<chkPvalColNames.length; x++)
						chkPvalColNames[x].setSelected(check);
					setOKEnabled();
				}
			});
			pvalChkRow.add(chkAllPval);
			pvalChkRow.setBorder(BorderFactory.createLineBorder(Color.BLACK));	
			pvalChkRow.setMinimumSize(pvalChkRow.getPreferredSize());
			pvalChkRow.setMaximumSize(pvalChkRow.getPreferredSize());
			
			pvalPanel.add(pvalChkRow);
			pvalPanel.add(Box.createVerticalStrut(5));
			
			pvalPanel.setMinimumSize(pvalPanel.getPreferredSize());
			pvalPanel.setMaximumSize(pvalPanel.getPreferredSize());
			pvalPanel.setAlignmentX(CENTER_ALIGNMENT); 
			
			return pvalPanel;
		}
		/*********************************************************************/
		private void syncThis() {
			boolean [] isChk;
			if (isGrp) isChk = colSeqPanel.getColSelect();
			else isChk = colGrpPanel.getColSelect();
			int index=0;
			
			for(int x=0; x<chkStaticColNames.length; x++)
 				chkStaticColNames[x].setSelected(isChk[index++]);
			
 			if(chkLibColNames != null)
 				for(int x=0; x<chkLibColNames.length; x++)
 					chkLibColNames[x].setSelected(isChk[index++]);
 			if(chkPvalColNames != null)
 				for(int x=0; x<chkPvalColNames.length && index < isChk.length; x++)
 					chkPvalColNames[x].setSelected(isChk[index++]);
		}
		
		// Accept is enabled if nothing is selected
		private void setOKEnabled() {
			boolean enable = false;
			for(int x=0; !enable && x<chkStaticColNames.length; x++)
				enable = chkStaticColNames[x].isSelected();
			if(chkLibColNames != null)
				for(int x=0; !enable && x<chkLibColNames.length; x++)
					enable = chkLibColNames[x].isSelected();
			if(chkPvalColNames != null)
				for(int x=0; !enable && x<chkPvalColNames.length; x++)
					enable = chkPvalColNames[x].isSelected();
			
			btnAccept.setEnabled(enable);
		}
 		public String [] getColNames() {
 			String [] colName = new String[totalColumns];
 			int index = 0;
 			
 			for(int x=0; x<chkStaticColNames.length; x++)
 				colName[index++] = chkStaticColNames[x].getText();
 			if(chkLibColNames != null)
 				for(int x=0; x<chkLibColNames.length; x++)
 					colName[index++] = chkLibColNames[x].getText();
 			if(chkPvalColNames != null)
 				for(int x=0; x<chkPvalColNames.length; x++)
 					colName[index++] = chkPvalColNames[x].getText();
 			
 			return colName;
 		}
 		
 		public boolean [] getColSelect() {
 			boolean [] isChk = new boolean[totalColumns];
 			int index = 0;
 			
 			for(int x=0; x<chkStaticColNames.length; x++)
 				isChk[index++] = chkStaticColNames[x].isSelected();
 			if(chkLibColNames != null)
 				for(int x=0; x<chkLibColNames.length; x++)
 					isChk[index++] = chkLibColNames[x].isSelected();
 			if(chkPvalColNames != null)
 				for(int x=0; x<chkPvalColNames.length; x++)
 					isChk[index++] = chkPvalColNames[x].isSelected();
 			
 			return isChk;
 		}
				
		private void saveSelections() {
			String prefs="";
			for(int x=0; x<nStatic; x++) {
				boolean b = chkStaticColNames[x].isSelected();
				bSaveSelect[x] = b;
				if (b) prefs = Static.combineSummary(prefs, x+"", "\t");
			}
			if(nLib>0) {
				for(int x=0, y=nStatic; x<nLib; x++, y++) {
					boolean b = chkLibColNames[x].isSelected();
					bSaveSelect[y] = b;
					if (b) prefs = Static.combineSummary(prefs, y+"", "\t");
				}
			}
			if (nPval>0) {
				for(int x=0, y=nStatic+nLib; x<nPval; x++, y++) {
					boolean b = chkPvalColNames[x].isSelected();
					bSaveSelect[y] = b;
					if (b) prefs = Static.combineSummary(prefs, y+"", "\t");
				}
			}
			prefsRoot.put(prefLabel, prefs);
		}
		
		private void restoreSelections() {
			for(int x=0; x<nStatic; x++)
					chkStaticColNames[x].setSelected(bSaveSelect[x]);
			if(nLib>0)
				for(int x=0, y=nStatic; x<nLib; x++, y++)
					chkLibColNames[x].setSelected(bSaveSelect[y]);
			if(nPval>0)
				for(int x=0, y=nStatic+nLib; x<nPval; x++, y++)
					chkPvalColNames[x].setSelected(bSaveSelect[y]);
		}
		
		private void initSelections() {
			nStatic = chkStaticColNames.length;
			nLib = (chkLibColNames==null ? 0 : chkLibColNames.length);
			nPval = (chkPvalColNames==null ? 0 : chkPvalColNames.length);
			totalColumns = nStatic + nLib + nPval;
			
			prefsRoot = theMainFrame.getPreferencesRoot();
			prefLabel = theMainFrame.getdbName() + hitPref; // sTCW database name + _hitCol
			String hitCol = prefsRoot.get(prefLabel, null); // sets to null if not set yet
			int cnt=0;
			
			if (hitCol!=null) {
				int offset = nStatic+nLib;
				String [] list = hitCol.split("\t");	
				for (int i=0; i<list.length; i++) {
					int x = Static.getInteger(list[i]);
					if (x<0 || x>=totalColumns) continue; 
					
					cnt++;
					if (x<nStatic) chkStaticColNames[x].setSelected(true);
					else if (x<offset) chkLibColNames[x-nStatic].setSelected(true);
					else chkPvalColNames[x-offset].setSelected(true);
				}
			}
			if (cnt==0) {
				for (int x=0; x<chkStaticColNames.length; x++) {
					if (isOnByDefault(x)) chkStaticColNames[x].setSelected(true);
				}
			}
			bSaveSelect = new boolean [totalColumns];
			saveSelections();
		}

		private JPanel selectPanel = null;
		private JButton btnAccept = null, btnDiscard = null;
		private JButton btnSyncThis = null;
		
		// these change based on isGrpView
		private int totalColumns=0, nStatic=0, nLib=0, nPval=0;
		private JCheckBox [] chkStaticColNames = null;
		private JCheckBox [] chkLibColNames = null;
		private JCheckBox [] chkPvalColNames = null;
		
		private boolean [] bSaveSelect = null;
		private boolean isGrp=true;
		
		private String prefLabel = "";
		private Preferences prefsRoot = null;
	} // end column panel
	/*****************************************************
	 * Check filters
	 */
	private boolean checkFilters() {
		
		return true;
	}
	/****************************************************************
	 * Database query
	 ***************************************************************/
	private void loadDataForTable(boolean isBuild)
	{
		final boolean bBuild=isBuild;
		Thread thread = new Thread(new Runnable() {
		public void run() {
			try {	
				if (bBuild) theParentTab.tableClear();
				
				boolean useSearch = queryPanel.useSearch();
				
				String status = "Querying database. Please Wait...";
				if (queryPanel.isDesc() && useSearch) status += " Description search can be slow";
				else if (queryPanel.chkSpecies.isSelected() &&
						speciesPanel.getNumSelectedSpecies()>0) status += " Species search can be slow";
				else status += " May be slow if getting many of the " + Out.df(totalSeqHitPairs) + " possible results";
				theParentTab.setStatus(status);
				queryPanel.enableAddToTable(false);		
				
				// Search: (1) Do search, if not found, do again with wildcards.
				// (2) Description - only do second wildcard search. (3) LoadFile - only do first exact search.
				boolean firstSearch=true, secondSearch=false;
				if (useSearch) {
					int type = queryPanel.getTypeSearch();
					if (type == 2) {
						firstSearch=false; secondSearch=true;
					}
					else if (type == 3) {
						secondSearch=true;
					}
				}
				
				ArrayList<Object []> results = null;
				if (firstSearch) results = loadFromDatabase(false);
				
				String statusSearch = queryPanel.getSummaryText();
				if(secondSearch && (results == null || results.size()==0)) {
					results = loadFromDatabase(true); // search with wild chars
					statusSearch = queryPanel.getSummaryColumn() + " contains " + statusSearch;
				}
				else if (useSearch) 
					statusSearch = queryPanel.getSummaryColumn() + " = " + statusSearch;
				
				theParentTab.setStatus("Populating table with " + results.size() + " sequence-hit pairs... ");
				
				// create/update status
				boolean useFilter = queryPanel.useFilter();
				filters = status="";
				if (useFilter) {
					status = queryPanel.getSummary();
					if (queryPanel.chkCount.isSelected())   status = strMerge(status, countPanel.getSummary());
					if (queryPanel.chkAnnoDBs.isSelected()) status = strMerge(status, annoDBPanel.getSummary());
					if (queryPanel.chkSpecies.isSelected()) status = strMerge(status, speciesPanel.getSummary());
					if (queryPanel.chkGOetc.isSelected())   status = strMerge(status, goPanel.getSummary());
					filters = status;
					status = "  Filters: " + status;
				}
				if (useSearch) {
					filters = statusSearch + ";" + filters;
					status = "  Search: " + statusSearch + status;
				}
				
				queryPanel.enableAddToTable(true);
				if (bBuild) theParentTab.tableBuild(results, status);
				else theParentTab.tableAdd(results, status);
			} 
			catch (Exception err) {
				queryPanel.enableAddToTable(true);
				theParentTab.setStatus("Error during query");
				JOptionPane.showMessageDialog(null, "Query failed due to unknown reasons ");
				ErrorReport.reportError(err, "Internal error: building hit table");
			}
		}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	/******************************************************************
	 * XXX Database calls
	 */
	/* the following two are called by BasicHitQueryTab */
	 public ArrayList<Object []> loadFromDatabase (boolean bSecond) throws Exception
	 {
        ArrayList<Object []> resultList = null;
        try {
        		String whereBegin =  		// more clauses added below. 
        			" FROM pja_db_unitrans_hits as tn, " +
        			"     pja_db_unique_hits as uq, " +
        			"     contig as ct" + 
            		" WHERE tn.DUHID = uq.DUHID" +
            		" AND   tn.CTGID = ct.CTGID";
        		
            // create columns to search on; the order is read in BasicHitQueryTab.HitSeqData
            int numSetFields = (libColNames == null) ? 0 : libColNames.length;
            numSetFields    += (pvalColNames==null)  ? 0 : pvalColNames.length;
            
            int numStaticFields = 17; // read from DB; not same as number of displayed columns
            String fields = 	
            	"tn.contigid, tn.uniprot_id, tn.e_value, tn.percent_id, " +
            	"tn.ctg_start,  tn.ctg_end, tn.prot_start, tn.prot_end, tn.alignment_len," +
            	"tn.filtered, tn.blast_rank, " + 
            	"uq.description, uq.species, uq.dbtype, uq.taxonomy, uq.length, ct.consensus_bases ";
            			
            if (hasGO) {
            		fields += ",uq.goBrief,uq.goList, uq.interpro,uq.kegg, uq.pfam, uq.ec "; 
            		numStaticFields += 6;
            }
            if(libColNames != null) {
            		String col = ", ct." + Globals.LIBRPKM;
	            	for(int x=0; x<libColNames.length; x++) 
	            		fields += col + libColNames[x];
            }
            if(pvalColNames != null) {
            		String col = ", ct." + Globals.PVALUE;
            		for(int x=0; x<pvalColNames.length; x++) 
            			fields += col + pvalColNames[x];
            }
            // build where clause
            String strQuery = "SELECT " + fields + whereBegin + queryPanel.getWhere(bSecond);
           
            if (queryPanel.useFilter()) { 
            		strQuery += queryPanel.getWhere();
            		if (queryPanel.chkCount.isSelected())   strQuery += countPanel.getWhere();
	            if (queryPanel.chkAnnoDBs.isSelected()) strQuery += annoDBPanel.getWhere();
	            if (queryPanel.chkSpecies.isSelected()) strQuery += speciesPanel.getWhere();
	            if (queryPanel.chkGOetc.isSelected())   strQuery += goPanel.getWhere();
            }
            strQuery += " ORDER BY tn.uniprot_id ASC";
 
       // run query
            DBConn dbc = theMainFrame.getNewDBC();
            ResultSet rset = dbc.executeQuery( strQuery ); 
            resultList = new ArrayList<Object []> ();
           
	    		int cnt=0;
            while( rset.next() )
	    		{	
            		int cutoff = queryPanel.getAlign();
            		if (cutoff>0) {
            			int hlen = rset.getInt(16);
            			int halign = Math.abs(rset.getInt(8)-rset.getInt(7)+1);
            			double align = ((double)halign/(double)hlen)*100.0;   			
            			if (align < (double) cutoff) continue;
            		}
	    	        Object [] readBuffer = new Object[numStaticFields + numSetFields];
	    	        
	    	        for (int i=0; i<numStaticFields; i++) {
	    	        		readBuffer[i] = rset.getString( i+1 );
	    	        }	
	    			for(int x=0; x<numSetFields; x++) {
	    				readBuffer[numStaticFields + x] = rset.getDouble(numStaticFields+x+1);
	    			}
	    			resultList.add(readBuffer);
	    			cnt++;
	    			if (cnt==1000) {
	    				theParentTab.setStatus("Loaded " + resultList.size() + " from database...");
	    				cnt=0;
	    			}
	    		}
            rset.close(); dbc.close();
	    		return resultList;
        }
        catch(Exception e) {
        		ErrorReport.reportError(e,"Error: reading database for Basic Hit Query");
        		throw e;
        }
	}
	public String loadFromDatabaseHitSeq(String hitID){
		try {
			DBConn dbc = theMainFrame.getNewDBC();
			LoadFromDB dbLoadObj = new LoadFromDB(dbc, metaData);
			
			SequenceData hitObj = dbLoadObj.loadHitData(hitID);
			
			if (hitObj==null) {
				System.err.println("Error getting sequence for " + hitID);
				return null;
			}
			return hitObj.getSequence();
		}
		catch(Exception e) {ErrorReport.reportError(e,"Error: reading database for hit data");}
		return null;
	}
	 /* the following 3 are for align. The actual load is in seqData.LoadFromDB */
	private HashMap <String, ContigData> loadFromDatabaseSeqObj(Vector <String> seqList){
		HashMap <String, ContigData> seqObjList = new HashMap <String, ContigData> ();
		try {
			DBConn dbc = theMainFrame.getNewDBC();
			LoadFromDB dbLoadObj = new LoadFromDB(dbc, metaData);
			
			for (String seqID : seqList) {
				if (!seqObjList.containsKey(seqID)) {
					ContigData ctgObj = dbLoadObj.loadDetail(seqID);
					seqObjList.put(seqID, ctgObj);
				}
			}		 
			return seqObjList;
		}
		catch(Exception e) {ErrorReport.reportError(e,"Error: reading database for sequence data");}
		return null;
	}
	private HashMap <String, SequenceData> loadFromDatabaseHitObj(Vector <String> hitList){
		HashMap <String, SequenceData> hitObjList = new HashMap <String, SequenceData> ();
		try {
			DBConn dbc = theMainFrame.getNewDBC();
			LoadFromDB dbLoadObj = new LoadFromDB(dbc, metaData);
			
			for (String hitID : hitList) {
				if (!hitObjList.containsKey(hitID)) {
					SequenceData hitObj = dbLoadObj.loadHitData(hitID);
					hitObjList.put(hitID, hitObj);
				}
			}		 
			return hitObjList;
		}
		catch(Exception e) {ErrorReport.reportError(e,"Error: reading database for hit data");}
		return null;
	}
	private Vector <BlastHitData> loadFromDatabaseBlastObj(
			Vector <String> seqList, Vector <String> hitList){
		Vector <BlastHitData> hitObjList = new Vector <BlastHitData> ();
		try {
			DBConn dbc = theMainFrame.getNewDBC();
			LoadFromDB dbLoadObj = new LoadFromDB(dbc, metaData);
			
			for (int i=0; i<seqList.size(); i++) {
				BlastHitData hitObj = dbLoadObj.loadBlastHitData(seqList.get(i), hitList.get(i));
				hitObjList.add(hitObj);
			}		 
			return hitObjList;
		}
		catch(Exception e) {ErrorReport.reportError(e,"Error: reading database for hit data");}
		return null;
	}
	private String strMerge(String t1, String t2) {
		if (t1!="" && t2!="") return t1 + "; " + t2;
		if (t1!="") return t1;
		return t2;
	}
	// End database query
	
	// Columns for table
	private String [] seqStaticColNames = null;
	private String [] grpStaticColNames = null;
	private String [] libColNames = null;
	private String [] pvalColNames = null;

	private boolean isGrpView=true;
	
	// main panel
	private MainQueryPanel queryPanel = null;
	private JPanel alignPanel = null;
	
	// sub panels
	private ColumnPanel colSeqPanel = null;
	private ColumnPanel colGrpPanel = null;
	private AnnoDBPanel annoDBPanel = null;
	private SpeciesPanel speciesPanel = null;
	private GOetcPanel goPanel = null;
	private CountPanel countPanel = null;

	private BasicHitQueryTab theParentTab = null;
	private STCWFrame theMainFrame = null;
	private MetaData metaData = null;
	private boolean hasGO=false;
	private JButton btnFindFile = null;
	private int totalSeqHitPairs=0;
	private String filters="";
}
