package sng.dataholders;

/*******************************************
 * Both runSingle and viewSingle use this class
 * DoBlast and DoUniProt create objects from files
 * 
 * Unitrans to DBhit database 
Only Unitrans coords are ever reversed (>< 604706  <>0  >>0  <<760342 for PaRpi to TRpla)
Unitran coords are relative to NT
cari_mix_08  sp|P31414|AVP1_ARATH    92.00   125  10  0  667   293     645   769     3e-63    231
cari_mix_12  sp|P07728|GLUA1_ORYSJ   100.00  129   0  0  34    420     1     129     2e-73    265
cari_mix_12  sp|Q6VAK4|HP1_ORYSJ     100.00  78    0  0  471   704     72    149     3e-42    161
cari_mix_13  sp|P07728|GLUA1_ORYSJ   100.00  126   0  0  29    406     1     126     1e-71    259
cari_mix_13  sp|P07728|GLUA1_ORYSJ   100.00  111   0  0  477   809     389   499     3e-61    224

megablast selfblast
Only the second coords ever seem to be reversed (>< 0  <>127054  >>0  <<271070 from PaRpi)
cari_mix_12     cari_mix_13     100.00  413     0  0  7      419     2       414     0.0 819
cari_mix_13     cari_mix_12     100.00  414     0  0  2      415     7       420     0.0 821
PaRpi_000011    PaRpi_007798    94.29   631     33 3  3246   3874    3554    2925    0.0 961

tblastx selfblast
Every combination seems to occur (>< 499619  <>495498  >>916132  <<941384 from PaRpi)
cari_mix_12     cari_mix_13     100.00  138     0  0  7     420   2       415     4e-95    335
cari_mix_12     cari_mix_13     99.28   138     1  0  419   6     414     1       2e-94    332
cari_mix_12     cari_mix_13     100.00  87      0  0  420   160   415     155     2e-77    273
cari_mix_12     cari_mix_13     100.00  33      0  0  105   7     100     2       2e-77    273
cari_mix_12     cari_mix_13     100.00  113     0  0  346   8     341     3       1e-76    273
cari_mix_12     cari_mix_13     100.00  93      0  0  9     287   4       282     2e-76    251
cari_mix_12     cari_mix_13     100.00  31      0  0  327   419   322     414     2e-76    251
cari_mix_12     cari_mix_13     100.00  137     0  0  8     418   3       413     6e-70    251
 ********************************************/
import java.lang.Comparable;
import java.io.*;

import sng.annotator.LineParser;
import util.methods.ErrorReport;


public class BlastHitData implements Serializable, Comparable <BlastHitData> 
{
	public static final String badHits = "hitsWarnings.log"; 
	public static final short BLASTFILE = 0;
	public static final short DB_UNITRANS = 1;
	public static final short DB_PAIRWISE = 2;
	private static int parseWarnings = 0;
	private static int prtWarn=0;
	private static String prtHead=null;

	static public void startHitWarnings(String msg) { // CAS311 only start file if a warning is ever printed
		prtHead=msg;
		prtWarn=0;
	}
	static public void printWarning(String msg) {
		try{
			BufferedWriter out=null;
			
			if (prtWarn==0) {
				out = new BufferedWriter(new FileWriter(badHits,false));
				out.write(prtHead + "\n");
			}
			else out = new BufferedWriter(new FileWriter(badHits,true));
			
			out.write(msg + "\n");
			out.close();
			prtWarn++;
		}
		catch (Exception e){ErrorReport.prtReport(e, "Print warning about blast hit");}
	}
	
	public boolean badHitData(int maxCoord, int minBitScore) {
		boolean rc=false;
		String err="";
		
		if (hitStart > maxCoord || hitEnd > maxCoord || ctgStart > maxCoord || ctgEnd > maxCoord) {
			err = "Database entry: coordinates exceed limit of " + maxCoord + " -- ignoring " +
			 "\n   " + contigID + " " + hitID + " " + ctgStart + " " + ctgEnd + " " + hitStart + " " + hitEnd;
			rc = true;
		}
		// XXX Heuristic for blast files; 32-bit on 64-bit machine causes this error
		// also, multiple HSPs can cause a evalue of 0.0
		if (eVal==0.0 && bitScore < minBitScore) {
			err = contigID + " " + hitID + " E-value=" + eVal + " alignLen=" + alignLen + " %sim=" + simPercent + " bitscore " + bitScore;
			rc = true;
		}
		if (rc == false) return false;
		
		try{
			  FileWriter fstream = new FileWriter(badHits,true);
			  BufferedWriter out = new BufferedWriter(fstream);
			  out.write(err + "\n");
			  out.close();
		}catch (Exception e){
			  ErrorReport.prtReport(e, "Print ignored hit");
		}
		return true;		  
	}
	
	public BlastHitData(String name, boolean isProtein, int start, int end, String msg) {
		descForAlign = msg;
		contigID = name;
		hitIsProtein = isProtein;
		ctgStart = start;
		ctgEnd = end;
	}
	
