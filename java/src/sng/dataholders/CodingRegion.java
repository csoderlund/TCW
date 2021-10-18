package sng.dataholders;

/**
 * CAS333 cleaned out stuff including comparable
 * Holds all of the data corresponding to a coding region.  The begin and
 * end are always relative to the sequence that the coding region
 * corresponds to.  
 */
public class CodingRegion
{
	public static final int TYPE_LARGEST_ORF = 1; // CAS334 only type there is
	
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
	public void clear(){}
	
	private int nType = 0;
	private boolean bHasBegin = false;
	private boolean bHasEnd = false;

	private int nFrame = 0;
	private int nBegin = 0;
	private int nEnd = -1;
}
