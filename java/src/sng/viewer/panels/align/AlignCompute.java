/**
 * AlignCompute 
 * BasicHitFilterPanel: Set of DB hit - sequence pairs to align
 * SeqDetail: align hits to sequence
 * CoreAnno.doHomologyTests: pairwise Nucleotide against nucleotide alignment
 * PairTopRowTab: display pairwise contigs
 */
package sng.viewer.panels.align;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import sng.dataholders.BlastHitData;
import sng.dataholders.ContigData;
import sng.dataholders.SequenceData;
import sng.dataholders.MultiCtgData;
import sng.viewer.panels.seqDetail.SeqDetailPanel;
import util.align.AlignPairOrig;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

public class AlignCompute 
{
	public static final short frameResult=1; 
	public static final short bestResult=2; 
	public static final short allResult=3;
	public static final short orientResult=4;
	public static final short pairResult=5;
	
	/**************** STATIC ALIGNMENT METHODS *******************************/
	/********************************************
	 * BasicHitFilterPanel: Set of DB hit - sequence pairs to align
	 */
	static public Vector<AlignData> DBhitsAlignDisplay(
			Vector <String> seqList, 
			Vector <String> hitList, 
			Vector <BlastHitData> blastList,
			HashMap <String, ContigData> seqObj,
			HashMap <String, SequenceData> hitObj, 
			short type, boolean isP)  
	{
		 dpAlgoObj = new AlignPairOrig ();
		 alignList = new Vector<AlignData> ();
		 typeResult = type;
		 isAAsTCW = isP;
		
		 for (int i=0; i<seqList.size(); i++) {
			 ContigData   ctgData = seqObj.get(seqList.get(i));
			 SequenceData seqData = hitObj.get(hitList.get(i));
			 seqData.setBlastHitData(blastList.get(i));
			 hitAlign(ctgData, seqData);
		 }
		 return alignList;
	}
	 /***********************************************
	 *XXX	Sequence to DB hits - SeqTopRowTab for SeqDetails
     ***********************************************/
    /*******************************************************
     * align selected or align selected all (i.e. all frames)
     * type is best or all_frames
     */
	static public Vector<AlignData> DBhitsAlignDisplay (
			String [] selected, 
			ContigData ctgData1, 
			short type, boolean isP, 
			SeqDetailPanel seqPanel) throws Exception 
	{
       dpAlgoObj = new AlignPairOrig ();
       alignList = new Vector<AlignData> ();
       typeResult = type;
       isAAsTCW = isP;
       
       for (String hitName : selected) {
	   		String seq =    	seqPanel.getHitSeq(hitName);
	   		String dbtype = 	seqPanel.getHitType(hitName);
	   		boolean isProtein = seqPanel.isHitAA(hitName); // CAS331 was calculating based on dbtype
	   		
	   		SequenceData seqData = new SequenceData(dbtype);
	   		seqData.setSequence(seq);
	   		seqData.setName(hitName);
	   		
	   		BlastHitData blastData = new BlastHitData(hitName, isProtein, 
	   				seqPanel.getHitStart(hitName), seqPanel.getHitEnd(hitName), seqPanel.getHitMsg(hitName));
	   		seqData.setBlastHitData(blastData);
	   		
	   		hitAlign(ctgData1, seqData);
       }
	
       clear(true);
       return alignList;
	}
	
	static private void hitAlign ( 
			ContigData ctgData, SequenceData seqData) 
	{		
		ctgDataI = ctgData;
		ctgDataII = null;
	
		SequenceData   seq1 = ctgData.getSeqData(); // for proteins
		if (!isAAsTCW) seq1 = seq1.newSeqDataNoGap();
		
		SequenceData seq2 = seqData.newSeqDataNoQual();
		BlastHitData hitData = seqData.getBlastHitData();
		
		if (!hitData.isAAhit()) {	// database is nt and hit is nt
			seq2.setIsNT(); 
			
			int tr = typeResult; // CAS311 changing type can mess a following AA align
			if (typeResult==frameResult) typeResult=bestResult;
			
			hitAlignNT(seq1, seq2, 1, 0, hitData);
			typeResult = tr;
		}
		else if (isAAsTCW)       // database and hit are aa
			hitAlignAA(seq1, seq2, 0, 0, hitData);
		else {						// database is nt and hit is as
			int f1 = hitData.getFrame1(seq1.getLength()); 
			hitAlignAA(seq1, seq2, f1, 0, hitData);
		}
    }
   
