package sng.assem;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Properties;
import java.sql.ResultSet;

import sng.assem.enums.*;
import util.database.DBConn;

public class Clone
{
	static Properties mProps = null;

	// these members used in all instances
	String mCloneName;      // name, without r/f
	String mFullName;  		// renamed to .r, .f
	String mOrigName;
	RF mRF;
	String mSeq = "";
	String mQual = "";
	int mSeqLen = 0;		// this should always be correct, even if full sequence not loaded
	int mQualLen = 0;
	int mID = 0;
	
	// These members used in assembly (not library load)
	int mMateID = 0;
	int mBuryCode = 0;
	Clone mParent = null;
	Clone mParent1 = null; 
	boolean mIsParent = false;
	boolean mFlipped = false;
	Clone mMate = null;
	MatePair mPair = null;
	TreeSet<Integer> mKids; 
	TreeSet<Integer> mParents;
	int mBuryLevel = 0;
	int mVMID = 0;
	Library mLib = null;
	int mLID;
	int mStart = 1;
	int mQStart = 1;
	int mEnd = 1;
	int mQEnd = 1;
	UC mUC = null;
	int mNGaps = 0;
	int mNTgaps = 0;
	int mMaxRight = 0; 
	int mNMis = 0;
	int mNExtras = 0;
	int mSCID = 0;
	SubContig mSC = null;
	boolean mBuried = false;
	String mGaps = "";
	String mTGaps = "";
	String mQueryLine = "";
	String mTargetLine = "";
	Vector<Integer> mGapArray;
	Vector<Integer> mTGapArray;
	int mNParents = 0;
	boolean mNoAlign = false; 
	
	// Called from load library; mID is set after sequence is added to db
	public Clone(Properties props, String origname,  String clonename, String dbName, RF rf)
	{
		if (mProps == null) mProps = props;
		
		mCloneName = clonename; // may have .r and .f removed
		mFullName = dbName; 	// may have '-' replaced; may have suffix replace with .r and .f
		mOrigName = origname;
		mRF = rf;				// R, F or UNK		
	}
	
	// Called from assembly
	public Clone(String fullname, String clonename, RF rf, int id, int mate_id, String seq,  int seqlen, Library lib, int libID, DBConn db)
	{
		mCloneName = clonename;
		mFullName = fullname;
		mID = id;
		mMateID = mate_id;
		mSeq = seq;
		mSeqLen = seqlen;
		mKids = new TreeSet<Integer>();
		mParents = new TreeSet<Integer>();
		mRF = rf;
		mLib = lib;
		mLID = libID;
	}

	public void checkQual(boolean fatal) 
	{
		int qlen = mQual.split("\\s+").length;
		String msg = mFullName + ": seq:" + mSeqLen + " qual:" + qlen;
		if (mSeqLen != qlen) Log.die(msg);
	}

	public void loadSequences(DBConn db) throws Exception
	{
		ResultSet rs = db.executeQuery("select sequence,quality from clone where cid=" + mID);
		rs.first();
		mSeq = rs.getString("sequence");
		mQual = rs.getString("quality");
		mSeqLen = mSeq.length();
		rs.close();
	}
	public void clearSequences()
	{
		mSeq = "";
		mQual = "";
	}
	public String getQualFromDB(DBConn db) throws Exception
	{
		String ret = "";
		ResultSet rs = db.executeQuery("select quality from clone where cid=" + mID);
		if (rs.first())
		{
			ret = rs.getString("quality");
		}
		if (ret.equals(""))
		{
			throw(new Exception("empty quality for clone " + mID));
		}
		return ret;
	}
	public boolean isBuryable(Clone parent, int hang) throws Exception
	{
		boolean ret = true;
		if (parent.mSCID != mSCID)
		{
			throw(new Exception("Attempt to bury " + mID + " in " + parent.mID + " in different contigs (" + mSCID + "," + parent.mSCID + ")"));
		}
		int hang1 = Math.max(0, parent.mStart - mStart);
		int hang2 = Math.max(0, mEnd - parent.mEnd);
		ret = (hang1 + hang2 <= hang);
		return ret;
	}
	public void dumpCoords(LogLevel lvl)
	{
		Log.msg(mFullName + " tstart:" + mStart + " tend:" + mEnd + " qstart:" + mQStart + " qend:" + mQEnd,lvl);	
	}
	public void flipGaps()
	{
		String[] gapArray = mGaps.trim().split("\\s+");
		String out = "";
		for (int i = 0; i < gapArray.length; i++)
		{
			if (gapArray[i].equals("")) continue;
			int gap = Integer.parseInt(gapArray[i]);
			gap = mSeqLen - gap -1;
			out = " " + gap + out; 
		}
		mGaps = out;
	}
	
	// Sort by left end ascending, right end descending
	// This defines the order of possible buries (a clone can be buried in a previous clone in this order)
	public static class CtgPosCmp implements Comparator<Clone>
	{
		public int compare(Clone a, Clone b)
		{
			if (a.mStart < b.mStart) return -1;
			else if (a.mStart > b.mStart) return +1;
			else if (a.mEnd > b.mEnd ) return -1;
			else if (a.mEnd < b.mEnd) return +1;
			return 0;
			
		}
	}
	// Sort by mismatches+gaps descending, then by length ascending
	// This is the order in which clones will be considered for cap burying
	public static class MismatchLengthCmp implements Comparator<Clone>
	{
		public int compare(Clone a, Clone b)
		{
			if (a.mNGaps > b.mNGaps) return -1;
			else if (a.mNGaps < b.mNGaps) return +1;
			else if (a.mSeqLen < b.mSeqLen) return -1;
			else if (a.mSeqLen > b.mSeqLen) return +1;
			return 0;
		}
	}
}
