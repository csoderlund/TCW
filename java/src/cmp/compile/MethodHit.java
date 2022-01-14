package cmp.compile;

/***************************************************************
 * Cluster on Best hitID or description
 * 
 */
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;

import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import cmp.database.Globals;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.BestAnno;

public class MethodHit {
	private final static String iDELIM = Globals.Methods.inDELIM;
	
	private boolean debug=false;
	private String groupFile = Globals.Methods.Hit.TYPE_NAME;
	private String hitName =   Globals.Methods.Hit.TYPE_NAME;
	private String [] hitTypes = {"HitID", "Description"}; 
	
	private final int DES_IDX = 1;
	 
	private int grpNum=0;
	
	 public boolean run(int idx, DBConn db, CompilePanel panel) {
		 
		Out.PrtDateMsg("\nStart execution of " + groupFile);
		if (!setParams(idx, db, panel)) return false;
		
		long startTime = Out.getTime();
		long allTime = startTime;
		Out.PrtSpMsg(1, "Start processing...");
		
		loadFromDB();
		if (bSuccess)
			computeBestGroups();
		if (bSuccess) 
			writeGroups(); // create grpMap<grpNum, hit>
		
		seqMap.clear(); hitMap.clear(); 
		
		Out.PrtSpMsgTime(1, "Finish computing " + hitName, startTime);
		Out.PrtSpMsg(1, "");
		 
		if (bSuccess) {
			GRPid=new MethodLoad(cmpDBC).run(idx, groupFile, cmpPanel); // loads the file just written
			if (GRPid==-1) bSuccess=false;
		}
		if (bSuccess)
			overWriteDesc();
			
		Out.PrtMsgTimeMem("Finish execution of " + hitName, allTime);
		return bSuccess; 
	}
	
	private boolean setParams(int idx, DBConn db, CompilePanel panel) {
		cmpDBC = db;
		cmpPanel = panel;
		
		MethodPanel theMethod = panel.getMethodPanel();
		prefix = theMethod.getMethodPrefixAt(idx);						// Groups should be prefixed with this
		String [] settings = theMethod.getSettingsAt(idx).split(iDELIM);
		
		if (settings.length<4) {
			Out.PrtWarn("Incorrect number of parameters: using defaults");
		}
		else {
			nHitType =  Static.getInteger(settings[1].trim());
			covCutoff = Static.getInteger(settings[2].trim());
			simCutoff = Static.getInteger(settings[3].trim());
			
			if (settings.length>=4) {
				int n = Static.getInteger(settings[4].trim());
				bAllHits = (n==1) ? false : true;
			}
		}
		if (covCutoff<0) covCutoff=Static.getInteger(Globals.Methods.Hit.COVERAGE_CUTOFF);
		if (simCutoff<0) simCutoff=Static.getInteger(Globals.Methods.Hit.SIMILARITY);
		if (nHitType<0 || nHitType>1) nHitType = Globals.Methods.Hit.TYPE_TOGGLE;
		
		String root = cmpPanel.getCurProjMethodDir() +  groupFile + "." + prefix + "_" + nHitType;
		groupFile = root;
		
		Out.PrtSpMsg(1, "Prefix:     " + prefix);
		Out.PrtSpMsg(1, "HitType:    " + hitTypes[nHitType]);
		Out.PrtSpMsg(1, "Coverage:   " + covCutoff);
		Out.PrtSpMsg(1, "Similarity: " + simCutoff);
		if (bAllHits) Out.PrtSpMsg(1, "All: Seqs must pair with all other seqs in cluster");
		else          Out.PrtSpMsg(1, "Any: Seqs must pair with at least one other seq in cluster");
		Out.PrtSpMsg(1, "");
		
		return true;
	}
	
