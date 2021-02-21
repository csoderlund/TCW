package sng.dataholders;

/*******************************************
 * Both runSingle and viewSingle use this class
 * DoBlast and DoUniProt create objects from files
 * Query	Subject					%ID		Len	MM	Gap	Qs	Qe	Ss	Se
 * bar_001	gi|1041540644|ref|XM_	87.160	514	64	2	301	813	639	127	9.66e-166	582   // NT-NT Ss<Se
 * bar_001	sp|Q963B6|RL10A_SPOFR	87.6	217	27	0	813	163	1	217	1.3e-106	380.6 // NT-AA Qs>Qe
 * pBar_001	sp|Q963B6|RL10A_SPOFR	87.6	217	27	0	1	217	1	217	1.5e-106	379.8 // AA-AA 
 * bar_001	bar_217					60.938	64	25	0	741	550	125	316	3.97e-27	101	  // NT-NT
 * 1. Can't flip a AA sequence, so only the NT may have Start<End
 * 2. Len is in AA coords for NT-AA, though the Start-End are in NT coords for NT sequence.
 * 
 * Start-End in SeqTable and SeqDetail are exactly like in Hit File
 * Coords in PairAlign are the Hit Coords from file
 ********************************************/
import java.lang.Comparable;
import java.io.*;

