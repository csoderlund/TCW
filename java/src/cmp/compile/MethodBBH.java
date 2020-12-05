package cmp.compile;
/**********************************************
 * Best Recipocal Hit
 */

import java.sql.ResultSet;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;

/******************************************************
 * Bi-directional best hit - sequences not from the same database
 * RULE 1: No selected set - do all pairs of sTCWs
 * RULE 2: Selected set only: if >2, it will be a N-way
 */
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;

import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import cmp.database.Globals;

public class MethodBBH {
	 private String groupFile = Globals.Methods.BestRecip.TYPE_NAME;
	 private String [] covTypes = {"Either", "Both"};
	 private String [] strType = {"Amino acid", "Nucleotide"};
		 
	 public boolean run(int idx, DBConn db, CompilePanel panel) {
		Out.PrtDateMsg("\nStart execution of BBH");
		if (!setParams(idx, db, panel)) return false;
		
		long startTime = Out.getTime();
		long allTime =  startTime;
		Out.PrtSpMsg(1, "Start processing...");
		
		if (bSuccess) loadSTCWdbs();
		if (bSuccess) loadSeqs();
		if (bSuccess) loadPairs();
		if (bSuccess) computeBBH();
		if (bSuccess) writeGroups();
		Out.PrtSpMsgTime(1, "Finish computing BBH", startTime);
		Out.PrtSpMsg(1, "");
		 
		if (bSuccess) 
			if (new MethodLoad(mDB).run(idx, groupFile, cmpPanel)==-1) 
				bSuccess=false;
		
		Out.PrtMsgTimeMem("Finish execution of BBH", allTime);
		return bSuccess;
	}
	
