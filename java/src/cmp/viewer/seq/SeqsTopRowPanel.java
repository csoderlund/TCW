package cmp.viewer.seq;

/*******************************************************
 Group, Pairs and sequences call this to create a sequence table
 
 CAS503 removed Blast... button
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.ui.UserPrompt;
import cmp.align.PairAlignData;
import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.pairs.PairTablePanel;
import cmp.viewer.seqDetail.SeqTopRowPanel;
import cmp.viewer.table.FieldData;
import cmp.viewer.table.TableUtil;

public class SeqsTopRowPanel extends JPanel  {
	private static final long serialVersionUID = 1L;
	private Color buttonColor = Color.LIGHT_GRAY; // new Color(230, 230, 255); 
	
	private static final String SEQID = FieldData.SEQID;
	private static final String HITID = FieldData.HITID;
	private static final String HITDESC = FieldData.HITDESC;
	
	private static final int bSEQ=Globals.bSEQ;
	private static final int bPAIR=Globals.bPAIR;
	private static final int bGRP=Globals.bGRP;
	private final static int MUSCLE = Globals.Ext.MUSCLE;
	private final static int MAFFT = Globals.Ext.MAFFT;
	
	private static final String helpHTML = "SeqTable.html";
	
	// List Sequences or Filter Sequences
	public SeqsTopRowPanel(MTCWFrame parentFrame, String tab) {
		//if (tab.startsWith(MTCWFrame.MENU_PREFIX)) is a list
		
		SeqsQueryPanel theQueryPanel = parentFrame.getSeqsQueryPanel();
		if (theQueryPanel!=null) {
			strSubQuery = theQueryPanel.getSQLwhere();
			strQuerySummary =  theQueryPanel.getQuerySummary();
		}
		else buildShortList(parentFrame);
		
		viewType= bSEQ;
		buildPanel(parentFrame, tab, strQuerySummary, strSubQuery, -1);
	}
	
	// from Cluster table 
	public SeqsTopRowPanel(MTCWFrame parentFrame, GrpTablePanel parentList, 
			String tab, String summary, String subQuery, int grpID, String hitStr, int rowNum) {
		theGrpTable = parentList;
		this.grpID = grpID;
		consensusHitID = hitStr; // CAS305
		
		viewType=bGRP;
		
		buildPanel(parentFrame, tab, summary, subQuery, rowNum);
	}
	// from Pair table
	public SeqsTopRowPanel(MTCWFrame parentFrame, PairTablePanel parentList, 
			String tab, String summary, String query, int rowNum) {
		thePairTable = parentList;
		
		viewType = bPAIR;
		buildPanel(parentFrame, tab, summary, query, rowNum);
	}
	private void buildPanel(MTCWFrame parentFrame, String tab, 
			String summary, String sqlWhere, int rownum) {
		theViewerFrame = parentFrame;
		hasGOs = (theViewerFrame.getInfo().getCntGO()>0);
		hasNTdbOnly = (theViewerFrame.getnNTdb()>0 && theViewerFrame.getnAAdb()==0);// CAS304 add AA check
		hasDBalign = (theViewerFrame.getInfo().hasDBalign()); 
		
		tabName = tab;
		strQuerySummary = summary;
		nParentRow = rownum;
		
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentY(LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		
		theSeqTable = new SeqsTablePanel(parentFrame, this, tab, summary, sqlWhere,  viewType);
		add(theSeqTable); // allows thread to write progress to panel
		theSeqTable.buildQueryThread();
	}
	
	public void buildPanel() {  
		removeAll();
		upperPanel = Static.createPagePanel(); 
	    JPanel topRow = create1stRow();
	    upperPanel.add(topRow);
	    upperPanel.add(Box.createVerticalStrut(5)); 
		upperPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, (int)upperPanel.getPreferredSize ().getHeight() ) );
	    add(upperPanel);
	     
	    theTablePanel = Static.createPagePanel();
	    JPanel botRow = create2ndRow();
	    theTablePanel.add(botRow);
	    theTablePanel.add(Box.createVerticalStrut(5));
	    theTablePanel.add(theSeqTable);
	    
	    lowerPanel = Static.createPagePanel(); 
	    lowerPanel.add(theTablePanel);
	    add(lowerPanel);
	}
	private void buildShortList(MTCWFrame parentFrame) {
		DBinfo info = parentFrame.getInfo();
		String x = info.getSampleSeq();
		if (x==null || x=="") return;
		String [] y = x.split(":");
		if (y.length!=2) Out.PrtError("seq Build short list '" + x + "'");
		strSubQuery = y[1];
		strQuerySummary = y[0];
	}
	/**************************************************************************/
	private JPanel create1stRow() {
	    JPanel topRow = Static.createRowPanel();
	     	
	    btnTablePanel = Static.createButton("Table", true);
	    btnTablePanel.addActionListener(new ActionListener() {
    	   	public void actionPerformed(ActionEvent arg0) {
    	   		setInActive();
    	   		btnTablePanel.setBackground(buttonColor);
    	   		btnTablePanel.setSelected(true);
    	   		opTable();
    	   	}
	    });
	    topRow.add(btnTablePanel);
	    btnTablePanel.setBackground(buttonColor);
   		btnTablePanel.setSelected(true);
	    topRow.add(Box.createHorizontalStrut(3));
	          
	 	topRow.add(new JLabel("Align (selected): "));   
	 	topRow.add(Box.createHorizontalStrut(3));
     	  
	 	createBtnPairAlign();
        topRow.add(btnPairAlign);
        topRow.add(Box.createHorizontalStrut(3)); 
        
        createBtnMultiDB();
        if (viewType==bGRP && hasDBalign && consensusHitID!=null) { // CAS305 if null, multiple selections
    		topRow.add(btnMSAdb);
    		topRow.add(Box.createHorizontalStrut(3)); 
        }
      
        createBtnMultiAlign();
        topRow.add(btnMSArun);
		topRow.add(Box.createHorizontalStrut(3)); 
        topRow.add(Box.createHorizontalStrut(10)); 
        
        createBtnTable();
		topRow.add(btnTable);
	    topRow.add(Box.createHorizontalGlue());
	    
	    if(nParentRow >= 0) { // if -1, then showing members from multiple clusters, and no Next/Prev
	 	   JPanel rowChangePanel = Static.createRowPanel();
	 	   rowChangePanel.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
	 	   
	 	   btnPrevRow = Static.createButton("<< Prev", true);
	 	   btnPrevRow.addActionListener(new ActionListener() {
	 		   public void actionPerformed(ActionEvent arg0) {
	 			   if (theGrpTable!=null) nParentRow = theGrpTable.getTranslatedRow(nParentRow - 1);
	 			   else 					  nParentRow = thePairTable.getTranslatedRow(nParentRow - 1);
	 			   loadNewRow(nParentRow);
	 		   }
	 	   });
	 	  
	 	   btnNextRow = Static.createButton("Next >>", true);
	 	   btnNextRow.addActionListener(new ActionListener() {
	 		   public void actionPerformed(ActionEvent arg0) {
	 			   if (theGrpTable!=null) nParentRow = theGrpTable.getTranslatedRow(nParentRow + 1);
	 			   else  				 nParentRow = thePairTable.getTranslatedRow(nParentRow + 1);
	 			   loadNewRow(nParentRow);
	 		   }
	 	   });
	 	   
	 	   rowChangePanel.add(btnPrevRow);
	 	   rowChangePanel.add(Box.createHorizontalStrut(1));
	 	   rowChangePanel.add(btnNextRow);
	 	   
	 	   topRow.add(rowChangePanel);
	    }
	    return topRow;
	}
	private JPanel create2ndRow() {
	 	JPanel botRow = Static.createRowPanel();
	 	botRow.add(new JLabel(" Selected:"));
	 	botRow.add(Box.createHorizontalStrut(3));
	 	    
	    	 btnViewDetails = Static.createButton(MTCWFrame.SEQ_DETAIL, false, Globals.FUNCTIONCOLOR);
	     btnViewDetails.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewDetailsTab();
			}
	     });
	     botRow.add(btnViewDetails);
	     botRow.add(Box.createHorizontalStrut(3));
	
	     btnViewGroups = Static.createButton(MTCWFrame.GRP_TABLE, false, Globals.FUNCTIONCOLOR);
	     btnViewGroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewClustersTab();
			}
	     });
	     botRow.add(btnViewGroups);
	     botRow.add(Box.createHorizontalStrut(3));
	     
	     btnViewPairs = Static.createButton(MTCWFrame.PAIR_TABLE, false, Globals.FUNCTIONCOLOR);
	     btnViewPairs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewPairsTab();
			}
	     });
	     botRow.add(btnViewPairs);
	     botRow.add(Box.createHorizontalStrut(3));
    	     
 		createBtnCopy();
		botRow.add(btnCopy);
	    botRow.add(Box.createHorizontalStrut(3));
	     
	    botRow.add(Box.createHorizontalStrut(3));
	    JButton btnClear = Static.createButton("Clear", true);
	    btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				theSeqTable.clearSelection();
			}
		});
	    botRow.add(btnClear);
	    botRow.add(Box.createHorizontalStrut(5));
	    botRow.add(Box.createHorizontalGlue());
	    
	    btnHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
	    btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame, 
						"Sequence table", "html/viewMultiTCW/" + helpHTML);
			}
		});
	    botRow.add(btnHelp);
	
		return botRow;
	}
	private void createBtnPairAlign() {
		btnPairAlign = Static.createButton("Pairwise...", true);
		btnPairAlign.addActionListener(new ActionListener() {
	   		public void actionPerformed(ActionEvent arg0) {
    	   		setInActive();
    	   		btnPairAlign.setBackground(buttonColor);
    	   		btnPairAlign.setSelected(true);
	   		}
		});
		btnPairAlign.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus("Pairwise alignments of all or selected sequences (AA,NT) or selected pair (AA,CDS,NT)");}
			public void mouseExited(MouseEvent e) {theViewerFrame.setStatus("");}
		});
	    final JPopupMenu pairPop = new JPopupMenu();
	    pairPop.setBackground(Color.WHITE);
	    itemAA = new JMenuItem(new AbstractAction("AA for each pair") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairAA();
 			}
 		});
	    pairPop.add(itemAA);
	    
	    itemNT = new JMenuItem(new AbstractAction("NT for each pair") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairNT();
 			}
 		});
	    itemCDS = new JMenuItem(new AbstractAction("AA,CDS,NT for one pair") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairCDS();
 			}
 		});
	    itemUTR = new JMenuItem(new AbstractAction("5UTR,CDS,3UTR for one pair ") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairUTR();
 			}
 		});
	    itemHIT0 = new JMenuItem(new AbstractAction("AA to sequence best hit") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opSeqHit0();
 			}
 		});
	    itemHIT1 = new JMenuItem(new AbstractAction("AA to cluster best hit") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opSeqHit1();
 			}
 		});
	    
	    if (hasNTdbOnly) { // Not available for protein sTCWs
		    pairPop.add(itemNT);
		    pairPop.add(itemCDS);
		    pairPop.add(itemUTR);
	    }
	    pairPop.add(itemHIT0);
	    if (viewType == bGRP) pairPop.add(itemHIT1); // no consensus hit
	    
	    btnPairAlign.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				pairPop.show(e.getComponent(), e.getX(), e.getY());
			}
	    });
	}
	private void createBtnMultiDB() {
		btnMSAdb = Static.createButton("MSAdb", true);
		btnMSAdb.addActionListener(new ActionListener() {
	   		public void actionPerformed(ActionEvent arg0) {
	   			setInActive();
	   			btnMSAdb.setBackground(buttonColor);
	   			btnMSAdb.setSelected(true);
       	   		opMultiDB();
	   		}
		});
	}
	private void createBtnMultiAlign() {
		btnMSArun = Static.createButton("MSA...", true);
		btnMSArun.addActionListener(new ActionListener() {
	   		public void actionPerformed(ActionEvent arg0) {
	   			setInActive();
	   			btnMSArun.setBackground(buttonColor);
	   			btnMSArun.setSelected(true);
 	   		}
		});
		
		btnMSArun.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus("Multiple alignment of all sequences or selected set");}
			public void mouseExited(MouseEvent e) {theViewerFrame.setStatus("");}
		});
		
	    final JPopupMenu multiPop = new JPopupMenu();
	    multiPop.setBackground(Color.WHITE);
	    
	    JMenuItem itemMuscle = new JMenuItem(new AbstractAction("MUSCLE-AA") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MUSCLE, Globals.AA);
 			}
 		});
	    multiPop.add(itemMuscle);
	    
	    JMenuItem itemMafftAA = new JMenuItem(new AbstractAction("MAFFT-AA") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MAFFT, Globals.AA);
 			}
 		});
	    multiPop.add(itemMafftAA);
	    
	    JMenuItem itemMafftCDS = new JMenuItem(new AbstractAction("MAFFT-CDS") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MAFFT, Globals.CDS);
 			}
 		});
	    if (hasNTdbOnly) multiPop.add(itemMafftCDS);
	    
	    JMenuItem itemMafftNT = new JMenuItem(new AbstractAction("MAFFT-NT") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MAFFT, Globals.NT);
 			}
 		});
	    if (hasNTdbOnly) multiPop.add(itemMafftNT);
	    
	    btnMSArun.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				multiPop.show(e.getComponent(), e.getX(), e.getY());
			}
	    });
	}
	private void createBtnCopy() {
		btnCopy = Static.createButton("Copy...", false);
	    final JPopupMenu copyPop = new JPopupMenu();
	    copyPop.setBackground(Color.WHITE);
	    
 		copyPop.add(new JMenuItem(new AbstractAction(SEQID) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				String seqid = theSeqTable.getSelectedColumn(SEQID);
 				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
 				cb.setContents(new StringSelection(seqid), null);
 			}
 		}));
		copyPop.add(new JMenuItem(new AbstractAction("Hit ID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String hitid = theSeqTable.getSelectedColumn(HITID);
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(hitid), null);
			}
		}));
		copyPop.add(new JMenuItem(new AbstractAction("Hit Description") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String desc = theSeqTable.getSelectedColumn(HITDESC);
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(desc), null);
			}
		}));
		copyPop.add(new JMenuItem(new AbstractAction("AA Sequence") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				int index = theSeqTable.getSelectedSQLid();
				String id = loadSeq(index);
				if (id!=null) {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(">" + id + "\n" + aaSeq), null);
				}
			}
		}));
		copyPop.add(new JMenuItem(new AbstractAction("CDS Sequence") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				int index = theSeqTable.getSelectedSQLid();
				String id = loadSeq(index);
				if (id!=null) {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(">" + id + "\n" + cdsSeq), null);
				}
			}
		}));
		copyPop.add(new JMenuItem(new AbstractAction("NT Sequence") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				int index = theSeqTable.getSelectedSQLid();
				String id = loadSeq(index);
				if (id!=null) {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(">" + id + "\n" + ntSeq), null);
				}
			}
		}));
		
		btnCopy.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				copyPop.show(e.getComponent(), e.getX(), e.getY());
			}
	    });
	}
	private void createBtnTable() {
		final JPopupMenu popup = new JPopupMenu();	
		 popup.setBackground(Color.WHITE);
		 
 		popup.add(new JMenuItem(new AbstractAction("Show Column Stats") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil().statsPopUp("Sequence: " + strQuerySummary, theSeqTable.getTable());	
 			}
 		}));
 		popup.addSeparator();
 		popup.add(new JMenuItem(new AbstractAction("Copy Table") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				Thread copyThread = new Thread(new Runnable() {
					public void run() {
						try {											
							Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
							String table = new TableUtil(theViewerFrame).createTableString(theSeqTable.getTable());
							cb.setContents(new StringSelection(table), null);
						} catch (Exception e) {ErrorReport.reportError(e, "Error copy table"); }
					}
				});
				copyThread.setPriority(Thread.MIN_PRIORITY);
				copyThread.start();
 			}
 		}));
 		popup.addSeparator();
 		popup.add(new JMenuItem(new AbstractAction("Export table (" + Globalx.CSV_SUFFIX + ")") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportTableTab(theSeqTable.getTable(), Globals.bSEQ);
 			}
 		}));
 		
 		popup.add(new JMenuItem(new AbstractAction("Export AA sequences (" + Globalx.FASTA_SUFFIX + ")") {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				new TableUtil(theViewerFrame).exportSeqFa(theSeqTable.getTableSQLid(), FieldData.AASEQ_SQL, false);
 			}
 		}));
 		if (hasNTdbOnly) {
	 		popup.add(new JMenuItem(new AbstractAction("Export CDS sequences (" + Globalx.FASTA_SUFFIX + ")") {
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportSeqFa(theSeqTable.getTableSQLid(), FieldData.NTSEQ_SQL, true);
	 			}
	 		}));
 		
	 		popup.add(new JMenuItem(new AbstractAction("Export NT sequences (" + Globalx.FASTA_SUFFIX + ")") {
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportSeqFa(theSeqTable.getTableSQLid(), FieldData.NTSEQ_SQL, false);
	 			}
	 		}));
 		}
 		// XXX
 		if (hasGOs) {
 			popup.addSeparator();
	 		popup.add(new JMenuItem(new AbstractAction("Export all GOs (" + Globalx.CSV_SUFFIX + ")...") {
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				new TableUtil(theViewerFrame).exportSeqGO(theSeqTable.getTableData(), strQuerySummary);
	 			}
	 		}));
 		}
 	
 		btnTable = Static.createButton("Table...", true);
    		btnTable.addMouseListener(new MouseAdapter() {
    	          public void mousePressed(MouseEvent e) {
    	              popup.show(e.getComponent(), e.getX(), e.getY());
    	          }
    	      });
	}
	
	private void loadNewRow(int rowNum) {
		String [] strVals;
		if (theGrpTable!=null) {
			  strVals = theGrpTable.getSeqQueryNext(rowNum);
			  grpID = Static.getInteger(strVals[3]);
		}
		else  {
			strVals = thePairTable.getSeqQueryNext(rowNum);
		}
		upperPanel.removeAll();
		lowerPanel.removeAll();
		
		// Clear so recreated for next pair/cluster. The GrpTable or PairTable stay the same.
		theTablePanel=theSeqTable=null;
		theCDSpairPanel=null;
		theAAPairPanel=null; 
		theNTPairPanel=null;
		theMultiDBPanel=null;
		theMultiPanel[0]=theMultiPanel[1]=theMultiPanel[2]=null;
		
		setVisible(false);
		buildPanel(theViewerFrame, strVals[0], strVals[1], strVals[2], rowNum);
	
        theViewerFrame.changePanelName(this, tabName, strQuerySummary);
		setVisible(true);
	}
	
	private void opTable () {
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		lowerPanel.add(theTablePanel);
		setVisible(false);
		setVisible(true);
	}
	private void opPairCDS() {
		if (!theSeqTable.correctType("AA")) return; 
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		if (tooManyPairwise()) return;
		
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		if (theCDSpairPanel!=null &&
			oldCDSset[0].equals(curSeqSet[0]) && oldCDSset[1].equals(curSeqSet[1])) 
		{
			lowerPanel.add(theCDSpairPanel);
		}
		else {
			oldCDSset = curSeqSet;
			
			int [] theLens = theSeqTable.getPairLens(); 
			theCDSpairPanel = new AlignPairView3Panel(theViewerFrame, curSeqSet, theLens, 0); 
			lowerPanel.add(theCDSpairPanel);		
		}
		setVisible(false);
		setVisible(true);
	}
	private void opPairUTR() {
		if (!theSeqTable.correctType("AA")) return; 
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		if (tooManyPairwise()) return;
		
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		if (theUTRpairPanel!=null &&
			oldUTRset[0].equals(curSeqSet[0]) && oldUTRset[1].equals(curSeqSet[1])) 
		{
			lowerPanel.add(theUTRpairPanel);
		}
		else {
			oldUTRset = curSeqSet;
			
			int [] theLens = theSeqTable.getPairLens(); 
			theUTRpairPanel = new AlignPairView3Panel(theViewerFrame, curSeqSet, theLens, 1); 
			lowerPanel.add(theUTRpairPanel);		
		}
		setVisible(false);
		setVisible(true);
	}
	private void opPairAA() {
		if (!theSeqTable.correctType("AA")) return; // if ESTscan, not all have AA sequence
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		if (tooManyPairwise()) return;
		
		
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		if (theAAPairPanel!=null && sameSet(oldAAset, curSeqSet)) {
			 lowerPanel.add(theAAPairPanel);
		}
		else {
			oldAAset=curSeqSet;
			
			theAAPairPanel = new AlignPairViewNPanel(theViewerFrame, curSeqSet,  PairAlignData.AlignAA); 	
			lowerPanel.add(theAAPairPanel);
		}
		setVisible(false);
		setVisible(true);
	}
	private void opSeqHit0() { // CAS305 Seq to its best hit
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		if (theAAHit0Panel!=null && sameSet(oldAAset, curSeqSet)) {
			 lowerPanel.add(theAAHit0Panel);
		}
		else {
			oldAAset=curSeqSet;
			
			theAAHit0Panel = new AlignPairViewNPanel(theViewerFrame, curSeqSet,  PairAlignData.AlignHIT0_AA); 	
			lowerPanel.add(theAAHit0Panel);
		}
		setVisible(false);
		setVisible(true);
	}
	private void opSeqHit1() { // CAS305 Seq to the cluster's best hit
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		if (theAAHit1Panel!=null && sameSet(oldAAset, curSeqSet)) {
			 lowerPanel.add(theAAHit1Panel);
		}
		else {
			oldAAset=curSeqSet;
			
			theAAHit1Panel = new AlignPairViewNPanel(theViewerFrame, curSeqSet,  
					PairAlignData.AlignHIT1_AA, consensusHitID); 	
			lowerPanel.add(theAAHit1Panel);
		}
		setVisible(false);
		setVisible(true);
	}
	private void opPairNT() {
		if (!theSeqTable.correctType("NT")) return; // should always have NT if from nt single tcw
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		if (tooManyPairwise()) return;
		
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		if (theNTPairPanel!=null && sameSet(oldNTSet, curSeqSet)) {
			lowerPanel.add(theNTPairPanel);
		}
		else {
			oldNTSet = curSeqSet;
			
			theNTPairPanel = new AlignPairViewNPanel(theViewerFrame, curSeqSet, PairAlignData.AlignNT); 
			lowerPanel.add(theNTPairPanel);
		}
		setVisible(false);
		setVisible(true);
	}
	private void opMultiDB() { // already aligned, in db
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		
		if (theMultiDBPanel !=null && sameSet(oldMultiDB, curSeqSet)) {
			lowerPanel.add(theMultiDBPanel);
		}
		else {
			oldMultiDB = curSeqSet;
			theMultiDBPanel = new AlignMultiViewPanel(theViewerFrame, curSeqSet, grpID);
			lowerPanel.add(theMultiDBPanel);
		}
		setVisible(false);
		setVisible(true);
	}
	private void opMultiAlign(int alignPgm, int type) {
		if (type == Globals.AA &&  !theSeqTable.correctType("AA")) return; // one could not have AA sequence
		if (type == Globals.NT &&  !theSeqTable.correctType("NT")) return;
		if (type == Globals.CDS && !theSeqTable.correctType("NT")) return;
		
		curSeqSet = theSeqTable.getSelectedSeqIDs();
		if (curSeqSet.length>50) { // CAS305
			String msg = "Selected " + curSeqSet.length + " for multiple alignment";
			if (!UserPrompt.showContinue("Multiple align", msg)) return;
		}
		
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		String [] oldSet;
		int algIndex;
		if (alignPgm==MUSCLE) 			{oldSet = oldMuscle;  algIndex=0;}
		else if (type == Globals.AA) 	{oldSet = oldMafftAA;   algIndex=1;}
		else if (type == Globals.CDS)	{oldSet = oldMafftCDS; algIndex=2;}
		else                             {oldSet = oldMafftNT;  algIndex=3;}
		
		if (theMultiPanel[algIndex]!=null && sameSet(oldSet, curSeqSet)) {
			lowerPanel.add(theMultiPanel[algIndex]);
		}
		else {
			if (alignPgm==MUSCLE)         oldMuscle=curSeqSet;
			else if (type == Globals.AA)  oldMafftAA=curSeqSet;
			else if (type == Globals.CDS) oldMafftCDS=curSeqSet;
			else                          oldMafftNT=curSeqSet;
			
			theMultiPanel[algIndex] = new AlignMultiViewPanel(theViewerFrame, curSeqSet, alignPgm, type);
			lowerPanel.add(theMultiPanel[algIndex]);
		}
		setVisible(false);
		setVisible(true);
	}
	private boolean tooManyPairwise() { // CAS305
		if (curSeqSet.length>20) {
			int n = curSeqSet.length;
			int m = (n*(n-1))/2;
			String msg = "Selected " + n + " sequences for " + m + " alignments";
			return !UserPrompt.showContinue("Pairwise align", msg);
		}
		return false;
	}
	private boolean sameSet(String [] oldSet, String [] newSet) {
		if (oldSet==null || oldSet.length==0) return false;
		boolean same=true;
		if (newSet.length==oldSet.length) {
			for (int i=0; i<newSet.length && same; i++)
				if (!newSet[i].equals(oldSet[i])) same=false;
		}
		else same = false;
		return same;
	}
	
	private void viewDetailsTab() {
		if(theSeqTable.getSelectedRowCount() != 1) { 
			JOptionPane.showMessageDialog(theViewerFrame, "Select a Sequence");
			return;
		}
		try {
			int row = theSeqTable.getSelectedRow();
			int utid = (Integer) theSeqTable.getSelectedSQLid();
			String strid = theSeqTable.getSelectedColumn(SEQID);
			SeqTopRowPanel newPanel = new SeqTopRowPanel(theViewerFrame, theSeqTable, strid, utid, row);
			theViewerFrame.addResultPanel(getInstance(), newPanel, strid, newPanel.getSummary());
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error viewing details");}
	}
	private void viewPairsTab() {
		if(theSeqTable.getSelectedRowCount() ==0 ) { 
			JOptionPane.showMessageDialog(theViewerFrame, "Select one or more sequences");
			return;
		}
		try{		
			int [] utid = theSeqTable.getSelectedSQLids();
			String [] seqid = theSeqTable.getSelectedSEQIDs();
			String list = null, tab = null, summary=null;
			int cnt=0, num=utid.length;
			for (int i=0; i<num; i++) {
				if (list==null) {
					list = "(" + utid[i];
					tab = summary = seqid[i];
				}
				else {
					list +=  "," + utid[i];
					if (cnt<8) summary += "," + seqid[i];
				}
				cnt++;
			}
			list += ")";
			if (num>=8) summary += "...";
			if (num>1) tab = utid.length + " seqs";
			summary = "Pairs for " + summary;
			tab = MTCWFrame.PAIR_PREFIX + ": " + tab;
			
			PairTablePanel pairPanel = new PairTablePanel(theViewerFrame, getInstance(), 
					tab, summary, list, theSeqTable.getSelectedRow());
			theViewerFrame.addResultPanel(getInstance(), pairPanel, pairPanel.getName(), pairPanel.getSummary());
			
		} catch(Exception e) {ErrorReport.reportError(e, "Error viewing groups");
		} catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error viewing groups", theViewerFrame);}
	}
	 
    public String [] getPairsQueryNext(int nextRow) {
    		int row = theSeqTable.getTranslatedRow(nextRow);
    		
		String name = theSeqTable.getRowSeqName(row);
		int id = theSeqTable.getRowSeqIndex(row);
	
		String [] retVal = new String[4];
		retVal[0] = "(" + id + ")";
		retVal[1] = MTCWFrame.PAIR_PREFIX + ": " + name;
		retVal[2] = "Sequence " + name + " pairs";
		retVal[3] = row + "";
	
		return retVal;
    }
    // select multiple rows to show clusters for multiple sequences
	private void viewClustersTab() {
		if(theSeqTable.getSelectedRowCount() ==0) { 
			JOptionPane.showMessageDialog(theViewerFrame, "Select a Sequence");
			return;
		}
		try{		
			int [] utid = theSeqTable.getSelectedSQLids();
			String utstr = theSeqTable.getSelectedColumn(SEQID);
			
			DBConn mDB = theViewerFrame.getDBConnection();
			Vector<Integer> ids = new Vector<Integer> ();
			ResultSet rset=null;
			for (int id : utid) {
				rset = mDB.executeQuery("SELECT PGid FROM pog_members WHERE UTid=" + id + " GROUP BY PGid");
				while(rset.next()) {
					int grpid = rset.getInt(1);
					if (!ids.contains(grpid)) ids.add(grpid);
				}
			}
			if (rset!=null) rset.close(); 
			mDB.close();
			
			if (ids.size()==0) {
				JOptionPane.showMessageDialog(null, 
						"No Clusters for sequence '" + utstr + "'", "Warning", JOptionPane.PLAIN_MESSAGE); // CAS305 was utid[0]
				return;
			}
			String subQuery = theSeqTable.getGroupQueryList(ids.toArray(new Integer[0]));
			String tab = MTCWFrame.GRP_PREFIX + ": " + utstr;
			String summary = "Clusters for " + utstr;
			if (utid.length==2) { // CAS305
				String utstr2=theSeqTable.getSelectedColumn2(SEQID);
				if (utstr2!=null) summary += ", " + utstr2;
			}
			if (utid.length > 2) summary += " plus " + (utid.length-1);
			
			GrpTablePanel grpPanel = new GrpTablePanel(theViewerFrame, getInstance(), 
					tab, summary, subQuery);
			theViewerFrame.addResultPanel(getInstance(), grpPanel, grpPanel.getName(), grpPanel.getSummary());

			ids.clear();
			
		} catch(Exception e) {ErrorReport.reportError(e, "Error viewing groups");
		} catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error viewing groups", theViewerFrame);}
	}
	/**************************************************
	 * Copy Sequence
	 */
	private String  loadSeq(int seqIndex) {
		try {
			DBConn conn = theViewerFrame.getDBConnection();
			
			ResultSet rs = conn.executeQuery("Select ntSeq, orf_start, orf_end, aaSeq, UTstr  from unitrans where UTid=" + seqIndex);
			if (rs == null) ErrorReport.die("load Sequence nt data");
			rs.next();
			ntSeq = rs.getString(1);
			if (ntSeq!=null && ntSeq.length()>0) 
				cdsSeq = ntSeq.substring(rs.getInt(2)-1, rs.getInt(3));
			else cdsSeq="";
	
			aaSeq = rs.getString(4);
			String utStr = rs.getString(5);			
			rs.close(); conn.close();
			return utStr; // CAS305 added the utStr to the clipboard for copy sequences
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Loading ntSeq"); return null;}
	}
	
	private SeqsTopRowPanel getInstance() {return this;}
	
	/**************************************************************/
	public void setTopEnabled(int selCount, int rowCount) {
		if (btnPairAlign==null) return;
		
		if (selCount!=0) btnMSAdb.setEnabled(false); 
		else btnMSAdb.setEnabled(true);
		
		int cntSelRow = (selCount==0) ? rowCount : selCount;
		boolean b = (cntSelRow != 1) ? true : false; // 0 is do all (rowCount)
		itemNT.setEnabled(b);
		itemAA.setEnabled(b);

		btnMSArun.setEnabled(b); 
		btnPairAlign.setEnabled(b); // off if NT and AA off
		
		b = (cntSelRow == 2) ? true : false;
		itemCDS.setEnabled(b); 
		itemUTR.setEnabled(b); 
		
		b = (selCount > 0) ? true : false;
		btnViewPairs.setEnabled(b);
		btnViewGroups.setEnabled(b); 
		
		b = (selCount == 1) ? true : false;
		btnViewDetails.setEnabled(b);
		btnCopy.setEnabled(b);
	}
	
	private void setInActive() {
		// this works on mac but not linux
		btnTablePanel.setSelected(false);
		btnPairAlign.setSelected(false);
		btnMSArun.setSelected(false);
		btnMSAdb.setSelected(false);
		
		// this works on linux but not mac
		btnTablePanel.setBackground(Color.white);
		btnPairAlign.setBackground(Color.white);
		btnMSArun.setBackground(Color.white);
		btnMSAdb.setBackground(Color.white);
	}
	public String getSummary() {return strQuerySummary;}
	public String getName() {
		if(theSeqTable == null) return tabName + " In progress"; 
		return tabName; 
	}
	
	/*******************************************************/
	
	private JPanel upperPanel;
    private JButton btnTablePanel = null;
    private JButton btnPairAlign = null, btnMSArun = null, btnMSAdb = null;
    private JMenuItem itemAA, itemNT, itemCDS, itemUTR, itemHIT0, itemHIT1;
    private JButton btnNextRow = null, btnPrevRow = null;
    
    private JPanel lowerPanel;
    private String [] curSeqSet;
    private String [] oldUTRset, oldAAset, oldNTSet, oldCDSset;
    private AlignPairView3Panel theCDSpairPanel = null, theUTRpairPanel = null;
	private AlignPairViewNPanel theAAPairPanel = null, theNTPairPanel = null;
	private AlignPairViewNPanel theAAHit0Panel = null, theAAHit1Panel = null;
	private String [] oldMuscle, oldMafftAA, oldMafftNT,oldMafftCDS, oldMultiDB;
	private AlignMultiViewPanel theMultiDBPanel =  null;
	private AlignMultiViewPanel [] theMultiPanel = new AlignMultiViewPanel [4];
	
	private JPanel theTablePanel;
    private JButton btnCopy = null, btnTable = null;
    private JButton btnViewDetails = null, btnViewGroups = null, btnViewPairs = null;
    private JButton btnHelp = null;
    private SeqsTablePanel theSeqTable = null;
    
	private MTCWFrame theViewerFrame = null;
	private GrpTablePanel theGrpTable = null;
	private PairTablePanel thePairTable = null;
	
	private String ntSeq=null, cdsSeq=null, aaSeq=null; // only loads if null
	private String tabName = "";
	private String strQuerySummary = "", strSubQuery="";
	private int nParentRow = -1, grpID = -1;
	private boolean hasNTdbOnly=false, hasGOs=false, hasDBalign;
	private int viewType=0;
	private String consensusHitID=null;
}
