package cmp.compile;

/******************************************************
 * All methods used the class to load groups from file
 * It also is used for a user-defined file to load.
 */
import java.io.*;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cmp.database.Globals;
import cmp.database.Schema;
import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.BestAnno;

public class MethodLoad {
	private static int debug=0; // >0 state out many records to output before dieing
	private static final String uniqueID = Globals.uniqueID;
	private static final String errorID = "Error";
	
	public MethodLoad(DBConn dbc) {
		mDB = dbc;
	}
	/***********************************************************
	 * Called by CompileMain to add a file of orthologs
	 */
	 public boolean run(int idx,  CompilePanel panel) {
		long allTime = Out.getTime();
		Out.PrtDateMsg("\nStart execution of " + Globals.Methods.UserDef.TYPE_NAME);
		
		methodPanel = panel.getMethodPanel();
		String [] settings = methodPanel.getSettingsAt(idx).split(":");	
		groupFile = settings[1];
		cmpPanel = panel;
		prefix = methodPanel.getMethodPrefixAt(idx);
		
		Out.PrtSpMsg(1, "Prefix:  " + prefix);
		Out.PrtSpMsg(1, "File:    " + groupFile);
		Out.PrtSpMsg(1, "");
		
		if (!processGroupFile(idx)) return false;
		
		 Out.PrtMsgTimeMem("Finish execution of User Defined", allTime);

		return bSuccess;
	}
		
	/*****************************************************************
	 * Called by all methods to load the file just generated
	 */
	 public int run(int idx, String file,  CompilePanel panel) {	// CAS305 return GRPid for MethodHit
		groupFile = file;
		cmpPanel = panel;
		methodPanel = panel.getMethodPanel();
		prefix = methodPanel.getMethodPrefixAt(idx);
			
		if (!processGroupFile(idx)) return -1;
		return GRPid;
	}
	 /***************************************************
	  * loadGroupFile
	  */
	 private boolean processGroupFile(int idx) {
		String methodType = methodPanel.getMethodTypeAt(idx);
		String comment =    methodPanel.getCommentAt(idx);
		String settings =   methodPanel.getSettingsAt(idx);
		methodName = Globals.getName(methodType, prefix);
	 
		Out.PrtSpMsg(1, "Loading cluster file: " + groupFile);
		
		File f = new File(groupFile);
		if (!f.exists()) {
			String tfile = groupFile;
			groupFile = cmpPanel.getCurProjAbsDir() + "/" + groupFile;
			f = new File (groupFile);
			if (!f.exists()) {
				Out.PrtError("Cannot find cluster file " + tfile);
				return false;
			}
		}

		// LOAD METHOD, GROUPS and MEMBERS
		step1_loadMethod(methodName, methodType, prefix, comment, settings);
		if (bSuccess) step2_initDSfromDB();
		if (bSuccess) step3_readFileofClusters();
		if (bSuccess) new Pairwise(cmpPanel).saveMethodPairwise(mDB, GRPid, prefix);
		
		return bSuccess;
	 }
	 /**************************************************************
	  * load method into pog_method table of database
	  */
	 private void step1_loadMethod(String name, String method, String prefix, String descript, String settings) {		
	 try {
		if (descript.contains("'")) {
			descript.replace("'", "");
			Out.PrtWarn("Removed quotes from description string: " + descript);
		}
		
		PreparedStatement ps = mDB.prepareStatement("INSERT INTO pog_method " +
				"(PMstr, PMtype, prefix, adddate, description, parameters) " +
				" values(?,?,?,NOW(),?, ?)");
		ps.setString(1, name);
		ps.setString(2, method);
		ps.setString(3, prefix);
		ps.setString(4, descript);
		ps.setString(5, settings);
		ps.execute();
		
		ResultSet rs = mDB.executeQuery( "select last_insert_id() as PMid");
		if (rs.next()) GRPid = rs.getInt("PMid");
		else ErrorReport.die("error getting last GRPid");
		rs.close();

		Schema.addDynamicMethod(mDB, prefix);
	} catch (Exception e) {ErrorReport.die(e, "error getting last PMid");bSuccess=false;}
	}
	 /***************************************************
	  * initialize: seqMap, prefMap, stcwVec from database
	  */
	 private void step2_initDSfromDB() {
	 try {
		File file = new File(groupFile);
		if (!file.exists()) {
			file = new File(cmpPanel.getCurProjAbsDir() + "/" + groupFile);
			if (!file.exists()) {
				Out.PrtError("File does not exist:" + groupFile);
				bSuccess=false;
				return;
			}
		}
			
		nSTCW = mDB.executeCount("SELECT COUNT(*) FROM assembly");

		ResultSet rs = mDB.executeQuery("SELECT ASMid, ASMstr, prefix FROM assembly");
		while (rs.next()) {
			prefMap.put(rs.getString(3), rs.getInt(1)); // prefix from cluster file
			stcwVec.add(rs.getString(2)); 
		}
		
		/*** Get Sequence names and IDs from database ****/
		rs = mDB.executeQuery("SELECT UTid, UTstr FROM unitrans");
		while (rs.next()) {
			seqMap.put(rs.getString("UTstr"), rs.getInt("UTid"));
		}
		rs.close();
		
		return;
	 }
	 catch (Exception e) {ErrorReport.reportError(e, "initialize data structures"); bSuccess=false; }
	 }
	 
