package sng.viewer.panels.seqDetail;

/********************************************************
 * The SeqTopRowTab does the Overview, Align DB hits, Next and Prev buttons
 * This file does the rest.
 * 
 * -Do not show Copy Hit if no hit for sequence.
 */
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.CodingRegion;
import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.dataholders.SequenceData;
import sng.viewer.STCWFrame;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.methods.FileHelpers;
import util.ui.MenuMapper;
import util.ui.MultilineTextPanel;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import util.database.DBConn;

public class SeqDetailPanel  extends JPanel implements MouseListener, ClipboardOwner {
	private static final long serialVersionUID = 4196828062981388452L;
	private static final int NUM_DISPLAY_ROWS = 15;
	private static final int MAX_TABLE_WIDTH = 550;
	
	private static final int MAX_COL = 130; // maximum size of column, e.g. description and species
	private static final String L = Globals.LIBCNT;
	private static final String LN = Globals.LIBRPKM;
	private static final String P = Globals.PVALUE;
	private static final String noGO = "---";
	
	private boolean isCAP=false;
	
	private String [] dbhits = {"Best hits (EV, AN", // completes below
			"Best per annoDB", "Distinct regions", "Unique species", 
			"Unique description", "All hits"};
	
	public SeqDetailPanel (STCWFrame frame, MultiCtgData theCluster )
	{
		theMainFrame = frame;
	
		for (int i=0; i<frameHitInfo.length; i++) {
			frameHitInfo[i]=null;
			frameHitStart[i]=frameHitEnd[i]=0;
		}
		
		metaData = frame.getMetaData();
		norm = metaData.getNorm(); // CAS304
		isAAtcw = metaData.isAAsTCW();
		if (metaData.hasGOs()) dbhits[0] += ", WG)";
		else dbhits[0] += ")";
		
		if (theCluster==null) return;
		ctgData = theCluster.getContig();
		if (ctgData==null) return; // This happens when another sTCW is opened, and CAP3 is run.
		if (ctgData.getContigID().contains("CAP3") && ctgData.getConsensusBases()==0) isCAP=true;
		
		nHits= ctgData.getCntTotal();
		hasHit = (nHits>0);
		loadHitData(); // initializes even if nHits=0
		loadDBdata();
		
		createPanel();
		createSecondRow ();
		
		// QueryTab creates a scroller, but this still appears necessary
		scroller = new JScrollPane ( ctgViewPanel );
		scroller.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scroller.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		scroller.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		UIHelpers.setScrollIncrements( scroller );
        
		// Add to the main panel
		setLayout( new BoxLayout ( this, BoxLayout.PAGE_AXIS ) );
		if (!isCAP) add ( toolPanel );
		add ( Box.createVerticalStrut(5) );
		add ( scroller );	
	}	

	private void createPanel() {
		String textArea = createTextArea();
		if (nHits>0) {
			makeHitTable();
			createTablePanel();
		}
		
		ctgViewPanel = Static.createPagePanel();
		ctgViewPanel.add( Box.createVerticalStrut(10) );
		super.setBackground(Color.WHITE);
		
		textAreaTop = new JTextArea ( textArea );
		textAreaTop.setEditable( false );		
		textAreaTop.setFont(new Font("monospaced", Font.PLAIN, 12));
		int nTextWidth = Math.max((int)textAreaTop.
				getPreferredSize().getWidth() + 5, 600);		
		textAreaTop.setMaximumSize( new Dimension ( nTextWidth, 
				(int)textAreaTop.getPreferredSize().getHeight() ) );	
		textAreaTop.addMouseListener(this);

		JPanel tableAndHeader = Static.createPagePanel();
		textAreaTop.setAlignmentX(LEFT_ALIGNMENT);
		tableAndHeader.add ( textAreaTop );

		if(theHitTable != null)
			tableAndHeader.add ( resultScroll );
		
		if (nHits > 0 && !FileHelpers.isMac()) {// URL doesn't work on Mac, but does in linux
			
			MultilineTextPanel textPanel = null;
			try {
				textPanel = new MultilineTextPanel ( Globals.textFont, 
					ctgData.getUniProtURL(), 1, 200, 1 ); 
		        textPanel.setBackground( Color.WHITE );   
			} catch (Exception e) {
				ErrorReport.reportError(e, "Internal error: making MultilineTextPanel");
			}
		
			tableAndHeader.add( Box.createVerticalStrut(5) );
			tableAndHeader.add( textPanel );
			tableAndHeader.add( Box.createVerticalStrut(5) );			
		}
		tableAndHeader.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
			
		ctgViewPanel.add( tableAndHeader );
		ctgViewPanel.add( Box.createVerticalStrut(10) );			
		invalidate();
		
		ctgViewPanel.add ( Box.createVerticalGlue() );	
	}
	
