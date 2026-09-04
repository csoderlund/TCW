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
		if (hasArg(args, "-v")) {
			System.out.println("Check variables and search paths");
			new HostsCfg(true);
			System.exit(0);
		}
		ManagerFrame mf = new ManagerFrame(); // Executions are done in RunCmd -- no command line are passed to it
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
