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

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.RunCmd;
import util.methods.Static;
import util.methods.TCWprops;

public class MultiAlignData {
	private final static int MUSCLE = Globals.Ext.MUSCLE;
	private final static int SEQUENCE_LINE_LENGTH = 80;
	private final static String BASEDIR = Globalx.ALIGNDIR;
	private static final String inFileName = "inSeq.fa";
	private static final String outFileName = "outAln.fa";
	private static final String traceFileName = "trace.log";
	private static final String score1FileName = "score1.txt";
	private static final String score2FileName = "score2.txt";
	
	public MultiAlignData(int alignPgm) {
		this.alignPgm = alignPgm;
		nameVec = new Vector<String> ();
		seqVec = new Vector<String> ();
		
		File b = new File(BASEDIR);
		if(!b.exists()) b.mkdir();
		outDir = BASEDIR + "/";
	}
	public MultiAlignData() {
		nameVec = new Vector<String> ();
		seqVec = new Vector<String> ();
	}
	// runMulti -  called from ScoreMulti with cluster names
	// viewMulti - called from seqs.AlignMultiPanel with any set of sequences
	public void addSequence(String name, String AASequence) {
		nameVec.add(name);
		seqVec.add(AASequence);
	}
	
	/*******************************************************
	 * the following gets the multi-alignment from the database
	 */
	public boolean getAlign(MTCWFrame theParentFrame, int grpID) {
		try {
			DBConn mDB = theParentFrame.getDBConnection();
			String conSeq = mDB.executeString("select conSeq from pog_groups where PGid=" + grpID);
			if (conSeq==null) {
				computeFailure("No alignment in database for this cluster", "No align");
				return false; 
			}
			// name and sequences in order from database
			int n=nameVec.size();
			String [] name = nameVec.toArray(new String[n]);
			String [] seq = seqVec.toArray(new String[n]);
			nameVec.clear(); seqVec.clear();
			
			// reordered based on alignment
			String [] aName = new String[n];
			String [] aSeq = new String[n];
			
			for (int i=0; i<name.length; i++) {
				String sql = "select alignMap from pog_members " +
						" where UTstr='" + name[i] + "' and PGid=" + grpID;
				String mapNpos = mDB.executeString(sql);
				if (mapNpos==null) {Out.prt("TCW error: " + sql); break;}
				
				String [] tok = mapNpos.split(Share.DELIM);
				if (tok.length!=2) Out.die("TCW error: " + name[i] + " " + mapNpos);
				
				int pos = Static.getInteger(tok[0])-1; // starts at 1
				if (pos<0 || pos>n) Out.die("TCW error: " + n + " " + pos + " " + name[i] + " " + mapNpos);
				String map = tok[1];
				
				// pos is the MAFFT aligned position, which is how we want to output these
				aName[pos] = name[i];
				aSeq[pos] = Share.uncompress(map, seq[i]);
				if (aSeq[pos]==null || aSeq[pos].length()==0) {
					Out.prt("TCW error: problem recreating sequences");
					Out.prt("   Sequence: " + seq[i]);
					Out.prt("   Map: " + mapNpos);
					aSeq[pos]="xxxxxxxxxxxxxxxxxxxxxxxxx";
				}
			}
			mDB.close();
			
			nameVec.add("Consensus");
			seqVec.add(conSeq);
			for (int i=0; i<n; i++) {
				nameVec.add(aName[i]);
				seqVec.add(aSeq[i]);
			}
			return true;
		}
		catch(Exception e) {ErrorReport.reportError(e, "Cannot get alignment from db"); return false;}
	}
	/*************************************************************************
	 * Everything below is for running a multi-alignment with scores
	 * viewMulti: prtCmd false, prtScores true - write scores to file
	 * runMulti:  prtCmd false, prtScores false
	 */
	public int runAlignPgm(boolean prtCmd, boolean prtScores, boolean isAA) {
		String cmd=""; 
		int rc=0;
		
		try {
			long time = Out.getTime();
			
			String x = (isAA) ? "AA_" : "NT_";
			String seqFile = outDir + x + inFileName;
			writeFASTA(seqFile);
			
			outAlignedFile = outDir + x + outFileName;
			File out = new File(outAlignedFile);
			if (out.exists()) out.delete();
			
			String cmdPath = TCWprops.getExtDir();
			if (alignPgm==MUSCLE) cmdPath +=  Globals.Ext.muscleExe;
			else 				  cmdPath +=  Globals.Ext.mafftExe;
			
			RunCmd rCmd = new RunCmd();
			
			if (alignPgm==Globals.Ext.MUSCLE) {
				cmd = cmdPath + " -in " + seqFile + " -out " + outAlignedFile;
				rc = rCmd.runP(cmd, prtCmd);
			}
			else { 
				int cpus = Runtime.getRuntime().availableProcessors(); 
				cmd = cmdPath + " --auto --reorder --thread " + cpus + " " + seqFile;
				String traceFile = outDir + traceFileName;
				
				rc = rCmd.runP(cmd, outAlignedFile, traceFile, prtCmd);
			}
			if (rc!=0) {
				Out.prt(cmd); // CAS303
				//computeFailure("Failed alignment", "Failed"); CAS303 prints in calling routine
				return rc;
			}
			
			if (!readFASTA(outAlignedFile)) {
				computeFailure("Failed reading alignment", "Failed");
				return rc;
			}
			
			computeConsensus(isAA);// create consensus at position 0
			if (prtScores) {
				String xx = (alignPgm==Globals.Ext.MUSCLE) ? "MUSCLE " : "MAFFT ";
				Out.PrtSpMsgTime(0, xx + (nameVec.size()-1) + " in " + outAlignedFile, time);
			}
			if (isAA) computeScores(prtScores, prtCmd); 
			return rc;
		} 
		catch(Exception e) {ErrorReport.reportError(e, "Exec: " + cmd); return -1;}
	}
	