	/********************************************************
	 * Compute clusters
	 */
	private void computeBestGroups() {
	try {
		Out.PrtSpMsg(2, "Compute best anno clusters from " + grpMap.size());
		int cntNoPairs=0;
		
		for (Grp gObj : grpMap.values()) {
			if (gObj.cntGrpSeq<=1) continue;
			
		// Find best hit to use as representative name (for desc)
			Vector <Hit> allHit = new Vector <Hit> ();
			for (int hitIdx : gObj.hits) allHit.add(hitMap.get(hitIdx));
			Collections.sort(allHit);
			gObj.bestHitObj = allHit.get(0);
									
		// Remove any seqs that don't have pairs
			HashSet <Integer> cpSeqs = new HashSet <Integer> ();
			
			int cnt=0, rm=0;
			boolean allPair=true;
			for (int seqIdx : gObj.grpSeqs) {
				Seq sObj = seqMap.get(seqIdx);
					
				for (int mIdx : gObj.grpSeqs) {
					if (sObj.bestHitIdx!=0 && seqIdx!=mIdx) {
						if (sObj.mate.contains(mIdx)) cnt++;
						if (bAllHits && !sObj.pair.contains(mIdx)) allPair=false; // CAS312 add totally connected
					}
				}
				if (cnt>=1 && allPair) {
					cpSeqs.add(seqIdx);
				}
				else {
					cntNoPairs++; 
					rm++;
					sObj.bestHitIdx=0; 
				}
			}
			if (rm>0) {
				gObj.grpSeqs = cpSeqs;
				gObj.cntGrpSeq = gObj.grpSeqs.size();
			}
			
		// Assign group number
			if (gObj.cntGrpSeq>1) {
				grpNum++;
				gObj.grpNum=grpNum;
			}
			grpIdxMap.put(gObj.grpNum, gObj);
		}
		if (cntNoPairs>0) {
			if (bAllHits) Out.PrtSpCntMsg(3, cntNoPairs, "Removed seqs for not pairing with all seqs in cluster");
			else          Out.PrtSpCntMsg(3, cntNoPairs, "Removed seqs for not pairing with at least one seq in cluster");
		}
		Out.PrtSpCntMsgZero(3, grpNum,  "Clusters from best anno hits " + hitTypes[nHitType]);	
	}
	catch (Exception e) {ErrorReport.die(e, "load Best Hits from DB"); bSuccess=false;}
	}
	
	/***********************************************************
	 * Need to load directly to keep description
	 */
	private void writeGroups() {
	try {
		Out.PrtSpMsg(2, "Write clusters to file " + groupFile);			
		PrintWriter outFH = new PrintWriter(new FileOutputStream(groupFile)); 
		
		int cnt=0;
		for (String key : grpMap.keySet()) {
			Grp gObj = grpMap.get(key);
			
			HashSet <Integer> seqs = gObj.grpSeqs;
			
			if (grpNum<=0 || seqs.size()==0) continue;
			
			grpIdxMap.put(gObj.grpNum, gObj);
			
			String grpStr = gObj.grpNum+"";
			
			for (Integer seq : seqs) {
				Seq seqObj = seqMap.get(seq);
				grpStr += " " + asmMap.get(seqObj.asmIdx) + "|" + seqObj.name;
			}
			outFH.println(grpStr);
			cnt++;
		}
		outFH.close();
		Out.PrtCntMsg(cnt, "Wrote clusters");
	}
	catch (Exception e) {ErrorReport.prtReport(e, "write clusters to file"); bSuccess=false;}
	}
	 /**************************************************************
	  * LoadMethod assigns a best hit for cluster - but need to make it correspond to these cluster
	  * XXX not changing anything right now, just checking
	  */
	 private void overWriteDesc() {		
	 try {
		 if (!debug) return;
		Out.PrtSpMsg(2, "Update description");
		int cnt=0;
		ResultSet rs = cmpDBC.executeQuery("select PGid, PGstr, HITid, HITstr from pog_groups where PMid=" + GRPid);
		while (rs.next()) {
			 //int dbGrpIdx =   rs.getInt(1);
			 String grpName = rs.getString(2);
			 int hitIdx =     rs.getInt(3);
			 String hitStr = rs.getString(4);
			 
			 // grpName = HT_000grpNum, where grpNum is index into grpMap
			 int grpNum=0;
			 Pattern patE = Pattern.compile("(\\w+)_(\\d+)"); //e.g. HT_00010
			 Matcher m = patE.matcher(grpName);
			 if (m.matches()) grpNum = Integer.parseInt(m.group(2));
			 else Out.die("Cannot parse cluster name: " + grpName);
			
			 if (!grpIdxMap.containsKey(grpNum)) Out.die("No cluster: " + grpName + " (" + m.group(2) + ", "+ grpNum + ")");
			 
			 Grp gObj = grpIdxMap.get(grpNum);
			
			 if (hitIdx!=gObj.bestHitObj.hitIdx) {
				 cnt++;
				 if (debug && cnt<10) 
					 Out.prt(String.format("%10s Load: %-20s  This: %-20s  %s", grpName, hitStr, gObj.bestHitObj.hitStr, gObj.bestHitObj.desc));
			 }
		}
		if (cnt==0) return;
		
		Out.PrtSpCntMsg(3, cnt, hitTypes[nHitType] +  " best hit changed");	
		
	} catch (Exception e) {ErrorReport.die(e, "error getting last PMid");bSuccess=false;}
	}
	
