package sng.viewer.panels.Basic;

/************************************************
 * Basic Hit Tab:
 * 	top row of buttons
 *  contains mapping of data (BasicHitFilterPanel) to table (BasicTablePanel)
 */
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
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

import sng.database.Globals;
import sng.database.MetaData;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.DisplayFloat;
import util.ui.UserPrompt;

public class BasicHitTab extends Tab {
	private static final long serialVersionUID = -5515249017308285183L;	
	private static final Color BGCOLOR = Globals.BGCOLOR;
	
	private String helpHTML =  Globals.helpDir + "BasicQueryHit.html";

	public BasicHitTab(STCWFrame parentFrame) {
		super(parentFrame, null);
		theParentFrame = parentFrame;
		metaData = parentFrame.getMetaData();
		hasGO = metaData.hasGOs();
		if (metaData.hasExpLevels()) numLibs = metaData.getLibNames().length;
		if (metaData.hasDE()) numPvals = metaData.getDENames().length;
		
		setBackground(BGCOLOR);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
		
		createTopRowPanel();
		add(topRowPanel);
		add(Box.createVerticalStrut(6));
		
		theFilterPanel = new BasicHitFilterPanel(theParentFrame, this);
		add(theFilterPanel);
		add(Box.createVerticalStrut(6));
		
		theTablePanel = new BasicTablePanel(theParentFrame, this, 
				theFilterPanel.getColNames(), theFilterPanel.getColSelect(), 
				theFilterPanel.getPvalColNames()); // CAS322 add last arg for highlights
		add(theTablePanel);
	}
	/******************************************
	 * Top button panel
	 */
	private void createTopRowPanel() {
		btnViewSeqs = new JButton("View Seqs");
		btnViewSeqs.setBackground(Globals.FUNCTIONCOLOR);
		btnViewSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewSelectedSeqs();
			}
		});
		btnViewSeqs.setEnabled(false);
		
		btnAlignSeqs = new JButton("Align");
		btnAlignSeqs.setBackground(Globals.FUNCTIONCOLOR);
		btnAlignSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewAlignSeqs();
			}
		});
		btnAlignSeqs.setEnabled(false);
		
// Copy
		final JPopupMenu copypopup = new JPopupMenu();
		copypopup.add(new JMenuItem(new AbstractAction("Hit ID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String hit = getSelectedHitID();
					if (hit!=null && hit!="")
						cb.setContents(new StringSelection(hit), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying Hit ID");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error copying Hit ID", null);}
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Description") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String hit = getSelectedDesc();
					if (hit!=null && hit!="")
						cb.setContents(new StringSelection(hit), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying Desc");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error copying Desc", null);}
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Hit Sequence") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String name = getSelectedHitID();
					String hit =  getSelectedHitSeq();
					if (hit!=null && hit!="") {
						String x = ">" + name + "\n" + hit; // CAS313
						cb.setContents(new StringSelection(x), null);
					}
					else System.err.println("Failed to get sequence for hit");
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying hit sequence");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error copying hit sequence", null);}
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
				
//Show
		final JPopupMenu selPopup = new JPopupMenu();
		if (metaData.hasGOs()) {
			selPopup.add(new JMenuItem(new AbstractAction("Assigned GOs for hit") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					showExportGOtree(GOtree.HIT_ASSIGNED, GOtree.DO_POPUP);
				}
			}));
			selPopup.add(new JMenuItem(new AbstractAction("All GOs for hit") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					showExportGOtree(GOtree.HIT_ALL, GOtree.DO_POPUP);
				}
			}));
		}
		selPopup.add(new JMenuItem(new AbstractAction("All columns of selected") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				showInfoForSelected();
			}
		}));
		btnShow = Static.createButton("Show...", false);
		btnShow.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                selPopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
	
