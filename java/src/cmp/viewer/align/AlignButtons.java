package cmp.viewer.align;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import cmp.align.PairAlignData;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import cmp.viewer.pairs.PairTablePanel;
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.seq.SeqsTablePanel;
import cmp.viewer.hits.HitTablePanel;
import cmp.viewer.seqDetail.SeqTopRowPanel;
import util.file.FileHelpers;
import util.methods.Static;
import util.ui.UserPrompt;

/**************************************************************
 * CAS340 Pairwise..., MSA..., MSAdb buttons and create panels, e.g.
 * 	AlignButtons alignButton = new AlignButtons(params);
 * 	JButton pairButton = alignButton.createPairAlign();	
 */
public class AlignButtons {
	private final static int MUSCLE = Globals.Ext.MUSCLE;
	private final static int MAFFT =  Globals.Ext.MAFFT;
	
	private final static int AA_CDS_NT=0; 	// AA,CDS,NT
	private final static int UTR_CDS  =1;   // 5UTR,CDS, 3UTR
	private final static int AlignAA = 		PairAlignData.AlignAA;
	private final static int AlignNT = 		PairAlignData.AlignNT;
	private final static int AlignHIT0_AA=	PairAlignData.AlignHIT0_AA;
	private final static int AlignHIT1_AA=	PairAlignData.AlignHIT1_AA;
	
    int pwIndex=0;			// Pair              Hit                                Detail
	String [] pwMsgN =    {"for each pair",       "for each pairs of nSeq", 	"to pair seqID"};
	String [] pwMsg3 =    {"for one pair",       "(N/A)",                       "to pair seqID"};
	String [] pwMsgHit0 = {"to seq best hitID",  "each nSeq to its best hitID", "to best hit(N/A)"};
	String [] pwMsgHit1 = {"to pair best hitID", "each nSeq to the row hitID",  "to hitID"};
	
