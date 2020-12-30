package sng.viewer;

import java.util.Vector;

import sng.database.DBInfo;
import sng.database.Overview;
import sng.database.Version;
import util.database.HostsCfg;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.Out;
/* 
 *  Main class for the viewSingleTCW 
 */

public class STCWMain
{	
	public static final String TCW_VERSION = "v" + Version.strTCWver; 
	public static final String TCW_VERSION_STRING = "viewSingleTCW " + 
		TCW_VERSION + " " + Version.strRelDate +" - " +  Version.URL; 

	public static boolean updateMSG = false; // update Overview, accessed from database.Overview
	public static int COVER1=0, COVER2=0;
	public static boolean doTIME = false, test=false;

	public static void main(String[] args)
	{   
		//System.err.println("Starting with: \n" + Runtime.getRuntime().freeMemory() + " bytes free");
		//System.err.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) + " bytes used");
		Out.prt("\n" + STCWMain.TCW_VERSION_STRING);
		
		if (!Version.checkJavaSupported())
			System.exit(-1);
		
		if (hasOption(args, "-h") 
				|| hasOption(args, "-help") 
				|| hasOption(args, "help")) 
		{
			System.err.println(
					"\n"
					+ "Usage:  viewSingleTCW [singleTCW ID or dbName] [-option]\n" 
					+ "	If a singleTCW ID or dbName is NOT provided, a panel of sTCW databases will be shown.\n"
					+ "\n"
					+ "A singleTCW ID or dbName must be provided to use either of the following options:\n"
					+ "  -o Regenerate Overview. o1 and o2 can be used with this option.\n" 
					+ "     -o1 N  set the 1st cover cutoff to N (default N=50)\n"
					+ "     -o2 N  set the 2nd cover cutoff to N (default N=90)\n"
					+ "  -w Output overview to terminal and exit.\n"
					+ "\n"
					+ "viewSingleTCW x  Prints the ID and dbName of all sTCW databases.\n\n"		
					);
			return;
		}
	
		if (hasOption(args, "-o")) {
			System.err.println("Update Overview");
			if (hasOption(args, "-o1")) COVER1 = getOptionNum(args, "-o1");
			if (hasOption(args, "-o2")) COVER2 = getOptionNum(args, "-o2");
			updateMSG = true;
		}
		if (hasOption(args, "-t")) {
			System.err.println("Time query executions turned on.");
			doTIME = true;
		}
		if (hasOption(args, "-test")) { // CAS313
			System.err.println("Test turned on.");
			test = true;
		}
		String dbstr = (args.length>0) ? args[0] : "";  
		HostsCfg hostsObj = new HostsCfg();  
	
		Vector <DBInfo> dbList = DBInfo.getDBlist(hostsObj, dbstr);
		if (dbList==null || dbList.size()==0) {
			System.err.println("No sTCW databases on " + hostsObj.host());
			return;
		}
		DBInfo dbObj = null;
		String dbx="";
		if (args.length > 0) {
			dbx = args[0];
			if (dbx.contains(":")) dbx = args[0].split(":")[0];
			
			for (DBInfo db : dbList) {
				if (db.getID().equals(dbx) || db.getdbName().equals(dbx) 
						|| db.getdbName().equals(Globalx.STCW + dbx)) {
					dbObj = db; 
					break;
				}
			}
			if (dbObj==null) {
				System.err.println("Error: Could not find database with ID or dbName=" + dbx); 
				System.err.println("Valid IDs with dbName:");
				for (DBInfo db : dbList) {
					System.err.format("   %-10s %s\n", db.getID(), db.getdbName());
				}
				System.exit(-1);
			}
			if (dbObj.getID().equals(Globalx.error) && !hasOption(args, "-w")) {
				System.exit(-1);
			}
		}
		// output overview to stdout
		if (hasOption(args, "-w")) {
			System.err.println("Show Overview only");
			showOverview(dbObj.getdbName(), hostsObj);
			System.exit(0);
		}
		STCWChooser.setupUIDefaults();
		if (args.length > 0) {
			STCWFrame frame = new STCWFrame ( hostsObj, dbObj); 
			frame.setVisible( true );
		}
		else {
			STCWChooser frame = new STCWChooser ( hostsObj, dbList ); 	
			frame.setVisible( true );
		}
		return;
	}
	private static int getOptionNum(String [] args, String name) {
		for (int i = 0;  i < args.length;  i++) {
			if (!args[i].equals(name)) continue;
		
			if (args.length>(i+1)) {
				if (args[i+1].startsWith("-")) return 0;
				
				String n = args[i+1];
				try {
					return Integer.parseInt(n);
				}
				catch (Exception e) {return 0;}
			}	
		}
		return 0;
	}
	static boolean hasOption(String[] args, String name)
	{
		for (int i = 0;  i < args.length;  i++)
			if (args[i].equals(name)) 
				return true;
		return false;
	}
	static private void showOverview(String db, HostsCfg hostsObj) {
		try {
			DBConn sdb = hostsObj.getDBConn(db);
			if (sdb==null) {
				System.err.println("Cannot output overview");
			}
			else {
				Overview ov = new Overview(sdb);
				Vector <String> lines = new Vector <String> ();
				ov.overview(lines);
				// just 2 lines, the second one is the Processing
				for (String s : lines) {
					if (s.contains("INFORMATION:")) break;
					System.out.println(s);  
				}
			}
		}
		catch (Exception e) {
			System.err.println("Cannot output overview");
			e.printStackTrace();
		}
	}
}