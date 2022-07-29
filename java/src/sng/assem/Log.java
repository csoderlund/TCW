package sng.assem;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;

import sng.assem.enums.LogAction;
import sng.assem.enums.LogLevel;
import util.file.FileHelpers;
import util.methods.TimeHelpers;

// Logging helper class. 
// Initialize it once to set the static members, and then all methods can be called statically, so
// a log object does not need to be passed around. 

// Most calls take a LogLevel enum, which tells which log to  put the message in, and whether to display it to the screen.
// (These actions are set up during initialization, using the LogAction enum). 
//


public class Log
{
	// log errors
	static public String errFile = "sTCW.error1.log";
	static private int nNumLog=0;
	
	// log status
	static private FileWriter 	dbgFW = null;   // debug file
	static private Formatter 	dbgFormat = null;
	
	static private FileWriter 	logFW = null;
	static private Formatter 	logFormat = null;
	static private String 		mLastMsg;
	static public File 			logFile = null;
	
	static public HashMap<LogLevel,HashSet<LogAction>> mActions = null;

	public Log() {
		mActions = new HashMap<LogLevel,HashSet<LogAction>>();		
		for (LogLevel lvl : LogLevel.values())
			mActions.put(lvl, new HashSet<LogAction>());
	}
	private Log(FileWriter fw) {
		this();
		logFW = fw;
		logFormat = new Formatter(logFW);
	}

	public Log(File file) throws Exception {
		this(new FileWriter(file, true));
		logFile = file;
		
		long fileSize = file.length();
		String fname = FileHelpers.removeRootPath(file.getAbsolutePath()); // CAS404
		if (fileSize==0)
			System.err.println("Log File: " + fname);
		else
			System.err.println("Log file (append): " + fname 
				+ "   Size: " + FileHelpers.getSize(fileSize)); // CAS404 add size
	}
	public static void setDebugLog(File file) throws Exception
	{
		dbgFW = new FileWriter(file,false); 
		dbgFormat = new Formatter(dbgFW);
	}
	public static void addLogAction(LogLevel lvl, LogAction action)
	{
		if (!mActions.containsKey(lvl)) 
			mActions.put(lvl, new HashSet<LogAction>());

		mActions.get(lvl).add(action);
	}
	public static String head1(String s, LogLevel lvl) // CAS404 date-time
	{		
		String x = String.format("%-40s", s);
		return msg("\n>>>" + x + " " + TimeHelpers.getDateTime() + "\n",lvl);
	}
	public static String head(String s, LogLevel lvl) // CAS404 changed to time only
	{		
		String x = String.format("%-40s", s);
		return msg("\n>>>" + x + " " + TimeHelpers.getTimeOnly() + "\n",lvl);
	}
	public static String head(String s) 
	{		
		String x = String.format("%-40s", s);
		return msg("\n>>>" + x + " " + TimeHelpers.getTimeOnly() + "\n", LogLevel.Basic);
	}
	public static String head2(String s) // no initial newline
	{		
		String x = String.format("%-40s", s);
		return msg(">>>" + x + " " + TimeHelpers.getTimeOnly() + "\n", LogLevel.Basic);
	}

