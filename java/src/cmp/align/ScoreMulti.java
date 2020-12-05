package cmp.align;

/*********************************************************
 * Scores a multiple alignment
 * runMulti - store in database 
 * viewMulti - written to file 
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;
import java.util.HashSet;

import cmp.database.Globals;

import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Out;
import util.methods.RunCmd;
import util.methods.Static;
import util.methods.TCWprops;

public class ScoreMulti {
	private final int bothGAP = 0;
	private final int unkGAP = -1;
	private final int aaGAP = -4;
	private final char gapCh = Globalx.gapCh;
	
	private final double pGap = 0.25;
	private final double pGap2 = 0.50;
	private final int maxRow = 50;
	
	public ScoreMulti() {} 
	
	 /**************************************************************
	  * Avg of columns sum of Sum of Pairs
	 // www.info.univ-angers.fr/~gh/Idas/Wphylog/guidetree.pdf
	 // though its averaged by the number of columns, otherwise, bigger clusters will likely have bigger scores
	 *****************************************************************/
	 private final char BORDER='+'; // leading or trailing gap
	 public double scoreSumOfPairs(String grpID, String [] alignedSeq) {
		 try {
			 scoreVec.clear();
			 int nRows = alignedSeq.length; 
			 int nCols = alignedSeq[0].length();
			
			 if (nRows>maxRow) {
				 char [] con = alignedSeq[0].toCharArray();
				 int nGap=0;
				 for (int i=0; i<nCols; i++) if (con[i]==gapCh) nGap++;
				 double fr = (double)nGap/(double)nCols;
				 if (fr>pGap) return Globalx.dNoScore; // CAS312
			 }
			 // Leading and trailing gaps should not be treated as embedded gap, as we don't know what it is
			 // aaObj.scoreCh will score it less then an embedded gap
			 char [][] seqs = new char [nRows] [nCols];
			 
			 int nGap=0;
			 for (int r=1, s=0; r<nRows; r++, s++) {
				 seqs[s] = alignedSeq[r].toCharArray();
				 
				 for (int i=0; i<nCols   && seqs[s][i]==gapCh; i++) seqs[s][i]=BORDER;
				 for (int i=nCols-1; i>0 && seqs[s][i]==gapCh; i--) seqs[s][i]=BORDER;
				 for (int i=0; i<nCols; i++) if (seqs[s][i]==gapCh) nGap++;
			 }
			 if (nRows>maxRow) {
				 double fr = (double)nGap/((double)nCols*(double)nRows);
				 if (fr>pGap2) return Globalx.dNoScore; // CAS312
			 }
			 nRows--;
			 
			 int sum=0;
			 for (int c=0; c<nCols; c++) {
				int col_stat =  0; 
			
				for (int r=0; r<nRows-1; r++) {
					for (int x=r+1; x<nRows; x++) {
						col_stat += scoreCh(seqs[r][c], seqs[x][c]);
					}
				}
				sum += col_stat;
			
				scoreVec.add(col_stat+"");
			}
			// Though there are (nRows*(nRows-1)/2) * nCols comparisons
			// The average is on the nCols since its the column sum that is relevant
			double score = (sum!=0) ? (Math.abs(sum)/(double)nCols) : 0; // CAS312 
			if (sum<0) score = -score;
			
			scoreVec.add(String.format("Sum %d  columns %d",sum, nCols));
			return score;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "scoreAvgSumOfPairs " + grpID);}
		 return Globalx.dNoScore;
	 }
	public double scoreCh(char c1, char c2) {
		if (c1==Share.gapCh && c2==Share.gapCh) return bothGAP;
		if (c1==Share.gapCh || c2==Share.gapCh) return aaGAP;
		if (c1==BORDER || c2==BORDER) return unkGAP; // set in ScoreMulti if gap is leading or trailing 
		
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
			 scoreVec.clear();

			 int Nrows = aaSeq.length-1; // 1st row is consensus 
			 int Lcols = aaSeq[0].length();
			 
			 // convert to char array and remove consensus
			 char [][] msa = new char [Nrows] [Lcols];
			 for (int r=1, s=0; s<Nrows; r++, s++) 
				 msa[s] = aaSeq[r].toCharArray();
			 
			 String aa = "ARNDCQEGHILKMFPSTWYV-"; 
			 char [] alphabet = aa.toCharArray();
			 int Kaa = aa.length();
			 
			 double [][] p = new double [Lcols][Kaa]; // probablity of each AA per column
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
			 double [] col_stat =  new double [Lcols];
			 
		 // calculate score for each column, 
			 for (int x=0; x<Lcols; x++) {
				col_stat[x] =  0.0; 
				for (int a=0; a<Kaa; a++) {
					for (int j=0; j<Nrows; j++) {
						if (msa[j][x]==alphabet[a]) 
							p[x][a] += weight[j];
					}
					if (p[x][a] != 0.0) {
						col_stat[x] -= p[x][a] * Math.log(p[x][a]);
					}
				}
				col_stat[x] *= lambda;
				col_stat[x] = (1-col_stat[x]); // MstatX low values are best; this flips it
				sum += col_stat[x];
				
				scoreVec.add(""+col_stat[x]);
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
	public double scoreMstatX(boolean prtCmd, String type, String alignedFile, String resultFile) {
		 try {
			 scoreVec.clear();
			 String cmd = TCWprops.getExtDir() + Globals.Ext.mstatxExe;
			 cmd +=  " -s " + type + "  -i " + alignedFile + " -o " + resultFile;
			 
			 RunCmd rCmd = new RunCmd();
			 int rc = rCmd.runP(cmd, prtCmd);
			 
			 if (rc!=0) return Globalx.dNoScore;
			 
			 double sum=0.0;
			 int nCols=0;
			 String line="";
			 
			 if (!FileHelpers.fileExists(resultFile)) { // CAS303
				 Out.Print(cmd);
				 Out.die("Did not produce output file - fatal error");
			 }
			 BufferedReader in = new BufferedReader(new FileReader(resultFile));
			 while((line = in.readLine()) != null) {
				 String [] tok = line.split("\\s+");
				 if (tok.length<2) continue;
				 double score = Static.getDouble(tok[1]);
				 if (score==Double.MAX_VALUE) score=0.0; // can get 'nan'
				 sum += score;
				 nCols++;
				 
				 scoreVec.add(score+"");
			 }
			 in.close();
			 double score = sum/(double) nCols;
			
			 return score;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "score MstatX");}
		 return Globalx.dNoScore;
	}
	public Vector <String> getColScore() {return scoreVec;}
	 
	private Vector<String> scoreVec =  new Vector <String> ();
}
