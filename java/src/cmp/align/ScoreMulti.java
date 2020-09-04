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
	
	public ScoreMulti() {} 
	
	 /**************************************************************
	  * Avg of columns sum of Sum of Pairs
	 // www.info.univ-angers.fr/~gh/Idas/Wphylog/guidetree.pdf
	 // though its averaged by the number of columns, otherwise, bigger clusters will likely have bigger scores
	 *****************************************************************/
	 private final char BORDER='+'; // leading or trailing gap
	 public double scoreSumOfPairs(boolean prt, String [] alignedSeq) {
		 try {
			 scoreVec.clear();
			 int nRows = alignedSeq.length; 
			 int nCols = alignedSeq[0].length();
			 int len = alignedSeq[0].length();
			
			 // Leading and trailing gaps should not be treated as embedded gap, as we don't know what it is
			 // aaObj.scoreCh will score it less then an embedded gap
			 char [][] seqs = new char [nRows] [nCols];
			 for (int r=1, s=0; r<nRows; r++, s++) {
				 seqs[s] = alignedSeq[r].toCharArray();
				 for (int i=0; i<len && seqs[s][i]=='-'; i++) seqs[s][i]=BORDER;
				 for (int i=len-1; i>0 && seqs[s][i]=='-'; i--) seqs[s][i]=BORDER;
			 }
			 nRows--;
			 
			 if (prt) // only done for viewMulti
					scoreVec.add(String.format("Col# Sum-of-pairs"));
			 int sum=0;
			 for (int c=0; c<nCols; c++) {
					int col_stat =  0; 
				
					for (int r=0; r<nRows-1; r++) {
						for (int x=r+1; x<nRows; x++) {
							col_stat += scoreCh(seqs[r][c], seqs[x][c]);
						}
					}
					sum += col_stat;
					if (prt) // only done for viewMulti
						scoreVec.add(String.format("%4d %4d",(c+1), col_stat ));
			}
			// Though there are (nRows*(nRows-1)/2) * nCols comparisons
			// The average is on the nCols since its the column sum that is relevant
			double score = (Math.abs(sum)/(double)nCols);
			if (sum<0) score = -score;
			
			 if (prt) // only done for viewMulti
					scoreVec.add(String.format("Sum %d  columns %d",sum, nCols));
			return score;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "scoreAvgSumOfPairs");}
		 return Globalx.dNoScore;
	 }
	
	public double scoreCh(char c1, char c2) {
		if (c1==Share.gapCh && c2==Share.gapCh) return bothGAP;
		if (c1==Share.gapCh || c2==Share.gapCh) return aaGAP;
		if (c1==BORDER || c2==BORDER) return unkGAP; // set in ScoreMulti if gap is leading or trailing 
		
		return ScoreAA.getBlosum(c1, c2);
	}
	/**************************************************
	 * Run MstatX trident
	 */
	public double scoreMstatX(boolean prt, boolean prtCmd, String alignedFile, String resultFile) {
		 try {
			 scoreVec.clear();
			 String cmd = TCWprops.getExtDir() + Globals.Ext.mstatxExe;
			 String type = Globals.Ext.score2.toLowerCase();
			 cmd +=  " -s " + type + "  -i " + alignedFile + " -o " + resultFile;
			 
			 RunCmd rCmd = new RunCmd();
			 int rc = rCmd.runP(cmd, prtCmd);
			 
			 if (rc!=0) {
				 if (rc < -1 && !prt) Out.die("Fatal error"); 
				 return Globalx.dNoScore;
			 }
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
				 double s = Static.getDouble(tok[1]);
				 if (s==Double.MAX_VALUE) s=0.0; // can get 'nan'
				 sum += s;
				 nCols++;
				 if (prt) // only done for viewMulti - rewrite the output with two extra lines
						scoreVec.add(String.format("%4d %.3f",nCols, s ));
			 }
			 in.close();
			 if (prt) // only done for viewMulti
					scoreVec.add(String.format("Sum %.3f  columns %d",sum, nCols));
			 return sum/(double) nCols;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "scoreTrident");}
		 return Globalx.dNoScore;
	}
	 /***********************************************************
	  * Copied from MstatX https://github.com/gcollet/MstatX
	  * Which was published in Valdar [2002] Scoring Residue Conservation
	  * This returns the same score as executing "mstatx --global -i inSeqs.fa"
	  * Where the global score is the mean of the column scores.
	  * @param name names associated with sequences
	  * @param seqs amino acid sequences
	  * @return score
	  * NOT WORKING 
	  */
	 public double scoreEntrophy(int grpID, String [] name, String [] aaSeq) {
		 try {
			 scoreVec.clear();

			 int nRows = aaSeq.length; // 1st row is consensus 
			 int nCols = aaSeq[0].length();
			 
			 String residues = "ARNDCQEGHILKMFPSTWYVBJZX*";
			 char [] alphabet = residues.toCharArray();
			 int nAA = residues.length();
			 
			 // convert to char array and remove consensus
			 char [][] seqs = new char [nRows] [nCols];
			 for (int r=1, s=0; r<nRows; r++, s++) 
				 seqs[s] = aaSeq[r].toCharArray();
			 nRows--;
			 
			 // calc number of AA symbols in each column (msa.getNtype(x))
			 int [] cntNtype = new int [nCols];
			 HashSet <Character> set = new HashSet <Character> ();
			 for (int c=0; c<nCols; c++) {
				 for (int r=0; r<nRows; r++)  
					 set.add(seqs[r][c]);
				 cntNtype[c] = set.size();
				 set.clear();
			 }
			 
			 // calc weight per sequence (calcSeqWeight)
			 // each seq is compared with all other seq for 'exact' matches, including itself
			 double [] weight = new double [nRows];
			 for (int r=0; r<nRows; r++) {
				 double w=0.0;
				 for (int c=0; c<nCols; c++) {
					 int n=0;
					 for (int x=0; x<nRows; x++) 
						 if (seqs[r][c]==seqs[x][c]) n++;
					 w += 1.0 / (double) (n * cntNtype[c]);
				 }
				 weight[r] = w/(double) nCols;
			 }
			 
			 double lambda = 1.0 / Math.log(Math.min(nAA,nRows));
			 double sum=0.0;
			 double [][] p = new double [nCols][nAA]; // probablity of each AA per column
			 for (int c=0; c<nCols; c++)
				 for (int a=0; a<nAA; a++) p[c][a]=0.0;
			 
			 // calculate score for each column, 
			 for (int c=0; c<nCols; c++) {
				double col_stat =  0.0; 
				for (int a=0; a<nAA; a++) {
					for (int r=0; r<nRows; r++) {
						if (seqs[r][c]==alphabet[a]) p[c][a] += weight[r];
					}
					if (p[c][a] != 0.0)
						col_stat -= p[c][a] * Math.log(p[c][a]);
				}
				col_stat *= lambda;
				sum += col_stat;
				if (name!=null) // only done for viewMulti
					scoreVec.add(String.format("%6.3f %4d", col_stat, (c+1)));
			 }	
			
			 double avg = sum/(double)nCols;
			
			 return avg;
		 }
		 catch(Exception e) {ErrorReport.reportError(e, "scoreEntrophy");}
		 return -1;
	 }
	
	 public Vector <String> getScore() {return scoreVec;}
	 
	 private Vector<String> scoreVec =  new Vector <String> ();
}