	/************************************************************
	* load the groups into pog_groups and group_members
	* orthoMCL format; the prefix of names are ASMstr, example input:
	* D3: dm1|cari_01 dm2|demo_mix_01
	*************************************************************/
	private void step3_readFileofClusters() {
	try {		
		Out.PrtSpMsg(2, "Start adding...");
		String line;
		int group=0, skip=0, add=0, badPre=0, cntUnique=0, cntWithGO=0, cntPrt=0;
		long ts = Out.getTime();
		
		File file = new File(groupFile);
		BufferedReader reader = new BufferedReader ( new FileReader ( file ) );
		String POGsql = step3a_createGroupSQL();	
		
		mDB.openTransaction(); 
		PreparedStatement psMem = mDB.prepareStatement("INSERT INTO pog_members " +
				"(PGid, PGstr, UTid, UTstr) values (?,?,?,?)");
		PreparedStatement psSeq = mDB.prepareStatement("UPDATE unitrans SET " 
				+ prefix + "=? WHERE UTstr=?");
		PreparedStatement psGrp = mDB.prepareStatement("update pog_groups set " +
				"HITid=?, HITstr=?, e_value=?, perAnno=? where PGid=?");
		
		while ((line = reader.readLine()) != null) 
		{
			
			if ( line.length() == 0 || line.charAt(0) == '#' ) continue;  			
			String [] members = line.split(" ");
			if (members == null || members.length < 2) {
				System.err.println("Bad line: " + line);
				continue;
			}
			if (members.length==2) continue;

			group++; cntPrt++;
			if (group > 999999) 
				ErrorReport.die("More than 999999 groups - email tcw@agcol.arizona.edu");
			String PGstr = String.format("%s_%06d", prefix, group);    			
    		
			if (cntPrt==1000) {
				Out.r("Adding cluster #" + group);
				cntPrt=0;
			}
			if (debug>0) {
				Out.prt(">>> " + PGstr);
				if (group>debug) Out.die("Debugging MethodLoad");
			}
			
    	/** compute # of members per sTCW to be loaded with sTCW information **/				
			int cntMem=0;
			int sTCWcnt [] = new int [nSTCW+1]; // count how many from each assm	
			for (int i=0; i<= nSTCW; i++) sTCWcnt[i] = 0;
			
			for (int i=1; i< members.length; i++) { // first is group - ignore
				Matcher m = grpPat.matcher(members[i]);
				if (!m.find()) continue;
				
				String preStr = m.group(1);
				if (prefMap.containsKey(preStr)) {
					int j = prefMap.get(preStr);
					sTCWcnt[j]++;
					cntMem++;
    				}
				else {
					Out.PrtWarn("Invalid prefix: " + preStr);
					badPre++;
					if (badPre>10)  {	
						Out.PrtWarn("Example Line :" + line);
						Out.PrtWarn("Too many bad prefixes -- check file");
						bSuccess=false;
						reader.close();
						return;
					}
					continue;
				}
	    		}
			
	    	/** add group to database -- need PGid for adding members **/
			int PGid = step3b_loadGroup(POGsql, PGstr, GRPid, cntMem, sTCWcnt);
			sTCWcnt = null;
               
    	/** find members and add to database **/  
			add=skip=0;
			Vector <Integer> memIdVec = new Vector <Integer> ();
			for (int i=1; i< members.length; i++) {
				String seqName = "";  				
				Matcher m = grpPat.matcher(members[i]);
				if (m.find()) seqName = m.group(2);
				else  Out.die("Invalid line: " + line + " member: " + members[i]);
				
				int seqID=0;
		    	if (seqMap.containsKey(seqName)) seqID = seqMap.get(seqName); 
		    	else {
		    		if (skip < 5)
		    			Out.PrtWarn("File contains sequence identifer not in database '" + seqName + "'");
				else if (skip==10) Out.PrtWarn("Surpressing further such error messages");
				skip++;
				if (skip>100 && add==0) {
					Out.PrtError("Too many bad indentifiers and no good ones -- check file");
					bSuccess=false;
					reader.close();
					return;
				}
				continue;
		    	}
		    	//"(PGid, PGstr, UTid, UTstr) values
	    		psMem.setInt(1, PGid);
	    		psMem.setString(2, PGstr);
	    		psMem.setInt(3, seqID);
	    		psMem.setString(4, seqName);
	    		psMem.execute();
	    		
	    		psSeq.setString(1, PGstr);
	    		psSeq.setString(2, seqName);
	    		psSeq.execute();
	    		cntSeqs++;
		    		
				memIdVec.add(seqMap.get(seqName));
				add++;
			}
			
    	/** add entry to pog_hits and compute best hit **/
			Desc bestDesc = descriptComputeBest(PGid, memIdVec);
			if (bestDesc!=null) {
				int score = Out.perI(bestDesc.totalCnt, memIdVec.size());
				if (score > 100) score = 100; 
				psGrp.setInt(1,    bestDesc.bestID);
				psGrp.setString(2, bestDesc.bestName);
				psGrp.setDouble(3, bestDesc.bestEval);
				psGrp.setInt(4, score);
				psGrp.setInt(5, PGid);
				psGrp.execute();	
				
				if (bestDesc.bestName.equals(Globals.uniqueID)) cntUnique++;
				if (bestDesc.cntGO>0) cntWithGO++;
			}
			memIdVec.clear();
		} // end while for reading a line containing a cluster
		mDB.closeTransaction(); 
		reader.close(); 
	
		System.err.print("                                                        \r");
		Out.PrtSpCntMsg(3, group, "Total Clusters    " + Out.df(cntSeqs) + " Sequences in clusters");
		Out.PrtSpCntMsgZero(3, cntUnique, "Unique - Clusters without hits");
		Out.PrtSpCntMsgZero(3, cntWithGO, "Best Hit has GO annotation");
		Out.PrtSpMsgTime(2, "Complete adding " + prefix, ts);
	}
	catch (Exception e) {ErrorReport.prtReport(e, "loading POGS"); bSuccess=false;}
	}
	/**
	 * Create string for adding groups, along with assembly columns that have names "A__<assmid>" 
	 **/
	private String step3a_createGroupSQL() {
	try {
		String POGsql = "INSERT INTO pog_groups (PGstr, PMid, count ";
		
		// same as received in columns, so output should be the same order
		for (int i=0; i < stcwVec.size(); i++) {
			String id = Globals.PRE_ASM + stcwVec.get(i);
			if (mDB.tableColumnExists("pog_groups", id))
				POGsql += ", " + Globals.PRE_ASM + stcwVec.get(i);
			else {
				Out.PrtError("No column in cluster table for database with sTCWid '" + stcwVec.get(i) + "'");
				Out.Print("This happens when the mTCW.cfg has an sTCWid inconsistent with the sTCWdb");
				Out.die("Delete database, project and start over");
			}
		}
		
		POGsql += ", taxa) values (";
		return POGsql;
	}
	catch (Exception e) {ErrorReport.die(e, "create Group");  return null;}
	}
	/**********************************************************************
	* Load Group
	*/
	private int step3b_loadGroup(String POGsql, String PGstr, int PMid, int count, int [] sTCWcnt) {
	String strQ="";
	try {
		strQ = POGsql + "'" + PGstr + "'," + PMid + "," + count + ",";
		
		// computing taxa 
		int n1=0, nGT1=0, id=0;
		for (int i=1; i<sTCWcnt.length; i++) {
			strQ += sTCWcnt[i] + ",";
			if (sTCWcnt[i]>0) n1++;
			if (sTCWcnt[i]>1) nGT1++;
		}
		String taxo = n1 + "x1";
		if (nGT1>0) taxo = n1 + "xN";
		
		strQ +=  "'" + taxo + "');";
		mDB.executeUpdate(strQ);
			
		ResultSet rs = mDB.executeQuery( "select last_insert_id() as PGid");
		if (rs.next()) id = rs.getInt("PGid");
		else ErrorReport.die("error getting last PGid");
		rs.close();
		return id;
	} catch (Exception e) {ErrorReport.die(e, "loading group " + PGstr + "\nSQL=" + strQ); return 0;}
	}
	
