package cmp.viewer;

import util.database.Globalx;

public class MTCWMain {
	public static boolean test = false;
	public static final String mTCW_VERSION_STRING = "viewMultiTCW " + 
			"v" + Globalx.strTCWver + " " + Globalx.strRelDate +" - " +  Globalx.URL; 
	
	public static void main(String[] args) {
		System.err.println(mTCW_VERSION_STRING);
		
		MTCWFrame mainWindow = null;
		
		if (args.length>0 && (args[0].equals("-h") || (args[0].equals("-help")))) {
			System.err.println("\nviewMultiTCW [dbname]");
			System.err.println("   dbname: mTCW database name. It does not need the prefix 'mTCW'.");
			System.err.println("   If no dbname is supplied, a mTCW database chooser window will popup. ");
			System.err.println("-o after the dbname will cause the Overview to be regenerated.");
			System.err.println("-  list all mTCW databases.\n");
			System.exit(0);
		}
		if(args.length > 0)
			mainWindow = new MTCWFrame(args[0], args);
		else
			mainWindow = new MTCWFrame();
		
		mainWindow.setVisible(true);
		return;
	}
}