	/***********************************************************
	 *  NT alignment
	 *********************************************************/
	static private void hitAlignNT ( 
			SequenceData seq1, SequenceData seq2, int f1, int f2, 
			BlastHitData hitData) {
		 
		 AlignData bestData = null, curData=null;
		
		if (f2 == 0) f2 = 1; // for user selected as there is no frame

		// if the result is NULL, its added to alignList as null
		bestData = alignNTdoDP (seq1, seq2, f1, f2, hitData);
		
		if (typeResult==frameResult) {
			alignList.add(bestData);
		}
		else if (typeResult==bestResult) { // try opposite direction and take best
			curData = alignNTdoDP (seq1, seq2, f1, -f2, hitData);		
			if (curData != null && cmpAlignData(curData, bestData)) 
					bestData = curData;
			alignList.add(bestData);
		}
		else if (typeResult==allResult) {
			alignList.add(bestData);
			curData = alignNTdoDP (seq1, seq2, f1, -f2, hitData);
			if (curData!=null) alignList.add(curData);
		}
	}

	/****************************************************************
	 * NT & NT DB: have both frames from database
	 * NT & AA DB: have NT frame from database	(display hit to sequence from seqDetails and Pairs Table)
	 * NT & NT DB: have NT frame from database, but none for NT annoDB
	 ***************************************************************/
	static private void hitAlignAA ( SequenceData seq1,  SequenceData seq2, 
			int f1, int f2, BlastHitData hitData) 
	{
		AlignData curData = null, bestData=null;
		
		if ((typeResult==frameResult) && f1!=0) { 	
			bestData = alignAAdoDP (seq1, seq2, f1, f2, hitData);
			alignList.add(bestData);
		}
		else if (f1==0 && f2==0) { 	// AA-AA protein database 
			bestData = alignAAdoDP (seq1, seq2, f1, f2, hitData);
			alignList.add(bestData);
		}
		else if (typeResult==bestResult && f1!=0 && f2==0) { // NT-AA
			int s1, e1;
			if (f1 < 0) 		{s1 = -3; e1 = -1;}
			else if (f1 > 0)	{s1 = 1;  e1 = 3;}
			else 				{s1 = -3; e1 = 3;}
			
			for ( int i = s1; i <= e1; i++ )
			{
				if (i == 0) continue;
				curData = alignAAdoDP (seq1, seq2, i, 0, hitData);	
				if (cmpAlignData(curData, bestData)) 
					bestData = curData;
			}
			alignList.add(bestData);
		}
		else if (typeResult==allResult && f1!=0 && f2==0) { // NT-AA
			for ( int i = 3; i >= -3; --i )
			{
				if (i == 0) continue;
				curData = alignAAdoDP (seq1, seq2, i, 0, hitData);	
				if (curData != null) alignList.add(curData);
			}
		}
		else if (typeResult==bestResult) { // uses megablast pairs orientation
			int s1, e1, s2, e2;
			if (f1 < 0) 		{s1 = -3; e1 = -1;}
			else if (f1 > 0)	{s1 = 1;  e1 = 3;}
			else 				{s1 = -3; e1 = 3;}
			
			if (f2 < 0) 		{s2 = -3; e2 = -1;}
			else if (f2 > 0)	{s2 = 1;  e2 = 3;}
			else 			    {s2 = -3; e2 = 3;}
			
			for ( int i = s1; i <= e1; i++ )
				for ( int j = s2; j <= e2; j++ )
				{
					if (i == 0 || j == 0) continue;
					curData = alignAAdoDP (seq1, seq2, i, j, hitData);	
					if (cmpAlignData(curData, bestData)) 
						bestData = curData;
				}
			alignList.add(bestData);
		}
		else if (typeResult==allResult) {
			for ( int i = 3; i >= -3; i-- )
				for ( int j = 3; j >= -3; j-- )
				{
					if (i==0 || j==0) continue;
					curData = alignAAdoDP (seq1, seq2, i, j, hitData);
					if (curData != null) alignList.add(curData);
				}
		}
		else {
			Out.die("***Internal error: alignAA " + typeResult + " " + f1 + " " + f2);
		}	
	}
		
