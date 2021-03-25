package sng.annotator;
/****************************************************
 * Adds GO annotation to a sTCW database. Uses the go_ database.
 * If the go_ database was created with newGOver.pl, then calls DoGO.modifyGOdb to 
 * 	make modifications.
 * Changes since v1.3.9 -- see DoGO.
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
	static private final String goUpTable =   Globalx.goUpTable;
	
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
			goDB = hosts.getDBConn(godbName);
			
			godbsrc = tcwDB.executeString("Select go_msg from assem_msg"); // CAS318 - use for messages instead of godbName
			if (godbsrc==null || godbsrc=="") godbsrc = "Unknown GO source";
			else godbsrc = godbsrc.substring(0, godbsrc.indexOf(" ")+1); // Just get version
			
			if (!makeGoTables(godbName)) return;
			
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
	
	private boolean makeGoTables(String godbName) {
		int NLOOP=1000; 
		ResultSet rs = null;
		long time=0;
		int cnt=0, cntSave=0;
		
		try {
			PreparedStatement ps1;
			HashSet <Integer>   goAllSet = new HashSet <Integer> ();
			HashMap <Integer, Hit2GO>  hitGoMap = new HashMap <Integer, Hit2GO> ();
			
			// Tables used from GO database - must prefix with database name for transfers
			String goGraphPath = 	godbName + ".graph_path";
			String goTerm = 		godbName + ".term";
			String goTerm2term = 	godbName + ".term2term";
			String goTCWup = 		godbName + "." + goUpTable;
			
			if (!hasAnnotation(tcwDB)) {
				Out.PrtError("GO was specified but there is no annotation!");
				return false;
			}
			if (!goDB.tableExists(goUpTable)) {// note, db.tableExists doesn't see this table which is in the other DB ??
				Out.PrtError("Cannot add GO tables because " + goTCWup + " does not exist - runAS");
				return false;
			}
			if (!goDB.tableExists("graph_path")) {
				Out.PrtError("Cannot add GO tables because " + goGraphPath + " does not exist.");
				return false;
			}
			
/** Create Tables **/
			Schema.createGOtables(tcwDB); // drops current 
			
			tcwDB.executeUpdate("update pja_db_unique_hits " +
				" set ec='', pfam='', kegg='', goList='', goBrief='', interpro=''");
			tcwDB.executeUpdate("update contig set PIDgo=0");
			tcwDB.executeUpdate("update pja_db_unitrans_hits set filter_gobest=0");
			tcwDB.executeUpdate("update assem_msg set go_msg='', go_slim='', go_ec=null");
			if (tcwDB.tableColumnExists("assem_msg", "goDE"))
				tcwDB.executeUpdate("update assem_msg set goDE=''");
			
/** Create Overview message for processing info: assem_msg.go_msg **/
			String msg = "GOdb: " + godbName + "  [GOs added with " + Globalx.sTCWver + "]"; // CAS318 added GOdb
			if (goDB.tableExists(goMetaTable)) {
				rs = goDB.executeQuery("select filename from " + goMetaTable);
				if (rs!=null && rs.next()) {
					String file = rs.getString(1);
					if (file!=null) msg = file + "  " + msg; 
				}
			}
			// CAS319 - changed from having isa and partof to go_rtypes; used in Basic.GOtree
			rs = goDB.executeQuery("select id, name from term  where term_type='external' order by id");
			String tMsg="";
			while (rs.next()) {
				tMsg += rs.getInt(1) + ":" + rs.getString(2) + ";";
			}
			tcwDB.executeUpdate("update assem_msg set go_msg='" + msg + "', go_rtypes='" + tMsg + "'");
			
			Out.PrtSpMsg(1, "Computing GOs for:");
			int nSeq = tcwDB.executeCount("select count(*) from contig");
			int nHit = tcwDB.executeCount("select count(*) from pja_db_unique_hits");
			prtCntMsg(nSeq, "Sequence");
			prtCntMsg(nHit, "Unique hits");
			
