package sng.assem;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import sng.assem.enums.*;
import sng.assem.helpers.*;
import util.database.DBConn;



// A cluster of contigs (or paired contigs; the Contig class) during TC iteration.
// 
public class Cluster
{
	AutoArray<Edge> mEdges;
	AutoArray<Contig> mContigs;
	int mID = 0;
	int mTCID;
	DBConn mLocalDB;
	AssemMain mAssem;
	int mThreadNum = 0;
	int mEdgesDone = 0;
	int mCurEdgeIdx = 0;
	
	public Cluster(int tcid,DBConn db,AssemMain assem)
	{
		mTCID = tcid;
		mEdges = new AutoArray<Edge>();
		mEdges.setSize(10);
		mContigs = new AutoArray<Contig>();
		mContigs.setSize(10);
		mLocalDB = db;
		mAssem = assem;
	}
	public void setDB(DBConn db)
	{
		mLocalDB = db;
	}



	public void clear()
	{
		mEdges.clear();
		mEdges = null;
		mContigs.clear();
		mContigs = null;
	}

	private Edge getNextEdge() throws Exception
	{
//		for (int i = mAssem.mID2Contig.minKey(); i <= mAssem.mID2Contig.maxKey(); i++)
//		{
//			if (mAssem.mID2Contig.containsKey(i))
//			{
//				mAssem.mID2Contig.get(i).totalSize();
//			}
//		}
//				
		Edge ret = null;
		for (; mCurEdgeIdx < mEdges.size(); mCurEdgeIdx++)
		{
			Edge e = mEdges.get(mCurEdgeIdx);
			e.idStr();
			e.adjustForMerges(mAssem.mMergesDone, mAssem.mID2Contig,false,mLocalDB);
			e.idStr();
			if (!e.mAttempted)
			{
				ret = e;
				break;
			}
		}
		return ret;

	}

	public void doAssembly(int TCID, File capTopDir, String tcstr, int tcnum, boolean strict) throws Exception
	{
		
		int[] ws = {7,12,50,30,20,10,10,10};
		int redund = 0;
		int i = 0;
		mEdgesDone = 0;
		Edge e = null;
		mEdges.sort(new Edge.ScoreCmp());
		int nLeft = mEdges.size();
		
		boolean bHeuristics = !mAssem.mProps.getProperty("HEURISTICS").equals("0");
		boolean bTwoBridge = mAssem.mProps.getProperty("REQUIRE_TWO_BRIDGE").equals("1");
		boolean bNoTest4 = mAssem.mProps.getProperty("NO_TEST4").equals("1");


		while ( (e = getNextEdge()) != null) 
		{			
			i++;

			String notes = tcstr + ":" + e.idStr();
	
			nLeft--;

			int capDirPrefix = e.mID/1000;
			File capDirTop = new File(capTopDir,"E" + capDirPrefix);
			Utils.checkCreateDir(capDirTop);
			File capdir = new File(capDirTop,"" + e.mID);
			Utils.checkCreateDir(capdir);
			Utils.clearDir(capdir);

			
			String caproot = "Edge" + e.mID + ".fasta";
			mLocalDB.executeUpdate("update ASM_tc_iter set merges_tried=merges_tried+1 where tcid=" + TCID);
			Utils.intTimerStart(mThreadNum);
			tcAssemble(e, capdir, tcstr, caproot);
			Utils.intTimerEnd(mThreadNum,"tcAssemble");
			
			AceFile aceFile = new AceFile(mAssem.mAID,mAssem.mIDStr, new File(capdir,caproot + ".cap.ace"),mAssem.mID2Clone,mAssem.mName2Clone,tcstr,mLocalDB);
			aceFile.doParse(false,0);
			if (aceFile.mContigs.size() == 0)
			{
				throw(new Exception(capdir + " no contigs at all!!"));
			}	
			if (aceFile.OK_CTG(e.totalSize(), e.isPaired(),strict,bHeuristics,bTwoBridge,bNoTest4,mAssem.mPairs))
			{
				Utils.intTimerStart(mThreadNum);
				Contig ctg = null;
				synchronized(mAssem) // may as well; otherwise we'll just be locking these tables anyway
				{
					Utils.intTimerEnd(mThreadNum,"mergeBlock");
					
					// Use a transaction to guarantee the contig, ASM_scontig, contclone tables are updated consistently
					mLocalDB.openTransaction();
					try
					{
						Utils.intTimerStart(mThreadNum);

						// Merge the contigs. Note that buried get left behind, to be collected later. 
						e.setAttempted(mLocalDB, 1,aceFile.mOK.toString(), aceFile.mOKInfo);
						SubContig sc1 = aceFile.mContigs.get(0);
						SubContig sc2 = (aceFile.mContigs.size() > 1 ? aceFile.mContigs.get(1) : null);
						sc1.mSource = notes;
						sc1.mTCID = TCID;
						sc1.doUpload(mLocalDB,"",false);
						if (sc2 != null) 
						{
							sc2.mSource = notes;
							sc2.mTCID = TCID;
				
							sc2.doUpload(mLocalDB,"",false);
						}
						ctg = new Contig(mAssem.mAID,sc1,sc2);
						ctg.upload(mLocalDB,TCID);
						mAssem.mID2Contig.put(ctg.mID,ctg);
						doMerge(e,ctg,mAssem.mMergesDone, mTCID);
						mLocalDB.closeTransaction();
						mLocalDB.executeUpdate("update ASM_tc_iter set merges_ok=merges_ok+1 where tcid=" + TCID);

						Utils.intTimerEnd(mThreadNum,"doMerge");

					}
					catch(Exception e1)
					{
						mLocalDB.rollbackTransaction();
						throw(e1);
					}
				}
				File recapdir = new File(capdir,"recap");
				Utils.checkCreateDir(recapdir);
				int nbury = 0;
				if (mAssem.mProps.getProperty("DO_CAP_BURY").equals("1"))
				{
					Utils.intTimerStart(mThreadNum);

					nbury = mAssem.capBuryContig(recapdir,ctg,"RECAP_ARGS",mLocalDB,mThreadNum,TCID);
					Utils.intTimerEnd(mThreadNum, "capBury");
				}
				Log.columns(ws, LogLevel.Dbg,mThreadNum,mID,e.idStr(), aceFile.mOK.toString(),ctg.idStr(),nbury,e.mID,aceFile.mOKInfo);

			}
			else
			{
				e.setAttempted(mLocalDB, 0,aceFile.mOK.toString(), aceFile.mOKInfo);
				Log.columns(ws, LogLevel.Detail,mThreadNum,mID,e.idStr(), aceFile.mOK.toString(),aceFile.mOKInfo,0,e.mID,aceFile.mOKInfo);
			}
			aceFile.clear();
			aceFile = null;
			if (!Utils.isDebug())
			{
				Utils.deleteDir(capdir);
			}
			mEdgesDone++;
		}
		//Log.msg("Thread " + mThreadNum + " finshed cluster " + mID + " : " + redund + " redundant edges",LogLevel.Detail);
		
	}	
	void doMerge(Edge edge, Contig newctg,ID2Obj<Integer> mergesDone,int TCID) throws Exception
	{
		mergesDone.put(edge.mC1.mID, newctg.mID);
		mergesDone.put(edge.mC2.mID, newctg.mID);
		mergesDone.checkGrow(newctg.mID); // ensure space for the new one
	
		mLocalDB.executeUpdate("update ASM_scontig set merged_to=" + newctg.mID + " where scid=" + edge.mC1.mID);
		mLocalDB.executeUpdate("update ASM_scontig set merged_to=" + newctg.mID + " where scid=" + edge.mC2.mID);
	
		mLocalDB.executeUpdate("update ASM_tc_edge set SCID_result=" + newctg.mID + " where EID=" + edge.mID);
		

	}			
	int assemblyProgress() throws Exception
	{
		if (mEdges == null) return 0;
		return (mEdges.size() - mCurEdgeIdx);
		
	}
	// Load the contig merges already done, so we can restore an interrupted assembly, for which there may be 
	// edges remaining for contigs which have already been merged.

