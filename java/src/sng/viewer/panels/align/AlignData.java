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
import sng.viewer.STCWMain;
import util.align.AAStatistics;
import util.align.AlignPairOrig;
import util.methods.Out;
import util.ui.DisplayFloat;

public class AlignData 
{
	private static boolean test = STCWMain.test;
	
	// Called from AlignCompute
	public AlignData ( AlignPairOrig dpAlgo,
				ContigData ctg1, ContigData ctg2,  
				SequenceData sd1, SequenceData sd2,
				int nFrame1, int nFrame2, BlastHitData hit,
				boolean isAAdb, 
				String s1, String s2)
	{
	    seqData1 = sd1;	// has gaps but not leading or trailing gapCh			
	    seqData2 = sd2; 
		nLength1 = seqData1.getLength();  
		nLength2 = seqData2.getLength();  
		strName1 = seqData1.getName();
		strName2 = seqData2.getName();	
		
		nAAFrame1 = nFrame1;	
 		nAAFrame2 = nFrame2;
 		alignSeq1 = 	s1;   // has leading/trailing and internal gaps
 		alignSeq2 = 	s2;
 		
 		blastData = hit;	 // blast results
 		isNTsTCW = !isAAdb;	 // used in setSeqInfo
			
		setLowHighIndex();
		setDPinfo(dpAlgo);
		setSeqInfo(ctg1, ctg2);  // ctg2 is null if Seq-hit align
		
		isNTseq  = seqData1.isDNA(); // else, its is translated ORF; 
		isHit    = (orfData2==null); // even if NT hit, no ORF
		isNTalign = (seqData1.isDNA() && seqData2.isDNA()) ? true : false;
		
		if (test) Out.debug("NTsTCW:" + isNTsTCW + " NTseq:" + isNTseq + " isHit:" + isHit 
				+ " NTalign:" + isNTalign + " ORF:" + (orfData1!=null) + (orfData2!=null));
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
		if (doSeq1 && doSeq2) System.out.println("Can't happen: " + alignSeq1.substring(len-5,len) + " " + alignSeq2.substring(len-5,len));
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
		if (doSeq1 && doSeq2) System.out.println("Can't happen: " + 
				alignSeq1.substring(0,5) + " " + alignSeq2.substring(0,5));

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
	
	private String alignSeq1=""; // padded with leading '-'
	private String alignSeq2="";
	private String matchSeq="";
	private int nStartAlign = 0, nEndAlign=0, score=0;
	private int [] ends = new int [4];
	
	/*****************************************************************/
	 private void setDPinfo (AlignPairOrig dpAlgo){		
		nLen = 		dpAlgo.getOLPlen(); 
		nMatch = 	dpAlgo.getOLPmatch();
		nStops = 	dpAlgo.getOLPstops();
		score = 	dpAlgo.getOLPscore();
		if (nLen != 0) dSim = ((double) nMatch/ (double) nLen) * 100.0;
	 }
	 
	 // All work gets done in MainAlignPanel
	 private void setSeqInfo (ContigData ctg1, ContigData ctg2) {
	    ctgData1 = ctg1; 	
	    strName1 = ctgData1.getContigID();
	    if (isNTsTCW) { 
	    	orfData1 = ctgData1.getORFCoding(); 
		    nORF1 = ctg1.getBestCodingFrame ();
	    }
		
		if (ctg2 != null) { // pairwise
			ctgData2 = ctg2;	
			strName2 = ctgData2.getContigID();
			if (isNTsTCW) { 
				orfData2 = ctgData2.getORFCoding();
				nORF2 = ctg2.getBestCodingFrame ();
			}
		}
		else {              // DB hit        
		    ctgData2 = null;
		    strName2 = seqData2.getName();
		}
	 }
	 // this sets the coordinates of the alignment
	private void setLowHighIndex() {
		// note different in max and mix for graphics vs sequence
		int low1=  seqData1.getLowIndex();
		int high1= seqData1.getHighIndex();
		int low2=  seqData2.getLowIndex();
		int high2= seqData2.getHighIndex();

		seqLowIndex =    Math.max (low1, low2 ); 
		graphLowIndex =  Math.min (low1, low2 ); 
		
		seqHighIndex =   Math.min (high1, high2 ); 
		graphHighIndex = Math.max (high1, high2 ); 

		// just a small overhang so longest is obvious for sequence view
		int lowest = Math.min (low1, low2 );
		for (int i = 1; i <= Globals.OVERHANG && seqLowIndex > lowest; i++) seqLowIndex--;
	
		int highest = Math.max (high1, high2 ); 
		for (int i = 1; i <= Globals.OVERHANG && seqHighIndex < highest; i++) seqHighIndex++;
	}
	 /******************** Methods for drawing *************************
	 * the following is called by PairAlignPanel.setBasesPerPixel for display 
	 */
	 public Vector<String> getDescription (int num )
	 { 
		Vector<String> lines = new Vector<String> ();    

		String s = "";
		if (num != 0) s = num + ". ";
		if (isNTalign) s+= "DP: NT "; else s += "DP: AA "; 
	
		s += "Sim " + new DisplayFloat(dSim) + "% ";
		if (!isNTalign) s += ", #Stops " + nStops; 
				
    	if (ctgData2 == null) s += "    " + blastData.getHitBlast(); // Seq-hit (from SeqDetailPanel)
    	else if (isNTalign)   s += "    " + blastData.getHitBlast(isNTalign); // Seq-Seq
    	else if (nAAFrame1==blastData.getFrame1() && nAAFrame2==blastData.getFrame2())
    			              s += "     " + blastData.getHitBlast(isNTalign);

	    lines.add(s);
	    return lines;
	}
	
    /****  Sequence 1 attributes  *******/
	public CodingRegion getORFCoding1 ( ) 	{ return orfData1; }
	public SequenceData getSequence1 ( ) 	{ return seqData1; } // seq1 is sequence to be drawn
	public String getName1 ( ) 				{ return strName1; }	
	public String getDisplayStr1 ( ) {
		return getFrameForDisplay(strName1, nAAFrame1, nORF1, isNTalign);
	}	
	/****  Sequence 2 attributes ****/
	public CodingRegion getORFCoding2 ( ) 	{ return orfData2; }
	public SequenceData getSequence2 ( ) 	{ return seqData2; }; // seq2 to be drawn
	public String getName2 ( ) 				{ return strName2; }	 	
	public String getDisplayStr2 ( ) { 
		return getFrameForDisplay(strName2, nAAFrame2, nORF2, isNTalign);
	};

	private String getFrameForDisplay(String name, int frame, int orf, boolean isNtAlign){
		if ( name == null) return "";
		
		String str = name;
		if (frame == 0 && orf == 0) { // Protein sequence
			str += "         ";
		}
		else if ( !isNtAlign ) { // Translated sequence
			if (frame < 0) str+= " (RF " + frame + ")";
			else str+= " (RF  " + frame + ")"; // extra blank
		}
		return str;
	}
	
	// Called from PairAlignPanel to determine the ruler
	public int getLowIndex (boolean drawSeq ) { 
		if (drawSeq) {
			/* CAS313
			int ix = seqLowIndex; 
			if (orfData1!=null && orfData1.getBegin() < ix) ix = orfData1.getBegin();
			if (orfData2!=null && orfData2.getBegin() < ix) ix = orfData2.getBegin();
			*/
			return seqLowIndex;
		}
		return graphLowIndex;
	}
	public int getHighIndex ( boolean drawSeq) { 
		if (drawSeq) {
			/* CAS313 orfData is in NT coords
			int ix = seqHighIndex; 
			if (orfData1!=null && orfData1.getEnd() > ix) ix = orfData1.getEnd();
			if (orfData2!=null && orfData2.getEnd() > ix) ix = orfData2.getEnd();
			*/
			return seqHighIndex;
		}
		return graphHighIndex;
	}    	
	/********** Computed values  **********/
	public double getOLPratio()  
	{ 
		if (nLen == 0) return 0;
		int nLength = Math.max(nLength1, nLength2);		
		return (double) nLen/nLength; 
	} 		
	public double getOLPsim ( ) {return dSim;}
	
	public int getOLPlen ( ) 	{ return nLen;}
	public int getOLPmatch ( ) 	{ return nMatch;}
	public int getOLPstops ( ) 	{ return nStops;}
	
	public int getLength1 ( ) 	{ return nLength1;}
	public int getLength2 ( ) 	{ return nLength2;}
	
	public void setAApwData(AlignData aa) 	{bestAAalignData = aa;}
	public boolean hasAAalignment() 		{return (bestAAalignData != null);} 
	public AlignData getAApwData ( ) 		{return bestAAalignData;}
	public BlastHitData getHitData() 		{return blastData;}
	
	public int getFrame1 () { 
		if (bestAAalignData!= null) return bestAAalignData.nAAFrame1;
		else return nAAFrame1;
	}
	public int getFrame2 () { 
		if (bestAAalignData!= null) return bestAAalignData.nAAFrame2;
		else return nAAFrame2;
	}
	public boolean isSeqPair()  {return !isHit;}    // orfCoding2==null
	public boolean isNTalign()  {return isNTalign;} // could be NT hit or Seq Pair
	public boolean isNTsTCW() 	{return isNTsTCW;}
	
	public void releaseSequences ( ) {
    	seqData1 = null;
    	seqData2 = null;	
    	ctgData1 = null;
    	orfData1 = null;
    	ctgData2 = null;
    	orfData2 = null;
    	alignSeq1=alignSeq2=matchSeq=null;
    	
    	if ( bestAAalignData != null )
        	bestAAalignData.releaseSequences();
	}

	/**************** Instance variables ***********************/
	
	private boolean isNTsTCW = false;
	private boolean isNTseq = false;
	private boolean isHit = false;
	private boolean isNTalign = false;
	
	private int graphLowIndex = 0;
	private int graphHighIndex = 0;
	private int seqLowIndex = 0;
	private int seqHighIndex = 0;
    	
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
	
	private AlignData bestAAalignData = null;
	private int nAAFrame1 = 0;
	private int nAAFrame2 = 0;

	private BlastHitData blastData = null;
 
    private int nMatch = 0;  // this goes into database as 'score', which is different from AlignPairOrig.score
    private int nLen = 0;  
    private int nStops = 0;
    private double dSim = 0;
}
