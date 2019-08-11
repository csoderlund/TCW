package util.ui;

/**********************************************
 * Used by FieldMapper for sTCW
 *
 */
public class DisplayInt extends Object implements Comparable<DisplayInt>
{
	public final static int ADD_COMMAS_ONLY = 0;
	public final static int ROUND_TO_KBPS = 1;
	public final static int ROUND_TO_MBPS = 2;

	public DisplayInt ( long nVal )
	{
		val = (int)nVal;
	}

	public DisplayInt ( String str )
	{
		if (str == null || str.length() == 0)  {
			val = 0;
			return;
		}
		try {
			val = Integer.parseInt ( str );
		}
		catch (Exception e) {
			float f = Float.parseFloat (str);
			val = (int)(f + 0.5f);
		}
	}	
	
	public boolean equals ( DisplayInt obj )
	{
		return compareTo ( obj ) == 0;
	}
	
	public int compareTo( DisplayInt obj )
	{
		int nRVal;
		
		nRVal = obj.val;			
	
		if ( val < nRVal )
			return -1;
		if ( val > nRVal )
			return 1;
		return 0;
	}    
    
    public long longValue() 
    {
    		return (long)val;
    }
    
    public int intValue() 
    {
    		return val;
    }
	
    public static String format ( int n, int nFormat )
	{
		// Determine the power of 1000
		int nTemp = n;
		int nPower = -1;
		do
		{
			++nPower;
			nTemp /= 1000;
		} while ( nTemp > 0 );
		
		// Decide what place to round to
		int nDiv = 0;
		switch ( nFormat ) {
			case ADD_COMMAS_ONLY:
				break; /* no-op */
			case ROUND_TO_KBPS:
			case ROUND_TO_MBPS:
				if ( nPower >= 1 ) {
					if ( nPower > 1 && nFormat == ROUND_TO_MBPS)
						nDiv = 1000000;
					else
						nDiv = 1000;
				}
				break;
			default:
				System.err.println ( "Invalid format value (" + nFormat + ") in DisplayInt.Format.");
				break;
		}
		
		// Do the rounding
		long nRemain = 0;
		boolean bAddZeroRemain = false;
		if ( nDiv > 0 ) {
			// Determine the one decimal value to display
			nRemain = n % nDiv;
			int nDiv2 = nDiv / 10;
			
			if ( ( nRemain % nDiv2 ) >= (nDiv2/2) ) {
				nRemain /= nDiv2;
				nRemain += 1;
			}
			else
				nRemain /= nDiv2;
			
			// Correct the non-decimal portion
			n /= nDiv;
			
			// Look for the case of x.9 rounding up
			if ( nRemain == 10 ) {
				nRemain = 0;
				++n;
				bAddZeroRemain = true;
			}
		}
		
		// Generate the output value essentially right to left
		String strOut = new String ();
		switch ( nDiv )
		{
			case 1000:
				strOut += " kbp";
				break;
			case 1000000:	
				strOut += " Mbp";
				break;
			case 0:
				break;
			default:
				System.err.println( "Invalid divisor (" + nDiv + ") in DisplayInt.Format." );
		}
		
		// Add the remainder
		if ( nRemain > 0 || bAddZeroRemain )
			strOut = "." + String.valueOf( nRemain ) + strOut;

		// Boundary condition, have 0 before the decimal
		if ( n == 0 ) {
			strOut = "0" + strOut;
			return strOut;
		}
		
		// Add the comma seperated values
		int nCur;
		while ( n > 0 ) {
			if ( n < 1000 ) {
				strOut = String.valueOf( n ) + strOut;
				n = 0;
			}
			else {
				// Get the next three digits
				nCur = n % 1000;
				strOut = String.valueOf( nCur ) + strOut;
				
				// Add commas and padding zeros
				if ( nCur < 10 )
					strOut = ",00" + strOut;
				else {
					if ( nCur < 100 )
						strOut = ",0" + strOut;
					else
						strOut = "," + strOut;
				}
				
				// Remove the digits that we added to the output
				n /= 1000;
			}
		}
		
		return strOut;
	}
	
    public String toString() 
    {
    		return Integer.toString(val); 
    }

	private int val;
}
