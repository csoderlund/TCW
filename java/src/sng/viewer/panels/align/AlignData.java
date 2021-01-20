/**
 * Class AlignData : holds the the data for two aligned sequences; 
 */
package sng.viewer.panels.align;

import java.lang.Math;
import java.util.Vector;

import sng.database.Globals;
import sng.dataholders.BlastHitData;
import sng.dataholders.CodingRegion;
import sng.dataholders.ContigData;
import sng.dataholders.SequenceData;
import util.align.AAStatistics;
import util.align.AlignPairOrig;
import util.methods.Out;

public class AlignData 
{
	// Called from AlignCompute
	public AlignData ( AlignPairOrig dpAlgo,
				ContigData ctg1, ContigData ctg2,  
				SequenceData sd1, SequenceData sd2,
				int frame1, int frame2, BlastHitData hit,
				boolean isAAdb, 
				String s1, String s2)
	{
	    seqData1 = sd1;	// has gaps but not leading or trailing gapCh			
	    seqData2 = sd2; 
		nLength1 = seqData1.getLength();  
		nLength2 = seqData2.getLength();  
		strName1 = seqData1.getName();
		strName2 = seqData2.getName();	
		
		nFrame1 = frame1;	
 		nFrame2 = frame2;
 		alignSeq1 = 	s1;   // has leading/trailing and internal gaps
 		alignSeq2 = 	s2;
 		
 		blastData = hit;	 // blast results
 		isNTsTCW = !isAAdb;	 // used in setSeqInfo
			
		setDPinfo(dpAlgo);
		setSeqInfo(ctg1, ctg2);  // ctg2 is null if Seq-hit align
		
		isHit     = (orfData2==null); // even if NT hit, no ORF
		isNTalign = (seqData1.isNT() && seqData2.isNT()) ? true : false;
	 }
/************************************************************************
 * For the popup on Sequence Hits display (MainToolAlignPanel)
 * This shows the original -- already computed -- alignment in the popup text window.
 */
	 public void computeMatch() {
		char gapCh = '-';
		String gapStr = "-";
		int len = alignSeq1.length(); // both same length because are padded for alignment
		int s1=1, e1=len, s2=1, e2=len;
		boolean hasStart=false, isGap=false, doSeq1=false, doSeq2=false;
		StringBuffer sb = new StringBuffer (len);
			
		// find last non-gap match from end. Doing this check fixes problem with
		// ------xxxxxxx
		// xxxxxx-xxxxx 
		if (alignSeq1.endsWith(gapStr)) doSeq2=true;
		if (alignSeq2.endsWith(gapStr)) doSeq1=true;
		if (doSeq1 && doSeq2) Out.bug("Can't happen: " + alignSeq1.substring(len-5,len) + " " + alignSeq2.substring(len-5,len));
		for (int i=len-1; i>0; i--) {
    			if (doSeq1 && alignSeq2.charAt(i)==gapCh) e1--;
    			else if (doSeq2 && alignSeq1.charAt(i)==gapCh) e2--;
    			else {nEndAlign=i+1; break;}
		}
		if (doSeq1) e1++;
		if (doSeq2) e2++;
		
		// find first non-gap match from beginning
		doSeq1=doSeq2=false;
		if (alignSeq1.startsWith(gapStr)) doSeq2=true;
		if (alignSeq2.startsWith(gapStr)) doSeq1=true;
		if (doSeq1 && doSeq2) Out.bug("Can't happen: " + alignSeq1.substring(0,5) + " " + alignSeq2.substring(0,5));

		int open=-10, extend=-1;
		
    	for (int i=0; i<len; i++) {
    		char c1 = alignSeq1.charAt(i);
    		char c2 = alignSeq2.charAt(i);
    		if (!hasStart) { // executes until first non-gap match
    			if (c1==gapCh || c2==gapCh) {
    				sb.append(" ");
    				continue;
    			}
    			else {
    				hasStart=true; 
    				if (doSeq1) s1=i+1;
    				if (doSeq2) s2=i+1;
    				nStartAlign=i;
    			}
    		}
			int s=0;
    		if (c1==gapCh || c2==gapCh) {
    			sb.append(" ");
    			if (isGap) s=open;
    			else {s=extend; isGap=true;}
    		}
    		else {
    			String m=" ";
    			if (isNTalign) { // CAS313 add
    				if (c1==c2) m=c1+"";
    			}
    			else {
    				m = AAStatistics.getSubChar(c1,  c2);
    			}
    			sb.append(m);
    			isGap=false;
    			if (i<nEndAlign) score += s;
    		}
    	}
    	matchSeq = sb.toString(); 
    	ends[0]=s1; ends[1]=e1; ends[2]=s2; ends[3]=e2;
	}
	public String getOrigSeq1() {return removeGaps(alignSeq1);}
	public String getOrigSeq2() {return removeGaps(alignSeq2);}
	private String removeGaps(String gstr) {
		StringBuffer sb = new StringBuffer ();
		for (int i=0; i<gstr.length(); i++) {
			char c = gstr.charAt(i);
			if (c!=Globals.gapCh) sb.append(c);
		}
		return sb.toString();
	}
	
