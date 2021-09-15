package sng.annotator;

import java.sql.PreparedStatement;
/*******************************************************
 * Delete redundant hits from pja_db_unitrans_hits and pja_db_unique_hits
 * The indexes do not change. Update pja_databases.
 * 3. add Good hit & GO
 * 4. add interface
 * 5. add to DoUniProt
 */
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeMap;

import sng.database.Schema;
import util.database.DBConn;
import util.database.HostsCfg;
import util.methods.BestAnno;
import util.methods.ErrorReport;
import util.methods.Out;

public class DoUniPrune {
	private final double useGO1 = 0.7, useGO2 = 0.9; // 1. No GO vs GOs  2. nGO>nGO
	
	private String godbName=null;
	private boolean isAAstcwDB=true, bUseSP=false;
	private int flank=30;
	
	private int pruneType=0; // 0 none 1 alignment 2 description
	private int prtPrune=0;
	private boolean bRestore=false; // command line only. if exists restore, else save.

	public static String tmp_seq="save_unitrans_hits", tmp_hit="save_unique_hits";
	
	public DoUniPrune(DBConn m, String godbName, boolean isAAstcwDB, boolean bUseSP, 
			int flank, int pruneType, boolean bRestore, int prtPrune) {
		tcwDB = m;
		this.godbName = godbName;
		this.isAAstcwDB = isAAstcwDB;
		this.bUseSP = bUseSP;
		this.flank = flank;
		this.pruneType = pruneType;
		this.bRestore=bRestore;
		this.prtPrune=prtPrune;
		
		compute();
	}
	