	/************************************************************
	 * Compute best BBH
	 * find non-ambiguous best, i.e. each only has one hit, and its reciprocal
	 */
	private void computeBBH() {
		boolean anySuccess=false;
		for (int i=0; i<nSTCW-1; i++) {
			for (int j=i+1; j<nSTCW; j++) {
				computeBBHsets(stcwIDs[i],stcwIDs[j]);
				if (!anySuccess) anySuccess=bSuccess;
			}
		}
		bSuccess = anySuccess;
		if (!bSuccess) return;
		
		if (bSetSelected && selectSTCW.size()>2) {
			mergeBBH();
		}
	}
	/**********************************************************************
	 * Only pairs that pass the similarity and overlap test are loaded.
	 * But that means the supposed BBH may not be loaded, but the second best may be
	 * and will appear to be the best.
	 * 1. do aaBest and ntBest and use them?
	 * 2. no longer allow NT BBH?
	 */
	private void computeBBHsets(int db1, int db2) {
	try {
		Out.PrtSpMsg(2, "Computing BBH for " + stcwMap.get(db1) + " and " + stcwMap.get(db2) + "...");
		int cntMate=0, cntNoMate=0, cntNotShared=0;
		
		for (int seqID : seqPairsMap.keySet()) {
			Seq seqObj = seqPairsMap.get(seqID);
			if (seqObj.dbIdx!=db1) continue;
			
			Best seqBestObj = seqObj.getBest(db2);
			if (seqBestObj.done) continue;
			if (seqBestObj.mateID==-1) continue;
			
			Seq  mateObj = seqPairsMap.get(seqBestObj.mateID);
			Best mateBestObj = mateObj.getBest(db1);
			if (mateBestObj.done) continue;
			
			if (mateBestObj.mateID!=-1 && mateBestObj.mateID!=seqID) {
				cntNotShared++;
				continue;
			}
			if (mateBestObj.mateID==-1) { 
				cntNoMate++; cntMate++;
				Out.PrtSpMsg(0, seqObj.seqName + " " + db1 + " " + db2 + " " + mateBestObj.mateName);
			}
			else cntMate++;
	
			mateBestObj.done=true;
			seqBestObj.done=true;
			
			// BBH Pair, add to set
			String full1 = seqObj.dbPrefix  + "|" + seqObj.seqName;
			String full2 = mateObj.dbPrefix + "|" + mateObj.seqName;
			
			String key;
			if (full1.compareToIgnoreCase(full2)<0) key = full1 + " " + full2;
			else key =  full2 + " " + full1;
			
			bbhSet.add(key);
		}
		if (bbhSet.size()==0) { // shouldn't happen
			bSuccess=false;
			Out.PrtWarn("No pairs found " + stcwMap.get(db1) + " " + stcwMap.get(db2));
			return;
		}
		
		String msg =  "Non-ambiguous best hit ";
		if (cntNoMate>0) msg += " (" + cntNoMate + " Best hit's mate has no best hit)";
		Out.PrtCntMsg(cntMate, msg);
		if (cntNotShared>0) Out.PrtCntMsg(cntNotShared, "Not shared best hit");	
	}
	catch (Exception e) {ErrorReport.reportError(e, "Error on find BBH"); bSuccess=false;}
	}
	/*******************************************************
	 * mergeBBH - only executed if the user selected >2 STCWs
	 */
	private void mergeBBH() {
	try {
		Out.PrtSpMsg(2, "Merging pairs ...");
		seqPairsMap.clear(); // done with this
		
		TreeMap <String, Merge> seqMergeMap =  new TreeMap <String,Merge> ();
		
		// asmName|seqName asmName|seqName
		for (String bbh : bbhSet) {
			String [] pair = bbh.split(" ");
	
			String [] mA = pair[0].split("\\|");
			String [] mB = pair[1].split("\\|");
			
			Merge seq;
			if (seqMergeMap.containsKey(mA[1])) 
				seq = seqMergeMap.get(mA[1]);
			else {
				seq = new Merge(mA[0], mA[1]);
				seqMergeMap.put(mA[1], seq);
			}
			seq.add(mB[0], mB[1]);
			
			if (seqMergeMap.containsKey(mB[1])) 
				seq = seqMergeMap.get(mB[1]);
			else {
				seq = new Merge(mB[0], mB[1]);
				seqMergeMap.put(mB[1], seq);
			}
			seq.add(mA[0], mA[1]);
		}
		bbhSet.clear();
		
		int cntNo=0;
		HashMap <String, Integer> nWay = new HashMap <String, Integer> ();
		
		for (String name : seqMergeMap.keySet()) {
			Merge seq = seqMergeMap.get(name);
			
			if (seq.done || seq.mergeMap.size()<nSTCW-1) continue;
			
			nWay.clear();
			for (String name2 : seq.mergeMap.keySet()) {
				Merge seq2 = seqMergeMap.get(name2);
				if (!nWay.containsKey(name2)) nWay.put(name2, 1);
				else nWay.put(name2, nWay.get(name2)+1);
				
				for (String name3 : seq2.mergeMap.keySet()) 
					if (!nWay.containsKey(name3)) nWay.put(name3, 1);
					else nWay.put(name3, nWay.get(name3)+1);
			}

			boolean isBad=false;
			for (String n : nWay.keySet()) {
				if (nWay.get(n)!=nSTCW-1) {
					isBad=true;
					break;
				}
			}
			if (isBad) {
				cntNo++;
				continue;
			}
			seq.done=true;
			String bbh= seq.dbName + "|" + name + " ";
			
			for (String name2 : seq.mergeMap.keySet()) {
				seq = seqMergeMap.get(name2);
				seq.done=true;
				
				bbh += seq.dbName + "|" + name2 + " ";
			}
			
			bbhSet.add(bbh);
		}	
		Out.PrtCntMsg(bbhSet.size(), nSTCW + "-way BBH");
		Out.PrtCntMsg(cntNo, "No n-way BBH");
		if (bbhSet.size()==0) bSuccess=false;
	}
	catch (Exception e) {ErrorReport.reportError(e, "Error on merge BBH"); bSuccess=false;}
	}
	/**********************************************************
	* Read ids and prefixed for selected sTCWdbs
	* The interface used the sTCWid (ASMstr) but the prefix needs to be written to file
	*/
	private void loadSTCWdbs() {
	try {
		Out.PrtSpMsg(2, "Load sTCW database information");
	
		ResultSet rs = mDB.executeQuery("select asmID, ASMstr, prefix from assembly");
		while (rs.next()) {
			String sid = rs.getString(2);
			if (selectSTCW.contains(sid)) 
				stcwMap.put(rs.getInt(1), rs.getString(3)); // prefix used to write file
		}
		nSTCW = stcwMap.size();
		Out.PrtCntMsg(nSTCW, "sTCW databases to compare");
		
		stcwIDs = new int [nSTCW];
		int j=0;
		for (int i : stcwMap.keySet()) stcwIDs[j++] = i;
	}
	catch (Exception e) {ErrorReport.reportError(e, "Error on load STCWdbs"); bSuccess=false;}
	}
	/**********************************************************
	* Read sequence lengths from database and enter into seqLenMap
	*/
	private void loadSeqs() {
	try {
		Out.PrtSpMsg(2, "Load " + strType[type] + " sequences from database");
		
		String msq;
		if (type==0) msq = "SELECT UTid, UTstr, ASMid FROM unitrans WHERE aaSeq is NOT NULL";
		else msq =         "SELECT UTid, UTstr, ASMid FROM unitrans WHERE ntSeq is NOT NULL";
		ResultSet rs = null;
		
		for (int i=0; i<nSTCW; i++) {
			rs = mDB.executeQuery(msq + " and ASMid=" + stcwIDs[i]);
			while (rs.next()) {
				int id = rs.getInt(1);
				String name = rs.getString(2);
				int sid = rs.getInt(3);
				seqPairsMap.put(id, new Seq(name, stcwMap.get(sid), sid));
			}
		}
		if (rs!=null) rs.close();		
		Out.PrtCntMsg(seqPairsMap.size(), "Sequences loaded from database");
	}
	catch (Exception e) {ErrorReport.prtReport(e, "getting sequences "); bSuccess=false;}
	}
	/**********************************************************
	 * Read sequence lengths from database and enter into seqLenMap
	 */
	private void loadPairs() {
	try {
		Out.PrtSpMsg(2, "Load " + strType[type] + " blast pairs from database");
		int cntLoad=0, cntMinSim=0, cntMinOlap=0;
		
		String msq="";
		if (type==0) msq = "select UTid1, UTid2, aaEval, aaSim, aaOlap1, aaOlap2 " +
							" from pairwise where aaSim>0 and asmID1!=asmID2 and AAbest=2";
		else         msq = "select UTid1, UTid2, ntEval, ntSim, ntOlap1, ntOlap2, PAIRid " +
							" from pairwise where ntSim>0 and asmID1!=asmID2";
		ResultSet rs = mDB.executeQuery(msq);
		
		while (rs.next()) {
			cntLoad++;
			int seqID1=rs.getInt(1);
			int seqID2=rs.getInt(2);
			
			// Only seqIDs from selectSTCW are in seqPairsMap
			if (!seqPairsMap.containsKey(seqID1)) continue;
			if (!seqPairsMap.containsKey(seqID2)) continue;
			
			double eval = rs.getDouble(3);
			int sim = (int) (rs.getDouble(4)+0.5); 
			
			if (sim < simCutoff) {
				cntMinSim++;
				continue;
			}	
			int olap1  = (int) (rs.getDouble(5)+0.5);
			int olap2  = (int) (rs.getDouble(6)+0.5);
			
			if (covTypes[covMode].startsWith("Either")) {// ok if one is greater
				if (olap1 < covCutoff && olap2 < covCutoff) {
					cntMinOlap++;		
					continue;
				}
			}
			else if (covTypes[covMode].startsWith("Both")) { // both must be greater
				if (olap1 < covCutoff || olap2 < covCutoff) {
					cntMinOlap++;		
					continue;
				}
			}
			Seq seqObj1 = seqPairsMap.get(seqID1);
			Seq seqObj2 = seqPairsMap.get(seqID2);
			
			seqObj1.addIfBest(seqObj2.dbIdx, seqID2, eval, sim, seqObj2.seqName);
			seqObj2.addIfBest(seqObj1.dbIdx, seqID1, eval, sim, seqObj1.seqName);
		}
		rs.close();
		Out.PrtCntMsg(cntLoad,    "Loaded pairs from different datasets");
		Out.PrtCntMsg(cntMinSim,  "Failed similarity ");
		Out.PrtCntMsg(cntMinOlap, "Failed coverage ");
		Out.PrtCntMsg(seqPairsMap.size(), "Sequences with potential best mate");

		if (seqPairsMap.size()==0) bSuccess=false;
	}
	catch (Exception e) {ErrorReport.die(e, "loading from database"); bSuccess=false;}
	}
	/*******************************************************************/
	private boolean setParams(int idx, DBConn db, CompilePanel panel) {
		MethodPanel theMethod = panel.getMethodPanel();
		String [] settings = theMethod.getSettingsAt(idx).split(":");
		if (settings.length<5) {
			Out.PrtError("Incorrect parameters: " + theMethod.getSettingsAt(idx));
			return false;
		}
		prefix = theMethod.getMethodPrefixAt(idx);	// Groups should be prefixed with this
		
		covCutoff = Static.getInteger(settings[1].trim());
		if (covCutoff<0) covCutoff=0;
		covMode = 		Static.getInteger(settings[2].trim());
		simCutoff = 	Static.getInteger(settings[3].trim());
		if (simCutoff<0) simCutoff=0;
		type = 			Static.getInteger(settings[4].trim());
		String dbs =    settings[5].trim();
		
		if (dbs.contains(",")) {							//Selected by User: Rule 2
			String [] list = dbs.split(",");
			for (int i=0; i<list.length; i++)
				selectSTCW.add(list[i]);
			bSetSelected=true;
		}
		if (selectSTCW.size()==0) {						// No selection: Rule 1
			String [] list = panel.getDBInfo().getASM();
			for (int i=0; i<list.length; i++)
				selectSTCW.add(list[i]);
			bSetSelected=false;
			dbs = "All pairs of sTCWs";
		}
		
		if (covMode>covTypes.length) {
			Out.PrtError("Coverage length types " + covMode + " must be <" + covTypes.length);
			return false;
		}
		if (type!=0 && type!=1) {
			Out.PrtError("Bad type " + type + " must be 0 or 1");
			return false;
		}
		
		mDB = db;
		cmpPanel = panel;
		
		String root  = cmpPanel.getCurProjMethodDir() +  groupFile + 
				"." + prefix + "_" + covCutoff + "-" + simCutoff;
		groupFile = root;
		
		Out.PrtSpMsg(1, "Prefix:     " + prefix);
		Out.PrtSpMsg(1, "Coverage:   " + (int) covCutoff + " (" + covTypes[covMode] + ")");
		Out.PrtSpMsg(1, "Similarity: " + (int) simCutoff);
		Out.PrtSpMsg(1, "STCWdb:     " + dbs);
		Out.PrtSpMsg(1, "");
		
		if (type == 1 && panel.getNumNTdb()<=1) {
			Out.PrtWarn("Must have two nucleotide singleTCWs to use nucleotide blast - abort BHH");
			return false;
		}
		return true;
	}
	