	private JPanel createSecondRow ( )
	{		
		menuLib = new JComboBox <MenuMapper> ();
		Dimension dim = new Dimension ( 100, (int)menuLib.getPreferredSize().getHeight() );
		menuLib.setPreferredSize( dim );
		menuLib.setMaximumSize ( dim );
		menuLib.addItem( new MenuMapper ( "Counts", 1 )); 
		menuLib.addItem( new MenuMapper ( norm, 2 )); 
		if (metaData.hasReps()) menuLib.addItem( new MenuMapper ( "Replicates", 3 )); 

		menuLib.setBackground(Color.WHITE);
		menuLib.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					MenuMapper selection = (MenuMapper)menuLib.getSelectedItem();
					int type = selection.asInt();
					if (type == 1)       {normLibs = false; repLibs=false;}
					else if	(type == 2)	 {normLibs = true; repLibs=false;}
					else                 {normLibs=false; repLibs=true;}
					redraw();
				}
			}
		);		
		/// DB Hits
		menuHit = new JComboBox <MenuMapper> ();
		dim = new Dimension ( 180, (int)menuLib.getPreferredSize().getHeight() );
		menuHit.setPreferredSize( dim );
		menuHit.setMaximumSize ( dim );
		menuHit.addItem( new MenuMapper ( dbhits[0], 16 ) );
		menuHit.addItem( new MenuMapper ( dbhits[1], 2 ) );
		menuHit.addItem( new MenuMapper ( dbhits[2], 1 ) );
		menuHit.addItem( new MenuMapper ( dbhits[3], 4 ) );
		menuHit.addItem( new MenuMapper ( dbhits[4], 8 ) );
		menuHit.addItem( new MenuMapper ( dbhits[5], 0));
		menuHit.setBackground(Color.WHITE);
		menuHit.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					MenuMapper selection = (MenuMapper)menuHit.getSelectedItem();
					int type = selection.asInt();
					filterType = type;	
					redraw();
				}				
			}
		);	
		/////////////////////////////
		final JPopupMenu copypopup = new JPopupMenu();
		
		copypopup.add(new JMenuItem(new AbstractAction("Seq ID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				saveTextToClipboard(ctgData.getContigID());
			}
		}));
		
		if (!metaData.bUseOrigName()) {// CAS311
			copypopup.add(new JMenuItem(new AbstractAction(metaData.getLongLabel()) {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					saveTextToClipboard(ctgData.getLongest());
				}
			}));
		}
		copypopup.add(new JMenuItem(new AbstractAction("Sequence") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				saveTextToClipboard(getUnitransString());
			}
		}));
		
		if (!isAAtcw) {
 			copypopup.add(new JMenuItem(new AbstractAction("Reverse complement") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					saveTextToClipboard(getRevCompString());
				}
			}));
	
 			if (metaData.hasORFs()) { // CAS304
				copypopup.add(new JMenuItem(new AbstractAction("Translated ORF (best frame)") {
					private static final long serialVersionUID = 4692812516440639008L;
					public void actionPerformed(ActionEvent e) {
						String x = getORFtransString();
						if (x!=null) saveTextToClipboard(x);
						else {
							JOptionPane.showMessageDialog(theMainFrame, 
									"No frame for sequence. Cannot translate.", 
									"Translated", JOptionPane.PLAIN_MESSAGE);
						}
					}
				}));
 			}
		}
		if (nHits>0) { 
			copypopup.addSeparator();
			copypopup.add(new JMenuItem(new AbstractAction("Selected hit - Hit ID") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					if (theHitTable==null) return;
					int [] selections = theHitTable.getSelectedRows();
					if (selections.length==0)
						JOptionPane.showMessageDialog(theMainFrame, 
								"Select a hit in the DB hits table.", 
								"Selected hit", JOptionPane.PLAIN_MESSAGE);
					else { 
						saveTextToClipboard(theHitData.get(selections[0]).hitName);
					}
				}
			}));
			copypopup.add(new JMenuItem(new AbstractAction("Selected hit - sequence") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					int [] selections = theHitTable.getSelectedRows();
					if (selections.length==0)
						JOptionPane.showMessageDialog(theMainFrame, 
								"Select a hit in the DB hits table.", 
								"Selected hit", JOptionPane.PLAIN_MESSAGE);
					else { 
						saveTextToClipboard(loadHitSequence(theHitData.get(selections[0]).hitName));
					}
				}
			}));
		}	
		JButton jbCopy = Static.createButton("Copy...", true);
		jbCopy.addMouseListener(new MouseAdapter() {
	            public void mousePressed(MouseEvent e) {
	                copypopup.show(e.getComponent(), e.getX(), e.getY());
	            }
	        });
		
		// Export
		final JPopupMenu exportpopup = new JPopupMenu();
		exportpopup.add(new JMenuItem(new AbstractAction("Sequence (.fasta)") {
			private static final long serialVersionUID = -4657464918724936018L;
			public void actionPerformed(ActionEvent e) {
				saveTextToFile(getUnitransString(), "Seq.fasta");
			}			
		}));
		
		if (metaData.hasAssembly()) {
			exportpopup.add(new JMenuItem(new AbstractAction("Aligned reads (.fasta)") {
				private static final long serialVersionUID = -8613746472039641395L;
				public void actionPerformed(ActionEvent e) {
					saveTextToFile(getAlignedReads(), "Reads.fasta");
				}
			}));
		}
		if (nHits>0) {
			exportpopup.add(new JMenuItem(new AbstractAction("DB hits (.fasta)") {
				private static final long serialVersionUID = 424842778593736605L;
				public void actionPerformed(ActionEvent arg0) {
					saveTextToFile(getAllDBHitsString(), "DBhits.fasta");
				}
			}));
		}
		JButton jbExport = Static.createButton("Export...", true);
		jbExport.addMouseListener(new MouseAdapter() {
	            public void mousePressed(MouseEvent e) {
	                exportpopup.show(e.getComponent(), e.getX(), e.getY());
	            }
	        });
		
		JButton btnHelp = Static.createButton("Help2", true, Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getInstance(),"Sequence Details", 
						"html/viewSingleTCW/ContigDetailPanel.html");
			}
		});
			
		// Top panel with buttons and drop-down		
		toolPanel = Static.createRowPanel();
		toolPanel.add( Box.createHorizontalStrut(5) );
		toolPanel.add(new JLabel(" Display:")); 	toolPanel.add( Box.createHorizontalStrut(3) );
		toolPanel.add( menuLib); 				toolPanel.add( Box.createHorizontalStrut(3) );
		if (nHits>0) toolPanel.add( menuHit );
		
		toolPanel.add( Box.createHorizontalStrut(40) );
		toolPanel.add( jbCopy );					toolPanel.add( Box.createHorizontalStrut(5) );
		toolPanel.add( jbExport );				toolPanel.add( Box.createHorizontalStrut(20) );
		
		toolPanel.add( Box.createHorizontalGlue() );
		toolPanel.add( btnHelp );				toolPanel.add( Box.createHorizontalStrut(5) );
		
		toolPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)toolPanel.getPreferredSize ().getHeight() ) );	
		return toolPanel;
	}
	private SeqDetailPanel getInstance() {return this;}
	/***********************************************
	 * Make the textArea
	 */
	private String createTextArea() {
		boolean isProteinDB = metaData.isAAsTCW();
		boolean isOneSeq = metaData.hasNoAssembly();
		int     nContigSets = metaData.nContigSets();
		boolean hasGO = metaData.hasGOs();
		int NUM_COLS = 7; // number of columns of lib/de data
	
		String textArea;
		String id = ctgData.getContigID() + "   ";
		String orig="";
		if (isCAP) orig = " Select Contig";
		else if (!metaData.bUseOrigName()) {
			orig = metaData.getLongLabel() + ": " + ctgData.getLongest() + "   ";
		}
		textArea = " Seq ID: " + id + orig +  ctgData.getUserNotes() + "\n\n";
		// showing CAPed results. The info in ctgData is not complete
		if (isCAP) return textArea;
		
		try {
			boolean hasCtgSets = false;
			if (seqNames != null && seqNames.length>0) hasCtgSets = true; 
	
			int maxrows = 5, maxcol=NUM_COLS+1;
			if (libNames!=null) maxrows = Math.max(libNames.length, maxrows);
			if (deNames!=null)  maxrows = Math.max(deNames.length, maxrows);
			rows = new String [maxrows][maxcol]; 
			for (int i=0; i<maxrows; i++) for (int j=0; j<maxcol; j++) rows[i][j]=null;
			
			/////////// table 1 ////////////////////
			int c=0, r=1;
			rows[0][c] =     "Length:  " + ctgData.getConsensusBases();
			
			if (isProteinDB) {
				c++;
				// nothing to display for this
			}
			else if (isOneSeq) {	
				c++;
				rows[0][c++] = "Best ORF: " + ctgData.getLongestORFCoords();
				rows[0][c++] = ctgData.getTCWNotes(); 
				if (metaData.hasPairWise())
					rows[0][c++] = "PairWise: " + ctgData.getCntPairwise();
			}
			else {
				r = 3;
				rows[1][c] =     "Aligned: " + ctgData.getAllReads();
				rows[2][c] =     "Buried:  " + ctgData.getNumBuried();
				c++;
				
				// column 2
				if (ctgData.hasSanger()) {
					rows[0][c] = "Mate Pairs: " + ctgData.getESTMatePairs();
					rows[1][c] = "5' ESTs:    " + ctgData.getEST5Prime();
					rows[2][c] = "3' ESTs:    " + ctgData.getEST3Prime();
					c++;
				}
				rows[0][c] =     "SNPs:       " + ctgData.getSNPCount();
				rows[1][c] =     "Best ORF:   " + ctgData.getLongestORFCoords();
				rows[2][c] =     "TCW Remark: " + ctgData.getTCWNotes();
				
				c++;
			}
			textArea += textTable(c, r);
		
			//// count set counts as per library counts
			if (hasCtgSets && nContigSets>0 && metaData.hasAssembly()) {
				c=r=0;
				rows[r][c++]= "Sequences:";
				for(int x=0; x<seqNames.length; x++) {
					if (seqNames[x] == null) continue;
				
					rows[r][c] = seqNames[x] + ": " + seqCounts[x] + " "; 
					c++;
					if (c == NUM_COLS) { 
						if ((r+1)==maxrows) {rows[r][c]="..."; break;}
						r++; c=0;
					}
				}
				if (r!=0) c=NUM_COLS;
				textArea += textTable(c, r+1);
			}		
					
			//// Table 2 - library counts either non-zero only or all; five per row
			if (libNames!=null) {
				c=r=0;
				if (normLibs) rows[r][c] = norm + ":  ";
				else rows[r][c] =          "Counts:";
				c++;
				for(int x=0 ; x<libNames.length; x++) {
					if (libNames[x] == null) continue;
					if ((repLibs && c>1) || c==NUM_COLS) { 
						if ((r+1)==maxrows) {rows[r][c]="..."; break;}
						r++; c=0; 
						rows[r][c++] = "";
					}
					if (!normLibs) {
						if (repLibs && libRepCntStr[x]!=null) 
							 rows[r][c] = libNames[x] + ": " + libCounts[x] + " - " + libRepCntStr[x]; 
						else rows[r][c] = libNames[x] + ": " + libCounts[x]; 
					}
					else {
						rows[r][c] = libNames[x] + ": " + Out.formatDouble(libRPKM[x]); 
					}
					c++;
				}
				if (r!=0) c=NUM_COLS;
				textArea += textTable(c, r+1);
			}
			
			if (deNames!=null) {
				c=r=0;
				rows[r][c++] = "P-vals:";
				for (int x=0; x<deNames.length; x++) {
					if (Math.abs(libDE[x]) >= 2.0) continue;
					
					if (c == NUM_COLS) { 
						if ((r+1)==maxrows) {rows[r][c]="..."; break;}
						r++; c=0;
						rows[r][c++] = "";
					}
					
					rows[r][c++] = deNames[x] + ": " + Out.formatDouble(libDE[x]); 
				}
				if (r!=0) c=NUM_COLS;
				textArea += textTable(c, r+1);
			}
			
			if (ctgData.getCntTotal()==0) {
				textArea += "DB hits: 0\n";
				return textArea;
			}
			//////////// table 4 ///////////////////
			c=0;
			rows[0][c] = "SwissProt: " + ctgData.getCntSwiss();
			rows[1][c] = "annoDBs:   " + nAnnoDBs; 	
			c++;
			
			rows[0][c] = "TrEMBL:  " + ctgData.getCntTrembl();
			rows[1][c] = "Regions: " + ctgData.getCntOverlap();
			c++;
			
			rows[0][c] = "Other:   " + (ctgData.getCntNT()+ctgData.getCntGI());
			rows[1][c] = "Species: " + ctgData.getCntSpecies();
			c++;
			
			rows[0][c] = "Total Hits:  " + ctgData.getCntTotal();
			rows[1][c] = "Unique desc: " + ctgData.getCntGene();
			c++;
			
			if (hasGO) {
				rows[0][c] = "Unique GOs : >=" + ctgData.getCntGO();
				rows[1][c] = " ";
				c++;
			}
			textArea += textTable(c, 2);	

			//// Header label for main table of DB hits
			String label=dbhits[0];
			r = 0;
			if      (filterType == 1)  {r = ctgData.getCntOverlap(); label = dbhits[2];}
			else if (filterType == 2)  {r = ctgData.getCntDBs(); label = dbhits[1];}
			else if (filterType == 4)  {r = ctgData.getCntSpecies();label = dbhits[3];}
			else if (filterType == 8)  {r = ctgData.getCntGene();label = dbhits[4];}
			else if (filterType == 0)  {r = ctgData.getCntTotal();label = dbhits[5];}
		
			if (r>0) textArea += " DB hits: " + label + ": " + r + "\n";
			else textArea += " DB hits: " + label + "\n";

			return textArea;
		} 
		catch(Exception e) {ErrorReport.reportError(e); return textArea + "\nError\n";}
	}

	 /*  Make table in textArea */
	private String textTable(int nCol, int nRow)
	{
		String lines = "";
		int c, r;
		
		int []collen = new int [nCol];
		for (c=0; c<nCol; c++) collen[c]=0;
				
        // determine column width
		// not all columns may be filled for last row
        for (c=0; c< nCol; c++) {
            for (r=0; r<nRow && rows[r][c] != null; r++) 
            		if (rows[r][c].length() > collen[c]) 
            			collen[c] = rows[r][c].length();
 
        }
        for (c=0; c< nCol; c++)  collen[c] += 4;
        
        // Output rows
        for (r=0; r<nRow; r++) {
        		String row = " ";
            for (c=0; c<nCol && rows[r][c] != null; c++) {
                 row += pad(rows[r][c],collen[c],1);
                 rows[r][c] = null; // so wouldn't reuse in next table
            }
            lines += row + "\n";
        }
        lines += "\n";
		return lines;
	}
    private static String pad(String s, int width, int o)
    {
            if (s.length() > width) {
                String t = s.substring(0, width-1);
                System.err.println("'" + s + "' truncated to '" + t + "'");
                s = t;
                s += " ";
            }
            else if (o == 0) { // left
                String t="";
                width -= s.length();
                while (width-- > 0) t += " ";
                s = t + s;
            }
            else {
                width -= s.length();
                while (width-- > 0) s += " ";
            }
            return s;
    }

	/********************************************************
	 * XXX Hit table code
	 * filter: 1=olap, 2=top3, 4=species, 8=gene, 16=besteval, 32=bestanno
	 * filterType: 
	 *  "List DB hits - best eval & anno", 16 ) );
		"List DB hits - best per annoDB", 2 ) );
		"List DB hits - minimal coverage", 1 ) );
		"List DB hits - unique species", 4 ) );
		"List DB hits - unique gene rep", 8 ) );
		"List DB hits - total", 0));
	 */
	private int makeHitTable() {	
		if (nHits==0) return 0;
		int x = 0;	
		if (theHitData!=null) theHitData.clear();
		theHitData = new ArrayList<HitListData> ();
	
		if (hitData==null) Out.die("hit null " + nHits);
		if (hitData.length==0) Out.die("hit 0 " + nHits);
		for (int i=0; i<nHits; i++) {
			HitListData hd = hitData[i];
			int fbit = hd.filter;
			
			// general case: if filterType & fbit != 0, then fbit is correct filter.
			if (filterType!=0 && filterType!=2 && filterType!=16 && (filterType&fbit) == 0) continue;
			if (filterType==2 && hd.nRank != 1) continue; // not set in fbit
			if (filterType==16 && hd.best==0) continue;
			
			theHitData.add(hd);
			x++;
		}	
		return x;
	}

	/**
	 * XXX HitListData class
	 * order of headings must correspond to order of SORT_BY values
	 */
	private String [] columnHeadings() {
		String [] x = { "Hit ID",  "annoDB", "Description", "Species",  
				"#GO", "Best","RF", "E-value", "%Sim",  "Align", "%Seq", "%Hit",   "Start", "End", "Bit"};
		return x;
	}
	private class HitListData {
		public static final int SORT_BY_NAME = 0; 
		public static final int SORT_BY_TYPE = 1; 
		public static final int SORT_BY_DESCRIP = 2; 
		public static final int SORT_BY_SPECIES = 3;
		public static final int SORT_BY_HASGO = 4;
		public static final int SORT_BY_BEST = 5;
		public static final int SORT_BY_FRAME = 6; 
		public static final int SORT_BY_EVAL = 7; 
		public static final int SORT_BY_PERCENT = 8; 
		public static final int SORT_BY_ALIGN = 9; 
		public static final int SORT_BY_pSEQ = 10; 
		public static final int SORT_BY_pHIT = 11;
		public static final int SORT_BY_SeqSTART = 12;
		public static final int SORT_BY_SeqEND = 13; 
		public static final int SORT_BY_BIT = 14;
			
		public HitListData( int id, String name, double eVal, String type, String description, String species, String go,
				int per, int alen, int start, int end,  String seq, int rank, int r, int fil,
				String inter, String kg, String pf, String ec, int gobest, 
				int pHitCov, int pSeqCov, double bits) {
			hitID = id;
			nRank = rank;
			hitName = name;
			eval = eVal;
			strType = type;
			strDesc = description;
			strSpecies = species;
			nPercent = per;
			nAlignLen = alen;
			
			nStart = start; 
			nEnd = end;
			if (!isAAtcw) {
				if (start < end) {
					nFrame = (start%3);
					if (nFrame==0) nFrame=3;
				}
				else {
					int x = (ctgData.getConsensusBases()-start+1);
					nFrame = (x%3);
					if (nFrame==0) nFrame=3;
					nFrame = -nFrame;
				}
			}
			else nFrame = 0;
			
			pHitAlign = pHitCov; 
			pSeqAlign = pSeqCov; 
			
			filter=fil;
			
			strSeq = seq;
			best = r;  
			if (best>0) {
				if ((filter&16)==16 && (filter&32)==32) strBest = "EV,AN";
				else if ((filter&16)==16) strBest = "EV";
				else if ((filter&32)==32) strBest = "AN";
				if (gobest==1) {
					if (strBest=="") strBest = "WG";
					else strBest += ",WG";
				}
				if (strBest.equals("EV,AN,WG")) strBest="All";
				
				int f = nFrame+3;
				if (frameHitInfo[f]==null) {
					String e = "0.0";
					if (eval!=0) e =  String.format("%.0E", eval); 
					frameHitInfo[nFrame+3] =  String.format("%s %s,%d%s", strBest, e, nPercent, "%");
					frameHitStart[nFrame+3] = nStart;
					frameHitEnd[nFrame+3] =   nEnd;
				}
			}
			nGO = noGO;
	   		if (go!=null && !go.equals("")) {
				String [] tok = go.split(";"); 
				int n = Integer.parseInt(tok[0].substring(1));
				nGO = n+"";
	   		}
	   		strKEGG=kg;
	   		strEC=ec;
	   		strPfam=pf; 
	   		strInterpro=inter;
	   		dBit = bits;
		}
		
		public Object getValueAt(int pos) {
			switch(pos) {
			case 0: return hitName;
			case 1: return strType;
			case 2: return strDesc;
			case 3: return strSpecies;
			case 4: return nGO;
			case 5: return strBest;	
			case 6: 
				if (nFrame==0) return "-";
				return nFrame;
			case 7:
				if(eval == 0) return new String("0.0");
				return (new DecimalFormat("0.0E0")).format((Double)eval); 
			case 8: return nPercent;
			case 9: return nAlignLen;
			case 10: return pSeqAlign;
			case 11: return pHitAlign;
			case 12: return nStart;
			case 13: return nEnd;
			case 14: return dBit;
			}
			return null;
		}
		
		public int compareTo(HitListData obj, boolean sortAsc, int field) {
			int order = 1;
			if(!sortAsc) order = -1;
			
			switch(field) { // CAS303 replaced new Double and new Integer with (Double) and (Integer)
			case SORT_BY_NAME: return order * hitName.compareTo(obj.hitName);
			case SORT_BY_EVAL: return order * ((Double) (eval)).compareTo((Double) (obj.eval));
			case SORT_BY_pSEQ: return order * ((Integer) (pSeqAlign)).compareTo((Integer)(obj.pSeqAlign));
			case SORT_BY_pHIT: return order * ((Integer)(pHitAlign)).compareTo((Integer)(obj.pHitAlign));
			case SORT_BY_FRAME: return order * ((Integer)(nFrame)).compareTo((Integer)(obj.nFrame));
			case SORT_BY_PERCENT: return order * ((Integer)(nPercent)).compareTo((Integer)(obj.nPercent));
			case SORT_BY_BEST: return order * strBest.compareTo(obj.strBest);
			case SORT_BY_TYPE: return order * strType.compareTo(obj.strType);
			case SORT_BY_DESCRIP: return order * strDesc.compareTo(obj.strDesc);
			case SORT_BY_SPECIES: return order * strSpecies.compareTo(obj.strSpecies);
			case SORT_BY_HASGO: return order * nGO.compareTo(obj.nGO);
			case SORT_BY_ALIGN: return order * ((Integer)(nAlignLen)).compareTo((Integer)(obj.nAlignLen));
			case SORT_BY_SeqSTART: return order * ((Integer)(nStart)).compareTo((Integer)(obj.nStart));
			case SORT_BY_SeqEND: return order * ((Integer)(nEnd)).compareTo((Integer)(obj.nEnd));
			case SORT_BY_BIT: return order * ((Double)(dBit)).compareTo((Double)(obj.dBit));
			}
			return 0;
		}
		private int hitID=0;
		private String hitName = "";
		private double eval = 0;
		private int nStart = -1;
		private int nEnd = -1;
		private int pSeqAlign = -1;
		private int pHitAlign = -1;
		private int nPercent = -1;
		private int nAlignLen = -1;
		private String strType = "";
		private String strDesc = "";
		private String strSpecies = "";
		private int nFrame = -1;
		private int filter=0;	// Used for distinct regions, etc
		private int nRank = -1; // Used to view best per annodb
		private String nGO=noGO;
		private String strSeq=null;
		private int best = 0;
		private String strBest="";
		private String strKEGG="", strEC="", strPfam="", strInterpro="";
		private double dBit=0;
	} // end HitData class
	
	/*
	 * Table support routines
	 */
	private void createTablePanel() {
		theHitTable = new JTable();
		theHitTable.setColumnSelectionAllowed( false );
		theHitTable.setCellSelectionEnabled( false );
		theHitTable.setRowSelectionAllowed( true );
		theHitTable.setShowHorizontalLines( false );
		theHitTable.setShowVerticalLines( false );	
		theHitTable.setIntercellSpacing ( new Dimension ( 1, 0 ) );
		theModel = new HitTableModel(theHitData);
		theHitTable.setModel(theModel);
		
		JTableHeader header = theHitTable.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(theModel.new ColumnListener(theHitTable));
		header.setReorderingAllowed(true);
		resizeColumns();
		
		resultScroll = new JScrollPane(theHitTable);
		resultScroll.setBorder( null );
		Dimension size = theHitTable.getPreferredSize();
		size.height = NUM_DISPLAY_ROWS * theHitTable.getRowHeight(); 
		size.width = Math.min(MAX_TABLE_WIDTH, theHitTable.getWidth());
		resultScroll.setPreferredSize(size);
		resultScroll.getViewport().setBackground(Color.WHITE);
		resultScroll.setAlignmentX(LEFT_ALIGNMENT);			
	}
	
	private void resizeColumns() {
		theHitTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int margin = 2;
		
		for(int x=0; x < theHitTable.getColumnCount(); x++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) 
				theHitTable.getColumnModel();
			TableColumn col = colModel.getColumn(x);
			int maxSize = 0;
			
			TableCellRenderer renderer = col.getHeaderRenderer();
			
			if(renderer == null)
				renderer = theHitTable.getTableHeader().getDefaultRenderer();
			
			Component comp = renderer.getTableCellRendererComponent(theHitTable, 
					col.getHeaderValue(), false, false, 0, 0);
			maxSize = comp.getPreferredSize().width;
			
			for(int y=0; y<theHitTable.getRowCount(); y++) {
				renderer = theHitTable.getCellRenderer(y, x);
				comp = renderer.getTableCellRendererComponent(theHitTable, 
						theHitTable.getValueAt(y, x), false, false, y, x);
				maxSize = Math.max(maxSize, comp.getPreferredSize().width);
			}
			maxSize += 2 * margin;
			
			if (x==4) col.setPreferredWidth(Math.min(maxSize, MAX_COL-40));
			else col.setPreferredWidth(Math.min(maxSize, MAX_COL));
			
		}
		((DefaultTableCellRenderer) 
				theHitTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	}
	
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}

	private class HitTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;
		
		private final String [] theLabels = columnHeadings();
		
		public HitTableModel(ArrayList<HitListData> displayValues) { theDisplayValues = displayValues;}
		
		public int getColumnCount() { return theLabels.length; }
        
		public int getRowCount() { return theDisplayValues.size(); }
        
		public Object getValueAt(int row, int col) { return theDisplayValues.get(row).getValueAt(col); }
        
		public String getColumnName(int col) { return theLabels[col]; }
        
        private ArrayList<HitListData> theDisplayValues = null;
		private boolean sortAsc = true;
		private int sortMode = HitListData.SORT_BY_EVAL;
        
  	  	public class ColumnListener extends MouseAdapter {
		    protected JTable table;

		    public ColumnListener(JTable t) {table = t;}

		    public void mouseClicked(MouseEvent e) {
			    	TableColumnModel colModel = table.getColumnModel();
			    	int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			    	int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();
	
			    	if (modelIndex < 0)return;
			    	if (sortMode == modelIndex) sortAsc = !sortAsc;
			    	else sortMode = modelIndex;
	
			    	for (int i = 0; i < table.getColumnCount(); i++) { 
			    		TableColumn column = colModel.getColumn(i);
			    		column.setHeaderValue(getColumnName(column.getModelIndex()));
			    	}
			    	table.getTableHeader().repaint();
	
			    	Collections.sort(theDisplayValues, new HitListComparator(sortAsc, sortMode));
			    	table.tableChanged(new TableModelEvent(HitTableModel.this));
			    	table.repaint();
		    }
  	  	}
	}

	private class HitListComparator implements Comparator<HitListData> {
		public HitListComparator(boolean sortAsc, int mode) {
			bSortAsc = sortAsc;
			nMode = mode;
		}		
		public int compare(HitListData obj1, HitListData obj2) { 
			return obj1.compareTo(obj2, bSortAsc, nMode); }
		
		private boolean bSortAsc;
		private int nMode;
	}
	
	
	/************  End Hit table code *************************/
	private void redraw() {
		scroller.remove(ctgViewPanel);
		createPanel();
		scroller.setViewportView( ctgViewPanel );
		revalidate();
		repaint();
	}
	
	private void saveTextToClipboard(String lines) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents( new StringSelection(lines), (ClipboardOwner) this );
	}
	
	/*****************************************************
	 * Export functions
	 */
	private String getUnitransString() {
		String retVal = ">" + ctgData.getContigID() + "  len=" + 
			ctgData.getSeqData().getLength() + "\n";
		retVal += ctgData.getSeqData().getSequence() + "\n";

		return retVal;
	}
	private String getRevCompString() {
		String retVal = ">" + ctgData.getContigID() + "  len=" + 
			ctgData.getSeqData().getLength() + " (rc)\n";
		retVal += ctgData.getSeqData().getSeqRevComp() + "\n";

		return retVal;
	}

	private String getORFtransString() {
		CodingRegion orf = ctgData.getORFCoding();
		if (orf!=null) {
			String aa = ctgData.getSeqData().getORFtrans(orf);
			String retVal = ">" + ctgData.getContigID() + " AAlen=" + 
				aa.length() + " rf=" + orf.getFrame() + "\n" +
				aa + "\n"; 
			return retVal;
		}
		return null;
	}
	
	private String getAlignedReads() {
		Vector<SequenceData> sequences = ctgData.getAllSequences();
		Iterator<SequenceData> seqIter = sequences.iterator();
		
		String retVal = "";
		while(seqIter.hasNext()) {
			SequenceData seq = seqIter.next();
			retVal += ">" + seq.getName() + "  len=" + seq.getLength() + "\n";
			retVal += seq.getSequence() + "\n";
		}
		return retVal;
	}
	
	private String getAllDBHitsString() {
		String retVal = "";
		
		for (HitListData hd : hitData) {
			retVal += ">" + hd.hitName + "  len=" + hd.strSeq.length() + "\n";
			retVal += hd.strSeq + "\n";
		}
		return retVal;
	}
	
	private void saveTextToFile(String lines, String dName) {
		final JFileChooser fc = new JFileChooser(theMainFrame.lastSaveFilePath);
		fc.setSelectedFile(new File(dName));
		final String theLines = lines;
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			final File f = fc.getSelectedFile();
			int writeOption = JOptionPane.YES_OPTION;
			if (f.exists()) {
				writeOption = JOptionPane.showConfirmDialog(
					    this,
					    "The file already exists, overwrite it?",
					    "Save to File",
					    JOptionPane.YES_NO_OPTION);
			}
			if (writeOption == JOptionPane.YES_OPTION) {
				theMainFrame.setLastPath(f.getPath());
				Thread theThread = new Thread(new Runnable() {
					public void run() {
					    	PrintWriter pw = null;
					    	try {
					    		System.out.println("Write to " + f.getPath());
					    		pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
					    		pw.print(theLines);
					    		pw.close();
					    	}
					    	catch(Exception e) {ErrorReport.prtReport(e,  "Could not write to " + f.getPath());}
					}
				});
				theThread.start();
			}
		}
	}
	// SeqTopRowTab
	public boolean hasHits () {return nHits>0;}
	public int cntSelectedHits() {
		if(theHitTable == null) return 0;
		int [] selections = theHitTable.getSelectedRows();
		return selections.length;
	}
	public String [] getBestHits() {
		Vector<String> retVal = new Vector<String> ();
		for (HitListData hd : theHitData) {
			if (!hd.strBest.equals("")) retVal.add(hd.hitName);
		}
		return retVal.toArray(new String[0]);
	}
	public String [] getSelectedHits() {
		int [] selections = theHitTable.getSelectedRows();

		Vector<String> retVal = new Vector<String> ();
		for(int x=0; x<selections.length; x++) {
			String hitID = theHitData.get(selections[x]).hitName;
	
			if(!retVal.contains(hitID)) retVal.add(hitID);
		}	
		return retVal.toArray(new String[0]);
	}
	public String getHitSeq(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).strSeq;
		else return null;
	}
	public String getHitType(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).strType.substring(0,2);
		else return null;
	}
	public int getHitSim(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).nPercent;
		else return 0;
	}
	public int getHitStart(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).nStart;
		else return 0;
	}
	public int getHitEnd(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).nEnd;
		else return 0;
	}
	// Msg is shown in the header line of Align Hits (should be same as LoadFromDB.loadBlastHitData)
	public String getHitMsg(String id) {
		if (!hitMap.containsKey(id)) return "error";
		HitListData hd = hitMap.get(id);
		String e = "0.0";
		if (hd.eval!=0) e =  String.format("%.0E", hd.eval); 
		
		String h = " Hit: " +  e        + ", "  + 
							hd.nPercent + "%, Align " + hd.nAlignLen  + "    " +
							hd.strType  + " " + hd.strDesc;
		return h;
	}
	
	// SeqGOPanel
	public String getSeqName() { return ctgData.getContigID();}
	public int getSeqID() { return ctgData.getCTGID();}
	public int getHitCount() {return hitMap.size();}
	public Double getHitEval(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).eval;
		else return -1.0;
	}
	public int getHitBest(String hitID) {
		int e = (!hitMap.get(hitID).strBest.equals("")) ? 1 : 0;
		return e;
	}
	public boolean hasHitGO(String id) {
		if (hitMap.containsKey(id)) {
			String ngo = hitMap.get(id).nGO;
			if (ngo.equals(noGO)) return false;
			return true;
		}
		else return false;
	}
	public String getHitGO(String id) {
		if (hitMap.containsKey(id)) {
			return hitMap.get(id).nGO;
		}
		else return null;
	}
	public int getHitAlignLen(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).nAlignLen;
		else return 0;
	}
	public int getHitPercent(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).nPercent;
		else return 0;
	}
	public int getHitFiltered(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).filter;
		else return 0;
	}
	public String getHitDesc(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).strDesc;
		else return null;
	}
	public String getHitKEGG(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).strKEGG;
		else return null;
	}
	public String getHitPfam(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).strPfam;
		else return null;
	}
	public String getHitEC(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).strEC;
		else return null;
	}
	public String getHitInterpro(String id) {
		if (hitMap.containsKey(id)) return hitMap.get(id).strInterpro;
		else return null;
	}
	public Vector <String> getHitList() {
		Vector <String> hits = new Vector <String> ();
		for (int i=0; i<hitData.length; i++) 
			hits.add(hitData[i].hitName);
		return hits;
	}
	public HashSet <String> getHitNameGO() { 
		if (hitData==null || hitData.length==0) return null;
		HashSet <String> hits = new HashSet <String> ();
		for (int i=0; i<hitData.length; i++)  {
			String ngo = hitData[i].nGO;
			if (!ngo.equals(noGO))
				hits.add(hitData[i].hitName);
		}
		return hits;
	}
	public HashMap <Integer, String> getHitIdName() { 
		if (hitData==null || hitData.length==0) return null;
		HashMap <Integer, String> hits = new HashMap <Integer, String> ();
		for (int i=0; i<hitData.length; i++) 
			hits.put(hitData[i].hitID, hitData[i].hitName);
		return hits;
	}
	
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
	}
	/**********************************************************
	 * XXX database calls
	 */
	private String loadHitSequence(String hitID) {
		String seq=">" + hitID + "\n"; 
		try {
			DBConn dbc = theMainFrame.getNewDBC();
			
			ResultSet rs = dbc.executeQuery("SELECT sequence from pja_db_unique_hits " +
					"where hitID='" + hitID + "'");
			if (rs.next()) seq+=rs.getString(1);
			rs.close();
			dbc.close(); 
		}
		catch ( Exception err ) {ErrorReport.reportError(err, "Error: reading database for hit sequence");}
		return seq;
	}
	// ctgData already loaded into memory. Get hit data here
	private void loadHitData() {
	try {
		hitMap = new HashMap <String, HitListData> ();
	
		if (ctgData.getCntTotal()==0) return;
		DBConn dbc = theMainFrame.getNewDBC();
		
		String strQ = 	"SELECT  " +
          		"q.DUHID, q.description, q.sequence, q.species, q.dbtype, q.taxonomy, " +
          		"q.goBrief, q.interpro, q.kegg, q.pfam, q.ec, " +  // goBrief, not goList because just want #n from front
          		
          		"t.uniprot_id, t.percent_id, t.alignment_len," +
          		"t.ctg_start, t.ctg_end, " +
          		"t.e_value, t.blast_rank, t.filtered, t.best_rank, t.filter_gobest,  " + // CAS303 rank->best_rank
          		"t.prot_cov, t.ctg_cov, t.bit_score " +
          		
          		"FROM pja_db_unique_hits as q " +
          		"JOIN pja_db_unitrans_hits as t " +
          		"WHERE t.DUHID = q.DUHID " + 
          		"AND   t.CTGID = " + ctgData.getCTGID() + 
          		" order by t.e_value ASC, t.best_rank DESC";
       
		  HashSet <String> annodbs = new HashSet <String> (); 
		  Vector <String> order = new Vector <String> (); // to keep ordered by e-value
          ResultSet rs = dbc.executeQuery( strQ );
    	  while( rs.next() )
    	  {	
    		  int i=1;
    		  int hitID = rs.getInt(i++);
    		  String desc = rs.getString(i++);
    		  String seq = rs.getString(i++);
    		  String species = rs.getString(i++);
    		  String type = rs.getString(i++);
    		  String tax = rs.getString(i++);
    		  String goBrief = rs.getString(i++);	
    		  String interpro = rs.getString(i++);
    		  String kegg = rs.getString(i++);
    		  String pfam = rs.getString(i++);
    		  String ec = rs.getString(i++);
    		  
    		  String hitName = rs.getString(i++);
    		  int percent = rs.getInt(i++); 
    		  int len = rs.getInt(i++);
    		  int start = rs.getInt(i++);
    		  int end = rs.getInt(i++);
    		  double eval = rs.getDouble(i++);
    		  int blast = rs.getInt(i++);
    		  int filter = rs.getInt(i++);
    		  int rank = rs.getInt(i++);
    		  int nGo = rs.getInt(i++);
    		  
    		  String typeTax="";
    		  String capType = type.toUpperCase();
    		  if (tax.length() < 4) typeTax= capType + tax;
    		  else typeTax = capType + tax.substring(0, 3);
    		  int pHitCov = rs.getInt(i++);
    		  int pSeqCov = rs.getInt(i++);
    		  double dbit = rs.getDouble(i++);
    		  
    		  if (!annodbs.contains(typeTax)) annodbs.add(typeTax);
    		  
    		  HitListData hd = new HitListData( hitID, hitName, eval, typeTax, desc, species, goBrief,
    				  percent, len, start, end,  seq, blast, rank, filter,
    				  interpro, kegg, pfam, ec, nGo, pHitCov, pSeqCov, dbit);
    		  hitMap.put(hitName, hd);
    		  order.add(hitName);
		}
		nHits= hitMap.size();
	
		if (nHits == 0) return;
		
		hitData = new HitListData [hitMap.size()];
		nAnnoDBs = annodbs.size();
		int x=0;
		for (String h : order) hitData[x++] = hitMap.get(h);
		order.clear(); order=null;
	} catch (Exception e) {ErrorReport.prtReport(e, "Getting hit data for " + ctgData.getContigID());}
	}
	// load from database. Not in ctgData
	private void loadDBdata()
	{	
	    ResultSet rs = null;
	    
		try {	
			int ctgID = ctgData.getCTGID();
			DBConn dbc = theMainFrame.getNewDBC();
			
 			libNames = metaData.getLibNamesbyLID();
 			seqNames = metaData.getSeqNames();
 			deNames = metaData.getDENames();
 			
 			if (libNames!=null) {
 				libCounts = new int[libNames.length];
 				libRPKM = new double[libNames.length];  
 			}
			if (seqNames!=null) 
				seqCounts = new int[seqNames.length]; 
			if (deNames!=null) 
				libDE = new double[deNames.length];
    			
			String fields=metaData.getDeLibColList();
			rs = dbc.executeQuery("SELECT " + fields + " FROM contig WHERE CTGID=" + ctgID);
			while(rs.next()) {
				if (libNames!=null) {
    				for(int x=0; x<libNames.length; x++) {
    					if (libNames[x]!=null) {
    						libCounts[x] = rs.getInt(L + libNames[x]);
    						libRPKM[x] =   rs.getDouble(LN + libNames[x]); 
    					}
    				}
				}
				if (seqNames!=null) {
    				for(int x=0; x<seqNames.length; x++) {
    					if (seqNames[x] !=null)
    						seqCounts[x] = rs.getInt(L + seqNames[x]);
    				}
				}
				if (deNames!=null) {
					for(int x=0; x<deNames.length; x++) 
						libDE[x] = rs.getDouble(P + deNames[x]);
				}
			}
			// get replicas
			libRepCntStr = new String[libNames.length];
     		for (int i=0; i<libNames.length; i++) libRepCntStr[i]=null;
      			
            rs = dbc.executeQuery("SELECT LID, count, rep FROM clone_exp where CID=" + ctgID);
            while (rs.next()) {
        		int rep = rs.getInt(3);
        		if (rep !=0) {
        			int id = rs.getInt(1);
        			int cnt = rs.getInt(2);	
        			if (libRepCntStr[id]==null) libRepCntStr[id] = " " + cnt;
        			else libRepCntStr[id] += ", " + cnt;
        		}
            }       
    		rs.close();    		
    		dbc.close();
		}
		catch ( Exception err ) {ErrorReport.reportError(err, "Error: reading database for getLibs");}
	}
	
	public boolean hasHit() { return hasHit; }
	public String [] getHitInfo() { return frameHitInfo;}
	public int [] getHitStart() { return frameHitStart;}
	public int [] getHitEnd() { return frameHitEnd;}
	
	private boolean hasHit=false;
	private String [] frameHitInfo = new String [7]; // for SeqFramePanel
	private int [] frameHitStart = new int [7];
	private int [] frameHitEnd = new int [7];
	
	private ContigData ctgData;
	private HitListData [] 	hitData;					 // all data
	private HashMap <String, HitListData> hitMap;	 // easy retrival 
	private ArrayList<HitListData> theHitData = null; // what is displayed
	
	private String [] 	libNames;
	private String [] 	seqNames;
	private String [] 	deNames;
	private int [] 		libCounts;
	private double [] 	libRPKM;
	private double [] 	libDE;
	private int [] 		seqCounts;
	private String []	libRepCntStr;
	private int nHits=0, nAnnoDBs=0;
	
	private boolean normLibs = false;
	private boolean repLibs = false;
	private int filterType = 16;
	private boolean isAAtcw = false;
	
	private JScrollPane scroller = null;
	private JPanel ctgViewPanel = null;
	private JPanel toolPanel = null;
	private JTextArea textAreaTop = null;
	private JTable theHitTable = null;
	private JScrollPane resultScroll = null;
	private JComboBox <MenuMapper> menuLib = null;
	private JComboBox <MenuMapper> menuHit = null;
	
	private STCWFrame theMainFrame = null; 
	private MetaData metaData = null;
	
	private String [][] rows = null;
	private HitTableModel theModel = null;
	private String norm="RPKM"; // CAS304
}
