package cmp.viewer;

import util.database.Globalx;
import util.methods.Out;

public class MTCWMain {
	public static boolean test = false;
	
	public static void main(String[] args) {
		Globalx.printHeader();;
		
		MTCWFrame mainWindow = null;
		
		if (args.length>0 && (args[0].equals("-h") || (args[0].equals("-help")))) {
			System.err.println("\nviewMultiTCW [dbname]");
			System.err.println("   dbname: mTCW database name. It does not need the prefix 'mTCW'.");
			System.err.println("   If no dbname is supplied, a mTCW database chooser window will popup. ");
			System.err.println("-o after the dbname will cause the Overview to be regenerated.");
			System.err.println("-  list all mTCW databases.\n");
			System.exit(0);
		}
		if(args.length > 0) {
			if (hasArg(args, "-test")) { 
				Out.prt("Test mode on");
				test=true; // CAS313 added for ScoreMulti SoP
			}
			
			mainWindow = new MTCWFrame(args[0], args);
		}
		else
			mainWindow = new MTCWFrame();
		
		mainWindow.setVisible(true);
		return;
	}
	static boolean hasArg(String [] args, String arg) { 
		for (int i=0; i<args.length; i++)
			if (args[i].equals(arg)) return true;
		return false;
	}
}