	 private void compress(String pMsg) {
	 try {
		int [] test = {1, 2, 3, 5, 10, 20}; // n=6  last element isn't used
		int n = test.length;
		int [] cntBest = new int [n];
		for (int i=0; i<n; i++) cntBest[i]=0;
		
		HashMap <String, Grp> comMap = new HashMap <String, Grp>  ();
		
		for (String desc : grpMap.keySet()) {
			Grp gObj = grpMap.get(desc);
			if (gObj.keep()) comMap.put(desc, gObj);  // only add keep 

			int nBest = gObj.cntGrpSeq;
			for (int i=0; i<n; i++) {
				if (i==0) {
					if (nBest <= test[i]) cntBest[i]++;
				}
				else if (i<(n-1)) {
					if (nBest > test[i-1] && nBest <= test[i]) cntBest[i]++;
				}
				else {
					if (nBest > test[i-1]) cntBest[i]++;
				}
			}
		}
		grpMap.clear();
		grpMap = comMap;
		Out.PrtCntMsg(grpMap.size(), pMsg);
		
		if (debug) {
			Out.PrtSpMsg(2, "Stats: ");
			String msg="Best Hits: ";
			for (int i=0; i<n-1; i++) msg += String.format(" %d:%-5d ", test[i], cntBest[i]);
			msg += String.format(" >%d:%-5d ", test[n-2], cntBest[n-1]);
			Out.PrtSpMsg(3, msg);
		}
		
	 } catch (Exception e) {ErrorReport.die(e, "compress");bSuccess=false;}
	 }
	/**********************************************************
	 * Load asm, hits and seqs
	 */
	private void loadFromDB() {
	try {
		Out.PrtSpMsg(2, "Load hits and seqs from database");
			
	/* asm (for output) */
		ResultSet rs = cmpDBC.executeQuery("select asmID, prefix from assembly");
		while (rs.next()) asmMap.put(rs.getInt(1), rs.getString(2));
	
	/* Unique hits  - create grpMap and hitMap of all hits*/
		rs = cmpDBC.executeQuery("select HITid, HITstr, description, nGO from unique_hits order by HITid");
		while (rs.next()) {
			int hitIdx = rs.getInt(1);
			String hitStr = rs.getString(2);
			String desc = rs.getString(3);
			int nGO = rs.getInt(4);
			
			String xdesc;
			if (nHitType==DES_IDX  && BestAnno.descIsGood(desc)) 
				 xdesc = BestAnno.getBestDesc(desc); // This is exact; descBrief is too generous for this
			else xdesc=hitStr;
			
			if (!grpMap.containsKey(xdesc)) grpMap.put(xdesc, new Grp());
			
			Grp gObj = grpMap.get(xdesc);
			gObj.addHit(hitIdx);
			
			hitMap.put(hitIdx, new Hit(hitIdx, hitStr, xdesc, desc, nGO, gObj));
		}
		if (hitMap.size()==0) {
			Out.PrtWarn("No hits data");
			bSuccess=false;
			return;
		}
		
		if (nHitType==DES_IDX) Out.PrtCntMsg(grpMap.size(), "Unique descriptions from " + hitMap.size() + " hits");
		else                   Out.PrtCntMsg(hitMap.size(), "Unique hits");
		
	/* Create Seq list with at least one hit - need len, bestANid  from unitrans table */
		rs = cmpDBC.executeQuery("select UTid, UTstr, asmID, aaLen, HITid from unitrans where HITid>0");
		while (rs.next()) {
			int  seqIdx = rs.getInt(1);
			String name = rs.getString(2);
			int  asmIdx = rs.getInt(3);
			int len =     rs.getInt(4);
			int bestANid = rs.getInt(5);
			
			Seq sObj = new Seq(asmIdx, name, len, bestANid);
			seqMap.put(seqIdx, sObj);
		}
		Out.PrtCntMsg(seqMap.size(), "Sequences with at least one hit");
		
	/* Add best hit per seq  */
		int failSim=0, failOlap=0, pass=0;
		rs = cmpDBC.executeQuery("select UTid, HITid, percent_id, alignment_len, e_value from unitrans_hits "
				+ " where bestAnno=1");
		while (rs.next()) {
			int seqIdx = 	rs.getInt(1);
			if (!seqMap.containsKey(seqIdx)) continue; // shouldn't happen
			
			int hitIdx = 	rs.getInt(2);	
			if (!hitMap.containsKey(hitIdx)) continue; // shouldn't happen
			
			int sim = 		rs.getInt(3);
			int alignLen = 	rs.getInt(4);
			double eval = 	rs.getDouble(5);
			
			if (sim<simCutoff) {
				failSim++;
				continue;
			}
			Seq sObj = seqMap.get(seqIdx);
			double cov = ((double) alignLen/(double) sObj.len)*100.0;
			if (cov<covCutoff) {
				failOlap++;
				continue;
			}
			pass++;
			Hit hObj = hitMap.get(hitIdx);
			hObj.addBest(seqIdx,  eval);  // adds to grpMap also; add to hit and grp counts
		}
		Out.PrtCntMsg(failSim,  "Seq-hit fail similarity");
		Out.PrtCntMsg(failOlap, "Seq-hit fail coverage");
		Out.PrtCntMsg(pass,     "Seq-hit pass parameters ");
	
	/* Seq pairs */
		failSim=failOlap=pass=0;
		rs = cmpDBC.executeQuery("select UTid1, UTid2, aaOlap1, aaOlap2, aaSim from pairwise");
		while (rs.next()) {
			int seq1=rs.getInt(1);
			int seq2=rs.getInt(2);
			
			if (seqMap.containsKey(seq1) && seqMap.containsKey(seq2)) {
				seqMap.get(seq1).addPair(seq2);
				seqMap.get(seq2).addPair(seq1);
			}
			else continue;
			
			int olap1 = rs.getInt(3);
			int olap2 = rs.getInt(4);
			if (olap1<covCutoff || olap2<covCutoff) {
				failOlap++;
				continue;
			}
			int sim = rs.getInt(5);
			if (sim<simCutoff) {
				failSim++;
				continue;
			}
			
			if (seqMap.containsKey(seq1) && seqMap.containsKey(seq2)) {
				seqMap.get(seq1).addMate(seq2);
				seqMap.get(seq2).addMate(seq1);
				pass++;
			}
		}
		Out.PrtCntMsg(failSim, "Seq-seq fail similarity");
		Out.PrtCntMsg(failOlap, "Seq-seq fail coverage");
		Out.PrtCntMsg(pass, "Seq-seq pair pass parameters ");
		rs.close();		
		
		compress("Hits with multiple seq");
	}
	catch (Exception e) {ErrorReport.die(e, "load Best Hits from DB"); bSuccess=false;}
	}
	/*******************************************************
	 * Grp
	 */
	private HashMap <String, Grp>  grpMap = new HashMap <String, Grp> ();        // desc, Hit
	private HashMap <Integer, Grp> grpIdxMap = new HashMap <Integer, Grp> ();	 // grpIdx, Hit
	
