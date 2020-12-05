package util.methods;

import java.io.PrintWriter;
import java.io.FileWriter;

import javax.swing.JFrame;
import javax.swing.JOptionPane;


import sng.database.Version;
import util.ui.UIHelpers;

/**
 * A set of static methods to report errors to a file for debugging 
 */
public class ErrorReport {
	// Default file name for reporting errors
	public static String strFileName="sTCW.error.log";
	
	// Tracks the number of errors that have been reported
	private static int nNumErrors = 0;

	// Change the target file name for errors;Cmp changes the name to mTCW.error.log
	public static void setErrorReportFileName(String fName) {
		strFileName = fName;
	}
	public static void reportError(String debugInfo) {
		System.err.println("Error: " + debugInfo);
	}
	/**
	 * Allow different parameters for reportError
	 */
	public static void reportError(Throwable e) {
		reportError(e, null, false);
	}
	public static void reportError(String debugInfo, Throwable e) {
        reportError(e, debugInfo);
    }
	public static void reportError(Throwable e, String debugInfo) {
		reportError(e, debugInfo, false);
	}
	public static void prtReport(Throwable e, String debugInfo) {
		reportError(e, debugInfo, false);
	}
	
	public static void reportFatalError(Error e, String debugInfo, JFrame parentFrame) {
		reportError(e, debugInfo);
		
		int answer = 1;			
		if(parentFrame != null ) {
			answer = JOptionPane.showOptionDialog(parentFrame, "A fatal error has occured, do you wish to continue?",
					"TCW", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
		}
		if(answer == 1) System.exit(-1);
	}
	public static void prtToErrFile(String debugInfo) { 
		PrintWriter pWriter = null;
		try {
			pWriter = new PrintWriter(new FileWriter(strFileName, true));
			pWriter.println(debugInfo + "\n");
			pWriter.close();
		} catch (Exception e1) {
			System.err.println("An error has occurred writing to file " + strFileName);
		}
	}
	private static void reportError(Throwable e, String debugInfo, boolean replaceContents) {
		PrintWriter pWriter = null;
		try {
			if(replaceContents) {
				pWriter = new PrintWriter(new FileWriter(strFileName));
				nNumErrors = 0;
			}
			else {
				pWriter = new PrintWriter(new FileWriter(strFileName, true));
			}
		} catch (Exception e1) {
			System.err.println("An error has occurred, however TCW was unable to create an error log file " + 
						strFileName);
			e1.printStackTrace();
			return;
		}
		nNumErrors++;

		if(debugInfo != null) {
			pWriter.println("Error: " + debugInfo);
			System.err.println("Error: " + debugInfo);
			System.err.println("See " + strFileName);
		}
		else System.err.println("Error: see " + strFileName);
	
		String x = (debugInfo!=null) ? debugInfo : "";
		pWriter.println("\n" + Version.TCWhead + " " + TimeHelpers.getDate() + " " + x); 
		e.printStackTrace(pWriter);
		pWriter.close();
	}
	
	/**
	 * Must be called when execution is about to end. If errors have been written the file, the user is prompted
	 * to email the log file to the specified email address
	 */
	public static void notifyUserOnClose(String email)
	{
		if(nNumErrors==0) return;
		
		System.err.println("There were errors during execution. Please email the " + strFileName + 
				" file to " + email + " so that we may correct the problem");
		
		nNumErrors = 0;
	}
	
	public static void die(Throwable e) {
		reportError(e, null);
		System.exit(-1);
	}
	public static void die(Throwable e, String debugInfo) {
		reportError(e, debugInfo);
		System.exit(-1);
	}
	public static void die(String debugInfo, Throwable e) {
		reportError(e, debugInfo);
		System.exit(-1);
	}
	public static void die(String debugInfo) {
		System.err.println("Fatal Error: " + debugInfo);
		System.exit(-1);
	}
	
	public static void ShowMemoryPercentFree()
	{
		Runtime rt = Runtime.getRuntime();
		long diff = rt.totalMemory() - rt.freeMemory();
		float percen = 1 - ((float)diff)/rt.totalMemory();
		System.err.println("Memory " + ((int)(percen * 100)) + "% free");
	}
	
	public static void ShowMemoryCounts()
	{
		System.err.println("Total memory " + Runtime.getRuntime().totalMemory());
		System.err.println("Free memory  " + Runtime.getRuntime().freeMemory());
	}
}
