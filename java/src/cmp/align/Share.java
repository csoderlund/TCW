package cmp.align;

/*******************************************
 * Compress and uncompress are used for both pairwise and MSA alignments
 */
import util.database.Globalx;
import util.methods.ErrorReport;
import cmp.database.Globals;

public class Share {
	public static final int minAlignLen=3; // some 3'UTR are only the stop; the sequence will be treated as 0 length
	public static final String noNTStr = "n";
	public static final char noAACh = 'X';	
	public static final String stopStr="*";
	public static final String DELIM="::";
	
// Ways to view CDS  && NT in PairText
	public static final int CDS_MATCH=0;
	public static final int CDS_TV=1;
	public static final int CDS_CpG=2;
	public static final int CDS_AA=3;
	public static final int CDS_ND=4;
	
	// TS/TV per positions - for display
	public static final String TS = "s";
	public static final String TV = "v";
	
	// CpG per position for display
	public static final String CpG12 = "|";
	public static final String CpG1  = "x";
	public static final String CpG2  = "x";
		
	// ND per codon - for display
	public static final String NND= "*"; // not same AA
	public static final String N4D ="4"; // four-fold
	public static final String N2D ="2"; // two-fold
	public static final String NxD ="x"; // none of the above
	
	// CDS per position - for Match display
	public static final String CDS_SYM="|"; // diff codon, same AA
	public static final String CDS_NONSYM ="*"; // diff codon, diff AA
			
	// CDS per position - for Amino Acid display
	public static final String AA_POS = Globalx.AA_POS; // blosum>0
	public static final String AA_NEG =  Globalx.AA_NEG; // blosum<=0
	
	// NT per postion
	public static final String NT_MM = "|"; 
	
	public static final String gapStr = Globals.gapStr;
	public static final char   gapCh = Globals.gapCh;

	public static String compress(String aSeq) {
		try {
			if (aSeq=="") return "";
			if (aSeq.endsWith(stopStr)) aSeq = aSeq.substring(0, aSeq.length()-1);
			
			String gapMap="";
			char [] base = aSeq.toCharArray();
			int cntGap=0;
			for (int i=0; i<base.length; i++) {
				if (base[i]=='-') {
					if (cntGap==0) {
						cntGap=1;
						gapMap += ":" + i;
					}
					else cntGap++;
				}
				else if (cntGap>0) {
					gapMap += "-" + cntGap;
					cntGap=0;
				}
			}
			if (cntGap>0) gapMap+= "-" + cntGap; // gap at end
			
			if (gapMap!="") gapMap = gapMap.substring(1);        // remove first ':'
			else gapMap="0";
			
			return gapMap;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "create gap map "); return "";}
	}
	 // recreates alignSeq from gapMap and original sequence 
	public static String uncompress(String gapMap, String seq) {
		try {
			if (gapMap.equals("0") || gapMap.equals("")) return seq;
			if (seq.endsWith(stopStr)) seq = seq.substring(0, seq.length()-1);
			
			String [] tok = gapMap.split(":");
			int [] gapStart = new int [tok.length];
			int [] gapNum = new int [tok.length];
			for (int i=0; i<tok.length; i++) {
				String [] x = tok[i].split("-");
				gapStart[i] = Integer.parseInt(x[0]);
				gapNum[i] = Integer.parseInt(x[1]);
			}	
			StringBuffer tmp = new StringBuffer ();
			char [] seqChar = seq.toCharArray();
			
			int seqX=0;	// index into seqChar
			int gapX=0;	// index into gapStart and gapNum
			int loc=0;	// location of gapped string, i.e. sequence plus gaps
			
			for (; seqX <seqChar.length; seqX++, loc++) {
				
				if (gapX<gapStart.length && gapStart[gapX]==loc) {
					for (int j=0; j<gapNum[gapX]; j++, loc++) tmp.append("-");
					gapX++;
				}
				tmp.append(seqChar[seqX]);
			}
			
			if (gapX<gapStart.length) {
				for (int j=0; j<gapNum[gapX]; j++) tmp.append("-");
			}
			return tmp.toString();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "creating realign sequences"); return "";}
	}
}
