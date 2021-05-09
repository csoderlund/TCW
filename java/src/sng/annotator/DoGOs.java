package sng.annotator;
/****************************************************
 * Adds GO annotation to a sTCW database. Uses the go_ database.
 * CAS323 break makeGoTables into multiple methods
 */
import java.io.BufferedReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import sng.database.MetaData;
import sng.database.Schema;
import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.file.FileC;
import util.file.FileHelpers;
import util.file.FileVerify;
import util.methods.ErrorReport;
import util.methods.Out;

public class DoGOs 
{ 
	static private final String goMetaTable = Globalx.goMetaTable;// CAS318 were prefixed with PAVE_
	static private final String goUpTable =   Globalx.goUpTable;  // "TCW_UniProt"
	static private final int NLOOP=1000; 
	
	public DoGOs(CoreDB sqlObj, String godbName, String slimSubset, String slimOBOFile) {
		try {
			Out.Print("");
			Out.PrtDateMsg("Start GO update");
			long startTime = Out.getTime();
			
			HostsCfg hosts = new HostsCfg();
			tcwDB = hosts.getDBConn(sqlObj.STCWdb);
			
			int cnt = tcwDB.executeCount("select count(*) from pja_db_unique_hits");
			if (cnt==0) {
				Out.PrtWarn("No annoDB proteins in database");
				return;
			}
			this.godbName = godbName;
			
			goDB = hosts.getDBConn(godbName);
			
			godbsrc = tcwDB.executeString("Select go_msg from assem_msg"); // CAS318 - use for messages instead of godbName
			if (godbsrc==null || godbsrc=="") godbsrc = "Unknown GO source";
			else godbsrc = godbsrc.substring(0, godbsrc.indexOf(" ")+1); // Just get version
			
			if (!makeGoTables()) return;
			
			if (slimSubset!=null && !slimSubset.equals("")) loadSlimSubset(slimSubset);
			else if (slimOBOFile!=null && !slimOBOFile.equals("")) loadSlimOBOFile(slimOBOFile);
			
			tcwDB.close(); goDB.close();
			Out.PrtMsgTime("Finish GO update", startTime);
		}
		catch(Exception e){ErrorReport.die(e, "Error adding GO tables");}
	}
	/***********************************************************************
	 * Create TCW go tables
	 */
	private boolean makeGoTables() {
		try {
			// Tables used from GO database - must prefix with database name for transfers
			tab_goGraphPath = 	godbName + ".graph_path";
			tab_goTerm = 		godbName + ".term";
			tab_goTerm2term = 	godbName + ".term2term";
			tab_goTCWup = 		godbName + "." + goUpTable;
			
			if (!hasAnnotation(tcwDB)) {
				Out.PrtError("GO was specified but there is no annotation!");
				return false;
			}
			if (!goDB.tableExists(goUpTable)) {// note, db.tableExists doesn't see this table which is in the other DB ??
				Out.PrtError("Cannot add GO tables because " + tab_goTCWup + " does not exist - runAS");
				return false;
			}
			if (!goDB.tableExists("graph_path")) {
				Out.PrtError("Cannot add GO tables because " + tab_goGraphPath + " does not exist.");
				return false;
			}
			
			/** Create Tables **/
			Schema.createGOtables(tcwDB); // drops current 
			
			tcwDB.executeUpdate("update pja_db_unique_hits " +
				" set ec='', pfam='', kegg='', goList='', goBrief='', interpro=''");
			tcwDB.executeUpdate("update contig set PIDgo=0");
			tcwDB.executeUpdate("update pja_db_unitrans_hits set filter_gobest=0");
			tcwDB.executeUpdate("update assem_msg set go_msg='', go_slim=''");
			if (tcwDB.tableColumnExists("libraryDE", "goMethod"))
				tcwDB.executeUpdate("update libraryDE set goMethod='', goCutoff=-1.0");
			
			if (!do1_assem_msg()) return false;
			if (!do2_pja_db_unique_hits()) return false;
			if (!do3_pja_db_uniprot_go()) return false;
			if (!do4_pja_unitrans_go()) return false;
			if (!do5_update_sequences()) return false; // contig and pja_db_unitrans_hits
			if (!do6_graph_path()) return false;
			if (!do7_go_info()) return false;
			if (!do8_update_evc()) return false;
		
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error adding GO tables");}
		return false;
	}
	private static boolean hasAnnotation(DBConn db) {
		try {
			return db.tableExists("pja_db_unitrans_hits");
		}
		catch(Exception e){ErrorReport.die(e, "Error finding go uniprot table in database");}
		return false;
	}
	
	public static DBConn connectToGODB(String host, String user, String pass, String godb) {		
		DBConn goDB = null;
		try {
			goDB = new DBConn(host, godb, user, pass);
		}
		catch(Exception e) {Out.PrtWarn("Could not connect to GO database " + godb);}
		return goDB;
	}
	
	/** Create Overview message for processing info: assem_msg.go_msg **/
	private boolean do1_assem_msg() {
		try {
			String msg = "GOdb: " + godbName + "  [GOs added with " + Globalx.sTCWver + "]"; // CAS318 added GOdb
			if (goDB.tableExists(goMetaTable)) {
				String file = goDB.executeString("select filename from " + goMetaTable);
				if (file!=null) msg = file + "  " + msg; 
			}
			// CAS319 - changed from having isa and partof to go_rtypes; used in Basic.GOtree
			ResultSet rs = goDB.executeQuery("select id, name from term  where term_type='external' order by id");
			String tMsg="";
			while (rs.next()) {
				tMsg += rs.getInt(1) + ":" + rs.getString(2) + ";";
			}
			rs.close();
			
			tcwDB.executeUpdate("update assem_msg set go_msg='" + msg + "', go_rtypes='" + tMsg + "'");
			
			Out.PrtSpMsg(1, "Computing GOs for:");
			nSeq = tcwDB.executeCount("select count(*) from contig");
			nHit = tcwDB.executeCount("select count(*) from pja_db_unique_hits");
			prtCntMsg(nSeq, "Sequence");
			prtCntMsg(nHit, "Unique hits");
			
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error adding assem_msg");}
		return false;
	}
	
	/***********************************************
	 * pja_db_unique_hits -- add kegg, pfam, ec  
	 ************************************************/
	private boolean do2_pja_db_unique_hits() {
		try {	
			Out.PrtSpMsg(1, "Add GO/Interpro/Kegg/Pfam/EC to unique hits table");
			int s = goDB.executeCount("select count(*) from " + goUpTable); // CAS318 add count - if many UniProts...
			Out.PrtSpMsg(2, "Transferring data from a table with " + Out.df(s) + " entries");
			if (s>5000000)
				Out.PrtSpMsg(3, "There will be no terminal status for this step - please be patient...");
			
			long time = Out.getTime();
			if (goDB.tableColumnExists(goUpTable, "interpro")) {// backwards compatible -- added interpro to V4 goDBs.
				tcwDB.executeUpdate("update pja_db_unique_hits as tw, " + tab_goTCWup + " as g " + 
				" set tw.kegg=g.kegg, tw.pfam=g.pfam, tw.ec=g.ec, tw.interpro=g.interpro, tw.goList=g.go " +
				" where g.upid=tw.hitid");
			} else {
				tcwDB.executeUpdate("update pja_db_unique_hits as tw, " + tab_goTCWup + " as g " + 
				" set tw.kegg=g.kegg, tw.pfam=g.pfam, tw.ec=g.ec, tw.goList=g.go " +
				" where g.upid=tw.hitid");
			}
			int ng = tcwDB.executeCount("select count(*) from pja_db_unique_hits where goList!=''");
			int ni = tcwDB.executeCount("select count(*) from pja_db_unique_hits where interpro!=''");
			int nk = tcwDB.executeCount("select count(*) from pja_db_unique_hits where kegg!=''");
			int np = tcwDB.executeCount("select count(*) from pja_db_unique_hits where pfam!=''");
			int ne = tcwDB.executeCount("select count(*) from pja_db_unique_hits where ec!=''");
			
			prtCntMsg(ng, "GO");
			prtCntMsg(ni, "Interpro");
			prtCntMsg(nk, "KEGG");
			prtCntMsg(np, "PFam");
			prtCntMsgMem(ne, "EC", time);
			
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error adding unique hits");}
		return false;
	}
	/************************************************************
	 * pja_db_unique_hits.goBrief is #gos per hit
	 * pja_uniprot_go: direct mappings only 
	 */
	private boolean do3_pja_db_uniprot_go() {
		try {
			/** build hitGoMap and goAllSet - direct hits only **/	
			Out.PrtSpMsg(1, "Build Hit-GO table");
			Out.PrtSpMsg(2, "Get Hits  ");
			long time = Out.getTime();
			
		/* Create hitGoMap of all assigned */
			ResultSet rs = tcwDB.executeQuery("select DUHID, hitID, goList from pja_db_unique_hits where goList!=''");
			while (rs.next())
			{
				int duhid = rs.getInt(1);
				hitGoMap.put(duhid, new Hit2GO(rs.getString(2), rs.getString(3))); // CREATE hit list of assigned GOs
			}
			rs.close();
			prtCntMsgMem(hitGoMap.size(), "hits to process", time);
			
		/* Set unique_hits #n GO per annoDB hit, create goAllSet of all assigned */
			Out.PrtSpMsg(2, "Hit to GO mapping                       ");
			int cnt=0, cntSave=0;
			time = Out.getTime();
			tcwDB.openTransaction(); 
			PreparedStatement ps1 = tcwDB.prepareStatement(
					"UPDATE pja_db_unique_hits SET goBrief = ? WHERE goList!='' and DUHID = ?");
			
			for (int duhid : hitGoMap.keySet()) {			
				Hit2GO hitGoObj = hitGoMap.get(duhid);
				String [] golist = hitGoObj.goList.split(";");
				String goBrief= String.format("#%02d",golist.length);
				ps1.setString(1, goBrief);
				ps1.setInt(2, duhid);
				ps1.addBatch();
				cnt++; cntSave++;
				if (cntSave==NLOOP) {
					Out.r("Update Hits " + cnt);
					cntSave=0;
					ps1.executeBatch();
				}
				
				for (String go : golist) { 
					String [] tok = go.split(":");
					if (tok.length<3) ErrorReport.die("Tok: " + go);
					int gonum = Integer.parseInt(tok[1]);
					
					hitGoObj.add(gonum, tok[2]);                        //  Hit2GO assigned GOs
					if (!goAllSet.contains(gonum)) goAllSet.add(gonum); // CREATE hash of all GOs
				}
				hitGoObj.goList = null; // don't need anymore
			}
			if (cntSave>0) ps1.executeBatch();
			ps1.close();
			tcwDB.closeTransaction(); 
			Out.r("                                               ");
			prtCntMsgMem(goAllSet.size(), "assigned GOs", time);

		/* pja_uniprot_go: direct mappings only */ 
			Out.PrtSpMsg(2, "Insert into Hit-GO table...");
			time = Out.getTime();
			cnt=cntSave=0;
			tcwDB.openTransaction(); 
			ps1 = tcwDB.prepareStatement(
				      "INSERT into pja_uniprot_go SET DUHID = ?, gonum=?, EC=?");
			for (int duhid : hitGoMap.keySet()) {				// hitGoMap
				Hit2GO hitObj = hitGoMap.get(duhid);
				for (int go : hitObj.goMap.keySet()) {
					ps1.setInt(1, duhid);
					ps1.setInt(2, go);
					ps1.setString(3, hitObj.goMap.get(go));
					ps1.addBatch();
					cnt++; cntSave++;
					if (cntSave == NLOOP) { 
						ps1.executeBatch();
						cntSave=0;
						Out.r("Insert Hit-GO " + cnt);
					}
				}
			}
			if (cntSave>0) ps1.executeBatch();
			ps1.close();
			tcwDB.closeTransaction();
			Out.r("                                               ");
			prtCntMsgMem(cnt, "Hit-GO pairs", time);
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error adding unique hits");}
		return false;
	}
	/***************************************************
	 * add ancestors to hitGoMap and goAllSet
	 * pja_unitrans_go: find GOs per sequence. assign best Hit for each GO 
	 ***************************************************/
	private boolean do4_pja_unitrans_go() {
		try {
		/** add ancestors to hitGoMap and goAllSet**/
			Out.PrtSpMsg(2, "Find all inherited...                     ");
			long time = Out.getTime();
			int cnt=0, cntSave=0;
			ResultSet rs;
			HashMap <Integer, String> ancMap = new HashMap <Integer, String> ();
			
			for (int hitIndex : hitGoMap.keySet()) {			
				Hit2GO hitGoObj = hitGoMap.get(hitIndex);
		
				for (Integer gonum : hitGoObj.goMap.keySet()) {
					String ec = "(" + hitGoObj.goMap.get(gonum) + ")";
					rs = tcwDB.executeQuery("select ancestor from " + tab_goGraphPath +
							" where child=" + gonum);
					while (rs.next()) {
						int gonum2 = rs.getInt(1);
						ancMap.put(gonum2, ec);	
					}
					rs.close();
				}
				for (int gonum2 : ancMap.keySet()) {
					if (!goAllSet.contains(gonum2))   
						goAllSet.add(gonum2);			
					if (!hitGoObj.goMap.containsKey(gonum2)) 
						hitGoObj.goMap.put(gonum2, ancMap.get(gonum2)); // add inherited
				}
				ancMap.clear();
				cnt++;
				if (cnt%NLOOP==0) Out.r("Find Hit-GO  " + cnt);
			}
			System.err.print("                                                         \r");
			prtCntMsgMem(goAllSet.size(), "assigned and inherited GOs", time);
			
		/** find GOs per sequence. assign best Hit for each GO **/
			// For each hit 
			Out.PrtSpMsg(1, "Build Seq-GO table ...           ");
			time = Out.getTime();
			cnt=cntSave=0;
			int cntHas=0, cntNot=0, cntZero=0;
			
			Vector <Integer> ctgSet = new Vector <Integer> ();
			rs = tcwDB.executeQuery("select ctgid from contig");
			while (rs.next()) ctgSet.add(rs.getInt(1));
			rs.close();
			
			HashMap <Integer, goSeqInfo> goSeqMap = new HashMap <Integer, goSeqInfo> ();
			
			tcwDB.openTransaction();
			PreparedStatement ps1 = tcwDB.prepareStatement("INSERT into pja_unitrans_go SET " +
				"CTGID=?, gonum=?, bestDUH=?, bestEval=?, bestEV=?, bestAN=?, direct=?, EC=?");
			
			Out.PrtSpMsg(2, "Insert into Seq-GO table...    ");
			
			for (int ctgid : ctgSet) {					
				boolean hasBest=false, isFirst=true;
				// get all GOs for sequence
				// 	get hitIDs. For each hitID, go through its GOs.
				// 		only add to seq-go list if not exist.
				rs = tcwDB.executeQuery("select " +
					" PID, DUHID, filter_best, filter_ovbest, e_value " +
					" from pja_db_unitrans_hits where CTGID=" + ctgid +
					" order by bit_score DESC,  filter_best DESC, filter_ovbest DESC, e_value ASC ");
					// CAS317 " order by e_value ASC,  filter_best DESC, filter_ovbest DESC, bit_score DESC");
				while (rs.next()) {
					int hitIndex = rs.getInt(2);    				 // pja_db_unique_hit id
					if (!hitGoMap.containsKey(hitIndex))  continue;   // no GO for hit
					Hit2GO hitGoObj = hitGoMap.get(hitIndex);
					
					for (int gonum: hitGoObj.goMap.keySet()) { 		// hitObj assigned and inherited
						String ec = hitGoObj.goMap.get(gonum);
						
						// direct=true may not coincide with bestHit
						if (goSeqMap.containsKey(gonum)) {
							if (!ec.startsWith("("))  {
								goSeqInfo gi = goSeqMap.get(gonum);
								gi.direct=true;
							}
							continue; 
						}
									
						int seqHitIndex = rs.getInt(1); 				// pja_db_unitrans_hit id (seq-hit pair)
						boolean ev=rs.getBoolean(3);
						boolean an=rs.getBoolean(4);
						double eval=rs.getDouble(5);		
					
						goSeqMap.put(gonum, new goSeqInfo(hitIndex, ev, an, eval, ec)); // Seq-BestHit with GO 	
						
						if (isFirst) {
							if (ev || an) hasBest=true;
							isFirst=false;
							seqPIDgoMap.put(ctgid, seqHitIndex); 
						}
					}
				}
			
				// update pja_unitrans_go for that sequence/ctgid
				for (int gonum : goSeqMap.keySet()) {
					goSeqInfo gi = goSeqMap.get(gonum);
					
					ps1.setInt(1, ctgid);
					ps1.setInt(2, gonum);
					ps1.setInt(3, gi.duhid);
					ps1.setDouble(4, gi.eval);
					ps1.setBoolean(5, gi.bestEV);
					ps1.setBoolean(6, gi.bestAN);
					ps1.setBoolean(7, gi.direct);
					ps1.setString(8, gi.ec);
					ps1.addBatch();
					cntSave++;cnt++; 
					if (cntSave==1000) {
						ps1.executeBatch();
						cntSave=0;
						Out.r("Insert Seq-GO " + cnt);
					}
				}
				if (goSeqMap.size()>0) {
					goSeqMap.clear();
					if (hasBest) cntHas++;
					else cntNot++;
				} else cntZero++;
			} // end loop through sequences
			if (cntSave>0) ps1.executeBatch();
			ps1.close();
			tcwDB.closeTransaction();
			
			prtCntMsg(cntHas, "sequences have bestBits or bestAnno with GOs ");
			prtCntMsg(cntNot, "sequences do not have bestBits or bestAnno with GOs ");
			prtCntMsgMem(cntZero,"sequences have no GO ", time);
			
			hitGoMap.clear();
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error adding sequence gos");}
		return false;
	}
	private class goSeqInfo { // Used for above method for goSeqMap 
		public goSeqInfo(int d, boolean e, boolean a, double ev, String c) {
			duhid=d; 
			bestEV=e; 
			bestAN=a; 
			eval=ev;
			ec=c;
			if (!ec.startsWith("(")) direct=true;
		}
		int duhid;
		boolean bestEV=false, bestAN=false, direct=false;
		double eval;
		String ec;
	}	
	private class Hit2GO { // created in do3 and used in do4
		public Hit2GO(String n, String g) {goList=g;}
		public void add(int g, String e) {
			if (!goMap.containsKey(g)) goMap.put(g, e);
		}
		String goList;
		HashMap <Integer, String> goMap = new HashMap <Integer, String> (); // GO, evidence code
	}
	/***************************************************
	 * contig with PIDgo
	 * pja_db_unitrans_hits with filter_gobest and update best_rank
	 */
	private boolean do5_update_sequences() {
		try {
			Out.PrtSpMsg(2, "Update database with best Hit with GO per sequence...           ");
			long time = Out.getTime();
			int cnt=0;
			tcwDB.openTransaction();
			PreparedStatement ps1 = tcwDB.prepareStatement("UPDATE contig SET PIDgo = ?  WHERE CTGID=?");
			
			for (int ctgid : seqPIDgoMap.keySet()) {
				ps1.setInt(1, seqPIDgoMap.get(ctgid));
				ps1.setInt(2, ctgid);
				ps1.execute();
				cnt++;
				if (cnt % NLOOP == 0) Out.r("Update Seq " + cnt);
			}
			ps1.close();
			tcwDB.closeTransaction();
			
			cnt=0;
			tcwDB.openTransaction();
			ps1 = tcwDB.prepareStatement("UPDATE pja_db_unitrans_hits SET filter_gobest = 1  " +
					"WHERE PID=?");
			for (int ctgid : seqPIDgoMap.keySet()) {
				ps1.setInt(1, seqPIDgoMap.get(ctgid));
				ps1.execute();
				cnt++;
				if (cnt % 1000 == 0) Out.r("Update Seq-hits " + cnt);
			}
			ps1.close();
			tcwDB.closeTransaction();
			Out.r("                                               ");
			
			// CAS303 change rank to best_rank for Mysql v8
			tcwDB.executeUpdate("update pja_db_unitrans_hits " +
					"set best_rank=(filter_best+filter_ovbest+filter_gobest)");
			prtCntMsgMem(cnt,"update sequences with best Hit with GO", time);
			seqPIDgoMap.clear();
			
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error adding sequence");}
		return false;
	}
	/********************************************************
	 * go_graph_path
	 * go_term2term
	 */
	private boolean do6_graph_path(){
		try {
			/** go_graph_path and go_term2term **/
			Out.PrtSpMsg(1, "Build GO tables");
			
			Out.PrtSpMsg(2, "Create graph_path from " + godbName + " for GOs");
			long time = Out.getTime();
			int cnt=0;
			tcwDB.openTransaction();
			for (int gonum : goAllSet) { 						// goAllSet
				tcwDB.executeUpdate("insert into " +
					" go_graph_path (relationship_type_id, distance,  child, ancestor) " +
					" select g.relationship_type_id, g.distance, g.child, g.ancestor " +
					" from " + tab_goGraphPath + " as g " +
					" where ancestor>0 and child=" + gonum);
			
				tcwDB.executeUpdate("insert into " +
					" go_term2term (relationship_type_id, child, parent) " +
					" select g.relationship_type_id, g.child, g.parent " +
					" from " + tab_goTerm2term + " as g " +
					" where parent>0 and child=" + gonum);
				cnt++;
				if (cnt % 1000 == 0) Out.r("Insert GO " + cnt);
			}
			tcwDB.closeTransaction();
			prtCntMsgMem(cnt,"processed", time);
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error graph path");}
		return false;
	}
	/************************************************************
	 *  XXX go_info table -- holds information about each direct and ancestor GO 
	 *************************************************************/
	private boolean do7_go_info(){
		try {	 
			// if UniProt has a GO that is not in the GO database, causes an inconsistency
			PreparedStatement ps2 = tcwDB.prepareStatement("INSERT into go_info SET " +
					"gonum=?, descr=?, term_type=?, level=?, bestEval=?, nUnitranHit=?, slim=?");

			String [] types = Globalx.GO_TERM_LIST; 
			
			Out.PrtSpMsg(2, "Create GO information table");
			long time = Out.getTime();
			int cnt=0, cntSave=0;
			int cntNotFound=0;
			tcwDB.openTransaction();
			for (int gonum : goAllSet)  							// goAllSet
			{	
				// from tcwDB pga_unitrans_go
				double eval = -2.0;
				ResultSet rs = tcwDB.executeQuery("SELECT bestEval FROM pja_unitrans_go " +
						"where gonum=" + gonum + " ORDER BY bestEval ASC LIMIT 1");
				if (rs.next())  eval = rs.getDouble(1);
				rs.close();
				
				int nUnitranHit = tcwDB.executeCount("select count(*) from pja_unitrans_go" +
						" where gonum=" + gonum);
				
				// from goDB.GO_TERM
				String name="obsolete - not in " + godbsrc;
				String type="biological_process"; // has to be in enum
				int level=0;
				
				rs = goDB.executeQuery("select name, term_type, level from " + tab_goTerm + " where gonum=" + gonum);
				if (rs.next()) {
					name = rs.getString(1);
					type = rs.getString(2);
					level = rs.getInt(3);
				}
				else cntNotFound++;
				rs.close();
				
				boolean found=false; 
				for (int i=0; i<types.length && !found; i++) {
					if (type.equals(types[i])) found=true;
				}
				if (!found) { 
					Out.PrtWarn("Incomplete record in GO database for " + String.format(Globalx.GO_FORMAT, gonum));
					cntNotFound++;
					continue;
				}
				ps2.setInt(1, gonum);
				ps2.setString(2, name);
				ps2.setString(3, type);
				ps2.setInt(4, level);
				ps2.setDouble(5, eval);
				ps2.setInt(6, nUnitranHit);
				ps2.setInt(7, 0);
				ps2.addBatch();
				cnt++; cntSave++;
				if (cntSave==NLOOP) {
					Out.r("Insert GO Info " + cnt);
					cntSave=0;
					ps2.executeBatch();
				}
			}
			if (cntSave>0) ps2.executeBatch();
			ps2.close();
			tcwDB.closeTransaction();
			
			prtCntMsgMem(cnt,"added unique GOs", time);
			if (cntNotFound>0) prtCntMsg(cntNotFound, "in UniProt but not in GO database");
			
			/** CAS318 - GOTRIM this is used for the Basic GO Trim function
			 * pja_gotree -- transfer go.go_tree to tw.go_tree where there is an entry in go_info 
			
			Out.PrtSpMsg(2, "Create GO tree");
			time = Out.getTime();
			tcwDB.executeUpdate("insert into pja_gotree " +
				" select 0, gt.gonum, gt.level, tw.descr, 0, tw.term_type  " +
	    			" from " + goTCWtree + " as gt " +
	    			" join go_info as tw on tw.gonum=gt.gonum " +
	    			" order by gt.idx asc");
			cnt = tcwDB.executeCount("select count(*) from pja_gotree");
			prtCntMsgMem(cnt, "branches", time);
			 **/			
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error go info");}
		return false;
	}
	/*******************************************************
	 * CAS323 create (assigned)(inherited) by CAT for each GO
	 */
	private boolean do8_update_evc(){
		try {
			// Create cat->ev mapping
			MetaData md = new MetaData();
			HashMap <String, String> evcMap = md.getEvCatMap();
			String [] catOrder = md.getEvClist();
			int nCat = evcMap.size();
			HashMap <String, EV> evcCatMap = new HashMap <String, EV> ();
			
			for (int ix=0; ix<nCat; ix++) {
				String cat = catOrder[ix];
				
				String [] tok = evcMap.get(cat).split(",");
				
				evcCatMap.put(cat, new EV(ix, tok));
			}
			
			// Set assigned for each go
			HashMap <Integer, goEV> goEvMap = new HashMap <Integer, goEV> ();
			for (int gonum : goAllSet) {
				goEV gv = new goEV(nCat);
				ResultSet rs = tcwDB.executeQuery("SELECT distinct EC FROM pja_uniprot_go WHERE gonum=" + gonum);
				while (rs.next()) {
					 gv.add(rs.getString(1)); // assigned set
				}
				rs.close();
				 
				goEvMap.put(gonum, gv); 
			}
			goAllSet.clear();
			
			// Set inherited EvC (if not assigned) for each go
			for (int gonum : goEvMap.keySet()) {
				 HashSet <String> asnSet = goEvMap.get(gonum).assigned;
				 if (asnSet.size()==0) continue;
				 
				 ResultSet rs = tcwDB.executeQuery("SELECT ancestor FROM go_graph_path WHERE child=" + gonum);
				 while (rs.next()) {
					 int ancGO = rs.getInt(1);
					 
					 goEV gvObj = goEvMap.get(ancGO);
					 gvObj.add(asnSet);
				 }
				 rs.close();
			}
			
			
			// create cat[i] = asm:inh
			for (int gonum : goEvMap.keySet()) {
				goEV gvObj = goEvMap.get(gonum);
				
				for (String cat : evcCatMap.keySet()) {
					String [] list = evcCatMap.get(cat).list;
					String asn = "", inh="";
					
					for (String ev : list) {
						if (gvObj.assigned.contains(ev)) {
							if (asn=="") 	asn=ev; 
							else 			asn += "," + ev;
						}
						else if (gvObj.inherited.contains(ev)) {
							if (inh=="") 	inh=ev; 
							else 			inh += "," + ev;
						}
					}
					if (inh!="") inh = "(" + inh + ")";
					int ix = evcCatMap.get(cat).ix;
					
					gvObj.cat[ix] = (asn!="" && inh!="") ? asn+" "+inh : asn+inh;
				}
			}
			
			// update database
			String evList = null;
			for (String cat : catOrder) {
				if (evList==null)	evList =        Globalx.GO_EvC+cat + "=?";
				else 				evList += "," + Globalx.GO_EvC+cat + "=?";
			}
		
			PreparedStatement ps1 = tcwDB.prepareStatement("UPDATE go_info SET  " + evList +
					" WHERE gonum=?");
			tcwDB.openTransaction();
			int cnt=0, cntSave=0;
			
			for (int gonum : goEvMap.keySet()) {
				goEV evObj =  goEvMap.get(gonum);
				for (int i=0; i<nCat; i++) 
					ps1.setString(i+1, evObj.cat[i]);
				ps1.setInt(nCat+1, gonum);
				ps1.addBatch();
				
				cnt++; cntSave++;
				if (cntSave==NLOOP) {
					Out.r("Update GO-EvC " + cnt);
					cntSave=0;
					ps1.executeBatch();
				}
			}
			if (cntSave>0) ps1.executeBatch();
			ps1.close();
			tcwDB.closeTransaction();
			Out.r("                                               ");
			
			return true;
		}
		catch(Exception e){ErrorReport.die(e, "Error evidence codes");}
		return false;
	}
	private class EV {
		public EV (int ix, String [] list) {
			this.list=list;
			this.ix = ix;
		}
		private String [] list; // retain order
		private int ix;
	}
	private class goEV {
		public goEV(int nCat) {
			cat = new String [nCat];
		}
		public void add(String ev) {
			if (!assigned.contains(ev))  assigned.add(ev);
		}
		public void add(HashSet <String> child) {
			for (String ev : child) {
				if (!assigned.contains(ev)) 
					if (!inherited.contains(ev)) inherited.add(ev);
			}
		}
		private HashSet <String> assigned  = new HashSet <String> ();
		private HashSet <String> inherited = new HashSet <String> ();
		private String [] cat;
	}
	/***********************************************************************
	 * Slim processing
	 * CAS318 changed to GO OBO - previous GO MySQL will not work
	 */
	private void loadSlimSubset(String slimSubset) {
		Out.PrtSpMsg(2, "Add Slim Subset " + slimSubset);
		
		try {
			boolean isOBO = (goDB.tableColumnExists(Globalx.goMetaTable, "isOBO"));
			if (!isOBO) {
				Out.PrtWarn("GO Slims are not available with pre-v318 GOdbs");
				return;
			}
			long time = Out.getTime();
			Vector <Integer> goSlims = new Vector <Integer> ();
				
			int subset_id=0;
			ResultSet rs = goDB.executeQuery("select id, name from term where term_type='subset'");
			while (rs.next()) {
				String name = rs.getString(2);
				if (name.equals(slimSubset)) {
					subset_id = rs.getInt(1);
					break;
				};
			}
			if (subset_id==0) {
				Out.PrtError("GO database does not contains Slim subset " + slimSubset);
				return;
			}
			// read slims for GOdb
			rs = goDB.executeQuery("select gonum from term_subset where subset_id=" + subset_id);
			while (rs.next()) 
				goSlims.add(rs.getInt(1));
	
			prtCntMsg(goSlims.size(), "Slims in " + slimSubset);
			
			// set slims in TCW
			int cnt=0;
			for (int gonum : goSlims) {
				cnt += tcwDB.executeUpdate("update go_info set slim=1 where gonum=" + gonum);
			}
			
			tcwDB.executeUpdate("update assem_msg set go_slim='" + slimSubset + "'");
			prtCntMsgMem(cnt, "Added Slims", time);
		}
		catch(Exception e){ErrorReport.die(e, "Error adding Slim subset from GO database");}
	}
	private void loadSlimOBOFile(String slimOBOFile) {
		Out.PrtSpMsg(2, "Add Slim OBO File " + slimOBOFile);
		
		String projDirName = runSTCWMain.getProjName();
		String path = FileC.addFixedPath(projDirName, slimOBOFile, FileC.dPROJ);
		
		FileVerify verFileObj = new FileVerify();
		if (!verFileObj.verify(FileC.bNoPrt, path, FileC.fOBO)) return;
		
		try {
			long time = Out.getTime();

			BufferedReader in = FileHelpers.openGZIP(path); // CAS315
			HashMap <Integer, String> goSlims = new HashMap <Integer, String> ();
			String line, name="";
			int gonum=0;
			
			while ((line = in.readLine()) != null) {
				if (line.equals("[Term]")) {
					if (gonum>0 && !name.equals("")) goSlims.put(gonum, name);
					gonum=0;
					name="";
				}
				else if (line.startsWith("id:") && line.contains("GO:")) {
					String tmp = line.substring(3).trim(); // remove id:
					tmp = tmp.substring(3).trim(); // remove GO:
					gonum = Integer.parseInt(tmp);
				}
				else if (line.startsWith("name:")) {
					name = line.substring(5).trim();
				}
			}
			if (gonum>0 && !name.equals("")) goSlims.put(gonum, name);
			in.close();
			
			prtCntMsg(goSlims.size(), "Slims in file");
			
			// set slims in TCW
			int cnt=0;
			for (int gonum2 : goSlims.keySet()) {
				cnt+=tcwDB.executeUpdate("update go_info set slim=1 where gonum=" + gonum2);
			}
			
			String file =  slimOBOFile;
			if (slimOBOFile.length()>100) {
				int len = slimOBOFile.length();
				file = "..." + slimOBOFile.substring(len-100);
			}
			tcwDB.executeUpdate("update assem_msg set go_slim='" + file + "'");
			prtCntMsgMem(cnt, "Added Slims", time);
		}
		catch(Exception e){ErrorReport.die(e, "Error adding Slim subset from GO database");}
	}
	private DecimalFormat df = new DecimalFormat("#,###,###");
	private void prtCntMsg(int x, String msg) {
		Out.PrtSpMsg(2, String.format("%10s ", df.format(x)) + msg);
	}
	private void prtCntMsgMem(int x, String msg, long time) {
		Out.PrtSpMsgTimeMem(2, String.format("%10s ", df.format(x)) + msg, time);
	}
	
	/*******************************************************************/
	private DBConn tcwDB;
	private DBConn goDB;
	private String godbsrc;
	
	private String godbName;
	private String tab_goGraphPath, tab_goTerm, tab_goTerm2term, tab_goTCWup; 
	
	private int nHit, nSeq;
	private HashSet <Integer>   goAllSet = new HashSet <Integer> (); // assigned and indirect
	private HashMap <Integer, Hit2GO>  hitGoMap = new HashMap <Integer, Hit2GO> (); // hit-GO,EvC
	private HashMap <Integer, Integer>   seqPIDgoMap = new HashMap <Integer, Integer> (); // best GO pid per seq
}