	private class Grp implements Comparable <Grp> {
		Grp() {}
		void addHit(int hitIdx) {
			hits.add(hitIdx);
		}
		void addBestSeq(int seqIdx) {
			if (!grpSeqs.contains(seqIdx)) grpSeqs.add(seqIdx);
			cntGrpSeq++;
		}
		
		public int compareTo(Grp g) {
			return 0;
		}
		public boolean keep() {
			if (grpNum>0) return true;
			if (cntGrpSeq>1) return true;
			return false;
		}
		int grpNum=0;
		int cntGrpSeq=0;
		Hit bestHitObj;
		Vector  <Integer> hits = new Vector  <Integer> ();
		
		HashSet <Integer> grpSeqs = new HashSet <Integer> (); // Best hits
	}
	 /*******************************************************
	  * Hit
	  *****************************************************/
	private HashMap <Integer, Hit> hitMap = new HashMap <Integer, Hit> ();  // hitIdx, Hit
	
	// This may represent the hitID or descr
	private class Hit implements Comparable <Hit> {
		Hit(int hitIdx, String hitStr, String shortDesc, String desc, int nGO, Grp gObj) {
			this.hitIdx = hitIdx;
			this.nGO = nGO;
			this.gObj = gObj;
			this.hitStr=hitStr;
			this.desc=desc;
		}
		void addBest(int seqIdx,  double eval) {
			if (eval<this.eval) this.eval=eval;
			gObj.addBestSeq(seqIdx);
			bCnt++;
		}
		
