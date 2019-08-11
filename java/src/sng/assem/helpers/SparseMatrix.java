/**
 * 
 */
package sng.assem.helpers;

import java.util.Arrays;
import java.util.Vector;
import java.util.Stack;
import java.util.TreeSet;


// Sparse matrix of integer (x,y) pairs (e.g., to mean object x overlapping object y)
// matrix contains (x,y) <==> rows[x] contains y 
// rows[x] may also contain MAX_VALUE entries (at the end, if sorted) which should be ignored
// After filling the rows, the rows must be sorted if you want to query with 'isSet'
// Not thread safe.


public class SparseMatrix 
{
	private int[][] rows = null;
	private int[] rowSizes = null;
	private int[] rowCurIdx = null;
	private int mSizeStart;
	private boolean mSorted = false;
	
	public SparseMatrix ( int nInRows, int nSizeStart)
	{
		mSizeStart = nSizeStart;
		rows = new int[nInRows][];	
		rowSizes = new int[nInRows];
		rowCurIdx = new int[nInRows];
		
		for (int i = 0; i < nInRows; i++)
		{
			rows[i] = new int[nSizeStart];
			rowSizes[i] = nSizeStart;
			rowCurIdx[i] = 0;
			for (int j = 0; j < nSizeStart; j++)
			{
				rows[i][j] = Integer.MAX_VALUE; // for compatibility with binary search
			}
		}
	}
	// Add a blank row at the position given
	// Shift the others over; if the last one wasn't empty, it is discarded!
	public void insertRow(int newR)
	{
		for (int r = rows.length - 1; r > newR; r--)
		{
			rows[r] = rows[r-1];
			rowSizes[r] = rowSizes[r-1];
			rowCurIdx[r] = rowCurIdx[r-1];			
		}
		rows[newR] = new int[mSizeStart];
		for (int j = 0; j < mSizeStart; j++)
		{
			rows[newR][j] = Integer.MAX_VALUE;	
		}
		rowSizes[newR] = mSizeStart;
		rowCurIdx[newR] = 0;
	}
	public void clear()
	{
		rows = null;
		rowSizes = null;
		rowCurIdx = null;
	}
	// Use this to add nodes for interpreting the matrix as a graph (used in TC transitive closure)
	public void setNode(int x, int y) throws Exception
	{
//		if (isSet(x,y) || isSet(y,x))
//		{
//			throw(new Exception("Setting already-set graph node "));			
//		}
		set(x,y);
		if (y != x)	set(y,x);
	}
	public void set( int x, int y) throws Exception
	{
	
		// grow the array if needed
		if (rowCurIdx[x] == rowSizes[x])
		{
			int newSize = rowSizes[x] + mSizeStart;
			int[] newRow = new int[newSize];
			for (int i = 0; i < rowCurIdx[x]; i++)
			{
				newRow[i] = rows[x][i];
			}
			for (int i = rowCurIdx[x]; i < newSize; i++)
			{
				newRow[i] = Integer.MAX_VALUE;;
			}
			rowSizes[x] += mSizeStart;
			rows[x] = null;
			rows[x] = newRow;
		}
		rows[x][rowCurIdx[x]] = y;
		rowCurIdx[x]++;
		
		if (mSorted)
		{
			sortRow(x);	
		}
	}
	public int numRows()
	{
		return rows.length;
	}
	public int rowCount(int i)
	{		
		return rowCurIdx[i];
	}
	
	// note there will be Integer.MAX_VALUE fillers in this row!!!
	public int[] getRow(int i)
	{
		return rows[i];
	}
	// return the non-filler entries of this row
	public Integer[] getRowAsIntegers(int i)
	{
		int numEntries = 0;
		for (int k = 0; k < rowCurIdx[i]; k++)
		{
			numEntries++;
		}
		Integer[] ret = new Integer[numEntries];
		int entryNum = 0;
		for (int k = 0; k < rowCurIdx[i]; k++)
		{
			ret[entryNum] = rows[i][k];
			entryNum++;
		}
		return ret;
	}
	public boolean isSet(int x, int y) throws Exception
	{
		if (!mSorted) 
		{
			throw(new Exception("SparseMatrix:Must sort before query"));
		}
		
		return (Arrays.binarySearch(rows[x],y) >= 0);
	}
	public int getIndex(int x, int y) throws Exception
	{
		if (!mSorted) 
		{
			throw(new Exception("SparseMatrix:Must sort before query"));
		}
		
		return Arrays.binarySearch(rows[x],y);
	}	
	// sort rows so that binary search can be used to interrogate
	public void sortRow(int r)
	{
		Arrays.sort(rows[r],0,rowCurIdx[r]);	
	}
	public void sortRows()
	{
		for (int i = 0; i < rows.length; i++)
		{
			sortRow(i);
		}
		mSorted = true;		
	}
	public void delete(int x, int y) throws Exception
	{
		int pos = Arrays.binarySearch(rows[x],y);
		if (pos < 0)
		{
			//throw( new Exception("SparseMatrix:Deleting nonexistent value"));
			return;
		}
		// shift the succeeding rows into the deleted space and reduce the entry count
		for (int i = pos; i < rowCurIdx[x] && i < rowSizes[x]-1; i++)
		{
			rows[x][i] = rows[x][i+1];
		}
		rows[x][rowSizes[x]-1] = Integer.MAX_VALUE; // in case rowCurIdx was at the very end
		rowCurIdx[x]--;
	}

	// Simple transitive closure using stack and marked nodes.
	// Note that it doesn't really matter if things are sorted. 
	public Vector<int[]> transitiveClosure()
	{
		Vector<int[]> retSets = new Vector<int[]>();
		
		TreeSet<Integer> usedNodes = new TreeSet<Integer>();
		Stack<Integer> curNodes = new Stack<Integer>();		
		TreeSet<Integer> curSet = new TreeSet<Integer>();
	
		for (int i = 0; i < rows.length; i++)
		{
			if (usedNodes.contains(i)) continue;
			
			assert(curNodes.empty());
			curNodes.push(i);

			while(!curNodes.empty())
			{
				int j = curNodes.pop();
				
				for (int k : getRow(j))
				{
					if (k == Integer.MAX_VALUE) continue;
					if (usedNodes.contains(k)) continue;
					usedNodes.add(k);
					curNodes.push(k);
				}
				curSet.add(j);
			}
			int[] curArray = new int[curSet.size()];
			int k = 0;
			for (Integer j : curSet)
			{
				curArray[k] = j;
				k++;
			}
			retSets.add(curArray);
			curSet = null;
			curSet = new TreeSet<Integer>();
		}
		
		return retSets;
	}
	
}
