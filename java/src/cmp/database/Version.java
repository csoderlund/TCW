package cmp.database;

import java.sql.ResultSet;
import java.util.HashSet;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.UserPrompt;
import cmp.compile.LoadSingleTCW; // db62 only

/***************************************************
 * If columns are being added for a version, add them here and to schema
 */
public class Version {
	public static final String DBver = "6.5"; 
	private final double nVer=6.5;
	
	/************************************************
	 * CAS310 added this with return value as nothing works if not updated
	 */
	public Version () {}
	public boolean run(DBConn mDB) {
		try {
			if (mDB==null) return false; 
			
			String ver = mDB.executeString("select schemver from info");
			if (ver.equals(DBver)) return true;
			
			Out.prt("mTCW database version mdb" + ver);
			tooOldDie(mDB);
			
			double d = Double.parseDouble(ver);
			if (d>nVer) {
				Out.PrtWarn("The mTCW database was created with a newer version of TCW.");
				Out.prt("   This version of the software uses database mdb" + DBver);
				return true; // CAS327 let it continue
			}
			String [] options = {"Update", "Ignore"};
			if (!UserPrompt.showOptions(options, "Database update", "The database needs a few small changes.")) {
				Out.PrtError("The mTCW database was created with a older version of TCW.");
				Out.prt("   This version of the software uses database mdb" + DBver);
				Out.prt("   Some feature will probably fail.");
				return false; // CAS330 all calling programs ignore this and continue 
			}
			if (d < 5.5) addv55(mDB);
			if (d < 5.6) addv56(mDB);
			if (d < 5.7) addv57(mDB);
			if (d < 5.8) addv58(mDB);
			if (d < 5.9) addv59(mDB);
			if (d < 6.0) addv60(mDB);
			if (d < 6.1) addv61(mDB);
			if (d < 6.2) addv62(mDB);
			if (d < 6.3) addv63(mDB);
			if (d < 6.4) addv64(mDB);
			if (d < 6.5) addv65(mDB);
			
			Out.prt("Complete update for mdb" + DBver);
			return true;
		}
		catch (Exception e) {ErrorReport.die(e, "Error checking schema"); return false;}
	}
	//--------------------------------------------------------//
	private void addv65(DBConn mDB) { // CAS330 (328) August 2021
		try {
			Out.PrtSpMsg(1, "Updating for mdb6.5 (v3.2.8) - add NTbest");
			mDB.tableCheckAddColumn("pairwise", "ntBest", "tinyint default -2", "");
			mDB.executeUpdate("update info set schemver='6.5'");
		}
		catch (Exception e) {ErrorReport.die(e, "Updating for mdb6.5");}
	}
	
	private void addv64(DBConn mDB) { // CAS327 July 2021
		try {
			Out.PrtSpMsg(1, "Updating for mdb6.4 (v3.2.7) -- add origStr");
			mDB.tableCheckAddColumn("unitrans", "origStr", "VARCHAR(30)", "");
			mDB.tableCheckAddColumn("info", "hasOrig", "tinyint default 0", ""); 
			mDB.executeUpdate("update info set schemver='6.4'");
		}
		catch (Exception e) {ErrorReport.die(e, "Updating for mdb6.4");}
	}
	