		public int compareTo(Hit h) { // for description, find best hit to use as representative
			if (bCnt>h.bCnt) return -1;
			if (bCnt<h.bCnt) return 1;
			if (eval<h.eval) return -1;
			if (eval>h.eval) return 1;
			if (nGO>h.nGO) return -1;
			if (nGO<h.nGO) return 1;
			return 0;
		}
		int bCnt=0;
		
		Grp gObj;				// For hitID, one per Hit; for desc, multiple hits may have this grop
		double eval=10000.0;
		int hitIdx=0, nGO=0;
		String hitStr, desc; // for debugging
	}
	/*******************************************************
	 * Seq
	 *****************************************************/
	private HashMap <Integer, Seq> seqMap = new HashMap <Integer, Seq> (); // seqIdx, Seq
	private class Seq  {
		Seq(int asmIdx, String name, int len, int bestHitIdx) { // need asmIdx, name for file output
			this.asmIdx = asmIdx; 
			this.name = name;
			this.len = len;
			this.bestHitIdx=bestHitIdx;
		}
		
		void addMate(int pairIdx) {mate.add(pairIdx);}
		void addPair(int pairIdx) {if (bAllHits) pair.add(pairIdx);}
		
		int asmIdx;
		
		String name;
		int len=0;
		int bestHitIdx=0;
		HashSet <Integer> mate = new HashSet <Integer> (); // fast search
		HashSet <Integer> pair = new HashSet <Integer> (); // fast search
	}
	/*******************************************************/
	private HashMap <Integer, String> asmMap = new HashMap <Integer, String> ();
	
	 private DBConn cmpDBC;			// database connection
	 private CompilePanel cmpPanel;	// get all parameters from this
	
	 private boolean bSuccess = true;
	 
	 private String prefix;
	 private int GRPid=-1;
	 
	 private boolean bAllHits = true; 	// default = all seqs must have hit
	 private int nHitType = -1; 		// default 1 for description
	 private int covCutoff = -1, simCutoff = -1;
}
