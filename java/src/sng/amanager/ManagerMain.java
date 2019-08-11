package sng.amanager;

import sng.database.Globals;
import sng.database.Version;
import util.database.Globalx;
import util.methods.FileHelpers;

public class ManagerMain {
	static boolean verbose=false;
	
	public static void main(String[] args) {
		System.out.println("\n----- runSingleTCW v" + Version.strTCWver + " " + 
				Version.strRelDate + " " + Version.URL + "-----");
		if (hasArg(args, "-v")) {
			verbose=true;
			System.out.println("Running in verbose mode");
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
}
