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
import java.util.HashSet;
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
		btnViewSeqs = Static.createButtonTab(Globals.seqTableLabel, true);
		btnViewSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewSelectedSeqs();
			}
		});
		btnViewSeqs.setEnabled(false);
		
		btnAlignSeqs = Static.createButtonPanel("Align", true);
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
					else Out.PrtErr("Failed to get sequence for hit");
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying hit sequence");
				} catch (Error er) {ErrorReport.reportFatalError(er, "Fatal error copying hit sequence", null);}
			}
		}));
		btnCopy = Static.createButton("Copy...", false);
		btnCopy.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                copypopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
				
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
		btnTable = Static.createButton("Table...", false);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablepopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		
		topRowPanel = Static.createRowPanel();
		topRowPanel.add(Static.createLabel(Globals.select));	topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnViewSeqs);				topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnAlignSeqs);				topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnCopy);					topRowPanel.add(Box.createHorizontalStrut(2));
		topRowPanel.add(btnShow);
		if (metaData.hasGOs()) {
			topRowPanel.add(Box.createHorizontalStrut(2));
			topRowPanel.add(btnExport);
		}
		
		topRowPanel.add(Box.createHorizontalStrut(25));
		topRowPanel.add(btnTable);
		
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
	public String getStatusCounts() { 
		return String.format("Seqs: %,d   Hits: %,d  Pairs: %,d    ",
				 seqCntMap.size(), hitGrpList.size(),  hitSeqList.size());
	}
	public void setStatus(String txt) {
		theTablePanel.setStatus(txt);
	}
	public void appendStatus(String txt) {
		theTablePanel.setStatus(theFilterStr + "       " + txt);
	}
	/*******************************************************
	 * Private methods
	 */
	private void viewSelectedSeqs() {
		if (theTablePanel.getSelectedRowCount()==0) return;
		
		String [] seqIDs = getSelectedSeqIDs();
		if (seqIDs==null) return;
	
		String tag = "HitSeqs"; // CAS335 was doing extra work to get tag
		
		if (isGrpView) {
			int [] rows = theTablePanel.getSelectedRows();
			if (rows.length==1) tag = hitGrpList.get(rows[0]).hd.strHitID;
			else tag = rows.length + " Hits";
		}
		
		theParentFrame.loadContigs(tag, seqIDs, STCWFrame.BASIC_QUERY_MODE_HIT );
	}
	private void viewAlignSeqs() {
		int [] rows = theTablePanel.getSelectedRows();
		// this is one-to-one pairing for alignment
		// BasicHitFilterPanel will create the unique map of ContigData and SequenceData objects
		Vector <String> seqList = new Vector <String> ();
		Vector <String> hitList = new Vector <String> ();
		
		if (isGrpView) {
			for (int r : rows) {
				String hit = hitGrpList.get(r).hd.strHitID;
			
				for(int x=0; x<hitSeqList.size(); x++) {
					if (hitSeqList.get(x).hd.strHitID.equals(hit)) {
						seqList.add(hitSeqList.get(x).strSeqID);
						hitList.add(hit);
					}
				}
			}
		}
		else {
			for (int r : rows) {
				seqList.add(hitSeqList.get(r).strSeqID);
				hitList.add(hitSeqList.get(r).hd.strHitID);
			}
		}
		 theFilterPanel.displayAlign(seqList, hitList);
	}
	private String [] getSelectedSeqIDs() {
		int [] rows = theTablePanel.getSelectedRows();
	
		HashSet <String> seqIDs = new HashSet <String> (); // CAS335 was vector
	
		if (!isGrpView) {
			for (int r : rows) {
				String seqid = hitSeqList.get(r).strSeqID;
				if (!seqIDs.contains(seqid)) seqIDs.add(seqid); // CAS335 add 'contains' check
			}
			return seqIDs.toArray(new String[0]);
		}
	
	/* GrpView  */
		
		if (rows.length==hitGrpList.size()) { // all rows selected - CAS335 - much faster using seqHitList
			for (HitSeqData sObj : hitSeqList) {
				if (!seqIDs.contains(sObj.strSeqID))
					seqIDs.add(sObj.strSeqID);
			}
			return seqIDs.toArray(new String[0]);
		}
		if (rows.length>5000 && hitSeqList.size()>50000) {// CAS335 add popup
			if (!UserPrompt.showContinue("Many Seq-Hit pairs", 
					rows.length + " rows to be removed, which can take long.\n\n"
					+ "The Seq-Hit table is much faster (Uncheck 'Group by Hit ID' and select).\n"))
			return null;	
		}
		// CAS335 go through seqs, once it finds a hit, then don't look for rest
		for (int r : rows) {
			String hitid = hitGrpList.get(r).hd.strHitID;
		
			for(int x=0; x<hitSeqList.size(); x++) {
				String seqid = hitSeqList.get(x).strSeqID;
				
				if(!seqIDs.contains(seqid))
					if(hitSeqList.get(x).hd.strHitID.equals(hitid)) 
						seqIDs.add(seqid);
			}
			if (r>999 && (r%1000)==0) 
				Out.r("Processed hits #" + r + ": seq #" + seqIDs.size());
		}
		Out.r("                                                                ");
		return seqIDs.toArray(new String[0]);	
	}
	
	private String getSelectedHitID() {
		if (theTablePanel.getSelectedRowCount()==0) return "";
		
		int row = theTablePanel.getSelectedRow();
		
		if (isGrpView) return hitGrpList.get(row).hd.strHitID;
		else return hitSeqList.get(row).hd.strHitID;
	}
	private String getSelectedDesc() {
		if (theTablePanel.getSelectedRowCount()==0) return "";
		
		int row = theTablePanel.getSelectedRow();
		
		if (isGrpView) return hitGrpList.get(row).hd.strDesc;
		else return hitSeqList.get(row).hd.strDesc;
	}
	private String getSelectedHitSeq() {
		if (theTablePanel.getSelectedRowCount()==0) return "";
		
		int row = theTablePanel.getSelectedRow();
		String hitID;
		if (isGrpView) hitID = hitGrpList.get(row).hd.strHitID;
		else hitID = hitSeqList.get(row).hd.strHitID;
		
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
				id = hitGrpList.get(row).hd.strHitID;
				for (int i=0; i<colNames.length; i++) {
					Object o = hitGrpList.get(row).getValueAt(i);
					String val = "" + o;
					String x = String.format("%-15s: %s", colNames[i], val);
					lines.add(x);
				}
				lines.add("");
				lines.add("* Heuristic search values corresponding to best e-value");
			}
			else {
				id = hitSeqList.get(row).strSeqID;
				for (int i=0; i<colNames.length; i++) {
					Object o = hitSeqList.get(row).getValueAt(i);
					String val = "" + o;
					String x = String.format("%-15s: %s", colNames[i], val);
					lines.add(x);
				}
				lines.add("");
				lines.add("+ Heuristic search values");
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
	/************************************************
	 * HitData -- CAS335 added this structure to reduce copies
	 */
	private class HitData {
		private String strHitID;
		private int hitLen;
		private String strDesc;
		private String strSpecies;
		private String strType;
		private String strTaxo;
		private int nGO = 0;
		private String strGO = null;
		private String strIP = null;
		private String strKEGG = null;
		private String strPFAM = null;
		private String strEC = null;
	}
	/*************************************************************
	 * HitSeqData
	 */
	private class HitSeqData {
		public HitSeqData(int r,  Object [] values) {
			nRowNum=r;
			int nCol=0;
			strSeqID = 		(String) values[nCol++];
			hd.strHitID = 	(String) values[nCol++];
			bitscore = 		Double.parseDouble((String) values[nCol++]); // CAS331
			evalue = 		Double.parseDouble((String) values[nCol++]);
			sim = 			Double.parseDouble((String) values[nCol++]);
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
			
			hd.strDesc = 		(String) values[nCol++];
			hd.strSpecies = 	(String) values[nCol++];
			hd.strType = 		(String) values[nCol++];
			hd.strTaxo = 		(String) values[nCol++];
			
			hd.hitLen = 	Integer.parseInt((String) values[nCol++]);
			seqLen = 		Integer.parseInt((String) values[nCol++]);
			seqAlign = 		Static.percent(Math.abs(sstart-send)+1,seqLen);
			hitAlign = 		Static.percent(Math.abs(hstart-hend)+1,hd.hitLen);
			
			if (hasGO) {
				String strGoBrief = (String) values[nCol++];
				if (strGoBrief!=null && !strGoBrief.equals("")) {
					String [] tok = strGoBrief.split(";"); // goBrief may have #N;GO...;GO...
					hd.nGO = Integer.parseInt(tok[0].substring(1));
		   		}
				else hd.nGO=0;
				hd.strGO = (String) values[nCol++];
				hd.strIP = (String) values[nCol++];
				hd.strKEGG = (String) values[nCol++];
				hd.strPFAM = (String) values[nCol++];
				hd.strEC   = (String) values[nCol++];
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
				
				case 3: return hd.strHitID;
				case 4: return strBest;
				case 5: return nRank;
				
				case 6: return new DisplayFloat(bitscore);
				case 7: return new DisplayFloat(evalue); 
				case 8: return new DisplayFloat(sim);
				case 9: return seqAlign;
				case 10: return hitAlign;
				case 11: return align;
				
				case 12: return hd.strDesc;
				case 13: return hd.strSpecies;
				case 14: return hd.strType;
				case 15: return hd.strTaxo;	
				}
			}
			if (hasGO) nCol+=6; // CAS322 was missing this
			if (hasGO && column<nCol) {
				switch(column) {
				case 16: return hd.nGO;
				case 17: return hd.strGO;
				case 18: return hd.strIP;
				case 19: return hd.strKEGG;
				case 20: return hd.strPFAM;
				case 21: return hd.strEC;
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
				
				case 3: return order * compareStrings(hd.strHitID,obj.hd.strHitID);
				case 4: return order * compareStrings(strBest, obj.strBest);
				case 5: return order * ((Integer) nRank).compareTo((Integer) obj.nRank);
				
				case 6: return order * ((Double) bitscore).compareTo((Double) obj.bitscore);
				case 7: return order * ((Double) evalue).compareTo((Double) obj.evalue);
				case 8: return order * ((Double) sim).compareTo((Double) obj.sim);
				case 9: return order * ((Integer) seqAlign).compareTo((Integer) obj.seqAlign);
				case 10: return order * ((Integer) hitAlign).compareTo((Integer) obj.hitAlign);
				case 11: return order * ((Integer) align).compareTo((Integer) obj.align);
				
				case 12: return order * compareStrings(hd.strDesc, obj.hd.strDesc);
				case 13: return order * compareStrings(hd.strSpecies, obj.hd.strSpecies);
				case 14: return order * compareStrings(hd.strType, obj.hd.strType);
				case 15: return order * compareStrings(hd.strTaxo, obj.hd.strTaxo);		
				}
			}
			nCol+=6;
			if (hasGO && column<nCol) {
				switch(column) {
				case 16: return order * ((Integer) hd.nGO).compareTo((Integer) obj.hd.nGO);
				case 17: return order * compareStrings(hd.strGO, obj.hd.strGO);
				case 18: return order * compareStrings(hd.strIP, obj.hd.strIP);
				case 19: return order * compareStrings(hd.strKEGG, obj.hd.strKEGG);
				case 20: return order * compareStrings(hd.strPFAM, obj.hd.strPFAM);
				case 21: return order * compareStrings(hd.strEC, obj.hd.strEC);
				}
			}
			if (column < (nCol+numLibs)) {
				int index = column-nCol;
				return order * (libCounts[index]).compareTo(obj.libCounts[index]);
			}
			nCol+=numLibs;
			int index = column-nCol;
			Double val1 = (Double) Math.abs(pvalCounts[index]);
			Double val2 = (Double) Math.abs(obj.pvalCounts[index]);
			if (val1==Globalx.dNoDE && val2==Globalx.dNoDE) return 0; // CAS331 check for noDe
			else if (val1==Globalx.dNoDE) return 1;
			else if (val2==Globalx.dNoDE) return -1;
			return order * val1.compareTo(val2);
		}
		
		public HitGroupData getGroupItem() {
			return new HitGroupData(this);
		}
		
		private int nRowNum = -1;
		private String strSeqID;
		private int seqLen; 
		
		private double bitscore;	// CAS331 added
		private double evalue;
		private double sim;			// CAS331 change from int
		private int align, seqAlign, hitAlign; 
		           
		private String strBest="";
		private int nRank=0;
		
		private Double [] libCounts = null;
		private Double [] pvalCounts = null;
		
		private HitData hd = new HitData();
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
			
			nSeqs = 1;
			
			hd = seq.hd;
			
			if (!seq.strBest.equals("")) nBest=1;
			if (seq.nRank==1) nRank1=1;
			
			bitscore = 	seq.bitscore;
			evalue =	seq.evalue;
			sim = 		seq.sim;
			seqAlign = 	seq.seqAlign;
			hitAlign = 	seq.hitAlign;
			align = 	seq.align;
			
			nLibCounts = new Double[numLibs];
			for(int x=0; x<nLibCounts.length; x++) nLibCounts[x] = seq.libCounts[x];
			
			nPvalCounts = new Double[numPvals];
			for(int x=0; x<nPvalCounts.length; x++) nPvalCounts[x] = seq.pvalCounts[x];
		}
		// The first seq-hit has been added, update with subsequent ones
		public void incrementSeqCount(Double [] lcounts, Double [] pcounts, String strBest, Double seqBit,
				Double seqEVal, double seqSim, int seqAlign, int hitAlign, int align, int nRank) { 
			nSeqs++; 
			for(int x=0; x<lcounts.length; x++) 
				if (nLibCounts[x] < lcounts[x]) nLibCounts[x] = lcounts[x];
			
			for(int x=0; x<pcounts.length; x++) 
				if (Math.abs(nPvalCounts[x]) > Math.abs(pcounts[x])) nPvalCounts[x] = pcounts[x];
			
			if (!strBest.equals("")) nBest++;
			if (nRank==1) nRank1++;
			
			if (seqBit>bitscore || (seqBit==bitscore && seqEVal < evalue) || (seqEVal==evalue && seqSim>sim)) {
				bitscore = seqBit;
				evalue=seqEVal;
				sim = seqSim;
				this.seqAlign = seqAlign;
				this.hitAlign = hitAlign;
				this.align    = align;		// CAS335 was getting seqAlign value
			}
		}
		
		public void setRowNum(int row) { nRowNum = row; }
		
		public boolean equals(HitSeqData hit) {
			return compareStrings(hd.strHitID, hit.hd.strHitID) == 0;
		}
		
		public int compareTo(HitGroupData obj) {
			return compareStrings(hd.strHitID, obj.hd.strHitID);
		}
		
		public Object getValueAt(int column) {
			int nCol=theFilterPanel.NUM_HIT_COL;
			if (column<nCol) {
				switch(column) {
				case 0: return nRowNum; // is sorted with the row, but not displayed
				case 1: return hd.strHitID;
				case 2: return hd.hitLen;
				
				case 3: return nSeqs;
				case 4: return nBest;
				case 5: return nRank1;
				
				case 6: return new DisplayFloat(bitscore);
				case 7: return new DisplayFloat(evalue); 
				case 8: return new DisplayFloat(sim); 
				case 9: return seqAlign;
				case 10: return hitAlign;
				case 11: return align;
				
				case 12: return hd.strDesc;
				case 13: return hd.strSpecies;
				case 14: return hd.strType;
				case 15: return hd.strTaxo;
				}
			}
			if (hasGO) nCol+=6;
			if (hasGO && column<nCol) {
				switch(column) {
				case 16: return hd.nGO;
				case 17: return hd.strGO;
				case 18: return hd.strIP;
				case 19: return hd.strKEGG;
				case 20: return hd.strPFAM;
				case 21: return hd.strEC;
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
				case 0: return 0; // row# does not change; CAS326 quit sorting;
				case 1: return order * compareStrings(hd.strHitID, obj.hd.strHitID);
				case 2: return order * ((Integer) hd.hitLen).compareTo((Integer) obj.hd.hitLen);
				
				case 3: return order * ((Integer) nSeqs).compareTo((Integer) obj.nSeqs);
				case 4: return order * ((Integer) nBest).compareTo((Integer) obj.nBest);
				case 5: return order * ((Integer) nRank1).compareTo((Integer) obj.nRank1);
				
				case 6: return order * ((Double) bitscore).compareTo((Double) obj.bitscore);
				case 7: return order * ((Double) evalue).compareTo((Double) obj.evalue);
				case 8: return order * ((Double) sim).compareTo((Double) obj.sim);
				case 9: return order * ((Integer) seqAlign).compareTo((Integer) obj.seqAlign);
				case 10: return order * ((Integer) hitAlign).compareTo((Integer) obj.hitAlign);
				case 11: return order * ((Integer) align).compareTo((Integer) obj.align);
				
				case 12: return order * compareStrings(hd.strDesc, obj.hd.strDesc);
				case 13: return order * compareStrings(hd.strSpecies, obj.hd.strSpecies);
				case 14: return order * compareStrings(hd.strType, obj.hd.strType);
				case 15: return order * compareStrings(hd.strTaxo, obj.hd.strTaxo);		
				}
			}
			if (hasGO) nCol += 6;
			if (hasGO && column<nCol) {
				switch(column) {
				case 16: return order * ((Integer) hd.nGO).compareTo((Integer) obj.hd.nGO);
				case 17: return order * compareStrings(hd.strGO, obj.hd.strGO);
				case 18: return order * compareStrings(hd.strIP, obj.hd.strIP);
				case 19: return order * compareStrings(hd.strKEGG, obj.hd.strKEGG);
				case 20: return order * compareStrings(hd.strPFAM, obj.hd.strPFAM);
				case 21: return order * compareStrings(hd.strEC, obj.hd.strEC);
				}
			}
			if (column< nCol+numLibs) {
				int index = column-nCol;
				return order * ( nLibCounts[index]).compareTo(obj.nLibCounts[index]);
			}
			nCol+=numLibs;
			int index = column-nCol;
			Double val1 = (Double) Math.abs(nPvalCounts[index]);
			Double val2 = (Double) Math.abs(obj.nPvalCounts[index]);
			if (val1==Globalx.dNoDE && val2==Globalx.dNoDE) return 0; // CAS331 check for noDe
			else if (val1==Globalx.dNoDE) return 1;
			else if (val2==Globalx.dNoDE) return -1;
			return order * val1.compareTo(val2);
		}
		
		public int nSeqs;
		private int nRowNum = -1;
		private HitData hd = null;
		
		private int nBest=0;
		private int nRank1=0;
		private double bitscore=0.0;
		private double evalue=2.0;
		private double sim=0.0;
		private int align, seqAlign, hitAlign;
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
	 /* The database call was performed in BasicHitFilterPanel, and passes in the results for display
	    There may be multiple HitSeqData for a seqID since there is one for each seq-hit pair */
	 public void tableBuild(ArrayList<Object []> results, String summary) {
		 try {
			 hitSeqList.clear();
			 seqCntMap.clear(); 
			 String x = " (of " + results.size() + ")";
			 int row=1, cnt=0;
			 for (Object [] rObj : results) {
				 String seqid = (String) rObj[0];
				 addSeqCntMap(seqid);
				 hitSeqList.add(new HitSeqData(row, rObj));
				 row++; cnt++;
				 if (cnt==10000) {
	    			setStatus("Added " + hitSeqList.size() + x + " to sequence table...        ");
	    			cnt=0;
	    		 }
			 }
			 theTablePanel.setStatus("Create Hits....");
			 isSorted=false;
			 convertToGroupedHits(true);
			 
			 theTablePanel.tableRefresh();
			 theFilterStr=getStatusCounts() + summary;
			 theTablePanel.setStatus(theFilterStr); 
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	 public void tableAdd(ArrayList<Object []> results, String summary) {
		 try {
			 String x = " (of " + results.size() + ")";
			 int row=hitSeqList.size();
			 int cnt=0, add=0;
			 HashSet <String> seqHitSet = new HashSet <String> (); // CAS338 for fast lookup - major speedup!
			 for (HitSeqData sd : hitSeqList) {
				 seqHitSet.add(sd.strSeqID + ":" + sd.hd.strHitID);
			 }
			 
			 for (Object [] o : results) {
				 String seqid = (String) o[0];
				 String hitid = (String) o[1];
				 String pair = seqid + ":" + hitid;
				 
				 if (!seqHitSet.contains(pair)) {
					hitSeqList.add(new HitSeqData(row, o));	
					row++; cnt++; add++;
					if (cnt==10000) {
						theTablePanel.setStatus("Add " + add + x + " to sequence table...        ");
		    			cnt=0;
		    		}
					addSeqCntMap(seqid);
				 }
			 }
			 theTablePanel.setStatus("Create Hits....");
			 isSorted=false;
			 convertToGroupedHits(true);
			 
			 theTablePanel.tableRefresh();
			 theFilterStr=getStatusCounts() + "(add " + add + ") " + summary;
			 theTablePanel.setStatus(theFilterStr);
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	private void addSeqCntMap(String seqid) {
		if (seqCntMap.containsKey(seqid)) 
			 seqCntMap.put(seqid, seqCntMap.get(seqid) +1);
		else seqCntMap.put(seqid, 1);
	}

	private void convertToGroupedHits(boolean first) {
		try {
			hitGrpList = new ArrayList<HitGroupData> ();
			int rowNum = 0;
			HashMap <String, HitGroupData> hitMap = new HashMap <String, HitGroupData> ();
		
			String x = "(of " + hitSeqList.size() + ") ...";
			int cnt=0;
			for (HitSeqData seq : hitSeqList) {
				if (hitMap.containsKey(seq.hd.strHitID)) {
					HitGroupData grp = hitMap.get(seq.hd.strHitID);
					grp.incrementSeqCount(seq.libCounts, seq.pvalCounts, seq.strBest, seq.bitscore,
							seq.evalue, seq.sim, seq.seqAlign, seq.hitAlign, seq.align, seq.nRank);
				}
				else {
					HitGroupData grp = seq.getGroupItem();
					grp.setRowNum(++rowNum);
					hitGrpList.add(grp);
					hitMap.put(seq.hd.strHitID, grp);
				}
				if (first) {
					cnt++;
					if (cnt%10000==0) {
		    			theTablePanel.setStatus("Added " + hitGrpList.size() + " to grouped table, processed " + cnt + x);
		    		}
				}
			}
			Runtime.getRuntime().gc();
		}
		catch (OutOfMemoryError e) {
			theTablePanel.setStatus("Out of memory -- make your filter more stringent");
			JOptionPane.showMessageDialog(null, "Out of memory ");
		}
	}
	public void convertToOrder() {
		if (!isSorted) return;
		isSorted=false; // do not reorder again until sorted again
		
		if (isGrpView) {
			hitGrpList.clear();
			convertToGroupedHits(false); 
			return;
		}
		
		try {
			HashMap <String, ArrayList <HitSeqData>> seqMap = 
					new HashMap <String, ArrayList <HitSeqData>> ();
			ArrayList <HitSeqData> tmp;
			for (HitSeqData seq : hitSeqList) {
				if (seqMap.containsKey(seq.hd.strHitID)) tmp = seqMap.get(seq.hd.strHitID);
				else tmp = new ArrayList <HitSeqData> ();
				tmp.add(seq);
				seqMap.put(seq.hd.strHitID, tmp);
			}
			
			hitSeqList.clear();
			hitSeqList = new ArrayList<HitSeqData> ();
			for (HitGroupData grp : hitGrpList) {
				if (seqMap.containsKey(grp.hd.strHitID)) {
					tmp = seqMap.get(grp.hd.strHitID);
					for (HitSeqData seq : tmp)
						hitSeqList.add(seq);
				}
			}
			Runtime.getRuntime().gc();
		}
		catch (OutOfMemoryError e) {
			theTablePanel.setStatus("Out of memory -- cannot sort list");
			JOptionPane.showMessageDialog(null, "Out of memory ");
		}
	}
	public void tableSort (boolean sortAsc, int mode) {
		isSorted=true;
		if(isGrpView) 
 			Collections.sort(hitGrpList, new HitGroupDataComparator(sortAsc, mode));
 		else 
 			Collections.sort(hitSeqList, new HitSeqDataComparator(sortAsc, mode));
	}
	public int getNumRow() {
		if (isGrpView) return hitGrpList.size();
	    return hitSeqList.size();
	}
	public Object getValueAt(int row, int index) {
 		if (isGrpView) 
 			 return hitGrpList.get(row).getValueAt(index);
 		else return hitSeqList.get(row).getValueAt(index);
	}
	/******************************************************
	 * Called from BasicTablePanel to Keep Selected or Remove Selected
	 * CAS335 BasicTable was calling row by row
	 */
	public void deleteFromList(int [] rows) {
		if (!isGrpView) {
			for(int x=rows.length-1; x>=0; x--) {
				String seqid = hitSeqList.get(rows[x]).strSeqID;
				
				int cnt =   seqCntMap.get(seqid);
				
				if (cnt==1) seqCntMap.remove(seqid);
				else 		seqCntMap.put(seqid, seqCntMap.get(seqid)-1);
	
				hitSeqList.remove(rows[x]);
			}
			hitGrpList.clear();
			convertToGroupedHits(false);
			
			theFilterStr=getStatusCounts();
			theTablePanel.setStatus(theFilterStr);
			return;
		}
		
	/* isGrpView - rebuilds */
		if (rows.length>5000 && hitSeqList.size()>50000) {// CAS335 add popup
			if (!UserPrompt.showContinue("Many Seq-Hit pairs", 
					rows.length + " rows to be removed, which can take long.\n\n"
					+ "The Seq-Hit table is much faster (Uncheck 'Group by Hit ID' and select).\n"))
			return;	
		}
			
		ArrayList <HitGroupData> rmHitObj = new ArrayList <HitGroupData> ();
		
		if (Globalx.debug) Out.prt("Before: " + getStatusCounts());
		for(int ih=rows.length-1; ih>=0; ih--) { 
			HitGroupData hitObj = hitGrpList.get(rows[ih]); 
			String hitID = hitObj.hd.strHitID;
			
			for (int is=hitSeqList.size()-1; is>=0; is--) { // Delete from end of HitSeqData
				HitSeqData seqObj = hitSeqList.get(is);
				
				if (seqObj.hd.strHitID.equals(hitID)) { // found a hit-seq pair
					String seqid = seqObj.strSeqID;
					
					int cnt =   seqCntMap.get(seqid);
					if (cnt==1) seqCntMap.remove(seqid);
					else 		seqCntMap.put(seqid, seqCntMap.get(seqid)-1);
					
					hitSeqList.remove(is);
				}
			}	
			rmHitObj.add(hitObj);
			if (ih>999 && ih%1000==0) 
				Out.r("Removed " + (rows.length-ih) + " of " + rows.length);
		}
		for (HitGroupData hitObj : rmHitObj) {
			hitGrpList.remove(hitObj);
			hitObj.hd=null;
			hitObj=null;
		}
		Out.r("                                                 ");
		theFilterStr=getStatusCounts();
		theTablePanel.setStatus(theFilterStr);
		if (Globalx.debug) Out.prt("After:  " + getStatusCounts());
	}
	
	public void enableTopButtons(int sel, int nRow) {
		btnViewSeqs.setEnabled(sel>0);
		btnAlignSeqs.setEnabled(sel>0);
		btnShow.setEnabled(sel==1);
		btnExport.setEnabled(sel==1);
		btnCopy.setEnabled(sel==1);
		btnTable.setEnabled(nRow>0);
	}
	public void enableAllButtons(boolean b) { // start of query (b=false), or fail query (b=true)
		btnViewSeqs.setEnabled(b);
		btnAlignSeqs.setEnabled(b);
		btnShow.setEnabled(b);
		btnExport.setEnabled(b);
		btnCopy.setEnabled(b);
		btnTable.setEnabled(b);
		
		theTablePanel.enableLowButtons(b);
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
	
	private ArrayList<HitSeqData> 		hitSeqList = new ArrayList<HitSeqData> ();	  // seq-hit pairs with hit values & descriptions
	private ArrayList<HitGroupData> 	hitGrpList = new ArrayList<HitGroupData> ();  // hits
	private HashMap <String, Integer>   seqCntMap = new HashMap <String, Integer> (); // # hits per seq; shows #seqs and when to remove
	
	//User interface
	private JButton btnViewSeqs = null, btnAlignSeqs = null, btnShow = null;
	private JButton btnTable = null, btnCopy = null, btnExport = null;
	
	private JPanel topRowPanel = null;
	private BasicHitFilterPanel theFilterPanel = null;
	private STCWFrame theParentFrame=null;
	private MetaData metaData = null;
	
	private boolean hasGO=true;
	private boolean isGrpView=true;
	private boolean isSorted=true;
	
	private int numLibs=0, numPvals=0;
	
	private String theFilterStr="";
}