	public String getAlignSeq1() 	{ return alignSeq1;}
	public String getAlignSeq2() 	{ return alignSeq2;}
	public String getMatcheSeq() 	{ return matchSeq;}
	public int getStartAlign() 		{ return nStartAlign;}
	public int getEndAlign() 		{ return nEndAlign;}
	public int getScore() 			{ return score;}
	public int [] getEnds() 		{ return ends;}
	
	/*****************************************************************/
	 private void setDPinfo (AlignPairOrig dpAlgo){		
		nLen = 		dpAlgo.getOLPlen(); 
		nMatch = 	dpAlgo.getOLPmatch();
		nStops = 	dpAlgo.getOLPstops();
		score = 	dpAlgo.getOLPscore();
		if (nLen != 0) dSim = ((double) nMatch/ (double) nLen) * 100.0;
	 }
	 
	 // All work gets done in PairAlignPanel
	 private void setSeqInfo (ContigData ctg1, ContigData ctg2) {
	    ctgData1 = ctg1; 	
	    strName1 = ctgData1.getContigID();
	    if (isNTsTCW) { 
	    	orfData1 = ctgData1.getORFCoding(); 
		    nORF1 =    ctg1.getBestCodingFrame ();
	    }
		
		if (ctg2 != null) { // pairwise
			ctgData2 = ctg2;	
			strName2 = ctgData2.getContigID();
			if (isNTsTCW) { 
				orfData2 = ctgData2.getORFCoding();
				nORF2 =    ctg2.getBestCodingFrame ();
			}
		}
		else {              // DB hit        
		    ctgData2 = null;
		    strName2 = seqData2.getName();
		}
	 }
	
	 /******************** Methods for drawing *************************
	 * the following is called by PairAlignPanel.init for display 
	 * ctgData1==null => AAdb
	 * ctgData2==null => Seq->Hit (If Hit==Nt, no way to know orientation)
	 */
	 public Vector<String> getDescription (int num )
	 { 
		Vector<String> lines = new Vector<String> ();    

		String s = "";
		if (num != 0) s = num + ". ";
		if (isNTalign) s+= "DP: NT "; else s += "DP: AA "; 
	
		s += String.format("Sim %3.1f%s ", dSim, "%"); // "Sim " + new DisplayFloat(dSim) + "% ";
		if (!isNTalign) s += ", #Stops " + nStops; 

		if (ctgData2 == null) { // seq to hit (where either can be AA or NT)
			if (isNTsTCW) {
				if (isNTalign) 							s += "    " + blastData.getHitBlast(); // NT hit
				else if (nFrame1==orfData1.getFrame()) 	s += "    " + blastData.getHitBlast(); // AA hit 
			}
			else 										s += "    " + blastData.getHitBlast(); // AAsTCW
    	}
		else { 					// pairwise
			if (isNTalign) 								s += "    " + blastData.getHitBlast(isNTalign); 
			else if (nFrame1==blastData.getFrame1() && nFrame2==blastData.getFrame2()) //  pairwise AA
													    s += "     " + blastData.getHitBlast(isNTalign);
		}
	    lines.add(s);
	    return lines;
	}
	
