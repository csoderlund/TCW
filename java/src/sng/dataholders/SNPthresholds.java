package sng.dataholders;

import java.io.Serializable;

/**
 *
 * A class for holding all of the "magic number" thresholds used during
 * biological calculations.  Currently the SNP thresholds, but I picture
 * this as expanding in the future.
 * @see sng.dataholders.ContigData
 * @see sng.dataholders.SNPData
 */
public class SNPthresholds implements Serializable
{
	// The window of perfectly matching bases required around a SNP
    public int getSNPPerfectWindow ( ) { return nSNPPerfectMatch; };
    public void setSNPPerfectWindow ( int n ) { nSNPPerfectMatch = n; };
	
	// The minimum redundancy of a base needed at a SNP position to
	// make the bases "group" qualify.
    public int getSNPMinRedundancy ( ) { return nSNPMinRedund; };
    public void setSNPMinRedundancy ( int n ) { nSNPMinRedund = n; };

	// The number of base "groups" that must meet the min redundancy
	// requirement in order for a position to be considered a SNP
    public int getSNPMinQualifiers ( ) { return nSNPQualifingGroups; };
    public void setSNPMinQualifiers ( int n ) { nSNPQualifingGroups = n; };  
    
	// The minimum redundancy of gaps and one other base needed
	// to consider a gap an InDel (and not a base-calling/transcription error)
    public int getInDelMinRedundancy ( ) { return nInDelMinRedund; };
    public void setInDelMinRedundancy ( int n ) { nInDelMinRedund = n; };
	
	private int nSNPPerfectMatch = 0;
	private int nInDelMinRedund = 2;
	private int nSNPMinRedund = 2;
	private int nSNPQualifingGroups = 2;
    
    private static final long serialVersionUID = 1;

}