// Export
		final JPopupMenu selExport = new JPopupMenu();
		if (metaData.hasGOs()) {
			selExport.add(new JMenuItem(new AbstractAction("Assigned GOs for hit*") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					showExportGOtree(GOtree.HIT_ASSIGNED, GOtree.DO_EXPORT_ASK);
				}
			}));
			selExport.add(new JMenuItem(new AbstractAction("All GOs for hit*") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					showExportGOtree(GOtree.HIT_ALL, GOtree.DO_EXPORT_ASK);
				}
			}));
		}
		btnExport = Static.createButton("Export...", false);
		btnExport.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                selExport.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		
		// Table
		final JPopupMenu tablepopup = new JPopupMenu();
		tablepopup.add(new JMenuItem(new AbstractAction("Show column stats") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					String sum = theFilterPanel.getFilters();
					theTablePanel.statsPopUp("Hit table; " + sum);
				} catch (Exception er) {ErrorReport.reportError(er, "Error on column stats");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error column stats", null);}
			}
		}));
		tablepopup.addSeparator();
		
		tablepopup.add(new JMenuItem(new AbstractAction("Copy table") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String table = theTablePanel.tableCopyString("\t");
					if (table!=null && table!="")
						cb.setContents(new StringSelection(table), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error copying table", null);}
			}
		}));
		tablepopup.addSeparator();
		
		tablepopup.add(new JMenuItem(new AbstractAction("Export table") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					theTablePanel.tableExportToFile(btnTable, "Hit");
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying table");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error copying table", null);}
			}
		}));
		btnTable = new JButton("Table...");
		btnTable.setBackground(Color.WHITE);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablepopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.setAlignmentX(RIGHT_ALIGNMENT);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					UserPrompt.displayHTMLResourceHelp(getParentFrame(), 
					"Basic DB Hits Query", helpHTML);
			}
		});

		topRowPanel = Static.createRowPanel();
		topRowPanel.add(new JLabel("Selected:"));
		topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnViewSeqs);
		topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnAlignSeqs);
		topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnCopy);
		topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnShow);
		if (metaData.hasGOs()) {
			topRowPanel.add(Box.createHorizontalStrut(2));
			topRowPanel.add(btnExport);
		}
		
		topRowPanel.add(Box.createHorizontalStrut(25));
		topRowPanel.add(btnTable);
		
		topRowPanel.add(Box.createHorizontalStrut(25));
		//topRowPanel.add( Box.createHorizontalGlue() ); remove setMaximumSize and min
		topRowPanel.add(btnHelp);
		// the Glue does not work with these on Mac
		topRowPanel.setMaximumSize(topRowPanel.getPreferredSize());
		topRowPanel.setMinimumSize(topRowPanel.getPreferredSize());
	}
	
	/********************************************************
	 * Methods called by BasicHitFilterPanel
	 */
	public void hideAll() {
		topRowPanel.setVisible(false);
		theTablePanel.setVisible(false);
		theParentFrame.setButtonsVisible(false);
	}
	
	public void showAll() {
		topRowPanel.setVisible(true);
		theTablePanel.setVisible(true);
	}
	
	
	public void setStatus(String txt) {theTablePanel.setStatus(txt);}
	
	/*******************************************************
	 * Private methods
	 */
	private void viewSelectedSeqs() {
		String [] seqIDs = getSelectedSeqIDs();
		String [] hitIDs = getSelectedHitIDs();
		if (seqIDs==null || hitIDs==null) return;
		
		String tag = (hitIDs.length>1) ? (hitIDs.length+" Hits") : hitIDs[0];
		theParentFrame.loadContigs(tag, seqIDs, STCWFrame.BASIC_QUERY_MODE_HIT );
	}
	private void viewAlignSeqs() {
		int [] rows = theTablePanel.getSelectedRows();
		// this is one to one pairing for alignment
		// BasicHitFilterPanel will create the unique map of ContigData and SequenceData objects
		Vector <String> seqList = new Vector <String> ();
		Vector <String> hitList = new Vector <String> ();
		
		if (isGrpView) {
			for (int r : rows) {
				String hit = grpObjList.get(r).strHitID;
			
				for(int x=0; x<seqObjList.size(); x++) {
					if(seqObjList.get(x).strHitID.equals(hit)) {
						seqList.add(seqObjList.get(x).strSeqID);
						hitList.add(hit);
					}
				}
			}
		}
		else {
			for (int r : rows) {
				seqList.add(seqObjList.get(r).strSeqID);
				hitList.add(seqObjList.get(r).strHitID);
			}
		}
		 theFilterPanel.displayAlign(seqList, hitList);
	}
	private String [] getSelectedSeqIDs() {
		int [] rows = theTablePanel.getSelectedRows();
		Vector<String> ids = new Vector<String> ();
		if (isGrpView) {
			for (int r : rows) {
				String hit = grpObjList.get(r).strHitID;
			
				for(int x=0; x<seqObjList.size(); x++) {
					if(seqObjList.get(x).strHitID.equals(hit)) {
						if(!ids.contains(seqObjList.get(x).strSeqID))
							ids.add(seqObjList.get(x).strSeqID);
					}
				}
			}
		}
		else {
			for (int r : rows) {
				ids.add(seqObjList.get(r).strSeqID);
			}
		}
		return ids.toArray(new String[0]);	
	}
	private String [] getSelectedHitIDs() {
		if (theTablePanel.getSelectedRowCount()==0) return null;
		
		int [] rows = theTablePanel.getSelectedRows();
		String [] ids = new String [rows.length];
		for (int r=0; r<rows.length; r++) {
			if (isGrpView) ids[r]  = grpObjList.get(rows[r]).strHitID;
			else  ids[r] = seqObjList.get(rows[r]).strHitID;
		}
		return ids;
	}
	private String getSelectedHitID() {
		if (theTablePanel.getSelectedRowCount()==0) return "";
		
		int row = theTablePanel.getSelectedRow();
		
		if (isGrpView) return grpObjList.get(row).strHitID;
		else return seqObjList.get(row).strHitID;
	}
	private String getSelectedDesc() {
		if (theTablePanel.getSelectedRowCount()==0) return "";
		
		int row = theTablePanel.getSelectedRow();
		
		if (isGrpView) return grpObjList.get(row).strDesc;
		else return seqObjList.get(row).strDesc;
	}
	private String getSelectedHitSeq() {
		if (theTablePanel.getSelectedRowCount()==0) return "";
		
		int row = theTablePanel.getSelectedRow();
		String hitID;
		if (isGrpView) hitID = grpObjList.get(row).strHitID;
		else hitID = seqObjList.get(row).strHitID;
		
		return theFilterPanel.loadFromDatabaseHitSeq(hitID);
	}
	/****************************************************
	 * Show GOs for selected
	 */
	private void showExportGOtree(int actionType, int outType) {
		try {
	    	String hitID = getSelectedHitID();
	    	String desc = getSelectedDesc(); // CAS324 add desc
			if (hitID=="") {
				JOptionPane.showMessageDialog(null, "No selected row ");
				return;
			}
			new GOtree(theParentFrame).computeSelected(hitID, desc, actionType, outType, btnShow);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Query failed");
			ErrorReport.prtReport(e, "Creating Sequence GO popup");
		}
	}
	/****************************************************
	 * Show GOs for selected
	 */
	private void showInfoForSelected() {
		try {
			if (theTablePanel.getSelectedRowCount()==0) {
				JOptionPane.showMessageDialog(null, "No selected row ");
				return;
			}
			Vector <String> lines = new Vector <String> ();
			String [] colNames = theFilterPanel.getColNames();
			String id="";
			int row = theTablePanel.getSelectedRow();
			if (isGrpView) {
				id = grpObjList.get(row).strHitID;
				for (int i=0; i<colNames.length; i++) {
					Object o = grpObjList.get(row).getValueAt(i);
					String val = "" + o;
					String x = String.format("%-15s: %s", colNames[i], val);
					lines.add(x);
				}
				lines.add("");
				lines.add("* Heuristic search values corresponding to best e-value");
			}
			else {
				id = seqObjList.get(row).strSeqID;
				for (int i=0; i<colNames.length; i++) {
					Object o = seqObjList.get(row).getValueAt(i);
					String val = "" + o;
					String x = String.format("%-15s: %s", colNames[i], val);
					lines.add(x);
				}
				lines.add("");
				lines.add("+ Blast values");
			}
			String [] alines = new String [lines.size()];
			lines.toArray(alines);
			UserPrompt.displayInfoMonoSpace(theParentFrame, "Info for " + id, alines);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Query failed");
			ErrorReport.prtReport(e, "Creating Sequence GO popup");
		}
	}
	/*************************************************************
	 * HitSeqData
	 */
	private class HitSeqData {
		public HitSeqData(int r,  Object [] values) {
			nRowNum=r;
			int nCol=0;
			strSeqID = 		(String) values[nCol++];
			strHitID = 		(String) values[nCol++];
			evalue = 		Double.parseDouble((String) values[nCol++]);
			sim = 			Integer.parseInt((String) values[nCol++]);
			int sstart = 	Integer.parseInt((String) values[nCol++]);
			int send   = 	Integer.parseInt((String) values[nCol++]);
			int hstart = 	Integer.parseInt((String) values[nCol++]);
			int hend = 		Integer.parseInt((String) values[nCol++]);
			align = 		Integer.parseInt((String) values[nCol++]);
			
			String f = 		(String) values[nCol++];
			int filtered = 	Integer.parseInt(f);
			if ((filtered & 16) != 0 && (filtered & 32) != 0) strBest="Both";
			else if ((filtered & 16) != 0) strBest="Bits";
			else if ((filtered & 32) != 0) strBest="Anno";
			else strBest="";
			
			nRank = 		Integer.parseInt((String) values[nCol++]);
			
			strDesc = 		(String) values[nCol++];
			strSpecies = 	(String) values[nCol++];
			strType = 		(String) values[nCol++];
			strTaxo = 		(String) values[nCol++];
			
			hitLen = 		Integer.parseInt((String) values[nCol++]);
			seqLen = 		Integer.parseInt((String) values[nCol++]);
			seqAlign = Static.percent(Math.abs(sstart-send)+1,seqLen);
			hitAlign = Static.percent(Math.abs(hstart-hend)+1,hitLen);
			
			if (hasGO) {
				String strGoBrief = (String) values[nCol++];
				if (strGoBrief!=null && !strGoBrief.equals("")) {
					String [] tok = strGoBrief.split(";"); // goBrief may have #N;GO...;GO...
					nGO = Integer.parseInt(tok[0].substring(1));
		   		}
				else nGO=0;
				strGO = (String) values[nCol++];
				strIP = (String) values[nCol++];
				strKEGG = (String) values[nCol++];
				strPFAM = (String) values[nCol++];
				strEC   = (String) values[nCol++];
			}
			
			libCounts = new Double[numLibs];
			for(int x=0; x<numLibs; x++) {
				libCounts[x] = (Double) values[nCol++];
			}
			pvalCounts = new Double[numPvals];
			for(int x=0; x<numPvals; x++) {
				pvalCounts[x] = (Double) values[nCol++];
			}
		}
		
		public Object getValueAt(int column) {
			int nCol=theFilterPanel.NUM_SEQ_COL;
			if (column<nCol) {
				switch(column) {
				case 0: return nRowNum;
				case 1: return strSeqID;
				case 2: return seqLen;
				
				case 3: return strHitID;
				case 4: return strBest;
				case 5: return nRank;
				
				case 6: return new DisplayFloat(evalue); 
				case 7: return sim;
				case 8: return seqAlign;
				case 9: return hitAlign;
				case 10: return align;
				
				case 11: return strDesc;
				case 12: return strSpecies;
				case 13: return strType;
				case 14: return strTaxo;	
				}
			}
			if (hasGO) nCol+=6; // CAS322 was missing this
			if (hasGO && column<nCol) {
				switch(column) {
				case 15: return nGO;
				case 16: return strGO;
				case 17: return strIP;
				case 18: return strKEGG;
				case 19: return strPFAM;
				case 20: return strEC;
				}
			}
			
			if (column < (nCol+numLibs))
				return new DisplayFloat(libCounts[column-nCol]);
			
			nCol+=numLibs;
			
			double pval = pvalCounts[column-nCol];
			if (Math.abs(pval)==Globalx.dNoDE) return Globalx.sNoVal; // CAS330
			return new DisplayFloat(pval);
		}

		public int compareTo(HitSeqData obj, boolean ascOrder, int column) {
			int order = 1;
			if(!ascOrder) order = -1;
			
			int nCol=theFilterPanel.NUM_SEQ_COL;
			if (column<nCol) {
				switch(column) {
				case 0: return 0; // CAS326 order * ((Integer) nRowNum).compareTo((Integer) obj.nRowNum);
				case 1: return order * compareStrings(strSeqID, obj.strSeqID);
				case 2: return order * ((Integer) seqLen).compareTo((Integer) obj.seqLen);
				
				case 3: return order * compareStrings(strHitID,obj.strHitID);
				case 4: return order * compareStrings(strBest, obj.strBest);
				case 5: return order * ((Integer) nRank).compareTo((Integer) obj.nRank);
				
				case 6: return order * ((Double) evalue).compareTo((Double) obj.evalue);
				case 7: return order * ((Integer) sim).compareTo((Integer) obj.sim);
				case 8: return order * ((Integer) seqAlign).compareTo((Integer) obj.seqAlign);
				case 9: return order * ((Integer) hitAlign).compareTo((Integer) obj.hitAlign);
				case 10: return order * ((Integer) align).compareTo((Integer) obj.align);
				
				case 11: return order * compareStrings(strDesc, obj.strDesc);
				case 12: return order * compareStrings(strSpecies, obj.strSpecies);
				case 13: return order * compareStrings(strType, obj.strType);
				case 14: return order * compareStrings(strTaxo, obj.strTaxo);		
				}
			}
			nCol+=6;
			if (hasGO && column<nCol) {
				switch(column) {
				case 15: return order * ((Integer) nGO).compareTo((Integer) obj.nGO);
				case 16: return order * compareStrings(strGO, obj.strGO);
				case 17: return order * compareStrings(strIP, obj.strIP);
				case 18: return order * compareStrings(strKEGG, obj.strKEGG);
				case 19: return order * compareStrings(strPFAM, obj.strPFAM);
				case 20: return order * compareStrings(strEC, obj.strEC);
				}
			}
			if (column < (nCol+numLibs)) {
				int index = column-nCol;
				return order * (libCounts[index]).compareTo(obj.libCounts[index]);
			}
			nCol+=numLibs;
			int index = column-nCol;
			return order * ((Double) Math.abs(pvalCounts[index])).compareTo((Double) Math.abs(obj.pvalCounts[index]));
		}
		
		public HitGroupData getGroupItem() {
			return new HitGroupData(this);
		}
		
		private int nRowNum = -1;
		private String strSeqID;
		private String strHitID;
		private double evalue;
		private int sim;
		private int align, seqAlign, hitAlign; // hitStart transferred to HitGroupData
		private int seqLen, hitLen;            // hitLen transferred to HitGroupData
		private String strDesc;
		private String strSpecies;
		private String strType;
		private String strTaxo;
		private String strBest="";
		private int nRank=0;
		private int nGO = 0;
		private String strGO = null;
		private String strIP = null;
		private String strKEGG = null;
		private String strPFAM = null;
		private String strEC = null;
		private Double [] libCounts = null;
		private Double [] pvalCounts = null;
	} // end hitSeqData
	private class HitSeqDataComparator implements Comparator<HitSeqData> {
		public HitSeqDataComparator(boolean sortAsc, int mode) {
			bSortAsc = sortAsc;
			nMode = mode;
		}		
		public int compare(HitSeqData obj1, HitSeqData obj2) { 
			return obj1.compareTo(obj2, bSortAsc, nMode); 
		}
		private boolean bSortAsc;
		private int nMode;
	}
	/****************************************************
	 * HitGroupData
	 */
	public class HitGroupData implements Comparable<HitGroupData> {
		public HitGroupData(HitSeqData seq) {
			strHitID = seq.strHitID;
			hitLen = seq.hitLen;
			nSeqs = 1;
			strDesc = 		seq.strDesc;
			strSpecies = 	seq.strSpecies;
			strType = 		seq.strType;
			strTaxo = 		seq.strTaxo;
			nGO =			seq.nGO;
			strGO = 			seq.strGO;
			strIP = 			seq.strIP;
			strKEGG = 		seq.strKEGG;
			strPFAM = 		seq.strPFAM;
			strEC = 			seq.strEC;
			if (!seq.strBest.equals("")) nBest=1;
			if (seq.nRank==1) nRank1=1;
			
			evalue=seq.evalue;
			sim = seq.sim;
			seqAlign = seq.seqAlign;
			hitAlign = seq.hitAlign;
			align = seq.align;
			
			nLibCounts = new Double[numLibs];
			for(int x=0; x<nLibCounts.length; x++) nLibCounts[x] = seq.libCounts[x];
			
			nPvalCounts = new Double[numPvals];
			for(int x=0; x<nPvalCounts.length; x++) nPvalCounts[x] = seq.pvalCounts[x];
		}
		public void incrementSeqCount(Double [] lcounts, Double [] pcounts, String strBest, 
				Double seqEVal, int seqSim, int sstart, int hstart, int seqAlign, int nRank) { 
			nSeqs++; 
			for(int x=0; x<lcounts.length; x++) 
				if (nLibCounts[x] < lcounts[x]) nLibCounts[x] = lcounts[x];
			
			for(int x=0; x<pcounts.length; x++) 
				if (Math.abs(nPvalCounts[x]) > Math.abs(pcounts[x])) nPvalCounts[x] = pcounts[x];
			
			if (!strBest.equals("")) nBest++;
			if (nRank==1) nRank1++;
			
			if (seqEVal < evalue || (seqEVal==evalue && seqSim>sim)) {
				evalue=seqEVal;
				sim = seqSim;
				seqAlign = sstart;
				hitAlign = hstart;
				align = seqAlign;
			}
		}
		
		public void setRowNum(int row) { nRowNum = row; }
		
		public boolean equals(HitSeqData hit) {
			return compareStrings(strHitID, hit.strHitID) == 0;
		}
		
		public int compareTo(HitGroupData obj) {
			return compareStrings(strHitID, obj.strHitID);
		}
		
		public Object getValueAt(int column) {
			int nCol=theFilterPanel.NUM_HIT_COL;
			if (column<nCol) {
				switch(column) {
				case 0: return nRowNum;
				case 1: return strHitID;
				case 2: return hitLen;
				
				case 3: return nSeqs;
				case 4: return nBest;
				case 5: return nRank1;
				
				case 6: return new DisplayFloat(evalue); 
				case 7: return sim;
				case 8: return seqAlign;
				case 9: return hitAlign;
				case 10: return align;
				
				case 11: return strDesc;
				case 12: return strSpecies;
				case 13: return strType;
				case 14: return strTaxo;
				}
			}
			if (hasGO) nCol+=6;
			if (hasGO && column<nCol) {
				switch(column) {
				case 15: return nGO;
				case 16: return strGO;
				case 17: return strIP;
				case 18: return strKEGG;
				case 19: return strPFAM;
				case 20: return strEC;
				}
			}		
			if (column < nCol+numLibs) {
				return new DisplayFloat(nLibCounts[column-nCol]);
			}
			nCol+= numLibs;
			
			double pval = nPvalCounts[column-nCol];
			if (Math.abs(pval)==Globalx.dNoDE) return Globalx.sNoVal; // CAS330
			return new DisplayFloat(pval);
		}
		
		public int compareTo(HitGroupData obj, boolean ascOrder, int column) {
			int order = 1;
			if(!ascOrder) order = -1;
			int nCol=theFilterPanel.NUM_HIT_COL;
			if (column<nCol) {
				switch(column) {
				case 0: return 0; // CAS326 order * ((Integer) nRowNum).compareTo((Integer) obj.nRowNum);
				case 1: return order * compareStrings(strHitID, obj.strHitID);
				case 2: return order * ((Integer) hitLen).compareTo((Integer) obj.hitLen);
				
				case 3: return order * ((Integer) nSeqs).compareTo((Integer) obj.nSeqs);
				case 4: return order * ((Integer) nBest).compareTo((Integer) obj.nBest);
				case 5: return order * ((Integer) nRank1).compareTo((Integer) obj.nRank1);
				
				
				case 6: return order * ((Double) evalue).compareTo((Double) obj.evalue);
				case 7: return order * ((Integer) sim).compareTo((Integer) obj.sim);
				case 8: return order * ((Integer) seqAlign).compareTo((Integer) obj.seqAlign);
				case 9: return order * ((Integer) hitAlign).compareTo((Integer) obj.hitAlign);
				case 10: return order * ((Integer) align).compareTo((Integer) obj.align);
				
				case 11: return order * compareStrings(strDesc, obj.strDesc);
				case 12: return order * compareStrings(strSpecies, obj.strSpecies);
				case 13: return order * compareStrings(strType, obj.strType);
				case 14: return order * compareStrings(strTaxo, obj.strTaxo);		
				}
			}
			if (hasGO) nCol += 6;
			if (hasGO && column<nCol) {
				switch(column) {
				case 15: return order * ((Integer) nGO).compareTo((Integer) obj.nGO);
				case 16: return order * compareStrings(strGO, obj.strGO);
				case 17: return order * compareStrings(strIP, obj.strIP);
				case 18: return order * compareStrings(strKEGG, obj.strKEGG);
				case 19: return order * compareStrings(strPFAM, obj.strPFAM);
				case 20: return order * compareStrings(strEC, obj.strEC);
				}
			}
			if (column< nCol+numLibs) {
				int index = column-nCol;
				return order * ( nLibCounts[index]).compareTo(obj.nLibCounts[index]);
			}
			nCol+=numLibs;
			int index = column-nCol;
			return order * ((Double) Math.abs(nPvalCounts[index])).compareTo((Double) Math.abs(obj.nPvalCounts[index]));
		}
		public String strHitID;
		public int nSeqs;
		private int nRowNum = -1;
		private String strDesc;
		private String strSpecies;
		private String strType;
		private String strTaxo;
		private String strIP = null;
		private int nGO=0;
		private String strGO = null;
		private String strKEGG = null;
		private String strPFAM = null;
		private String strEC = null;
		private int nBest=0;
		private int nRank1=0;
		private double evalue=2.0;
		private int sim;
		private int align, seqAlign, hitAlign, hitLen;
		private Double [] nLibCounts = null;
		private Double [] nPvalCounts = null;
	} // end HitGroupData
	private class HitGroupDataComparator implements Comparator<HitGroupData> {
		public HitGroupDataComparator(boolean sortAsc, int mode) {
			bSortAsc = sortAsc;
			nMode = mode;
		}		
		public int compare(HitGroupData obj1, HitGroupData obj2) { 
			return obj1.compareTo(obj2, bSortAsc, nMode); }
		
		private boolean bSortAsc;
		private int nMode;
	}
	
	 private static int compareStrings(String str1, String str2) {
	    	if(str1 == null && str2 == null) return 0;
	    	if(str1 == null) return -1;
	    	if(str2 == null) return 1;
	    	return str1.compareToIgnoreCase(str2); 
	 }
	
	/***********************************************
	 * Table
	 */
	 // The database call was performed in BasicHitFilterPanel, and passes in the results for display
	 // may may multiple HitSeqData for a seqID since there is one for each seq-hit pair
	 public void tableBuild(ArrayList<Object []> results, String summary) {
		 try {
			 enableTopButtons(false);
			
			 seqObjList.clear();
			 seqCntMap.clear(); 
			 String x = " (of " + results.size() + ") ...";
			 int row=1, cnt=0;
			 for (Object [] o : results) {
				 String seqid = (String) o[0];
				 seqListAdd(seqid);
				 seqObjList.add(new HitSeqData(row, o));
				 row++; cnt++;
				 if (cnt==10000) {
	    				setStatus("Added " + seqObjList.size() + x + " to sequence table...        ");
	    				cnt=0;
	    			 }
			 }
			 setStatus("Create Hits....");
			 isSorted=false;
			 convertToGroupedHits(true);
			 setStatus("Done");
			 
			 theTablePanel.tableRefresh();
			 theTablePanel.setStatus(getStatus() + summary);
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	 public void tableAdd(ArrayList<Object []> results, String summary) {
		 try {
			 enableTopButtons(false);
			 String x = " (of " + results.size() + ") ...";
			 int row=seqObjList.size();
			 int cnt=0, add=0;
			 
			 for (Object [] o : results) {
				 boolean isAdd=true;
				 String seqid = (String) o[0];
				 String hitid = (String) o[1];
				 for (HitSeqData sd : seqObjList) {
					 if (sd.strSeqID.equals(seqid) && sd.strHitID.equals(hitid)) {
						 isAdd = false;
						 break;
					 }
				 }
				 if (isAdd) {
					seqObjList.add(new HitSeqData(row, o));	
					row++; cnt++; add++;
					if (cnt==10000) {
		    				setStatus("Add " + add + x + " to sequence table...        ");
		    				cnt=0;
		    			}
					seqListAdd(seqid);
				 }
			 }
			 setStatus("Create Hits....");
			 isSorted=false;
			 convertToGroupedHits(true);
			 setStatus("Done");
			 
			 theTablePanel.tableRefresh();
			 theTablePanel.setStatus(getStatus() + "(added " + add + ") " + summary);
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	private void seqListAdd(String seqid) {
		if (seqCntMap.containsKey(seqid)) 
			 seqCntMap.put(seqid, seqCntMap.get(seqid) +1);
		else seqCntMap.put(seqid, 1);
	}
	private void seqListRemove(String seqid) {
		if (seqCntMap.containsKey(seqid)) {
			int cnt = seqCntMap.get(seqid);
			if (cnt==1) seqCntMap.remove(seqid);
			else 		seqCntMap.put(seqid, seqCntMap.get(seqid)-1);
		}
	}
	public String getStatus() { 
		return String.format("Seqs: %,d   Hits: %,d  Pairs: %,d    ",
				 seqCntMap.size(), grpObjList.size(),  seqObjList.size());
	}
	private void convertToGroupedHits(boolean first) {
		try {
			grpObjList = new ArrayList<HitGroupData> ();
			int rowNum = 0;
			HashMap <String, HitGroupData> hitMap = new HashMap <String, HitGroupData> ();
		
			String x = "(of " + seqObjList.size() + ") ...";
			int cnt=0;
			for (HitSeqData seq : seqObjList) {
				if (hitMap.containsKey(seq.strHitID)) {
					HitGroupData grp = hitMap.get(seq.strHitID);
					grp.incrementSeqCount(seq.libCounts, seq.pvalCounts, seq.strBest, 
							seq.evalue, seq.sim, seq.seqAlign, seq.hitAlign, seq.align, seq.nRank);
				}
				else {
					HitGroupData grp = seq.getGroupItem();
					grp.setRowNum(++rowNum);
					grpObjList.add(grp);
					hitMap.put(seq.strHitID, grp);
				}
				if (first) {
					cnt++;
					if (cnt%10000==0) {
		    				setStatus("Added " + grpObjList.size() + " to grouped table, processed " + 
		    						cnt + x);
		    			}
				}
			}
			Runtime.getRuntime().gc();
		}
		catch (OutOfMemoryError e) {
			setStatus("Out of memory -- make your filter more stringent");
			JOptionPane.showMessageDialog(null, "Out of memory ");
		}
	}
	public void convertToOrder() {
		if (!isSorted) return;
		isSorted=false; // do not reorder again until sorted again
		
		if (isGrpView) {
			grpObjList.clear();
			convertToGroupedHits(false); 
			return;
		}
		
		try {
			HashMap <String, ArrayList <HitSeqData>> seqMap = 
					new HashMap <String, ArrayList <HitSeqData>> ();
			ArrayList <HitSeqData> tmp;
			for (HitSeqData seq : seqObjList) {
				if (seqMap.containsKey(seq.strHitID)) tmp = seqMap.get(seq.strHitID);
				else tmp = new ArrayList <HitSeqData> ();
				tmp.add(seq);
				seqMap.put(seq.strHitID, tmp);
			}
			
			seqObjList.clear();
			seqObjList = new ArrayList<HitSeqData> ();
			for (HitGroupData grp : grpObjList) {
				if (seqMap.containsKey(grp.strHitID)) {
					tmp = seqMap.get(grp.strHitID);
					for (HitSeqData seq : tmp)
						seqObjList.add(seq);
				}
			}
			Runtime.getRuntime().gc();
		}
		catch (OutOfMemoryError e) {
			setStatus("Out of memory -- cannot sort list");
			JOptionPane.showMessageDialog(null, "Out of memory ");
		}
	}
	public void tableSort (boolean sortAsc, int mode) {
		isSorted=true;
		if(isGrpView) {
 			Collections.sort(grpObjList, 
					new HitGroupDataComparator(sortAsc, mode));
		}
 		else {
 			Collections.sort(seqObjList, 
 					new HitSeqDataComparator(sortAsc, mode));
 		}
	}
	public int getNumRow() {
		if (isGrpView) return grpObjList.size();
	    return seqObjList.size();
	}
	public Object getValueAt(int row, int index) {
 		if (isGrpView) 
 			 return grpObjList.get(row).getValueAt(index);
 		else return seqObjList.get(row).getValueAt(index);
	}
	public void removeFromList(int row) {
		if (isGrpView) {
			String hitID = grpObjList.get(row).strHitID;
			
			ArrayList <HitSeqData> tmpSeq = seqObjList;
			seqObjList = new ArrayList<HitSeqData> ();
			seqCntMap = new HashMap <String, Integer> (seqCntMap.size());
			for (HitSeqData seq : tmpSeq) // hitSeq data is one-to-one
				if (!seq.strHitID.equals(hitID)) {
					seqObjList.add(seq);
					seqListAdd(seq.strSeqID);
				}
			
			grpObjList.remove(row);
		}
		else { 
			ArrayList <HitGroupData> tmpGrp = grpObjList;
			grpObjList = new ArrayList<HitGroupData> ();
			for (HitGroupData grp : tmpGrp) 
				if (grp.nSeqs>0) grpObjList.add(grp);
			
			seqListRemove(seqObjList.get(row).strSeqID);
			seqObjList.remove(row);
		}
	}
	public void updateTopButtons(int row) {
		btnViewSeqs.setEnabled(row>0);
		btnAlignSeqs.setEnabled(row>0);
		btnShow.setEnabled(row==1);
		btnExport.setEnabled(row==1);
		btnCopy.setEnabled(row==1);
	}
	public void enableTopButtons(boolean b) {
		btnViewSeqs.setEnabled(b);
		btnAlignSeqs.setEnabled(b);
		btnShow.setEnabled(b);
		btnExport.setEnabled(b);
		btnCopy.setEnabled(b);
	}
	public void tableHitRecalc() { // called after delete objects from table
		if (!isGrpView) {
			grpObjList.clear();
			convertToGroupedHits(false);
		}
	}
	public void tableRefresh(boolean b) {
		isGrpView = b;
		theTablePanel.setColumns(theFilterPanel.getColNames(), theFilterPanel.getColSelect());
		theTablePanel.tableRefresh();
	}
	public void tableClear() {
		ArrayList<Object []> results = new ArrayList <Object[]> ();
		tableBuild(results, "Executing search....");
	}
	
	//Table panel
	private BasicTablePanel theTablePanel = null;
	
	private ArrayList<HitSeqData> seqObjList = new ArrayList<HitSeqData> ();
	private ArrayList<HitGroupData> grpObjList = new ArrayList<HitGroupData> ();
	private HashMap <String, Integer> seqCntMap = new HashMap <String, Integer> ();
	
	//User interface
	private JButton btnViewSeqs = null;
	private JButton btnAlignSeqs = null;
	private JButton btnShow = null;
	private JButton btnTable = null;
	private JButton btnCopy = null;
	private JButton btnExport = null;
	private JButton btnHelp = null;
	
	private JPanel topRowPanel = null;
	private BasicHitFilterPanel theFilterPanel = null;
	private STCWFrame theParentFrame=null;
	private MetaData metaData = null;
	
	private boolean hasGO=true;
	private boolean isGrpView=true;
	private boolean isSorted=true;
	
	private int numLibs=0, numPvals=0;
}
