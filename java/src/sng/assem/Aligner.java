package sng.assem;

import java.util.regex.Pattern;
import java.lang.StringBuilder;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;


// Smith-Waterman sequence aligner for doing the final alignment of EST to consensus

public class Aligner 
{
	String mQuery;
	String mTarget;
	String mQueryOut;
	String mTargetOut;
	String mQName;
	int mQStart;
	int mTStart;
	int mQEnd;
	int mTEnd;
	String mGaps;
	String mTGaps;
	int mNGaps;
	int mNTGaps;
	int mBasesAligned;
	int mMismatches;
	
	TreeMap<String,TreeSet<Integer>> mNmerMap;
	
	Pattern mPatInitNum;
	Pattern mPatFinalNum;
	
	Aligner(String target)
	{
		mTarget = target;
		
		mPatInitNum = Pattern.compile("^\\s*(\\d+)\\s+.*");
		mPatFinalNum = Pattern.compile(".*\\s+(\\d+)\\s*$");

		initNmerMap();
	}
	void initNmerMap()
	{
		int nmerSize = 8;
		
		mNmerMap = new TreeMap<String,TreeSet<Integer>>();
		
		for (int i = 0; i + nmerSize <= mTarget.length(); i++)
		{
			String nmer = mTarget.substring(i,i + nmerSize);
			if (!mNmerMap.containsKey(nmer))
			{
				mNmerMap.put(nmer,new TreeSet<Integer>());
			}
			mNmerMap.get(nmer).add(i);
		}
	}
	void clear()
	{
		mQuery = "";
		mTarget = "";
		clearOutputs();
	}
	void clearQuery()
	{
		mQuery = "";
		clearOutputs();
	}
	void clearOutputs()
	{
		mQueryOut = "";
		mTargetOut = "";
		mGaps = "";
		mTGaps = "";
		mBasesAligned = 0;
		mNGaps = 0;
		mNTGaps = 0;
		mQStart = 0;
		mTStart = 0;
	}
	int basesAligned ()
	{
		int qLen = mQuery.length();
		int goodAlign = mQueryOut.replaceAll("[^agctAGCT]","").length();
		return ( (100*goodAlign)/qLen);
	}	
	int pctQueryAligned ()
	{
		int qLen = mQuery.length();
		return ( (100*mBasesAligned)/qLen);
	}
	void printAlignment2(){}
	void printAlignment()
	{
		StringBuilder str1 = new  StringBuilder("Query: " + mQStart + " ");
		StringBuilder str2 = new StringBuilder("Target: " + mTStart + " ");
		while (str1.length() > str2.length())
		{
			str2.append(" ");	
		}
		while (str2.length() > str1.length())
		{
			str1.append(" ");	
		}	
		StringBuilder strBlank = new StringBuilder("");
		while (str1.length() > strBlank.length())
		{
			strBlank.append(" ");	
		}	
		for (int i = 0; i < mTargetOut.length(); i++)
		{
			str2.append(mTargetOut.charAt(i));
		}
		Log.msg(str2.toString());
		str2.setLength(0);
		
		for (int i = 0; i < mTargetOut.length(); i++)
		{
			if (mQueryOut.charAt(i) == mTargetOut.charAt(i))
			{
				strBlank.append("|");	
			}
			else
			{
				strBlank.append(" ");	
			}
		}		
		Log.msg(strBlank.toString());
		strBlank.setLength(0);
		for (int i = 0; i < mQueryOut.length(); i++)
		{
			str1.append(mQueryOut.charAt(i));
		}
		Log.msg(str1.toString());
		str1.setLength(0);
	}
	
	// This smith-waterman routine is more complicated looking than it needs to be because
	// it has an optimization which has been disabled. 
	