    /************************************************************************
     * CoreAnno.doHomologyTests: 
     *************************************************************************/
	static public AlignData pairAlignCore ( AlignPairOrig dpAlgo, 
			ContigData ctgData1,  ContigData ctgData2, BlastHitData pairHitData, boolean isAAstcw)
	{	
		if (isAAstcw) 	return pairAlignCoreAA(dpAlgo, ctgData1, ctgData2, pairHitData, pairResult);	
		else 			return pairAlignCoreNT(dpAlgo, ctgData1, ctgData2, pairHitData, bestResult);
	}
	 /************************************************************************
     * AA-sTCW
     *************************************************************************/
	static private AlignData pairAlignCoreAA ( AlignPairOrig dpAlgo, 
			ContigData ctgData1,  ContigData ctgData2, BlastHitData pairHitData, short type)
	{	
		dpAlgoObj = dpAlgo;
        alignList = new Vector<AlignData> ();

		ctgDataI =  ctgData1;
		ctgDataII = ctgData2;
		typeResult = type;
		
		SequenceData aaSeq1 = ctgData1.getSeqData();
		SequenceData aaSeq2 = ctgData2.getSeqData();
		aaSeq1.setIsAA(); // CAS314
		aaSeq2.setIsAA();
		
		AlignData aaObj = alignAAdoDP (aaSeq1, aaSeq2, 0, 0, pairHitData);
		
		return aaObj;
	}
	/************************************************************************
     * NT-sTCW
     * CAS314 - rewrote so DP frame is always ORF frame or pair hit frame.
     * 	too confusing in viewSTCW if its neither - and just means its ambiguous
     *************************************************************************/
	static private AlignData pairAlignCoreNT ( AlignPairOrig dpAlgo, 
			ContigData ctgData1,  ContigData ctgData2, BlastHitData pairHitData, short type)
	{	
		dpAlgoObj = dpAlgo;
        alignList = new Vector<AlignData> ();

		ctgDataI =  ctgData1;
		ctgDataII = ctgData2;
		typeResult = type;
		
		SequenceData ntSeq1 = ctgData1.getSeqData().newSeqDataNoGap();
		SequenceData ntSeq2 = ctgData2.getSeqData().newSeqDataNoGap();
		AlignData ntObj=null, aaObj=null;
		boolean isGood=false;
		
	// The ORF frames may not be the same as the Pair Hit frame; only use HIT if better than ORF  
		int orf_F1=ctgData1.getFrame();
		int orf_F2=ctgData2.getFrame(); 
		
		int hit_F1 = pairHitData.getFrame1(ntSeq1.getLength()); // Pair Hit frame  1/-1 for NT
		int hit_F2 = pairHitData.getFrame2(ntSeq2.getLength());
		
		boolean isSameFr  = (orf_F1==hit_F1 && orf_F2==hit_F2);
		boolean isSameOr  = (isSameOrient(hit_F1, orf_F1) && isSameOrient(hit_F1, orf_F1));
		boolean isAAhit   = pairHitData.isAAhit();
		
		aaObj = alignAAdoDP (ntSeq1, ntSeq2, orf_F1, orf_F2, pairHitData);				// ALIGN ORF frame
		if (isGoodEnough(aaObj)) isGood = true;

	// AA hit with different frames
		if (!isGood && isAAhit && !isSameFr) {
			AlignData fObj = alignAAdoDP (ntSeq1, ntSeq2, hit_F1, hit_F2, pairHitData); // ALIGN AA Hit frame
			if (isGoodEnough(fObj)) {
				isGood = true;
				aaObj=fObj;
			}	
			else if (cmpAlignData(fObj, aaObj)) aaObj = fObj; 
		}
		
	// Align NT in the selected aaObj orientation - if NT hit, then ORF orientation
		ntObj = alignNTdoDP (ntSeq1, ntSeq2, aaObj.getFrame1(), aaObj.getFrame2(), pairHitData); // ALIGN NT
		
	// Return 
		// (1) is GOOD regardless of AA or NT
		// (2) is AA hit - use either ORF or hit
		// (3) is NT hit - same orientation as ORF - use it even if not good
		if (isGood || isAAhit || (!isAAhit && isSameOr)) { 
			ntObj.setAApwData(aaObj); // AA attached to NT AlignData object
			return ntObj;
		}
		
	// NT only hit, ORF frame bad, NT not same orientation as ORF
		cnt6tries++; 
		if (Globalx.debug) {
			String msg = String.format("(this %d %d; sim %3.1f; stops %d; len %d) (ORF %d %d, Hit %d %d) %s %s",   
				aaObj.getFrame1(), aaObj.getFrame2(), aaObj.getOLPsim(), aaObj.getOLPstops(), aaObj.getOLPlen(),
				orf_F1, orf_F2, hit_F1, hit_F2, ctgDataI.getContigID(), ctgDataII.getContigID());
			Out.debug("Try #" + cnt6tries + msg + "                    ");
		}
		for ( int i = 3; i >= -3; i-- ) {
			if (i==0 || !isSameOrient(hit_F1, i)) continue;
			
			for ( int j = 3; j >= -3; j-- ) {
				if (j==0 || !isSameOrient(hit_F2, j)) continue;
				if (i==orf_F1  && j==orf_F2)continue;
				
				AlignData aa = alignAAdoDP (ntSeq1, ntSeq2, i, j, pairHitData);	
				if (cmpAlignData(aa, aaObj)) aaObj = aa;
			}
		}
		if (Globalx.debug) {
			String msg = String.format("(this %d %d; sim %3.1f; stops %d; len %d)",   
					aaObj.getFrame1(), aaObj.getFrame2(), aaObj.getOLPsim(), aaObj.getOLPstops(), aaObj.getOLPlen());
			Out.debug("    #" + cnt6tries + msg + "                    ");
		}
	
		ntObj.setAApwData(aaObj);
		return ntObj;
	}
	public static void setCnt6() {cnt6tries=0;}
	public static int getCnt6() {return cnt6tries;}
	private static int cnt6tries=0;
	private static boolean isSameOrient(int f1, int f2) {
		if (f1>0 && f2>0) return true;
		if (f1<0 && f2<0) return true;
		return false;
	}
	private static boolean isGoodEnough(AlignData aObj) {
		if (aObj==null) return false;
		double dSim = aObj.getOLPsim();
		int nStop = aObj.getOLPstops();
		
		if (dSim>90) return true;
		if (dSim>80 && nStop<50) return true;
		if (dSim>60 && nStop<10) return true;
		if (dSim>50 && nStop<5) return true;
		return false;
	}
    /*************************************************************************
     * PairTopRowTab: display pairwise contigs
     * Get sequence and ORF frame from ctgData
     * Get blast hit values and hit frame from hitData
     *************************************************************************/
	static public Vector<AlignData> pairAlignDisplay ( 
	    		MultiCtgData pairCtgObj, short type ) throws Exception
	{
		try {
	        dpAlgoObj = new AlignPairOrig ();
	        alignList = new Vector<AlignData> ();
	        typeResult = type;
	   
	        ctgDataI = pairCtgObj.getContigAt(0);	
      		SequenceData ntSeq1 = ctgDataI.getSeqData().newSeqDataNoGap();
      				
    	    ctgDataII = pairCtgObj.getContigAt(1);
    	    SequenceData ntSeq2 = ctgDataII.getSeqData().newSeqDataNoGap();
	    			
            BlastHitData hitData = pairCtgObj.getPairHit(); // Blast and CoreAnno values from database
		 	int f1 = hitData.getFrame1();  
 			int f2 = hitData.getFrame2(); 
 		
		 	AlignData alignObj;
            if (type==frameResult) {
        		alignObj = alignNTdoDP (ntSeq1, ntSeq2, f1, f2, hitData);
        		if (alignObj != null) alignList.add(alignObj);
        		
        		alignObj = alignAAdoDP (ntSeq1, ntSeq2, f1, f2, hitData);
        		if (alignObj != null) alignList.add(alignObj);
            }
            else { // all frame
        		AlignData nt1Obj = alignNTdoDP (ntSeq1, ntSeq2, 1, 1, hitData);
        		if (nt1Obj != null)  alignList.add(nt1Obj);
        		AlignData nt2Obj = alignNTdoDP (ntSeq1, ntSeq2, 1, -1, hitData);
        		if (nt2Obj != null)  alignList.add(nt2Obj);
        		
        		int of1=1, of2=1;
        		if (nt1Obj.getOLPsim() < nt2Obj.getOLPsim()) of2=-1;
        		boolean orient = isSameOrient(of1, of2);
				
        		alignObj = alignAAdoDP (ntSeq1, ntSeq2, f1, f2, hitData);
				if (alignObj != null) alignList.add(alignObj);
				
        		for ( int i = 3; i >= -3; i-- )
        			for ( int j = 3; j >= -3; j-- )
        			{
        				if (i==0 || j==0) continue;
        				if (i==f1 && j==f2) continue;
        				if (type==orientResult) {
        					boolean o = isSameOrient(i, j);
        					if (o!=orient) continue;
        				}
        				alignObj = alignAAdoDP (ntSeq1, ntSeq2, i, j, hitData);
        				if (alignObj != null) alignList.add(alignObj);
        			}
            }
	        clear(true);
	        return alignList;
		}
		catch (Exception e) {crash(e, "multiple pairwise align");}
		return null;
	 }
	 /*************************************************************************
     * PairTopRowTab: display pairwise contigs for AA-STCW CAS314
     *************************************************************************/
	static public Vector<AlignData> pairAlignDisplayAA (MultiCtgData pairCtgObj) throws Exception
	{
		try {
	        dpAlgoObj = new AlignPairOrig ();
	        alignList = new Vector<AlignData> ();
	        typeResult = pairResult;
	   
	        ctgDataI = pairCtgObj.getContigAt(0);	
      		SequenceData aaSeq1 = ctgDataI.getSeqData().newSeqDataNoGap();
      		aaSeq1.setIsAA();
      		
    	    ctgDataII = pairCtgObj.getContigAt(1);
    	    SequenceData aaSeq2 = ctgDataII.getSeqData().newSeqDataNoGap();
    	    aaSeq2.setIsAA();
    	    
            BlastHitData hitData = pairCtgObj.getPairHit(); // Blast and CoreAnno values from database
		 	
		 	AlignData alignObj = alignAAdoDP (aaSeq1, aaSeq2, 0, 0, hitData);
        	if (alignObj != null) alignList.add(alignObj);
            
	        clear(true);
	        return alignList;
		}
		catch (Exception e) {crash(e, "multiple pairwise align");}
		return null;
	 }
	/************************************************
	 * 	Align first 3 shared hits to both sequences of the pair
	 */
	static public Vector<AlignData> pairAlignSharedHits (
				MultiCtgData pairListObj, boolean isP) throws Exception
	{
        dpAlgoObj = new AlignPairOrig ();
        alignList = new Vector<AlignData> ();
        isAAsTCW = isP;
        typeResult = frameResult; 
		
        ContigData ctgData1 = pairListObj.getContigAt(0);	
		ArrayList <SequenceData> dbSeqHitList = ctgData1.seqDataHitList();			
		if (dbSeqHitList==null) return alignList;
 
        ContigData ctgData2 = pairListObj.getContigAt(1);		
	    ArrayList <SequenceData> dbSeqList2 = ctgData2.seqDataHitList(); 
		if (dbSeqList2==null) return alignList;
		
		// make list of shared DB hits
		int nHits = 3;	
		ArrayList<String> dbHitSet = new ArrayList<String>();
		
		for (int j = 0; j<dbSeqHitList.size() && dbHitSet.size() < nHits; j++)
		{
            SequenceData dbSeqData1 = dbSeqHitList.get(j);
            String hitname1 = dbSeqData1.getName();
            	
			for (int k = 0; k<dbSeqList2.size(); k++)
			{
	           	SequenceData dbSeqData2 = dbSeqList2.get(k);
	            	String hitname2 = dbSeqData2.getName();
	            	
	            	if (hitname1.equals(hitname2)) {
	            		dbHitSet.add(hitname2);
	            		break;
	            	}
			}
		}
		
		// make alignment list with DB hits grouped      
	    for (String sharedHitName : dbHitSet)
	    {
			for (int j = 0; j<dbSeqHitList.size(); j++)
			{
	            	SequenceData dbSeqData1 = dbSeqHitList.get(j);
	            	String hitname1 = dbSeqData1.getName();
	            	if (sharedHitName.equals(hitname1)) {
	                hitAlign (ctgData1, dbSeqData1);
	                	break;
	            	}
			}
			for (int j = 0; j<dbSeqList2.size(); j++)
			{
            	SequenceData dbSeqData2 = dbSeqList2.get(j);
            	String hitname2 = dbSeqData2.getName();
            	if (sharedHitName.equals(hitname2)) {
            		hitAlign (ctgData2, dbSeqData2);
                	break;
            	}
			}
		}	
	    clear(true);
		return alignList;
	}
	