	public void clear() {
		nameVec.clear();
		seqVec.clear();
	}
	private void writeFASTA(String name) {
		try {
			int lineLen = SEQUENCE_LINE_LENGTH;
			PrintWriter out = new PrintWriter(new FileWriter(name));
			
			Iterator<String> nameIter = nameVec.iterator();
			Iterator<String> seqIter = seqVec.iterator();
			
			while(nameIter.hasNext()) {
				out.println(">" + nameIter.next());
				int pos = 0;
				String seq = seqIter.next();
				if (seq.endsWith(Share.stopStr)) seq = seq.substring(0, seq.length()-1);
				for(;(pos + lineLen) < seq.length(); pos += lineLen) {
					out.println(seq.substring(pos, pos+lineLen));
				}
				out.println(seq.substring(pos));
			}
			out.close();
		} catch(Exception e) {ErrorReport.reportError(e);}
	}

	private boolean readFASTA(String name) {
		try {
			if (!new File(name).exists()) {
				Out.PrtError("File does not exist: " + name);
				return false;
			}
			nameVec.clear();
			seqVec.clear();
			
			BufferedReader in = new BufferedReader(new FileReader(name));
			
			String theSequence = "", theSequenceName = "", line;
			while((line = in.readLine()) != null) {
				if(line.startsWith(">")) {
					if(theSequenceName.length() > 0 && theSequence.length() > 0) {
						nameVec.add(theSequenceName);
						seqVec.add(theSequence);
					}
					theSequenceName =  line.substring(1);
					theSequence = "";
				} else {
					theSequence += line;
				}
			}
			if(theSequenceName.length() > 0 && theSequence.length() > 0) {
				nameVec.add(theSequenceName);
				seqVec.add(theSequence);
			}
			in.close();	
			
			return true;
		} catch(Exception e) {ErrorReport.reportError(e); return false;}
	}
	// viewMultiTCW will try to display - 
	private void computeFailure(String msg, String name) {
		try {
			Out.PrtError(msg);
			nameVec.insertElementAt(name, 0);
			seqVec.insertElementAt("*************************************", 0);
			
			String file = outDir + score1FileName;
		 	File f = new File(file);
		 	if  (f.isFile() && f.exists()) f.delete();
		 	
			file = outDir + score2FileName;
			f = new File(file);
		 	if  (f.isFile() && f.exists()) f.delete();
		} catch(Exception e) {ErrorReport.reportError(e); }
	}
	/****************************************************************
	 * Set consensus for alignment
	 */
	private void computeConsensus(boolean isAA) {
		String conSeq = "";
		
		int seqLen = seqVec.get(0).length(); //All sequences are aligned.. they are all the same length
		
		//Get counts of each symbol
		Hashtable<Character, Integer> counts = new Hashtable<Character, Integer>();
		for(int x=0; x<seqLen; x++) {
			counts.clear();
			for (String seq : seqVec) {
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
		nameVec.insertElementAt("Consensus", 0);
		seqVec.insertElementAt(conSeq, 0);
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
	/********************************************************
	 * Writes score to file when Muscle or Mafft is run from viewMulti
	 */
	public void computeScores(boolean prtScores, boolean prtCmd) {
		try {
			ScoreMulti obj = new ScoreMulti();
			
			String resultFile = outDir + score1FileName;
			score1 = obj.scoreSumOfPairs(prtScores, seqVec.toArray(new String[seqVec.size()]));
			
			if (prtScores) {
				Vector <String> colScore = obj.getScore();
					
				PrintWriter out = new PrintWriter(new FileWriter(resultFile));
				out.format("Average of Sum of Sum of Pairs %.3f\n", score1);
				for (String d : colScore) 
					out.println(d);
				out.close();
			}
		
			resultFile = outDir + score2FileName;
			score2 = obj.scoreMstatX(prtScores, prtCmd, outAlignedFile, resultFile);
			
			if (prtScores) {
				Vector <String> colScore = obj.getScore();
				
				PrintWriter out = new PrintWriter(new FileWriter(resultFile));
				out.format("Trident %.3f\n", score2);
				for (String d : colScore) 
					out.println(d);
				out.close();
			}
		} 
		catch(Exception e) {ErrorReport.reportError(e, "Write Scores");}
	}
	
	/** get results by calling program **/
	public int getNumSequences() { return seqVec.size(); }
	
	public String [] getSequenceNames() { 
		return nameVec.toArray(new String[seqVec.size()]); 
	}
	
	public String [] getSequences() { // returns aligned seqs after alignSequences is called
		return seqVec.toArray(new String[seqVec.size()]); 
	}

	public double getScore1() {return score1;}
	public double getScore2() {return score2;}
	
	private double score1=Globalx.dNoScore, score2=Globalx.dNoVal;
	private String outDir=null, outAlignedFile=null;
	// holds names, where 'consensus' is the first entry after alignment
	private Vector<String> nameVec = null;
	// holds original, then aligned, where the first entry is the consensus
	private Vector<String> seqVec = null; 
	private ScoreAA scoreObj = new ScoreAA ();
	private int alignPgm;
}