	void initMerges(TreeMap<Integer,Integer> mergesDone) throws Exception
	{
		ResultSet rs = mLocalDB.executeQuery("select ASM_scontig.SCID, ASM_scontig.merged_to from ASM_scontig " + 
				" join tc_clust_sctg on ASM_scontig.scid=tc_clust_sctg.scid where tc_clust_sctg.CLUSTID='" + mID + "' and ASM_scontig.merged_to != ASM_scontig.SCID ");
		while (rs.next())
		{
			mergesDone.put(rs.getInt("SCID"),rs.getInt("merged_to"));
		}
		rs.close();
		if (mergesDone.size() > 0) Log.msg("Cluster " + mID + ":" + mergesDone.size() + " previous contig merges loaded",LogLevel.Detail);
	}

	void tcAssemble(Edge e, File capdir, String tcstr, String caproot) throws Exception
	{
		File ctgFile = new File(capdir,caproot);
		File qualFile = new File(capdir,ctgFile.getName() + ".qual");
		BufferedWriter ctgFileW = new BufferedWriter(new FileWriter(ctgFile));
		BufferedWriter qualFileW = new BufferedWriter(new FileWriter(qualFile));
		//Utils.intTimerStart(mThreadNum);
		int nClones = e.writeClones(ctgFileW, qualFileW, mLocalDB);
		//Utils.intTimerEnd(mThreadNum, "Twrite");
		int maxSize = Integer.parseInt(mAssem.mProps.getProperty("MAX_CAP_SIZE"));
		if (nClones > maxSize)
		{
			Log.msg("Edge " + e.mID + ":skipping large assembly of " + nClones,LogLevel.Detail);
			return;
		}


		ctgFileW.flush();
		qualFileW.flush();

		String cmd = mAssem.mProps.getProperty("CAP_CMD") + " " + ctgFile.getName() + " " + mAssem.mProps.getProperty("CAP_ARGS");
		Utils.appendToFile(cmd,capdir,"cmd.list");
		Utils.recordCmd(mAssem.mAID,tcstr.toUpperCase() + "CAP",cmd,mLocalDB);

		//Utils.intTimerStart(mThreadNum);
		int exitValue = Utils.runCommand(cmd.split("\\s+"), capdir, false, false, null,mThreadNum);
		//Utils.intTimerEnd(mThreadNum,"Tcap");
		Utils.checkExitValue(exitValue,cmd);	
		ctgFileW.close();
		qualFileW.close();
	}
	
	public static class SizeCmp implements Comparator<Cluster>
	{
		public int compare(Cluster a, Cluster b)
		{
			int scorea = a.mContigs.size();
			int scoreb = b.mContigs.size();
			if (scorea > scoreb) return -1;
			else if (scorea < scoreb) return 1;
			return 0;
		}
	}
}