	 /**********************************************************
	  * XXX shared code
	  */
	  static private AlignData alignNTdoDP (  
			SequenceData seqData1, SequenceData seqData2, 
			int f1, int f2, BlastHitData hitData) 
	  {		
		SequenceData ntSeq1, ntSeq2;
		if ( f1 < 0) ntSeq1 = seqData1.newSeqDataRevComp();
		else ntSeq1 = seqData1.newSeqDataNoQual();
		if ( f2 < 0) ntSeq2 = seqData2.newSeqDataRevComp();
		else ntSeq2 = seqData2.newSeqDataNoQual();
		
		ntSeq1.setSequenceToLower();
		ntSeq2.setSequenceToLower();
		
		// see comment below for same code in alignAAdoDP
		if (!dpAlgoObj.DPalign ( ntSeq1.getSequence(), ntSeq2.getSequence(), isNT )) // XXX
				return null;	
		String seq1 = dpAlgoObj.getHorzResult(Globalx.gapCh); 
		String seq2 = dpAlgoObj.getVertResult(Globalx.gapCh);
	
		AlignData curAlign = new AlignData ( 
				dpAlgoObj, ctgDataI, ctgDataII, 
				ntSeq1, ntSeq2, f1, f2, hitData, isAAsTCW, seq1, seq2);

		return curAlign;
	} 
   	static private AlignData alignAAdoDP ( 
    		SequenceData ntSeq1, SequenceData xxSeq2, int f1, int f2, 
    		BlastHitData hitData)
    {
	    try {
	    	SequenceData aaSeq1, aaSeq2;
	 
			if ( f1 < 0)      aaSeq1 = ntSeq1.newSeqDataRevComp().newSeqDataNTtoAA(Math.abs(f1));
			else if (f1 != 0) aaSeq1 = ntSeq1.newSeqDataNTtoAA(f1);
			else              aaSeq1 = ntSeq1.newSeqData();  // protein sequence
			
			if ( f2 < 0)      aaSeq2 = xxSeq2.newSeqDataRevComp().newSeqDataNTtoAA(Math.abs(f2));
			else if (f2 !=0)  aaSeq2 = xxSeq2.newSeqDataNTtoAA(f2);
			else              aaSeq2 = xxSeq2.newSeqData(); // protein sequence		
	
			if (!dpAlgoObj.DPalign (aaSeq1.getSequence(), aaSeq2.getSequence(), !isNT )) // XXX
						return null;
		
			// Builds the alignment string with trailing and internal gaps
			String seq1 = dpAlgoObj.getHorzResult(Globalx.gapCh); 
			String seq2 = dpAlgoObj.getVertResult(Globalx.gapCh); 
			
			return new AlignData ( 
					dpAlgoObj, ctgDataI, ctgDataII, 
					aaSeq1, aaSeq2, f1, f2, hitData, isAAsTCW, seq1, seq2);
	    }
	    catch (Exception e) {crash(e, "AA DP alignment");}
	    return null;
    }
    // heuristics - true: cur better than best, false: keep best
    static private boolean cmpAlignData(AlignData cur, AlignData best) 
    {
		if (best==null) return true;
		if (cur==null) return false;
		double sim1 = cur.getOLPsim();
		double sim2 = best.getOLPsim();
		if (sim1>sim2) return true;
		if (sim1<sim2) return false;
		if (cur.getOLPstops() > best.getOLPstops()) return true;
		return false;
    } 

    static private void crash(Exception err, String msg) {
   		System.err.println(msg);
   		ErrorReport.reportError(err,msg);
		System.exit(-1);
    }
 // used by all alignment routines for current alignment
 	final static boolean isNT=true;
 	static int typeResult=frameResult; 
 	static AlignPairOrig dpAlgoObj;
 	static ContigData ctgDataI=null, ctgDataII=null; // ctgDataII != null for runSTCW.pairwise
 	static private boolean isAAsTCW = false;
 	
 	// results
 	static Vector<AlignData> alignList = null;	
 	public static int cntNewBest=0; // for core pairwise
 	
     static private void clear(boolean flag) {
     	if (flag) dpAlgoObj.clear();
     	ctgDataI = ctgDataII = null;
     }
}
