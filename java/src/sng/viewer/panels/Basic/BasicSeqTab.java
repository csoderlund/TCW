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
import util.ui.UserPrompt;

public class BasicSeqTab extends Tab {
	private static final long serialVersionUID = -5515249017308285183L;	
	private static final Color BGCOLOR = Globals.BGCOLOR;
	private final String helpHTML = Globals.helpDir + "BasicQuerySeq.html";

	public BasicSeqTab(STCWFrame parentFrame) {
		super(parentFrame, null);
		theParentFrame = parentFrame;	
		
		setBackground(BGCOLOR);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		createTopRowPanel();
		add(topRowPanel);
		add(Box.createVerticalStrut(6));
		
		theFilterPanel = new BasicSeqFilterPanel(theParentFrame, this);
		add(theFilterPanel);
		add(Box.createVerticalStrut(6));
		
		theTablePanel = new BasicTablePanel(theParentFrame, this, 
				theFilterPanel.getColNames(), theFilterPanel.getColSelect());
		add(theTablePanel);
	}
	/******************************************
	 * Top button panel
	 */
	private void createTopRowPanel() {
		btnViewSeqs = new JButton(Globals.seqTableLabel);
		btnViewSeqs.setBackground(Globals.FUNCTIONCOLOR);
		btnViewSeqs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewSelectedSeqs();
			}
		});
		btnViewSeqs.setEnabled(false);
		
		btnDetail = new JButton(Globals.seqDetailLabel);
		btnDetail.setBackground(Globals.FUNCTIONCOLOR);
		btnDetail.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewSelectedDetail();
			}
		});
		btnDetail.setEnabled(false);
		
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
		if (!theParentFrame.getMetaData().bUseOrigName()) {// CAS311
			String longLabel = theParentFrame.getMetaData().getLongLabel(); // CAS311
			
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
		btnCopy = new JButton("Copy...");
		btnCopy.setBackground(Color.WHITE);
		btnCopy.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                copypopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });		
		btnCopy.setEnabled(false);
		
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
		btnTable = new JButton("Table...");
		btnTable.setBackground(Color.WHITE);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablepopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });	
		
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					UserPrompt.displayHTMLResourceHelp(theParentFrame, 
					"Filter Sequences", helpHTML);
			}
		});

		topRowPanel = Static.createRowPanel();
		topRowPanel.add(new JLabel("Selected:"));
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnViewSeqs);
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnDetail);
		topRowPanel.add(Box.createHorizontalStrut(1));
		topRowPanel.add(btnCopy);
		topRowPanel.add(Box.createHorizontalStrut(60));
		topRowPanel.add(btnTable);
		topRowPanel.add(Box.createHorizontalStrut(160)); // CAS334 help was off the page
		topRowPanel.add(Box.createHorizontalGlue());
		topRowPanel.add(btnHelp);
		topRowPanel.setMaximumSize(topRowPanel.getPreferredSize());
		topRowPanel.setMinimumSize(topRowPanel.getPreferredSize());
	}
	
	/********************************************************
	 * XXX Methods called by BasicSeqFilterPanel
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
	private void viewSelectedDetail() {
		String [] seqNames = getSelectedSeqs();
		int len = seqNames.length;
		if (len==0) return;

		theParentFrame.addSeqDetailPage(seqNames[0], this, -1);
	}
	private void viewSelectedSeqs() {
		String [] seqNames = getSelectedSeqs();
		int len = seqNames.length;
		if (len==0) return;

		String tag = (len>1) ? "Seqs" : seqNames[0];
		theParentFrame.loadContigs(tag, seqNames, STCWFrame.BASIC_QUERY_MODE_SEQ );
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
			 enableTopButtons(false);
			 seqObjList.clear();
			 int row=1;
			 for (Object [] o : results) {
				 seqObjList.add(new SeqData(row, o));
				 row++;
			 }
			 theTablePanel.tableRefresh();			
			 String r = String.format("Results: %,d      %s", seqObjList.size(), summary);
			 theTablePanel.setStatus(r);
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	 public void tableAdd(ArrayList<Object []> results, String summary) {
		 try {
			 enableTopButtons(false);
			
			 int row=seqObjList.size();
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
					 row++;
				 }
			 }
			 
			 theTablePanel.tableRefresh();			
			 String r = String.format("Results: %,d       %s", seqObjList.size(), summary);
			 theTablePanel.setStatus(r);
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Updating table");}
	}
	 /*********************************************************
	  * CAS334 Queries the database, but then selects any rows that match
	  */
	 public void tableSelect(ArrayList<Object []> results, String summary) {
		 try {
			 theTablePanel.setStatus("Selecting " + results.size() + " possible rows. Please Wait..");
			 enableTopButtons(false);
			
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
				 if (i>0 && i%5000==0) setStatus("Selected " + cnt + " from " + i + ". Still working...");
			 }
		
			 theTablePanel.setStatus("");
			 //theTablePanel.tableRefresh();	Loses highlight		
			 String r = String.format("Results: %,d   Select %d  %s", seqObjList.size(), cnt, summary);
			 theTablePanel.setStatus(r);
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
	public void removeFromList(int row) {
		 seqObjList.remove(row);
	}
	public void updateTopButtons(int row) {
		btnViewSeqs.setEnabled(row>0);
		btnCopy.setEnabled(row==1);
		btnDetail.setEnabled(row==1);
	}
	public void enableTopButtons(boolean b) {
		btnViewSeqs.setEnabled(b);
		btnCopy.setEnabled(b);
		btnDetail.setEnabled(b);
	}
	public void enableBottomButtons(boolean b) { // CAS334 for new SELECT
		theTablePanel.enableButtons(b);
	}
	public void tableRefresh() {
		theTablePanel.setColumns(theFilterPanel.getColNames(), theFilterPanel.getColSelect());
		theTablePanel.tableRefresh();
	}
	public void close() {
		seqObjList.clear();
	}
	//Table panel
	private BasicTablePanel theTablePanel = null;
	private Vector<SeqData> seqObjList = new Vector<SeqData> (); // CAS334 was ArrayList; possible synchronization problem
	
	//User interface
	private JButton btnViewSeqs = null;
	private JButton btnDetail = null;	// CAS334
	private JButton btnTable = null;
	private JButton btnCopy = null;
	private JButton btnHelp = null;
	
	private JPanel topRowPanel = null;
	private BasicSeqFilterPanel theFilterPanel = null;
	private STCWFrame theParentFrame=null;
}
