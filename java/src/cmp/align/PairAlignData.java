package cmp.align;
// PairAlignData in the cmp.align package
import java.sql.ResultSet;
import java.util.Vector;

import cmp.database.Globals;
import util.align.AlignPairOrig;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;

public class PairAlignData implements Comparable<PairAlignData> {
	public final static int AlignNT=0;
	public final static int AlignAA=1;
	public final static int AlignCDS=2;
	public final static int AlignCDS_AA=3;
	public final static int AlignHIT0_AA=4; // leave at end
	public final static int AlignHIT1_AA=5;
	
	private final char    gapCh=Globals.gapCh; 
	private final String  gapStr=Globals.gapStr;
	private final String  indel = gapStr+gapStr+gapStr;
	
	public PairAlignData () {}
	/*********************************************************
	 * aa or nt sequence: 
	 * viewMulti and runMulti
	 */
	public PairAlignData(DBConn mDB, String [] seqIDs, int type) { 
		run(mDB, seqIDs, type);
	}
	public void run(DBConn mDB, String [] seqIDs, int type) { // CAS310 can be called with new params
		strID1=seqIDs[0];
		strID2=seqIDs[1];
		alignType = type;
		
		switch (type) {
		case AlignAA:
			isCDS=false; isNT=false;	
			loadSequencesFromDB(mDB, seqIDs); // aa sequence
			align(theSeq1, theSeq2);
			break;
		case AlignNT:
			isCDS=false; isNT=true;
			loadSequencesFromDB(mDB, seqIDs); // nt sequence
			align(theSeq1, theSeq2);
			break;
		case AlignCDS:
			isCDS=true; isNT=true;
			loadSequencesFromDB(mDB, seqIDs); // nt sequence
			if (orfStart1==0 || orfEnd1==0) return;
			if (orfStart2==0 || orfEnd2==0) return;
			align(theCDS1, theCDS2);
			repairForCDS(); 
			break;
		case AlignCDS_AA:
			isCDS=true; isNT=false;
			loadSequencesFromDB(mDB, seqIDs); // nt and aa sequence loaded. 
			if (orfStart1==0 || orfEnd1==0) return;
			if (orfStart2==0 || orfEnd2==0) return;
			
			// Obsolete for ESTscan: The TCW CDS=AA*3, but if the AA is from estScan, they can be different lengths
			if (theSeq1.length()*3!=theCDS1.length() || theSeq2.length()*3!=theCDS2.length()) {
				isNT=true;
				align(theCDS1, theCDS2);
				repairForCDS(); 
			}
			else {
				align(theSeq1, theSeq2);
				fit2cds();
				isNT=true;
			}
			removeHangingGap(); // wait until after fit2cds
			break;
		case AlignHIT0_AA:
			isCDS=false; isNT=false;	
			loadHitAndSeqFromDB(mDB, seqIDs, 0); // CAS305  sequence to best hit
			align(theSeq1, theSeq2);
			break;
		case AlignHIT1_AA:
			isCDS=false; isNT=false;	
			loadHitAndSeqFromDB(mDB, seqIDs, 1); // CAS305 sequence to consensus hit
			align(theSeq1, theSeq2);
			break;
		}
	}
	// Align 5' and 3' for PairStats of runMulti
	public void utrAlign(String utr1, String utr2, int type) {
		align(utr1, utr2);
		orfStart1=1;
		orfStart2=1;
		orfEnd1=alignCropSeq1.length();
		orfEnd2=alignCropSeq2.length();
	}
	// Align 5' or 3; for Sequence Detail of viewMultiTCW
	public PairAlignData(String name1, String name2, String utr1, String utr2, boolean isUTR5) {
		int minLen=Share.minAlignLen;
		
		this.strID1 = name1;
		this.strID2 = name2;
		this.theSeq1 = utr1; // needs to access this in alignPairView3
		this.theSeq2 = utr2;
		
		align(utr1, utr2);
		if (nOLPscore == 0) {
			if (utr1.length()>minLen) {
				alignFullSeq1=utr1;
				alignFullSeq2="";  
				for (int i=0; i<utr1.length(); i++) alignFullSeq2+="-";
			}
			else if (utr2.length()>minLen) {
				alignFullSeq2=utr2;
				alignFullSeq1="";
				for (int i=0; i<utr2.length(); i++) alignFullSeq1+="-";
			}
			else {
				alignFullSeq2=alignFullSeq1="---";
				alignCropSeq1=alignCropSeq2="---"; 
				orfEnd1=orfEnd2=3;
			}
		}
		else {
			orfStart1=1;
			orfStart2=1;
			orfEnd1=alignCropSeq1.length();
			orfEnd2=alignCropSeq2.length();
		}
		
		this.isUTR5=isUTR5;
		this.isUTR3=!isUTR5;
	}
	
