package sng.amanager;

import sng.database.Globals;
import sng.database.Version;
import util.database.Globalx;
import util.database.HostsCfg;
import util.methods.FileHelpers;

public class ManagerMain {
	static public boolean verbose=false, chkDB=false;
	
	public static void main(String[] args) {
		System.out.println("\n----- runSingleTCW v" + Version.strTCWver + " " + 
				Version.strRelDate + " " + Version.URL + "-----");
		
		if (hasArgStart(args, "-h") || hasArgStart(args, "--h")) {
			System.out.println("Usage: runSingleTCW [-v]");
			System.out.println("       -v print important MySQL variables");
			System.exit(0);
		}
		if (hasArg(args, "-d")) {
			verbose=true;
			System.out.println("Running in verbose mode");
		}
		if (hasArg(args, "-v")) {
			chkDB=true;
			System.out.println("Check MySQL variables and search paths");
			new HostsCfg(true);
			System.exit(0);
		}
		FileHelpers.mergeDir(Globals.OLDLIBDIR, Globalx.PROJDIR, true); 
		
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