	/*****************************************************************
	 *  Called from above, and from PairStats for pair description
	 *	List of brief descriptions with count and bestAnno count
	 *		List of HitIDs with that description, with nGO
	 */
	private Desc descriptComputeBest(int grpID, Vector <Integer> seqIDvec) {
		String seqList=null;
		for (int UTid : seqIDvec) {
			if (seqList==null) seqList = " (UTid = " + UTid;
			else seqList += " or UTid = " + UTid;		
		}
		seqList += ")";
		
		return new BestHit().compute(grpID, seqIDvec.size(), seqList); /* COMPUTE */
	}
	// called from pairwise for pairs
	public String [] descriptComputeBest(int grpID, int [] seqIDarr) {
		String seqList=null;
		for (int UTid : seqIDarr) {
			if (seqList==null) seqList = " (UTid = " + UTid;
			else seqList += " or UTid = " + UTid;		
		}
		seqList += ")";
		
		Desc bestDescObj = new BestHit().compute(grpID, seqIDarr.length, seqList); /* COMPUTE */
		
		String [] rt = {"0", Globals.noneID};
		if (bestDescObj.totalCnt>1) {
			rt[0]=bestDescObj.bestID+"";
			rt[1]=bestDescObj.bestName;
		}
		return rt;
	}
	private class BestHit {
		private HashMap <String, Desc> 	descHitMap = new HashMap <String, Desc> ();
		private int grpSize;
		private Desc bestDescObj=null;
		