	/*****************************************************
	 * Groups are written to file to be read in a clusters
	 */
	private void writeGroups() {
	try {
		Out.PrtSpMsg(2, "Write groups to file " + groupFile);			
		PrintWriter outFH = new PrintWriter(new FileOutputStream(groupFile)); 
		int group=1;
		for (String mems : bbhSet) {
			if (!mems.equals("dead")) {
				outFH.println(group + " " + mems);
				group++;
			}
		}
		outFH.close();
		Out.PrtSpMsg(3, "Wrote " + (group-1) + " clusters");
		bbhSet.clear();
	}
	catch (Exception e) {ErrorReport.prtReport(e, "write clusters to file"); bSuccess=false;}
	}
	
	/*************************************************************/
	private class Seq {
		public Seq (String seqName, String set, int index) {
			this.seqName = seqName;
			this.dbPrefix = set;
			this.dbIdx = index;
			
			for (int i : stcwMap.keySet()) 
				bestMap.put(i, new Best()); 
		}
		public void addIfBest (int asmIdx, int mate, double eval, double sim, String name) {
			Best b = bestMap.get(asmIdx);
			if (eval < b.eval || (eval==b.eval && sim > b.sim)) { 
				b.mateID = mate;
				b.eval = eval;
				b.sim = sim;
				b.mateName = name;
			}
		}
		