	private void addv63(DBConn mDB) { //v3.1.2 Nov/2020
		try {
			Out.PrtSpMsg(1, "Updating for mdb6.3 (v3.1.2) - add infoKeys");
			if (!mDB.tableExist("infoKeys")) {
				String sqlU = "CREATE TABLE infoKeys (" + // CAS312 db63 add info here from now on
						"KEYid 		tinyint NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
						"iKey varchar(20), " + 
						"iVal text," +
						"unique (iKey) " +
						") ENGINE=MyISAM;";
				mDB.executeUpdate(sqlU);
			}
			DBinfo info = new DBinfo(mDB);
			info.updateInfoKey("MSAscore1", "Sum-of-Pairs");
			info.updateInfoKey("MSAscore2", "MstatX Trident");
			
			if (!mDB.tableExist("pog_scores")) {
				String sqlU = "CREATE TABLE pog_scores ( " +
					"PGid int, " +
					"score1 text, "		+ //comma-delimited list of column score1
					"score2 text,  "	+ //comma-delimited list of column score2
					"nType  tinytext, "   + //comma-delimited list of AA/cnt (max 11 char for each of 21)
					"unique (PGid), " 	+ // add v3.1.3
					"index idx1(PGid)"  + // add v3.1.3
					") ENGINE=MyISAM;";
				mDB.executeUpdate(sqlU);
			}
			mDB.executeUpdate("update info set schemver='6.3'");
		}
		catch (Exception e) {ErrorReport.die(e, "Update to version 6.3"); }
	}
	//--------------------------------------------------------//
	private void addv62(DBConn mDB) { // v3.1.0 Oct/2020
		try {
			Out.PrtSpMsg(1, "Updating for mdb6.2 - add new hit columns");
			mDB.tableCheckAddColumn("unique_hits", "nBest", "int default 0", null);
			mDB.tableCheckAddColumn("unique_hits", "nSeq", "int default 0", null); 
			mDB.tableCheckAddColumn("unique_hits", "e_value", "double default 0", null); 
			mDB.tableCheckAddColumn("unique_hits", "e_value", "double default 0", null); 
			
			mDB.tableCheckAddColumn("info", "hitSQL", "text", "seqSQL");
			
			int cnt = mDB.executeCount("select count(*) from pairwise where PCC!=" + Globalx.dNoVal + " limit 1");
			if (cnt>0) mDB.tableCheckAddColumn("info", "hasPCC", "tinyint default 1", "hasDE");
			else       mDB.tableCheckAddColumn("info", "hasPCC", "tinyint default 0", "hasDE");
			
			cnt = mDB.executeCount("select count(*) from pog_groups where conLen>0 limit 1");
			if (cnt>0) mDB.tableCheckAddColumn("info", "hasMA", "tinyint default 1", "hasPCC");
			else       mDB.tableCheckAddColumn("info", "hasMA", "tinyint default 0", "hasPCC");
			
			mDB.tableCheckAddColumn("info", "lastVer", "tinytext", "compiledate");
			mDB.tableCheckAddColumn("info", "lastDate", "date", "lastVer");
			
			new LoadSingleTCW(mDB).step6_updateUniqueHits();
			
			mDB.executeUpdate("update info set schemver='6.2'");
		}
		catch (Exception e) {ErrorReport.die(e, "Updating for mdb6.2");}
	}
	private void addv61(DBConn mDB) { // v2.15 5/15/19
		try {
			Out.PrtSpMsg(1, "Updating for mdb6.1");
			Out.PrtWarn("Remove Pairs and re-add Pairs, Methods and Stats.");
			mDB.tableCheckAddColumn("pairwise", "nSNPs", "int default -2", "pOlap2");
			mDB.tableCheckAddColumn("pairwise", "cAlign", "int default -2", "aaBest"); 
			mDB.executeUpdate("update info set schemver='6.1'");
		}
		catch (Exception e) {ErrorReport.die(e, "Updating for mdb6.1");}
	}
	private void addv60(DBConn mDB) { // 2/20/19
		try {
			Out.PrtSpMsg(1, "Updating for mdb6.0");
			if (!UserPrompt.showContinue("Database update", "If GOs are in the database, they" +
					"will be removed and need to be re-added.")) 
				return;
			Out.PrtSpMsg(2, "Removing GO sequences...");
			mDB.tableDelete("go_seq");
			Out.PrtSpMsg(2, "Removing GO info...");
			mDB.tableDelete("go_info");
			Out.PrtSpMsg(2, "Removing GO paths...");
			mDB.tableDelete("go_graph_path");
			Out.PrtSpMsg(2, "Add new columns...");
			// bestEval can be double, or boolean 
			mDB.tableCheckAddColumn("go_seq", "bestHITstr", "varchar(30)", "bestEval"); 
			mDB.tableCheckAddColumn("unitrans_hits", "bestGO", "tinyint default 0", "bestEval");
			mDB.executeUpdate("update info set schemver='6.0'");
		}
		catch (Exception e) {ErrorReport.die(e, "Updating for mdb6.0");}
	}
	private void addv59(DBConn mDB) {
		try {
			Out.PrtSpMsg(1, "Updating for db5.9 (slow on big databases)...");
			mDB.executeUpdate("update info set schemver='5.9'");
			
			Out.r("add column 1 of 6");
			mDB.tableCheckRenameColumn("pog_groups", "avgDiff", "score1", "float default -10000.0");
			Out.r("add column 2 of 6");
			mDB.tableCheckRenameColumn("pog_groups", "seDiff", "score2", "float default -10000.0");
			Out.r("add column 3 of 6");
			mDB.tableCheckAddColumn("pog_groups", "conSeq", "mediumText", "minPCC");
			Out.r("add column 4 of 6");
			mDB.tableCheckAddColumn("pog_groups", "conLen", "int default 0", "conSeq");
			Out.r("add column 5 of 6");
			mDB.tableCheckAddColumn("pog_groups", "sdLen", "float default -2.0", "conLen");
			Out.r("add column 6 of 6");
			mDB.tableCheckAddColumn("pog_members", "alignMap", "text", "UTstr");
		}
		catch (Exception e) {ErrorReport.die(e, "Update database v5.8");}
	}	
			
	private void addv58(DBConn mDB) {
		try {
			Out.PrtSpMsg(1, "Updating for db5.8 (slow on big databases)...");
			mDB.executeUpdate("update info set schemver='5.8'");
			mDB.tableCheckAddColumn("pairwise", "aaBest", "tinyint default 0", "aaBit"); 
			Out.PrtSpMsg(2, "Remove pairs and reload in order to create new BBH clusters");
		}
		catch (Exception e) {ErrorReport.die(e, "Update database v5.8");}
	}	
			
