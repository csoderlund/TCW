package cmp.compile;

/*********************************************************
 * Run multi aligment on all clusters in database and score.
 * Save score in database
 * CAS312 add score only; add save column scores to DB
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import cmp.align.MultiAlignData;
import cmp.align.Share;
import cmp.compile.panels.CompilePanel;
import cmp.database.Globals;
import cmp.database.DBinfo;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Stats;

public class MultiStats {
	
	public MultiStats(CompilePanel theCompilePanel) {
		mDB=theCompilePanel.getDBconn();
		infoObj = theCompilePanel.getDBInfo();
	}
	public void scoreAll(boolean bRedoScores) { // runMulti
		try {
			long startTime = Out.getTime();
			String msg="";
			
			if (bRedoScores) nGrpIdx = mDB.executeCount("select count(*) from pog_groups");
			else             nGrpIdx = mDB.executeCount("select count(*) from pog_groups where conLen=0");
			nGrpMin = mDB.executeCount("select min(PGid) from pog_groups");
			nGrpMax = mDB.executeCount("select max(PGid) from pog_groups");
			Out.PrtDateMsg("Multi-align " + nGrpIdx + " clusters");
			
			if (bRedoScores) 
				msg = redoAllScoresSave();
			else             
				msg = runAlignScoreSave();
				
			Out.rClear();
			String sum = new Summary(mDB).getMethodScoreTable();
			Out.prt(sum);
			Out.PrtMsgTimeMem(msg, startTime);
		}
		catch(Exception e) {ErrorReport.reportError(e, "score all");}
	}
	/********************************************************************
	 * XXX Align and save
	 */
	 private String runAlignScoreSave() {
		 try {
			MultiAlignData multiObj = new MultiAlignData();
			
			String [] score = multiObj.getScoreMethods().split(";");
			
			infoObj.updateInfoKey("MSAscore1", score[0]); 
			infoObj.updateInfoKey("MSAscore2", score[1]); 
			
			// if alignment failed
			PreparedStatement psF = mDB.prepareStatement("update pog_groups set " +
				"conLen=0, sdLen=0, " + "score1=" + Globalx.dNoScore +    ", score2=" + Globalx.dNoVal + " where PGid=?");
			
			// alignment for group - group already exists
			PreparedStatement psG = mDB.prepareStatement("update pog_groups set " +
				"conLen=?, sdLen=?, score1=?, score2=?, conSeq=? where PGid=?");
			
			// alignment for member - members already exist
			PreparedStatement psM = mDB.prepareStatement("update pog_members set " +
					" alignMap=? where UTstr=? and PGid=?");
			
			int cnt=0, cntSeq = 0, cntPrt=100, cntErr=0;
			mDB.openTransaction(); 
			for (int grpID=nGrpMin; grpID<=nGrpMax; grpID++) 
			{
				int hasRun = mDB.executeInteger("select conLen from pog_groups where PGid=" + grpID);
				if (hasRun!=0) continue; // CAS326 -1 not exist; >0 been done; 0 exist with no consensus
				
				multiObj.clear();
				cnt++;
				
				nSeqs = loadSeqFromDB(grpID); // global seqName and seqStr
				if (nSeqs==0) continue; // some may have been removed
				
				cntSeq+=nSeqs;
				cntPrt+=nSeqs;
				if (cntPrt>=100) { // CAS326
					Out.r("Process clusters " + cnt + " and sequences " + cntSeq);
					cntPrt=0;
				}
				
			// Alignment
				if (!runAlign(grpID, multiObj)) { 
					psF.setInt(1, grpID);
					psF.execute();
					cntErr++;
					if (cntErr>20) return "Too many alignment errors - aborting";
					else continue;
				}	
				String [] alignedSeqs =  multiObj.getAlignSeq();
				String [] alignedNames = multiObj.getSequenceNames();
				
				double score1 =       multiObj.getMSA_gScore1();
				double score2 =       multiObj.getMSA_gScore2();
				
				psG.setInt(1, alignedSeqs[0].length());
				psG.setDouble(2, sdLen);
				psG.setDouble(3, score1);
				psG.setDouble(4, score2);
				psG.setString(5, alignedSeqs[0]);
				psG.setInt(6, grpID);
				psG.execute();
				
				// save aligned sequences
				for (int pos=1; pos<alignedSeqs.length; pos++) { 
					String map = pos + Share.DELIM + Share.compress(alignedSeqs[pos]);
					psM.setString(1, map);
					psM.setString(2, alignedNames[pos]);
					psM.setInt(3, grpID);
					psM.addBatch();
				}
				psM.executeBatch();
				
				saveScores(grpID, multiObj); // CAS313
			}
			mDB.closeTransaction(); 
			
			mDB.executeUpdate("update info set hasMA=1"); // CAS310 add
			
			return "Complete multi-align of " + cnt + " clusters";
		 }
		 catch(Exception e) {ErrorReport.die(e, "run align and score"); return "Error";}
	 }
	
	 //-- write the sequences to file, align and score --//
	 private boolean runAlign(int grpID, MultiAlignData multiObj) {
		try {
			// Sequences to be aligned
			String [] name = seqName.toArray(new String[nSeqs]);
			String [] seqs = seqStr.toArray(new String[nSeqs]);
			double [] len = new double [seqStr.size()];
			double sumLen=0;
			
			// Execute Mafft
			for (int s=0; s<nSeqs; s++)  {
				multiObj.addSequence(name[s], seqs[s]);
				len[s] =  seqs[s].length(); // for sdLen
				sumLen += seqs[s].length();
			}
			int rc = multiObj.runAlignPgm(Globals.Ext.MAFFT, true); // isAA
			
			// Execute Muscle if Mafft fails
			if (rc!=0) { 
				String cl = mDB.executeString("select PGstr from pog_groups where PGid=" + grpID);
				Out.prt("*** MAFFT failed for cluster " + cl + " -- try MUSCLE..."); 
				
				multiObj.clear();
				for (int s=0; s<nSeqs; s++)  multiObj.addSequence(name[s], seqs[s]); 
				
				int rc2 = multiObj.runAlignPgm(Globals.Ext.MUSCLE, true/*isAA*/);
				
				if (rc2!=0) {
					Out.prt("*** MUSCLE failed for cluster " + cl);
					if (rc < -1) Out.die("Fatal Error on alignments"); 
					return false;
				}
			}
			sdLen = Stats.stdDev(sumLen/(double) nSeqs, len);
			if (Double.isNaN(sdLen)) {
				sdLen = Globalx.dNoVal;
				String cl = mDB.executeString("select PGstr from pog_groups where PGid=" + grpID);
				Out.prt("*** stLen failed for cluster " + cl + " -- set to " + sdLen); 
			}
			return true;
		}
		catch(Exception e) {ErrorReport.die(e, "run align");}
	    return false;
	 }
	 /***********************************************************
	  * Score only
	  */
	 private String redoAllScoresSave() {
		 try {
			MultiAlignData multiObj =  new MultiAlignData(); 
				
			String [] score = multiObj.getScoreMethods().split(";"); 
			infoObj.updateInfoKey("MSAscore1", score[0]); 
			infoObj.updateInfoKey("MSAscore2", score[1]); 
			
			// clear current scores
			mDB.tableDelete("pog_scores"); // CAS342; CAS401 change dNoVal to dNoScore
			mDB.executeUpdate("update pog_groups set score1=" + Globalx.dNoScore + ", score2=" + Globalx.dNoScore);
			
			PreparedStatement psG = mDB.prepareStatement(
					"update pog_groups set score1=?, score2=? where PGid=?");
			
			int cnt=0,  fail=0, cntBad=0;
			for (int grpID=nGrpMin; grpID<=nGrpMax; grpID++) 
			{	
				multiObj.clear();
				
				loadSeqFromDB(grpID); // seqName and seqStr
				if (seqName.size()==0) continue; // some may have been removed
				
				for (int s=0; s<seqName.size(); s++)  
					multiObj.addSequence(seqName.get(s), seqStr.get(s)); // need sequence to create each alignment
				
				if (!multiObj.loadAlignFromDB(mDB, grpID)) {
					fail++; 
					if (fail>20) Out.die("Too many errors");
					else continue;
				} 
				
				if (!multiObj.scoreOnly())                {fail++; continue;}
				
				double score1 = multiObj.getMSA_gScore1();
				if (score1==Globalx.dNoScore) cntBad++;
				double score2 = multiObj.getMSA_gScore2();
				
				psG.setDouble(1, score1);
				psG.setDouble(2, score2);
				psG.setInt(3, grpID);
				psG.execute();
				
				saveScores(grpID, multiObj);
				
				cnt++;
				if (cnt % 1000==0) {
					if (cntBad>0) Out.r("Scored " + cnt + " Skip SoP " + cntBad);
					else Out.r("Scored " + cnt);
				}
			}
			
			Out.PrtSpCntMsgNz(1, cntBad, "Too many gaps - no SoP score");
			return "Complete scoring of " + cnt + " clusters";
		 }
		 catch(Exception e) {ErrorReport.die(e, "redo align ");}
		 return "Error";
	 }
	 private boolean saveScores(int grpid, MultiAlignData multiObj) {
		 try {
			 String strScore1=multiObj.getColScores1();
			 String strScore2=multiObj.getColScores2();
			
			 mDB.executeUpdate("insert into pog_scores set PGid=" + grpid + 
					", score1='" + strScore1 + "', score2='" + strScore2 + "'");
			 return true;
		 }
		 catch(Exception e) {ErrorReport.die(e, "run align save " + multiObj.getColScores1()
		 + " for " + grpid); return false;}
	 }
	 /*********************************************************/
	 //-- load group sequences --/
	 private int loadSeqFromDB(int id) {
    	try {
    	 	seqName.clear();
	    	seqStr.clear();
	    
	    	ResultSet rs = mDB.executeQuery("Select unitrans.UTstr, aaSeq from unitrans " +
	    			" join pog_members on unitrans.UTid = pog_members.UTid " +
	    			" where PGid=" + id );
	   
	    	while (rs.next()) {
	    		String name = rs.getString(1);
	    		String seq = rs.getString(2);
    			if (seq!=null && !seq.equals("")) {
    				seqName.add(name);
    				seqStr.add(seq);
    			}
	    	}
    		return seqStr.size(); 
    	}
    	catch(Exception e) {ErrorReport.die(e, "load sequences");}
    	return -1;
	 }
	 /********************************************************/
	 private double sdLen=0.0; // align standard deviation of MSA lengths
	 
	 private Vector<String> seqStr =   new Vector <String> (); // unaligned sequences
	 private Vector<String> seqName =  new Vector <String> ();
	 
	 private int nSeqs=0, nGrpIdx=0, nGrpMin, nGrpMax;
	 private DBConn mDB = null;
	 private DBinfo infoObj = null;
}
