package sng.dataholders;

/**
 * CAS333 cleaned out stuff including comparable
 * Holds all of the data corresponding to a coding region.  The begin and
 * end are always relative to the sequence that the coding region
 * corresponds to.  
 */
public class CodingRegion
{
	public static final int TYPE_UNKWOWN = 0;
	public static final int TYPE_ORF = 1;
	public static final int TYPE_LARGEST_ORF = 2;
	public static final int TYPE_UNIPROT = 3;
	
	/**
	 * Creates a new CodingRegion object with one of the following types:
	 * (TYPE_UNKNOWN | TYPE_ORF | TYPE_LARGEST_ORF | TYPE_UNIPROT)
	 * @param nInType The type of the coding region 
	 */
	public CodingRegion ( int nInType ) { nType = nInType; }
		
	public int getBegin () { return nBegin; };
	public void setBegin ( int n ) { nBegin = n; };
	
	public boolean getHasBegin () { return bHasBegin; };
	public void setHasBegin ( boolean b ) { bHasBegin = b; };
	
	public int getEnd () { return nEnd; };
	public void setEnd ( int n ) { nEnd = n; };
	
	public boolean getHasEnd () { return bHasEnd; };
	public void setHasEnd ( boolean b ) { bHasEnd = b; };
	
	public int getFrame () { return nFrame; };
	public void setFrame ( int n ) { nFrame = n; }
	
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
	public void clear(){
		// nothing to clear
	}
	
	private int nType = TYPE_UNKWOWN;
	private boolean bHasBegin = false;
	private boolean bHasEnd = false;

	private int nFrame = 0;
	private int nBegin = 0;
	private int nEnd = -1;
	private int nLen = 0;
}
