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
	private final int BUILTIN =     Globals.MSA.BUILTIN_SCORE;
	private final int MSTATX_TYPE = Globals.MSA.MSTATX_SCORE;

	private int    msaScoreType1=BUILTIN, msaScoreType2 = BUILTIN;  // Defaults set in runMTCWMain
	private String msaScoreName1 = Globals.MSA.SoP, msaScoreName2 = Globals.MSA.Wep;
	private String scoreMethods=null;
	private int nMSTATX=0;
	
	private final int    SEQUENCE_LINE_LENGTH = 80;
	private final int    MUSCLE =   Globals.Ext.MUSCLE;
	private final String BASEDIR =  Globalx.ALIGNDIR;
	
	private final String inFileName =     "inSeq.fa";
	private final String outFileName =    "outAln.fa";
	private final String msaTraceFileName =  "trace.log";
	private final String score1FileName = "msa_score1.txt"; // Mstatx results
	private final String score2FileName = "msa_score2.txt"; // Mstatx results
	private final String consName = Globals.MSA.consName;
	
	/*******************************************************
	 * Already aligned: Get the MSA from database
	 * 1. View: seq.align.MultiViewPanel
	 * 2. Run:  compile.MultiStats
	 */
	public MultiAlignData(boolean isRun) {
		if (isRun) setRunMultiParam();
	}
	public boolean getAlignFromDB(DBConn mDB, int grpID) {
		try {	
			ResultSet rs = mDB.executeQuery("select conSeq, count, PGstr from pog_groups where PGid="+grpID);
			if (!rs.next()) {
				computeFailure("No alignment in database for this cluster " + grpID, "No align");
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
	 * Run: can only score AA. 
	 */
	public boolean scoreOnly() {
		try {
			if (msaScoreType1==MSTATX_TYPE || msaScoreType2==MSTATX_TYPE) {
				setOutAlgnFile(true);
				writeSeqVec(outAlgnFile, algnSeqVec); // writes the aligned nameVec and seqVec
			}
			scoreMSA(false /* prt cmd */); 
			return true;
		}
		catch(Exception e) {ErrorReport.reportError(e, "Cannot get alignment from db"); return false;}
	}
	/*************************************************************************
	 * XXX Everything below is for running a multi-alignment with scores
	 */
	public MultiAlignData(int alignPgm, boolean isRun) {
		this.alignPgm = alignPgm;
		
		if (isRun) setRunMultiParam();
	}
	public void setAlignPgm(int alignPgm) { // CAS312 for run.multiAlign
		this.alignPgm = alignPgm;
		clear();
	}
	
	// viewMulti: prtCmd false, prtScores true - write scores to file
	// runMulti:  prtCmd false, prtScores false
	public int runAlignPgm(boolean prtCmd, boolean prtScores, boolean isAA) {
		String cmd=""; 
		int rc=0;
		
		try {
			long time = Out.getTime();

			setOutAlgnFile(isAA); // creates ResultAlign directory if necessary
			File out = new File(outAlgnFile);
			if (out.exists()) out.delete();
			
			String x = (isAA) ? "AA_" : "NT_";
			String seqFile = outDir + x + inFileName;
			
			writeSeqVec(seqFile, origSeqVec);
			
			String cmdPath = TCWprops.getExtDir();
			if (alignPgm==MUSCLE) cmdPath +=  Globals.Ext.muscleExe;
			else 				  cmdPath +=  Globals.Ext.mafftExe;
	
		/** run alignment **/		
			RunCmd rCmd = new RunCmd();
			
			if (alignPgm==Globals.Ext.MUSCLE) {
				cmd = cmdPath + " -in " + seqFile + " -out " + outAlgnFile;
				rc = rCmd.runP(cmd, prtCmd);
			}
			else { 
				int cpus = Runtime.getRuntime().availableProcessors(); 
				cmd = cmdPath + " --auto --reorder --thread " + cpus + " " + seqFile;
				String traceFile = outDir + msaTraceFileName;
				
				rc = rCmd.runP(cmd, outAlgnFile, traceFile, prtCmd);
			}
			if (rc!=0) {
				Out.prt(cmd); // CAS303
				//computeFailure("Failed alignment", "Failed"); CAS303 prints in calling routine
				return rc;
			}
			
			if (!readAlgnSeq(outAlgnFile)) {
				computeFailure("Failed reading alignment", "Failed");
				return rc;
			}
		
		/** create consensus at pos0 **/
			computeConsensus(isAA);
			if (prtScores) {
				String xx = (alignPgm==Globals.Ext.MUSCLE) ? "MUSCLE " : "MAFFT ";
				Out.PrtSpMsgTime(0, xx + (nameVec.size()-1) + " in " + outAlgnFile, time);
			}
			
		/** compute MSA score **/
			if (isAA) scoreMSA(prtCmd); 
			return rc;
		} 
		catch(Exception e) {ErrorReport.reportError(e, "Exec: " + cmd); return -1;}
	}
	
	// Set consensus for alignment
	private void computeConsensus(boolean isAA) {
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
			Vector <SymbolCount> cList = new Vector<SymbolCount> ();
			for(Map.Entry<Character, Integer> val : counts.entrySet())
				cList.add(new SymbolCount(val.getKey(), val.getValue()));
		
			//Map no longer needed at this point
			counts.clear();
			Collections.sort(cList);
			
			//Test if the there is one symbol most frequent
			if( getTotalCount(cList) <= 1)
				conSeq += Globals.gapCh;
			else if( cList.size() == 1 || (cList.get(0).count > cList.get(1).count && cList.get(0).symbol != Globals.gapCh))
				conSeq += cList.get(0).symbol;
			else if ( cList.get(0).symbol == Globals.gapCh && (cList.size() == 2 || cList.get(1).count > cList.get(2).count))
				conSeq += cList.get(1).symbol;
			else
				conSeq += getMostCommonRelated(cList, isAA);
		}
		nameVec.insertElementAt(consName, 0);
		algnSeqVec.insertElementAt(conSeq, 0);
	}
	private Character getMostCommonRelated(Vector<SymbolCount> symbols, boolean isAA) {
		//Special case: need at least 2 non-gap values to have consensus
		if(getTotalCount(symbols) == 1)
			return Globals.gapCh;
		
		//Create/initialize counters
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
	private static int getTotalCount(Vector <SymbolCount> cList) {
		int retVal = 0;
		
		Iterator<SymbolCount> iter = cList.iterator();
		while(iter.hasNext()) {
			SymbolCount temp = iter.next();
			if(temp.symbol != Globals.gapCh)
				retVal += temp.count;
		}
		return retVal;
	}
	//Data structure for sorting/retrieving counts
	private class SymbolCount implements Comparable<SymbolCount> {
		public SymbolCount(Character symbol, Integer count) {
			this.symbol = symbol;
			this.count = count;
		}
		public int compareTo(SymbolCount arg) {
			return -1 * count.compareTo(arg.count);
		}
		public Character symbol;
		public Integer count;
	}
	// viewMultiTCW will try to display - 
	private void computeFailure(String msg, String name) {
		try {
			Out.PrtError(msg);
			nameVec.insertElementAt(name, 0);
			algnSeqVec.insertElementAt("*************************************", 0);
		} catch(Exception e) {ErrorReport.reportError(e); }
	}
	/*********************************************************
	// Compute MSA score
	 * prtSc -  for built-in - print to file (MstatX prints to file)
	 * prtCmd - MstatX - print command
	******************************************************/
	private void scoreMSA(boolean prtCmd) {
		try {
			ScoreMulti smObj = new ScoreMulti();
			String [] alignSeq=null;
			if (nMSTATX<2) alignSeq = algnSeqVec.toArray(new String[algnSeqVec.size()]);
			
		/** Score 1 Default type0 Sum-of-pairs **/
			if (msaScoreType1==MSTATX_TYPE) 
				msa_score1 = smObj.scoreMstatX(prtCmd, msaScoreName1, outAlgnFile, outDir+score1FileName);
			else 
				msa_score1 = smObj.scoreSumOfPairs(grpName, alignSeq);
			colScores1 = smObj.getColScore();
				
		/** Score 2 Default type1 Built-in Entropy **/		
			if (msaScoreType2==MSTATX_TYPE) 
				msa_score2 = smObj.scoreMstatX(prtCmd, msaScoreName2, outAlgnFile, outDir+score2FileName);
			else 
				msa_score2 = smObj.scoreEntropy(grpName, alignSeq);
			colScores2 = smObj.getColScore();
			
			if (Double.isNaN(msa_score1)) msa_score1=Globalx.dNoVal; // SoP can be negative....
			if (Double.isNaN(msa_score2)) msa_score2=Globalx.dNoVal;
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
	 * 2. Write aligned sequences for scoreing
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
	/*****************************************************
	 * Setup for scores.
	 */
	private void setRunMultiParam() {
		msaScoreType1 = runMTCWMain.msaScore1; 
		msaScoreName1 = runMTCWMain.strScore1;
		String prt1 = msaScoreName1;
		if (msaScoreType1==MSTATX_TYPE) {
			prt1=  "Mstatx " + prt1;
			msaScoreName1 = msaScoreName1.toLowerCase(); // for Mstatx
			nMSTATX++;
		}	
		msaScoreType2 = runMTCWMain.msaScore2; 
		msaScoreName2 = runMTCWMain.strScore2;
		String prt2 = msaScoreName2;
		if (msaScoreType2==MSTATX_TYPE) {
			prt2=  "Mstatx " + prt2;
			msaScoreName2 = msaScoreName2.toLowerCase(); // for Mstatx
			nMSTATX++;
		}
		scoreMethods =  prt1 + ";" + prt2;
		
		Out.PrtSpMsg(1,"MSA Score1 " + prt1);
		Out.PrtSpMsg(1,"MSA Score2 " + prt2);
	}
	
	// TODO cmp.viewer.seq.align.MultiViewPanel for aligning a selected set
	// Get score types from dbInfo
	private void setViewMultiParam(String type1, String type2) {
		if (type1.startsWith("MstatX")) {
			msaScoreType1 = MSTATX_TYPE;
			msaScoreName1 = type1.substring(type1.indexOf(" "));
		}
		if (type2.startsWith("MstatX")) {
			msaScoreType2 = MSTATX_TYPE;
			msaScoreName2 = type1.substring(type1.indexOf(" "));
		}
	}
	
	private void setOutAlgnFile(boolean isAA) {
		String x = (isAA) ? "AA_" : "NT_";
		File b = new File(BASEDIR);
		if(!b.exists()) b.mkdir();
		outDir = BASEDIR + "/";
		outAlgnFile = outDir + x + outFileName;
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
	 * seqVec has been changed from original sequence to aligned
	 ***/
	public String [] getSequenceNames() { 
		return nameVec.toArray(new String[nameVec.size()]); 
	}
	public String [] getAlignSeq() { // returns aligned seqs after alignSequences is called
		return algnSeqVec.toArray(new String[algnSeqVec.size()]); 
	}
	public double getMSA_Score1() {return msa_score1;}
	public double getMSA_Score2() {return msa_score2;}
	
	public String getScoreMethods() { return scoreMethods;} // for entering into database
	
	public void clear() {
		nameVec.clear();
		algnSeqVec.clear();
		origSeqVec.clear();
		if (colScores1!=null) colScores1.clear();
		if (colScores2!=null) colScores2.clear();
	}
	/*********************************************************************/
	private double msa_score1=Globalx.dNoVal, msa_score2=Globalx.dNoVal;
	private String outDir=null, outAlgnFile=null;
	
	// holds names, where 'consensus' is the first entry after alignment
	private Vector<String> nameVec = new Vector<String> ();
	
	// (1) holds original, (2) then aligned, where the first entry is the consensus
	private Vector<String> origSeqVec = new Vector<String> (); 
	private Vector<String> algnSeqVec = new Vector<String> ();
	
	// holds column scores
	private Vector <String> colScores1 = null; // col# score; skip last line
	private Vector <String> colScores2 = null;
	
	private ScoreAA scoreObj = new ScoreAA ();
	private int alignPgm;
	private String grpName="";
}