	// read blast file
	public BlastHitData (boolean isProtein, String line) {
		String[] tokens = line.split("\t");
		if (tokens.length < 12) {
			ErrorReport.reportError("Hit line: " + line);
			contigID = hitID = ""; simPercent=0.0; alignLen=0; 
			return;
		}
		contigID = tokens[0];	
		simPercent = Double.parseDouble(tokens[2]);
		alignLen = Integer.parseInt(tokens[3]);
		mismatches = Integer.parseInt(tokens[4]);
		gapOpen = Integer.parseInt(tokens[5]);
		ctgStart = Integer.parseInt(tokens[6]);
		ctgEnd = Integer.parseInt(tokens[7]);
		hitStart = Integer.parseInt(tokens[8]);
		hitEnd = Integer.parseInt(tokens[9]);
		eVal = Double.parseDouble(tokens[10]);
		bitScore = Float.parseFloat(tokens[11]);	
						 
		LineParser lp = new LineParser();
		if (lp.parseLine(tokens[1])) {
			hitID = lp.getHitID();
			dbType = lp.getDBtype();
		}
		else 
		{
			if (parseWarnings < 20)
				ErrorReport.reportError("Cannot parse: "  + line);
			parseWarnings++;
			if (parseWarnings == 20)
				ErrorReport.reportError("No further warnings shown");
			hitID = null;
			return;
		}
		// this length problem is reported in DoUniProt
		if (hitID.length() > 30) hitID = hitID.substring(0,30);
		hitIsProtein = isProtein;	
		if (ctgEnd<=0) ctgEnd+=3; 	
		if (ctgStart > ctgEnd) ctg_orient = -1;		
	}
	// called from database routine: pairsTable.LoadPairFromDB, seqDetail.LoadFromDB
	public BlastHitData (int type, String line) {			
		String[] tokens = line.split("\t");
	
		if (type == DB_PAIRWISE) { 
			contigID = tokens[0];
			hitID = tokens[1];
			if (tokens.length < 4) {
				ErrorReport.reportError("Bad line: " + line);
				return;
			}
			eVal = Double.parseDouble(tokens[2]);
			simPercent = Double.parseDouble(tokens[3]);
			alignLen = Integer.parseInt(tokens[4]);
			
			ctgStart = Integer.parseInt(tokens[5]);
			ctgEnd = Integer.parseInt(tokens[6]);
			hitStart = Integer.parseInt(tokens[7]);
			hitEnd = Integer.parseInt(tokens[8]);
			nFrame1 = Integer.parseInt(tokens[9]);
			nFrame2 = Integer.parseInt(tokens[10]);
	
			//sharedHit = tokens[11];
			int b = Integer.parseInt(tokens[12]);
			int x = Integer.parseInt(tokens[13]);
			if (b==1) isSelf=true;
			if (x==1) isTself = true;
			hitIsProtein = false;	
			if (ctgStart > ctgEnd) ctg_orient = -1;
			return;
		}	
		
		if (type == DB_UNITRANS) { 
			// from pja_db_unitrans
			contigID = tokens[0];
			hitID = tokens[1];
			simPercent = Double.parseDouble(tokens[2]);
			alignLen = Integer.parseInt(tokens[3]);
			ctgStart = Integer.parseInt(tokens[4]);
			ctgEnd = Integer.parseInt(tokens[5]);
			hitStart = Integer.parseInt(tokens[6]);
			hitEnd = Integer.parseInt(tokens[7]);
			eVal = Double.parseDouble(tokens[8]);
			bitScore = Float.parseFloat(tokens[9]);
			
			int x = Integer.parseInt(tokens[11]);
			if (x==1) hitIsProtein = true; 
			blastRank = Integer.parseInt(tokens[12]);
			filtered = Integer.parseInt(tokens[13]);
			
			// from pja_db_unique
			dbType = tokens[14];
			dbTaxo = tokens[15];
			
			rank = Integer.parseInt(tokens[18]); // filter_best+filter_ovbest+filter_gobest
			if (ctgStart > ctgEnd) ctg_orient = -1;
			return;
		}	
	}
	
	// sorting blast hits for pairwise comparison
	public int compareTo(BlastHitData b) {
		if (this.eVal < b.eVal) return -1;
		if (this.eVal == b.eVal && this.bitScore > b.bitScore) return -1;
		if (this.eVal == b.eVal && this.bitScore == b.bitScore) return 0;
		return 1;
	}

	/**
	 *  MainPairAlignData/AlignmentData to be displayed in alignment header
	 */
	public String getHitBlast(boolean isNT) {
		if (isNT && isTself) return "";   // hit from tblastx
		if (!isNT && !isTself) return ""; // hit from megablast
		String e = String.format("%.1E", eVal);
		
		return "Hit: " + e + "; Sim " + (int) (simPercent+0.5)  +  "% ; Align " + alignLen;
	}
	// for Seq-hit pair
	public String getHitBlast() { 
		return descForAlign;
	}
	
