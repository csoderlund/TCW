package sng.assem;

import java.util.TreeMap;

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
	private int[] mLibSizes = null;
	private int mLibTotal = 0;
	private TreeMap<Integer,Integer> mLID2Idx = null;
	
	// AssemSkip
	public RStat(int [] libSizes)
	{
		mLibSizes = new int[libSizes.length]; 
		for(int x = 0; x < mLibSizes.length; x++) mLibSizes[x] = libSizes[x];
		
		mLibTotal = 0;
		for (int i = 0; i < libSizes.length; i++) mLibTotal += libSizes[i];
	}	
	
	// AssemSkip
	public double calcRstat(int[] ctgCounts) {
		try {
			if (ctgCounts.length != mLibSizes.length)
				Log.die("TCW error in calcRstat: " + ctgCounts.length + " " + mLibSizes.length);
				
			if (mLibSizes.length == 0) return 0.0;
			
			int ctgTotal = 0;
			for (int i = 0; i < ctgCounts.length; i++)
				ctgTotal += ctgCounts[i];
		
			double f = ((double)ctgTotal)/((double)mLibTotal); // frequency of this gene (=contig) in the entire population
			
			double rstat = 0.0;
			for (int i = 0; i < ctgCounts.length; i++) {
				double x = (double)ctgCounts[i];			
				double expected = f*(double)mLibSizes[i];
				if (x > 0.0 && expected > 0.0)
					rstat += x*Math.log10(x/expected);
			}
			rstat = ((double)Math.round(rstat*10))/10.0;
		
			if (rstat < -.5)
				System.err.println("Warning: negative R-Stat " + rstat);
			if (rstat < 0) rstat = 0.0;
			
			return rstat;
		} 
		catch (Exception e) {Log.error(e, "Calc Rstat"); return -1.0;}
	}
	
	// AssemMain
	// Convenience methods to allow the object to be used directly with
	// tables indexed on the database LID, instead of a 0-indexed array.
	public RStat(TreeMap<Integer,Integer> libSizes)
	{
		mLID2Idx = new TreeMap<Integer,Integer>();
		mLibTotal = 0;
		int numLibs = libSizes.keySet().size();
		mLibSizes = new int[numLibs];
		for (int i = 0; i < numLibs; i++) mLibSizes[i] = 0;	
		
		int i = 0;
		for (int lid : libSizes.keySet()) {
			mLibSizes[i] = libSizes.get(lid);
			mLibTotal += mLibSizes[i];
			mLID2Idx.put(lid,i);
			i++;
		}
	}
	// SubContig
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
}
