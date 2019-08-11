package sng.runDE;

import java.util.Vector;


import sng.database.DBInfo;
import sng.database.Globals;
import sng.database.Version;
import util.database.HostsCfg;

public class QRMain {
	public static boolean verbose=false;
	public static void main(String[] args) {

		 try {
			 System.err.println("\nrunDE " + Version.TCWhead);
			 if (args.length>0 && args[0].startsWith("-h")) {
				 System.err.println("Execute runDE with no arguments to get TCW database list to choose from");
				 System.err.println("Or: runDE <ID>");
				 System.err.println("Or: runDE <database name>");
				 System.err.println("Or: runDE x  -- will list all TCW database names" );
				 System.err.println("Or: runDE <ID | database name> -v  -- prints filtered rows");
				 System.exit(-1);
			 }
			 HostsCfg hostsObj = new HostsCfg();  
			 
				String dbstr = (args.length>0) ? args[0] : ""; 
			 Vector <DBInfo> dbList = DBInfo.getDBlist(hostsObj, dbstr);
			 if (dbList==null || dbList.size()==0) {
				System.err.println("No sTCW databases on " + hostsObj.host());
				System.exit(-1);
			 }
			
			 if (args.length == 0) 
				 new QRFrame(hostsObj, dbList);
			 else { 
				if (args.length>1 && args[1].equals("-v")) {
					verbose = true;
					System.out.println("Verbose mode");
				}
				String dbx = args[0];
				DBInfo dbObj = null;
				for (DBInfo db : dbList) {
					if (db.getID().equals(dbx) || db.getdbName().equals(dbx)
							|| db.getdbName().equals(Globals.STCW + dbx)) {
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
				new QRFrame(hostsObj, dbObj);
			 }
		}
		catch(java.lang.UnsatisfiedLinkError e)
		{
			System.out.println("\n\n***Probably you need to put libjri.so into one of these directories:***\n");
			System.out.println(System.getProperty("java.library.path") );
			e.printStackTrace();
		}
		catch(Exception e)
		{
			System.out.println("Fatal error in runDE");
			e.printStackTrace();
		}
	}
}
