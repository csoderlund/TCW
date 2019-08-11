package cmp.compile;

/*********************************************************
 * Run multi aligment on all clusters in database and score.
 * Save score in database
 * to redo scores, from mysql: update pog_groups set conLen=0
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import cmp.align.MultiAlignData;
import cmp.align.Share;
import cmp.database.Globals;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Stats;

public class MultiStats {
	
	public MultiStats(DBConn db) {
		mDB=db;
	}
	public void scoreAll() { // runMulti
		try {
			long startTime = Out.getTime();
			MultiAlignData algObj =  new MultiAlignData(Globals.MultiAlign.MAFFT);
			MultiAlignData algObj2 = new MultiAlignData(Globals.MultiAlign.MUSCLE);
			String [] alignedSeqs, alignedNames;
			double score1, score2;
			
			// A cluster set may have been removed, or a new set added
			int nCnt = mDB.executeCount("select count(*) from pog_groups where conLen=0");
			int nMin = mDB.executeCount("select min(PGid) from pog_groups");
			int nMax = mDB.executeCount("select max(PGid) from pog_groups");
			Out.PrtDateMsg("Multi-align " + nCnt + " clusters");
			
			PreparedStatement ps1 = mDB.prepareStatement("update pog_groups " +
					"set conLen=0, sdLen=0, " + "score1=" + Globalx.dNoScore +    ", score2=" + Globalx.dNoVal + " where PGid=?");
			PreparedStatement ps2 = mDB.prepareStatement("update pog_groups set " +
					"conLen=?, sdLen=?, score1=?, score2=?, conSeq=? where PGid=?");
			PreparedStatement ps3 = mDB.prepareStatement("update pog_members set " +
					" alignMap=? where UTstr=? and PGid=?");
			int cnt=0;
			mDB.openTransaction(); 
			for (int grpID=nMin; grpID<=nMax; grpID++) {
				int nSeqs = loadSeqsFromDB(grpID);
				if (nSeqs<=1)  continue; // may have been run already
				
				cnt++;
				Out.r("Process cluster #" + cnt + " with " + nSeqs + " members ");
				
				String [] name = seqName.toArray(new String[seqStr.size()]);
				String [] seqs = seqStr.toArray(new String[seqStr.size()]);
				double [] len = new double [seqStr.size()];
				
				// Sequences to be aligned
				double sumLen=0;
				for (int s=0; s<nSeqs; s++)  {
					algObj.addSequence(name[s], seqs[s]);
					len[s] =  seqs[s].length();
					sumLen += seqs[s].length();
				}
				int rc = algObj.runAlignPgm(false, false, true);
				
				if (rc!=0) {
					String cl = mDB.executeString("select PGstr from pog_groups where PGid=" + grpID);
					Out.PrtError("MAFFT failed for cluster " + cl + " -- try MUSCLE...");
					
					for (int s=0; s<nSeqs; s++)  algObj2.addSequence(name[s], seqs[s]);
					int rc2 = algObj2.runAlignPgm(false, false, true);
					if (rc2!=0) {
						Out.PrtError("MUSCLE failed for cluster " + cl);
						if (rc < -1) Out.die("Fatal Error on alignments"); 
						
						ps1.setInt(1, grpID);
						ps1.execute();
						continue;
					}
					alignedSeqs =  algObj2.getSequences();
					alignedNames = algObj2.getSequenceNames();
					score1 =       algObj2.getScore1();
					score2 =       algObj2.getScore2();
					algObj2.clear();
				}
				else { // the sequences will not be in the same order
					alignedSeqs =  algObj.getSequences();
					alignedNames = algObj.getSequenceNames();
					score1 =       algObj.getScore1();
					score2 =       algObj.getScore2();
				}
				algObj.clear();
				
				int    conLen = alignedSeqs[0].length();
				double sdLen = Stats.stdDev(sumLen/(double) nSeqs, len);
				
				ps2.setInt(1, conLen);
				ps2.setDouble(2, sdLen);
				ps2.setDouble(3,score1);
				ps2.setDouble(4,score2);
				ps2.setString(5, alignedSeqs[0]);
				ps2.setInt(6, grpID);
				ps2.execute();
				
				for (int pos=1; pos<alignedSeqs.length; pos++) {
					String map = pos + Share.DELIM + Share.compress(alignedSeqs[pos]);
					ps3.setString(1, map);
					ps3.setString(2, alignedNames[pos]);
					ps3.setInt(3, grpID);
					ps3.execute();
				}
			}
			mDB.closeTransaction(); 
			
			Out.r("                                                ");
			String sum = new Summary(mDB).getMethodScoreTable();
			Out.prt(sum);
			Out.PrtDateMsgTime("Complete multi-align of " + cnt + " clusters", startTime);
		}
		catch(Exception e) {ErrorReport.reportError(e, "score all");}
	}
	
	 private int loadSeqsFromDB(int id) {
	    	ResultSet rs = null;
	   
	    	try {
	    	 	seqName.clear();
		    	seqStr.clear();
		    	
		    	int hasRun = mDB.executeInteger("select conLen from pog_groups where PGid=" + id);
		    	if (hasRun>0 || hasRun==-1) return 0;
		    	
		    	rs = mDB.executeQuery("Select unitrans.UTstr, aaSeq from unitrans " +
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
	 private Vector<String> seqStr =   new Vector <String> ();
	 private Vector<String> seqName =  new Vector <String> ();
	 private DBConn mDB = null;
}
