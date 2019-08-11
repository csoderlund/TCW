package sng.assem;

import java.util.regex.Matcher;
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

		int maxDiff = (optimized ? 20 : Integer.MAX_VALUE);


		//int tStart = 0;
		Utils.intTimerStart(nThread);
/*		if (optimized)
		{
			// Guess the start point of the alignment so we can use the optimization.
			// The start point has to be correct within maxDiff for it to work. 
			
			int nmerSize = 8;
			int nHits = 0;
			int avgStart = 0;
			for (int i = 0; i + nmerSize <= mQuery.length(); i++)
			{
				String nmer = mQuery.substring(i,i + nmerSize);
				if (mNmerMap.containsKey(nmer))
				{
					if (mNmerMap.get(nmer).size() > 1) continue; // non-unique ones can throw off the average considerably
					for (int tLoc : mNmerMap.get(nmer))
					{
						int tStart4 = tLoc - i; // project back the start point by the query position
						nHits++;
						avgStart += tStart4;
					}
				}
			}
			int minMatchLoc = Integer.MAX_VALUE;		
			if (nHits > 10)
			{
				int tStart1 = avgStart/nHits;
				nHits = 0;
				avgStart = 0;
				// pass two: get rid of outliers, which can throw off the average by too much
				for (int i = 0; i + nmerSize <= mQuery.length(); i++)
				{
					String nmer = mQuery.substring(i,i + nmerSize);
					if (mNmerMap.containsKey(nmer))
					{
						if (mNmerMap.get(nmer).size() > 1) continue; // non-unique ones can throw off the average considerably
						for (int tLoc : mNmerMap.get(nmer))
						{
							int tStart3 = tLoc - i; // project back the start point by the query position
							if (Math.abs(tStart3 - tStart1) >= 5*maxDiff) continue;
							
							minMatchLoc = Math.min(minMatchLoc,tLoc);
							nHits++;
							avgStart += tStart3;
						}
					}
				}	
				if (nHits < 10)
				{
					mBasesAligned = 0;
					mNGaps = mQuery.length();
					return;					
				}
				tStart = avgStart/nHits;
				tStart -= 3;
				if (tStart < minMatchLoc - maxDiff) tStart = minMatchLoc - maxDiff; // don't start too much before the first good match (can happen if badly trimmed at start)
				if (tStart < 0) tStart = 0;
			}
			else
			{
				mBasesAligned = 0;
				mNGaps = mQuery.length();
				return;
			}
		}
*/		
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
		/*
		// WN 5/14/12 Since wasn't using the optimization anyway, put the loop back to
		// simpler form so can be more sure it's working. There seemed to be some glitches in DcG.  
		// The outer loop is in terms of N = r + c in order to allow for the optimization, but it
		// didn't turn out to be that beneficial. 
		// The optimization is to cut off the inner loop when the score gets too far below the max score seen so far. 
		// It is basically what megablast does.
		
		for (int N = 2 + tStart; N <= qlen + tlen; N++)
		{
			int startC = maxC; // for the optimization, start at the point which had the biggest score last time
			for (int c = startC; c <= N ; c++)
			{
				int r = N - c;
				if (r > qlen) continue;
				if  ( c > tlen || r == 0) break;
				
				int thisScore = (mQuery.charAt(r-1) == mTarget.charAt(c-1)  ? matchScore : mismatchScore);
				if (thisScore == matchScore && mQuery.charAt(r-1) == 'n')
				{
					thisScore = nMatchScore;	
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
				else if (H[r][c] < maxScore - maxDiff) 
				{
					// initialize enough of the boxes so we can't wind up using unitialized ones
					H[r][c] = 0; 
					if (r > 0 && c < tlen) H[r-1][c+1] = 0;
					break;
				}
			}
			for (int c = startC - 1; c >= 1 + tStart ; c--)
			{
				int r = N - c;
				if (r > qlen ) break;
				if ( c > tlen || r <= 0) continue;
				
				int thisScore = (mQuery.charAt(r-1) == mTarget.charAt(c-1) ? matchScore : mismatchScore);	
				if (thisScore == matchScore && mQuery.charAt(r-1) == 'N')
				{
					thisScore = nMatchScore;	
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
				H[r][c] = Math.max(0,H[r][c]);
								
				if (H[r][c] > maxScore)
				{
					maxScore = H[r][c];
					maxR = r;
					maxC = c;
				}
				else if (H[r][c] < maxScore - maxDiff) 
				{
					// initialize enough of the boxes so we can't wind up using unitialized ones
					H[r][c] = 0; 
					if (r < qlen && c > 0) H[r+1][c-1] = 0;					
					break;
				}
				
			}	
		}
		*/
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
