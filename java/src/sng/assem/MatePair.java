package sng.assem;

import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import sng.assem.enums.PairOlap;
import sng.assem.enums.RF;


// A mate pair of clones.

public class MatePair
{
	public Clone m5Prime = null;
	public Clone m3Prime = null;

	MatePair mParent = null;
	TreeSet<Integer> mKids; 
	int mBuryLevel = -1;
	int mIdx;
	boolean mSelfOlap = false;
	TreeMap<Integer,HashSet<PairOlap>> mOlapCandidates;
	
	public MatePair()
	{
		mKids = new TreeSet<Integer>();
		mOlapCandidates = new TreeMap<Integer,HashSet<PairOlap>>();
	}
	public void set(Clone c) throws Exception
	{
		if (c.mRF == RF.R)
		{
			if (m3Prime != null) 
			{
				throw(new Exception("Tried to re-set MatePair 3' with clone " + c.mFullName + " (already set to " + m3Prime.mFullName + ")"));
			}
			m3Prime = c;
		}
		else if (c.mRF == RF.F)	
		{
			if (m5Prime != null) 
			{
				throw(new Exception("Tried to re-set MatePair 5' with clone " + c.mFullName + " (already set to " + m5Prime.mFullName + ")"));
			}
			m5Prime = c;
		}
		else throw(new Exception("Tried to set MatePair with unmated clone " + c.mFullName));
	}
	public void set5Prime(Clone c)
	{
		m5Prime = c;
	}	
	public int length() throws Exception
	{
		if (m5Prime == null || m3Prime == null)
		{
			throw( new Exception("Tried to get length for incomplete mate pair!! "));
		}
		return m5Prime.mSeq.length() + m3Prime.mSeq.length();
	}
	public String idStr()
	{
		return m5Prime.mFullName + "," + m3Prime.mFullName;
	}
}
