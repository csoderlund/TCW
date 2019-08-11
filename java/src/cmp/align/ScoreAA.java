package cmp.align;

import java.util.Vector;

import util.align.AlignPairOrig;
import util.database.Globalx;
import util.methods.Out;

public class ScoreAA {
	private final char noAACh = Share.noAACh;	
	private final String noNTStr = Share.noNTStr;
	
	public String aaMatch(String alignSeq1, String alignSeq2) {
		alignLen = alignSeq1.length();
		StringBuffer sb = new StringBuffer (alignSeq1.length());
		char [] s1 = alignSeq1.toCharArray();
		char [] s2 = alignSeq2.toCharArray();
   		boolean inGap=false;
		
		for (int i=0; i<alignLen; i++) {
			if (s1[i]==Share.gapCh || s2[i]==Share.gapCh) {
				if (!inGap) {
					cntOpen++; 
					inGap=true;
				} 
				else cntGap++;
				
				sb.append(" ");
			}
			else {
				inGap=false;
				cntNoGap++;
				
				if (s1[i]==s2[i]) {
		   			sb.append(s1[i]);
		   			cntExact++;
		   		}
		   		else if (isHighSub(s1[i], s2[i])) {
		   			sb.append(Share.AA_POS);
		   			cntPos++;
		   		}
		   		else {
		   			sb.append(Share.AA_NEG);
		   			cntNeg++;
		   		}
			}
		}	
	   	return sb.toString();
	}
	public void addHeader(Vector <String> lines, String [] hangCol) {
		String r1c1 = String.format("%s %3s",  hangCol[0],"");
		String r2c1 = String.format("%s %3s",  hangCol[1],"");
		String r3c1 = String.format("%s %3s",  hangCol[2],"");
		
		String r1c2 = String.format("%-10s %4d %4s", "AA align:", cntNoGap, "");
		String r2c2 = String.format("%-10s %4d %4s", "GapOpen:", cntOpen, "");
		String r3c2 = String.format("%-10s %4d %4s", "Gaps: ", (cntGap+cntOpen), "");
		
		
		String r1c3 = String.format("%-16s %4d %s", "Exact:", cntExact, Out.perFtxtP(cntExact, cntNoGap));
		String r2c3 = String.format("%-16s %4d %s", Globalx.blosumPos + ":", cntPos, Out.perFtxtP(cntPos, cntNoGap));
		String r3c3 = String.format("%-16s %4d %s", Globalx.blosumNeg + ":", cntNeg, Out.perFtxtP(cntNeg, cntNoGap));
		
		lines.add(r1c1+r1c2+r1c3);
		lines.add(r2c1+r2c2+r2c3);
		lines.add(r3c1+r3c2+r3c3);
		lines.add("");
	}
	public void legendAA(Vector <String> lines) {
		lines.add("LEGEND: ");
		lines.add("  AA char = AA match");
		lines.add("        " + Share.AA_POS +  " = " + Globalx.blosumPosLegend);
		lines.add("        " + Share.AA_NEG +  " = " + Globalx.blosumNeg);
	}
	/*******************************************************
	 * Conversion routines
	 * Much of this is also in util.align.AAstatistics
	 */
	public char codonToAA(String codon) {
		char c=noAACh;
		if (codon.contains(Share.gapStr)) return c;
		if (codon.contains(noNTStr)) return c;
		
		if (codon.equals("ttt") || codon.equals("ttc")) return AMINO_Phe; 
		
		if (codon.equals("tta") || codon.equals("ttg")) return AMINO_Leu; 
		if (codon.equals("ctt") || codon.equals("ctc")) return AMINO_Leu; 
		if (codon.equals("cta") || codon.equals("ctg")) return AMINO_Leu;
		
		if (codon.equals("att") || codon.equals("atc")) return AMINO_Ile; 
		if (codon.equals("ata")) 					   return AMINO_Ile;
		
		if (codon.equals("atg"))                        return AMINO_Met;
		
		if (codon.equals("gtt") || codon.equals("gtc")) return AMINO_Val; 
		if (codon.equals("gta") || codon.equals("gtg")) return AMINO_Val;
		
		if (codon.equals("tct") || codon.equals("tcc")) return AMINO_Ser; 
		if (codon.equals("tca") || codon.equals("tcg")) return AMINO_Ser;
		
		if (codon.equals("cct") || codon.equals("ccc")) return AMINO_Pro; 
		if (codon.equals("cca") || codon.equals("ccg")) return AMINO_Pro;
		
		if (codon.equals("act") || codon.equals("acc")) return AMINO_Thr; 
		if (codon.equals("aca") || codon.equals("acg")) return AMINO_Thr;
		
		if (codon.equals("gct") || codon.equals("gcc")) return AMINO_Ala; 
		if (codon.equals("gca") || codon.equals("gcg")) return AMINO_Ala;
		
		if (codon.equals("tat") || codon.equals("tac")) return AMINO_Tyr; 
		
		if (codon.equals("taa") || codon.equals("tag")) return AMINO_Stop;
		
		if (codon.equals("cat") || codon.equals("cac")) return AMINO_His; 
		
		if (codon.equals("caa") || codon.equals("cag")) return AMINO_Gln; 
		
		if (codon.equals("aat") || codon.equals("aac")) return AMINO_Asn; 
		
		if (codon.equals("aaa") || codon.equals("aag")) return AMINO_Lys; 
		
		if (codon.equals("gat") || codon.equals("gac")) return AMINO_Asp; 
		
		if (codon.equals("gaa") || codon.equals("gag")) return AMINO_Glu; 
		
		if (codon.equals("tgt") || codon.equals("tgc")) return AMINO_Cys; 
		if (codon.equals("tga")) 					   return AMINO_Stop;
		
		if (codon.equals("tgg"))                        return AMINO_Trp;
		
		if (codon.equals("cgt") || codon.equals("cgc")) return AMINO_Arg; 
		if (codon.equals("cga") || codon.equals("cgg")) return AMINO_Arg;
		
		if (codon.equals("agt") || codon.equals("agc")) return AMINO_Ser; 
		
		if (codon.equals("aga") || codon.equals("agg")) return AMINO_Arg; 
		
		if (codon.equals("ggt") || codon.equals("ggc")) return AMINO_Gly; 
		if (codon.equals("gga") || codon.equals("ggg")) return AMINO_Gly;
		
		System.out.println("Warning: Codon not recognized " + codon);
		return c;
	}

