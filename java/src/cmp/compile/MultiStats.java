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
				
			Out.r("                                                ");
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
			MultiAlignData multiObj = new MultiAlignData(Globals.Ext.MAFFT, true /* from runMulti */);
			
			String [] score = multiObj.getScoreMethods().split(";");
			
			infoObj.updateInfoKey("MSAscore1", score[0]); 
			infoObj.updateInfoKey("MSAscore2", score[1]); 
			
			// if alignment failed
			PreparedStatement psF = mDB.prepareStatement("update pog_groups set " +
				"conLen=0, sdLen=0, " + "score1=" + Globalx.dNoScore +    ", score2=" + Globalx.dNoVal + " where PGid=?");
			
			// alignment for group
			PreparedStatement psG = mDB.prepareStatement("update pog_groups set " +
				"conLen=?, sdLen=?, score1=?, score2=?, conSeq=? where PGid=?");
			
			// alignment for member
			PreparedStatement psM = mDB.prepareStatement("update pog_members set " +
					" alignMap=? where UTstr=? and PGid=?");
			
			int cnt=0, cntErr=0;
			mDB.openTransaction(); 
			for (int grpID=nGrpMin; grpID<=nGrpMax; grpID++) {
				if (cntErr>20) return "Too many alignment errors - aborting";
				
				int hasRun = mDB.executeInteger("select conLen from pog_groups where PGid=" + grpID);
				if (hasRun>0) continue;
				
				nSeqs = loadSeqFromDB(grpID);
				
				cnt++;
				Out.r("Process cluster #" + cnt + " with " + nSeqs + " members ");
				
			// Alignment
				if (!runAlign(grpID, multiObj)) { 
					psF.setInt(1, grpID);
					psF.execute();
					cntErr++;
					continue;
				}	
				String [] alignedSeqs =  multiObj.getAlignSeq();
				String [] alignedNames = multiObj.getSequenceNames();
				double score1 =       multiObj.getMSA_Score1();
				double score2 =       multiObj.getMSA_Score2();
				
				psG.setInt(1, alignedSeqs[0].length());
				psG.setDouble(2, sdLen);
				psG.setDouble(3, score1);
				psG.setDouble(4, score2);
				psG.setString(5, alignedSeqs[0]);
				psG.setInt(6, grpID);
				psG.execute();
				
				for (int pos=1; pos<alignedSeqs.length; pos++) {
					String map = pos + Share.DELIM + Share.compress(alignedSeqs[pos]);
					psM.setString(1, map);
					psM.setString(2, alignedNames[pos]);
					psM.setInt(3, grpID);
					psM.addBatch();
				}
				psM.executeBatch();
			}
			mDB.closeTransaction(); 
			
			mDB.executeUpdate("update info set hasMA=1"); // CAS310 add
			
			return "Complete multi-align of " + cnt + " clusters";
		 }
		 catch(Exception e) {ErrorReport.die(e, "run align and score"); return "Error";}
	 }
	
	 //-- muscleObj and mafftObj write the sequences to file, align and score --//
	 private boolean runAlign(int grpID, MultiAlignData multiObj) {
		try {
			// Sequences to be aligned
			String [] name = seqName.toArray(new String[nSeqs]);
			String [] seqs = seqStr.toArray(new String[nSeqs]);
			double [] len = new double [seqStr.size()];
			double sumLen=0;
			
			// Execute Mafft
			multiObj.setAlignPgm(Globals.Ext.MAFFT); // clears
			for (int s=0; s<nSeqs; s++)  {
				multiObj.addSequence(name[s], seqs[s]);
				len[s] =  seqs[s].length(); // for sdLen
				sumLen += seqs[s].length();
			}
			int rc = multiObj.runAlignPgm(false, false, true); // prtCmd, prtScore, isAA
			
			// Execute Muscle if Mafft failes
			if (rc!=0) { 
				String cl = mDB.executeString("select PGstr from pog_groups where PGid=" + grpID);
				Out.prt("*** MAFFT failed for cluster " + cl + " -- try MUSCLE..."); 
				
				multiObj.setAlignPgm(Globals.Ext.MUSCLE); // clears, so re-add sequences
				for (int s=0; s<nSeqs; s++)  multiObj.addSequence(name[s], seqs[s]); 
				
				int rc2 = multiObj.runAlignPgm(false /*prtCmd*/, false/*prtScore*/, true/*isAA*/);
				
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
			MultiAlignData multiObj =  new MultiAlignData(true /* is run */); // set and print score type
				
			String [] score = multiObj.getScoreMethods().split(";"); 
			infoObj.updateInfoKey("MSAscore1", score[0]); 
			infoObj.updateInfoKey("MSAscore2", score[1]); 
			
			double def1 = Globalx.dNoScore;
			if (!score[0].contentEquals(Globals.MSA.SoP)) def1 = Globalx.dNoVal;
			mDB.executeUpdate(
					"update pog_groups set score1=" + def1 + ", score2=" + Globalx.dNoVal);
			
			PreparedStatement psS = mDB.prepareStatement(
					"update pog_groups set score1=?, score2=? where PGid=?");
			
			int cnt=0,  fail=0, cntAdd=0, cntBad=0;
			for (int grpID=nGrpMin; grpID<=nGrpMax; grpID++) {
				if (fail>20) Out.die("Too many errors");
				
				multiObj.clear();
				
				loadSeqFromDB(grpID);
				
				for (int s=0; s<seqName.size(); s++)  
					multiObj.addSequence(seqName.get(s), seqStr.get(s)); // need sequence to create each alignment
				
				if (!multiObj.getAlignFromDB(mDB, grpID)) {fail++; continue;} // maps alignment to seq
				
				if (!multiObj.scoreOnly())                {fail++; continue;}
				
				double score1 = multiObj.getMSA_Score1();
				if (score1==Globalx.dNoScore) cntBad++;
				double score2 = multiObj.getMSA_Score2();
				
				psS.setDouble(1, score1);
				psS.setDouble(2, score2);
				psS.setInt(3, grpID);
				psS.addBatch();
				cntAdd++;
				if (cntAdd==1000) {
					psS.executeBatch();
					cntAdd=0;
				}
				cnt++;
				if (cnt % 1000==0) {
					if (cntBad>0) Out.r("Scored " + cnt + " Skip SoP " + cntBad);
					else Out.r("Scored " + cnt);
				}
			}
			if (cntAdd>0) psS.executeBatch();
			
			Out.PrtSpCntMsgNz(1, cntBad, "Too many gaps - no SoP score");
			return "Complete scoring of " + cnt + " clusters";
		 }
		 catch(Exception e) {ErrorReport.die(e, "run align");}
		 return "Error";
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