	public AlignButtons (MTCWFrame vFrame, SeqsTablePanel seqPanel) { 
		theSeqTable = seqPanel;
		init(vFrame, 0);
	}
	public AlignButtons (MTCWFrame vFrame, PairTablePanel pairPanel) {
		thePairTable = pairPanel;
		init(vFrame, 0);
	}
	public AlignButtons (MTCWFrame vFrame, GrpTablePanel grpPanel) {
		theGrpTable = grpPanel;
		init(vFrame, 0);
	}
	public AlignButtons (MTCWFrame vFrame, HitTablePanel hitPanel) {
		theHitTable = hitPanel;
		init(vFrame, 1);
	}
	public AlignButtons (MTCWFrame vFrame, SeqTopRowPanel detPanel) {
		theDetailTable = detPanel;
		init(vFrame, 2);
	}
	private void init(MTCWFrame vFrame, int pw) {
		pwIndex = pw;
		theViewerFrame = vFrame;
		hasNTdbOnly = (theViewerFrame.getnNTdb()>0 && theViewerFrame.getnAAdb()==0); // items that will not work with mTCW that have AAdbs
	}
	/*****************************************************************************/
	public JButton createBtnPairAlign() {
		boolean active = (theDetailTable!=null) ? true : false;
		JButton btnPairAlign = Static.createButtonTab("Pairwise...", active);
		btnPairAlign.addActionListener(new ActionListener() {
	   		public void actionPerformed(ActionEvent arg0) {
    	   		setPWitemsActive();
	   		}
		});
		btnPairAlign.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus("Pairwise alignments of all or selected sequences (AA,NT) or selected pair (AA,CDS,NT)");}
			public void mouseExited(MouseEvent e) {theViewerFrame.setStatus("");}
		});
	    final JPopupMenu pairPop = new JPopupMenu();
	    pairPop.setBackground(Color.WHITE);
	    
	    itemAA = new JMenuItem(new AbstractAction("AA " + pwMsgN[pwIndex]) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairAA();
 			}
 		});
	    pairPop.add(itemAA);
	    
	    itemNT = new JMenuItem(new AbstractAction("NT " + pwMsgN[pwIndex]) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairNT();
 			}
 		});
	    itemCDS = new JMenuItem(new AbstractAction("AA,CDS,NT " + pwMsg3[pwIndex]) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairCDS();
 			}
 		});
	    itemUTR = new JMenuItem(new AbstractAction("5UTR,CDS,3UTR " +  pwMsg3[pwIndex]) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opPairUTR();
 			}
 		});
	   
	    itemHIT0 = new JMenuItem(new AbstractAction("AA " +  pwMsgHit0[pwIndex]) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				opSeqHit0();
 			}
 		});  
	    itemHIT1 = new JMenuItem(new AbstractAction("AA " +  pwMsgHit1[pwIndex]) {
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
	    pairPop.add(itemHIT1); // no consensus hit
	    
	    btnPairAlign.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				pairPop.show(e.getComponent(), e.getX(), e.getY());
			}
	    });
	    
	    return btnPairAlign;
	}
	
	public JButton createBtnMultiAlign() { 
		JButton btnMSArun = Static.createButtonTab("MSA...", false);
		btnMSArun.addActionListener(new ActionListener() {
	   		public void actionPerformed(ActionEvent arg0) {
 	   		}
		});
		
		btnMSArun.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus("Multiple alignment of all sequences or selected set");}
			public void mouseExited(MouseEvent e) {theViewerFrame.setStatus("");}
		});
		
	    final JPopupMenu multiPop = new JPopupMenu();
	    multiPop.setBackground(Color.WHITE);

	    String hit = (theGrpTable==null) ? "" : "+hit";
	    itemMuscle = new JMenuItem(new AbstractAction("MUSCLE-AA"+hit) {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MUSCLE, Globals.AA);
 			}
 		});
	    if (!FileHelpers.isMacM4()) multiPop.add(itemMuscle); // CAS405 not available
	    
	    itemMafftAA = new JMenuItem(new AbstractAction("MAFFT-AA"+hit) {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MAFFT, Globals.AA);
 			}
 		});
	    multiPop.add(itemMafftAA);
	    
	    itemMafftCDS = new JMenuItem(new AbstractAction("MAFFT-CDS") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MAFFT, Globals.CDS);
 			}
 		});
	    if (hasNTdbOnly) multiPop.add(itemMafftCDS);
	    
	    itemMafftNT = new JMenuItem(new AbstractAction("MAFFT-NT") {
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
	    return btnMSArun;
	}
	public JButton createBtnMultiDB() {
		JButton btnMSAdb = Static.createButtonTab("MSAdb", false);
		btnMSAdb.addActionListener(new ActionListener() {
	   		public void actionPerformed(ActionEvent arg0) {
	   			boolean hasMSA = theGrpTable.selectedHasMSA();
	   			if (!hasMSA) {
	   				UserPrompt.showWarn("The selected cluster does not have a precomputed MSA");
	   				return;
	   			}
       	   		opMultiDB();
	   		}
		});
		return btnMSAdb;
	}
	/*********************************************************/
	private void opPairAA() {
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab =  Globals.tagPW  + val[0]+ ": AA";
		String sum = val[1];
		int row = Static.getInteger(val[2], -1);
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame, this, curSeqSet, AlignAA, sum, row); 
		
		displayTab(newPanel, tab, sum);	
	}
	private void opPairNT() {
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab =  Globals.tagPW  + val[0]+ ": NT";
		String sum =  val[1];
		int row = Static.getInteger(val[2], -1);
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame, this, curSeqSet,AlignNT,sum, row);
		
		displayTab(newPanel, tab, sum);
	}
	private void opPairCDS() {
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab =  Globals.tagPW  + val[0]+ ": CDS";
		String sum = val[1];
		int row = Static.getInteger(val[2], -1);
		
		JPanel newPanel = new Pair3ViewPanel(theViewerFrame, this, curSeqSet, AA_CDS_NT, sum, row); 
		
		displayTab(newPanel, tab, sum);
	}
	private void opPairUTR() {
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab = Globals.tagPW  + val[0]+ ": UTR";
		String sum = val[1];
		int row = Static.getInteger(val[2], -1);
		
		JPanel newPanel = new Pair3ViewPanel(theViewerFrame, this, curSeqSet,  UTR_CDS, sum, row); 
		
		displayTab(newPanel, tab, sum);
	}
	private void opSeqHit0() { 	// to each seq's best hit
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab = Globals.tagPW  + val[0]+ ": Hit";
		String sum = val[1];
		int row = Static.getInteger(val[2], -1);
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame, this, curSeqSet,  AlignHIT0_AA, sum, row); 	
		
		displayTab(newPanel, tab, sum);
	}
	private void opSeqHit1() { // CAS305 the cluster/pair best hit	against each seq
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		
		String x = ": Best";
		if (theDetailTable!=null) { // special case for Details
			String seq = curSeqSet[0];
			curSeqSet = new String [1];
			curSeqSet[0] = seq;
			x="";
		}
		String hitID =  getHitID();   // Detail needs this before getTabSum to set tab/sum
		String [] val = getTabSum();
		String tab = Globals.tagPW  + val[0] + x;
		String sum = val[1];
		int row = Static.getInteger(val[2], -1);
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame,this, curSeqSet,AlignHIT1_AA,sum, hitID, row); 	
		displayTab(newPanel, tab, sum);
	}
	
	private void opMultiDB() { // already aligned, in db
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab = Globals.tagMxx + val[0] + makeTagMSAdb();
		String sum = val[1];
		int row = Static.getInteger(val[2], -1);
		
		int grpID = getGrpSQLID();
		JPanel newPanel = new MultiViewPanel(theViewerFrame, this, curSeqSet, grpID, sum, row);
		displayTab(newPanel, tab, sum);
	}
	private void opMultiAlign(int alignPgm, int type) {
		String [] curSeqSet = getSeqIDlist();
		if (noSeqToAlign(curSeqSet)) return;
		if (curSeqSet.length>50) { // CAS305
			String msg = "Selected " + curSeqSet.length + " for multiple alignment";
			if (!UserPrompt.showContinue("Multiple align", msg)) return;
		}
		String [] val = getTabSum();
		
		String tab = Globals.tagMxx +  val[0] + ": " + makeTagMSA( alignPgm, type);
		String sum = val[1];
		int row = Static.getInteger(val[2], -1);
		String hitID = getHitID();
		
		JPanel newPanel  = new MultiViewPanel(theViewerFrame, this, curSeqSet,hitID, alignPgm, type, sum, row);
			
		displayTab(newPanel, tab, sum);
	}
	public String makeTagMSA(int alignPgm, int type) { // MultiViewPanel needs this too
		String pgm = (alignPgm==MUSCLE) ? "Mus" : "Maf"; 
		if (type==Globals.AA) 		pgm = pgm + "AA";
		else if (type==Globals.NT) 	pgm = pgm + "NT";
		else 						pgm = pgm + "CDS";
		return pgm;
	}
	public String makeTagMSAdb() {
		return  ": MSAdb";
	}
	/*********************************************************************/
	public void setPWitemsActive() { 
		if (thePairTable==null && theHitTable==null && theDetailTable==null) return;
			
		itemAA.setEnabled(true); 
		itemNT.setEnabled(true);    // not shown if !hasNTdbOnly
		itemCDS.setEnabled(true);   // not shown if !hasNTdbOnly
		itemUTR.setEnabled(true); 
		itemHIT0.setEnabled(true);	// best hit for each sequence
		itemHIT1.setEnabled(true);  // only best hit for one pair
		
		if (thePairTable!=null) {
			int nsel = thePairTable.getSelectedCount();
			itemCDS.setEnabled(nsel==1);   
			itemUTR.setEnabled(nsel==1); 
		}
		if (theHitTable!=null) {	
			itemCDS.setEnabled(false);  
			itemUTR.setEnabled(false); 
		}
		else if (theDetailTable!=null){ // only one can be selected
			itemHIT0.setEnabled(false);
			itemHIT1.setEnabled(theDetailTable.hasHits());
			if (!theDetailTable.hasPairs()) {
				itemAA.setEnabled(false); 
				itemNT.setEnabled(false);    // not shown if !hasNTdbOnly
				itemCDS.setEnabled(false);   // not shown if !hasNTdbOnly
				itemUTR.setEnabled(false); 
			}
		}
	}

	/********************************************************************
	 * alignments
	 */
	public String [] getSeqIDlist() { // list of UTstr
		if (theSeqTable!=null)  	return theSeqTable.getAlignSeqIDs(); 
		if (thePairTable!=null) 	return thePairTable.getAlignSeqIDs(); 
		if (theGrpTable!=null)  	return theGrpTable.getAlignSeqIDs();
		if (theHitTable!=null)  	return theHitTable.getAlignSeqIDs();
		if (theDetailTable!=null) 	return theDetailTable.getAlignSeqIDs();
			
		return null;
	}
	private String [] getTabSum() { // return String [] = {tab, summary, row#}
		if (theSeqTable!=null)  	return theSeqTable.getTabSum();
		if (thePairTable!=null) 	return thePairTable.getTabSum(); 
		if (theGrpTable!=null)  	return theGrpTable.getTabSum();
		if (theHitTable!=null)  	return theHitTable.getTabSum();
		if (theDetailTable!=null) 	return theDetailTable.getTabSum();
		
		return null;
	}
	private int getGrpSQLID() {
		if (theGrpTable!=null) return theGrpTable.getGrpSQLID();
		return 1;
	}
	private String getHitID() {
		if (theGrpTable!=null) 		return theGrpTable.getHitStr();
		if (thePairTable!=null) 	return thePairTable.getHitStr();
		if (theHitTable!=null) 		return theHitTable.getHitStr();
		if (theDetailTable!=null) 	return theDetailTable.getHitStr();
		return null;
	}
	private void displayTab(JPanel newPanel, String tab, String sum) {
		if (theSeqTable!=null) 		 	theViewerFrame.addResultPanel(theSeqTable,  newPanel, tab, sum);	
		else if (thePairTable!=null) 	theViewerFrame.addResultPanel(thePairTable, newPanel, tab, sum);	
		else if (theGrpTable!=null)  	theViewerFrame.addResultPanel(theGrpTable,  newPanel, tab, sum);
		else if (theHitTable!=null)  	theViewerFrame.addResultPanel(theHitTable,  newPanel, tab, sum);
		else if (theDetailTable!=null)  theViewerFrame.addResultPanel(theDetailTable,  newPanel, tab, sum);
	}
	private boolean tooManyPairwise(String [] curSeqSet) { // CAS305
		if (curSeqSet.length>20) {
			int n = curSeqSet.length;
			int m = (n*(n-1))/2;
			String msg = "Selected " + n + " sequences for " + m + " alignments";
			return !UserPrompt.showContinue("Pairwise align", msg);
		}
		return false;
	}
	private boolean noSeqToAlign(String [] curSeqSet) {
		if (curSeqSet==null || curSeqSet.length<=1) {
			UserPrompt.showMsg("No sequences to align");
			return true;
		}
		return false;
	}
	public String [] getNextRow(int nRow) { 
		if (theGrpTable!=null)  return theGrpTable.getNextGrpForMSA(nRow);
		if (thePairTable!=null) return thePairTable.getNextPairForPW(nRow);
		if (theHitTable!=null)  return theHitTable.getNextHitForPW(nRow);
		
		return null;
	}
	public String [] getSeqIDrow(int row) {
		if (theGrpTable!=null)  return theGrpTable.getAlignSeqIDs(row);
		if (thePairTable!=null) return thePairTable.getAlignSeqIDs(row);
		if (theHitTable!=null)  return theHitTable.getAlignSeqIDs(row);
		return null;
	}
	
 	/*********************************************************/
	private JMenuItem itemAA, itemNT, itemCDS, itemUTR, itemHIT0, itemHIT1;	
	private JMenuItem itemMuscle, itemMafftAA, itemMafftCDS, itemMafftNT;
	
	private MTCWFrame      theViewerFrame = null;
	private SeqsTablePanel theSeqTable = null;
	private PairTablePanel thePairTable = null;
	private GrpTablePanel  theGrpTable = null;
	private HitTablePanel  theHitTable = null;
	private SeqTopRowPanel theDetailTable = null;
	
	private boolean hasNTdbOnly=false;
}