		public Best getBest(int asmIdx) { return bestMap.get(asmIdx);}
		
		String dbPrefix;
		int    dbIdx;
		String seqName;
		
		TreeMap <Integer, Best> bestMap = new TreeMap <Integer, Best> (); // seqIdx, Best for set
	}
	private class Best {
		public Best() {}
		
		int mateID=-1;
		String mateName="";
		double eval=100.0;
		double sim = 0;
		boolean done=false;
	}
	private class Merge {
		public Merge(String db, String name) {
			dbName=db;
			seqName = name;
		}
		public void add(String db, String name) {
			if (mergeMap.containsKey(name)) 
				Out.prt(seqName + " Conflict: " + db + " " + name);
			mergeMap.put(name, db);
		}
		String dbName, seqName;
		TreeMap <String, String> mergeMap = new TreeMap <String, String> (); 
		boolean done=false;
	}
	
	 private CompilePanel cmpPanel;
	 private DBConn mDB;
	
	 boolean bSuccess=true;
	 private String prefix;
	 private double covCutoff, simCutoff;
	 private int type, covMode;
	
	private boolean bSetSelected=false;
	private int nSTCW=0;
	private int [] stcwIDs;
	private TreeMap <Integer, Seq>    seqPairsMap =  new TreeMap <Integer,Seq> (); // seqID, list of pairs
	private TreeSet <String>  		  bbhSet =  new TreeSet <String> ();			// name1:name2
	private TreeMap <Integer, String> stcwMap = new TreeMap <Integer, String> (); // asmID, prefix
	private HashSet <String> selectSTCW = new HashSet <String> (); 				// Selected in MethodBBHPanel
}
