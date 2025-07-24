package sng.assem;

import java.sql.ResultSet;
import java.util.Comparator;
import java.util.HashSet;
import java.io.BufferedWriter;

import sng.assem.enums.*;
import sng.assem.helpers.*;
import util.database.DBConn;


// Weighted edge between two contigs, during TC iteration.
// Note these are Contig objects which can have one or two subcontigs.

public class Edge implements Comparable <Edge>
{
	Contig mC1 = null;
	Contig mC2 = null;
	int mScore =0;  // score will stay 0 for the links that get added just for mate pairs
	int mID; 
	boolean mAttempted = false;
	boolean mInProgress = false;
	AssemMain mAssem;
	int mTCID;
	int mOlap = 0;
	int mIdentity = 0;
	int mHang = 0;
	
	public Edge(Contig c1, Contig c2, int score, AssemMain assem, int tcid) throws Exception
	{
		mAssem = assem;
		mTCID = tcid;
		mC1 = c1;
		mC2 = c2;
		mScore = score;
		if (mC1 == null || mC2 == null) 
		{
			throw(new Exception("Invalid edge created!"));
		}
	}
	public String idStr() throws Exception
	{
		String ret = mC1.idStr() + "+" + mC2.idStr();
		return ret;
	}
	public boolean intersectingEdge(Edge e2)
	{
		if (e2.mC1 == mC1 || e2.mC1 == mC2 || e2.mC2 == mC1 || e2.mC2 == mC2)
		{
			return true;
		}
		return false;
	}
	// Update the edge for prior merges, including updating it in the database. 
	public boolean adjustForMerges(ID2Obj<Integer> mergesDone, ID2Obj<Contig> id2ctg, boolean redundantOnly, DBConn db) throws Exception
	{
		if (mAttempted)
		{
			return false;
		}
		boolean changed = false;

		if (mergesDone.containsKey(mC1.mID))
		{
			int newID1 = getMergedID(mC1.mID,mergesDone);
			if (!id2ctg.containsKey(newID1)) throw(new Exception("Merge result contig " + newID1 + " not in contig list"));
			mC1 = id2ctg.get(newID1);
			changed = true;
		}
		if (mergesDone.containsKey(mC2.mID))
		{
			int newID2 = getMergedID(mC2.mID,mergesDone);
			if (!id2ctg.containsKey(newID2)) throw(new Exception("Merge result contig " + newID2 + " not in contig list"));
			id2ctg.get(newID2).totalSize();
			mC2 = id2ctg.get(newID2);
			changed = true;
		}		
		
		
		if (changed)
		{
			setAttempted(db,0,"merged","");
		}
		return changed;
	}
	
	// This method is only used when the edge has been modified by a merge
	// and needs to be replaced by a new edge. 

	public void upload(DBConn db) throws Exception
	{
			// check that its not already redundant
		ResultSet rs = db.executeQuery("select * from ASM_tc_edge where SCID1=" + mC1.mID + " and SCID2=" + mC2.mID);
		if (rs.next())
		{
			mID = 0;
			return;
		}
		rs.close();
		try
		{
			db.executeUpdate("insert into ASM_tc_edge (TCID,SCID1,SCID2,score,errstr,errinfo,EID_from) " +
				" values(" + mTCID + "," + mC1.mID + "," + mC2.mID + "," + mScore + ",'',''," + mID + ")");		
			mID = db.lastID();
		}
		catch(Exception e1)
		{
			Log.msg("SCID1:" + mC1.mID + "  SCID2:" + mC2.mID,LogLevel.Detail);
			throw(e1);
		}
	}
	static public int getMergedID(int id, ID2Obj<Integer> mergesDone) throws Exception
	{
		int newID = id;
		while (mergesDone.containsKey(newID))
		{
			Integer _newID = mergesDone.get(newID);
			if (_newID == null) break;
			newID = _newID;
		}
		return newID;
	}	

	public int totalSize() 
	{
		return mC1.totalSize() + mC2.totalSize();
	}


	// Write the clones linked by this contig, in preparation for assembly.
	public int writeClones(BufferedWriter w, BufferedWriter qw, DBConn db) throws Exception
	{
		int numWritten = 0;
		HashSet<SubContig> ctgWritten = new HashSet<SubContig>();
		numWritten += mC1.writeClones(w, qw, db, ctgWritten);
		numWritten += mC2.writeClones(w, qw, db, ctgWritten);	
		ctgWritten.clear();
		ctgWritten = null;
		return numWritten;
	}
	// Merges can make both sides of an edge the same
	public boolean nullMerge() throws Exception
	{
		return (mC1 == mC2);
	}
	public void setAttempted(DBConn db, int succeeded,String errStr, String errInfo) throws Exception
	{
		if (mAttempted)
		{
			throw(new Exception("set attempted twice on edge " + mID));
		}
		mAttempted = true;
		db.executeUpdate("update ASM_tc_edge set attempted=1,succeeded=" + succeeded + 
				",errstr='" + errStr + "',errinfo='" + errInfo + "' where EID=" + mID);

	}
	// Indicates whether we expect two contigs from the assembly for this edge. 
	// If both contigs have only one subcontig, then we should not accept two contigs output.
	public boolean isPaired()
	{
		if (mC1.mSC2 != null || mC2.mSC2 != null) return true;
		return false;
	}

	public int compareTo(Edge e)
	{
		int scorea = mScore;
		int scoreb = e.mScore; // CAS304 (Edge)e
		if (scorea > scoreb) return -1;
		else if (scorea < scoreb) return 1;
		return 0;	
	}
	static class ScoreCmp implements Comparator<Edge>
	{
		public ScoreCmp()
		{
		}
		public int compare(Edge a, Edge b)
		{
			int scorea = a.mScore;
			int scoreb = b.mScore;
			if (scorea > scoreb) return -1;
			else if (scorea < scoreb) return 1;
			return 0;
		}
	}
	static class SizeCmp implements Comparator<Edge>
	{
		public SizeCmp()
		{
		}
		public int compare(Edge a, Edge b) 
		{
			int size1 = a.totalSize();
			int size2 = b.totalSize();

			if (size1 < size2) return -1;
			else if (size2 < size1) return 1;
			return 0;

		}
	}	

}