/** pja_db_unique_hits -- add kegg, pfam, ec  ***/
			Out.PrtSpMsg(1, "Add GO/Interpro/Kegg/Pfam/EC to unique hits table");
			int s = goDB.executeCount("select count(*) from " + goUpTable); // CAS318 add count - if many UniProts...
			Out.PrtSpMsg(2, "Transferring data from a table with " + Out.df(s) + " entries");
			if (s>5000000)
				Out.PrtSpMsg(3, "There will be no terminal status for this step - please be patient...");
			
			time = Out.getTime();
			if (goDB.tableColumnExists(goUpTable, "interpro")) {// backwards compatible -- added interpro to V4 goDBs.
				tcwDB.executeUpdate("update pja_db_unique_hits as tw, " + goTCWup + " as g " + 
				" set tw.kegg=g.kegg, tw.pfam=g.pfam, tw.ec=g.ec, tw.interpro=g.interpro, tw.goList=g.go " +
				" where g.upid=tw.hitid");
			} else {
				tcwDB.executeUpdate("update pja_db_unique_hits as tw, " + goTCWup + " as g " + 
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
			
/** build hitGoMap and goAllSet - direct hits only **/	
			Out.PrtSpMsg(1, "Build hit-GO table");
			Out.PrtSpMsg(2, "Get hits  ");
			time = Out.getTime();
			rs = tcwDB.executeQuery("select DUHID, hitID, goList from pja_db_unique_hits where goList!=''");
			while (rs.next())
			{
				int duhid = rs.getInt(1);
				hitGoMap.put(duhid, new Hit2GO(rs.getString(2), rs.getString(3))); // CREATE hit list of assigned GOs
			}
			prtCntMsgMem(hitGoMap.size(), "hits to process", time);
			
			// Build goBrief per annoDB hit, create goSet of all assigned
			Out.PrtSpMsg(2, "Hit to GO mapping                       ");
			cnt=cntSave=0;
			time = Out.getTime();
			tcwDB.openTransaction(); 
			ps1 = tcwDB.prepareStatement(
				      "UPDATE pja_db_unique_hits SET goBrief = ? " +
				      " WHERE goList!='' and DUHID = ?");
			for (int duhid : hitGoMap.keySet()) {			// hitGoMap
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

			/** pja_uniprot_go: direct mappings only **/ 
			Out.PrtSpMsg(2, "Insert into hit-go table...");
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
			prtCntMsgMem(cnt, "hit-GO pairs", time);
			
			/** add ancestors to hitGoMap and goAllSet**/
			Out.PrtSpMsg(2, "Find all inherited...                     ");
			time = Out.getTime();
			cnt=0;
			HashMap <Integer, String> ancMap = new HashMap <Integer, String> ();
			
			for (int hitIndex : hitGoMap.keySet()) {			
				Hit2GO hitGoObj = hitGoMap.get(hitIndex);
		
				for (Integer gonum : hitGoObj.goMap.keySet()) {
					String ec = "(" + hitGoObj.goMap.get(gonum) + ")";
					rs = tcwDB.executeQuery("select ancestor from " + goGraphPath +
							" where child=" + gonum);
					while (rs.next()) {
						int gonum2 = rs.getInt(1);
						ancMap.put(gonum2, ec);	
					}
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
			
			HashMap <Integer, Integer>   seqPIDgoMap = new HashMap <Integer, Integer> ();
			HashMap <Integer, goSeqInfo> goSeqMap = new HashMap <Integer, goSeqInfo> ();
			
			tcwDB.openTransaction();
			ps1 = tcwDB.prepareStatement("INSERT into pja_unitrans_go SET " +
				"CTGID=?, gonum=?, bestDUH=?, bestEval=?, bestEV=?, bestAN=?, direct=?, EC=?");
			
			Out.PrtSpMsg(2, "Insert into seq-go table...    ");
			
			for (int ctgid : ctgSet) {						// hitGoMap
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
			
			Out.PrtSpMsg(2, "Update database with best Hit with GO per sequence...           ");
			time = Out.getTime();
			cnt=0;
			tcwDB.openTransaction();
			ps1 = tcwDB.prepareStatement("UPDATE contig SET PIDgo = ?  WHERE CTGID=?");
			
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
			
	/** go_graph_path and go_term2term **/
			Out.PrtSpMsg(1, "Build GO tables");
			
			Out.PrtSpMsg(2, "Create graph_path from " + godbName + " for GOs");
			time = Out.getTime();
			cnt=0;
			tcwDB.openTransaction();
			for (int gonum : goAllSet) { 						// goAllSet
				tcwDB.executeUpdate("insert into " +
					" go_graph_path (relationship_type_id, distance,  child, ancestor) " +
					" select g.relationship_type_id, g.distance, g.child, g.ancestor " +
					" from " + goGraphPath + " as g " +
					" where ancestor>0 and child=" + gonum);
			
				tcwDB.executeUpdate("insert into " +
					" go_term2term (relationship_type_id, child, parent) " +
					" select g.relationship_type_id, g.child, g.parent " +
					" from " + goTerm2term + " as g " +
					" where parent>0 and child=" + gonum);
				cnt++;
				if (cnt % 1000 == 0) Out.r("Insert GO " + cnt);
			}
			tcwDB.closeTransaction();
			prtCntMsgMem(cnt,"processed", time);
			
/** XXX go_info table -- holds information about each direct and ancestor GO  **/
			 
			// if UniPort has a GO that is not in the GO database, causes an inconsistency
			String [] ecList = new MetaData().getEClist();
			String ecStr = ecList[0]+"=?";
			for (int i=1; i<ecList.length; i++) ecStr += "," + ecList[i] + "=?";
			PreparedStatement ps2 = tcwDB.prepareStatement("INSERT into go_info SET " +
					"gonum=?, descr=?, term_type=?, level=?, bestEval=?, nUnitranHit=?, slim=?,"
					+ ecStr);

			HashSet <String> ecFullSet = new HashSet <String>  ();
			for (String ec : ecList) ecFullSet.add(ec);
			String [] types = Globalx.GO_TERM_LIST; 
			
			Out.PrtSpMsg(2, "Create GO information table");
			time = Out.getTime();
			cnt=cntSave=0;
			int cntNotFound=0;
			tcwDB.openTransaction();
			for (int gonum : goAllSet)  							// goAllSet
			{	
				double eval = -2.0;
				rs = tcwDB.executeQuery("SELECT bestEval FROM pja_unitrans_go " +
						"where gonum=" + gonum + " ORDER BY bestEval ASC LIMIT 1");
				if (rs.next())  eval = rs.getDouble(1);
				
				int nUnitranHit = tcwDB.executeCount("select count(*) from pja_unitrans_go" +
						" where gonum=" + gonum);
				
				int [] ecBin= new int [ecList.length];
				HashSet <String> ecSet = makeECset(gonum);
				int x=0;
				for (String ec : ecList) {
					ecBin[x++] = (ecSet.contains(ec)) ? 1 : 0;
				}
				
				String name="obsolete - not in " + godbsrc;
				String type="biological_process"; // has to be in enum
				int level=0;
				
				rs = goDB.executeQuery("select name, term_type, level from " + goTerm + " where gonum=" + gonum);
				if (rs.next()) {
					name = rs.getString(1);
					type = rs.getString(2);
					level = rs.getInt(3);
				}
				else cntNotFound++;
				
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
				for (int i=0, j=8; i<ecBin.length; i++, j++)
					ps2.setInt(j, ecBin[i]);
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
			goAllSet.clear();
			
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
		catch(Exception e){ErrorReport.die(e, "Error adding GO tables");}
		return false;
	}
	private HashSet <String> makeECset(int gonum) {
		HashSet <String> ecSet = new HashSet <String> ();
		try {
			// get ancestors
			Vector <Integer> goList = new Vector <Integer> ();
			ResultSet rs = tcwDB.executeQuery("select child from go_graph_path " +
					"where child>0 and ancestor=" + gonum);
			while (rs.next()) {
				goList.add(rs.getInt(1)); // will include gonum
			}
			// find the direct hit EC for each ancestor
			for (int gonum2 : goList) {
				rs = tcwDB.executeQuery("SELECT distinct EC FROM pja_uniprot_go WHERE gonum=" + gonum2);
			
				while (rs.next()) ecSet.add(rs.getString(1));
			}
			rs.close();
		}
		catch(Exception e){ErrorReport.die(e, "Getting EC list for go " + gonum);}
		return ecSet;
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
	/********** XXX variables for updateGO ***********************/
	private class Hit2GO {
		public Hit2GO(String n, String g) {name=n; goList=g;}
		public void add(int g, String e) {
			if (!goMap.containsKey(g)) goMap.put(g, e);
		}
		String name, goList;
		HashMap <Integer, String> goMap = new HashMap <Integer, String> (); // GO, evidence code
	}
	private class goSeqInfo {
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
	
	private static boolean hasAnnotation(DBConn db)
	{
		try{
			return db.tableExists("pja_db_unitrans_hits");
		}
		catch(Exception e){ErrorReport.die(e, "Error finding go uniprot table in database");}
		return false;
	}
	
	public static DBConn connectToGODB(String host, String user, String pass, String godb)
	{		
		DBConn goDB = null;
		try {
			goDB = new DBConn(host, godb, user, pass);
		}
		catch(Exception e) {Out.PrtWarn("Could not connect to GO database " + godb);}
		return goDB;
	}
	private DBConn tcwDB;
	private DBConn goDB;
	private String godbsrc;
}
