package sng.assem;

import java.util.Arrays;
import java.util.TreeMap;

// Moving this from assem to util because used in BasicQueryData, but don't think really used
// Compute Rstat statistics 
//
// Usage: 
// Initialize with array of library sizes 
// Then call calcRstat with array of contig library counts 
// The arrays must be of the same size and with same order for libraries
//
// Versions taking TreeMap structures indexed by library database ID are also provided
//
// To get p-value thresholds, call getThreshold(pValue), e.g. getThreshold(.97) for 97% confidence.
// On the first call, it calculates rstats for 1000 random contigs. 
// On successive calls, it uses this data to estimate the thresholds. 
//

public class RStat 
{
	int[] mLibSizes = null;
	int mLibTotal = 0;
	double[] mTrialVals = null;
	TreeMap<Integer,Integer> mLID2Idx = null;
	
	public RStat(int[] libSizes)
	{
		mLibSizes = new int[libSizes.length]; 
		for(int x = 0; x < mLibSizes.length; x++)
			mLibSizes[x] = libSizes[x];
		
		mLibTotal = 0;
		for (int i = 0; i < libSizes.length; i++)
		{		
			mLibTotal += libSizes[i];
		}
	}
	public RStat(Integer[] libSizes)
	{
		mLibSizes = new int[libSizes.length]; 
		for(int x = 0; x < mLibSizes.length; x++)
			mLibSizes[x] = libSizes[x];
		//System.err.println(Utils.joinInts(mLibSizes, ","));
		//System.err.println(" ");
		
		mLibTotal = 0;
		for (int i = 0; i < libSizes.length; i++)
		{		
			mLibTotal += libSizes[i];
		}
	}	
	public static void testRstat() 
	{
		int[] libSizes = new int[4];
		libSizes[0] = 1000; 
		libSizes[1] = 5000;
		libSizes[2] = 10000;
		libSizes[3] = 50000;
		
		try
		{
			RStat rs = new RStat(libSizes);
			rs.initThreshold();
			
			for (double p = .75; p <= 1.0; p += .01)
			{
				double thresh = rs.getThreshold(p);	
				System.err.println(p + "\t" + thresh + "\n");
			}
		}
		catch(Exception e)
		{
		
		}
		System.exit(0);
	}
	public double calcRstat(int[] ctgCounts) throws Exception
	{
		if (ctgCounts.length != mLibSizes.length)
		{
			throw(new Exception("Rstat: ctgCounts does not agree with library number"));	
		}
		//System.err.println(Utils.joinInts(ctgCounts, ","));
		
		if (mLibSizes.length == 0) return 0.0;
		int ctgTotal = 0;
		for (int i = 0; i < ctgCounts.length; i++)
		{
			ctgTotal += ctgCounts[i];
		}
		double f = ((double)ctgTotal)/((double)mLibTotal); // frequency of this gene (=contig) in the entire population
		double rstat = 0.0;
		for (int i = 0; i < ctgCounts.length; i++)
		{
			double x = (double)ctgCounts[i];			
			double expected = f*(double)mLibSizes[i];
			if (x > 0.0 && expected > 0.0)
			{
				rstat += x*Math.log10(x/expected);
			}
		}
		rstat = ((double)Math.round(rstat*10))/10.0;
		//System.err.println(rstat);
		if (rstat < -.5)
		{
			System.err.println("Warning: negative R-Stat " + rstat);
		}
		if (rstat < 0) rstat = 0.0;
		
		return rstat;
	}
	
	// Convenience methods to allow the object to be used directly with
	// tables indexed on the database LID, instead of a 0-indexed array.
	public RStat(TreeMap<Integer,Integer> libSizes)
	{
		mLID2Idx = new TreeMap<Integer,Integer>();
		mLibTotal = 0;
		int numLibs = libSizes.keySet().size();
		mLibSizes = new int[numLibs];
		for (int i = 0; i < numLibs; i++)
		{
			mLibSizes[i] = 0;	
		}
		int i = 0;
		for (int lid : libSizes.keySet())
		{
			mLibSizes[i] = libSizes.get(lid);
			mLibTotal += mLibSizes[i];
			mLID2Idx.put(lid,i);
			i++;
		}
	}
	public double calcRstat(TreeMap<Integer,Integer> ctgCounts) throws Exception
	{
		int[] ctgCountsArr = new int[mLibSizes.length];
		for (int i = 0; i < ctgCountsArr.length; i++)
		{
			ctgCountsArr[i] = 0;	
		}
		for (int lid : ctgCounts.keySet())
		{
			int idx = mLID2Idx.get(lid);
			ctgCountsArr[idx] = ctgCounts.get(lid);
		}
		return calcRstat(ctgCountsArr);
	}
	
	// Make a histogram of R values from a large number of randomly 
	// generated contigs of random sizes. 
	public void initThreshold() throws Exception
	{
		int nTries = 10000;
		mTrialVals = null;
		mTrialVals = new double[nTries];
		for (int i = 0; i < nTries; i++)
		{
			mTrialVals[i] = singleTrialRstat();
		}
		Arrays.sort(mTrialVals);		
	}
	public double getThreshold(double pValue) throws Exception
	{
		if (mTrialVals == null) initThreshold();
		double thresh = 0.0;
		if (pValue < 0.0) pValue = 0.0;
		if (pValue > 1.0) pValue = 1.0;
		int rankValue = (int)(pValue*mTrialVals.length);
		if (rankValue >= mTrialVals.length) rankValue = mTrialVals.length - 1;
		return mTrialVals[rankValue];
	}
	public double pFromR(int r)
	{
		int numHigher;
		for (numHigher = mTrialVals.length - 1; numHigher >= 0; numHigher--)
		{
			if (mTrialVals[numHigher] < r) break;	
		}
		numHigher = mTrialVals.length -1 - numHigher;
		return ((double)numHigher)/((double)mTrialVals.length);
	}
	public double singleTrialRstat() throws Exception
	{
		double res = 0.0;
		int ctgSize =  1 + (int)(1000*Math.random());
		int[] ctgCounts = new int[mLibSizes.length];
		for (int i = 0; i < mLibSizes.length; i++)
		{
			ctgCounts[i] = 0;	
		}
		for (int i = 0; i < ctgSize; i++)
		{
			int lib = randomLib();
			ctgCounts[lib]++;
		}
		res = calcRstat(ctgCounts);
		return res;
	}
	// Select a library at random, weighted by their size
	public int randomLib()
	{
		int cloneNum = (int)Math.floor(Math.random()*mLibTotal);
		int runningSize = 0;
		int libNum = 0;
		for (int i = 0; i < mLibSizes.length; i++)
		{
			runningSize += mLibSizes[i];
			if (runningSize >= cloneNum)
			{
				libNum = i;
				break;
			}
		}
		return libNum;
	}

}