	public String getHitBlast(int len1, int len2) { 
		String e = String.format("%.1E", eVal);
		String x = " " + e + " " + (int) (simPercent+0.5)  +  "% AL: " + alignLen;
		x += "  ( " + ctgStart + " " + ctgEnd +  " " + len1 + " )";
		x += "  ( " + hitStart + " " + hitEnd +  " " + len2  + " )";
		return x;
	}
	
	// annotator.DoUniProt
    public void setPID ( int p ) {  /*PID = p;*/ }  // never used
    public void setCTGID (int p) {CTGID = p;}
   
    // Blast
    public double getBitScore() {return bitScore;}      
    public double getPercentID() { return simPercent;}   
    public int getAlignLen() { return alignLen;}
    public double getEVal() {return eVal;}
    public String getStrEVal() {
    		String s = Double.toString(eVal);
    		int i = s.indexOf("E");
    		if (i==-1) return "0";
    		return s.substring(i);
    }
    
    // Contig
    public String getContigID() { return contigID;}  
    public int getCTGID() { return CTGID;} 
    public int getCtgStart( ) { return ctgStart; }    
    public int getCtgEnd ( ) { return ctgEnd; }   
    public int getCtgOrient ( ) { return ctg_orient; }
    public int getMisMatches() { return mismatches;}
    public int getGapOpen() { return gapOpen;}
    
	// DB hit or ctg2 hit
    public boolean hitIsProtein () { return hitIsProtein;}
    public String getHitID ( ) { return hitID; }
    public int getHitStart() { return hitStart; }
    public int getHitEnd() { return hitEnd; }   
    
    // computed values
    public void setBlastRank ( int n ) { blastRank = n; }
    public int getBlastRank ( ) { return blastRank; }  
    public int getRank ( ) { return rank; }  
    public void setIsProtein(boolean b) { hitIsProtein = b;}
	public boolean isProtein ( ) { return hitIsProtein; }	

	// specific for DB hits 
    public String getDBtype() { return dbType;}
    public String getDBtaxo() { return dbTaxo;}
    public void setDBtaxo(String s) { dbTaxo = s;} // DoUniProt
	public String getDBtypeTaxo() {
		if (dbType=="" || dbTaxo=="") return "";
		String capType = dbType.toUpperCase();
		if (dbTaxo.length() < 4) return capType + dbTaxo;
		return capType + dbTaxo.substring(0, 3);
	}
	
	public int getFiltered() { return filtered;}
	public void setFiltered(int x) { filtered = x;}
	
	public int getFrame1() { return nFrame1;}
	public int getFrame2() { return nFrame2;}
	
	// pairwise: nucleotide coordinates even though tblastx was used
	public int getFrame1(int ntlen) {
		return calcFrame(ntlen, ctgStart, ctgEnd);
	}
	public int getFrame2(int ntlen) {
		return calcFrame(ntlen, hitStart, hitEnd);
	}
	private int calcFrame(int len, int start, int end) {
		int frame=0, orient=1;
		
		if (end<=start) {
			start = len - start + 1;
			orient = -1;
		}
		frame = start % 3;
		if (frame==0) frame = 3;
		if (orient<0) frame = -frame;
		return frame;
	}
	public void setIsHit (boolean b) {isDB = b;}
	public void setIsSelf (boolean b) {isSelf = b;};
	public void setIsTself (boolean b) {isTself = b;}
	public void setSharedHitID(String s) { sharedHitID = s;}
	public boolean getIsHit () {return isDB;}
	public boolean getIsSelf () {return isSelf;};
	public boolean getIsTself () {return isTself;}	
	public boolean getIsShared () {return (sharedHitID!="");}	
	public String getSharedHitID() { return sharedHitID;}
		
	public void clear()
	{
		contigID=hitID=dbType=sharedHitID="";
		simPercent=eVal=0.0;
		alignLen=mismatches=gapOpen=ctgStart=ctgEnd=hitStart=hitEnd=0;
		blastRank=rank=ctg_orient=filtered=0;
		isDB=isSelf=isTself=false;	
	}
	 
	// from database or computed
	private int nFrame1=0;
	private int nFrame2=0;
	
    //// variable names correspond to pja_ table fields	
    private int CTGID;
    
    ///////  Blast fields
	private String contigID;
    public String hitID = "";
	private double simPercent; // stored in database as integer in field percent_id
	private int alignLen;
	private int mismatches;
	private int gapOpen;
	private int ctgStart;	// coordinates start at 1
	private int ctgEnd;
	private int hitStart;
	private int hitEnd;
	private float bitScore;
	private double eVal;
	
	// not from blast file
	private int blastRank = 0;
	private int rank=0;
	private int ctg_orient = 1;
	private boolean hitIsProtein = false;
	private String descForAlign="";
	
	// specific for DB hits
	private String dbType = ""; 		// blast file or sequence type
	private String dbTaxo = ""; 		// from PAVE parameter	
	private int filtered=0;			// computed in DoUniProt

	// pairwise alignments
	private boolean isDB=false, isSelf=false, isTself=false;
	private String sharedHitID = "";
	private static final long serialVersionUID = 1;
}