		public Desc compute(int grpID, int grpSize, String seqList) {
			try {
				this.grpSize = grpSize;
				boolean rc;
				
				rc = loadBestAnnoFromDB(seqList); 	if (!rc) return retVal(errorID);
				if (descHitMap.size()==0) 			
					return retVal(uniqueID); 
				
				rc = loadCntAllAnnoFromDB(seqList); if (!rc) return retVal(errorID);
				
				rc = findBest();           			if (!rc) return retVal(errorID);
				
				return bestDescObj;
			}
			catch (Exception e) {ErrorReport.die(e, "compute Best Hit"); return null;}  
		}
		private Desc retVal(String id) {
			bestDescObj = new Desc();
			bestDescObj.update(0, id, 0.0, 0, false,  false,  "");
			bestDescObj.totalCnt=grpSize;
			return bestDescObj;
		}
		private boolean findBest() {
			try {
				Vector <Desc> listDesc = new Vector <Desc> ();
				for (Desc d : descHitMap.values() ) {
					d.finish();
					listDesc.add(d);
				}
				Collections.sort(listDesc);
				if (debug>0) for (Desc d : listDesc) d.prt();
				bestDescObj=listDesc.get(0);
						
				return true;
			}
			catch (Exception e) {ErrorReport.die(e, "find best"); return false;}  
		}
		
