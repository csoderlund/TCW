package cmp.align;

/*********************************************************
 * Scores a multiple alignment - first row is consensus
 * runMulti - store in database 
 * viewMulti - written to file 
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.util.HashSet;

import cmp.compile.runMTCWMain;
import cmp.database.Globals;
import cmp.viewer.MTCWMain;

import util.database.Globalx;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.RunCmd;
import util.methods.Static;
import util.methods.TCWprops;

public class ScoreMulti {
	public boolean bTest = false;
	public boolean SoP_NORM = runMTCWMain.bNSoP;  // CAS313; CAS401 makes average /nCols (was doing min-max normalization)
	
	private final int  bothGAP =  0;
	private final int  hangGAP =  0;
	private final int  aaGAP   = -4;
	private final char gapCh = Globalx.gapCh;
	
	// SoP takes forever on MSA of many rows - when there is usually many gaps - so it does not compute full SoP in this case
	private final double pGap  = 0.25;
	private final double pGap2 = 0.50;
	private final int    maxRow = 50;
	
	public ScoreMulti() {
		if (runMTCWMain.test || MTCWMain.test)  bTest=true;
	} 
	
	 /**************************************************************
	  * Sum of Sum of Pairs
	 // www.info.univ-angers.fr/~gh/Idas/Wphylog/guidetree.pdf
	 // Its averaged by the number of columns, otherwise, bigger clusters will likely have bigger scores
	 *****************************************************************/
	 private final char BORDER=Globalx.hangCh; // leading or trailing gap
	 public double scoreSumOfPairs(String grpID, String [] alignedSeq) {
		 try {
			 int nRows = alignedSeq.length; 
			 int nCols = alignedSeq[0].length();
			 dScores = new double [nCols];
			 strScores = null;
		
			 if (nRows>maxRow) {
				 char [] con = alignedSeq[0].toCharArray();
				 int nGap=0;
				 for (int i=0; i<nCols; i++) if (con[i]==gapCh) nGap++;
				 double fr = (double)nGap/(double)nCols;
				 if (fr>pGap) 
					 return Globalx.dNoScore; // CAS312
			 }
			 // Leading and trailing gaps should not be treated as embedded gap, as we don't know what it is
			 // aaObj.scoreCh will score it less then an embedded gap
			 char [][] seqs = new char [nRows] [nCols];
			 seqs[0] = alignedSeq[0].toCharArray();
			 
			 int nGap=0;
			 for (int r=1; r<nRows; r++) {
				 seqs[r] = alignedSeq[r].toCharArray();
				
				 for (int i=0; i<nCols   && seqs[r][i]==gapCh; i++) seqs[r][i]=BORDER;
				 for (int i=nCols-1; i>0 && seqs[r][i]==gapCh; i--) seqs[r][i]=BORDER;
				 
				 for (int i=0; i<nCols; i++) if (seqs[r][i]==gapCh) nGap++;
			 }
			 if (nRows>maxRow) {
				 double fr = (double)nGap/((double)nCols*(double)nRows);
				 if (fr>pGap2) return Globalx.dNoScore; // CAS312
			 }
			
			 for (int c=0; c<nCols; c++) {
				 dScores[c] =  0; 
				
				for (int r=1; r<nRows-1; r++) { // first is consensus 
					char a = seqs[r][c];
					
					for (int x=r+1; x<nRows; x++) 
						dScores[c] += scoreCh(a, seqs[x][c]);
				}
			}
			
			// #cmp = (nRows*(nRows-1)/2) * nCols
			// CAS401 -- was a pseudo min-max normalization, changed to /#cmp
			 nRows--;
			double n = (SoP_NORM) ? nCols : ((nRows*(nRows-1)/2) * nCols);
			double sum = 0.0;
			for (double x : dScores) sum += x;
			
			double score = (sum!=0) ? (Math.abs(sum)/n) : 0; // CAS312 
			if (sum<0) score = -score;
			
			return score;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "scoreAvgSumOfPairs " + grpID);}
		 return Globalx.dNoScore;
	 }
	
	private double scoreCh(char c1, char c2) {
		if (c1==Share.gapCh && c2==Share.gapCh) return bothGAP;
		if (c1==Share.gapCh || c2==Share.gapCh) return aaGAP;
		if (c1==BORDER || c2==BORDER) return hangGAP; // set in ScoreMulti if gap is leading or trailing 
		
		return ScoreAA.getBlosum(c1, c2);
	}
	
	 /***********************************************************
	  * Copied from MstatX https://github.com/gcollet/MstatX - wentrophy.cpp
	  * Which was published in Valdar [2002] Scoring Residue Conservation
	  * This returns the same score as executing "mstatx --global -i inSeqs.fa"
	  * Where the global score is the mean of the column scores.
	  */
	 public double scoreEntropy(String grpID, String [] aaSeq) {
		 try {
			 int Nrows = aaSeq.length-1; // 1st row is consensus 
			 int Lcols = aaSeq[0].length();
			 dScores = new double [Lcols];
			 
			 // convert to char array and remove consensus
			 char [][] msa = new char [Nrows] [Lcols];
			 for (int r=1, s=0; s<Nrows; r++, s++) 
				 msa[s] = aaSeq[r].toCharArray();
			 
			 String aa = "ARNDCQEGHILKMFPSTWYV-"; 
			 char [] alphabet = aa.toCharArray();
			 int Kaa = aa.length();
			 
			 double [][] p = new double [Lcols][Kaa]; // probability of each AA per column
			 for (int c=0; c<Lcols; c++)
				 for (int a=0; a<Kaa; a++) p[c][a]=0.0;
			 
		// calc number of AA symbols in each column (msa.getNtype(x))
			 int [] cntNtype = new int [Lcols];
			 HashSet <Character> set = new HashSet <Character> ();
			 for (int c=0; c<Lcols; c++) {
				 for (int r=0; r<Nrows; r++)  
					 set.add(msa[r][c]);
				 cntNtype[c] = set.size();
				 set.clear();
			 }
			 
		// calc weight per sequence (calcSeqWeight)
			 double [] weight = new double [Nrows];
			 for (int iSeq=0; iSeq<Nrows; iSeq++) {
				 weight[iSeq] = calcSeqWeight(iSeq, msa, cntNtype, Lcols, Nrows);
			 }
			 
			 double lambda = 1.0 / Math.log(Math.min(Kaa,Nrows));
			 double sum=0.0;
			
		 // calculate score for each column, 
			 for (int x=0; x<Lcols; x++) {
				dScores[x] =  0.0; 
				for (int a=0; a<Kaa; a++) {
					for (int j=0; j<Nrows; j++) {
						if (msa[j][x]==alphabet[a]) 
							p[x][a] += weight[j];
					}
					if (p[x][a] != 0.0) {
						dScores[x] -= p[x][a] * Math.log(p[x][a]);
					}
				}
				dScores[x] *= lambda;
				dScores[x] = (1-dScores[x]); // MstatX low values are best; this flips it
				sum += dScores[x];
			 }	
			
			 double score = sum/(double)Lcols;
			
			 return score;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "scoreEntrophy " + grpID);}
		 return -1;
	 }
	 private double calcSeqWeight(int i, char [][] msa, int [] cntNtype, int Lcols, int Nrows) {
		 double w=0.0;
		 
		 for (int xCol=0; xCol<Lcols; xCol++) {
			 int k = cntNtype[xCol];
			 int n=0;
			 for (int iSeq=0; iSeq<Nrows; iSeq++) {
				 if (msa[i][xCol] == msa[iSeq][xCol]) n++;
			 }
			 w += 1.0/((double) n * (double) k);
		 }
		 w /= (double) Lcols;
		
		 return w;
	 }
	
	 /**************************************************
	 * Run MstatX trident
	 * mStatx -s trident -i inputfile -o outputfile
	 * mStatx -g gives global score, which is the same as sum/(double) nCols
	 * mStatx default is -s wentrophy
	 */
	public double scoreMstatX(String type, String alignedFile, String resultFile) {
		 try {
			 String cmd = TCWprops.getExtDir() + Globals.Ext.mstatxExe;
			 cmd +=  " -s " + type + "  -i " + alignedFile + " -o " + resultFile;
			 
			 RunCmd rCmd = new RunCmd();
			 int rc = rCmd.runP(cmd, false);
			 
			 if (rc!=0) return Globalx.dNoScore;
			 
			 double sum=0.0;
			 int nCols=0;
			 String line="";
			 
			 if (!FileHelpers.fileExists(resultFile)) { // CAS303
				 Out.Print(cmd);
				 Out.die("Did not produce output file - fatal error");
			 }
			 Vector <Double> scoreVec = new Vector <Double> ();
			 BufferedReader in = new BufferedReader(new FileReader(resultFile));
			 while((line = in.readLine()) != null) {
				 String [] tok = line.split("\\s+");
				 if (tok.length<2) continue;
				 double score = Static.getDouble(tok[1]);
				 if (score==Double.MAX_VALUE) score=0.0; // can get 'nan'
				 sum += score;
				 nCols++;
				 
				 scoreVec.add(score);
			 }
			 in.close();
			 double score = sum/(double) nCols;
			 
			 dScores = new double [scoreVec.size()];
			 int c=0;
			 for (double d : scoreVec) dScores[c++] = d;
			
			 return score;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "score MstatX");}
		 return Globalx.dNoScore;
	}

	public double [] getScores() {return dScores;} // for saving in database from MultiAlignData
	
	public String [] getStrSc() {				  // for writing to file     " "
		if (strScores==null) {
			strScores = new String [dScores.length];
			for (int i=0; i<dScores.length; i++) 
				strScores[i] = String.format("%3d. %.3f", i, dScores[i]);
		}
		return strScores;
	}   
	
	/***********************************************
	 * Return trimStart, trimEnd
	 * CAS313 - MAFFT can align a few AA between long stretches of gaps, hence, heuristics to skip them.
	 * MultAlignPanel - used on Consensus to determine where to trim
	 */
	public static int [] trimCoords(String seqStr) {
		final int lenOfGap=10;
		final int lenOfCh=4;
		final int nSkipGap=3;
		
		int len = seqStr.length();
		int [] trim = new int [2];
		trim[0]=0; trim[1]=len;
		
		try {
			int c, tS=0;
			int nGap=0, nCh=0, nSkip=0;
			boolean inGap=false;
			
			for (c=0; c <len; c++) {
				if (seqStr.charAt(c)==Globalx.gapCh) {
					nGap++; 
					inGap=true;
				}
				else {
					if (inGap) {
						if (nGap>lenOfGap) tS=c;
						else nSkip++;
						
						if (nSkip>nSkipGap) break; // too many short gap stretches
						
						nCh=nGap=0;
					}
					nCh++;
					if (nCh>lenOfCh) break;        // enough characters in a row to show regardless of trim
				}
			}
			trim[0]=tS;
			
			int tE=len;
			nCh=nGap=nSkip=0;
			inGap=false;
			
			for (c=len-1; c > 0; c--) {
				if (seqStr.charAt(c)==Globalx.gapCh) {
					nGap++; 
					inGap=true;
				}
				else {
					if (inGap) {
						if (nGap>lenOfGap) tE=c;
						else nSkip++;
						
						if (nSkip>nSkipGap) break;
						
						nCh=nGap=0;
					}
					nCh++;
					if (nCh>lenOfCh) break;
				}
			}
			trim[1]=tE;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "creating realign sequences");}
		return trim;
	}
	
	private double [] dScores;
	private String [] strScores;
}
