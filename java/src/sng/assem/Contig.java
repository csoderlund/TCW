package sng.assem;

import java.io.BufferedWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import sng.assem.enums.LogLevel;
import util.database.DBConn;


// Class corresponding to the 'ASM_scontig' table in the database.
// Referred to in some comments as "supercontig".

// Represents the original Pave concept of contig, which could have two paired sub-contigs.
// In this implementation they are kept separate and stored as mSC1 and mSC2. 
// At the end, they will be joined by N's. 

public class Contig
{
	SubContig mSC1 = null;
	SubContig mSC2 = null;
	int mID = 0;
	int mMergedTo = 0;
	HashMap<Contig,Edge> mLinks; // edges from this contig, during TC
	boolean mCapBuryDone = false;
	int mCapBuries = 0;
	int mIdx = -1;  // An index generated during loadContigs, which is consecutive from 0 (unlike the database ID)
	int mAID;
	int mTCID = 0;
	
	public Contig(int aid, SubContig s1, SubContig s2)
	{
		mAID = aid;
		mLinks = new HashMap<Contig,Edge>();
		if (s1 != null)
		{
			mSC1 = s1;
			if (s2 != null)
			{
				if (s1.mID < s2.mID)
				{
					mSC1 = s1;
					mSC2 = s2;
				}
				else
				{
					mSC2 = s1;
					mSC1 = s2;			
				}				
			}
		}
	}

	public String idStr()
	{
		return mID + "(" + totalSize() + ")";
	}
	public int totalSize() 
	{
		int size = 0;
		if (mSC1 != null) size += mSC1.numClones();
		if (mSC2 != null) size += mSC2.numClones();
		return size;
	}
	public int writeClones(BufferedWriter w, BufferedWriter qw, DBConn db, HashSet<SubContig> ctgWritten) throws Exception
	{
		int numWritten = 0;
		if (mSC1 != null) numWritten += mSC1.writeClones(w, qw, db, ctgWritten);
		if (mSC2 != null) numWritten += mSC2.writeClones(w, qw, db, ctgWritten);
		return numWritten;
	}
	// CASZ 1Sept19 MariaDB 10.4.7 fails to insert if an openTransaction is performed before this is called
	// some of the calls to upload had open/close and some did not; they have all been removed.
	// CASZ 24Sept19 - the doUnpairedCliqueUpload lock/unlock also screwed it up (or maybe it was only this?)
	public void upload(DBConn db, int tcid) throws Exception
	{
			int id1 = mSC1.mID;
			int id2 = (mSC2 != null) ? mSC2.mID : 0;
			
			db.executeUpdate("insert into ASM_scontig (AID,CTID1,CTID2,TCID) " +
						"VALUES(" + mAID + "," + id1 + "," + id2 + "," + tcid + ")");
			
			mID = db.lastID();
			
			db.executeUpdate("update ASM_scontig set merged_to=" + mID + " where SCID=" + mID );
			db.executeUpdate("update contig set SCTGID=" + mID + " where CTGID=" + id1);
			if (id2 > 0) 
				db.executeUpdate("update contig set SCTGID=" + mID + " where CTGID=" + id2);
	}
	public void setCapBuryDone(boolean done, DBConn db) throws Exception
	{
		mCapBuryDone = done;
		int val = (done ? 1 : 0);
		db.executeUpdate("update ASM_scontig set capbury_done=" + val + " where SCID=" + mID);
	}
	public void clearSeqs()
	{
		if (mSC1 != null)
		{
			mSC1.mCCS = "";
			mSC1.mQual = "";
		}
		if (mSC2 != null)
		{
			mSC2.mCCS = "";
			mSC2.mQual = "";
		}		
	}
	public void clearBuries()  throws Exception
	{
		mSC1.clearBuries();
		if (mSC2 != null) mSC2.clearBuries();
	}
	public void clear()  throws Exception
	{
		mSC1.clear();
		if (mSC2 != null) mSC2.clear();
	}	
	public void loadSequence(DBConn db) throws Exception
	{
		mSC1.loadCCS(db);
		if (mSC2 != null) mSC2.loadCCS(db);
	}
	static class SizeCmp implements Comparator<Contig>
	{
		public int compare(Contig a, Contig b) 
		{				
			if (a == null)
			{
				if (b == null) return 0;
				else return +1;
			}
			else if (b == null) return -1;
			int scorea = a.totalSize();
			int scoreb = b.totalSize();
			if (scorea > scoreb) return -1;
			else if (scorea < scoreb) return 1;
			return 0;
		}
	}	
}