	void SWAlign(String query, String qname, boolean optimized, int nThread) throws Exception
	{
		mQuery = query;
		mQName = qname;

		int matchScore = 1; //3;
		int mismatchScore = 0; //-1;
		int gapScore = -1;
		int nMatchScore = 1; // if they both have an n, it may be meaningful


		Utils.intTimerStart(nThread);

		int qlen = mQuery.length();
		int tlen = mTarget.length();
		
		int[][] H = new int[qlen + 1][tlen + 1]; // scores
		char[][] B = new char[qlen + 1][tlen + 1]; // backtrace
		
		for (int c = 0; c < tlen + 1; c++)
		{
			H[0][c] = 0;	
		}
		for (int r = 0; r < qlen + 1; r++)
		{
			H[r][0]=0; //tStart] = 0;	
		}		
		
		// Build the scoring matrix and track the backtrace info.
		// Note that the "0" row/column of the matrix don't correspond to a sequence position but 
		// are for initialization. 
		// The sequences start at "1" in the matrix coordinates. 
		int maxScore = 0;
		int maxR = 1;
		int maxC = 1; // + tStart;
		for (int r=1; r <= qlen; r++)
		{
			for (int c = 1; c <= tlen ; c++)
			{
				int thisScore = mismatchScore;
				char thisChar = mQuery.charAt(r-1);
				if (mQuery.charAt(r-1) == mTarget.charAt(c-1))
				{
					if (thisChar == 'n' || thisChar == 'N')
					{
						thisScore = nMatchScore;
					}
					else
					{
						thisScore = matchScore;
					}
				}
								
				int scoreDiag = H[r-1][c-1] + thisScore;
				int scoreTGap = H[r-1][c] + gapScore;
				int scoreQGap = H[r][c-1] + gapScore;
				
				if (scoreDiag >= scoreTGap && scoreDiag >= scoreQGap)
				{
					H[r][c] = scoreDiag;
					B[r][c] = 'D';
				}
				else if (scoreTGap >= scoreDiag && scoreTGap >= scoreQGap)
				{
					H[r][c] = scoreTGap;
					B[r][c] = 'T';
				}
				else
				{
					H[r][c] = scoreQGap;
					B[r][c] = 'Q';
				}
					
				if (H[r][c] > maxScore)
				{
					maxScore = H[r][c];
					maxR = r;
					maxC = c;
				}
			}
		}
		
		// Backtrace to get the alignment
		
		int r = maxR;
		int c = maxC;
		clearOutputs();
		Vector<Integer> gaps = new Vector<Integer>();
		Vector<Integer> tgaps = new Vector<Integer>();
		StringBuilder tout = new StringBuilder();
		StringBuilder qout = new StringBuilder();
		
		mQEnd = r;
		mTEnd = c;
		mBasesAligned = 0;
		mMismatches = 0;
		
		while (r > 0 && c > 0 && H[r][c] > 0)
		{
			mQStart = r;
			mTStart = c;
						
			if (B[r][c] == 'D')
			{
				qout.append(mQuery.charAt(r-1));
				tout.append(mTarget.charAt(c-1));
				if (mQuery.charAt(r-1) == mTarget.charAt(c-1))
				{
					mBasesAligned++;	
				}
				else
				{
					mMismatches++;	
				}
				r--;
				c--;				
			}
			else if (B[r][c] == 'T')
			{
				qout.append(mQuery.charAt(r-1));
				tout.append("-");
				tgaps.insertElementAt(c,0);
				mNTGaps++;
				//mMismatches++; WN why was this here?
				r--;				
			}
			else if (B[r][c] == 'Q')
			{
				tout.append(mTarget.charAt(c-1));
				qout.append("-");
				gaps.insertElementAt(c,0);		
				mNGaps++;
				c--;								
			}

		}
		H = null;
		B = null;
		
		mGaps = Utils.strIntVectorJoin(gaps," ");
		mTGaps = Utils.strIntVectorJoin(tgaps," ");
		mQueryOut = qout.reverse().toString();
		mTargetOut = tout.reverse().toString();
		qout.setLength(0);
		tout.setLength(0);
		gaps.clear();
		tgaps.clear();
		if (optimized)
		{
			Utils.intTimerEnd(nThread,"OptAlign");	
		}
		else
		{
			Utils.intTimerEnd(nThread,"UnoptAlign");	
		}
	}
	double errRate() throws Exception
	{
		if (mBasesAligned == 0)
		{
			throw(new Exception("errRate called on nonaligned sequences"));	
		}
		double denom = (double)mMismatches + (double)mBasesAligned;
		double err = (((double)mMismatches)/denom);
		return err;
	}

}
