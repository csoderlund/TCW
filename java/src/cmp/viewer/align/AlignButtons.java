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
import util.methods.Static;
import util.ui.UserPrompt;

/**************************************************************
 * CAS340 Pairwise..., MSA..., MSAdb buttons and create panels, e.g.
 * 	AlignButtons alignButton = new AlignButtons(params);
 * 	JButton pairButton = alignButton.createPairAlign();	
 */
public class AlignButtons {
	private final static int bSEQ= Globals.bSEQ;
	private final static int bPAIR=Globals.bPAIR;
	private final static int bGRP= Globals.bGRP;
	private final static int bHIT= Globals.bHIT;
	
	private final static int MUSCLE = Globals.Ext.MUSCLE;
	private final static int MAFFT =  Globals.Ext.MAFFT;
	
	private final static int AA_CDS_NT=0; 	// AA,CDS,NT
	private final static int UTR_CDS  =1;   // 5UTR,CDS, 3UTR
	private final static int AlignAA = 		PairAlignData.AlignAA;
	private final static int AlignNT = 		PairAlignData.AlignNT;
	private final static int AlignHIT0_AA=	PairAlignData.AlignHIT0_AA;
	private final static int AlignHIT1_AA=	PairAlignData.AlignHIT1_AA;
	
	private final static String PW = "PW: ";
	private final static String Mxx = "MSA: ";
	
	public AlignButtons (MTCWFrame vFrame, SeqsTablePanel seqPanel) { // Not used
		theSeqTable = seqPanel;
		init(vFrame, bSEQ);
	}
	public AlignButtons (MTCWFrame vFrame, PairTablePanel pairPanel) {
		thePairTable = pairPanel;
		init(vFrame, bPAIR);
	}
	public AlignButtons (MTCWFrame vFrame, GrpTablePanel grpPanel) {
		theGrpTable = grpPanel;
		init(vFrame, bGRP);
	}
	public AlignButtons (MTCWFrame vFrame, HitTablePanel hitPanel) {
		theHitTable = hitPanel;
		init(vFrame, bHIT);
	}
	private void init(MTCWFrame vFrame, int vType) {
		theViewerFrame = vFrame;
		viewType = vType;
		hasNTdbOnly = (theViewerFrame.getnNTdb()>0 && theViewerFrame.getnAAdb()==0); // items that will not work with mTCW that have AAdbs
	}
	/*****************************************************************************/
	public JButton createBtnPairAlign() {
		JButton btnPairAlign = Static.createButtonTab("Pairwise...", false);
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
	    itemHIT1 = new JMenuItem(new AbstractAction("AA to pair best hit") {
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
	    if (viewType == bGRP || viewType == bPAIR) pairPop.add(itemHIT1); // no consensus hit
	    
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

	    itemMuscle = new JMenuItem(new AbstractAction("MUSCLE-AA") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				opMultiAlign(MUSCLE, Globals.AA);
 			}
 		});
	    multiPop.add(itemMuscle);
	    
	    itemMafftAA = new JMenuItem(new AbstractAction("MAFFT-AA") {
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
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab =  PW  + val[0]+ ": AA";
		String sum = val[1];
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame,curSeqSet, AlignAA, sum); 
		
		displayTab(newPanel, tab, sum);	
	}
	private void opPairNT() {
		String [] curSeqSet = getSeqIDlist();
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab =  PW  + val[0]+ ": NT";
		String sum =  val[1];
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame,curSeqSet,AlignNT,sum);
		
		displayTab(newPanel, tab, sum);
	}
	private void opPairCDS() {
		String [] curSeqSet = getSeqIDlist();
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab =  PW  + val[0]+ ": CDS";
		String sum = val[1];
		
		JPanel newPanel = new Pair3ViewPanel(theViewerFrame, curSeqSet, AA_CDS_NT, sum); 
		
		displayTab(newPanel, tab, sum);
	}
	private void opPairUTR() {
		String [] curSeqSet = getSeqIDlist();
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab = PW  + val[0]+ ": UTR";
		String sum = val[1];
		
		JPanel newPanel = new Pair3ViewPanel(theViewerFrame, curSeqSet,  UTR_CDS, sum); 
		
		displayTab(newPanel, tab, sum);
	}
	private void opSeqHit0() { 	
		String [] curSeqSet = getSeqIDlist();
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab = PW  + val[0]+ ": Hit";
		String sum = val[1];
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame, curSeqSet,  AlignHIT0_AA, sum); 	
		
