package sng.assem;

import sng.assem.enums.RF;

// A helper class to parse clone names
public class CloneName
{
	String mFull;
	String mClone;
	RF mRF = RF.Unk;
	String mRSuffix;
	String mFSuffix;

	public CloneName(String fullname, String rsuf, String fsuf)
	{
		mFull = fullname;
		mClone = fullname;
		mRSuffix = rsuf;
		mFSuffix = fsuf;
	}

	public boolean parse()
	{
		if (!mRSuffix.equals("") && mFull.endsWith(mRSuffix))
		{
			mRF = RF.R;
			mClone = mFull.substring(0, mFull.length() - mRSuffix.length());
			return true;
		} else if (!mFSuffix.equals("") && mFull.endsWith(mFSuffix))
		{
			mRF = RF.F;
			mClone = mFull.substring(0, mFull.length() - mRSuffix.length());
			return true;
		}

		return false;
	}
	public static String replaceRF2(String in, String fsuf, String rsuf)
	{
		String out = in;
		if (!rsuf.equals("") && in.endsWith(rsuf))
		{
			out = in.substring(0, in.length() - rsuf.length());
			out += ".r";
		} 
		else if (!fsuf.equals("") && in.endsWith(fsuf))
		{
			out = in.substring(0, in.length() - fsuf.length());
			out += ".f";
		} 
		return out;
	}
	public String replaceRF()
	{
		switch (mRF)
		{
		case F:
			return mClone + ".f";
		case R:
			return mClone + ".r";
		default:
			return mFull;
		}
	}
}