    /****  Sequence 1 attributes  *******/
	public CodingRegion getORFCoding1 ( ) 	{ return orfData1; }
	public SequenceData getSeqData1 ( ) 	{ return seqData1; } // seq1 is sequence to be drawn
	public String getName1 ( ) 				{ return strName1; }	
	public String getDisplayStr1 ( ) {
		return getFrameForDisplay(strName1, nFrame1, nORF1, isNTalign);
	}	
	/****  Sequence 2 attributes ****/
	public CodingRegion getORFCoding2 ( ) 	{ return orfData2; }
	public SequenceData getSeqData2 ( ) 	{ return seqData2; }; // seq2 to be drawn
	public String getName2 ( ) 				{ return strName2; }	 	
	public String getDisplayStr2 ( ) { 
		return getFrameForDisplay(strName2, nFrame2, nORF2, isNTalign);
	};

	private String getFrameForDisplay(String name, int frame, int orf, boolean isNtAlign){
		if ( name == null) return "";
		
		String str = name;
		if (frame == 0 && orf == 0) { // Protein sequence
							str += "         ";
		}
		else if ( !isNtAlign ) { // Translated sequence
			if (frame < 0) 	str+= " (RF " + frame + ")";
			else 			str+= " (RF  " + frame + ")"; // extra blank
		}
		else {
			if (frame < 0) 	str+= " (R)"; // CAS314
			else str += " (F)";
		}
		return str;
	}
	
	/********** Computed values  **********/
	// Core only
	public void setAApwData(AlignData aa) 	{bestAAalignData = aa;}
	public AlignData getAApwData ( ) 		{return bestAAalignData;}
		
	public double getOLPratio()  
	{ 
		if (nLen == 0) return 0;
		int nLength = Math.max(nLength1, nLength2);		
		return (double) nLen/nLength; 
	} 		
	public int getOLPlen ( ) 	{ return nLen;}
	public int getOLPmatch ( ) 	{ return nMatch;}
	
	// core and view
	public double getOLPsim ( ) {return dSim;}
	public int getOLPstops ( ) 	{ return nStops;}
	
	public BlastHitData getHitData() 		{return blastData;}
	
	public int getFrame1 () { 
		if (bestAAalignData!= null) return bestAAalignData.nFrame1; // core only
		else return nFrame1;
	}
	public int getFrame2 () { 
		if (bestAAalignData!= null) return bestAAalignData.nFrame2; // core only
		else return nFrame2;
	}
	
	// view
	public boolean isNTalign()  {return isNTalign;} // could be NT hit or Seq Pair
	public boolean isNTsTCW() 	{return isNTsTCW;}
	
	public void releaseSequences ( ) {
    	seqData1 = seqData2 = null;	
    	ctgData1 = ctgData2 = null;
    	orfData1 = orfData2 = null;
    	alignSeq1=alignSeq2=matchSeq=null;
    	
    	if ( bestAAalignData != null )
        	bestAAalignData.releaseSequences();
	}

	/**************** Instance variables ***********************/
	// computed
	private String alignSeq1=""; // padded with leading '-'
	private String alignSeq2="";
	private String matchSeq="";
	private int nStartAlign = 0, nEndAlign=0, score=0;
	private int [] ends = new int [4];
	
	// other
	private boolean isNTsTCW = false;
	private boolean isHit = false;
	private boolean isNTalign = false;
	
	private SequenceData seqData1 = null;
	private String strName1 = "";
	private int nLength1 = 0; 
	
	private SequenceData seqData2 = null;
	private String strName2 = "";
	private int nLength2 = 0; 
	
	private ContigData ctgData1 = null;
	private CodingRegion orfData1 = null;
	private int nORF1 = 0;

	private ContigData ctgData2 = null;
	private CodingRegion orfData2 = null;
	private int nORF2 = 0;
	
	private AlignData bestAAalignData = null; // Used by CoreAnno; AA DP within NT DP object
	
	// The results are aligned in these frames. Could be ORF, Pair Hit frame, or 1/-1 for NT align
	private int nFrame1 = 0, nFrame2 = 0;

	private BlastHitData blastData = null;
 
    private int nMatch = 0;  // this goes into database as 'score', which is different from AlignPairOrig.score
    private int nLen = 0;  
    private int nStops = 0;
    private double dSim = 0;
}
