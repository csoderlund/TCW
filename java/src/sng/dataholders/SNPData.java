package sng.dataholders;

import java.io.Serializable;
import java.util.Vector;
import java.util.Comparator;
import java.util.Collections;

import util.database.Globalx;



/**
 * Holds the data for a SNP candidate (e.g. the frequency of each base at the position).  
 * Also encapsulates some of the hueristics (e.g. if the candidate's gap should be InDels.
 * SNP redundancy score.)
 * @see sng.dataholders.ContigData
 * @see sng.dataholders.SNPthresholds
 * @see sng.dataholders.SNPData
 * @see sng.viewer.panels.seqDetail.SNPTable
 */

public class SNPData implements Serializable
{	
	public static final int TYPE_UNKNOWN = -1;
	public static final int TYPE_DNA = 0;
	public static final int TYPE_AMINO_ACIDS = 1;	
	
    public SNPData ( int nInType )
	{
    	nType = nInType;
		charCounts = new int [(int)'Z'];
		for ( int i = 0; i < charCounts.length; ++i )
			charCounts[i] = 0;
	}
	
    public int getMisMatchCount ( char chConsensusBase )
    {
        return getNonGapCount ( ) - charCounts [ chConsensusBase ];
    }
    
    public int getGapCount ( ) { return charCounts [Globalx.gapCh]; };
    
    public int getNonGapCount ( ) { return nTotal - charCounts [Globalx.gapCh]; }
    
    public int getTotalCount ( ) { return nTotal; };
    
    public void setMaybeSNP ( boolean b ) { bMaybeSNP = b; };
    public boolean maybeSNP ( ) { return bMaybeSNP; };
	
    public void setPosition ( int n ) { nSNPPosition = n; };
    public int getPosition () { return nSNPPosition; };
	
    public int getCoSegregationScore ( ) { return nCoSegScore; };
    public void setCoSegregationScore ( int n ) { nCoSegScore = n; };
	
    public int getCoSegregationGroup ( ) { return nCoGroup; };
    public void setCoSegregationGroup ( int n ) { nCoGroup = n; };
	
    public int getMissingCount ( ) { return nMissingCount; }
	
    public int getSNPRedundancy ( SNPthresholds thresholds )
	{
		int nMin = Integer.MAX_VALUE;
		int nQualifiers = 0;
		
		if ( charCounts ['A'] >= thresholds.getSNPMinRedundancy() )
		{
			nMin = Math.min ( nMin, charCounts ['A'] );
			++nQualifiers;
		}
		if ( charCounts ['T'] >= thresholds.getSNPMinRedundancy() )
		{
			nMin = Math.min ( nMin, charCounts ['T'] );
			++nQualifiers;
		}
		if ( charCounts ['G'] >= thresholds.getSNPMinRedundancy() )
		{
			nMin = Math.min ( nMin, charCounts ['G'] );
			++nQualifiers;
		}
		if ( charCounts ['C'] >= thresholds.getSNPMinRedundancy() )
		{
			nMin = Math.min ( nMin, charCounts ['C'] );
			++nQualifiers;
		}
		if ( maybeInDel ( thresholds ) && charCounts [Globalx.gapCh] >= thresholds.getSNPMinRedundancy() )
		{
			nMin = Math.min ( nMin, charCounts [Globalx.gapCh] );
			++nQualifiers;
		}
		
		if ( nQualifiers >= thresholds.getSNPMinQualifiers() )
			return nMin;
		else
			return 0;
	}
	
    public char getMajorityBase ( )
	{
		int nMaxIdx = findIndexOfMax( charCounts );
		
		// See if the max is unique
		int nCountWithMax = 0;
		for ( int i = 0; i < charCounts.length; ++i ) 
		{
			if ( charCounts[i] == charCounts[nMaxIdx] )
				++nCountWithMax;
			if ( nCountWithMax > 1 )
				break;
		}
        
        if ( nCountWithMax > 1 )
        {
        	if ( nType == TYPE_DNA )
        		return 'N';
        	else
        		return 'X';
        }
        else
            return (char)nMaxIdx;
	}
    private int findIndexOfMax ( int [] intArray )
	{
		if ( intArray.length <= 0 )
			throw new RuntimeException ("The array is empty...  Q: If an empty array falls into a method and nobody cares does it have a max value?  A: The glass is 20.7% empty.");
		
		int nMax = 0;
		for ( int i = 1; i < intArray.length; ++i )
			if ( intArray[i] > nMax )
				nMax = i;
		
		return nMax;
	}
	
    public void addToCounts ( char chBase )
	{
		++charCounts [ (int)Character.toUpperCase( chBase ) ];		
		++nTotal;
	}
	
    public void incrementMissingCount ( )
	{
		++nMissingCount;
	}
	
    public boolean isPerfectMatch ( ) 
	{
		return nTotal == charCounts ['A'] || 
					nTotal == charCounts ['T'] || 
						nTotal == charCounts ['G'] || 
							nTotal == charCounts ['C'];
	}
	
    public boolean maybeInDel ( SNPthresholds thresholds )
	{
		return charCounts [Globalx.gapCh] >= thresholds.getInDelMinRedundancy() &&
					( charCounts ['A'] >= thresholds.getInDelMinRedundancy() || 
							charCounts ['T'] >= thresholds.getInDelMinRedundancy() ||
								charCounts ['G'] >= thresholds.getInDelMinRedundancy() ||
									charCounts ['C'] >= thresholds.getInDelMinRedundancy() );
	}
	
	
	public static void sortByPosition ( Vector<SNPData> listOfSNPS )
	{
		Collections.sort ( listOfSNPS, 
				new Comparator<SNPData> ()
				{		
				 	public int compare(SNPData snp1, SNPData snp2)
				 	{
				 		return snp1.getPosition() - snp2.getPosition ( );	 		
				 	}

				 	public boolean equals(Object obj) 
				 	{
				 		return false;
				 	}
				}
			);	}
	
    public static void sortByCoSegregationGroup ( Vector<SNPData> listOfSNPS )
	{
		Collections.sort ( listOfSNPS, 
				new Comparator<SNPData> ()
				{		
				 	public int compare(SNPData snp1, SNPData snp2)
				 	{
				 		return snp1.getCoSegregationGroup() - snp2.getCoSegregationGroup ( );	 		
				 	}

				 	public boolean equals(Object obj) 
				 	{
				 		return false;
				 	}
				}
			);
	}
    
    public void setValuesFromDBBaseVarsField(String theField) {
    	String [] counts = theField.split(",");
    	for(int x=0; x<counts.length; x++) {
    		String [] tempSet = counts[x].split(":");
    		char charVal = tempSet[0].trim().charAt(0);
    		int count = Integer.parseInt(tempSet[1].trim());
    		if(charVal == Globalx.gapCh)
    			charCounts[Globalx.gapCh] = count;
    		else
    			charCounts[charVal] = count;
    		nTotal += count;
    	}
    }

    private static final long serialVersionUID = 1;
    
	// Counts for each base and gaps
	private int [] charCounts = null;
	
	private int nMissingCount = 0; // Number of ESTs that end before/start after the SNP position
	private int nTotal = 0; // The total number of ESTs that have something (maybe a gap) aligned at the position
	private int nType = TYPE_UNKNOWN;
	private int nCoSegScore = -1;
	private int nCoGroup = -1;
	private int nSNPPosition = -1;
	private boolean bMaybeSNP = false; // Set to true by the client class if the SNP has passed all filters}
}