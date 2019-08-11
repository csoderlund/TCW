package util.ui;

/**
 * @author brian
 *
 * A simple class to make it easier to have combo boxes where the
 * descriptive name maps to number internally.
 */
public class MenuMapper 
{
	public MenuMapper ( String strInName, int nInVal )
	{
		strMenuName = strInName;
		fNumber = (double)nInVal;
	}
	
	public boolean equals ( Object obj )
	{
		MenuMapper in = (MenuMapper)obj;
		
		if ( fNumber == in.fNumber )
		{
			// OK, this is a side effect... sorry.  Cheap and easy way
			// to prevent blank combo selections
			if ( in.strMenuName == null || in.strMenuName == "" ) in.strMenuName = strMenuName;
			if ( strMenuName == null || strMenuName == "" )    strMenuName = in.strMenuName;		
				
			return true;
		}
		else return false;
	}
	
	public String toString () { return strMenuName; };
	
	public int asInt ( ) { return (int)fNumber; }
	
	private String strMenuName;
	private double fNumber;
}