		private boolean loadCntAllAnnoFromDB(String seqList) {
			try {
				// count the ones that have this descript
				ResultSet rs = mDB.executeQuery(
					"SELECT tr.UTid, tr.HITid, uq.description " +
					"FROM unitrans_hits as tr JOIN unique_hits as uq " +
					"WHERE " + seqList + " AND tr.HITid = uq.HITid " +
					"order by tr.UTid");
	
				HashSet <Desc> curHitSet = new HashSet <Desc> (); // there can be duplicate desc; just count once
				int curSeq=0;
				while (rs.next()) {	
					int seqID = rs.getInt(1);
					if (curSeq!=seqID) {
						curHitSet.clear();
						curSeq=seqID;
					}
					int hitID = rs.getInt(2);
					String descript = rs.getString(3).trim();
					String dBrief =   BestAnno.descBrief(descript);
					
					if (descHitMap.containsKey(dBrief)) {
						Desc x = descHitMap.get(dBrief);
						if (!curHitSet.contains(x)) {
							x.cnt(hitID);
							curHitSet.add(x);
						}
					}
				}
				rs.close();
				
				return true;
			}
			catch (Exception e) {ErrorReport.die(e, "read best hits"); return false;}  
		}
		/*******************************************************************/
		private boolean loadBestAnnoFromDB(String grpSeqList) {
			try {
				ResultSet rs = mDB.executeQuery(
						"SELECT tr.HITid, tr.HITstr, tr.e_value, uq.description, uq.dbtype, uq.nGO " +
						"FROM unitrans_hits as tr " +
						"JOIN unique_hits as uq " +
						"WHERE " + grpSeqList + " AND tr.HITid = uq.HITid AND " +
						" (tr.bestAnno=1 OR tr.bestEval=1 OR tr.bestGO=1) " +
						"order by tr.e_value");				// CAS305 added order by
				while (rs.next()) {	
					int hitID = rs.getInt(1);
					String hitStr = rs.getString(2);
					double eval = rs.getDouble(3);
					
					String desc = rs.getString(4).trim();
					if (desc.contains("{ECO ")) desc = desc.substring(0,desc.indexOf("{ECO")); 
					
					String dbType = rs.getString(5);
					boolean bIsSP = (dbType.equals("sp")) ? true : false;
					
					int nGO = rs.getInt(6);
					
					String  dBrief =    BestAnno.descBrief(desc);    // CAS305 moved to BestAnno
					boolean bGoodAnno = BestAnno.descIsGood(dBrief); // CAS305 moved to BestAnno
					//if (debug>0) Out.prt(String.format("%5b %-20s  %-30s", bGoodAnno, dBrief, desc));
					
					if (!descHitMap.containsKey(dBrief)) {
						Desc x = new Desc();
						x.update(hitID, hitStr, eval, nGO, bIsSP, bGoodAnno,  dBrief);
						descHitMap.put(dBrief, x);
					}
					else {
						Desc x = descHitMap.get(dBrief);
						x.update(hitID, hitStr, eval, nGO, bIsSP, bGoodAnno,  dBrief);
					}
				}
				return true;
			}
			catch (Exception e) {ErrorReport.die(e, "Load best Hits: " + grpSeqList); return false;}  
		}
	}
	// Object for each description. Keep counts for all hitIDs that have this description.
	private class Desc implements Comparable <Desc> {
		static final double useSP = 0.90;
		static final double DLOW = 1E-308; 
		
