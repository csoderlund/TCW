package cmp.compile;

/***********************************************************
 * best hit Closure - each BBH is clustered with all other hits that result in complete graph.
 * 			and remaining non-BBH are cluster to form complete graphs
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.sql.ResultSet;

import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import cmp.database.Globals;

import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;

public class MethodClosure {
	 private final static String iDELIM = Globals.Methods.inDELIM;
	
	 private final String [] covTypes = {"Either", "Both"};
	 private final String [] strType = {"Amino acid", "Nucleotide"};
	 
	 private String groupFile =  Globals.Methods.Closure.TYPE_NAME;
	 
	 public boolean run(int idx, DBConn db, CompilePanel panel) {
		 
		Out.PrtDateMsg("\nStart execution of " + Globals.Methods.Closure.TYPE_NAME);
		if (!setParams(idx, db, panel)) return false;
		
		long startTime = Out.getTime();
		long allTime = startTime;
		Out.PrtSpMsg(1, "Start processing...");
		
		loadFromDB();
		if (bSuccess)  new BestHitGroups().computeGroups();
		if (bSuccess) writeGroups();
		Out.PrtSpMsgTime(1, "Finish computing " + Globals.Methods.Closure.TYPE_NAME, startTime);
		Out.PrtSpMsg(1, "");
		 
		if (bSuccess) 
			if (new MethodLoad(cmpDBC).run(idx, groupFile, cmpPanel)==-1) // loads the file just written
				bSuccess=false;
				
		Out.PrtMsgTimeMem("Finish execution of " + Globals.Methods.Closure.TYPE_NAME, allTime);
		return bSuccess; 
	}
	
	private boolean setParams(int idx, DBConn db, CompilePanel panel) {
		cmpDBC = db;
		cmpPanel = panel;
		
		MethodPanel theMethod = panel.getMethodPanel();
		prefix = theMethod.getMethodPrefixAt(idx);			// Groups should be prefixed with this
		
		String [] settings = theMethod.getSettingsAt(idx).split(iDELIM);

		if (settings.length<5) {
			Out.PrtError("Incorrect parameters: '" + theMethod.getSettingsAt(idx) + "' - using defaults");
		}
		else {
			covCutoff = Static.getInteger(settings[1].trim());
			covMode = 	Static.getInteger(settings[2].trim());
			simCutoff = Static.getInteger(settings[3].trim());
			type = 		Static.getInteger(settings[4].trim());
		}
		if (covCutoff<0) covCutoff=Static.getInteger(Globals.Methods.Closure.COVERAGE_CUTOFF);
		if (simCutoff<0) simCutoff=Static.getInteger(Globals.Methods.Closure.SIMILARITY);
		if (covMode<0 || covMode>covTypes.length)  covMode=Globals.Methods.Closure.COV_TOGGLE;
		if (type!=0 && type!=1) type = 0;
		
		String root = cmpPanel.getCurProjMethodDir() + groupFile + "." + prefix + "_" + covCutoff + "-" + simCutoff;
		groupFile = root;
		
		Out.PrtSpMsg(1, "Prefix:     " + prefix);
		Out.PrtSpMsg(1, "Coverage:   " + covCutoff + " (" + covTypes[covMode] + ")");
		Out.PrtSpMsg(1, "Similarity: " + simCutoff);
		Out.PrtSpMsg(1, "");
		
		if (type == 1 && panel.getNumNTdb()<=0) {
			Out.PrtWarn("Must have at least one nucleotide singleTCWs to use nucleotide blast - abort Transitive");
			return false;
		}
		return true;
	}
	
	/**********************************************************
	 * Read sequence lengths from database and enter into seqLenMap
	 */
	private void loadFromDB() {
	try {
		Out.PrtSpMsg(2, "Load " + strType[type] + " sequences from database");
				
		String msq = "SELECT UTstr, asmID FROM unitrans";
		ResultSet rs = cmpDBC.executeQuery(msq);
		while (rs.next()) {
			Seq seq = new Seq(rs.getInt(2));
			seqMap.put(rs.getString(1), seq);
		}
		rs.close();		
		Out.PrtCntMsg( seqMap.size(), " Sequences loaded from database");
		
		int cntPass=0, cntMinSim=0, cntMinOlap=0, cntPair=0;
		Out.PrtSpMsg(2, "Load " + strType[type] + " blast pairs from database");
		if (type==0) msq = "select UTstr1, UTstr2, aaEval, aaSim, aaOlap1, aaOlap2, aaBit, aaBest from pairwise where aaSim>0";
		else         msq = "select UTstr1, UTstr2, ntEval, ntSim, ntOlap1, ntOlap2, ntBit, aaBest from pairwise where ntSim>0";
		rs = cmpDBC.executeQuery(msq);
		while (rs.next()) {
			cntPair++;
			String seq1=rs.getString(1);
			String seq2=rs.getString(2);
			double eval = rs.getDouble(3);
			int sim =   (int) (rs.getDouble(4)+0.5); // CAS305 was not rounding
			int olap1 = (int) (rs.getDouble(5)+0.5); // CAS305 was not rounding
			int olap2 = (int) (rs.getDouble(6)+0.5); // CAS305 was not rounding
			int bit   = rs.getInt(7);
			int aaBest = (type==0) ? rs.getInt(8) : 0;
			
			if (sim < simCutoff) {
				cntMinSim++;
				continue;
			}
			else {
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
			}
			cntPass++;
			seqMap.get(seq1).add(seq2);
			seqMap.get(seq2).add(seq1);
			
			Pair pair = new Pair(seq1, seq2, eval, sim, olap1, olap2, bit, aaBest);
			pairList.add(pair);
		}
		rs.close();		
		Out.PrtCntMsg(cntPair, "Loaded pairs");
		Out.PrtCntMsg(cntMinSim, "Failed similarity ");
		Out.PrtCntMsg(cntMinOlap, "Failed coverage");
		Out.PrtCntMsg(cntPass, "Passed Pairs");
		
		rs = cmpDBC.executeQuery("select asmID, prefix from assembly");
		while (rs.next()) asmMap.put(rs.getInt(1), rs.getString(2));
		rs.close();
	}
	catch (Exception e) {ErrorReport.die(e, "load data from DB"); bSuccess=false;}
	}
	
	/***********************************************************
	 * Write grpSeqs into a file with one group per line.
	 * The blast file has the taxo with each name, e.g. Pa|PaRi_0001
	 * So it does not need writing here. 
	 */
	private void writeGroups() {
	try {
		Out.PrtSpMsg(2, "Write clusters to file " + groupFile);			
		PrintWriter outFH = new PrintWriter(new FileOutputStream(groupFile)); 
		int group=1;
		for (int idx : grpMap.keySet()) {
			Vector <String> seqs = grpMap.get(idx).seqNames;
		
			String grp = group+"";
			//String grp = idx+"";
			for (String seq : seqs) {
				if (seq!=null && !seq.equals("")) {
					Seq seqObj = seqMap.get(seq);
					grp += " " + asmMap.get(seqObj.asmID) + "|" + seq;
				}
			}
			outFH.println(grp);
			group++;
		}
		outFH.close();
		Out.PrtSpMsg(3, "Wrote " + (group-1) + " clusters");
	}
	catch (Exception e) {ErrorReport.prtReport(e, "write clusters to file"); bSuccess=false;}
	}
	
	
	private class BestHitGroups {
		private void computeGroups() {
			try {
				Out.PrtSpMsg(2, "Compute cluster");
				Collections.sort(pairList); // BBH are first
				
				int grpMax=1, cnt=0, cntNoJoin=0;
				
				for (Pair pair : pairList) 
				{
					int grp1 = seqMap.get(pair.seq1).grpID;
					int grp2 = seqMap.get(pair.seq2).grpID;
					if (grp1 != 0 && grp1 == grp2) 	continue;      // both in same group
					
					boolean goodGrp1 = (grp1==0) ? false : hasClosureSeqInGrp(pair.seq2, grp1); // seq2 closer with grp1
					boolean goodGrp2 = (grp2==0) ? false : hasClosureSeqInGrp(pair.seq1, grp2); // seq1 closer with grp2
				
					if (goodGrp1 && goodGrp2 && hasClosureGrp1Grp2(grp1, grp2)) { // merge
						int mergeID  = (grp1 < grp2) ? grp1 : grp2;
						int deleteID = (grp1 > grp2) ? grp1 : grp2;
						Vector <String> mergeSeqs = grpMap.get(deleteID).seqNames;
						grpMap.get(mergeID).addSeqs(mergeSeqs);
						grpMap.remove(deleteID);
					}
					else if (goodGrp1 && grp2==0) {
						grpMap.get(grp1).add(pair.seq2);
					}
					else if (goodGrp2 && grp1==0) {
						grpMap.get(grp2).add(pair.seq1);
					}
					else if (grp1==0 && grp2==0) {	
						Grp grp = new Grp(grpMax);  // new
						grp.add(pair.seq1);
						grp.add(pair.seq2);
						grpMap.put(grpMax, grp);
						grpMax++;
					}
					else cntNoJoin++;
					cnt++;
					if (cnt%10==0) Out.r("processed pairs " + cnt);
				}
				System.err.print("                                                             \r");
				
				Out.PrtCntMsg(grpMap.size(), "Total clusters");
				if (cntNoJoin>0) Out.PrtCntMsg(cntNoJoin, "Good pairs not joined in cluster");
				if (grpMap.size()==0) bSuccess=false;
			}
			catch (Exception e) {
				writeGroups();
				ErrorReport.prtReport(e, "compute clusters");
				bSuccess=false;
			}
		}
		
		private boolean hasClosureSeqInGrp(String seqStr, int grpid) {
			Vector <String>  seqs= grpMap.get(grpid).seqNames;
			for (String s : seqs) {
				Seq seq = seqMap.get(s);
				if (!seq.isPair(seqStr)) return false;
			}
			return true;
		}
		private boolean hasClosureGrp1Grp2(int grp1, int grp2) {
			Vector <String>  seqs= grpMap.get(grp1).seqNames;
			for (String s : seqs) {
				if (!hasClosureSeqInGrp(s, grp2)) return false;
			}
			return true;
		}
	} // end BestHitGroups
	
	/*****************************************************
	 * Data structures:
	 * Use seqName instead of integer seqID because write seqName to file to be
	 * consistent with orthoMCL
	 */
	private class Grp {
		public Grp (int id) {
			this.id = id;
		}
		public void add(String s) { 
			seqNames.add(s);
			seqMap.get(s).grpID=id;
		}
		public void addSeqs(Vector <String> list) {
			for (String s : list) {
				seqNames.add(s);
				seqMap.get(s).grpID=id;
			}
		}
		int id;
		Vector <String> seqNames = new Vector <String> ();
	}
	
	private class Seq  {
		Seq(int asmID) {this.asmID = asmID;}
		void add(String seq) {seqPairs.add(seq);}
		boolean isPair(String seq) {return seqPairs.contains(seq);}
		int grpID=0, asmID=0;
		HashSet <String> seqPairs = new HashSet <String> ();
	}
	private class Pair implements Comparable <Pair>{
		public Pair (String seq1, String seq2, double eval, int sim, int olap1, int olap2, int bit, int aaBest) {
			this.seq1=seq1;
			this.seq2=seq2;
			this.eval=eval;
			this.bit = bit;
			this.aaBest=aaBest;
		}
		public int compareTo(Pair p) {
			if (aaBest>p.aaBest) return -1;
			if (aaBest<p.aaBest) return  1;
			if (eval<p.eval)     return -1;
			if (eval>p.eval)     return  1;
			if (bit>p.bit)       return -1;
			if (bit<p.bit)       return  1;
			return 0;
		}
		String seq1, seq2;
		double eval;
		double bit;
		int aaBest;
	}
	 private DBConn cmpDBC;			// database connection
	 private CompilePanel cmpPanel;	// get all parameters from this
	
	 private boolean bSuccess = true;
	 private int type=-1, covMode=-1;
	 private String prefix;
	 private int covCutoff=-1, simCutoff = -1;
	
	 private HashMap <Integer, String> asmMap = new HashMap <Integer, String> ();
	 private HashMap <String, Seq>     seqMap = new HashMap <String, Seq> ();
	 private HashMap <Integer, Grp>    grpMap = new HashMap <Integer, Grp> (); // grpID, list of SeqNames;
	 private Vector <Pair>  pairList =  new Vector <Pair> ();
}