	public static String indentMsg(String s, LogLevel lvl)
	{
		return msg("\t" + s,lvl);	
	}
	public static synchronized void finish() {
		try {
			System.err.print("-------------------------------------------------------------\n");
			logFW.write("-------------------------------------------------------------\n");
			logFW.flush();
		}
		catch (Exception e) {}
	}
	// Basic writes to Terminal and Log; Detail also writes to detail log
	public static synchronized String msg(String s, LogLevel lvl) 
	{
		if (mActions == null)
		{
			System.err.println(s);
			mLastMsg = s;
			return mLastMsg;
		}
		try
		{
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.println(s);
				System.err.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFW.write(s + "\n");
				logFW.flush();
			}
			
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFW.write(s + "\n");
				dbgFW.flush();
			}		
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		mLastMsg = s + "\n";;
		return mLastMsg;
	}
	public static String msg(String s)
	{
		return msg(s, LogLevel.Detail);	
	}
	// Indented tables...
	static synchronized String columnsInd(int w, LogLevel lvl, Object... vals) 
	{
		try
		{
			String fmt = "\t";
			for (int i = 1; i <= vals.length; i++)
			{
				fmt += "%-" + w + "s";
			}
			fmt += "\n";
			
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFormat.format(fmt, vals);
				logFormat.flush();
			}
	
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFormat.format(fmt, vals);
				dbgFormat.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.printf(fmt, vals);
			}
			mLastMsg = String.format(fmt,vals);
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		return mLastMsg;
	}	
	static synchronized String columns(int w, LogLevel lvl, Object... vals) 
	{
		try
		{
			String fmt = "";
			for (int i = 1; i <= vals.length; i++)
			{
				fmt += "%-" + w + "s";
			}
			fmt += "\n";
			
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFormat.format(fmt, vals);
				logFormat.flush();
			}
	
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFormat.format(fmt, vals);
				dbgFormat.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.printf(fmt, vals);
			}
			mLastMsg = String.format(fmt,vals);
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		return mLastMsg;
	}
	static String newLine(LogLevel lvl)
	{
		msg("",lvl);	
		return mLastMsg;
	}
	static String columns(int[] ws, LogLevel lvl, Object... vals)
	{
		try
		{
			String fmt = "";
			int nfields = Math.min(ws.length,vals.length);
			for (int i = 0; i < nfields; i++)
			{
				fmt += "%-" + ws[i] + "s";
			}
			fmt += "\n";
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFormat.format(fmt, vals);
				logFormat.flush();
			}
	
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFormat.format(fmt, vals);
				dbgFormat.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.printf(fmt, vals);
			}
			mLastMsg = String.format(fmt,vals) + "\n";;
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		return mLastMsg;
	}

	public static synchronized void  write(char c) {
		System.err.print(c);
	}
	public static void timeStamp() {
      	Date date = new java.util.Date();
 		msg("Current Date Time : " + date.toString(),LogLevel.Detail);
	}
	
	public static void errLog(String msg){
		Log.indentMsg("Error -- " + msg, LogLevel.Basic);
	}
	public static void warnLog(String msg) {
		Log.indentMsg("Warning -- " + msg, LogLevel.Basic);
	}
	static void termOut(String msg)
	{
		System.err.println("\t" + msg);	
	}
	/*********************************************************
	 * Error report
	 * CAS304 add 
	 */
	private static void exception(Throwable e, String debugInfo, boolean replaceContents) {
		
		PrintWriter pWriter = null;
		try {
			if(replaceContents) {
				pWriter = new PrintWriter(new FileWriter(errFile));
				nNumLog = 0; 
			}
			else {
				pWriter = new PrintWriter(new FileWriter(errFile, true));
			}
		} catch (Exception e1) {
			System.err.println("An error has occurred, however TCW was unable to create an error log file " + errFile);
			e1.printStackTrace();
			return;
		}
		if (nNumLog==0) {
			pWriter.println("");
			Date date = new java.util.Date();
			pWriter.println("Current Date Time : " + date.toString());
		}
		nNumLog++; 
		pWriter.println(debugInfo);
		
		if(debugInfo != null) {
			System.err.println(debugInfo + "  " + e.getMessage());
			System.err.println("See " + errFile);
		}
		else System.err.println("Error: see " + errFile);
		
		e.printStackTrace(pWriter);
		pWriter.close();
	}
	private static void out(String debugInfo) {
		PrintWriter pWriter = null;
		try {
			pWriter = new PrintWriter(new FileWriter(errFile, true));
			if (nNumLog==0) {
				pWriter.println("");
				Date date = new java.util.Date();
				pWriter.println("Current Date Time : " + date.toString());
			}
			nNumLog++; 
			pWriter.println(debugInfo);
			
			pWriter.close();	
		} catch (Exception e1) {
			System.err.println("An error has occurred, however TCW was unable to create an error log file " + errFile);
			e1.printStackTrace();
			return;
		}
	}
	public static void warnFile(String s) {
		out( "Warning -- " + s);
	}
	public static void warn(String s) {
		System.err.println( "Warning -- " +s);
		out( "Warning -- " + s);
	}
	public static void errFile(String s) {
		out( "Error   -- " + s);
	}
	public static void error(String s) {
		System.err.println("Error   -- " +s);
		out( "Error   -- " + s);
	}
	public static void warn(Exception e, String s) {
		exception(e, "Warning -- " + s, false);
	}
	public static void error(Exception e, String s) {
		exception(e, "Error   -- " + s, false);
	}
	
	public static void die(Exception e, String s) { 
		exception(e, "Error -- " + s, false);
		die(s);
	}
	public static void die(String s) {
		try {
			msg("Fatal Error -- " + s,LogLevel.Basic);
		}
		catch(Exception e) {
			System.err.println(s);
			System.exit(-1);
		}
		System.exit(-1);
	}
}