	/****************************************
	 * runMulti.SumStats - crop realigned sequences
	 */
	
	public void crop(String seq1, String seq2) {
		alignFullSeq1= seq1;
		alignFullSeq2= seq2;
		removeHangingGap();
	}
	/************************************************************/
	private void align(String seq1, String seq2) {
		int minLen=Share.minAlignLen;
		
		if (seq1.length()>minLen && seq2.length()>minLen) {
		
			alignObj.DPalign(seq1, seq2, isNT);
		
			alignFullSeq1 = alignObj.getHorzResult(gapCh);
			alignFullSeq2 = alignObj.getVertResult(gapCh);
			nOLPscore     = alignObj.getOLPmatch();
			
			if (alignType!=AlignCDS_AA) removeHangingGap();
		}
		else {// Either seq1 or seq2 all overhang, which should not be scored
			alignCropSeq1 = alignCropSeq2 = "";
			nOLPscore = 0;
		}
	}
	
	/*****************************************
	 * The AA sequence was aligned. The CDS sequence will be fit to its alignment
	 * alignSeq is AA and will be changed to NT
	 */
	private void fit2cds() {
	try {
		int nLen = theCDS1.length(), aLen=alignFullSeq1.length();
		String cSeq1 = "", cSeq2="";
		
		for (int i=0, j=0; j<aLen; j++) {
			char a1 = alignFullSeq1.charAt(j);
			if (a1==gapCh) cSeq1 +="---";
			else {
				if (i<nLen) { 
					cSeq1 += theCDS1.substring(i, i+3);
					i+=3;
				}
				else Out.PrtError("TCW error with trailing gaps 1-" + nLen);
			}
		}
		
		nLen = theCDS2.length();
		for (int i=0, j=0; j<aLen; j++) {
			char a2 = alignFullSeq2.charAt(j);
			if (a2==gapCh) cSeq2 +="---";
			else {
				if (i<nLen) { 
					cSeq2 += theCDS2.substring(i, i+3);
					i+=3;
				}
				else Out.PrtError("TCW error with trailing gaps 2-" + nLen);
			}
		}
		
		alignCropSeq1 = alignFullSeq1 = cSeq1;
		alignCropSeq2 = alignFullSeq2 = cSeq2;
		if (cSeq1.length()!=cSeq1.length())
			Out.PrtError("Aligned CDS different lengths: " + cSeq1.length() + " " + cSeq1.length() );
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Fitting CDS to AA alignment");}
	}
	/**********************************************
	 * Load sequences
	 */
	private boolean loadSequencesFromDB(DBConn conn, String [] IDs) {
    	ResultSet rs = null;
  
    	try {
	    	String seqField = "aaSeq";
	    	switch (alignType) {
			case AlignAA:     seqField = "aaSeq"; break;
			case AlignNT:     seqField = "ntSeq"; break;
			case AlignCDS:    seqField = "ntSeq"; break;
			case AlignCDS_AA: seqField = "aaSeq, ntSeq"; break;
	    	}
	    	// 1st sequence
    		rs = conn.executeQuery("SELECT orf_start, orf_end, " + seqField +
    					" FROM unitrans WHERE UTstr= '" + IDs[0] + "'");	
    		if (!rs.next()) {
    			Out.PrtWarn("Error reading database for sequence " + IDs[0]);
    			return false;
    		}
    		orfStart1 = rs.getInt(1);
    		orfEnd1 = rs.getInt(2);
    		strORF1 = orfStart1 + ".." +  orfEnd1;
    		theSeq1 = rs.getString(3);
    		
    		if (alignType!=AlignAA) {
    			String tmp = (alignType==AlignCDS_AA) ? rs.getString(4) : theSeq1;
    			theCDS1 =  tmp.substring(orfStart1-1, orfEnd1);
    			the5UTR1 = tmp.substring(0, orfStart1);
    			the3UTR1 = tmp.substring(orfEnd1-3);	
    			if (the5UTR1.length()<=Share.minAlignLen) the5UTR1="";
    			if (the3UTR1.length()<=Share.minAlignLen) the3UTR1="";
    		}
    		
    		// 2nd sequence
	    	rs = conn.executeQuery("SELECT orf_start, orf_end, " + seqField +
					" FROM unitrans WHERE UTstr= '" + IDs[1] + "'");	
    		if (!rs.next()) {
    			Out.PrtWarn("Error reading database for sequence " + IDs[1]);
    			return false;
    		}
    		orfStart2 = rs.getInt(1);
    		orfEnd2 = rs.getInt(2);
    		strORF2 = orfStart2 + ".." +  orfEnd2;
    		
    		theSeq2 = rs.getString(3);
    		if (alignType!=AlignAA) {
    			String tmp = (alignType==AlignCDS_AA) ? rs.getString(4) : theSeq2;
    			theCDS2 =  tmp.substring(orfStart2-1, orfEnd2);
    			the5UTR2 = tmp.substring(0, orfStart2);
    			the3UTR2 = tmp.substring(orfEnd2-3);	
    			if (the5UTR2.length()<=Share.minAlignLen) the5UTR2="";
    			if (the3UTR2.length()<=Share.minAlignLen) the3UTR2="";
    		}
    		
    		rs.close();
	    	return true;
    	}
    	catch(Exception e) {
    		ErrorReport.reportError(e, "Loading data for alignment");
    		return false;
    	}
	}
	private boolean loadHitAndSeqFromDB(DBConn conn, String [] IDs, int type) {
		try {
			// 1st sequence
    		ResultSet rs = conn.executeQuery("SELECT orf_start, orf_end, aaSeq, HITid " +
    					" FROM unitrans WHERE UTstr= '" + IDs[0] + "'");	
    		if (!rs.next()) {
    			rs.close();
    			Out.PrtWarn("Error reading database for sequence " + IDs[0]);
    			return false;
    		}
    		orfStart1 = rs.getInt(1);
    		orfEnd1 = rs.getInt(2);
    		strORF1 = orfStart1 + ".." +  orfEnd1;
    		theSeq1 = rs.getString(3);
    		int hitid = rs.getInt(4);
    		rs.close();
    		
    		boolean isAA=false;
    		// 2nd sequence
    		if (type==0) { // Best hit for sequence
	    		ResultSet rs1 = conn.executeQuery("Select HITstr, sequence, isProtein from unique_hits where HITid=" + hitid);
	    		if (!rs1.next()) {
	    			rs1.close();
	    			Out.PrtWarn("Error reading database for hit sequence " + hitid);
	    			return false;
	    		}
	    		strID2 = rs1.getString(1);
	    		theSeq2 = rs1.getString(2);
	    		isAA = rs1.getBoolean(3);
	    		rs1.close();
    		}
    		else {   // Majority hit
    			ResultSet rs2 =  conn.executeQuery("Select sequence, isProtein from unique_hits where HITstr='" + IDs[1] + "'");
	    		if (!rs2.next()) {
	    			rs2.close();
	    			Out.PrtWarn("Error reading database for hit sequence " + hitid);
	    			return false;
	    		}
	    		strID2 = IDs[1];
	    		theSeq2 = rs2.getString(1);
	    		isAA = rs2.getBoolean(2);
	    		rs2.close();
    		}
    		if (!isAA) { // CAS310 - when best hit is NT
    			Out.prt("Translating NT best hit to AA (assuming ORF)");
	    		theSeq2 = new ScoreAA().nt2aa(theSeq2);
    		}
			return true;
		}
		catch(Exception e) {
    		ErrorReport.reportError(e, "Loading hit and seq for alignment");
    		return false;
    	}
	}
	public int compareTo(PairAlignData obj) {
		return strID2.compareTo(obj.strID2);
		// return -1 * nOLPscore.compareTo(obj.nOLPscore); CAS305 
	}
	public Vector <String> getDescLines() {
		Vector <String> d = new Vector <String> ();
		
		if (isCDS) {
			d.add("CDS");
		}
		else if (isUTR5) {
			d.add("5'UTR");
		}
		else if (isUTR3) {
			d.add("3'UTR");
		}
		else {
			String type = (isNT) ? "NT   " : "AA   ";
			String orf =  (isNT) ?  "     ORF1 " + strORF1 + "  ORF2 " + strORF2 : "";
			d.add(type + orf);
		}
		return d;
	}
	public boolean isGood() {
		if (alignFullSeq1==null || alignFullSeq1.length()<3) return false;
		if (alignFullSeq2==null || alignFullSeq2.length()<3) return false;
		return alignObj.isGood();
	}
	public boolean isNT() { return isNT;}
	public String getSeqID1() {return strID1;}
	public String getSeqID2() {return strID2;}
	public String getSeq1() { return theSeq1; }
	public String getSeq2() { return theSeq2; }
	public String getCDS1() {return theCDS1;}
	public String getCDS2() { return theCDS2;}  
	public String get5UTR1() { return the5UTR1; }
	public String get5UTR2() { return the5UTR2; }
	public String get3UTR1() { return the3UTR1;} 
	public String get3UTR2() { return the3UTR2; }
	public int [] getORFs() { 
		int [] orfs = new int [4];
		orfs[0] = orfStart1; orfs[1]=orfEnd1;
		orfs[2] = orfStart2; orfs[3]=orfEnd2;
		return orfs;
	}
	public String getAlignCropSeq1() { return alignCropSeq1; }
	public String getAlignCropSeq2() { return alignCropSeq2; }
	public String getAlignFullSeq1() { return alignFullSeq1; }
	public String getAlignFullSeq2() { return alignFullSeq2; }
	public int [] getHangEnds() { return noHangEnds;}
	
	/***********************************************************
	 * Combines gaps to increase indels. Only done if CDS.length!=AA.length*3
	 * Done before the hang is removed
	 */
	private void repairForCDS() { 
		Repair rp = new Repair();
		rp.repairForCDS();	
		alignFullSeq1=alignCropSeq1; 
		alignFullSeq2=alignCropSeq2;
	}
	
	/*****************************************************************
	 * Remove leading and trailing gaps. Change ORF coords for text display.
	 * -----aaaaaaaaaaaaaaaa
	 * aaaaaaaaaaaaaaa------
	 */
	private void removeHangingGap() {
		if (alignFullSeq1.length()!=alignFullSeq2.length()) {
			Out.PrtError("TCW error on lengths: " + alignFullSeq1.length() + " " + alignFullSeq2.length());
			return;
		}
		if (alignFullSeq1.length()<=Share.minAlignLen) {
			alignCropSeq1 =alignCropSeq2 ="";
			noHangEnds[0]=noHangEnds[1]=noHangEnds[2]=noHangEnds[3]=0;
			return;
		}
		int len = alignFullSeq1.length(); // both same length because are padded for alignment
		int s1=1, s2=1, start=0;
	    	for (int i=0; i<len; i++) {
	    		if (alignFullSeq1.charAt(i)==gapCh)      s2++;
	    		else if (alignFullSeq2.charAt(i)==gapCh) s1++;
	    		else {
	    			start=i;
	    			break;
	    		}
	    	}
	    	if (s1==len || s2==len) { 
	    		alignCropSeq1=alignCropSeq2="";
	    		noHangEnds[0]=noHangEnds[1]=noHangEnds[2]=noHangEnds[3]=0;
			return;
	    	}
	    	int e1=len, e2=len, end=len;
		for (int i=len-1; i>0; i--) {
    			if (alignFullSeq1.charAt(i)==gapCh)      e1--;
    			else if (alignFullSeq2.charAt(i)==gapCh) e2--;
    			else {
    				end=i+1;
    				break;
    			}
		}
		
		alignCropSeq1 = alignFullSeq1.substring(start, end);
		alignCropSeq2 = alignFullSeq2.substring(start, end);
		
		// Used in PairAlign - see setHang. The above only makes sense for how PairAlign uses the coords.
		noHangEnds[0]=s1; noHangEnds[1]=e1; noHangEnds[2]=s2; noHangEnds[3]=e2;
	}
	/****************************************************
	 *  Modifies alignSeq
	 */
	private class Repair {
		/*********************************
		 * cgc ggc ggc	-> cgc ggc ggc
		 * cg- --- --c  -> cgc --- ---
		 * Works with or without hanging end
		 */
		public int repairForCDS() {
			int swap=0;
			int seqLen = alignCropSeq1.length(); // both same length because are padded for alignment
		    StringBuffer seq1 = new StringBuffer (seqLen);
		    StringBuffer seq2 = new StringBuffer (seqLen);
		    StringBuffer tmp1 = new StringBuffer ();
		    StringBuffer tmp2 = new StringBuffer ();
		    
		    int index=0;
		    while (index<=seqLen-3) {
		    	seq1c1 = alignCropSeq1.substring(index, index+3);
				seq2c1 = alignCropSeq2.substring(index, index+3);
				
				// find codon1 with gaps
				boolean isIndel =  seq1c1.equals(indel) || seq2c1.equals(indel);
				boolean hasGap1 =  seq1c1.contains(gapStr);
				boolean hasGap2 =  seq2c1.contains(gapStr);
				boolean noGap = (!hasGap1 && !hasGap2) || (hasGap1 && hasGap2);
				
				if (isIndel || noGap) {
					seq1.append(seq1c1);
					seq2.append(seq2c1);
					index+=3;
					continue;
				}
				// find codon2 with gaps in same sequence
				for (index+=3; index<=seqLen-3; index+=3) {
					seq1c2 = alignCropSeq1.substring(index, index+3);
					seq2c2 = alignCropSeq2.substring(index, index+3);
					
					if ((hasGap1 && seq1c2.equals(indel)) || (hasGap2 && seq2c2.equals(indel))) {
						tmp1.append(seq1c2);
						tmp2.append(seq2c2);
						continue;
					}
					boolean seq1hasGapIn2ndCodon =  seq1c2.contains(gapStr);
					boolean seq2hasGapIn2ndCodon =  seq2c2.contains(gapStr);
					
					if (hasGap1 && seq1hasGapIn2ndCodon) {
						swap1(); swap++;
					}
					else if (hasGap2 && seq2hasGapIn2ndCodon) {
						swap2(); swap++;
					}
					
					break;
				}
				seq1.append(seq1c1+tmp1.toString()+seq1c2);
				seq2.append(seq2c1+tmp2.toString()+seq2c2);
				
				tmp1.delete(0, tmp1.length());
				tmp2.delete(0, tmp2.length());
				index += 3;
		    }
			alignCropSeq1 = seq1.toString();
			alignCropSeq2 = seq2.toString();
			seq1=seq2=null;
			return swap;
		}
		private void swap1() {
			char [] a = new char[3];
		    char [] b = new char[3];
		    
			a[0] = seq1c1.charAt(0); a[1] = seq1c1.charAt(1); a[2] = seq1c1.charAt(2);
			b[0] = seq1c2.charAt(0); b[1] = seq1c2.charAt(1); b[2] = seq1c2.charAt(2);
			int acnt=0, bcnt=0;
			for (int k=0; k<3; k++) {
				if (a[k]==gapCh) acnt++;
				if (b[k]==gapCh) bcnt++;
			}
			if (acnt==2 && bcnt==1) {          							//cgc att --> cgc att
				seq1c2 = seq1c1.replace(gapStr, "") + seq1c2.replace(gapStr, ""); //c.. .tt     ... ctt
				seq1c1 = indel;
			}
			else if (acnt==1 && bcnt==2) {	   							//cgc att --> cgc att
				seq1c1 = seq1c1.replace(gapStr, "") + seq1c2.replace(gapStr, "");	//cg. ..t     cgt ...
				seq1c2 = indel;
			}
		}
		public void swap2() {
			char [] a = new char[3];
		    char [] b = new char[3];
		    
			a[0] = seq2c1.charAt(0); a[1] = seq2c1.charAt(1); a[2] = seq2c1.charAt(2);
			b[0] = seq2c2.charAt(0); b[1] = seq2c2.charAt(1); b[2] = seq2c2.charAt(2);
			
			int acnt=0, bcnt=0;
			for (int k=0; k<3; k++) {
				if (a[k]==gapCh) acnt++;
				if (b[k]==gapCh) bcnt++;
			}
			if (acnt==2 && bcnt==1) {          							//cgc att --> cgc att
				seq2c2 = seq2c1.replace(gapStr, "") + seq2c2.replace(gapStr, ""); //c.. .tt     ... ctt
				seq2c1 = indel;
			}
			else if (acnt==1 && bcnt==2) {	   							//cgc att --> cgc att
				seq2c1 = seq2c1.replace(gapStr, "") + seq2c2.replace(gapStr, "");	//cg. ..t     cgt ...
				seq2c2 = indel;
			}
		}
		String seq1c1="", seq1c2="", seq2c1="", seq2c2="";
		
	}
	public void clear() {
		alignCropSeq1 = alignCropSeq2="";
		alignFullSeq1 = alignFullSeq2="";
		orfStart1=orfStart2=orfEnd1=orfEnd2=0;
		isNT=true; isCDS=false; isUTR5=false; isUTR3=false;
		strID1 =  strID2 = "";
		strORF1 = strORF2 = ""; 
		theCDS1 = theCDS2 = ""; 
		the5UTR1 = the5UTR2 = ""; 
		the3UTR1 = the3UTR2 = "";
	}
	
	private int alignType=0;
	private boolean isNT=true, isCDS=false, isUTR5=false, isUTR3=false;
	private String strID1 = "", strID2 = "";
	private String strORF1 = "", strORF2 = ""; // coords
	private String theCDS1 = "", theCDS2 = ""; 
	private String the5UTR1 = "", the5UTR2 = ""; 
	private String the3UTR1 = "", the3UTR2 = ""; 
	private int    orfStart1=0, orfStart2=0, orfEnd1=0, orfEnd2=0;
	
	private AlignPairOrig alignObj =  new AlignPairOrig(); // CAS310 reuse
	
	// these three apply to whatever was aligned last
	private String theSeq1 = "", theSeq2 = ""; // the aligned seq
	private String alignCropSeq1 = "", alignCropSeq2=""; // no Hang
	private String alignFullSeq1 = "", alignFullSeq2=""; // has Hang
	
	private int [] noHangEnds = {-1, -1, -1, -1}; // after removing hanging gap;  start1, end1, start2, end2
    
    private Integer nOLPscore = -1; 
}
