package util.methods;

import java.util.Arrays;
import java.util.Vector;

import util.database.Globalx;

/**************************************
 * Rstats: called from assembler for libraries
 * setQuartile:
 */
public class Stats 
{
	/*******************************
	 *  Rstat:  
	// Initialize with array of library sizes 
	// Then call calcRstat with array of contig library counts 
	// The arrays must be of the same size and with same order for libraries
	//
	// Versions taking TreeMap structures indexed by library database ID are also provided
	//
	// To get p-value thresholds, call getThreshold(pValue), e.g. getThreshold(.97) for 97% confidence.
	// On the first call, it calculates rstats for 1000 random contigs. 
	// On successive calls, it uses this data to estimate the thresholds. 
	**********************************/
	int[] mLibSizes = null;
	int mLibTotal = 0;
	
	public Stats(Integer[] libSizes)
	{
		mLibSizes = new int[libSizes.length]; 
		for(int x = 0; x < mLibSizes.length; x++) mLibSizes[x] = libSizes[x];
		
		mLibTotal = 0;
		for (int i = 0; i < libSizes.length; i++) mLibTotal += libSizes[i];
	}	
	public double calcRstat(int[] ctgCounts) throws Exception
	{
		if (ctgCounts.length != mLibSizes.length)
			throw(new Exception("Rstat: ctgCounts does not agree with library number"));	
		
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
		
		if (rstat < -.5)
		{
			System.err.println("Warning: negative R-Stat " + rstat);
		}
		if (rstat < 0) rstat = 0.0;
		
		return rstat;
	}
	/***********************************************************
	 * Static methods - they all expect the input vals to be >= 0
	 * and dNoVal (-2) to be ignored.
	 ********************************************************/
	