	/**************************************************************
	 * Called from everything that aligns in cmp
	 * ISSUB
	 */
	public static String getSubChar(char a1, char a2) {
		if (a1==a2) return a1+"";
		if (a1==Globalx.gapCh || a2==Globalx.gapCh) return " ";
		
		int score = getBlosum(a1, a2);
		if (score>0) return "+";
		else return " ";
	}
	public boolean isHighSub(char a1, char a2) {
		if (a1==Share.gapCh) return false;
		if (a2==Share.gapCh) return false;
	
		int idx1 = residues.indexOf(a1);
		int idx2 = residues.indexOf(a2);
		if (idx1==-1 || idx2==-1) {
			if (idx1==-1) System.out.println("Warning: AA not recognized a1 " + a1 + " ");
			if (idx2==-1) System.out.println("Warning: AA not recognized a2 " + a2 + " ");
			return false;
		}
		if (blosum[idx1][idx2]>0) return true; // as blast does
		else return false;
	}
	
	static public int getBlosum(char a1, char a2) {
		int idx1 = residues.indexOf(a1);
		int idx2 = residues.indexOf(a2);
		if (idx1==-1 || idx2==-1) {
			if (idx1==-1) System.out.println("Warning: AA not recognized a1 " + a1 + " ");
			if (idx2==-1) System.out.println("Warning: AA not recognized a2 " + a2 + " ");
			return -10;
		}
		return blosum[idx1][idx2];
	}
	public String getAbbr(char aa) {
		if (aa=='F') return "Phe";
		if (aa=='I') return "Ile";
		if (aa=='M') return "Met";
		if (aa=='L') return "Leu";
		if (aa=='V') return "Val";
		if (aa=='P') return "Pro";
		if (aa=='S') return "Ser";
		if (aa=='T') return "Thr";
		if (aa=='A') return "Ala";
		if (aa=='Y') return "Tyr";
		if (aa=='*') return "Stop";
		if (aa=='Q') return "Gln";
		if (aa=='K') return "Lys";
		if (aa=='E') return "Glu";
		if (aa=='W') return "Trp";
		if (aa=='G') return "Gly";
		if (aa=='H') return "His";
		if (aa=='N') return "Asn";
		if (aa=='D') return "Asp";
		if (aa=='C') return "Cys";
		if (aa=='R') return "Arg";
		return "???";
	}
	// BLOSUM62 7/17/19 from ftp://ftp.ncbi.nlm.nih.gov/blast/matrices/BLOSUM62
	static String residues = "ARNDCQEGHILKMFPSTWYVBZX*";
	public static final int blosum[][] = new int [][] {
			{ 4, -1, -2, -2,  0, -1, -1,  0, -2, -1, -1, -1, -1, -2, -1,  1,  0, -3, -2,  0, -2, -1,  0, -4},
			{-1,  5,  0, -2, -3,  1,  0, -2,  0, -3, -2,  2, -1, -3, -2, -1, -1, -3, -2, -3, -1,  0, -1, -4},
			{-2,  0,  6,  1, -3,  0,  0,  0,  1, -3, -3,  0, -2, -3, -2,  1,  0, -4, -2, -3,  3,  0, -1, -4},
			{-2, -2,  1,  6, -3,  0,  2, -1, -1, -3, -4, -1, -3, -3, -1,  0, -1, -4, -3, -3,  4,  1, -1, -4},
			{ 0, -3, -3, -3,  9, -3, -4, -3, -3, -1, -1, -3, -1, -2, -3, -1, -1, -2, -2, -1, -3, -3, -2, -4},
			{-1,  1,  0,  0, -3,  5,  2, -2,  0, -3, -2,  1,  0, -3, -1,  0, -1, -2, -1, -2,  0,  3, -1, -4},
			{-1,  0,  0,  2, -4,  2,  5, -2,  0, -3, -3,  1, -2, -3, -1,  0, -1, -3, -2, -2,  1,  4, -1, -4},
			{ 0, -2,  0, -1, -3, -2, -2,  6, -2, -4, -4, -2, -3, -3, -2,  0, -2, -2, -3, -3, -1, -2, -1, -4},
			{-2,  0,  1, -1, -3,  0,  0, -2,  8, -3, -3, -1, -2, -1, -2, -1, -2, -2,  2, -3,  0,  0, -1, -4},
			{-1, -3, -3, -3, -1, -3, -3, -4, -3,  4,  2, -3,  1,  0, -3, -2, -1, -3, -1,  3, -3, -3, -1, -4},
			{-1, -2, -3, -4, -1, -2, -3, -4, -3,  2,  4, -2,  2,  0, -3, -2, -1, -2, -1,  1, -4, -3, -1, -4},
			{-1,  2,  0, -1, -3,  1,  1, -2, -1, -3, -2,  5, -1, -3, -1,  0, -1, -3, -2, -2,  0,  1, -1, -4},
			{-1, -1, -2, -3, -1,  0, -2, -3, -2,  1,  2, -1,  5,  0, -2, -1, -1, -1, -1,  1, -3, -1, -1, -4},
			{-2, -3, -3, -3, -2, -3, -3, -3, -1,  0,  0, -3,  0,  6, -4, -2, -2,  1,  3, -1, -3, -3, -1, -4},
			{-1, -2, -2, -1, -3, -1, -1, -2, -2, -3, -3, -1, -2, -4,  7, -1, -1, -4, -3, -2, -2, -1, -2, -4},
			{ 1, -1,  1,  0, -1,  0,  0,  0, -1, -2, -2,  0, -1, -2, -1,  4,  1, -3, -2, -2,  0,  0,  0, -4},
			{ 0, -1,  0, -1, -1, -1, -1, -2, -2, -1, -1, -1, -1, -2, -1,  1,  5, -2, -2,  0, -1, -1,  0, -4},
			{-3, -3, -4, -4, -2, -2, -3, -2, -2, -3, -2, -3, -1,  1, -4, -3, -2, 11,  2, -3, -4, -3, -2, -4},
			{-2, -2, -2, -3, -2, -1, -2, -3,  2, -1, -1, -2, -1,  3, -3, -2, -2,  2,  7, -1, -3, -2, -1, -4},
			{ 0, -3, -3, -3, -1, -2, -2, -3, -3,  3,  1, -2,  1, -1, -2, -2,  0, -3, -1,  4, -3, -2, -1, -4},
			{-2, -1,  3,  4, -3,  0,  1, -1,  0, -3, -4,  0, -3, -3, -2,  0, -1, -4, -3, -3,  4,  1, -1, -4},
			{-1,  0,  0,  1, -3,  3,  4, -2,  0, -3, -3,  1, -1, -3, -1,  0, -1, -3, -2, -2,  1,  4, -1, -4},
			{ 0, -1, -1, -1, -2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2,  0,  0, -2, -1, -1, -1, -1, -1, -4},
			{-4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4,  1}
	};
	// Constants for the letters representing amino acids
	private final char AMINO_Phe = 'F';
	private final char AMINO_Ile = 'I';
	private final char AMINO_Met = 'M';
	private final char AMINO_Leu = 'L';
	private final char AMINO_Val = 'V';
	private final char AMINO_Pro = 'P';
	private final char AMINO_Ser = 'S';
	private final char AMINO_Thr = 'T';
	private final char AMINO_Ala = 'A';
	private final char AMINO_Tyr = 'Y';
	private final char AMINO_Stop = '*';
	private final char AMINO_Gln = 'Q';
	private final char AMINO_Lys = 'K';
	private final char AMINO_Glu = 'E';
	private final char AMINO_Trp = 'W';
	private final char AMINO_Gly = 'G';
	private final char AMINO_His = 'H';
	private final char AMINO_Asn = 'N';
	private final char AMINO_Asp = 'D';
	private final char AMINO_Cys = 'C';
	private final char AMINO_Arg = 'R';
	
	int alignLen=0, cntNoGap=0, cntPos=0, cntNeg=0, cntGap=0, cntOpen=0, cntExact=0;
}
