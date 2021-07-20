package sng.viewer.panels.Basic;

/**************************************************
 * Creates the Top Row of buttons and the Filter panel
 * 
 * query: go_info for everything except:
 * 		pja_gotree for making the 'show tree' view and DEtrim
 * 		pja_unitrans_go for selecting sequences to view
 * CAS324 removed Trim stuff for now....
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
	
	private final String helpHTML =   Globals.helpDir + "BasicQueryGO.html";
	private final String goHelpHTML = Globals.helpDir + "goHelp/index.html"; // CAS318 new GO help
	private final String goEvCHTML =  Globals.helpDir + "goHelp/evc.html"; // CAS323
	
	private static final Color BGCOLOR = Globals.BGCOLOR;
	
	private static final int LABEL_CHK_WIDTH1 = 50;
	private static final int LABEL_WIDTH2 = 57;
	private static final String GO_FORMAT = Globalx.GO_FORMAT;
	
	private static final String pvalColLabel = "Select";
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
		
		if (pvalColumnsPanel!=null) {
			pvalColumnsPanel.setVisible(false);
			setPvalLabel();
			add(pvalColumnsPanel);
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
		
		btnViewSeqs = new JButton("View Seqs");
		btnViewSeqs.setBackground(Globals.FUNCTIONCOLOR);
		btnViewSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewSequencesFromSelected();
			}
		});
		btnViewSeqs.setEnabled(false);
		
        createTopCopy();
        createTopSelected();
	    createTopTable();
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, 
						"Basic GO Query", helpHTML);
			}
		});
		
		topRowPanel.add(new JLabel("Selected: "));
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnViewSeqs);
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnSelCopy);
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnSelShow);
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnSelExport);
		topRowPanel.add(Box.createHorizontalStrut(5));
		
		topRowPanel.add(new JLabel("Table: "));
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnTableShow);
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnTableExport);
		topRowPanel.add(Box.createHorizontalStrut(5));
		
		topRowPanel.add( Box.createHorizontalGlue() );
		topRowPanel.add(btnHelp);
		topRowPanel.setMaximumSize(topRowPanel.getPreferredSize());
		topRowPanel.setMinimumSize(topRowPanel.getPreferredSize());
	}
	/**************************************************************/
	private void createTopCopy() {
		final JPopupMenu copypopup = new JPopupMenu();
		
		copypopup.add(new JMenuItem(new AbstractAction(Globalx.goID) {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String ts = goTablePanel.getSelectedGO();
					if (ts!=null)cb.setContents(new StringSelection(ts), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying gonum"); }
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction(Globalx.goTerm) {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String ts = goTablePanel.getSelectedGOdesc();
					if (ts!=null)cb.setContents(new StringSelection(ts), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying  description"); }
			}
		}));
		btnSelCopy = Static.createButton("Copy...", false);
		btnSelCopy.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                copypopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
	}
	/**************************************************************
	 * Selected popup and export
	 */
	private void createTopSelected() {
	// Selected show
		final JPopupMenu selPopup = new JPopupMenu();
		selPopup.add(new JMenuItem(new AbstractAction("Hits - assigned") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.GO_HITS, GOtree.DO_POPUP, btnSelShow);
			}
		}));
		selPopup.add(new JMenuItem(new AbstractAction("Hits - inherited") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.GO_ALL_HITS, GOtree.DO_POPUP, btnSelShow);
			}
		}));	
		selPopup.add(new JMenuItem(new AbstractAction("Sequence - has hit with GO") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.GO_SEQ, GOtree.DO_POPUP, btnSelShow);
			}
		}));	
		selPopup.addSeparator();
		
		selPopup.add(new JMenuItem(new AbstractAction("GO - Neighborhood with relations") { // CAS318 put first
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.NEIGHBORS,GOtree.DO_POPUP,  btnSelShow);
			}
		}));
		selPopup.add(new JMenuItem(new AbstractAction("GO - Ancestors") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.ANCESTORS, GOtree.DO_POPUP, btnSelShow);
			}
		}));	
		selPopup.add(new JMenuItem(new AbstractAction("GO - Descendants") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.DESCENDANTS, GOtree.DO_POPUP, btnSelShow);
			}
		}));	
		
		selPopup.add(new JMenuItem(new AbstractAction("GO - Ancestor path table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.PATHS, GOtree.DO_POPUP, btnSelShow);
			}
		}));
		
		selPopup.add(new JMenuItem(new AbstractAction("GO - Related in table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showRelatedFromTable(GOtree.DO_POPUP, btnSelShow);
			}
		}));	
		
		btnSelShow = Static.createButton("Show...", false);
		btnSelShow.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                selPopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		/**************************************************************************/
		final JPopupMenu selExport = new JPopupMenu();
		selExport.add(new JMenuItem(new AbstractAction("Hits - assigned*") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.GO_HITS, GOtree.DO_EXPORT_ASK, btnSelExport);
			}
		}));
		selExport.add(new JMenuItem(new AbstractAction("Hits - inherited*") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.GO_ALL_HITS, GOtree.DO_EXPORT_ASK, btnSelExport);
			}
		}));	
		selExport.add(new JMenuItem(new AbstractAction("Sequence - has hit with GO*") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.GO_SEQ, GOtree.DO_EXPORT_ASK, btnSelExport);
			}
		}));	
		selExport.addSeparator();
		
		selExport.add(new JMenuItem(new AbstractAction("GO - Neighborhood with relations") { // CAS318 put first
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.NEIGHBORS,GOtree.DO_EXPORT_ALL,  btnSelExport);
			}
		}));
		selExport.add(new JMenuItem(new AbstractAction("GO - Ancestors*") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.ANCESTORS, GOtree.DO_EXPORT_ASK, btnSelExport);
			}
		}));	
		selExport.add(new JMenuItem(new AbstractAction("GO - Descendants*") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.DESCENDANTS, GOtree.DO_EXPORT_ASK, btnSelExport);
			}
		}));		
		selExport.add(new JMenuItem(new AbstractAction("GO - Ancestor path table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showExportGOtreeSelected(GOtree.PATHS, GOtree.DO_EXPORT_ALL, btnSelExport);
			}
		}));	
		selExport.add(new JMenuItem(new AbstractAction("GO - Related in table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				goTablePanel.showRelatedFromTable(GOtree.DO_EXPORT_ALL, btnSelExport);
			}
		}));		
		
		btnSelExport = Static.createButton("Export...", false);
		btnSelExport.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                selExport.show(e.getComponent(), e.getX(), e.getY());
            }
        });
	}
	/************************************************************
	 * Table popup and export - All in BasicGOTable
	 */
	private void createTopTable() {
		
		final JPopupMenu tablePopup = new JPopupMenu();
		tablePopup.add(new JMenuItem(new AbstractAction("Column Stats") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					goTablePanel.statsPopUp("GO " + theStatusStr);
				} catch (Exception er) {ErrorReport.reportError(er, "Error on column stats");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error column stats", null);}
			}
		}));
		tablePopup.addSeparator();
		
		tablePopup.add(new JMenuItem(new AbstractAction("Each GO parents with relation") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					btnTableShow.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableShow, GOtree.ALL_PARENTS, GOtree.DO_POPUP);
					
					enabledFunctions(true);
					btnTableShow.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		tablePopup.add(new JMenuItem(new AbstractAction("Set of ancestors") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					btnTableShow.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableShow, GOtree.ALL_ANCESTORS, GOtree.DO_POPUP);
					
					enabledFunctions(true);
					btnTableShow.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		tablePopup.add(new JMenuItem(new AbstractAction("Longest paths (slow and large results)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableShow.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableShow, GOtree.LONGEST_PATHS, GOtree.DO_POPUP);
					
					enabledFunctions(true);
					btnTableShow.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		
		tablePopup.add(new JMenuItem(new AbstractAction("All paths (slow and very large results)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableShow.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableShow, GOtree.ALL_PATHS, GOtree.DO_POPUP);
					
					enabledFunctions(true);
					btnTableShow.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		
		btnTableShow = Static.createButton("Show...", false);
		btnTableShow.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablePopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		
	/*************** Export *****************************
	 ***********************************************/
		final JPopupMenu tableExport = new JPopupMenu();
		
		tableExport.add(new JMenuItem(new AbstractAction("Table columns") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.tableExport(btnTableExport, 0);
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		tableExport.add(new JMenuItem(new AbstractAction("Table columns with -log10(Pval)") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.tableExport(btnTableExport, 5); // CAS326 new
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		tableExport.add(new JMenuItem(new AbstractAction("GO IDs only") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.tableExport(btnTableExport, 4); // CAS324 new
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		tableExport.add(new JMenuItem(new AbstractAction("SeqID with GOs") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.tableExport(btnTableExport, 1);
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		tableExport.add(new JMenuItem(new AbstractAction("Create/Merge #Seq") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false);
					enabledFunctions(false);
					
					goTablePanel.tableExport(btnTableExport, 2);
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error export table", null);}
			}
		}));
		
		// Export using GOtree.java
		tableExport.addSeparator();
		tableExport.add(new JMenuItem(new AbstractAction("Each GO parents with relation") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableExport, GOtree.ALL_PARENTS, GOtree.DO_EXPORT_ALL);
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		tableExport.add(new JMenuItem(new AbstractAction("Set of ancestors") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableExport, GOtree.ALL_ANCESTORS, GOtree.DO_EXPORT_ALL);
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		tableExport.add(new JMenuItem(new AbstractAction("Longest paths (slow and large results)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableExport,GOtree.LONGEST_PATHS,GOtree.DO_EXPORT_ALL);
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		
		tableExport.add(new JMenuItem(new AbstractAction("All paths (slow and very large results)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTableExport.setEnabled(false); 
					enabledFunctions(false);
					
					goTablePanel.showExportGOtreeTable(btnTableExport, GOtree.ALL_PATHS, GOtree.DO_EXPORT_ALL);
					
					enabledFunctions(true);
					btnTableExport.setEnabled(true);
				} catch (Exception er) {ErrorReport.reportError(er, "Error all paths for table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error all paths for table", null);}
			}
		}));
		tableExport.addSeparator();	
		tableExport.add(new JMenuItem(new AbstractAction("Copy Table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String ts = goTablePanel.tableCopyString("\t");
					cb.setContents(new StringSelection(ts), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copy table"); }
			}
		}));
		
		btnTableExport = Static.createButton("Export...", false);
		btnTableExport.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tableExport.show(e.getComponent(), e.getX(), e.getY());
            }
        });
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
		createFilterRow4Enrich();
		
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
		
		lblGOID = Static.createLabel(Globalx.goID, false);
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
		
		lblDesc = Static.createLabel(Globalx.goTerm + " (Substring)", false);
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
					if (fr.run(btnFindFile, "GO File", FileC.dRESULTEXP, FileC.fTXT)) {
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
		
		lblSeqHit = createLabel("Seq-hit:", LABEL_WIDTH2); // CAS317 label was partial on linux
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
		row2.add(Box.createHorizontalStrut(5));
		
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
		row2.add(Box.createHorizontalStrut(25));
		
		// [x] [Evidence Codes] 
		lblHitGO = Static.createLabel("Hit-GO:"); // CAS317 label was partial on linux
		row2.add(lblHitGO); row2.add(Box.createHorizontalStrut(5));
		
		chkUseEvC = Static.createCheckBox("", false);
    	chkUseEvC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnEvC.setEnabled(chkUseEvC.isSelected());
			}
		});   
	
		createEvCPanel();
		btnEvC = Static.createButton("Evidence", false, Globals.MENUCOLOR);
		btnEvC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				evidCodePanel.setVisible(true);
				mainPanel.setVisible(false);
			}
		});
		row2.add(chkUseEvC);
		row2.add(btnEvC); row2.add(Box.createHorizontalStrut(1));
		JButton btnInfo = Static.createButton("Info", true, Globals.MENUCOLOR);
		btnInfo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserPrompt.displayHTMLResourceHelp(theParentFrame, 
							"EvC Info", goEvCHTML);
				}
		});
		row2.add(btnInfo);
		filterPanel.add(row2);
	}
	private void createFilterRow3Level() {
		JPanel row3 = Static.createRowPanel();
		row3.add(createLabel("", lblFilter.getPreferredSize().width + 
				radUseFilter.getPreferredSize().width));
		row3.add(createLabel("Level:", lblSeqHit.getPreferredSize().width));
		
		// Dropdown
		cmbTermTypes = new ButtonComboBox();
		cmbTermTypes.addItem("Any " + Globalx.goFullOnt);
		for(int x=0; x<theTermTypes.length; x++)
			cmbTermTypes.addItem(theTermTypes[x]);
		cmbTermTypes.setSelectedIndex(0);
		cmbTermTypes.setMaximumSize(cmbTermTypes.getPreferredSize());
		cmbTermTypes.setMinimumSize(cmbTermTypes.getPreferredSize());
		cmbTermTypes.setBackground(Globals.BGCOLOR);
		row3.add(cmbTermTypes);
		row3.add(Box.createHorizontalStrut(15));
		
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
		row3.add(Box.createHorizontalStrut(15));
		
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
		row3.add(txtLevelMin); 	row3.add(Box.createHorizontalStrut(2));
		row3.add(lblTo);		row3.add(Box.createHorizontalStrut(2));
		row3.add(txtLevelMax);	row3.add(Box.createHorizontalStrut(15));
		
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
		else row3.add(Box.createHorizontalStrut(lblSlims.getWidth()+chkSlims.getWidth()));
			
		if (pvalColumnNames.size()==0) {
			row3.add(Box.createHorizontalGlue());
			row3.add(Box.createHorizontalStrut(15));
			row3.add(btnClearAll);
		}
		filterPanel.add(row3);
	}
	private void createFilterRow4Enrich() {
		JPanel row4 = Static.createRowPanel();
		row4.add(createLabel("", lblFilter.getPreferredSize().width + 
				radUseFilter.getPreferredSize().width));
		
		row4.add(createLabel("Enrich:", lblSeqHit.getPreferredSize().width-2));
		
		boolean enable=false;
		chkUseEnrich = Static.createCheckBox("", enable);
    	chkUseEnrich.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enablePvalSection(chkUseEnrich.isSelected());
			}
		});  
    	row4.add(chkUseEnrich);		row4.add(Box.createHorizontalStrut(4));
    	
		lblCutoff = Static.createLabel("Pval<", enable);
		row4.add(lblCutoff); 		row4.add(Box.createHorizontalStrut(1));
		txtCutoff = Static.createTextField(DEF_PVAL,4, enable);
	    row4.add(txtCutoff);		row4.add(Box.createHorizontalStrut(2));
	     	
    	lblDErow = Static.createLabel(" for ", enable);
    	row4.add(lblDErow);			row4.add(Box.createHorizontalStrut(1));
		
    	createPvalColPanel();
    	btnPval = Static.createButton(pvalColLabel, false, Globals.MENUCOLOR);
		btnPval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pvalColumnsPanel.setVisible(true);
				mainPanel.setVisible(false);
			}
		});
		row4.add(btnPval);			row4.add(Box.createHorizontalStrut(30));
		 
    	lblnDEseq = Static.createLabel("#Seqs", false);
    	String [] type = {"All", "DE", "upDE", "dnDE"};
    	boxDEseq = new ButtonComboBox();
    	boxDEseq.addItems(type);
    	boxDEseq.setEnabled(false);
    	boxDEseq.setSelectedIndex(0);
    
    	boxDEseq.setMaximumSize(boxDEseq.getPreferredSize());
    	boxDEseq.setMinimumSize(boxDEseq.getPreferredSize());
    		
		row4.add(lblnDEseq);
		row4.add(boxDEseq);	
		
		if (pvalColumnNames.size()>0) {
			row4.add(Box.createHorizontalGlue());
			row4.add(Box.createHorizontalStrut(15));
			row4.add(btnClearAll);
		
			filterPanel.add(row4);
			filterPanel.add(Box.createVerticalStrut(5));
			enablePvalSection(false);
		}
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
		
		JButton btnGoHelp = new JButton("GO Help");
		btnGoHelp.setBackground(Globals.HELPCOLOR);
		btnGoHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "GO Help", goHelpHTML);
			}
		});
		btnGoHelp.setAlignmentX(RIGHT_ALIGNMENT);
		row5.add(Box.createHorizontalGlue());
		row5.add(Box.createHorizontalStrut(160));
	    row5.add(btnGoHelp);
	    
	   
	    row5.setMaximumSize(row5.getPreferredSize());
	    
		filterPanel.add(row5);
	}
	private void createPvalColPanel() { 
		pvalColumnsPanel = Static.createPageCenterPanel();
		
	 	JLabel header = new JLabel("<HTML><H2>Select p-value to filter</H2></HTML>");
	 	header.setAlignmentX(Component.CENTER_ALIGNMENT);
	 	header.setMaximumSize(header.getPreferredSize());
	 	header.setMinimumSize(header.getPreferredSize());
	 	pvalColumnsPanel.add(Box.createVerticalStrut(10));
	    pvalColumnsPanel.add(header);
    	
	    JPanel deColPanel = Static.createPageCenterPanel();
	    	
	    JPanel row = Static.createRowPanel();
	    ButtonGroup grpDE = new ButtonGroup();
		lblPvalAny = Static.createLabel("Any", true);
		row.add(lblPvalAny);
		row.add(Box.createHorizontalStrut(2));
		
		chkPvalAny = new JRadioButton(); grpDE.add(chkPvalAny);
		row.add(chkPvalAny);
		
		lblPvalAll = Static.createLabel("Every", true);
		row.add(lblPvalAll);
		
		chkPvalAll = new JRadioButton(); grpDE.add(chkPvalAll);
		chkPvalAny.setSelected(true);
		row.add(chkPvalAll);
		
		chkPvalAny.setBackground(Globals.BGCOLOR);
		chkPvalAll.setBackground(Globals.BGCOLOR);
		
		deColPanel.add(row);
		deColPanel.add(Box.createVerticalStrut(10));
		
    	chkPvalColFilter = new JCheckBox[pvalColumnNames.size()];
    	for(int x=0; x<chkPvalColFilter.length; x++) {
    		chkPvalColFilter[x] = new JCheckBox(pvalColumnNames.get(x));
    		chkPvalColFilter[x].setBackground(BGCOLOR);
    		chkPvalColFilter[x].setSelected(true);
    		chkPvalColFilter[x].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				 	boolean allSelected = true;
				 	for(int x=0; x<chkPvalColFilter.length && allSelected; x++)
				 		allSelected = chkPvalColFilter[x].isSelected();
				 	chkSelectPvals.setSelected(allSelected);
				}
			});
    		deColPanel.add(chkPvalColFilter[x]);
    	}
    	deColPanel.add(Box.createHorizontalStrut(10));
	    	
	    JPanel checkPanel = Static.createRowPanel();
	    chkSelectPvals = new JCheckBox("Check/uncheck all");
    	chkSelectPvals.setSelected(true);
    	chkSelectPvals.setBackground(BGCOLOR);
    	chkSelectPvals.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				boolean isSelected = chkSelectPvals.isSelected();
				for(int x=0; x<chkPvalColFilter.length; x++)
					chkPvalColFilter[x].setSelected(isSelected);
			}
		});
    	checkPanel.add(chkSelectPvals);
    	checkPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    
    	deColPanel.add(checkPanel);
	    deColPanel.setMaximumSize(deColPanel.getPreferredSize());
    	deColPanel.setMinimumSize(deColPanel.getPreferredSize());
    	
    	pvalColumnsPanel.add(Box.createVerticalStrut(10));
    	pvalColumnsPanel.add(deColPanel);
    	
    	// buttons
    	JPanel buttonPanel = Static.createRowCenterPanel();
    	JButton keepButton = new JButton("Accept");
    	keepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setPvalLabel();
				
				pvalColumnsPanel.setVisible(false);
				mainPanel.setVisible(true);
			}
		});
	    buttonPanel.add(keepButton);
	   
    	JButton discardButton = new JButton("Discard");
    	discardButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pvalColumnsPanel.setVisible(false);
				mainPanel.setVisible(true);
			}
		});
	   
	 	//buttonPanel.add(Box.createHorizontalStrut(10));
    	//buttonPanel.add(discardButton);
    	buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
    	buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
   
    	pvalColumnsPanel.add(Box.createVerticalStrut(20));
    	pvalColumnsPanel.add(buttonPanel);
	}
	// CAS323 change for new EvC
	private void createEvCPanel() { 
		MetaData md = theParentFrame.getMetaData();
		String [] ecList = md.getEvClist();
		String [] ecDesc = md.getEvCdesc();
		
		evidCodePanel = new JPanel();
		evidCodePanel.setLayout(new BoxLayout(evidCodePanel, BoxLayout.PAGE_AXIS)); // Y_AXIS
		evidCodePanel.setBackground(Globalx.BGCOLOR);
		evidCodePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		evidCodePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		
		evidCodePanel = Static.createPageCenterPanel();	
		String html = "<HTML><H2>Select evidence code category - see Info</H2></HTML>";
	 	JLabel header = new JLabel(html); 
	 	header.setAlignmentX(Component.CENTER_ALIGNMENT);
	 	header.setMaximumSize(header.getPreferredSize());
	 	header.setMinimumSize(header.getPreferredSize());
    	
		JPanel evcPanel = Static.createPageCenterPanel();
	   
    	chkEvCfilter = new JCheckBox[ecList.length];
    	int width=0;
    	for(int x=0; x<chkEvCfilter.length; x++) {
    		JPanel ecRow = Static.createRowPanel();
    		chkEvCfilter[x] = Static.createCheckBox(ecList[x], true);
    		if (width==0) width = chkEvCfilter[x].getPreferredSize().width;
    		
    		ecRow.add(chkEvCfilter[x]);
    		Dimension d = chkEvCfilter[x].getPreferredSize();
	       	if (d.width < 100) ecRow.add(Box.createHorizontalStrut(100 - d.width));
	       		
    	 	ecRow.add(Box.createHorizontalStrut(5));
    	 	JLabel lblDesc = new JLabel(ecDesc[x]);
    	 	lblDesc.setFont(new Font(lblDesc.getFont().getName(),Font.PLAIN,lblDesc.getFont().getSize()));
    	 	ecRow.add(lblDesc);
    	 	
    	 	evcPanel.add(ecRow);
    	}
    	evcPanel.add(Box.createVerticalStrut(10));
    	evcPanel.setMaximumSize(evcPanel.getPreferredSize());
   	    evcPanel.setMinimumSize(evcPanel.getPreferredSize());
	    	
	    JPanel lowerPanel = Static.createPageCenterPanel();
	    JPanel checkRow = Static.createRowPanel();
	    chkSelectEvCs = Static.createCheckBox("Check/uncheck all", true);
    	chkSelectEvCs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				for(int x=0; x<chkEvCfilter.length; x++)
					if (chkEvCfilter[x].isEnabled())
						chkEvCfilter[x].setSelected(chkSelectEvCs.isSelected());
			}
		});
    	checkRow.add(chkSelectEvCs);
    	checkRow.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    	lowerPanel.add(checkRow);
    	lowerPanel.add(Box.createVerticalStrut(15));
    	
    	 
    	JPanel acceptPanel = Static.createRowPanel();
	    JButton keepButton = new JButton("Accept");
	    	keepButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				evidCodePanel.setVisible(false);
				mainPanel.setVisible(true);
			}
		});
	    acceptPanel.add(keepButton);
	    lowerPanel.add(acceptPanel);
	    lowerPanel.setMaximumSize(lowerPanel.getPreferredSize());
   	    lowerPanel.setMinimumSize(lowerPanel.getPreferredSize());
	 
	    	
	    evidCodePanel.add(Box.createVerticalStrut(20));
    	evidCodePanel.add(header);
    	evidCodePanel.add(Box.createVerticalStrut(5));
    	evidCodePanel.add(evcPanel);
    	evidCodePanel.add(Box.createVerticalStrut(10));
    	evidCodePanel.add(lowerPanel);
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
		btnSelShow.setEnabled(enable);
		btnSelCopy.setEnabled(enable);
		btnSelExport.setEnabled(enable);
	}
    /**************************
     * BUILD TABLE
     */
	private void loadBuildStart() {
		txtStatus.setText("Building table....");
		
		updateTopButtons();
		btnBuildTable.setSelected(true);
		btnBuildTable.setEnabled(false);
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
	
		goTablePanel.tableRefresh();
		
		btnBuildTable.setSelected(false);
		btnBuildTable.setEnabled(true);
		btnTableShow.setEnabled(goTablePanel.getRowCount() > 0);
		btnTableExport.setEnabled(goTablePanel.getRowCount() > 0);
	}
	public void deleteFinish(int rowcnt) {
		txtStatus.setText(String.format("Results: %,d GOs", rowcnt));
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
				theVal = theVal.trim(); // CAS327
				try {
					if (loadList!=null && searchStr.startsWith(loadStart)) { // gonums checked in loadFile
						theStatusStr = "GO ID=" + searchStr;
						String x = Static.addQuoteDBList(loadList);
						theWhereStr = " where go_info.gonum  IN ("  + x + ")"; 
					}
					else {
						theStatusStr="GO ID=" + String.format(GO_FORMAT, Integer.parseInt(theVal));
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
			if (evColumnNames.length > 0 && chkUseEvC.isSelected()) {
				tmp = makeQueryEvCClause();
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
			if (pvalColumnNames.size() > 0 && chkUseEnrich.isSelected()) {
				tmp = makeQueryDEClause();
				theWhereStr = strAndMerge(theWhereStr, tmp);
			}
			if (theWhereStr.equals("")) theWhereStr=" ";
			else theWhereStr = " where " + theWhereStr;
			theStatusStr = "    Filter: " + theStatusStr;
			
			if (chkUseEnrich.isSelected())
				theStatusStr += "   #Seqs: " + boxDEseq.getSelectedItem();
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
		if (pvalColumnNames.size()==0 || !chkUseEnrich.isSelected()) return null;
		
		String deCol=null;
		for (int i = 0; i < pvalColumnNames.size(); i++) {
			if (chkPvalColFilter[i].isSelected()) {
				if (deCol!=null) return null;
				deCol = pvalColumnNames.get(i);
			}
		}
		return deCol;
	}
	private String makeQueryDEClause()
	{
		if (pvalColumnNames.size()==0 || !chkUseEnrich.isSelected()) return "";
		String clause = "", summary="";
		
		String thresh = txtCutoff.getText().trim();
		if (thresh.startsWith("e") || thresh.startsWith("E")) thresh = "1" + thresh;
		
		String ops = chkPvalAny.isSelected() ? "|" : "&";
		String opc = chkPvalAny.isSelected() ? " or " : " and ";
		String pCol = "go_info." + Globals.PVALUE ;
		
		for (int i = 0; i < pvalColumnNames.size(); i++){
			if (chkPvalColFilter[i].isSelected()) {
				if (!summary.equals("")) {
					clause += opc;
					summary += ops;
				}
				summary += pvalColumnNames.get(i);
			
				clause += pCol + pvalColumnNames.get(i) + "<" + thresh; 
			}
		}
		if (clause == "") {
			chkUseEnrich.setSelected(false);
			enablePvalSection(false); 
			return "";
		}
		
		summary = "Pval<" + thresh + " " + summary;
		theStatusStr = strMerge(theStatusStr,summary);
				
		return " (" + clause + ") ";
	}
	private String makeQueryEvCClause()
	{
		int cnt=0;
		for(int x=0; x<chkEvCfilter.length; x++)
			if (chkEvCfilter[x].isSelected()) cnt++;
		if (cnt==evColumnNames.length || cnt==0) return ""; // ecColumNames are only ones in DB
		
		String ops = "|";
		String opc = " or ";
		
		String [] allNames = theParentFrame.getMetaData().getEvClist();
		String clause = "", summary="";
		cnt=0;
		for(int x=0; x<chkEvCfilter.length; x++)
			if (chkEvCfilter[x].isSelected()) {
				if (clause != "") clause += opc;
				clause += Globalx.GO_EvC + allNames[x] + "!=''";
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
	 *  Enrich: #nSeq Up, Down, either
	 *  return: e.g. select c.contigid from pja_unitrans_go as g
	 *	join contig as c on c.CTGID=g.CTGID 
	 *	where (abs(c.P_RoSt) < 0.05 and c.P_RoSt<0) and g.gonum
	 */	
	public String  makeNseqClause(boolean doWhere) {
		if (!chkUseEnrich.isSelected() || radUseSearch.isSelected()) return ""; 
		int x = boxDEseq.getSelectedIndex();
		if (x==0) return "";
		
		String nSeqOp = null, clause=""; 
		if (x==2) nSeqOp=">0"; else if (x==3) nSeqOp="<0";
		String deOp = (chkPvalAny.isSelected()) ? " or " : " and "; 
		
		for (int i = 0; i < pvalColumnNames.size(); i++) {
			if (chkPvalColFilter[i].isSelected()) {
				String de = pvalColumnNames.get(i);
				String deTabCol = Globals.PVALUE + de; 
				double cutoff = 0.05;
				if (goPvalMapWithP.containsKey(deTabCol)) 
					 cutoff = goPvalMapWithP.get(deTabCol); // CAS322 was de
				else Out.PrtWarn(de + " does not have a stored cutoff, use default 0.05");
				
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
			evColumnNames = theParentFrame.getMetaData().getEvClist();
			
			theTermTypes = Globalx.GO_TERM_LIST;
			
			DBConn mDB = theParentFrame.getNewDBC();
			MAX_LEVEL = mDB.executeCount("select max(level) from go_info");
			mDB.close();
			
			goPvalMapWithP =   theParentFrame.getMetaData().getGoPvalPcolMap();
			
			String [] cols = theParentFrame.getMetaData().getGoPvalCols();
			pvalColumnNames = new Vector<String>();
			for (int i = 0; i < cols.length; i++)
	            pvalColumnNames.addElement(cols[i]);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Query failed ");
			ErrorReport.prtReport(e, "Error getting count names");
		}
	}
	
	public JCheckBox [] getPvalSelect() { return chkPvalColFilter;}
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
				String msg = "Incorrect GO IDs:\n" + bad;
				JOptionPane.showMessageDialog(null, 
						msg, "Incorrect GO IDs", JOptionPane.PLAIN_MESSAGE);
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
	private void enablePvalSection(boolean enable)
    {
	 	lblDErow.setEnabled(enable);
	 	
		lblCutoff.setEnabled(enable);
	    txtCutoff.setEnabled(enable);
	  
	    btnPval.setEnabled(enable);
	    lblnDEseq.setEnabled(enable);
	    boxDEseq.setEnabled(enable);
    }
    
    private void enabledFunctions(boolean b) {
		btnBuildTable.setEnabled(b);
		btnAddTable.setEnabled(b);
    }
	private void clear() {
		loadList=null; 
		loadStart="";
		txtSubString.setText("");
		
		/* CAS324
		chkGOID.setSelected(true);
		lblGOID.setEnabled(true);
		lblDesc.setEnabled(false);
		*/
		
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
			
		if (chkUseEnrich!=null) {
			chkUseEnrich.setSelected(false);
			txtCutoff.setText(DEF_PVAL);
			boxDEseq.setSelectedIndex(0);
			enablePvalSection(false);
		}
		if (chkUseEvC!=null) {
			chkUseEvC.setSelected(false);
			btnEvC.setEnabled(false);
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
		if (chkUseEvC!=null) chkUseEvC.setEnabled(bfilter);
		
		// Level
		lblRange.setEnabled(bfilter);lblTo.setEnabled(bfilter);
		cmbTermTypes.setEnabled(bfilter);
		chkLevelSpecific.setEnabled(bfilter); chkLevelRange.setEnabled(bfilter);
		txtLevelSpecific.setEnabled(bfilter); txtLevelMin.setEnabled(bfilter); txtLevelMax.setEnabled(bfilter);
		if (chkSlims!=null) chkSlims.setEnabled(bfilter);
		
		// DE
	  	if (pvalColumnNames.size() > 0) {
	  		chkUseEnrich.setEnabled(bfilter);
	  	}
		if (!bfilter) {
			if (evColumnNames.length>0)   btnEvC.setEnabled(bfilter);
			if (pvalColumnNames.size() > 0) btnPval.setEnabled(bfilter);
		}
		else {
			if (evColumnNames.length>0)
				if (chkUseEvC.isSelected()) btnEvC.setEnabled(bfilter);
			if (pvalColumnNames.size() > 0)
				if (chkUseEnrich.isSelected()) btnPval.setEnabled(bfilter);
		}
	}
	private void setPvalLabel() {
		int cnt=0; 
		String name="";
		for(int x=0; x<chkPvalColFilter.length; x++)
			if (chkPvalColFilter[x].isSelected()) {
				cnt++;
				name = chkPvalColFilter[x].getText();
			}
		if (cnt==0) {
			btnPval.setText(pvalColLabel);
			chkUseEnrich.setSelected(false);
			enablePvalSection(false);
		}
		else {
			if (cnt==1) btnPval.setText(name);
			else if (chkPvalAll.isSelected()) btnPval.setText("Every " + cnt);
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
	private JButton btnViewSeqs = null, btnSelCopy = null, btnSelShow = null, btnSelExport = null;
	private JButton btnTableShow = null, btnTableExport = null;
	
	//Column select panel
	private BasicGOTablePanel goTablePanel=null;
	
	// EC panel	
	private JPanel evidCodePanel=null;
	private JCheckBox chkSelectEvCs = null;
	private JCheckBox [] chkEvCfilter = null;
	
	// DE Panel
	private JPanel pvalColumnsPanel=null;
	private JCheckBox chkSelectPvals = null;
	private JCheckBox [] chkPvalColFilter = null;
		
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
	private JLabel lblSeqHit = null, lblHitGO;
	private JCheckBox chkUseEval = null, chkUseNSeq = null, chkUseEvC = null;
	private JTextField txtEvalVal = null, txtNSeqVal = null;
	private JButton btnEvC=null;
			
	// Level row
	private JLabel lblSpecific = null, lblRange = null, lblTo = null;
	private ButtonComboBox cmbTermTypes = null;
	private JRadioButton chkLevelSpecific = null, chkLevelRange = null;
	private JTextField txtLevelSpecific = null, txtLevelMin = null, txtLevelMax = null;
	private JLabel lblSlims = null;
	private JCheckBox chkSlims = null;
	
	// Enrich row
	private JCheckBox chkUseEnrich = null;
	private JLabel lblCutoff = null, lblnDEseq=null, lblDErow=null, lblPvalAny=null, lblPvalAll=null;
	private JTextField txtCutoff = null;
	private JButton btnPval=null;
	private JRadioButton chkPvalAny = null, chkPvalAll = null;
	private ButtonComboBox boxDEseq = null;

	// Last row
	private JButton btnBuildTable = null, btnAddTable = null, btnSelectColumns = null;

	//Data members
	private int nLevelMin = -1, nLevelMax = -1;
	private STCWFrame theParentFrame = null;
	
	private Vector <String> loadList = null;
	private String loadStart=""; 
	
	private String []      evColumnNames = null;

	private String [] theTermTypes = null;
	private String currentGO = null; 
	private BasicGOLoadFromDB loadObj=null;
	private TreeMap <String, Double> goPvalMapWithP = null;
	private Vector<String> pvalColumnNames = null;   // dePvalMap.keySet - P_
	
	boolean singleMode = false;
	private String theStatusStr="";
	// keep join on contig - much faster
	private String sqlForGoSeqs = " FROM pja_unitrans_go as p " +
			" JOIN contig as c ON c.CTGID = p.CTGID WHERE ";
}
