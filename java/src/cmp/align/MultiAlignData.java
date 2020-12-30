package cmp.align;
/**********************************************
 * Alignment for MSA
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.sql.ResultSet;

import cmp.database.Globals;
import cmp.database.DBinfo;
import cmp.compile.runMTCWMain;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.RunCmd;
import util.methods.Static;
import util.methods.TCWprops;

public class MultiAlignData {
	// if either of the following is 1, then running MstatX
	private final int tBUILT =  Globals.MSA.BUILTIN_SCORE;
	private final int tMSTATX = Globals.MSA.MSTATX_SCORE;

	private int    msaScoreType1=tBUILT, msaScoreType2 = tBUILT;  // Defaults set in runMTCWMain
	private String msaScoreName1 = Globals.MSA.SoP, msaScoreName2 = Globals.MSA.Wep;
	private String scoreMethods=null;
	private int    nMSTATX=0;
	
	private final int    SEQUENCE_LINE_LENGTH = 80;
	private final String BASEDIR =  Globalx.ALIGNDIR;
	
	private final String inFileName =     	"inSeq.fa";
	private final String outFileName =    	"outAln.fa";
	private final String msaTraceFileName = "trace.log";
	private final String mstatxPre = "MstatX";
	private final String consName = Globals.MSA.consName;
	
	/*******************************************************
	 * Already aligned: Get the MSA from database
	 */
	// Run:  compile.MultiStats - score type can be changed in runMultiTCW
	public MultiAlignData() {
		this.isRun = true;
		
		msaScoreType1 = runMTCWMain.msaScore1; 
		msaScoreName1 = runMTCWMain.strScore1;
		String prt1 = msaScoreName1;
		if (msaScoreType1==tMSTATX) {
			prt1=  mstatxPre + " " + prt1;
			msaScoreName1 = msaScoreName1.toLowerCase(); // for Mstatx
			nMSTATX++;
		}	
		msaScoreType2 = runMTCWMain.msaScore2; 
		msaScoreName2 = runMTCWMain.strScore2;
		String prt2 = msaScoreName2;
		if (msaScoreType2==tMSTATX) {
			prt2=  mstatxPre + " " + prt2;
			msaScoreName2 = msaScoreName2.toLowerCase(); // for Mstatx
			nMSTATX++;
		}
		scoreMethods =  prt1 + ";" + prt2;
		
		Out.PrtSpMsg(1,"MSA Score1 " + prt1);
		Out.PrtSpMsg(1,"MSA Score2 " + prt2);
	}
	// View: seq.align.MultiViewPanel - get score type from DBinfo
	public MultiAlignData(DBinfo info) {
		this.isRun = false;
		if (info==null) return; // MSAdb
		
		String l = mstatxPre.toLowerCase();
		
		String s1 = info.getMSA_Score1();
		if (s1.toLowerCase().startsWith(l)) {
			msaScoreType1 = tMSTATX;
			msaScoreName1 = s1.substring(s1.indexOf(" ")).trim();
			nMSTATX++;
		}
		else msaScoreName1 = s1.trim();
		
		String s2 = info.getMSA_Score2();
		if (s2.toLowerCase().startsWith(l)) {
			msaScoreType2 = tMSTATX;
			msaScoreName2 = s2.substring(s2.indexOf(" ")).trim();
			nMSTATX++;
		}
		else msaScoreName2 = s2.trim();
	}
	/**********************************************************************
	 * viewMulti - MSAdb
	 * runMulti -  scores only
	 */
	public boolean loadAlignFromDB(DBConn mDB, int grpID) {
		try {	
			ResultSet rs = mDB.executeQuery("select conSeq, count, PGstr from pog_groups where PGid="+grpID);
			if (!rs.next()) {
				alignFailure("No alignment in database for this cluster " + grpID, "No align");
				return false; 
			}
			String conSeq = rs.getString(1);
			int n = rs.getInt(2);
			grpName = rs.getString(3);
			
			// name and sequences in order from database
			String [] name = nameVec.toArray(new String[n]);
			String [] seq =  origSeqVec.toArray(new String[n]);
			nameVec.clear(); origSeqVec.clear();
			
			// reordered based on alignment
			String [] aName = new String[n];
			String [] aSeq = new String[n];
			
			for (int i=0; i<name.length; i++) {
				String sql = "select alignMap from pog_members " +
						" where UTstr='" + name[i] + "' and PGid=" + grpID;
				String mapNpos = mDB.executeString(sql);
				if (mapNpos==null) return rcPrt(sql);
				
				String [] tok = mapNpos.split(Share.DELIM);
				if (tok.length!=2) return rcPrt(name[i] + " " + mapNpos);
				
				int pos = Static.getInteger(tok[0])-1; // starts at 1
				if (pos<0 || pos>=n) return rcPrt("  n: " + n  + "   " + name[i] + " " + mapNpos);
					
				String map = tok[1];
				
				// pos is the MAFFT aligned position, which is how we want to output these
				aName[pos] = name[i];
				aSeq[pos] = Share.uncompress(map, seq[i]);
				
				if (aSeq[pos]==null || aSeq[pos].length()==0) return rcPrt(seq[i] + "   Map: " + mapNpos);
			}
			nameVec.add(consName);
			algnSeqVec.add(conSeq);
			for (int i=0; i<n; i++) {
				nameVec.add(aName[i]);
				algnSeqVec.add(aSeq[i]);
			}
			
			rs = mDB.executeQuery("select score1, score2 from pog_scores where PGid=" + grpID);
			if (rs.next()) {
				strColScores1=rs.getString(1);
				strColScores2=rs.getString(2);
			}
			else strColScores1=strColScores2=null;
				
			return true;
		}
		catch(Exception e) {
			ErrorReport.reportError(e, "Cannot get alignment from db " + grpID); return false;
		}
	}
	private boolean rcPrt(String msg) {
		Out.PrtErr("Stored wrong: " + grpName + " " + msg);
		return false;
	}
	/*****************************************************************
	 * Run: can only score AA. Called from cmp.compile.MultiStats
	 */
	public boolean scoreOnly() {
		try {
			if (msaScoreType1==tMSTATX || msaScoreType2==tMSTATX) {
				setOutAlgnFile(true);
				writeSeqVec(outAlgnFile, algnSeqVec); // writes the previously aligned sequences
			}
			scoreMSA(); 
			return true;
		}
		catch(Exception e) {ErrorReport.reportError(e, "Cannot get alignment from db"); return false;}
	}
	/*************************************************************************
	 * XXX multi-alignment with scores
	 * viewMulti: bView true - write scores to file
	 * runMulti:  bView false - write scores to DB
	 */
	public int runAlignPgm(int alignPgm, boolean isAA) {
		int rc = runAlign(alignPgm, isAA);
		if (rc!=0) return rc;
		
		alignConsensus(isAA);
		if (isAA) scoreMSA(); 
			
		return 0;
	}
	private int runAlign(int alignPgm, boolean isAA) {
		String cmd=""; 
		int rc=0;
		
		try {
		/** write sequence to file **/
			setOutAlgnFile(isAA); // creates ResultAlign directory if necessary
			File out = new File(outAlgnFile);
			if (out.exists()) out.delete();
			
			String x = (isAA) ? "AA_" : "NT_";
			String seqFile = outDir + x + inFileName;
			
			writeSeqVec(seqFile, origSeqVec);
			
			String cmdPath = TCWprops.getExtDir();
			
		/** run alignment **/		
			RunCmd rCmd = new RunCmd();
			
			if (alignPgm==Globals.Ext.MUSCLE) {
				cmd = cmdPath + Globals.Ext.muscleExe + " -in " + seqFile + " -out " + outAlgnFile;
				rc = rCmd.runP(cmd, false /* prt cmd */);
			}
			else { 
				int cpus = Runtime.getRuntime().availableProcessors(); 
				
				cmd = cmdPath + Globals.Ext.mafftExe + " --auto --reorder --thread " + cpus + " " + seqFile;
				String traceFile = outDir + msaTraceFileName;
				
				rc = rCmd.runP(cmd, outAlgnFile, traceFile,  false /* prt cmd */);
			}
			if (rc!=0) {
				Out.prt(cmd); // CAS303
				return rc;
			}
			
		/** read alignment to nameVec and alignSeqVec **/	
			if (!readAlgnSeq(outAlgnFile)) {
				alignFailure("Failed reading alignment", "Failed");
				return -1;
			}
			return 0;
		} 
		catch(Exception e) {ErrorReport.reportError(e, "Exec: " + cmd); return -1;}
	}
	
	/************************************************
	 * Compute consensus for alignment
	 */
	private void alignConsensus(boolean isAA) {
		String conSeq = "";
		
		int seqLen = algnSeqVec.get(0).length(); //All sequences are aligned.. they are all the same length
		
		//Get counts of each symbol
		Hashtable<Character, Integer> counts = new Hashtable<Character, Integer>();
		for(int x=0; x<seqLen; x++) {
			counts.clear();
			for (String seq : algnSeqVec) {
				char c= seq.charAt(x);
				if (counts.containsKey(c)) counts.put(c, counts.get(c) + 1);
				else counts.put(c, 1);
			}
			
			//Convert counts to a ordered list
			Vector <AlignSymbol> cList = new Vector<AlignSymbol> ();
			for(Map.Entry<Character, Integer> val : counts.entrySet())
				cList.add(new AlignSymbol(val.getKey(), val.getValue()));
		
			//Map no longer needed at this point
			counts.clear();
			Collections.sort(cList);
			
			//Test if the there is one symbol most frequent
			if( alignTotalCount(cList) <= 1)
				conSeq += Globals.gapCh;
			else if(cList.size() == 1 || (cList.get(0).count>cList.get(1).count && cList.get(0).symbol!=Globals.gapCh))
				conSeq += cList.get(0).symbol;
			else if (cList.get(0).symbol == Globals.gapCh && (cList.size()==2 || cList.get(1).count>cList.get(2).count))
				conSeq += cList.get(1).symbol;
			else
				conSeq += alignBestChar(cList, isAA);
		}
		nameVec.insertElementAt(consName, 0);
		algnSeqVec.insertElementAt(conSeq, 0);
	}
	private Character alignBestChar(Vector<AlignSymbol> symbols, boolean isAA) {
		//Special case: need at least 2 non-gap values to have consensus
		if(alignTotalCount(symbols) == 1)
			return Globals.gapCh;
		
		int [] relateCounts = new int[symbols.size()];
		for(int x=0; x<relateCounts.length; x++)
			relateCounts[x] = 0;
		
		//Going with the assumption that relationships are not mutually inclusive
		for(int x=0; x<relateCounts.length; x++) {
			for(int y=x+1; y<relateCounts.length; y++) {
				if (isAA) {
					if(scoreObj.isHighSub(symbols.get(x).symbol, symbols.get(y).symbol)) {
						relateCounts[x]++;
						relateCounts[y]++;
					}
				}
				else {
					if (symbols.get(x).symbol == symbols.get(y).symbol) {
						relateCounts[x]++;
						relateCounts[y]++;
					}
				}
			}
		}
		//Find highest value
		int maxPos = 0;
		
		for(int x=1; x<relateCounts.length; x++) {
			if( (relateCounts[x]) > (relateCounts[maxPos]) )
				maxPos = x;
		}
		return symbols.get(maxPos).symbol;
	}
	private static int alignTotalCount(Vector <AlignSymbol> cList) {
		int retVal = 0;
		
		Iterator<AlignSymbol> iter = cList.iterator();
		while(iter.hasNext()) {
			AlignSymbol temp = iter.next();
			if(temp.symbol != Globals.gapCh)
				retVal += temp.count;
		}
		return retVal;
	}
	//Data structure for sorting/retrieving counts
	private class AlignSymbol implements Comparable<AlignSymbol> {
		public AlignSymbol(Character symbol, Integer count) {
			this.symbol = symbol;
			this.count = count;
		}
		public int compareTo(AlignSymbol arg) {
			return -1 * count.compareTo(arg.count);
		}
		public Character symbol;
		public Integer count;
	}
	// viewMultiTCW will try to display - 
	private void alignFailure(String msg, String name) {
		try {
			Out.PrtError(msg);
			nameVec.insertElementAt(name, 0);
			algnSeqVec.insertElementAt("*************************************", 0);
		} catch(Exception e) {ErrorReport.reportError(e); }
	}
	/*********************************************************
	// Compute MSA score
	 * bView -  true - print to file if builtin, false - save to DB
	 * prtCmd - MstatX - print command
	******************************************************/
	private void scoreMSA() {
		try {
			ScoreMulti smObj = new ScoreMulti();
			
			String [] alignSeq= (nMSTATX<2) ? algnSeqVec.toArray(new String[algnSeqVec.size()]) : null;
			
		/** Score 1 Default type0 Sum-of-pairs **/
			if (msaScoreType1==tMSTATX) {
				String resultFile = getScoreFile(mstatxPre + "_" + msaScoreName1);
				glScore1 = smObj.scoreMstatX(msaScoreName1, outAlgnFile, resultFile);
			}
			else 
				glScore1 = smObj.scoreSumOfPairs(grpName, alignSeq, isRun);
			
			double [] score1 = smObj.getScores();
			String [] tScore1 = smObj.getStrSc();
			if (Double.isNaN(glScore1)) glScore1=Globalx.dNoVal; // non-norm SoP can be negative....
				
		/** Score 2 Default type1 Built-in Entropy **/	
			if (msaScoreType2==tMSTATX) {
				String resultFile = getScoreFile(mstatxPre + "_"  + msaScoreName2);
				glScore2 = smObj.scoreMstatX(msaScoreName2, outAlgnFile, resultFile);
			}
			else 
				glScore2 = smObj.scoreEntropy(grpName, alignSeq);
			
			double [] score2 = smObj.getScores();
			String [] tScore2 = smObj.getStrSc();
			if (Double.isNaN(glScore2)) glScore2=Globalx.dNoVal;
			
			if (!isRun) {
				if (msaScoreType1!=tMSTATX && tScore1!=null) writeScore("SoP", tScore1);
				if (msaScoreType2!=tMSTATX && tScore2!=null) writeScore("Wentropy", tScore2);
			}
			
		/** Comma delimited list for saving and for MSA View **/
			strColScores1=strColScores2=null;
			for (double d : score1) {
				if (strColScores1==null)  	strColScores1 =  String.format("%.3f", d);
				else 						strColScores1 += String.format(", %.3f", d);
			}
			for (double d : score2) {
				if (strColScores2==null)  	strColScores2 =  String.format("%.3f", d);
				else 						strColScores2 += String.format(", %.3f", d);
			}
		} 
		catch(Exception e) {ErrorReport.reportError(e, "Write Scores");}
	}
	/*******************************************************************
	 * Read fasta formated alignment from Muscle/Mafft
	 *****/
	private boolean readAlgnSeq(String name) {
		try {
			if (!new File(name).exists()) {
				Out.PrtError("File does not exist: " + name);
				return false;
			}
			clear(); // don't need origSeqVec anymore
			
			BufferedReader in = new BufferedReader(new FileReader(name));
			
			String theSequence = "", theSequenceName = "", line;
			while((line = in.readLine()) != null) {
				if(line.startsWith(">")) {
					if(theSequenceName.length() > 0 && theSequence.length() > 0) {
						nameVec.add(theSequenceName);
						algnSeqVec.add(theSequence);
					}
					theSequenceName =  line.substring(1);
					theSequence = "";
				} else {
					theSequence += line;
				}
			}
			if(theSequenceName.length() > 0 && theSequence.length() > 0) {
				nameVec.add(theSequenceName);
				algnSeqVec.add(theSequence);
			}
			in.close();	
			
			return true;
		} catch(Exception e) {ErrorReport.reportError(e); return false;}
	}
	/*******************************************************************
	 * 1. Write original sequences for MSA
	 * 2. Write aligned sequences for scoring MSTATX
	 *****/
	private void writeSeqVec(String fname, Vector <String> seqVec) {
		try {
			int lineLen = SEQUENCE_LINE_LENGTH;
			PrintWriter out = new PrintWriter(new FileWriter(fname));
			
			for (int i=0; i<nameVec.size(); i++) {
				String name = nameVec.get(i);
				if (name.contentEquals(consName)) continue;
				out.println(">" + name);
				
				String seq = seqVec.get(i);
				if (seq.endsWith(Share.stopStr)) seq = seq.substring(0, seq.length()-1);
				
				int pos = 0;
				for(;(pos + lineLen) < seq.length(); pos += lineLen) {
					out.println(seq.substring(pos, pos+lineLen));
				}
				out.println(seq.substring(pos));
			}
			out.close();
		} catch(Exception e) {ErrorReport.reportError(e);}
	}
	
	private void writeScore(String name, String [] score) { 
		try {
			String fname = getScoreFile(name);
			PrintWriter out = new PrintWriter(new FileWriter(fname));
			
			for (int i=0; i<score.length; i++) {
				out.println(score[i]);
			}
			out.close();
		} catch(Exception e) {ErrorReport.reportError(e);}
	}
	private void setOutAlgnFile(boolean isAA) {
		String x = (isAA) ? "AA_" : "NT_";
		File b = new File(BASEDIR);
		if(!b.exists()) b.mkdir();
		outDir = BASEDIR + "/";
		outAlgnFile = outDir + x + outFileName;
	}
	private String getScoreFile(String name) {
		 return outDir + Globals.MSA.filePrefix + name + ".txt";
	}
	/******************************************************
	 * Set sequence by calling program
	 * runMulti -  called from ScoreMulti
	 * viewMulti - called from seqs.Align.MultiViewPanel 
	 */
	public void addSequence(String name, String AASequence) {
		nameVec.add(name);
		origSeqVec.add(AASequence);
	}
	/***************************************************
	 * get results by calling program 
	 ***/
	public String [] getSequenceNames() { 
		return nameVec.toArray(new String[nameVec.size()]); 
	}
	public String [] getAlignSeq() { // returns aligned seqs after alignSequences is called
		return algnSeqVec.toArray(new String[algnSeqVec.size()]); 
	}
	
	public double getMSA_gScore1() {return glScore1;}
	public double getMSA_gScore2() {return glScore2;}
	
	public String getGlScores()   {return String.format("(%.3f,%.3f)", glScore1, glScore2);}
	public String getColScores1() {return strColScores1;} 
	public String getColScores2() {return strColScores2;}
	
	public String getScoreMethods() { return scoreMethods;} // for entering into database
	
	public void clear() {
		nameVec.clear();
		algnSeqVec.clear();
		origSeqVec.clear();
	}
	/*********************************************************************/
	private double glScore1=Globalx.dNoVal, glScore2=Globalx.dNoVal;
	private String outDir=null, outAlgnFile=null;
	
	// holds names, where 'consensus' is the first entry after alignment
	private Vector<String> nameVec = new Vector<String> ();
	
	// (1) holds original, (2) then aligned, where the first entry is the consensus
	private Vector<String> origSeqVec = new Vector<String> (); 
	private Vector<String> algnSeqVec = new Vector<String> ();
	
	// holds column scores
	private String strColScores1=null, strColScores2=null; // comma delimited list
	
	private ScoreAA scoreObj = new ScoreAA ();
	private String grpName=""; // Only for loadAlignFromDB
	
	private boolean isRun=false;
}
