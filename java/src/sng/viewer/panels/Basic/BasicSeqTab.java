package sng.viewer.panels.Basic;
/************************************************
 * Basic Sequence Tab:
 * 	top row of buttons
 *  contains mapping of data (BasicSeqFilterPanel) to table (BasicTablePanel)
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
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import sng.database.Globals;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;

public class BasicSeqTab extends Tab {
	private static final long serialVersionUID = -5515249017308285183L;	
	private static final Color BGCOLOR = Globals.BGCOLOR;

	public BasicSeqTab(STCWFrame parentFrame) {
		super(parentFrame, null);
		theMainFrame = parentFrame;	
		
		setBackground(BGCOLOR);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		createTopRowPanel();
		add(topRowPanel);
		add(Box.createVerticalStrut(6));
		
		theFilterPanel = new BasicSeqFilterPanel(theMainFrame, this);
		add(theFilterPanel);
		add(Box.createVerticalStrut(6));
		
		theTablePanel = new BasicTablePanel(theMainFrame, this, theFilterPanel.getColNames(), theFilterPanel.getColSelect());
		add(theTablePanel);
	}
	/******************************************
	 * Top button panel
	 */
	private void createTopRowPanel() {
		btnViewSeqs = Static.createButton(Globals.seqTableLabel, false, Globals.FUNCTIONCOLOR);
		btnViewSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewSelectedSeqs();
			}
		});
		btnDetail = Static.createButton(Globals.seqDetailLabel, false, Globals.FUNCTIONCOLOR);
		btnDetail.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewSelectedDetail();
			}
		});
		// Copy
		final JPopupMenu copypopup = new JPopupMenu();
		
		copypopup.add(new JMenuItem(new AbstractAction("Seq ID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					int row = theTablePanel.getSelectedRow();
					if (row>=0) {
						String seq = seqObjList.get(row).strSeqID;
						if (seq!=null && seq!="")
							cb.setContents(new StringSelection(seq), null);
					}
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying Seq ID");}
			}
		}));
		if (!theMainFrame.getMetaData().bUseOrigName()) {// CAS311
			String longLabel = theMainFrame.getMetaData().getLongLabel(); // CAS311
			
			copypopup.add(new JMenuItem(new AbstractAction(longLabel) {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					try {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						int row = theTablePanel.getSelectedRow();
						if (row>=0) {
							String seq = seqObjList.get(row).longestRead;
							if (seq!=null && seq!="")
								cb.setContents(new StringSelection(seq), null);
						}
					} catch (Exception er) {ErrorReport.reportError(er, "Error copying HitID"); }
				}
			}));
		}
		copypopup.add(new JMenuItem(new AbstractAction("Best HitID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					int row = theTablePanel.getSelectedRow();
					if (row>=0) {
						String seq = seqObjList.get(row).strBestHit;
						if (seq!=null && seq!="")
							cb.setContents(new StringSelection(seq), null);
					}
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying HitID"); }
			}
		}));	
		btnCopy = Static.createButton("Copy...", false);
		btnCopy.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                copypopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });		
		
		// Table
		final JPopupMenu tablepopup = new JPopupMenu();
		tablepopup.add(new JMenuItem(new AbstractAction("Copy Table") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					String table = theTablePanel.tableCopyString("\t");
					if (table!=null && table!="")
						cb.setContents(new StringSelection(table), null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copying table"); }
			}
		}));
		tablepopup.addSeparator();
		tablepopup.add(new JMenuItem(new AbstractAction("Export Table") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					theTablePanel.tableExportToFile(btnTable, "Seq");
				} catch (Exception er) {ErrorReport.reportError(er, "Error export table"); }
			}
		}));
		btnTable = Static.createButton("Table...", false);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablepopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });	

		topRowPanel = Static.createRowPanel();
		topRowPanel.add(new JLabel("Selected:"));	topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnViewSeqs);				topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnDetail);					topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnCopy);					topRowPanel.add(Box.createHorizontalStrut(40));
		topRowPanel.add(btnTable);		
		
		topRowPanel.setMaximumSize(topRowPanel.getPreferredSize());
		topRowPanel.setMinimumSize(topRowPanel.getPreferredSize());
	}
	
	/********************************************************
	 * XXX Methods called by BasicSeqFilterPanel
	 */
	public void hideAll() {
		topRowPanel.setVisible(false);
		theTablePanel.setVisible(false);
		theMainFrame.setButtonsVisible(false);
	}
	public void showAll() {
		topRowPanel.setVisible(true);
		theTablePanel.setVisible(true);
	}
	public void setStatus(String txt) {
		theTablePanel.setStatus(txt);
	}
	public void appendStatus(String txt) {
		theTablePanel.setStatus(theFilterStr + "        " + txt);
	}
	/*******************************************************
	 * Private methods
	 */
	private void viewSelectedDetail() {
		String [] seqNames = getSelectedSeqs();
		int len = seqNames.length;
		if (len==0) return;

		theMainFrame.addSeqDetailPage(seqNames[0], this, -1);
	}
	private void viewSelectedSeqs() {
		String [] seqNames = getSelectedSeqs();
		int len = seqNames.length;
		if (len==0) return;

		String tag = (len>1) ? "Seqs" : seqNames[0];
		theMainFrame.loadContigs(tag, seqNames, STCWFrame.BASIC_QUERY_MODE_SEQ );
	}
	private String [] getSelectedSeqs() {
		int [] rows = theTablePanel.getSelectedRows();
		String [] ids = new String [rows.length];
		for (int r=0; r<rows.length; r++)
			ids[r]  = seqObjList.get(rows[r]).strSeqID;
		return ids;
	}
	
	/*************************************************************
	 * XXX Data classes
	 */
	private class SeqData {
		public SeqData(int r, Object [] values) {
			nRowNum = r;
			strSeqID = (String) values[0];
			longestRead = (String) values[1];
			strTCW = (String) values[2];
			strUser = (String) values[3];
			nTotalExp = (Integer) values[4];
			strBestHit = (String) values[5];
			if (strBestHit==null) strBestHit=""; 
		}
		public Object getValueAt(int column) {
			switch(column) {
				case 0: return nRowNum;
				case 1: return strSeqID;
				case 2: return longestRead;
				case 3: return strTCW;
				case 4: return strUser;
				case 5: return nTotalExp;
				case 6: return strBestHit;
				default:
					return 0;
			}
		}
		public int compareTo(SeqData obj, boolean ascOrder, int column) {
			int order = 1;
			if(!ascOrder) order = -1;
			
			switch(column) {
			case 0: return 0; // CAS326 order * ((Integer) nRowNum).compareTo((Integer) obj.nRowNum);
			case 1: return order * strSeqID.compareTo(obj.strSeqID);
			case 2: return order * compareStrings(longestRead, obj.longestRead);
			case 3: return order * compareStrings(strTCW, obj.strTCW);
			case 4: return order * compareStrings(strUser, obj.strUser);
			case 5: return order * ((Integer) nTotalExp).compareTo((Integer) obj.nTotalExp);
			
			case 6: return order * compareStrings(strBestHit, obj.strBestHit);
			default: return 0;
			}
		}
		private int compareStrings(String str1, String str2) {
			if(str1==null && str2==null) return -1;
			if(str1==null) return -1;
			if(str2==null) return 1;
			return str1.compareTo(str2);
		}
		private int nRowNum = -1;
		private String strSeqID = "";
		private int nTotalExp = 0;
		private String strTCW = "", strUser="";
		private String strBestHit = "";
		private String longestRead="";
	}
	private class SeqDataComparator implements Comparator<SeqData> {
		public SeqDataComparator(boolean sortAsc, int mode) {
			bSortAsc = sortAsc;
			nMode = mode;
		}		
		public int compare(SeqData obj1, SeqData obj2) { return obj1.compareTo(obj2, bSortAsc, nMode); }
		
		private boolean bSortAsc;
		private int nMode;
	}
	
	/***********************************************
	 * Table
	 */
	 // The database call was performed in BasicSeqFilterPanel, and passes in the results for display
	 public void tableBuild(ArrayList<Object []> results, String summary) {
		 try {
			 theTablePanel.setStatus("Building " + results.size() + " rows...");
			 
			 seqObjList.clear();
			 int row=1;
			 for (Object [] o : results) {
				 seqObjList.add(new SeqData(row, o));
				 row++;
			 }
			 theTablePanel.tableRefresh();	
			 theFilterStr = String.format("Seqs: %,d      %s", seqObjList.size(), summary);
			 theTablePanel.setStatus(theFilterStr);
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	public void tableAdd(ArrayList<Object []> results, String summary) {
		 try {
			 theTablePanel.setStatus("Adding " + results.size() + " rows. Please Wait..");
			
			 int row=seqObjList.size(), cnt=0;
			 for (Object [] o : results) {
				 boolean add=true;
				 String seqid = (String) o[0];
				 for (SeqData sd : seqObjList) {
					 if (sd.strSeqID.equals(seqid)) {
						 add = false;
						 break;
					 }
				 }
				 if (add) {
					 seqObjList.add(new SeqData(row, o));
					 row++; cnt++;
				 }
			 }
			 theTablePanel.tableRefresh();	
			 theFilterStr = String.format("Seqs: %,d  (add %d)     %s", seqObjList.size(),  cnt, summary);
			 theTablePanel.setStatus(theFilterStr);
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	 /*********************************************************
	  * CAS334 Queries the database, but then selects any rows that match
	  */
	 public void tableSelect(ArrayList<Object []> results, String summary) {
		 try {
			 theTablePanel.setStatus("Selecting " + results.size() + " possible rows. Please Wait..");
			
			 int cnt=0;
			 for (int i=0; i<seqObjList.size(); i++) { 
				 for (Object [] o : results) {
					 String seqid = (String) o[0];
					 if (seqObjList.get(i).strSeqID.equals(seqid)) {
						 theTablePanel.selectRow(i);
						 cnt++;
						 break;
					 }
				 }
				 if (i>0 && i%5000==0) 
					 theTablePanel.setStatus("Selected " + cnt + " from " + i + ". Still working...");
			 }	
			 appendStatus(String.format("Select %,d", cnt));
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	public void tableSort (boolean sortAsc, int mode) {
		try {// CAS314 Comparison method violates its general contract!
			Collections.sort(seqObjList, new SeqDataComparator(sortAsc, mode));		
		} catch (Exception e) {}
	}
	public int getNumRow() {
		return seqObjList.size();
	}
	public Object getValueAt(int row, int index) {
		 return seqObjList.get(row).getValueAt(index);
	}
	public void deleteFromList(int [] rows) { // CAS335 BasicTable was calling row by row
		for(int x=rows.length-1; x>=0; x--) {
			seqObjList.remove(rows[x]);
		}
		theFilterStr = "Seqs: " + getNumRow();
		theTablePanel.setStatus(theFilterStr);
	}
	
	public void enableButtons(boolean b) {
		btnViewSeqs.setEnabled(b);
		btnCopy.setEnabled(b);
		btnDetail.setEnabled(b);
		btnTable.setEnabled(b);
		theTablePanel.enableButtons(b);
	}
	public void updateTopButtons(int nSel, int nRow) {
		btnViewSeqs.setEnabled(nSel>0);
		btnCopy.setEnabled(nSel==1);
		btnDetail.setEnabled(nSel==1);
		btnTable.setEnabled(nRow>0);
	}
	
	public void tableRefresh() {
		theTablePanel.setColumns(theFilterPanel.getColNames(), theFilterPanel.getColSelect());
		theTablePanel.tableRefresh();
	}
	public void close() {
		seqObjList.clear();
	}
	public BasicSeqFilterPanel getFilterPanel() { return theFilterPanel;}
	public BasicTablePanel getTablePanel() { return theTablePanel;}
	
	//Table panel
	private BasicTablePanel theTablePanel = null;
	private Vector<SeqData> seqObjList = new Vector<SeqData> (); // CAS334 was ArrayList; possible synchronization problem
	
	//User interface
	private JButton btnViewSeqs = null, btnTable = null, btnCopy = null, btnDetail = null;	// CAS334 add detail
	
	private JPanel topRowPanel = null;
	private BasicSeqFilterPanel theFilterPanel = null;
	private STCWFrame theMainFrame=null;
	
	private String theFilterStr=""; // append to this
}
