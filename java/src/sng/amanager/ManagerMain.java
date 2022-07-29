package sng.amanager;

import util.database.HostsCfg;
import util.database.Globalx;

/************************************************
 * runSingleTCW
 */
public class ManagerMain {
	
	public static void main(String[] args) {
		Globalx.printHeader();
		
		if (hasArgStart(args, "-h") || hasArgStart(args, "--h")) {
			System.out.println("Usage: runSingleTCW [-v]");
			System.out.println("       -v print important MySQL variables and Search paths");
			System.exit(0);
		}
		if (hasArg(args, "-d")) {
			Globalx.debug=true;
			System.out.println("Running in global debug mode");
		}
		if (hasArg(args, "-v")) {
			System.out.println("Check variables and search paths");
			new HostsCfg(true);
			System.exit(0);
		}
		//FileHelpers.mergeDir(Globals.OLDLIBDIR, Globalx.PROJDIR, true); CAS314 don't need anymore
		
		ManagerFrame mf = new ManagerFrame();
		mf.setVisible(true);
	}
	static boolean hasArg(String [] args, String arg) {
		for (int i=0; i<args.length; i++)
			if (args[i].equals(arg)) return true;
		return false;
	}
	static boolean hasArgStart(String [] args, String arg) {
		for (int i=0; i<args.length; i++)
			if (args[i].startsWith(arg)) return true;
		return false;
	}
}