	public void compute() {
		try {
			String stype = (pruneType==1) ? "alignment" : "description";
			Out.PrtSpDateMsg(2, "Remove same " + stype + " hits");
			
			long time = Out.getTime();
			init(); 			if (!bRC) return;
			
			findDupsPerAnno();  if (!bRC) return;
			Out.PrtSpMsg(0, "");
			
			// Assign best_anno, etc
			DoUniAssign assignObj = new DoUniAssign(tcwDB);
			
			bRC = assignObj.Step2_processAllHitsPerSeq(isAAstcwDB, bUseSP, flank, seqIdxSet); if (!bRC) return;
			
			bRC = assignObj.Step3_saveSpeciesSQLTable(); if (!bRC) return;
			
			updateAssmMsg();
			Out.PrtSpMsgTimeMem(1, "Complete remove and update ", time);
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Computing duplicate hits");}
	}
	/******************************************************
	 * For each annoDB
	 * 		for each seq
	 * 			copy unique to tmp_seq tables
	 */
	private int nTotalUniquePrune=0;
	private int cntKeepAnnoSeqHit=0, cntPruneAnnoSeqHit=0, cntNoRank1=0;
	private HashSet <Integer> 	 keepAnnoDBSet = new HashSet <Integer> (); // Hits to keep
	
	private void findDupsPerAnno() {
		try {
			long time = Out.getTime();
			loadDataFromDB();	if (!bRC) return;
			if (pruneType==0) return; // must read data first so can do Step2
			
			// Loop through anno
			for (int dbIdx : dbIdxMap.keySet()) {
				Out.PrtSpMsg(3, "Process " + dbIdxMap.get(dbIdx));
				if (prtPrune>0) prtHeader();
				
				int cnt=0, cntPrt=0, num=seqIdxSet.size();
				for (int seqID : seqIdxSet) {	
					findSeqDup(dbIdx, seqID);	
					
					if (!bRC) return;
					
					cntPrt++; cnt++;
					if (cntPrt==1000) {
						Out.rp("Process ", cnt, num); 
						cntPrt=0;
					}
				}
			
				Out.PrtSpCntMsg3(4, cntKeepAnnoSeqHit, "Seq hits", 
									cntPruneAnnoSeqHit, "Removed", 
									cntNoRank1, "Updated Rank=1");
				
				deleteUniqueHits(dbIdx); 	if (!bRC) return;
				
				cntKeepAnnoSeqHit=cntPruneAnnoSeqHit=cntNoRank1=0;
				keepAnnoDBSet.clear();
			}
			Out.PrtSpMsg(3, "Final");
			prtTotals();
			Out.PrtSpMsgTimeMem(2, "Complete remove " + String.format("%,d", nTotalUniquePrune) + " unique hits", time);
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Find duplicate hits");}
	}
	/*********************************************************
	 * Prune based on description - HitList order description, bitscore, eval, GOs
	 * Prune based on alignment   - HitList order bitscore, eval, sim, align, GOs
	 * Hence, not by rank.
	 * 	After delete all, sort ArrayList by Rank. If no #1, make first #1 and update.
	 */
	private void findSeqDup(int dbIdx, int seqID) {
		try {
			ArrayList <HitData> hitList = loadHitDataForSeq(dbIdx, seqID);
			if (hitList==null || hitList.size()==0) return;
			
			HashSet <Integer> delSeqHitSet =  new HashSet <Integer> (); // seqHitIdx (pja_db_unitrans_hits.PID)
			HitData lastHD=null;
			
			for (HitData hd : hitList) { 
				if (lastHD==null || !isSame(lastHD, hd)) {
					lastHD = hd;
					cntKeepAnnoSeqHit++;
					continue;
				}
				if (cntPruneAnnoSeqHit<prtPrune) hd.prt(seqID, lastHD);
				cntPruneAnnoSeqHit++;
				
				if (lastHD.isBestGO(hd)) {
					delSeqHitSet.add(hd.seqHitIdx);
					hd.bDelete=true;
				}
				else {
					delSeqHitSet.add(lastHD.seqHitIdx); 
					lastHD.bDelete=true;
					lastHD = hd;
				}
			}
			// find hits that must be kept
			for (HitData hd: hitList) {
				if (!delSeqHitSet.contains(hd.seqHitIdx)) {
					if (!keepAnnoDBSet.contains(hd.hitIdx))
						 keepAnnoDBSet.add(hd.hitIdx);
				}
			}
			
			// CAS332 may be removing rank=1
			Collections.sort(hitList);
			for (HitData hd : hitList) {
				if (hd.rank==1 && !hd.bDelete) break;
				if (hd.rank!=1 && !hd.bDelete) {
					cntNoRank1++;
					tcwDB.executeUpdate("update pja_db_unitrans_hits set blast_rank=1 where PID=" + hd.seqHitIdx);
					break;
				}
			}
			hitList.clear();
			
			// delete redundant hits for this seq; what if is a rank=1
			for (int seqHitIdx : delSeqHitSet) {
				tcwDB.executeUpdate("delete from pja_db_unitrans_hits where PID=" + seqHitIdx);
			}
			delSeqHitSet.clear();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Find duplicate hits"); bRC=false;}
	}
	private boolean isSame(HitData lastHD, HitData hd) {
		if (pruneType==1) return lastHD.isSame(hd);
		else return lastHD.desc.contentEquals(hd.desc);
	}
	
	/************************************************
	 * Delete redundant hits for this DB
	 */
	private void deleteUniqueHits(int dbid) {
		try {
			// get indices for this annoDB
			ArrayList <Integer> allHits = new ArrayList <Integer> ();
			ResultSet rs = tcwDB.executeQuery("select DUHID from pja_db_unique_hits where DBID="+dbid);
			while (rs.next()) allHits.add(rs.getInt(1));
			
			int cntRm = allHits.size() - keepAnnoDBSet.size();
			Out.r("Delete unused unique hits - " + cntRm);
			
			// remove all that are not in keep
			for (int duhid : allHits) {
				if (!keepAnnoDBSet.contains(duhid)) {
					tcwDB.executeUpdate("delete from pja_db_unique_hits where DUHID=" + duhid);
					nTotalUniquePrune++;
				}
			}
			// update overview info
			tcwDB.executeUpdate("update pja_databases  "
					+ "set nTotalHits=" + cntKeepAnnoSeqHit
					+ ",   nUniqueHits="+ keepAnnoDBSet.size()
					+ " where DBID=" + dbid);
			Out.PrtSpCntMsg2(4, keepAnnoDBSet.size(), "Unique hits", cntRm, "Removed");
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Find duplicate hits");}
	}
	/************************************************
	 * Load annoDBs and seq index
	 */
	private void loadDataFromDB() {
		try {
			ResultSet rs = tcwDB.executeQuery("select DBID, dbtype, taxonomy from pja_databases" );
			while (rs.next()) {
				int id = rs.getInt(1);
				String type = rs.getString(2);
				String tax = rs.getString(3);
				dbIdxMap.put(id, type + "-" + tax);
			}
			rs = tcwDB.executeQuery("select CTGID from contig" ); // Called from runSingle - no PID yet
			while (rs.next()) {
				int id = rs.getInt(1);
				seqIdxSet.add(id);
			}
			rs.close();
			
			DBConn goDB=null;
			if (godbName!=null && godbName!="") {
				HostsCfg hosts = new HostsCfg();
				goDB = hosts.getDBConn(godbName);
			}
			if (goDB==null) {
				Out.PrtWarn("No GO database - cannot use GO count for finding best hit to save.");
				return;
			}
			/************************************************
			 * Get goList from goDB
			 * The GO records have not been loaded yet, so need to create goBrief for #GOs
			 */
			Out.PrtSpMsg(3, "Find GO counts from " + godbName + " for UniProt hits");
			tcwDB.executeUpdate("UPDATE pja_db_unique_hits SET goBrief = ''");
			
			ArrayList <GoData>  hitList = new ArrayList <GoData> ();   
			
			int cntHit=0;
			rs = tcwDB.executeQuery("select DUHID, hitID from pja_db_unique_hits" ); 
			while (rs.next()) {
				GoData gObj = new GoData ();
				gObj.duhid = rs.getInt(1);
				gObj.hitName = rs.getString(2);
				gObj.nGO = "";
				hitList.add(gObj);
				cntHit++;
				if (cntHit%1000==0) Out.r("Load hits " + cntHit);
			}
			rs.close();
			
			cntHit=0;
			for (GoData gObj : hitList) {
				rs = goDB.executeQuery("select go from TCW_UniProt where UPid='" + gObj.hitName +"'");
				if (!rs.next()) continue;
				String go = rs.getString(1).trim();
				if (go==null || go.contentEquals("")) continue;
				
				String [] tok = go.split(";");
				gObj.nGO = String.format("#%02d",tok.length);
				
				cntHit++;
				if (cntHit%1000==0) Out.r("Load GOs " + cntHit);
			}
			goDB.close();
			
			int cnt=0, cntSave=0;
			tcwDB.openTransaction(); 
			PreparedStatement ps1 = tcwDB.prepareStatement(
					"UPDATE pja_db_unique_hits SET goBrief = ? WHERE DUHID = ?");
			
			for (GoData gObj: hitList) {	
				if (gObj.nGO=="") continue;
				
				ps1.setString(1, gObj.nGO);
				ps1.setInt(2, gObj.duhid);
				ps1.addBatch();
				
				cnt++; cntSave++;
				if (cntSave==1000) {
					Out.r("Update Hits " + cnt);
					cntSave=0;
					ps1.executeBatch();
				}
			}
			if (cntSave>0) ps1.executeBatch();
			ps1.close();
			tcwDB.closeTransaction(); 
			hitList.clear();
			
			Out.PrtSpCntMsg(4, cnt, "Hits with GOs");
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Load data from DB"); bRC=false;}
	}
	private ArrayList <HitData> loadHitDataForSeq (int dbID, int seqID) {
        ArrayList <HitData> hitList= new ArrayList <HitData> ();
        try {
            String strQ = 	"SELECT " +
            		"uh.DUHID, uh.hitID, uh.description, uh.length, uh.goBrief, " +
            		"sh.PID, sh.e_value, sh.bit_score, sh.percent_id, sh.alignment_len, " +
            		"sh.gap_open, sh.mismatches, " +
            		"sh.ctg_start, sh.ctg_end, sh.prot_start, sh.prot_end, sh.blast_rank " +
		    		"FROM pja_db_unique_hits   as uh " +
		    		"JOIN pja_db_unitrans_hits as sh " +
		    		"ON sh.DUHID = uh.DUHID " +
		    		"WHERE sh.CTGID = " + seqID + " and uh.DBID= " + dbID + " ";
            
            if (pruneType==1)// DIAMOND does not order by percent_id, which is important for isSame
            	strQ += "order by sh.bit_score DESC, sh.e_value ASC, sh.percent_id DESC, sh.alignment_len DESC, uh.goBrief DESC"; 
            else 	
            	strQ += "order by uh.description, sh.bit_score DESC, sh.e_value ASC, uh.goBrief DESC"; 
            
            ResultSet rset = tcwDB.executeQuery( strQ );
            while( rset.next() ) {	
            	int i=1;
            	HitData hit = new HitData ();
                hit.hitIdx = 	rset.getInt(i++);
                hit.hitID = 	rset.getString(i++);
                hit.desc = 		rset.getString(i++).trim().toLowerCase();
                if (hit.desc.contains("{") && hit.desc.endsWith("}")) // e.g. ZF-HD family protein {ORGLA09G0180300.1} 
                	hit.desc = hit.desc.substring(0, hit.desc.indexOf("{"));
                hit.length = 	rset.getInt(i++);
                
                String goBrief = rset.getString(i++);
                if (goBrief==null ) goBrief = "";
                else goBrief = goBrief.trim();
                
                try {	
                	if (goBrief=="" || goBrief.length()<=1) hit.nGO = 0;
                	else hit.nGO = 	Integer.parseInt(goBrief.substring(1));
                } catch (Exception e) {Out.die("cannot parse '" + goBrief + "'" + " for " + hit.hitID);}
                
                hit.seqHitIdx 	= rset.getInt(i++);
                hit.eVal 		= rset.getDouble(i++); 
                hit.bitScore 	= rset.getDouble(i++);
                hit.sim 		= rset.getDouble(i++);
                hit.align 		= rset.getInt(i++);
                hit.gap 		= rset.getInt(i++);
                hit.mm 			= rset.getInt(i++);
		        hit.seqStart 	= rset.getInt(i++);
		        hit.seqEnd 		= rset.getInt(i++); 
		        hit.hitStart 	= rset.getInt(i++);
		        hit.hitEnd 		= rset.getInt(i++); 
		        hit.rank		= rset.getInt(i++);
		        
		        hit.isGood = BestAnno.descIsGood(hit.desc); 
		        
		        hitList.add(hit);
	    	}
            if ( rset != null ) rset.close();
            
	    	return hitList;
        }
        catch(Exception e) {ErrorReport.die(e, "Reading database for hit data");}	
        return null;
	}
	private void prtTotals() {
		try {
			int cntSeqHit = tcwDB.executeCount("select count(*) from pja_db_unitrans_hits");
			int cntUnique = tcwDB.executeCount("select count(*) from pja_db_unique_hits");
			Out.PrtSpCntMsg2(4, cntSeqHit, "Seq hits", cntUnique, "Unique hit");
		}
		catch(Exception e) {ErrorReport.prtReport(e, "print totals"); }
	}
	private void init() { 
		try {
			if (tcwDB.tableExists("pja_uniprot_go")) { // do first case they say no
				if (!runSTCWMain.yesNo("Must delete current GO assignments before continuing")) // should only happen from command line
						Out.die("Cannot continue with GOs existing in database");
				Schema.dropGOtables(tcwDB);
			}
			
			if (tcwDB.tableColumnExists("assem_msg", "anno_msg"))
		 		tcwDB.executeUpdate("update assem_msg set anno_msg=''");
			
			if (bRestore) {
				if (tcwDB.tableExist(tmp_seq)) {
					Out.PrtSpMsg(3, "Restore original hits from " + tmp_seq + " and " + tmp_hit);
					copyTable(tmp_seq, "pja_db_unitrans_hits");
					copyTable(tmp_hit, "pja_db_unique_hits");
					
					if (pruneType==0) {
						Out.PrtSpMsg(1, "Original tables restored and saved tables dropped");
						tcwDB.tableDrop(tmp_seq);
						tcwDB.tableDrop(tmp_hit);
					}
				}
				else {
					Out.PrtSpMsg(3, "Create tables " + tmp_seq + " and " + tmp_hit);
					createTable(tmp_seq, "pja_db_unitrans_hits");
					createTable(tmp_hit, "pja_db_unique_hits");
				}
			}
			Out.PrtSpMsg(3, "Initial");
			prtTotals();
		}
		catch (Exception e) {
			bRC = false;
			ErrorReport.prtReport(e, "Init remove redundant");
		}
	}
	private boolean createTable(String new_tbl, String old_tbl) {
		try {
			tcwDB.executeUpdate("create table " + new_tbl  + " like "          + old_tbl);
			tcwDB.executeUpdate("insert "       + new_tbl  + " select * from " + old_tbl);
			return true;
		}
		catch(Exception e) {ErrorReport.die(e, "Computing duplicate hits"); return false;}
	}
	private boolean copyTable(String tmp_tbl, String orig_tbl) {
		try {
			tcwDB.tableDrop(orig_tbl);
			createTable(orig_tbl, tmp_tbl);
			return true;
		}
		catch(Exception e) {ErrorReport.die(e, "Computing duplicate hits"); return false;}
	}
	private void updateAssmMsg() {
		try {
			if (!tcwDB.tableColumnExists("assem_msg", "prune")) 
				tcwDB.tableCheckAddColumn("assem_msg", "prune", "tinyint default -1", null);
			tcwDB.executeUpdate("update assem_msg set prune = " + pruneType);
		}
		catch(Exception e) {ErrorReport.die(e, "Set prune type in DB"); }
	}
	private class GoData {
		int duhid;
		String hitName;
		String nGO;
	}
	private class HitData implements Comparable <HitData>{
  		int seqHitIdx, hitIdx; // pja_db_unitrans_hits.pid, pja_db_unique_hits.duhid
  		String hitID, desc;
  		double eVal, bitScore, sim;
  		int align, mm, gap, seqStart, seqEnd, hitStart, hitEnd, length;
  		int nGO, rank;
  		boolean isGood, bDelete=false;
  		
  		public int compareTo(HitData x) {
  			if (rank<x.rank) return -1;
  			if (rank>x.rank) return  1;
  			return 0;
		}
  		boolean isSame(HitData hd) { // GOs, Desc, Species can be different when everything else is the same
  			if (hd.bitScore!=bitScore) 	return false;
  			if (hd.eVal!=eVal) 			return false;
  			if (hd.sim!=sim) 			return false;
  			if (hd.align!=align) 		return false;
  			if (hd.gap!=gap) 			return false;
  			if (hd.mm!=mm) 				return false;
  			if (hd.seqStart!=seqStart) 	return false;
  			if (hd.seqEnd!=seqEnd) 		return false;
  			if (hd.hitStart!=hitStart) 	return false;
  			if (hd.hitEnd!=hitEnd) 		return false;
  			if (hd.length!=length) 		return false;
  			if (!hd.desc.equals(desc))  return false; // XX
  			return true;
  		}
  		
  		boolean isBestGO(HitData hd) { // for same alignment
  			if (hd.isGood && !isGood) return false; // XX
  			
  			if (hd.nGO>0 && nGO==0) {
    			double aDiff = bitScore * useGO1; 
    			
    			if (hd.bitScore >= aDiff) return false;
			}
  			else if (hd.nGO>nGO+1) {
  				double aDiff = bitScore * useGO2; 
    			
    			if (hd.bitScore >= aDiff) return false;
  			}
  			return true;
  		}
  		void prt(int seqID, HitData lasthd) {
  			String e = String.format("%.1E", eVal);
  			String s1 = String.format("%d-%d", seqStart, seqEnd);
  			String s2 = String.format("%d-%d", hitStart, hitEnd);
  			String s3 = (lasthd==null) ? "" : "(" + lasthd.hitID + " " + lasthd.nGO + ")";
  			String d = (desc.length()>30) ? desc.substring(0,29) : desc;
  			String x = String.format("%5d: %-16s %6.0f %9s %3.0f%s %5d %3d %10s %10s %3d %6b   %-30s %s", 
  				seqID, hitID, bitScore, e, sim, "%", align, gap, s1, s2, nGO, isGood, d, s3);
  			Out.prt(x);
  		}
    }
	private void prtHeader() {
		String x = String.format("%5s: %-16s %6s %9s %3s%s %5s %3s %10s %10s %3s %6s   %-30s %s", 
  				"seq#", "hitID", "Bit", "eVal", "Sim", "%", "Align", "Gap", "ss-se", "hs-he", "nGO", 
  				"isGood", "Description (30 char max)", "Keep hitID nGO");
		Out.prt(x);
	}
	private TreeMap <Integer, String> 	dbIdxMap = new TreeMap <Integer, String> (); // annoDBs
	private HashSet <Integer>  			seqIdxSet = new HashSet <Integer> ();
	
	private boolean bRC=true;
	private DBConn tcwDB;
}
