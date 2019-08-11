package sng.database;

import util.database.Globalx;

/**********************************************
 * Information about versions.
 * Database schema updated in Schema.java
 */

public class Version {
	// These top two are used for both sTCW and mTCW. Everything else is strictly sTCW.
	public static final String strRelDate = 	Globalx.strRelDate; 
	public static final String strTCWver = 	Globalx.strTCWver;
	public static final String URL = 		Globalx.URL;
	public static final String TCWhead = 	Globalx.TCWhead;
	
	public static final String strDBver = "5.4"; // singleTCW database version
	public static final String sTCWhead = "sTCW v" + strTCWver + " " + strRelDate; 	
	public static final String sTCWtitle = "viewSingleTCW v" + strTCWver;
	private static final int REQUIRED_JAVA_MAJOR_VERSION = 1; 
	private static final int REQUIRED_JAVA_MINOR_VERSION = 7; 
	
	public static boolean checkJavaSupported() {	
		String version = System.getProperty("java.version");
		if (version != null) {
			String[] subs = version.split("\\.");
			int major = 0;
			int minor = 0;

			if (subs.length > 0) major  = Integer.valueOf(subs[0]);
			if (subs.length > 1) minor  = Integer.valueOf(subs[1]);
			
			if (major < REQUIRED_JAVA_MAJOR_VERSION
				|| (major == REQUIRED_JAVA_MAJOR_VERSION && minor < REQUIRED_JAVA_MINOR_VERSION))
			{
				String msg = 
						"The installed Java version is "
					    + getInstalledJavaVersionStr() + ".  \n"
					    + "TCW requires version "
					    + getRequiredJavaVersionStr() + " or later.  \n"
					    + "Please visit http://java.com/download/ to upgrade.";
				System.err.println(msg);
			}
			return true;
		}
		else 
			System.err.println("Could not determine java version.");
		
		return false;
	}
	
	private static String getRequiredJavaVersionStr() {
		return REQUIRED_JAVA_MAJOR_VERSION
				+ "." + REQUIRED_JAVA_MINOR_VERSION;
	}

	private static String getInstalledJavaVersionStr() {
		return System.getProperty("java.version");
	}

	public static void printTCWversion()
	{
		System.err.println("\nTCW (Transcriptome Computational Workbench) v" + Globalx.strTCWver + "\n");
	}
}