		// Find best hitID for the description and add to count of sequences with this anno
		public void update(int hitID, String name, double eval, int nGO, boolean bsp, boolean bGood, String desc) {
			boolean bAssign=false;
			if (annoCnt==0) { // firstTime
				bIsGood = bGood; 
				bestDesc=desc;
				bAssign=true;
			}
			annoCnt++;
			
			if (!bAssign) { // if eval is close, but new hit is SP or more GOs, replace
				boolean bCheck=true;
				if (bestEval!=eval) {
	    			double ev = (eval==0.0) ? DLOW : eval;
	    			if (ev>cmpEval)         bCheck=false; 
				}
				
				if (bCheck) { 
					if (nGO > cntGO)        bAssign=true;
					else if (bsp && !bIsSP) bAssign=true;
				}
			}
			if (bAssign) {
				bestEval	= eval;
				bestID 		= hitID;
				bestName 	= name;
				bIsSP 		= bsp;
				cntGO 		= nGO;
				
				double thisEv = (bestEval==0.0) ? DLOW : bestEval;
				cmpEval = Math.exp(Math.log(thisEv) * useSP); 
			}
		}
		public void cnt(int hitID) {
			totalCnt++;
		}
		public void finish() {
			cntTA = annoCnt+totalCnt;
		}
		public int compareTo(Desc cur) {
			if      (bIsGood && !cur.bIsGood) 	return -1;
			else if (!bIsGood && cur.bIsGood)	return 1;
			else if (cntTA    > cur.cntTA) 		return -1;
			else if (cntTA    < cur.cntTA) 		return 1;
			else if (bestEval < cur.bestEval) 	return -1;
			else if (bestEval > cur.bestEval) 	return 1;
			else if (bIsSP  && !cur.bIsSP)  	return -1;
			else if (!bIsSP  && cur.bIsSP)  	return 1;
			else if (cntGO 	  > cur.cntGO)  	return -1; // CAS305 was after cntTA
			else if (cntGO    < cur.cntGO)  	return 1;
			return 0;
		}
		public void prt() {
			String msg = String.format("Good=%-5b Sp=%-5b best=%-3d total=%-3d GO=%-3d %.0e (%.0e) %-20s %-30s", 
					bIsGood, bIsSP, annoCnt, totalCnt, cntGO, bestEval, cmpEval, bestName, bestDesc);
			Out.prt(msg);
		}
		private int annoCnt=0; 		// #seqs where this is best anno
		private int totalCnt=0; 	// #seqs that have a hit with this anno
		private int cntTA=0;		// annoCnt+totalCnt (totalCnt includes annoCnt -- this give emphasize)
		
		private double bestEval=100.0, cmpEval=100.0;
		private int bestID=0, cntGO;
		private String bestName="", bestDesc=""; // bestDesc used for debugging
		private boolean bIsSP=false, bIsGood=true; 
	} // End BestHit class
	
	 /***********************************************************/
	 private int cntSeqs=0, nSTCW=0;
	
	 private int GRPid=-1;
	 private DBConn mDB;
	 private CompilePanel cmpPanel;
	 private MethodPanel methodPanel;
	
	 private boolean bSuccess=true;
	 private String prefix, methodName;
	 private String groupFile;
	 
	 private Pattern grpPat = Pattern.compile("(\\S+)\\|(\\S+)"); 
	 private HashMap <String, Integer> 	seqMap = new HashMap <String, Integer> ();
	 private Vector <String> 			stcwVec = new Vector <String> ();
	 private HashMap <String, Integer> 	prefMap = new HashMap <String, Integer> ();
}
