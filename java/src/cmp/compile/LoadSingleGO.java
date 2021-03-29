package cmp.compile;
/*****************************************************
 * create GO tables
 * When this starts, the unique_hits already contain the hits and goList of assigned GOs.
 * Create go_seq of all assigned and inherited GOs, along with best e-value
 * This is not using the GOdb, so need to get go_graph and go_info from individual data 
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import cmp.compile.panels.CompilePanel;

public class LoadSingleGO {	
	public LoadSingleGO(DBConn dbc, CompilePanel panel) {
		mDB = dbc;
		cmpPanel = panel;
	}
	public boolean run() {
		Out.PrtDateMsg("\nLoading GOs from SingleTCWs");
		trunTable();
		if (!loadSeqsIdx()) return false;
		if (!loadHitsWithDirectGOs()) return false;
		if (!transferGOgraph()) return false; 
		if (!transferGOinfo()) return false;
		if (!addHitAncestors()) return false;
		if (!saveSeqGOtables()) return false;
		if (!saveGOinfo()) return false;
		
		cmpPanel.getDBInfo().resetGO(mDB);
		
		return true;
	}
	private boolean trunTable() {
		try { 
			mDB.tableDelete("go_seq");
			mDB.tableDelete("go_info");
			mDB.tableDelete("go_graph_path");
		}
		catch(Exception e) {	ErrorReport.prtReport(e, "Cannot build GO tables");}
		return false; 
	}
	private boolean loadSeqsIdx() {
		try {
			Out.PrtSpMsg(1, "Loading sequence info");
			ResultSet rs = mDB.executeQuery("select UTid from unitrans");
			while (rs.next()) {
				seqVec.add(rs.getInt(1));
			}
			rs.close();
			Out.PrtSpCntMsg(2, seqVec.size(), "Total sequences");
			return true;
		}
		catch(Exception e) {	ErrorReport.prtReport(e, "Cannot build GO tables");}
		return false;
	}
	private boolean loadHitsWithDirectGOs() {
		try {
			long time = Out.getTime();
			Out.PrtSpMsg(1, "Loading hit info");
			int cntNoGO=0;
			
			ResultSet rs = mDB.executeQuery("select HITid, HITstr, goList from unique_hits");
			while (rs.next()) {
				int hitid = rs.getInt(1);
				String hitName = rs.getString(2);
				String list = rs.getString(3);
				if (list==null || list.trim().equals("")) {
					cntNoGO++;
					continue;
				}
				
				String [] goList = list.split(";");
				Hit2GO hg = new Hit2GO(hitName);
				for (String g : goList) {
					String [] tok = g.split(":"); // GO:00123:EC
					if (tok.length!=3) ErrorReport.die(hitid + " -- " + g + " --- " + list);
					int gonum = Integer.parseInt(tok[1]);
					String ec = tok[2];
					hg.add(gonum, ec); 
					// Seed goMap with all direct GOs
					if (!goMap.containsKey(gonum)) goMap.put(gonum, new GOinfo());
				}
				hitMap.put(hitid, hg);
			}
			rs.close();
			Out.PrtSpCntMsg(2,cntNoGO, 		"hits without GOs");
			Out.PrtSpCntMsg(2,hitMap.size(), "hits with GOs");
			Out.PrtSpCntMsg(2,goMap.size(), 	"assigned GOs");
			Out.PrtSpMsgTimeMem(1, "Finish load hit", time);
			return true;
		}
		catch(Exception e) {	ErrorReport.prtReport(e, "Cannot build GO tables");}
		return false;
	}
	// transfer all assigned and inherited GOs from the singleTCW go_graph_path
	private boolean transferGOgraph() {
		try {
			long time = Out.getTime();
			Out.PrtSpMsg(1, "Transferring GO ancestors from singleTCWs");
			ResultSet rs=null;
			int cntAddGO=0;
			
			for(int x=0; x<cmpPanel.getSpeciesCount(); x++) {
				DBConn sDB = runMTCWMain.getDBCstcw(cmpPanel, x);
				if (!sDB.tableExist("go_info")) {
					sDB.close();
					Out.PrtSpMsg(3, "No GOs in dataset");
					continue;
				}
				String dbName = cmpPanel.getSpeciesDB(x);
				
				HashSet <Integer> goDB = new HashSet <Integer> ();
				rs = sDB.executeQuery("select gonum from go_info");
				while (rs.next()) goDB.add(rs.getInt(1));
				rs.close();
				
				int cnt=0;
				mDB.openTransaction();
				for (int gonum : goMap.keySet()) {
					GOinfo gi = goMap.get(gonum);
					if (gi.added) continue;
					if (!goDB.contains(gonum)) continue;
					gi.added=true;
					
					// CAS318 removed relation_distance as not in OBO
					mDB.executeUpdate("insert into " +
					" go_graph_path (relationship_type_id, distance,  child, ancestor) " +
					" select g.relationship_type_id, g.distance,  g.child, g.ancestor " +
					" from " + dbName + ".go_graph_path as g " +
					" where ancestor>0 and child=" + gonum);
					
					cnt++; cntAddGO++;
					if (cnt%1000==0) Out.r("added GO-ancestors " + cnt);
				}
				if (rs!=null) rs.close();
				mDB.closeTransaction();
				sDB.close();
				
				String msg = (x==0) ? "add ancestors from " : "additional    from ";
				Out.PrtSpCntMsg(2, cnt, msg + cmpPanel.getSpeciesSTCWid(x));
				if (cntAddGO>=goMap.size()) break;
			}
			/************************************************/
			Out.PrtSpMsg(1, "Add inherited to GO list");
			rs = mDB.executeQuery("select DISTINCT(child) from go_graph_path");
			while (rs.next()) {
				int gonum = rs.getInt(1);
				if (!goMap.containsKey(gonum)) goMap.put(rs.getInt(1), new GOinfo());
			}
			rs = mDB.executeQuery("select DISTINCT(ancestor) from go_graph_path");
			while (rs.next()) {
				int gonum = rs.getInt(1);
				if (!goMap.containsKey(gonum)) goMap.put(rs.getInt(1), new GOinfo());
			}
			Out.PrtSpCntMsg(2, goMap.size(), " assigned and inherited GOs");
			
			return true;
		} catch(Exception e) {	
			ErrorReport.prtReport(e, "Cannot build GO graph table");
		} catch (OutOfMemoryError error) {
			ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run");
		}	
		return false;
	}
	// transfer all assigned and inherited GOs from the singleTCW go_graph_path
	private boolean transferGOinfo() {
		try {
			long time = Out.getTime();
			Out.PrtSpMsg(1, "Transferring GO information from singleTCWs");
			ResultSet rs=null;
			int cntAddGO=0;
			
			for(int x=0; x<cmpPanel.getSpeciesCount(); x++) {
				DBConn sDB = runMTCWMain.getDBCstcw(cmpPanel, x);
				if (!sDB.tableExist("go_info")) {
					sDB.close();
					continue;
				}
				
				HashSet <Integer> goDB = new HashSet <Integer> ();
				rs = sDB.executeQuery("select gonum from go_info");
				while (rs.next()) goDB.add(rs.getInt(1));
				rs.close();
				
				int cnt=0;
				mDB.openTransaction();
				for (int gonum : goMap.keySet()) {
					GOinfo gi = goMap.get(gonum);
					if (gi.hasInfo) continue;
					if (!goDB.contains(gonum)) continue;
					gi.hasInfo=true;
					
					rs = sDB.executeQuery("select descr, term_type, level from go_info where gonum=" + gonum);
					if (rs.next()) {
						gi.desc =  rs.getString(1);
						gi.term_type =  rs.getString(2);
						gi.level = rs.getInt(3);
					}
					else System.out.println("No sTCW record for " + gonum + "                    "); 
					
					cnt++; cntAddGO++;
					if (cnt%1000==0) Out.r("added GO-info " + cnt);
				}
				if (rs!=null) rs.close();
				mDB.closeTransaction();
				sDB.close();
				
				String msg = (x==0) ? "add GO-info from " : "additional  from ";
				Out.PrtSpCntMsg(2, cnt, msg + cmpPanel.getSpeciesSTCWid(x));
			}
			Out.PrtSpCntMsgTimeMem(2, cntAddGO, "total added GO", time);
			return true;
		} catch(Exception e) {	
			ErrorReport.prtReport(e, "Cannot build GO graph table");
		} catch (OutOfMemoryError error) {
			ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run");
		}	
		return false;
	}
	// Add to each hit all inherited GOs
	private boolean addHitAncestors() {
		try {
			long time = Out.getTime();
			Out.PrtSpMsg(1, "Assign inherited GOs to hits");
			ResultSet rs=null;
			int cnt=0;
			
			for (int hitIndex : hitMap.keySet()) {			
				Hit2GO hitObj = hitMap.get(hitIndex);
				int [] goSet = new int [hitObj.goMap.size()];
				int i=0;
				for (Integer gonum : hitObj.goMap.keySet()) goSet[i++]=gonum;
				
				for (int gonum : goSet) {
					String ec = "(" + hitObj.goMap.get(gonum) + ")";
					rs = mDB.executeQuery("select ancestor from go_graph_path where child=" + gonum);
					while (rs.next()) {
						int gonum2 = rs.getInt(1);
						if (!hitObj.goMap.containsKey(gonum2)) {
							hitObj.goMap.put(gonum2, ec); // add inherited
							cnt++;
							if (cnt%1000==0) Out.r("Add inherited to hit " + cnt);
						}
					}
				}
			}
			if (rs!=null) rs.close();
			Out.PrtSpCntMsgTimeMem(2, cnt, "inherited hit-GOs associations", time);
			
			return true;
		} catch(Exception e) {	
			ErrorReport.prtReport(e, "Cannot assign hit-GO ancestors");
		} catch (OutOfMemoryError error) {
			ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run");
		}	
		return false;
	}
	// save each assigned and inherited seq-GO pair
	private boolean saveSeqGOtables() {
		try {
			long time = Out.getTime();
			Out.PrtSpMsg(1, "Save GOs per sequence");
			PreparedStatement ps = mDB.prepareStatement("INSERT INTO go_seq " +
					"(UTid, gonum, bestHITstr, bestEval, bestEV, bestAN, EC, direct) " +
					" values(?,?,?,?,?,?,?, ?)");
			
			int cntAllSeq=0;
			HashMap <Integer, Seq2GO> seqGOMap = new HashMap <Integer, Seq2GO> ();
			
			for (int seqIdx : seqVec) {
				seqGOMap.clear();
				
				ResultSet rs=mDB.executeQuery("select HITid, HITstr, e_value, bestEval, bestAnno from unitrans_hits " +
						" where UTid=" + seqIdx + 
						" order by e_value ASC, bestEval DESC, bestAnno DESC, bit_score DESC");
				
				while (rs.next()) {
					int hitIdx = rs.getInt(1);
					if (!hitMap.containsKey(hitIdx)) continue; // not in list if no GOs for hit
					
					String hitName = rs.getString(2);
					double  eval = rs.getDouble(3);
					boolean bestEval = rs.getBoolean(4);
					boolean bestAnno = rs.getBoolean(5);
					
					Hit2GO hitObj = hitMap.get(hitIdx);
					hitObj.nSeq++;
					
					for (int gonum : hitObj.goMap.keySet()) {
						String ec = hitObj.goMap.get(gonum);
						if (seqGOMap.containsKey(gonum)) {
							Seq2GO sg = seqGOMap.get(gonum);
							if (!ec.contains("(")) sg.direct=true;
						}
						else {
							Seq2GO sg = new Seq2GO(hitName, bestEval, bestAnno, eval, ec);
							seqGOMap.put(gonum, sg);
						}
					}
				} 
				/****************************************************/
				int cntSave=0;
				mDB.openTransaction();
				for (int gonum : seqGOMap.keySet()) {
					Seq2GO sg = seqGOMap.get(gonum);
			
					ps.setInt(1,     seqIdx);
					ps.setInt(2,     gonum);
					ps.setString(3,  sg.hitName);
					ps.setDouble(4,  sg.bestEval);
					ps.setBoolean(5, sg.bestEV);
					ps.setBoolean(6, sg.bestAN);
					ps.setString(7,  sg.bestEC);
					ps.setBoolean(8, sg.direct);
					
					ps.addBatch();			
					cntSave++; cntAllSeq++;
					if(cntSave==1000)  {
						cntSave=0;
						ps.executeBatch();	
						Out.r("added GO-seq pairs " + cntAllSeq);
					}	
					GOinfo gi = goMap.get(gonum);
					gi.nSeqs++;
					if (sg.bestEval<gi.bestEval) gi.bestEval=sg.bestEval;
				}
				if (cntSave>0) ps.executeBatch();
				mDB.closeTransaction();
			} // end loop through sequences
			ps.close();
			
			if (runMTCWMain.test) { //  one occurrence on mTCW_rhi (NnR/OlR/Os)
				int cnt=0;
				for (Hit2GO hg : hitMap.values()) {
					if (hg.nSeq==0) {
						cnt++;
						if (cnt<10) Out.prt(hg.name);
					}
				}
				Out.PrtSpCntMsgZero(2, cnt, "Hits not attached to sequences");
			}
			
			Out.PrtSpCntMsgTimeMem(2, cntAllSeq, "GO-seq pairs", time);
			
	   		return true;
		} catch(Exception e) {	
			ErrorReport.prtReport(e, "Cannot build GO tables");
		} catch (OutOfMemoryError error) {
			ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run");
		}
		return false;
	}
	
	// wait until end because count sequences with GO and bestEval, even though not using right now
	private boolean saveGOinfo() {
		try {
			long time = Out.getTime();
			Out.PrtSpMsg(1, "Save GO information for " + goMap.size() + " GOs");
			mDB.openTransaction();
			int cnt=0, cntSave=0, cntBadType=0;
			String [] types = Globalx.GO_TERM_LIST; 
			PreparedStatement ps2 = mDB.prepareStatement("INSERT INTO go_info " +
					"(gonum, descr, term_type, level, bestEval, nSeqs) values(?,?,?,?,?, ?)");
			
			for (int gonum : goMap.keySet()) {
				GOinfo gi = goMap.get(gonum);
				
				boolean found=false; 
				for (int i=0; i<types.length && !found; i++) {
					if (gi.term_type.equals(types[i])) found=true;
				}
				if (!found) {
					if (cntBadType==0) {
						Out.PrtWarn("Incorrect Term type: '" + gi.term_type + "' for GO " + gonum);
						Out.prt("   this can happen if the sTCWdb have changed since the mTCWdb was built");
						Out.prt("   other 'incorrect term type' warning will be surpressed");
					}
					cntBadType++;
					continue;
				}
				
				ps2.setInt(1, gonum);
				ps2.setString(2, gi.desc);
				ps2.setString(3, gi.term_type);
				ps2.setInt(4,    gi.level);
				ps2.setDouble(5, gi.bestEval);
				ps2.setInt(6,    gi.nSeqs);
				ps2.addBatch();
				
				cnt++; cntSave++; 
				if(cntSave==1000)  {
					cntSave=0;
					ps2.executeBatch();
					Out.r("insert GO info " + cnt);
				}	
			}
			if (cntSave>0)ps2.executeBatch();
			ps2.close();
			mDB.closeTransaction();
			
			Out.PrtSpCntMsgZero(2, cntBadType, "Incorrect term type");
			Out.PrtSpCntMsgTimeMem(2, cnt, "Add GO info", time);
			
			return true;
		} catch(Exception e) {	
			ErrorReport.prtReport(e, "Cannot build go_info table");
		} catch (OutOfMemoryError error) {
			ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run");
		}	
		return false;
	}

	/************************************************************/
	// per sDB 
	private Vector <Integer> seqVec = new Vector <Integer> (); 
	private class Seq2GO {
		public Seq2GO(String name, boolean bEV, boolean bAN, double ev, String ec) {
			hitName=name; 
			bestEval=ev; 
			bestAN=bAN; 
			bestEV=bEV;
			bestEC=ec;
			if (!ec.startsWith("(")) direct=true;
		}
		String hitName;
		boolean bestEV=false, bestAN=false, direct=false;
		double bestEval;
		String bestEC;
	}
	// goMap is assigned and inherited - can get all ancestors from current sDB
	private HashMap <Integer, Hit2GO> hitMap = new HashMap <Integer, Hit2GO> ();
	private class Hit2GO {
		public Hit2GO(String n) {name=n;}
		public void add(int g, String e) {
			if (!goMap.containsKey(g)) goMap.put(g, e);
		}
		int nSeq=0;
		String name;
		HashMap <Integer, String> goMap = new HashMap <Integer, String> (); // GO, evidence code
	}
	
	private HashMap <Integer, GOinfo> goMap = new HashMap <Integer, GOinfo> ();
	private class GOinfo {
		int level=0;
		String desc="", term_type="";
		int nSeqs=0;
		double bestEval=1000.0; // this is in the database, but not used because can vary for filtered set.
		boolean added=false, hasInfo=false;
	}
	
	private CompilePanel cmpPanel;
	private DBConn mDB;
}