	/*********************************************************
	 * This uses the method that divides the list in half to get 1st and 3rd
	 * Tested with: https://www.hackmath.net/en/calculator/quartile-q1-q3 Method 2
	 */
	 static public double [] setQuartiles(Vector <Double> vals, double noVal) { // CAS313 make noVal argument
		double [] qrt = {0.0,0.0,0.0}; 
		
		int n = 0;
		for (double v : vals) 
			if (v!=noVal) n++;
		if (n==0) return qrt;
		 
		double [] dArr = new double [n];
		int r=0;
		for (double v : vals) 
			if (v != noVal) dArr[r++]=v;
		Arrays.sort(dArr);
		 
		double x = ((double)n-1.0)/2.0;
		int q2 = (int) x;
		
		int q1U, q3L;
		
		// median
		if (x == Math.floor(x) && !Double.isInfinite(x))  { // odd n
			qrt[1] = dArr[q2];
			q1U=q2-1;
			q3L=q2+1;
		}
		else {
			qrt[1] = (dArr[q2]+dArr[q2+1])/2.0;
			q1U=q2;
			q3L=q2+1;
		}
		// 1st quartile
		x = (double)q1U/2.0;
		int q1 = (int) x;
		if (q1>=0 && q1<(n-1)) {
			if (x == Math.floor(x) && !Double.isInfinite(x)) 
				qrt[0] = dArr[q1];
			else qrt[0] = (dArr[q1]+dArr[q1+1])/2.0;
		}
		else qrt[0] = dArr[0];
		
		// 3rd quartile
		x = ((double)((n-1)-q3L)/2.0) + q3L;
		int q3 = (int) x;
		if (q3>=0 && q3<(n-1)) {
			if (x == Math.floor(x) && !Double.isInfinite(x)) 
				qrt[2] = dArr[q3];
			else qrt[2] = (dArr[q3]+dArr[q3+1])/2.0;
		}	
		else qrt[2] = dArr[n-1];
	
		return qrt;
	 }
	 // CAS313 added for ScoreMulti - which is not using Vector and needs min,max
	 static public double [] setQuartiles(double [] vals) {
		double [] qrt = {0.0,0.0,0.0, 0.0, 0.0}; // CAS313 1st, median, 3rd
		 
		double [] dArr = vals.clone();
		Arrays.sort(dArr);
		int n = dArr.length;
		 
		double x = ((double)n-1.0)/2.0;
		int q2 = (int) x;
		
		int q1U, q3L;
		
		// median
		if (x == Math.floor(x) && !Double.isInfinite(x))  { // odd n
			qrt[1] = dArr[q2];
			q1U=q2-1;
			q3L=q2+1;
		}
		else {
			qrt[1] = (dArr[q2]+dArr[q2+1])/2.0;
			q1U=q2;
			q3L=q2+1;
		}
		// 1st quartile
		x = (double)q1U/2.0;
		int q1 = (int) x;
		if (q1>=0 && q1<(n-1)) {
			if (x == Math.floor(x) && !Double.isInfinite(x)) 
				qrt[0] = dArr[q1];
			else qrt[0] = (dArr[q1]+dArr[q1+1])/2.0;
		}
		else qrt[0] = dArr[0];
		
		// 3rd quartile
		x = ((double)((n-1)-q3L)/2.0) + q3L;
		int q3 = (int) x;
		if (q3>=0 && q3<(n-1)) {
			if (x == Math.floor(x) && !Double.isInfinite(x)) 
				qrt[2] = dArr[q3];
			else qrt[2] = (dArr[q3]+dArr[q3+1])/2.0;
		}	
		else qrt[2] = dArr[n-1];
		
		qrt[3] = dArr[0];
		qrt[4] = dArr[n-1];
		
		return qrt;
	 }
	 /****************************************
	  * Average and Stddev
	  */
	 static public double [] stdDev(int [] vals) {
		 double sd = 0.0, avg=0.0, n=vals.length;
		 long sum=0;
		 
		 for (int i : vals) sum += i;
		 avg = (double)sum/n;
		 
		 for (int i : vals) {
			 sd += Math.pow((avg-(double)i), 2);
		 }
		 sd = Math.sqrt(sd/(n-1)); 
		 double [] rc = {avg, sd};
		 
		 return rc;
	 }
	 static public double [] stdDev(double [] vals) {
		 double sd = 0.0, avg=0.0, n=0;
		 double [] rc = {0.0, 0.0};
		 
		 for (double i : vals) {
			 if (i!=0) {
				 avg += i;
				 n++;
			 }
		 }
		 if (avg==0.0) return rc;
		 avg = avg/n;
		 
		 for (double i : vals) {
			 if (i!=0) sd += Math.pow((avg-i), 2);
		 }
		 sd = Math.sqrt(sd/(n-1)); 
		 
		 rc[0] = avg;
		 rc[1] = sd; 
		 return rc;
	 }
	 static public double stdDev(double avg, double [] vals) {
		 if (avg==0.0) return 0.0;
		 
		 double sd = 0.0, n=vals.length;
		 
		 for (double i : vals) sd += Math.pow((avg-i), 2);
		 sd = Math.sqrt(sd/(n-1)); 
		 
		 return sd;
	 }
	 static public double stdDev(double avg, int [] vals) {
		 if (avg==0.0) return 0.0;
		 
		 double sd = 0.0, n=vals.length;
		 
		 for (int i : vals) sd += Math.pow((avg-(double)i), 2);
		 sd = Math.sqrt(sd/(n-1)); 
		 
		 return sd;
	 }
	 /***********************************************************
	  * XXX Show table stats
	  * sTCW: Basic annoDB, Basic GO, Main Table
	  * mTCW: Cluster, Sequence and Pair tables
	  * Check for undefined entries and do not count them
	  * TODO: DE and N-fold change can be negative - make absolute
	  */
	 static public String [] avgColHead() {
		 String [] head =  {"Average", "StdDev", "Medium", "Range", "Low", "High", "Sum", "Count"};
		 return head;
	 }
	 static public double [] averages(String colName, double [] vals, boolean isScore) {
		 // indices into rc for the 8 respective stats
		 int avgX=0, stdX=1, medX=2, rngX=3, lowX=4, highX=5, sumX=6, cntX=7;
		 double [] rc = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		 double sum=0.0;
		 
		// find type so know what to ignore
		 int t1=0, t2=0, type=0, n=vals.length;
		 for (double v : vals) {
			 if ((v>= -1.0 && v<=1.0) || Math.abs(v)==Globalx.dNoDE)  t1++;
			 
			 if (v>=0 || v==Globalx.dNoVal || v==Globalx.dNullVal) t2++;
		 }
		 if (colName.contains(Globalx.frameField)) type=0;
		 else if (t2==n)    type=2; 
		 else if (t1==n)    type=1; 
		 else if (isScore)  type=3;
		 else {
			Out.prt(String.format("Cannot compute stats for %s (t1=%d t2=%d n=%d %b)",
					colName, t1, t2, n, isScore));
			return rc;
		}
		
		// determine valid entries, reset n to only valid
		n=0; 
		for (int i=0; i<vals.length; i++) {
			double v = vals[i];
			if (type==1 && Math.abs(v)==Globalx.dNoDE) continue;
			if (type==2 && vals[i]<=Globalx.dNullVal) continue;
			if (type==3 && vals[i]==Globalx.dNoScore) continue;
				
			n++;
		}
		if (n==0) return rc; 
			
		// sum
		 double [] dArr = new double [n];
		 for (int i=0, r=0; i<vals.length; i++) {
			double v = vals[i];
			if (type==1 && Math.abs(v)==Globalx.dNoDE) continue;
			if (type==2 && v<=Globalx.dNullVal) continue;
			if (type==3 && v==Globalx.dNoScore) continue;
			
			sum +=    v;
			dArr[r++]=v;
		 }
	
		 rc[sumX] = sum;
		 rc[avgX] = (sum==0.0) ? 0.0 : (sum/ (double) n); 
		 rc[stdX] = stdDev(rc[avgX], dArr);
		 
		 Arrays.sort(dArr);
		 int m=n/2;
		 if (m<dArr.length) {
			 if (n%2 == 0) rc[medX] = (dArr[m-1]+dArr[m])/2;
			 else if (m < dArr.length) rc[medX] = dArr[m]; 
		 }
		 else rc[medX] = 0;
		 
		 rc[lowX] =  dArr[0];
		 rc[highX] = dArr[n-1];
		 rc[rngX] =  rc[highX]-rc[lowX];
		 rc[cntX] =  n;
		 return rc;
	 }
}
