package util.align;

/************************************************************
 * CAS326 moved this computation from cmp.align.MultiAlignData to here
 * Creates the consensus sequence from a set of aligned sequences
 * TODO Make consensus for pair aligns - easier to view
 */
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import util.database.Globalx;

public class Consensus {
	/************************************************
	 * Compute consensus for alignment
	 */
	private final String consName = "Consensus";
	
	public void alignConsensus(boolean isAA, Vector <String> nameVec, Vector <String> algnSeqVec) {
		String conSeq = "";
		
		int seqLen = algnSeqVec.get(0).length(); //All sequences are aligned.. they are all the same length
		
		// Get counts of each symbol
		Hashtable<Character, Integer> counts = new Hashtable<Character, Integer>();
		for(int x=0; x<seqLen; x++) {
			counts.clear();
			for (String seq : algnSeqVec) {
				char c= seq.charAt(x);
				if (counts.containsKey(c)) counts.put(c, counts.get(c) + 1);
				else counts.put(c, 1);
			}
			
			//Convert counts to a ordered list
			Vector <AlignSymbol> cList = new Vector<AlignSymbol> ();
			for (Map.Entry<Character, Integer> val : counts.entrySet())
				cList.add(new AlignSymbol(val.getKey(), val.getValue()));
		
			//Map no longer needed at this point
			counts.clear();
			Collections.sort(cList);
			
			//Test if the there is one symbol most frequent
			if( alignTotalCount(cList) <= 1)
				conSeq += Globalx.gapCh;
			else if(cList.size() == 1 || (cList.get(0).count>cList.get(1).count && cList.get(0).symbol!=Globalx.gapCh))
				conSeq += cList.get(0).symbol;
			else if (cList.get(0).symbol == Globalx.gapCh && (cList.size()==2 || cList.get(1).count>cList.get(2).count))
				conSeq += cList.get(1).symbol;
			else
				conSeq += alignBestChar(cList, isAA);
		}
		nameVec.insertElementAt(consName, 0);
		algnSeqVec.insertElementAt(conSeq, 0);
	}
	private Character alignBestChar(Vector<AlignSymbol> symbols, boolean isAA) {
		//Special case: need at least 2 non-gap values to have consensus
		if(alignTotalCount(symbols) == 1)
			return Globalx.gapCh;
		
		int [] relateCounts = new int[symbols.size()];
		for(int x=0; x<relateCounts.length; x++)
			relateCounts[x] = 0;
		
		//Going with the assumption that relationships are not mutually inclusive
		for(int x=0; x<relateCounts.length; x++) {
			for(int y=x+1; y<relateCounts.length; y++) {
				if (isAA) {
					if(AAStatistics.isHighSub(symbols.get(x).symbol, symbols.get(y).symbol)) {
						relateCounts[x]++;
						relateCounts[y]++;
					}
				}
				else {
					if (symbols.get(x).symbol == symbols.get(y).symbol) {
						relateCounts[x]++;
						relateCounts[y]++;
					}
				}
			}
		}
		//Find highest value
		int maxPos = 0;
		
		for(int x=1; x<relateCounts.length; x++) {
			if( (relateCounts[x]) > (relateCounts[maxPos]) )
				maxPos = x;
		}
		return symbols.get(maxPos).symbol;
	}
	private static int alignTotalCount(Vector <AlignSymbol> cList) {
		int retVal = 0;
		
		Iterator<AlignSymbol> iter = cList.iterator();
		while(iter.hasNext()) {
			AlignSymbol temp = iter.next();
			if(temp.symbol != Globalx.gapCh)
				retVal += temp.count;
		}
		return retVal;
	}
	//Data structure for sorting/retrieving counts
	private class AlignSymbol implements Comparable<AlignSymbol> {
		public AlignSymbol(Character symbol, Integer count) {
			this.symbol = symbol;
			this.count = count;
		}
		public int compareTo(AlignSymbol arg) {
			return -1 * count.compareTo(arg.count);
		}
		public Character symbol;
		public Integer count;
	}
}