		displayTab(newPanel, tab, sum);
	}
	private void opSeqHit1() { // CAS305 Seq to the cluster's best hit	
		String [] curSeqSet = getSeqIDlist();
		if (tooManyPairwise(curSeqSet)) return;
		
		String [] val = getTabSum();
		String tab = PW  + val[0]+ ": Best";
		String sum = val[1];
		String hitID = getHitID();
		
		JPanel newPanel = new PairNViewPanel(theViewerFrame,curSeqSet,AlignHIT1_AA,sum, hitID); 	
		displayTab(newPanel, tab, sum);
	}
	
	private void opMultiDB() { // already aligned, in db
		String [] curSeqSet = getSeqIDlist();
		String [] val = getTabSum();
		String tab = Mxx + val[0] + ": MSAdb";
		String sum = val[1];
		
		int grpID = getGrpSQLID();
		JPanel newPanel = new MultiViewPanel(theViewerFrame, curSeqSet, grpID, sum);
		displayTab(newPanel, tab, sum);
	}
	private void opMultiAlign(int alignPgm, int type) {
		String [] curSeqSet = getSeqIDlist();
		if (curSeqSet.length>50) { // CAS305
			String msg = "Selected " + curSeqSet.length + " for multiple alignment";
			if (!UserPrompt.showContinue("Multiple align", msg)) return;
		}
		String [] val = getTabSum();
		
		String pgm = (alignPgm==MUSCLE) ? "Mus" : "Maf"; 
		if (type==Globals.AA) 		pgm = pgm + "AA";
		else if (type==Globals.NT) 	pgm = pgm + "NT";
		else 						pgm = pgm + "CDS";
		
		String tab = Mxx +  val[0] + " :" + pgm;
		String sum = val[1];
		
		JPanel newPanel  = new MultiViewPanel(theViewerFrame, curSeqSet, alignPgm, type, sum);
			
		displayTab(newPanel, tab, sum);
	}
	/*********************************************************************/
	public void setPWitemsActive() { 
		if (thePairTable==null) return;
		int selCount = thePairTable.getSelectedCount();
		
		itemAA.setEnabled(true); 
		itemNT.setEnabled(true);    // not shown if !hasNTdbOnly
		itemHIT0.setEnabled(true);	// best hit per sequence
		
		boolean b = (selCount==1);
		itemHIT1.setEnabled(b);  // only best consensus hit for one pair
		itemCDS.setEnabled(b);   // not shown if !hasNTdbOnly
		itemUTR.setEnabled(b); 
	}
	
	/********************************************************************
	 * SeqTable not aligning anything right now
	 */
	private String [] getSeqIDlist() { // list of UTstr
		if (theSeqTable!=null)  return  theSeqTable.getSelectedSeqIDs(); 
		if (thePairTable!=null) return  thePairTable.getSelectedSeqIDs(); 
		if (theGrpTable!=null)  return  theGrpTable.getSelectedSeqIDs();
		return null;
	}
	private String [] getTabSum() {
		if (theSeqTable!=null)  return theSeqTable.getTabSum();
		if (thePairTable!=null) return thePairTable.getTabSum(); 
		if (theGrpTable!=null)  return theGrpTable.getTabSum();
		return null;
	}
	private int getGrpSQLID() {
		if (theGrpTable!=null) return theGrpTable.getGrpSQLID();
		return 1;
	}
	private String getHitID() {
		if (thePairTable!=null) return thePairTable.getHitStr();
		return null;
	}
	private void displayTab(JPanel newPanel, String tab, String sum) {
		if (theSeqTable!=null) 		 theViewerFrame.addResultPanel(theSeqTable,  newPanel, tab, sum);	
		else if (thePairTable!=null) theViewerFrame.addResultPanel(thePairTable, newPanel, tab, sum);	
		else if (theGrpTable!=null)  theViewerFrame.addResultPanel(theGrpTable,  newPanel, tab, sum);
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
	/*********************************************************/
	private JMenuItem itemAA, itemNT, itemCDS, itemUTR, itemHIT0, itemHIT1;	
	private JMenuItem itemMuscle, itemMafftAA, itemMafftCDS, itemMafftNT;
	
	private MTCWFrame      theViewerFrame = null;
	private SeqsTablePanel theSeqTable = null;
	private PairTablePanel thePairTable = null;
	private GrpTablePanel  theGrpTable = null;
	private HitTablePanel  theHitTable = null;
	
	private boolean hasNTdbOnly=false;
	private int viewType=0;
}
