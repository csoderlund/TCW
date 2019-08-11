package sng.dataholders;

import java.io.Serializable;
import java.lang.Comparable;

/**
 * Holds all of the data corresponding to a coding region.  The begin and
 * end are always relative to the sequence that the coding region
 * corresponds to.  
 */
public class CodingRegion implements Comparable<CodingRegion>, Serializable
{
	public static final int TYPE_UNKWOWN = 0;
	public static final int TYPE_ORF = 1;
	public static final int TYPE_LARGEST_ORF = 2;
	public static final int TYPE_UNIPROT = 3;
	
	private boolean debug = false;
	/**
	 * Creates a new CodingRegion object with one of the following types:
	 * (TYPE_UNKWOWN | TYPE_ORF | TYPE_LARGEST_ORF | TYPE_UNIPROT)
	 * @param nInType The type of the coding region 
	 */
	public CodingRegion ( int nInType ) { nType = nInType; 
		if (debug) System.err.println("Create Coding region " + nType);}
	
	public void setType ( int nInType ) { nType = nInType; }
	
	public String getDescription ( )
	{
		int len = (nEnd-nBegin)+1;
		String s = "Frame " + nFrame + " Start " + nBegin + " End " + nEnd + " Len " + len ;
		switch ( nType )
		{
		case TYPE_ORF: return "ORF " + s;
		case TYPE_LARGEST_ORF: return "Largest ORF " + s;
		case TYPE_UNIPROT: return "Protein ORF " + s;
		default: return "?";
		}
	}
		
	public int getBegin () { return nBegin; };
	public void setBegin ( int n ) { nBegin = n; };
	
	public boolean getHasBegin () { return bHasBegin; };
	public void setHasBegin ( boolean b ) { bHasBegin = b; };
	
	public int getEnd () { return nEnd; };
	public void setEnd ( int n ) { nEnd = n; };
	
	public boolean getHasEnd () { return bHasEnd; };
	public void setHasEnd ( boolean b ) { bHasEnd = b; };
	
	public int getLength () { return nEnd - nBegin + 1; }
	
	public int getFrame () { return nFrame; };
	public void setFrame ( int n ) { nFrame = n; }
	
	public boolean getIsComplement ( ) { return nFrame < 0; }
	
	public boolean getHasStartCodon ()
	{
		return ( getHasBegin () && !getIsComplement ( ) ) ||
				( getHasEnd () && getIsComplement ( ) );
	}
	
	public int getStartCodonIndex ( ) 
	{
		if ( !getIsComplement ( ) )
		{
			if ( getHasBegin () )
				return nBegin;
			else
				return nBegin + Math.abs( nFrame ) - 1;
		}
		else
		{
			if ( getHasEnd () )
				return nEnd;  
			else
				return nEnd + Math.abs( nFrame ) - 1;
		}
	}
	
	public int getStopIndex ( )
	{
		if ( !getIsComplement ( ) )
			return nEnd;
		else
			return nBegin;
	}
	
	public int compareTo(CodingRegion rORF)
	{
		// First compare the width.  The wider the better.
		if ( getLength () != rORF.getLength() )
			return rORF.getLength() - getLength ();
		
		// Otherwise if only one has both start and stop consider it better
		if ( getHasBegin () && getHasEnd () && (!rORF.getHasBegin() || !rORF.getHasEnd() )  )
			return -1;
		if ( rORF.getHasBegin () && rORF.getHasEnd () && (!getHasBegin() || !getHasEnd() )  )
			return 1;
		
		return 0;
	}
	
	public Object clone () { return cloneRegion (); }
	
	/**
	 * @return copy of the coding region
	 */
	public CodingRegion cloneRegion ( )
	{
		CodingRegion copy = new CodingRegion ( nType );
		copy.bHasBegin = bHasBegin;
		copy.bHasEnd = bHasEnd;
		copy.nFrame = nFrame;
		copy.nEnd = nEnd;
		copy.nBegin = nBegin;
		
		return copy;
	}

	public void sanityCheck ()
	{
		if ( nFrame < -3 || nFrame == 0 || nFrame > 3 )
			throw new RuntimeException ( "Invalid frame of " + nFrame );
		if ( nBegin > nEnd )
			throw new RuntimeException ( "Coding Begin > End" );
		if ( nEnd - nBegin < 6 )
			throw new RuntimeException ( "Coding region less than two codons: " + nEnd + " " + nBegin );
	}
	

	public CodingRegion getComplement ( int nSeqMinIndex, int nSeqMaxIndex )
	{
		// Sanity
		String s = "MinIndex " + nSeqMinIndex + " MaxIndex " +  nSeqMaxIndex + " begin " +  nBegin  + " end " + nEnd;
		if ( nSeqMinIndex > nSeqMaxIndex )
			throw new RuntimeException ( "Min index > Max index. " + s);
		if ( nBegin < nSeqMinIndex )
			throw new RuntimeException ( "Begin < than min index. " + s);
		if ( nEnd > nSeqMaxIndex )
			throw new RuntimeException ( "End > than max index. " + s);
		
		CodingRegion comp = cloneRegion ();
		comp.bHasBegin = bHasEnd;
		comp.bHasEnd = bHasBegin;
		comp.nFrame = -nFrame;
		comp.nEnd = nSeqMaxIndex - (nBegin - nSeqMinIndex);
		comp.nBegin = nSeqMaxIndex - (nEnd - nSeqMinIndex);		
		comp.sanityCheck();
		return comp;		
	}
	
	public void clear()
	{
		// nothing to clear
	}
	
	private int nType = TYPE_UNKWOWN;
	private boolean bHasBegin = false;
	private boolean bHasEnd = false;

	private int nFrame = 0;
	private int nBegin = 0;
	private int nEnd = -1;
	private int nLen = 0;
	
    private static final long serialVersionUID = 1;
}
