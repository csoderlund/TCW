
package util.ui;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Used by Sequence Table Columns, and the Basic queries
 * User set in UIfieldRounding. 
 */
public class DisplayFloat implements Comparable<DisplayFloat>
{
	// set in UIfieldRounding and preferences
	static public double Largest=999999.99, Smallest=0.01;
	static public int Num_sig = 2, Num_dec = 2;
	static public int Use_mode = 1;
	
	// used by UIfieldRounding for user to set rounding type
	static public final int USE_FORMAT = 1;
	static public final int USE_BIGDECIMAL = 2;
	
	// this is for percent, so dMultiplier is always 100
	public DisplayFloat ( Object inVal, double dMultiplier )
	{
		setValue ( inVal );
		d *= dMultiplier;
		if (d>100) d=100.0; 				 
	}
	
	public DisplayFloat ( Object inVal )
	{
		setValue ( inVal );
	}
	
	private void setValue ( Object inVal ) 
	{ 
        if ( inVal instanceof BigDecimal )	
        {
            // Oracle uses these for floats.  Mostly annoying because they
            // "toString" w/o using scientific notation.
            BigDecimal bd = (BigDecimal)inVal;
            d = bd.doubleValue();
        } 
        else if ( inVal instanceof Integer )
        {
        		Integer i = (Integer)inVal;
        		d = i.doubleValue();
        }
        else if ( inVal instanceof Double )
        {
        	// In MySQL this seems to be the default the for MAX/MIN.
        		Double dbl = (Double)inVal;
        		d = dbl.doubleValue();
        }
        else if ( inVal instanceof Float )
        {
        		Float flt = (Float)inVal;
        		d = flt.doubleValue();             	                	
        }
        else if ( inVal instanceof String )
        {
	        	String str = (String)inVal;
	        	if (!(str == null || str.length() == 0)) 
	        		d = Double.parseDouble( str );
	        }
	        else
	        	throw new RuntimeException ( "Unexepected numeric type of " + inVal.getClass() );
	}
	
	public double getValue ( ) { return d; }
	
	public int compareTo ( DisplayFloat r )
	{
		DisplayFloat rNum = r;
		return Double.compare( d, rNum.d );
	}
	public String toString ( ) 
	{
		if (Use_mode == USE_BIGDECIMAL) return useBigDecimal();
		else return useFormat();
	}
	// This routine first applies the decimal rounding to numDec places.
    // If this returns zero, then it goes to significant digits with numSig places.
    // E.g., for numSig=numDec=2 the results are
    // 45.678 --> 45.68, .00045678 --> .00046
    private String useBigDecimal()
    {
	    	BigDecimal bd1 = new BigDecimal(d);
	  
	    	int numSig = Math.min(bd1.precision(), Num_sig); // can't ask for more sig figs than we started with
	    	
	    	BigDecimal bd2 = bd1.setScale(Num_dec, RoundingMode.HALF_UP);
	    	if (bd2.precision() >= numSig) 
	    		return bd2.toString();// didn't lose sig figs with the decimal rounding
	    
	    	BigDecimal bd3 = new BigDecimal(d, new MathContext(numSig));
    		return bd3.toString();
    }
    
	private String useFormat() {
		if (d==0) return "0.0";
		
		String format="";
		double ad = Math.abs(d);
		
		if (ad>=1) {
			if (ad>Largest) format = "%." + Num_sig + "E";
			else            format = "%." + Num_dec + "f";
			
			return String.format(format, d);
		}
		else {
			if (ad<Smallest) {
				format =  "%." + (Num_sig-1) + "E";
				return String.format(format, d);
			}
			else {
				return new BigDecimal(d, new MathContext(Num_sig)).toPlainString();
			}
		}
	}
	
	/*************************************************
	 * Set rounding from defaults or when changed in Columns
	 */	
	public static String getPrefString()
	{
		return Use_mode + "," + Num_sig + "," + Num_dec 
				+ "," + Largest + "," + Smallest;
	}
	public static void setRoundingPrefs(String str)
	{
		if (str.trim().length()==0) return;
		String[] arr = str.trim().split(",");
		int n = arr.length;
		try
		{
			if (n>0) Use_mode = Integer.parseInt(arr[0]);
			if (n>1) Num_sig = Integer.parseInt(arr[1]);
			if (n>2) Num_dec = Integer.parseInt(arr[2]);
			if (n>3) Largest = Double.parseDouble(arr[3]);
			if (n>4) Smallest = Double.parseDouble(arr[4]);
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			System.err.println("Invalid rounding preferences:" + str);
		}
	}
	
	public static void setFields(int mode, int sig, int dec, double lg, double sm) {
    		Use_mode = mode;
    		Num_sig = sig;
    		Num_dec = dec;
    		Largest=lg;
    		Smallest=sm;
    }
	private double d;
}
