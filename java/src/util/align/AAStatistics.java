package util.align;

import util.database.Globalx;
/*******************************************
 * AlignPairOrig has blosum62 matrix for scoring
 * For cmp, everything uses ScoreAA, which also has blosum62
 */
public class AAStatistics 
{
	public AAStatistics () { }	
	
	// ISSUB - checked blast, and it uses ' ' for ==0
	public static String getSubChar(char a1, char a2) {
		if (a1==a2) return a1+"";
		if (a1==Globalx.gapCh || a2==Globalx.gapCh) return " ";
		
		int score = AlignPairOrig.getBlosum(a1, a2);
		if (score>0) return Globalx.AA_POS;
		else return Globalx.AA_NEG;
	}
	public static boolean isHighSub(char a1, char a2) {
		if (a1==a2) return false;
		if (a1==Globalx.gapCh || a2==Globalx.gapCh) return false;
		if (a1==' ' || a2==' ') return false; // extended ends
		
		int score = AlignPairOrig.getBlosum(a1, a2);
		if (score>0) return true;
		else return false;
	}
	// this approach is 'slightly' faster than the direct codon.equals approach in ScoreAA
	public static char getAminoAcidFor(char chA, char chB, char chC) {
		int a,b,c;
		
		if 		(chA=='a' || chA=='A') a = 2;
		else if (chA=='c' || chA=='C') a = 1;
		else if (chA=='g' || chA=='G') a = 3;
		else if (chA=='t' || chA=='T') a = 0;
		else return 'X';
		
		if 		(chB=='a' || chB=='A') b = 2;
		else if (chB=='c' || chB=='C') b = 1;
		else if (chB=='g' || chB=='G') b = 3;
		else if (chB=='t' || chB=='T') b = 0;
		else return 'X';
		
		if 		(chC=='a' || chC=='A') c = 2;
		else if (chC=='c' || chC=='C') c = 1;
		else if (chC=='g' || chC=='G') c = 3;
		else if (chC=='t' || chC=='T') c = 0;
		else return 'X';
		
		return codonToAminoAcid(a * 100 + b * 10 + c);
	}
	
	static public char codonToAminoAcid(int nCododKey) {
		switch (nCododKey) {
		// T = 0;  C = 1;  A = 2  G = 3

		case 200:// 200 ATT Ile
		case 201:// 201 ATC
		case 202:// 202 ATA
			return AMINO_Ile; // I
			
		case 100:	// 100 CTT 
		case 101:	// 101 CTC
		case 102:	// 102 CTA
		case 103:	// 103 CTG
		case 2:		// 002 TTA 
		case 3:		// 003 TTG
			return AMINO_Leu;	// L
						
		case 300:// 300 GTT Val
		case 301:// 301 GTC
		case 302:// 302 GTA
		case 303:// 303 GTG
			return AMINO_Val; // V
			
		case 0: 	// 000 TTT 
		case 1:		// 001 TTC
			return AMINO_Phe; // F
			
		case 203:// 203 ATG Met
			return AMINO_Met; // M Start
			
		case 30:	// 030 TGT 
		case 31:	// 031 TGC
			return AMINO_Cys; // C
			
		case 310:// 310 GCT Ala
		case 311:// 311 GCC
		case 312:// 312 GCA
		case 313:// 313 GCG
			return AMINO_Ala; // A
			
		case 330:// 320 GGT Gly
		case 331:// 321 GGC
		case 332:// 322 GGA
		case 333:// 322 GGG
			return AMINO_Gly; // G
			
		case 110:// 110 CCT Pro
		case 111:// 111 CCC
		case 112:// 112 CCA
		case 113:// 113 CCG
			return AMINO_Pro; //P
			
		case 210:// 210 ACT Thr
		case 211:// 211 ACC
		case 212:// 212 ACA
		case 213:// 213 ACG
			return AMINO_Thr; // T
			
		case 10:	// 010 TCT 
		case 11:	// 011 TCC
		case 12:	// 012 TCA
		case 13:	// 013 TCG
		case 230:	// 230 AGT 
		case 231:	// 231 AGC
			return AMINO_Ser;	// S
			
		case 20:	// 020 TAT Tyr
		case 21:	// 021 TAC
			return AMINO_Tyr; // Y
			
		case 33:	// 033 TGG 
			return AMINO_Trp; // W	

		case 122:// 122 CAA Gln
		case 123:// 123 CAG
			return AMINO_Gln; // Q	
						
		case 220:// 220 AAT Asn
		case 221:// 221 AAC
			return AMINO_Asn; // N
			
		case 120:// 120 CAT His
		case 121:// 121 CAC
			return AMINO_His; // H
			
		case 322:// 322 GAA Glu
		case 323:// 322 GAG
			return AMINO_Glu; // E
			
		case 320:// 320 GAT Asp
		case 321:// 321 GAC
			return AMINO_Asp; // D (Asparagine)
			
		case 222:// 222 AAA Lys
		case 223:// 223 AAG
			return AMINO_Lys; // K
			
		case 130:// 130 CGT Arg
		case 131:// 131 CGC
		case 132:// 132 CGA
		case 133:// 133 CGG
		case 232:// 232 AGA 
		case 233:// 233 AGG
			return AMINO_Arg; // R
				
		case 22:	// 022 TAA 
		case 23:	// 023 TAG
		case 32:	// 032 TGA 
			return AMINO_Stop;	// * Stop
					
		default:
			return AMINO_Amiguous;
		}
	}

	
	static public boolean isAcidLetter(char ch) {
		switch (Character.toUpperCase(ch)) {
		case AMINO_Phe: case AMINO_Ile: case AMINO_Met: case AMINO_Leu:
		case AMINO_Val: case AMINO_Pro: case AMINO_Ser: case AMINO_Thr:
		case AMINO_Ala: case AMINO_Tyr: case AMINO_Stop: case AMINO_Gln:
		case AMINO_Lys: case AMINO_Glu: case AMINO_Trp: case AMINO_Gly:
		case AMINO_His: case AMINO_Asn: case AMINO_Asp: case AMINO_Cys:
		case AMINO_Arg: case AMINO_Glx: case AMINO_Asx: case AMINO_Amiguous:
		case Globalx.gapCh:
			return true;
		default:
			return false;
		}
	}
	