	// 7/31/18
	private void addv57(DBConn mDB) {
		try {
			Out.PrtSpMsg(1, "Updating for db5.7 (slow on big databases)...");
			mDB.executeUpdate("update info set schemver='5.7'");
			
			// i knew i would live to regret making percentages tinyint and would change..
			int cnt=1, tot=19;
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "ntSim", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "ntOlap1", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "ntOlap2", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "aaSim", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "aaOlap1", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "aaOlap2", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pOlap1", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pOlap2", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pCmatch", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pCsyn", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pC4d", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pC2d", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pCnonsyn", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pAmatch", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pAsub", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pAmis", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pDiffCDS", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pDiffUTR5", "float default -2.0");
			Out.rp("Change column ", cnt++, tot);
			mDB.tableCheckModifyColumn("pairwise", "pDiffUTR3", "float default -2.0");
		}
		catch (Exception e) {ErrorReport.die(e, "Error updating schema");}
	}	
	private void addv56(DBConn mDB) {
		try {
			Out.PrtSpMsg(1, "Updating for db5.6...");
			mDB.executeUpdate("update info set schemver='5.6'");
			
			// columns pGC, pCpG and nIndel are removed, but not bothering here
			mDB.tableCheckAddColumn("pairwise", "GC", "float default -2.0", "tstv");
			mDB.tableCheckAddColumn("pairwise", "CpGn", "float default -2.0", "GC");
			mDB.tableCheckAddColumn("pairwise", "CpGc", "float default -2.0", "CpGn");
			mDB.tableCheckAddColumn("pairwise", "nOpen", "smallint default -2", "nGap");
			
			String sqlU = "create table pairMap (" +
					"PAIRid 		int, " +
					"cds			text," +   // n-m:n-M###n-m   '###' delinates cds1 from cds2
					"utr5		text," +
					"utr3		text," +
					"index idx1(PAIRid) " +
				") ";
			if (!mDB.tableExist("pairMap")) mDB.executeUpdate(sqlU);
			
		}
		catch (Exception e) {ErrorReport.die(e, "Error updating schema");}
	}
	
	/**********************************************************
	 * These changes are part of v2.6. (post date 6/6/18 - first release of v2.6)
	 * Was 5.3, made 5.4 for a few more changes
	 */
	private void addv55(DBConn mDB) {
		try {
			Out.PrtSpMsg(1, "Updating for db5.5...");
			mDB.executeUpdate("update info set schemver='5.5'");
			
			// v5.5 
			mDB.executeUpdate("alter table pairwise add index(HITid) ");
			int max = mDB.executeCount("select MAX(HITid) from unique_hits");
			int row = mDB.executeCount("select count(*) from unique_hits");
			if (max==row) removeExtra(row, mDB);
			
			// v5.4
			mDB.tableCheckAddColumn("info", "grpSQL", "text", "");
			mDB.tableCheckAddColumn("info", "pairSQL", "text", "");
			mDB.tableCheckAddColumn("info", "seqSQL",  "text", "");
			
			// not sure when I added these, but they were in Pairwise
			mDB.tableCheckAddColumn("info", "aaPgm", "tinytext", "kaksInfo");
			mDB.tableCheckAddColumn("info", "ntPgm", "tinytext", "aaPgm");
			mDB.tableCheckAddColumn("info", "aaInfo", "tinytext", "kaksInfo");
			mDB.tableCheckAddColumn("info", "ntInfo", "tinytext", "aaInfo");
			
			// v5.3
			mDB.tableCheckAddColumn("pog_groups", "minPCC", "double default 0.0", "perPCC");
			mDB.executeUpdate("ALTER TABLE pog_method MODIFY prefix VARCHAR(10)");
		}
		catch (Exception e) {ErrorReport.die(e, "Error updating schema");}
	}
	// remove all unique_hits not used - this  is also in LoadSingleTCW
	private void removeExtra(int nRow, DBConn mDB) {
		try {
			Out.PrtSpMsg(1, "Removing unused hits from " + nRow);
			HashSet <Integer> keep = new HashSet<Integer> ();
			ResultSet rs = mDB.executeQuery("select unique_hits.HITid from unique_hits " +
					"join unitrans_hits on unitrans_hits.HITid = unique_hits.HITid");
			while (rs.next()) keep.add(rs.getInt(1));
			int cnt=0;
			for (int i=0; i<nRow; i++) {
				if (!keep.contains(i)) {
					mDB.executeUpdate("delete from unique_hits where HITid=" + i);
					cnt++;
					if (cnt%1000==0) Out.r("Remove " + cnt);
				}
			}
			Out.PrtSpMsg(1, "Removed " + cnt);
		}
		catch (Exception e) {ErrorReport.die(e, "Error updating schema");}
	}
	private void tooOldDie(DBConn mDB) {
		try {
			if (mDB.tableColumnExists("pairwise", "alignCDS1")) 
				ErrorReport.die("Database is too old to update - rebuild");
			if (!mDB.tableExist("go_info"))  
				ErrorReport.die("Database is too old to update - rebuild");
		}
		catch (Exception e) {ErrorReport.die(e, "Error updating schema");}
	}
}