import sng.annotator.LineParser;
import util.database.Globalx;
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
		
		if (hitStart > maxCoord || hitEnd > maxCoord || seqStart > maxCoord || seqEnd > maxCoord) {
			err = "Database entry: coordinates exceed limit of " + maxCoord + " -- ignoring " +
			 "\n   " + contigID + " " + hitID + " " + seqStart + " " + seqEnd + " " + hitStart + " " + hitEnd;
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
		}catch (Exception e){ErrorReport.prtReport(e, "Print ignored hit");}
		return true;		  
	}
	// View: LoadFromDB.loadBlastHitData for Basic AnnoDB
	public BlastHitData(String name, boolean isProtein, int start, int end, String msg) {
		descForAlign = msg;
		contigID = name;
		isAAhit = isProtein;
		seqStart = start;
		seqEnd = end;
	}
	
	// Annotator: read blast file for both DoUniprot and CoreAnno via DoBlast to load file
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
		seqStart = Integer.parseInt(tokens[6]);
		seqEnd = Integer.parseInt(tokens[7]);
		hitStart = Integer.parseInt(tokens[8]);
		hitEnd = Integer.parseInt(tokens[9]);
		eVal = Double.parseDouble(tokens[10]);
		bitScore = Float.parseFloat(tokens[11]);	
						 
		LineParser lp = new LineParser();
		if (lp.parseLine(tokens[1])) {
			hitID = lp.getHitID();
			dbType = lp.getDBtype();
		}
		else {
			if (parseWarnings < 20) ErrorReport.reportError("Cannot parse: "  + line);
			parseWarnings++;
			if (parseWarnings == 20) ErrorReport.reportError("No further warnings shown");
			hitID = null;
			return;
		}
		// this length problem is reported in DoUniProt
		if (hitID.length() > 30) hitID = hitID.substring(0,30);
		isAAhit = isProtein;	
		if (seqEnd<=0) seqEnd+=3; 	
		if (seqStart > seqEnd) ctg_orient = -1;		
	}
	// View: called from database routine: pairsTable.LoadPairFromDB, seqDetail.LoadFromDB
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
			
			seqStart = Integer.parseInt(tokens[5]);
			seqEnd = Integer.parseInt(tokens[6]);
			hitStart = Integer.parseInt(tokens[7]);
			hitEnd = Integer.parseInt(tokens[8]);
			nFrame1 = Integer.parseInt(tokens[9]);
			nFrame2 = Integer.parseInt(tokens[10]);
	
			sharedHitID = tokens[11];
			pairHitType = (tokens.length>=13) ? tokens[12] : "Unk"; // Types: AA, NT, ORF 
			if (pairHitType.startsWith(Globalx.typeAA) || pairHitType.startsWith(Globalx.typeORF)) isAAhit=true;	
			
			if (seqStart > seqEnd) ctg_orient = -1;
			return;
		}	
		
		if (type == DB_UNITRANS) { 
			// from pja_db_unitrans
			contigID = tokens[0];
			hitID = tokens[1];
			simPercent = Double.parseDouble(tokens[2]);
			alignLen = Integer.parseInt(tokens[3]);
			seqStart = Integer.parseInt(tokens[4]);
			seqEnd = Integer.parseInt(tokens[5]);
			hitStart = Integer.parseInt(tokens[6]);
			hitEnd = Integer.parseInt(tokens[7]);
			eVal = Double.parseDouble(tokens[8]);
			bitScore = Float.parseFloat(tokens[9]);
			
			int x = Integer.parseInt(tokens[11]);
			if (x==1) isAAhit = true; 
			blastRank = Integer.parseInt(tokens[12]);
			filtered = Integer.parseInt(tokens[13]);
			
			// from pja_db_unique
			dbType = tokens[14];
			dbTaxo = tokens[15];
			
			rank = Integer.parseInt(tokens[18]); // filter_best+filter_ovbest+filter_gobest
			if (seqStart > seqEnd) ctg_orient = -1;
			return;
		}	
	}
	/******************************************************************************/
	// XXX sorting blast hits for pairwise comparison  CAS314 sort on bitscore only
	// Called in CoreAnno on pairs
	// CAS317 called on DoUniProt for adding sequence hitList for an annoDB
	public int compareTo(BlastHitData b) {
		if (this.bitScore > b.bitScore) return -1;
		if (this.bitScore < b.bitScore) return 1;
		
		if (this.eVal < b.eVal) return -1; // CAS317 
		if (this.eVal > b.eVal) return  1;
		
		return 0;
	}

	/**
	 * GetDescription for AlignData
	 *  1. for Similar pairs  - formatted in 3 different places
	 */
	public String getHitBlast(boolean isNT) {
		if (isNT && isAAhit) return "";   // hit from tblastx
		if (!isNT && !isAAhit) return ""; // hit from AA->
		String e = String.format("%.1E", eVal);
		if (eVal==0.0) e = "0.0";
		
		return String.format("Pair Hit: %s, %d%s, Align %d", e, (int) (simPercent+0.5), "%", alignLen);
	}
	/**********************************
	 *  2. Constructed in: 
	 *    Seq-Hit: seqDetail.SeqDetailPanel.getHitMsg  for Sequence  Detail
	 *    Seq-Hit: seqDetail.LoadFromDB.loadBlastHitData for Basic AnnoDB
	*****/
	public String getHitBlast() { 
		return descForAlign;
	}
	public String getHitBlast(int len1, int len2) { 
		String e = String.format("%.1E", eVal);
		if (eVal==0.0) e = "0.0";
		String x = " " + e + " " + (int) (simPercent+0.5)  +  "% AL: " + alignLen;
		x += "  ( " + seqStart + " " + seqEnd +  " " + len1 + " )";
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
    public int getCtgStart( ) { return seqStart; }    
    public int getCtgEnd ( ) { return seqEnd; }   
    public int getCtgOrient ( ) { return ctg_orient; }
    public int getMisMatches() { return mismatches;}
    public int getGapOpen() { return gapOpen;}
    
	// DB hit or ctg2 hit
    public boolean isAAhit () { return isAAhit;}
    public String  getHitID ( ) { return hitID; }
    public int getHitStart() { return hitStart; }
    public int getHitEnd() { return hitEnd; }   
    
    // computed values
    public void setBlastRank ( int n ) { blastRank = n; }
    public int getBlastRank ( ) { return blastRank; }  
    public int getRank ( ) { return rank; }  

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
	
	// pairwise: nucleotide coordinates
	public int getFrame1(int ntlen) {
		return calcFrame(ntlen, seqStart, seqEnd);
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
		if (isAAhit) {
			frame = start % 3;
			if (frame==0) frame = 3;
			if (orient<0) frame = -frame;
		}
		else { // CAS314
			frame = (orient<0) ? -1 : 1;
		}
		return frame;
	}
	
	public void setSharedHitID(String s) { sharedHitID = s;}
	
	public void setPairHitType(String type) { // typeAA, typeORF, typeNT
		if (pairHitType.equals("")) pairHitType = type;
		else if (!pairHitType.contains(type)) pairHitType += "::" + type;
	}
	
	public boolean getIsShared () {return (sharedHitID!="");}	
	public String getSharedHitID() { return sharedHitID;} // annoDB hits
	public String getPairHitType() {return pairHitType;} // pair hits
		
	public void clear(){
		contigID=hitID=dbType=sharedHitID="";
		simPercent=eVal=0.0;
		alignLen=mismatches=gapOpen=seqStart=seqEnd=hitStart=hitEnd=0;
		blastRank=rank=ctg_orient=filtered=0;
		pairHitType = sharedHitID = "";	
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
	private int mismatches, gapOpen;
	private int seqStart, seqEnd;	// coordinates start at 1
	private int hitStart, hitEnd;
	private float bitScore;
	private double eVal;
	
	// not from blast file
	private int blastRank = 0;
	private int rank=0;
	private int ctg_orient = 1;
	private boolean isAAhit = false;
	private String descForAlign="";
	
	// specific for DB hits
	private String dbType = ""; 		// blast file or sequence type
	private String dbTaxo = ""; 		// from TCW parameter	
	private int filtered=0;				// computed in DoUniProt

	// pairwise alignments
	private String sharedHitID = "", pairHitType="";
	private static final long serialVersionUID = 1;
}
