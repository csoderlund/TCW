package sng.viewer.panels.Basic;

/**************************************************
 * Creates the Top Row of buttons and the Filter panel
 * 
 * query: go_info for everything except:
 * 		pja_gotree for making the 'show tree' view and DEtrim
 * 		pja_unitrans_go for selecting sequences to view
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import sng.database.Globals;
import sng.database.MetaData;
import sng.util.Tab;
import sng.viewer.STCWFrame;

import util.database.DBConn;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileRead;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;

import util.ui.ButtonComboBox;
import util.ui.UserPrompt;

public class BasicGOFilterTab extends Tab {
	private static final long serialVersionUID = 5545816581105885864L;
	
	private final String helpHTML =  Globals.helpDir + "BasicQueryGO.html";
			
	private boolean doDEtrim=true; // this seems to work, but doesn't reduce by much. 
	
	private static final Color BGCOLOR = Globals.BGCOLOR;
	
	private static final int LABEL_CHK_WIDTH1 = 50;
	private static final int LABEL_WIDTH2 = 55;
	private static final String GO_FORMAT = Globalx.GO_FORMAT;
	
	private static final String deColLabel = "Select";
	private static final String DEF_EVAL = "1E-40";
	private static final String DEF_PVAL = "0.05";
	private static final int MIN_LEVEL = 1;
	private static int MAX_LEVEL = 16;
	
	public BasicGOFilterTab(STCWFrame parentFrame) {
		super(parentFrame, null);
		setBackground(Color.white);
		theParentFrame = parentFrame;
			
		nLevelMin = MIN_LEVEL;
		nLevelMax = MAX_LEVEL;
		loadInitData();
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		goTablePanel = new BasicGOTablePanel(theParentFrame, this);
		loadObj = new BasicGOLoadFromDB(parentFrame, this, goTablePanel);
		add(goTablePanel.getColumnPanel());
		
		createMainPanel();
		
		if (deColumnsPanel!=null) {
			deColumnsPanel.setVisible(false);
			setPvalLabel();
			add(deColumnsPanel);
		}
		if (evidCodePanel!=null) {
			evidCodePanel.setVisible(false);
			add(evidCodePanel);
		}
		enableLevels();
		
		add(mainPanel);
	}
	
	/***********************
	 *  MAIN
	 */
	private void createMainPanel() {
		createTopRowPanel();
		createFilterPanel();
		createStatusBar();
		
		mainPanel = Static.createPagePanel();

		mainPanel.add(topRowPanel);
		
		mainPanel.add(filterPanel);
		mainPanel.add(Box.createVerticalStrut(3));
		
		mainPanel.add(txtStatus);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(goTablePanel.getTablePanel());
		mainPanel.add(Box.createVerticalStrut(5)); 
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	}
	private void createStatusBar() {
		txtStatus = new JTextField(400);
		txtStatus.setAlignmentX(LEFT_ALIGNMENT);
		txtStatus.setHorizontalAlignment(JTextField.LEFT);
		txtStatus.setEditable(false);
		txtStatus.setBackground(BGCOLOR);
		txtStatus.setBorder(BorderFactory.createEmptyBorder());
		Dimension dimStat = txtStatus.getPreferredSize();
		dimStat.width = 500;
		txtStatus.setPreferredSize(dimStat);
		txtStatus.setMaximumSize(txtStatus.getPreferredSize());
	}
	/**********************************
	 *  XXX TOP BUTTONS
	 */
	private void createTopRowPanel() {
		topRowPanel = Static.createRowPanel();
		
		btnViewSeqs = new JButton("View Sequences");
		btnViewSeqs.setBackground(Globals.FUNCTIONCOLOR);
		btnViewSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewSequencesFromSelected();
			}
		});
		btnViewSeqs.setEnabled(false);
	
		
        createTopCopy();
        createTopShow();
	    createTopTable();
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, 
						"Basic GO Query", helpHTML);
			}
		});
		JButton btnGoHelp = new JButton("GO Help");
		btnGoHelp.setBackground(Globals.HELPCOLOR);
		btnGoHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, 
						"Go Help", "html/viewSingleTCW/GO.html");
			}
		});
		topRowPanel.add(new JLabel("For selected: "));
		topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnViewSeqs);
		topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnCopy);
		topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnShow);
		topRowPanel.add(Box.createHorizontalStrut(20));
		topRowPanel.add(btnTable);
		topRowPanel.add(Box.createHorizontalStrut(20));
		
		topRowPanel.add( Box.createHorizontalGlue() );
		topRowPanel.add(btnHelp);
		topRowPanel.add(Box.createHorizontalStrut(3));
		topRowPanel.add(btnGoHelp);
		topRowPanel.setMaximumSize(topRowPanel.getPreferredSize());
		topRowPanel.setMinimumSize(topRowPanel.getPreferredSize());
	}
	/**************************************************************/
	private void createTopCopy() {
		final JPopupMenu copypopup = new JPopupMenu();
		
		copypopup.add(new JMenuItem(new AbstractAction("GO term") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String ts = goTablePanel.getSelectedGO();
					if (ts!=null)cb.setContents(new StringSelection(ts), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying gonum"); }
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("GO description") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String ts = goTablePanel.getSelectedGOdesc();
					if (ts!=null)cb.setContents(new StringSelection(ts), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying  description"); }
			}
		}));
		btnCopy = new JButton("Copy...");
		btnCopy.setBackground(Color.WHITE);
		btnCopy.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                copypopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		btnCopy.setEnabled(false);
	}
	/**************************************************************/
	private void createTopShow() {
		final JPopupMenu showpopup = new JPopupMenu();
		showpopup.add(new JMenuItem(new AbstractAction("Hits - assigned") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showHitsForSelected(GOtree.HITS, btnShow);
			}
		}));
		showpopup.add(new JMenuItem(new AbstractAction("Hits - inherited") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showHitsForSelected(GOtree.ALL_HITS, btnShow);
			}
		}));	
		showpopup.add(new JMenuItem(new AbstractAction("Sequence - has hit with GO") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showHitsForSelected(GOtree.HIT_GO_SEQ, btnShow);
			}
		}));	
		showpopup.addSeparator();
		
		showpopup.add(new JMenuItem(new AbstractAction("GO - Ancestor list by level") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showPathsForSelected(GOtree.ANCESTORS, btnShow);
			}
		}));	
		showpopup.add(new JMenuItem(new AbstractAction("GO - Descendant list by level") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showPathsForSelected(GOtree.DESCENDENTS, btnShow);
			}
		}));	
		showpopup.add(new JMenuItem(new AbstractAction("GO - Neighbor list and relation") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showPathsForSelected(GOtree.NEIGHBORS, btnShow);
			}
		}));
		showpopup.add(new JMenuItem(new AbstractAction("GO - Related in table by table order") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showRelatedFromTable(btnShow);
			}
		}));	
		
		showpopup.add(new JMenuItem(new AbstractAction("GO - Ancestor list by distance and relation") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showPathsForSelected(GOtree.ANC_DIST, btnShow);
			}
		}));	
		showpopup.add(new JMenuItem(new AbstractAction("GO - Ancestor path table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showPathsForSelected(GOtree.PATHS, btnShow);
			}
		}));	
		/** the numbering is slightly off - not as useful as the above
		showpopup.add(new JMenuItem(new AbstractAction("GO - Ancestor Tree (slow)") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showPathsForSelected(GOtree.TREE, btnShow);
			}
		}));	
		**/
		
		btnShow = Static.createButton("Show...", false);
		btnShow.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                showpopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
	}
	/************************************************************/
	private void createTopTable() {
		final JPopupMenu tablepopup = new JPopupMenu();
		
		tablepopup.add(new JMenuItem(new AbstractAction("Show Column Stats") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					goTablePanel.statsPopUp("GO " + theStatusStr);
				} catch (Exception er) {ErrorReport.reportError(er, "Error on column stats");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error column stats", null);}
			}
		}));
		tablepopup.addSeparator();
		
		tablepopup.add(new JMenuItem(new AbstractAction("Copy Table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String ts = goTablePanel.makeCopyTableString("\t");
					cb.setContents(new StringSelection(ts), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copy table"); }
			}
		}));
		tablepopup.addSeparator();
		
		tablepopup.add(new JMenuItem(new AbstractAction("Export Table ("+Globalx.TSV_SUFFIX+")") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTable.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.exportTable(btnTable, 0);
					
					enabledFunctions(true);
					btnTable.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		tablepopup.add(new JMenuItem(new AbstractAction("Export SeqID with GOs ("+Globalx.TSV_SUFFIX+")") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTable.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.exportTable(btnTable, 1);
					
					enabledFunctions(true);
					btnTable.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		tablepopup.add(new JMenuItem(new AbstractAction("Export/Merge #Seq ("+Globalx.TSV_SUFFIX+")") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTable.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.exportTable(btnTable, 2);
					
					enabledFunctions(true);
					btnTable.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		
		// Export using GOtree.java
		tablepopup.addSeparator();
		tablepopup.add(new JMenuItem(new AbstractAction("All Ancestors") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					btnTable.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportAllPaths(btnTable, GOtree.ALL_ANCESTORS);
					
					enabledFunctions(true);
					btnTable.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		tablepopup.add(new JMenuItem(new AbstractAction("Longest Paths (slow and large results)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTable.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportAllPaths(btnTable,GOtree.LONGEST_PATHS);
					
					enabledFunctions(true);
					btnTable.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		
		tablepopup.add(new JMenuItem(new AbstractAction("All Paths (slow and very large results)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTable.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportAllPaths(btnTable, GOtree.ALL_PATHS);
					
					enabledFunctions(true);
					btnTable.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		
		btnTable = new JButton("Table...");
		btnTable.setBackground(Color.WHITE);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablepopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		btnTable.setEnabled(false);
	}
	 /*****************************
     *  CONTROL panel -- filters
     */
	private void createFilterPanel() {
		filterPanel = Static.createPagePanel();
				
		filterPanel.add(Box.createVerticalStrut(2));
		filterPanel.add(new JSeparator());
		filterPanel.add(Box.createVerticalStrut(2));	// need for Linux or line too close
		
		btnClearAll = new JButton("Clear All");
		btnClearAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					clear();
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Clear");}
		}});
		btnClearAll.setMargin(new Insets(0, 0, 0, 0));
		btnClearAll.setFont(new Font(btnClearAll.getFont().getName(),Font.PLAIN,10));
		btnClearAll.setAlignmentX(RIGHT_ALIGNMENT);
		
		createFilterRow1Search();
		
		filterPanel.add(Box.createVerticalStrut(3));
		filterPanel.add(new JSeparator());
		filterPanel.add(Box.createVerticalStrut(3));	
		
		createFilterRow2SeqHit(); filterPanel.add(Box.createVerticalStrut(5));
		createFilterRow3Level();  filterPanel.add(Box.createVerticalStrut(5));
		createFilterRow4DE();
		
	    filterPanel.add(Box.createVerticalStrut(3));
		filterPanel.add(new JSeparator());
		filterPanel.add(Box.createVerticalStrut(3));	// need for Linux or line too close
		
		createFilterRow5Results();
		
		enableSections(); 
		filterPanel.setMaximumSize(filterPanel.getPreferredSize());
		filterPanel.setMinimumSize(filterPanel.getPreferredSize());
	}
	private void createFilterRow1Search() {
		JPanel row1 = Static.createRowPanel();
		radUseSearch = Static.createRadioButton("", false);
		radUseSearch.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ae) {
		    	  	enableSections();
		      }
		 });
		lblSearch = createLabel("Search", LABEL_CHK_WIDTH1);
		row1.add(radUseSearch);
		row1.add(lblSearch);
		
		lblGOID = Static.createLabel("GO term", false);
		chkGOID = Static.createRadioButton("", true);
		chkGOID.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ae) {
		    	  	boolean enable = chkGOID.isSelected();
		    	  	lblGOID.setEnabled(enable);
		    	  	lblDesc.setEnabled(!enable);
		      }
		 });
		row1.add(chkGOID);
		row1.add(lblGOID);
		row1.add(Box.createHorizontalStrut(2));
		
		lblDesc = Static.createLabel("Description (Substring)", false);
		chkGODesc =  Static.createRadioButton("", false);
		chkGODesc.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ae) {
		    	  	boolean enable = chkGODesc.isSelected();
		    	  	lblGOID.setEnabled(!enable);
		    	  	lblDesc.setEnabled(enable);
		      }
		 });
		row1.add(chkGODesc);
		row1.add(lblDesc);
		row1.add(Box.createHorizontalStrut(2));
		
		ButtonGroup grp = new ButtonGroup();
		grp.add(chkGOID);
		grp.add(chkGODesc);	
		
		txtSubString = Static.createTextField("", 20);
		row1.add(txtSubString);
		
		btnFindFile = Static.createButton("Load File", false);
		btnFindFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					FileRead fr = new FileRead("GO", FileC.bNoVer, FileC.bNoPrt); // CAS316
					if (fr.run(btnFindFile, "GO File", FileC.dUSER, FileC.fTXT)) {
						loadFile(fr.getRelativeFile());
					}
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Error finding file");}
			}});
		row1.add(btnFindFile);
		row1.add(Box.createHorizontalStrut(30)); // too widen the panel
		add(row1);
		add(Box.createVerticalStrut(5));	
		
		filterPanel.add(row1);
	}
	private void createFilterRow2SeqHit() {
	// row2 Seq-hit  [x] E-value [    ]   [x] #Seqs [   ]     [x] Evidence Codes  o Any o Every 
		JPanel row2 = Static.createRowPanel();
		radUseFilter = Static.createRadioButton("",true);
		radUseFilter.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ae) {
		    	  	enableSections();
		      }
		 });
		lblFilter = createLabel("Filter", LABEL_CHK_WIDTH1);
		row2.add(radUseFilter);
		row2.add(lblFilter);
		
		ButtonGroup useGrp = new ButtonGroup();
		useGrp.add(radUseSearch);
		useGrp.add(radUseFilter);
		
		lblSeqHit = createLabel("Seq-hit:", LABEL_WIDTH2);
		row2.add(lblSeqHit);
		
		// [x] E-value [    ]
		chkUseEval = Static.createCheckBox("E-value<=", false);
    		chkUseEval.addActionListener(new ActionListener() {
  		      public void actionPerformed(ActionEvent ae) {
  		    	  	boolean enable = chkUseEval.isSelected();
  		    	  	txtEvalVal.setEnabled(enable);
  		      }
  		 });
    		txtEvalVal = Static.createTextField(DEF_EVAL, 5, false);
    		row2.add(chkUseEval);
    		row2.add(txtEvalVal);
    		row2.add(Box.createHorizontalStrut(30));
    		
    		// [x] #Seqs [    ]
    		chkUseNSeq = Static.createCheckBox("#Seqs>=", false);
    		chkUseNSeq.addActionListener(new ActionListener() {
  		      public void actionPerformed(ActionEvent ae) {
  		    	  	boolean enable = chkUseNSeq.isSelected();
		    	  	txtNSeqVal.setEnabled(enable);
  		      }
  		 });
    		txtNSeqVal = Static.createTextField("2", 3, false);
    		row2.add(chkUseNSeq);
    		row2.add(txtNSeqVal);
    		row2.add(Box.createHorizontalStrut(30));
    		
    		// [x] [Evidence Codes] 
    		if (ecColumnNames.length>0) {
    			chkUseEC = Static.createCheckBox("", false);
	        	chkUseEC.addActionListener(new ActionListener() {
	    			public void actionPerformed(ActionEvent e) {
	    				btnEC.setEnabled(chkUseEC.isSelected());
	    			}
	    		});   
			
	    		createECPanel();
	    		btnEC = Static.createButton("Evidence Codes", false, Globals.MENUCOLOR);
	    		btnEC.addActionListener(new ActionListener() {
	    			public void actionPerformed(ActionEvent arg0) {
	    				evidCodePanel.setVisible(true);
	    				mainPanel.setVisible(false);
	    			}
	    		});
	    		row2.add(chkUseEC);
	    		row2.add(btnEC);
    		}
    		filterPanel.add(row2);
	}
	private void createFilterRow3Level() {
		JPanel row3 = Static.createRowPanel();
		row3.add(createLabel("", lblFilter.getPreferredSize().width + 
				radUseFilter.getPreferredSize().width));
		row3.add(createLabel("Level:", lblSeqHit.getPreferredSize().width));
		
		// Dropdown
		cmbTermTypes = new ButtonComboBox();
		cmbTermTypes.addItem("Any domain");
		for(int x=0; x<theTermTypes.length; x++)
			cmbTermTypes.addItem(theTermTypes[x]);
		cmbTermTypes.setSelectedIndex(0);
		cmbTermTypes.setMaximumSize(cmbTermTypes.getPreferredSize());
		cmbTermTypes.setMinimumSize(cmbTermTypes.getPreferredSize());
		cmbTermTypes.setBackground(Globals.BGCOLOR);
		row3.add(cmbTermTypes);
		row3.add(Box.createHorizontalStrut(30));
		
		// Specific []
		boolean enable = false;
		lblSpecific = Static.createLabel("Specific ", enable);
		chkLevelSpecific = new JRadioButton();
		chkLevelSpecific.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ae) {
		    	  		enableLevels();
		      }
		 });
		chkLevelSpecific.setBackground(Globals.BGCOLOR);
		
		txtLevelSpecific = Static.createTextField("2", 2, enable);
		txtLevelSpecific.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent ae) {
		    	 	if (badLevel(txtLevelSpecific.getText())) txtLevelSpecific.setText("2");
		     }
		 });
		
		row3.add(chkLevelSpecific);
		row3.add(lblSpecific);
		row3.add(txtLevelSpecific);
		row3.add(Box.createHorizontalStrut(30));
		
		/// Range [] to []
		enable=true;
		lblRange = Static.createLabel("Range ", enable);
		lblTo = Static.createLabel("to", enable);
		chkLevelRange = new JRadioButton();
		chkLevelRange.addActionListener(new ActionListener() {
		      public void actionPerformed(ActionEvent ae) {
		    	  	enableLevels();
		      }
		 });
		chkLevelRange.setBackground(Globals.BGCOLOR);
		
		ButtonGroup grpLevel = new ButtonGroup();
		grpLevel.add(chkLevelSpecific);
		grpLevel.add(chkLevelRange);
		
		chkLevelRange.setSelected(true);
		
		txtLevelMin = Static.createTextField(MIN_LEVEL + "", 2);
		txtLevelMin.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent ae) {
		    	 	if (badLevel(txtLevelMin.getText())) txtLevelMin.setText(MIN_LEVEL+"");
		     }
		 });
		txtLevelMax = Static.createTextField(MAX_LEVEL + "", 2);
		txtLevelSpecific.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent ae) {
		    	 	if (badLevel(txtLevelMax.getText())) txtLevelMax.setText(MAX_LEVEL+"");
		     }
		});
		
		row3.add(chkLevelRange);
		row3.add(lblRange);
		row3.add(txtLevelMin);
		row3.add(Box.createHorizontalStrut(2));
		row3.add(lblTo);
		row3.add(Box.createHorizontalStrut(2));
		row3.add(txtLevelMax);
		row3.add(Box.createHorizontalStrut(30));
		
		// Slim
		enable=false;
		lblSlims = Static.createLabel("Slim", enable);
		chkSlims = Static.createCheckBox("", enable);
		chkSlims.addActionListener(new ActionListener() {
		     public void actionPerformed(ActionEvent ae) {
		    	 	lblSlims.setEnabled(chkSlims.isSelected());
		     }
		});
		if (theParentFrame.getMetaData().hasSlims()) {
			row3.add(chkSlims);
			row3.add(lblSlims);
		}
			
		if (deColumnNames.size()==0) {
			row3.add(Box.createHorizontalGlue());
			row3.add(Box.createHorizontalStrut(15));
			row3.add(btnClearAll);
		}
		filterPanel.add(row3);
	}
	private void createFilterRow4DE() {
		if (deColumnNames.size()==0)  return;
		
		JPanel row4 = Static.createRowPanel();
		row4.add(createLabel("", lblFilter.getPreferredSize().width + 
				radUseFilter.getPreferredSize().width));
		
		row4.add(createLabel("DE:", lblSeqHit.getPreferredSize().width-2));
		
		boolean enable=false;
		chkUseDE = Static.createCheckBox("", enable);
    	chkUseDE.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableDESection(chkUseDE.isSelected());
			}
		});  
    	row4.add(chkUseDE);
    	row4.add(Box.createHorizontalStrut(4));
        	
		lblCutoff = Static.createLabel("Pval<", enable);
		row4.add(lblCutoff);
		txtCutoff = Static.createTextField(DEF_PVAL,4, enable);
	    row4.add(txtCutoff);
	    row4.add(Box.createHorizontalStrut(2));
	     	
    	lblDErow = Static.createLabel(" for ", enable);
    	row4.add(lblDErow);
		row4.add(Box.createHorizontalStrut(2));
		
    	createDEPanel();
    	btnPval = Static.createButton(deColLabel, false, Globals.MENUCOLOR);
		btnPval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				deColumnsPanel.setVisible(true);
				mainPanel.setVisible(false);
			}
		});
		row4.add(btnPval);
		row4.add(Box.createHorizontalStrut(30));
		 
    	lblShow = Static.createLabel("#Seqs", false);
    	String [] type = {"All", "DE", "upDE", "dnDE"};
    	boxShowDE = new ButtonComboBox();
    	boxShowDE.addItems(type);
    	boxShowDE.setEnabled(false);
    	boxShowDE.setSelectedIndex(0);
    
    	boxShowDE.setMaximumSize(boxShowDE.getPreferredSize());
    	boxShowDE.setMinimumSize(boxShowDE.getPreferredSize());
    		
		if (dePvalMap!=null) {
			row4.add(lblShow);
			row4.add(boxShowDE);
		}				
		row4.add(Box.createHorizontalGlue());
		row4.add(Box.createHorizontalStrut(15));
		row4.add(btnClearAll);
	
		filterPanel.add(row4);
		filterPanel.add(Box.createVerticalStrut(5));

		enableDESection(false);
	}
	private void createFilterRow5Results() {
	    	JPanel row5 = Static.createRowPanel();
	    	row5.add(createLabel("  Results", lblFilter.getPreferredSize().width + 
					radUseFilter.getPreferredSize().width));
			
	    	filterPanel.add(Box.createVerticalStrut(5));
		btnBuildTable = new JButton("BUILD TABLE");
		btnBuildTable.setBackground(Globals.FUNCTIONCOLOR);
		btnBuildTable.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				goTablePanel.clear();
				loadBuildStart();
			}
		});
		row5.add(btnBuildTable);
		row5.add(Box.createHorizontalStrut(10));
		
		btnAddTable = new JButton("ADD to TABLE");
		btnAddTable.setBackground(Globals.FUNCTIONCOLOR);
		btnAddTable.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goTablePanel.clearTrim();
				loadBuildStart();
			}
		});
		row5.add(btnAddTable);
		row5.add(Box.createHorizontalStrut(10));
		
		btnSelectColumns = new JButton("Columns");
		btnSelectColumns.setBackground(Globals.MENUCOLOR);
		btnSelectColumns.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				goTablePanel.showColumns();
				mainPanel.setVisible(false);
			}
		});
		row5.add(btnSelectColumns);
	    
		deTrimLabel = new JLabel("DE-trim: ");
		chkComputeDEtrim = new JCheckBox("Compute");
		chkComputeDEtrim.setSelected(false);
		chkComputeDEtrim.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				trimStart();
			}
		});   
		chkComputeDEtrim.setBackground(Globals.BGCOLOR);
		
		chkShowDEtrim = new JCheckBox("Show Only");
		chkShowDEtrim.setSelected(false);
		chkShowDEtrim.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			goTablePanel.toggleTrimmedView(chkShowDEtrim.isSelected());
		}
		});   
		chkShowDEtrim.setBackground(Globals.BGCOLOR);
		
		if (deColumnNames.size() > 0 && doDEtrim)
		{				
			row5.add(Box.createHorizontalStrut(15));
			row5.add(deTrimLabel);
	    		row5.add(chkComputeDEtrim);
			row5.add(chkShowDEtrim);
			enableDEtrim();
		}
		filterPanel.add(row5);
	}
	private void createDEPanel() { 
		deColumnsPanel = Static.createPageCenterPanel();
		
	 	JLabel header = new JLabel("<HTML><H2>Select DE p-value to filter</H2></HTML>");
	 	header.setAlignmentX(Component.CENTER_ALIGNMENT);
	 	header.setMaximumSize(header.getPreferredSize());
	 	header.setMinimumSize(header.getPreferredSize());
	 	deColumnsPanel.add(Box.createVerticalStrut(10));
	    deColumnsPanel.add(header);
    	
	    JPanel deColPanel = Static.createPageCenterPanel();
	    	
	    JPanel row = Static.createRowPanel();
	    ButtonGroup grpDE = new ButtonGroup();
		lblDEAny = Static.createLabel("Any", true);
		row.add(lblDEAny);
		row.add(Box.createHorizontalStrut(2));
		
		chkDEAny = new JRadioButton(); grpDE.add(chkDEAny);
		row.add(chkDEAny);
		
		lblDEAll = Static.createLabel("Every", true);
		row.add(lblDEAll);
		
		chkDEAll = new JRadioButton(); grpDE.add(chkDEAll);
		chkDEAny.setSelected(true);
		row.add(chkDEAll);
		
		chkDEAny.setBackground(Globals.BGCOLOR);
		chkDEAll.setBackground(Globals.BGCOLOR);
		
		deColPanel.add(row);
		deColPanel.add(Box.createVerticalStrut(10));
		
    	chkDEfilter = new JCheckBox[deColumnNames.size()];
    	for(int x=0; x<chkDEfilter.length; x++) {
    		chkDEfilter[x] = new JCheckBox(deColumnNames.get(x));
    		chkDEfilter[x].setBackground(BGCOLOR);
    		chkDEfilter[x].setSelected(true);
    		chkDEfilter[x].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				 	boolean allSelected = true;
				 	for(int x=0; x<chkDEfilter.length && allSelected; x++)
				 		allSelected = chkDEfilter[x].isSelected();
				 	chkSelectDEs.setSelected(allSelected);
				}
			});
    		deColPanel.add(chkDEfilter[x]);
    	}
    	deColPanel.add(Box.createHorizontalStrut(10));
	    	
	    JPanel checkPanel = Static.createRowPanel();
	    chkSelectDEs = new JCheckBox("Check/uncheck all");
    	chkSelectDEs.setSelected(true);
    	chkSelectDEs.setBackground(BGCOLOR);
    	chkSelectDEs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				boolean isSelected = chkSelectDEs.isSelected();
				for(int x=0; x<chkDEfilter.length; x++)
					chkDEfilter[x].setSelected(isSelected);
			}
		});
    	checkPanel.add(chkSelectDEs);
    	checkPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    
    	deColPanel.add(checkPanel);
	    deColPanel.setMaximumSize(deColPanel.getPreferredSize());
    	deColPanel.setMinimumSize(deColPanel.getPreferredSize());
    	
    	deColumnsPanel.add(Box.createVerticalStrut(10));
    	deColumnsPanel.add(deColPanel);
    	
    	// buttons
    	JPanel buttonPanel = Static.createRowCenterPanel();
    	JButton keepButton = new JButton("Accept");
    	keepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setPvalLabel();
				
				deColumnsPanel.setVisible(false);
				mainPanel.setVisible(true);
			}
		});
	    buttonPanel.add(keepButton);
	   
    	JButton discardButton = new JButton("Discard");
    	discardButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deColumnsPanel.setVisible(false);
				mainPanel.setVisible(true);
			}
		});
	   
	 	//buttonPanel.add(Box.createHorizontalStrut(10));
    	//buttonPanel.add(discardButton);
    	buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
    	buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
   
    	deColumnsPanel.add(Box.createVerticalStrut(20));
    	deColumnsPanel.add(buttonPanel);
	}
	
	private void createECPanel() { 
		MetaData md = theParentFrame.getMetaData();
		if (md.getECinDB().size()==0) return;
		String [] ecList = md.getEClist();
		String [] ecDesc = md.getECdesc();
		HashSet <String> ecInDB = md.getECinDB();
		
		evidCodePanel = Static.createPageCenterPanel();		
	 	JLabel header = new JLabel("<HTML><H2>Select EC (evidence code) to filter</H2></HTML>");
	 	header.setAlignmentX(Component.CENTER_ALIGNMENT);
	 	header.setMaximumSize(header.getPreferredSize());
	 	header.setMinimumSize(header.getPreferredSize());
    	
		JPanel ecPanel = Static.createPageCenterPanel();
	   
    	chkECfilter = new JCheckBox[ecList.length];
    	int width=0;
    	for(int x=0; x<chkECfilter.length; x++) {
    		JPanel ecRow = Static.createRowPanel();
    		chkECfilter[x] = new JCheckBox(ecList[x]);
    		if (width==0) width = chkECfilter[x].getPreferredSize().width;
    		chkECfilter[x].setBackground(BGCOLOR);
    		if (ecInDB.contains(ecList[x])) 
    			 chkECfilter[x].setSelected(true);
    		else chkECfilter[x].setEnabled(false);
    		
    		ecRow.add(chkECfilter[x]);
    		Dimension d = chkECfilter[x].getPreferredSize();
	       	if(d.width < 100) ecRow.add(Box.createHorizontalStrut(100 - d.width));
	       		
    	 	ecRow.add(Box.createHorizontalStrut(10));
    	 	JLabel lblDesc = new JLabel(ecDesc[x]);
    	 	lblDesc.setFont(new Font(lblDesc.getFont().getName(),Font.PLAIN,lblDesc.getFont().getSize()));
    	 	ecRow.add(lblDesc);
    	 	
    	 	ecPanel.add(ecRow);
    	}
    	ecPanel.add(Box.createVerticalStrut(5));
	    	
	    JPanel lowerPanel = Static.createRowPanel();
	    chkSelectECs = Static.createCheckBox("Check/uncheck all", true);
    	chkSelectECs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				for(int x=0; x<chkECfilter.length; x++)
					if (chkECfilter[x].isEnabled())
						chkECfilter[x].setSelected(chkSelectECs.isSelected());
			}
		});
    	chkSelectECs.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    	lowerPanel.add(chkSelectECs);
    	lowerPanel.add(Box.createHorizontalStrut(25));
	   
	    JButton keepButton = new JButton("Accept");
	    	keepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				evidCodePanel.setVisible(false);
				mainPanel.setVisible(true);
			}
		});
	    lowerPanel.add(keepButton);
	   
	    ecPanel.add(lowerPanel);	
	    ecPanel.setMaximumSize(ecPanel.getPreferredSize());
	    ecPanel.setMinimumSize(ecPanel.getPreferredSize());
	    	
	    evidCodePanel.add(Box.createVerticalStrut(5));
    	evidCodePanel.add(header);
    	evidCodePanel.add(Box.createVerticalStrut(5));
    	evidCodePanel.add(ecPanel);
    	evidCodePanel.add(Box.createVerticalStrut(20));
	}
	
	/**********************************************************
	 * methods for top row
	 */
	private void viewSequencesFromSelected() {
		btnViewSeqs.setEnabled(false);
		String [] contigNames = loadSelectedContigs();
		btnViewSeqs.setEnabled(true);
		if (contigNames==null) return;
		
		int nGO = goTablePanel.getSelectedRowCount();
		String label = (nGO==1) ? currentGO : nGO + " GOs";

		// always go to Table 
		//if(contigNames.length == 1)
		//	getParentFrame().addContigPage(contigNames[0], this, theTable.getSelectedRow(), STCWFrame.BasicGOQuery);
		//else 
			getParentFrame().loadContigs(label, contigNames, STCWFrame.BASIC_QUERY_MODE_GO );
	}
	/**********************************************
	 * XXX if Up Only or Down Only, only load associated contigs
	 * NOTE: Filter DE<0.05 and hasGO gives many more sequences because it does not include GOseq<0.05
	 * The ones shown are only the ones included in 
	 */
	private String [] loadSelectedContigs() {
		try {
			int [] goIDs = goTablePanel.getSelectedGOs();
			currentGO = String.format(GO_FORMAT, goIDs[0]);
			
			String theQuery = "SELECT DISTINCT(c.contigid) ";
			String deWhere = makeNseqClause(false); // if #Seq DE, upDE or dnDE is selected
			if (deWhere!="") theQuery += sqlForGoSeqs +  deWhere + " and p.gonum";
			else theQuery += sqlForGoSeqs + " p.gonum";
			
			if(goIDs.length == 1) theQuery += " = " + goIDs[0];
			else {
				theQuery += " IN (" + goIDs[0];
				for(int x=1; x<goIDs.length; x++) theQuery += ", " + goIDs[x];
				theQuery += ")";
			}	
			Vector<String> retVal = new Vector<String> ();
			
			DBConn mDB = theParentFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery(theQuery);
			while(rs.next()) {
				retVal.add(rs.getString(1));
			}
			rs.close(); 
			mDB.close();
			
			if (retVal.size()==0) {
				String msg = "No sequences have any hit with this GO assigned or child"; 
				JOptionPane.showMessageDialog(null, msg);
				return null;
			}
			return retVal.toArray(new String[0]);
		}
		catch(Exception e) {
			txtStatus.setText("Query failed");
			JOptionPane.showMessageDialog(null, "Query failed");
			ErrorReport.prtReport(e, "Error getting sequence for go ID");
		}
		return null;
	}
	
	/*************************
	 * Utility display methods
	 */
	private JLabel createLabel(String label, int width) {
		JLabel tmp = new JLabel(label);
		Dimension dim = tmp.getPreferredSize();
		dim.width = width;
		tmp.setPreferredSize(dim);
		tmp.setMaximumSize(tmp.getPreferredSize());
		return tmp;
	}
	
	public void updateTopButtons() {
		boolean enable = (goTablePanel.getSelectedRowCount()>0);
		btnViewSeqs.setEnabled(enable);
		enable = (goTablePanel.getSelectedRowCount()==1);
		btnShow.setEnabled(enable);
		btnCopy.setEnabled(enable);
	}
    /**************************
     * BUILD TABLE
     */
	private void loadBuildStart() {
		txtStatus.setText("Building table....");
		
		updateTopButtons();
		btnBuildTable.setSelected(true);
		btnBuildTable.setEnabled(false);
		
		chkComputeDEtrim.setSelected(false);
		chkShowDEtrim.setSelected(false); 
		enableDEtrim();
		
		btnBuildTable.setSelected(false);
		
		try {
			checkIFvalues();
			
			txtStatus.setText("Performing query - please wait");
			loadObj.runQuery(BasicGOLoadFromDB.BUILD);
		}
		catch(Exception e) {
			txtStatus.setText("Query failed");
			JOptionPane.showMessageDialog(null, "Query failed ");
			ErrorReport.prtReport(e, "Error when building table");
		}
	}
	// called from thread when finished
	public void loadBuildFinish(int rowadd, int rowcnt) {
		String msg;
		if (rowadd==rowcnt)
			msg = String.format("Results: %,d  GOs   %s", rowcnt, theStatusStr);
		else 
			msg = String.format("Results: %,d (add %,d) GOs   %s", rowcnt, rowadd, theStatusStr);
		txtStatus.setText(msg);
	
		enableDEtrim();
		
		goTablePanel.buildRowMap();
		
		btnBuildTable.setSelected(false);
		btnBuildTable.setEnabled(true);
		btnTable.setEnabled(goTablePanel.getRowCount() > 0);
	}
	public void deleteFinish(int rowcnt) {
		txtStatus.setText(String.format("Results: %,d GOs", rowcnt));
		chkComputeDEtrim.setSelected(false);
		chkShowDEtrim.setSelected(false);
		enableDEtrim();
	}
	
	private void checkIFvalues() {
		try {
			if(chkLevelRange.isSelected()) {
				nLevelMin = Integer.parseInt(txtLevelMin.getText());
				nLevelMax = Integer.parseInt(txtLevelMax.getText());
			}
			else {
				nLevelMin = nLevelMax = Integer.parseInt(txtLevelSpecific.getText());
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Leval(s) must be numeric -- set to defaults");
			nLevelMin = MIN_LEVEL;
			nLevelMax = MAX_LEVEL;
		}
		
		try {
			if (chkUseEval.isSelected()) {
				Double.parseDouble(txtEvalVal.getText());
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Best E-val must be numeric - set to default");
			txtEvalVal.setText(DEF_EVAL);
		}
		
		try {
			if (chkUseNSeq.isSelected()) {
				Integer.parseInt(txtNSeqVal.getText());
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "#Seq must be numeric - set to default");
			txtNSeqVal.setText("2");
		}
	}
	
	// called by Building query
	public String makeQuerySelectClause() {
		String theQuery = goTablePanel.getColumns(); 
		String theWhereStr="";
		
		theStatusStr="";
		String searchStr = (radUseSearch.isSelected()) ? txtSubString.getText() : "";
		singleMode = false;
		if(searchStr.length() > 0) {
			singleMode = true;
			if(chkGOID.isSelected()) {
				String theVal = searchStr.replaceAll("[A-Za-z:]", "");
				
				try {
					if (loadList!=null && searchStr.startsWith(loadStart)) { // gonums checked in loadFile
						theStatusStr = "GO Term=" + searchStr;
						String x = Static.addQuoteDBList(loadList);
						theWhereStr = " where go_info.gonum  IN ("  + x + ")"; 
					}
					else {
						theStatusStr="GO Term=" + String.format(GO_FORMAT, Integer.parseInt(theVal));
						theWhereStr= " where go_info.gonum = " + theVal ;
					}
				}
				catch(Exception e) {
					theStatusStr="    *****Invalid GO ID: '" + searchStr + "'*****";
					txtStatus.setText("Error: Invalid GO ID: " + searchStr);
					theWhereStr = " where go_info.descr LIKE '%xxyy9zzcc7bbttgg1%'"; // force search failure
				}
			}
			else {
				if (loadList!=null && searchStr.startsWith(loadStart)) {
					theStatusStr = "Description=" + searchStr;
					String x = Static.addQuoteDBList(loadList);
					theWhereStr= " where go_info.descr  IN ("  + x + ")"; 
				}
				else {
					theStatusStr= "Description contains '" + searchStr + "'";
					theWhereStr= " where go_info.descr LIKE '%" + searchStr + "%' ";
				}
			}
			theStatusStr = "    Search: " + theStatusStr;
		}
		
		if (!singleMode)
		{
			theStatusStr="";
			String tmp="";
			if (chkUseEval.isSelected()) {
				String eval = txtEvalVal.getText().trim();
				if (eval.startsWith("e") || eval.startsWith("E")) eval = "1" + eval;
				theWhereStr = " go_info.bestEval<=" + eval;
				theStatusStr = strMerge(theStatusStr, "E-value " + eval);
			}
			if (chkUseNSeq.isSelected()) {
				String nseq = txtNSeqVal.getText().trim();
				theWhereStr = strAndMerge(theWhereStr, " go_info.nUnitranHit>=" + nseq);
				theStatusStr = strMerge(theStatusStr, "#Seq " + nseq);
			}
			if (ecColumnNames.length > 0 && chkUseEC.isSelected())
			{
				tmp = makeQueryECClause();
				theWhereStr = strAndMerge(theWhereStr, tmp);
			}
			if(cmbTermTypes.getSelectedIndex() > 0) {
				tmp = "(go_info.term_type = '" + cmbTermTypes.getSelectedItem() + "') ";
				theWhereStr = strAndMerge(theWhereStr, tmp);
				theStatusStr = strMerge(theStatusStr, cmbTermTypes.getSelectedItem());
			}
			if(chkLevelSpecific.isSelected()) {
				tmp = "go_info.level = " + txtLevelSpecific.getText();
				theWhereStr = strAndMerge(theWhereStr, tmp);
				theStatusStr = strMerge(theStatusStr, "Level=" + txtLevelSpecific.getText());
			}
			else {
				if (nLevelMin>MIN_LEVEL || nLevelMax<MAX_LEVEL) {
					tmp = "(go_info.level >= " + nLevelMin + " and go_info.level <= " + nLevelMax + ")";
					theWhereStr = strAndMerge(theWhereStr, tmp);
					theStatusStr = strMerge(theStatusStr, "Level [" + nLevelMin + "," + nLevelMax + "]");
				}
			}
			if (chkSlims.isSelected()) {
				tmp = " go_info.slim=1";
				theWhereStr = strAndMerge(theWhereStr, tmp);
				theStatusStr = strMerge(theStatusStr, "Is Slim");
			}
			if (deColumnNames.size() > 0 && chkUseDE.isSelected())
			{
				tmp = makeQueryDEClause();
				theWhereStr = strAndMerge(theWhereStr, tmp);
			}
			if (theWhereStr.equals("")) theWhereStr=" ";
			else theWhereStr = " where " + theWhereStr;
			theStatusStr = "    Filter: " + theStatusStr;
			
			if (boxShowDE!=null && chkUseDE.isSelected())
				theStatusStr += "   #Seqs: " + boxShowDE.getSelectedItem();
		}
		
		theQuery += " " + theWhereStr + " order by go_info.gonum";
		return theQuery;
	}
	
	private String strMerge(String t1, String t2) {
		if (t1!="" && t2!="") return t1 + "; " + t2;
		if (t1!="") return t1;
		return t2;
	}
	private String strAndMerge(String t1, String t2) {
		if (t1!="" && t2!="") return t1 + " and " + t2;
		if (t1!="") return t1;
		return t2;
	}
	// BasicGOTablePanel: if one DE column selected, use for Export #Seq Merge
	public String selectOneDE() {
		if (deColumnNames.size()==0 || !chkUseDE.isSelected()) return null;
		
		String deCol=null;
		for (int i = 0; i < deColumnNames.size(); i++)
		{
			if (chkDEfilter[i].isSelected())
			{
				if (deCol!=null) return null;
				deCol = deColumnNames.get(i);
			}
		}
		return deCol;
	}
	private String makeQueryDEClause()
	{
		if (deColumnNames.size()==0 || !chkUseDE.isSelected()) return "";
		String clause = "", summary="";
		
		String thresh = txtCutoff.getText().trim();
		if (thresh.startsWith("e") || thresh.startsWith("E")) thresh = "1" + thresh;
		
		String ops = chkDEAny.isSelected() ? "|" : "&";
		String opc = chkDEAny.isSelected() ? " or " : " and ";
		String deg = "go_info." + Globals.PVALUE;
		
		for (int i = 0; i < deColumnNames.size(); i++)
		{
			if (chkDEfilter[i].isSelected())
			{
				if (!summary.equals("")) {
					clause += opc;
					summary += ops;
				}
				summary += deColumnNames.get(i);
				clause += deg + deColumnNames.get(i) + "<" + thresh; 
			}
		}
		if (clause == "") {
			chkUseDE.setSelected(false);
			enableDESection(false); 
			return "";
		}
		
		summary = "Pval<" + thresh + " " + summary;
		theStatusStr = strMerge(theStatusStr,summary);
				
		return " (" + clause + ") ";
	}
	private String makeQueryECClause()
	{
		int cnt=0;
    		for(int x=0; x<chkECfilter.length; x++)
    			if (chkECfilter[x].isSelected()) cnt++;
    		if (cnt==ecColumnNames.length || cnt==0) return ""; // ecColumNames are only ones in DB
    		
    		String ops = "|";
    		String opc = " or ";
    		
    		String [] allNames = theParentFrame.getMetaData().getEClist();
    		String clause = "", summary="";
    		cnt=0;
    		for(int x=0; x<chkECfilter.length; x++)
    			if (chkECfilter[x].isSelected()) {
    				if (clause != "") clause += opc;
    				clause += allNames[x] + "=1";
    				if (cnt<=3) {
    					if (summary!="")summary += ops;
    					if (cnt<3) summary += allNames[x];
    					else if (cnt==3) summary += "...";
    				}
    				cnt++;
    			}
		theStatusStr = strMerge(theStatusStr, summary);
		
		return " (" + clause + ") ";
	}
	/*************************
	 *  return: e.g. select c.contigid from pja_unitrans_go as g
	 *	join contig as c on c.CTGID=g.CTGID 
	 *	where (abs(c.P_RoSt) < 0.05 and c.P_RoSt<0) and g.gonum
	 */
	
	public String  makeNseqClause(boolean doWhere) {
		if (dePvalMap==null || boxShowDE==null || 
				!chkUseDE.isSelected() || radUseSearch.isSelected()) return ""; 
		int x = boxShowDE.getSelectedIndex();
		if (x==0) return "";
		
		String nSeqOp = null, clause=""; 
		if (x==2) nSeqOp=">0"; else if (x==3) nSeqOp="<0";
		String deOp = (chkDEAny.isSelected()) ? " or " : " and "; 
		
		for (int i = 0; i < deColumnNames.size(); i++)
		{
			if (chkDEfilter[i].isSelected())
			{
				String de = deColumnNames.get(i);
				String deTabCol = "c." + Globals.PVALUE +de;
				double cutoff = 0.05;
				if (dePvalMap.containsKey(de)) cutoff = dePvalMap.get(de);
				else Out.PrtWarn(de + "does not have a stored cutoff, use default 0.05");
				
				if (!clause.equals("")) clause += deOp;
				
				clause += "(abs(" + deTabCol + ") < " + cutoff; // GOseq was run with deTabCol<thresh
				if (nSeqOp!=null) clause += " and " + deTabCol + nSeqOp;
				clause += ")";
			}
		}
		if (clause!="") clause = "(" + clause + ")";
		if (doWhere) return sqlForGoSeqs +  clause + " and p.gonum";
		else return clause;
	}
	/********************************************************
	 * DBQ to initalize
	 */
	private void loadInitData() {
		try {
			DBConn mDB = theParentFrame.getNewDBC();
			ResultSet rs=null;
			// for DE columns
			deColumnNames = new Vector<String> ();
			rs = mDB.executeQuery("SHOW COLUMNS FROM go_info LIKE 'P\\_%'");
			while(rs.next()) {
				String deName = rs.getString(1).substring(2);
				deColumnNames.add(deName);
			}
			rs.close();
			mDB.close();
			
			// init rest of columns
			HashSet <String> ecSet = theParentFrame.getMetaData().getECinDB();
			String [] ecList = theParentFrame.getMetaData().getEClist();
			ecColumnNames = new String [ecSet.size()];
			int x=0;
			for (String ec : ecList) {
				if (ecSet.contains(ec))
					ecColumnNames[x++] = ec;
			}
			
			MAX_LEVEL = mDB.executeCount("select max(level) from go_info");
		
			theTermTypes = Globalx.GO_TERM_LIST;
			dePvalMap =   theParentFrame.getMetaData().getDegPvalMap(mDB);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Query failed ");
			ErrorReport.prtReport(e, "Error getting count names");
		}
	}
	
	/*************************************************************
	 * Compute Trimmed (BasicGOLoadFromDB) 
	 */
	private void trimStart() {
		txtStatus.setText("Computing DE-trim set....");
		chkComputeDEtrim.setEnabled(false);
		btnBuildTable.setEnabled(false);
		
		loadObj.runQuery(BasicGOLoadFromDB.TRIM);	
	}
	public void trimFinish() {
		String msg = goTablePanel.trimTable();
		
		btnBuildTable.setEnabled(true);
		chkShowDEtrim.setEnabled(true);
		txtStatus.setText("Results: " + msg + theStatusStr);
	}
	public JCheckBox [] getDEselect() { return chkDEfilter;}
	public ButtonComboBox getCmbTermTypes() {return cmbTermTypes;}
	
	/**************************************************************/
	public void setErrorStatus(String status) {
		btnBuildTable.setEnabled(true);
		txtStatus.setText(status);
	}
	public int getNseqs() {
		if (chkUseNSeq.isSelected()) return Static.getInteger(txtNSeqVal.getText().trim());
		else return 0;
	}
	public String getStatusStr() {
		return theStatusStr;
	}
	
	/****************************************************************
	 * Search/Filter utilities
	 */
	private void loadFile(String fileName) {
		try {
			boolean isDesc = chkGODesc.isSelected();
			loadList= new Vector <String> ();
			int cntBad=0;
			String line, bad="", goTxt="";
			BufferedReader file = new BufferedReader(new FileReader(fileName));
			
			while((line = file.readLine()) != null) {
				line.replace("\n","").trim();
				if (line.equals("")) continue;
				if (line.startsWith("#")) continue;
				
				if (isDesc) {
					loadList.add(line);
					if (goTxt=="") goTxt=line;
				}
				else {
					String gonum = line.replaceAll("[A-Za-z:]", "");
					try {
						Integer.parseInt(gonum);
						if (goTxt=="") goTxt=line;
						loadList.add(gonum);
					}
					catch (Exception e) {
						if (bad=="") bad = line;
						else bad += ", " + line;
						cntBad++;
					}
				}
			}
			file.close(); 
			if (cntBad>0) {
				String msg = "Incorrect GO terms:\n" + bad;
				JOptionPane.showMessageDialog(null, 
						msg, "Incorrect GO terms", JOptionPane.PLAIN_MESSAGE);
			}
			if (loadList.size()==0) {
				loadList = null;
				loadStart = "";
				txtSubString.setText("");
			}
			else {
				loadStart=goTxt;
				if (loadList.size()>1) goTxt+=",...";
				if (isDesc) txtSubString.setText(goTxt);
				else txtSubString.setText(goTxt);
			}
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error loading file");}
	}
	private STCWFrame getInstance() {return theParentFrame;}
	private boolean badLevel(String num) {
		try {
			int x = Integer.parseInt(num);
			if (x<1 || x>16) {
				JOptionPane.showMessageDialog(null, "Level must be between 1 and 16");
				return false;
			}
			return true;
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Must enter Integer");
			return false;
		}
	}
	private void enableLevels() {
		boolean isF= radUseFilter.isSelected();
		boolean isSp = chkLevelSpecific.isSelected();
		boolean isRg = chkLevelRange.isSelected();
		
		lblSpecific.setEnabled(isSp & isF);
		txtLevelSpecific.setEnabled(isSp & isF);
			
		lblRange.setEnabled(isRg & isF);
		lblTo.setEnabled(isRg & isF);
		txtLevelMin.setEnabled(isRg & isF);
		txtLevelMax.setEnabled(isRg & isF);
	}
	private void enableDESection(boolean enable)
    {
	 	lblDErow.setEnabled(enable);
	 	
		lblCutoff.setEnabled(enable);
	    txtCutoff.setEnabled(enable);
	  
	    btnPval.setEnabled(enable);
	    lblShow.setEnabled(enable);
	    boxShowDE.setEnabled(enable);
    }
    private void enableDEtrim() {
		if (deColumnNames.size() == 0) return;
	
		boolean enable = chkUseDE.isSelected();
		deTrimLabel.setEnabled(enable);
		chkComputeDEtrim.setEnabled(enable);
		if (!enable) chkComputeDEtrim.setSelected(false);
		chkShowDEtrim.setEnabled(chkComputeDEtrim.isSelected());
    }
    private void enabledFunctions(boolean b) {
		btnBuildTable.setEnabled(b);
		btnAddTable.setEnabled(b);
    }
	private void clear() {
		loadList=null; 
		loadStart="";
		txtSubString.setText("");
		chkGOID.setSelected(true);
		lblGOID.setEnabled(true);
		lblDesc.setEnabled(false);
		
		chkUseEval.setSelected(false);
		txtEvalVal.setText(DEF_EVAL);
		txtEvalVal.setEnabled(false);
		
		chkUseNSeq.setSelected(false);
		txtNSeqVal.setText("2");
		txtNSeqVal.setEnabled(false);
		
		cmbTermTypes.setSelectedIndex(0);
		txtLevelSpecific.setText("2");
		chkLevelRange.setSelected(true);
		txtLevelMin.setText(MIN_LEVEL + "");
		txtLevelMax.setText(MAX_LEVEL + "");
		enableLevels();
		
		lblSlims.setEnabled(false);
		chkSlims.setSelected(false);
			
		if (chkUseDE!=null) {
			chkUseDE.setSelected(false);
			txtCutoff.setText(DEF_PVAL);
			boxShowDE.setSelectedIndex(0);
			enableDESection(false);
		}
		if (chkUseEC!=null) {
			chkUseEC.setSelected(false);
			btnEC.setEnabled(false);
		}
	}
	private void enableSections() {
		boolean bsearch = radUseSearch.isSelected();
		boolean bfilter = radUseFilter.isSelected();
		 
		//Search
		lblGOID.setEnabled(bsearch); lblDesc.setEnabled(bsearch);
		chkGOID.setEnabled(bsearch); chkGODesc.setEnabled(bsearch);
		txtSubString.setEnabled(bsearch);
		btnFindFile.setEnabled(bsearch);
		if (bsearch) {
			boolean b = chkGOID.isSelected();
			lblGOID.setEnabled(b); lblDesc.setEnabled(!b);
		}
		
		// Seq-hit
		chkUseEval.setEnabled(bfilter); chkUseNSeq.setEnabled(bfilter); 
		if (chkUseEC!=null) chkUseEC.setEnabled(bfilter);
		
		// Level
		lblRange.setEnabled(bfilter);lblTo.setEnabled(bfilter);
		cmbTermTypes.setEnabled(bfilter);
		chkLevelSpecific.setEnabled(bfilter); chkLevelRange.setEnabled(bfilter);
		txtLevelSpecific.setEnabled(bfilter); txtLevelMin.setEnabled(bfilter); txtLevelMax.setEnabled(bfilter);
		if (chkSlims!=null) chkSlims.setEnabled(bfilter);
		
		// DE
	  	if (deColumnNames.size() > 0)
	  	{
	  		chkUseDE.setEnabled(bfilter);
	  	}
		if (!bfilter) {
			if (ecColumnNames.length>0)   btnEC.setEnabled(bfilter);
			if (deColumnNames.size() > 0) btnPval.setEnabled(bfilter);
		}
		else {
			if (ecColumnNames.length>0)
				if (chkUseEC.isSelected()) btnEC.setEnabled(bfilter);
			if (deColumnNames.size() > 0)
				if (chkUseDE.isSelected()) btnPval.setEnabled(bfilter);
		}
	}
	private void setPvalLabel() {
		int cnt=0; 
		String name="";
		for(int x=0; x<chkDEfilter.length; x++)
			if (chkDEfilter[x].isSelected()) {
				cnt++;
				name = chkDEfilter[x].getText();
			}
		if (cnt==0) {
			btnPval.setText(deColLabel);
			chkUseDE.setSelected(false);
			enableDESection(false);
		}
		else {
			if (cnt==1) btnPval.setText(name);
			else if (chkDEAll.isSelected()) btnPval.setText("Every " + cnt);
			else btnPval.setText("Any " + cnt);
		}
	}
	public void showMain() {
		mainPanel.setVisible(true);
	}
	public void setStatus(String msg) {
		txtStatus.setText(msg);
	}
	/***********************************************
	 * private
	 */
	//  Main panel, hidden when selecting columns
	private JPanel mainPanel = null;
	private JTextField txtStatus = null;
	
	//Top button panel
	private JPanel topRowPanel = null;
	private JButton btnViewSeqs = null, btnTable = null, btnCopy = null, btnShow = null;
	
	//Column select panel
	private BasicGOTablePanel goTablePanel=null;
	
	// EC panel	
	private JPanel evidCodePanel=null;
	private JCheckBox chkSelectECs = null;
	private JCheckBox [] chkECfilter = null;
	
	// DE Panel
	private JPanel deColumnsPanel=null;
	private JCheckBox chkSelectDEs = null;
	private JCheckBox [] chkDEfilter = null;
		
//Search/Filter panel	
	private JPanel filterPanel = null;
	private JButton btnClearAll = null;
	
	// search
	private JRadioButton radUseSearch = null, chkGOID = null, chkGODesc = null;
	private JLabel lblSearch = null, lblGOID=null, lblDesc=null;
	private JTextField txtSubString = null;
	private JButton btnFindFile = null;
	
	private JRadioButton radUseFilter = null;
	private JLabel lblFilter = null;
	
	// Seq-hit row
	private JLabel lblSeqHit = null;
	private JCheckBox chkUseEval = null, chkUseNSeq = null, chkUseEC = null;
	private JTextField txtEvalVal = null, txtNSeqVal = null;
	private JButton btnEC=null;
			
	// Level row
	private JLabel lblSpecific = null, lblRange = null, lblTo = null;
	private ButtonComboBox cmbTermTypes = null;
	private JRadioButton chkLevelSpecific = null, chkLevelRange = null;
	private JTextField txtLevelSpecific = null, txtLevelMin = null, txtLevelMax = null;
	private JLabel lblSlims = null;
	private JCheckBox chkSlims = null;
	
	// DE row
	private JCheckBox chkUseDE = null;
	private JLabel lblCutoff = null, lblShow=null, lblDErow=null, lblDEAny=null, lblDEAll=null;
	private JTextField txtCutoff = null;
	private JButton btnPval=null;
	private JRadioButton chkDEAny = null, chkDEAll = null;
	private ButtonComboBox boxShowDE = null;

	// Last row
	private JButton btnBuildTable = null, btnAddTable = null, btnSelectColumns = null;
	
	private JLabel deTrimLabel=null;
	private JCheckBox chkShowDEtrim = null, chkComputeDEtrim = null;

	
	//Data members
	private int nLevelMin = -1, nLevelMax = -1;
	private STCWFrame theParentFrame = null;
	
	private Vector <String> loadList = null;
	private String loadStart=""; 
	
	private Vector<String> deColumnNames = null;
	private String []      ecColumnNames = null;

	private String [] theTermTypes = null;
	private String currentGO = null; 
	private BasicGOLoadFromDB loadObj=null;
	private TreeMap <String, Double> dePvalMap = null;
	
	boolean singleMode = false;
	private String theStatusStr="";
	// keep join on contig - much faster
	private String sqlForGoSeqs = " FROM pja_unitrans_go as p " +
			" JOIN contig as c ON c.CTGID = p.CTGID WHERE ";
}