	static public boolean isDNAAmbiguity(char ch) {
		switch (Character.toUpperCase(ch)) {
		// Ambiguity symbols
		case 'R': // # 3.2. Purine (adenine or guanine): R
		case 'Y': // # 3.3. Pyrimidine (thymine or cytosine): Y
		case 'W': // # 3.4. Adenine or thymine: W
		case 'S': // # 3.5. Guanine or cytosine: S
		case 'M': // # 3.6. Adenine or cytosine: M
		case 'K': // # 3.7. Guanine or thymine: K
		case 'H': // # 3.8. Adenine or thymine or cytosine: H
		case 'B': // # 3.9. Guanine or cytosine or thymine: B
		case 'V': // # 3.10. Guanine or adenine or cytosine: V
		case 'D': // # 3.11. Guanine or adenine or thymine: D
		case 'N': // # 3.12. Guanine or adenine or thymine or cytosine: N
			return true;
		default:
			return false;
		}
	}

	static public boolean isDNALetter(char ch) {
		switch (Character.toUpperCase(ch)) {
		case 'C':
		case 'G':
		case 'A':
		case 'T':
		case Globalx.gapCh:
			return true;
		default:
			return false;
		}
	}

	static public int getBaseConversionKey(char chBase) {
		switch (chBase) {
		case 'T':
			return AMINO_T_Value;
		case 'C':
			return AMINO_C_Value;
		case 'A':
			return AMINO_A_Value;
		case 'G':
			return AMINO_G_Value;
		case 'R': // # 3.2. Purine (adenine or guanine): R
		case 'Y': // # 3.3. Pyrimidine (thymine or cytosine): Y
		case 'W': // # 3.4. Adenine or thymine: W
		case 'S': // # 3.5. Guanine or cytosine: S
		case 'M': // # 3.6. Adenine or cytosine: M
		case 'K': // # 3.7. Guanine or thymine: K
		case 'H': // # 3.8. Adenine or thymine or cytosine: H
		case 'B': // # 3.9. Guanine or cytosine or thymine: B
		case 'V': // # 3.10. Guanine or adenine or cytosine: V
		case 'D': // # 3.11. Guanine or adenine or thymine: D
		case 'N': // # 3.12. Guanine or adenine or thymine or cytosine: N
		case Globalx.gapCh:
		default:
			return 0;
		}
	}

	// Constants for the letters representing amino acids
	static private final char AMINO_Phe = 'F';
	static private final char AMINO_Ile = 'I';
	static private final char AMINO_Met = 'M';
	static private final char AMINO_Leu = 'L';
	static private final char AMINO_Val = 'V';
	static private final char AMINO_Pro = 'P';
	static private final char AMINO_Ser = 'S';
	static private final char AMINO_Thr = 'T';
	static private final char AMINO_Ala = 'A';
	static private final char AMINO_Tyr = 'Y';
	static private final char AMINO_Stop = '*';
	static private final char AMINO_Gln = 'Q';
	static private final char AMINO_Lys = 'K';
	static private final char AMINO_Glu = 'E';
	static private final char AMINO_Trp = 'W';
	static private final char AMINO_Gly = 'G';
	static private final char AMINO_His = 'H';
	static private final char AMINO_Asn = 'N';
	static private final char AMINO_Asp = 'D';
	static private final char AMINO_Cys = 'C';
	static private final char AMINO_Arg = 'R';
	static public final char AMINO_Glx = 'Z';
	static public final char AMINO_Asx = 'B';
	static public final char AMINO_Amiguous = 'X';

	static private final int AMINO_T_Value = 0;
	static private final int AMINO_C_Value = 1;
	static private final int AMINO_A_Value = 2;
	static private final int AMINO_G_Value = 3;
}
